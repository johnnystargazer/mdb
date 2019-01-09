package sg.com.stargazer.client.feed;

import java.io.IOException;
import java.util.function.Function;

import sg.com.stargazer.client.TestClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GrpcClientPartition extends BaseClientPartition implements Function<Long, Void> {
    private TestClient testClient;

    public GrpcClientPartition(Integer partition, ClientConfig clientConfig) throws Exception {
        super(partition, clientConfig);
        this.testClient = new TestClient(clientConfig.getQueryUrl());
    }

    public TxConsumer newClient() throws IOException {
        Client client = new DataAwaredClient(clientConfig.getUrl(), testClient);
        client.setCallback(this);
        client.start();
        return client;
    }

    @Override
    public Void apply(Long t) {
        log.info("finish bactch in {} ms ", t);
        return null;
    }
}
