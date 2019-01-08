package sg.com.stargazer.res.proto;

import java.util.ArrayList;
import java.util.List;

public class SendedContainer {
    private List<Long> txIds = new ArrayList<>(500);
    private List<String> referIds = new ArrayList<>(500);

    public void put(Long id, String ref) {
        txIds.add(id);
        referIds.add(ref);
    }
}
