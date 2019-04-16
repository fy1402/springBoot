package com.example.netty.netty_server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.LOCATION;

/**
 * Created by i-feng on 2019/4/15.
 */

@Log4j2
@Order(value = 4)
@Component
public class HttpFileServer implements CommandLineRunner {

    @Value("${netty_httpfile_server_port}")
    private int netty_httpfile_server_port;

    private static  final String default_url = "/netty权威指南/src/main/java/com/example/netty/";

    @Override
    public void run(String... strings) throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    bind(netty_httpfile_server_port, default_url);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void bind(int port, String url) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                                    .addLast("http-decoder", new HttpRequestDecoder())
                                    .addLast("http-aggregator", new HttpObjectAggregator(65536))
                                    .addLast("http-encoder", new HttpResponseEncoder())
                                    .addLast("http-chunked", new ChunkedWriteHandler())
                                    .addLast("fileServerHandler", new HttpFileServerHandler(url));
                        }
                    });
            ChannelFuture f = b.bind("127.0.0.1", port).sync();

            log.info("HTTP 文件目录服务器启动， 网址是 ： " + "http://127.0.0.1:" + port + url);

            f.channel().closeFuture().sync();

        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }


}

@Log4j2
class HttpFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final String url;

    public HttpFileServerHandler(String url) {
        this.url = url;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception {
        if (!fullHttpRequest.decoderResult().isSuccess()) {
            sendError(channelHandlerContext, HttpResponseStatus.BAD_REQUEST);
            return;
        }
        if (fullHttpRequest.method() != HttpMethod.GET) {
            sendError(channelHandlerContext, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }

        final String uri = fullHttpRequest.uri();
        final String path = sanitizeUri(uri);

        if (path == null) {
            sendError(channelHandlerContext, HttpResponseStatus.FORBIDDEN);
            return;
        }

        File file = new File(path);
        if (file.isHidden() || !file.exists()) {
            sendError(channelHandlerContext, HttpResponseStatus.NOT_FOUND);
            return;
        }
        if (file.isDirectory()) {
            if (uri.endsWith("/")) {
                sendListing(channelHandlerContext, file);
            } else {
                sendRedirect(channelHandlerContext, uri + "/");
            }
            return;
        }

        if (!file.isFile()) {
            sendError(channelHandlerContext, HttpResponseStatus.FORBIDDEN);
            return;
        }

        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "r"); // 以只读的方式打开文件
        } catch (FileNotFoundException e) {
            sendError(channelHandlerContext, HttpResponseStatus.NOT_FOUND);
            return;
        }
        long fileLength = randomAccessFile.length();

        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        HttpUtil.setContentLength(response, fileLength);
        setContentTypeHeader(response, file);
        if (HttpUtil.isKeepAlive(fullHttpRequest)) {
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
        channelHandlerContext.write(response);

        ChannelFuture sendChannelFuture;
        sendChannelFuture = channelHandlerContext.write(new ChunkedFile(randomAccessFile, 0, fileLength, 8192), channelHandlerContext.newProgressivePromise());
        sendChannelFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture channelProgressiveFuture, long l, long l1) throws Exception {
                if (l1 < 0) {
                    log.error("Transfer progress: " + l);
                } else {
                    log.error("Transfer progress: " + l + "/" + l1);
                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture channelProgressiveFuture) throws Exception {
                log.info("Transfer complete");
            }
        });
        ChannelFuture lastContentFuture = channelHandlerContext.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!HttpUtil.isKeepAlive(fullHttpRequest)) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

    private String sanitizeUri(String uri) {
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            try {
                uri = URLDecoder.decode(uri, "ISO-8859-1");
            } catch (UnsupportedEncodingException e1) {
                throw new Error();
            }
        }
        if (!uri.startsWith(url)) {
            return null;
        }
        if (!uri.startsWith("/")) {
            return null;
        }

        uri = uri.replace('/', File.separatorChar);

        if (uri.contains(File.separator + "." )
                || uri.contains("." + File.separator)
                || uri.startsWith(".")
                || uri.endsWith(".")
                || INSECURE_URI.matcher(uri).matches()) {
            return null;
        }

        return System.getProperty("user.dir") + File.separator + uri;
    }


    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");

    private static void sendListing(ChannelHandlerContext ctx, File dir) {

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(CONTENT_TYPE, "text/html;charset=UTF-8");

        StringBuilder buf = new StringBuilder();
        String dirPath = dir.getPath();
        buf.append("<!DOCTYPE html>\r\n");
        buf.append("<html><head><title>");
        buf.append(dirPath);
        buf.append(" 目录 ");
        buf.append("</title></head><body>\r\n");
        buf.append("<h3>");
        buf.append(dirPath).append(" 目录: ");
        buf.append("</h3>\r\n");
        buf.append("<ul>");
        buf.append("<li>链接：<a href=\"../\">..</a></li>\r\n");
        for (File f : dir.listFiles()) {
            if (f.isHidden() || !f.canRead()) {
                continue;
            }
            String name = f.getName();
            if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
                continue;
            }
            buf.append("<li>链接：<a href=\"");
            buf.append(name);
            buf.append("\">");
            buf.append(name);
            buf.append("</a></li>\r\n");
        }
        buf.append("</ul></body></html>\r\n");

        ByteBuf byteBuf = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
        response.content().writeBytes(byteBuf);
        byteBuf.release();
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


    public static void sendRedirect(ChannelHandlerContext ctx, String newUri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        response.headers().set(LOCATION, newUri);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void sendError(ChannelHandlerContext channelHandlerContext, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

        channelHandlerContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
        response.headers().set(CONTENT_TYPE, mimetypesFileTypeMap.getContentType(file.getPath()));
    }

}
