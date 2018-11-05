package sg.com.stargazer.client.feed;

import java.io.IOException;

public class GrpcClientPartition extends BaseClientPartition {
    public GrpcClientPartition(Integer partition, ClientConfig clientConfig) {
        super(partition, clientConfig);
    }

    public TxConsumer newClient() throws IOException {
        Client client = new Client(clientConfig.getUrl());
        client.start();
        return client;
    }
}
