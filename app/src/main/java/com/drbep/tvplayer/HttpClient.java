package com.drbep.tvplayer;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

final class HttpClient {
    private static final String TAG = "HttpClient";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    static final int MAX_RESPONSE_BYTES = 8 * 1024 * 1024;

    static final class Response {
        final int code;
        final String body;
        final String finalUrl;

        Response(int code, String body) {
            this(code, body, "");
        }

        Response(int code, String body, String finalUrl) {
            this.code = code;
            this.body = body;
            this.finalUrl = finalUrl == null ? "" : finalUrl;
        }

        boolean isSuccessful() {
            return code >= 200 && code < 300;
        }
    }

    Response get(String url, int connectTimeoutMs, int readTimeoutMs, Map<String, String> headers) throws Exception {
        return get(url, connectTimeoutMs, readTimeoutMs, headers, MAX_RESPONSE_BYTES);
    }

    Response get(String url, int connectTimeoutMs, int readTimeoutMs, Map<String, String> headers, int maxResponseBytes) throws Exception {
        return request("GET", url, connectTimeoutMs, readTimeoutMs, headers, null, maxResponseBytes);
    }

    Response delete(String url, int connectTimeoutMs, int readTimeoutMs, Map<String, String> headers) throws Exception {
        return request("DELETE", url, connectTimeoutMs, readTimeoutMs, headers, null, MAX_RESPONSE_BYTES);
    }

    Response postJson(String url, JSONObject payload, int connectTimeoutMs, int readTimeoutMs, Map<String, String> headers) throws Exception {
        byte[] body = payload == null ? new byte[0] : payload.toString().getBytes(StandardCharsets.UTF_8);
        return request("POST", url, connectTimeoutMs, readTimeoutMs, headers, body, MAX_RESPONSE_BYTES);
    }

    Response putJson(String url, JSONObject payload, int connectTimeoutMs, int readTimeoutMs, Map<String, String> headers) throws Exception {
        byte[] body = payload == null ? new byte[0] : payload.toString().getBytes(StandardCharsets.UTF_8);
        return request("PUT", url, connectTimeoutMs, readTimeoutMs, headers, body, MAX_RESPONSE_BYTES);
    }

    JSONObject getJsonObject(String url, int connectTimeoutMs, int readTimeoutMs, Map<String, String> headers, String errorContext) throws Exception {
        return parseObject(requireSuccess(get(url, connectTimeoutMs, readTimeoutMs, headers), errorContext).body, errorContext);
    }

    JSONArray getJsonArray(String url, int connectTimeoutMs, int readTimeoutMs, Map<String, String> headers, String errorContext) throws Exception {
        return parseArray(requireSuccess(get(url, connectTimeoutMs, readTimeoutMs, headers), errorContext).body, errorContext);
    }

    Response requireSuccess(Response response, String errorContext) {
        if (response == null) {
            throw new IllegalStateException(errorContext + ": respuesta vacia");
        }
        if (!response.isSuccessful()) {
            throw new IllegalStateException(errorContext + ": HTTP " + response.code);
        }
        return response;
    }

    JSONObject parseObject(String body, String errorContext) {
        try {
            return new JSONObject(body == null ? "" : body);
        } catch (Exception e) {
            throw new IllegalStateException(errorContext + ": JSON object invalido", e);
        }
    }

    JSONArray parseArray(String body, String errorContext) {
        try {
            return new JSONArray(body == null ? "" : body);
        } catch (Exception e) {
            throw new IllegalStateException(errorContext + ": JSON array invalido", e);
        }
    }

    private Response request(String method, String url, int connectTimeoutMs, int readTimeoutMs, Map<String, String> headers, byte[] body, int maxResponseBytes) throws Exception {
        if (maxResponseBytes <= 0) {
            throw new IllegalArgumentException("limite de respuesta invalido");
        }
        OkHttpClient client = NetworkClients.withTimeouts(connectTimeoutMs, readTimeoutMs);
        Request.Builder request = new Request.Builder()
                .url(url)
                .header("Cache-Control", "no-cache");
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.header(entry.getKey(), entry.getValue());
            }
        }
        RequestBody requestBody = body == null ? null : RequestBody.create(JSON_MEDIA_TYPE, body);
        request.method(method, requestBody);
        try (okhttp3.Response response = client.newCall(request.build()).execute()) {
            int code = response.code();
            ResponseBody responseBody = response.body();
            long contentLengthLong = responseBody == null ? 0L : responseBody.contentLength();
            int contentLength = contentLengthLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) contentLengthLong;
            if (contentLength > maxResponseBytes) {
                throw new IllegalStateException("respuesta HTTP demasiado grande: " + contentLength + " bytes");
            }
            String responseText = responseBody == null ? "" : readAll(responseBody.byteStream(), maxResponseBytes);
            Log.d(TAG, method + " " + safeEndpoint(url) + " status=" + code + " responseChars=" + responseText.length());
            return new Response(code, responseText, response.request().url().toString());
        }
    }

    static String readAll(InputStream inputStream, int maxBytes) throws Exception {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("limite de respuesta invalido");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(32 * 1024, maxBytes));
        byte[] buffer = new byte[16 * 1024];
        try (InputStream stream = inputStream) {
            int read;
            while ((read = stream.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedIOException("lectura HTTP cancelada");
                }
                if (read > maxBytes - output.size()) {
                    throw new IllegalStateException("respuesta HTTP supera el limite de " + maxBytes + " bytes");
                }
                output.write(buffer, 0, read);
            }
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }

    private static String safeEndpoint(String value) {
        try {
            okhttp3.HttpUrl parsed = okhttp3.HttpUrl.get(value == null ? "" : value);
            return parsed.scheme() + "://" + parsed.host() + parsed.encodedPath();
        } catch (Exception ignored) {
            return "endpoint-invalido";
        }
    }
}
