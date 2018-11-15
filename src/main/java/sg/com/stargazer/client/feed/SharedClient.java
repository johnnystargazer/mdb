package sg.com.stargazer.client.feed;

import java.net.URL;

import com.dashur.mdb.Tx;
import com.dashur.mdb.Tx.Transaction;

/**
 * Have't implement yet
 *
 */
public class SharedClient extends Client {
    private ClientConfig clientConfig;

    public SharedClient(URL url, ClientConfig clientConfig) {
        super(url);
        this.clientConfig = clientConfig;
    }

    public SharedClient(URL url) {
        super(url);
    }

    @Override
    public void onNext(byte[] bytes) {
        try {
            Transaction tx = Tx.Transaction.parser().parseFrom(bytes);
            long waitSec = clientConfig.scheduleAfter(tx);
            if (waitSec > 0) {
                Thread.sleep(waitSec);
            }
            super.onNext(bytes);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
