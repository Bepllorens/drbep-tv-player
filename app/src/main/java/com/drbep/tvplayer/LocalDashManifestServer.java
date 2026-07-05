package com.drbep.tvplayer;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class LocalDashManifestServer {
    private static final String TAG = "LocalDashManifest";

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private ServerSocket serverSocket;
    private int port;

    synchronized String register(String channelId, String sourceUrl, String kidHex) throws Exception {
        if (isBlank(channelId) || isBlank(sourceUrl) || isBlank(kidHex)) {
            return "";
        }
        ensureStarted();
        String id = channelId.trim();
        entries.put(id, new Entry(sourceUrl.trim(), kidHex.trim()));
        return "http://127.0.0.1:" + port + "/dash/" + id + "/manifest.mpd";
    }

    private void ensureStarted() throws Exception {
        if (serverSocket != null && !serverSocket.isClosed()) {
            return;
        }
        serverSocket = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
        port = serverSocket.getLocalPort();
        Thread thread = new Thread(this::serveLoop, "drbep-dash-manifest");
        thread.setDaemon(true);
        thread.start();
        Log.w(TAG, "started on 127.0.0.1:" + port);
    }

    private void serveLoop() {
        while (serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                Thread thread = new Thread(() -> handle(socket), "drbep-dash-request");
                thread.setDaemon(true);
                thread.start();
            } catch (Exception e) {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    Log.w(TAG, "accept failed", e);
                }
            }
        }
    }

    private void handle(Socket socket) {
        try (Socket s = socket) {
            s.setSoTimeout(8000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII));
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.trim().isEmpty()) {
                return;
            }
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                // Drain headers.
            }
            String[] parts = requestLine.split(" ");
            if (parts.length < 2 || !"GET".equals(parts[0])) {
                writeResponse(s, 405, "text/plain", "method not allowed".getBytes(StandardCharsets.UTF_8));
                return;
            }
            String channelId = extractChannelId(parts[1]);
            Entry entry = channelId.isEmpty() ? null : entries.get(channelId);
            if (entry == null) {
                writeResponse(s, 404, "text/plain", "manifest not registered".getBytes(StandardCharsets.UTF_8));
                return;
            }
            try {
                ManifestFetch fetch = fetchManifestWithRetries(entry.sourceUrl);
                String patched = PlayerController.patchDashManifestForLocalClearKey(fetch.body, fetch.finalUrl, entry.kidHex);
                entry.updateCachedManifest(patched);
                writeResponse(s, 200, "application/dash+xml", patched.getBytes(StandardCharsets.UTF_8));
                Log.w(TAG, "served channel=" + channelId + " bytes=" + patched.length());
            } catch (Exception fetchError) {
                String cached = entry.cachedManifestIfFresh();
                if (!isBlank(cached)) {
                    writeResponse(s, 200, "application/dash+xml", cached.getBytes(StandardCharsets.UTF_8));
                    Log.w(TAG, "served stale manifest channel=" + channelId + " bytes=" + cached.length(), fetchError);
                    return;
                }
                throw fetchError;
            }
        } catch (Exception e) {
            Log.w(TAG, "request failed", e);
            try {
                writeResponse(socket, 500, "text/plain", "manifest error".getBytes(StandardCharsets.UTF_8));
            } catch (Exception ignored) {
            }
        }
    }

    private static String extractChannelId(String path) {
        if (path == null) {
            return "";
        }
        String clean = path.split("\\?", 2)[0];
        String prefix = "/dash/";
        String suffix = "/manifest.mpd";
        if (!clean.startsWith(prefix) || !clean.endsWith(suffix)) {
            return "";
        }
        return clean.substring(prefix.length(), clean.length() - suffix.length()).trim();
    }

    private static ManifestFetch fetchManifestWithRetries(String sourceUrl) throws Exception {
        Exception lastError = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return fetchManifest(sourceUrl);
            } catch (Exception e) {
                lastError = e;
                if (attempt < 3) {
                    try {
                        Thread.sleep(attempt == 1 ? 250L : 700L);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw interrupted;
                    }
                }
            }
        }
        throw lastError == null ? new IllegalStateException("DASH manifest fetch failed") : lastError;
    }

    private static ManifestFetch fetchManifest(String sourceUrl) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(sourceUrl).openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/dash+xml,text/xml,*/*");
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("Accept-Language", "es-ES,es;q=0.9,en;q=0.8");
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36");
            int code = conn.getResponseCode();
            InputStream input = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String body = input == null ? "" : readAll(input);
            if (code < 200 || code >= 300 || body.trim().isEmpty()) {
                throw new IllegalStateException("HTTP " + code + " fetching DASH manifest");
            }
            return new ManifestFetch(conn.getURL().toString(), body);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void writeResponse(Socket socket, int code, String contentType, byte[] body) throws Exception {
        byte[] payload = body == null ? new byte[0] : body;
        String reason = code == 200 ? "OK" : code == 404 ? "Not Found" : code == 405 ? "Method Not Allowed" : "Error";
        String headers = "HTTP/1.1 " + code + " " + reason + "\r\n"
                + "Content-Type: " + contentType + "; charset=utf-8\r\n"
                + "Content-Length: " + payload.length + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Connection: close\r\n\r\n";
        OutputStream output = socket.getOutputStream();
        output.write(headers.getBytes(StandardCharsets.US_ASCII));
        output.write(payload);
        output.flush();
    }

    private static String readAll(InputStream inputStream) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        try (InputStream stream = inputStream) {
            int read;
            while ((read = stream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class Entry {
        private static final long CACHED_MANIFEST_TTL_MS = 5L * 60L * 1000L;

        final String sourceUrl;
        final String kidHex;
        private volatile String cachedManifest = "";
        private volatile long cachedAtMs = 0L;

        Entry(String sourceUrl, String kidHex) {
            this.sourceUrl = sourceUrl;
            this.kidHex = kidHex;
        }

        void updateCachedManifest(String manifest) {
            if (isBlank(manifest)) {
                return;
            }
            cachedManifest = manifest;
            cachedAtMs = System.currentTimeMillis();
        }

        String cachedManifestIfFresh() {
            String manifest = cachedManifest;
            if (isBlank(manifest) || cachedAtMs <= 0L || System.currentTimeMillis() - cachedAtMs > CACHED_MANIFEST_TTL_MS) {
                return "";
            }
            return manifest;
        }
    }

    private static final class ManifestFetch {
        final String finalUrl;
        final String body;

        ManifestFetch(String finalUrl, String body) {
            this.finalUrl = finalUrl;
            this.body = body;
        }
    }
}
