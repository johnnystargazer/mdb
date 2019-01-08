package sg.com.stargazer.res;

import static spark.Spark.get;
import sg.com.stargazer.res.rest.Async;

public class AyncServerStart {
    static public void main(String[] args) throws Exception {
        get("/aync", new Async());
    }
}