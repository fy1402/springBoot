package com.example.netty.echo.server;

import com.example.netty.echo.handler.EchoServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;

//        创建 ServerBootstrap 实例来引导服务器并随后绑定
//        创建并分配一个 NioEventLoopGroup 实例来处理事件的处理，如接受新的连接和读/写数据。
//        指定本地 InetSocketAddress 给服务器绑定
//        通过 EchoServerHandler 实例给每一个新的 Channel 初始化
//        最后调用 ServerBootstrap.bind() 绑定服务器


@Log4j2
public class EchoServer {

    private final int port = 8090;

    public EchoServer() {}

//    public static void main(String[] args) throws Exception {
//        if (args.length != 1) {
//            log.info(
//                    "Usage: " + EchoServer.class.getSimpleName() +
//                            " <port>");
//            return;
//        }
//        int port = Integer.parseInt(args[0]);        //1 设置端口值（抛出一个 NumberFormatException 如果该端口参数的格式不正确）
//        new EchoServer(port).start();                //2 呼叫服务器的 start() 方法
//    }

    public void start() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup(); //3 创建 EventLoopGroup
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)                                //4 创建 ServerBootstrap
                    .channel(NioServerSocketChannel.class)        //5  指定使用 NIO 的传输 Channel
                    .localAddress(new InetSocketAddress(port))    //6  设置 socket 地址使用所选的端口
                    .childHandler(new ChannelInitializer<SocketChannel>() { //7  添加 EchoServerHandler 到 Channel 的 ChannelPipeline
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            ch.pipeline().addLast(
                                    new EchoServerHandler());
                        }
                    });

            ChannelFuture f = b.bind().sync();            //8 绑定的服务器;sync 等待服务器关闭
            System.out.println(EchoServer.class.getName() + " started and listen on " + f.channel().localAddress());
            f.channel().closeFuture().sync();            //9 关闭 channel 和 块，直到它被关闭
        } finally {
            group.shutdownGracefully().sync();            //10 关机的 EventLoopGroup，释放所有资源。
        }
    }
}
