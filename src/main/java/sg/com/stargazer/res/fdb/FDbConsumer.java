package sg.com.stargazer.res.fdb;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import sg.com.stargazer.client.feed.TxConsumer;
import sg.com.stargazer.res.proto.ProtoService;

import com.apple.foundationdb.Transaction;
import com.dashur.mdb.Tx.Request;
import com.google.common.base.Stopwatch;
import com.google.protobuf.DynamicMessage;

@Slf4j
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
        Stopwatch watch = Stopwatch.createStarted();
        try {
            tx.commit().get();
        } catch (Exception e) {
            // TODO retry
            e.printStackTrace();
        }
// future.get(10, TimeUnit.SECONDS);
        tx.close();
        log.info("commit tx for batch in {} ms", watch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Override
    public void onNext(Request request, byte[] bs) throws Exception {
        DynamicMessage dynamicMessage = protoService.getMessage(bs);
        txProcessor.process(tx, dynamicMessage, bs);
    }
}
