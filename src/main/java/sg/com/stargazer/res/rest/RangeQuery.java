package sg.com.stargazer.res.rest;

import java.io.OutputStreamWriter;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;

import lombok.extern.slf4j.Slf4j;
import sg.com.stargazer.res.fdb.DbServer;
import sg.com.stargazer.res.proto.ProtoField;
import sg.com.stargazer.res.proto.ProtoService;
import sg.com.stargazer.res.rest.DayDuration.HourDurationIterator;
import sg.com.stargazer.res.util.Constant;
import spark.Response;
import spark.Route;

import com.apple.foundationdb.KeyValue;
import com.apple.foundationdb.Transaction;
import com.apple.foundationdb.async.AsyncIterator;
import com.apple.foundationdb.directory.DirectoryLayer;
import com.apple.foundationdb.directory.DirectorySubspace;
import com.apple.foundationdb.tuple.Tuple;
import com.google.common.collect.Maps;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;

@Slf4j
public class RangeQuery implements Route {
    private ProtoService protoService;
    private DbServer dbServer;
    private Map<String, ProtoField> filerableFields = Maps.newHashMap();
    final DirectoryLayer dir = new DirectoryLayer();

    public RangeQuery(ProtoService protoService, DbServer dbServer) {
        this.protoService = protoService;
        this.dbServer = dbServer;
        protoService.getAllFields().stream().forEach(a -> {
            if (!"account_id".equals(a.getName())) {
                filerableFields.put(a.getName(), a);
            }
        });
        log.info("fiterable fields {} ", filerableFields);
    }

    @Override
    public Object handle(spark.Request request, Response res) throws Exception {
        res.status(200);
        res.type("application/json");
        String accountId = request.queryParams("account_id");
        Page page = new Page(request);
        DayDuration day =
            DayDuration.of(request.queryParamOrDefault("from", "today"), request.queryParamOrDefault("to", "today"));
        ZonedDateTime from = day.getStart();
        ZonedDateTime to = day.getEnd();
        long fromRange = from.toInstant().toEpochMilli();
        long toRange = to.toInstant().toEpochMilli();
        DayDuration dayDuration = new DayDuration(from, to);
        HourDurationIterator dayIt = dayDuration.iterator();
        List<Filter> filters =
            request.queryParams().stream().map(a -> filerableFields.get(a)).filter(p -> p != null)
                .map(f -> new Filter(f, request.queryParams(f.getName()))).collect(Collectors.toList());
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        BruteForceFilter bruteForceFilter = new BruteForceFilter(filters);
        ServletOutputStream out = res.raw().getOutputStream();
        OutputStreamWriter streamWriter = new OutputStreamWriter(out);
        Transaction tr = dbServer.getDb().createTransaction();
        out.print("[");
        while (dayIt.hasNext()) {
            ZonedDateTime now = dayIt.next();
            List<String> path = Constant.getAccountRangePath(now, Long.valueOf(accountId));
            log.info("try {}  path {} ", now, path);
            DirectorySubspace foo = dir.createOrOpen(dbServer.getDb(), path).join();
            try {
                byte[] s = foo.pack(Tuple.from(fromRange, 1L));
                byte[] e = foo.pack(Tuple.from(toRange, Long.MAX_VALUE));
                AsyncIterator<KeyValue> range = tr.getRange(s, e, Integer.MAX_VALUE, true).iterator();
                while (range.hasNext()) {
                    KeyValue keyValue = range.next();
                    byte[] value = keyValue.getValue();
                    DynamicMessage dynamicMessage = protoService.getMessage(value);
                    if (bruteForceFilter.accept(dynamicMessage)) {
                        page.increase();
                        if (page.write()) {
                            if (!atomicBoolean.get()) {
                                atomicBoolean.set(true);
                            } else {
                                out.print(",");
                            }
                            JsonFormat.printer().appendTo(dynamicMessage, streamWriter);
                            streamWriter.flush();
                        }
                        if (page.finish()) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tr.close();
        out.print("]");
        out.close();
        return null;
    }

    public static void main(String[] args) {
        System.out.println(System.currentTimeMillis());
        Tuple t1 = Tuple.from(1540199813014L, 1l);
        Tuple t2 = Tuple.from(1540199813014L, Long.MAX_VALUE);
        log.info("\n{} \n{} ", t1.pack(), t2.pack());
    }
}