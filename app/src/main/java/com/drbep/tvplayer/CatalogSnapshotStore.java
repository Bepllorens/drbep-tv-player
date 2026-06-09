package com.drbep.tvplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

final class CatalogSnapshotStore {
    private static final String TAG = "CatalogSnapshotStore";
    private static final String OFFLINE_SCHEMA_V2 = "drbep-offline-catalog-v2";
    private static final String OFFLINE_SIGNATURE_ALG = "RS256";
    private static final String PREFS = "drbep_catalog_snapshot";
    private static final String PREF_SOURCE_URL = "source_url";
    private static final String PREF_ACCESS_TOKEN = "access_token";
    private static final String PREF_DEVICE_ID = "device_id";
    private static final String PREF_UPDATED_AT_MS = "updated_at_ms";
    private static final String PREF_EXPIRES_AT_MS = "expires_at_ms";
    private static final String PREF_GENERATED_AT_MS = "generated_at_ms";
    private static final String PREF_SUBJECT = "subject";
    private static final String PREF_PERMISSIONS = "permissions";
    private static final String PREF_PERMISSIONS_FINGERPRINT = "permissions_fingerprint";
    private static final String PREF_PERMISSIONS_CHANGED_AT_MS = "permissions_changed_at_ms";
    private static final String PREF_CHANNEL_COUNT = "channel_count";
    private static final String PREF_VOD_COUNT = "vod_count";
    private static final String PREF_EPG_CHANNEL_COUNT = "epg_channel_count";
    private static final String PREF_EPG_PROGRAM_COUNT = "epg_program_count";
    private static final String PREF_EPG_UNTIL_MS = "epg_until_ms";
    private static final String PREF_SCHEMA = "schema";
    private static final String PREF_SOURCE_BASE_URL = "source_base_url";
    private static final String PREF_PAYLOAD_FINGERPRINT = "payload_fingerprint";
    private static final String PREF_VERIFICATION_STATE = "verification_state";
    private static final String PREF_VERIFICATION_MESSAGE = "verification_message";
    private static final String PREF_LAST_REJECTED_AT_MS = "last_rejected_at_ms";
    private static final String PREF_LAST_REJECTED_REASON = "last_rejected_reason";
    private static final String PREF_LAST_REJECTED_PREVIOUS_CHANNELS = "last_rejected_previous_channels";
    private static final String PREF_LAST_REJECTED_CANDIDATE_CHANNELS = "last_rejected_candidate_channels";
    private static final String PREF_LAST_REJECTED_PREVIOUS_TOTAL = "last_rejected_previous_total";
    private static final String PREF_LAST_REJECTED_CANDIDATE_TOTAL = "last_rejected_candidate_total";
    private static final String SNAPSHOT_FILE = "catalog_snapshot.json";
    private static final String LAST_GOOD_SNAPSHOT_FILE = "catalog_snapshot.last_good.json";
    private static final String SNAPSHOT_TMP_FILE = "catalog_snapshot.tmp.json";
    private static final String VERIFICATION_OK = "ok";
    private static final String VERIFICATION_WARNING = "warning";
    private static final String VERIFICATION_ERROR = "error";

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
        return readSnapshotObject(snapshotFile(), "catalogo local guardado", false);
    }

    JSONObject loadLastKnownGoodSnapshotObject() throws Exception {
        File lastGood = lastGoodSnapshotFile();
        if (lastGood.exists() && lastGood.length() > 0L) {
            return readSnapshotObject(lastGood, "ultimo catalogo bueno", false);
        }
        return readSnapshotObject(snapshotFile(), "catalogo local guardado", false);
    }

    boolean hasLastKnownGoodSnapshot() {
        File file = lastGoodSnapshotFile();
        return file.exists() && file.length() > 0L;
    }

    private JSONObject readSnapshotObject(File file, String label) throws Exception {
        return readSnapshotObject(file, label, true);
    }

    private JSONObject readSnapshotObject(File file, String label, boolean verifySignature) throws Exception {
        if (!file.exists() || file.length() <= 0L) {
            throw new IllegalStateException("no hay " + label);
        }
        long startMs = System.currentTimeMillis();
        // Read directly into a StringBuilder via BufferedReader to avoid allocating
        // both a byte[] and a String simultaneously (saves ~27 MB peak on a 27 MB catalog).
        StringBuilder sb = new StringBuilder((int) file.length());
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), 65536)) {
            char[] buf = new char[8192];
            int n;
            while ((n = reader.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
        }
        long readMs = System.currentTimeMillis() - startMs;
        long parseStartMs = System.currentTimeMillis();
        JSONObject payload = new JSONObject(sb.toString());
        sb = null; // allow GC before validation
        long parseMs = System.currentTimeMillis() - parseStartMs;
        long validateStartMs = System.currentTimeMillis();
        validateSnapshotPayload(payload, verifySignature);
        long validateMs = System.currentTimeMillis() - validateStartMs;
        Log.i(TAG, "snapshot read label=" + label
                + " bytes=" + file.length()
                + " verifySignature=" + verifySignature
                + " readMs=" + readMs
                + " parseMs=" + parseMs
                + " validateMs=" + validateMs
                + " totalMs=" + (System.currentTimeMillis() - startMs));
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
        validateSnapshotPayload(payload);
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

    JSONObject reportDeviceStatus(String baseUrl, SnapshotStatus status, String event, boolean success, long durationMs, String detail) throws Exception {
        return reportDeviceStatus(baseUrl, status, event, success, durationMs, detail, null);
    }

    JSONObject reportDeviceStatus(String baseUrl, SnapshotStatus status, String event, boolean success, long durationMs, String detail, JSONObject extra) throws Exception {
        String endpoint = joinUrl(baseUrl, "/api/offline/device/status");
        JSONObject payload = new JSONObject()
                .put("device_id", getDeviceId())
                .put("event", event == null ? "" : event.trim())
                .put("success", success)
                .put("duration_ms", Math.max(0L, durationMs))
                .put("detail", detail == null ? "" : detail.trim())
                .put("package_name", context.getPackageName())
                .put("version_name", BuildConfig.VERSION_NAME)
                .put("version_code", BuildConfig.VERSION_CODE);
        if (extra != null) {
            Iterator<String> keys = extra.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                payload.put(key, extra.opt(key));
            }
        }
        if (status != null) {
            payload.put("catalog_available", status.available)
                    .put("catalog_expired", status.expired)
                    .put("channels", status.channelCount)
                    .put("vod", status.vodCount)
                    .put("epg_channels", status.epgChannelCount)
                    .put("epg_programs", status.epgProgramCount)
                    .put("updated_at_ms", status.updatedAtMs)
                    .put("generated_at_ms", status.generatedAtMs)
                    .put("expires_at_ms", status.expiresAtMs)
                    .put("schema", status.schema)
                    .put("subject", status.subject)
                    .put("permissions", status.permissions)
                    .put("payload_fingerprint", status.payloadFingerprint)
                    .put("permissions_fingerprint", status.permissionsFingerprint)
                    .put("verification_state", status.verificationState)
                    .put("verification_message", status.verificationMessage)
                    .put("source_base_url", status.sourceBaseUrl);
        }
        HttpClient.Response response = httpClient.postJson(endpoint, payload, 5000, 10000, buildSnapshotHeaders());
        httpClient.requireSuccess(response, "enviando estado offline");
        try {
            return response.body != null && !response.body.trim().isEmpty() ? new JSONObject(response.body) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    void reportPlaybackHeartbeat(String baseUrl, JSONObject payload) throws Exception {
        if (payload == null) {
            throw new IllegalArgumentException("heartbeat vacio");
        }
        String endpoint = joinUrl(baseUrl, "/api/playback/heartbeat");
        payload.put("device_id", getDeviceId())
                .put("version_name", BuildConfig.VERSION_NAME)
                .put("version_code", BuildConfig.VERSION_CODE);
        HttpClient.Response response = httpClient.postJson(endpoint, payload, 5000, 10000, buildSnapshotHeaders());
        httpClient.requireSuccess(response, "enviando heartbeat de reproduccion");
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
        validateSnapshotPayload(payload);
        File current = snapshotFile();
        validateSnapshotDoesNotRegress(payload, current);
        backupCurrentSnapshotIfUseful(current);
        File tmp = tmpSnapshotFile();
        String schema = normalizeSchema(payload.optString("schema", ""));
        String payloadFingerprint = buildPayloadFingerprint(payload);
        String permissionsFingerprint = buildPermissionsFingerprint(payload.optJSONObject("permissions"));
        String previousPermissionsFingerprint = prefs.getString(PREF_PERMISSIONS_FINGERPRINT, "");
        long permissionsChangedAtMs = prefs.getLong(PREF_PERMISSIONS_CHANGED_AT_MS, 0L);
        if (!permissionsFingerprint.isEmpty()
                && !previousPermissionsFingerprint.trim().isEmpty()
                && !permissionsFingerprint.equals(previousPermissionsFingerprint.trim())) {
            permissionsChangedAtMs = System.currentTimeMillis();
        } else if (previousPermissionsFingerprint.trim().isEmpty() && !permissionsFingerprint.isEmpty()) {
            permissionsChangedAtMs = 0L;
        }
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
                .putLong(PREF_GENERATED_AT_MS, parseGeneratedAtMs(payload))
                .putString(PREF_SUBJECT, firstNonEmpty(payload.optString("subject", ""), payload.optString("user", ""), payload.optString("device_name", "")))
                .putString(PREF_PERMISSIONS, describePermissions(payload.optJSONObject("permissions")))
                .putString(PREF_PERMISSIONS_FINGERPRINT, permissionsFingerprint)
                .putLong(PREF_PERMISSIONS_CHANGED_AT_MS, permissionsChangedAtMs)
                .putInt(PREF_CHANNEL_COUNT, countCatalogRows(payload, "channels"))
                .putInt(PREF_VOD_COUNT, countCatalogRows(payload, "vod") + countCatalogRows(payload, "adult") + countCatalogRows(payload, "runtime_movies"))
                .putInt(PREF_EPG_CHANNEL_COUNT, countOfflineEpgChannels(payload))
                .putInt(PREF_EPG_PROGRAM_COUNT, countOfflineEpgPrograms(payload))
                .putLong(PREF_EPG_UNTIL_MS, parseOfflineEpgUntilMs(payload))
                .putString(PREF_SCHEMA, schema)
                .putString(PREF_SOURCE_BASE_URL, payload.optString("source_base_url", "").trim())
                .putString(PREF_PAYLOAD_FINGERPRINT, payloadFingerprint)
                .putString(PREF_VERIFICATION_STATE, VERIFICATION_OK)
                .putString(PREF_VERIFICATION_MESSAGE, "")
                .remove(PREF_LAST_REJECTED_AT_MS)
                .remove(PREF_LAST_REJECTED_REASON)
                .remove(PREF_LAST_REJECTED_PREVIOUS_CHANNELS)
                .remove(PREF_LAST_REJECTED_CANDIDATE_CHANNELS)
                .remove(PREF_LAST_REJECTED_PREVIOUS_TOTAL)
                .remove(PREF_LAST_REJECTED_CANDIDATE_TOTAL)
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
                .remove(PREF_GENERATED_AT_MS)
                .remove(PREF_SUBJECT)
                .remove(PREF_PERMISSIONS)
                .remove(PREF_PERMISSIONS_FINGERPRINT)
                .remove(PREF_PERMISSIONS_CHANGED_AT_MS)
                .remove(PREF_CHANNEL_COUNT)
                .remove(PREF_VOD_COUNT)
                .remove(PREF_EPG_CHANNEL_COUNT)
                .remove(PREF_EPG_PROGRAM_COUNT)
                .remove(PREF_EPG_UNTIL_MS)
                .remove(PREF_SCHEMA)
                .remove(PREF_SOURCE_BASE_URL)
                .remove(PREF_PAYLOAD_FINGERPRINT)
                .remove(PREF_VERIFICATION_STATE)
                .remove(PREF_VERIFICATION_MESSAGE)
                .remove(PREF_LAST_REJECTED_AT_MS)
                .remove(PREF_LAST_REJECTED_REASON)
                .remove(PREF_LAST_REJECTED_PREVIOUS_CHANNELS)
                .remove(PREF_LAST_REJECTED_CANDIDATE_CHANNELS)
                .remove(PREF_LAST_REJECTED_PREVIOUS_TOTAL)
                .remove(PREF_LAST_REJECTED_CANDIDATE_TOTAL)
                .apply();
    }

    void wipeLocalData() {
        clear();
        prefs.edit()
                .remove(PREF_SOURCE_URL)
                .remove(PREF_ACCESS_TOKEN)
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
                prefs.getLong(PREF_GENERATED_AT_MS, 0L),
                expired,
                prefs.getInt(PREF_CHANNEL_COUNT, 0),
                prefs.getInt(PREF_VOD_COUNT, 0),
                prefs.getInt(PREF_EPG_CHANNEL_COUNT, 0),
                prefs.getInt(PREF_EPG_PROGRAM_COUNT, 0),
                prefs.getLong(PREF_EPG_UNTIL_MS, 0L),
                prefs.getString(PREF_SCHEMA, ""),
                getSourceUrl(fallbackUrl),
                prefs.getString(PREF_SOURCE_BASE_URL, ""),
                getDeviceId(),
                prefs.getString(PREF_SUBJECT, ""),
                prefs.getString(PREF_PERMISSIONS, ""),
                prefs.getString(PREF_PAYLOAD_FINGERPRINT, ""),
                prefs.getString(PREF_PERMISSIONS_FINGERPRINT, ""),
                prefs.getLong(PREF_PERMISSIONS_CHANGED_AT_MS, 0L),
                prefs.getString(PREF_VERIFICATION_STATE, ""),
                prefs.getString(PREF_VERIFICATION_MESSAGE, ""),
                !getAccessToken().trim().isEmpty(),
                hasLastKnownGoodSnapshot(),
                prefs.getLong(PREF_LAST_REJECTED_AT_MS, 0L),
                prefs.getString(PREF_LAST_REJECTED_REASON, ""),
                prefs.getInt(PREF_LAST_REJECTED_PREVIOUS_CHANNELS, 0),
                prefs.getInt(PREF_LAST_REJECTED_CANDIDATE_CHANNELS, 0),
                prefs.getInt(PREF_LAST_REJECTED_PREVIOUS_TOTAL, 0),
                prefs.getInt(PREF_LAST_REJECTED_CANDIDATE_TOTAL, 0)
        );
    }

    VerificationReport verifyStoredSnapshot(String fallbackUrl) {
        SnapshotStatus status = getStatus(fallbackUrl);
        if (!status.available) {
            return new VerificationReport(false, VERIFICATION_ERROR, "no hay catalogo local", status);
        }
        try {
            JSONObject payload = readSnapshotObject(snapshotFile(), "catalogo local guardado");
            String computedPayloadFingerprint = buildPayloadFingerprint(payload);
            String computedPermissionsFingerprint = buildPermissionsFingerprint(payload.optJSONObject("permissions"));
            StringBuilder warnings = new StringBuilder();
            String schema = normalizeSchema(payload.optString("schema", ""));
            if (schema.isEmpty()) {
                appendWarning(warnings, "snapshot legado sin schema declarado");
            } else if (!OFFLINE_SCHEMA_V2.equals(schema)) {
                appendWarning(warnings, "schema no esperado: " + schema);
            }
            long expiresAtMs = parseExpiresAtMs(payload);
            if (expiresAtMs <= 0L) {
                appendWarning(warnings, "snapshot sin caducidad declarada");
            } else {
                long remainingMs = expiresAtMs - System.currentTimeMillis();
                if (remainingMs <= 0L) {
                    appendWarning(warnings, "snapshot ya caducado");
                } else if (remainingMs <= 24L * 60L * 60L * 1000L) {
                    appendWarning(warnings, "snapshot caduca pronto");
                }
            }
            if (!status.payloadFingerprint.isEmpty() && !status.payloadFingerprint.equals(computedPayloadFingerprint)) {
                appendWarning(warnings, "el fichero local no coincide con la huella guardada");
            }
            if (!status.permissionsFingerprint.isEmpty() && !status.permissionsFingerprint.equals(computedPermissionsFingerprint)) {
                appendWarning(warnings, "los permisos del fichero no coinciden con los guardados");
            }
            if (status.sourceBaseUrl.trim().isEmpty()) {
                appendWarning(warnings, "sin source_base_url declarado");
            }
            String message = warnings.toString().trim();
            if (message.isEmpty()) {
                return new VerificationReport(true, VERIFICATION_OK, "snapshot verificado correctamente", status);
            }
            return new VerificationReport(true, VERIFICATION_WARNING, message, status);
        } catch (Exception e) {
            return new VerificationReport(false, VERIFICATION_ERROR, e.getMessage(), status);
        }
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

    // Minimum file size considered a valid catalog (1 MB). A real catalog with
    // channels is always much larger; this avoids parsing the full 27 MB JSON
    // just to check it has content, which would OOM while ExoPlayer is active.
    private static final long MIN_VALID_SNAPSHOT_BYTES = 1024 * 1024L;

    private void backupCurrentSnapshotIfUseful(File source) {
        if (source == null || !source.exists() || source.length() < MIN_VALID_SNAPSHOT_BYTES) {
            return;
        }
        try {
            copyFile(source, lastGoodSnapshotFile());
        } catch (Exception ignored) {
            // Keep the previous last-good snapshot if the copy fails.
        }
    }

    private void validateSnapshotDoesNotRegress(JSONObject payload, File current) throws Exception {
        if (payload == null || current == null || !current.exists() || current.length() <= 0L) {
            return;
        }
        int previousLive = prefs.getInt(PREF_CHANNEL_COUNT, 0);
        int previousVod = prefs.getInt(PREF_VOD_COUNT, 0);
        int previousTotal = previousLive + previousVod;
        if (previousLive <= 0 && previousTotal <= 0) {
            return;
        }

        int candidateLive = countCatalogRows(payload, "channels");
        int candidateVod = countCatalogRows(payload, "vod")
                + countCatalogRows(payload, "adult")
                + countCatalogRows(payload, "runtime_movies");
        int candidateTotal = candidateLive + candidateVod;
        String previousPermissionsFingerprint = prefs.getString(PREF_PERMISSIONS_FINGERPRINT, "");
        String candidatePermissionsFingerprint = buildPermissionsFingerprint(payload.optJSONObject("permissions"));
        boolean permissionsChanged = !previousPermissionsFingerprint.trim().isEmpty()
                && !candidatePermissionsFingerprint.trim().isEmpty()
                && !previousPermissionsFingerprint.trim().equals(candidatePermissionsFingerprint.trim());
        if (permissionsChanged) {
            return;
        }

        boolean liveDrop = isSuspiciousCatalogDrop(previousLive, candidateLive, 20, 10);
        boolean totalDrop = isSuspiciousCatalogDrop(previousTotal, candidateTotal, 30, 15);
        if (!liveDrop && !totalDrop) {
            return;
        }
        String reason = "catalogo candidato reducido: canales "
                + candidateLive + "/" + previousLive
                + " · total " + candidateTotal + "/" + previousTotal
                + " · sin cambio de permisos";
        prefs.edit()
                .putLong(PREF_LAST_REJECTED_AT_MS, System.currentTimeMillis())
                .putString(PREF_LAST_REJECTED_REASON, reason)
                .putInt(PREF_LAST_REJECTED_PREVIOUS_CHANNELS, previousLive)
                .putInt(PREF_LAST_REJECTED_CANDIDATE_CHANNELS, candidateLive)
                .putInt(PREF_LAST_REJECTED_PREVIOUS_TOTAL, previousTotal)
                .putInt(PREF_LAST_REJECTED_CANDIDATE_TOTAL, candidateTotal)
                .putString(PREF_VERIFICATION_STATE, VERIFICATION_WARNING)
                .putString(PREF_VERIFICATION_MESSAGE, reason)
                .apply();
        throw new IllegalStateException(reason + "; se conserva el ultimo catalogo bueno");
    }

    static boolean isSuspiciousCatalogDrop(int previousCount, int candidateCount, int minimumPreviousCount, int minimumCandidateCount) {
        if (previousCount < minimumPreviousCount) {
            return false;
        }
        return candidateCount < Math.max(minimumCandidateCount, Math.round(previousCount * 0.65f));
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

    private static long parseGeneratedAtMs(JSONObject payload) {
        if (payload == null) {
            return 0L;
        }
        long generatedAt = Math.max(payload.optLong("generated_at_ms", 0L), payload.optLong("generated_at", 0L));
        if (generatedAt > 0L && generatedAt < 10_000_000_000L) {
            return generatedAt * 1000L;
        }
        return Math.max(0L, generatedAt);
    }

    private void validateSnapshotPayload(JSONObject payload) {
        validateSnapshotPayload(payload, true);
    }

    private void validateSnapshotPayload(JSONObject payload, boolean verifySignature) {
        validateSnapshotSchema(payload);
        if (verifySignature) {
            validateSnapshotSignature(payload);
        }
        validateSnapshotForThisDevice(payload);
        validateSnapshotHasContent(payload);
        validateSnapshotTimestamps(payload);
    }

    private static void validateSnapshotSchema(JSONObject payload) {
        String schema = normalizeSchema(payload == null ? "" : payload.optString("schema", ""));
        if (!schema.isEmpty() && !OFFLINE_SCHEMA_V2.equals(schema)) {
            throw new IllegalStateException("schema offline no compatible: " + schema);
        }
    }

    private static void validateSnapshotSignature(JSONObject payload) {
        String publicKey = BuildConfig.OFFLINE_SNAPSHOT_PUBLIC_KEY == null ? "" : BuildConfig.OFFLINE_SNAPSHOT_PUBLIC_KEY.trim();
        if (publicKey.isEmpty()) {
            return;
        }
        if (payload == null) {
            throw new IllegalStateException("snapshot firmado vacio");
        }
        JSONObject signature = payload.optJSONObject("signature");
        if (signature == null) {
            throw new IllegalStateException("snapshot sin firma");
        }
        String alg = signature.optString("alg", "").trim();
        if (!OFFLINE_SIGNATURE_ALG.equalsIgnoreCase(alg)) {
            throw new IllegalStateException("algoritmo de firma no compatible");
        }
        String expectedKeyId = BuildConfig.OFFLINE_SNAPSHOT_KEY_ID == null ? "" : BuildConfig.OFFLINE_SNAPSHOT_KEY_ID.trim();
        String keyId = signature.optString("key_id", "").trim();
        if (!expectedKeyId.isEmpty() && !expectedKeyId.equals(keyId)) {
            throw new IllegalStateException("firma emitida con otra clave");
        }
        String signatureValue = signature.optString("value", "").trim();
        if (signatureValue.isEmpty()) {
            throw new IllegalStateException("firma vacia");
        }
        try {
            byte[] rawSignature = android.util.Base64.decode(signatureValue, android.util.Base64.DEFAULT);
            PublicKey key = loadOfflineSnapshotPublicKey(publicKey);
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            updateCanonicalSnapshotPayload(verifier, payload);
            if (!verifier.verify(rawSignature)) {
                throw new IllegalStateException("firma offline no valida");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("no se pudo verificar la firma del snapshot");
        }
    }

    private static String normalizeSchema(String schema) {
        return schema == null ? "" : schema.trim();
    }

    private static void validateSnapshotTimestamps(JSONObject payload) {
        long expiresAtMs = parseExpiresAtMs(payload);
        if (expiresAtMs > 0L && System.currentTimeMillis() > expiresAtMs) {
            throw new IllegalStateException("catalogo descargado ya caducado");
        }
        long generatedAtMs = parseGeneratedAtMs(payload);
        if (generatedAtMs > 0L && expiresAtMs > 0L && generatedAtMs > expiresAtMs) {
            throw new IllegalStateException("catalogo inconsistente: generado despues de caducar");
        }
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

    private static PublicKey loadOfflineSnapshotPublicKey(String base64Key) throws Exception {
        byte[] der = android.util.Base64.decode(base64Key, android.util.Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static void updateCanonicalSnapshotPayload(Signature verifier, JSONObject payload) throws Exception {
        appendCanonicalJson(verifier, payload, true);
    }

    private static void appendCanonicalJson(Signature verifier, Object value, boolean rootObject) throws Exception {
        if (value == null || value == JSONObject.NULL) {
            updateSignature(verifier, "null");
            return;
        }
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            ArrayList<String> keys = new ArrayList<>();
            Iterator<String> iterator = object.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                if (rootObject && "signature".equals(key)) {
                    continue;
                }
                keys.add(key);
            }
            Collections.sort(keys);
            updateSignature(verifier, "{");
            for (int i = 0; i < keys.size(); i++) {
                if (i > 0) {
                    updateSignature(verifier, ",");
                }
                String key = keys.get(i);
                updateSignature(verifier, goStyleJsonQuote(key));
                updateSignature(verifier, ":");
                appendCanonicalJson(verifier, object.opt(key), false);
            }
            updateSignature(verifier, "}");
            return;
        }
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            updateSignature(verifier, "[");
            for (int i = 0; i < array.length(); i++) {
                if (i > 0) {
                    updateSignature(verifier, ",");
                }
                appendCanonicalJson(verifier, array.opt(i), false);
            }
            updateSignature(verifier, "]");
            return;
        }
        if (value instanceof Boolean) {
            updateSignature(verifier, Boolean.TRUE.equals(value) ? "true" : "false");
            return;
        }
        if (value instanceof Number) {
            updateSignature(verifier, numberToCanonicalString((Number) value));
            return;
        }
        updateSignature(verifier, goStyleJsonQuote(String.valueOf(value)));
    }

    private static void updateSignature(Signature verifier, String value) throws Exception {
        verifier.update(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String numberToCanonicalString(Number value) {
        if (value == null) {
            return "0";
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return String.valueOf(value.longValue());
        }
        if (value instanceof Float || value instanceof Double) {
            double dbl = value.doubleValue();
            if (Math.rint(dbl) == dbl) {
                return String.format(Locale.US, "%.0f", dbl);
            }
            return String.valueOf(dbl);
        }
        return String.valueOf(value);
    }

    private static String goStyleJsonQuote(String value) {
        String quoted = JSONObject.quote(value == null ? "" : value);
        quoted = quoted.replace("\\/", "/");
        quoted = quoted.replace("<", "\\u003c")
                .replace(">", "\\u003e")
                .replace("&", "\\u0026");
        quoted = quoted.replace("\u2028", "\\u2028")
                .replace("\u2029", "\\u2029");
        return quoted;
    }

    private static String buildPayloadFingerprint(JSONObject payload) throws Exception {
        return sha256Hex(payload == null ? "" : payload.toString());
    }

    private static String buildPermissionsFingerprint(JSONObject permissions) throws Exception {
        return sha256Hex(permissions == null ? "" : permissions.toString());
    }

    private static String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder(hash.length * 2);
        for (byte item : hash) {
            out.append(String.format(Locale.US, "%02x", item));
        }
        return out.toString();
    }

    private static void appendWarning(StringBuilder warnings, String message) {
        if (warnings == null || message == null || message.trim().isEmpty()) {
            return;
        }
        if (warnings.length() > 0) {
            warnings.append(" · ");
        }
        warnings.append(message.trim());
    }

    static final class SnapshotStatus {
        final boolean available;
        final long sizeBytes;
        final long updatedAtMs;
        final long expiresAtMs;
        final long generatedAtMs;
        final boolean expired;
        final int channelCount;
        final int vodCount;
        final int epgChannelCount;
        final int epgProgramCount;
        final long epgUntilMs;
        final String schema;
        final String sourceUrl;
        final String sourceBaseUrl;
        final String deviceId;
        final String subject;
        final String permissions;
        final String payloadFingerprint;
        final String permissionsFingerprint;
        final long permissionsChangedAtMs;
        final String verificationState;
        final String verificationMessage;
        final boolean hasAccessToken;
        final boolean hasLastGoodBackup;
        final long lastRejectedAtMs;
        final String lastRejectedReason;
        final int lastRejectedPreviousChannels;
        final int lastRejectedCandidateChannels;
        final int lastRejectedPreviousTotal;
        final int lastRejectedCandidateTotal;

        SnapshotStatus(boolean available, long sizeBytes, long updatedAtMs, long expiresAtMs, boolean expired, int channelCount, int vodCount, String sourceUrl, String deviceId, String subject, String permissions, boolean hasAccessToken) {
            this(available, sizeBytes, updatedAtMs, expiresAtMs, 0L, expired, channelCount, vodCount, 0, 0, 0L, "", sourceUrl, "", deviceId, subject, permissions, "", "", 0L, "", "", hasAccessToken, false);
        }

        SnapshotStatus(boolean available, long sizeBytes, long updatedAtMs, long expiresAtMs, boolean expired, int channelCount, int vodCount, String sourceUrl, String deviceId, String subject, String permissions, boolean hasAccessToken, boolean hasLastGoodBackup) {
            this(available, sizeBytes, updatedAtMs, expiresAtMs, 0L, expired, channelCount, vodCount, 0, 0, 0L, "", sourceUrl, "", deviceId, subject, permissions, "", "", 0L, "", "", hasAccessToken, hasLastGoodBackup);
        }

        SnapshotStatus(boolean available, long sizeBytes, long updatedAtMs, long expiresAtMs, long generatedAtMs, boolean expired, int channelCount, int vodCount, int epgChannelCount, int epgProgramCount, long epgUntilMs, String schema, String sourceUrl, String sourceBaseUrl, String deviceId, String subject, String permissions, String payloadFingerprint, String permissionsFingerprint, long permissionsChangedAtMs, String verificationState, String verificationMessage, boolean hasAccessToken, boolean hasLastGoodBackup) {
            this(available, sizeBytes, updatedAtMs, expiresAtMs, generatedAtMs, expired, channelCount, vodCount, epgChannelCount, epgProgramCount, epgUntilMs, schema, sourceUrl, sourceBaseUrl, deviceId, subject, permissions, payloadFingerprint, permissionsFingerprint, permissionsChangedAtMs, verificationState, verificationMessage, hasAccessToken, hasLastGoodBackup, 0L, "", 0, 0, 0, 0);
        }

        SnapshotStatus(boolean available, long sizeBytes, long updatedAtMs, long expiresAtMs, long generatedAtMs, boolean expired, int channelCount, int vodCount, int epgChannelCount, int epgProgramCount, long epgUntilMs, String schema, String sourceUrl, String sourceBaseUrl, String deviceId, String subject, String permissions, String payloadFingerprint, String permissionsFingerprint, long permissionsChangedAtMs, String verificationState, String verificationMessage, boolean hasAccessToken, boolean hasLastGoodBackup, long lastRejectedAtMs, String lastRejectedReason, int lastRejectedPreviousChannels, int lastRejectedCandidateChannels, int lastRejectedPreviousTotal, int lastRejectedCandidateTotal) {
            this.available = available;
            this.sizeBytes = sizeBytes;
            this.updatedAtMs = updatedAtMs;
            this.expiresAtMs = expiresAtMs;
            this.generatedAtMs = generatedAtMs;
            this.expired = expired;
            this.channelCount = channelCount;
            this.vodCount = vodCount;
            this.epgChannelCount = epgChannelCount;
            this.epgProgramCount = epgProgramCount;
            this.epgUntilMs = epgUntilMs;
            this.schema = schema == null ? "" : schema;
            this.sourceUrl = sourceUrl == null ? "" : sourceUrl;
            this.sourceBaseUrl = sourceBaseUrl == null ? "" : sourceBaseUrl;
            this.deviceId = deviceId == null ? "" : deviceId;
            this.subject = subject == null ? "" : subject;
            this.permissions = permissions == null ? "" : permissions;
            this.payloadFingerprint = payloadFingerprint == null ? "" : payloadFingerprint;
            this.permissionsFingerprint = permissionsFingerprint == null ? "" : permissionsFingerprint;
            this.permissionsChangedAtMs = permissionsChangedAtMs;
            this.verificationState = verificationState == null ? "" : verificationState;
            this.verificationMessage = verificationMessage == null ? "" : verificationMessage;
            this.hasAccessToken = hasAccessToken;
            this.hasLastGoodBackup = hasLastGoodBackup;
            this.lastRejectedAtMs = lastRejectedAtMs;
            this.lastRejectedReason = lastRejectedReason == null ? "" : lastRejectedReason;
            this.lastRejectedPreviousChannels = lastRejectedPreviousChannels;
            this.lastRejectedCandidateChannels = lastRejectedCandidateChannels;
            this.lastRejectedPreviousTotal = lastRejectedPreviousTotal;
            this.lastRejectedCandidateTotal = lastRejectedCandidateTotal;
        }
    }

    static final class VerificationReport {
        final boolean valid;
        final String state;
        final String message;
        final SnapshotStatus status;

        VerificationReport(boolean valid, String state, String message, SnapshotStatus status) {
            this.valid = valid;
            this.state = state == null ? "" : state;
            this.message = message == null ? "" : message;
            this.status = status;
        }
    }
}
