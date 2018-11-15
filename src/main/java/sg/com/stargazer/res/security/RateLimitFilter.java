package sg.com.stargazer.res.security;

import java.util.concurrent.TimeUnit;

import sg.com.stargazer.res.exception.HttpException;
import spark.Filter;
import spark.Request;
import spark.Response;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;

public class RateLimitFilter implements Filter {
    private LoadingCache<Long, RateLimiter> loadingCache = CacheBuilder.newBuilder().maximumSize(100000)
        .expireAfterAccess(10, TimeUnit.MINUTES).build(new CacheLoader<Long, RateLimiter>() {
            @Override
            public RateLimiter load(Long key) throws Exception {
                // 5 request / sec
                return RateLimiter.create(5);
            }
        });

    @Override
    public void handle(Request request, Response response) throws Exception {
        TokenUser tokenUser = request.attribute(SecurityFilter.TOKEN_USER);
        Long accountId = tokenUser.getAccountId();
        RateLimiter limit = loadingCache.get(accountId);
        boolean lock = limit.tryAcquire();
        if (!lock) {
            throw new HttpException(400, "request too frequently");
        }
    }
}
