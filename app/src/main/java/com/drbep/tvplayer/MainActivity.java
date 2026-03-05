package com.drbep.tvplayer;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.fragment.app.FragmentActivity;

public class MainActivity extends FragmentActivity {
    private static final String TAG = "DRBEP-TV-Native";
    private static final long OVERLAY_HIDE_MS = 6000L;
    private static final long STATUS_HIDE_MS = 2500L;
    private static final long MENU_DOUBLE_PRESS_MS = 450L;
    private static final String PREFS = "drbep_tv_prefs";
    private static final String PREF_LAST_CHANNEL_ID = "last_channel_id";
    private static final String PREF_FAVORITES = "favorite_channel_ids";

    private static final int FILTER_ALL = 0;
    private static final int FILTER_PLATFORM = 1;
    private static final int FILTER_CUSTOM_GROUP = 2;

    private PlayerView playerView;
    private TextView errorText;
    private TextView statusText;
    private TextView filterText;
    private View channelOverlay;
    private RecyclerView channelList;

    private ExoPlayer player;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final List<ChannelItem> channels = new ArrayList<>();
    private final List<ChannelItem> allChannels = new ArrayList<>();
    private final List<ChannelFilter> filters = new ArrayList<>();
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

    private final Runnable hideOverlayRunnable = this::hideOverlay;
    private final Runnable hideStatusRunnable = () -> {
        if (statusText != null) {
            statusText.setVisibility(View.GONE);
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

        setupPlayer();
        setupChannelList();
        enableImmersiveMode();
        loadChannels();
    }

    private String resolveBaseUrl() {
        String raw = BuildConfig.FORCE_FIRESTICK_URL ? BuildConfig.FIRESTICK_LOCKED_URL : BuildConfig.PLAYER_URL;
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
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        playerView.setUseController(false);
        playerView.setKeepScreenOn(true);
        playerView.setFocusable(true);
        playerView.setFocusableInTouchMode(true);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                String msg = "Error de reproduccion: " + error.getMessage();
                showError(msg);
                Log.w(TAG, msg, error);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    showStatus("Buffering...");
                } else if (playbackState == Player.STATE_READY) {
                    hideError();
                    showStatus(currentIndex >= 0 && currentIndex < channels.size()
                            ? channels.get(currentIndex).name
                            : "Listo");
                }
            }
        });
    }

    private void setupChannelList() {
        channelAdapter = new ChannelAdapter();
        channelList.setLayoutManager(new LinearLayoutManager(this));
        channelList.setAdapter(channelAdapter);
    }

    private void loadChannels() {
        showStatus("Cargando canales...");
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
                    uiHandler.post(() -> showError("No se pudieron cargar canales: " + e.getMessage()));
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

                String playUrl = baseUrl + "/live/" + id;

                int platformId = (int) o.optLong("platform_id", 0L);
                String platformName = o.optString("platform_name", "").trim();
                String sourceGroup = o.optString("group", "").trim();

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

                parsed.add(new ChannelItem(id, name, playUrl, i, platformId, platformName, sourceGroup, customGroups));
            }

            long activePlatformID = payload.optLong("active_platform_id", 0L);
            List<ChannelFilter> parsedFilters = buildFiltersFromCatalog(parsed, activePlatformID);
            String defaultFilterKey = activePlatformID > 0 ? "platform:" + activePlatformID : "all";
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
                String playUrl = o.optString("play_url", "").trim();
                String sourceGroup = o.optString("group", "").trim();
                if (id.isEmpty() || playUrl.isEmpty()) {
                    continue;
                }
                if (playUrl.startsWith("/")) {
                    playUrl = baseUrl + playUrl;
                }
                parsed.add(new ChannelItem(id, name, playUrl, i, 0, "Plataforma activa", sourceGroup, new ArrayList<>()));
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
            if (item.platformId > 0) {
                if (!platformNames.containsKey(item.platformId)) {
                    String pName = item.platformName == null ? "" : item.platformName.trim();
                    if (pName.isEmpty()) {
                        pName = "ID " + item.platformId;
                    }
                    platformNames.put(item.platformId, pName);
                }
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
            showError("No hay canales disponibles para este filtro");
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
        String mime = inferMimeType(ch.playUrl);
        MediaItem item = new MediaItem.Builder().setUri(ch.playUrl).setMimeType(mime).build();

        player.setMediaItem(item);
        player.prepare();
        player.setPlayWhenReady(autoPlay);

        hideError();
        showStatus(ch.name);
    }

    private String inferMimeType(String url) {
        String u = url.toLowerCase(Locale.ROOT);
        if (u.contains(".mpd")) {
            return MimeTypes.APPLICATION_MPD;
        }
        if (u.contains(".m3u8")) {
            return MimeTypes.APPLICATION_M3U8;
        }
        return null;
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
            if (a.favorite != b.favorite) {
                return a.favorite ? -1 : 1;
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
            return item.platformId == filter.platformId;
        }
        if (filter.type == FILTER_CUSTOM_GROUP) {
            for (String name : item.customGroups) {
                if (name != null && name.equalsIgnoreCase(filter.groupName)) {
                    return true;
                }
            }
            return false;
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
            showStatus("Sin canales para el filtro seleccionado");
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
            filterText.setText("Filtro: Todos");
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
        errorText.setText(
                "Error de reproduccion\n\n" +
                        (reason == null ? "desconocido" : reason) +
                        "\n\nBase URL: " + baseUrl +
                        "\n\nPulsa MENU para abrir lista/reintentar"
        );
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
                } else if (player != null) {
                    boolean playing = player.isPlaying();
                    player.setPlayWhenReady(!playing);
                    showStatus(playing ? "Pausado" : "Reproduciendo");
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (player != null) {
                    boolean playing = player.isPlaying();
                    player.setPlayWhenReady(!playing);
                    showStatus(playing ? "Pausado" : "Reproduciendo");
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
            toggleFavoriteSelected();
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
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    private static String readAll(InputStream in) throws Exception {
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
        }
        return out.toString();
    }

    private static final class ChannelItem {
        final String id;
        final String name;
        final String playUrl;
        final int originalOrder;
        final int platformId;
        final String platformName;
        final String sourceGroup;
        final List<String> customGroups;
        boolean favorite;

        ChannelItem(String id, String name, String playUrl, int originalOrder, int platformId, String platformName, String sourceGroup, List<String> customGroups) {
            this.id = id;
            this.name = name;
            this.playUrl = playUrl;
            this.originalOrder = originalOrder;
            this.platformId = platformId;
            this.platformName = platformName;
            this.sourceGroup = sourceGroup;
            this.customGroups = customGroups;
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

            ChannelVH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.channelName);
            }
        }
    }
}
