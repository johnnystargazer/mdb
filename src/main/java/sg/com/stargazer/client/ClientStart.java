package sg.com.stargazer.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import lombok.extern.slf4j.Slf4j;

import com.dashur.mdb.Tx.Request;
import com.google.common.base.Stopwatch;
import com.google.protobuf.ByteString;

@Slf4j
public class ClientStart {
    static Client newClient() throws IOException {
        Client client = new Client("localhost", 8080);
        client.start();
        return client;
    }

    public static void main(String[] args) throws IOException {
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(4);
        Stopwatch time = Stopwatch.createStarted();
        AtomicLong atomicLong = new AtomicLong();
        ZipFile zip = new ZipFile("P_0-3642-2018-10-22T20:19:34Z.zip");
        Client client = newClient();
        for (Enumeration e = zip.entries(); e.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
            String thisLine = null;
            while ((thisLine = reader.readLine()) != null) {
                long count = atomicLong.incrementAndGet();
                if (count % 1000 == 0) {
                    Client oldClient = client;
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            oldClient.complete();
                        }
                    });
                    client = newClient();
                    Request request =
                        Request.newBuilder().setProtobuf(ByteString.copyFrom(Base64.getDecoder().decode(thisLine)))
                            .build();
                    client.onNext(request);
                }
            }
        }
        Client oldClient = client;
        exec.execute(new Runnable() {
            @Override
            public void run() {
                oldClient.complete();
            }
        });
        exec.shutdown();
        exec.awaitTermination(1, TimeUnit.MINUTES);
        log.info("processed {} in {} sec ", atomicLong.get(), time.elapsed(TimeUnit.SECONDS));
    }
}
