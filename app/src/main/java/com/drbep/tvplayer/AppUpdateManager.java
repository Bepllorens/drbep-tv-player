package com.drbep.tvplayer;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class AppUpdateManager {
    private static final String LATEST_PATH = "/api/offline/app/latest";
    private static final String APK_MIME = "application/vnd.android.package-archive";

    private final Context context;
    private final HttpClient httpClient;
    private final CatalogSnapshotStore catalogSnapshotStore;

    AppUpdateManager(Context context, CatalogSnapshotStore catalogSnapshotStore) {
        this.context = context.getApplicationContext();
        this.catalogSnapshotStore = catalogSnapshotStore;
        this.httpClient = new HttpClient();
    }

    UpdateInfo fetchLatest(String baseUrl) throws Exception {
        JSONObject payload = httpClient.getJsonObject(joinUrl(baseUrl, LATEST_PATH), 5000, 12000, jsonHeaders(), "buscando actualizacion");
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
                resolveUrl(baseUrl, payload.optString("apk_url", "").trim()),
                payload.optString("sha256", "").trim().toLowerCase(Locale.ROOT),
                payload.optBoolean("required", false),
                changelog
        );
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
