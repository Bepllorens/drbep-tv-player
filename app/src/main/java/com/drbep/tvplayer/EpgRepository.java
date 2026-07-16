package com.drbep.tvplayer;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class EpgRepository {
    private static final String TAG = "EpgRepository";
    private static final String PUBLIC_EPG_BASE_URL = "https://iptv.bepllorens.com";
    private static final String OFFLINE_PUBLIC_BASE_URL = "https://fire.tvbep.com";

    static final class EpgProgram implements Serializable {
        private static final long serialVersionUID = 1L;

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
    private static final long MAX_OFFLINE_EPG_SNAPSHOT_BYTES = 32L * 1024L * 1024L;
    private static final int REMOTE_EPG_CONNECT_TIMEOUT_MS = 10000;
    private static final int REMOTE_EPG_READ_TIMEOUT_MS = 15000;
    private static final int PREFERRED_REMOTE_EPG_CONNECT_TIMEOUT_MS = 1500;
    private static final int PREFERRED_REMOTE_EPG_READ_TIMEOUT_MS = 2500;
    private static final int COMPACT_REMOTE_EPG_CONNECT_TIMEOUT_MS = 5000;
    private static final int COMPACT_REMOTE_EPG_READ_TIMEOUT_MS = 6000;

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
        return fetchProgramPairsForChannels(channelItems, true);
    }

    Map<String, EpgProgramPair> fetchProgramPairsForChannels(List<ChannelItem> channelItems, boolean allowRemoteFallback) throws Exception {
        return fetchProgramPairsForChannels(channelItems, allowRemoteFallback, false);
    }

    Map<String, EpgProgramPair> fetchProgramPairsForChannels(List<ChannelItem> channelItems, boolean allowRemoteFallback, boolean preferRemote) throws Exception {
        return fetchProgramPairsForChannels(channelItems, allowRemoteFallback, preferRemote, true);
    }

    Map<String, EpgProgramPair> fetchProgramPairsForChannels(List<ChannelItem> channelItems, boolean allowRemoteFallback, boolean preferRemote, boolean allowOfflineSnapshotScan) throws Exception {
        Map<String, EpgProgramPair> out = new LinkedHashMap<>();
        if (channelItems == null || channelItems.isEmpty()) {
            return out;
        }
        if (preferRemote && allowRemoteFallback) {
            try {
                boolean compactRemoteOnly = standaloneMode && !allowOfflineSnapshotScan;
                Map<String, EpgProgramPair> remote = fetchRemoteProgramPairsForChannels(
                        channelItems,
                        compactRemoteOnly ? COMPACT_REMOTE_EPG_CONNECT_TIMEOUT_MS : PREFERRED_REMOTE_EPG_CONNECT_TIMEOUT_MS,
                        compactRemoteOnly ? COMPACT_REMOTE_EPG_READ_TIMEOUT_MS : PREFERRED_REMOTE_EPG_READ_TIMEOUT_MS,
                        compactRemoteOnly
                );
                if (!remote.isEmpty()) {
                    Log.w(TAG, "EPG remote preferred loaded channels="
                            + channelItems.size()
                            + " matched=" + remote.size());
                    return remote;
                }
                Log.w(TAG, "EPG remote preferred empty channels="
                        + channelItems.size()
                        + " compactOnly=" + compactRemoteOnly);
            } catch (Exception e) {
                Log.w(TAG, "EPG remote preferred failed; falling back to offline snapshot channels="
                        + channelItems.size(), e);
            }
        }
        if (standaloneMode && !allowOfflineSnapshotScan) {
            out.putAll(buildInlineProgramPairsForChannels(channelItems));
            if (!out.isEmpty()) {
                Log.w(TAG, "EPG inline catalog loaded channels="
                        + channelItems.size()
                        + " matched=" + out.size());
                return out;
            }
        }
        if (!canReadOfflineEpgSnapshot()) {
            return fetchRemoteProgramPairsForChannels(channelItems);
        }
        long now = System.currentTimeMillis();
        List<ChannelItem> remoteFallbackChannels = new ArrayList<>();
        Map<String, List<EpgProgram>> targetedPrograms = loadOfflineProgramsForRequestedChannels(channelItems, allowOfflineSnapshotScan);
        for (ChannelItem channel : channelItems) {
            if (channel == null || channel.isVod || channel.id == null || channel.id.trim().isEmpty()) {
                continue;
            }
            List<EpgProgram> rows = targetedPrograms.get(channel.id.trim());
            if (rows == null) {
                rows = new ArrayList<>();
            }
            if (rows.isEmpty()) {
                EpgProgramPair inlinePair = buildInlineProgramPair(channel);
                if (inlinePair != null) {
                    out.put(channel.id.trim(), inlinePair);
                } else {
                    remoteFallbackChannels.add(channel);
                }
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
        if (!remoteFallbackChannels.isEmpty() && allowRemoteFallback && allowOfflineSnapshotScan) {
            try {
                out.putAll(fetchRemoteProgramPairsForChannels(remoteFallbackChannels));
            } catch (Exception e) {
                Log.w(TAG, "offline EPG remote fallback failed channels="
                        + remoteFallbackChannels.size()
                        + " returningLocalPairs=" + out.size(), e);
            }
        }
        if (!remoteFallbackChannels.isEmpty() && !allowRemoteFallback) {
                Log.w(TAG, "offline EPG partial missing local rows channels="
                        + remoteFallbackChannels.size()
                        + " returningLocalPairs=" + out.size());
        }
        if (!remoteFallbackChannels.isEmpty() && !allowOfflineSnapshotScan) {
            Log.w(TAG, "offline EPG cache miss; snapshot scan skipped channels="
                    + remoteFallbackChannels.size()
                    + " returningLocalPairs=" + out.size());
        }
        return out;
    }

    private Map<String, EpgProgramPair> buildInlineProgramPairsForChannels(List<ChannelItem> channelItems) {
        Map<String, EpgProgramPair> out = new LinkedHashMap<>();
        if (channelItems == null || channelItems.isEmpty()) {
            return out;
        }
        for (ChannelItem channel : channelItems) {
            if (channel == null || channel.isVod || channel.id == null || channel.id.trim().isEmpty()) {
                continue;
            }
            EpgProgramPair pair = buildInlineProgramPair(channel);
            if (pair != null) {
                out.put(channel.id.trim(), pair);
            }
        }
        return out;
    }

    private EpgProgramPair buildInlineProgramPair(ChannelItem channel) {
        if (channel == null || channel.id == null || channel.id.trim().isEmpty()) {
            return null;
        }
        String currentTitle = channel.nowProgram == null ? "" : channel.nowProgram.trim();
        String nextTitle = channel.nextProgram == null ? "" : channel.nextProgram.trim();
        if (currentTitle.isEmpty() && nextTitle.isEmpty()) {
            return null;
        }
        String channelId = channel.id.trim();
        EpgProgram current = currentTitle.isEmpty()
                ? null
                : new EpgProgram(channelId, channel.name, channel.tvgId, currentTitle, "", "", "", "", "", -1);
        EpgProgram next = nextTitle.isEmpty()
                ? null
                : new EpgProgram(channelId, channel.name, channel.tvgId, nextTitle, "", "", "", "", "", -1);
        return new EpgProgramPair(current, next);
    }

    private List<EpgProgram> buildInlineProgramsForChannel(ChannelItem channel, int maxItems, long nowMs) {
        List<EpgProgram> out = new ArrayList<>();
        if (channel == null || channel.isVod || channel.id == null || channel.id.trim().isEmpty()) {
            return out;
        }
        String currentTitle = channel.nowProgram == null ? "" : channel.nowProgram.trim();
        String nextTitle = channel.nextProgram == null ? "" : channel.nextProgram.trim();
        if (currentTitle.isEmpty() && nextTitle.isEmpty()) {
            return out;
        }
        String channelId = channel.id.trim();
        long roundedNow = Math.max(0L, (nowMs / 60000L) * 60000L);
        long currentStartMs = Math.max(0L, roundedNow - 30L * 60L * 1000L);
        long currentEndMs = roundedNow + 30L * 60L * 1000L;
        long nextEndMs = currentEndMs + 60L * 60L * 1000L;
        if (!currentTitle.isEmpty()) {
            out.add(new EpgProgram(
                    channelId,
                    channel.name,
                    channel.tvgId,
                    currentTitle,
                    channel.logoUrl == null ? "" : channel.logoUrl,
                    "",
                    java.time.Instant.ofEpochMilli(currentStartMs).toString(),
                    java.time.Instant.ofEpochMilli(currentEndMs).toString(),
                    "",
                    50
            ));
        }
        if (!nextTitle.isEmpty() && (maxItems <= 0 || out.size() < maxItems)) {
            out.add(new EpgProgram(
                    channelId,
                    channel.name,
                    channel.tvgId,
                    nextTitle,
                    channel.logoUrl == null ? "" : channel.logoUrl,
                    "",
                    java.time.Instant.ofEpochMilli(currentEndMs).toString(),
                    java.time.Instant.ofEpochMilli(nextEndMs).toString(),
                    "",
                    -1
            ));
        }
        if (maxItems > 0 && out.size() > maxItems) {
            return new ArrayList<>(out.subList(0, maxItems));
        }
        return out;
    }

    private EpgProgram selectInlineProgram(ChannelItem channel, List<EpgProgram> inlinePrograms, boolean next) {
        if (inlinePrograms == null || inlinePrograms.isEmpty()) {
            return null;
        }
        if (!next) {
            return inlinePrograms.get(0);
        }
        if (inlinePrograms.size() > 1) {
            return inlinePrograms.get(1);
        }
        String currentTitle = channel == null || channel.nowProgram == null ? "" : channel.nowProgram.trim();
        return currentTitle.isEmpty() ? inlinePrograms.get(0) : null;
    }

    List<EpgProgram> fetchChannelPrograms(String channelId, int maxItems) throws Exception {
        String cacheKey = String.valueOf(channelId).trim() + "|" + maxItems;
        long now = System.currentTimeMillis();
        CachedPrograms cached = programsCache.get(cacheKey);
        if (cached != null && now - cached.loadedAtMs < PROGRAMS_CACHE_MS) {
            return new ArrayList<>(cached.items);
        }
        if (canReadOfflineEpgSnapshot()) {
            List<EpgProgram> programs = loadOfflineChannelPrograms(channelId, maxItems);
            programsCache.put(cacheKey, new CachedPrograms(now, programs));
            return programs;
        }
        List<EpgProgram> programs = fetchRemoteChannelPrograms(channelId, maxItems);
        programsCache.put(cacheKey, new CachedPrograms(now, programs));
        return programs;
    }

    List<EpgProgram> fetchChannelPrograms(ChannelItem channel, int maxItems) throws Exception {
        if (channel == null) {
            return new ArrayList<>();
        }
        if (!canReadOfflineEpgSnapshot()) {
            List<EpgProgram> programs = fetchRemoteChannelPrograms(channel.id, maxItems);
            if (!programs.isEmpty()) {
                return programs;
            }
            programs = fetchRemoteProgramsForChannel(channel, maxItems);
            if (!programs.isEmpty()) {
                return programs;
            }
            return buildInlineProgramsForChannel(channel, maxItems, System.currentTimeMillis());
        }
        long now = System.currentTimeMillis();
        List<EpgProgram> rows = loadOfflineProgramsForRequestedChannel(channel);
        if (rows.isEmpty()) {
            List<EpgProgram> inlinePrograms = buildInlineProgramsForChannel(channel, maxItems, now);
            if (!inlinePrograms.isEmpty()) {
                return inlinePrograms;
            }
            try {
                List<EpgProgram> programs = fetchRemoteChannelPrograms(channel.id, maxItems);
                if (!programs.isEmpty()) {
                    return programs;
                }
                programs = fetchRemoteProgramsForChannel(channel, maxItems);
                if (!programs.isEmpty()) {
                    return programs;
                }
            } catch (Exception e) {
                Log.w(TAG, "offline EPG channel fallback failed channel="
                        + safeChannelLabel(channel)
                        + " maxItems=" + maxItems, e);
            }
            return new ArrayList<>();
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
        if (out.isEmpty()) {
            return buildInlineProgramsForChannel(channel, maxItems, now);
        }
        return out;
    }

    Map<String, List<EpgProgram>> fetchChannelProgramsForChannels(List<ChannelItem> channelItems, int maxItems) throws Exception {
        return fetchChannelProgramsForChannels(channelItems, maxItems, true);
    }

    Map<String, List<EpgProgram>> fetchChannelProgramsForChannels(List<ChannelItem> channelItems, int maxItems, boolean allowOfflineSnapshotScan) throws Exception {
        return fetchChannelProgramsForChannels(channelItems, maxItems, allowOfflineSnapshotScan, false);
    }

    Map<String, List<EpgProgram>> fetchChannelProgramsForChannelsDirect(List<ChannelItem> channelItems, int maxItems) throws Exception {
        return fetchChannelProgramsForChannels(channelItems, maxItems, true, true);
    }

    private Map<String, List<EpgProgram>> fetchChannelProgramsForChannels(List<ChannelItem> channelItems, int maxItems, boolean allowOfflineSnapshotScan, boolean directSnapshotRead) throws Exception {
        Map<String, List<EpgProgram>> out = new LinkedHashMap<>();
        if (channelItems == null || channelItems.isEmpty()) {
            return out;
        }
        if (!canReadOfflineEpgSnapshot()) {
            for (ChannelItem channel : channelItems) {
                if (channel == null || channel.id == null || channel.id.trim().isEmpty()) {
                    continue;
                }
                out.put(channel.id.trim(), fetchChannelPrograms(channel, maxItems));
            }
            return out;
        }
        long now = System.currentTimeMillis();
        Map<String, List<EpgProgram>> targetedPrograms = directSnapshotRead
                ? loadOfflineProgramsForRequestedChannelsDirect(channelItems)
                : loadOfflineProgramsForRequestedChannels(channelItems, allowOfflineSnapshotScan);
        for (ChannelItem channel : channelItems) {
            if (channel == null || channel.isVod || channel.id == null || channel.id.trim().isEmpty()) {
                continue;
            }
            String channelId = channel.id.trim();
            List<EpgProgram> rows = targetedPrograms.get(channelId);
            List<EpgProgram> programs = new ArrayList<>();
            if (rows != null && !rows.isEmpty()) {
                int limit = maxItems <= 0 ? rows.size() : maxItems;
                for (EpgProgram program : rows) {
                    if (parseIsoMillis(program.endTime) <= now) {
                        continue;
                    }
                    programs.add(programWithProgress(program, now));
                    if (programs.size() >= limit) {
                        break;
                    }
                }
            }
            if (programs.isEmpty()) {
                programs.addAll(buildInlineProgramsForChannel(channel, maxItems, now));
            }
            out.put(channelId, programs);
        }
        return out;
    }

    private Map<String, List<EpgProgram>> loadOfflineProgramsForRequestedChannelsDirect(List<ChannelItem> channelItems) {
        Map<String, List<EpgProgram>> empty = new LinkedHashMap<>();
        if (snapshotStore == null || channelItems == null || channelItems.isEmpty()) {
            return empty;
        }
        Set<String> channelIds = new LinkedHashSet<>();
        for (ChannelItem channel : channelItems) {
            if (channel == null || channel.isVod || channel.id == null) {
                continue;
            }
            String clean = channel.id.trim();
            if (!clean.isEmpty()) {
                channelIds.add(clean);
            }
        }
        if (channelIds.isEmpty()) {
            return empty;
        }
        long startMs = System.currentTimeMillis();
        try {
            Map<String, List<EpgProgram>> programs = snapshotStore.loadEpgProgramsForChannelIdsDirect(channelIds);
            int programCount = 0;
            for (List<EpgProgram> rows : programs.values()) {
                programCount += rows == null ? 0 : rows.size();
            }
            Log.w(TAG, "offline EPG targeted loaded mode=direct requested="
                    + channelIds.size()
                    + " matched=" + programs.size()
                    + " programs=" + programCount
                    + " totalMs=" + (System.currentTimeMillis() - startMs));
            return programs;
        } catch (Exception e) {
            Log.w(TAG, "offline EPG direct targeted load failed requested=" + channelIds.size(), e);
            return new LinkedHashMap<>();
        }
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
        if (canReadOfflineEpgSnapshot()) {
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
        return fetchRemoteProgramForChannel(channelId, next);
    }

    EpgProgram fetchProgramForChannel(ChannelItem channel, boolean next) throws Exception {
        if (channel == null) {
            return null;
        }
        if (!canReadOfflineEpgSnapshot()) {
            EpgProgram direct = fetchRemoteProgramForChannel(channel.id, next);
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
        List<EpgProgram> rows = loadOfflineProgramsForRequestedChannel(channel);
        if (rows.isEmpty()) {
            List<EpgProgram> inlinePrograms = buildInlineProgramsForChannel(channel, next ? 2 : 1, now);
            EpgProgram inlineProgram = selectInlineProgram(channel, inlinePrograms, next);
            if (inlineProgram != null) {
                return inlineProgram;
            }
            try {
                EpgProgram direct = fetchRemoteProgramForChannel(channel.id, next);
                if (direct != null) {
                    return direct;
                }
                List<EpgProgram> fallbackPrograms = fetchRemoteProgramsForChannel(channel, next ? 2 : 1);
                if (!fallbackPrograms.isEmpty()) {
                    return next && fallbackPrograms.size() > 1 ? fallbackPrograms.get(1) : fallbackPrograms.get(0);
                }
            } catch (Exception e) {
                Log.w(TAG, "offline EPG single-program fallback failed channel="
                        + safeChannelLabel(channel)
                        + " next=" + next, e);
            }
            return null;
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
        List<EpgProgram> inlinePrograms = buildInlineProgramsForChannel(channel, next ? 2 : 1, now);
        return selectInlineProgram(channel, inlinePrograms, next);
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
        List<EpgProgram> rows = loadOfflineProgramsForRequestedChannelId(channelId);
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

    private Map<String, List<EpgProgram>> loadOfflineProgramsForRequestedChannels(List<ChannelItem> channelItems) {
        return loadOfflineProgramsForRequestedChannels(channelItems, true);
    }

    private Map<String, List<EpgProgram>> loadOfflineProgramsForRequestedChannels(List<ChannelItem> channelItems, boolean allowSnapshotScan) {
        Map<String, List<EpgProgram>> empty = new LinkedHashMap<>();
        if (snapshotStore == null || channelItems == null || channelItems.isEmpty()) {
            return empty;
        }
        Set<String> channelIds = new LinkedHashSet<>();
        for (ChannelItem channel : channelItems) {
            if (channel == null || channel.isVod || channel.id == null) {
                continue;
            }
            String clean = channel.id.trim();
            if (!clean.isEmpty()) {
                channelIds.add(clean);
            }
        }
        return loadOfflineProgramsForChannelIds(channelIds, allowSnapshotScan);
    }

    private List<EpgProgram> loadOfflineProgramsForRequestedChannel(ChannelItem channel) {
        if (channel == null || channel.isVod) {
            return new ArrayList<>();
        }
        return loadOfflineProgramsForRequestedChannelId(channel.id);
    }

    private List<EpgProgram> loadOfflineProgramsForRequestedChannelId(String channelId) {
        String clean = channelId == null ? "" : channelId.trim();
        if (clean.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, List<EpgProgram>> rowsByChannel = loadOfflineProgramsForChannelIds(java.util.Collections.singleton(clean));
        List<EpgProgram> rows = rowsByChannel.get(clean);
        return rows == null ? new ArrayList<>() : rows;
    }

    private Map<String, List<EpgProgram>> loadOfflineProgramsForChannelIds(Set<String> channelIds) {
        return loadOfflineProgramsForChannelIds(channelIds, true);
    }

    private Map<String, List<EpgProgram>> loadOfflineProgramsForChannelIds(Set<String> channelIds, boolean allowSnapshotScan) {
        if (snapshotStore == null || channelIds == null || channelIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        long startMs = System.currentTimeMillis();
        try {
            Map<String, List<EpgProgram>> programs = allowSnapshotScan
                    ? snapshotStore.loadEpgProgramsForChannelIds(channelIds)
                    : snapshotStore.loadCachedEpgProgramsForChannelIds(channelIds);
            int programCount = 0;
            for (List<EpgProgram> rows : programs.values()) {
                programCount += rows == null ? 0 : rows.size();
            }
            Log.w(TAG, "offline EPG targeted loaded mode="
                    + (allowSnapshotScan ? "scan" : "cache")
                    + " requested="
                    + channelIds.size()
                    + " matched=" + programs.size()
                    + " programs=" + programCount
                    + " totalMs=" + (System.currentTimeMillis() - startMs));
            return programs;
        } catch (Exception e) {
            Log.w(TAG, "offline EPG targeted load failed requested=" + channelIds.size(), e);
            return new LinkedHashMap<>();
        }
    }

    private Map<String, List<EpgProgram>> loadOfflineProgramMap() throws Exception {
        CachedOfflineProgramMap cached = loadCachedOfflineProgramMap();
        return cached == null ? new LinkedHashMap<>() : cached.byChannelId;
    }

    private boolean shouldUseOfflineEpg() {
        if (!standaloneMode) {
            return false;
        }
        if (!canReadOfflineEpgSnapshot()) {
            return false;
        }
        try {
            CachedOfflineProgramMap cached = loadCachedOfflineProgramMap();
            boolean usable = cached != null && !cached.byChannelId.isEmpty();
            if (!usable) {
                Log.w(TAG, "offline EPG disabled: program map empty");
            }
            return usable;
        } catch (Exception e) {
            Log.w(TAG, "offline EPG disabled: failed to load program map", e);
            return false;
        }
    }

    private boolean canReadOfflineEpgSnapshot() {
        return standaloneMode && shouldReadOfflineEpgSnapshot();
    }

    private boolean shouldReadOfflineEpgSnapshot() {
        if (snapshotStore == null) {
            Log.w(TAG, "offline EPG disabled: snapshot store missing");
            return false;
        }
        CatalogSnapshotStore.SnapshotStatus status = snapshotStore.getStatus(baseUrl);
        if (status == null) {
            Log.w(TAG, "offline EPG disabled: snapshot status missing");
            return false;
        }
        if (!status.available) {
            Log.w(TAG, "offline EPG disabled: snapshot unavailable");
            return false;
        }
        if (status.expired) {
            Log.w(TAG, "offline EPG disabled: snapshot expired");
            return false;
        }
        if (status.epgProgramCount <= 0) {
            Log.w(TAG, "offline EPG disabled: snapshot has no programs");
            return false;
        }
        if (status.sizeBytes > MAX_OFFLINE_EPG_SNAPSHOT_BYTES) {
            Log.w(TAG, "offline EPG disabled: snapshot too large sizeBytes="
                    + status.sizeBytes
                    + " maxBytes=" + MAX_OFFLINE_EPG_SNAPSHOT_BYTES
                    + " epgPrograms=" + status.epgProgramCount);
            return false;
        }
        return true;
    }

    private Map<String, EpgProgramPair> fetchRemoteProgramPairsForChannels(List<ChannelItem> channelItems) throws Exception {
        return fetchRemoteProgramPairsForChannels(channelItems, REMOTE_EPG_CONNECT_TIMEOUT_MS, REMOTE_EPG_READ_TIMEOUT_MS);
    }

    private Map<String, EpgProgramPair> fetchRemoteProgramPairsForChannels(List<ChannelItem> channelItems, int connectTimeoutMs, int readTimeoutMs) throws Exception {
        return fetchRemoteProgramPairsForChannels(channelItems, connectTimeoutMs, readTimeoutMs, false);
    }

    private Map<String, EpgProgramPair> fetchRemoteProgramPairsForChannels(List<ChannelItem> channelItems, int connectTimeoutMs, int readTimeoutMs, boolean offlinePublicOnly) throws Exception {
        Map<String, EpgProgramPair> out = new LinkedHashMap<>();
        if (channelItems == null || channelItems.isEmpty()) {
            return out;
        }
        if (channelItems.size() <= 3) {
            for (ChannelItem channel : channelItems) {
                if (channel == null || channel.isVod || channel.id == null || channel.id.trim().isEmpty()) {
                    continue;
                }
                EpgProgramPair pair = fetchRemoteProgramPairForChannel(channel, connectTimeoutMs, readTimeoutMs, offlinePublicOnly);
                if (pair != null && (pair.current != null || pair.next != null)) {
                    out.put(channel.id.trim(), pair);
                }
            }
            if (!out.isEmpty()) {
                Log.w(TAG, "EPG remote channel loaded channels="
                        + channelItems.size()
                        + " matched=" + out.size()
                        + " offlinePublicOnly=" + offlinePublicOnly);
                return out;
            }
        }
        RemoteProgramIndex index = buildRemoteProgramIndex(fetchRemoteNowProgramsDetailed(connectTimeoutMs, readTimeoutMs, offlinePublicOnly));
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

    private EpgProgramPair fetchRemoteProgramPairForChannel(ChannelItem channel, int connectTimeoutMs, int readTimeoutMs, boolean offlinePublicOnly) throws Exception {
        if (channel == null || channel.id == null || channel.id.trim().isEmpty()) {
            return null;
        }
        List<EpgProgram> programs = fetchRemoteChannelPrograms(channel.id, 24, connectTimeoutMs, readTimeoutMs, offlinePublicOnly);
        if (programs.isEmpty()) {
            return null;
        }
        List<EpgProgram> matchingPrograms = filterProgramsForChannel(programs, channel);
        if (matchingPrograms.isEmpty()) {
            Log.w(TAG, "EPG remote channel mismatch requested="
                    + safeChannelLabel(channel)
                    + " first="
                    + (programs.get(0) == null ? "" : programs.get(0).channelName)
                    + " firstTvg="
                    + (programs.get(0) == null ? "" : programs.get(0).tvgId)
                    + " firstId="
                    + (programs.get(0) == null ? "" : programs.get(0).channelId));
            return null;
        }
        programs = matchingPrograms;
        long now = System.currentTimeMillis();
        EpgProgram current = null;
        EpgProgram next = null;
        for (EpgProgram program : programs) {
            long startMs = parseIsoMillis(program.startTime);
            long endMs = parseIsoMillis(program.endTime);
            if (current == null && startMs <= now && endMs > now) {
                current = programWithProgress(program, now);
                continue;
            }
            if (startMs > now && (next == null || startMs < parseIsoMillis(next.startTime))) {
                next = programWithProgress(program, now);
            }
        }
        if (current == null && !programs.isEmpty()) {
            current = programWithProgress(programs.get(0), now);
            if (programs.size() > 1 && next == null) {
                next = programWithProgress(programs.get(1), now);
            }
        }
        return new EpgProgramPair(current, next);
    }

    private static List<EpgProgram> filterProgramsForChannel(List<EpgProgram> programs, ChannelItem channel) {
        List<EpgProgram> out = new ArrayList<>();
        if (programs == null || programs.isEmpty() || channel == null) {
            return out;
        }
        for (EpgProgram program : programs) {
            if (programMatchesChannel(program, channel)) {
                out.add(program);
            }
        }
        return out;
    }

    private static boolean programMatchesChannel(EpgProgram program, ChannelItem channel) {
        if (program == null || channel == null) {
            return false;
        }
        String channelTvgId = normalizeLookupKey(channel.tvgId);
        String programTvgId = normalizeLookupKey(program.tvgId);
        if (!channelTvgId.isEmpty() && channelTvgId.equals(programTvgId)) {
            return true;
        }
        String channelName = normalizeLookupKey(channel.name);
        String programName = normalizeLookupKey(program.channelName);
        if (!channelName.isEmpty() && channelName.equals(programName)) {
            return true;
        }
        String channelId = normalizeLookupKey(channel.id);
        String programChannelId = normalizeLookupKey(program.channelId);
        return !channelId.isEmpty()
                && channelId.equals(programChannelId)
                && (programName.isEmpty() || channelName.isEmpty() || channelName.equals(programName));
    }

    private List<EpgProgram> fetchRemoteChannelPrograms(String channelId, int maxItems) throws Exception {
        return fetchRemoteChannelPrograms(channelId, maxItems, REMOTE_EPG_CONNECT_TIMEOUT_MS, REMOTE_EPG_READ_TIMEOUT_MS, false);
    }

    private List<EpgProgram> fetchRemoteChannelPrograms(String channelId, int maxItems, int connectTimeoutMs, int readTimeoutMs, boolean offlinePublicOnly) throws Exception {
        String cleanChannelId = channelId == null ? "" : channelId.trim();
        if (cleanChannelId.isEmpty()) {
            return new ArrayList<>();
        }
        HttpClient.Response response = getRemoteEpg("/api/epg/channel/" + cleanChannelId, connectTimeoutMs, readTimeoutMs, offlinePublicOnly);
        if (!response.isSuccessful()) {
            return new ArrayList<>();
        }
        String body = response.body == null ? "" : response.body.trim();
        if (body.isEmpty() || "null".equalsIgnoreCase(body)) {
            return new ArrayList<>();
        }
        JSONArray arr = httpClient.parseArray(body, "cargando guia EPG del canal");
        List<EpgProgram> programs = new ArrayList<>();
        int limit = maxItems <= 0 ? arr.length() : Math.min(arr.length(), maxItems);
        for (int i = 0; i < limit; i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) {
                continue;
            }
            programs.add(fromJson(item));
        }
        return programs;
    }

    private EpgProgram fetchRemoteProgramForChannel(String channelId, boolean next) throws Exception {
        String cleanChannelId = channelId == null ? "" : channelId.trim();
        if (cleanChannelId.isEmpty()) {
            return null;
        }
        HttpClient.Response response = getRemoteEpg("/api/epg/channel/" + cleanChannelId + (next ? "/next" : "/current"));
        if (response.code == 404) {
            return null;
        }
        return fromJson(httpClient.parseObject(httpClient.requireSuccess(response, "cargando programa EPG").body, "cargando programa EPG"));
    }

    private List<EpgProgram> fetchRemoteNowProgramsDetailed() throws Exception {
        return fetchRemoteNowProgramsDetailed(REMOTE_EPG_CONNECT_TIMEOUT_MS, REMOTE_EPG_READ_TIMEOUT_MS);
    }

    private List<EpgProgram> fetchRemoteNowProgramsDetailed(int connectTimeoutMs, int readTimeoutMs) throws Exception {
        return fetchRemoteNowProgramsDetailed(connectTimeoutMs, readTimeoutMs, false);
    }

    private List<EpgProgram> fetchRemoteNowProgramsDetailed(int connectTimeoutMs, int readTimeoutMs, boolean offlinePublicOnly) throws Exception {
        HttpClient.Response response = getRemoteEpg("/api/epg/now", connectTimeoutMs, readTimeoutMs, offlinePublicOnly);
        if (!response.isSuccessful()) {
            Log.w(TAG, "EPG remote now unavailable code="
                    + response.code
                    + " offlinePublicOnly=" + offlinePublicOnly);
            return new ArrayList<>();
        }
        List<EpgProgram> programs = parseProgramsArray(response.body, "cargando EPG actual");
        if (programs.isEmpty()) {
            Log.w(TAG, "EPG remote now empty offlinePublicOnly=" + offlinePublicOnly);
        }
        return programs;
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
        return getRemoteEpg(path, REMOTE_EPG_CONNECT_TIMEOUT_MS, REMOTE_EPG_READ_TIMEOUT_MS);
    }

    private HttpClient.Response getRemoteEpg(String path, int connectTimeoutMs, int readTimeoutMs) throws Exception {
        return getRemoteEpg(path, connectTimeoutMs, readTimeoutMs, false);
    }

    private HttpClient.Response getRemoteEpg(String path, int connectTimeoutMs, int readTimeoutMs, boolean offlinePublicOnly) throws Exception {
        Exception firstError = null;
        HttpClient.Response firstHttpError = null;
        int safeConnectTimeoutMs = Math.max(500, connectTimeoutMs);
        int safeReadTimeoutMs = Math.max(500, readTimeoutMs);
        for (String candidate : remoteEpgBaseUrlCandidates(offlinePublicOnly)) {
            try {
                HttpClient.Response response = httpClient.get(candidate + path, safeConnectTimeoutMs, safeReadTimeoutMs, buildRequestHeaders());
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
        return remoteEpgBaseUrlCandidates(false);
    }

    private List<String> remoteEpgBaseUrlCandidates(boolean offlinePublicOnly) {
        List<String> candidates = new ArrayList<>();
        if (offlinePublicOnly) {
            addCandidate(candidates, OFFLINE_PUBLIC_BASE_URL);
            return candidates;
        }
        if (standaloneMode) {
            addCandidate(candidates, OFFLINE_PUBLIC_BASE_URL);
        }
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
        String tvgId = normalizeLookupKey(channel.tvgId);
        if (!tvgId.isEmpty() && index.byTvgId.containsKey(tvgId)) {
            return index.byTvgId.get(tvgId);
        }
        String name = normalizeLookupKey(channel.name);
        if (!name.isEmpty() && index.byChannelName.containsKey(name)) {
            return index.byChannelName.get(name);
        }
        String channelId = normalizeLookupKey(channel.id);
        if (!channelId.isEmpty() && index.byChannelId.containsKey(channelId)) {
            EpgProgram candidate = index.byChannelId.get(channelId);
            return programMatchesChannel(candidate, channel) ? candidate : null;
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

    private static String safeChannelLabel(ChannelItem channel) {
        if (channel == null) {
            return "";
        }
        String id = channel.id == null ? "" : channel.id.trim();
        String name = channel.name == null ? "" : channel.name.trim();
        if (id.isEmpty()) {
            return name;
        }
        if (name.isEmpty()) {
            return id;
        }
        return id + " " + name;
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
