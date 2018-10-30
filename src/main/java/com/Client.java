package com;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import lombok.extern.slf4j.Slf4j;

import com.dashur.mdb.ProtoServiceGrpc;
import com.dashur.mdb.ProtoServiceGrpc.ProtoServiceStub;
import com.dashur.mdb.Tx.Request;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

@Slf4j
public class Client {
    // private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldClent.class);
    private final ManagedChannel channel;
    private final ProtoServiceGrpc.ProtoServiceBlockingStub blockingStub;
    ProtoServiceStub aync;
    AtomicBoolean atomicBoolean = new AtomicBoolean(true);

    /** Construct client connecting to HelloWorld server at {@code host:port}. */
    public Client(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        blockingStub = ProtoServiceGrpc.newBlockingStub(channel);
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
                    atomicBoolean.set(true);
                    System.out.println("receive value " + value);
                }

                @Override
                public void onError(Throwable t) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onCompleted() {
                    // TODO Auto-generated method stub
                }
            };
        return x;
    }

    public void start() throws IOException {
        StreamObserver<Empty> client = newStreamer();
        StreamObserver<Request> stream = aync.many(client);
        AtomicLong atomicLong = new AtomicLong();
        ZipFile zip = new ZipFile("P_0-3642-2018-10-22T20:19:34Z.zip");
        for (Enumeration e = zip.entries(); e.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
            String thisLine = null;
            while ((thisLine = reader.readLine()) != null) {
                long count = atomicLong.incrementAndGet();
                if (count % 400 == 0) {
                    stream.onCompleted();
                    atomicBoolean.set(false);
                    while (!atomicBoolean.get()) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }
                    stream = aync.many(client);
                }
                Request request =
                    Request.newBuilder().setProtobuf(ByteString.copyFrom(Base64.getDecoder().decode(thisLine))).build();
                stream.onNext(request);
            }
        }
        System.out.println(atomicLong.get());
        stream.onCompleted();
    }

    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the
     * greeting.
     */
    public static void main(String[] args) throws Exception {
        Client client = new Client("localhost", 8080);
        try {
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
        for (;;) {
            Thread.sleep(200000);
        }
    }
}
