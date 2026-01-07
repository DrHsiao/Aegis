package com.aegis;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class AegisServer {

    private final AppConfig config;
    private final RuleManager ruleManager;
    private final DisruptorManager disruptorManager;
    private final IPBlacklistManager blacklistManager;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public AegisServer(AppConfig config, RuleManager ruleManager, DisruptorManager disruptorManager,
            IPBlacklistManager blacklistManager) {
        this.config = config;
        this.ruleManager = ruleManager;
        this.disruptorManager = disruptorManager;
        this.blacklistManager = blacklistManager;
    }

    public void start() throws InterruptedException {
        int port = config.getServer().getPort();

        SslContext sslContext = null;
        try {
            sslContext = SslContextFactory.createSslContext(config.getServer().getSsl());
        } catch (Exception e) {
            log.error("Failed to initialize SSL context", e);
            return;
        }

        final SslContext finalSslContext = sslContext;
        bossGroup = NettyTransportFactory.createEventLoopGroup(1, null);
        workerGroup = NettyTransportFactory.createEventLoopGroup(0, null);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NettyTransportFactory.getServerSocketChannelClass())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            // 0. 開頭檢查 IP 黑名單
                            ch.pipeline().addLast(new SecurityHandler(blacklistManager));

                            if (finalSslContext != null) {
                                ch.pipeline().addLast(finalSslContext.newHandler(ch.alloc()));
                            }
                            // 關鍵要求：設定 maxHeaderSize 為 32768 (32KB)
                            // HttpRequestDecoder(int maxInitialLineLength, int maxHeaderSize, int
                            // maxChunkSize)
                            ch.pipeline().addLast(new HttpRequestDecoder(4096, 32768, 8192));
                            ch.pipeline().addLast(new HttpResponseEncoder());

                            // 1. 加入 HttpObjectAggregator (Max 2MB) 以組裝 Chunked 請求
                            int maxContentLength = parseMaxContentLength(config.getMaxContentLength());
                            ch.pipeline().addLast(new HttpObjectAggregator(maxContentLength));

                            // 實作 ProxyFrontendHandler: 處理轉發邏輯，傳入 ruleManager 與 disruptorManager
                            ch.pipeline().addLast(new ProxyFrontendHandler(config, ruleManager, disruptorManager));
                        }
                    });

            log.info("Aegis Server starting on port {}", port);
            ChannelFuture f = b.bind(port).sync();
            log.info("Aegis Server started successfully");
            f.channel().closeFuture().sync();
        } finally {
            stop();
        }
    }

    public void stop() {
        if (bossGroup != null)
            bossGroup.shutdownGracefully();
        if (workerGroup != null)
            workerGroup.shutdownGracefully();
        log.info("Aegis Server stopped");
    }

    private int parseMaxContentLength(String size) {
        if (size == null || size.isEmpty())
            return 2 * 1024 * 1024;
        Pattern pattern = Pattern.compile("(\\d+)\\s*(KB|MB|GB|B)?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(size);
        if (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            if (unit == null)
                return value;
            switch (unit.toUpperCase()) {
                case "KB":
                    return value * 1024;
                case "MB":
                    return value * 1024 * 1024;
                case "GB":
                    return value * 1024 * 1024 * 1024;
                default:
                    return value;
            }
        }
        return 2 * 1024 * 1024;
    }
}
