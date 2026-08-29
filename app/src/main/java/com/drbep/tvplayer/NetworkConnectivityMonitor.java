package com.drbep.tvplayer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

final class NetworkConnectivityMonitor {
    private static final String TAG = "NetworkMonitor";

    interface Listener {
        void onNetworkStateChanged(Snapshot snapshot);
    }

    static final class Snapshot {
        final boolean available;
        final boolean validated;
        final boolean metered;
        final String transport;
        final long changedAtElapsedMs;

        Snapshot(boolean available, boolean validated, boolean metered, String transport, long changedAtElapsedMs) {
            this.available = available;
            this.validated = validated;
            this.metered = metered;
            this.transport = transport == null || transport.trim().isEmpty() ? "desconocida" : transport.trim();
            this.changedAtElapsedMs = Math.max(0L, changedAtElapsedMs);
        }

        static Snapshot unavailable(long nowElapsedMs) {
            return new Snapshot(false, false, false, "sin red", nowElapsedMs);
        }
    }

    private final ConnectivityManager connectivityManager;
    private final Handler callbackHandler;
    private final Listener listener;
    private final ConnectivityManager.NetworkCallback networkCallback;
    private boolean started;
    private Snapshot current;

    NetworkConnectivityMonitor(Context context, Handler callbackHandler, Listener listener) {
        connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        this.callbackHandler = callbackHandler;
        this.listener = listener;
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                publishCurrentSystemState();
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                publishCurrentSystemState();
            }

            @Override
            public void onLost(Network network) {
                publishCurrentSystemState();
            }
        };
    }

    void start() {
        if (started || connectivityManager == null) {
            return;
        }
        started = true;
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback);
        } catch (RuntimeException e) {
            Log.w(TAG, "network callback unavailable", e);
        }
        publishCurrentSystemState();
    }

    void stop() {
        if (!started || connectivityManager == null) {
            return;
        }
        started = false;
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } catch (RuntimeException ignored) {
        }
    }

    Snapshot current() {
        return current == null ? Snapshot.unavailable(0L) : current;
    }

    private void publishCurrentSystemState() {
        Snapshot next = readCurrentSystemState();
        callbackHandler.post(() -> publish(next));
    }

    private Snapshot readCurrentSystemState() {
        if (connectivityManager == null) {
            return Snapshot.unavailable(SystemClock.elapsedRealtime());
        }
        Network network = connectivityManager.getActiveNetwork();
        NetworkCapabilities capabilities = network == null ? null : connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return Snapshot.unavailable(SystemClock.elapsedRealtime());
        }
        boolean validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        return new Snapshot(
                true,
                validated,
                connectivityManager.isActiveNetworkMetered(),
                transportLabel(capabilities),
                SystemClock.elapsedRealtime()
        );
    }

    private void publish(Snapshot next) {
        if (!started || sameState(current, next)) {
            return;
        }
        current = next;
        Log.w(TAG, "network state available=" + next.available
                + " validated=" + next.validated
                + " metered=" + next.metered
                + " transport=" + next.transport);
        if (listener != null) {
            listener.onNetworkStateChanged(next);
        }
    }

    private static boolean sameState(Snapshot left, Snapshot right) {
        return left != null && right != null
                && left.available == right.available
                && left.validated == right.validated
                && left.metered == right.metered
                && left.transport.equals(right.transport);
    }

    private static String transportLabel(NetworkCapabilities capabilities) {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "ethernet";
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "wifi";
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "movil";
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return "vpn";
        return "otra";
    }
}
