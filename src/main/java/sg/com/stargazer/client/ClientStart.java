package sg.com.stargazer.client;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Stopwatch;

@Slf4j
public class ClientStart {
    public static void main(String[] args) throws IOException, InterruptedException {
        ClientConfig clientConfig = new ClientConfig();
        ZonedDateTime start = ZonedDateTime.of(2018, 10, 1, 0, 50, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime stop = ZonedDateTime.of(2018, 12, 1, 1, 0, 0, 0, ZoneId.of("UTC"));
        clientConfig.setStart(start);
        clientConfig.setStop(stop);
        List<Thread> t = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            ClientPartition clientPartition = new ClientPartition(i, clientConfig);
            Thread thread = new Thread(clientPartition);
            t.add(thread);
        }
        Stopwatch time = Stopwatch.createStarted();
        for (Thread thread : t) {
            thread.start();
        }
        for (Thread thread : t) {
            thread.join();
        }
        clientConfig.shutdown();
        log.info(" in {} sec ", time.elapsed(TimeUnit.SECONDS));
    }
}
