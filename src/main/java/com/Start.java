package com;

import static spark.Spark.get;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;

import java.io.InputStream;

import com.google.common.collect.ImmutableList;
import com.rest.GetOneBy;
import com.rest.RangeQuery;

//@Slf4j
public class Start {
    private static final ApplicationProtocolConfig ALPN = new ApplicationProtocolConfig(Protocol.ALPN,
        SelectorFailureBehavior.NO_ADVERTISE, SelectedListenerFailureBehavior.ACCEPT, ImmutableList.of("h2"));

    static public void main(String[] args) throws Exception {
        DbServer dbServer = new DbServer();
        dbServer.start();
        dbServer.cleanTx();
        InputStream stream = Start.class.getClassLoader().getResourceAsStream("test.protoset");
        ProtoService protoService = new ProtoService(stream);
        get("/hello", new RangeQuery(true, protoService, dbServer));
        get("/hello2", new RangeQuery(false, protoService, dbServer));
        get("/tx", new GetOneBy(protoService, dbServer));
        RpcServer rpcServer = new RpcServer(dbServer, protoService);
        rpcServer.start();
    }
}