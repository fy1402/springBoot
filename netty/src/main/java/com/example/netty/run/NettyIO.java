package com.example.netty.run;

import com.example.netty.transport.nettyIO.NettyNioServer;
import com.example.netty.transport.nettyIO.NettyOioServer;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class NettyIO {

    @Bean
    public void initNettyOio() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                startNettyOio();
            }
        }).start();
    }

    @Bean
    public void initNettyNio() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                startNettyNio();
            }
        }).start();
    }

    public void startNettyOio() {
        log.info("netty阻塞IO，开始启动...");
        NettyOioServer server = new NettyOioServer();
        try {
            server.server(8085);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startNettyNio() {
        log.info("netty非阻塞IO，开始启动...");
        NettyNioServer server = new NettyNioServer();
        try {
            server.server(8086);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
