package sg.com.stargazer.res.rest;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.AsyncContext;

import spark.Request;
import spark.Response;
import spark.Route;

public class Async implements Route {
    @Override
    public Object handle(Request request, Response response) throws Exception {
        AsyncContext aync = request.raw().startAsync();
        aync.start(new Runnable() {
            @Override
            public void run() {
                try {
                    PrintWriter writer = aync.getResponse().getWriter();
                    writer.write("----------");
                    writer.flush();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        return aync;
    }
}
