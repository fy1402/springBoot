package com.example.netty.Bootstrapping;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;

import java.net.InetSocketAddress;

/**
 * 创建一个新的 ServerBootstrap 来创建和绑定新的 Channel
 * 指定 EventLoopGroups 从 ServerChannel 和接收到的管道来注册并获取 EventLoops
 * 指定 Channel 类来使用
 * 设置处理器用于处理接收到的管道的 I/O 和数据
 * 通过配置的引导来绑定管道
 * ChannelInitializer 负责设置 ChannelPipeline
 * 实现 initChannel() 来添加需要的处理器到 ChannelPipeline。一旦完成了这方法 ChannelInitializer 将会从 ChannelPipeline 删除自身。
 * 通过 ChannelInitializer, Netty 允许你添加你程序所需的多个 ChannelHandler 到 ChannelPipeline
 */
public class UsingChannelInitializer {

    public void server() throws InterruptedException {

        ServerBootstrap bootstrap = new ServerBootstrap();//1
        bootstrap.group(new NioEventLoopGroup(), new NioEventLoopGroup())  //2
                .channel(NioServerSocketChannel.class)  //3
                .childHandler(new ChannelInitializerImpl()); //4
        ChannelFuture future = bootstrap.bind(new InetSocketAddress(8080));  //5
        future.sync();
    }
}

final class ChannelInitializerImpl extends ChannelInitializer<Channel> {  //6
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline(); //7
        pipeline.addLast(new HttpClientCodec());
        pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));

    }
}
