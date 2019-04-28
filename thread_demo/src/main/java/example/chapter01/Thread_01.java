package example.chapter01;

import lombok.extern.log4j.Log4j2;

import java.util.concurrent.TimeUnit;

public class Thread_01 {

    public static void main(String[] args) {

        TicketWindow thcket1 = new TicketWindow("柜台1");
        thcket1.start();

        TicketWindow thcket2 = new TicketWindow("柜台2");
        thcket2.start();

        TicketWindow thcket3 = new TicketWindow("柜台3");
        thcket3.start();

        TicketWindow thcket4 = new TicketWindow("柜台4");
        thcket4.start();
    }




    public static void thread() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
        thread.start();
    }

}

@Log4j2
class TicketWindow extends Thread {
    private final int max = 50;
    private final String name;
    private static int index = 1;
    public TicketWindow(String msg) {
        this.name = msg;
    }

    @Override
    public void run() {
        while (index <= max) {
            log.info("柜台：" + name + " 当前的号码为：" + index++);
        }
    }
}
