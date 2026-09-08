package com.drbep.tvplayer;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/** Three independent summary reads; failures remain isolated by each caller. */
final class StartupHomeSummaryBatch<V, R> implements AutoCloseable {
    final Future<V> vod;
    final Future<R> completed;
    final Future<R> scheduled;

    StartupHomeSummaryBatch(ExecutorService executor, Callable<V> vodRead,
                            Callable<R> completedRead, Callable<R> scheduledRead) {
        Future<V> v = null;
        Future<R> c = null;
        Future<R> s = null;
        try {
            v = executor.submit(vodRead);
            c = executor.submit(completedRead);
            s = executor.submit(scheduledRead);
        } catch (RuntimeException failure) {
            if (v != null) v.cancel(true);
            if (c != null) c.cancel(true);
            if (s != null) s.cancel(true);
            throw failure;
        }
        vod = v;
        completed = c;
        scheduled = s;
    }

    @Override public void close() {
        if (!vod.isDone()) vod.cancel(true);
        if (!completed.isDone()) completed.cancel(true);
        if (!scheduled.isDone()) scheduled.cancel(true);
    }
}
