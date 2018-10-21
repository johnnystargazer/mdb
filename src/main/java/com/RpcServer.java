package com;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import com.apple.foundationdb.Transaction;
import com.apple.foundationdb.directory.DirectoryLayer;
import com.apple.foundationdb.directory.DirectorySubspace;
import com.apple.foundationdb.tuple.Tuple;
import com.dashur.mdb.ProtoServiceGrpc;
import com.dashur.mdb.Tx.Request;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Empty;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.rest.ProtoField;

@Slf4j
public class RpcServer extends ProtoServiceGrpc.ProtoServiceImplBase {
    public DbServer dbServer;
    private ProtoService protoService;
    private ProtoField id;
    private ProtoField accountId;
    private ProtoField externalRef;
    private ProtoField created;

    public RpcServer(DbServer dbServer, ProtoService protoService) {
        this.dbServer = dbServer;
        this.protoService = protoService;
        id = protoService.getFieldDescriptorByName("id");
        accountId = protoService.getFieldDescriptorByName("account_id");
        externalRef = protoService.getFieldDescriptorByName("external_ref");
        created = protoService.getFieldDescriptorByName("created");
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
        String[] path = request.getPathList().toArray(new String[request.getPathList().size()]);
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<Request> many(StreamObserver<Empty> responseObserver) {
        Transaction tx = dbServer.getDb().createTransaction();
        final DirectoryLayer dir = new DirectoryLayer();
        return new StreamObserver<Request>() {
            @Override
            public void onNext(Request point) {
                try {
                    byte[] bs = point.getProtobuf().toByteArray();
                    DynamicMessage dynamicMessage = protoService.getMessage(bs);
                    Object idValue = id.getValuefromMessage(dynamicMessage);
                    Object accountIdValue = accountId.getValuefromMessage(dynamicMessage);//
                    String exter = (String) externalRef.getValuefromMessage(dynamicMessage);
                    DynamicMessage dt = (DynamicMessage) created.getValuefromMessage(dynamicMessage);
                    ZonedDateTime time = zonedDatetime(Timestamp.parseFrom(dt.toByteArray()));
                    List<String> path = Constant.getRangePath(time, (Long) accountIdValue);
                    DirectorySubspace foo = dir.createOrOpen(tx, path).join();
                    Long millsec = time.toInstant().toEpochMilli();
                    byte[] rangeKey = foo.pack(Tuple.from(millsec, idValue));
                    tx.set(rangeKey, bs);
                    // ==================================
                    List<String> idPath = Constant.getIdPath(time);
                    DirectorySubspace idSpace = dir.createOrOpen(tx, idPath).join();
                    byte[] key = idSpace.pack(idValue);
                    tx.set(idSpace.pack(idValue), rangeKey);
                    // =================================
                    List<String> extPath = Constant.getExtPath(time);
                    DirectorySubspace extSpace = dir.createOrOpen(tx, extPath).join();
                    tx.set(extSpace.pack(exter), rangeKey);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable t) {
                // log.error("grpc failed ", t);
                tx.cancel();
            }

            @Override
            public void onCompleted() {
                try {
                    // log.info("commit transaction ");
                    tx.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                responseObserver.onNext(Empty.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
    }
}
