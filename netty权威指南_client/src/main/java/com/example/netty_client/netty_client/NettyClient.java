package com.example.netty_client.netty_client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.marshalling.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.log4j.Log4j2;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by i-feng on 2019/4/17.
 */

@Log4j2
@Component
public class NettyClient implements CommandLineRunner{

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    EventLoopGroup group = new NioEventLoopGroup();

    @Override
    public void run(String... strings) throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    connect("192.168.199.213", 8086);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    private void connect(String host, int port) throws InterruptedException {

        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                                    .addLast("MessageDecoder", new NettyMessageDecoder(1024 * 1024, 4, 4, -8, 0))
                                    .addLast("MessageEncoder", new NettyMessageEncoder())
                                    .addLast("readTimeoutHandler", new ReadTimeoutHandler(10))
                                    .addLast("LoginAuthHandler", new LoginAuthReqHandler())
                                    .addLast("HeartBeatHandler", new HeartBeatReqHandler());
                        }
                    });


            ChannelFuture f = b.connect(host, port).sync();

            log.info("Netty Client 连接开启: " + host + ":" + port);

            f.channel().closeFuture().sync();
        } finally {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        TimeUnit.SECONDS.sleep(5);

                        try {
                            connect(host, port);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}


/**
 * 定义 HeartBeatReqHandler， 客户端心跳发送业务
 */
@Log4j2
class HeartBeatReqHandler extends SimpleChannelInboundHandler<NettyMessage> {

    private volatile ScheduledFuture<?> heartBeat;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, NettyMessage nettyMessage) throws Exception {
        log.info("HeartBeatReqHandler  Read0");
        if (nettyMessage.getHeader() != null && nettyMessage.getHeader().getType() == (byte)2) {
            HeartBeatReqHandler handler = new HeartBeatReqHandler();
            // 心跳定时器，
            heartBeat = channelHandlerContext.executor().scheduleAtFixedRate(new HeartBeatTask(channelHandlerContext), 0, 5000, TimeUnit.MILLISECONDS);
        } else if (nettyMessage.getHeader() != null && nettyMessage.getHeader().getType() == (byte)6) {
            log.info("Client receive server heart beat message : ---> " + nettyMessage);
        } else {
            channelHandlerContext.fireChannelRead(nettyMessage);
        }
    }

    private class HeartBeatTask implements Runnable {

        private final ChannelHandlerContext ctx;

        public HeartBeatTask(final ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            NettyMessage message;
        }
    }

    private NettyMessage buildHeartBeat() {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType((byte)5);
        return message;
    }
}


/**
 * 定义LoginAuthReqHandler， 客户端发送请求的业务ChannelHandler
 */
@Log4j2
class LoginAuthReqHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(buildLoginReq());
    }

    private NettyMessage buildLoginReq() {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType((byte)3); // 握手请求消息
        message.setHeader(header);
        return message;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("收到握手应答消息");
        NettyMessage nettyMessage = (NettyMessage)msg;
        if (nettyMessage.getHeader() != null && nettyMessage.getHeader().getType() == 4) {
            byte loginResult = (byte)nettyMessage.getBody();
            if (loginResult != (byte)0) {
                ctx.close(); //握手失败
            } else {
                log.info("login is ok " + nettyMessage);
                ctx.fireChannelRead(nettyMessage);
            }
        } else {
            ctx.fireChannelRead(nettyMessage);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }
}

    /**
        * NettyMessage的Encoder，注意消息长度的计算方法，以及最后把Message传递出去
        */
final class NettyMessageEncoder extends MessageToMessageEncoder<NettyMessage> {

    private NettyMarshallingEncoder marshallingEncoder;

    public NettyMessageEncoder(){
        marshallingEncoder = MarshallingCodeCFactory.buildMarshallingEncoder();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, NettyMessage msg,
                          List<Object> out) throws Exception {
        if(msg == null || msg.getHeader() == null){
            throw new Exception("The encode message is null");
        }

        ByteBuf sendBuf = Unpooled.buffer();
        sendBuf.writeInt(msg.getHeader().getCrcCode());
        sendBuf.writeInt(msg.getHeader().getLength());
        sendBuf.writeLong(msg.getHeader().getSessionID());
        sendBuf.writeByte(msg.getHeader().getType());
        sendBuf.writeByte(msg.getHeader().getPriority());
        sendBuf.writeInt(msg.getHeader().getAttachment().size());

        String key = null;
        byte[] keyArray = null;
        Object value = null;
        for(Map.Entry<String, Object> param: msg.getHeader().getAttachment().entrySet()){
            key = param.getKey();
            keyArray = key.getBytes("UTF-8");
            sendBuf.writeInt(keyArray.length);
            sendBuf.writeBytes(keyArray);
            value = param.getValue();
            marshallingEncoder.encode(ctx, value, sendBuf);
        }
        key = null;
        keyArray = null;
        value = null;
        if(msg.getBody() != null){
            marshallingEncoder.encode(ctx, msg.getBody(), sendBuf);
        }

//		sendBuf.writeInt(0);
        // 在第4个字节出写入Buffer的长度
        int readableBytes = sendBuf.readableBytes();
        sendBuf.setInt(4, readableBytes);

        // 把Message添加到List传递到下一个Handler
        out.add(sendBuf);
    }
}

