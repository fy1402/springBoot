package com.example.netty.webSocket.handler;

import com.example.netty.webSocket.config.NettyConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import lombok.extern.log4j.Log4j2;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// 接受/处理/响应客户端的webSocket的核心业务处理类
@Log4j2
public class MyWebSocketHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handshaker;

    private static Map<String,String> cache = new HashMap<String, String>();

    private static String WEB_SOCKET_URL = "ws://localhost:8888/websocket";

    // 客户端与服务端建立连接时调用
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        super.channelActive(ctx);
        NettyConfig.group.add(ctx.channel());
        log.info("客户端与服务端连接开启...");
    }

    // 客户端与服务端断开连接时调用
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//        super.channelInactive(ctx);
        NettyConfig.group.remove(ctx.channel());
        log.info("客户端与服务端连接关闭...");
    }

    // 服务端在接受客户端发送过来的数据后调用
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//        super.channelReadComplete(ctx);
        ctx.flush();
    }

    // 建立连接之后抛出异常时调用
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        ctx.close();
    }

    // 服务端处理客户端webSocket请求的核心方法
    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {

        if (o instanceof FullHttpRequest) {
            // 处理客户端向服务端发起http握手请求的业务
            handHttpRequest(channelHandlerContext, (FullHttpRequest) o);
        } else if (o instanceof WebSocketFrame) {
            // 处理webSocket的连接业务
            log.info(channelHandlerContext.channel().id());
            handWebSocketFrame(channelHandlerContext, (WebSocketFrame) o);
        }
    }

    // 处理客户端与服务端之前的webSocket的业务
    private void handWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 判断是否为关闭webSocket的指令
        if (frame instanceof CloseWebSocketFrame) {
             handshaker.close(ctx.channel(), ((CloseWebSocketFrame) frame).retain());
        }
        // 判断是否是ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
        }
        // 判断是否为二进制消息，如果是，抛出异常
        if (!(frame instanceof TextWebSocketFrame)) {
            log.info("目前不支持二进制消息");
            throw new RuntimeException("【" + this.getClass().getName() + "】不支持此消息");
        }
        // 返回应答消息
        // 获取客服端向服务端发送的消息
        String request = ((TextWebSocketFrame) frame).text();
        log.info("服务端收到客户端的消息====>>>" + request);

        TextWebSocketFrame tws = new TextWebSocketFrame(new Date().toString() + ctx.channel().id() + "===>>>" + request);

        // 群发，服务端向每一个连接的客户端发送消息
        NettyConfig.group.writeAndFlush(tws);


        NettyConfig.group.writeAndFlush(tws, null);

    }

    // 处理客户端向服务端发起请求的业务
    private void handHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        // upgrade http握手请求的请求头
        if (request.decoderResult().isFailure() || !("websocket".equals(request.headers().get("Upgrade")))) {
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        // websocket 工厂类
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(WEB_SOCKET_URL, null, false);
        handshaker = factory.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
             handshaker.handshake(ctx.channel(), request);
        }
    }

    // 服务端向客户端响应消息
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request, DefaultFullHttpResponse response) {
        if (response.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(response.status().toString(), CharsetUtil.UTF_8);
            response.content().writeBytes(buf);
            buf.release();
        }
        // 服务端向客户端发送数据
        ChannelFuture f = ctx.channel().writeAndFlush(response);
        if (response.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
