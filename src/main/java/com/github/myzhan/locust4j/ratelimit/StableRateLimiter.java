package com.github.myzhan.locust4j.ratelimit;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.github.myzhan.locust4j.Log;

/**
 * This limiter distributes permits at a configurable rate. Each {@link #acquire()} blocks until a permit is available.
 *
 * @author myzhan
 * @date 2018/12/07
 */
public class StableRateLimiter extends AbstractRateLimiter {

    private final long maxThreshold;
    private final AtomicLong threshold;
    private final long period;
    private final TimeUnit unit;
    private ScheduledExecutorService updateTimer;
    private boolean stopped;

    public StableRateLimiter(long maxThreshold) {
        this(maxThreshold, 1, TimeUnit.SECONDS);
    }

    public StableRateLimiter(long maxThreshold, long period, TimeUnit unit) {
        this.maxThreshold = maxThreshold;
        this.threshold = new AtomicLong(maxThreshold);
        this.period = period;
        this.unit = unit;
        this.stopped = true;
    }

    @Override
    public void start() {
        updateTimer = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("update-timer");
                return thread;
            }
        });
        updateTimer.scheduleAtFixedRate(new RateUpdater(this, period, unit), 0, 1, TimeUnit.SECONDS);
        stopped = false;
        Log.debug(String
            .format("Task execute rate is limited to %d per %d %s", maxThreshold, period, unit.name().toLowerCase()));
    }

    @Override
    public boolean acquire() {
        long permit = this.threshold.decrementAndGet();
        if (permit < 0) {
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException ex) {
                    stopped = true;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void update() {
        threshold.set(maxThreshold);
    }

    @Override
    public void stop() {
        stopped = true;
        updateTimer.shutdownNow();
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }
}
