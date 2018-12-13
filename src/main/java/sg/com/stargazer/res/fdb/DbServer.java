package sg.com.stargazer.res.fdb;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.extern.slf4j.Slf4j;
import sg.com.stargazer.res.util.Constant;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.Transaction;
import com.apple.foundationdb.directory.DirectoryLayer;
import com.apple.foundationdb.directory.DirectorySubspace;
import com.google.common.collect.Lists;

@Slf4j
public class DbServer {
    private Database db;
    final DirectoryLayer dir = new DirectoryLayer();

    public void shutdown() {
        this.db.close();
    }

    public void list() {
        try {
            Transaction tx = db.createTransaction();
            List<String> sub = dir.list(tx, Lists.newArrayList("Transaction", "id")).join();
            sub.stream().forEach(a -> {
                log.info("directory {} ", a);
            });
            tx.commit();
            tx.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void test() throws InterruptedException, ExecutionException, TimeoutException {
        Transaction tx = db.createTransaction();
        List<String> t = Lists.newArrayList("test1", "test2");
        DirectorySubspace directorySubspace = dir.createOrOpen(db, t).join();
        byte[] key = directorySubspace.pack(192L);
        tx.set(key, "abc".getBytes());
        CompletableFuture<Void> fu = tx.commit();
        fu.get(1, TimeUnit.MINUTES);
        System.out.println("finish commit");
        tx.close();
        tx = db.createTransaction();
        CompletableFuture<byte[]> future = tx.get(key);
        byte[] bs = future.get(100, TimeUnit.SECONDS);
        if (bs == null) {
            System.out.println("got issue ?");
        } else {
            System.out.println(new String(bs));
        }
        tx.close();
    }

    public void cleanTx() {
        try {
            Transaction tx = db.createTransaction();
            List<String> main = Constant.getRangePath(ZonedDateTime.now());
            List<String> sub = dir.list(tx, main).join();
            sub.stream().forEach(a -> {
                List<String> arr = Lists.newArrayList(main);
                arr.add(a);
                dir.remove(db, arr);
            });
            tx.commit();
            tx.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cleanAll() {
        try {
            Transaction tx = db.createTransaction();
            List<String> subs = dir.list(db).join();
            for (String path : subs) {
                dir.remove(db, Lists.newArrayList(path));
            }
            tx.commit();
            tx.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * clean data in path , but directory still exists
     * 
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void cleanTxByPath(List<String> path) throws InterruptedException, ExecutionException {
        Transaction tx = db.createTransaction();
        DirectorySubspace sub = dir.createOrOpen(tx, path).join();
        tx.clear(sub.range());
        tx.commit().get();
        tx.close();
    }

    public Transaction newTransaction() {
        return db.createTransaction();
    }

    public Database getDb() {
        return db;
    }

    public void start(String dbFile) {
        System.out.println("Start with " + dbFile);
        FDB fdb = FDB.selectAPIVersion(520);
        db = fdb.open(dbFile);
    }
}
