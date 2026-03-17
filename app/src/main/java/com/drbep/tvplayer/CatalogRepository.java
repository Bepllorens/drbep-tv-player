package com.drbep.tvplayer;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

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
    private static final int FILTER_ALL = 0;
    private static final int FILTER_PLATFORM = 1;
    private static final int FILTER_CUSTOM_GROUP = 2;
    private static final int FILTER_VOD = 3;
    private static final int FILTER_VOD_ADULT = 4;

    private final String baseUrl;
    private final HttpClient httpClient;

    CatalogRepository(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new HttpClient();
    }

    CatalogLoadResult fetchCatalogChannels() throws Exception {
        JSONObject payload = httpClient.getJsonObject(
                baseUrl + "/api/channels/catalog?include_disabled=0",
                10000,
                15000,
                java.util.Collections.singletonMap("Accept", "application/json"),
                "cargando catalogo"
        );
        JSONArray channelsArray = payload.optJSONArray("channels");
        if (channelsArray == null) {
            channelsArray = new JSONArray();
        }

        List<ChannelItem> parsed = new ArrayList<>(channelsArray.length());
        for (int i = 0; i < channelsArray.length(); i++) {
            JSONObject channel = channelsArray.optJSONObject(i);
            if (channel == null) {
                continue;
            }

            String id = channel.optString("id", "").trim();
            if (id.isEmpty() || "null".equalsIgnoreCase(id)) {
                long numericId = channel.optLong("id", 0L);
                if (numericId > 0L) {
                    id = String.valueOf(numericId);
                }
            }

            String name = channel.optString("name", "Canal").trim();
            if ("0".equals(id) || id.isEmpty() || name.isEmpty()) {
                continue;
            }

            String logo = channel.optString("logo", "").trim();
            String sourceGroup = channel.optString("group", "").trim();
            String playbackUrl = baseUrl + "/live/" + id;
            String fallbackUrl = buildFallbackPlayUrl(id);
            String tvgId = channel.optString("tvg_id", "").trim();
            int platformId = (int) channel.optLong("platform_id", 0L);
            String platformName = channel.optString("platform_name", "").trim();
            int sortOrder = channel.optInt("sort_order", Integer.MAX_VALUE);
            if (sortOrder <= 0) {
                sortOrder = channel.optInt("dial", i + 1);
            }
            List<String> customGroups = new ArrayList<>();
            JSONArray groupsArray = channel.optJSONArray("custom_groups");
            if (groupsArray != null) {
                for (int j = 0; j < groupsArray.length(); j++) {
                    String groupName = groupsArray.optString(j, "").trim();
                    if (!groupName.isEmpty()) {
                        customGroups.add(groupName);
                    }
                }
            }

            // Real VOD items come from /api/vod/tivify; ignore linear false positives here.
            boolean isVod = false;
            boolean isAdultVod = false;

            parsed.add(new ChannelItem(
                    id,
                    name,
                    logo,
                    sourceGroup,
                    playbackUrl,
                    fallbackUrl,
                    i,
                    sortOrder,
                    isVod,
                    isAdultVod,
                    platformId,
                    platformName,
                    customGroups,
                    "",
                    "",
                    false
            ));
        }

        appendTivifyVodItems(parsed);

        long activePlatformId = payload.optLong("active_platform_id", 0L);
        StartupFilterConfig startupConfig = parseStartupFilterConfig(payload.optJSONObject("tv_player_startup"));
        List<ChannelFilter> filters = buildFiltersFromCatalog(parsed, activePlatformId, startupConfig);
        return new CatalogLoadResult(parsed, filters, resolveDefaultFilterKey(filters, startupConfig));
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

            String id = channel.optString("id", "").trim();
            String name = channel.optString("name", "Canal").trim();
            String logo = channel.optString("logo", "").trim();
            String playUrl = channel.optString("play_url", "").trim();
            String sourceGroup = channel.optString("group", "").trim();
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
                    false
            ));
        }

        List<ChannelFilter> filters = new ArrayList<>();
        filters.add(new ChannelFilter("all", "Todos", FILTER_ALL, 0, ""));
        return new CatalogLoadResult(parsed, filters, "all");
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
            String selectedUrl = firstNonEmpty(
                    row.optString("selected_url", ""),
                    row.optString("dash_url", ""),
                    row.optString("hls_url", "")
            );
            if (selectedUrl.isEmpty()) {
                continue;
            }
            String title = row.optString("title", "VOD").trim();
            if (title.isEmpty()) {
                title = "VOD";
            }
            String logo = row.optString("poster", "").trim();
            String group = firstNonEmpty(row.optString("group", ""), row.optString("carousel", ""), adult ? "VOD Adulto" : "VOD");
            boolean hasKeys = row.optBoolean("has_keys", false);
            JSONObject clearKeys = row.optJSONObject("clear_keys");
            if (!hasKeys && clearKeys != null && clearKeys.length() > 0) {
                hasKeys = true;
            }

            parsed.add(new ChannelItem(
                    buildVodItemId(selectedUrl, title, adult),
                    title,
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
                    hasKeys ? buildVodLicenseUrl(selectedUrl) : "",
                    true
            ));
        }
    }

    private List<ChannelFilter> buildFiltersFromCatalog(List<ChannelItem> parsed, long activePlatformId, StartupFilterConfig startupConfig) {
        LinkedHashMap<String, ChannelFilter> byKey = new LinkedHashMap<>();
        byKey.put("all", new ChannelFilter("all", "Todos", FILTER_ALL, 0, ""));

        Map<Integer, String> platformNames = new LinkedHashMap<>();
        Set<String> customGroupNames = new HashSet<>();
        boolean hasVod = false;
        boolean hasAdultVod = false;
        for (ChannelItem item : parsed) {
            if (item.isAdultVod) {
                hasAdultVod = true;
            } else if (item.isVod) {
                hasVod = true;
            }
            if (item.platformId > 0 && !platformNames.containsKey(item.platformId)) {
                String platformName = item.platformName == null ? "" : item.platformName.trim();
                if (platformName.isEmpty()) {
                    platformName = "ID " + item.platformId;
                }
                platformNames.put(item.platformId, platformName);
            }
            for (String groupName : item.customGroups) {
                String trimmed = groupName == null ? "" : groupName.trim();
                if (!trimmed.isEmpty()) {
                    customGroupNames.add(trimmed);
                }
            }
        }

        if (activePlatformId > 0) {
            int activeId = (int) activePlatformId;
            String activeName = platformNames.containsKey(activeId) ? platformNames.get(activeId) : ("ID " + activeId);
            byKey.put("platform:" + activeId, new ChannelFilter("platform:" + activeId, "Plataforma activa: " + activeName, FILTER_PLATFORM, activeId, ""));
        }

        List<Integer> platformIds = new ArrayList<>(platformNames.keySet());
        Collections.sort(platformIds);
        for (int platformId : platformIds) {
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
        if (hasVod) {
            byKey.put("vod", new ChannelFilter("vod", "VOD", FILTER_VOD, 0, ""));
        }
        if (hasAdultVod) {
            byKey.put("vod-adult", new ChannelFilter("vod-adult", "VOD Adulto", FILTER_VOD_ADULT, 0, ""));
        }

        List<ChannelFilter> filters = new ArrayList<>(byKey.values());
        if (startupConfig == null || startupConfig.enabledFilterKeys.isEmpty()) {
            return filters;
        }
        List<ChannelFilter> filtered = new ArrayList<>();
        for (ChannelFilter filter : filters) {
            if (startupConfig.enabledFilterKeys.contains(filter.key)) {
                filtered.add(filter);
            }
        }
        if (filtered.isEmpty()) {
            return filters;
        }
        return filtered;
    }

    private String buildFallbackPlayUrl(String id) {
        if (id == null || id.trim().isEmpty()) {
            return "";
        }
        try {
            Long.parseLong(id.trim());
            return baseUrl + "/proxy/manifest/" + id.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String buildVodLicenseUrl(String selectedUrl) {
        String token = Base64.encodeToString(selectedUrl.getBytes(StandardCharsets.UTF_8), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        return baseUrl + "/api/vod/tivify/clearkey?u=" + token;
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
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    return trimmed;
                }
            }
        }
        return "";
    }

    private static String sanitizeFilterKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
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
            if (!desired.isEmpty()) {
                for (ChannelFilter filter : filters) {
                    if (desired.equals(filter.key)) {
                        return filter.key;
                    }
                }
            }
            if (!config.enabledFilterKeys.isEmpty()) {
                for (String enabledKey : config.enabledFilterKeys) {
                    for (ChannelFilter filter : filters) {
                        if (enabledKey.equals(filter.key)) {
                            return filter.key;
                        }
                    }
                }
            }
        }
        return filters.get(0).key;
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
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

