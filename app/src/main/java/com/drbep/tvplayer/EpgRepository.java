package com.drbep.tvplayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class EpgRepository {
    private static final String PUBLIC_EPG_BASE_URL = "https://iptv.bepllorens.com";
    private static final String OFFLINE_PUBLIC_BASE_URL = "https://fire.tvbep.com";

    static final class EpgProgram {
        final String channelId;
        final String channelName;
        final String tvgId;
        final String title;
        final String icon;
        final String description;
        final String startTime;
        final String endTime;
        final String category;
        final int progress;

        EpgProgram(String channelId, String channelName, String title, String icon, String description, String startTime, String endTime, int progress) {
            this(channelId, channelName, "", title, icon, description, startTime, endTime, "", progress);
        }

        EpgProgram(String channelId, String channelName, String tvgId, String title, String icon, String description, String startTime, String endTime, String category, int progress) {
            this.channelId = channelId;
            this.channelName = channelName;
            this.tvgId = tvgId == null ? "" : tvgId;
            this.title = title;
            this.icon = icon;
            this.description = description;
            this.startTime = startTime;
            this.endTime = endTime;
            this.category = category == null ? "" : category;
            this.progress = progress;
        }
    }

    static final class EpgProgramPair {
        final EpgProgram current;
        final EpgProgram next;

        EpgProgramPair(EpgProgram current, EpgProgram next) {
            this.current = current;
            this.next = next;
        }
    }

    private final String baseUrl;
    private final HttpClient httpClient;
    private final CatalogSnapshotStore snapshotStore;
    private final boolean standaloneMode;
    private final Map<String, CachedPrograms> programsCache = new HashMap<>();
    private final Map<String, CachedPrograms> categoryCache = new HashMap<>();
    private CachedNowPrograms cachedNowPrograms;
    private CachedOfflineProgramMap cachedOfflineProgramMap;
    private String cachedRemoteEpgBaseUrl = "";
    private static final long PROGRAMS_CACHE_MS = 120000L;
    private static final long NOW_CACHE_MS = 45000L;
    private static final long OFFLINE_PROGRAM_MAP_CACHE_MS = 120000L;
    private static final long MAX_OFFLINE_EPG_SNAPSHOT_BYTES = 12L * 1024L * 1024L;

    EpgRepository(String baseUrl) {
        this(baseUrl, null, false);
    }

    EpgRepository(String baseUrl, CatalogSnapshotStore snapshotStore, boolean standaloneMode) {
        this.baseUrl = baseUrl;
        this.snapshotStore = snapshotStore;
        this.standaloneMode = standaloneMode;
        this.httpClient = new HttpClient();
    }

    Map<String, String> fetchNowPrograms() throws Exception {
        long now = System.currentTimeMillis();
        if (cachedNowPrograms != null && now - cachedNowPrograms.loadedAtMs < NOW_CACHE_MS) {
            return new LinkedHashMap<>(cachedNowPrograms.items);
        }
        if (shouldUseOfflineEpg()) {
            Map<String, String> updates = new LinkedHashMap<>();
            for (EpgProgram program : loadOfflineNowPrograms()) {
                String title = program.title == null ? "" : program.title.trim();
                if (title.isEmpty()) {
                    continue;
                }
                if (program.progress >= 0) {
                    title = title + " (" + program.progress + "%)";
                }
                updates.put(program.channelId, title);
            }
            cachedNowPrograms = new CachedNowPrograms(now, updates);
            return updates;
        }
        HttpClient.Response response = getRemoteEpg("/api/epg/now");
        if (!response.isSuccessful()) {
            return new HashMap<>();
        }

        JSONArray arr = httpClient.parseArray(response.body, "cargando EPG actual");
        Map<String, String> updates = new HashMap<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String channelId = extractProgramChannelId(item);
            if (channelId.isEmpty()) {
                continue;
            }
            String title = item.optString("title", "").trim();
            int progress = item.optInt("progress", -1);
            if (title.isEmpty()) {
                continue;
            }
            if (progress >= 0) {
                title = title + " (" + progress + "%)";
            }
            updates.put(channelId, title);
        }
        cachedNowPrograms = new CachedNowPrograms(now, updates);
        return updates;
    }

    Map<String, String> fetchNowProgramsForChannels(List<ChannelItem> channelItems) throws Exception {
        if (!shouldUseOfflineEpg()) {
            return fetchNowPrograms();
        }
        Map<String, String> updates = new LinkedHashMap<>();
        for (Map.Entry<String, EpgProgramPair> entry : fetchProgramPairsForChannels(channelItems).entrySet()) {
            EpgProgram program = entry.getValue() == null ? null : entry.getValue().current;
            if (program == null || program.title == null || program.title.trim().isEmpty()) {
                continue;
            }
            String title = program.title.trim();
            if (program.progress >= 0) {
                title = title + " (" + program.progress + "%)";
            }
            updates.put(entry.getKey(), title);
        }
        cachedNowPrograms = new CachedNowPrograms(System.currentTimeMillis(), updates);
        return updates;
    }

    Map<String, EpgProgramPair> fetchProgramPairsForChannels(List<ChannelItem> channelItems) throws Exception {
        Map<String, EpgProgramPair> out = new LinkedHashMap<>();
        if (channelItems == null || channelItems.isEmpty()) {
            return out;
        }
        if (!shouldUseOfflineEpg()) {
            return fetchRemoteProgramPairsForChannels(channelItems);
        }
        long now = System.currentTimeMillis();
        List<ChannelItem> remoteFallbackChannels = new ArrayList<>();
        for (ChannelItem channel : channelItems) {
            if (channel == null || channel.isVod || channel.id == null || channel.id.trim().isEmpty()) {
                continue;
            }
            List<EpgProgram> rows = resolveOfflinePrograms(channel.id, channel.name, channel.tvgId);
            if (rows.isEmpty()) {
                remoteFallbackChannels.add(channel);
                continue;
            }
            EpgProgram current = null;
            EpgProgram next = null;
            for (EpgProgram row : rows) {
                long startMs = parseIsoMillis(row.startTime);
                long endMs = parseIsoMillis(row.endTime);
                if (current == null && startMs <= now && endMs > now) {
                    current = programWithProgress(row, now);
                    continue;
                }
                if (startMs > now) {
                    next = programWithProgress(row, now);
                    break;
                }
            }
            if (current != null || next != null) {
                out.put(channel.id.trim(), new EpgProgramPair(current, next));
            }
        }
        if (!remoteFallbackChannels.isEmpty()) {
            out.putAll(fetchRemoteProgramPairsForChannels(remoteFallbackChannels));
        }
        return out;
    }

    List<EpgProgram> fetchChannelPrograms(String channelId, int maxItems) throws Exception {
        String cacheKey = String.valueOf(channelId).trim() + "|" + maxItems;
        long now = System.currentTimeMillis();
        CachedPrograms cached = programsCache.get(cacheKey);
        if (cached != null && now - cached.loadedAtMs < PROGRAMS_CACHE_MS) {
            return new ArrayList<>(cached.items);
        }
        if (shouldUseOfflineEpg()) {
            List<EpgProgram> programs = loadOfflineChannelPrograms(channelId, maxItems);
            programsCache.put(cacheKey, new CachedPrograms(now, programs));
            return programs;
        }
        HttpClient.Response response = getRemoteEpg("/api/epg/channel/" + channelId);
        if (!response.isSuccessful()) {
            return new ArrayList<>();
        }
        String body = response.body == null ? "" : response.body.trim();
        if (body.isEmpty() || "null".equals(body)) {
            return new ArrayList<>();
        }
        JSONArray arr = httpClient.parseArray(body, "cargando guia EPG del canal");
        List<EpgProgram> programs = new ArrayList<>();
        int limit = Math.min(arr.length(), maxItems);
        for (int i = 0; i < limit; i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) {
                continue;
            }
            programs.add(fromJson(item));
        }
        programsCache.put(cacheKey, new CachedPrograms(now, programs));
        return programs;
    }

    List<EpgProgram> fetchChannelPrograms(ChannelItem channel, int maxItems) throws Exception {
        if (channel == null) {
            return new ArrayList<>();
        }
        if (!shouldUseOfflineEpg()) {
            List<EpgProgram> programs = fetchChannelPrograms(channel.id, maxItems);
            if (!programs.isEmpty()) {
                return programs;
            }
            return fetchRemoteProgramsForChannel(channel, maxItems);
        }
        long now = System.currentTimeMillis();
        List<EpgProgram> rows = resolveOfflinePrograms(channel.id, channel.name, channel.tvgId);
        if (rows.isEmpty()) {
            return fetchRemoteProgramsForChannel(channel, maxItems);
        }
        List<EpgProgram> out = new ArrayList<>();
        int limit = maxItems <= 0 ? rows.size() : maxItems;
        for (EpgProgram program : rows) {
            if (parseIsoMillis(program.endTime) <= now) {
                continue;
            }
            out.add(programWithProgress(program, now));
            if (out.size() >= limit) {
                break;
            }
        }
        return out;
    }

    List<EpgProgram> fetchPastChannelPrograms(ChannelItem channel, int maxItems, int daysBack) throws Exception {
        if (channel == null || channel.isVod) {
            return new ArrayList<>();
        }
        if (!shouldUseOfflineEpg()) {
            return new ArrayList<>();
        }
        long now = System.currentTimeMillis();
        long since = now - Math.max(1, daysBack) * 24L * 60L * 60L * 1000L;
        List<EpgProgram> rows = resolveOfflinePrograms(channel.id, channel.name, channel.tvgId);
        List<EpgProgram> out = new ArrayList<>();
        for (int i = rows.size() - 1; i >= 0; i--) {
            EpgProgram program = rows.get(i);
            long startMs = parseIsoMillis(program.startTime);
            long endMs = parseIsoMillis(program.endTime);
            if (endMs <= 0L || startMs <= 0L || endMs > now || endMs < since) {
                continue;
            }
            out.add(programWithProgress(program, now));
            if (maxItems > 0 && out.size() >= maxItems) {
                break;
            }
        }
        return out;
    }

    EpgProgram fetchProgramForChannel(String channelId, boolean next) throws Exception {
        if (shouldUseOfflineEpg()) {
            long now = System.currentTimeMillis();
            for (EpgProgram program : loadOfflineChannelPrograms(channelId, 64)) {
                long startMs = parseIsoMillis(program.startTime);
                long endMs = parseIsoMillis(program.endTime);
                if (!next && startMs <= now && endMs > now) {
                    return program;
                }
                if (next && startMs > now) {
                    return program;
                }
            }
            return null;
        }
        HttpClient.Response response = getRemoteEpg("/api/epg/channel/" + channelId + (next ? "/next" : "/current"));
        if (response.code == 404) {
            return null;
        }
        return fromJson(httpClient.parseObject(httpClient.requireSuccess(response, "cargando programa EPG").body, "cargando programa EPG"));
    }

    EpgProgram fetchProgramForChannel(ChannelItem channel, boolean next) throws Exception {
        if (channel == null) {
            return null;
        }
        if (!shouldUseOfflineEpg()) {
            EpgProgram direct = fetchProgramForChannel(channel.id, next);
            if (direct != null) {
                return direct;
            }
            List<EpgProgram> fallbackPrograms = fetchRemoteProgramsForChannel(channel, next ? 2 : 1);
            if (fallbackPrograms.isEmpty()) {
                return null;
            }
            return next && fallbackPrograms.size() > 1 ? fallbackPrograms.get(1) : fallbackPrograms.get(0);
        }
        long now = System.currentTimeMillis();
        List<EpgProgram> rows = resolveOfflinePrograms(channel.id, channel.name, channel.tvgId);
        if (rows.isEmpty()) {
            EpgProgram direct = fetchProgramForChannel(channel.id, next);
            if (direct != null) {
                return direct;
            }
            List<EpgProgram> fallbackPrograms = fetchRemoteProgramsForChannel(channel, next ? 2 : 1);
            if (fallbackPrograms.isEmpty()) {
                return null;
            }
            return next && fallbackPrograms.size() > 1 ? fallbackPrograms.get(1) : fallbackPrograms.get(0);
        }
        for (EpgProgram program : rows) {
            long startMs = parseIsoMillis(program.startTime);
            long endMs = parseIsoMillis(program.endTime);
            if (!next && startMs <= now && endMs > now) {
                return programWithProgress(program, now);
            }
            if (next && startMs > now) {
                return programWithProgress(program, now);
            }
        }
        return null;
    }

    List<EpgProgram> fetchNowProgramsDetailed() throws Exception {
        String cacheKey = "now-detailed";
        long now = System.currentTimeMillis();
        CachedPrograms cached = categoryCache.get(cacheKey);
        if (cached != null && now - cached.loadedAtMs < NOW_CACHE_MS) {
            return new ArrayList<>(cached.items);
        }
        if (shouldUseOfflineEpg()) {
            List<EpgProgram> items = loadOfflineNowPrograms();
            categoryCache.put(cacheKey, new CachedPrograms(now, items));
            return items;
        }
        HttpClient.Response response = getRemoteEpg("/api/epg/now");
        if (!response.isSuccessful()) {
            return new ArrayList<>();
        }
        List<EpgProgram> items = parseProgramsArray(response.body, "cargando EPG actual");
        categoryCache.put(cacheKey, new CachedPrograms(now, items));
        return items;
    }

    List<EpgProgram> fetchCategoryPrograms(String type, int hours) throws Exception {
        String cacheKey = String.valueOf(type).trim() + "|" + hours;
        long now = System.currentTimeMillis();
        CachedPrograms cached = categoryCache.get(cacheKey);
        if (cached != null && now - cached.loadedAtMs < PROGRAMS_CACHE_MS) {
            return new ArrayList<>(cached.items);
        }
        if (shouldUseOfflineEpg()) {
            List<EpgProgram> items = filterOfflineCategoryPrograms(type, hours);
            categoryCache.put(cacheKey, new CachedPrograms(now, items));
            return items;
        }
        HttpClient.Response response = getRemoteEpg("/api/epg/category?type=" + type + "&hours=" + hours);
        if (!response.isSuccessful()) {
            return new ArrayList<>();
        }
        List<EpgProgram> items = parseProgramsArray(response.body, "cargando categoria EPG");
        categoryCache.put(cacheKey, new CachedPrograms(now, items));
        return items;
    }

    private List<EpgProgram> parseProgramsArray(String body, String context) throws Exception {
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.isEmpty() || "null".equals(trimmed)) {
            return new ArrayList<>();
        }
        JSONArray arr = httpClient.parseArray(trimmed, context);
        List<EpgProgram> programs = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) {
                continue;
            }
            programs.add(fromJson(item));
        }
        return programs;
    }

    private static EpgProgram fromJson(JSONObject item) {
        return new EpgProgram(
                item.optString("channel_id", ""),
                item.optString("channel_name", ""),
                item.optString("tvg_id", ""),
                item.optString("title", "Sin titulo"),
                item.optString("icon", ""),
                item.optString("description", ""),
                item.optString("start_time", ""),
                item.optString("end_time", ""),
                item.optString("category", ""),
                item.optInt("progress", -1)
        );
    }

    private List<EpgProgram> loadOfflineNowPrograms() throws Exception {
        long now = System.currentTimeMillis();
        List<EpgProgram> out = new ArrayList<>();
        for (List<EpgProgram> rows : loadOfflineProgramMap().values()) {
            for (EpgProgram program : rows) {
                long startMs = parseIsoMillis(program.startTime);
                long endMs = parseIsoMillis(program.endTime);
                if (startMs <= now && endMs > now) {
                    out.add(programWithProgress(program, now));
                    break;
                }
            }
        }
        return out;
    }

    private List<EpgProgram> loadOfflineChannelPrograms(String channelId, int maxItems) throws Exception {
        List<EpgProgram> rows = resolveOfflinePrograms(channelId, "", "");
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        long now = System.currentTimeMillis();
        List<EpgProgram> out = new ArrayList<>();
        int limit = maxItems <= 0 ? rows.size() : maxItems;
        for (EpgProgram program : rows) {
            if (parseIsoMillis(program.endTime) <= now) {
                continue;
            }
            out.add(programWithProgress(program, now));
            if (out.size() >= limit) {
                break;
            }
        }
        return out;
    }

    private List<EpgProgram> filterOfflineCategoryPrograms(String type, int hours) throws Exception {
        long now = System.currentTimeMillis();
        long end = now + Math.max(1, hours) * 60L * 60L * 1000L;
        String[] keywords = categoryKeywords(type);
        if (keywords.length == 0) {
            return new ArrayList<>();
        }
        List<EpgProgram> out = new ArrayList<>();
        for (List<EpgProgram> rows : loadOfflineProgramMap().values()) {
            for (EpgProgram program : rows) {
                long startMs = parseIsoMillis(program.startTime);
                long endMs = parseIsoMillis(program.endTime);
                if (endMs <= now || startMs >= end || !matchesAny(program.category + " " + program.title + " " + program.description, keywords)) {
                    continue;
                }
                out.add(programWithProgress(program, now));
            }
        }
        out.sort((left, right) -> Long.compare(parseIsoMillis(left.startTime), parseIsoMillis(right.startTime)));
        return out;
    }

    private Map<String, List<EpgProgram>> loadOfflineProgramMap() throws Exception {
        CachedOfflineProgramMap cached = loadCachedOfflineProgramMap();
        return cached == null ? new LinkedHashMap<>() : cached.byChannelId;
    }

    private boolean shouldUseOfflineEpg() {
        if (!standaloneMode) {
            return false;
        }
        if (!shouldReadOfflineEpgSnapshot()) {
            return false;
        }
        try {
            CachedOfflineProgramMap cached = loadCachedOfflineProgramMap();
            return cached != null && !cached.byChannelId.isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean shouldReadOfflineEpgSnapshot() {
        if (snapshotStore == null) {
            return false;
        }
        CatalogSnapshotStore.SnapshotStatus status = snapshotStore.getStatus(baseUrl);
        if (status == null || !status.available || status.expired || status.epgProgramCount <= 0) {
            return false;
        }
        return status.sizeBytes <= MAX_OFFLINE_EPG_SNAPSHOT_BYTES;
    }

    private Map<String, EpgProgramPair> fetchRemoteProgramPairsForChannels(List<ChannelItem> channelItems) throws Exception {
        Map<String, EpgProgramPair> out = new LinkedHashMap<>();
        if (channelItems == null || channelItems.isEmpty()) {
            return out;
        }
        RemoteProgramIndex index = buildRemoteProgramIndex(fetchNowProgramsDetailed());
        for (ChannelItem channel : channelItems) {
            if (channel == null || channel.isVod || channel.id == null || channel.id.trim().isEmpty()) {
                continue;
            }
            EpgProgram current = matchRemoteProgram(index, channel);
            if (current != null) {
                out.put(channel.id.trim(), new EpgProgramPair(current, null));
            }
        }
        return out;
    }

    private List<EpgProgram> fetchRemoteProgramsForChannel(ChannelItem channel, int maxItems) throws Exception {
        List<EpgProgram> out = new ArrayList<>();
        if (channel == null || channel.isVod) {
            return out;
        }
        EpgProgramPair pair = fetchRemoteProgramPairsForChannels(java.util.Collections.singletonList(channel)).get(channel.id == null ? "" : channel.id.trim());
        if (pair == null) {
            return out;
        }
        if (pair.current != null) {
            out.add(pair.current);
        }
        if (pair.next != null && out.size() < Math.max(1, maxItems)) {
            out.add(pair.next);
        }
        return out;
    }

    private CachedOfflineProgramMap loadCachedOfflineProgramMap() throws Exception {
        Map<String, List<EpgProgram>> empty = new LinkedHashMap<>();
        if (snapshotStore == null) {
            return new CachedOfflineProgramMap(System.currentTimeMillis(), "", empty, new HashMap<>(), new HashMap<>());
        }
        if (!shouldReadOfflineEpgSnapshot()) {
            cachedOfflineProgramMap = new CachedOfflineProgramMap(System.currentTimeMillis(), "", empty, new HashMap<>(), new HashMap<>());
            return cachedOfflineProgramMap;
        }
        String fingerprint = buildOfflineSnapshotFingerprint();
        long now = System.currentTimeMillis();
        if (cachedOfflineProgramMap != null
                && fingerprint.equals(cachedOfflineProgramMap.snapshotFingerprint)
                && now - cachedOfflineProgramMap.loadedAtMs < OFFLINE_PROGRAM_MAP_CACHE_MS) {
            return cachedOfflineProgramMap;
        }

        Map<String, List<EpgProgram>> byChannelId = new LinkedHashMap<>();
        Map<String, List<EpgProgram>> byTvgId = new HashMap<>();
        Map<String, List<EpgProgram>> byName = new HashMap<>();
        JSONObject snapshot = snapshotStore.loadSnapshotObject();
        JSONObject epg = extractSnapshotEpg(snapshot);
        JSONObject programs = epg == null ? null : epg.optJSONObject("programs");
        if (programs == null) {
            cachedOfflineProgramMap = new CachedOfflineProgramMap(now, fingerprint, byChannelId, byTvgId, byName);
            return cachedOfflineProgramMap;
        }
        java.util.Iterator<String> keys = programs.keys();
        while (keys.hasNext()) {
            String channelId = keys.next();
            JSONArray arr = programs.optJSONArray(channelId);
            if (arr == null) {
                continue;
            }
            List<EpgProgram> rows = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.optJSONObject(i);
                if (item != null) {
                    rows.add(fromJson(item, String.valueOf(channelId).trim()));
                }
            }
            if (!rows.isEmpty()) {
                String normalizedChannelId = String.valueOf(channelId).trim();
                byChannelId.put(normalizedChannelId, rows);
                EpgProgram first = rows.get(0);
                String normalizedTvgId = normalizeLookupKey(first.tvgId);
                if (!normalizedTvgId.isEmpty() && !byTvgId.containsKey(normalizedTvgId)) {
                    byTvgId.put(normalizedTvgId, rows);
                }
                String normalizedName = normalizeLookupKey(first.channelName);
                if (!normalizedName.isEmpty() && !byName.containsKey(normalizedName)) {
                    byName.put(normalizedName, rows);
                }
            }
        }
        cachedOfflineProgramMap = new CachedOfflineProgramMap(now, fingerprint, byChannelId, byTvgId, byName);
        return cachedOfflineProgramMap;
    }

    private List<EpgProgram> resolveOfflinePrograms(String channelId, String channelName, String tvgId) throws Exception {
        CachedOfflineProgramMap cached = loadCachedOfflineProgramMap();
        Map<String, List<EpgProgram>> byId = cached.byChannelId;
        String key = String.valueOf(channelId).trim();
        List<EpgProgram> direct = byId.get(key);
        if (direct != null && !direct.isEmpty()) {
            return direct;
        }
        String normalizedTvgId = normalizeLookupKey(tvgId);
        if (!normalizedTvgId.isEmpty()) {
            List<EpgProgram> byTvg = cached.byTvgId.get(normalizedTvgId);
            if (byTvg != null && !byTvg.isEmpty()) {
                return byTvg;
            }
        }
        String normalizedName = normalizeLookupKey(channelName);
        if (!normalizedName.isEmpty()) {
            List<EpgProgram> byChannelName = cached.byChannelName.get(normalizedName);
            if (byChannelName != null && !byChannelName.isEmpty()) {
                return byChannelName;
            }
        }
        return new ArrayList<>();
    }

    private String buildOfflineSnapshotFingerprint() {
        if (snapshotStore == null) {
            return "";
        }
        CatalogSnapshotStore.SnapshotStatus status = snapshotStore.getStatus("");
        return status.updatedAtMs + ":" + status.sizeBytes + ":" + status.expiresAtMs;
    }

    private HttpClient.Response getRemoteEpg(String path) throws Exception {
        Exception firstError = null;
        HttpClient.Response firstHttpError = null;
        for (String candidate : remoteEpgBaseUrlCandidates()) {
            try {
                HttpClient.Response response = httpClient.get(candidate + path, 10000, 15000, buildRequestHeaders());
                if (response.isSuccessful()) {
                    cachedRemoteEpgBaseUrl = candidate;
                    return response;
                }
                if (response.code == 401 || response.code == 403) {
                    cachedRemoteEpgBaseUrl = candidate;
                    return response;
                }
                if (response.code != 404 && firstHttpError == null) {
                    firstHttpError = response;
                }
            } catch (Exception e) {
                if (firstError == null) {
                    firstError = e;
                }
            }
        }
        if (firstHttpError != null) {
            return firstHttpError;
        }
        if (firstError != null) {
            throw firstError;
        }
        return new HttpClient.Response(404, "");
    }

    private List<String> remoteEpgBaseUrlCandidates() {
        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, cachedRemoteEpgBaseUrl);
        addCandidate(candidates, baseUrl);
        if (snapshotStore != null) {
            CatalogSnapshotStore.SnapshotStatus status = snapshotStore.getStatus("");
            addCandidate(candidates, status == null ? "" : status.sourceBaseUrl);
            addCandidate(candidates, extractBaseUrl(status == null ? "" : status.sourceUrl));
        }
        addCandidate(candidates, PUBLIC_EPG_BASE_URL);
        addCandidate(candidates, OFFLINE_PUBLIC_BASE_URL);
        return candidates;
    }

    private static void addCandidate(List<String> candidates, String value) {
        String clean = extractBaseUrl(value);
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

    private static String extractBaseUrl(String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) {
            return "";
        }
        try {
            java.net.URI uri = new java.net.URI(clean);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            if (scheme == null || host == null) {
                return "";
            }
            if (port > 0) {
                return scheme + "://" + host + ":" + port;
            }
            return scheme + "://" + host;
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String extractProgramChannelId(JSONObject item) {
        if (item == null) {
            return "";
        }
        String direct = item.optString("channel_id", "").trim();
        if (!direct.isEmpty() && !"null".equalsIgnoreCase(direct)) {
            return direct;
        }
        Object raw = item.opt("channel_id");
        if (raw instanceof Number) {
            long numeric = ((Number) raw).longValue();
            return numeric > 0L ? String.valueOf(numeric) : "";
        }
        long fallback = item.optLong("channel_id", -1L);
        return fallback > 0L ? String.valueOf(fallback) : "";
    }

    private static RemoteProgramIndex buildRemoteProgramIndex(List<EpgProgram> programs) {
        RemoteProgramIndex index = new RemoteProgramIndex();
        if (programs == null) {
            return index;
        }
        for (EpgProgram program : programs) {
            if (program == null) {
                continue;
            }
            putFirst(index.byChannelId, normalizeLookupKey(program.channelId), program);
            putFirst(index.byTvgId, normalizeLookupKey(program.tvgId), program);
            putFirst(index.byChannelName, normalizeLookupKey(program.channelName), program);
        }
        return index;
    }

    private static void putFirst(Map<String, EpgProgram> target, String key, EpgProgram program) {
        if (target == null || key == null || key.isEmpty() || program == null || target.containsKey(key)) {
            return;
        }
        target.put(key, program);
    }

    private static EpgProgram matchRemoteProgram(RemoteProgramIndex index, ChannelItem channel) {
        if (index == null || channel == null) {
            return null;
        }
        String channelId = normalizeLookupKey(channel.id);
        if (!channelId.isEmpty() && index.byChannelId.containsKey(channelId)) {
            return index.byChannelId.get(channelId);
        }
        String tvgId = normalizeLookupKey(channel.tvgId);
        if (!tvgId.isEmpty() && index.byTvgId.containsKey(tvgId)) {
            return index.byTvgId.get(tvgId);
        }
        String name = normalizeLookupKey(channel.name);
        if (!name.isEmpty() && index.byChannelName.containsKey(name)) {
            return index.byChannelName.get(name);
        }
        return null;
    }

    private Map<String, String> buildRequestHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/json");
        if (snapshotStore != null) {
            String token = snapshotStore.getAccessToken();
            if (token != null && !token.trim().isEmpty()) {
                headers.put("Authorization", "Bearer " + token.trim());
                headers.put("X-DRBEP-Access-Token", token.trim());
            }
            String deviceId = snapshotStore.getDeviceId();
            if (deviceId != null && !deviceId.trim().isEmpty()) {
                headers.put("X-DRBEP-Device-Id", deviceId.trim());
            }
        }
        return headers;
    }

    private static JSONObject extractSnapshotEpg(JSONObject snapshot) {
        if (snapshot == null) {
            return null;
        }
        JSONObject epg = snapshot.optJSONObject("epg");
        if (epg != null) {
            return epg;
        }
        JSONObject catalog = snapshot.optJSONObject("catalog");
        return catalog == null ? null : catalog.optJSONObject("epg");
    }

    private static EpgProgram fromJson(JSONObject item, String fallbackChannelId) {
        EpgProgram program = fromJson(item);
        if (program.channelId != null && !program.channelId.trim().isEmpty()) {
            return program;
        }
        return new EpgProgram(
                fallbackChannelId == null ? "" : fallbackChannelId.trim(),
                program.channelName,
                program.tvgId,
                program.title,
                program.icon,
                program.description,
                program.startTime,
                program.endTime,
                program.category,
                program.progress
        );
    }

    private static EpgProgram programWithProgress(EpgProgram program, long nowMs) {
        if (program == null) {
            return null;
        }
        long startMs = parseIsoMillis(program.startTime);
        long endMs = parseIsoMillis(program.endTime);
        int progress = -1;
        if (startMs > 0 && endMs > startMs && startMs <= nowMs && endMs > nowMs) {
            progress = (int) Math.max(0L, Math.min(100L, ((nowMs - startMs) * 100L) / (endMs - startMs)));
        }
        return new EpgProgram(program.channelId, program.channelName, program.tvgId, program.title, program.icon, program.description, program.startTime, program.endTime, program.category, progress);
    }

    private static long parseIsoMillis(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return java.time.Instant.parse(value.trim()).toEpochMilli();
        } catch (Exception ignored) {
        }
        try {
            return java.time.OffsetDateTime.parse(value.trim()).toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }
        try {
            return java.time.ZonedDateTime.parse(value.trim()).toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }
        try {
            return java.time.LocalDateTime.parse(value.trim()).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String normalizeLookupKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matchesAny(String value, String[] keywords) {
        String text = value == null ? "" : value.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (keyword != null && !keyword.trim().isEmpty() && text.contains(keyword.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String[] categoryKeywords(String type) {
        String key = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if ("movies".equals(key)) {
            return new String[]{"movie", "movies", "pelicula", "peliculas", "cine", "film"};
        }
        if ("series".equals(key)) {
            return new String[]{"serie", "series", "episode", "episodio", "season", "temporada"};
        }
        if ("sports".equals(key)) {
            return new String[]{"sport", "sports", "deporte", "deportes", "futbol", "football", "basket", "tenis", "tennis", "formula", "motogp"};
        }
        return new String[0];
    }

    private static final class CachedPrograms {
        final long loadedAtMs;
        final List<EpgProgram> items;

        CachedPrograms(long loadedAtMs, List<EpgProgram> items) {
            this.loadedAtMs = loadedAtMs;
            this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
        }
    }

    private static final class RemoteProgramIndex {
        final Map<String, EpgProgram> byChannelId = new HashMap<>();
        final Map<String, EpgProgram> byTvgId = new HashMap<>();
        final Map<String, EpgProgram> byChannelName = new HashMap<>();
    }

    private static final class CachedNowPrograms {
        final long loadedAtMs;
        final Map<String, String> items;

        CachedNowPrograms(long loadedAtMs, Map<String, String> items) {
            this.loadedAtMs = loadedAtMs;
            this.items = items == null ? new LinkedHashMap<>() : new LinkedHashMap<>(items);
        }
    }

    private static final class CachedOfflineProgramMap {
        final long loadedAtMs;
        final String snapshotFingerprint;
        final Map<String, List<EpgProgram>> byChannelId;
        final Map<String, List<EpgProgram>> byTvgId;
        final Map<String, List<EpgProgram>> byChannelName;

        CachedOfflineProgramMap(long loadedAtMs, String snapshotFingerprint, Map<String, List<EpgProgram>> byChannelId, Map<String, List<EpgProgram>> byTvgId, Map<String, List<EpgProgram>> byChannelName) {
            this.loadedAtMs = loadedAtMs;
            this.snapshotFingerprint = snapshotFingerprint == null ? "" : snapshotFingerprint;
            this.byChannelId = byChannelId == null ? new LinkedHashMap<>() : byChannelId;
            this.byTvgId = byTvgId == null ? new HashMap<>() : byTvgId;
            this.byChannelName = byChannelName == null ? new HashMap<>() : byChannelName;
        }
    }
}
