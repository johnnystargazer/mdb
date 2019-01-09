package sg.com.stargazer.res;

import sg.com.stargazer.client.feed.PartitionedClient;

public class ServerStart {
    static public void main(String[] args) throws Exception {
        if (args.length == 0) {
            FdbTestServerStart.main(args);
        } else {
            PartitionedClient.main(args);
        }
    }
}