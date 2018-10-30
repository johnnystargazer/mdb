package sg.com.stargazer.res.rest;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletOutputStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.codec.binary.Hex;

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
import com.google.protobuf.util.JsonFormat;

@Slf4j
public class GetOneBy implements Route {
    private ProtoService protoService;
    private DbServer dbServer;
    final DirectoryLayer dir = new DirectoryLayer();

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
            CompletableFuture<byte[]> data = tr.get(key);
            try {
                byte[] dt = data.get(1, TimeUnit.SECONDS);
                return dt;
            } catch (Exception e) {
                e.printStackTrace();
            }
            current = current.minusMonths(1);
        }
        return null;
    }

    private byte[] getIdKey(Transaction tr, Long txId) {
        ZonedDateTime current = ZonedDateTime.now();
        for (int i = 0; i < 12; i++) {
            List<String> path = Constant.getIdPath(current);
            log.info("try path {} ", path);
            DirectorySubspace directorySubspace = dir.createOrOpen(dbServer.getDb(), path).join();
            byte[] key = directorySubspace.pack(Constant.hashId((txId)));
            log.info("key for tx id {}  is {} , {} ", txId, key, Hex.encodeHexString(key));
            CompletableFuture<byte[]> data = tr.get(key);
            try {
                byte[] dt = data.get(1, TimeUnit.SECONDS);
                return dt;
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
        if (getByTxId(res, id)) {
            return null;
        }
        String ext = id;
        ServletOutputStream out = res.raw().getOutputStream();
        Transaction tr = dbServer.getDb().createTransaction();
        byte[] key = getExtKey(tr, ext);
        if (key == null) {
            res.status(404);
            res.type("application/json");
            out.close();
            return null;
        }
        try {
            CompletableFuture<byte[]> data = tr.get(key);
            byte[] dt = data.get(1, TimeUnit.SECONDS);
            res.status(200);
            res.type("application/json");
            DynamicMessage dynamicMessage = protoService.getMessage(dt);
            log.info("found message {} ", dynamicMessage);
            OutputStreamWriter streamWriter = new OutputStreamWriter(out);
            JsonFormat.printer().appendTo(dynamicMessage, streamWriter);
            streamWriter.flush();
            out.close();
        } catch (Exception e) {
            res.status(404);
            res.type("application/json");
            e.printStackTrace();
            out.close();
        } finally {
            tr.close();
        }
        return null;
    }

    private Boolean getByTxId(Response res, String id) throws IOException {
        Long txId;
        try {
            txId = Long.valueOf(id);
        } catch (Exception e) {
            return false;
        }
        Transaction tr = dbServer.getDb().createTransaction();
        byte[] key = getIdKey(tr, txId);
        if (key == null) {
            return false;
        }
        ServletOutputStream out = res.raw().getOutputStream();
        try {
            CompletableFuture<byte[]> data = tr.get(key);
            byte[] dt = data.get(1, TimeUnit.SECONDS);
            res.status(200);
            res.type("application/json");
            DynamicMessage dynamicMessage = protoService.getMessage(dt);
            log.info("found message {} ", dynamicMessage);
            OutputStreamWriter streamWriter = new OutputStreamWriter(out);
            JsonFormat.printer().appendTo(dynamicMessage, streamWriter);
            streamWriter.flush();
            out.close();
            return true;
        } catch (Exception e) {
            res.status(404);
            res.type("application/json");
            e.printStackTrace();
            out.close();
            return false;
        } finally {
            tr.close();
        }
    }
}
