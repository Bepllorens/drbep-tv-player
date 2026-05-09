package com.drbep.tvplayer;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class CatalogSnapshotStore {
    private static final String PREFS = "drbep_catalog_snapshot";
    private static final String PREF_SOURCE_URL = "source_url";
    private static final String PREF_ACCESS_TOKEN = "access_token";
    private static final String PREF_DEVICE_ID = "device_id";
    private static final String PREF_UPDATED_AT_MS = "updated_at_ms";
    private static final String PREF_EXPIRES_AT_MS = "expires_at_ms";
    private static final String PREF_SUBJECT = "subject";
    private static final String PREF_PERMISSIONS = "permissions";
    private static final String PREF_CHANNEL_COUNT = "channel_count";
    private static final String PREF_VOD_COUNT = "vod_count";
    private static final String SNAPSHOT_FILE = "catalog_snapshot.json";

    private final Context context;
    private final SharedPreferences prefs;
    private final HttpClient httpClient;

    CatalogSnapshotStore(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.httpClient = new HttpClient();
        ensureDeviceId();
    }

    JSONObject loadSnapshotObject() throws Exception {
        SnapshotStatus status = getStatus("");
        if (status.expired) {
            throw new IllegalStateException("catalogo local caducado");
        }
        File file = snapshotFile();
        if (!file.exists() || file.length() <= 0L) {
            throw new IllegalStateException("no hay catalogo local guardado");
        }
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream inputStream = new FileInputStream(file)) {
            int offset = 0;
            while (offset < bytes.length) {
                int read = inputStream.read(bytes, offset, bytes.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
        }
        return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
    }

    JSONObject refreshFromConfiguredUrl(String fallbackUrl) throws Exception {
        String sourceUrl = getSourceUrl(fallbackUrl);
        if (sourceUrl.isEmpty()) {
            throw new IllegalStateException("no hay URL de catalogo configurada");
        }
        HttpClient.Response response = httpClient.get(
                sourceUrl,
                10000,
                45000,
                buildSnapshotHeaders()
        );
        httpClient.requireSuccess(response, "actualizando catalogo local");
        JSONObject payload = new JSONObject(response.body == null ? "" : response.body);
        validateSnapshotForThisDevice(payload);
        saveSnapshotObject(payload, sourceUrl);
        return payload;
    }

    void saveSnapshotObject(JSONObject payload, String sourceUrl) throws Exception {
        if (payload == null) {
            throw new IllegalArgumentException("catalogo local vacio");
        }
        try (FileOutputStream outputStream = new FileOutputStream(snapshotFile(), false)) {
            outputStream.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }
        prefs.edit()
                .putString(PREF_SOURCE_URL, sourceUrl == null ? "" : sourceUrl.trim())
                .putLong(PREF_UPDATED_AT_MS, System.currentTimeMillis())
                .putLong(PREF_EXPIRES_AT_MS, parseExpiresAtMs(payload))
                .putString(PREF_SUBJECT, firstNonEmpty(payload.optString("subject", ""), payload.optString("user", ""), payload.optString("device_name", "")))
                .putString(PREF_PERMISSIONS, describePermissions(payload.optJSONObject("permissions")))
                .putInt(PREF_CHANNEL_COUNT, countCatalogRows(payload, "channels"))
                .putInt(PREF_VOD_COUNT, countCatalogRows(payload, "vod") + countCatalogRows(payload, "adult") + countCatalogRows(payload, "runtime_movies"))
                .apply();
    }

    void clear() {
        try {
            File file = snapshotFile();
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception ignored) {
        }
        prefs.edit()
                .remove(PREF_UPDATED_AT_MS)
                .remove(PREF_EXPIRES_AT_MS)
                .remove(PREF_SUBJECT)
                .remove(PREF_PERMISSIONS)
                .remove(PREF_CHANNEL_COUNT)
                .remove(PREF_VOD_COUNT)
                .apply();
    }

    String getSourceUrl(String fallbackUrl) {
        String configured = prefs.getString(PREF_SOURCE_URL, "");
        if (configured != null && !configured.trim().isEmpty()) {
            return configured.trim();
        }
        return fallbackUrl == null ? "" : fallbackUrl.trim();
    }

    void setSourceUrl(String sourceUrl) {
        prefs.edit().putString(PREF_SOURCE_URL, sourceUrl == null ? "" : sourceUrl.trim()).apply();
    }

    String getAccessToken() {
        return prefs.getString(PREF_ACCESS_TOKEN, "");
    }

    void setAccessToken(String accessToken) {
        prefs.edit().putString(PREF_ACCESS_TOKEN, accessToken == null ? "" : accessToken.trim()).apply();
    }

    String getDeviceId() {
        return prefs.getString(PREF_DEVICE_ID, "");
    }

    SnapshotStatus getStatus(String fallbackUrl) {
        File file = snapshotFile();
        long expiresAtMs = prefs.getLong(PREF_EXPIRES_AT_MS, 0L);
        boolean expired = expiresAtMs > 0L && System.currentTimeMillis() > expiresAtMs;
        return new SnapshotStatus(
                file.exists() && file.length() > 0L,
                file.exists() ? file.length() : 0L,
                prefs.getLong(PREF_UPDATED_AT_MS, 0L),
                expiresAtMs,
                expired,
                prefs.getInt(PREF_CHANNEL_COUNT, 0),
                prefs.getInt(PREF_VOD_COUNT, 0),
                getSourceUrl(fallbackUrl),
                getDeviceId(),
                prefs.getString(PREF_SUBJECT, ""),
                prefs.getString(PREF_PERMISSIONS, ""),
                !getAccessToken().trim().isEmpty()
        );
    }

    private File snapshotFile() {
        return new File(context.getFilesDir(), SNAPSHOT_FILE);
    }

    private static int countCatalogRows(JSONObject payload, String key) {
        if (payload == null || key == null) {
            return 0;
        }
        if (payload.optJSONArray(key) != null) {
            return payload.optJSONArray(key).length();
        }
        JSONObject catalog = payload.optJSONObject("catalog");
        if (catalog != null && catalog.optJSONArray(key) != null) {
            return catalog.optJSONArray(key).length();
        }
        return 0;
    }

    private Map<String, String> buildSnapshotHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        String token = getAccessToken();
        if (token != null && !token.trim().isEmpty()) {
            headers.put("Authorization", "Bearer " + token.trim());
        }
        String deviceId = getDeviceId();
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            headers.put("X-DRBEP-Device-Id", deviceId.trim());
        }
        return headers;
    }

    private void ensureDeviceId() {
        if (prefs.getString(PREF_DEVICE_ID, "").trim().isEmpty()) {
            prefs.edit().putString(PREF_DEVICE_ID, UUID.randomUUID().toString()).apply();
        }
    }

    private static long parseExpiresAtMs(JSONObject payload) {
        if (payload == null) {
            return 0L;
        }
        long expiresAt = Math.max(payload.optLong("expires_at_ms", 0L), payload.optLong("expires_at", 0L));
        if (expiresAt > 0L && expiresAt < 10_000_000_000L) {
            return expiresAt * 1000L;
        }
        return Math.max(0L, expiresAt);
    }

    private void validateSnapshotForThisDevice(JSONObject payload) {
        if (payload == null) {
            return;
        }
        String expectedDeviceId = payload.optString("device_id", "").trim();
        if (!expectedDeviceId.isEmpty() && !expectedDeviceId.equals(getDeviceId())) {
            throw new IllegalStateException("catalogo asignado a otro dispositivo");
        }
    }

    private static String describePermissions(JSONObject permissions) {
        if (permissions == null || permissions.length() == 0) {
            return "";
        }
        StringBuilder output = new StringBuilder();
        java.util.Iterator<String> iterator = permissions.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (output.length() > 0) {
                output.append(" · ");
            }
            output.append(key).append("=").append(permissions.opt(key));
        }
        return output.toString();
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    static final class SnapshotStatus {
        final boolean available;
        final long sizeBytes;
        final long updatedAtMs;
        final long expiresAtMs;
        final boolean expired;
        final int channelCount;
        final int vodCount;
        final String sourceUrl;
        final String deviceId;
        final String subject;
        final String permissions;
        final boolean hasAccessToken;

        SnapshotStatus(boolean available, long sizeBytes, long updatedAtMs, long expiresAtMs, boolean expired, int channelCount, int vodCount, String sourceUrl, String deviceId, String subject, String permissions, boolean hasAccessToken) {
            this.available = available;
            this.sizeBytes = sizeBytes;
            this.updatedAtMs = updatedAtMs;
            this.expiresAtMs = expiresAtMs;
            this.expired = expired;
            this.channelCount = channelCount;
            this.vodCount = vodCount;
            this.sourceUrl = sourceUrl == null ? "" : sourceUrl;
            this.deviceId = deviceId == null ? "" : deviceId;
            this.subject = subject == null ? "" : subject;
            this.permissions = permissions == null ? "" : permissions;
            this.hasAccessToken = hasAccessToken;
        }
    }
}
