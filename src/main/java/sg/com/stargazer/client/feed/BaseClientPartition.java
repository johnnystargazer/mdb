package sg.com.stargazer.client.feed;

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
public abstract class BaseClientPartition extends Thread {
    Integer partition;
    ClientConfig clientConfig;
    List<File> files;
    AtomicLong atomicLong = new AtomicLong(0);
    AtomicLong successCount = new AtomicLong(0);
    Integer batchSize = 500;

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public long getCount() {
        return successCount.get();
    }

    public abstract TxConsumer newClient() throws IOException;

    public BaseClientPartition(Integer partition, ClientConfig clientConfig) {
        this.partition = partition;
        this.clientConfig = clientConfig;
        this.files = clientConfig.getFileByTime(partition);
    }

    public void startTask() throws ZipException, IOException, InterruptedException {
        log.info("sending {} for partition {} ", files, partition);
        for (File file : files) {
            ZipFile zip = new ZipFile(file);
            TxConsumer client = newClient();
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
                ZipEntry entry = e.nextElement();
                BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
                String thisLine = null;
                while ((thisLine = reader.readLine()) != null) {
                    long count = atomicLong.incrementAndGet();
                    if (count % batchSize == 0) {
                        TxConsumer oldClient = client;
                        clientConfig.execute(new Runnable() {
                            @Override
                            public void run() {
                                oldClient.complete();
                                successCount.addAndGet(batchSize);
                            }
                        });
                        client = newClient();
                    }
                    process(client, thisLine);
                }
            }
            TxConsumer oldClient = client;
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

    protected void process(TxConsumer client, String thisLine) {
        try {
            byte[] bs = Base64.getDecoder().decode(thisLine);
            Request request = Request.newBuilder().setProtobuf(ByteString.copyFrom(bs)).build();
            client.onNext(request, bs);
        } catch (Exception il) {
            il.printStackTrace();
            log.info("failed to decode");
        }
    }

    @Override
    public void run() {
        try {
            startTask();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
