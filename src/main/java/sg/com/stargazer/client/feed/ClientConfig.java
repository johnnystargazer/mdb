package sg.com.stargazer.client.feed;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import com.dashur.mdb.Tx;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

@Data
@Slf4j
public class ClientConfig {
    private String base;
    private DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd['T']HH:mm:ss");
    private ZonedDateTime start;
    private Long startSec;
    private ZonedDateTime startAt = ZonedDateTime.now();
    private Long startAtSec = startAt.toEpochSecond();
    private ZonedDateTime stop;
    private Long timeDiff;
    private Double speed;
    ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
    private URL url;
    private ZoneId TIMEZONE = ZoneId.of("UTC");
    private Long batchSec = 5L;
    private CountDownLatch countDownLatch = new CountDownLatch(18);
    final Lock lock = new ReentrantLock();
    final Condition notFull = lock.newCondition();

    /**
     * 
     * @param transaction
     * @return seconds for wait
     */
    public long scheduleAfter(Tx.Transaction transaction) {
        if (speed <= 0) {
            return -1;
        }
        // current time
        long sec = transaction.getCreated().getSeconds();
        long realDiff = sec - startSec;
        long now = ZonedDateTime.now().toEpochSecond();
        long timePassed = now + batchSec - startAtSec;
        return Double.valueOf((timePassed * speed - realDiff) / speed).longValue();
    }

    public void setUrl(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    public void setPath(String path) {
        this.base = path;
        log.info("path {} ", path);
    }

    public void setSpeed(String speed) {
        this.speed = Double.valueOf(speed);
    }

    public void setStart(String start) {
        this.start = LocalDateTime.from(FORMAT.parse(start)).atZone(TIMEZONE);
        this.startSec = this.start.toEpochSecond();
        this.timeDiff = this.startAt.toEpochSecond() - this.start.toEpochSecond();
    }

    public void setStop(String stop) {
        this.stop = LocalDateTime.from(FORMAT.parse(stop)).atZone(TIMEZONE);
    }

    public List<File> getFileByTime(Integer partition) {
        File b = new File(base);
        File archive = new File(b, "archive");
        if (!archive.exists()) {
            log.warn("archive folder not exists ");
            return Collections.emptyList();
        }
        File p = new File(archive, partition.toString());
        if (!p.exists()) {
            log.warn("partition folder not exists ");
            return Collections.emptyList();
        }
        File[] files = p.listFiles();
        Map<ZonedDateTime, File> maps = Maps.newLinkedHashMap();
        for (File file : files) {
            String name = file.getName();
            try {
                LocalDateTime date =
                    LocalDateTime.from(FORMAT.parse(name.substring(name.indexOf("-", name.indexOf("-") + 1) + 1,
                        name.length() - 5)));
                ZonedDateTime time = date.atZone(ZoneId.of("UTC"));
                maps.put(time, file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        List<File> fs = Lists.newLinkedList();
        List<ZonedDateTime> keys = Lists.newArrayList(maps.keySet());
        Collections.sort(keys);
        ZonedDateTime last = null;
        Boolean find = Boolean.FALSE;
        for (ZonedDateTime time : keys) {
            if (start.compareTo(time) <= 0) {
                if (!find) {
                    if (last != null) {
                        fs.add(maps.get(last));
                    }
                    if (stop.compareTo(time) >= 0) {
                        fs.add(maps.get(time));
                    }
                    find = true;
                } else {
                    if (stop.compareTo(time) >= 0) {
                        fs.add(maps.get(time));
                    }
                }
            }
            if (find) {
                if (stop.compareTo(time) <= 0) {
                    break;
                }
            }
            last = time;
        }
        return fs;
    }

    public static void main(String[] args) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setStart("2018-10-01T00:00:00");
        clientConfig.setStop("2018-12-01T00:00:00");
        List<File> fs = clientConfig.getFileByTime(0);
        log.info("found {} ", fs);
    }

    private int count;
    private int max = 12;

    // MAX 12 batch
    public void execute(Runnable runnable) throws InterruptedException {
        try {
            lock.lock();
            while (count == max) {
                notFull.await();
            }
            ++count;
            ListenableFuture future = service.submit(runnable);
            Futures.addCallback(future, new FutureCallback<Object>() {
                @Override
                public void onSuccess(Object result) {
                    try {
                        lock.lock();
                        --count;
                        notFull.signal();
                    } finally {
                        lock.unlock();
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        lock.lock();
                        --count;
                        notFull.signal();
                    } finally {
                        lock.unlock();
                    }
                }
            });
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() throws InterruptedException {
        service.shutdown();
        service.awaitTermination(1, TimeUnit.MINUTES);
    }

    private SharedClient sharedClient;

    public SharedClient getSharedClient() {
        if (sharedClient == null) {
            return new SharedClient(url);
        }
        return null;
    }
}
