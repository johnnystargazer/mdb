package sg.com.stargazer.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import lombok.extern.slf4j.Slf4j;

import com.dashur.mdb.ProtoServiceGrpc;
import com.dashur.mdb.ProtoServiceGrpc.ProtoServiceStub;
import com.dashur.mdb.Tx.Request;
import com.google.common.base.Stopwatch;
import com.google.protobuf.ByteString;
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
                }

                @Override
                public void onCompleted() {
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
    }
}
