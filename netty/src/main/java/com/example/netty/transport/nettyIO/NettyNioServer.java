package com.example.netty.transport.nettyIO;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.log4j.Log4j2;

import java.nio.charset.Charset;

/**
 * 1.创建一个 ServerBootstrap
 * 2.使用 NioEventLoopGroup 允许非阻塞模式（NIO）
 * 3.指定 ChannelInitializer 将给每个接受的连接调用
 * 4.添加的 ChannelInboundHandlerAdapter() 接收事件并进行处理
 * 5.写信息到客户端，并添加 ChannelFutureListener 当一旦消息写入就关闭连接
 * 6.绑定服务器来接受连接
 * 7.释放所有资源
 */

@Log4j2
public class NettyNioServer {

    public void server(int port) throws InterruptedException {
        final ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hi!\r\n", Charset.forName("UTF-8")));

        NioEventLoopGroup group = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();    //1
            b.group(new NioEventLoopGroup(), new NioEventLoopGroup())
                    .channel(NioServerSocketChannel.class)
                    .localAddress(port)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
//                                    super.channelActive(ctx);
                                    ctx.writeAndFlush(buf.duplicate())                //5
                                            .addListener(ChannelFutureListener.CLOSE);
                                    log.info(buf.duplicate());
                                }
                            });
                        }
                    });
            ChannelFuture future = b.bind().sync();
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}
