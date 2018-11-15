package sg.com.stargazer.res.fdb;

import java.time.ZonedDateTime;
import java.util.List;

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
     */
    public void cleanTxByPath(List<String> path) {
        Transaction tx = db.createTransaction();
        DirectorySubspace sub = dir.createOrOpen(tx, path).join();
        tx.clear(sub.range());
        tx.commit();
        tx.close();
    }

    public Transaction newTransaction() {
        return db.createTransaction();
    }

    public Database getDb() {
        return db;
    }

    public void start() {
        FDB fdb = FDB.selectAPIVersion(520);
        db = fdb.open();
    }
}
