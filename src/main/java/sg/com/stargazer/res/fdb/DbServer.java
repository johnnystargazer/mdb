package sg.com.stargazer.res.fdb;

import java.time.ZonedDateTime;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import sg.com.stargazer.res.util.Constant;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.Range;
import com.apple.foundationdb.Transaction;
import com.apple.foundationdb.directory.DirectoryLayer;
import com.apple.foundationdb.directory.DirectorySubspace;
import com.google.common.collect.Lists;

@Slf4j
public class DbServer {
    private Database db;
    final DirectoryLayer dir = new DirectoryLayer();

    public void cleanTx() {
        try {
            Transaction tx = db.createTransaction();
            List<String> main = Constant.getRangePath(ZonedDateTime.now());
            List<String> sub = dir.list(tx, main).join();
            sub.stream().forEach(a -> {
                List<String> x = Lists.newArrayList(main);
                x.add(a);
                Range range = dir.open(tx, x).join().range();
                tx.clear(range);
                tx.commit();
            });
            tx.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cleanAll() {
        try {
            Transaction tx = db.createTransaction();
            tx.clear(new Range(new byte[] { Byte.MIN_VALUE }, new byte[] { Byte.MAX_VALUE }));
            tx.commit();
            tx.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
