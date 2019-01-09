package sg.com.stargazer.client.feed;

import java.net.URL;
import java.util.List;
import java.util.Random;

import sg.com.stargazer.client.TestClient;
import lombok.extern.slf4j.Slf4j;

import com.dashur.mdb.Tx.Request;
import com.dashur.mdb.Tx.Transaction;
import com.google.common.collect.Lists;

@Slf4j
public class DataAwaredClient extends Client {
    private List<Transaction> txs = Lists.newArrayList();
    private static Random random = new Random();
    private TestClient testClient;

    public DataAwaredClient(URL url, TestClient testClient) {
        super(url);
        this.testClient = testClient;
    }

    @Override
    protected void success() {
        super.success();
        if (txs.size() > 0) {
            Transaction transaction = txs.get(random.nextInt(txs.size()));
            try {
                testClient.sendById(transaction);
                testClient.sendByCompany(transaction);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onNext(Request request, byte[] bs) {
        try {
            Transaction tx = Transaction.parseFrom(bs);
            super.onNext(request, bs);
            txs.add(tx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
