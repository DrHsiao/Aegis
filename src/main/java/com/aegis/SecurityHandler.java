package com.aegis;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
@ChannelHandler.Sharable
public class SecurityHandler extends ChannelInboundHandlerAdapter {

    private final IPBlacklistManager blacklistManager;

    public SecurityHandler(IPBlacklistManager blacklistManager) {
        this.blacklistManager = blacklistManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String clientIp = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();

        if (blacklistManager.isBlacklisted(clientIp)) {
            log.warn("Blocking connection from blacklisted IP: {}", clientIp);

            // 傳回 403 Forbidden
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.FORBIDDEN,
                    Unpooled.copiedBuffer("Access Denied: Your IP is blacklisted.", CharsetUtil.UTF_8));
            response.headers().set("Content-Type", "text/plain; charset=UTF-8");
            response.headers().set("Content-Length", response.content().readableBytes());

            ctx.writeAndFlush(response).addListener(f -> ctx.close());
            return;
        }

        super.channelActive(ctx);
    }
}
