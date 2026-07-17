package com.drbep.tvplayer;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CatalogRepository {
    private static final String TAG = "CatalogRepository";
    private static final int FILTER_ALL = 0;
    private static final int FILTER_PLATFORM = 1;
    private static final int FILTER_CUSTOM_GROUP = 2;
    private static final int FILTER_VOD = 3;
    private static final int FILTER_VOD_ADULT = 4;
    private static final int FILTER_FAVORITES = 5;
    private static final int MAX_CATALOG_TEXT_FIELD_CHARS = 8192;
    private static final int MAX_CATALOG_URL_FIELD_CHARS = 8192;

    private final String baseUrl;
    private final HttpClient httpClient;
    private final CatalogSnapshotStore snapshotStore;
    private final boolean standaloneMode;

    CatalogRepository(String baseUrl) {
        this(baseUrl, null, false);
    }

    CatalogRepository(String baseUrl, CatalogSnapshotStore snapshotStore, boolean standaloneMode) {
        this.baseUrl = baseUrl;
        this.httpClient = new HttpClient();
        this.snapshotStore = snapshotStore;
        this.standaloneMode = standaloneMode;
    }

    CatalogLoadResult fetchCatalogChannels() throws Exception {
        if (standaloneMode) {
            if (snapshotStore == null) {
                throw new IllegalStateException("catalogo local no configurado");
            }
            String snapshotUrl = baseUrl + "/api/offline/snapshot";
            CatalogLoadResult cached = snapshotStore.loadStartupParsedCache(snapshotUrl);
            if (cached != null) {
                Log.w(TAG, "using parsed startup catalog cache channels=" + cached.channels.size());
                return cached;
            }
            CatalogLoadResult result = parseCatalogPayload(snapshotStore.loadStartupSnapshotObject(snapshotUrl), false);
            snapshotStore.saveStartupParsedCache(snapshotUrl, result);
            return result;
        }
        JSONObject payload = fetchRemoteCatalogPayload();
        return parseCatalogPayload(payload, true);
    }

    CatalogLoadResult fetchStartupLiveCatalogChannels() throws Exception {
        if (!standaloneMode) {
            return fetchCatalogChannels();
        }
        if (snapshotStore == null) {
            throw new IllegalStateException("catalogo local no configurado");
        }
        String snapshotUrl = baseUrl + "/api/offline/snapshot";
        CatalogLoadResult cached = snapshotStore.loadStartupParsedCache(snapshotUrl);
        if (cached != null) {
            Log.w(TAG, "using parsed startup catalog cache channels=" + cached.channels.size());
            return cached.withLoadSource("startup-cache");
        }
        return parseCatalogPayload(snapshotStore.loadStartupSnapshotObject(snapshotUrl), false, false, "startup-live");
    }

    CatalogLoadResult hydrateFullStartupCatalog() throws Exception {
        if (!standaloneMode) {
            return fetchCatalogChannels();
        }
        if (snapshotStore == null) {
            throw new IllegalStateException("catalogo local no configurado");
        }
        String snapshotUrl = baseUrl + "/api/offline/snapshot";
        CatalogLoadResult result = parseCatalogPayload(snapshotStore.loadSnapshotObject(), false, true, "startup-hydrate");
        snapshotStore.saveStartupParsedCache(snapshotUrl, result);
        return result;
    }

    CatalogLoadResult refreshSnapshotFromConfiguredUrl(String fallbackUrl) throws Exception {
        if (snapshotStore == null) {
            throw new IllegalStateException("catalogo local no configurado");
        }
        CatalogLoadResult result = parseCatalogPayload(snapshotStore.refreshFromConfiguredUrl(fallbackUrl), false);
        snapshotStore.saveStartupParsedCache(fallbackUrl, result);
        return result;
    }

    CatalogLoadResult fetchLocalSnapshotCatalog() throws Exception {
        if (snapshotStore == null) {
            throw new IllegalStateException("catalogo local no configurado");
        }
        return parseCatalogPayload(snapshotStore.loadSnapshotObject(), false);
    }

    CatalogLoadResult fetchLastKnownGoodSnapshotCatalog() throws Exception {
        if (snapshotStore == null) {
            throw new IllegalStateException("catalogo local no configurado");
        }
        return parseCatalogPayload(snapshotStore.loadLastKnownGoodSnapshotObject(), false);
    }

    private JSONObject fetchRemoteCatalogPayload() throws Exception {
        return httpClient.getJsonObject(
                baseUrl + "/api/channels/catalog?include_disabled=0",
                10000,
                15000,
                java.util.Collections.singletonMap("Accept", "application/json"),
                "cargando catalogo"
        );
    }

    private CatalogLoadResult parseCatalogPayload(JSONObject rawPayload, boolean appendRemoteVod) {
        return parseCatalogPayload(rawPayload, appendRemoteVod, true, appendRemoteVod ? "remote-full" : "full");
    }

    private CatalogLoadResult parseCatalogPayload(JSONObject rawPayload, boolean appendRemoteVod, boolean includeSnapshotVod, String loadSource) {
        long startMs = System.currentTimeMillis();
        JSONObject payload = normalizeSnapshotPayload(rawPayload);
        long normalizeMs = System.currentTimeMillis() - startMs;
        long permissionsStartMs = System.currentTimeMillis();
        OfflinePermissions offlinePermissions = parseOfflinePermissions(rawPayload);
        long permissionsMs = System.currentTimeMillis() - permissionsStartMs;
        JSONArray channelsArray = payload.optJSONArray("channels");
        if (channelsArray == null) {
            channelsArray = new JSONArray();
        }

        long liveStartMs = System.currentTimeMillis();
        List<ChannelItem> parsed = new ArrayList<>(channelsArray.length());
        for (int i = 0; i < channelsArray.length(); i++) {
            JSONObject channel = channelsArray.optJSONObject(i);
            if (channel == null) {
                continue;
            }

            String id = safeCatalogText(channel.optString("id", ""));
            if (id.isEmpty() || "null".equalsIgnoreCase(id)) {
                long numericId = channel.optLong("id", 0L);
                if (numericId > 0L) {
                    id = String.valueOf(numericId);
                }
            }

            String name = safeCatalogText(channel.optString("name", "Canal"));
            if ("0".equals(id) || id.isEmpty() || name.isEmpty()) {
                continue;
            }

            String logo = safeCatalogUrl(channel.optString("logo", ""));
            String sourceGroup = safeCatalogText(channel.optString("group", ""));
            String platformName = safeCatalogText(channel.optString("platform_name", ""));
            boolean plutoChannel = standaloneMode && platformName.toLowerCase(Locale.ROOT).contains("pluto");
            String playbackUrl = standaloneMode
                    ? firstNonEmpty(
                    plutoChannel ? safeCatalogUrl(channel.optString("url", "")) : safeCatalogUrl(channel.optString("play_url", "")),
                    plutoChannel ? safeCatalogUrl(channel.optString("source_url", "")) : "",
                    safeCatalogUrl(channel.optString("play_url", "")),
                    safeCatalogUrl(channel.optString("stream_url", "")),
                    safeCatalogUrl(channel.optString("url", "")),
                    safeCatalogUrl(channel.optString("source_url", ""))
            )
                    : baseUrl + "/live/" + id;
            if (standaloneMode) {
                playbackUrl = absolutizeUrl(playbackUrl);
            }
            String fallbackUrl = buildFallbackPlayUrl(id);
            boolean directPlayback = standaloneMode;
            if (standaloneMode && channel.has("direct_playback")) {
                directPlayback = channel.optBoolean("direct_playback", directPlayback);
            }
            String tvgId = firstNonEmpty(
                    safeCatalogText(channel.optString("custom_tvg_id", "")),
                    safeCatalogText(channel.optString("tvg_id", ""))
            );
            int platformId = (int) channel.optLong("platform_id", 0L);
            String playbackProfile = safeCatalogText(channel.optString("playback_profile", ""));
            int sortOrder = channel.has("sort_order") && !channel.isNull("sort_order")
                    ? channel.optInt("sort_order", Integer.MAX_VALUE)
                    : Integer.MAX_VALUE;
            if (sortOrder == Integer.MAX_VALUE) {
                sortOrder = channel.optInt("dial", i + 1);
            }
            int displayOrder = standaloneMode ? i + 1 : sortOrder;
            List<String> customGroups = new ArrayList<>();
            JSONArray groupsArray = channel.optJSONArray("custom_groups");
            if (groupsArray != null) {
                for (int j = 0; j < groupsArray.length(); j++) {
                    String groupName = safeCatalogText(groupsArray.optString(j, ""));
                    if (!groupName.isEmpty()) {
                        customGroups.add(groupName);
                    }
                }
            }

            // Real VOD items come from /api/vod/tivify; ignore linear false positives here.
            boolean isVod = false;
            boolean isAdultVod = false;

            ChannelItem item = new ChannelItem(
                    id,
                    name,
                    tvgId,
                    logo,
                    sourceGroup,
                    playbackUrl,
                    fallbackUrl,
                    i,
                    displayOrder,
                    isVod,
                    isAdultVod,
                    platformId,
                    platformName,
                    customGroups,
                    firstNonEmpty(safeCatalogText(channel.optString("drm_scheme", "")), safeCatalogText(channel.optString("drm_type", ""))),
                    resolveSecureDrmLicenseReference(id, channel),
                    "",
                    directPlayback,
                    playbackProfile
            );
            item.nowProgram = safeCatalogText(channel.optString("now_program", ""));
            item.nextProgram = safeCatalogText(channel.optString("next_program", ""));
            JSONObject groupOrder = channel.optJSONObject("group_order");
            if (groupOrder != null) {
                java.util.Iterator<String> keys = groupOrder.keys();
                while (keys.hasNext()) {
                    String groupName = keys.next();
                    int order = groupOrder.optInt(groupName, 0);
                    groupName = safeCatalogText(groupName);
                    if (!groupName.isEmpty() && order > 0) {
                        item.groupOrder.put(groupName.toLowerCase(Locale.ROOT), order);
                    }
                }
            }
            parsed.add(item);
        }
        long liveParseMs = System.currentTimeMillis() - liveStartMs;

        long vodStartMs = System.currentTimeMillis();
        if (includeSnapshotVod) {
            appendSnapshotVodItems(parsed, payload, offlinePermissions);
        }
        if (appendRemoteVod) {
            appendTivifyVodItems(parsed);
            appendRuntimeVodItems(parsed);
        }
        long vodParseMs = System.currentTimeMillis() - vodStartMs;

        long filtersStartMs = System.currentTimeMillis();
        long activePlatformId = payload.optLong("active_platform_id", 0L);
        StartupFilterConfig startupConfig = parseStartupFilterConfig(payload.optJSONObject("tv_player_startup"));
        List<ChannelFilter> filters = buildFiltersFromCatalog(parsed, activePlatformId, startupConfig, offlinePermissions);
        long filtersMs = System.currentTimeMillis() - filtersStartMs;
        int liveItems = 0;
        int vodItems = 0;
        for (ChannelItem item : parsed) {
            if (item == null) {
                continue;
            }
            if (item.isVod) {
                vodItems++;
            } else {
                liveItems++;
            }
        }
        long totalMs = System.currentTimeMillis() - startMs;
        Log.i(TAG, "catalog parsed channels=" + channelsArray.length()
                + " totalItems=" + parsed.size()
                + " liveItems=" + liveItems
                + " vodItems=" + vodItems
                + " filters=" + filters.size()
                + " appendRemoteVod=" + appendRemoteVod
                + " includeSnapshotVod=" + includeSnapshotVod
                + " source=" + loadSource
                + " normalizeMs=" + normalizeMs
                + " permissionsMs=" + permissionsMs
                + " liveParseMs=" + liveParseMs
                + " vodParseMs=" + vodParseMs
                + " filtersMs=" + filtersMs
                + " totalMs=" + totalMs);
        return new CatalogLoadResult(
                parsed,
                filters,
                resolveDefaultFilterKey(filters, startupConfig),
                offlinePermissions,
                !includeSnapshotVod,
                safeCatalogText(loadSource),
                liveItems,
                vodItems,
                normalizeMs,
                permissionsMs,
                liveParseMs,
                vodParseMs,
                filtersMs,
                totalMs
        );
    }

    private JSONObject normalizeSnapshotPayload(JSONObject rawPayload) {
        if (rawPayload == null) {
            return new JSONObject();
        }
        JSONObject catalog = rawPayload.optJSONObject("catalog");
        if (catalog != null) {
            copyIfMissing(rawPayload, catalog, "vod");
            copyIfMissing(rawPayload, catalog, "adult");
            copyIfMissing(rawPayload, catalog, "adult_vod");
            copyIfMissing(rawPayload, catalog, "tivify_vod");
            copyIfMissing(rawPayload, catalog, "tivify_adult");
            copyIfMissing(rawPayload, catalog, "runtime_movies");
            copyIfMissing(rawPayload, catalog, "runtime_vod");
            copyIfMissing(rawPayload, catalog, "movies");
            copyIfMissing(rawPayload, catalog, "movistar_movies");
            copyIfMissing(rawPayload, catalog, "movistar_series");
            return catalog;
        }
        return rawPayload;
    }

    private static void copyIfMissing(JSONObject source, JSONObject target, String key) {
        if (source == null || target == null || key == null || target.has(key) || !source.has(key)) {
            return;
        }
        try {
            target.put(key, source.opt(key));
        } catch (Exception ignored) {
        }
    }

    CatalogLoadResult fetchActiveChannels() throws Exception {
        JSONArray channelsArray = httpClient.getJsonArray(
                baseUrl + "/api/channels",
                10000,
                15000,
                java.util.Collections.singletonMap("Accept", "application/json"),
                "cargando canales"
        );
        List<ChannelItem> parsed = new ArrayList<>(channelsArray.length());
        for (int i = 0; i < channelsArray.length(); i++) {
            JSONObject channel = channelsArray.optJSONObject(i);
            if (channel == null) {
                continue;
            }

            String id = safeCatalogText(channel.optString("id", ""));
            String name = safeCatalogText(channel.optString("name", "Canal"));
            String logo = safeCatalogUrl(channel.optString("logo", ""));
            String playUrl = safeCatalogUrl(channel.optString("play_url", ""));
            String sourceGroup = safeCatalogText(channel.optString("group", ""));
            if (id.isEmpty() || playUrl.isEmpty()) {
                continue;
            }
            if (playUrl.startsWith("/")) {
                playUrl = baseUrl + playUrl;
            }

            boolean isVod = isLikelyVod("", name, "", sourceGroup, new ArrayList<>());
            boolean isAdultVod = isVod && isLikelyAdultVod(name, sourceGroup, new ArrayList<>());
            parsed.add(new ChannelItem(
                    id,
                    name,
                    firstNonEmpty(
                            safeCatalogText(channel.optString("custom_tvg_id", "")),
                            safeCatalogText(channel.optString("tvg_id", ""))
                    ),
                    logo,
                    sourceGroup,
                    playUrl,
                    buildFallbackPlayUrl(id),
                    i,
                    i + 1,
                    isVod || isAdultVod,
                    isAdultVod,
                    0,
                    "Plataforma activa",
                    new ArrayList<>(),
                    "",
                    "",
                    "",
                    false
            ));
        }

        List<ChannelFilter> filters = new ArrayList<>();
        filters.add(new ChannelFilter("all", "Todos", FILTER_ALL, 0, ""));
        return new CatalogLoadResult(parsed, filters, "all", new OfflinePermissions());
    }

    private void appendTivifyVodItems(List<ChannelItem> parsed) {
        try {
            JSONObject payload = httpClient.getJsonObject(
                    baseUrl + "/api/vod/tivify",
                    10000,
                    20000,
                    java.util.Collections.singletonMap("Accept", "application/json"),
                    "cargando vod"
            );
            appendVodArray(parsed, payload.optJSONArray("vod"), false);
            appendVodArray(parsed, payload.optJSONArray("adult"), true);
        } catch (Exception e) {
            // Live TV should still load even if Tivify VOD is temporarily unavailable.
        }
    }

    private void appendSnapshotVodItems(List<ChannelItem> parsed, JSONObject payload, OfflinePermissions offlinePermissions) {
        if (payload == null) {
            return;
        }
        if (offlinePermissions.allowsTivifyVod()) {
            appendVodArray(parsed, firstArray(payload, "vod", "tivify_vod"), false);
        }
        if (offlinePermissions.allowsTivifyAdultVod()) {
            appendVodArray(parsed, firstArray(payload, "adult", "adult_vod", "tivify_adult"), true);
        }
        if (offlinePermissions.allowsRuntimeVod()) {
            appendRuntimeVodArray(parsed, firstArray(payload, "runtime_movies", "movies", "runtime_vod"), "vod:runtime:movies", "Runtime Peliculas");
        }
        if (offlinePermissions.allowsMovistarVod()) {
            appendMovistarVodArray(parsed, firstArray(payload, "movistar_movies"), "vod:movistar:movies", "Movistar Peliculas");
            appendMovistarVodArray(parsed, firstArray(payload, "movistar_series"), "vod:movistar:series", "Movistar Series");
        }
    }

    private static JSONArray firstArray(JSONObject payload, String... keys) {
        if (payload == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            JSONArray array = payload.optJSONArray(key);
            if (array != null) {
                return array;
            }
        }
        return null;
    }

    private void appendVodArray(List<ChannelItem> parsed, JSONArray rows, boolean adult) {
        if (rows == null) {
            return;
        }
        int baseOrder = parsed.size() + 1;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) {
                continue;
            }
            String title = safeCatalogText(row.optString("title", "VOD"));
            if (title.isEmpty()) {
                title = "VOD";
            }
            String logo = safeCatalogUrl(row.optString("poster", ""));
            String group = firstNonEmpty(safeCatalogText(row.optString("group", "")), safeCatalogText(row.optString("carousel", "")), adult ? "VOD Adulto" : "VOD");
            String description = safeCatalogText(row.optString("description", ""));
            String year = safeCatalogText(row.optString("year", ""));
            long durationSeconds = parseLongSafe(firstNonEmpty(safeCatalogText(row.optString("duration", "")), safeCatalogText(row.optString("duration_seconds", ""))));
            boolean hasKeys = row.optBoolean("has_keys", false);
            JSONObject clearKeys = row.optJSONObject("clear_keys");
            if (!hasKeys && clearKeys != null && clearKeys.length() > 0) {
                hasKeys = true;
            }
            boolean secureDrm = row.optBoolean("secure_drm", false)
                    || !safeCatalogText(row.optString("drm_ref", "")).isEmpty()
                    || !safeCatalogText(row.optString("drm_reference", "")).isEmpty()
                    || !safeCatalogText(row.optString("secure_drm_ref", "")).isEmpty();
            if (secureDrm) {
                hasKeys = true;
            }
            String selectedUrl = hasKeys
                    ? firstNonEmpty(
                            safeCatalogUrl(row.optString("dash_url", "")),
                            safeCatalogUrl(row.optString("selected_url", "")),
                            safeCatalogUrl(row.optString("play_url", "")),
                            safeCatalogUrl(row.optString("url", "")),
                            safeCatalogUrl(row.optString("hls_url", "")),
                            safeCatalogUrl(row.optString("stream_url", ""))
                    )
                    : firstNonEmpty(
                            safeCatalogUrl(row.optString("selected_url", "")),
                            safeCatalogUrl(row.optString("dash_url", "")),
                            safeCatalogUrl(row.optString("hls_url", "")),
                            safeCatalogUrl(row.optString("play_url", "")),
                            safeCatalogUrl(row.optString("url", "")),
                            safeCatalogUrl(row.optString("stream_url", ""))
                    );
            if (selectedUrl.isEmpty()) {
                continue;
            }
            String drmRef = firstNonEmpty(
                    safeCatalogText(row.optString("drm_ref", "")),
                    safeCatalogText(row.optString("drm_reference", "")),
                    safeCatalogText(row.optString("secure_drm_ref", ""))
            );
            String drmLicenseUrl = "";
            if (hasKeys) {
                drmLicenseUrl = absolutizeUrl(firstNonEmpty(
                        safeCatalogUrl(row.optString("license_url", "")),
                        buildVodLicenseUrlFromRef(drmRef),
                        buildVodLicenseUrl(selectedUrl)
                ));
            }

            parsed.add(new ChannelItem(
                    buildVodItemId(selectedUrl, title, adult),
                    title,
                    "",
                    logo,
                    group,
                    selectedUrl,
                    "",
                    baseOrder + i,
                    baseOrder + i,
                    true,
                    adult,
                    0,
                    "Tivify VOD",
                    new ArrayList<>(),
                    hasKeys ? "clearkey" : "",
                    drmLicenseUrl,
                    adult ? "vod:tivify:adult" : "vod:tivify:general",
                    true,
                    description,
                    year,
                    durationSeconds
            ));
        }
    }

    private void appendMovistarVodArray(List<ChannelItem> parsed, JSONArray rows, String vodFilterKey, String platformName) {
        if (rows == null) {
            return;
        }
        int baseOrder = parsed.size() + 1;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) {
                continue;
            }
            String selectedUrl = firstNonEmpty(
                    safeCatalogUrl(row.optString("play_url", "")),
                    safeCatalogUrl(row.optString("playback_endpoint", "")),
                    safeCatalogUrl(row.optString("stream_url", "")),
                    safeCatalogUrl(row.optString("hls_url", "")),
                    safeCatalogUrl(row.optString("dash_url", ""))
            );
            if (selectedUrl.isEmpty()) {
                continue;
            }
            selectedUrl = absolutizeUrl(selectedUrl);
            String title = firstNonEmpty(
                    safeCatalogText(row.optString("title", "")),
                    safeCatalogText(row.optString("episode_title", "")),
                    "Movistar VOD"
            );
            String logo = safeCatalogUrl(row.optString("poster", ""));
            String group = firstNonEmpty(
                    safeCatalogText(row.optString("genre", "")),
                    safeCatalogText(row.optString("kind", "")),
                    platformName
            );
            String description = safeCatalogText(row.optString("description", ""));
            String year = safeCatalogText(row.optString("year", ""));
            long durationSeconds = parseLongSafe(safeCatalogText(row.optString("duration_minutes", ""))) * 60L;
            String drmScheme = firstNonEmpty(
                    safeCatalogText(row.optString("drm_type", "")),
                    safeCatalogText(row.optString("drm_scheme", ""))
            );
            String drmLicenseUrl = firstNonEmpty(
                    safeCatalogUrl(row.optString("license_url", "")),
                    safeCatalogUrl(row.optString("drm_license_url", ""))
            );
            drmLicenseUrl = absolutizeUrl(drmLicenseUrl);

            parsed.add(new ChannelItem(
                    buildVodItemId(selectedUrl, title, false),
                    title,
                    "",
                    logo,
                    group,
                    selectedUrl,
                    "",
                    baseOrder + i,
                    baseOrder + i,
                    true,
                    false,
                    0,
                    platformName,
                    new ArrayList<>(),
                    drmScheme,
                    drmLicenseUrl,
                    vodFilterKey,
                    true,
                    description,
                    year,
                    durationSeconds
            ));
        }
    }

    private void appendRuntimeVodItems(List<ChannelItem> parsed) {
        try {
            JSONObject payload = httpClient.getJsonObject(
                    baseUrl + "/api/vod/runtime",
                    10000,
                    20000,
                    java.util.Collections.singletonMap("Accept", "application/json"),
                    "cargando runtime vod"
            );
            appendRuntimeVodArray(parsed, payload.optJSONArray("movies"), "vod:runtime:movies", "Runtime Peliculas");
        } catch (Exception e) {
            // Live TV and Tivify VOD should still load even if Runtime VOD is temporarily unavailable.
        }
    }

    private void appendRuntimeVodArray(List<ChannelItem> parsed, JSONArray rows, String vodFilterKey, String platformName) {
        if (rows == null) {
            return;
        }
        int baseOrder = parsed.size() + 1;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) {
                continue;
            }
            String selectedUrl = firstNonEmpty(
                    safeCatalogUrl(row.optString("playback_endpoint", "")),
                    safeCatalogUrl(row.optString("stream_url", ""))
            );
            if (selectedUrl.isEmpty()) {
                continue;
            }
            selectedUrl = absolutizeUrl(selectedUrl);
            String title = safeCatalogText(row.optString("title", "Runtime VOD"));
            if (title.isEmpty()) {
                title = "Runtime VOD";
            }
            String logo = safeCatalogUrl(row.optString("poster", ""));
            String group = firstNonEmpty(safeCatalogText(row.optString("categories", "")), safeCatalogText(row.optString("kind", "")), platformName);
            String description = safeCatalogText(row.optString("description", ""));
            String year = safeCatalogText(row.optString("year", ""));
            long durationSeconds = parseLongSafe(firstNonEmpty(safeCatalogText(row.optString("duration", "")), safeCatalogText(row.optString("duration_seconds", ""))));

            parsed.add(new ChannelItem(
                    buildVodItemId(selectedUrl, title, false),
                    title,
                    "",
                    logo,
                    group,
                    selectedUrl,
                    "",
                    baseOrder + i,
                    baseOrder + i,
                    true,
                    false,
                    0,
                    platformName,
                    new ArrayList<>(),
                    "",
                    "",
                    vodFilterKey,
                    true,
                    description,
                    year,
                    durationSeconds
            ));
        }
    }

    List<ChannelFilter> buildFiltersFromCatalog(List<ChannelItem> parsed, long activePlatformId, StartupFilterConfig startupConfig, OfflinePermissions offlinePermissions) {
        LinkedHashMap<String, ChannelFilter> byKey = new LinkedHashMap<>();
        byKey.put("all", new ChannelFilter("all", "Todos", FILTER_ALL, 0, ""));
        byKey.put("favorites", new ChannelFilter("favorites", "Favoritos", FILTER_FAVORITES, 0, ""));

        Map<Integer, String> platformNames = new LinkedHashMap<>();
        Map<String, String> vodFilterLabels = new LinkedHashMap<>();
        Set<String> customGroupNames = new HashSet<>();
        boolean hasVod = false;
        boolean hasAdultVod = false;
        for (ChannelItem item : parsed) {
            if (item.isAdultVod) {
                hasAdultVod = true;
            } else if (item.isVod) {
                hasVod = true;
            }
            String vodFilterKey = safeCatalogText(item.vodFilterKey);
            if (item.isVod && !vodFilterKey.isEmpty() && !vodFilterLabels.containsKey(vodFilterKey)) {
                vodFilterLabels.put(vodFilterKey, buildVodFilterLabel(vodFilterKey, item.platformName, item.isAdultVod));
            }
            if (item.platformId > 0 && !platformNames.containsKey(item.platformId)) {
                String platformName = safeCatalogText(item.platformName);
                if (platformName.isEmpty()) {
                    platformName = "ID " + item.platformId;
                }
                platformNames.put(item.platformId, platformName);
            }
            for (String groupName : item.customGroups) {
                String trimmed = safeCatalogText(groupName);
                if (!trimmed.isEmpty()) {
                    customGroupNames.add(trimmed);
                }
            }
        }

        if (activePlatformId > 0 && shouldExposePlatformFilter((int) activePlatformId, offlinePermissions)) {
            int activeId = (int) activePlatformId;
            String activeName = platformNames.containsKey(activeId) ? platformNames.get(activeId) : ("ID " + activeId);
            byKey.put("platform:" + activeId, new ChannelFilter("platform:" + activeId, "Plataforma activa: " + activeName, FILTER_PLATFORM, activeId, ""));
        }

        List<Integer> platformIds = new ArrayList<>(platformNames.keySet());
        Collections.sort(platformIds);
        for (int platformId : platformIds) {
            if (!shouldExposePlatformFilter(platformId, offlinePermissions)) {
                continue;
            }
            String key = "platform:" + platformId;
            if (byKey.containsKey(key)) {
                continue;
            }
            byKey.put(key, new ChannelFilter(key, "Plataforma: " + platformNames.get(platformId), FILTER_PLATFORM, platformId, ""));
        }

        List<String> groupNames = new ArrayList<>(customGroupNames);
        groupNames.sort(String::compareToIgnoreCase);
        for (String groupName : groupNames) {
            byKey.put("custom-group:" + groupName.toLowerCase(Locale.ROOT), new ChannelFilter("custom-group:" + groupName.toLowerCase(Locale.ROOT), "Grupo: " + groupName, FILTER_CUSTOM_GROUP, 0, groupName));
        }
        for (Map.Entry<String, String> entry : vodFilterLabels.entrySet()) {
            String key = entry.getKey();
            int type = "vod:tivify:adult".equals(key) ? FILTER_VOD_ADULT : FILTER_VOD;
            byKey.put(key, new ChannelFilter(key, entry.getValue(), type, 0, ""));
        }
        boolean hasSpecificVodFilters = !vodFilterLabels.isEmpty();
        if (hasVod && !hasSpecificVodFilters) {
            byKey.put("vod", new ChannelFilter("vod", "VOD", FILTER_VOD, 0, ""));
        }
        boolean hasSpecificAdultVodFilter = vodFilterLabels.containsKey("vod:tivify:adult");
        if (hasAdultVod && !hasSpecificAdultVodFilter) {
            byKey.put("vod-adult", new ChannelFilter("vod-adult", "VOD Adulto", FILTER_VOD_ADULT, 0, ""));
        }

        List<ChannelFilter> filters = new ArrayList<>(byKey.values());
        if (startupConfig == null || startupConfig.enabledFilterKeys.isEmpty()) {
            return filters;
        }
        List<ChannelFilter> filtered = new ArrayList<>();
        for (ChannelFilter filter : filters) {
            if ("favorites".equals(filter.key) || startupConfig.enabledFilterKeys.contains(filter.key)) {
                filtered.add(filter);
            }
        }
        if (filtered.isEmpty() || containsOnlyFavoritesFilter(filtered)) {
            return filters;
        }
        return filtered;
    }

    private boolean shouldExposePlatformFilter(int platformId, OfflinePermissions offlinePermissions) {
        if (platformId <= 0 || offlinePermissions == null || !standaloneMode) {
            return platformId > 0;
        }
        if (offlinePermissions.allowedPlatformIds.isEmpty()) {
            return true;
        }
        return offlinePermissions.allowedPlatformIds.contains(platformId);
    }

    private static boolean containsOnlyFavoritesFilter(List<ChannelFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return false;
        }
        for (ChannelFilter filter : filters) {
            if (filter != null && !"favorites".equals(filter.key)) {
                return false;
            }
        }
        return true;
    }

    private String buildFallbackPlayUrl(String id) {
        String safeId = safeCatalogText(id);
        if (safeId.isEmpty()) {
            return "";
        }
        try {
            Long.parseLong(safeId);
            return baseUrl + "/proxy/manifest/" + safeId;
        } catch (Exception ignored) {
            return "";
        }
    }

    private String absolutizeUrl(String url) {
        String trimmed = safeCatalogUrl(url);
        if (trimmed.startsWith("/")) {
            return baseUrl + trimmed;
        }
        return trimmed;
    }

    private static String buildVodFilterLabel(String key, String platformName, boolean adult) {
        if ("vod:tivify:general".equals(key)) {
            return "Tivify VOD";
        }
        if ("vod:tivify:adult".equals(key)) {
            return "Tivify Adulto";
        }
        if ("vod:runtime:movies".equals(key)) {
            return "Runtime Peliculas";
        }
        if ("vod:movistar:movies".equals(key)) {
            return "Movistar Peliculas";
        }
        if ("vod:movistar:series".equals(key)) {
            return "Movistar Series";
        }
        String fallback = safeCatalogText(platformName);
        if (!fallback.isEmpty()) {
            return fallback;
        }
        return adult ? "VOD Adulto" : "VOD";
    }

    private String buildVodLicenseUrl(String selectedUrl) {
        String token = Base64.encodeToString(selectedUrl.getBytes(StandardCharsets.UTF_8), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        return baseUrl + "/api/vod/tivify/clearkey?u=" + token;
    }

    private String buildVodLicenseUrlFromRef(String drmRef) {
        String ref = safeCatalogText(drmRef);
        if (ref.isEmpty()) {
            return "";
        }
        return baseUrl + "/api/vod/tivify/clearkey?r=" + Uri.encode(ref);
    }

    private static String resolveSecureDrmLicenseReference(String channelId, JSONObject channel) {
        if (channel == null) {
            return "";
        }
        String explicitReference = firstNonEmpty(
                safeCatalogText(channel.optString("drm_ref", "")),
                safeCatalogText(channel.optString("drm_reference", "")),
                safeCatalogText(channel.optString("secure_drm_ref", ""))
        );
        if (!explicitReference.isEmpty()) {
            return "drbep-secure-stream:" + safeCatalogText(explicitReference);
        }
        JSONObject clearKeys = channel.optJSONObject("clearkey");
        if (clearKeys != null && clearKeys.length() > 0) {
            return "drbep-secure-stream:" + safeCatalogText(channelId);
        }
        return firstNonEmpty(
                safeCatalogUrl(channel.optString("drm_license_url", "")),
                safeCatalogUrl(channel.optString("license_url", ""))
        );
    }

    private static String buildVodItemId(String selectedUrl, String title, boolean adult) {
        String source = (adult ? "adult:" : "vod:") + firstNonEmpty(selectedUrl, title);
        return "vod-" + UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String trimmed = safeCatalogText(value);
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return "";
    }

    private static long parseLongSafe(String value) {
        String trimmed = safeCatalogText(value);
        if (trimmed.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(trimmed);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String sanitizeFilterKey(String value) {
        return safeCatalogText(value).toLowerCase(Locale.ROOT);
    }

    private static String safeCatalogText(String value) {
        return safeTrimBounded(value, MAX_CATALOG_TEXT_FIELD_CHARS);
    }

    private static String safeCatalogUrl(String value) {
        return safeTrimBounded(value, MAX_CATALOG_URL_FIELD_CHARS);
    }

    private static String safeTrimBounded(String value, int maxChars) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        // Avoid String.trim() on malformed catalog fields containing huge JSON
        // fragments; trim copies the whole string and can OOM on Fire TV.
        if (value.length() > maxChars) {
            return "";
        }
        return value.trim();
    }

    private static StartupFilterConfig parseStartupFilterConfig(JSONObject payload) {
        StartupFilterConfig config = new StartupFilterConfig();
        if (payload == null) {
            return config;
        }
        JSONArray enabled = payload.optJSONArray("enabled_filter_keys");
        if (enabled != null) {
            for (int i = 0; i < enabled.length(); i++) {
                String key = sanitizeFilterKey(enabled.optString(i, ""));
                if (!key.isEmpty() && !config.enabledFilterKeys.contains(key)) {
                    config.enabledFilterKeys.add(key);
                }
            }
        }
        config.defaultFilterKey = sanitizeFilterKey(payload.optString("default_filter_key", ""));
        return config;
    }

    private static String resolveDefaultFilterKey(List<ChannelFilter> filters, StartupFilterConfig config) {
        if (filters == null || filters.isEmpty()) {
            return "all";
        }
        if (config != null) {
            String desired = sanitizeFilterKey(config.defaultFilterKey);
            if (!desired.isEmpty() && !isHeavyStartupFilterKey(desired)) {
                for (ChannelFilter filter : filters) {
                    if (desired.equals(filter.key)) {
                        return filter.key;
                    }
                }
            }
            if (!config.enabledFilterKeys.isEmpty()) {
                for (String enabledKey : config.enabledFilterKeys) {
                    if (isHeavyStartupFilterKey(enabledKey)) {
                        continue;
                    }
                    for (ChannelFilter filter : filters) {
                        if (enabledKey.equals(filter.key)) {
                            return filter.key;
                        }
                    }
                }
            }
        }
        for (ChannelFilter filter : filters) {
            if (filter != null && filter.type == FILTER_PLATFORM) {
                return filter.key;
            }
        }
        for (ChannelFilter filter : filters) {
            if (filter != null && filter.type == FILTER_CUSTOM_GROUP) {
                return filter.key;
            }
        }
        for (ChannelFilter filter : filters) {
            if (filter != null && filter.type != FILTER_ALL && filter.type != FILTER_FAVORITES) {
                return filter.key;
            }
        }
        return filters.get(0).key;
    }

    private static boolean isHeavyStartupFilterKey(String key) {
        return key == null || key.trim().isEmpty() || "all".equals(key.trim());
    }

    private static OfflinePermissions parseOfflinePermissions(JSONObject rawPayload) {
        OfflinePermissions permissions = new OfflinePermissions();
        if (rawPayload == null) {
            return permissions;
        }
        JSONObject payload = rawPayload.optJSONObject("permissions");
        if (payload == null) {
            return permissions;
        }
        permissions.liveEnabled = payload.optBoolean("live", true);
        permissions.vodEnabled = payload.optBoolean("vod", true);
        permissions.tivifyAdultEnabled = payload.optBoolean("tivify_adult", true);
        permissions.runtimeEnabled = payload.optBoolean("runtime", true);
        permissions.movistarVodEnabled = payload.optBoolean("movistar_vod", true);
        permissions.canViewRecordings = payload.optBoolean("recordings_view", true);
        permissions.canScheduleRecordings = payload.optBoolean("recordings_schedule", true);
        permissions.canDeleteRecordings = payload.optBoolean("recordings_delete", false);
        JSONArray platformIds = payload.optJSONArray("platform_ids");
        if (platformIds != null) {
            for (int i = 0; i < platformIds.length(); i++) {
                int platformId = platformIds.optInt(i, 0);
                if (platformId > 0) {
                    permissions.allowedPlatformIds.add(platformId);
                }
            }
        }
        permissions.protectAdultVod = payload.optBoolean("parental_vod_adult", false);
        permissions.protectedFilterKeys.addAll(parseStringArray(payload.optJSONArray("parental_filter_keys")));
        permissions.protectedChannelIds.addAll(parseStringArray(payload.optJSONArray("parental_channel_ids")));
        permissions.protectedGroupNames.addAll(parseStringArray(payload.optJSONArray("parental_group_names")));
        return permissions;
    }

    private static Set<String> parseStringArray(JSONArray values) {
        Set<String> output = new HashSet<>();
        if (values == null) {
            return output;
        }
        for (int i = 0; i < values.length(); i++) {
            String value = safeLower(values.optString(i, ""));
            if (!value.isEmpty()) {
                output.add(value);
            }
        }
        return output;
    }

    private static boolean isLikelyVod(String externalId, String name, String tvgId, String groupTitle, List<String> customGroups) {
        String normalizedName = safeLower(name);
        String normalizedTvgId = safeLower(tvgId);
        String normalizedGroup = safeLower(groupTitle);
        if (containsVodToken(normalizedName) || containsVodToken(normalizedTvgId) || containsVodToken(normalizedGroup)) {
            return true;
        }
        if (customGroups != null) {
            for (String customGroup : customGroups) {
                if (containsVodToken(safeLower(customGroup))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isLikelyAdultVod(String name, String groupTitle, List<String> customGroups) {
        if (containsAdultToken(safeLower(name)) || containsAdultToken(safeLower(groupTitle))) {
            return true;
        }
        if (customGroups != null) {
            for (String customGroup : customGroups) {
                if (containsAdultToken(safeLower(customGroup))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsAdultToken(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.contains("adult")
                || text.contains("xxx")
                || text.contains("porno")
                || text.contains("erot")
                || text.contains("sex")
                || text.contains("saten");
    }

    private static boolean containsVodToken(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        if (text.contains("vodafone")) {
            return false;
        }
        if (text.equals("vod")) {
            return true;
        }
        return text.contains(" vod ")
                || text.startsWith("vod ")
                || text.endsWith(" vod")
                || text.contains("vod/")
                || text.contains("/vod")
                || text.contains("vod-")
                || text.contains("-vod")
                || text.contains("_vod")
                || text.contains("vod_")
                || text.contains("vod:");
    }

    private static String safeLower(String value) {
        return safeCatalogText(value).toLowerCase(Locale.ROOT);
    }
}

final class ChannelItem implements Serializable {
    private static final long serialVersionUID = 1L;

    final String id;
    final String name;
    final String tvgId;
    final String logoUrl;
    final String group;
    final String playUrl;
    final String fallbackPlayUrl;
    final int originalOrder;
    final int dashboardOrder;
    final boolean isVod;
    final boolean isAdultVod;
    final int platformId;
    final String platformName;
    final List<String> customGroups;
    final Map<String, Integer> groupOrder;
    final String drmScheme;
    final String drmLicenseUrl;
    final String vodFilterKey;
    final boolean directPlayback;
    final String playbackProfile;
    final String vodDescription;
    final String vodYear;
    final long vodDurationSeconds;
    boolean favorite;
    String nowProgram;
    String nextProgram;

    ChannelItem(String id, String name, String tvgId, String logoUrl, String group, String playUrl, String fallbackPlayUrl, int originalOrder, int dashboardOrder, boolean isVod, boolean isAdultVod, int platformId, String platformName, List<String> customGroups, String drmScheme, String drmLicenseUrl, String vodFilterKey, boolean directPlayback) {
        this(id, name, tvgId, logoUrl, group, playUrl, fallbackPlayUrl, originalOrder, dashboardOrder, isVod, isAdultVod, platformId, platformName, customGroups, drmScheme, drmLicenseUrl, vodFilterKey, directPlayback, "", "", 0L, "");
    }

    ChannelItem(String id, String name, String tvgId, String logoUrl, String group, String playUrl, String fallbackPlayUrl, int originalOrder, int dashboardOrder, boolean isVod, boolean isAdultVod, int platformId, String platformName, List<String> customGroups, String drmScheme, String drmLicenseUrl, String vodFilterKey, boolean directPlayback, String vodDescription, String vodYear, long vodDurationSeconds) {
        this(id, name, tvgId, logoUrl, group, playUrl, fallbackPlayUrl, originalOrder, dashboardOrder, isVod, isAdultVod, platformId, platformName, customGroups, drmScheme, drmLicenseUrl, vodFilterKey, directPlayback, vodDescription, vodYear, vodDurationSeconds, "");
    }

    ChannelItem(String id, String name, String tvgId, String logoUrl, String group, String playUrl, String fallbackPlayUrl, int originalOrder, int dashboardOrder, boolean isVod, boolean isAdultVod, int platformId, String platformName, List<String> customGroups, String drmScheme, String drmLicenseUrl, String vodFilterKey, boolean directPlayback, String playbackProfile) {
        this(id, name, tvgId, logoUrl, group, playUrl, fallbackPlayUrl, originalOrder, dashboardOrder, isVod, isAdultVod, platformId, platformName, customGroups, drmScheme, drmLicenseUrl, vodFilterKey, directPlayback, "", "", 0L, playbackProfile);
    }

    ChannelItem(String id, String name, String tvgId, String logoUrl, String group, String playUrl, String fallbackPlayUrl, int originalOrder, int dashboardOrder, boolean isVod, boolean isAdultVod, int platformId, String platformName, List<String> customGroups, String drmScheme, String drmLicenseUrl, String vodFilterKey, boolean directPlayback, String vodDescription, String vodYear, long vodDurationSeconds, String playbackProfile) {
        this.id = safeText(id);
        this.name = safeText(name);
        this.tvgId = safeText(tvgId);
        this.logoUrl = safeUrl(logoUrl);
        this.group = safeText(group);
        this.playUrl = safeUrl(playUrl);
        this.fallbackPlayUrl = safeUrl(fallbackPlayUrl);
        this.originalOrder = originalOrder;
        this.dashboardOrder = dashboardOrder;
        this.isVod = isVod;
        this.isAdultVod = isAdultVod;
        this.platformId = platformId;
        this.platformName = safeText(platformName);
        this.customGroups = customGroups;
        this.groupOrder = new LinkedHashMap<>();
        this.drmScheme = safeText(drmScheme);
        this.drmLicenseUrl = safeUrl(drmLicenseUrl);
        this.vodFilterKey = safeText(vodFilterKey).toLowerCase(Locale.ROOT);
        this.directPlayback = directPlayback;
        this.playbackProfile = safeText(playbackProfile).toLowerCase(Locale.ROOT);
        this.vodDescription = safeText(vodDescription);
        this.vodYear = safeText(vodYear);
        this.vodDurationSeconds = Math.max(0L, vodDurationSeconds);
        this.nowProgram = "";
        this.nextProgram = "";
    }

    private static String safeText(String value) {
        return safeTrimBounded(value, 8192);
    }

    private static String safeUrl(String value) {
        return safeTrimBounded(value, 8192);
    }

    private static String safeTrimBounded(String value, int maxChars) {
        if (value == null || value.isEmpty() || value.length() > maxChars) {
            return "";
        }
        return value.trim();
    }
}

final class ChannelFilter implements Serializable {
    private static final long serialVersionUID = 1L;

    final String key;
    final String label;
    final int type;
    final int platformId;
    final String groupName;

    ChannelFilter(String key, String label, int type, int platformId, String groupName) {
        this.key = key;
        this.label = label;
        this.type = type;
        this.platformId = platformId;
        this.groupName = groupName;
    }
}

final class StartupFilterConfig {
    final List<String> enabledFilterKeys = new ArrayList<>();
    String defaultFilterKey = "";
}

final class OfflinePermissions implements Serializable {
    private static final long serialVersionUID = 1L;

    boolean liveEnabled = true;
    boolean vodEnabled = true;
    boolean tivifyAdultEnabled = true;
    boolean runtimeEnabled = true;
    boolean movistarVodEnabled = true;
    boolean canViewRecordings = true;
    boolean canScheduleRecordings = true;
    boolean canDeleteRecordings = false;
    boolean protectAdultVod = false;
    final Set<Integer> allowedPlatformIds = new HashSet<>();
    final Set<String> protectedFilterKeys = new HashSet<>();
    final Set<String> protectedChannelIds = new HashSet<>();
    final Set<String> protectedGroupNames = new HashSet<>();

    boolean allowsLiveChannel(int platformId) {
        if (!liveEnabled) {
            return false;
        }
        if (allowedPlatformIds.isEmpty() || platformId <= 0) {
            return true;
        }
        return allowedPlatformIds.contains(platformId);
    }

    boolean allowsTivifyVod() {
        return vodEnabled;
    }

    boolean allowsTivifyAdultVod() {
        return vodEnabled && tivifyAdultEnabled;
    }

    boolean allowsRuntimeVod() {
        return runtimeEnabled;
    }

    boolean allowsMovistarVod() {
        return vodEnabled && movistarVodEnabled;
    }

    boolean hasParentalRules() {
        return protectAdultVod
                || !protectedFilterKeys.isEmpty()
                || !protectedChannelIds.isEmpty()
                || !protectedGroupNames.isEmpty();
    }

    boolean isProtectedFilter(ChannelFilter filter) {
        if (filter == null) {
            return false;
        }
        String filterKey = normalizeSafe(filter.key);
        if (!filterKey.isEmpty() && protectedFilterKeys.contains(filterKey)) {
            return true;
        }
        if (protectAdultVod && filter.type == 4) {
            return true;
        }
        if (filter.type == 2) {
            String groupName = normalizeSafe(filter.groupName);
            return !groupName.isEmpty() && protectedGroupNames.contains(groupName);
        }
        return false;
    }

    boolean isProtectedItem(ChannelItem item) {
        if (item == null) {
            return false;
        }
        String itemId = normalizeSafe(item.id);
        if (!itemId.isEmpty() && protectedChannelIds.contains(itemId)) {
            return true;
        }
        if (protectAdultVod && item.isAdultVod) {
            return true;
        }
        if (matchesProtectedGroup(item.group)) {
            return true;
        }
        if (item.customGroups != null) {
            for (String groupName : item.customGroups) {
                if (matchesProtectedGroup(groupName)) {
                    return true;
                }
            }
        }
        String vodFilterKey = normalizeSafe(item.vodFilterKey);
        return !vodFilterKey.isEmpty() && protectedFilterKeys.contains(vodFilterKey);
    }

    private boolean matchesProtectedGroup(String value) {
        String normalized = normalizeSafe(value);
        return !normalized.isEmpty() && protectedGroupNames.contains(normalized);
    }

    private static String normalizeSafe(String value) {
        if (value == null || value.isEmpty() || value.length() > 8192) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

final class CatalogLoadResult implements Serializable {
    private static final long serialVersionUID = 1L;

    final List<ChannelItem> channels;
    final List<ChannelFilter> filters;
    final String defaultFilterKey;
    final OfflinePermissions offlinePermissions;
    final boolean liveOnly;
    final String loadSource;
    final int liveItems;
    final int vodItems;
    final long normalizeMs;
    final long permissionsMs;
    final long liveParseMs;
    final long vodParseMs;
    final long filtersMs;
    final long totalParseMs;

    CatalogLoadResult(List<ChannelItem> channels, List<ChannelFilter> filters, String defaultFilterKey) {
        this(channels, filters, defaultFilterKey, new OfflinePermissions());
    }

    CatalogLoadResult(List<ChannelItem> channels, List<ChannelFilter> filters, String defaultFilterKey, OfflinePermissions offlinePermissions) {
        this(channels, filters, defaultFilterKey, offlinePermissions, false, "", 0, 0, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    CatalogLoadResult(
            List<ChannelItem> channels,
            List<ChannelFilter> filters,
            String defaultFilterKey,
            OfflinePermissions offlinePermissions,
            boolean liveOnly,
            String loadSource,
            int liveItems,
            int vodItems,
            long normalizeMs,
            long permissionsMs,
            long liveParseMs,
            long vodParseMs,
            long filtersMs,
            long totalParseMs
    ) {
        this.channels = channels == null ? new ArrayList<>() : channels;
        this.filters = filters == null ? new ArrayList<>() : filters;
        this.defaultFilterKey = defaultFilterKey == null || defaultFilterKey.trim().isEmpty() ? "all" : defaultFilterKey.trim();
        this.offlinePermissions = offlinePermissions == null ? new OfflinePermissions() : offlinePermissions;
        this.liveOnly = liveOnly;
        this.loadSource = loadSource == null ? "" : loadSource.trim();
        this.liveItems = liveItems;
        this.vodItems = vodItems;
        this.normalizeMs = Math.max(0L, normalizeMs);
        this.permissionsMs = Math.max(0L, permissionsMs);
        this.liveParseMs = Math.max(0L, liveParseMs);
        this.vodParseMs = Math.max(0L, vodParseMs);
        this.filtersMs = Math.max(0L, filtersMs);
        this.totalParseMs = Math.max(0L, totalParseMs);
    }

    CatalogLoadResult withLoadSource(String source) {
        return new CatalogLoadResult(
                channels,
                filters,
                defaultFilterKey,
                offlinePermissions,
                liveOnly,
                source,
                liveItems,
                vodItems,
                normalizeMs,
                permissionsMs,
                liveParseMs,
                vodParseMs,
                filtersMs,
                totalParseMs
        );
    }
}
