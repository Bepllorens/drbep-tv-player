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

    static Call.Factory licenseCallFactory() {
        // Transport only. Media3 retains ownership of license URLs, bodies and
        // per-item authorization headers; default TLS verification is unchanged.
        return new PlaybackCallFactory(20_000, 30_000);
    }

    static OkHttpClient withTimeouts(int connectTimeoutMs, int readTimeoutMs) {
        return SHARED.newBuilder()
                .connectTimeout(Math.max(1, connectTimeoutMs), TimeUnit.MILLISECONDS)
                .readTimeout(Math.max(1, readTimeoutMs), TimeUnit.MILLISECONDS)
                .build();
    }

    static Request disneyMediaRequest(Request request) {
        String host = request.url().host();
        if (!host.equals("media.dssott.com") && !host.endsWith(".media.dssott.com")) return request;
        Request.Builder clean = request.newBuilder().removeHeader("Authorization").removeHeader("Cookie");
        for (String name : request.headers().names()) {
            if (name.regionMatches(true, 0, "X-DRBEP-", 0, 8)) clean.removeHeader(name);
        }
        return clean.build();
    }

    static final class PlaybackCallFactory implements Call.Factory {
        private final AtomicInteger readTimeoutMs;
        private final int connectTimeoutMs;
        private final boolean reuseTransport;
        private final TransportSlot[] transports = new TransportSlot[2];

        private static final class TransportSlot {
            final int connect, read;
            final OkHttpClient client;
            TransportSlot(int connect, int read, OkHttpClient client) {
                this.connect=connect; this.read=read; this.client=client;
            }
        }

        PlaybackCallFactory(int connectTimeoutMs, int initialReadTimeoutMs) {
            this(connectTimeoutMs, initialReadTimeoutMs, false);
        }

        PlaybackCallFactory(int connectTimeoutMs, int initialReadTimeoutMs, boolean reuseTransport) {
            this.reuseTransport = reuseTransport;
            this.connectTimeoutMs = Math.max(1, connectTimeoutMs);
            this.readTimeoutMs = new AtomicInteger(Math.max(1, initialReadTimeoutMs));
        }

        void setReadTimeoutMs(int value) {
            readTimeoutMs.set(Math.max(1, value));
        }

        // Slots contain transport configuration only, never requests or response bodies.
        // Media and license factories remain separate. Each route retains at most one client.
        synchronized OkHttpClient clientForRequest(Request request) {
            boolean protectedHost = CloudflareRecoveryDns.isProtectedHost(request.url().host());
            int connect = protectedHost ? Math.min(connectTimeoutMs, PROTECTED_HOST_CONNECT_TIMEOUT_MS)
                    : connectTimeoutMs;
            int read = readTimeoutMs.get();
            int index = protectedHost ? 1 : 0;
            TransportSlot slot = transports[index];
            if (reuseTransport && slot != null && slot.connect == connect && slot.read == read) {
                return slot.client;
            }
            OkHttpClient client = NetworkClients.withTimeouts(connect, read)
                    .newBuilder()
                    .addNetworkInterceptor(chain -> chain.proceed(disneyMediaRequest(chain.request())))
                    .build();
            if (reuseTransport) transports[index] = new TransportSlot(connect, read, client);
            return client;
        }

        @Override
        public Call newCall(Request request) {
            return clientForRequest(request).newCall(disneyMediaRequest(request));
        }
    }
}
