package com.drbep.tvplayer;

import android.util.Log;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;

final class NetworkClients {
    private static final int PROTECTED_HOST_CONNECT_TIMEOUT_MS = 2_500;
    private static final CloudflareRecoveryDns DNS = CloudflareRecoveryDns.createDefault();
    private static final AtomicReference<String> LAST_REPORTED_ROUTE = new AtomicReference<>("");
    private static final OkHttpClient SHARED = new OkHttpClient.Builder()
            .dns(DNS)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .eventListenerFactory(call -> new EventListener() {
                @Override
                public void connectionAcquired(Call activeCall, Connection connection) {
                    String host = activeCall.request().url().host();
                    if (!CloudflareRecoveryDns.isProtectedHost(host)) {
                        return;
                    }
                    java.net.InetAddress address = connection.route().socketAddress().getAddress();
                    String label = DNS.isRecoveryAddress(address) ? "cloudflare-healed" : "cloudflare-normal";
                    String key = label + ":" + (address == null ? "unknown" : address.getHostAddress());
                    if (LAST_REPORTED_ROUTE.getAndSet(key).equals(key)) {
                        return;
                    }
                    Log.w("BackendNetworkRoute", "host=" + host + " route=" + key);
                }
            })
            .build();

    private NetworkClients() {
    }

    static OkHttpClient shared() {
        return SHARED;
    }

    static OkHttpClient withTimeouts(int connectTimeoutMs, int readTimeoutMs) {
        return SHARED.newBuilder()
                .connectTimeout(Math.max(1, connectTimeoutMs), TimeUnit.MILLISECONDS)
                .readTimeout(Math.max(1, readTimeoutMs), TimeUnit.MILLISECONDS)
                .build();
    }

    static final class PlaybackCallFactory implements Call.Factory {
        private final AtomicInteger readTimeoutMs;
        private final int connectTimeoutMs;

        PlaybackCallFactory(int connectTimeoutMs, int initialReadTimeoutMs) {
            this.connectTimeoutMs = Math.max(1, connectTimeoutMs);
            this.readTimeoutMs = new AtomicInteger(Math.max(1, initialReadTimeoutMs));
        }

        void setReadTimeoutMs(int value) {
            readTimeoutMs.set(Math.max(1, value));
        }

        @Override
        public Call newCall(Request request) {
            int effectiveConnectTimeoutMs = CloudflareRecoveryDns.isProtectedHost(request.url().host())
                    ? Math.min(connectTimeoutMs, PROTECTED_HOST_CONNECT_TIMEOUT_MS)
                    : connectTimeoutMs;
            return NetworkClients.withTimeouts(effectiveConnectTimeoutMs, readTimeoutMs.get())
                    .newCall(request);
        }
    }
}
