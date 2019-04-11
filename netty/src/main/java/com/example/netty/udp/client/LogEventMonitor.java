package com.example.netty.udp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;


/**
 * 引导 NioDatagramChannel。设置 SO_BROADCAST socket 选项。
 * 添加 ChannelHandler 到 ChannelPipeline
 * 绑定的通道。注意,在使用 DatagramChannel 是没有连接，因为这些 无连接
 * 构建一个新的 LogEventMonitor
 */
public class LogEventMonitor {

    private final Bootstrap bootstrap;
    private final EventLoopGroup group;
    public LogEventMonitor(InetSocketAddress address) {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)  //1
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast(new LogEventDecoder());  //2
                        pipeline.addLast(new LogEventHandler());
                    }
                }).localAddress(address);

    }

    public Channel bind() {
        return bootstrap.bind().syncUninterruptibly().channel();  //3
    }

    public void stop() {
        group.shutdownGracefully();
    }

    public static void main(String[] args) throws Exception {
//        if (args.length != 1) {
//            throw new IllegalArgumentException("Usage: LogEventMonitor <port>");
//        }
        LogEventMonitor monitor = new LogEventMonitor(new InetSocketAddress(8087));  //4
        try {
            Channel channel = monitor.bind();
            System.out.println("LogEventMonitor running");

            channel.closeFuture().await();
        } finally {
            monitor.stop();
        }
    }
}
