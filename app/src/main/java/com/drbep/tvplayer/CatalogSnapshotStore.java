package com.drbep.tvplayer;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
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
    private static final String PREF_EPG_CHANNEL_COUNT = "epg_channel_count";
    private static final String PREF_EPG_PROGRAM_COUNT = "epg_program_count";
    private static final String PREF_EPG_UNTIL_MS = "epg_until_ms";
    private static final String SNAPSHOT_FILE = "catalog_snapshot.json";
    private static final String LAST_GOOD_SNAPSHOT_FILE = "catalog_snapshot.last_good.json";
    private static final String SNAPSHOT_TMP_FILE = "catalog_snapshot.tmp.json";

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
        return readSnapshotObject(snapshotFile(), "catalogo local guardado");
    }

    JSONObject loadLastKnownGoodSnapshotObject() throws Exception {
        File lastGood = lastGoodSnapshotFile();
        if (lastGood.exists() && lastGood.length() > 0L) {
            return readSnapshotObject(lastGood, "ultimo catalogo bueno");
        }
        return readSnapshotObject(snapshotFile(), "catalogo local guardado");
    }

    boolean hasLastKnownGoodSnapshot() {
        File file = lastGoodSnapshotFile();
        return file.exists() && file.length() > 0L;
    }

    private JSONObject readSnapshotObject(File file, String label) throws Exception {
        if (!file.exists() || file.length() <= 0L) {
            throw new IllegalStateException("no hay " + label);
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
        JSONObject payload = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        validateSnapshotHasContent(payload);
        validateSnapshotForThisDevice(payload);
        return payload;
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
        validateSnapshotHasContent(payload);
        saveSnapshotObject(payload, sourceUrl);
        return payload;
    }

    JSONObject startActivation(String baseUrl, String label) throws Exception {
        String endpoint = joinUrl(baseUrl, "/api/offline/activation/start");
        JSONObject request = new JSONObject()
                .put("device_id", getDeviceId())
                .put("label", label == null || label.trim().isEmpty() ? "Fire Stick offline" : label.trim());
        HttpClient.Response response = httpClient.postJson(endpoint, request, 10000, 20000, jsonHeaders());
        httpClient.requireSuccess(response, "creando codigo de activacion");
        return new JSONObject(response.body == null ? "" : response.body);
    }

    JSONObject pollActivation(String baseUrl, String code) throws Exception {
        String cleanCode = code == null ? "" : code.replaceAll("\\D", "");
        String endpoint = joinUrl(baseUrl, "/api/offline/activation/" + cleanCode + "?device_id=" + getDeviceId());
        HttpClient.Response response = httpClient.get(endpoint, 10000, 20000, jsonHeaders());
        httpClient.requireSuccess(response, "consultando activacion");
        return new JSONObject(response.body == null ? "" : response.body);
    }

    void applyActivationPayload(JSONObject payload, String baseUrl) throws Exception {
        if (payload == null) {
            throw new IllegalArgumentException("activacion vacia");
        }
        String token = payload.optString("token", "").trim();
        String snapshotUrl = payload.optString("snapshot_url", "").trim();
        if (token.isEmpty() || snapshotUrl.isEmpty()) {
            throw new IllegalStateException("activacion sin token o URL");
        }
        setAccessToken(token);
        setSourceUrl(resolveUrl(baseUrl, snapshotUrl));
    }

    void saveSnapshotObject(JSONObject payload, String sourceUrl) throws Exception {
        if (payload == null) {
            throw new IllegalArgumentException("catalogo local vacio");
        }
        validateSnapshotForThisDevice(payload);
        validateSnapshotHasContent(payload);
        File current = snapshotFile();
        backupCurrentSnapshotIfUseful(current);
        File tmp = tmpSnapshotFile();
        try (FileOutputStream outputStream = new FileOutputStream(tmp, false)) {
            outputStream.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }
        if (!tmp.renameTo(current)) {
            try (FileOutputStream outputStream = new FileOutputStream(current, false)) {
                outputStream.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
        prefs.edit()
                .putString(PREF_SOURCE_URL, sourceUrl == null ? "" : sourceUrl.trim())
                .putLong(PREF_UPDATED_AT_MS, System.currentTimeMillis())
                .putLong(PREF_EXPIRES_AT_MS, parseExpiresAtMs(payload))
                .putString(PREF_SUBJECT, firstNonEmpty(payload.optString("subject", ""), payload.optString("user", ""), payload.optString("device_name", "")))
                .putString(PREF_PERMISSIONS, describePermissions(payload.optJSONObject("permissions")))
                .putInt(PREF_CHANNEL_COUNT, countCatalogRows(payload, "channels"))
                .putInt(PREF_VOD_COUNT, countCatalogRows(payload, "vod") + countCatalogRows(payload, "adult") + countCatalogRows(payload, "runtime_movies"))
                .putInt(PREF_EPG_CHANNEL_COUNT, countOfflineEpgChannels(payload))
                .putInt(PREF_EPG_PROGRAM_COUNT, countOfflineEpgPrograms(payload))
                .putLong(PREF_EPG_UNTIL_MS, parseOfflineEpgUntilMs(payload))
                .apply();
        backupCurrentSnapshotIfUseful(current);
    }

    void clear() {
        try {
            File file = snapshotFile();
            if (file.exists()) {
                file.delete();
            }
            File tmp = tmpSnapshotFile();
            if (tmp.exists()) {
                tmp.delete();
            }
            File lastGood = lastGoodSnapshotFile();
            if (lastGood.exists()) {
                lastGood.delete();
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
                .remove(PREF_EPG_CHANNEL_COUNT)
                .remove(PREF_EPG_PROGRAM_COUNT)
                .remove(PREF_EPG_UNTIL_MS)
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
                prefs.getInt(PREF_EPG_CHANNEL_COUNT, 0),
                prefs.getInt(PREF_EPG_PROGRAM_COUNT, 0),
                prefs.getLong(PREF_EPG_UNTIL_MS, 0L),
                getSourceUrl(fallbackUrl),
                getDeviceId(),
                prefs.getString(PREF_SUBJECT, ""),
                prefs.getString(PREF_PERMISSIONS, ""),
                !getAccessToken().trim().isEmpty(),
                hasLastKnownGoodSnapshot()
        );
    }

    private File snapshotFile() {
        return new File(context.getFilesDir(), SNAPSHOT_FILE);
    }

    private File lastGoodSnapshotFile() {
        return new File(context.getFilesDir(), LAST_GOOD_SNAPSHOT_FILE);
    }

    private File tmpSnapshotFile() {
        return new File(context.getFilesDir(), SNAPSHOT_TMP_FILE);
    }

    private void backupCurrentSnapshotIfUseful(File source) {
        if (source == null || !source.exists() || source.length() <= 0L) {
            return;
        }
        try {
            JSONObject current = readSnapshotObject(source, "catalogo actual");
            validateSnapshotHasContent(current);
            copyFile(source, lastGoodSnapshotFile());
        } catch (Exception ignored) {
            // Keep the previous last-good snapshot if the current file is damaged.
        }
    }

    private static void copyFile(File source, File target) throws Exception {
        try (FileInputStream inputStream = new FileInputStream(source);
             FileOutputStream outputStream = new FileOutputStream(target, false)) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, read);
            }
        }
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

    private static int countOfflineEpgChannels(JSONObject payload) {
        JSONObject epg = payload == null ? null : payload.optJSONObject("epg");
        if (epg == null) {
            return 0;
        }
        int declared = epg.optInt("channel_count", 0);
        if (declared > 0) {
            return declared;
        }
        JSONObject programs = epg.optJSONObject("programs");
        return programs == null ? 0 : programs.length();
    }

    private static int countOfflineEpgPrograms(JSONObject payload) {
        JSONObject epg = payload == null ? null : payload.optJSONObject("epg");
        if (epg == null) {
            return 0;
        }
        int declared = epg.optInt("program_count", 0);
        if (declared > 0) {
            return declared;
        }
        JSONObject programs = epg.optJSONObject("programs");
        if (programs == null) {
            return 0;
        }
        int total = 0;
        java.util.Iterator<String> keys = programs.keys();
        while (keys.hasNext()) {
            org.json.JSONArray arr = programs.optJSONArray(keys.next());
            if (arr != null) {
                total += arr.length();
            }
        }
        return total;
    }

    private Map<String, String> buildSnapshotHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        String token = getAccessToken();
        if (token != null && !token.trim().isEmpty()) {
            headers.put("Authorization", "Bearer " + token.trim());
            headers.put("X-DRBEP-Access-Token", token.trim());
        }
        String deviceId = getDeviceId();
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            headers.put("X-DRBEP-Device-Id", deviceId.trim());
        }
        return headers;
    }

    private static Map<String, String> jsonHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private static String joinUrl(String baseUrl, String path) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    private static String resolveUrl(String baseUrl, String maybeRelative) throws Exception {
        if (maybeRelative == null || maybeRelative.trim().isEmpty()) {
            return "";
        }
        URI uri = new URI(maybeRelative.trim());
        if (uri.isAbsolute()) {
            return uri.toString();
        }
        String base = baseUrl == null ? "" : baseUrl.trim();
        if (base.isEmpty()) {
            return maybeRelative.trim();
        }
        return new URI(base.endsWith("/") ? base : base + "/").resolve(maybeRelative.trim()).toString();
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

    private static long parseOfflineEpgUntilMs(JSONObject payload) {
        JSONObject epg = payload == null ? null : payload.optJSONObject("epg");
        if (epg == null) {
            return 0L;
        }
        long until = epg.optLong("until", 0L);
        if (until > 0L && until < 10_000_000_000L) {
            return until * 1000L;
        }
        return Math.max(0L, until);
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

    private static void validateSnapshotHasContent(JSONObject payload) {
        int live = countCatalogRows(payload, "channels");
        int vod = countCatalogRows(payload, "vod")
                + countCatalogRows(payload, "adult")
                + countCatalogRows(payload, "runtime_movies");
        if (live <= 0 && vod <= 0) {
            throw new IllegalStateException("catalogo descargado sin canales ni VOD; se conserva el ultimo catalogo bueno");
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
        final int epgChannelCount;
        final int epgProgramCount;
        final long epgUntilMs;
        final String sourceUrl;
        final String deviceId;
        final String subject;
        final String permissions;
        final boolean hasAccessToken;
        final boolean hasLastGoodBackup;

        SnapshotStatus(boolean available, long sizeBytes, long updatedAtMs, long expiresAtMs, boolean expired, int channelCount, int vodCount, String sourceUrl, String deviceId, String subject, String permissions, boolean hasAccessToken) {
            this(available, sizeBytes, updatedAtMs, expiresAtMs, expired, channelCount, vodCount, 0, 0, 0L, sourceUrl, deviceId, subject, permissions, hasAccessToken, false);
        }

        SnapshotStatus(boolean available, long sizeBytes, long updatedAtMs, long expiresAtMs, boolean expired, int channelCount, int vodCount, String sourceUrl, String deviceId, String subject, String permissions, boolean hasAccessToken, boolean hasLastGoodBackup) {
            this(available, sizeBytes, updatedAtMs, expiresAtMs, expired, channelCount, vodCount, 0, 0, 0L, sourceUrl, deviceId, subject, permissions, hasAccessToken, hasLastGoodBackup);
        }

        SnapshotStatus(boolean available, long sizeBytes, long updatedAtMs, long expiresAtMs, boolean expired, int channelCount, int vodCount, int epgChannelCount, int epgProgramCount, long epgUntilMs, String sourceUrl, String deviceId, String subject, String permissions, boolean hasAccessToken, boolean hasLastGoodBackup) {
            this.available = available;
            this.sizeBytes = sizeBytes;
            this.updatedAtMs = updatedAtMs;
            this.expiresAtMs = expiresAtMs;
            this.expired = expired;
            this.channelCount = channelCount;
            this.vodCount = vodCount;
            this.epgChannelCount = epgChannelCount;
            this.epgProgramCount = epgProgramCount;
            this.epgUntilMs = epgUntilMs;
            this.sourceUrl = sourceUrl == null ? "" : sourceUrl;
            this.deviceId = deviceId == null ? "" : deviceId;
            this.subject = subject == null ? "" : subject;
            this.permissions = permissions == null ? "" : permissions;
            this.hasAccessToken = hasAccessToken;
            this.hasLastGoodBackup = hasLastGoodBackup;
        }
    }
}
