package sg.com.stargazer.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

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
