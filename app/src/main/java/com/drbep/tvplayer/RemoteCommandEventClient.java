package com.drbep.tvplayer;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/** Receives device commands immediately while the existing heartbeat remains a fallback. */
final class RemoteCommandEventClient {
    private static final String TAG = "RemoteCommandEvents";
    private final CatalogSnapshotStore snapshotStore;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile HttpURLConnection connection;
    private volatile Thread worker;

    interface Listener {
        void onCommand(JSONObject command);
    }

    RemoteCommandEventClient(CatalogSnapshotStore snapshotStore) {
        this.snapshotStore = snapshotStore;
    }

    synchronized void start(String baseUrl, Listener listener) {
        if (running.get() || snapshotStore == null || listener == null) {
            return;
        }
        running.set(true);
        worker = new Thread(() -> runLoop(baseUrl, listener), "drbep-command-events");
        worker.setDaemon(true);
        worker.start();
    }

    synchronized void stop() {
        running.set(false);
        HttpURLConnection active = connection;
        connection = null;
        if (active != null) {
            active.disconnect();
        }
        Thread activeWorker = worker;
        worker = null;
        if (activeWorker != null) {
            activeWorker.interrupt();
        }
    }

    boolean isRunning() {
        return running.get();
    }

    private void runLoop(String baseUrl, Listener listener) {
        int attempt = 0;
        while (running.get()) {
            try {
                streamOnce(baseUrl, listener);
                attempt = 0;
            } catch (Exception e) {
                if (running.get()) {
                    Log.d(TAG, "event stream disconnected; heartbeat remains active", e);
                }
            }
            if (!running.get()) {
                break;
            }
            try {
                Thread.sleep(retryDelayMs(attempt++));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        running.set(false);
    }

    private void streamOnce(String baseUrl, Listener listener) throws Exception {
        String token = safe(snapshotStore.getAccessToken());
        String deviceId = safe(snapshotStore.getDeviceId());
        if (token.isEmpty() || deviceId.isEmpty()) {
            throw new IllegalStateException("device activation is not ready");
        }
        HttpURLConnection active = (HttpURLConnection) new URL(endpoint(baseUrl)).openConnection();
        connection = active;
        active.setRequestMethod("GET");
        active.setConnectTimeout(12_000);
        active.setReadTimeout(35_000);
        active.setUseCaches(false);
        active.setRequestProperty("Accept", "text/event-stream");
        active.setRequestProperty("Authorization", "Bearer " + token);
        active.setRequestProperty("X-DRBEP-Access-Token", token);
        active.setRequestProperty("X-DRBEP-Device-Id", deviceId);
        int status = active.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("event stream HTTP " + status);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(active.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder data = new StringBuilder();
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (data.length() > 0) {
                        try {
                            listener.onCommand(new JSONObject(data.toString()));
                        } catch (Exception e) {
                            Log.d(TAG, "ignored malformed event", e);
                        }
                        data.setLength(0);
                    }
                    continue;
                }
                if (line.startsWith("data:")) {
                    if (data.length() > 0) {
                        data.append('\n');
                    }
                    data.append(line.substring(5).trim());
                }
            }
        } finally {
            if (connection == active) {
                connection = null;
            }
            active.disconnect();
        }
    }

    static String endpoint(String baseUrl) {
        return safe(baseUrl).replaceAll("/+$", "") + "/api/offline/device/events";
    }

    static long retryDelayMs(int attempt) {
        int bounded = Math.max(0, Math.min(4, attempt));
        return Math.min(15_000L, 1_000L << bounded);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
