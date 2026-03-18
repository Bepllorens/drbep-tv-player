package com.drbep.tvplayer;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Rect;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    private static final String PREF_PLAYBACK_MODES = "playback_mode_by_channel";
    private static final String PREF_REMINDERS = "channel_reminders";
    private static final String PREF_RECENT_CHANNELS = "recent_channel_items";
    private static final int FILTER_ALL = 0;
    private static final int FILTER_PLATFORM = 1;
    private static final int FILTER_CUSTOM_GROUP = 2;
    private static final int FILTER_VOD = 3;
    private static final long TIMELINE_WINDOW_MS = 12L * 60L * 60L * 1000L;
    private static final long TIMELINE_SHIFT_MS = 2L * 60L * 60L * 1000L;

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
    private TextView quickSearchQueryText;
    private TextView quickSearchResultText;
    private TextView recordingsSectionText;
    private TextView versionBadgeText;
    private ImageView recordingDetailPosterImage;
    private TextView recordingDetailTitleText;
    private TextView recordingDetailMetaText;
    private TextView recordingDetailPathText;
    private TextView recordingDetailActionText;
    private View channelOverlay;
    private View zapBanner;
    private View quickSearchOverlay;
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
    private PlaybackModeStore playbackModeStore;
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
    private int selectedRecordingIndex = 0;
    private boolean recordingsScheduledMode;
    private final StringBuilder quickSearchBuffer = new StringBuilder();
    private final List<ChannelItem> quickSearchMatches = new ArrayList<>();
    private int quickSearchSelectionIndex = 0;
    private final Set<String> favoriteChannelIds = new HashSet<>();
    private final Map<String, PlayerController.StreamInfo> streamInfoByChannelId = new HashMap<>();
    private RecordingsRepository.RecordingsResult currentRecordingsResult;
    private RecordingsAdapter recordingsAdapter;

    private static final class TimelineChannelPrograms {
        final ChannelItem channel;
        final List<EpgRepository.EpgProgram> programs;

        TimelineChannelPrograms(ChannelItem channel, List<EpgRepository.EpgProgram> programs) {
            this.channel = channel;
            this.programs = programs;
        }
    }

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
    private final Runnable clearQuickSearchRunnable = this::clearQuickSearchOverlay;
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
        quickSearchOverlay = findViewById(R.id.quickSearchOverlay);
        quickSearchQueryText = findViewById(R.id.quickSearchQueryText);
        quickSearchResultText = findViewById(R.id.quickSearchResultText);
        recordingsSectionText = findViewById(R.id.recordingsSectionText);
        versionBadgeText = findViewById(R.id.versionBadgeText);
        recordingDetailPosterImage = findViewById(R.id.recordingDetailPosterImage);
        recordingDetailTitleText = findViewById(R.id.recordingDetailTitleText);
        recordingDetailMetaText = findViewById(R.id.recordingDetailMetaText);
        recordingDetailPathText = findViewById(R.id.recordingDetailPathText);
        recordingDetailActionText = findViewById(R.id.recordingDetailActionText);
        channelOverlay = findViewById(R.id.channelOverlay);
        recordingsPanel = findViewById(R.id.recordingsPanel);
        channelList = findViewById(R.id.channelList);
        recordingsRecyclerView = findViewById(R.id.recordingsRecyclerView);

        if (versionBadgeText != null) {
            versionBadgeText.setText("v" + BuildConfig.VERSION_NAME);
        }

        baseUrl = resolveBaseUrl();
    catalogRepository = new CatalogRepository(baseUrl);
    epgRepository = new EpgRepository(baseUrl);
        recordingsRepository = new RecordingsRepository(baseUrl);
    httpClient = new HttpClient();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        reminderStore = new ReminderStore(prefs, PREF_REMINDERS);
        recentChannelsStore = new RecentChannelsStore(prefs, PREF_RECENT_CHANNELS);
        favoriteOrderStore = new FavoriteOrderStore(prefs, PREF_FAVORITE_ORDER);
        playbackModeStore = new PlaybackModeStore(prefs, PREF_PLAYBACK_MODES);
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
            public void openPlaybackModeSelector(ChannelItem channelItem) {
                MainActivity.this.showPlaybackModeDialog(channelItem);
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
        playbackModeStore.load();
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
        updateRecordingsDetailPanel();
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
        if (playbackRequest != null && !playbackRequest.directPlayback) {
            playerController.resolveStreamInfoAndReplayIfNeeded(playbackRequest, autoPlay, streamInfoByChannelId);
        }

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

    private void openTimelineGuideAroundSelection() {
        if (channels.isEmpty()) {
            return;
        }
        int anchorIndex = selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size()
                ? selectedOverlayIndex
                : (currentIndex >= 0 && currentIndex < channels.size() ? currentIndex : 0);
        openTimelineGuide(anchorIndex, System.currentTimeMillis());
    }

    private void openTimelineGuide(int anchorIndex, long windowStartMs) {
        if (channels.isEmpty()) {
            return;
        }
        if (anchorIndex < 0) {
            anchorIndex = 0;
        }
        if (anchorIndex >= channels.size()) {
            anchorIndex = channels.size() - 1;
        }
        final int selectedIndex = anchorIndex;
        final long selectedWindowStartMs = windowStartMs;
        showStatus(getString(R.string.status_loading_guide));
        ioExecutor.execute(() -> {
            try {
                List<TimelineChannelPrograms> rows = new ArrayList<>();
                for (ChannelItem channel : channels) {
                    List<EpgRepository.EpgProgram> programs = epgRepository.fetchChannelPrograms(channel.id, 12);
                    rows.add(new TimelineChannelPrograms(channel, programs));
                }
                uiHandler.post(() -> {
                    if (rows.isEmpty()) {
                        showStatus(getString(R.string.status_no_epg_for_channel));
                        return;
                    }
                    showTimelineGuideDialog(rows, selectedWindowStartMs, channels.get(selectedIndex).id);
                });
            } catch (Exception e) {
                Log.w(TAG, "timeline guide failed", e);
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
        loadRecordingsPanel(recordingsScheduledMode, null);
    }

    private void loadRecordingsPanel(boolean scheduledMode, String preferredId) {
        recordingsScheduledMode = scheduledMode;
        showStatus(getString(scheduledMode ? R.string.status_loading_scheduled_recordings : R.string.status_loading_recordings));
        final String desiredId = preferredId;
        ioExecutor.execute(() -> {
            try {
                RecordingsRepository.RecordingsResult result = scheduledMode
                        ? recordingsRepository.fetchScheduledRecordings()
                        : recordingsRepository.fetchCompletedRecordings();
                if (result.items.isEmpty()) {
                    uiHandler.post(() -> {
                        if (isRecordingsPanelVisible()) {
                            hideRecordingsPanel();
                        }
                        showStatus(getString(scheduledMode ? R.string.status_no_scheduled_recordings : R.string.status_no_recordings));
                    });
                    return;
                }
                uiHandler.post(() -> showRecordingsPanel(result, desiredId));
            } catch (Exception e) {
                Log.w(TAG, scheduledMode ? "open scheduled recordings failed" : "open recordings failed", e);
                uiHandler.post(() -> showStatus(getString(scheduledMode ? R.string.status_failed_load_scheduled_recordings : R.string.status_failed_load_recordings)));
            }
        });
    }

    private void refreshRecordingsPanel() {
        RecordingsRepository.RecordingItem selected = getSelectedRecordingItem();
        loadRecordingsPanel(recordingsScheduledMode, selected == null ? null : selected.id);
    }

    private void switchRecordingsMode(boolean scheduledMode) {
        if (recordingsScheduledMode == scheduledMode && isRecordingsPanelVisible()) {
            return;
        }
        RecordingsRepository.RecordingItem selected = getSelectedRecordingItem();
        loadRecordingsPanel(scheduledMode, selected == null ? null : selected.id);
    }

    private void cancelSelectedScheduledRecording() {
        RecordingsRepository.RecordingItem item = getSelectedRecordingItem();
        if (item == null || item.playable) {
            return;
        }
        showStatus(getString(R.string.status_canceling_scheduled_recording));
        ioExecutor.execute(() -> {
            try {
                recordingsRepository.deleteScheduledRecording(item.id);
                uiHandler.post(() -> {
                    showStatus(getString(R.string.status_scheduled_recording_canceled));
                    refreshRecordingsPanel();
                });
            } catch (Exception e) {
                Log.w(TAG, "cancel scheduled recording failed", e);
                uiHandler.post(() -> showStatus(getString(R.string.status_failed_cancel_scheduled_recording)));
            }
        });
    }

    private void showScheduledRecordingEditDialog() {
        RecordingsRepository.RecordingItem item = getSelectedRecordingItem();
        if (item == null || item.playable) {
            return;
        }
        String[] options = new String[]{
                getString(R.string.recording_action_shift_earlier),
                getString(R.string.recording_action_shift_later),
                getString(R.string.recording_action_extend),
                getString(R.string.recording_action_shorten)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_recording_edit_time)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        adjustSelectedScheduledRecording(-15L * 60L * 1000L, -15L * 60L * 1000L);
                    } else if (which == 1) {
                        adjustSelectedScheduledRecording(15L * 60L * 1000L, 15L * 60L * 1000L);
                    } else if (which == 2) {
                        adjustSelectedScheduledRecording(0L, 15L * 60L * 1000L);
                    } else if (which == 3) {
                        adjustSelectedScheduledRecording(0L, -15L * 60L * 1000L);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void adjustSelectedScheduledRecording(long startDeltaMs, long endDeltaMs) {
        RecordingsRepository.RecordingItem item = getSelectedRecordingItem();
        if (item == null || item.playable) {
            return;
        }
        long startMs = parseIsoMillis(item.startTime);
        long endMs = parseIsoMillis(item.endTime);
        if (startMs <= 0L || endMs <= 0L) {
            showStatus(getString(R.string.status_failed_update_scheduled_recording));
            return;
        }
        long updatedStart = startMs + startDeltaMs;
        long updatedEnd = endMs + endDeltaMs;
        if (updatedEnd <= updatedStart) {
            showStatus(getString(R.string.status_invalid_scheduled_recording_window));
            return;
        }
        showStatus(getString(R.string.status_updating_scheduled_recording));
        ioExecutor.execute(() -> {
            try {
                recordingsRepository.updateScheduledRecording(item.id, formatIsoMillis(updatedStart), formatIsoMillis(updatedEnd));
                uiHandler.post(() -> {
                    showStatus(getString(R.string.status_scheduled_recording_updated));
                    refreshRecordingsPanel();
                });
            } catch (Exception e) {
                Log.w(TAG, "update scheduled recording failed", e);
                uiHandler.post(() -> showStatus(getString(R.string.status_failed_update_scheduled_recording)));
            }
        });
    }

    private void playRecording(RecordingsRepository.RecordingItem item, String basePath) {
        if (item == null || !item.playable) {
            showStatus(getString(R.string.status_recording_not_playable));
            return;
        }
        String url = recordingsRepository.buildPlaybackUrl(item, basePath);
        playerController.playRecording(buildRecordingTitle(item), url);
        hideRecordingsPanel();
        hideOverlay();
    }

    private RecordingsRepository.RecordingItem getSelectedRecordingItem() {
        if (currentRecordingsResult == null || currentRecordingsResult.items.isEmpty()) {
            return null;
        }
        if (selectedRecordingIndex < 0 || selectedRecordingIndex >= currentRecordingsResult.items.size()) {
            selectedRecordingIndex = 0;
        }
        return currentRecordingsResult.items.get(selectedRecordingIndex);
    }

    private void showRecordingActionsDialog() {
        RecordingsRepository.RecordingItem item = getSelectedRecordingItem();
        if (item == null) {
            return;
        }
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        if (item.playable) {
            options.add(getString(R.string.recording_action_play));
            actions.add(this::playSelectedRecording);
        } else {
            options.add(getString(R.string.recording_action_edit_time));
            actions.add(this::showScheduledRecordingEditDialog);
            options.add(getString(R.string.recording_action_cancel));
            actions.add(this::cancelSelectedScheduledRecording);
        }
        options.add(getString(R.string.recording_action_refresh));
        actions.add(this::refreshRecordingsPanel);
        options.add(getString(recordingsScheduledMode ? R.string.recording_action_switch_completed : R.string.recording_action_switch_scheduled));
        actions.add(() -> switchRecordingsMode(!recordingsScheduledMode));
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_recording_actions)
                .setItems(options.toArray(new String[0]), (dialog, which) -> actions.get(which).run())
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
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

    private static String formatIsoMillis(long value) {
        SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
        return out.format(new Date(value));
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
        clearQuickSearchOverlay();
        hideRecordingsPanel();
        updateOverlayPanel();
        channelOverlayCoordinator.showOverlay(channelOverlay, uiHandler, hideOverlayRunnable, OVERLAY_HIDE_MS);
    }

    private void hideOverlay() {
        channelOverlayCoordinator.hideOverlay(channelOverlay);
    }

    private void showRecordingsPanel(RecordingsRepository.RecordingsResult result) {
        showRecordingsPanel(result, null);
    }

    private void showRecordingsPanel(RecordingsRepository.RecordingsResult result, String preferredId) {
        if (recordingsPanel == null || recordingsRecyclerView == null) {
            showRecordingsDialog(result);
            return;
        }
        clearQuickSearchOverlay();
        hideOverlay();
        currentRecordingsResult = result;
        recordingsScheduledMode = result.scheduledMode;
        selectedRecordingIndex = 0;
        if (preferredId != null && !preferredId.trim().isEmpty()) {
            for (int i = 0; i < result.items.size(); i++) {
                if (preferredId.equals(result.items.get(i).id)) {
                    selectedRecordingIndex = i;
                    break;
                }
            }
        }
        recordingsAdapter = new RecordingsAdapter(result);
        recordingsRecyclerView.setAdapter(recordingsAdapter);
        recordingsRecyclerView.scrollToPosition(selectedRecordingIndex);
        updateRecordingsDetailPanel();
        recordingsPanel.setVisibility(View.VISIBLE);
    }

    private void hideRecordingsPanel() {
        if (recordingsPanel != null) {
            recordingsPanel.setVisibility(View.GONE);
        }
        currentRecordingsResult = null;
        selectedRecordingIndex = 0;
        updateRecordingsDetailPanel();
    }

    private boolean isQuickSearchVisible() {
        return quickSearchOverlay != null && quickSearchOverlay.getVisibility() == View.VISIBLE;
    }

    private void handleQuickSearchCharacter(char value) {
        if (!Character.isLetterOrDigit(value)) {
            return;
        }
        quickSearchBuffer.append(Character.toLowerCase(value));
        updateQuickSearchOverlay();
    }

    private void deleteQuickSearchCharacter() {
        if (quickSearchBuffer.length() == 0) {
            clearQuickSearchOverlay();
            return;
        }
        quickSearchBuffer.deleteCharAt(quickSearchBuffer.length() - 1);
        if (quickSearchBuffer.length() == 0) {
            clearQuickSearchOverlay();
            return;
        }
        updateQuickSearchOverlay();
    }

    private void moveQuickSearchSelection(int delta) {
        if (quickSearchMatches.isEmpty()) {
            return;
        }
        quickSearchSelectionIndex += delta;
        if (quickSearchSelectionIndex < 0) {
            quickSearchSelectionIndex = quickSearchMatches.size() - 1;
        }
        if (quickSearchSelectionIndex >= quickSearchMatches.size()) {
            quickSearchSelectionIndex = 0;
        }
        updateQuickSearchOverlay();
    }

    private void tuneQuickSearchSelection() {
        if (quickSearchMatches.isEmpty()) {
            return;
        }
        if (quickSearchSelectionIndex < 0 || quickSearchSelectionIndex >= quickSearchMatches.size()) {
            quickSearchSelectionIndex = 0;
        }
        tuneChannelById(quickSearchMatches.get(quickSearchSelectionIndex).id);
        clearQuickSearchOverlay();
    }

    private void updateQuickSearchOverlay() {
        if (quickSearchOverlay == null || quickSearchQueryText == null || quickSearchResultText == null) {
            return;
        }
        String query = quickSearchBuffer.toString().trim();
        if (query.isEmpty()) {
            clearQuickSearchOverlay();
            return;
        }
        quickSearchMatches.clear();
        quickSearchMatches.addAll(searchChannels(query, 6));
        if (quickSearchSelectionIndex >= quickSearchMatches.size()) {
            quickSearchSelectionIndex = 0;
        }
        quickSearchOverlay.setVisibility(View.VISIBLE);
        quickSearchQueryText.setText(query.toUpperCase(Locale.getDefault()));
        if (quickSearchMatches.isEmpty()) {
            quickSearchResultText.setText(getString(R.string.quick_search_no_results));
        } else {
            ChannelItem selected = quickSearchMatches.get(quickSearchSelectionIndex);
            String primaryMeta = selected.nowProgram != null && !selected.nowProgram.trim().isEmpty() ? selected.nowProgram : selected.group;
            if (primaryMeta == null || primaryMeta.trim().isEmpty()) {
                primaryMeta = getString(R.string.search_channel_action_hint);
            }
            quickSearchResultText.setText(getString(
                    R.string.quick_search_result,
                    getString(R.string.quick_search_result_index, quickSearchSelectionIndex + 1, quickSearchMatches.size()) + "  ·  " + selected.name,
                    primaryMeta
            ));
        }
        uiHandler.removeCallbacks(clearQuickSearchRunnable);
        uiHandler.postDelayed(clearQuickSearchRunnable, 3200L);
    }

    private void clearQuickSearchOverlay() {
        quickSearchBuffer.setLength(0);
        quickSearchMatches.clear();
        quickSearchSelectionIndex = 0;
        if (quickSearchOverlay != null) {
            quickSearchOverlay.setVisibility(View.GONE);
        }
        uiHandler.removeCallbacks(clearQuickSearchRunnable);
    }

    private List<ChannelItem> searchChannels(String query, int limit) {
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
            if (results.size() >= limit) {
                break;
            }
        }
        return results;
    }

    private void moveRecordingsSelection(int delta) {
        if (currentRecordingsResult == null || currentRecordingsResult.items.isEmpty()) {
            return;
        }
        selectedRecordingIndex += delta;
        if (selectedRecordingIndex < 0) {
            selectedRecordingIndex = currentRecordingsResult.items.size() - 1;
        }
        if (selectedRecordingIndex >= currentRecordingsResult.items.size()) {
            selectedRecordingIndex = 0;
        }
        if (recordingsAdapter != null) {
            recordingsAdapter.notifyDataSetChanged();
        }
        if (recordingsRecyclerView != null) {
            recordingsRecyclerView.scrollToPosition(selectedRecordingIndex);
        }
        updateRecordingsDetailPanel();
    }

    private void playSelectedRecording() {
        if (currentRecordingsResult == null || currentRecordingsResult.items.isEmpty()) {
            return;
        }
        if (selectedRecordingIndex < 0 || selectedRecordingIndex >= currentRecordingsResult.items.size()) {
            selectedRecordingIndex = 0;
        }
        RecordingsRepository.RecordingItem item = currentRecordingsResult.items.get(selectedRecordingIndex);
        if (!item.playable) {
            showRecordingActionsDialog();
            return;
        }
        playRecording(item, currentRecordingsResult.basePath);
    }

    private void updateRecordingsDetailPanel() {
        if (recordingsSectionText == null || recordingDetailPosterImage == null || recordingDetailTitleText == null || recordingDetailMetaText == null || recordingDetailPathText == null || recordingDetailActionText == null) {
            return;
        }
        if (currentRecordingsResult == null || currentRecordingsResult.items.isEmpty()) {
            recordingsSectionText.setText(getString(recordingsScheduledMode ? R.string.title_recordings_scheduled : R.string.title_recordings_completed));
            recordingDetailTitleText.setText(getString(R.string.recordings_detail_empty));
            recordingDetailMetaText.setText("");
            recordingDetailPathText.setText("");
            recordingDetailActionText.setText(getString(recordingsScheduledMode ? R.string.recordings_panel_action_hint_scheduled : R.string.recordings_panel_action_hint));
            recordingDetailPathText.setVisibility(View.GONE);
            recordingDetailPosterImage.setVisibility(View.GONE);
            Glide.with(this).clear(recordingDetailPosterImage);
            return;
        }
        if (selectedRecordingIndex < 0 || selectedRecordingIndex >= currentRecordingsResult.items.size()) {
            selectedRecordingIndex = 0;
        }
        RecordingsRepository.RecordingItem item = currentRecordingsResult.items.get(selectedRecordingIndex);
        recordingsSectionText.setText(getString(currentRecordingsResult.scheduledMode ? R.string.title_recordings_scheduled : R.string.title_recordings_completed));
        recordingDetailTitleText.setText(buildRecordingTitle(item));
        recordingDetailMetaText.setText(buildRecordingMeta(item));
        if (item.playable) {
            recordingDetailPathText.setVisibility(View.VISIBLE);
            recordingDetailPathText.setText(getString(R.string.recordings_path, item.path == null ? "" : item.path));
        } else {
            recordingDetailPathText.setVisibility(View.GONE);
            recordingDetailPathText.setText("");
        }
        recordingDetailActionText.setText(getString(currentRecordingsResult.scheduledMode ? R.string.recordings_panel_action_hint_scheduled : R.string.recordings_panel_action_hint));
        bindRecordingPoster(recordingDetailPosterImage, item.poster);
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

        if (!isRecordingsPanelVisible()) {
            int unicode = event.getUnicodeChar();
            if (unicode > 0 && Character.isLetterOrDigit((char) unicode)) {
                handleQuickSearchCharacter((char) unicode);
                return true;
            }
        }

        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                long now = System.currentTimeMillis();
                if (isQuickSearchVisible()) {
                    clearQuickSearchOverlay();
                    return true;
                }
                if (isRecordingsPanelVisible()) {
                    switchRecordingsMode(false);
                    return true;
                }
                if (now - lastMenuPressedAtMs <= MENU_DOUBLE_PRESS_MS) {
                    lastMenuPressedAtMs = 0L;
                    if (isOverlayVisible()) {
                        if (selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size()) {
                            openTimelineGuideAroundSelection();
                        }
                    } else if (currentIndex >= 0 && currentIndex < channels.size()) {
                        openTimelineGuide(currentIndex, System.currentTimeMillis());
                    } else {
                        showOverlay();
                    }
                    return true;
                }
                lastMenuPressedAtMs = now;
                showV12ToolsMenu();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (isQuickSearchVisible()) {
                    clearQuickSearchOverlay();
                    return true;
                }
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
                if (isQuickSearchVisible()) {
                    moveQuickSearchSelection(-1);
                    return true;
                }
                if (isRecordingsPanelVisible()) {
                    moveRecordingsSelection(-1);
                    return true;
                }
                tuneRelative(-1);
                return true;
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
            case KeyEvent.KEYCODE_PAGE_DOWN:
                if (isQuickSearchVisible()) {
                    moveQuickSearchSelection(1);
                    return true;
                }
                if (isRecordingsPanelVisible()) {
                    moveRecordingsSelection(1);
                    return true;
                }
                tuneRelative(1);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (isQuickSearchVisible()) {
                    moveQuickSearchSelection(-1);
                    return true;
                }
                if (isRecordingsPanelVisible()) {
                    moveRecordingsSelection(-1);
                    return true;
                }
                if (isOverlayVisible()) {
                    moveOverlaySelection(-1);
                } else {
                    tuneRelative(-1);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (isQuickSearchVisible()) {
                    moveQuickSearchSelection(1);
                    return true;
                }
                if (isRecordingsPanelVisible()) {
                    moveRecordingsSelection(1);
                    return true;
                }
                if (isOverlayVisible()) {
                    moveOverlaySelection(1);
                } else {
                    tuneRelative(1);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (isQuickSearchVisible()) {
                    clearQuickSearchOverlay();
                    return true;
                }
                if (isRecordingsPanelVisible()) {
                    hideRecordingsPanel();
                    return true;
                }
                if (isOverlayVisible()) {
                    cycleFilter(-1);
                } else {
                    showOverlay();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (isQuickSearchVisible()) {
                    tuneQuickSearchSelection();
                    return true;
                }
                if (isRecordingsPanelVisible()) {
                    switchRecordingsMode(true);
                    return true;
                }
                if (isOverlayVisible()) {
                    cycleFilter(1);
                } else {
                    showOverlay();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (isQuickSearchVisible()) {
                    tuneQuickSearchSelection();
                    return true;
                }
                if (isRecordingsPanelVisible()) {
                    playSelectedRecording();
                    return true;
                }
                if (isOverlayVisible()) {
                    tuneToIndex(selectedOverlayIndex, true);
                    hideOverlay();
                } else if (playerController != null) {
                    playerController.togglePlayback();
                }
                return true;
            case KeyEvent.KEYCODE_INFO:
                if (isQuickSearchVisible()) {
                    showChannelSearchDialog();
                    return true;
                }
                if (isRecordingsPanelVisible()) {
                    showRecordingActionsDialog();
                    return true;
                }
                if (isOverlayVisible()) {
                    if (selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size()) {
                        openTimelineGuideAroundSelection();
                    }
                    return true;
                }
                if (currentIndex >= 0 && currentIndex < channels.size()) {
                    openTimelineGuide(currentIndex, System.currentTimeMillis());
                } else {
                    showOverlay();
                }
                return true;
            case KeyEvent.KEYCODE_SEARCH:
                if (isQuickSearchVisible()) {
                    showChannelSearchDialog();
                    return true;
                }
                showChannelSearchDialog();
                return true;
            case KeyEvent.KEYCODE_DEL:
            case KeyEvent.KEYCODE_FORWARD_DEL:
                if (isQuickSearchVisible()) {
                    deleteQuickSearchCharacter();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_RECORD:
                if (isOverlayVisible() && selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size()) {
                    createScheduleFromEndpoint(channels.get(selectedOverlayIndex), false);
                } else if (currentIndex >= 0 && currentIndex < channels.size()) {
                    createScheduleFromEndpoint(channels.get(currentIndex), false);
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                if (playerController != null && playerController.seekTimeshiftBack()) {
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (playerController != null && playerController.seekTimeshiftForward()) {
                    return true;
                }
                break;
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
        if (keyCode == KeyEvent.KEYCODE_INFO || keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            showV12ToolsMenu();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (isQuickSearchVisible()) {
                clearQuickSearchOverlay();
                return true;
            }
            if (isRecordingsPanelVisible()) {
                hideRecordingsPanel();
                return true;
            }
            if (isOverlayVisible()) {
                if (selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size()) {
                    openTimelineGuideAroundSelection();
                    return true;
                }
            } else if (currentIndex >= 0 && currentIndex < channels.size()) {
                openTimelineGuide(currentIndex, System.currentTimeMillis());
                return true;
            }
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
        return new PlayerController.PlaybackRequest(
                channelItem.id,
                channelItem.name,
                channelItem.platformName,
                channelItem.playUrl,
                channelItem.fallbackPlayUrl,
                playbackModeStore == null ? PlaybackModeStore.MODE_AUTO : playbackModeStore.getMode(channelItem.id),
                channelItem.drmScheme,
                channelItem.drmLicenseUrl,
                channelItem.directPlayback
        );
    }

    private void showPlaybackModeDialog(ChannelItem channelItem) {
        if (channelItem == null || playbackModeStore == null) {
            return;
        }
        String currentMode = playbackModeStore.getMode(channelItem.id);
        int checkedItem = PlaybackModeStore.MODE_DIRECT.equals(currentMode)
                ? 1
                : (PlaybackModeStore.MODE_PROXY.equals(currentMode) ? 2 : 0);
        String[] options = getResources().getStringArray(R.array.playback_mode_options);
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_playback_mode)
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    String selectedMode = which == 1
                            ? PlaybackModeStore.MODE_DIRECT
                            : (which == 2 ? PlaybackModeStore.MODE_PROXY : PlaybackModeStore.MODE_AUTO);
                    playbackModeStore.setMode(channelItem.id, selectedMode);
                    showStatus(getString(R.string.status_playback_mode_changed, options[which]));
                    dialog.dismiss();
                    if (currentIndex >= 0 && currentIndex < channels.size() && channelItem.id.equals(channels.get(currentIndex).id)) {
                        tuneToIndex(currentIndex, true);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
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
        clearQuickSearchOverlay();
        String[] options = new String[]{
                getString(R.string.tools_menu_timeline_guide),
                getString(R.string.tools_menu_search_channels),
                getString(R.string.tools_menu_recent_channels),
                getString(R.string.tools_menu_playback_diagnostics),
                getString(R.string.tools_menu_recordings_panel)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.tools_menu_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openTimelineGuideAroundSelection();
                    } else if (which == 1) {
                        showChannelSearchDialog();
                    } else if (which == 2) {
                        showRecentChannelsDialog();
                    } else if (which == 3) {
                        showPlaybackDiagnosticsDialog();
                    } else if (which == 4) {
                        openRecordingsBrowser();
                    }
                })
                .setNegativeButton(R.string.dialog_close, null)
                .show();
    }

    private void showChannelSearchDialog() {
        clearQuickSearchOverlay();
        hideOverlay();
        hideRecordingsPanel();
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_channel_search, null, false);
        EditText input = dialogView.findViewById(R.id.channelSearchInput);
        RecyclerView recyclerView = dialogView.findViewById(R.id.channelSearchResults);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<ChannelItem> filteredItems = new ArrayList<>();
        filteredItems.addAll(allChannels);
        final AlertDialog[] dialogHolder = new AlertDialog[1];
        SearchChannelAdapter adapter = new SearchChannelAdapter(filteredItems, item -> {
            tuneChannelById(item.id);
            if (dialogHolder[0] != null) {
                dialogHolder[0].dismiss();
            }
        });
        recyclerView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_search_channels)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_close, null)
                .create();
        dialogHolder[0] = dialog;

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
        adapter.submitList(searchChannels(query, 25));
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

    private void showTimelineGuideDialog(List<TimelineChannelPrograms> rows, long windowStartMs, String anchorChannelId) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_timeline_guide, null, false);
        android.widget.ScrollView timelineVerticalScroll = dialogView.findViewById(R.id.timelineVerticalScroll);
        TextView timelineNowButton = dialogView.findViewById(R.id.timelineNowButton);
        TextView timelineNextButton = dialogView.findViewById(R.id.timelineNextButton);
        TextView timelineCloseButton = dialogView.findViewById(R.id.timelineCloseButton);
        TextView windowText = dialogView.findViewById(R.id.timelineWindowText);
        LinearLayout headerRow = dialogView.findViewById(R.id.timelineHeaderRow);
        LinearLayout rowsContainer = dialogView.findViewById(R.id.timelineRowsContainer);
        ImageView timelineProgramPosterImage = dialogView.findViewById(R.id.timelineProgramPosterImage);
        TextView timelineProgramTitleText = dialogView.findViewById(R.id.timelineProgramTitleText);
        TextView timelineProgramMetaText = dialogView.findViewById(R.id.timelineProgramMetaText);
        TextView timelineProgramDescText = dialogView.findViewById(R.id.timelineProgramDescText);
        final View[] initialFocus = new View[1];
        final boolean[] suppressInitialFocusScroll = new boolean[]{true};
        final List<List<View>> focusRows = new ArrayList<>();
        final Map<View, Integer> focusCenters = new HashMap<>();
        final Runnable clearTimelineProgramDetail = () -> {
            if (timelineProgramPosterImage != null) {
                timelineProgramPosterImage.setVisibility(View.GONE);
                Glide.with(this).clear(timelineProgramPosterImage);
            }
            if (timelineProgramTitleText != null) {
                timelineProgramTitleText.setText(getString(R.string.timeline_no_epg));
            }
            if (timelineProgramMetaText != null) {
                timelineProgramMetaText.setText(getString(R.string.timeline_program_detail_hint));
            }
            if (timelineProgramDescText != null) {
                timelineProgramDescText.setText(getString(R.string.timeline_program_desc_empty));
            }
        };

        long windowEndMs = windowStartMs + TIMELINE_WINDOW_MS;
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE d MMM", Locale.getDefault());
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        windowText.setText(getString(
                R.string.timeline_window_label,
                dayFormat.format(new Date(windowStartMs)),
                hourFormat.format(new Date(windowStartMs)),
                hourFormat.format(new Date(windowEndMs))
        ));

        android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int dialogWidthPx = (int) (displayMetrics.widthPixels * 0.98f);
        int horizontalChrome = dp(20);
        int labelWidth = Math.max(dp(108), Math.min(dp(132), (int) (dialogWidthPx * 0.18f)));
        int stripWidth = Math.max(dp(540), dialogWidthPx - labelWidth - horizontalChrome);
        int totalWindowMinutes = (int) (TIMELINE_WINDOW_MS / 60000L);
        float minuteWidth = stripWidth / (float) totalWindowMinutes;
        int headerSlotMinutes = TIMELINE_WINDOW_MS >= 6L * 60L * 60L * 1000L ? 60 : 30;
        int headerSlotCount = Math.max(1, totalWindowMinutes / headerSlotMinutes);
        int headerSlotWidth = stripWidth / headerSlotCount;

        TextView spacer = new TextView(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(labelWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
        headerRow.addView(spacer);
        for (int i = 0; i < headerSlotCount; i++) {
            TextView hourLabel = new TextView(this);
            hourLabel.setLayoutParams(new LinearLayout.LayoutParams(headerSlotWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            long slotStartMs = windowStartMs + (i * headerSlotMinutes * 60L * 1000L);
            hourLabel.setText(hourFormat.format(new Date(slotStartMs)));
            hourLabel.setTextColor(i % 2 == 0 ? 0xFFA7D0FF : 0xFF6F92B8);
            hourLabel.setTextSize(11f);
            hourLabel.setPadding(dp(4), dp(4), dp(4), dp(4));
            headerRow.addView(hourLabel);
        }

        for (TimelineChannelPrograms row : rows) {
            final int rowIndex = focusRows.size();
            final List<View> rowFocusables = new ArrayList<>();

            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.topMargin = dp(6);
            rowLayout.setLayoutParams(rowParams);

            LinearLayout channelLabel = new LinearLayout(this);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(labelWidth, dp(62));
            channelLabel.setLayoutParams(labelParams);
            channelLabel.setBackgroundColor(0xFF1A2532);
            channelLabel.setGravity(Gravity.CENTER_VERTICAL);
            channelLabel.setOrientation(LinearLayout.HORIZONTAL);
            channelLabel.setPadding(dp(8), dp(6), dp(8), dp(6));

            ImageView channelLogo = new ImageView(this);
            LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(26), dp(26));
            logoParams.rightMargin = dp(8);
            channelLogo.setLayoutParams(logoParams);
            channelLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            if (row.channel.logoUrl != null && !row.channel.logoUrl.trim().isEmpty()) {
                Glide.with(this).load(row.channel.logoUrl.trim()).into(channelLogo);
            }
            channelLabel.addView(channelLogo);

            TextView channelNameText = new TextView(this);
            channelNameText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            channelNameText.setText(row.channel.name);
            channelNameText.setTextColor(0xFFFFFFFF);
            channelNameText.setTextSize(12f);
            channelNameText.setMaxLines(2);
            channelLabel.addView(channelNameText);
            rowLayout.addView(channelLabel);

            LinearLayout strip = new LinearLayout(this);
            strip.setOrientation(LinearLayout.HORIZONTAL);
            strip.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            strip.setBackgroundColor(0xFF101820);

            int usedWidth = 0;
            boolean hasVisibleProgram = false;
            for (EpgRepository.EpgProgram program : row.programs) {
                long startMs = parseIsoMillis(program.startTime);
                long endMs = parseIsoMillis(program.endTime);
                if (endMs <= windowStartMs || startMs >= windowEndMs || endMs <= startMs) {
                    continue;
                }
                hasVisibleProgram = true;
                int visibleStartMinutes = (int) Math.max(0L, (Math.max(windowStartMs, startMs) - windowStartMs) / 60000L);
                int visibleEndMinutes = (int) Math.max(visibleStartMinutes + 1L, (Math.min(windowEndMs, endMs) - windowStartMs) / 60000L);
                int targetOffsetWidth = Math.round(visibleStartMinutes * minuteWidth);
                if (targetOffsetWidth > usedWidth) {
                    View spacerView = new View(this);
                    spacerView.setLayoutParams(new LinearLayout.LayoutParams(targetOffsetWidth - usedWidth, dp(62)));
                    strip.addView(spacerView);
                    usedWidth = targetOffsetWidth;
                }

                int durationMinutes = Math.max(15, visibleEndMinutes - visibleStartMinutes);
                int blockWidth = Math.max(dp(72), Math.round(durationMinutes * minuteWidth));
                final int centerMinute = visibleStartMinutes + (durationMinutes / 2);
                TextView block = new TextView(this);
                LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(blockWidth, dp(62));
                blockParams.rightMargin = dp(2);
                block.setLayoutParams(blockParams);
                block.setFocusable(true);
                block.setFocusableInTouchMode(true);
                block.setPadding(dp(8), dp(6), dp(8), dp(6));
                block.setText((program.title == null || program.title.trim().isEmpty() ? getString(R.string.label_program_default) : program.title)
                        + "\n" + shortTime(program.startTime) + " - " + shortTime(program.endTime));
                block.setTextColor(0xFFFFFFFF);
                block.setTextSize(11f);
                block.setMaxLines(3);
                boolean live = program.progress >= 0;
                applyTimelineBlockState(block, live, false);
                block.setOnFocusChangeListener((v, hasFocus) -> {
                    applyTimelineBlockState(block, live, hasFocus);
                    if (hasFocus) {
                        if (timelineProgramTitleText != null) {
                            timelineProgramTitleText.setText(program.title == null || program.title.trim().isEmpty()
                                    ? getString(R.string.label_program_default)
                                    : program.title);
                        }
                        if (timelineProgramMetaText != null) {
                            String timelineMeta = row.channel.name + "  ·  " + shortTime(program.startTime) + " - " + shortTime(program.endTime);
                            if (live) {
                                timelineMeta = timelineMeta + "  ·  " + getString(R.string.guide_program_now);
                            }
                            timelineProgramMetaText.setText(timelineMeta);
                        }
                        if (timelineProgramDescText != null) {
                            String description = program.description == null || program.description.trim().isEmpty()
                                    ? getString(R.string.timeline_program_desc_empty)
                                    : program.description.trim();
                            timelineProgramDescText.setText(description);
                        }
                        if (timelineProgramPosterImage != null) {
                            String posterUrl = program.icon == null || program.icon.trim().isEmpty() ? row.channel.logoUrl : program.icon;
                            if (posterUrl == null || posterUrl.trim().isEmpty()) {
                                timelineProgramPosterImage.setVisibility(View.GONE);
                                Glide.with(this).clear(timelineProgramPosterImage);
                            } else {
                                timelineProgramPosterImage.setVisibility(View.VISIBLE);
                                Glide.with(this).load(posterUrl.trim()).centerCrop().into(timelineProgramPosterImage);
                            }
                        }
                    }
                    if (hasFocus && !suppressInitialFocusScroll[0]) {
                        ensureTimelineBlockVisible(timelineVerticalScroll, v);
                    }
                });
                block.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) {
                        return false;
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        return moveTimelineFocus(timelineVerticalScroll, focusRows, focusCenters, rowIndex, -1, centerMinute);
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        return moveTimelineFocus(timelineVerticalScroll, focusRows, focusCenters, rowIndex, 1, centerMinute);
                    }
                    return false;
                });
                block.setOnClickListener(v -> channelActionsCoordinator.showProgramActionMenu(row.channel, program));
                focusCenters.put(block, centerMinute);
                rowFocusables.add(block);
                if (anchorChannelId != null && anchorChannelId.equals(row.channel.id) && initialFocus[0] == null) {
                    initialFocus[0] = block;
                } else if (initialFocus[0] == null) {
                    initialFocus[0] = block;
                }
                strip.addView(block);
                usedWidth += blockWidth + dp(2);
            }

            if (!hasVisibleProgram) {
                TextView empty = new TextView(this);
                empty.setLayoutParams(new LinearLayout.LayoutParams(stripWidth, dp(62)));
                empty.setBackgroundColor(0xFF1E2630);
                empty.setGravity(Gravity.CENTER_VERTICAL);
                empty.setPadding(dp(10), dp(8), dp(10), dp(8));
                empty.setText(R.string.timeline_no_epg);
                empty.setTextColor(0xFFBFD0E6);
                strip.addView(empty);
            }

            focusRows.add(rowFocusables);
            rowLayout.addView(strip);
            rowsContainer.addView(rowLayout);
        }

        clearTimelineProgramDetail.run();

        android.app.Dialog timelineDialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        timelineDialog.setContentView(dialogView);
        timelineDialog.setCancelable(true);
        timelineDialog.setOnShowListener(d -> {
            if (timelineDialog.getWindow() != null) {
                timelineDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
            if (timelineVerticalScroll != null) {
                timelineVerticalScroll.post(() -> timelineVerticalScroll.scrollTo(0, 0));
            }
            if (initialFocus[0] != null) {
                initialFocus[0].requestFocus();
                initialFocus[0].post(() -> suppressInitialFocusScroll[0] = false);
            } else {
                suppressInitialFocusScroll[0] = false;
            }
        });
        timelineDialog.setOnDismissListener(d -> enableImmersiveMode());
        timelineNowButton.setOnClickListener(v -> {
            timelineDialog.dismiss();
            openTimelineGuideAroundSelection();
        });
        timelineNextButton.setOnClickListener(v -> {
            timelineDialog.dismiss();
            int anchorIndex = selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size() ? selectedOverlayIndex : Math.max(0, currentIndex);
            openTimelineGuide(anchorIndex, windowStartMs + TIMELINE_SHIFT_MS);
        });
        timelineCloseButton.setOnClickListener(v -> timelineDialog.dismiss());
        timelineDialog.show();
    }

    private boolean moveTimelineFocus(android.widget.ScrollView timelineVerticalScroll, List<List<View>> focusRows, Map<View, Integer> focusCenters, int fromRowIndex, int direction, int preferredCenterMinute) {
        int rowIndex = fromRowIndex + direction;
        while (rowIndex >= 0 && rowIndex < focusRows.size()) {
            List<View> targetRow = focusRows.get(rowIndex);
            if (targetRow != null && !targetRow.isEmpty()) {
                View best = null;
                int bestDistance = Integer.MAX_VALUE;
                for (View candidate : targetRow) {
                    if (candidate == null) {
                        continue;
                    }
                    Integer centerMinute = focusCenters.get(candidate);
                    int distance = centerMinute == null ? Integer.MAX_VALUE : Math.abs(centerMinute - preferredCenterMinute);
                    if (best == null || distance < bestDistance) {
                        best = candidate;
                        bestDistance = distance;
                    }
                }
                if (best != null) {
                    if (!best.requestFocus()) {
                        return false;
                    }
                    ensureTimelineBlockVisible(timelineVerticalScroll, best);
                    return true;
                }
            }
            rowIndex += direction;
        }
        return false;
    }

    private void ensureTimelineBlockVisible(android.widget.ScrollView timelineVerticalScroll, View target) {
        if (timelineVerticalScroll == null || target == null) {
            return;
        }
        timelineVerticalScroll.post(() -> {
            Rect rect = new Rect();
            target.getDrawingRect(rect);
            timelineVerticalScroll.offsetDescendantRectToMyCoords(target, rect);
            int viewportTop = timelineVerticalScroll.getScrollY();
            int viewportBottom = viewportTop + timelineVerticalScroll.getHeight();
            int topPadding = dp(18);
            int bottomPadding = dp(18);
            if (rect.top < viewportTop + topPadding) {
                timelineVerticalScroll.smoothScrollTo(0, Math.max(0, rect.top - topPadding));
            } else if (rect.bottom > viewportBottom - bottomPadding) {
                timelineVerticalScroll.smoothScrollTo(0, Math.max(0, rect.bottom - timelineVerticalScroll.getHeight() + bottomPadding));
            }
        });
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
        appendDiagnosticLine(message, getString(R.string.diagnostics_playback_mode, formatPlaybackModeLabel(diagnostics.playbackMode)));
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

    private String formatPlaybackModeLabel(String playbackMode) {
        if (PlaybackModeStore.MODE_DIRECT.equals(playbackMode)) {
            return getString(R.string.playback_mode_direct);
        }
        if (PlaybackModeStore.MODE_PROXY.equals(playbackMode)) {
            return getString(R.string.playback_mode_proxy);
        }
        return getString(R.string.playback_mode_auto);
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

    private String buildRecordingTitle(RecordingsRepository.RecordingItem item) {
        if (item == null) {
            return getString(R.string.recordings_detail_empty);
        }
        if (item.programTitle != null && !item.programTitle.trim().isEmpty()) {
            return item.programTitle.trim();
        }
        if (item.name != null && !item.name.trim().isEmpty()) {
            return item.name.trim();
        }
        return getString(R.string.recordings_detail_empty);
    }

    private String buildRecordingMeta(RecordingsRepository.RecordingItem item) {
        if (item == null) {
            return getString(R.string.diagnostics_value_unknown);
        }
        if (!item.playable) {
            String start = shortTime(item.startTime);
            String end = shortTime(item.endTime);
            String status = item.status == null || item.status.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : item.status.trim();
            String baseMeta = getString(R.string.recording_meta_scheduled, start, end, status);
            if (item.channelName != null && !item.channelName.trim().isEmpty()) {
                return item.channelName.trim() + "  ·  " + baseMeta;
            }
            return baseMeta;
        }
        String modified = item.modified == null || item.modified.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : item.modified;
        String sizeLabel = item.size <= 0L ? getString(R.string.recording_size_unknown) : humanReadableSize(item.size);
        String baseMeta = getString(R.string.recording_meta, modified, sizeLabel);
        if (item.channelName != null && !item.channelName.trim().isEmpty()) {
            return item.channelName.trim() + "  ·  " + baseMeta;
        }
        return baseMeta;
    }

    private void bindRecordingPoster(ImageView imageView, String posterUrl) {
        if (imageView == null) {
            return;
        }
        if (posterUrl == null || posterUrl.trim().isEmpty()) {
            imageView.setVisibility(View.GONE);
            Glide.with(this).clear(imageView);
            return;
        }
        imageView.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(posterUrl.trim())
                .centerCrop()
                .into(imageView);
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

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private void applyTimelineBlockState(TextView block, boolean live, boolean focused) {
        if (block == null) {
            return;
        }
        int bgColor;
        if (focused) {
            bgColor = live ? 0xFF49A06E : 0xFF4A6F98;
            block.setScaleX(1.03f);
            block.setScaleY(1.03f);
        } else {
            bgColor = live ? 0xFF276B49 : 0xFF2B4056;
            block.setScaleX(1.0f);
            block.setScaleY(1.0f);
        }
        block.setBackgroundColor(bgColor);
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
            holder.badge.setBackgroundTintList(ColorStateList.valueOf(ch.isVod ? 0xAA7A4A19 : 0xAA215D8A));
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
                holder.card.setBackgroundTintList(ColorStateList.valueOf(0xFF2A7C86));
            } else if (tuned) {
                holder.card.setBackgroundTintList(ColorStateList.valueOf(0xCC334457));
            } else {
                holder.card.setBackgroundTintList(ColorStateList.valueOf(0xFF202833));
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
            View card;
            TextView name;
            TextView badge;
            TextView meta;
            ImageView logo;

            ChannelVH(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.channelCard);
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
            if (program.progress >= 0) {
                holder.badge.setText(getString(R.string.guide_program_now));
                holder.badge.setBackgroundColor(0xAA266D3E);
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.progressBar.setProgress(Math.min(100, Math.max(0, program.progress)));
            } else if (position == 1) {
                holder.badge.setText(getString(R.string.guide_program_next));
                holder.badge.setBackgroundColor(0xAA405C86);
                holder.progressBar.setVisibility(View.GONE);
            } else {
                holder.badge.setText(getString(R.string.guide_program_later));
                holder.badge.setBackgroundColor(0xAA4B5361);
                holder.progressBar.setVisibility(View.GONE);
            }
            holder.itemView.setOnClickListener(v -> channelActionsCoordinator.showProgramActionMenu(channel, program));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        final class GuideProgramVH extends RecyclerView.ViewHolder {
            final TextView time;
            final TextView badge;
            final TextView title;
            final android.widget.ProgressBar progressBar;
            final TextView meta;

            GuideProgramVH(@NonNull View itemView) {
                super(itemView);
                time = itemView.findViewById(R.id.programTimeText);
                badge = itemView.findViewById(R.id.programBadgeText);
                title = itemView.findViewById(R.id.programTitleText);
                progressBar = itemView.findViewById(R.id.programProgressBar);
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
            holder.name.setText(buildRecordingTitle(item));
            holder.meta.setText(buildRecordingMeta(item));
            boolean selected = position == selectedRecordingIndex;
            holder.itemView.setBackgroundColor(selected ? 0xFF80542A : 0xFF2C2419);
            holder.itemView.setOnClickListener(v -> {
                selectedRecordingIndex = position;
                notifyDataSetChanged();
                updateRecordingsDetailPanel();
                playRecording(item, result.basePath);
            });
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
        interface OnChannelChosenListener {
            void onChannelChosen(ChannelItem item);
        }

        private final List<ChannelItem> items = new ArrayList<>();
        private final OnChannelChosenListener listener;

        SearchChannelAdapter(List<ChannelItem> initialItems, OnChannelChosenListener listener) {
            this.listener = listener;
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
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChannelChosen(item);
                }
            });
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
