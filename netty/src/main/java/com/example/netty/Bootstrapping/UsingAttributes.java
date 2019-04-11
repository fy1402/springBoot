package com.example.netty.Bootstrapping;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;

/**
 * 新建一个 AttributeKey 用来存储属性值
 * 新建 Bootstrap 用来创建客户端管道并连接他们
 * 指定 EventLoopGroups 从和接收到的管道来注册并获取 EventLoop
 * 指定 Channel 类
 * 设置处理器来处理管道的 I/O 和数据
 * 检索 AttributeKey 的属性及其值
 * 设置 ChannelOption 将会设置在管道在连接或者绑定
 * 存储 id 属性
 * 通过配置的 Bootstrap 来连接到远程主机
 */
public class UsingAttributes {

//    private final AttributeKey<Integer> id = new AttributeKey<Integer>("ID");//1

    public void server() {

        Bootstrap bootstrap = new Bootstrap(); //2
        bootstrap.group(new NioEventLoopGroup()) //3
                .channel(NioSocketChannel.class) //4
                .handler(new SimpleChannelInboundHandler<ByteBuf>() { //5
                    @Override
                    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
//                        Integer idValue = ctx.channel().attr(id).get();  //6
                        // do something  with the idValue
                    }

                    @Override
                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
                        System.out.println("Reveived data");
                    }
                });
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);   //7
//        bootstrap.attr(id, 123456); //8

        ChannelFuture future = bootstrap.connect(new InetSocketAddress("www.manning.com", 80));   //9
        future.syncUninterruptibly();
    }
}
