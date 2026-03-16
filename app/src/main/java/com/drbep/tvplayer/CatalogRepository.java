package com.drbep.tvplayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

final class CatalogRepository {
    private static final int FILTER_ALL = 0;
    private static final int FILTER_PLATFORM = 1;
    private static final int FILTER_CUSTOM_GROUP = 2;
    private static final int FILTER_VOD = 3;

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
                String externalId = channel.optString("external_id", "").trim();
                String tvgId = channel.optString("tvg_id", "").trim();
                int platformId = (int) channel.optLong("platform_id", 0L);
                String platformName = channel.optString("platform_name", "").trim();
                int sortOrder = channel.optInt("sort_order", Integer.MAX_VALUE);
                if (sortOrder <= 0) {
                    sortOrder = channel.optInt("dial", i + 1);
                }
                boolean isVod = channel.optBoolean("is_vod", false) || isLikelyVod(externalId, name, tvgId, sourceGroup);

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
                        platformId,
                        platformName,
                        customGroups
                ));
            }

            long activePlatformId = payload.optLong("active_platform_id", 0L);
            return new CatalogLoadResult(parsed, buildFiltersFromCatalog(parsed, activePlatformId), "all");
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

            parsed.add(new ChannelItem(
                    id,
                    name,
                    logo,
                    sourceGroup,
                    playUrl,
                    buildFallbackPlayUrl(id),
                    i,
                    i + 1,
                    isLikelyVod("", name, "", sourceGroup),
                    0,
                    "Plataforma activa",
                    new ArrayList<>()
            ));
        }

        List<ChannelFilter> filters = new ArrayList<>();
        filters.add(new ChannelFilter("all", "Todos", FILTER_ALL, 0, ""));
        return new CatalogLoadResult(parsed, filters, "all");
    }

    private List<ChannelFilter> buildFiltersFromCatalog(List<ChannelItem> parsed, long activePlatformId) {
        LinkedHashMap<String, ChannelFilter> byKey = new LinkedHashMap<>();
        byKey.put("all", new ChannelFilter("all", "Todos", FILTER_ALL, 0, ""));

        Map<Integer, String> platformNames = new LinkedHashMap<>();
        Set<String> customGroupNames = new HashSet<>();
        for (ChannelItem item : parsed) {
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
        byKey.put("vod", new ChannelFilter("vod", "VOD", FILTER_VOD, 0, ""));

        return new ArrayList<>(byKey.values());
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

    private static boolean isLikelyVod(String externalId, String name, String tvgId, String groupTitle) {
        String normalizedName = safeLower(name);
        String normalizedTvgId = safeLower(tvgId);
        String normalizedGroup = safeLower(groupTitle);
        return containsVodToken(normalizedName) || containsVodToken(normalizedTvgId) || containsVodToken(normalizedGroup);
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
    final int platformId;
    final String platformName;
    final List<String> customGroups;
    boolean favorite;
    String nowProgram;

    ChannelItem(String id, String name, String logoUrl, String group, String playUrl, String fallbackPlayUrl, int originalOrder, int dashboardOrder, boolean isVod, int platformId, String platformName, List<String> customGroups) {
        this.id = id;
        this.name = name;
        this.logoUrl = logoUrl;
        this.group = group;
        this.playUrl = playUrl;
        this.fallbackPlayUrl = fallbackPlayUrl;
        this.originalOrder = originalOrder;
        this.dashboardOrder = dashboardOrder;
        this.isVod = isVod;
        this.platformId = platformId;
        this.platformName = platformName;
        this.customGroups = customGroups;
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