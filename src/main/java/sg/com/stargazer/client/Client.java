package sg.com.stargazer.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

import com.dashur.mdb.ProtoServiceGrpc;
import com.dashur.mdb.ProtoServiceGrpc.ProtoServiceStub;
import com.dashur.mdb.Tx.Request;
import com.google.protobuf.Empty;

@Slf4j
public class Client {
    private final ManagedChannel channel;
    ProtoServiceStub aync;
    private AtomicBoolean available = new AtomicBoolean(true);

    public Client(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        aync = ProtoServiceGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    private io.grpc.stub.StreamObserver<com.google.protobuf.Empty> newStreamer() {
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> x =
            new io.grpc.stub.StreamObserver<com.google.protobuf.Empty>() {
                @Override
                public void onNext(Empty value) {
                    available.set(true);
                }

                @Override
                public void onError(Throwable t) {
                    log.error("exception when commit", t);
                    available.set(true);
                }

                @Override
                public void onCompleted() {
                    log.info(" === complete ==");
                }
            };
        return x;
    }

    StreamObserver<Empty> client;
    StreamObserver<Request> stream;

    public void start() throws IOException {
        client = newStreamer();
        stream = aync.many(client);
    }

    public void onNext(Request request) {
        stream.onNext(request);
    }

    public void complete() {
        try {
            stream.onCompleted();
            available.set(false);
            while (!available.get()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            client.onCompleted();
            shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
