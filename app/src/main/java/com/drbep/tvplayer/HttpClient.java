package com.drbep.tvplayer;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class HttpClient {
    static final class Response {
        final int code;
        final String body;

        Response(int code, String body) {
            this.code = code;
            this.body = body;
        }

        boolean isSuccessful() {
            return code >= 200 && code < 300;
        }
    }

    Response get(String url, int connectTimeoutMs, int readTimeoutMs, Map<String, String> headers) throws Exception {
        return request("GET", url, connectTimeoutMs, readTimeoutMs, headers, null);
    }

    Response postJson(String url, JSONObject payload, int connectTimeoutMs, int readTimeoutMs, Map<String, String> headers) throws Exception {
        byte[] body = payload == null ? new byte[0] : payload.toString().getBytes(StandardCharsets.UTF_8);
        return request("POST", url, connectTimeoutMs, readTimeoutMs, headers, body);
    }

    private Response request(String method, String url, int connectTimeoutMs, int readTimeoutMs, Map<String, String> headers, byte[] body) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            if (body != null) {
                conn.setDoOutput(true);
                try (OutputStream outputStream = conn.getOutputStream()) {
                    outputStream.write(body);
                }
            }

            int code = conn.getResponseCode();
            InputStream inputStream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String responseBody = inputStream == null ? "" : readAll(inputStream);
            return new Response(code, responseBody);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readAll(InputStream inputStream) throws Exception {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        return output.toString();
    }
}