package sg.com.stargazer.res.fdb;

import sg.com.stargazer.client.feed.TxConsumer;
import sg.com.stargazer.res.proto.ProtoService;

import com.apple.foundationdb.Transaction;
import com.dashur.mdb.Tx.Request;
import com.google.protobuf.DynamicMessage;

public class FDbConsumer extends Thread implements TxConsumer {
    private TxProcessor txProcessor;
    private ProtoService protoService;
    private Transaction tx;

    public FDbConsumer(TxProcessor txProcessor, ProtoService protoService) {
        tx = txProcessor.newTransaction();
        this.protoService = protoService;
        this.txProcessor = txProcessor;
    }

    @Override
    public void complete() {
        tx.commit();
        tx.close();
    }

    @Override
    public void onNext(Request request, byte[] bs) throws Exception {
        DynamicMessage dynamicMessage = protoService.getMessage(bs);
        txProcessor.process(tx, dynamicMessage, bs);
    }
}
