package com.example.netty;

import com.example.netty.test.FixedLengthFrameDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.CharsetUtil;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.Test;


/**
 * 测试增加 @Test 注解
 * 新建 ByteBuf 并用字节填充它
 * 新增 EmbeddedChannel 并添加 FixedLengthFrameDecoder 用于测试
 * 写数据到 EmbeddedChannel
 * 标记 channel 已经完成
 * 读产生的消息并且校验
 *
 *
 * duplicate()共享数据缓冲区,独立维护readerindex和writerindex
 * slice()共享数据缓冲区中可读的一部分,独立维护readerindex和writerindex
 * slice(int, int)共享数据缓冲区中可读的一部分,独立维护readerindex和writerindex
 * readSlice(int) read表示会增加源ByteBuf的readerindex
 * retainedDuplicate() retained 表示会增加引用计数
 * retainedSlice()
 * retainedSlice(int, int)
 * readRetainedSlice(int)
 */

@Log4j2
public class FixedLengthFrameDecoderTest {

    @Test    //1
    public void testFramesDecoded() {
        ByteBuf buf = Unpooled.buffer(); //2
        for (int i = 0; i < 9; i++) {
            buf.writeByte(i);
        }
        ByteBuf input = buf.duplicate();

        EmbeddedChannel channel = new EmbeddedChannel(new FixedLengthFrameDecoder(3)); //3
        Assert.assertFalse(channel.writeInbound(input.readBytes(2))); //4
        Assert.assertTrue(channel.writeInbound(input.readBytes(7)));

        Assert.assertTrue(channel.finish());  //5
        ByteBuf read = (ByteBuf) channel.readInbound();
        Assert.assertEquals(buf.readSlice(3), read);
        read.release();

        read = (ByteBuf) channel.readInbound();
        Assert.assertEquals(buf.readSlice(3), read);
        read.release();

        read = (ByteBuf) channel.readInbound();
        Assert.assertEquals(buf.readSlice(3), read);
        read.release();

        Assert.assertNull(channel.readInbound());
        buf.release();
    }


    @Test
    public void testFramesDecoded2() {
        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            buf.writeByte(i);
        }
        ByteBuf input = buf.duplicate();

        EmbeddedChannel channel = new EmbeddedChannel(new FixedLengthFrameDecoder(3));
        Assert.assertFalse(channel.writeInbound(input.readBytes(2)));
        Assert.assertTrue(channel.writeInbound(input.readBytes(7)));

        Assert.assertTrue(channel.finish());
        ByteBuf read = (ByteBuf) channel.readInbound();
        Assert.assertEquals(buf.readSlice(3), read);
        read.release();

        read = (ByteBuf) channel.readInbound();
        Assert.assertEquals(buf.readSlice(3), read);
        read.release();

        read = (ByteBuf) channel.readInbound();
        Assert.assertEquals(buf.readSlice(3), read);
        read.release();

        Assert.assertNull(channel.readInbound());
        buf.release();
    }


    public String convertByteBufToString(ByteBuf buf) {
        String str;
        if (buf.hasArray()) { // 处理堆缓冲区
            str = new String(buf.array(), buf.arrayOffset() + buf.readerIndex(), buf.readableBytes());
        } else { // 处理直接缓冲区以及复合缓冲区
            byte[] bytes = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), bytes);
            str = new String(bytes, 0, buf.readableBytes());
        }
        return str;
    }
}


