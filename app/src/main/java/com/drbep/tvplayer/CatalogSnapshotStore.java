package com.drbep.tvplayer;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

final class CatalogSnapshotStore {
    private static final String PREFS = "drbep_catalog_snapshot";
    private static final String PREF_SOURCE_URL = "source_url";
    private static final String PREF_UPDATED_AT_MS = "updated_at_ms";
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
    }

    JSONObject loadSnapshotObject() throws Exception {
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
                Collections.singletonMap("Accept", "application/json")
        );
        httpClient.requireSuccess(response, "actualizando catalogo local");
        JSONObject payload = new JSONObject(response.body == null ? "" : response.body);
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
        prefs.edit().remove(PREF_UPDATED_AT_MS).remove(PREF_CHANNEL_COUNT).remove(PREF_VOD_COUNT).apply();
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

    SnapshotStatus getStatus(String fallbackUrl) {
        File file = snapshotFile();
        return new SnapshotStatus(
                file.exists() && file.length() > 0L,
                file.exists() ? file.length() : 0L,
                prefs.getLong(PREF_UPDATED_AT_MS, 0L),
                prefs.getInt(PREF_CHANNEL_COUNT, 0),
                prefs.getInt(PREF_VOD_COUNT, 0),
                getSourceUrl(fallbackUrl)
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

    static final class SnapshotStatus {
        final boolean available;
        final long sizeBytes;
        final long updatedAtMs;
        final int channelCount;
        final int vodCount;
        final String sourceUrl;

        SnapshotStatus(boolean available, long sizeBytes, long updatedAtMs, int channelCount, int vodCount, String sourceUrl) {
            this.available = available;
            this.sizeBytes = sizeBytes;
            this.updatedAtMs = updatedAtMs;
            this.channelCount = channelCount;
            this.vodCount = vodCount;
            this.sourceUrl = sourceUrl == null ? "" : sourceUrl;
        }
    }
}
