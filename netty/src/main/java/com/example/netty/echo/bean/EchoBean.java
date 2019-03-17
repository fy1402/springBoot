package com.example.netty.echo.bean;

import com.example.netty.echo.server.EchoServer;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class EchoBean {

    @Bean
    public void initEchoBean() throws Exception {
        log.info("Usage: " + EchoServer.class.getSimpleName() + " <port>");
        EchoServer server = new EchoServer();
        server.start();
    }
}
