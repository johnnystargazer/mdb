package sg.com.stargazer.client.feed;

import com.dashur.mdb.Tx.Request;

public interface TxConsumer {
    void complete();

    void onNext(Request request, byte[] bs) throws Exception;
}
