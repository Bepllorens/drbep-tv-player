package com.drbep.tvplayer;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.net.HttpURLConnection;
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
    private static final String PREF_FAVORITE_ORDER = "favorite_order_ids";
    private static final String PREF_REMINDERS = "channel_reminders";
    private static final String PREF_RECENT_CHANNELS = "recent_channel_items";
    private static final int FILTER_ALL = 0;
    private static final int FILTER_PLATFORM = 1;
    private static final int FILTER_CUSTOM_GROUP = 2;
    private static final int FILTER_VOD = 3;

    private PlayerView playerView;
    private TextView errorText;
    private TextView statusText;
    private TextView filterText;
    private TextView overlayCurrentChannelText;
    private TextView overlayCurrentMetaText;
    private TextView overlayPlaybackRouteText;
    private TextView overlayRecentText;
    private TextView zapChannelText;
    private TextView zapMetaText;
    private View channelOverlay;
    private View zapBanner;
    private View recordingsPanel;
    private RecyclerView channelList;
    private RecyclerView recordingsRecyclerView;

    private PlayerController playerController;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final List<ChannelItem> channels = new ArrayList<>();
    private final List<ChannelItem> allChannels = new ArrayList<>();
    private final List<ChannelFilter> filters = new ArrayList<>();
    private final Map<String, String> epgNowByChannelId = new HashMap<>();
    private ChannelAdapter channelAdapter;
    private CatalogRepository catalogRepository;
    private EpgRepository epgRepository;
    private RecordingsRepository recordingsRepository;
    private ReminderStore reminderStore;
    private RecentChannelsStore recentChannelsStore;
    private FavoriteOrderStore favoriteOrderStore;
    private ChannelActionsCoordinator channelActionsCoordinator;
    private ChannelOverlayCoordinator channelOverlayCoordinator;
    private HttpClient httpClient;
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
    private final Runnable hideZapBannerRunnable = () -> {
        if (zapBanner != null) {
            zapBanner.setVisibility(View.GONE);
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
        overlayCurrentChannelText = findViewById(R.id.overlayCurrentChannelText);
        overlayCurrentMetaText = findViewById(R.id.overlayCurrentMetaText);
        overlayPlaybackRouteText = findViewById(R.id.overlayPlaybackRouteText);
        overlayRecentText = findViewById(R.id.overlayRecentText);
        zapBanner = findViewById(R.id.zapBanner);
        zapChannelText = findViewById(R.id.zapChannelText);
        zapMetaText = findViewById(R.id.zapMetaText);
        channelOverlay = findViewById(R.id.channelOverlay);
        recordingsPanel = findViewById(R.id.recordingsPanel);
        channelList = findViewById(R.id.channelList);
        recordingsRecyclerView = findViewById(R.id.recordingsRecyclerView);

        baseUrl = resolveBaseUrl();
    catalogRepository = new CatalogRepository(baseUrl);
    epgRepository = new EpgRepository(baseUrl);
        recordingsRepository = new RecordingsRepository(baseUrl);
    httpClient = new HttpClient();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        reminderStore = new ReminderStore(prefs, PREF_REMINDERS);
        recentChannelsStore = new RecentChannelsStore(prefs, PREF_RECENT_CHANNELS);
        favoriteOrderStore = new FavoriteOrderStore(prefs, PREF_FAVORITE_ORDER);
        channelOverlayCoordinator = new ChannelOverlayCoordinator(channels, allChannels, filters, favoriteChannelIds, favoriteOrderStore);
        channelActionsCoordinator = new ChannelActionsCoordinator(this, new ChannelActionsCoordinator.Host() {
            @Override
            public void tuneSelectedChannel() {
                MainActivity.this.tuneSelectedChannel();
            }

            @Override
            public void toggleFavoriteSelected() {
                MainActivity.this.toggleFavoriteSelected();
            }

            @Override
            public void moveFavoriteSelected(int delta) {
                MainActivity.this.moveFavoriteSelected(delta);
            }

            @Override
            public void openMiniGuide(ChannelItem channelItem) {
                MainActivity.this.openMiniGuideForChannel(channelItem);
            }

            @Override
            public void scheduleCurrentProgram(ChannelItem channelItem) {
                MainActivity.this.createScheduleFromEndpoint(channelItem, false);
            }

            @Override
            public void scheduleNextProgram(ChannelItem channelItem) {
                MainActivity.this.createScheduleFromEndpoint(channelItem, true);
            }

            @Override
            public void createCurrentReminder(ChannelItem channelItem) {
                MainActivity.this.createReminderFromEndpoint(channelItem, false);
            }

            @Override
            public void createNextReminder(ChannelItem channelItem) {
                MainActivity.this.createReminderFromEndpoint(channelItem, true);
            }

            @Override
            public void openRecordings() {
                MainActivity.this.openRecordingsBrowser();
            }

            @Override
            public void scheduleProgram(ChannelItem channelItem, EpgRepository.EpgProgram program) {
                MainActivity.this.scheduleProgram(channelItem, program);
            }

            @Override
            public void createReminder(ChannelItem channelItem, EpgRepository.EpgProgram program) {
                MainActivity.this.createReminder(channelItem, program);
            }
        });
        lastChannelId = prefs.getString(PREF_LAST_CHANNEL_ID, "");
        favoritesOnly = false;
        lastMenuPressedAtMs = 0L;
        Set<String> storedFavorites = prefs.getStringSet(PREF_FAVORITES, new HashSet<>());
        if (storedFavorites != null) {
            favoriteChannelIds.addAll(storedFavorites);
        }
        reminderStore.load();
        recentChannelsStore.load();
        favoriteOrderStore.load();
        favoriteOrderStore.syncToFavorites(favoriteChannelIds);

        setupPlayer();
        setupChannelList();
        setupRecordingsPanel();
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

    private void setupRecordingsPanel() {
        if (recordingsRecyclerView != null) {
            recordingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void loadChannels() {
        showStatus(getString(R.string.status_loading_channels));
        ioExecutor.execute(() -> {
            try {
                CatalogLoadResult result = catalogRepository.fetchCatalogChannels();
                uiHandler.post(() -> applyLoadedChannels(result));
            } catch (Exception catalogErr) {
                Log.w(TAG, "catalog load failed, fallback to /api/channels", catalogErr);
                try {
                    CatalogLoadResult fallback = catalogRepository.fetchActiveChannels();
                    uiHandler.post(() -> applyLoadedChannels(fallback));
                } catch (Exception e) {
                    Log.e(TAG, "load channels failed", e);
                    uiHandler.post(() -> showError(getString(R.string.error_load_channels, e.getMessage())));
                }
            }
        });
    }

    private void applyLoadedChannels(CatalogLoadResult result) {
        syncOverlayCoordinator();
        channelOverlayCoordinator.applyLoadedChannels(result, lastChannelId);
        syncOverlayStateFromCoordinator();
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
        recentChannelsStore.add(ch.id, ch.name);
        playerController.resetFallbackState();
        PlayerController.StreamInfo cachedStreamInfo = streamInfoByChannelId.get(ch.id);
        PlayerController.PlaybackRequest playbackRequest = toPlaybackRequest(ch);
        playerController.playChannel(playbackRequest, autoPlay, cachedStreamInfo);
        playerController.resolveStreamInfoAndReplayIfNeeded(playbackRequest, autoPlay, streamInfoByChannelId);

        hideError();
        showStatus(ch.name);
        updateOverlayPanel();
        showZapBanner(ch);
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }


    private void loadEpgNow() {
        ioExecutor.execute(() -> {
            try {
                Map<String, String> updates = epgRepository.fetchNowPrograms();

                uiHandler.post(() -> {
                    epgNowByChannelId.clear();
                    epgNowByChannelId.putAll(updates);
                    for (ChannelItem item : allChannels) {
                        item.nowProgram = epgNowByChannelId.getOrDefault(item.id, "");
                    }
                    channelAdapter.notifyDataSetChanged();
                    updateOverlayPanel();
                });
            } catch (Exception e) {
                Log.w(TAG, "load epg now failed", e);
            }
        });
    }

    private void showChannelActionMenu() {
        if (channels.isEmpty() || selectedOverlayIndex < 0 || selectedOverlayIndex >= channels.size()) {
            return;
        }
        ChannelItem ch = channels.get(selectedOverlayIndex);
        boolean fav = favoriteChannelIds.contains(ch.id);
        channelActionsCoordinator.showChannelActionMenu(ch, fav);
    }

    private void openMiniGuideForChannel(ChannelItem ch) {
        if (ch == null) {
            return;
        }
        showStatus(getString(R.string.status_loading_guide));
        ioExecutor.execute(() -> {
            try {
                List<EpgRepository.EpgProgram> items = epgRepository.fetchChannelPrograms(ch.id, 8);
                uiHandler.post(() -> {
                    if (items.isEmpty()) {
                        showStatus(getString(R.string.status_no_epg_for_channel));
                        return;
                    }
                    showMiniGuideDialog(ch, items);
                });
            } catch (Exception e) {
                Log.w(TAG, "mini guide failed", e);
                uiHandler.post(() -> showStatus(getString(R.string.status_failed_load_guide)));
            }
        });
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
        showStatus(getString(next ? R.string.status_searching_next_program : R.string.status_searching_current_program));
        ioExecutor.execute(() -> {
            try {
                EpgRepository.EpgProgram program = epgRepository.fetchProgramForChannel(ch.id, next);
                if (program == null) {
                    uiHandler.post(() -> showStatus(getString(R.string.status_no_program_in_epg)));
                    return;
                }
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
            }
        });
    }

    private void scheduleProgram(ChannelItem ch, EpgRepository.EpgProgram program) {
        if (ch == null || program == null) {
            return;
        }
        ioExecutor.execute(() -> {
            try {
                JSONObject req = new JSONObject();
                req.put("channel_id", Long.parseLong(ch.id));
                req.put("channel_name", ch.name);
                req.put("tvg_id", "");
                req.put("program_title", program.title == null || program.title.trim().isEmpty() ? ch.name : program.title);
                req.put("poster", program.icon == null || program.icon.trim().isEmpty() ? ch.logoUrl : program.icon);
                req.put("start_time", program.startTime == null ? "" : program.startTime);
                req.put("end_time", program.endTime == null ? "" : program.endTime);

                HttpClient.Response response = httpClient.postJson(
                        baseUrl + "/api/recordings/schedule",
                        req,
                        10000,
                        15000,
                        java.util.Collections.singletonMap("Content-Type", "application/json")
                );
                if (!response.isSuccessful()) {
                    throw new IllegalStateException("schedule HTTP " + response.code);
                }
                uiHandler.post(() -> showStatus(getString(R.string.status_recording_scheduled)));
            } catch (Exception e) {
                Log.w(TAG, "schedule program failed", e);
                uiHandler.post(() -> showStatus(getString(R.string.status_failed_schedule_recording)));
            }
        });
    }

    private void createReminder(ChannelItem ch, EpgRepository.EpgProgram program) {
        if (ch == null || program == null) {
            return;
        }
        long startAt = parseIsoMillis(program.startTime);
        if (startAt <= 0) {
            showStatus(getString(R.string.status_failed_create_reminder));
            return;
        }
        String title = program.title == null || program.title.trim().isEmpty() ? getString(R.string.label_program_default) : program.title;
        ReminderStore.ReminderItem item = new ReminderStore.ReminderItem(ch.id, ch.name, title, startAt, false);
        reminderStore.addReminder(item);
        showStatus(getString(R.string.status_reminder_created));
    }

    private void openRecordingsBrowser() {
        showStatus(getString(R.string.status_loading_recordings));
        ioExecutor.execute(() -> {
            try {
                RecordingsRepository.RecordingsResult result = recordingsRepository.fetchRecordings();
                if (result.items.isEmpty()) {
                    uiHandler.post(() -> showStatus(getString(R.string.status_no_recordings)));
                    return;
                }
                uiHandler.post(() -> showRecordingsPanel(result));
            } catch (Exception e) {
                Log.w(TAG, "open recordings failed", e);
                uiHandler.post(() -> showStatus(getString(R.string.status_failed_load_recordings)));
            }
        });
    }

    private void playRecording(RecordingsRepository.RecordingItem item, String basePath) {
        if (item == null) {
            return;
        }
        String url = recordingsRepository.buildPlaybackUrl(item, basePath);
        playerController.playRecording(item.name, url);
        hideRecordingsPanel();
        hideOverlay();
    }

    private void checkReminderNotifications() {
        if (reminderStore == null) {
            return;
        }
        List<ReminderStore.ReminderItem> dueItems = reminderStore.collectDueNotifications(System.currentTimeMillis());
        if (!dueItems.isEmpty()) {
            ReminderStore.ReminderItem lastDueItem = dueItems.get(dueItems.size() - 1);
            showStatus(getString(R.string.status_reminder_due, lastDueItem.channelName, lastDueItem.title));
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

    private void tuneSelectedChannel() {
        tuneToIndex(selectedOverlayIndex, true);
        hideOverlay();
    }

    private void moveOverlaySelection(int delta) {
        syncOverlayCoordinator();
        channelOverlayCoordinator.moveOverlaySelection(delta);
        syncOverlayStateFromCoordinator();
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

    private void cycleFilter(int delta) {
        syncOverlayCoordinator();
        ChannelFilter filter = channelOverlayCoordinator.cycleFilter(delta);
        syncOverlayStateFromCoordinator();
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

        if (filter != null) {
            showStatus(getString(R.string.status_filter_changed, filter.label));
        }
        showOverlay();
    }

    private void updateFilterText() {
        syncOverlayCoordinator();
        channelOverlayCoordinator.updateFilterText(filterText, this);
    }

    private void saveFavorites() {
        if (prefs != null) {
            prefs.edit().putStringSet(PREF_FAVORITES, new HashSet<>(favoriteChannelIds)).apply();
        }
        favoriteOrderStore.syncToFavorites(favoriteChannelIds);
    }

    private void toggleFavoriteSelected() {
        if (channels.isEmpty() || selectedOverlayIndex < 0 || selectedOverlayIndex >= channels.size()) {
            return;
        }
        syncOverlayCoordinator();
        boolean added = channelOverlayCoordinator.toggleFavoriteSelected();
        syncOverlayStateFromCoordinator();
        showStatus(getString(added ? R.string.status_favorite_added : R.string.status_favorite_removed));
        String selectedId = selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size() ? channels.get(selectedOverlayIndex).id : null;
        if (added) {
            favoriteOrderStore.addIfMissing(selectedId);
        } else {
            favoriteOrderStore.remove(selectedId);
        }
        saveFavorites();
        channelAdapter.notifyDataSetChanged();
        if (selectedOverlayIndex >= 0) {
            channelList.scrollToPosition(selectedOverlayIndex);
        }
        showOverlay();
    }

    private void moveFavoriteSelected(int delta) {
        if (channels.isEmpty() || selectedOverlayIndex < 0 || selectedOverlayIndex >= channels.size()) {
            return;
        }
        ChannelItem selected = channels.get(selectedOverlayIndex);
        if (!selected.favorite) {
            showStatus(getString(R.string.status_favorite_move_unavailable));
            return;
        }
        boolean moved = favoriteOrderStore.move(selected.id, delta);
        if (!moved) {
            showStatus(getString(R.string.status_favorite_move_unavailable));
            return;
        }
        syncOverlayCoordinator();
        String currentId = currentIndex >= 0 && currentIndex < channels.size() ? channels.get(currentIndex).id : "";
        channelOverlayCoordinator.refreshVisibleChannels(currentId, selected.id);
        syncOverlayStateFromCoordinator();
        channelAdapter.notifyDataSetChanged();
        if (selectedOverlayIndex >= 0) {
            channelList.scrollToPosition(selectedOverlayIndex);
        }
        showStatus(getString(delta < 0 ? R.string.status_favorite_moved_up : R.string.status_favorite_moved_down));
        showOverlay();
    }

    private void toggleFavoritesOnlyMode() {
        syncOverlayCoordinator();
        boolean nowFavoritesOnly = channelOverlayCoordinator.toggleFavoritesOnlyMode();
        syncOverlayStateFromCoordinator();
        channelAdapter.notifyDataSetChanged();

        if (channels.isEmpty() && nowFavoritesOnly) {
            showStatus(getString(R.string.status_favorites_only_empty));
            return;
        }

        if (selectedOverlayIndex >= 0) {
            channelList.scrollToPosition(selectedOverlayIndex);
        }
        showStatus(getString(nowFavoritesOnly ? R.string.status_favorites_only_on : R.string.status_favorites_only_off));
        showOverlay();
    }

    private boolean isOverlayVisible() {
        return channelOverlayCoordinator.isOverlayVisible(channelOverlay);
    }

    private boolean isRecordingsPanelVisible() {
        return recordingsPanel != null && recordingsPanel.getVisibility() == View.VISIBLE;
    }

    private void showOverlay() {
        updateOverlayPanel();
        channelOverlayCoordinator.showOverlay(channelOverlay, uiHandler, hideOverlayRunnable, OVERLAY_HIDE_MS);
    }

    private void hideOverlay() {
        channelOverlayCoordinator.hideOverlay(channelOverlay);
    }

    private void showRecordingsPanel(RecordingsRepository.RecordingsResult result) {
        if (recordingsPanel == null || recordingsRecyclerView == null) {
            showRecordingsDialog(result);
            return;
        }
        hideOverlay();
        recordingsRecyclerView.setAdapter(new RecordingsAdapter(result));
        recordingsPanel.setVisibility(View.VISIBLE);
    }

    private void hideRecordingsPanel() {
        if (recordingsPanel != null) {
            recordingsPanel.setVisibility(View.GONE);
        }
    }

    private void showStatus(String text) {
        if (statusText == null || text == null || text.trim().isEmpty()) {
            return;
        }
        statusText.setText(text);
        statusText.setVisibility(View.VISIBLE);
        updateOverlayPanel();
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
                if (isRecordingsPanelVisible()) {
                    hideRecordingsPanel();
                    return true;
                }
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
                if (isRecordingsPanelVisible()) {
                    hideRecordingsPanel();
                    return true;
                }
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
            case KeyEvent.KEYCODE_SEARCH:
                showChannelSearchDialog();
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
        if (keyCode == KeyEvent.KEYCODE_INFO) {
            showV12ToolsMenu();
            return true;
        }
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
    private PlayerController.PlaybackRequest toPlaybackRequest(ChannelItem channelItem) {
        if (channelItem == null) {
            return null;
        }
        return new PlayerController.PlaybackRequest(channelItem.id, channelItem.name, channelItem.playUrl, channelItem.fallbackPlayUrl);
    }

    private void syncOverlayCoordinator() {
        channelOverlayCoordinator.syncState(currentIndex, selectedOverlayIndex, favoritesOnly, selectedFilterKey);
    }

    private void syncOverlayStateFromCoordinator() {
        currentIndex = channelOverlayCoordinator.getCurrentIndex();
        selectedOverlayIndex = channelOverlayCoordinator.getSelectedOverlayIndex();
        favoritesOnly = channelOverlayCoordinator.isFavoritesOnly();
        selectedFilterKey = channelOverlayCoordinator.getSelectedFilterKey();
    }

    private void updateOverlayPanel() {
        if (overlayCurrentChannelText == null || overlayCurrentMetaText == null || overlayPlaybackRouteText == null || overlayRecentText == null) {
            return;
        }
        ChannelItem currentChannel = (currentIndex >= 0 && currentIndex < channels.size()) ? channels.get(currentIndex) : null;
        if (currentChannel == null) {
            overlayCurrentChannelText.setText(getString(R.string.status_ready));
            overlayCurrentMetaText.setText(getString(R.string.overlay_current_program_empty));
        } else {
            overlayCurrentChannelText.setText(currentChannel.name);
            String currentProgram = currentChannel.nowProgram == null || currentChannel.nowProgram.trim().isEmpty()
                    ? getString(R.string.overlay_current_program_empty)
                    : getString(R.string.overlay_current_program, currentChannel.nowProgram);
            overlayCurrentMetaText.setText(currentProgram);
        }

        PlayerController.PlaybackDiagnostics diagnostics = playerController == null ? null : playerController.getPlaybackDiagnostics();
        String routeLabel = diagnostics == null || diagnostics.routeLabel == null || diagnostics.routeLabel.trim().isEmpty()
                ? getString(R.string.diagnostics_state_idle)
                : diagnostics.routeLabel;
        overlayPlaybackRouteText.setText(getString(R.string.overlay_playback_route, routeLabel));

        List<RecentChannelsStore.RecentChannelItem> items = recentChannelsStore == null ? new ArrayList<>() : recentChannelsStore.getItems();
        if (items.isEmpty()) {
            overlayRecentText.setText(getString(R.string.overlay_recent_channels_empty));
            return;
        }
        List<String> names = new ArrayList<>();
        int max = Math.min(4, items.size());
        for (int i = 0; i < max; i++) {
            names.add(items.get(i).channelName);
        }
        overlayRecentText.setText(getString(R.string.overlay_recent_channels, joinLabels(names)));
    }

    private void showV12ToolsMenu() {
        String[] options = new String[]{
                getString(R.string.tools_menu_search_channels),
                getString(R.string.tools_menu_recent_channels),
            getString(R.string.tools_menu_playback_diagnostics),
            getString(R.string.tools_menu_recordings_panel)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.tools_menu_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showChannelSearchDialog();
                    } else if (which == 1) {
                        showRecentChannelsDialog();
                    } else if (which == 2) {
                        showPlaybackDiagnosticsDialog();
                    } else if (which == 3) {
                        openRecordingsBrowser();
                    }
                })
                .setNegativeButton(R.string.dialog_close, null)
                .show();
    }

    private void showChannelSearchDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_channel_search, null, false);
        EditText input = dialogView.findViewById(R.id.channelSearchInput);
        RecyclerView recyclerView = dialogView.findViewById(R.id.channelSearchResults);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<ChannelItem> filteredItems = new ArrayList<>();
        filteredItems.addAll(allChannels);
        SearchChannelAdapter adapter = new SearchChannelAdapter(filteredItems);
        recyclerView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_search_channels)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_close, null)
                .create();

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSearchResults(adapter, s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        dialog.setOnShowListener(d -> {
            input.requestFocus();
            filterSearchResults(adapter, "");
        });
        dialog.show();
    }

    private void filterSearchResults(SearchChannelAdapter adapter, String query) {
        if (adapter == null) {
            return;
        }
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<ChannelItem> results = new ArrayList<>();
        for (ChannelItem item : allChannels) {
            if (normalizedQuery.isEmpty()) {
                results.add(item);
            } else {
                String haystack = (item.name + " " + item.group + " " + joinLabels(item.customGroups)).toLowerCase(Locale.ROOT);
                if (haystack.contains(normalizedQuery)) {
                    results.add(item);
                }
            }
            if (results.size() >= 25) {
                break;
            }
        }
        adapter.submitList(results);
    }

    private void showMiniGuideDialog(ChannelItem channel, List<EpgRepository.EpgProgram> items) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_list_panel, null, false);
        RecyclerView recyclerView = dialogView.findViewById(R.id.dialogRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new GuideProgramAdapter(channel, items));
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_guide, channel.name))
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_close, null)
                .show();
    }

    private void showRecordingsDialog(RecordingsRepository.RecordingsResult result) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_list_panel, null, false);
        RecyclerView recyclerView = dialogView.findViewById(R.id.dialogRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new RecordingsAdapter(result));
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_recordings_visual)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_close, null)
                .show();
    }

    private void showRecentChannelsDialog() {
        List<RecentChannelsStore.RecentChannelItem> items = recentChannelsStore == null ? new ArrayList<>() : recentChannelsStore.getItems();
        if (items.isEmpty()) {
            showStatus(getString(R.string.overlay_recent_channels_empty));
            return;
        }
        List<String> labels = new ArrayList<>();
        SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
        for (RecentChannelsStore.RecentChannelItem item : items) {
            labels.add(getString(R.string.recent_channel_item, item.channelName, format.format(new Date(item.watchedAt))));
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_recent_channels)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    if (which >= 0 && which < items.size()) {
                        tuneRecentChannel(items.get(which).channelId);
                    }
                })
                .setNegativeButton(R.string.dialog_close, null)
                .show();
    }

    private void tuneRecentChannel(String channelId) {
        int index = findChannelIndexById(channelId);
        if (index < 0) {
            syncOverlayCoordinator();
            channelOverlayCoordinator.setSelectedFilterKey("all");
            channelOverlayCoordinator.setFavoritesOnly(false);
            channelOverlayCoordinator.refreshVisibleChannels(lastChannelId, channelId);
            syncOverlayStateFromCoordinator();
            channelAdapter.notifyDataSetChanged();
            updateFilterText();
            index = findChannelIndexById(channelId);
        }
        if (index >= 0) {
            tuneToIndex(index, true);
        }
    }

    private void tuneChannelById(String channelId) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return;
        }
        int index = findChannelIndexById(channelId);
        if (index < 0) {
            syncOverlayCoordinator();
            channelOverlayCoordinator.setSelectedFilterKey("all");
            channelOverlayCoordinator.setFavoritesOnly(false);
            channelOverlayCoordinator.refreshVisibleChannels(lastChannelId, channelId);
            syncOverlayStateFromCoordinator();
            channelAdapter.notifyDataSetChanged();
            updateFilterText();
            index = findChannelIndexById(channelId);
        }
        if (index >= 0) {
            tuneToIndex(index, true);
        }
    }

    private void showPlaybackDiagnosticsDialog() {
        PlayerController.PlaybackDiagnostics diagnostics = playerController == null ? null : playerController.getPlaybackDiagnostics();
        if (diagnostics == null || (diagnostics.channelName == null || diagnostics.channelName.trim().isEmpty()) && (diagnostics.targetUrl == null || diagnostics.targetUrl.trim().isEmpty())) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_playback_diagnostics)
                    .setMessage(getString(R.string.diagnostics_none))
                    .setNegativeButton(R.string.dialog_close, null)
                    .show();
            return;
        }

        StringBuilder message = new StringBuilder();
        appendDiagnosticLine(message, getString(R.string.diagnostics_channel, safeText(diagnostics.channelName)));
        appendDiagnosticLine(message, getString(R.string.diagnostics_state, safeText(diagnostics.playbackState)));
        appendDiagnosticLine(message, getString(R.string.diagnostics_route, safeText(diagnostics.routeLabel)));
        appendDiagnosticLine(message, getString(R.string.diagnostics_target, safeText(diagnostics.targetUrl)));
        appendDiagnosticLine(message, getString(R.string.diagnostics_mime, fallbackUnknown(diagnostics.mimeType)));
        appendDiagnosticLine(message, getString(R.string.diagnostics_drm, fallbackUnknown(diagnostics.drmType)));
        appendDiagnosticLine(message, getString(R.string.diagnostics_encrypted, getString(diagnostics.encrypted ? R.string.diagnostics_value_yes : R.string.diagnostics_value_no)));
        appendDiagnosticLine(message, getString(R.string.diagnostics_fallback, getString(diagnostics.usingFallback ? R.string.diagnostics_value_yes : R.string.diagnostics_value_no)));
        if (diagnostics.lastError != null && !diagnostics.lastError.trim().isEmpty()) {
            appendDiagnosticLine(message, getString(R.string.diagnostics_last_error, diagnostics.lastError));
        }
        appendDiagnosticLine(message, getString(R.string.diagnostics_recent, buildRecentDiagnosticsSummary()));

        new AlertDialog.Builder(this)
                .setTitle(R.string.title_playback_diagnostics)
                .setMessage(message.toString().trim())
                .setNegativeButton(R.string.dialog_close, null)
                .show();
    }

    private String buildRecentDiagnosticsSummary() {
        List<RecentChannelsStore.RecentChannelItem> items = recentChannelsStore == null ? new ArrayList<>() : recentChannelsStore.getItems();
        if (items.isEmpty()) {
            return getString(R.string.diagnostics_value_unknown);
        }
        List<String> labels = new ArrayList<>();
        int max = Math.min(5, items.size());
        for (int i = 0; i < max; i++) {
            labels.add(items.get(i).channelName);
        }
        return joinLabels(labels);
    }

    private static void appendDiagnosticLine(StringBuilder builder, String line) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        builder.append(line);
    }

    private String fallbackUnknown(String value) {
        return value == null || value.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : value;
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private static String joinLabels(List<String> labels) {
        StringBuilder builder = new StringBuilder();
        for (String label : labels) {
            if (label == null || label.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("  ·  ");
            }
            builder.append(label.trim());
        }
        return builder.toString();
    }

    private void showZapBanner(ChannelItem channelItem) {
        if (zapBanner == null || zapChannelText == null || zapMetaText == null || channelItem == null) {
            return;
        }
        zapChannelText.setText(channelItem.name);
        String meta = channelItem.nowProgram == null || channelItem.nowProgram.trim().isEmpty()
                ? getString(R.string.zap_banner_empty_meta)
                : channelItem.nowProgram;
        zapMetaText.setText(meta);
        zapBanner.setVisibility(View.VISIBLE);
        uiHandler.removeCallbacks(hideZapBannerRunnable);
        uiHandler.postDelayed(hideZapBannerRunnable, 2200L);
    }

    private String buildGuideMeta(EpgRepository.EpgProgram program) {
        if (program == null) {
            return getString(R.string.status_open_program_actions);
        }
        if (program.progress >= 0) {
            return getString(R.string.guide_program_progress, program.progress, getString(R.string.status_open_program_actions));
        }
        return getString(R.string.guide_program_meta, getString(R.string.status_open_program_actions));
    }

    private String buildRecordingMeta(RecordingsRepository.RecordingItem item) {
        String modified = item == null || item.modified == null || item.modified.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : item.modified;
        String sizeLabel = (item == null || item.size <= 0L) ? getString(R.string.recording_size_unknown) : humanReadableSize(item.size);
        return getString(R.string.recording_meta, modified, sizeLabel);
    }

    private static String humanReadableSize(long sizeBytes) {
        if (sizeBytes <= 0L) {
            return "0 B";
        }
        String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        double value = sizeBytes;
        int unitIndex = 0;
        while (value >= 1024d && unitIndex < units.length - 1) {
            value = value / 1024d;
            unitIndex++;
        }
        if (unitIndex == 0) {
            return ((long) value) + " " + units[unitIndex];
        }
        return String.format(Locale.US, "%.1f %s", value, units[unitIndex]);
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
            holder.badge.setText(getString(ch.isVod ? R.string.channel_badge_vod : R.string.channel_badge_live));
            holder.badge.setBackgroundColor(ch.isVod ? 0xAA704C18 : 0xAA204A8A);
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
            TextView badge;
            TextView meta;
            ImageView logo;

            ChannelVH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.channelName);
                badge = itemView.findViewById(R.id.channelBadge);
                meta = itemView.findViewById(R.id.channelMeta);
                logo = itemView.findViewById(R.id.channelLogo);
            }
        }
    }

    private final class GuideProgramAdapter extends RecyclerView.Adapter<GuideProgramAdapter.GuideProgramVH> {
        private final ChannelItem channel;
        private final List<EpgRepository.EpgProgram> items;

        GuideProgramAdapter(ChannelItem channel, List<EpgRepository.EpgProgram> items) {
            this.channel = channel;
            this.items = items;
        }

        @NonNull
        @Override
        public GuideProgramVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_epg_program, parent, false);
            return new GuideProgramVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GuideProgramVH holder, int position) {
            EpgRepository.EpgProgram program = items.get(position);
            holder.time.setText(shortTime(program.startTime) + " - " + shortTime(program.endTime));
            holder.title.setText(program.title == null || program.title.trim().isEmpty() ? getString(R.string.label_program_default) : program.title);
            holder.meta.setText(buildGuideMeta(program));
            holder.itemView.setOnClickListener(v -> channelActionsCoordinator.showProgramActionMenu(channel, program));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        final class GuideProgramVH extends RecyclerView.ViewHolder {
            final TextView time;
            final TextView title;
            final TextView meta;

            GuideProgramVH(@NonNull View itemView) {
                super(itemView);
                time = itemView.findViewById(R.id.programTimeText);
                title = itemView.findViewById(R.id.programTitleText);
                meta = itemView.findViewById(R.id.programMetaText);
            }
        }
    }

    private final class RecordingsAdapter extends RecyclerView.Adapter<RecordingsAdapter.RecordingVH> {
        private final RecordingsRepository.RecordingsResult result;

        RecordingsAdapter(RecordingsRepository.RecordingsResult result) {
            this.result = result;
        }

        @NonNull
        @Override
        public RecordingVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_recording, parent, false);
            return new RecordingVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecordingVH holder, int position) {
            RecordingsRepository.RecordingItem item = result.items.get(position);
            holder.name.setText(item.name);
            holder.meta.setText(buildRecordingMeta(item));
            holder.itemView.setOnClickListener(v -> playRecording(item, result.basePath));
        }

        @Override
        public int getItemCount() {
            return result.items.size();
        }

        final class RecordingVH extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView meta;

            RecordingVH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.recordingNameText);
                meta = itemView.findViewById(R.id.recordingMetaText);
            }
        }
    }

    private final class SearchChannelAdapter extends RecyclerView.Adapter<SearchChannelAdapter.SearchChannelVH> {
        private final List<ChannelItem> items = new ArrayList<>();

        SearchChannelAdapter(List<ChannelItem> initialItems) {
            submitList(initialItems);
        }

        void submitList(List<ChannelItem> newItems) {
            items.clear();
            if (newItems != null) {
                items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SearchChannelVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_search_channel, parent, false);
            return new SearchChannelVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SearchChannelVH holder, int position) {
            ChannelItem item = items.get(position);
            holder.name.setText(item.favorite ? "★ " + item.name : item.name);
            String primaryMeta = item.nowProgram != null && !item.nowProgram.trim().isEmpty() ? item.nowProgram : item.group;
            if (primaryMeta == null || primaryMeta.trim().isEmpty()) {
                primaryMeta = getString(R.string.search_channel_action_hint);
            }
            holder.meta.setText(getString(R.string.search_channel_meta, primaryMeta, item.isVod ? getString(R.string.channel_badge_vod) : getString(R.string.channel_badge_live)));
            holder.itemView.setOnClickListener(v -> tuneChannelById(item.id));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        final class SearchChannelVH extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView meta;

            SearchChannelVH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.searchChannelNameText);
                meta = itemView.findViewById(R.id.searchChannelMetaText);
            }
        }
    }
}
