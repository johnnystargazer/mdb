package sg.com.stargazer.client;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Data
@Getter
@Setter
@Slf4j
public class ClientConfig {
    private String base = "/home/pdcc/workspace/proto";
    private DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd['T']HH:mm:ss");
    private ZonedDateTime start;
    private ZonedDateTime stop;
    private ScheduledExecutorService exec = Executors.newScheduledThreadPool(4);

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
            LocalDateTime date =
                LocalDateTime.from(FORMAT.parse(name.substring(name.indexOf("-", name.indexOf("-") + 1) + 1,
                    name.length() - 5)));
            ZonedDateTime time = date.atZone(ZoneId.of("UTC"));
            maps.put(time, file);
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
        ZonedDateTime start = ZonedDateTime.of(2018, 10, 1, 0, 50, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime stop = ZonedDateTime.of(2018, 12, 1, 1, 0, 0, 0, ZoneId.of("UTC"));
        clientConfig.setStart(start);
        clientConfig.setStop(stop);
        List<File> fs = clientConfig.getFileByTime(0);
        log.info("found {} ", fs);
    }

    public void execute(Runnable runnable) {
        exec.execute(runnable);
    }

    public void shutdown() throws InterruptedException {
        exec.shutdown();
        exec.awaitTermination(1, TimeUnit.MINUTES);
    }
}
