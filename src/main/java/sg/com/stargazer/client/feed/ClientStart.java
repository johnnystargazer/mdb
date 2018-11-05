package sg.com.stargazer.client.feed;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Stopwatch;

@Slf4j
public class ClientStart {
    public static void main(String[] args) throws IOException, InterruptedException {
        ClientConfig clientConfig = new ClientConfig();
        Properties properties = new Properties();
        InputStream intput = ClientStart.class.getClassLoader().getResourceAsStream("client-config.properties");
        properties.load(intput);
        intput.close();
        clientConfig.setStart((String) properties.get("start"));
        clientConfig.setStop((String) properties.get("end"));
        clientConfig.setUrl((String) properties.get("restUrl"));
        clientConfig.setSpeed((String) properties.get("speed"));
        File file = new File(".");
        clientConfig.setBase((String) properties.getOrDefault("dataPath", file.getAbsoluteFile()));
        List<GrpcClientPartition> t = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            GrpcClientPartition clientPartition = new GrpcClientPartition(i, clientConfig);
            t.add(clientPartition);
        }
        Stopwatch time = Stopwatch.createStarted();
        AtomicBoolean run = new AtomicBoolean(true);
        Thread monitor = new Thread(new Runnable() {
            @Override
            public void run() {
                for (;;) {
                    try {
                        Thread.sleep(10000);
                        long count = 0;
                        for (GrpcClientPartition clientPartition : t) {
                            count += clientPartition.getCount();
                        }
                        long sec = time.elapsed(TimeUnit.SECONDS);
                        log.info(" processed {} in {} sec , speed  {} /sec ", count, sec, count / (sec * 1.0));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (!run.get()) {
                        break;
                    }
                }
            }
        });
        for (Thread thread : t) {
            thread.start();
        }
        monitor.start();
        for (Thread thread : t) {
            thread.join();
        }
        clientConfig.shutdown();
        log.info(" in {} sec ", time.elapsed(TimeUnit.SECONDS));
        run.set(false);
    }
}
