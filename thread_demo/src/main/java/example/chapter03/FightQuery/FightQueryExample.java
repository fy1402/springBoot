package example.chapter03.FightQuery;

import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Log4j2
public class FightQueryExample {

    public static void main(String[] args) {
        List<String> result = search("SH", "BJ");
        log.info("========== result ==========");
        result.forEach(log::info);
    }

    private static List<String> fightCompany = Arrays.asList("CSA", "CEA", "HNA");

    private static List<String> search(String origin, String dest) {
        final List<String> result = new ArrayList<String>();

        // 创建查询航班信息的列表
        List<FightQueryTask> fightQueryTasks = fightCompany.stream().map(f -> createSearchTask(f, origin, dest)).collect(toList());

        // 分别启动这几个线程
        fightQueryTasks.forEach(Thread::start);

        // 分别调用线程的join方法，阻塞当前线程
        fightQueryTasks.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // 在此之前当前线程会阻塞住，获取每一个查询线程的结果，并且加入到result中
        fightQueryTasks.stream().map(FightQueryTask::get).forEach(result::addAll);

        return result;
    }

    private static FightQueryTask createSearchTask(String fight, String origin, String dest) {
        return new FightQueryTask(fight, origin, dest);
    }
}
