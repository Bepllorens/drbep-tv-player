package com.drbep.tvplayer;

import java.io.InterruptedIOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import static org.junit.Assert.*;

public class RequestCancellationScopeTest {
    @Test public void cancellationUnblocksRealHttpWithoutWaitingForReadTimeout() throws Exception {
        RequestCancellationScope scope = new RequestCancellationScope();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch received = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (java.net.ServerSocket server = new java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            pool.submit(() -> {
                try (java.net.Socket socket = server.accept()) {
                    new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream())).readLine();
                    received.countDown();
                    release.await(5, TimeUnit.SECONDS);
                } catch (Exception ignored) { }
            });
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build();
            okhttp3.Call call = client.newCall(new okhttp3.Request.Builder()
                    .url("http://127.0.0.1:" + server.getLocalPort() + "/slow").build());
            Future<?> response = pool.submit(scope.wrap(() -> {
                try (RequestCancellationScope.Registration ignored = RequestCancellationScope.registerCurrent(call::cancel);
                     okhttp3.Response result = call.execute()) { return result.code(); }
            }));
            assertTrue(received.await(3, TimeUnit.SECONDS));
            scope.close();
            try { response.get(2, TimeUnit.SECONDS); fail("request did not cancel"); }
            catch (ExecutionException expected) { assertTrue(expected.getCause() instanceof java.io.IOException); }
            assertTrue(call.isCanceled());
            assertEquals(0, scope.activeRequestCount());
            client.connectionPool().evictAll();
            client.dispatcher().executorService().shutdown();
        } finally { release.countDown(); scope.close(); pool.shutdownNow(); }
    }
    @Test public void closeCancelsActiveRequestWithoutInterruptingWorker() throws Exception {
        RequestCancellationScope scope = new RequestCancellationScope();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        CountDownLatch registered = new CountDownLatch(1);
        CountDownLatch cancelled = new CountDownLatch(1);
        try {
            Future<Boolean> result = pool.submit(scope.wrap(() -> {
                try (RequestCancellationScope.Registration ignored = RequestCancellationScope.registerCurrent(cancelled::countDown)) {
                    registered.countDown();
                    return cancelled.await(2, TimeUnit.SECONDS);
                }
            }));
            assertTrue(registered.await(2, TimeUnit.SECONDS));
            scope.close();
            assertTrue(result.get(2, TimeUnit.SECONDS));
        } finally { scope.close(); pool.shutdownNow(); }
    }

    @Test public void completedAndUnrelatedRequestsAreNotCancelled() throws Exception {
        RequestCancellationScope scope = new RequestCancellationScope();
        AtomicBoolean cancelled = new AtomicBoolean();
        scope.wrap(() -> {
            RequestCancellationScope.registerCurrent(() -> cancelled.set(true)).close();
            return null;
        }).call();
        RequestCancellationScope.registerCurrent(() -> cancelled.set(true));
        scope.close();
        assertFalse(cancelled.get());
    }

    @Test public void lateRegistrationAfterCloseCannotStartRequest() throws Exception {
        RequestCancellationScope scope = new RequestCancellationScope();
        AtomicBoolean cancelled = new AtomicBoolean();
        scope.wrap(() -> {
            scope.close();
            try {
                RequestCancellationScope.registerCurrent(() -> cancelled.set(true));
                fail("closed scope accepted a request");
            } catch (InterruptedIOException expected) { assertTrue(cancelled.get()); }
            return null;
        }).call();
    }

    @Test public void closedQueuedTaskDoesNotRun() throws Exception {
        RequestCancellationScope scope = new RequestCancellationScope();
        scope.close();
        try { scope.wrap(() -> { fail("ran after close"); return null; }).call(); fail(); }
        catch (InterruptedIOException expected) { }
    }
}
