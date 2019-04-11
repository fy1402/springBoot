package com.example.netty.test;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;


/**
 * 继承 MessageToMessageEncoder 用于编码消息到另外一种格式
 * 检查是否有足够的字节用于编码
 * 读取下一个输入 ByteBuf 产出的 int 值，并计算绝对值
 * 写 int 到编码的消息 List
 */
public class AbsIntegerEncoder extends MessageToMessageEncoder<ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        while (in.readableBytes() >= 4) { //2
            int value = Math.abs(in.readInt());//3
            out.add(value);  //4
        }
    }
}
