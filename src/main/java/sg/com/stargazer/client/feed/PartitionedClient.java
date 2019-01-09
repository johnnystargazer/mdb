package sg.com.stargazer.client.feed;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

import com.github.myzhan.locust4j.AbstractTask;
import com.github.myzhan.locust4j.Locust;
import com.github.myzhan.locust4j.taskset.AbstractTaskSet;
import com.github.myzhan.locust4j.taskset.WeighingTaskSet;
import com.google.common.base.Function;

@Slf4j
public class PartitionedClient {
    public static void main(String[] args) throws IOException, InterruptedException {
        Integer partition = Integer.valueOf(args[1]);
        Locust locust = Locust.getInstance();
        locust.setMasterHost(args[0]);
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
        clientConfig.setQueryUrl((String) properties.getProperty("queryUrl"));
        File file = new File(".");
        clientConfig.setPath((String) properties.getOrDefault("dataPath", file.getAbsoluteFile().getParentFile()
            .getAbsolutePath()));
        clientConfig.setMax(10);
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
                        Locust.getInstance().recordSuccess("batch-500", "partition-" + partition, t, 1);
                        Locust.getInstance().recordSuccess("batch-500", "partition-all", t, 1);
                        return null;
                    }
                };
                for (;;) {
                    clientPartition.start();
                    clientPartition.join();
                }
            }
        });
        locust.run("simulator_" + partition, taskSet);
        locust.setRunnerShutdownHook(new Function<Void, Void>() {
            @Override
            public Void apply(Void input) {
                try {
                    clientConfig.shutdown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }
}
