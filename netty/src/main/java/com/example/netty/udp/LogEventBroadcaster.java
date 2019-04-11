package com.example.netty.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;

/**
 * 引导 NioDatagramChannel 。为了使用广播，我们设置 SO_BROADCAST 的 socket 选项
 * 绑定管道。注意当使用 Datagram Channel 时，是没有连接的
 * 如果需要，可以设置文件的指针指向文件的最后字节
 * 设置当前文件的指针，这样不会把旧的发出去
 * 写一个 LogEvent 到管道用于保存文件名和文件实体。(我们期望每个日志实体是一行长度)
 * 存储当前文件的位置，这样，我们可以稍后继续
 * 睡 1 秒。如果其他中断退出循环就重新启动它。
 * 构造一个新的实例 LogEventBroadcaster 并启动它
 */
public class LogEventBroadcaster {

    private final Bootstrap bootstrap;
    private final File file;
    private final EventLoopGroup group;

    public LogEventBroadcaster(InetSocketAddress address, File file) {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new LogEventEncoder(address)); //1

        this.file = file;
    }

    public void run() throws IOException {
        Channel ch = bootstrap.bind(0).syncUninterruptibly().channel(); //2
        System.out.println("LogEventBroadcaster running");
        long pointer = 0;
        for (;;) {
            long len = file.length();
            if (len < pointer) {
                // file was reset
                pointer = len; //3
            } else if (len > pointer) {
                // Content was added
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                raf.seek(pointer);  //4
                String line;
                while ((line = raf.readLine()) != null) {
                    ch.writeAndFlush(new LogEvent(null, -1, file.getAbsolutePath(), line));  //5
                }
                pointer = raf.getFilePointer(); //6
                raf.close();
            }
            try {
                Thread.sleep(1000);  //7
            } catch (InterruptedException e) {
                Thread.interrupted();
                break;
            }
        }
    }

    public void stop() {
        group.shutdownGracefully();
    }

    public static void main(String[] args) throws Exception {
//        if (args.length != 2) {
//            throw new IllegalArgumentException();
//        }
        File file = new File("/Users/fy/Desktop/log.log");
        LogEventBroadcaster broadcaster = new LogEventBroadcaster(new InetSocketAddress("255.255.255.255",
                8087), file);  //8
        try {
            broadcaster.run();
        } finally {
            broadcaster.stop();
        }
    }
}
