package com.drbep.tvplayer;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class AppUpdateManager {
    private static final String LATEST_PATH = "/api/offline/app/latest";
    private static final String APK_MIME = "application/vnd.android.package-archive";
    private static final String PUBLIC_FALLBACK_BASE_URL = "https://iptv.bepllorens.com";
    private static final String LAN_FALLBACK_BASE_URL = "http://192.168.93.223:8080";
    private static final String PAYLOAD_SOURCE_BASE_URL = "_source_base_url";

    private final Context context;
    private final HttpClient httpClient;
    private final CatalogSnapshotStore catalogSnapshotStore;

    AppUpdateManager(Context context, CatalogSnapshotStore catalogSnapshotStore) {
        this.context = context.getApplicationContext();
        this.catalogSnapshotStore = catalogSnapshotStore;
        this.httpClient = new HttpClient();
    }

    UpdateInfo fetchLatest(String baseUrl) throws Exception {
        return fetchLatest(baseUrl, BuildConfig.UPDATE_CHANNEL);
    }

    UpdateInfo fetchLatest(String baseUrl, String channel) throws Exception {
        JSONObject payload = fetchLatestPayload(baseUrl, channel);
        String sourceBaseUrl = payload.optString(PAYLOAD_SOURCE_BASE_URL, baseUrl).trim();
        List<String> changelog = new ArrayList<>();
        JSONArray array = payload.optJSONArray("changelog");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                String item = array.optString(i, "").trim();
                if (!item.isEmpty()) {
                    changelog.add(item);
                }
            }
        }
        return new UpdateInfo(
                payload.optBoolean("update_enabled", false),
                payload.optInt("version_code", 0),
                payload.optString("version_name", "").trim(),
                resolveUrl(sourceBaseUrl.isEmpty() ? baseUrl : sourceBaseUrl, payload.optString("apk_url", "").trim()),
                payload.optString("sha256", "").trim().toLowerCase(Locale.ROOT),
                payload.optBoolean("required", false),
                changelog
        );
    }

    private JSONObject fetchLatestPayload(String baseUrl, String channel) throws Exception {
        Exception firstError = null;
        String updateChannel = channel == null || channel.trim().isEmpty() ? BuildConfig.UPDATE_CHANNEL : channel.trim();
        for (String candidate : updateBaseUrlCandidates(baseUrl)) {
            try {
                String path = LATEST_PATH
                        + "?channel=" + Uri.encode(updateChannel)
                        + "&current_version_code=" + BuildConfig.VERSION_CODE;
                if (catalogSnapshotStore != null) {
                    path += "&device_id=" + Uri.encode(catalogSnapshotStore.getDeviceId());
                }
                JSONObject payload = httpClient.getJsonObject(joinUrl(candidate, path), 10000, 30000, jsonHeaders(), "buscando actualizacion");
                payload.put(PAYLOAD_SOURCE_BASE_URL, candidate);
                return payload;
            } catch (Exception e) {
                if (firstError == null) {
                    firstError = e;
                }
            }
        }
        throw firstError == null ? new IllegalStateException("no hay URL de actualizacion") : firstError;
    }

    private static List<String> updateBaseUrlCandidates(String baseUrl) {
        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, baseUrl);
        addCandidate(candidates, PUBLIC_FALLBACK_BASE_URL);
        addCandidate(candidates, LAN_FALLBACK_BASE_URL);
        return candidates;
    }

    private static void addCandidate(List<String> candidates, String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) {
            return;
        }
        for (String existing : candidates) {
            if (existing.equalsIgnoreCase(clean)) {
                return;
            }
        }
        candidates.add(clean);
    }

    File downloadApk(UpdateInfo info, Progress progress) throws Exception {
        if (info == null || info.apkUrl.isEmpty()) {
            throw new IllegalStateException("URL de APK vacia");
        }
        File dir = new File(context.getCacheDir(), "updates");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("No se pudo preparar la descarga");
        }
        File outFile = new File(dir, "drbep-tv-offline-" + info.versionCode + ".apk");
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(info.apkUrl).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(60000);
            for (Map.Entry<String, String> entry : playbackHeaders().entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("descarga APK HTTP " + code);
            }
            long total = conn.getContentLengthLong();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            long done = 0L;
            try (InputStream input = conn.getInputStream(); FileOutputStream output = new FileOutputStream(outFile, false)) {
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read == 0) {
                        continue;
                    }
                    output.write(buffer, 0, read);
                    digest.update(buffer, 0, read);
                    done += read;
                    if (progress != null) {
                        progress.onProgress(done, total);
                    }
                }
            }
            String actualSha = toHex(digest.digest());
            if (!info.sha256.isEmpty() && !info.sha256.equalsIgnoreCase(actualSha)) {
                if (!outFile.delete()) {
                    outFile.deleteOnExit();
                }
                throw new IllegalStateException("SHA-256 del APK no coincide");
            }
            return outFile;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    void installApk(File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            throw new IllegalStateException("APK descargado no encontrado");
        }
        InstallPreflight preflight = checkInstallPreflight(apkFile);
        if (!preflight.ok) {
            throw new IllegalStateException(preflight.message);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.getPackageManager().canRequestPackageInstalls()) {
            Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:" + context.getPackageName()))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(settingsIntent);
            return;
        }
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", apkFile);
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_MIME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent fallback = new Intent(Intent.ACTION_INSTALL_PACKAGE)
                    .setData(uri)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(fallback);
        }
    }

    InstallPreflight checkInstallPreflight(File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            return new InstallPreflight(false, "APK descargado no encontrado", "", 0, "", 0);
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo archiveInfo = packageManager.getPackageArchiveInfo(apkFile.getAbsolutePath(), PackageManager.GET_SIGNATURES);
            if (archiveInfo == null) {
                return new InstallPreflight(false, "Android no puede leer el APK descargado", "", 0, "", 0);
            }
            PackageInfo installedInfo = packageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            String archivePackage = archiveInfo.packageName == null ? "" : archiveInfo.packageName;
            String installedPackage = installedInfo.packageName == null ? context.getPackageName() : installedInfo.packageName;
            int archiveVersion = archiveInfo.versionCode;
            int installedVersion = installedInfo.versionCode;
            if (!installedPackage.equals(archivePackage)) {
                return new InstallPreflight(false, "APK de otro paquete: " + archivePackage, archivePackage, archiveVersion, installedPackage, installedVersion);
            }
            if (archiveVersion <= installedVersion) {
                return new InstallPreflight(false, "APK no es mas nuevo: " + archiveVersion + " <= " + installedVersion, archivePackage, archiveVersion, installedPackage, installedVersion);
            }
            Set<String> installedHashes = signatureHashes(installedInfo);
            Set<String> archiveHashes = signatureHashes(archiveInfo);
            if (installedHashes.isEmpty() || archiveHashes.isEmpty()) {
                return new InstallPreflight(false, "No se pudo validar la firma del APK", archivePackage, archiveVersion, installedPackage, installedVersion);
            }
            if (!installedHashes.equals(archiveHashes)) {
                return new InstallPreflight(false, "Firma offline no valida: esta instalacion no puede actualizarse encima. Requiere reinstalacion limpia.", archivePackage, archiveVersion, installedPackage, installedVersion);
            }
            return new InstallPreflight(true, "preflight OK", archivePackage, archiveVersion, installedPackage, installedVersion);
        } catch (Exception e) {
            return new InstallPreflight(false, e.getMessage() == null ? "preflight de instalacion fallido" : e.getMessage(), "", 0, context.getPackageName(), 0);
        }
    }

    private static Set<String> signatureHashes(PackageInfo info) throws Exception {
        Set<String> hashes = new HashSet<>();
        if (info == null || info.signatures == null) {
            return hashes;
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (Signature signature : info.signatures) {
            if (signature == null) {
                continue;
            }
            digest.reset();
            hashes.add(toHex(digest.digest(signature.toByteArray())));
        }
        return hashes;
    }

    private Map<String, String> playbackHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "*/*");
        if (catalogSnapshotStore != null) {
            String token = catalogSnapshotStore.getAccessToken();
            if (token != null && !token.trim().isEmpty()) {
                headers.put("Authorization", "Bearer " + token.trim());
                headers.put("X-DRBEP-Access-Token", token.trim());
            }
            String deviceId = catalogSnapshotStore.getDeviceId();
            if (deviceId != null && !deviceId.trim().isEmpty()) {
                headers.put("X-DRBEP-Device-Id", deviceId.trim());
            }
        }
        return headers;
    }

    private static Map<String, String> jsonHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        return headers;
    }

    private static String joinUrl(String baseUrl, String path) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    private static String resolveUrl(String baseUrl, String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("/")) {
            return joinUrl(baseUrl, trimmed);
        }
        return trimmed;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            out.append(String.format(Locale.ROOT, "%02x", b));
        }
        return out.toString();
    }

    interface Progress {
        void onProgress(long bytesRead, long totalBytes);
    }

    static final class InstallPreflight {
        final boolean ok;
        final String message;
        final String apkPackageName;
        final int apkVersionCode;
        final String installedPackageName;
        final int installedVersionCode;

        InstallPreflight(boolean ok, String message, String apkPackageName, int apkVersionCode, String installedPackageName, int installedVersionCode) {
            this.ok = ok;
            this.message = message == null ? "" : message;
            this.apkPackageName = apkPackageName == null ? "" : apkPackageName;
            this.apkVersionCode = apkVersionCode;
            this.installedPackageName = installedPackageName == null ? "" : installedPackageName;
            this.installedVersionCode = installedVersionCode;
        }
    }

    static final class UpdateInfo {
        final boolean updateEnabled;
        final int versionCode;
        final String versionName;
        final String apkUrl;
        final String sha256;
        final boolean required;
        final List<String> changelog;

        UpdateInfo(boolean updateEnabled, int versionCode, String versionName, String apkUrl, String sha256, boolean required, List<String> changelog) {
            this.updateEnabled = updateEnabled;
            this.versionCode = versionCode;
            this.versionName = versionName == null ? "" : versionName;
            this.apkUrl = apkUrl == null ? "" : apkUrl;
            this.sha256 = sha256 == null ? "" : sha256;
            this.required = required;
            this.changelog = changelog == null ? new ArrayList<>() : changelog;
        }

        boolean isNewerThanCurrent() {
            return updateEnabled && versionCode > BuildConfig.VERSION_CODE && !apkUrl.trim().isEmpty();
        }
    }
}
