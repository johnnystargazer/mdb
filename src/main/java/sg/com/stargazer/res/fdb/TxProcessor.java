package sg.com.stargazer.res.fdb;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
    private ProtoField companyId;
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
        companyId = protoService.getFieldDescriptorByName("company_id");
        externalRef = protoService.getFieldDescriptorByName("external_ref");
        created = protoService.getFieldDescriptorByName("created");
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
                ts.commit().get();
                ts.close();
                return result;
            }
        });
    private int i = 0;

    public void process(Transaction tx, DynamicMessage dynamicMessage, byte[] bs) throws Exception {
        Object idValue = id.getValuefromMessage(dynamicMessage);
        Object accountIdValue = accountId.getValuefromMessage(dynamicMessage);//
        Object compnayIdValue = companyId.getValuefromMessage(dynamicMessage);//
        String exter = (String) externalRef.getValuefromMessage(dynamicMessage);
        DynamicMessage dt = (DynamicMessage) created.getValuefromMessage(dynamicMessage);
        ZonedDateTime time = Constant.zonedDatetime(Timestamp.parseFrom(dt.toByteArray()));
        Long millsec = time.toInstant().toEpochMilli();
        // for
        List<String> path = Constant.getCompanyRangePath(time, (Long) compnayIdValue);
        DirectorySubspace foo = loadingCache.get(path);
        byte[] companyRangeKey = foo.pack(Tuple.from(millsec, idValue));
        tx.set(companyRangeKey, bs);
        // ==================================
        indexId(tx, idValue, time, companyRangeKey);
        indexExtref(tx, exter, time, companyRangeKey);
        indexAccount(tx, (Long) accountIdValue, time, companyRangeKey, millsec, (Long) idValue);
        /**
         * not support append data in java api , better force unique ext id
         */
    }

    public void indexAccount(Transaction tx, Long accountId, ZonedDateTime time, byte[] companyRangeKey, Long millsec,
        Long idValue) throws ExecutionException {
        List<String> idPath = Constant.getAccountRangePath(time, accountId);
        DirectorySubspace idSpace = loadingCache.get(idPath);
        byte[] accountKey = idSpace.pack(Tuple.from(millsec, idValue));
        tx.set(accountKey, companyRangeKey);
    }

    private void indexExtref(Transaction tx, String exter, ZonedDateTime time, byte[] companyRangeKey)
        throws ExecutionException {
        List<String> extPath = Constant.getExtPath(time);
        DirectorySubspace extSpace = loadingCache.get(extPath);
        tx.set(extSpace.pack(exter), companyRangeKey);
    }

    private void indexId(Transaction tx, Object idValue, ZonedDateTime time, byte[] companyRangeKey)
        throws ExecutionException {
        List<String> idPath = Constant.getIdPath(time);
        DirectorySubspace idSpace = loadingCache.get(idPath);
        byte[] key = idSpace.pack(Constant.hashId((Long) idValue));
        tx.set(key, companyRangeKey);
    }
}
