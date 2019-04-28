package example.chapter02;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class DaemonThread {

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
        Thread.sleep(2_000L);
        log.info("Main Thread finish");
    }

}
