package com.example.netty.echo.bean;

import com.example.netty.echo.server.EchoServer;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Log4j2
@Order(2)
@Component
public class EchoBean {


}
