package com.drbep.tvplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.URI;
import java.security.KeyStore;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class CatalogSnapshotStore {
    private static final String TAG = "CatalogSnapshotStore";
    private static final String OFFLINE_SCHEMA_V2 = "drbep-offline-catalog-v2";
    private static final String OFFLINE_SIGNATURE_ALG = "RS256";
    private static final String PREFS = "drbep_catalog_snapshot";
    private static final String PREF_SOURCE_URL = "source_url";
    private static final String PREF_ACCESS_TOKEN = "access_token";
    private static final String PREF_ACCESS_TOKEN_ENCRYPTED = "access_token_encrypted_v1";
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
    private static final String PREF_CATALOG_FINGERPRINT = "catalog_fingerprint";
    private static final String PREF_LAST_STARTUP_CACHE_HIT_MS = "last_startup_cache_hit_ms";
    private static final String PREF_VERIFICATION_STATE = "verification_state";
    private static final String PREF_VERIFICATION_MESSAGE = "verification_message";
    private static final String PREF_LAST_REJECTED_AT_MS = "last_rejected_at_ms";
    private static final String PREF_LAST_REJECTED_REASON = "last_rejected_reason";
    private static final String PREF_LAST_REJECTED_PREVIOUS_CHANNELS = "last_rejected_previous_channels";
    private static final String PREF_LAST_REJECTED_CANDIDATE_CHANNELS = "last_rejected_candidate_channels";
    private static final String PREF_LAST_REJECTED_PREVIOUS_TOTAL = "last_rejected_previous_total";
    private static final String PREF_LAST_REJECTED_CANDIDATE_TOTAL = "last_rejected_candidate_total";
    private static final String PREF_FORCE_STARTUP_SNAPSHOT_REFRESH = "force_startup_snapshot_refresh";
    private static final String SNAPSHOT_FILE = "catalog_snapshot.json";
    private static final String LAST_GOOD_SNAPSHOT_FILE = "catalog_snapshot.last_good.json";
    private static final String SNAPSHOT_TMP_FILE = "catalog_snapshot.tmp.json";
    private static final String STARTUP_PARSED_CACHE_FILE = "catalog_startup_parsed.cache";
    private static final String FULL_PARSED_CACHE_FILE = "catalog_full_parsed.cache";
    private static final String STARTUP_PLAYBACK_CACHE_FILE = "catalog_startup_playback.cache";
    private static final String VOD_RESUME_ITEMS_CACHE_FILE = "vod_resume_items.cache";
    private static final String EPG_CHANNEL_CACHE_FILE = "catalog_epg_channels.cache";
    private static final int STARTUP_PARSED_CACHE_VERSION = 2;
    private static final int STARTUP_PLAYBACK_CACHE_VERSION = 1;
    private static final int EPG_CHANNEL_CACHE_VERSION = 1;
    // Bump whenever a release learns a new catalog collection. Otherwise an APK
    // upgrade can keep a valid snapshot fingerprint while reusing a parsed cache
    // produced by an older parser (for example, before Plex VOD existed).
    private static final int STARTUP_PARSED_BINARY_FORMAT_VERSION = 6;
    private static final int MAX_BINARY_CACHE_ITEMS = 1_000_000;
    private static final int MAX_BINARY_CACHE_STR_BYTES = 4 * 1024 * 1024;
    static final int MAX_SNAPSHOT_HTTP_BYTES = 24 * 1024 * 1024;
    static final long MAX_LOCAL_SNAPSHOT_BYTES = 24L * 1024L * 1024L;
    private static final long MAX_EPG_CHANNEL_CACHE_BYTES = 2L * 1024L * 1024L;
    private static final int MAX_EPG_CHANNEL_CACHE_CHANNELS = 160;
    private static final long TARGET_EPG_BACKGROUND_LOCK_WAIT_MS = 750L;
    private static final long TARGET_EPG_INTERACTIVE_LOCK_WAIT_MS = 250L;
    private static final String SNAPSHOT_ENCRYPTION_ALIAS = "drbep_catalog_snapshot_aes_v1";
    private static final byte[] SNAPSHOT_ENCRYPTED_MAGIC = "DRBEPENC1\n".getBytes(StandardCharsets.US_ASCII);
    private static final int SNAPSHOT_GCM_IV_BYTES = 12;
    private static final int SNAPSHOT_GCM_TAG_BITS = 128;
    private static final String VERIFICATION_OK = "ok";
    private static final String VERIFICATION_WARNING = "warning";
    private static final String VERIFICATION_ERROR = "error";
    private static final String LEGACY_PUBLIC_BASE_URL = "https://iptv.bepllorens.com";

    private final Context context;
    private final SharedPreferences prefs;
    private final HttpClient httpClient;
    private final ReentrantLock targetEpgReadLock = new ReentrantLock();
    private final ReentrantLock snapshotMaterializationLock = new ReentrantLock();

    CatalogSnapshotStore(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.httpClient = new HttpClient();
        ensureDeviceId();
        migrateLegacyPublicBaseUrl();
    }

    JSONObject loadSnapshotObject() throws Exception {
        SnapshotStatus status = getStatus("");
        if (status.expired) {
            throw new IllegalStateException("catalogo local caducado");
        }
        return readSnapshotObject(snapshotFile(), "catalogo local guardado", false);
    }

    Map<String, List<EpgRepository.EpgProgram>> loadEpgProgramsForChannelIds(Set<String> channelIds) throws Exception {
        Map<String, List<EpgRepository.EpgProgram>> out = new LinkedHashMap<>();
        if (channelIds == null || channelIds.isEmpty()) {
            return out;
        }
        Set<String> requestedIds = new java.util.LinkedHashSet<>();
        for (String channelId : channelIds) {
            String clean = channelId == null ? "" : channelId.trim();
            if (!clean.isEmpty()) {
                requestedIds.add(clean);
            }
        }
        if (requestedIds.isEmpty()) {
            return out;
        }
        SnapshotStatus status = getStatus("");
        String fingerprint = buildEpgCacheFingerprint(status);
        EpgChannelCache cached = readEpgChannelCache(fingerprint);
        Set<String> missingIds = new java.util.LinkedHashSet<>();
        for (String requestedId : requestedIds) {
            if (cached.rowsByChannelId.containsKey(requestedId)) {
                out.put(requestedId, copyProgramList(cached.rowsByChannelId.get(requestedId)));
            } else {
                missingIds.add(requestedId);
            }
        }
        if (missingIds.isEmpty()) {
            Log.i(TAG, "target EPG cache hit requested=" + requestedIds.size()
                    + " matched=" + out.size()
                    + " cachedChannels=" + cached.rowsByChannelId.size());
            return out;
        }
        File file = snapshotFile();
        if (!file.exists() || file.length() <= 0L) {
            throw new IllegalStateException("no hay catalogo local guardado");
        }
        long startMs = System.currentTimeMillis();
        Map<String, List<EpgRepository.EpgProgram>> loaded = new LinkedHashMap<>();
        boolean scannedAllPrograms = true;
        boolean locked = false;
        try {
            locked = targetEpgReadLock.tryLock(TARGET_EPG_BACKGROUND_LOCK_WAIT_MS, TimeUnit.MILLISECONDS);
            if (!locked) {
                Log.w(TAG, "target EPG read skipped because snapshot reader is busy requested="
                        + requestedIds.size() + " matched=" + out.size());
                return out;
            }
            try {
                try (InputStream inputStream = snapshotInputStream(file);
                     JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    try {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String name = reader.nextName();
                            if ("epg".equals(name)) {
                                readTargetEpgObject(reader, missingIds, loaded);
                            } else if ("catalog".equals(name)) {
                                readTargetCatalogObject(reader, missingIds, loaded);
                            } else {
                                reader.skipValue();
                            }
                        }
                        reader.endObject();
                    } catch (TargetEpgComplete ignored) {
                        scannedAllPrograms = false;
                        // Closing the stream is enough once every requested channel has been read.
                    }
                }
            } finally {
                targetEpgReadLock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "target EPG read interrupted; returning cached partial result requested="
                    + requestedIds.size() + " matched=" + out.size(), e);
            return out;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "target EPG read exhausted memory; returning cached partial result requested="
                    + requestedIds.size() + " matched=" + out.size(), e);
            return out;
        } catch (Exception e) {
            Log.w(TAG, "target EPG read failed; returning cached partial result requested="
                    + requestedIds.size() + " matched=" + out.size(), e);
            return out;
        }
        if (scannedAllPrograms) {
            for (String missingId : missingIds) {
                if (!loaded.containsKey(missingId)) {
                    loaded.put(missingId, new ArrayList<>());
                }
            }
        }
        for (Map.Entry<String, List<EpgRepository.EpgProgram>> entry : loaded.entrySet()) {
            out.put(entry.getKey(), copyProgramList(entry.getValue()));
        }
        if (!loaded.isEmpty()) {
            mergeEpgChannelCache(fingerprint, loaded);
        }
        Log.i(TAG, "target EPG read requested=" + requestedIds.size()
                + " matched=" + out.size()
                + " cacheHits=" + (requestedIds.size() - missingIds.size())
                + " loaded=" + loaded.size()
                + " bytes=" + file.length()
                + " totalMs=" + (System.currentTimeMillis() - startMs));
        return out;
    }

    Map<String, List<EpgRepository.EpgProgram>> loadCachedEpgProgramsForChannelIds(Set<String> channelIds) throws Exception {
        Map<String, List<EpgRepository.EpgProgram>> out = new LinkedHashMap<>();
        if (channelIds == null || channelIds.isEmpty()) {
            return out;
        }
        Set<String> requestedIds = new java.util.LinkedHashSet<>();
        for (String channelId : channelIds) {
            String clean = channelId == null ? "" : channelId.trim();
            if (!clean.isEmpty()) {
                requestedIds.add(clean);
            }
        }
        if (requestedIds.isEmpty()) {
            return out;
        }
        SnapshotStatus status = getStatus("");
        String fingerprint = buildEpgCacheFingerprint(status);
        EpgChannelCache cached = readEpgChannelCache(fingerprint);
        for (String requestedId : requestedIds) {
            if (cached.rowsByChannelId.containsKey(requestedId)) {
                out.put(requestedId, copyProgramList(cached.rowsByChannelId.get(requestedId)));
            }
        }
        Log.i(TAG, "target EPG cache-only requested=" + requestedIds.size()
                + " matched=" + out.size()
                + " cachedChannels=" + cached.rowsByChannelId.size());
        return out;
    }

    Map<String, List<EpgRepository.EpgProgram>> loadEpgProgramsForChannelIdsDirect(Set<String> channelIds) throws Exception {
        Map<String, List<EpgRepository.EpgProgram>> out = new LinkedHashMap<>();
        if (channelIds == null || channelIds.isEmpty()) {
            return out;
        }
        Set<String> requestedIds = new java.util.LinkedHashSet<>();
        for (String channelId : channelIds) {
            String clean = channelId == null ? "" : channelId.trim();
            if (!clean.isEmpty()) {
                requestedIds.add(clean);
            }
        }
        if (requestedIds.isEmpty()) {
            return out;
        }
        File file = snapshotFile();
        if (!file.exists() || file.length() <= 0L) {
            throw new IllegalStateException("no hay catalogo local guardado");
        }
        long startMs = System.currentTimeMillis();
        boolean scannedAllPrograms = true;
        boolean locked = false;
        try {
            locked = targetEpgReadLock.tryLock(TARGET_EPG_INTERACTIVE_LOCK_WAIT_MS, TimeUnit.MILLISECONDS);
            if (!locked) {
                Log.w(TAG, "target EPG direct read deferred because snapshot reader is busy requested="
                        + requestedIds.size() + " matched=" + out.size());
                return copyProgramMap(out);
            }
            try {
                try (InputStream inputStream = snapshotInputStream(file);
                     JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    try {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String name = reader.nextName();
                            if ("epg".equals(name)) {
                                readTargetEpgObject(reader, requestedIds, out);
                            } else if ("catalog".equals(name)) {
                                readTargetCatalogObject(reader, requestedIds, out);
                            } else {
                                reader.skipValue();
                            }
                        }
                        reader.endObject();
                    } catch (TargetEpgComplete ignored) {
                        scannedAllPrograms = false;
                    }
                }
            } finally {
                targetEpgReadLock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "target EPG direct read interrupted; returning partial result requested="
                    + requestedIds.size() + " matched=" + out.size(), e);
            return copyProgramMap(out);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "target EPG direct read exhausted memory; returning partial result requested="
                    + requestedIds.size() + " matched=" + out.size(), e);
            return copyProgramMap(out);
        } catch (Exception e) {
            Log.w(TAG, "target EPG direct read failed; returning partial result requested="
                    + requestedIds.size() + " matched=" + out.size(), e);
            return copyProgramMap(out);
        }
        if (scannedAllPrograms) {
            for (String requestedId : requestedIds) {
                if (!out.containsKey(requestedId)) {
                    out.put(requestedId, new ArrayList<>());
                }
            }
        }
        Log.i(TAG, "target EPG direct read requested=" + requestedIds.size()
                + " matched=" + out.size()
                + " bytes=" + file.length()
                + " totalMs=" + (System.currentTimeMillis() - startMs));
        return copyProgramMap(out);
    }

    JSONObject loadStartupSnapshotObject(String fallbackUrl) throws Exception {
        File file = snapshotFile();
        SnapshotStatus status = getStatus(fallbackUrl);
        if (file.exists() && file.length() > 0L && status != null && status.available && !status.expired) {
            prefs.edit().putLong(PREF_LAST_STARTUP_CACHE_HIT_MS, System.currentTimeMillis()).apply();
            Log.w(TAG, "startup cache-first: using locally verified unexpired snapshot bytes=" + file.length());
            return loadSnapshotObject();
        }
        if ((!file.exists() || file.length() <= 0L) && hasRefreshCredentials(fallbackUrl)) {
            Log.w(TAG, "startup cache missing; downloading live-only bootstrap snapshot");
            return refreshStartupLiveFromConfiguredUrl(fallbackUrl);
        }
        return loadSnapshotObject();
    }

    private static boolean localSnapshotNeedsVodRepair(SnapshotStatus status) {
        if (status == null || !status.available || status.expired || status.vodCount > 0) {
            return false;
        }
        String permissions = status.permissions == null ? "" : status.permissions.toLowerCase(Locale.US);
        return permissions.contains("vod=true");
    }

    JSONObject loadLastKnownGoodSnapshotObject() throws Exception {
        File lastGood = lastGoodSnapshotFile();
        if (lastGood.exists() && lastGood.length() > 0L) {
            return readSnapshotObject(lastGood, "ultimo catalogo bueno", false);
        }
        return readSnapshotObject(snapshotFile(), "catalogo local guardado", false);
    }

    CatalogLoadResult loadStartupParsedCache(String fallbackUrl) {
        File file = startupParsedCacheFile();
        if (!file.exists() || file.length() <= 0L) {
            return null;
        }
        SnapshotStatus status = getStatus(fallbackUrl);
        if (status == null || !status.available || status.expired || status.payloadFingerprint.isEmpty()) {
            return null;
        }
        try {
            CatalogLoadResult result = readStartupParsedCacheBinary(file, status, true);
            if (result == null) {
                return null;
            }
            if (result.channels == null || result.channels.isEmpty() || result.filters == null || result.filters.isEmpty()) {
                throw new IllegalStateException("cache de arranque vacia");
            }
            if (result.channels.size() > 50000) {
                prefs.edit().putBoolean(PREF_FORCE_STARTUP_SNAPSHOT_REFRESH, true).apply();
                throw new IllegalStateException("cache de arranque sobredimensionada: canales=" + result.channels.size());
            }
            prefs.edit().putLong(PREF_LAST_STARTUP_CACHE_HIT_MS, System.currentTimeMillis()).apply();
            Log.w(TAG, "startup parsed catalog cache hit channels=" + result.channels.size() + " filters=" + result.filters.size());
            return result;
        } catch (Exception | OutOfMemoryError e) {
            Log.w(TAG, "startup parsed catalog cache ignored", e);
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            return null;
        }
    }

    void saveStartupParsedCache(String fallbackUrl, CatalogLoadResult result) {
        if (result == null || result.channels == null || result.channels.isEmpty()) {
            return;
        }
        SnapshotStatus status = getStatus(fallbackUrl);
        if (status == null || !status.available || status.expired || status.payloadFingerprint.isEmpty()) {
            return;
        }
        try {
            writeStartupParsedCacheBinary(startupParsedCacheFile(), status, result);
            Log.w(TAG, "startup parsed catalog cache saved channels=" + result.channels.size() + " filters=" + (result.filters == null ? 0 : result.filters.size()));
        } catch (Exception e) {
            Log.w(TAG, "failed to save startup parsed catalog cache", e);
            //noinspection ResultOfMethodCallIgnored
            startupParsedCacheFile().delete();
        }
    }

    CatalogLoadResult loadFullParsedCache(String fallbackUrl) {
        File file = fullParsedCacheFile();
        if (!file.exists() || file.length() <= 0L) {
            return null;
        }
        SnapshotStatus status = getStatus(fallbackUrl);
        if (status == null || !status.available || status.expired || status.payloadFingerprint.isEmpty()) {
            return null;
        }
        try {
            CatalogLoadResult result = readStartupParsedCacheBinary(file, status, false);
            if (result == null || result.channels == null || result.channels.isEmpty()) {
                return null;
            }
            Log.w(TAG, "full parsed catalog cache hit channels=" + result.channels.size());
            return result.withLoadSource("full-cache");
        } catch (Exception | OutOfMemoryError e) {
            Log.w(TAG, "full parsed catalog cache ignored", e);
            deleteFileQuietly(file);
            return null;
        }
    }

    boolean hasFullParsedCache(String fallbackUrl) {
        File file = fullParsedCacheFile();
        SnapshotStatus status = getStatus(fallbackUrl);
        return file.exists()
                && file.length() > 0L
                && status != null
                && status.available
                && !status.expired
                && parsedCacheHeaderMatches(file, status);
    }

    List<ChannelItem> loadFullParsedChannelsByIds(String fallbackUrl, Set<String> channelIds) {
        List<ChannelItem> matches = new ArrayList<>();
        if (channelIds == null || channelIds.isEmpty()) {
            return matches;
        }
        Set<String> requestedIds = new java.util.LinkedHashSet<>();
        for (String rawId : channelIds) {
            String id = rawId == null ? "" : rawId.trim();
            if (!id.isEmpty()) {
                requestedIds.add(id);
            }
        }
        File file = fullParsedCacheFile();
        SnapshotStatus status = getStatus(fallbackUrl);
        if (requestedIds.isEmpty() || !file.exists() || file.length() <= 0L || status == null || status.expired) {
            return matches;
        }
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file), 1 << 16);
             GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream, 1 << 16);
             DataInputStream in = new DataInputStream(new BufferedInputStream(gzipInputStream, 1 << 16))) {
            int formatVersion = in.readInt();
            if (formatVersion != 2 && formatVersion != STARTUP_PARSED_BINARY_FORMAT_VERSION) {
                return matches;
            }
            String payloadFingerprint = readStr(in);
            String catalogFingerprint = readStr(in);
            String permissionsFingerprint = readStr(in);
            if (!parsedCacheFingerprintMatches(status, payloadFingerprint, catalogFingerprint, permissionsFingerprint)) {
                return matches;
            }
            if (formatVersion >= 3) {
                in.readBoolean();
                readStr(in);
                in.readInt();
                in.readInt();
            }
            readStr(in);
            readOfflinePermissions(in);
            int filterCount = in.readInt();
            if (filterCount < 0 || filterCount > MAX_BINARY_CACHE_ITEMS) {
                return matches;
            }
            for (int i = 0; i < filterCount; i++) {
                readFilter(in);
            }
            int channelCount = in.readInt();
            if (channelCount < 0 || channelCount > MAX_BINARY_CACHE_ITEMS) {
                return matches;
            }
            for (int i = 0; i < channelCount && matches.size() < requestedIds.size(); i++) {
                if ((i & 127) == 0) {
                    throwIfInterrupted("lectura de VOD empezados cancelada");
                }
                ChannelItem channel = readChannel(in);
                if (channel != null && channel.isVod && requestedIds.contains(channel.id)) {
                    matches.add(channel);
                }
            }
            Log.i(TAG, "target VOD cache read requested=" + requestedIds.size() + " matched=" + matches.size());
        } catch (Exception | OutOfMemoryError e) {
            Log.w(TAG, "target VOD cache read failed; returning partial result requested="
                    + requestedIds.size() + " matched=" + matches.size(), e);
        }
        return matches;
    }

    int fetchRemoteVodCount(String fallbackUrl) throws Exception {
        String metaUrl = snapshotMetaUrl(getSourceUrl(fallbackUrl));
        if (metaUrl.isEmpty()) {
            return getStatus(fallbackUrl).vodCount;
        }
        HttpClient.Response response = httpClient.get(metaUrl, 5000, 12000, buildSnapshotHeaders());
        httpClient.requireSuccess(response, "consultando total VOD");
        JSONObject payload = new JSONObject(response.body == null ? "" : response.body);
        int vodCount = Math.max(0, payload.optInt("vod_count", 0));
        int channelCount = Math.max(0, payload.optInt("channel_count", 0));
        prefs.edit().putInt(PREF_VOD_COUNT, vodCount).putInt(PREF_CHANNEL_COUNT, channelCount).apply();
        return vodCount;
    }

    private boolean parsedCacheHeaderMatches(File file, SnapshotStatus status) {
        if (file == null || status == null) {
            return false;
        }
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file), 1 << 16);
             GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream, 1 << 16);
             DataInputStream in = new DataInputStream(new BufferedInputStream(gzipInputStream, 1 << 16))) {
            int formatVersion = in.readInt();
            if (formatVersion != 2 && formatVersion != STARTUP_PARSED_BINARY_FORMAT_VERSION) {
                return false;
            }
            String payloadFingerprint = readStr(in);
            String catalogFingerprint = readStr(in);
            String permissionsFingerprint = readStr(in);
            return parsedCacheFingerprintMatches(
                    status,
                    payloadFingerprint,
                    catalogFingerprint,
                    permissionsFingerprint
            );
        } catch (Exception e) {
            return false;
        }
    }

    void saveFullParsedCache(String fallbackUrl, CatalogLoadResult result) {
        if (result == null || result.channels == null || result.channels.isEmpty() || result.liveOnly) {
            return;
        }
        SnapshotStatus status = getStatus(fallbackUrl);
        if (status == null || !status.available || status.expired || status.payloadFingerprint.isEmpty()) {
            return;
        }
        try {
            writeStartupParsedCacheBinary(fullParsedCacheFile(), status, result);
            Log.w(TAG, "full parsed catalog cache saved channels=" + result.channels.size());
        } catch (Exception e) {
            Log.w(TAG, "failed to save full parsed catalog cache", e);
            deleteFileQuietly(fullParsedCacheFile());
        }
    }

    ChannelItem loadStartupPlaybackChannel(String fallbackUrl) {
        File file = startupPlaybackCacheFile();
        if (!file.exists() || file.length() <= 0L) {
            return null;
        }
        SnapshotStatus status = getStatus(fallbackUrl);
        if (status == null || !status.available || status.expired || status.payloadFingerprint.isEmpty()) {
            return null;
        }
        try {
            Object decoded = readEncryptedObject(file);
            if (!(decoded instanceof StartupPlaybackChannelCache)) {
                throw new IllegalStateException("tipo de cache de canal invalido");
            }
            StartupPlaybackChannelCache cache = (StartupPlaybackChannelCache) decoded;
            if (!cache.matches(status)) {
                return null;
            }
            ChannelItem channel = cache.channel;
            if (channel == null || channel.id == null || channel.id.trim().isEmpty() || channel.isVod) {
                throw new IllegalStateException("cache de canal vacia");
            }
            Log.w(TAG, "startup playback channel cache hit id=" + channel.id + " name=" + channel.name);
            return channel;
        } catch (Exception e) {
            Log.w(TAG, "startup playback channel cache ignored", e);
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            return null;
        }
    }

    void saveStartupPlaybackChannel(String fallbackUrl, ChannelItem channel) {
        if (channel == null || channel.id == null || channel.id.trim().isEmpty() || channel.isVod) {
            return;
        }
        SnapshotStatus status = getStatus(fallbackUrl);
        if (status == null || !status.available || status.expired || status.payloadFingerprint.isEmpty()) {
            return;
        }
        try {
            writeEncryptedObject(startupPlaybackCacheFile(), new StartupPlaybackChannelCache(status, channel));
            Log.w(TAG, "startup playback channel cache saved id=" + channel.id + " name=" + channel.name);
        } catch (Exception e) {
            Log.w(TAG, "failed to save startup playback channel cache", e);
            //noinspection ResultOfMethodCallIgnored
            startupPlaybackCacheFile().delete();
        }
    }

    boolean hasLastKnownGoodSnapshot() {
        File file = lastGoodSnapshotFile();
        return file.exists() && file.length() > 0L;
    }

    private JSONObject readSnapshotObject(File file, String label) throws Exception {
        return readSnapshotObject(file, label, true);
    }

    private JSONObject readSnapshotObject(File file, String label, boolean verifySignature) throws Exception {
        snapshotMaterializationLock.lockInterruptibly();
        try {
            if (!file.exists() || file.length() <= 0L) {
                throw new IllegalStateException("no hay " + label);
            }
            ensureSnapshotFileWithinLimit(file);
            long startMs = System.currentTimeMillis();
            boolean encrypted = isEncryptedSnapshotFile(file);
            StringBuilder sb = readSnapshotString(file, encrypted);
            long readMs = System.currentTimeMillis() - startMs;
            long parseStartMs = System.currentTimeMillis();
            String rawJson = sb.toString();
            JSONObject payload = new JSONObject(rawJson);
            sb = null; // allow GC before validation
            long parseMs = System.currentTimeMillis() - parseStartMs;
            long validateStartMs = System.currentTimeMillis();
            validateSnapshotPayload(payload, verifySignature);
            if (!encrypted) {
                rewriteSnapshotEncrypted(file, rawJson);
            }
            rawJson = null;
            long validateMs = System.currentTimeMillis() - validateStartMs;
            Log.i(TAG, "snapshot read label=" + label
                    + " bytes=" + file.length()
                    + " verifySignature=" + verifySignature
                    + " encrypted=" + encrypted
                    + " readMs=" + readMs
                    + " parseMs=" + parseMs
                    + " validateMs=" + validateMs
                    + " totalMs=" + (System.currentTimeMillis() - startMs));
            return payload;
        } finally {
            snapshotMaterializationLock.unlock();
        }
    }

    JSONObject refreshFromConfiguredUrl(String fallbackUrl) throws Exception {
        // El snapshot completo incluye decenas de miles de entradas EPG y no cabe de
        // forma segura en el heap de 192 MB de algunos Fire TV. El snapshot ligero
        // mantiene TV y VOD; la EPG se obtiene y cachea por canales bajo demanda.
        return refreshStartupLiteFromConfiguredUrl(fallbackUrl);
    }

    PendingSnapshot downloadPendingSnapshotFromConfiguredUrl(String fallbackUrl) throws Exception {
        String configuredUrl = getSourceUrl(fallbackUrl);
        String sourceUrl = appendStartupLiteQuery(configuredUrl);
        if (sourceUrl.isEmpty()) {
            throw new IllegalStateException("no hay URL de catalogo configurada");
        }
        HttpClient.Response response = httpClient.get(
                sourceUrl,
                10000,
                30000,
                buildSnapshotHeaders(),
                MAX_SNAPSHOT_HTTP_BYTES
        );
        httpClient.requireSuccess(response, "descargando candidato de catalogo");
        String rawBody = response.body == null ? "" : response.body;
        JSONObject payload = new JSONObject(rawBody);
        validateSnapshotPayload(payload);
        validateSnapshotDoesNotRegress(payload, snapshotFile(), true);
        return new PendingSnapshot(payload, rawBody, configuredUrl);
    }

    void commitPendingSnapshot(PendingSnapshot pending) throws Exception {
        if (pending == null || pending.payload == null) {
            throw new IllegalArgumentException("candidato de catalogo vacio");
        }
        saveSnapshotObject(pending.payload, pending.sourceUrl, pending.rawJson, true);
    }

    boolean remoteCatalogFingerprintMatchesStored(String fallbackUrl) throws Exception {
        return remoteCatalogFingerprintMatches(fallbackUrl);
    }

    JSONObject refreshStartupLiteFromConfiguredUrl(String fallbackUrl) throws Exception {
        snapshotMaterializationLock.lockInterruptibly();
        try {
            String sourceUrl = appendStartupLiteQuery(getSourceUrl(fallbackUrl));
            if (sourceUrl.isEmpty()) {
                throw new IllegalStateException("no hay URL de catalogo configurada");
            }
            HttpClient.Response response = httpClient.get(
                    sourceUrl,
                    10000,
                    30000,
                    buildSnapshotHeaders(),
                    MAX_SNAPSHOT_HTTP_BYTES
            );
            httpClient.requireSuccess(response, "actualizando catalogo local ligero");
            String rawBody = response.body == null ? "" : response.body;
            JSONObject payload = new JSONObject(rawBody);
            validateSnapshotPayload(payload);
            saveSnapshotObject(payload, getSourceUrl(fallbackUrl), rawBody, true);
            return payload;
        } finally {
            snapshotMaterializationLock.unlock();
        }
    }

    JSONObject downloadStartupLiveBootstrapIfPossible(String fallbackUrl) throws Exception {
        SnapshotStatus status = getStatus(fallbackUrl);
        if (status == null || !status.available || status.expired || !hasRefreshCredentials(fallbackUrl)) {
            return null;
        }
        String sourceUrl = appendStartupLiveQuery(getSourceUrl(fallbackUrl));
        HttpClient.Response response = httpClient.get(
                sourceUrl,
                10000,
                30000,
                buildSnapshotHeaders(),
                MAX_SNAPSHOT_HTTP_BYTES
        );
        httpClient.requireSuccess(response, "descargando catalogo de arranque en directo");
        JSONObject payload = new JSONObject(response.body == null ? "" : response.body);
        validateSnapshotPayload(payload);
        String localPermissions = status.permissionsFingerprint == null ? "" : status.permissionsFingerprint.trim();
        String remotePermissions = buildPermissionsFingerprint(payload.optJSONObject("permissions"));
        if (!localPermissions.isEmpty()
                && !remotePermissions.isEmpty()
                && !localPermissions.equals(remotePermissions)) {
            throw new SecurityException("los permisos del catalogo local han cambiado");
        }
        return payload;
    }

    private JSONObject refreshStartupLiveFromConfiguredUrl(String fallbackUrl) throws Exception {
        snapshotMaterializationLock.lockInterruptibly();
        try {
            String sourceUrl = appendStartupLiveQuery(getSourceUrl(fallbackUrl));
            if (sourceUrl.isEmpty()) {
                throw new IllegalStateException("no hay URL de catalogo configurada");
            }
            HttpClient.Response response = httpClient.get(
                    sourceUrl,
                    10000,
                    30000,
                    buildSnapshotHeaders(),
                    MAX_SNAPSHOT_HTTP_BYTES
            );
            httpClient.requireSuccess(response, "preparando catalogo de canales en directo");
            String rawBody = response.body == null ? "" : response.body;
            JSONObject payload = new JSONObject(rawBody);
            validateSnapshotPayload(payload);
            saveSnapshotObject(payload, getSourceUrl(fallbackUrl), rawBody, true);
            return payload;
        } finally {
            snapshotMaterializationLock.unlock();
        }
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

    void acknowledgeDeviceMessage(String baseUrl, String messageId, String status) throws Exception {
        String cleanMessageId = messageId == null ? "" : messageId.trim();
        if (cleanMessageId.isEmpty()) {
            return;
        }
        String endpoint = joinUrl(baseUrl, "/api/offline/messages/" + Uri.encode(cleanMessageId) + "/ack");
        JSONObject payload = new JSONObject()
                .put("device_id", getDeviceId())
                .put("status", "read".equalsIgnoreCase(status) ? "read" : "delivered");
        HttpClient.Response response = httpClient.postJson(endpoint, payload, 5000, 10000, buildSnapshotHeaders());
        httpClient.requireSuccess(response, "confirmando aviso remoto");
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
        saveSnapshotObject(payload, sourceUrl, "");
    }

    void saveSnapshotObject(JSONObject payload, String sourceUrl, String rawJson) throws Exception {
        saveSnapshotObject(payload, sourceUrl, rawJson, false);
    }

    private void saveSnapshotObject(JSONObject payload, String sourceUrl, String rawJson, boolean allowReducedCatalog) throws Exception {
        if (payload == null) {
            throw new IllegalArgumentException("catalogo local vacio");
        }
        validateSnapshotPayload(payload);
        File current = snapshotFile();
        validateSnapshotDoesNotRegress(payload, current, allowReducedCatalog);
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
        // rawJson can exceed 50 MB. String.trim() creates another full-sized String on
        // Fire OS and was the final allocation that caused an OOM during background sync.
        String jsonToWrite = rawJson == null || rawJson.isEmpty() ? payload.toString() : rawJson;
        writeSnapshotString(tmp, jsonToWrite);
        if (!tmp.renameTo(current)) {
            writeSnapshotString(current, jsonToWrite);
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
        deleteFileQuietly(epgChannelCacheFile());
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
                .putInt(PREF_VOD_COUNT, countCatalogRows(payload, "vod") + countCatalogRows(payload, "adult")
                        + countCatalogRows(payload, "runtime_movies") + countCatalogRows(payload, "movistar_movies")
                        + countCatalogRows(payload, "movistar_series") + countCatalogRows(payload, "plex_vod")
                        + countCatalogRows(payload, "prime_vod"))
                .putInt(PREF_EPG_CHANNEL_COUNT, countOfflineEpgChannels(payload))
                .putInt(PREF_EPG_PROGRAM_COUNT, countOfflineEpgPrograms(payload))
                .putLong(PREF_EPG_UNTIL_MS, parseOfflineEpgUntilMs(payload))
                .putString(PREF_SCHEMA, schema)
                .putString(PREF_SOURCE_BASE_URL, payload.optString("source_base_url", "").trim())
                .putString(PREF_PAYLOAD_FINGERPRINT, payloadFingerprint)
                .putString(PREF_CATALOG_FINGERPRINT, payload.optString("catalog_fingerprint", "").trim())
                .putString(PREF_VERIFICATION_STATE, VERIFICATION_OK)
                .putString(PREF_VERIFICATION_MESSAGE, "")
                .remove(PREF_LAST_REJECTED_AT_MS)
                .remove(PREF_LAST_REJECTED_REASON)
                .remove(PREF_LAST_REJECTED_PREVIOUS_CHANNELS)
                .remove(PREF_LAST_REJECTED_CANDIDATE_CHANNELS)
                .remove(PREF_LAST_REJECTED_PREVIOUS_TOTAL)
                .remove(PREF_LAST_REJECTED_CANDIDATE_TOTAL)
                .remove(PREF_FORCE_STARTUP_SNAPSHOT_REFRESH)
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
            File parsedCache = startupParsedCacheFile();
            if (parsedCache.exists()) {
                parsedCache.delete();
            }
            deleteFileQuietly(fullParsedCacheFile());
            File playbackCache = startupPlaybackCacheFile();
            if (playbackCache.exists()) {
                playbackCache.delete();
            }
            File epgCache = epgChannelCacheFile();
            if (epgCache.exists()) {
                epgCache.delete();
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
                .remove(PREF_CATALOG_FINGERPRINT)
                .remove(PREF_LAST_STARTUP_CACHE_HIT_MS)
                .remove(PREF_VERIFICATION_STATE)
                .remove(PREF_VERIFICATION_MESSAGE)
                .remove(PREF_LAST_REJECTED_AT_MS)
                .remove(PREF_LAST_REJECTED_REASON)
                .remove(PREF_LAST_REJECTED_PREVIOUS_CHANNELS)
                .remove(PREF_LAST_REJECTED_CANDIDATE_CHANNELS)
                .remove(PREF_LAST_REJECTED_PREVIOUS_TOTAL)
                .remove(PREF_LAST_REJECTED_CANDIDATE_TOTAL)
                .remove(PREF_FORCE_STARTUP_SNAPSHOT_REFRESH)
                .apply();
    }

    void wipeLocalData() {
        clear();
        prefs.edit()
                .remove(PREF_SOURCE_URL)
                .remove(PREF_ACCESS_TOKEN)
                .remove(PREF_ACCESS_TOKEN_ENCRYPTED)
                .apply();
    }

    String getSourceUrl(String fallbackUrl) {
        String configured = prefs.getString(PREF_SOURCE_URL, "");
        if (configured != null && !configured.trim().isEmpty()) {
            return normalizeSnapshotSourceUrl(configured);
        }
        return normalizeSnapshotSourceUrl(fallbackUrl);
    }

    void setSourceUrl(String sourceUrl) {
        prefs.edit().putString(PREF_SOURCE_URL, normalizeSnapshotSourceUrl(sourceUrl)).apply();
    }

    private void migrateLegacyPublicBaseUrl() {
        String targetBaseUrl = normalizeBaseUrl(BuildConfig.OFFLINE_BASE_URL);
        if (targetBaseUrl.isEmpty()) {
            return;
        }
        String sourceUrl = prefs.getString(PREF_SOURCE_URL, "");
        String sourceBaseUrl = prefs.getString(PREF_SOURCE_BASE_URL, "");
        String migratedSourceUrl = migrateLegacyPublicUrl(sourceUrl, targetBaseUrl);
        String migratedSourceBaseUrl = migrateLegacyPublicUrl(sourceBaseUrl, targetBaseUrl);
        if (!safeString(sourceUrl).equals(migratedSourceUrl) || !safeString(sourceBaseUrl).equals(migratedSourceBaseUrl)) {
            prefs.edit()
                    .putString(PREF_SOURCE_URL, migratedSourceUrl)
                    .putString(PREF_SOURCE_BASE_URL, migratedSourceBaseUrl)
                    .apply();
            Log.i(TAG, "offline public base URL migrated to " + targetBaseUrl);
        }
    }

    private static String migrateLegacyPublicUrl(String value, String targetBaseUrl) {
        String clean = safeString(value);
        String legacy = normalizeBaseUrl(LEGACY_PUBLIC_BASE_URL);
        if (clean.isEmpty() || legacy.isEmpty() || targetBaseUrl.isEmpty()) {
            return clean;
        }
        if (clean.equalsIgnoreCase(legacy)) {
            return targetBaseUrl;
        }
        if (clean.toLowerCase(Locale.ROOT).startsWith((legacy + "/").toLowerCase(Locale.ROOT))) {
            return targetBaseUrl + clean.substring(legacy.length());
        }
        return clean;
    }

    private static String normalizeBaseUrl(String value) {
        String clean = safeString(value);
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    private static String safeString(String value) {
        return value == null ? "" : value.trim();
    }

    String getAccessToken() {
        String encrypted = prefs.getString(PREF_ACCESS_TOKEN_ENCRYPTED, "");
        if (encrypted != null && !encrypted.trim().isEmpty()) {
            try {
                return decryptPreferenceValue(encrypted.trim());
            } catch (Exception e) {
                Log.e(TAG, "failed to decrypt access token", e);
                return "";
            }
        }
        String legacy = prefs.getString(PREF_ACCESS_TOKEN, "");
        if (legacy == null || legacy.trim().isEmpty()) {
            return "";
        }
        String clean = legacy.trim();
        // Transparently migrate installations that predate encrypted credentials.
        setAccessToken(clean);
        return clean;
    }

    void setAccessToken(String accessToken) {
        String clean = accessToken == null ? "" : accessToken.trim();
        if (clean.isEmpty()) {
            prefs.edit()
                    .remove(PREF_ACCESS_TOKEN)
                    .remove(PREF_ACCESS_TOKEN_ENCRYPTED)
                    .apply();
            return;
        }
        try {
            prefs.edit()
                    .putString(PREF_ACCESS_TOKEN_ENCRYPTED, encryptPreferenceValue(clean))
                    .remove(PREF_ACCESS_TOKEN)
                    .apply();
        } catch (Exception e) {
            // Never write a newly entered credential back in plaintext.
            Log.e(TAG, "failed to encrypt access token", e);
        }
    }

    private String encryptPreferenceValue(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSnapshotKey());
        byte[] encrypted = cipher.doFinal((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        if (iv == null || iv.length != SNAPSHOT_GCM_IV_BYTES) {
            throw new IllegalStateException("invalid credential iv");
        }
        byte[] payload = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
        return android.util.Base64.encodeToString(payload, android.util.Base64.NO_WRAP);
    }

    private String decryptPreferenceValue(String encoded) throws Exception {
        byte[] payload = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP);
        if (payload.length <= SNAPSHOT_GCM_IV_BYTES) {
            throw new IllegalStateException("invalid credential payload");
        }
        byte[] iv = java.util.Arrays.copyOfRange(payload, 0, SNAPSHOT_GCM_IV_BYTES);
        byte[] encrypted = java.util.Arrays.copyOfRange(payload, SNAPSHOT_GCM_IV_BYTES, payload.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSnapshotKey(), new GCMParameterSpec(SNAPSHOT_GCM_TAG_BITS, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8).trim();
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
                prefs.getString(PREF_CATALOG_FINGERPRINT, ""),
                prefs.getString(PREF_PERMISSIONS_FINGERPRINT, ""),
                prefs.getLong(PREF_LAST_STARTUP_CACHE_HIT_MS, 0L),
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

    private File startupParsedCacheFile() {
        return new File(context.getFilesDir(), STARTUP_PARSED_CACHE_FILE);
    }

    private File fullParsedCacheFile() {
        return new File(context.getFilesDir(), FULL_PARSED_CACHE_FILE);
    }

    private File startupPlaybackCacheFile() {
        return new File(context.getFilesDir(), STARTUP_PLAYBACK_CACHE_FILE);
    }

    private File vodResumeItemsCacheFile() {
        return new File(context.getFilesDir(), VOD_RESUME_ITEMS_CACHE_FILE);
    }

    private File epgChannelCacheFile() {
        return new File(context.getFilesDir(), EPG_CHANNEL_CACHE_FILE);
    }

    private static final long STARTUP_LITE_REFRESH_THRESHOLD_BYTES = 12L * 1024L * 1024L;

    // Minimum file size considered a valid catalog (1 MB). A real catalog with
    // channels is always much larger; this avoids parsing the full 27 MB JSON
    // just to check it has content, which would OOM while ExoPlayer is active.
    private static final long MIN_VALID_SNAPSHOT_BYTES = 1024 * 1024L;

    private void backupCurrentSnapshotIfUseful(File source) {
        if (source == null || !source.exists() || source.length() < MIN_VALID_SNAPSHOT_BYTES) {
            return;
        }
        try {
            File lastGood = lastGoodSnapshotFile();
            copyFile(source, lastGood);
            if (!isEncryptedSnapshotFile(lastGood)) {
                StringBuilder rawBackup = readSnapshotString(lastGood, false);
                rewriteSnapshotEncrypted(lastGood, rawBackup.toString());
            }
        } catch (Exception ignored) {
            // Keep the previous last-good snapshot if the copy fails.
        }
    }

    private void validateSnapshotDoesNotRegress(JSONObject payload, File current, boolean allowReducedCatalog) throws Exception {
        if (allowReducedCatalog) {
            return;
        }
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
                + countCatalogRows(payload, "runtime_movies")
                + countCatalogRows(payload, "movistar_movies")
                + countCatalogRows(payload, "movistar_series")
                + countCatalogRows(payload, "plex_vod")
                + countCatalogRows(payload, "prime_vod");
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
        if (!liveDrop || !totalDrop) {
            return;
        }
        int lastRejectedCandidateLive = prefs.getInt(PREF_LAST_REJECTED_CANDIDATE_CHANNELS, 0);
        int lastRejectedCandidateTotal = prefs.getInt(PREF_LAST_REJECTED_CANDIDATE_TOTAL, 0);
        boolean sameReducedCandidateAlreadySeen = lastRejectedCandidateLive == candidateLive
                && lastRejectedCandidateTotal == candidateTotal
                && candidateLive > 0
                && candidateTotal > 0;
        boolean coherentLargeCatalog = candidateLive >= 1000 && candidateTotal >= 1000;
        if (sameReducedCandidateAlreadySeen || coherentLargeCatalog) {
            Log.w(TAG, "accepting reduced catalog candidate channels="
                    + candidateLive + "/" + previousLive
                    + " total=" + candidateTotal + "/" + previousTotal
                    + " repeated=" + sameReducedCandidateAlreadySeen
                    + " coherentLarge=" + coherentLargeCatalog);
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

    private static void deleteFileQuietly(File file) {
        try {
            if (file != null && file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        } catch (Exception ignored) {
        }
    }

    private static void writeUtf8String(FileOutputStream outputStream, String value) throws Exception {
        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write(value == null ? "" : value);
        }
    }

    private StringBuilder readSnapshotString(File file, boolean encrypted) throws Exception {
        ensureSnapshotFileWithinLimit(file);
        InputStream inputStream = encrypted ? encryptedSnapshotInputStream(file) : new FileInputStream(file);
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8), 65536)) {
            StringBuilder sb = new StringBuilder((int) Math.min(file.length(), 8L * 1024L * 1024L));
            char[] buf = new char[8192];
            int n;
            while ((n = reader.read(buf)) != -1) {
                throwIfInterrupted("lectura de snapshot cancelada");
                if (n > MAX_LOCAL_SNAPSHOT_BYTES - sb.length()) {
                    throw new IllegalStateException("snapshot local supera el limite de " + MAX_LOCAL_SNAPSHOT_BYTES + " bytes");
                }
                sb.append(buf, 0, n);
            }
            return sb;
        }
    }

    static void ensureSnapshotFileWithinLimit(File file) {
        if (file != null && file.exists() && file.length() > MAX_LOCAL_SNAPSHOT_BYTES) {
            throw new IllegalStateException("snapshot local demasiado grande: " + file.length() + " bytes");
        }
    }

    private static void throwIfInterrupted(String message) throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException(message);
        }
    }

    private InputStream snapshotInputStream(File file) throws Exception {
        return isEncryptedSnapshotFile(file) ? encryptedSnapshotInputStream(file) : new FileInputStream(file);
    }

    private static void readTargetCatalogObject(JsonReader reader, Set<String> requestedIds, Map<String, List<EpgRepository.EpgProgram>> out) throws Exception {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("epg".equals(name)) {
                readTargetEpgObject(reader, requestedIds, out);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    private static void readTargetEpgObject(JsonReader reader, Set<String> requestedIds, Map<String, List<EpgRepository.EpgProgram>> out) throws Exception {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("programs".equals(name)) {
                readTargetProgramsObject(reader, requestedIds, out);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    private static void readTargetProgramsObject(JsonReader reader, Set<String> requestedIds, Map<String, List<EpgRepository.EpgProgram>> out) throws Exception {
        reader.beginObject();
        while (reader.hasNext()) {
            String channelId = reader.nextName();
            String cleanChannelId = channelId == null ? "" : channelId.trim();
            if (requestedIds.contains(cleanChannelId)) {
                List<EpgRepository.EpgProgram> rows = readProgramArray(reader, cleanChannelId);
                if (!rows.isEmpty()) {
                    out.put(cleanChannelId, rows);
                    if (out.size() >= requestedIds.size()) {
                        throw new TargetEpgComplete();
                    }
                }
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    private static List<EpgRepository.EpgProgram> readProgramArray(JsonReader reader, String fallbackChannelId) throws Exception {
        List<EpgRepository.EpgProgram> rows = new ArrayList<>();
        if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            reader.skipValue();
            return rows;
        }
        reader.beginArray();
        while (reader.hasNext()) {
            if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                rows.add(readProgramObject(reader, fallbackChannelId));
            } else {
                reader.skipValue();
            }
        }
        reader.endArray();
        return rows;
    }

    private static EpgRepository.EpgProgram readProgramObject(JsonReader reader, String fallbackChannelId) throws Exception {
        String channelId = "";
        String channelName = "";
        String tvgId = "";
        String title = "Sin titulo";
        String icon = "";
        String description = "";
        String startTime = "";
        String endTime = "";
        String category = "";
        int progress = -1;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("channel_id".equals(name)) {
                channelId = readJsonString(reader);
            } else if ("channel_name".equals(name)) {
                channelName = readJsonString(reader);
            } else if ("tvg_id".equals(name)) {
                tvgId = readJsonString(reader);
            } else if ("title".equals(name)) {
                title = readJsonString(reader);
                if (title.trim().isEmpty()) {
                    title = "Sin titulo";
                }
            } else if ("icon".equals(name)) {
                icon = readJsonString(reader);
            } else if ("description".equals(name)) {
                description = readJsonString(reader);
            } else if ("start_time".equals(name)) {
                startTime = readJsonString(reader);
            } else if ("end_time".equals(name)) {
                endTime = readJsonString(reader);
            } else if ("category".equals(name)) {
                category = readJsonString(reader);
            } else if ("progress".equals(name)) {
                progress = readJsonInt(reader, -1);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        String cleanChannelId = channelId == null ? "" : channelId.trim();
        if (cleanChannelId.isEmpty()) {
            cleanChannelId = fallbackChannelId == null ? "" : fallbackChannelId.trim();
        }
        return new EpgRepository.EpgProgram(cleanChannelId, channelName, tvgId, title, icon, description, startTime, endTime, category, progress);
    }

    private static String readJsonString(JsonReader reader) throws Exception {
        JsonToken token = reader.peek();
        if (token == JsonToken.NULL) {
            reader.nextNull();
            return "";
        }
        if (token == JsonToken.STRING || token == JsonToken.NUMBER) {
            return reader.nextString();
        }
        if (token == JsonToken.BOOLEAN) {
            return String.valueOf(reader.nextBoolean());
        }
        reader.skipValue();
        return "";
    }

    private static int readJsonInt(JsonReader reader, int fallback) throws Exception {
        JsonToken token = reader.peek();
        if (token == JsonToken.NULL) {
            reader.nextNull();
            return fallback;
        }
        if (token == JsonToken.NUMBER) {
            return reader.nextInt();
        }
        if (token == JsonToken.STRING) {
            try {
                return Integer.parseInt(reader.nextString());
            } catch (Exception ignored) {
                return fallback;
            }
        }
        reader.skipValue();
        return fallback;
    }

    private static final class TargetEpgComplete extends RuntimeException {
    }

    private void writeSnapshotString(File file, String value) throws Exception {
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSnapshotKey());
            byte[] iv = cipher.getIV();
            if (iv == null || iv.length != SNAPSHOT_GCM_IV_BYTES) {
                throw new IllegalStateException("iv de catalogo cifrado invalido");
            }
            outputStream.write(SNAPSHOT_ENCRYPTED_MAGIC);
            outputStream.write(iv);
            try (CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher)) {
                writeUtf8StringToStream(cipherOutputStream, value);
            }
        }
    }

    private Object readEncryptedObject(File file) throws Exception {
        try (InputStream inputStream = encryptedSnapshotInputStream(file);
             GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
             ObjectInputStream objectInputStream = new ObjectInputStream(gzipInputStream)) {
            return objectInputStream.readObject();
        }
    }

    private void writeEncryptedObject(File file, Serializable value) throws Exception {
        if (value == null) {
            throw new IllegalArgumentException("objeto cifrado vacio");
        }
        ByteArrayOutputStream rawOutput = new ByteArrayOutputStream(128 * 1024);
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(rawOutput);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(gzipOutputStream)) {
            objectOutputStream.writeObject(value);
        }
        encryptRawBytesToFile(file, rawOutput.toByteArray());
    }

    synchronized Map<String, ChannelItem> loadVodResumeItems() {
        File file = vodResumeItemsCacheFile();
        if (!file.exists() || file.length() <= 0L) {
            return new LinkedHashMap<>();
        }
        try {
            Object decoded = readEncryptedObject(file);
            if (!(decoded instanceof Map)) {
                throw new IllegalStateException("cache de fichas VOD invalida");
            }
            Map<String, ChannelItem> items = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) decoded).entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof ChannelItem) {
                    ChannelItem item = (ChannelItem) entry.getValue();
                    if (item.isVod && !item.id.isEmpty()) {
                        items.put((String) entry.getKey(), item);
                    }
                }
                if (items.size() >= 80) {
                    break;
                }
            }
            return items;
        } catch (Exception e) {
            Log.w(TAG, "VOD resume item cache ignored", e);
            deleteFileQuietly(file);
            return new LinkedHashMap<>();
        }
    }

    synchronized void saveVodResumeItem(ChannelItem item) {
        if (item == null || !item.isVod || item.id.isEmpty()) {
            return;
        }
        try {
            Map<String, ChannelItem> items = loadVodResumeItems();
            items.remove(item.id);
            items.put(item.id, item);
            while (items.size() > 80) {
                items.remove(items.keySet().iterator().next());
            }
            writeEncryptedObject(vodResumeItemsCacheFile(), (Serializable) new LinkedHashMap<>(items));
        } catch (Exception e) {
            Log.w(TAG, "failed to save VOD resume item", e);
        }
    }

    synchronized void removeVodResumeItem(String itemId) {
        String cleanId = itemId == null ? "" : itemId.trim();
        if (cleanId.isEmpty()) {
            return;
        }
        try {
            Map<String, ChannelItem> items = loadVodResumeItems();
            if (items.remove(cleanId) != null) {
                writeEncryptedObject(vodResumeItemsCacheFile(), (Serializable) new LinkedHashMap<>(items));
            }
        } catch (Exception e) {
            Log.w(TAG, "failed to remove VOD resume item", e);
        }
    }

    synchronized void clearVodResumeItems() {
        deleteFileQuietly(vodResumeItemsCacheFile());
    }

    private synchronized EpgChannelCache readEpgChannelCache(String fingerprint) {
        String cleanFingerprint = fingerprint == null ? "" : fingerprint.trim();
        if (cleanFingerprint.isEmpty()) {
            return new EpgChannelCache(cleanFingerprint, new LinkedHashMap<>());
        }
        File file = epgChannelCacheFile();
        if (!file.exists() || file.length() <= 0L) {
            return new EpgChannelCache(cleanFingerprint, new LinkedHashMap<>());
        }
        if (file.length() > MAX_EPG_CHANNEL_CACHE_BYTES) {
            Log.w(TAG, "target EPG cache too large; deleting bytes=" + file.length());
            deleteFileQuietly(file);
            return new EpgChannelCache(cleanFingerprint, new LinkedHashMap<>());
        }
        try {
            Object decoded = readEncryptedObject(file);
            if (!(decoded instanceof EpgChannelCache)) {
                throw new IllegalStateException("tipo de cache EPG invalido");
            }
            EpgChannelCache cache = (EpgChannelCache) decoded;
            if (!cache.matches(cleanFingerprint)) {
                return new EpgChannelCache(cleanFingerprint, new LinkedHashMap<>());
            }
            return cache;
        } catch (Exception e) {
            Log.w(TAG, "target EPG cache ignored", e);
            deleteFileQuietly(file);
            return new EpgChannelCache(cleanFingerprint, new LinkedHashMap<>());
        }
    }

    private synchronized void mergeEpgChannelCache(String fingerprint, Map<String, List<EpgRepository.EpgProgram>> loaded) {
        if (loaded == null || loaded.isEmpty()) {
            return;
        }
        String cleanFingerprint = fingerprint == null ? "" : fingerprint.trim();
        if (cleanFingerprint.isEmpty()) {
            return;
        }
        try {
            EpgChannelCache cache = readEpgChannelCache(cleanFingerprint);
            cache.rowsByChannelId.putAll(copyProgramMap(loaded));
            trimEpgChannelCache(cache);
            writeEncryptedObject(epgChannelCacheFile(), cache);
            if (epgChannelCacheFile().length() > MAX_EPG_CHANNEL_CACHE_BYTES) {
                Log.w(TAG, "target EPG cache exceeded limit after save; deleting bytes="
                        + epgChannelCacheFile().length());
                deleteFileQuietly(epgChannelCacheFile());
                return;
            }
            Log.i(TAG, "target EPG cache saved channels=" + loaded.size()
                    + " totalCached=" + cache.rowsByChannelId.size());
        } catch (Exception e) {
            Log.w(TAG, "target EPG cache save failed channels=" + loaded.size(), e);
            deleteFileQuietly(epgChannelCacheFile());
        }
    }

    private static void trimEpgChannelCache(EpgChannelCache cache) {
        if (cache == null || cache.rowsByChannelId == null) {
            return;
        }
        while (cache.rowsByChannelId.size() > MAX_EPG_CHANNEL_CACHE_CHANNELS) {
            Iterator<String> iterator = cache.rowsByChannelId.keySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            iterator.next();
            iterator.remove();
        }
    }

    private static String buildEpgCacheFingerprint(SnapshotStatus status) {
        if (status == null) {
            return "";
        }
        String payload = status.payloadFingerprint == null ? "" : status.payloadFingerprint.trim();
        if (!payload.isEmpty()) {
            return payload;
        }
        return status.updatedAtMs + ":" + status.sizeBytes + ":" + status.expiresAtMs + ":" + status.epgProgramCount;
    }

    private static Map<String, List<EpgRepository.EpgProgram>> copyProgramMap(Map<String, List<EpgRepository.EpgProgram>> input) {
        Map<String, List<EpgRepository.EpgProgram>> out = new LinkedHashMap<>();
        if (input == null) {
            return out;
        }
        for (Map.Entry<String, List<EpgRepository.EpgProgram>> entry : input.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            if (!key.isEmpty()) {
                out.put(key, copyProgramList(entry.getValue()));
            }
        }
        return out;
    }

    private static List<EpgRepository.EpgProgram> copyProgramList(List<EpgRepository.EpgProgram> input) {
        return input == null ? new ArrayList<>() : new ArrayList<>(input);
    }

    private void encryptRawBytesToFile(File file, byte[] rawBytes) throws Exception {
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileOutputStream outputStream = new FileOutputStream(tmp, false)) {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSnapshotKey());
            byte[] iv = cipher.getIV();
            if (iv == null || iv.length != SNAPSHOT_GCM_IV_BYTES) {
                throw new IllegalStateException("iv de cache cifrada invalido");
            }
            outputStream.write(SNAPSHOT_ENCRYPTED_MAGIC);
            outputStream.write(iv);
            try (CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher)) {
                cipherOutputStream.write(rawBytes);
            }
        }
        if (!tmp.renameTo(file)) {
            copyFile(tmp, file);
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }

    // --- Serializacion binaria manual de la cache de catalogo de arranque (rapida, sin reflexion) ---
    // La cache se guarda SOLO con gzip (sin cifrado AES-GCM) porque el fichero vive en
    // /data/data/.../files/ (protegido por permisos de app) y los datos ya estan firmados
    // por el snapshot. En el Fire Stick 4K Max, el descifrado AES-GCM por software tarda
    // ~10-17s para ~30 MB, mientras que gzip+binario tarda ~1-2s. La integridad real la
    // garantiza la firma del snapshot; si el fichero se corrompe, el try/catch lo borra y
    // fuerza reconstruccion.
    private void writeStartupParsedCacheBinary(File file, SnapshotStatus status, CatalogLoadResult result) throws Exception {
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tmp, false);
             BufferedOutputStream bos = new BufferedOutputStream(fos, 1 << 16);
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bos, 1 << 16);
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(gzipOutputStream, 1 << 16))) {
            out.writeInt(STARTUP_PARSED_BINARY_FORMAT_VERSION);
            writeStr(out, status == null ? "" : status.payloadFingerprint);
            writeStr(out, status == null ? "" : status.catalogFingerprint);
            writeStr(out, status == null ? "" : status.permissionsFingerprint);
            out.writeBoolean(result.liveOnly);
            writeStr(out, result.loadSource);
            out.writeInt(Math.max(0, result.liveItems));
            out.writeInt(Math.max(0, result.vodItems));
            writeStr(out, result.defaultFilterKey);
            writeOfflinePermissions(out, result.offlinePermissions);
            List<ChannelFilter> filters = result.filters;
            int filterCount = filters == null ? 0 : filters.size();
            out.writeInt(filterCount);
            for (int i = 0; i < filterCount; i++) {
                writeFilter(out, filters.get(i));
            }
            List<ChannelItem> channels = result.channels;
            int channelCount = channels == null ? 0 : channels.size();
            out.writeInt(channelCount);
            for (int i = 0; i < channelCount; i++) {
                writeChannel(out, channels.get(i));
            }
        }
        if (!tmp.renameTo(file)) {
            copyFile(tmp, file);
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }

    private CatalogLoadResult readStartupParsedCacheBinary(File file, SnapshotStatus status, boolean liveStartup) throws Exception {
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file), 1 << 16);
             GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream, 1 << 16);
             DataInputStream in = new DataInputStream(new BufferedInputStream(gzipInputStream, 1 << 16))) {
            int formatVersion = in.readInt();
            if (formatVersion != 2 && formatVersion != STARTUP_PARSED_BINARY_FORMAT_VERSION) {
                throw new IllegalStateException("version de cache binaria invalida: " + formatVersion);
            }
            String payloadFingerprint = readStr(in);
            String catalogFingerprint = readStr(in);
            String permissionsFingerprint = readStr(in);
            if (!parsedCacheFingerprintMatches(status, payloadFingerprint, catalogFingerprint, permissionsFingerprint)) {
                return null;
            }
            boolean liveOnly = formatVersion >= 3 && in.readBoolean();
            String loadSource = formatVersion >= 3 ? readStr(in) : "startup-cache-v2-migrated";
            int liveItems = formatVersion >= 3 ? Math.max(0, in.readInt()) : 0;
            int vodItems = formatVersion >= 3 ? Math.max(0, in.readInt()) : 0;
            String defaultFilterKey = readStr(in);
            OfflinePermissions permissions = readOfflinePermissions(in);
            int filterCount = in.readInt();
            if (filterCount < 0 || filterCount > MAX_BINARY_CACHE_ITEMS) {
                throw new IOException("numero de filtros invalido=" + filterCount);
            }
            List<ChannelFilter> filters = new ArrayList<>(filterCount);
            for (int i = 0; i < filterCount; i++) {
                filters.add(readFilter(in));
            }
            int channelCount = in.readInt();
            if (channelCount < 0 || channelCount > MAX_BINARY_CACHE_ITEMS) {
                throw new IOException("numero de canales invalido=" + channelCount);
            }
            List<ChannelItem> channels = new ArrayList<>(channelCount);
            for (int i = 0; i < channelCount; i++) {
                if ((i & 127) == 0) {
                    throwIfInterrupted("lectura de cache de catalogo cancelada");
                }
                channels.add(readChannel(in));
            }
            if (formatVersion == 2 && liveStartup) {
                copyFile(file, fullParsedCacheFile());
                List<ChannelItem> liveChannels = new ArrayList<>(channels.size());
                for (ChannelItem channel : channels) {
                    if (channel != null && !channel.isVod) {
                        liveChannels.add(channel);
                    }
                }
                List<ChannelFilter> liveFilters = new ArrayList<>(filters.size());
                for (ChannelFilter filter : filters) {
                    if (filter != null && filter.type != 3 && filter.type != 4) {
                        liveFilters.add(filter);
                    }
                }
                if (permissions.vodEnabled) {
                    liveFilters.add(new ChannelFilter("vod", "VOD", 3, 0, ""));
                }
                if (permissions.tivifyAdultEnabled) {
                    liveFilters.add(new ChannelFilter("vod-adult", "VOD Adulto", 4, 0, ""));
                }
                channels = liveChannels;
                filters = liveFilters;
                liveOnly = true;
                liveItems = liveChannels.size();
                vodItems = 0;
                Log.w(TAG, "startup parsed cache v2 migrated to live-only channels=" + liveItems);
            }
            return new CatalogLoadResult(
                    channels,
                    filters,
                    defaultFilterKey,
                    permissions,
                    liveOnly,
                    loadSource,
                    liveItems,
                    vodItems,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L
            );
        }
    }

    private boolean parsedCacheFingerprintMatches(SnapshotStatus status, String payloadFingerprint, String catalogFingerprint, String permissionsFingerprint) {
        if (status == null) {
            return false;
        }
        return safeFingerprintEquals(payloadFingerprint, status.payloadFingerprint)
                && safeFingerprintEquals(catalogFingerprint, status.catalogFingerprint)
                && safeFingerprintEquals(permissionsFingerprint, status.permissionsFingerprint);
    }

    private static boolean safeFingerprintEquals(String left, String right) {
        String a = left == null ? "" : left.trim();
        String b = right == null ? "" : right.trim();
        return a.equals(b);
    }

    private void writeOfflinePermissions(DataOutputStream out, OfflinePermissions permissions) throws IOException {
        OfflinePermissions p = permissions == null ? new OfflinePermissions() : permissions;
        out.writeBoolean(p.liveEnabled);
        out.writeBoolean(p.vodEnabled);
        out.writeBoolean(p.tivifyAdultEnabled);
        out.writeBoolean(p.runtimeEnabled);
        out.writeBoolean(p.movistarVodEnabled);
        out.writeBoolean(p.canViewRecordings);
        out.writeBoolean(p.canScheduleRecordings);
        out.writeBoolean(p.canDeleteRecordings);
        out.writeBoolean(p.protectAdultVod);
        out.writeInt(p.allowedPlatformIds.size());
        for (Integer platformId : p.allowedPlatformIds) {
            out.writeInt(platformId == null ? 0 : platformId);
        }
        writeStringSet(out, p.protectedFilterKeys);
        writeStringSet(out, p.protectedChannelIds);
        writeStringSet(out, p.protectedGroupNames);
    }

    private OfflinePermissions readOfflinePermissions(DataInputStream in) throws IOException {
        OfflinePermissions p = new OfflinePermissions();
        p.liveEnabled = in.readBoolean();
        p.vodEnabled = in.readBoolean();
        p.tivifyAdultEnabled = in.readBoolean();
        p.runtimeEnabled = in.readBoolean();
        p.movistarVodEnabled = in.readBoolean();
        p.canViewRecordings = in.readBoolean();
        p.canScheduleRecordings = in.readBoolean();
        p.canDeleteRecordings = in.readBoolean();
        p.protectAdultVod = in.readBoolean();
        int platformCount = in.readInt();
        if (platformCount < 0 || platformCount > MAX_BINARY_CACHE_ITEMS) {
            throw new IOException("numero de plataformas invalido=" + platformCount);
        }
        for (int i = 0; i < platformCount; i++) {
            p.allowedPlatformIds.add(in.readInt());
        }
        readStringSet(in, p.protectedFilterKeys);
        readStringSet(in, p.protectedChannelIds);
        readStringSet(in, p.protectedGroupNames);
        return p;
    }

    private void writeFilter(DataOutputStream out, ChannelFilter filter) throws IOException {
        writeStr(out, filter.key);
        writeStr(out, filter.label);
        out.writeInt(filter.type);
        out.writeInt(filter.platformId);
        writeStr(out, filter.groupName);
    }

    private ChannelFilter readFilter(DataInputStream in) throws IOException {
        String key = readStr(in);
        String label = readStr(in);
        int type = in.readInt();
        int platformId = in.readInt();
        String groupName = readStr(in);
        return new ChannelFilter(key, label, type, platformId, groupName);
    }

    private void writeChannel(DataOutputStream out, ChannelItem channel) throws IOException {
        writeStr(out, channel.id);
        writeStr(out, channel.name);
        writeStr(out, channel.tvgId);
        writeStr(out, channel.logoUrl);
        writeStr(out, channel.group);
        writeStr(out, channel.playUrl);
        writeStr(out, channel.fallbackPlayUrl);
        out.writeInt(channel.originalOrder);
        out.writeInt(channel.dashboardOrder);
        out.writeBoolean(channel.isVod);
        out.writeBoolean(channel.isAdultVod);
        out.writeInt(channel.platformId);
        writeStr(out, channel.platformName);
        List<String> customGroups = channel.customGroups;
        if (customGroups == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(customGroups.size());
            for (String group : customGroups) {
                writeStr(out, group);
            }
        }
        Map<String, Integer> groupOrder = channel.groupOrder;
        int groupOrderSize = groupOrder == null ? 0 : groupOrder.size();
        out.writeInt(groupOrderSize);
        if (groupOrder != null) {
            for (Map.Entry<String, Integer> entry : groupOrder.entrySet()) {
                writeStr(out, entry.getKey());
                out.writeInt(entry.getValue() == null ? 0 : entry.getValue());
            }
        }
        writeStr(out, channel.drmScheme);
        writeStr(out, channel.drmLicenseUrl);
        writeStr(out, channel.vodFilterKey);
        out.writeBoolean(channel.directPlayback);
        writeStr(out, channel.playbackProfile);
        writeStr(out, channel.vodDescription);
        writeStr(out, channel.vodYear);
        out.writeLong(channel.vodDurationSeconds);
        out.writeBoolean(channel.favorite);
        writeStr(out, channel.nowProgram);
        writeStr(out, channel.nextProgram);
    }

    private ChannelItem readChannel(DataInputStream in) throws IOException {
        String id = readStr(in);
        String name = readStr(in);
        String tvgId = readStr(in);
        String logoUrl = readStr(in);
        String group = readStr(in);
        String playUrl = readStr(in);
        String fallbackPlayUrl = readStr(in);
        int originalOrder = in.readInt();
        int dashboardOrder = in.readInt();
        boolean isVod = in.readBoolean();
        boolean isAdultVod = in.readBoolean();
        int platformId = in.readInt();
        String platformName = readStr(in);
        int customGroupsSize = in.readInt();
        List<String> customGroups;
        if (customGroupsSize < 0) {
            customGroups = null;
        } else {
            if (customGroupsSize > MAX_BINARY_CACHE_ITEMS) {
                throw new IOException("customGroups invalido=" + customGroupsSize);
            }
            customGroups = new ArrayList<>(customGroupsSize);
            for (int i = 0; i < customGroupsSize; i++) {
                customGroups.add(readStr(in));
            }
        }
        int groupOrderSize = in.readInt();
        if (groupOrderSize < 0 || groupOrderSize > MAX_BINARY_CACHE_ITEMS) {
            throw new IOException("groupOrder invalido=" + groupOrderSize);
        }
        Map<String, Integer> groupOrder = new LinkedHashMap<>();
        for (int i = 0; i < groupOrderSize; i++) {
            String key = readStr(in);
            int value = in.readInt();
            groupOrder.put(key, value);
        }
        String drmScheme = readStr(in);
        String drmLicenseUrl = readStr(in);
        String vodFilterKey = readStr(in);
        boolean directPlayback = in.readBoolean();
        String playbackProfile = readStr(in);
        String vodDescription = readStr(in);
        String vodYear = readStr(in);
        long vodDurationSeconds = in.readLong();
        boolean favorite = in.readBoolean();
        String nowProgram = readStr(in);
        String nextProgram = readStr(in);
        ChannelItem channel = new ChannelItem(id, name, tvgId, logoUrl, group, playUrl, fallbackPlayUrl,
                originalOrder, dashboardOrder, isVod, isAdultVod, platformId, platformName, customGroups,
                drmScheme, drmLicenseUrl, vodFilterKey, directPlayback, vodDescription, vodYear,
                vodDurationSeconds, playbackProfile);
        if (!groupOrder.isEmpty()) {
            channel.groupOrder.putAll(groupOrder);
        }
        channel.favorite = favorite;
        channel.nowProgram = nowProgram;
        channel.nextProgram = nextProgram;
        return channel;
    }

    private void writeStringSet(DataOutputStream out, Set<String> values) throws IOException {
        if (values == null) {
            out.writeInt(0);
            return;
        }
        out.writeInt(values.size());
        for (String value : values) {
            writeStr(out, value);
        }
    }

    private void readStringSet(DataInputStream in, Set<String> target) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > MAX_BINARY_CACHE_ITEMS) {
            throw new IOException("numero de elementos invalido=" + count);
        }
        for (int i = 0; i < count; i++) {
            target.add(readStr(in));
        }
    }

    private static void writeStr(DataOutputStream out, String value) throws IOException {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readStr(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > MAX_BINARY_CACHE_STR_BYTES) {
            throw new IOException("longitud de cadena invalida=" + length);
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void rewriteSnapshotEncrypted(File file, String rawJson) {
        if (file == null || rawJson == null || rawJson.isEmpty()) {
            return;
        }
        File encryptedTmp = new File(file.getParentFile(), file.getName() + ".enc.tmp");
        try {
            writeSnapshotString(encryptedTmp, rawJson);
            if (!encryptedTmp.renameTo(file)) {
                writeSnapshotString(file, rawJson);
                //noinspection ResultOfMethodCallIgnored
                encryptedTmp.delete();
            }
            Log.i(TAG, "snapshot migrated to encrypted storage file=" + file.getName());
        } catch (Exception e) {
            Log.w(TAG, "failed to migrate snapshot to encrypted storage file=" + file.getName(), e);
            //noinspection ResultOfMethodCallIgnored
            encryptedTmp.delete();
        }
    }

    private InputStream encryptedSnapshotInputStream(File file) throws Exception {
        FileInputStream inputStream = new FileInputStream(file);
        byte[] magic = new byte[SNAPSHOT_ENCRYPTED_MAGIC.length];
        if (inputStream.read(magic) != magic.length || !java.util.Arrays.equals(magic, SNAPSHOT_ENCRYPTED_MAGIC)) {
            inputStream.close();
            throw new IllegalStateException("cabecera de catalogo cifrado invalida");
        }
        byte[] iv = new byte[SNAPSHOT_GCM_IV_BYTES];
        if (inputStream.read(iv) != iv.length) {
            inputStream.close();
            throw new IllegalStateException("iv de catalogo cifrado invalido");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSnapshotKey(), new GCMParameterSpec(SNAPSHOT_GCM_TAG_BITS, iv));
        return new CipherInputStream(inputStream, cipher);
    }

    private boolean isEncryptedSnapshotFile(File file) {
        if (file == null || !file.exists() || file.length() < SNAPSHOT_ENCRYPTED_MAGIC.length + SNAPSHOT_GCM_IV_BYTES) {
            return false;
        }
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] magic = new byte[SNAPSHOT_ENCRYPTED_MAGIC.length];
            return inputStream.read(magic) == magic.length && java.util.Arrays.equals(magic, SNAPSHOT_ENCRYPTED_MAGIC);
        } catch (Exception ignored) {
            return false;
        }
    }

    private SecretKey getOrCreateSnapshotKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        java.security.Key existing = keyStore.getKey(SNAPSHOT_ENCRYPTION_ALIAS, null);
        if (existing instanceof SecretKey) {
            return (SecretKey) existing;
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                SNAPSHOT_ENCRYPTION_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setUnlockedDeviceRequired(false);
        }
        keyGenerator.init(builder.build());
        return keyGenerator.generateKey();
    }

    private static void writeUtf8StringToStream(java.io.OutputStream outputStream, String value) throws Exception {
        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write(value == null ? "" : value);
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

    private boolean hasRefreshCredentials(String fallbackUrl) {
        return !getAccessToken().trim().isEmpty() && !getSourceUrl(fallbackUrl).trim().isEmpty();
    }

    private boolean remoteCatalogFingerprintMatches(String fallbackUrl) throws Exception {
        String localFingerprint = prefs.getString(PREF_CATALOG_FINGERPRINT, "").trim();
        if (localFingerprint.isEmpty()) {
            return false;
        }
        String metaUrl = snapshotMetaUrl(getSourceUrl(fallbackUrl));
        if (metaUrl.isEmpty()) {
            return false;
        }
        HttpClient.Response response = httpClient.get(metaUrl, 5000, 12000, buildSnapshotHeaders());
        if (response == null) {
            throw new IllegalStateException("meta de catalogo sin respuesta");
        }
        if (response.code == 401 || response.code == 403) {
            throw new SecurityException("catalogo remoto no autorizado: HTTP " + response.code);
        }
        httpClient.requireSuccess(response, "comprobando huella de catalogo");
        JSONObject payload = new JSONObject(response.body == null ? "" : response.body);
        String remoteFingerprint = payload.optString("catalog_fingerprint", "").trim();
        return !remoteFingerprint.isEmpty() && remoteFingerprint.equals(localFingerprint);
    }

    private static String snapshotMetaUrl(String sourceUrl) throws Exception {
        String clean = sourceUrl == null ? "" : sourceUrl.trim();
        if (clean.isEmpty()) {
            return "";
        }
        URI uri = new URI(clean);
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (path.endsWith("/api/offline/snapshot/meta")) {
            return uri.toString();
        }
        if (!path.endsWith("/api/offline/snapshot")) {
            return "";
        }
        URI meta = new URI(
                uri.getScheme(),
                uri.getAuthority(),
                path + "/meta",
                uri.getQuery(),
                uri.getFragment()
        );
        return meta.toString();
    }

    static String appendStartupLiteQuery(String sourceUrl) {
        return appendSnapshotModeQuery(sourceUrl, "startup_lite");
    }

    static String appendStartupLiveQuery(String sourceUrl) {
        return appendSnapshotModeQuery(sourceUrl, "startup_live");
    }

    static String normalizeSnapshotSourceUrl(String sourceUrl) {
        return appendSnapshotModeQuery(sourceUrl, "");
    }

    private static String appendSnapshotModeQuery(String sourceUrl, String mode) {
        String clean = sourceUrl == null ? "" : sourceUrl.trim();
        if (clean.isEmpty()) {
            return clean;
        }
        int fragmentIndex = clean.indexOf('#');
        String fragment = fragmentIndex >= 0 ? clean.substring(fragmentIndex) : "";
        String withoutFragment = fragmentIndex >= 0 ? clean.substring(0, fragmentIndex) : clean;
        int queryIndex = withoutFragment.indexOf('?');
        String base = queryIndex >= 0 ? withoutFragment.substring(0, queryIndex) : withoutFragment;
        String rawQuery = queryIndex >= 0 ? withoutFragment.substring(queryIndex + 1) : "";
        ArrayList<String> parameters = new ArrayList<>();
        if (!rawQuery.isEmpty()) {
            for (String parameter : rawQuery.split("&")) {
                String trimmed = parameter == null ? "" : parameter.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int equals = trimmed.indexOf('=');
                String name = (equals >= 0 ? trimmed.substring(0, equals) : trimmed).trim();
                if ("startup_live".equalsIgnoreCase(name)
                        || "startup_lite".equalsIgnoreCase(name)
                        || "lite".equalsIgnoreCase(name)) {
                    continue;
                }
                parameters.add(trimmed);
            }
        }
        if (mode != null && !mode.isEmpty()) {
            parameters.add(mode + "=1");
        }
        return base + (parameters.isEmpty() ? "" : "?" + String.join("&", parameters)) + fragment;
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
                + countCatalogRows(payload, "runtime_movies")
                + countCatalogRows(payload, "movistar_movies")
                + countCatalogRows(payload, "movistar_series")
                + countCatalogRows(payload, "plex_vod")
                + countCatalogRows(payload, "prime_vod");
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
        if (payload == null) {
            return "";
        }
        JSONObject signature = payload.optJSONObject("signature");
        if (signature != null) {
            String signatureFingerprint = signature.optString("key_id", "").trim()
                    + ":"
                    + signature.optString("value", "").trim();
            if (!signatureFingerprint.trim().equals(":")) {
                return sha256Hex(signatureFingerprint);
            }
        }
        JSONObject catalog = payload.optJSONObject("catalog");
        JSONObject epg = payload.optJSONObject("epg");
        String lightweightFingerprint = normalizeSchema(payload.optString("schema", ""))
                + "|" + payload.optLong("generated_at", 0L)
                + "|" + payload.optLong("expires_at", 0L)
                + "|" + countCatalogRows(catalog, "channels")
                + "|" + countCatalogRows(catalog, "vod")
                + "|" + countCatalogRows(epg, "programs");
        return sha256Hex(lightweightFingerprint);
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

    private static final class StartupParsedCatalogCache implements Serializable {
        private static final long serialVersionUID = 1L;

        final int version;
        final String payloadFingerprint;
        final String catalogFingerprint;
        final String permissionsFingerprint;
        final CatalogLoadResult result;

        StartupParsedCatalogCache(SnapshotStatus status, CatalogLoadResult result) {
            this.version = STARTUP_PARSED_CACHE_VERSION;
            this.payloadFingerprint = status == null ? "" : status.payloadFingerprint;
            this.catalogFingerprint = status == null ? "" : status.catalogFingerprint;
            this.permissionsFingerprint = status == null ? "" : status.permissionsFingerprint;
            this.result = result;
        }

        boolean matches(SnapshotStatus status) {
            if (version != STARTUP_PARSED_CACHE_VERSION || status == null) {
                return false;
            }
            return safeEquals(payloadFingerprint, status.payloadFingerprint)
                    && safeEquals(catalogFingerprint, status.catalogFingerprint)
                    && safeEquals(permissionsFingerprint, status.permissionsFingerprint);
        }

        private static boolean safeEquals(String left, String right) {
            String a = left == null ? "" : left.trim();
            String b = right == null ? "" : right.trim();
            return a.equals(b);
        }
    }

    private static final class StartupPlaybackChannelCache implements Serializable {
        private static final long serialVersionUID = 1L;

        final int version;
        final String payloadFingerprint;
        final String catalogFingerprint;
        final String permissionsFingerprint;
        final ChannelItem channel;

        StartupPlaybackChannelCache(SnapshotStatus status, ChannelItem channel) {
            this.version = STARTUP_PLAYBACK_CACHE_VERSION;
            this.payloadFingerprint = status == null ? "" : status.payloadFingerprint;
            this.catalogFingerprint = status == null ? "" : status.catalogFingerprint;
            this.permissionsFingerprint = status == null ? "" : status.permissionsFingerprint;
            this.channel = channel;
        }

        boolean matches(SnapshotStatus status) {
            if (version != STARTUP_PLAYBACK_CACHE_VERSION || status == null) {
                return false;
            }
            // La URL firmada del canal sigue siendo valida mientras lo sean el snapshot
            // local y sus permisos. Un cambio de VOD no debe invalidar este fast-path.
            return StartupParsedCatalogCache.safeEquals(permissionsFingerprint, status.permissionsFingerprint);
        }
    }

    static final class PendingSnapshot {
        final JSONObject payload;
        final String rawJson;
        final String sourceUrl;

        PendingSnapshot(JSONObject payload, String rawJson, String sourceUrl) {
            this.payload = payload;
            this.rawJson = rawJson == null ? "" : rawJson;
            this.sourceUrl = sourceUrl == null ? "" : sourceUrl;
        }
    }

    private static final class EpgChannelCache implements Serializable {
        private static final long serialVersionUID = 1L;

        final int version;
        final String snapshotFingerprint;
        final Map<String, List<EpgRepository.EpgProgram>> rowsByChannelId;

        EpgChannelCache(String snapshotFingerprint, Map<String, List<EpgRepository.EpgProgram>> rowsByChannelId) {
            this.version = EPG_CHANNEL_CACHE_VERSION;
            this.snapshotFingerprint = snapshotFingerprint == null ? "" : snapshotFingerprint.trim();
            this.rowsByChannelId = rowsByChannelId == null ? new LinkedHashMap<>() : rowsByChannelId;
        }

        boolean matches(String fingerprint) {
            return version == EPG_CHANNEL_CACHE_VERSION
                    && StartupParsedCatalogCache.safeEquals(snapshotFingerprint, fingerprint);
        }
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
        final String catalogFingerprint;
        final String permissionsFingerprint;
        final long lastStartupCacheHitMs;
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
            this(available, sizeBytes, updatedAtMs, expiresAtMs, 0L, expired, channelCount, vodCount, 0, 0, 0L, "", sourceUrl, "", deviceId, subject, permissions, "", "", "", 0L, 0L, "", "", hasAccessToken, false);
        }

        SnapshotStatus(boolean available, long sizeBytes, long updatedAtMs, long expiresAtMs, boolean expired, int channelCount, int vodCount, String sourceUrl, String deviceId, String subject, String permissions, boolean hasAccessToken, boolean hasLastGoodBackup) {
            this(available, sizeBytes, updatedAtMs, expiresAtMs, 0L, expired, channelCount, vodCount, 0, 0, 0L, "", sourceUrl, "", deviceId, subject, permissions, "", "", "", 0L, 0L, "", "", hasAccessToken, hasLastGoodBackup);
        }

        SnapshotStatus(boolean available, long sizeBytes, long updatedAtMs, long expiresAtMs, long generatedAtMs, boolean expired, int channelCount, int vodCount, int epgChannelCount, int epgProgramCount, long epgUntilMs, String schema, String sourceUrl, String sourceBaseUrl, String deviceId, String subject, String permissions, String payloadFingerprint, String catalogFingerprint, String permissionsFingerprint, long lastStartupCacheHitMs, long permissionsChangedAtMs, String verificationState, String verificationMessage, boolean hasAccessToken, boolean hasLastGoodBackup) {
            this(available, sizeBytes, updatedAtMs, expiresAtMs, generatedAtMs, expired, channelCount, vodCount, epgChannelCount, epgProgramCount, epgUntilMs, schema, sourceUrl, sourceBaseUrl, deviceId, subject, permissions, payloadFingerprint, catalogFingerprint, permissionsFingerprint, lastStartupCacheHitMs, permissionsChangedAtMs, verificationState, verificationMessage, hasAccessToken, hasLastGoodBackup, 0L, "", 0, 0, 0, 0);
        }

        SnapshotStatus(boolean available, long sizeBytes, long updatedAtMs, long expiresAtMs, long generatedAtMs, boolean expired, int channelCount, int vodCount, int epgChannelCount, int epgProgramCount, long epgUntilMs, String schema, String sourceUrl, String sourceBaseUrl, String deviceId, String subject, String permissions, String payloadFingerprint, String catalogFingerprint, String permissionsFingerprint, long lastStartupCacheHitMs, long permissionsChangedAtMs, String verificationState, String verificationMessage, boolean hasAccessToken, boolean hasLastGoodBackup, long lastRejectedAtMs, String lastRejectedReason, int lastRejectedPreviousChannels, int lastRejectedCandidateChannels, int lastRejectedPreviousTotal, int lastRejectedCandidateTotal) {
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
            this.catalogFingerprint = catalogFingerprint == null ? "" : catalogFingerprint;
            this.permissionsFingerprint = permissionsFingerprint == null ? "" : permissionsFingerprint;
            this.lastStartupCacheHitMs = lastStartupCacheHitMs;
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
