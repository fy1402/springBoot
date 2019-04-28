package example.chapter03;


import lombok.extern.log4j.Log4j2;

import java.util.concurrent.TimeUnit;

@Log4j2
public class InterruptThreadExit {

    public static void main(String[] args) throws InterruptedException {
//        exit1();
//        exit2();
        exit3();
    }

    private static void exit1() throws InterruptedException {
        Thread t = new Thread() {
            @Override
            public void run() {
                log.info("I will start work");
                while (!isInterrupted()) {
                    //working
                }
                log.info("I will be exit work");
            }
        };
        t.start();
        TimeUnit.SECONDS.sleep(1);
        log.info("System will be shutdown.");
        t.interrupt();
    }

    private static void exit2() {
        Thread t = new Thread() {
            @Override
            public void run() {
                log.info("I will start work");
                for (;;) {
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                log.info("I will be exit work");
            }
        };
        t.start();
        t.interrupt();
    }

    public static void exit3() throws InterruptedException {
        MyTask task = new MyTask();
        task.start();
        TimeUnit.SECONDS.sleep(1);
        log.info("System will be shoudown");
        task.close();
    }

    static class MyTask extends Thread{

        private volatile boolean close = false;

        @Override
        public void run() {
            log.info("I will be start work");
            while (!close && !isInterrupted()) {
                // 正在运行
            }
            log.info("I will exit work");
        }

        public void close() {
            this.close = true;
            this.interrupt();
        }
    }
}
