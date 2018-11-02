package sg.com.stargazer.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import lombok.extern.slf4j.Slf4j;

import com.dashur.mdb.Tx.Request;
import com.google.protobuf.ByteString;

@Slf4j
public class ClientPartition implements Runnable {
    private Integer partition;
    private ClientConfig clientConfig;
    private List<File> files;
    private AtomicLong atomicLong = new AtomicLong(0);

    private Client newClient() throws IOException {
        Client client = new Client(clientConfig.getUrl());
        client.start();
        return client;
    }

    public ClientPartition(Integer partition, ClientConfig clientConfig) {
        this.partition = partition;
        this.clientConfig = clientConfig;
        this.files = clientConfig.getFileByTime(partition);
    }

    public void start() throws ZipException, IOException {
        log.info("sending {} for partition {} ", files, partition);
        for (File file : files) {
            ZipFile zip = new ZipFile(file);
            Client client = newClient();
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
                ZipEntry entry = e.nextElement();
                BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
                String thisLine = null;
                while ((thisLine = reader.readLine()) != null) {
                    long count = atomicLong.incrementAndGet();
                    if (count % 1000 == 0) {
                        Client oldClient = client;
                        clientConfig.execute(new Runnable() {
                            @Override
                            public void run() {
                                oldClient.complete();
                            }
                        });
                        client = newClient();
                        byte[] bs = Base64.getDecoder().decode(thisLine);
                        Request request = Request.newBuilder().setProtobuf(ByteString.copyFrom(bs)).build();
                        client.onNext(request);
                    }
                }
            }
            Client oldClient = client;
            clientConfig.execute(new Runnable() {
                @Override
                public void run() {
                    oldClient.complete();
                }
            });
            zip.close();
        }
        log.info("total send {} for partition {} ", atomicLong, partition);
    }

    @Override
    public void run() {
        try {
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
