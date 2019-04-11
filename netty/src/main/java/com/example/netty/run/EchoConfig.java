package com.example.netty.run;


import com.example.netty.echo.server.EchoServer;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class EchoConfig  implements CommandLineRunner {

    @Override
    public void run(String... args) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        initEchoBean();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
    }

    public void initEchoBean() throws Exception {
        log.info("Usage: " + EchoServer.class.getSimpleName() + " <port>");
        EchoServer server = new EchoServer();
        server.start(8082);
    }
}
