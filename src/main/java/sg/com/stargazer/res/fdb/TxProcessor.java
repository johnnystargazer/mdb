package sg.com.stargazer.res.fdb;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import sg.com.stargazer.res.proto.ProtoField;
import sg.com.stargazer.res.proto.ProtoService;
import sg.com.stargazer.res.util.Constant;

import com.apple.foundationdb.Transaction;
import com.apple.foundationdb.directory.DirectoryLayer;
import com.apple.foundationdb.directory.DirectorySubspace;
import com.apple.foundationdb.tuple.Tuple;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Timestamp;

@Slf4j
public class TxProcessor {
    public DbServer dbServer;
    private ProtoField id;
    private ProtoField accountId;
    private ProtoField externalRef;
    private ProtoField created;
    private static final DirectoryLayer dir = new DirectoryLayer();

    public Transaction newTransaction() {
        return dbServer.newTransaction();
    }

    public TxProcessor(DbServer dbServer, ProtoService protoService) {
        this.dbServer = dbServer;
        id = protoService.getFieldDescriptorByName("id");
        accountId = protoService.getFieldDescriptorByName("account_id");
        externalRef = protoService.getFieldDescriptorByName("external_ref");
        created = protoService.getFieldDescriptorByName("created");
    }

    public static ZonedDateTime zonedDatetime(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()).atZone(ZoneId.systemDefault());
    }

    /**
     * very slow if not cache
     */
    private LoadingCache<List<String>, DirectorySubspace> loadingCache = CacheBuilder.newBuilder().maximumSize(100000)
        .expireAfterAccess(10, TimeUnit.MINUTES).build(new CacheLoader<List<String>, DirectorySubspace>() {
            public DirectorySubspace load(List<String> key) throws Exception {
                Transaction ts = dbServer.newTransaction();
                CompletableFuture<DirectorySubspace> res = dir.createOrOpen(ts, key);
                DirectorySubspace result = res.join();
                ts.commit();
                ts.close();
                return result;
            }
        });

    public void process(Transaction tx, DynamicMessage dynamicMessage, byte[] bs) throws Exception {
        Object idValue = id.getValuefromMessage(dynamicMessage);
        log.info("id {} ", idValue);
        Object accountIdValue = accountId.getValuefromMessage(dynamicMessage);//
        String exter = (String) externalRef.getValuefromMessage(dynamicMessage);
        DynamicMessage dt = (DynamicMessage) created.getValuefromMessage(dynamicMessage);
        ZonedDateTime time = zonedDatetime(Timestamp.parseFrom(dt.toByteArray()));
        List<String> path = Constant.getRangePath(time, (Long) accountIdValue);
        DirectorySubspace foo = loadingCache.get(path);
        Long millsec = time.toInstant().toEpochMilli();
        byte[] rangeKey = foo.pack(Tuple.from(millsec, idValue));
        tx.set(rangeKey, bs);
        // ==================================
        List<String> idPath = Constant.getIdPath(time);
        DirectorySubspace idSpace = loadingCache.get(idPath);
        tx.set(idSpace.pack(Constant.hashId((Long) idValue)), rangeKey);
// tx.set(idSpace.pack(idValue), rangeKey);
        // =================================
        /**
         * not support append data in java api , better force unique ext id
         */
        List<String> extPath = Constant.getExtPath(time);
        DirectorySubspace extSpace = loadingCache.get(extPath);
        tx.set(extSpace.pack(exter), rangeKey);
    }
}
