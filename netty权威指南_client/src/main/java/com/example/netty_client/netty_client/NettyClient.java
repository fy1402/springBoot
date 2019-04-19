package com.example.netty_client.netty_client;

import com.nio.serlizable.Header;
import com.nio.serlizable.NettyMessage;
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
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.log4j.Log4j2;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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
//                    connect("192.168.199.143", 8086);
                    connect("192.168.199.213", 8086); // mac
//                    connect("172.18.44.32", 8086);
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
//                                    .addLast(new ProtobufVarint32FrameDecoder())
                                    .addLast(new NettyMessageDecoder(1024 * 1024, 4, 4, -8, 0))
                                    .addLast(new NettyMessageEncoder())
                                    .addLast("readTimeoutHandler", new ReadTimeoutHandler(10))
                                    .addLast(new LoginAuthReqHandler())
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
        if (nettyMessage.getHeader() != null && nettyMessage.getHeader().getType() == (byte)4) {
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
            NettyMessage message = buildHeartBeat();
            log.info("client send heart message :　" + message);;
            ctx.writeAndFlush(message);
        }
    }

    private NettyMessage buildHeartBeat() {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType((byte)5);
        message.setHeader(header);
        return message;
    }
}

/**
 * 服务的心跳应答
 */
@Log4j2
class HeartBeatRespHandler extends SimpleChannelInboundHandler<NettyMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, NettyMessage nettyMessage) throws Exception {
        //返回心跳应答消息
        if (nettyMessage.getHeader() != null && nettyMessage.getHeader().getType() == (byte)5) {
            log.info("Receive client heart beat message : ---> " + nettyMessage);
            NettyMessage message = buildHeartBeat();
            log.info("Send heart beat response message to client : ---> " + message);
            channelHandlerContext.writeAndFlush(message);
        } else  {
            channelHandlerContext.fireChannelRead(nettyMessage);
        }
    }

    private NettyMessage buildHeartBeat() {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType((byte)6);
        message.setHeader(header);
        return message;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
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
        NettyMessage nettyMessage = (NettyMessage)msg;
        if (nettyMessage.getHeader() != null && nettyMessage.getHeader().getType() == 4) {
            if (nettyMessage.getBody() != null) {
                byte loginResult = (byte)nettyMessage.getBody();
                if (loginResult != (byte)0) {
                    ctx.close(); //握手失败
                } else {
                    log.info("login is ok " + nettyMessage);
                    ctx.fireChannelRead(nettyMessage);
                }
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
 * 定义LoginAuthRespHandler类，服务器端响应Login的业务ChannelHandler
 */
@Log4j2
class LoginAuthRespHandler extends ChannelInboundHandlerAdapter {

    private Map<String, Boolean> nodeCheck = new ConcurrentHashMap<String, Boolean>();

    private String[] whiteList = {"127.0.0.1", "192.168.199.143", "192.168.199.213"};

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
        NettyMessage nettyMessage = (NettyMessage)msg;
        if (nettyMessage.getHeader() != null && nettyMessage.getHeader().getType() == (byte)3) {
            String nodeIndex = channelHandlerContext.channel().remoteAddress().toString();
            NettyMessage loginResp = null;

            // 重复登录， refuse
            if (nodeCheck.containsKey(nodeIndex)) {
                loginResp = buildLoginResponse((byte)-1);
            } else {
                InetSocketAddress address = (InetSocketAddress) channelHandlerContext.channel().remoteAddress();
                String ip = address.getAddress().getHostAddress();
                boolean isOk = false;
                for (String wip : whiteList) {
                    if (wip.equals(ip)) {
                        isOk = true;
                        break;
                    }
                }
                loginResp = isOk ? buildLoginResponse((byte) 0) : buildLoginResponse((byte) -1);
                if (isOk) {
                    nodeCheck.put(nodeIndex, true);
                }
            }
            log.info("The login response is : " + loginResp + " body [" + loginResp.getBody() + "]" );
            channelHandlerContext.writeAndFlush(loginResp);

        } else {
            channelHandlerContext.fireChannelRead(nettyMessage);
        }
    }

    private NettyMessage buildLoginResponse(byte result) {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType((byte)4); // 握手应答消息
        message.setHeader(header);
        message.setBody(result);
        return message;
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
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

    /**
     * byteOrder是指明Length字段是大端序还是小端序，因为Netty要读取Length字段的值，所以大端小端要设置好，默认Netty是大端序ByteOrder.BIG_ENDIAN。

     maxFrameLength是指最大包长度，如果Netty最终生成的数据包超过这个长度，Netty就会报错。

     lengthFieldOffset是指明Length的偏移位，在这里应该是2，因为先导码有2个Byte。

     lengthFieldLength是Length字段长度，这里是2，Length字段占2个Byte。

     lengthAdjustment 这个参数很多时候设为负数，这是最让小伙伴们迷惑的。下面我用一整段话来解释这个参数：
     当Netty利用lengthFieldOffset（偏移位）和lengthFieldLength（Length字段长度）成功读出Length字段的值后，Netty认为这个值是指从Length字段之后，到包结束一共还有多少字节，如果这个值是13，那么Netty就会再等待13个Byte的数据到达后，拼接成一个完整的包。但是更多时候，Length字段的长度，是指整个包的长度，如果是这种情况，当Netty读出Length字段的时候，它已经读取了包的4个Byte的数据，所以，后续未到达的数据只有9个Byte，即13 - 4 = 9，这个时候，就要用lengthAdjustment来告诉Netty，后续的数据并没有13个Byte，要减掉4个Byte，所以lengthAdjustment要设为 -4！！！

     initialBytesToStrip之前的几个参数，已经足够netty识别出整个数据包了，但是很多时候，调用者只关心包的内容，包的头部完全可以丢弃掉，initialBytesToStrip就是用来告诉Netty，识别出整个数据包之后，我只要从initialBytesToStrip起的数据，比如这里initialBytesToStrip设置为4，那么Netty就会跳过包头，解析出包的内容“12345678”。

     failFast 参数一般设置为true，当这个参数为true时，netty一旦读到Length字段，并判断Length超过maxFrameLength，就立即抛出异常。
     * @param maxFrameLength
     * @param lengthFieldOffset
     * @param lengthFieldLength
     * @param lengthAdjustment
     * @param initialBytesToStrip
     */
    public NettyMessageDecoder(int maxFrameLength, int lengthFieldOffset,
                               int lengthFieldLength,int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
        marshallingDecoder = MarshallingCodeCFactory.buildMarshallingDecoder();
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception{
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
class NettyMarshallingEncoder extends MarshallingEncoder{

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
