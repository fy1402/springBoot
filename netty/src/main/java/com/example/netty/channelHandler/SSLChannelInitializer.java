package com.example.netty.channelHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

public class SSLChannelInitializer extends ChannelInitializer<Channel> {

    private final SslContext context;
    private final boolean startTls;
    private final boolean client;
    public SSLChannelInitializer(SslContext context, boolean client, boolean startTls) {   //1
        this.context = context;
        this.startTls = startTls;
        this.client = client;
    }

//    在大多数情况下,SslHandler 将成为 ChannelPipeline 中的第一个 ChannelHandler 。
// 这将确保所有其他 ChannelHandler 应用他们的逻辑到数据后加密后才发生,从而确保他们的变化是安全的。

    @Override
    protected void initChannel(Channel ch) throws Exception {
        SSLEngine engine = context.newEngine(ch.alloc());  //2
        engine.setUseClientMode(client); //3
        ch.pipeline().addFirst("ssl", new SslHandler(engine, startTls));  //4
    }
}
