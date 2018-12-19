package sg.com.stargazer.res.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import sg.com.stargazer.res.fdb.DbServer;
import sg.com.stargazer.res.fdb.TxProcessor;
import sg.com.stargazer.res.proto.ProtoService;

import com.apple.foundationdb.Transaction;
import com.dashur.mdb.ProtoServiceGrpc;
import com.dashur.mdb.Tx.Request;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;

@Slf4j
public class RpcServer extends ProtoServiceGrpc.ProtoServiceImplBase {
    public DbServer dbServer;
    private ProtoService protoService;
    private TxProcessor txProcessor;

    public RpcServer(DbServer dbServer, ProtoService protoService) {
        this.dbServer = dbServer;
        this.protoService = protoService;
        this.txProcessor = new TxProcessor(dbServer, protoService);
    }

    public static ZonedDateTime zonedDatetime(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()).atZone(ZoneId.systemDefault());
    }

    public void start() throws Exception {
        ServerBuilder<?> server = ServerBuilder.forPort(8080);
        Server bd = server.addService(this).build();
        bd.start();
        bd.awaitTermination();
    }

    @Override
    public void one(Request request, StreamObserver<Empty> responseObserver) {
        try {
            Transaction tx = dbServer.getDb().createTransaction();
            byte[] bs = request.getProtobuf().toByteArray();
            DynamicMessage dynamicMessage = protoService.getMessage(bs);
            txProcessor.process(tx, dynamicMessage, bs);
            tx.commit().get();
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public StreamObserver<Request> many(StreamObserver<Empty> responseObserver) {
        Transaction tx = dbServer.getDb().createTransaction();
        return new StreamObserver<Request>() {
            @Override
            public void onNext(Request request) {
                try {
                    byte[] bs = request.getProtobuf().toByteArray();
                    DynamicMessage dynamicMessage = protoService.getMessage(bs);
                    txProcessor.process(tx, dynamicMessage, bs);
                } catch (Exception e) {
                    //
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable t) {
                tx.cancel();
                tx.close();
            }

            @Override
            public void onCompleted() {
                try {
                    CompletableFuture<Void> future = tx.commit();
                    future.get(10, TimeUnit.SECONDS);
                    tx.close();
                    responseObserver.onNext(Empty.newBuilder().build());
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    e.printStackTrace();
                    responseObserver.onError(e);
                }
            }
        };
    }

    public void stop() {
        // TODO Auto-generated method stub
    }
}
