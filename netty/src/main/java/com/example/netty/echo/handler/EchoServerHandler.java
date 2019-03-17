package com.example.netty.echo.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import lombok.extern.log4j.Log4j2;


// 将会将接受到的数据的拷贝发送给客户端

//     ChannelHandler 是给不同类型的事件调用
//     应用程序实现或扩展 ChannelHandler 挂接到事件生命周期和 提供自定义应用逻辑。


@Log4j2
@ChannelHandler.Sharable  // 标识这类的实例之间可以在 channel 里面共享
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    // 每个信息入站都会调用
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        super.channelRead(ctx, msg);
        ByteBuf in = (ByteBuf) msg;
        log.info("Server received: " + in.toString(CharsetUtil.UTF_8));
        ctx.write(in);  // 将所接收的消息返回给发送者。注意，这还没有冲刷数据
    }

    // 通知处理器最后的 channelread() 是当前批处理中的最后一条消息时调用
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//        super.channelReadComplete(ctx);
        //冲刷所有待审消息到远程节点。关闭通道后，操作完成
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

//    读操作时捕获到异常时调用
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        ctx.close();
    }


}