final class ChannelItem {
    final String id;
    final String name;
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
    final String drmScheme;
    final String drmLicenseUrl;
    final boolean directPlayback;
    boolean favorite;
    String nowProgram;

    ChannelItem(String id, String name, String logoUrl, String group, String playUrl, String fallbackPlayUrl, int originalOrder, int dashboardOrder, boolean isVod, boolean isAdultVod, int platformId, String platformName, List<String> customGroups, String drmScheme, String drmLicenseUrl, boolean directPlayback) {
        this.id = id;
        this.name = name;
        this.logoUrl = logoUrl;
        this.group = group;
        this.playUrl = playUrl;
        this.fallbackPlayUrl = fallbackPlayUrl;
        this.originalOrder = originalOrder;
        this.dashboardOrder = dashboardOrder;
        this.isVod = isVod;
        this.isAdultVod = isAdultVod;
        this.platformId = platformId;
        this.platformName = platformName;
        this.customGroups = customGroups;
        this.drmScheme = drmScheme == null ? "" : drmScheme.trim();
        this.drmLicenseUrl = drmLicenseUrl == null ? "" : drmLicenseUrl.trim();
        this.directPlayback = directPlayback;
        this.nowProgram = "";
    }
}

final class ChannelFilter {
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

final class CatalogLoadResult {
    final List<ChannelItem> channels;
    final List<ChannelFilter> filters;
    final String defaultFilterKey;

    CatalogLoadResult(List<ChannelItem> channels, List<ChannelFilter> filters, String defaultFilterKey) {
        this.channels = channels;
        this.filters = filters;
        this.defaultFilterKey = defaultFilterKey;
    }
}
