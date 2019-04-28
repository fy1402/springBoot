package example.chapter01;

import lombok.extern.log4j.Log4j2;

public class RunnableTest01 {

    public static void main(String[] args) {

        final TicketWindowRunnable task = new TicketWindowRunnable();

        Thread thread1 = new Thread(task, "窗口1");
        Thread thread2 = new Thread(task, "窗口2");
        Thread thread3 = new Thread(task, "窗口3");
        Thread thread4 = new Thread(task, "窗口4");

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
    }


}

@Log4j2
class TicketWindowRunnable implements Runnable {

    private static int index = 1;

    private final static int max = 50;

    @Override
    public void run() {
        while (index <= max) {
            log.info(Thread.currentThread() + " 的号码是: " + index++);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
