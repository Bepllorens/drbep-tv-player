package com.drbep.tvplayer;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends FragmentActivity {
    private static final String TAG = "DRBEP-TV-Native";
    private static final long OVERLAY_HIDE_MS = 6000L;
    private static final long STATUS_HIDE_MS = 2500L;
    private static final long MENU_DOUBLE_PRESS_MS = 450L;
    private static final String PREFS = "drbep_tv_prefs";
    private static final String PREF_LAST_CHANNEL_ID = "last_channel_id";
    private static final String PREF_FAVORITES = "favorite_channel_ids";
    private static final String PREF_REMINDERS = "channel_reminders";
    private static final int FILTER_ALL = 0;
    private static final int FILTER_PLATFORM = 1;
    private static final int FILTER_CUSTOM_GROUP = 2;
    private static final int FILTER_VOD = 3;

    private PlayerView playerView;
    private TextView errorText;
    private TextView statusText;
    private TextView filterText;
    private View channelOverlay;
    private RecyclerView channelList;

    private PlayerController playerController;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final List<ChannelItem> channels = new ArrayList<>();
    private final List<ChannelItem> allChannels = new ArrayList<>();
    private final List<ChannelFilter> filters = new ArrayList<>();
    private final Map<String, String> epgNowByChannelId = new HashMap<>();
    private final List<ReminderItem> reminders = new ArrayList<>();
    private ChannelAdapter channelAdapter;
    private String baseUrl;
    private SharedPreferences prefs;

    private int currentIndex = -1;
    private int selectedOverlayIndex = 0;
    private boolean favoritesOnly;
    private long lastMenuPressedAtMs;
    private String lastChannelId;
    private String selectedFilterKey = "all";
    private final Set<String> favoriteChannelIds = new HashSet<>();
    private final Map<String, PlayerController.StreamInfo> streamInfoByChannelId = new HashMap<>();

    private final Runnable hideOverlayRunnable = this::hideOverlay;
    private final Runnable hideStatusRunnable = () -> {
        if (statusText != null) {
            statusText.setVisibility(View.GONE);
        }
    };
    private final Runnable reminderTickRunnable = new Runnable() {
        @Override
        public void run() {
            checkReminderNotifications();
            uiHandler.postDelayed(this, 30000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.playerView);
        errorText = findViewById(R.id.errorText);
        statusText = findViewById(R.id.statusText);
        filterText = findViewById(R.id.filterText);
        channelOverlay = findViewById(R.id.channelOverlay);
        channelList = findViewById(R.id.channelList);

        baseUrl = resolveBaseUrl();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        lastChannelId = prefs.getString(PREF_LAST_CHANNEL_ID, "");
        favoritesOnly = false;
        lastMenuPressedAtMs = 0L;
        Set<String> storedFavorites = prefs.getStringSet(PREF_FAVORITES, new HashSet<>());
        if (storedFavorites != null) {
            favoriteChannelIds.addAll(storedFavorites);
        }
        loadReminders();

        setupPlayer();
        setupChannelList();
        enableImmersiveMode();
        loadChannels();
        uiHandler.postDelayed(reminderTickRunnable, 30000L);
    }

    private String resolveBaseUrl() {
        String raw = BuildConfig.PLAYER_URL;
        if (BuildConfig.FORCE_FIRESTICK_URL && BuildConfig.FIRESTICK_LOCKED_URL != null && !BuildConfig.FIRESTICK_LOCKED_URL.trim().isEmpty()) {
            raw = BuildConfig.FIRESTICK_LOCKED_URL;
        }
        if (raw == null || raw.trim().isEmpty()) {
            return "http://127.0.0.1:8080";
        }
        Uri uri = Uri.parse(raw.trim());
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        if (scheme == null || host == null) {
            return "http://127.0.0.1:8080";
        }
        if (port > 0) {
            return scheme + "://" + host + ":" + port;
        }
        return scheme + "://" + host;
    }

    private void setupPlayer() {
        playerController = new PlayerController(this, playerView, baseUrl, ioExecutor, uiHandler, new PlayerController.Host() {
            @Override
            public void showStatus(String text) {
                MainActivity.this.showStatus(text);
            }

            @Override
            public void showError(String text) {
                MainActivity.this.showError(text);
            }

            @Override
            public void hideError() {
                MainActivity.this.hideError();
            }

            @Override
            public boolean isChannelCurrent(String channelId) {
                ChannelItem current = (currentIndex >= 0 && currentIndex < channels.size()) ? channels.get(currentIndex) : null;
                return current != null && channelId != null && channelId.equals(current.id);
            }
        });
        playerController.initialize();
    }

    private void setupChannelList() {
        channelAdapter = new ChannelAdapter();
        channelList.setLayoutManager(new LinearLayoutManager(this));
        channelList.setAdapter(channelAdapter);
    }

    private void loadChannels() {
        showStatus(getString(R.string.status_loading_channels));
        ioExecutor.execute(() -> {
            try {
                CatalogLoadResult result = fetchCatalogChannels();
                uiHandler.post(() -> applyLoadedChannels(result));
            } catch (Exception catalogErr) {
                Log.w(TAG, "catalog load failed, fallback to /api/channels", catalogErr);
                try {
                    CatalogLoadResult fallback = fetchActiveChannels();
                    uiHandler.post(() -> applyLoadedChannels(fallback));
                } catch (Exception e) {
                    Log.e(TAG, "load channels failed", e);
                    uiHandler.post(() -> showError(getString(R.string.error_load_channels, e.getMessage())));
                }
            }
        });
    }

    private CatalogLoadResult fetchCatalogChannels() throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/api/channels/catalog?include_disabled=0");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("HTTP " + code + " cargando catalogo");
            }

            String body = readAll(conn.getInputStream());
            JSONObject payload = new JSONObject(body);
            JSONArray arr = payload.optJSONArray("channels");
            if (arr == null) {
                arr = new JSONArray();
            }

            List<ChannelItem> parsed = new ArrayList<>(arr.length());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                String id = o.optString("id", "").trim();
                if (id.isEmpty() || "null".equalsIgnoreCase(id)) {
                    long numericID = o.optLong("id", 0L);
                    if (numericID > 0) {
                        id = String.valueOf(numericID);
                    }
                }
                String name = o.optString("name", "Canal").trim();
                if ("0".equals(id) || id.isEmpty() || name.isEmpty()) {
                    continue;
                }

                String logo = o.optString("logo", "").trim();
                String sourceGroup = o.optString("group", "").trim();
                String playbackUrl = baseUrl + "/live/" + id;
                String fallbackUrl = buildFallbackPlayUrl(id);
                String externalId = o.optString("external_id", "").trim();
                String tvgId = o.optString("tvg_id", "").trim();

                int platformId = (int) o.optLong("platform_id", 0L);
                String platformName = o.optString("platform_name", "").trim();
                int sortOrder = o.optInt("sort_order", Integer.MAX_VALUE);
                if (sortOrder <= 0) {
                    sortOrder = o.optInt("dial", i + 1);
                }
                boolean isVod = o.optBoolean("is_vod", false) || isLikelyVod(externalId, name, tvgId, sourceGroup);

                List<String> customGroups = new ArrayList<>();
                JSONArray groupsArr = o.optJSONArray("custom_groups");
                if (groupsArr != null) {
                    for (int j = 0; j < groupsArr.length(); j++) {
                        String g = groupsArr.optString(j, "").trim();
                        if (!g.isEmpty()) {
                            customGroups.add(g);
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

            long activePlatformID = payload.optLong("active_platform_id", 0L);
            List<ChannelFilter> parsedFilters = buildFiltersFromCatalog(parsed, activePlatformID);
            String defaultFilterKey = "all";
            return new CatalogLoadResult(parsed, parsedFilters, defaultFilterKey);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private CatalogLoadResult fetchActiveChannels() throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/api/channels");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("HTTP " + code + " cargando canales");
            }

            String body = readAll(conn.getInputStream());
            JSONArray arr = new JSONArray(body);
            List<ChannelItem> parsed = new ArrayList<>(arr.length());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                String id = o.optString("id", "").trim();
                String name = o.optString("name", "Canal").trim();
                String logo = o.optString("logo", "").trim();
                String playUrl = o.optString("play_url", "").trim();
                String sourceGroup = o.optString("group", "").trim();
                if (id.isEmpty() || playUrl.isEmpty()) {
                    continue;
                }
                if (playUrl.startsWith("/")) {
                    playUrl = baseUrl + playUrl;
                }
                String fallbackUrl = buildFallbackPlayUrl(id);
                parsed.add(new ChannelItem(
                        id,
                        name,
                        logo,
                        sourceGroup,
                        playUrl,
                        fallbackUrl,
                        i,
                        i + 1,
                        isLikelyVod("", name, "", sourceGroup),
                        0,
                        "Plataforma activa",
                        new ArrayList<>()
                ));
            }

            List<ChannelFilter> parsedFilters = new ArrayList<>();
            parsedFilters.add(new ChannelFilter("all", "Todos", FILTER_ALL, 0, ""));
            return new CatalogLoadResult(parsed, parsedFilters, "all");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private List<ChannelFilter> buildFiltersFromCatalog(List<ChannelItem> parsed, long activePlatformID) {
        LinkedHashMap<String, ChannelFilter> byKey = new LinkedHashMap<>();
        byKey.put("all", new ChannelFilter("all", "Todos", FILTER_ALL, 0, ""));

        Map<Integer, String> platformNames = new LinkedHashMap<>();
        Set<String> customGroupNames = new HashSet<>();
        for (ChannelItem item : parsed) {
            if (item.platformId > 0 && !platformNames.containsKey(item.platformId)) {
                String pName = item.platformName == null ? "" : item.platformName.trim();
                if (pName.isEmpty()) {
                    pName = "ID " + item.platformId;
                }
                platformNames.put(item.platformId, pName);
            }
            for (String g : item.customGroups) {
                String trimmed = g == null ? "" : g.trim();
                if (!trimmed.isEmpty()) {
                    customGroupNames.add(trimmed);
                }
            }
        }

        if (activePlatformID > 0) {
            int activeID = (int) activePlatformID;
            String activeName = platformNames.containsKey(activeID) ? platformNames.get(activeID) : ("ID " + activeID);
            String key = "platform:" + activeID;
            byKey.put(key, new ChannelFilter(key, "Plataforma activa: " + activeName, FILTER_PLATFORM, activeID, ""));
        }

        List<Integer> platformIDs = new ArrayList<>(platformNames.keySet());
        Collections.sort(platformIDs);
        for (int pid : platformIDs) {
            String key = "platform:" + pid;
            if (byKey.containsKey(key)) {
                continue;
            }
            byKey.put(key, new ChannelFilter(key, "Plataforma: " + platformNames.get(pid), FILTER_PLATFORM, pid, ""));
        }

        List<String> groupNames = new ArrayList<>(customGroupNames);
        groupNames.sort(String::compareToIgnoreCase);
        for (String g : groupNames) {
            String key = "custom-group:" + g.toLowerCase(Locale.ROOT);
            byKey.put(key, new ChannelFilter(key, "Grupo: " + g, FILTER_CUSTOM_GROUP, 0, g));
        }
        byKey.put("vod", new ChannelFilter("vod", "VOD", FILTER_VOD, 0, ""));

        return new ArrayList<>(byKey.values());
    }

    private void applyLoadedChannels(CatalogLoadResult result) {
        allChannels.clear();
        allChannels.addAll(result.channels);

        filters.clear();
        filters.addAll(result.filters);

        int foundFilterIndex = findFilterIndexByKey(selectedFilterKey);
        if (foundFilterIndex < 0) {
            foundFilterIndex = findFilterIndexByKey(result.defaultFilterKey);
        }
        if (foundFilterIndex < 0) {
            foundFilterIndex = 0;
        }
        selectedFilterKey = filters.isEmpty() ? "all" : filters.get(foundFilterIndex).key;

        rebuildVisibleChannels(lastChannelId, lastChannelId);
        channelAdapter.notifyDataSetChanged();
        updateFilterText();

        if (channels.isEmpty()) {
            showError(getString(R.string.error_no_channels_for_filter));
            return;
        }

        int startIndex = 0;
        if (lastChannelId != null && !lastChannelId.trim().isEmpty()) {
            int found = findChannelIndexById(lastChannelId);
            if (found >= 0) {
                startIndex = found;
            }
        }
        tuneToIndex(startIndex, true);
        loadEpgNow();
    }

    private void tuneToIndex(int index, boolean autoPlay) {
        if (channels.isEmpty()) {
            return;
        }
        if (index < 0) {
            index = channels.size() - 1;
        }
        if (index >= channels.size()) {
            index = 0;
        }

        currentIndex = index;
        selectedOverlayIndex = index;
        channelAdapter.notifyDataSetChanged();
        channelList.scrollToPosition(index);

        ChannelItem ch = channels.get(index);
        saveLastChannelId(ch.id);
        playerController.resetFallbackState();
        PlayerController.StreamInfo cachedStreamInfo = streamInfoByChannelId.get(ch.id);
        PlayerController.PlaybackRequest playbackRequest = toPlaybackRequest(ch);
        playerController.playChannel(playbackRequest, autoPlay, cachedStreamInfo);
        playerController.resolveStreamInfoAndReplayIfNeeded(playbackRequest, autoPlay, streamInfoByChannelId);

        hideError();
        showStatus(ch.name);
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isLikelyVod(String externalId, String name, String tvgId, String groupTitle) {
        String n = safeLower(name);
        String t = safeLower(tvgId);
        String g = safeLower(groupTitle);
        return containsVodToken(n) || containsVodToken(t) || containsVodToken(g);
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

    private void loadEpgNow() {
        ioExecutor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(baseUrl + "/api/epg/now");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    return;
                }

                JSONArray arr = new JSONArray(readAll(conn.getInputStream()));
                Map<String, String> updates = new HashMap<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) {
                        continue;
                    }
                    String channelId = String.valueOf(o.optLong("channel_id", -1L));
                    if ("-1".equals(channelId)) {
                        continue;
                    }
                    String title = o.optString("title", "").trim();
                    int progress = o.optInt("progress", -1);
                    if (title.isEmpty()) {
                        continue;
                    }
                    if (progress >= 0) {
                        title = title + " (" + progress + "%)";
                    }
                    updates.put(channelId, title);
                }

                uiHandler.post(() -> {
                    epgNowByChannelId.clear();
                    epgNowByChannelId.putAll(updates);
                    for (ChannelItem item : allChannels) {
                        item.nowProgram = epgNowByChannelId.getOrDefault(item.id, "");
                    }
                    channelAdapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                Log.w(TAG, "load epg now failed", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private void showChannelActionMenu() {
        if (channels.isEmpty() || selectedOverlayIndex < 0 || selectedOverlayIndex >= channels.size()) {
            return;
        }
        ChannelItem ch = channels.get(selectedOverlayIndex);
        boolean fav = favoriteChannelIds.contains(ch.id);

        String[] options = new String[]{
                "Sintonizar",
                fav ? "Quitar de favoritos" : "Añadir a favoritos",
                "Mini guia",
                "Grabar programa en emision",
                "Grabar proximo programa",
                "Crear recordatorio (ahora)",
                "Crear recordatorio (proximo)",
                "Ver grabaciones"
        };

        new AlertDialog.Builder(this)
                .setTitle(ch.name)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            tuneToIndex(selectedOverlayIndex, true);
                            hideOverlay();
                            break;
                        case 1:
                            toggleFavoriteSelected();
                            break;
                        case 2:
                            openMiniGuideForChannel(ch);
                            break;
                        case 3:
                            createScheduleFromEndpoint(ch, false);
                            break;
                        case 4:
                            createScheduleFromEndpoint(ch, true);
                            break;
                        case 5:
                            createReminderFromEndpoint(ch, false);
                            break;
                        case 6:
                            createReminderFromEndpoint(ch, true);
                            break;
                        case 7:
                            openRecordingsBrowser();
                            break;
                        default:
                            break;
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void openMiniGuideForChannel(ChannelItem ch) {
        if (ch == null) {
            return;
        }
        showStatus(getString(R.string.status_loading_guide));
        ioExecutor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(baseUrl + "/api/epg/channel/" + ch.id);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Accept", "application/json");
                if (conn.getResponseCode() < 200 || conn.getResponseCode() >= 300) {
                    throw new IllegalStateException("EPG HTTP " + conn.getResponseCode());
                }

                JSONArray arr = new JSONArray(readAll(conn.getInputStream()));
                List<JSONObject> items = new ArrayList<>();
                List<String> lines = new ArrayList<>();
                int max = Math.min(arr.length(), 8);
                for (int i = 0; i < max; i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) {
                        continue;
                    }
                    String title = o.optString("title", "Sin titulo");
                    String start = shortTime(o.optString("start_time", ""));
                    String end = shortTime(o.optString("end_time", ""));
                    lines.add(start + " - " + end + "  " + title);
                    items.add(o);
                }

                uiHandler.post(() -> {
                    if (lines.isEmpty()) {
                        showStatus(getString(R.string.status_no_epg_for_channel));
                        return;
                    }
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(getString(R.string.title_guide, ch.name))
                            .setItems(lines.toArray(new String[0]), (d, index) -> {
                                if (index >= 0 && index < items.size()) {
                                    showProgramActionMenu(ch, items.get(index));
                                }
                            })
                            .setNegativeButton(R.string.dialog_close, null)
                            .show();
                });
            } catch (Exception e) {
                Log.w(TAG, "mini guide failed", e);
                uiHandler.post(() -> showStatus(getString(R.string.status_failed_load_guide)));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private void showProgramActionMenu(ChannelItem ch, JSONObject program) {
        if (ch == null || program == null) {
            return;
        }
        String title = program.optString("title", "Programa");
        String[] options = new String[]{"Grabar", "Recordatorio"};
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        scheduleProgram(ch, program);
                    } else if (which == 1) {
                        createReminder(ch, program);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void createScheduleFromEndpoint(ChannelItem ch, boolean next) {
        fetchProgramForChannel(ch, next, false);
    }

    private void createReminderFromEndpoint(ChannelItem ch, boolean next) {
        fetchProgramForChannel(ch, next, true);
    }

    private void fetchProgramForChannel(ChannelItem ch, boolean next, boolean reminderOnly) {
        if (ch == null) {
            return;
        }
        String suffix = next ? "/next" : "/current";
        showStatus(getString(next ? R.string.status_searching_next_program : R.string.status_searching_current_program));
        ioExecutor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(baseUrl + "/api/epg/channel/" + ch.id + suffix);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Accept", "application/json");
                int code = conn.getResponseCode();
                if (code == 404) {
                    uiHandler.post(() -> showStatus(getString(R.string.status_no_program_in_epg)));
                    return;
                }
                if (code < 200 || code >= 300) {
                    throw new IllegalStateException("EPG HTTP " + code);
                }
                JSONObject program = new JSONObject(readAll(conn.getInputStream()));
                uiHandler.post(() -> {
                    if (reminderOnly) {
                        createReminder(ch, program);
                    } else {
                        scheduleProgram(ch, program);
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "fetch program failed", e);
                uiHandler.post(() -> showStatus(getString(R.string.status_failed_get_program)));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private void scheduleProgram(ChannelItem ch, JSONObject program) {
        if (ch == null || program == null) {
            return;
        }
        ioExecutor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(baseUrl + "/api/recordings/schedule");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject req = new JSONObject();
                req.put("channel_id", Long.parseLong(ch.id));
                req.put("channel_name", ch.name);
                req.put("tvg_id", "");
                req.put("program_title", program.optString("title", ch.name));
                req.put("poster", program.optString("icon", ch.logoUrl));
                req.put("start_time", program.optString("start_time", ""));
                req.put("end_time", program.optString("end_time", ""));

                byte[] payload = req.toString().getBytes(StandardCharsets.UTF_8);
                conn.getOutputStream().write(payload);

                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    throw new IllegalStateException("schedule HTTP " + code);
                }
                uiHandler.post(() -> showStatus(getString(R.string.status_recording_scheduled)));
            } catch (Exception e) {
                Log.w(TAG, "schedule program failed", e);
                uiHandler.post(() -> showStatus(getString(R.string.status_failed_schedule_recording)));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private void createReminder(ChannelItem ch, JSONObject program) {
        if (ch == null || program == null) {
            return;
        }
        long startAt = parseIsoMillis(program.optString("start_time", ""));
        if (startAt <= 0) {
            showStatus(getString(R.string.status_failed_create_reminder));
            return;
        }
        ReminderItem item = new ReminderItem(ch.id, ch.name, program.optString("title", "Programa"), startAt, false);
        reminders.add(item);
        saveReminders();
        showStatus(getString(R.string.status_reminder_created));
    }

    private void openRecordingsBrowser() {
        showStatus(getString(R.string.status_loading_recordings));
        ioExecutor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(baseUrl + "/api/recordings/files");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(20000);
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    throw new IllegalStateException("recordings HTTP " + code);
                }

                JSONObject body = new JSONObject(readAll(conn.getInputStream()));
                String basePath = body.optString("path", "");
                JSONArray files = body.optJSONArray("files");
                if (files == null || files.length() == 0) {
                    uiHandler.post(() -> showStatus(getString(R.string.status_no_recordings)));
                    return;
                }

                List<RecordingItem> items = new ArrayList<>();
                List<String> labels = new ArrayList<>();
                for (int i = 0; i < files.length(); i++) {
                    JSONObject f = files.optJSONObject(i);
                    if (f == null) {
                        continue;
                    }
                    RecordingItem item = new RecordingItem(
                            f.optString("name", ""),
                            f.optString("path", ""),
                            f.optLong("size", 0L),
                            f.optString("modified", "")
                    );
                    items.add(item);
                    labels.add(item.name + (item.modified.isEmpty() ? "" : "  ·  " + item.modified));
                }

                uiHandler.post(() -> new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.title_recordings)
                        .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                            if (which >= 0 && which < items.size()) {
                                playRecording(items.get(which), basePath);
                            }
                        })
                        .setNegativeButton(R.string.dialog_close, null)
                        .show());
            } catch (Exception e) {
                Log.w(TAG, "open recordings failed", e);
                uiHandler.post(() -> showStatus(getString(R.string.status_failed_load_recordings)));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private void playRecording(RecordingItem item, String basePath) {
        if (item == null) {
            return;
        }
        String rel = item.path;
        if (basePath != null && !basePath.trim().isEmpty() && rel != null && rel.startsWith(basePath)) {
            rel = rel.substring(basePath.length());
            if (rel.startsWith("/")) {
                rel = rel.substring(1);
            }
        }
        if (rel == null || rel.trim().isEmpty()) {
            rel = item.name;
        }

        String encoded = encodePath(rel);
        String url = baseUrl + "/recordings/remux/" + encoded;
        playerController.playRecording(item.name, url);
        hideOverlay();
    }

    private static String encodePath(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String[] parts = raw.split("/");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append('/');
            }
            try {
                out.append(URLEncoder.encode(p, "UTF-8").replace("+", "%20"));
            } catch (Exception ignored) {
                out.append(p);
            }
        }
        return out.toString();
    }

    private void loadReminders() {
        reminders.clear();
        if (prefs == null) {
            return;
        }
        String raw = prefs.getString(PREF_REMINDERS, "[]");
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                reminders.add(new ReminderItem(
                        o.optString("channel_id", ""),
                        o.optString("channel_name", ""),
                        o.optString("title", "Programa"),
                        o.optLong("start_at", 0L),
                        o.optBoolean("notified", false)
                ));
            }
        } catch (Exception e) {
            Log.w(TAG, "load reminders failed", e);
        }
    }

    private void saveReminders() {
        if (prefs == null) {
            return;
        }
        JSONArray arr = new JSONArray();
        for (ReminderItem item : reminders) {
            JSONObject o = new JSONObject();
            try {
                o.put("channel_id", item.channelId);
                o.put("channel_name", item.channelName);
                o.put("title", item.title);
                o.put("start_at", item.startAtMillis);
                o.put("notified", item.notified);
                arr.put(o);
            } catch (Exception ignored) {
            }
        }
        prefs.edit().putString(PREF_REMINDERS, arr.toString()).apply();
    }

    private void checkReminderNotifications() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        List<ReminderItem> toRemove = new ArrayList<>();
        for (ReminderItem item : reminders) {
            if (item == null) {
                continue;
            }
            if (item.notified) {
                if (now > item.startAtMillis + 10 * 60 * 1000L) {
                    toRemove.add(item);
                    changed = true;
                }
                continue;
            }
            long delta = item.startAtMillis - now;
            if (delta <= 60 * 1000L && delta >= -60 * 1000L) {
                item.notified = true;
                changed = true;
                showStatus("Recordatorio: " + item.channelName + " - " + item.title);
            }
        }
        if (!toRemove.isEmpty()) {
            reminders.removeAll(toRemove);
        }
        if (changed) {
            saveReminders();
        }
    }

    private static long parseIsoMillis(String iso) {
        if (iso == null || iso.trim().isEmpty()) {
            return 0L;
        }
        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p, Locale.US);
                Date d = f.parse(iso);
                if (d != null) {
                    return d.getTime();
                }
            } catch (Exception ignored) {
            }
        }
        return 0L;
    }

    private static String shortTime(String iso) {
        long ms = parseIsoMillis(iso);
        if (ms <= 0L) {
            return "--:--";
        }
        SimpleDateFormat out = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return out.format(new Date(ms));
    }

    private void tuneRelative(int delta) {
        if (channels.isEmpty()) {
            return;
        }
        int next = (currentIndex < 0) ? 0 : currentIndex + delta;
        tuneToIndex(next, true);
    }

    private void moveOverlaySelection(int delta) {
        if (channels.isEmpty()) {
            return;
        }
        if (selectedOverlayIndex < 0 || selectedOverlayIndex >= channels.size()) {
            selectedOverlayIndex = currentIndex >= 0 ? currentIndex : 0;
        }
        selectedOverlayIndex += delta;
        if (selectedOverlayIndex < 0) {
            selectedOverlayIndex = channels.size() - 1;
        }
        if (selectedOverlayIndex >= channels.size()) {
            selectedOverlayIndex = 0;
        }
        channelAdapter.notifyDataSetChanged();
        channelList.scrollToPosition(selectedOverlayIndex);
        showOverlay();
    }

    private int findChannelIndexById(String channelID) {
        if (channelID == null || channelID.trim().isEmpty()) {
            return -1;
        }
        for (int i = 0; i < channels.size(); i++) {
            if (channelID.equals(channels.get(i).id)) {
                return i;
            }
        }
        return -1;
    }

    private void saveLastChannelId(String channelID) {
        lastChannelId = channelID;
        if (prefs != null && channelID != null) {
            prefs.edit().putString(PREF_LAST_CHANNEL_ID, channelID).apply();
        }
    }

    private void applyFavoritesAndSort(List<ChannelItem> target) {
        for (ChannelItem item : target) {
            item.favorite = favoriteChannelIds.contains(item.id);
        }
        target.sort((a, b) -> {
            int byDashboardOrder = Integer.compare(a.dashboardOrder, b.dashboardOrder);
            if (byDashboardOrder != 0) {
                return byDashboardOrder;
            }
            return Integer.compare(a.originalOrder, b.originalOrder);
        });
    }

    private void rebuildVisibleChannels(String keepCurrentChannelId, String keepSelectedChannelId) {
        applyFavoritesAndSort(allChannels);

        channels.clear();
        for (ChannelItem item : allChannels) {
            if (!channelMatchesCurrentFilter(item)) {
                continue;
            }
            if (!favoritesOnly || item.favorite) {
                channels.add(item);
            }
        }

        currentIndex = findChannelIndexById(keepCurrentChannelId);
        selectedOverlayIndex = findChannelIndexById(keepSelectedChannelId);

        if (selectedOverlayIndex < 0 && currentIndex >= 0) {
            selectedOverlayIndex = currentIndex;
        }
        if (selectedOverlayIndex < 0 && !channels.isEmpty()) {
            selectedOverlayIndex = 0;
        }
    }

    private boolean channelMatchesCurrentFilter(ChannelItem item) {
        ChannelFilter filter = getSelectedFilter();
        if (filter == null || filter.type == FILTER_ALL) {
            return true;
        }
        if (filter.type == FILTER_PLATFORM) {
            return item.platformId == filter.platformId && !item.isVod;
        }
        if (filter.type == FILTER_CUSTOM_GROUP) {
            for (String name : item.customGroups) {
                if (name != null && name.equalsIgnoreCase(filter.groupName)) {
                    return true;
                }
            }
            return false;
        }
        if (filter.type == FILTER_VOD) {
            return item.isVod;
        }
        return true;
    }

    private ChannelFilter getSelectedFilter() {
        for (ChannelFilter filter : filters) {
            if (filter.key.equals(selectedFilterKey)) {
                return filter;
            }
        }
        return filters.isEmpty() ? null : filters.get(0);
    }

    private int findFilterIndexByKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return -1;
        }
        for (int i = 0; i < filters.size(); i++) {
            if (key.equals(filters.get(i).key)) {
                return i;
            }
        }
        return -1;
    }

    private void cycleFilter(int delta) {
        if (filters.isEmpty()) {
            return;
        }
        int currentFilterIndex = findFilterIndexByKey(selectedFilterKey);
        if (currentFilterIndex < 0) {
            currentFilterIndex = 0;
        }

        int next = currentFilterIndex + delta;
        if (next < 0) {
            next = filters.size() - 1;
        }
        if (next >= filters.size()) {
            next = 0;
        }

        selectedFilterKey = filters.get(next).key;

        String keepCurrentID = (currentIndex >= 0 && currentIndex < channels.size()) ? channels.get(currentIndex).id : "";
        String keepSelectedID = (selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size()) ? channels.get(selectedOverlayIndex).id : keepCurrentID;

        rebuildVisibleChannels(keepCurrentID, keepSelectedID);
        channelAdapter.notifyDataSetChanged();
        updateFilterText();

        if (channels.isEmpty()) {
            showStatus(getString(R.string.status_no_channels_for_filter));
            showOverlay();
            return;
        }

        if (currentIndex < 0) {
            tuneToIndex(0, true);
        } else if (selectedOverlayIndex >= 0) {
            channelList.scrollToPosition(selectedOverlayIndex);
        }

        ChannelFilter filter = getSelectedFilter();
        if (filter != null) {
            showStatus("Filtro: " + filter.label);
        }
        showOverlay();
    }

    private void updateFilterText() {
        if (filterText == null) {
            return;
        }
        ChannelFilter filter = getSelectedFilter();
        if (filter == null) {
            filterText.setText(getString(R.string.filter_all_label));
            return;
        }
        filterText.setText("Filtro: " + filter.label);
    }

    private void saveFavorites() {
        if (prefs != null) {
            prefs.edit().putStringSet(PREF_FAVORITES, new HashSet<>(favoriteChannelIds)).apply();
        }
    }

    private void toggleFavoriteSelected() {
        if (channels.isEmpty() || selectedOverlayIndex < 0 || selectedOverlayIndex >= channels.size()) {
            return;
        }

        String selectedID = channels.get(selectedOverlayIndex).id;
        String currentID = (currentIndex >= 0 && currentIndex < channels.size()) ? channels.get(currentIndex).id : "";

        if (favoriteChannelIds.contains(selectedID)) {
            favoriteChannelIds.remove(selectedID);
            showStatus("Eliminado de favoritos");
        } else {
            favoriteChannelIds.add(selectedID);
            showStatus("Añadido a favoritos");
        }
        saveFavorites();

        rebuildVisibleChannels(currentID, selectedID);
        channelAdapter.notifyDataSetChanged();
        if (selectedOverlayIndex >= 0) {
            channelList.scrollToPosition(selectedOverlayIndex);
        }
        showOverlay();
    }

    private void toggleFavoritesOnlyMode() {
        String currentID = (currentIndex >= 0 && currentIndex < channels.size()) ? channels.get(currentIndex).id : "";
        String selectedID = (selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size()) ? channels.get(selectedOverlayIndex).id : currentID;

        favoritesOnly = !favoritesOnly;
        rebuildVisibleChannels(currentID, selectedID);
        channelAdapter.notifyDataSetChanged();

        if (channels.isEmpty() && favoritesOnly) {
            showStatus("Solo favoritos activado (sin favoritos)");
            return;
        }

        if (selectedOverlayIndex >= 0) {
            channelList.scrollToPosition(selectedOverlayIndex);
        }
        showStatus(favoritesOnly ? "Solo favoritos: ON" : "Solo favoritos: OFF");
        showOverlay();
    }

    private boolean isOverlayVisible() {
        return channelOverlay != null && channelOverlay.getVisibility() == View.VISIBLE;
    }

    private void showOverlay() {
        if (channelOverlay == null) {
            return;
        }
        channelOverlay.setVisibility(View.VISIBLE);
        uiHandler.removeCallbacks(hideOverlayRunnable);
        uiHandler.postDelayed(hideOverlayRunnable, OVERLAY_HIDE_MS);
    }

    private void hideOverlay() {
        if (channelOverlay == null) {
            return;
        }
        channelOverlay.setVisibility(View.GONE);
    }

    private void showStatus(String text) {
        if (statusText == null || text == null || text.trim().isEmpty()) {
            return;
        }
        statusText.setText(text);
        statusText.setVisibility(View.VISIBLE);
        uiHandler.removeCallbacks(hideStatusRunnable);
        uiHandler.postDelayed(hideStatusRunnable, STATUS_HIDE_MS);
    }

    private void showError(String reason) {
        if (errorText == null) {
            return;
        }
        errorText.setVisibility(View.VISIBLE);
        errorText.setText(getString(
            R.string.error_playback_details,
            reason == null ? getString(R.string.error_unknown_reason) : reason,
            baseUrl
        ));
    }

    private void hideError() {
        if (errorText != null) {
            errorText.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event);
        }

        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                long now = System.currentTimeMillis();
                if (now - lastMenuPressedAtMs <= MENU_DOUBLE_PRESS_MS) {
                    lastMenuPressedAtMs = 0L;
                    toggleFavoritesOnlyMode();
                    return true;
                }
                lastMenuPressedAtMs = now;
                if (isOverlayVisible()) {
                    hideOverlay();
                } else {
                    showOverlay();
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (isOverlayVisible()) {
                    hideOverlay();
                    return true;
                }
                finish();
                return true;
            case KeyEvent.KEYCODE_CHANNEL_UP:
            case KeyEvent.KEYCODE_PAGE_UP:
                tuneRelative(-1);
                return true;
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
            case KeyEvent.KEYCODE_PAGE_DOWN:
                tuneRelative(1);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (isOverlayVisible()) {
                    moveOverlaySelection(-1);
                } else {
                    tuneRelative(-1);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (isOverlayVisible()) {
                    moveOverlaySelection(1);
                } else {
                    tuneRelative(1);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (isOverlayVisible()) {
                    cycleFilter(-1);
                } else {
                    showOverlay();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (isOverlayVisible()) {
                    cycleFilter(1);
                } else {
                    showOverlay();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (isOverlayVisible()) {
                    tuneToIndex(selectedOverlayIndex, true);
                    hideOverlay();
                } else if (playerController != null) {
                    playerController.togglePlayback();
                }
                return true;
            case KeyEvent.KEYCODE_INFO:
                if (isOverlayVisible()) {
                    if (selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size()) {
                        openMiniGuideForChannel(channels.get(selectedOverlayIndex));
                    }
                } else {
                    showOverlay();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_RECORD:
                if (isOverlayVisible() && selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size()) {
                    createScheduleFromEndpoint(channels.get(selectedOverlayIndex), false);
                } else if (currentIndex >= 0 && currentIndex < channels.size()) {
                    createScheduleFromEndpoint(channels.get(currentIndex), false);
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (playerController != null) {
                    playerController.togglePlayback();
                    return true;
                }
                break;
            default:
                break;
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, @NonNull KeyEvent event) {
        if (isOverlayVisible() && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
            showChannelActionMenu();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersiveMode();
            if (playerView != null) {
                playerView.requestFocus();
            }
        }
    }

    private void enableImmersiveMode() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacksAndMessages(null);
        ioExecutor.shutdownNow();
        if (playerController != null) {
            playerController.release();
            playerController = null;
        }
        super.onDestroy();
    }

    static String readAll(InputStream in) throws Exception {
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
        }
        return out.toString();
    }

    private PlayerController.PlaybackRequest toPlaybackRequest(ChannelItem channelItem) {
        if (channelItem == null) {
            return null;
        }
        return new PlayerController.PlaybackRequest(channelItem.id, channelItem.name, channelItem.playUrl, channelItem.fallbackPlayUrl);
    }

    private static final class ChannelItem {
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

    private static final class ChannelFilter {
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

    private static final class CatalogLoadResult {
        final List<ChannelItem> channels;
        final List<ChannelFilter> filters;
        final String defaultFilterKey;

        CatalogLoadResult(List<ChannelItem> channels, List<ChannelFilter> filters, String defaultFilterKey) {
            this.channels = channels;
            this.filters = filters;
            this.defaultFilterKey = defaultFilterKey;
        }
    }

    private static final class RecordingItem {
        final String name;
        final String path;
        final long size;
        final String modified;

        RecordingItem(String name, String path, long size, String modified) {
            this.name = name;
            this.path = path;
            this.size = size;
            this.modified = modified;
        }
    }

    private static final class ReminderItem {
        final String channelId;
        final String channelName;
        final String title;
        final long startAtMillis;
        boolean notified;

        ReminderItem(String channelId, String channelName, String title, long startAtMillis, boolean notified) {
            this.channelId = channelId;
            this.channelName = channelName;
            this.title = title;
            this.startAtMillis = startAtMillis;
            this.notified = notified;
        }
    }

    private final class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelVH> {
        @NonNull
        @Override
        public ChannelVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_channel, parent, false);
            return new ChannelVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ChannelVH holder, int position) {
            ChannelItem ch = channels.get(position);
            holder.name.setText(ch.favorite ? "★ " + ch.name : ch.name);
            if (ch.nowProgram != null && !ch.nowProgram.trim().isEmpty()) {
                holder.meta.setText(ch.nowProgram);
            } else if (ch.group != null && !ch.group.trim().isEmpty()) {
                holder.meta.setText(ch.group);
            } else {
                holder.meta.setText("");
            }

            Glide.with(holder.logo.getContext())
                    .load(ch.logoUrl)
                    .centerCrop()
                    .placeholder(android.R.color.transparent)
                    .error(android.R.color.transparent)
                    .into(holder.logo);

            boolean selected = (position == selectedOverlayIndex);
            boolean tuned = (position == currentIndex);

            if (selected) {
                holder.itemView.setBackgroundColor(0xFF2D5BFF);
            } else if (tuned) {
                holder.itemView.setBackgroundColor(0x663F8CFF);
            } else {
                holder.itemView.setBackgroundColor(0x00000000);
            }

            holder.itemView.setOnClickListener(v -> {
                selectedOverlayIndex = position;
                tuneToIndex(position, true);
                hideOverlay();
            });
        }

        @Override
        public int getItemCount() {
            return channels.size();
        }

        class ChannelVH extends RecyclerView.ViewHolder {
            TextView name;
            TextView meta;
            ImageView logo;

            ChannelVH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.channelName);
                meta = itemView.findViewById(R.id.channelMeta);
                logo = itemView.findViewById(R.id.channelLogo);
            }
        }
    }
}
