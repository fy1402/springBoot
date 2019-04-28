package example.chapter03;

import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Log4j2
public class ThreadJoin {

    static {
        log.info("static");
    }

    public static void main(String[] args) {

        List<Thread> list = IntStream.range(1, 3).mapToObj(ThreadJoin::create).collect(Collectors.toList());

        list.forEach(Thread::start);

//        for (Thread thread : list) {
//            try {
//                thread.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }

        for (int i = 0; i < 10; i++) {
            log.info(Thread .currentThread().getName() + "#" + i);
            shortSleep();
        }
    }


    private static Thread create(int seq) {
        return new Thread(()-> {
            for (int i = 0; i < 10; i++) {
                log.info(Thread .currentThread().getName() + "#" + i);
                shortSleep();
            }
        }, String.valueOf(seq));
    }

    private static void shortSleep() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
