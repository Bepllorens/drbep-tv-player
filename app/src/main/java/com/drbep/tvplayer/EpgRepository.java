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
    static final class EpgProgram {
        final String channelId;
        final String channelName;
        final String title;
        final String icon;
        final String description;
        final String startTime;
        final String endTime;
        final String category;
        final int progress;

        EpgProgram(String channelId, String channelName, String title, String icon, String description, String startTime, String endTime, int progress) {
            this(channelId, channelName, title, icon, description, startTime, endTime, "", progress);
        }

        EpgProgram(String channelId, String channelName, String title, String icon, String description, String startTime, String endTime, String category, int progress) {
            this.channelId = channelId;
            this.channelName = channelName;
            this.title = title;
            this.icon = icon;
            this.description = description;
            this.startTime = startTime;
            this.endTime = endTime;
            this.category = category == null ? "" : category;
            this.progress = progress;
        }
    }

    private final String baseUrl;
    private final HttpClient httpClient;
    private final CatalogSnapshotStore snapshotStore;
    private final boolean standaloneMode;
    private final Map<String, CachedPrograms> programsCache = new HashMap<>();
    private final Map<String, CachedPrograms> categoryCache = new HashMap<>();
    private CachedNowPrograms cachedNowPrograms;
    private static final long PROGRAMS_CACHE_MS = 120000L;
    private static final long NOW_CACHE_MS = 45000L;

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
        if (standaloneMode) {
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
        HttpClient.Response response = httpClient.get(baseUrl + "/api/epg/now", 10000, 15000, java.util.Collections.singletonMap("Accept", "application/json"));
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
            String channelId = String.valueOf(item.optLong("channel_id", -1L));
            if ("-1".equals(channelId)) {
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

    List<EpgProgram> fetchChannelPrograms(String channelId, int maxItems) throws Exception {
        String cacheKey = String.valueOf(channelId).trim() + "|" + maxItems;
        long now = System.currentTimeMillis();
        CachedPrograms cached = programsCache.get(cacheKey);
        if (cached != null && now - cached.loadedAtMs < PROGRAMS_CACHE_MS) {
            return new ArrayList<>(cached.items);
        }
        if (standaloneMode) {
            List<EpgProgram> programs = loadOfflineChannelPrograms(channelId, maxItems);
            programsCache.put(cacheKey, new CachedPrograms(now, programs));
            return programs;
        }
        HttpClient.Response response = httpClient.get(
                baseUrl + "/api/epg/channel/" + channelId,
                10000,
                15000,
                java.util.Collections.singletonMap("Accept", "application/json")
        );
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

    EpgProgram fetchProgramForChannel(String channelId, boolean next) throws Exception {
        if (standaloneMode) {
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
        HttpClient.Response response = httpClient.get(baseUrl + "/api/epg/channel/" + channelId + (next ? "/next" : "/current"), 10000, 15000, java.util.Collections.singletonMap("Accept", "application/json"));
        if (response.code == 404) {
            return null;
        }
        return fromJson(httpClient.parseObject(httpClient.requireSuccess(response, "cargando programa EPG").body, "cargando programa EPG"));
    }

    List<EpgProgram> fetchNowProgramsDetailed() throws Exception {
        String cacheKey = "now-detailed";
        long now = System.currentTimeMillis();
        CachedPrograms cached = categoryCache.get(cacheKey);
        if (cached != null && now - cached.loadedAtMs < NOW_CACHE_MS) {
            return new ArrayList<>(cached.items);
        }
        if (standaloneMode) {
            List<EpgProgram> items = loadOfflineNowPrograms();
            categoryCache.put(cacheKey, new CachedPrograms(now, items));
            return items;
        }
        HttpClient.Response response = httpClient.get(baseUrl + "/api/epg/now", 10000, 15000, java.util.Collections.singletonMap("Accept", "application/json"));
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
        if (standaloneMode) {
            List<EpgProgram> items = filterOfflineCategoryPrograms(type, hours);
            categoryCache.put(cacheKey, new CachedPrograms(now, items));
            return items;
        }
        HttpClient.Response response = httpClient.get(baseUrl + "/api/epg/category?type=" + type + "&hours=" + hours, 10000, 15000, java.util.Collections.singletonMap("Accept", "application/json"));
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
        String key = String.valueOf(channelId).trim();
        List<EpgProgram> rows = loadOfflineProgramMap().get(key);
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
        Map<String, List<EpgProgram>> out = new LinkedHashMap<>();
        if (snapshotStore == null) {
            return out;
        }
        JSONObject snapshot = snapshotStore.loadSnapshotObject();
        JSONObject epg = snapshot.optJSONObject("epg");
        JSONObject programs = epg == null ? null : epg.optJSONObject("programs");
        if (programs == null) {
            return out;
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
                    rows.add(fromJson(item));
                }
            }
            if (!rows.isEmpty()) {
                out.put(String.valueOf(channelId).trim(), rows);
            }
        }
        return out;
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
        return new EpgProgram(program.channelId, program.channelName, program.title, program.icon, program.description, program.startTime, program.endTime, program.category, progress);
    }

    private static long parseIsoMillis(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return java.time.Instant.parse(value.trim()).toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
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

    private static final class CachedNowPrograms {
        final long loadedAtMs;
        final Map<String, String> items;

        CachedNowPrograms(long loadedAtMs, Map<String, String> items) {
            this.loadedAtMs = loadedAtMs;
            this.items = items == null ? new LinkedHashMap<>() : new LinkedHashMap<>(items);
        }
    }
}
