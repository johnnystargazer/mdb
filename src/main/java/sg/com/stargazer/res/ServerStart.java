package sg.com.stargazer.res;

import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;

import sg.com.stargazer.res.exception.ErrorModel;
import sg.com.stargazer.res.exception.HttpException;
import sg.com.stargazer.res.fdb.DbServer;
import sg.com.stargazer.res.grpc.RpcServer;
import sg.com.stargazer.res.proto.ProtoService;
import sg.com.stargazer.res.rest.DynamicMessageResponseTransformer;
import sg.com.stargazer.res.rest.GetOneBy;
import sg.com.stargazer.res.rest.Import;
import sg.com.stargazer.res.rest.RangeQuery;
import sg.com.stargazer.res.security.RateLimitFilter;
import sg.com.stargazer.res.security.SecurityFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

public class ServerStart {
    private static final ApplicationProtocolConfig ALPN = new ApplicationProtocolConfig(Protocol.ALPN,
        SelectorFailureBehavior.NO_ADVERTISE, SelectedListenerFailureBehavior.ACCEPT, ImmutableList.of("h2"));

    static public void main(String[] args) throws Exception {
        Boolean security = Boolean.FALSE;
        ObjectMapper objectMapper = new ObjectMapper();
        DbServer dbServer = new DbServer();
        String clusterInfo = System.getenv("FDB_CLUSTER");
        if (clusterInfo != null) {
            File file = new File("cluster_info");
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(clusterInfo);
            fileWriter.flush();
            fileWriter.close();
            dbServer.start(file.getAbsolutePath());
        } else {
            dbServer.start(null);
        }
        dbServer.list();
        dbServer.test();
        // //hT4GheYX:LK4nCVQb@127.0.0.1:4500
        InputStream stream = ServerStart.class.getClassLoader().getResourceAsStream("test.protoset");
        ProtoService protoService = new ProtoService(stream);
        get("/tx/:id", new GetOneBy(protoService, dbServer), new DynamicMessageResponseTransformer());
        get("/tx", new RangeQuery(protoService, dbServer));
        get("/import", new Import(protoService, dbServer));
        before((request, response) -> {
            response.type("application/json"); // default response type json
        });
        if (security) {
            before(new SecurityFilter()); // if enable will check oauth2 header from dashur and rate limit check
            before(new RateLimitFilter());
            // RateLimiter
        }
        exception(
            HttpException.class,
            (e, request, response) -> {
                response.status(e.getCode());
                try {
                    String res =
                        objectMapper.writeValueAsString(ErrorModel.builder().code(e.getCode()).message(e.getBody())
                            .build());
                    response.body(res);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            });
        RpcServer rpcServer = new RpcServer(dbServer, protoService);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                dbServer.shutdown();
                rpcServer.stop();
            }
        });
        rpcServer.start();
    }
}