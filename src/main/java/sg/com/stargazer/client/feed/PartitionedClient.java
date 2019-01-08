package sg.com.stargazer.client.feed;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

import com.github.myzhan.locust4j.AbstractTask;
import com.github.myzhan.locust4j.Locust;
import com.github.myzhan.locust4j.taskset.AbstractTaskSet;
import com.github.myzhan.locust4j.taskset.WeighingTaskSet;
import com.google.common.base.Stopwatch;

@Slf4j
public class PartitionedClient {
    public static void main(String[] args) throws IOException, InterruptedException {
        Integer partition = 1;
        Locust locust = Locust.getInstance();
        locust.setVerbose(true);
        locust.setMaxRPS(1000);
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
        clientConfig.setPath((String) properties.getOrDefault("dataPath", file.getAbsoluteFile().getParentFile()
            .getAbsolutePath()));
        clientConfig.setMax(2);
        AbstractTaskSet taskSet = new WeighingTaskSet("test", 100);
        taskSet.addTask(new AbstractTask() {
            @Override
            public int getWeight() {
                return 100;
            }

            @Override
            public String getName() {
                return "partition-" + partition;
            }

            @Override
            public void execute() throws Exception {
                GrpcClientPartition clientPartition = new GrpcClientPartition(partition, clientConfig) {
                    public Void apply(Long t) {
                        locust.getInstance().recordSuccess("test", "partition-" + partition, t, 1);
                        return null;
                    }
                };
                Stopwatch time = Stopwatch.createStarted();
                AtomicBoolean run = new AtomicBoolean(true);
                Thread monitor = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (;;) {
                            try {
                                Thread.sleep(10000);
                                long count = clientPartition.getCount();
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
                clientPartition.start();
                monitor.start();
                clientPartition.join();
                clientConfig.shutdown();
                log.info(" in {} sec ", time.elapsed(TimeUnit.SECONDS));
                run.set(false);
            }
        });
        locust.run("simulator_" + partition, taskSet);
    }
}
