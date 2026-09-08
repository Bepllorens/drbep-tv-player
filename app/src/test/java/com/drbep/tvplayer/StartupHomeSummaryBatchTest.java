package com.drbep.tvplayer;

import org.junit.Test;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class StartupHomeSummaryBatchTest {
    @Test public void closingCancelsPendingWork() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        CountDownLatch gate = new CountDownLatch(1);
        Callable<Integer> read = () -> { gate.await(); return 1; };
        try {
            StartupHomeSummaryBatch<Integer,Integer> batch = new StartupHomeSummaryBatch<>(pool, read, read, read);
            batch.close();
            assertTrue(batch.vod.isCancelled());
            assertTrue(batch.completed.isCancelled());
            assertTrue(batch.scheduled.isCancelled());
        } finally { gate.countDown(); pool.shutdownNow(); }
    }

    @Test public void startsAllReadsBeforeWaiting() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        CountDownLatch started = new CountDownLatch(3);
        Callable<Integer> read = () -> {
            started.countDown();
            if (!started.await(3, TimeUnit.SECONDS)) throw new AssertionError("reads serialized");
            return 7;
        };
        try (StartupHomeSummaryBatch<Integer,Integer> batch = new StartupHomeSummaryBatch<>(pool, read, read, read)) {
            assertEquals(Integer.valueOf(7), batch.vod.get(4, TimeUnit.SECONDS));
            assertEquals(Integer.valueOf(7), batch.completed.get(4, TimeUnit.SECONDS));
            assertEquals(Integer.valueOf(7), batch.scheduled.get(4, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }

    @Test public void failedReadDoesNotDiscardOtherSummaries() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try (StartupHomeSummaryBatch<Integer,Integer> batch = new StartupHomeSummaryBatch<>(pool,
                () -> { throw new IllegalStateException("unavailable"); }, () -> 2, () -> 3)) {
            try { batch.vod.get(3, TimeUnit.SECONDS); fail("expected error"); }
            catch (ExecutionException expected) { assertTrue(expected.getCause() instanceof IllegalStateException); }
            assertEquals(Integer.valueOf(2), batch.completed.get(3, TimeUnit.SECONDS));
            assertEquals(Integer.valueOf(3), batch.scheduled.get(3, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }
}
