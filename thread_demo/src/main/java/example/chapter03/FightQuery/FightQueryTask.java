package example.chapter03.FightQuery;

import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


@Log4j2
public class FightQueryTask extends Thread implements FightQueryService {

    private final String origin;

    private final String destiantion;

    private final List<String> fightList = new ArrayList<String>();

    public FightQueryTask(String airLine, String origin, String destiantion) {
        super("[" + airLine + "]");
        this.origin = origin;
        this.destiantion = destiantion;
    }

    @Override
    public void run() {
        log.info("{}-query from {} to {}", this.getName(), origin, destiantion);

        int randomVal = ThreadLocalRandom.current().nextInt(10);

        try {
            TimeUnit.SECONDS.sleep(randomVal);
            this.fightList.add(getName() + "-" + randomVal);
            log.info("the fight:{} list query successfull ", this.getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }




    @Override
    public List<String> get() {
        return this.fightList;
    }



}
