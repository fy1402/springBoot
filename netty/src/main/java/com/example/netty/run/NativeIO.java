package com.example.netty.run;

import com.example.netty.transport.nativeIO.IO.PlainNioServer;
import com.example.netty.transport.nativeIO.IO.PlainOioServer;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Log4j2
@Component
public class NativeIO implements CommandLineRunner {

    @Override
    public void run(String... args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                initNativeIOBean();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                initNativeNioBean();
            }
        }).start();
    }


//    @Bean
    public void initNativeIOBean() {

        log.info("原生阻塞IO，开始启动...");

        PlainOioServer server = new PlainOioServer();
        try {
            server.serve(8083);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initNativeNioBean() {
        log.info("原生非阻塞IO, 开始启动...");
        PlainNioServer server = new PlainNioServer();
        try {
            server.serve(8084);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
