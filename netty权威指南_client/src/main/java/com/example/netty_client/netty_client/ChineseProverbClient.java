package com.example.netty_client.netty_client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * Created by i-feng on 2019/4/16.
 */

@Log4j2
@Order(value = 5)
@Component
public class ChineseProverbClient implements CommandLineRunner{

    @Override
    public void run(String... strings) throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
//                try {
//                    connect(8085);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
        }).start();
    }

    private void connect(int port) throws InterruptedException {

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b
                    .group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChineseProverbClientHandler());

            ChannelFuture f = b.bind(port).sync();

            log.info("开始谚语字典查询");
            DatagramPacket datagramPacket = new DatagramPacket(Unpooled.copiedBuffer("谚语字典查询?", CharsetUtil.UTF_8), new InetSocketAddress("192.168.199.213", port));
            f.channel().writeAndFlush(datagramPacket).sync();
            if (!f.channel().closeFuture().await(15000)) {
                log.info("查询超时！");
            }
        } finally {
            group.shutdownGracefully();
        }
    }




}

@Log4j2
class ChineseProverbClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
            String response = datagramPacket.content().toString(CharsetUtil.UTF_8);
            if (response.startsWith("谚语查询结果："))  {
                log.info(response);
                channelHandlerContext.close();
            }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }
}