/**
 * 定义NettyMessageDecoder类，注意设置LengthFieldBasedFrameDecoder的几个重要参数，直接影响到解码的结果
 */
final class NettyMessageDecoder extends LengthFieldBasedFrameDecoder {

    private NettyMarshallingDecoder marshallingDecoder;

    public NettyMessageDecoder(int maxFrameLength, int lengthFieldOffset,
                               int lengthFieldLength,int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
        marshallingDecoder = MarshallingCodeCFactory.buildMarshallingDecoder();
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        return decode1(ctx, in);
    }

    public Object decode1(ChannelHandlerContext ctx, ByteBuf in) throws Exception{
        ByteBuf frame = (ByteBuf)super.decode(ctx, in);
        if(frame == null){
            return null;
        }

        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setCrcCode(frame.readInt());
        header.setLength(frame.readInt());
        header.setSessionID(frame.readLong());
        header.setType(frame.readByte());
        header.setPriority(frame.readByte());

        int size = frame.readInt();
        if(size > 0){
            Map<String, Object> attach = new HashMap<String, Object>(size);
            int keySize = 0;
            byte[] keyArray = null;
            String key = null;
            for(int i=0; i<size; i++){
                keySize = frame.readInt();
                keyArray = new byte[keySize];
                in.readBytes(keyArray);
                key = new String(keyArray, "UTF-8");
                attach.put(key, marshallingDecoder.decode(ctx, frame));
            }
            key = null;
            keyArray = null;
            header.setAttachment(attach);
        }
        if(frame.readableBytes() > 0){
            message.setBody(marshallingDecoder.decode(ctx, frame));
        }
        message.setHeader(header);
        return message;
    }
}

/**
 * 定义MarshallingCodeCFactory工厂类来获取JBoss Marshalling 类
 */
class MarshallingCodeCFactory {
    public static NettyMarshallingDecoder buildMarshallingDecoder(){
        MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("serial");
        MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setVersion(5);
        UnmarshallerProvider provider = new DefaultUnmarshallerProvider(marshallerFactory, configuration);
        NettyMarshallingDecoder decoder = new NettyMarshallingDecoder(provider, 10240);
        return decoder;
    }

    public static NettyMarshallingEncoder buildMarshallingEncoder(){
        MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("serial");
        MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setVersion(5);
        MarshallerProvider provider = new DefaultMarshallerProvider(marshallerFactory, configuration);
        NettyMarshallingEncoder encoder = new NettyMarshallingEncoder(provider);
        return encoder;
    }
}

/**
 * 扩展MarshallingEncoder 和 MarshallingDecoder
 */
class NettyMarshallingEncoder extends MarshallingEncoder {

    public NettyMarshallingEncoder(MarshallerProvider provider) {
        super(provider);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception{
        super.encode(ctx, msg, out);
    }
}

class NettyMarshallingDecoder extends MarshallingDecoder {

    public NettyMarshallingDecoder(UnmarshallerProvider provider) {
        super(provider);
    }

    public NettyMarshallingDecoder(UnmarshallerProvider provider, int maxObjectSize){
        super(provider, maxObjectSize);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        return super.decode(ctx, in);
    }

}

/**
 * Netty  消息定义类
 */
final class NettyMessage {
    private Header header; // 消息头
    private Object body; // 消息体

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    @Override
    public String toString(){
        return "NettyMessage [header=" + header + "]";
    }
}

/**
 * Netty 消息头 定义
 */
final class Header {
    private int crcCode = 0xabef0101;
    private int length; //消息长度
    private long sessionID; // 回话ID
    private byte type; // 消息类型
    private byte priority; // 消息优先级
    private Map<String, Object> attachment = new HashMap<String, Object>(); // 附件

    public int getCrcCode() {
        return crcCode;
    }

    public void setCrcCode(int crcCode) {
        this.crcCode = crcCode;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public long getSessionID() {
        return sessionID;
    }

    public void setSessionID(long sessionID) {
        this.sessionID = sessionID;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte getPriority() {
        return priority;
    }

    public void setPriority(byte priority) {
        this.priority = priority;
    }

    public Map<String, Object> getAttachment() {
        return attachment;
    }

    public void setAttachment(Map<String, Object> attachment) {
        this.attachment = attachment;
    }

    @Override
    public String toString(){
        return "Header [crcCode=" + crcCode + ", length=" + length + ", sessionID=" + sessionID
                + ", type=" + type + ", priority=" + priority + ", attachment=" + attachment + "]";
    }
}
