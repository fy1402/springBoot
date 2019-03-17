package com.example.netty_client.echo.bean;


import com.example.netty_client.echo.client.EchoClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class EchoClientBean {

    @Bean
    public void initEchoClientBean() {
        EchoClient client = new EchoClient();
        try {
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
