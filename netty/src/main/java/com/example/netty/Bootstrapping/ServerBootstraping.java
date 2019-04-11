package com.example.netty.Bootstrapping;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;


/**
 * 创建要给新的 ServerBootstrap 来创建新的 SocketChannel 管道并绑定他们
 * 指定 EventLoopGroup 用于从注册的 ServerChannel 中获取EventLoop 和接收到的管道
 * 指定要使用的管道类
 * 设置子处理器用于处理接收的管道的 I/O 和数据
 * 通过配置引导来绑定管道
 */
public class ServerBootstraping {

    public void server() {
        NioEventLoopGroup group = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap(); //1
        bootstrap.group(group) //2
                .channel(NioServerSocketChannel .class) //3
                .childHandler(new SimpleChannelInboundHandler<ByteBuf>() { //4
                                  @Override
                                  protected void channelRead0(ChannelHandlerContext ctx,
                                                              ByteBuf byteBuf) throws Exception {
                                      System.out.println("Reveived data");
                                      byteBuf.clear();
                                  }
                              }
                );
        ChannelFuture future = bootstrap.bind(new InetSocketAddress(8080)); //5
        future.addListener(new ChannelFutureListener() {
                               @Override
                               public void operationComplete(ChannelFuture channelFuture)
                                       throws Exception {
                                   if (channelFuture.isSuccess()) {
                                       System.out.println("Server bound");
                                   } else {
                                       System.err.println("Bound attempt failed");
                                       channelFuture.cause().printStackTrace();
                                   }
                               }
                           }
        );
    }


}
