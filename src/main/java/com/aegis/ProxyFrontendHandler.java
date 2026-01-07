package com.aegis;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ProxyFrontendHandler extends ChannelInboundHandlerAdapter {

    private final AppConfig config;
    private final RuleManager ruleManager;
    private final DisruptorManager disruptorManager;
    private volatile Channel outboundChannel;
    private final List<Object> pendings = new ArrayList<>();
    private boolean isTransparentMode = false;

    public ProxyFrontendHandler(AppConfig config, RuleManager ruleManager, DisruptorManager disruptorManager) {
        this.config = config;
        this.ruleManager = ruleManager;
        this.disruptorManager = disruptorManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final Channel inboundChannel = ctx.channel();

        // Start the connection attempt to upstream
        Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop())
                .channel(NettyTransportFactory.getSocketChannelClass())
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        // For HTTP proxying to IIS/Tomcat
                        // We use HttpClientCodec to talk to upstream
                        ch.pipeline().addLast(new HttpClientCodec());
                        ch.pipeline().addLast(new ProxyBackendHandler(inboundChannel));
                    }
                });

        ChannelFuture f = b.connect(config.getUpstream().getHost(), config.getUpstream().getPort());
        outboundChannel = f.channel();

        f.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("Connected to upstream: {}:{}", config.getUpstream().getHost(),
                        config.getUpstream().getPort());
                synchronized (pendings) {
                    for (Object pending : pendings) {
                        outboundChannel.writeAndFlush(pending);
                    }
                    pendings.clear();
                }
            } else {
                log.error("Failed to connect to upstream", future.cause());
                inboundChannel.close();
                synchronized (pendings) {
                    for (Object pending : pendings) {
                        ReferenceCountUtil.release(pending);
                    }
                    pendings.clear();
                }
            }
        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;

            // 處理 /health/live 端點
            if ("/health/live".equals(request.uri())) {
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                        Unpooled.copiedBuffer("OK", CharsetUtil.UTF_8));
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                return;
            }

            // 1. 清除舊 X-Forwarded-For，注入真實 Socket IP
            request.headers().remove("X-Forwarded-For");
            String remoteIp = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
            request.headers().add("X-Forwarded-For", remoteIp);

            // 2. 移除 Accept-Encoding Header (強制後端回傳 Plain Text)
            request.headers().remove(HttpHeaderNames.ACCEPT_ENCODING);

            // 異步分析：將流量數據推送至 Disruptor RingBuffer
            if (request instanceof FullHttpRequest) {
                FullHttpRequest fullRequest = (FullHttpRequest) request;
                String body = fullRequest.content().toString(CharsetUtil.UTF_8);
                disruptorManager.publishEvent(remoteIp, request.method().name(), request.uri(), body);
            }

            // 偵測 NTLM/Negotiate
            String authHeader = request.headers().get(HttpHeaderNames.AUTHORIZATION);
            if (authHeader != null && (authHeader.startsWith("NTLM") || authHeader.startsWith("Negotiate"))) {
                log.info("NTLM/Negotiate detected, enabling connection pinning and transparent mode");
                isTransparentMode = true;
            }

            // 3. 處理 "Connection: Upgrade" (WebSocket/SignalR)
            if (request.headers().contains(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true) ||
                    request.headers().contains(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true)) {
                log.info("WebSocket/Upgrade detected, switching to transparent mode");
                isTransparentMode = true;
            }
        }

        if (outboundChannel != null && outboundChannel.isActive()) {
            outboundChannel.writeAndFlush(msg).addListener(f -> {
                if (f.isSuccess()) {
                    if (isTransparentMode) {
                        // 一旦偵測到升級或 NTLM，切換為全透傳模式
                        switchToTransparentMode(ctx);
                        switchToTransparentMode(outboundChannel.pipeline().context(ProxyBackendHandler.class));
                    }
                } else {
                    log.error("Forward failed", f.cause());
                    ctx.channel().close();
                }
            });
        } else {
            // Upstream connection not ready yet, buffer the message
            synchronized (pendings) {
                if (outboundChannel != null && outboundChannel.isActive()) {
                    outboundChannel.writeAndFlush(msg);
                } else {
                    pendings.add(msg);
                }
            }
        }
    }

    private void switchToTransparentMode(Object contextOrCtx) {
        ChannelPipeline pipeline;
        if (contextOrCtx instanceof ChannelHandlerContext) {
            pipeline = ((ChannelHandlerContext) contextOrCtx).pipeline();
        } else {
            return;
        }

        // Remove HTTP related handlers to allow raw ByteBuf forwarding
        if (pipeline.get(HttpRequestDecoder.class) != null) {
            pipeline.remove(HttpRequestDecoder.class);
        }
        if (pipeline.get(HttpResponseEncoder.class) != null) {
            pipeline.remove(HttpResponseEncoder.class);
        }
        if (pipeline.get(HttpClientCodec.class) != null) {
            pipeline.remove(HttpClientCodec.class);
        }
        if (pipeline.get(HttpObjectAggregator.class) != null) {
            pipeline.remove(HttpObjectAggregator.class);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            ProxyFrontendHandler.closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Frontend error", cause);
        ProxyFrontendHandler.closeOnFlush(ctx.channel());
    }

    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
