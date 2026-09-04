package com.drbep.tvplayer;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LocalSmoothManifestServer {
    private static final String TAG = "LocalSmoothManifest";
    private static final String PLAYREADY_SYSTEM_ID = "9A04F079-9840-4286-AB92-E65BE0885F95";
    private static final String CLEARKEY_SYSTEM_ID = "E2719D58-A985-B3C9-781A-B030AF78D30E";

    private final Map<String, String> sources = new ConcurrentHashMap<>();
    private ServerSocket serverSocket;
    private int port;

    synchronized String register(String channelId, String sourceUrl) throws Exception {
        if (isBlank(channelId) || isBlank(sourceUrl)) {
            return "";
        }
        ensureStarted();
        String id = channelId.trim();
        sources.put(id, sourceUrl.trim());
        return "http://127.0.0.1:" + port + "/smooth/" + id + "/Manifest";
    }

    synchronized void close() {
        sources.clear();
        ServerSocket socket = serverSocket;
        serverSocket = null;
        port = 0;
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void ensureStarted() throws Exception {
        if (serverSocket != null && !serverSocket.isClosed()) {
            return;
        }
        serverSocket = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
        port = serverSocket.getLocalPort();
        Thread thread = new Thread(this::serveLoop, "drbep-smooth-manifest");
        thread.setDaemon(true);
        thread.start();
        Log.w(TAG, "started on 127.0.0.1:" + port);
    }

    private void serveLoop() {
        while (serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                Thread thread = new Thread(() -> handle(socket), "drbep-smooth-request");
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
            String path = parts[1];
            String channelId = extractChannelId(path);
            String sourceUrl = channelId.isEmpty() ? "" : sources.get(channelId);
            if (isBlank(sourceUrl)) {
                writeResponse(s, 404, "text/plain", "manifest not registered".getBytes(StandardCharsets.UTF_8));
                return;
            }
            ManifestFetch fetch = fetchManifest(sourceUrl);
            String patched = patchManifest(fetch.body, manifestBaseUrl(fetch.finalUrl));
            writeResponse(s, 200, "application/vnd.ms-sstr+xml", patched.getBytes(StandardCharsets.UTF_8));
            Log.w(TAG, "served channel=" + channelId + " bytes=" + patched.length());
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
        String prefix = "/smooth/";
        String suffix = "/Manifest";
        if (!clean.startsWith(prefix) || !clean.endsWith(suffix)) {
            return "";
        }
        return clean.substring(prefix.length(), clean.length() - suffix.length()).trim();
    }

    private static ManifestFetch fetchManifest(String sourceUrl) throws Exception {
        HttpClient.Response response = new HttpClient().get(
                sourceUrl,
                6000,
                15000,
                Collections.singletonMap("Accept", "application/vnd.ms-sstr+xml,text/xml,*/*")
        );
        if (!response.isSuccessful() || response.body.trim().isEmpty()) {
            throw new IllegalStateException("HTTP " + response.code + " fetching Smooth manifest");
        }
        return new ManifestFetch(response.finalUrl.isEmpty() ? sourceUrl : response.finalUrl, response.body);
    }

    static String patchManifest(String manifest, String baseUrl) {
        String patched = manifest == null ? "" : manifest;
        patched = patched
                .replaceAll("(?i)SystemID=\\\"" + PLAYREADY_SYSTEM_ID + "\\\"", "SystemID=\"" + CLEARKEY_SYSTEM_ID + "\"")
                .replaceAll("(?i)SystemID=\\\"\\{" + PLAYREADY_SYSTEM_ID + "\\}\\\"", "SystemID=\"{" + CLEARKEY_SYSTEM_ID + "}\"");
        return absolutizeFragmentUrls(patched, baseUrl);
    }

    private static String absolutizeFragmentUrls(String manifest, String baseUrl) {
        if (isBlank(manifest) || isBlank(baseUrl)) {
            return manifest;
        }
        Pattern pattern = Pattern.compile("Url=\\\"([^\\\"]+)\\\"");
        Matcher matcher = pattern.matcher(manifest);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String value = xmlUnescape(matcher.group(1));
            if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("data:")) {
                matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement("Url=\"" + xmlEscape(baseUrl + value) + "\""));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String manifestBaseUrl(String manifestUrl) {
        String value = manifestUrl == null ? "" : manifestUrl.trim();
        int index = value.lastIndexOf('/');
        if (index < 0) {
            return value;
        }
        return value.substring(0, index + 1);
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

    private static String xmlEscape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("\"", "&quot;");
    }

    private static String xmlUnescape(String value) {
        return value == null ? "" : value.replace("&amp;", "&").replace("&quot;", "\"");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
