package com.example.netty.run;


import com.example.netty.echo.server.EchoServer;
import com.example.netty.transport.nativeIO.IO.PlainOioServer;
import com.example.netty.webSocket.config.MyWebSocketChannelHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Log4j2
@Component
public class WebSocketConfig implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                initWebSocket();
            }
        }).start();
    }

    public void initWebSocket() {
       log.info("服务端开始启用webSocket连接...");
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workGroup);
            b.channel(NioServerSocketChannel.class);
            b.childHandler(new MyWebSocketChannelHandler());

            log.info("服务端开启等待客户端webSocket连接...");

            Channel channel = b.bind(8081).sync().channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 优雅退出
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

}
