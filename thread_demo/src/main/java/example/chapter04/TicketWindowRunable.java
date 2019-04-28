package example.chapter04;


import lombok.extern.log4j.Log4j2;

@Log4j2
public class TicketWindowRunable implements Runnable {

    private int index = 0;
    private final static int MAX_INDEX = 500;

    @Override
    public void run() {

        while (index <= MAX_INDEX) {
            log.info(Thread.currentThread().getName() +", index = " + index++);
        }
    }

    private int indexAdd() {
        return index += 1;
    }

    public static void main(String[] args) {

        final Runnable task = new TicketWindowRunable();

        Thread t1 = new Thread(task, "一号窗口");
        Thread t2 = new Thread(task, "二号窗口");
        Thread t3 = new Thread(task, "三号窗口");
        Thread t4 = new Thread(task, "四号窗口");

        t1.start();
        t2.start();
        t3.start();
        t4.start();
    }
}
