package sg.com.stargazer.res.rest;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.codec.binary.Hex;

import sg.com.stargazer.res.exception.HttpException;
import sg.com.stargazer.res.fdb.DbServer;
import sg.com.stargazer.res.proto.ProtoService;
import sg.com.stargazer.res.util.Constant;
import spark.Request;
import spark.Response;
import spark.Route;

import com.apple.foundationdb.Transaction;
import com.apple.foundationdb.directory.DirectoryLayer;
import com.apple.foundationdb.directory.DirectorySubspace;
import com.google.protobuf.DynamicMessage;

@Slf4j
public class GetOneBy implements Route {
    private ProtoService protoService;
    private DbServer dbServer;
    final DirectoryLayer dir = new DirectoryLayer();
    private static int TIMEOUT = 100;

    public GetOneBy(ProtoService protoService, DbServer dbServer) {
        this.protoService = protoService;
        this.dbServer = dbServer;
    }

    private byte[] getExtKey(Transaction tr, String extRefId) {
        ZonedDateTime current = ZonedDateTime.now();
        for (int i = 0; i < 12; i++) {
            List<String> path = Constant.getExtPath(current);
            DirectorySubspace directorySubspace = dir.createOrOpen(dbServer.getDb(), path).join();
            byte[] key = directorySubspace.pack(extRefId);
            log.info("key for tx id {}  is {} , {}  in path {} ", extRefId, key, Hex.encodeHexString(key), path);
            CompletableFuture<byte[]> data = tr.get(key);
            try {
                byte[] dt = data.get(TIMEOUT, TimeUnit.SECONDS);
                if (dt != null) {
                    return dt;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            current = current.minusMonths(1);
        }
        return null;
    }

    public static void main(String[] args) {
        ZonedDateTime current = ZonedDateTime.now();
        for (int i = 0; i < 12; i++) {
            List<String> path = Constant.getIdPath(current);
            log.info("try path {} ", path);
            current = current.minusMonths(1);
        }
    }

    private byte[] getIdKey(Transaction tr, Long txId) {
        ZonedDateTime current = ZonedDateTime.now();
        for (int i = 0; i < 12; i++) {
            List<String> path = Constant.getIdPath(current);
            DirectorySubspace directorySubspace = dir.createOrOpen(dbServer.getDb(), path).join();
            byte[] key = directorySubspace.pack(Constant.hashId((txId)));
            log.info("key for tx id {}  is {} , {}  in path {} ", txId, key, Hex.encodeHexString(key), path);
            CompletableFuture<byte[]> data = tr.get(key);
            try {
                byte[] dt = data.get(TIMEOUT, TimeUnit.SECONDS);
                if (dt != null) {
                    log.info("found key ", Hex.encodeHexString(dt));
                    return dt;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            current = current.minusMonths(1);
        }
        return null;
    }

    @Override
    public Object handle(Request request, Response res) throws Exception {
        String id = request.params(":id");
        log.info("id {} ", id);
        Transaction tr = dbServer.getDb().createTransaction();
        Optional<DynamicMessage> byId = getByTxId(res, id, tr);
        if (byId.isPresent()) {
            return byId.get();
        }
        String ext = id;
        byte[] key = getExtKey(tr, ext);
        if (key == null) {
            throw new HttpException(404, "Not found");
        }
        try {
            CompletableFuture<byte[]> data = tr.get(key);
            byte[] dt = data.get(1, TimeUnit.SECONDS);
            DynamicMessage dynamicMessage = protoService.getMessage(dt);
            return dynamicMessage;
        } catch (Exception e) {
            throw new HttpException(500, e.getMessage());
        } finally {
            tr.close();
        }
    }

    private Optional<DynamicMessage> getByTxId(Response res, String id, Transaction tr) throws IOException {
        Long txId;
        try {
            txId = Long.valueOf(id);
        } catch (Exception e) {
            return Optional.empty();
        }
        byte[] key = getIdKey(tr, txId);
        if (key == null) {
            return Optional.empty();
        }
        try {
            CompletableFuture<byte[]> data = tr.get(key);
            byte[] dt = data.get(1, TimeUnit.SECONDS);
            DynamicMessage dynamicMessage = protoService.getMessage(dt);
            return Optional.of(dynamicMessage);
        } catch (Exception e) {
            throw new HttpException(500, e.getMessage());
        } finally {
            tr.close();
        }
    }
}
