package com.drbep.tvplayer;

import org.junit.Test;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class PlaybackTransportReuseTest {
    private Request request(String host) {
        return new Request.Builder().url("https://" + host + "/test").build();
    }
    @Test public void reusesConfigurationButNotCallsOrRequestHeaders() {
        NetworkClients.PlaybackCallFactory f = new NetworkClients.PlaybackCallFactory(5000, 10000, true);
        Request first=request("example.test").newBuilder().header("Authorization","test-only").build();
        Request second=request("other.test");
        assertSame(f.clientForRequest(first), f.clientForRequest(second));
        Call a=f.newCall(first), b=f.newCall(second);
        assertNotSame(a,b);
        assertNull(b.request().header("Authorization"));
        a.cancel();
        assertTrue(a.isCanceled()); assertFalse(b.isCanceled());
        assertFalse(a.isExecuted()); assertFalse(b.isExecuted());
    }
    @Test public void timeoutChangeReplacesTransportWithoutChangingExistingCalls() {
        NetworkClients.PlaybackCallFactory f = new NetworkClients.PlaybackCallFactory(5000, 10000, true);
        Request r=request("example.test");
        OkHttpClient old=f.clientForRequest(r);
        f.setReadTimeoutMs(20000);
        OkHttpClient fresh=f.clientForRequest(r);
        assertNotSame(old,fresh);
        assertEquals(10000,old.readTimeoutMillis());
        assertEquals(20000,fresh.readTimeoutMillis());
        assertSame(fresh,f.clientForRequest(r));
        assertSame(old.connectionPool(),fresh.connectionPool());
        assertEquals(1,fresh.networkInterceptors().size());
        assertSame(old.eventListenerFactory(),fresh.eventListenerFactory());
        assertSame(old.dns(),fresh.dns());
        assertEquals(old.retryOnConnectionFailure(),fresh.retryOnConnectionFailure());
    }
    @Test public void protectedAndOrdinaryRoutesKeepSeparateTimeouts() {
        NetworkClients.PlaybackCallFactory f = new NetworkClients.PlaybackCallFactory(5000, 10000, true);
        Request normal=request("example.test"), protectedRoute=request("fire.tvbep.com");
        assertTrue(CloudflareRecoveryDns.isProtectedHost(protectedRoute.url().host()));
        OkHttpClient a=f.clientForRequest(normal), b=f.clientForRequest(protectedRoute);
        assertEquals(5000,a.connectTimeoutMillis()); assertEquals(2500,b.connectTimeoutMillis());
        assertNotSame(a,b);
        assertSame(a,f.clientForRequest(normal)); assertSame(b,f.clientForRequest(protectedRoute));
    }
    @Test public void unchangedDefaultDoesNotReuseTransport() {
        NetworkClients.PlaybackCallFactory f = new NetworkClients.PlaybackCallFactory(5000,10000);
        assertNotSame(f.clientForRequest(request("example.test")),f.clientForRequest(request("example.test")));
    }
    @Test public void concurrentRequestsShareOnlyTheTransport() throws Exception {
        NetworkClients.PlaybackCallFactory f = new NetworkClients.PlaybackCallFactory(5000,10000,true);
        ExecutorService executor=Executors.newFixedThreadPool(4);
        try {
            List<Future<OkHttpClient>> results=new ArrayList<>();
            for(int i=0;i<40;i++) results.add(executor.submit(()->f.clientForRequest(request("example.test"))));
            OkHttpClient expected=results.get(0).get(5,TimeUnit.SECONDS);
            for(Future<OkHttpClient> result:results) assertSame(expected,result.get(5,TimeUnit.SECONDS));
        } finally {executor.shutdownNow();}
    }
}
