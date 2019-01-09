package sg.com.stargazer.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import sg.com.stargazer.res.util.Constant;

import com.dashur.mdb.Tx.Transaction;
import com.github.myzhan.locust4j.Locust;
import com.google.common.base.Stopwatch;

public class TestClient {
    private OkHttpClient client;
    private URL url;
    private Executor executor = Executors.newFixedThreadPool(10);

    public TestClient(URL url) throws Exception {
        this.url = new URL(url.getProtocol() + "://" + url.getHost() + ":4567");
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        builder.readTimeout(10, TimeUnit.SECONDS);
        builder.connectTimeout(5, TimeUnit.SECONDS);
        builder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Stopwatch stopwatch = Stopwatch.createStarted();
                Request req = chain.request();
                Response res = chain.proceed(chain.request());
                long time = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);
                List<String> path = req.url().pathSegments();
                String url;
// System.out.println("" + path + " size " + path.size());
                if (path.size() > 1) {
                    url = "/tx/:tx_id";
                } else {
                    url = "/tx?company_id";
                }
                if (res.code() == 200) {
                    Locust.getInstance().recordSuccess("http", url, time, res.body().bytes().length);
                } else {
                    Locust.getInstance().recordFailure("http", url, time, "" + res.code());
                }
                return res;
            }
        });
        this.client = builder.build();
    }

    private DateTimeFormatter PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public void sendByCompany(Transaction request) throws MalformedURLException {
        ZonedDateTime time = Constant.zonedDatetime(request.getTransactionTime());
        time = time.withZoneSameInstant(Constant.ZONE_ID);
        String from = time.format(PATTERN);
        ZonedDateTime endTime = time.minusHours(1);
        String end = endTime.format(PATTERN);
        String query = "/tx?company_id=" + request.getCompanyId() + "&from=" + end + "&to=" + from;
        // System.out.println(query);
        URL reqUrl = new URL(url, query);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Response res = client.newCall(new Request.Builder().url(reqUrl).build()).execute();
                    res.body().close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void sendById(Transaction request) throws Exception {
        URL reqUrl = new URL(url, "/tx/" + request.getId());
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Response res = client.newCall(new Request.Builder().url(reqUrl).build()).execute();
                    res.body().close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
