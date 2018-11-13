package sg.com.stargazer.res.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;
import sg.com.stargazer.client.feed.BaseClientPartition;
import sg.com.stargazer.client.feed.ClientConfig;
import sg.com.stargazer.client.feed.ClientStart;
import sg.com.stargazer.client.feed.TxConsumer;
import sg.com.stargazer.res.fdb.DbServer;
import sg.com.stargazer.res.fdb.FDbConsumer;
import sg.com.stargazer.res.fdb.TxProcessor;
import sg.com.stargazer.res.proto.ProtoService;
import spark.Request;
import spark.Response;
import spark.Route;

import com.apple.foundationdb.directory.DirectoryLayer;
import com.google.common.base.Stopwatch;

@Slf4j
public class Import implements Route {
    private ProtoService protoService;
    private DbServer dbServer;
    private TxProcessor txProcessor;
    final DirectoryLayer dir = new DirectoryLayer();

    public Import(ProtoService protoService, DbServer dbServer) {
        this.protoService = protoService;
        this.dbServer = dbServer;
        txProcessor = new TxProcessor(dbServer, protoService);
    }

    private void start() throws Exception {
        ClientConfig clientConfig = new ClientConfig();
        Properties properties = new Properties();
        InputStream intput = ClientStart.class.getClassLoader().getResourceAsStream("client-config.properties");
        properties.load(intput);
        intput.close();
        File file = new File(".");
        clientConfig.setStart((String) properties.get("start"));
        clientConfig.setStop((String) properties.get("end"));
        clientConfig.setUrl((String) properties.get("restUrl"));
        clientConfig.setSpeed((String) properties.get("speed"));
        clientConfig.setPath((String) properties.getOrDefault("dataPath", file.getAbsoluteFile().getParentFile()
            .getPath()));
        List<BaseClientPartition> t = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            BaseClientPartition clientPartition = new BaseClientPartition(i, clientConfig) {
                @Override
                public TxConsumer newClient() throws IOException {
                    return new FDbConsumer(txProcessor, protoService);
                }
            };
            t.add(clientPartition);
        }
        AtomicBoolean run = new AtomicBoolean(true);
        Stopwatch time = Stopwatch.createStarted();
        Thread monitor = new Thread(new Runnable() {
            @Override
            public void run() {
                while (run.get()) {
                    try {
                        Thread.sleep(10000);
                        long count = 0;
                        for (BaseClientPartition clientPartition : t) {
                            count += clientPartition.getCount();
                        }
                        long sec = time.elapsed(TimeUnit.SECONDS);
                        log.info(" processed {} in {} sec , speed  {} /sec ", count, sec, count / (sec * 1.0));
                    } catch (Exception e) {
                        e.printStackTrace();
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
        log.info("total in {} sec ", time.elapsed(TimeUnit.SECONDS));
        run.set(false);
    }

    @Override
    public Object handle(Request request, Response res) throws Exception {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    start();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        res.status(200);
        res.type("application/json");
        res.body("started");
        return null;
    }
}
