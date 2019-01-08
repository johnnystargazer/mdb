package sg.com.stargazer.client.feed;

import java.io.IOException;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GrpcClientPartition extends BaseClientPartition implements Function<Long, Void> {
    public GrpcClientPartition(Integer partition, ClientConfig clientConfig) {
        super(partition, clientConfig);
    }

    public TxConsumer newClient() throws IOException {
        Client client = new Client(clientConfig.getUrl());
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
