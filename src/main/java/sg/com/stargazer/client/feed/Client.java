package sg.com.stargazer.client.feed;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

import com.dashur.mdb.ProtoServiceGrpc;
import com.dashur.mdb.ProtoServiceGrpc.ProtoServiceStub;
import com.dashur.mdb.Tx.Request;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

@Slf4j
public class Client implements TxConsumer {
    private ManagedChannel channel;
    ProtoServiceStub aync;
    CountDownLatch latch;
    private URL url;
    private Stopwatch stopwatch;
    private Function<Long, Void> callback;

    public Client(URL url) {
        this.url = url;
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
    }

    protected void success() {
        stopwatch.stop();
        long mill = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        if (callback != null) {
            callback.apply(mill);
        }
        latch.countDown();
        requests.clear();
    }

    private io.grpc.stub.StreamObserver<com.google.protobuf.Empty> newStreamer() {
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> x =
            new io.grpc.stub.StreamObserver<com.google.protobuf.Empty>() {
                @Override
                public void onNext(Empty value) {
                    success();
                }

                @Override
                public void onError(Throwable t) {
                    log.error("batch failed will start a new batch", t);
                    try {
                        close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        start();
                        retry();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCompleted() {
                    // log.info(" === complete ==");
                }
            };
        return x;
    }

    StreamObserver<Empty> client;
    StreamObserver<Request> stream;

    public void start() throws IOException {
        channel = ManagedChannelBuilder.forAddress(url.getHost(), url.getPort()).usePlaintext().build();
        aync = ProtoServiceGrpc.newStub(channel);
        client = newStreamer();
        stream = aync.many(client);
    }

    private List<Request> requests = Lists.newArrayList();

    public void onNext(byte[] bytes) {
        Request request = Request.newBuilder().setProtobuf(ByteString.copyFrom(bytes)).build();
        onNext(request, bytes);
    }

    public void onNext(Request request, byte[] bs) {
        requests.add(request);
    }

    private void close() throws InterruptedException {
        client.onCompleted();
        shutdown();
    }

    private void push() {
        stopwatch = Stopwatch.createStarted();
        for (Request request : requests) {
            stream.onNext(request);
        }
    }

    public void retry() {
        try {
            push();
            stream.onCompleted();
        } catch (Exception e) {
            log.error("complete failed", e);
        }
    }

    public void complete() {
        try {
            push();
            latch = new CountDownLatch(1);
            stream.onCompleted();
            latch.await();
            close();
        } catch (Exception e) {
            log.error("complete failed", e);
        }
    }

    public void setCallback(Function<Long, Void> callback) {
        this.callback = callback;
    }
}
