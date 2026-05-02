package com.drbep.tvplayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
        final int progress;

        EpgProgram(String channelId, String channelName, String title, String icon, String description, String startTime, String endTime, int progress) {
            this.channelId = channelId;
            this.channelName = channelName;
            this.title = title;
            this.icon = icon;
            this.description = description;
            this.startTime = startTime;
            this.endTime = endTime;
            this.progress = progress;
        }
    }

    private final String baseUrl;
    private final HttpClient httpClient;
    private final Map<String, CachedPrograms> programsCache = new HashMap<>();
    private final Map<String, CachedPrograms> categoryCache = new HashMap<>();
    private CachedNowPrograms cachedNowPrograms;
    private static final long PROGRAMS_CACHE_MS = 120000L;
    private static final long NOW_CACHE_MS = 45000L;

    EpgRepository(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new HttpClient();
    }

    Map<String, String> fetchNowPrograms() throws Exception {
        long now = System.currentTimeMillis();
        if (cachedNowPrograms != null && now - cachedNowPrograms.loadedAtMs < NOW_CACHE_MS) {
            return new LinkedHashMap<>(cachedNowPrograms.items);
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
                item.optInt("progress", -1)
        );
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
