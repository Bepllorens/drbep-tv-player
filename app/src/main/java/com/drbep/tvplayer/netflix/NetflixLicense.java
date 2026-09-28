package com.drbep.tvplayer.netflix;

import android.media.MediaDrm;
import android.util.Base64;
import android.util.Log;
import org.json.*;
import java.net.URL;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;

/** A streaming content-license check using only the device's native CDM. */
final class NetflixLicense {
    private static final String TAG = "DRBEPNativeCDM";
    private static final UUID WIDEVINE = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed");
    private static final String ENDPOINT = "https://nrdp25.prod.ftl.netflix.com/nq/nrdjs/pbo_license/%5E1.0.0/router";

    static byte[] exchange(NativeMslSession transport, JSONObject masterToken,
            String identity, JSONObject auth, byte[] session, byte[] challenge, String licensePath) throws Exception {
        if (licensePath == null || licensePath.isEmpty()) throw new IllegalArgumentException("Missing license path");
        long now = System.currentTimeMillis();
        JSONObject app = new JSONObject().put("version", 2).put("url", licensePath)
                .put("id", now * 10).put("esn", identity).put("languages", new JSONArray().put("es-ES"))
                .put("echo", "sessionId").put("params", new JSONArray().put(new JSONObject()
                        .put("sessionId", Base64.encodeToString(session, Base64.NO_WRAP))
                        .put("clientTime", now / 1000).put("xid", Long.toString(now))
                        .put("challengeBase64", Base64.encodeToString(challenge, Base64.NO_WRAP))));
        long id = new java.security.SecureRandom().nextInt(Integer.MAX_VALUE - 1);
        byte[] body = MslMessages.request(transport, masterToken, identity, id,
                app.toString().getBytes(StandardCharsets.UTF_8), auth);
        HttpsURLConnection connection = (HttpsURLConnection) new URL(ENDPOINT).openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000); connection.setReadTimeout(25000);
            connection.setRequestMethod("POST"); connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Encoding", "msl_v1");
            connection.setRequestProperty("User-Agent", "DRBEP-Offline-Native/1");
            connection.setRequestProperty("X-Netflix.Client.Request.Name", "license");
            connection.setRequestProperty("X-DeviceModel", android.os.Build.MODEL);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream out = connection.getOutputStream()) { out.write(body); }
            int status = connection.getResponseCode();
            Log.i(TAG, "content_license_http=" + status);

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream input = (status == 200 ? connection.getInputStream() : connection.getErrorStream())) {
                if (input == null) throw new IOException("Empty license error");
                byte[] buffer = new byte[4096]; int n;
                while ((n = input.read(buffer)) != -1) {
                    if (bytes.size() + n > MslMessages.MAX_BYTES) throw new IOException("License response too large");
                    bytes.write(buffer, 0, n);
                }
            }
            String wire = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            if (status != 200) {
                StringBuilder topics = new StringBuilder();
                String lower = wire.toLowerCase(java.util.Locale.ROOT);
                for (String word : new String[] {"url", "path", "parse", "json", "header", "payload", "signature", "token", "expired", "invalid", "missing", "license", "challenge", "session", "compression"})
                    if (lower.contains(word)) topics.append(word).append(',');
                Log.i(TAG, "content_license_http_error_topics=" + topics);
                throw new IOException("License HTTP failure");
            }
            JSONObject first = (JSONObject) new JSONTokener(wire).nextValue();
            if (first.has("errordata")) {
                JSONObject error = new JSONObject(new String(Base64.decode(first.getString("errordata"), Base64.DEFAULT), StandardCharsets.UTF_8));
                Log.i(TAG, "content_license_msl_error=" + error.optInt("errorcode", -1));
                throw new IOException("License MSL failure");
            }
            byte[] clear = MslMessages.response(transport, wire, id + 1);
            try {
                Object result = new JSONTokener(new String(clear, StandardCharsets.UTF_8)).nextValue();
                Log.i(TAG, "content_license_response_authenticated=true");
                JSONObject license = findLicense(result, 0);
                if (license == null) {
                    java.util.regex.Matcher codes = java.util.regex.Pattern.compile("(?:playapi|nccp)-[0-9]{1,8}")
                            .matcher(String.valueOf(result));
                    while (codes.find()) Log.i(TAG, "content_license_provider_code=" + codes.group());
                    Log.i(TAG, "content_license_missing=true");
                    throw new IOException("No content license returned");
                }
                return Base64.decode(license.getString("licenseResponseBase64"), Base64.DEFAULT);
            } finally { Arrays.fill(clear, (byte) 0); }
        } finally { connection.disconnect(); Arrays.fill(body, (byte) 0); }
    }

    private static JSONObject findLicense(Object value, int depth) {
        if (depth > 8) return null;
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            if (object.has("licenseResponseBase64")) return object;
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) { JSONObject found = findLicense(object.opt(keys.next()), depth + 1); if (found != null) return found; }
        } else if (value instanceof JSONArray) {
            JSONArray list = (JSONArray) value;
            for (int i = 0; i < list.length(); i++) { JSONObject found = findLicense(list.opt(i), depth + 1); if (found != null) return found; }
        }
        return null;
    }

    static String licensePath(Object value, int depth) {
        if (depth > 4 || !(value instanceof JSONObject)) return null;
        JSONObject object = (JSONObject) value;
        JSONObject links = object.optJSONObject("links");
        JSONObject license = links == null ? null : links.optJSONObject("license");
        if (license != null) return license.optString("href", "");
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) { String found = licensePath(object.opt(keys.next()), depth + 1); if (found != null) return found; }
        return null;
    }

    static JSONObject videoTrack(Object value, int depth) {
        if (depth > 4 || !(value instanceof JSONObject)) return null;
        JSONObject object = (JSONObject) value;
        JSONArray tracks = object.optJSONArray("videoTracks");
        if (tracks != null && tracks.length() > 0) return tracks.optJSONObject(0);
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) { JSONObject found = videoTrack(object.opt(keys.next()), depth + 1); if (found != null) return found; }
        return null;
    }
}
