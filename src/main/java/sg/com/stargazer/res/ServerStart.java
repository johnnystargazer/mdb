package sg.com.stargazer.res;

import static spark.Spark.get;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;

import java.io.InputStream;

import sg.com.stargazer.res.fdb.DbServer;
import sg.com.stargazer.res.grpc.RpcServer;
import sg.com.stargazer.res.proto.ProtoService;
import sg.com.stargazer.res.rest.DynamicMessageResponseTransformer;
import sg.com.stargazer.res.rest.GetOneBy;
import sg.com.stargazer.res.rest.Import;
import sg.com.stargazer.res.rest.RangeQuery;

import com.google.common.collect.ImmutableList;

public class ServerStart {
    private static final ApplicationProtocolConfig ALPN = new ApplicationProtocolConfig(Protocol.ALPN,
        SelectorFailureBehavior.NO_ADVERTISE, SelectedListenerFailureBehavior.ACCEPT, ImmutableList.of("h2"));

    static public void main(String[] args) throws Exception {
        DbServer dbServer = new DbServer();
        dbServer.start();
        InputStream stream = ServerStart.class.getClassLoader().getResourceAsStream("test.protoset");
        ProtoService protoService = new ProtoService(stream);
        get("/tx/:id", new GetOneBy(protoService, dbServer));
        get("/tx", new RangeQuery(protoService, dbServer), new DynamicMessageResponseTransformer());
        get("/import", new Import(protoService, dbServer));
        RpcServer rpcServer = new RpcServer(dbServer, protoService);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                dbServer.shutdown();
            }
        });
        rpcServer.start();
    }
}