package com.example.netty.webSocket.config;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;


// Netty的配置信息
public class NettyConfig {

    // 存储每一个客户端进来的channel对象
    public static ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

}
