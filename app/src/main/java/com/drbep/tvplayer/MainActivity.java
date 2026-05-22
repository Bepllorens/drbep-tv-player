package com.drbep.tvplayer;

import android.net.Uri;
import android.os.Build;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.LruCache;
import android.text.TextWatcher;
import android.util.Log;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import com.caverock.androidsvg.SVG;

import java.io.InputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends FragmentActivity {
    private static final String TAG = "DRBEP-TV-Native";
    private static final long OVERLAY_HIDE_MS = 6000L;
    private static final long STATUS_HIDE_MS = 2500L;
    private static final long TOUCH_CONTROLS_HIDE_MS = 3000L;
    private static final long TV_TIMESHIFT_HUD_HIDE_MS = 3500L;
    private static final long MENU_DOUBLE_PRESS_MS = 450L;
    private static final long LIVE_BADGE_THRESHOLD_MS = 15000L;
    private static final long RECORDINGS_AUTO_REFRESH_MS = 60000L;
    private static final long OFFLINE_CATALOG_AUTO_REFRESH_MS = 6L * 60L * 60L * 1000L;
    private static final long OFFLINE_CATALOG_EXPIRY_REFRESH_MS = 12L * 60L * 60L * 1000L;
    private static final long OFFLINE_CATALOG_RETRY_BASE_MS = 15L * 60L * 1000L;
    private static final long OFFLINE_CATALOG_RETRY_MAX_MS = 60L * 60L * 1000L;
    private static final long OFFLINE_STARTUP_MAINTENANCE_GRACE_MS = 5L * 60L * 1000L;
    private static final long OFFLINE_APP_UPDATE_STARTUP_DELAY_MS = 2L * 60L * 1000L;
    private static final int OFFLINE_SYNC_HISTORY_LIMIT = 8;
    private static final int CHANNEL_LOGO_PREFETCH_LIMIT = 36;
    private static final int SEARCH_LOGO_PREFETCH_LIMIT = 18;
    private static final String PREFS = "drbep_tv_prefs";
    private static final String PREF_LAST_CHANNEL_ID = "last_channel_id";
    private static final String PREF_LAST_FILTER_KEY = "last_filter_key";
    private static final String PREF_FAVORITES_ONLY = "favorites_only";
    private static final String PREF_FAVORITES = "favorite_channel_ids";
    private static final String PREF_FAVORITE_ORDER = "favorite_order_ids";
    private static final String PREF_PLAYBACK_MODES = "playback_mode_by_channel";
    private static final String PREF_REMINDERS = "channel_reminders";
    private static final String PREF_RECENT_CHANNELS = "recent_channel_items";
    private static final String PREF_RECORDING_RESUME_POSITIONS = "recording_resume_positions";
    private static final String PREF_VOD_RESUME_POSITIONS = "vod_resume_positions";
    private static final String PREF_GLOBAL_SEARCH_RECENTS = "global_search_recents";
    private static final String PREF_STARTUP_HUB_DISABLED = "startup_hub_disabled";
    private static final String PREF_LAST_VOD_ID = "last_vod_id";
    private static final String PREF_CHANNEL_COLLECTIONS = "channel_collections";
    private static final String PREF_CHANNEL_PROFILES = "channel_profiles";
    private static final String PREF_PLAYBACK_DIAGNOSTICS = "playback_diagnostics";
    private static final String PREF_PLAYBACK_REPAIR_ENABLED = "playback_repair_enabled";
    private static final String PREF_PLAYBACK_LEARNED_MODES = "playback_learned_modes";
    private static final String PREF_OFFLINE_SYNC_HISTORY = "offline_sync_history";
    private static final String PREF_MULTIVIEW_PRESET_PREFIX = "multiview_preset_";
    private static final String PREF_LAST_UPDATE_PROMPT_VERSION_CODE = "last_update_prompt_version_code";
    private static final String PREF_LAST_SEEN_APP_VERSION_CODE = "last_seen_app_version_code";
    private static final int MULTIVIEW_PRESET_COUNT = 3;
    private static final String PREF_TABLET_ORIENTATION_LOCK = "tablet_orientation_lock";
    private static final int FILTER_ALL = 0;
    private static final int FILTER_PLATFORM = 1;
    private static final int FILTER_CUSTOM_GROUP = 2;
    private static final int FILTER_VOD = 3;
    private static final int FILTER_VOD_ADULT = 4;
    private static final int FILTER_FAVORITES = 5;
    private static final long TIMELINE_WINDOW_MS = 12L * 60L * 60L * 1000L;
    private static final long TIMELINE_SHIFT_MS = 2L * 60L * 60L * 1000L;
    private static final String RECORDINGS_DAY_ALL = "all";
    private static final String RECORDINGS_DAY_TODAY = "today";
    private static final String RECORDINGS_DAY_TOMORROW = "tomorrow";
    private static final String RECORDINGS_DAY_WEEK = "week";
    private static final int GLOBAL_SEARCH_HEADER = 0;
    private static final int GLOBAL_SEARCH_HISTORY = 1;
    private static final int GLOBAL_SEARCH_CHANNEL = 2;
    private static final int GLOBAL_SEARCH_VOD = 3;
    private static final int GLOBAL_SEARCH_EPG = 4;
    private static final int GLOBAL_SEARCH_RECORDING = 5;
    private static final int GLOBAL_SEARCH_FILTER_ALL = 0;
    private static final int GLOBAL_SEARCH_FILTER_TV = 1;
    private static final int GLOBAL_SEARCH_FILTER_VOD = 2;
    private static final int GLOBAL_SEARCH_FILTER_FAVORITES = 3;
    private static final int GLOBAL_SEARCH_FILTER_EPG = 4;
    private static final int GLOBAL_SEARCH_FILTER_RECORDINGS = 5;

    private PlayerView playerView;
    private TextView errorText;
    private TextView statusText;
    private TextView filterText;
    private TextView touchPrevFilterButton;
    private TextView touchNextFilterButton;
    private TextView touchSearchButton;
    private TextView touchRecentButton;
    private TextView touchFavoritesButton;
    private TextView quickTvButton;
    private TextView quickVodButton;
    private TextView quickAdultButton;
    private TextView quickGrabButton;
    private View touchHomeHub;
    private TextView touchHomeTitleText;
    private TextView touchHomeSubtitleText;
    private TextView touchHomeTvButton;
    private TextView touchHomeVodButton;
    private TextView touchHomeAdultButton;
    private TextView touchHomeGrabButton;
    private TextView touchHomeRecentButton;
    private TextView touchHomeFavoritesButton;
    private TextView touchHomeListButton;
    private TextView touchHomeMultiButton;
    private View multiViewContainer;
    private TextView multiViewCloseButton;
    private TextView multiViewHintText;
    private final PlayerView[] multiPlayerViews = new PlayerView[4];
    private final View[] multiTiles = new View[4];
    private final TextView[] multiLabels = new TextView[4];
    private final TextView[] multiAudioBadges = new TextView[4];
    private final List<PlayerController> multiPlayerControllers = new ArrayList<>();
    private final List<ChannelItem> multiViewChannels = new ArrayList<>();
    private final String[] multiViewChannelIds = new String[4];
    private int multiViewActiveIndex = 0;
    private boolean mainWasPlayingBeforeMultiView;
    private EditText overlaySearchInput;
    private TextView overlayCurrentChannelText;
    private TextView overlayCurrentMetaText;
    private TextView overlayPlaybackRouteText;
    private TextView overlayEmptyText;
    private TextView overlayRecentText;
    private TextView zapChannelText;
    private TextView zapMetaText;
    private TextView quickSearchQueryText;
    private TextView quickSearchResultText;
    private TextView recordingsSectionText;
    private TextView recordingsSummaryText;
    private TextView recordingsHintText;
    private TextView recordingsCompletedButton;
    private TextView recordingsScheduledButton;
    private TextView recordingsRefreshButton;
    private TextView versionBadgeText;
    private TextView hdrBadgeText;
    private TextView liveStateBadgeText;
    private View touchControlsBar;
    private View timeshiftBarContainer;
    private TextView touchListButton;
    private TextView touchGuideButton;
    private TextView touchPreviousButton;
    private TextView touchInfoButton;
    private TextView touchVodLibraryButton;
    private TextView touchToolsButton;
    private TextView touchRotateButton;
    private TextView touchRewindButton;
    private TextView touchPlayPauseButton;
    private TextView touchForwardButton;
    private TextView timeshiftStatusText;
    private TextView timeshiftLiveButton;
    private View playbackGestureLayer;
    private ImageView recordingDetailPosterImage;
    private TextView recordingDetailTitleText;
    private TextView recordingDetailMetaText;
    private TextView recordingDetailPathText;
    private TextView recordingDetailActionText;
    private android.app.Dialog activeTimelineDialog;
    private LinearLayout activeVodVisualFilterRow;
    private List<TimelineChannelPrograms> activeTimelineRows = new ArrayList<>();
    private List<RecordingsRepository.RecordingItem> activeTimelineScheduledItems = new ArrayList<>();
    private List<RecordingsRepository.RecordingItem> activeProgramScheduledItems = new ArrayList<>();
    private long activeTimelineWindowStartMs;
    private String activeTimelineAnchorChannelId;
    private int activeTimelineFocusedCenterMinute = -1;
    private String lastTimelineAnchorChannelId;
    private long lastTimelineWindowStartMs;
    private int lastTimelineFocusedCenterMinute = -1;
    private String lastVisualEpgChannelId;
    private String lastVisualEpgProgramStartTime;
    private String currentPlaybackRecordingId;
    private String currentPlaybackReturnChannelId;
    private String currentPlaybackVodId;
    private String lastVodId;
    private final Map<String, Long> recordingResumePositions = new HashMap<>();
    private final Map<String, Long> vodResumePositions = new HashMap<>();
    private boolean refreshingTimelineDialog;
    private View channelOverlay;
    private View zapBanner;
    private View quickSearchOverlay;
    private View recordingsPanel;
    private RecyclerView channelList;
    private RecyclerView recordingsRecyclerView;
    private SeekBar timeshiftSeekBar;

    private PlayerController playerController;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService epgExecutor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable recordingsAutoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (!recordingsAutoRefreshEnabled || !isRecordingsPanelVisible()) {
                return;
            }
            refreshRecordingsPanel();
            uiHandler.postDelayed(this, RECORDINGS_AUTO_REFRESH_MS);
        }
    };
    private final Runnable vodProgressSaveRunnable = new Runnable() {
        @Override
        public void run() {
            rememberCurrentVodPosition();
            uiHandler.postDelayed(this, 15_000L);
        }
    };
    private final Runnable offlineCatalogAutoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            runOfflineMaintenance(false);
            scheduleOfflineCatalogAutoRefresh();
        }
    };
    private final Runnable offlineCatalogRetryRunnable = new Runnable() {
        @Override
        public void run() {
            runOfflineMaintenance(false);
        }
    };
    private final List<ChannelItem> channels = new ArrayList<>();
    private final List<ChannelItem> allChannels = new ArrayList<>();
    private final List<ChannelFilter> filters = new ArrayList<>();
    private final Map<String, String> epgNowByChannelId = new HashMap<>();
    private final Map<String, EpgRepository.EpgProgramPair> epgProgramPairByChannelId = new HashMap<>();
    private volatile boolean epgLoadInFlight = false;
    private final LruCache<String, Drawable> channelLogoCache = new LruCache<>(96);
    private ChannelAdapter channelAdapter;
    private CatalogRepository catalogRepository;
    private CatalogSnapshotStore catalogSnapshotStore;
    private EpgRepository epgRepository;
    private RecordingsRepository recordingsRepository;
    private ReminderStore reminderStore;
    private RecentChannelsStore recentChannelsStore;
    private FavoriteOrderStore favoriteOrderStore;
    private PlaybackModeStore playbackModeStore;
    private ChannelCollectionStore channelCollectionStore;
    private ChannelProfileStore channelProfileStore;
    private PlaybackDiagnosticsStore playbackDiagnosticsStore;
    private ChannelActionsCoordinator channelActionsCoordinator;
    private ChannelOverlayCoordinator channelOverlayCoordinator;
    private RemoteInputRouter remoteInputRouter;
    private TouchControlsController touchControlsController;
    private HttpClient httpClient;
    private AppUpdateManager appUpdateManager;
    private AudioManager audioManager;
    private String baseUrl;
    private SharedPreferences prefs;

    private int currentIndex = -1;
    private int selectedOverlayIndex = 0;
    private boolean favoritesOnly;
    private boolean startupHubShown;
    private String lastChannelId;
    private String selectedFilterKey = "all";
    private final StringBuilder quickSearchBuffer = new StringBuilder();
    private final List<ChannelItem> quickSearchMatches = new ArrayList<>();
    private final List<String> globalSearchRecents = new ArrayList<>();
    private int quickSearchSelectionIndex = 0;
    private final Set<String> favoriteChannelIds = new HashSet<>();
    private final Map<String, String> temporaryPlaybackModesByChannelId = new HashMap<>();
    private final Map<String, String> learnedPlaybackModesByChannelId = new HashMap<>();
    private final Map<String, Set<String>> playbackRepairAttemptsByChannelId = new HashMap<>();
    private final Map<String, PlayerController.StreamInfo> streamInfoByChannelId = new HashMap<>();
    private RecordingsAdapter recordingsAdapter;
    private final RecordingsController recordingsController = new RecordingsController();
    private String recordingsChannelFilter = "";
    private String recordingsDayFilter = RECORDINGS_DAY_ALL;
    private boolean touchDeviceMode;
    private boolean recordingsAutoRefreshEnabled;
    private boolean playbackRepairEnabled = true;
    private long lastCatalogLoadDurationMs;
    private long lastEpgNowLoadDurationMs;
    private long lastApplyChannelsDurationMs;
    private long lastImageCacheClearMs;
    private float touchGestureDownX = Float.NaN;
    private float touchGestureDownY = Float.NaN;
    private boolean timeshiftSeekUserDragging;
    private boolean tabletOrientationLocked;
    private float tabletBrightnessLevel = 0.5f;
    private float touchGestureLastY = Float.NaN;
    private boolean touchGestureVerticalHandled;
    private boolean appUpdateCheckRunning;
    private boolean offlineCatalogRefreshRunning;
    private AppUpdateManager.UpdateInfo lastKnownAppUpdateInfo;
    private long lastAppUpdateCheckMs;
    private String lastAppUpdateError = "";
    private long lastOfflineCatalogRefreshAttemptMs;
    private long lastOfflineCatalogRefreshSuccessMs;
    private String lastOfflineCatalogRefreshError = "";
    private long lastOfflineMaintenanceMs;
    private String lastOfflineMaintenanceError = "";
    private long activityCreatedAtMs;
    private String epgFullLoadScheduledForChannelId = "";
    private boolean epgFullCatalogLoaded;
    private boolean epgFullCatalogLoadRequested;
    private int offlineCatalogRetryCount;
    private int globalSearchGeneration;
    private int globalSearchFilter = GLOBAL_SEARCH_FILTER_ALL;
    private Runnable pendingGlobalSearchRunnable;

    private static final class TimelineChannelPrograms {
        final ChannelItem channel;
        final List<EpgRepository.EpgProgram> programs;

        TimelineChannelPrograms(ChannelItem channel, List<EpgRepository.EpgProgram> programs) {
            this.channel = channel;
            this.programs = programs;
        }
    }


    private static final class VisualEpgEntry {
        final ChannelItem channel;
        final EpgRepository.EpgProgram program;

        VisualEpgEntry(ChannelItem channel, EpgRepository.EpgProgram program) {
            this.channel = channel;
            this.program = program;
        }
    }


    private static final class VisualEpgSection {
        final String title;
        final List<VisualEpgEntry> entries;

        VisualEpgSection(String title, List<VisualEpgEntry> entries) {
            this.title = title;
            this.entries = entries;
        }
    }

    private static final class EpgSearchResult {
        final ChannelItem channel;
        final EpgRepository.EpgProgram program;

        EpgSearchResult(ChannelItem channel, EpgRepository.EpgProgram program) {
            this.channel = channel;
            this.program = program;
        }
    }

    private static final class GlobalSearchResult {
        final int type;
        final String title;
        final String meta;
        final String badge;
        final ChannelItem channel;
        final EpgSearchResult epgResult;
        final RecordingsRepository.RecordingItem recording;
        final String recordingBasePath;

        GlobalSearchResult(int type, String title, String meta, String badge, ChannelItem channel, EpgSearchResult epgResult, RecordingsRepository.RecordingItem recording, String recordingBasePath) {
            this.type = type;
            this.title = title;
            this.meta = meta;
            this.badge = badge;
            this.channel = channel;
            this.epgResult = epgResult;
            this.recording = recording;
            this.recordingBasePath = recordingBasePath;
        }
    }

    private static final class StartupHubState {
        final ChannelItem currentChannel;
        final ChannelItem lastVod;
        final RecordingsRepository.RecordingItem resumeRecording;
        final String resumeRecordingBasePath;
        final int completedRecordings;
        final int scheduledRecordings;

        StartupHubState(ChannelItem currentChannel, ChannelItem lastVod, RecordingsRepository.RecordingItem resumeRecording, String resumeRecordingBasePath, int completedRecordings, int scheduledRecordings) {
            this.currentChannel = currentChannel;
            this.lastVod = lastVod;
            this.resumeRecording = resumeRecording;
            this.resumeRecordingBasePath = resumeRecordingBasePath;
            this.completedRecordings = completedRecordings;
            this.scheduledRecordings = scheduledRecordings;
        }
    }

    private final Runnable hideOverlayRunnable = this::hideOverlay;
    private final Runnable hideStatusRunnable = () -> {
        if (statusText != null) {
            statusText.setVisibility(View.GONE);
        }
    };
    private final Runnable hideHdrBadgeRunnable = () -> {
        if (hdrBadgeText != null) {
            hdrBadgeText.setVisibility(View.GONE);
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
        activityCreatedAtMs = System.currentTimeMillis();
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.playerView);
        errorText = findViewById(R.id.errorText);
        statusText = findViewById(R.id.statusText);
        filterText = findViewById(R.id.filterText);
        touchPrevFilterButton = findViewById(R.id.touchPrevFilterButton);
        touchNextFilterButton = findViewById(R.id.touchNextFilterButton);
        touchSearchButton = findViewById(R.id.touchSearchButton);
        touchRecentButton = findViewById(R.id.touchRecentButton);
        touchFavoritesButton = findViewById(R.id.touchFavoritesButton);
        quickTvButton = findViewById(R.id.quickTvButton);
        quickVodButton = findViewById(R.id.quickVodButton);
        quickAdultButton = findViewById(R.id.quickAdultButton);
        quickGrabButton = findViewById(R.id.quickGrabButton);
        touchHomeHub = findViewById(R.id.touchHomeHub);
        touchHomeTitleText = findViewById(R.id.touchHomeTitleText);
        touchHomeSubtitleText = findViewById(R.id.touchHomeSubtitleText);
        touchHomeTvButton = findViewById(R.id.touchHomeTvButton);
        touchHomeVodButton = findViewById(R.id.touchHomeVodButton);
        touchHomeAdultButton = findViewById(R.id.touchHomeAdultButton);
        touchHomeGrabButton = findViewById(R.id.touchHomeGrabButton);
        touchHomeRecentButton = findViewById(R.id.touchHomeRecentButton);
        touchHomeFavoritesButton = findViewById(R.id.touchHomeFavoritesButton);
        touchHomeListButton = findViewById(R.id.touchHomeListButton);
        touchHomeMultiButton = findViewById(R.id.touchHomeMultiButton);
        multiViewContainer = findViewById(R.id.multiViewContainer);
        multiViewCloseButton = findViewById(R.id.multiViewCloseButton);
        multiViewHintText = findViewById(R.id.multiViewHintText);
        multiPlayerViews[0] = findViewById(R.id.multiPlayerView1);
        multiPlayerViews[1] = findViewById(R.id.multiPlayerView2);
        multiPlayerViews[2] = findViewById(R.id.multiPlayerView3);
        multiPlayerViews[3] = findViewById(R.id.multiPlayerView4);
        multiTiles[0] = findViewById(R.id.multiTile1);
        multiTiles[1] = findViewById(R.id.multiTile2);
        multiTiles[2] = findViewById(R.id.multiTile3);
        multiTiles[3] = findViewById(R.id.multiTile4);
        multiLabels[0] = findViewById(R.id.multiLabel1);
        multiLabels[1] = findViewById(R.id.multiLabel2);
        multiLabels[2] = findViewById(R.id.multiLabel3);
        multiLabels[3] = findViewById(R.id.multiLabel4);
        multiAudioBadges[0] = findViewById(R.id.multiAudioBadge1);
        multiAudioBadges[1] = findViewById(R.id.multiAudioBadge2);
        multiAudioBadges[2] = findViewById(R.id.multiAudioBadge3);
        multiAudioBadges[3] = findViewById(R.id.multiAudioBadge4);
        overlaySearchInput = findViewById(R.id.overlaySearchInput);
        overlayCurrentChannelText = findViewById(R.id.overlayCurrentChannelText);
        overlayCurrentMetaText = findViewById(R.id.overlayCurrentMetaText);
        overlayPlaybackRouteText = findViewById(R.id.overlayPlaybackRouteText);
        overlayEmptyText = findViewById(R.id.overlayEmptyText);
        overlayRecentText = findViewById(R.id.overlayRecentText);
        zapBanner = findViewById(R.id.zapBanner);
        zapChannelText = findViewById(R.id.zapChannelText);
        zapMetaText = findViewById(R.id.zapMetaText);
        quickSearchOverlay = findViewById(R.id.quickSearchOverlay);
        quickSearchQueryText = findViewById(R.id.quickSearchQueryText);
        quickSearchResultText = findViewById(R.id.quickSearchResultText);
        recordingsSectionText = findViewById(R.id.recordingsSectionText);
        recordingsSummaryText = findViewById(R.id.recordingsSummaryText);
        recordingsHintText = findViewById(R.id.recordingsHintText);
        recordingsCompletedButton = findViewById(R.id.recordingsCompletedButton);
        recordingsScheduledButton = findViewById(R.id.recordingsScheduledButton);
        recordingsRefreshButton = findViewById(R.id.recordingsRefreshButton);
        versionBadgeText = findViewById(R.id.versionBadgeText);
        hdrBadgeText = findViewById(R.id.hdrBadgeText);
        liveStateBadgeText = findViewById(R.id.liveStateBadgeText);
        touchControlsBar = findViewById(R.id.touchControlsBar);
        timeshiftBarContainer = findViewById(R.id.timeshiftBarContainer);
        touchListButton = findViewById(R.id.touchListButton);
        touchGuideButton = findViewById(R.id.touchGuideButton);
        touchPreviousButton = findViewById(R.id.touchPreviousButton);
        touchInfoButton = findViewById(R.id.touchInfoButton);
        touchVodLibraryButton = findViewById(R.id.touchVodLibraryButton);
        touchToolsButton = findViewById(R.id.touchToolsButton);
        touchRotateButton = findViewById(R.id.touchRotateButton);
        touchRewindButton = findViewById(R.id.touchRewindButton);
        touchPlayPauseButton = findViewById(R.id.touchPlayPauseButton);
        touchForwardButton = findViewById(R.id.touchForwardButton);
        timeshiftStatusText = findViewById(R.id.timeshiftStatusText);
        timeshiftLiveButton = findViewById(R.id.timeshiftLiveButton);
        playbackGestureLayer = findViewById(R.id.playbackGestureLayer);
        recordingDetailPosterImage = findViewById(R.id.recordingDetailPosterImage);
        recordingDetailTitleText = findViewById(R.id.recordingDetailTitleText);
        recordingDetailMetaText = findViewById(R.id.recordingDetailMetaText);
        recordingDetailPathText = findViewById(R.id.recordingDetailPathText);
        recordingDetailActionText = findViewById(R.id.recordingDetailActionText);
        channelOverlay = findViewById(R.id.channelOverlay);
        recordingsPanel = findViewById(R.id.recordingsPanel);
        channelList = findViewById(R.id.channelList);
        if (channelOverlay != null) {
            channelOverlay.setClickable(true);
            channelOverlay.setOnTouchListener((v, event) -> {
                if (touchDeviceMode) {
                    uiHandler.removeCallbacks(hideOverlayRunnable);
                }
                return false;
            });
        }
        if (recordingsPanel != null) {
            recordingsPanel.setClickable(true);
            recordingsPanel.setOnTouchListener((v, event) -> true);
        }
        recordingsRecyclerView = findViewById(R.id.recordingsRecyclerView);
        timeshiftSeekBar = findViewById(R.id.timeshiftSeekBar);

        if (versionBadgeText != null) {
            versionBadgeText.setVisibility(View.GONE);
        }
        if (liveStateBadgeText != null) {
            liveStateBadgeText.setClickable(true);
            liveStateBadgeText.setFocusable(true);
            liveStateBadgeText.setOnClickListener(v -> retryCurrentPlayback());
            liveStateBadgeText.setOnLongClickListener(v -> {
                showPlaybackDiagnosticsDialog();
                return true;
            });
        }

        baseUrl = resolveBaseUrl();
        catalogSnapshotStore = new CatalogSnapshotStore(this);
        catalogRepository = new CatalogRepository(baseUrl, catalogSnapshotStore, BuildConfig.STANDALONE_MODE);
        epgRepository = new EpgRepository(baseUrl, catalogSnapshotStore, BuildConfig.STANDALONE_MODE);
        recordingsRepository = new RecordingsRepository(baseUrl);
    httpClient = new HttpClient();
        appUpdateManager = new AppUpdateManager(this, catalogSnapshotStore);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        reminderStore = new ReminderStore(prefs, PREF_REMINDERS);
        recentChannelsStore = new RecentChannelsStore(prefs, PREF_RECENT_CHANNELS);
        favoriteOrderStore = new FavoriteOrderStore(prefs, PREF_FAVORITE_ORDER);
        playbackModeStore = new PlaybackModeStore(prefs, PREF_PLAYBACK_MODES);
        channelCollectionStore = new ChannelCollectionStore(prefs, PREF_CHANNEL_COLLECTIONS);
        channelProfileStore = new ChannelProfileStore(prefs, PREF_CHANNEL_PROFILES);
        playbackDiagnosticsStore = new PlaybackDiagnosticsStore(prefs, PREF_PLAYBACK_DIAGNOSTICS);
        loadRecordingResumePositions();
        loadVodResumePositions();
        loadGlobalSearchRecents();
        loadLearnedPlaybackModes();
        channelOverlayCoordinator = new ChannelOverlayCoordinator(channels, allChannels, filters, favoriteChannelIds, favoriteOrderStore, channelCollectionStore, channelProfileStore);
        channelActionsCoordinator = new ChannelActionsCoordinator(this, new ChannelActionsCoordinator.Host() {
            @Override
            public void tuneSelectedChannel() {
                MainActivity.this.tuneSelectedChannel();
            }

            @Override
            public void tuneChannel(ChannelItem channelItem) {
                if (channelItem != null) {
                    MainActivity.this.tuneChannelById(channelItem.id);
                }
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
            public void openPersonalListsSelector(ChannelItem channelItem) {
                MainActivity.this.showPersonalListsDialog(channelItem);
            }

            @Override
            public void openChannelProfile(ChannelItem channelItem) {
                MainActivity.this.showChannelProfileDialog(channelItem);
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
            public boolean isProgramScheduled(ChannelItem channelItem, EpgRepository.EpgProgram program) {
                return MainActivity.this.isProgramScheduled(channelItem, program, activeProgramScheduledItems);
            }

            @Override
            public void cancelScheduledProgram(ChannelItem channelItem, EpgRepository.EpgProgram program) {
                MainActivity.this.cancelScheduledProgram(channelItem, program);
            }

            @Override
            public void createReminder(ChannelItem channelItem, EpgRepository.EpgProgram program) {
                MainActivity.this.createReminder(channelItem, program);
            }
        });
        lastChannelId = prefs.getString(PREF_LAST_CHANNEL_ID, "");
        selectedFilterKey = prefs.getString(PREF_LAST_FILTER_KEY, "all");
        favoritesOnly = prefs.getBoolean(PREF_FAVORITES_ONLY, false);
        playbackRepairEnabled = prefs.getBoolean(PREF_PLAYBACK_REPAIR_ENABLED, true);
        lastVodId = prefs.getString(PREF_LAST_VOD_ID, "");
        favoritesOnly = false;
        remoteInputRouter = new RemoteInputRouter(createRemoteInputHost(), MENU_DOUBLE_PRESS_MS);
        Set<String> storedFavorites = prefs.getStringSet(PREF_FAVORITES, new HashSet<>());
        if (storedFavorites != null) {
            favoriteChannelIds.addAll(storedFavorites);
        }
        reminderStore.load();
        recentChannelsStore.load();
        favoriteOrderStore.load();
        playbackModeStore.load();
        channelCollectionStore.load();
        channelProfileStore.load();
        playbackDiagnosticsStore.load();
        favoriteOrderStore.syncToFavorites(favoriteChannelIds);
        touchDeviceMode = detectTouchDeviceMode();
        touchControlsController = new TouchControlsController(uiHandler, createTouchControlsHost(), TOUCH_CONTROLS_HIDE_MS, TV_TIMESHIFT_HUD_HIDE_MS);
        if (!touchDeviceMode) {
            if (quickTvButton != null) quickTvButton.setVisibility(View.GONE);
            if (quickVodButton != null) quickVodButton.setVisibility(View.GONE);
            if (quickAdultButton != null) quickAdultButton.setVisibility(View.GONE);
            if (quickGrabButton != null) quickGrabButton.setVisibility(View.GONE);
        }
        if (isOfflineRecordingsDisabled()) {
            if (quickGrabButton != null) quickGrabButton.setVisibility(View.GONE);
            if (touchHomeGrabButton != null) touchHomeGrabButton.setVisibility(View.GONE);
        }
        tabletOrientationLocked = prefs.getBoolean(PREF_TABLET_ORIENTATION_LOCK, false);
        initializeTabletBrightness();
        applyTabletOrientationMode();

        setupPlayer();
        setupChannelList();
        setupRecordingsPanel();
        setupTouchControls();
        enableImmersiveMode();
        loadChannels();
        showPostUpdateNotesIfNeeded();
        scheduleAppUpdateCheckOnStartup();
        scheduleOfflineCatalogAutoRefresh();
        uiHandler.postDelayed(reminderTickRunnable, 30000L);
        uiHandler.postDelayed(vodProgressSaveRunnable, 15_000L);
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

            @Override
            public void showHdrBadge(String label) {
                MainActivity.this.showHdrBadge(label);
            }

            @Override
            public boolean isPlaybackRepairEnabled() {
                return MainActivity.this.playbackRepairEnabled;
            }

            @Override
            public void recordPlaybackError(PlayerController.PlaybackRequest request, PlayerController.PlaybackDiagnostics diagnostics) {
                MainActivity.this.recordPlaybackError(request, diagnostics);
            }

            @Override
            public void onFirstVideoFrameRendered(String channelId) {
                MainActivity.this.scheduleFullEpgLoadAfterFirstFrame(channelId);
            }
        });
        playerController.initialize();
    }

    private void setupChannelList() {
        channelAdapter = new ChannelAdapter();
        channelList.setLayoutManager(new LinearLayoutManager(this));
        channelList.setHasFixedSize(true);
        channelList.setItemViewCacheSize(24);
        channelList.setAdapter(channelAdapter);
    }

    private void setupRecordingsPanel() {
        if (recordingsRecyclerView != null) {
            recordingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
        if (recordingsCompletedButton != null) {
            recordingsCompletedButton.setOnClickListener(v -> switchRecordingsMode(false));
        }
        if (recordingsScheduledButton != null) {
            recordingsScheduledButton.setOnClickListener(v -> switchRecordingsMode(true));
        }
        if (recordingsRefreshButton != null) {
            recordingsRefreshButton.setOnClickListener(v -> refreshRecordingsPanel());
        }
        updateRecordingsDetailPanel();
    }

    private boolean detectTouchDeviceMode() {
        PackageManager pm = getPackageManager();
        boolean hasTouchscreen = pm != null && pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
        boolean hasLeanback = pm != null && pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK);
        return hasTouchscreen && !hasLeanback;
    }

    private TouchControlsController.Host createTouchControlsHost() {
        return new TouchControlsController.Host() {
            @Override
            public boolean isTouchDeviceMode() {
                return touchDeviceMode;
            }

            @Override
            public boolean isTouchControlsVisible() {
                return touchControlsBar != null && touchControlsBar.getVisibility() == View.VISIBLE;
            }

            @Override
            public boolean isOverlayVisible() {
                return MainActivity.this.isOverlayVisible();
            }

            @Override
            public boolean isRecordingsPanelVisible() {
                return MainActivity.this.isRecordingsPanelVisible();
            }

            @Override
            public boolean isMultiViewVisible() {
                return MainActivity.this.isMultiViewVisible();
            }

            @Override
            public boolean hasSeekablePlayback() {
                return playerController != null && playerController.getPlaybackSeekState() != null;
            }

            @Override
            public void setTouchControlsVisible(boolean visible) {
                if (touchControlsBar != null) {
                    touchControlsBar.setVisibility(visible ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void hideTouchHomeHub() {
                if (touchHomeHub != null) {
                    touchHomeHub.setVisibility(View.GONE);
                }
            }

            @Override
            public void hideTimeshiftBar() {
                if (timeshiftBarContainer != null) {
                    timeshiftBarContainer.setVisibility(View.GONE);
                }
            }

            @Override
            public void updateTouchHomeHub() {
                MainActivity.this.updateTouchHomeHub();
            }

            @Override
            public void updateTimeshiftBar() {
                MainActivity.this.updateTimeshiftBar();
            }
        };
    }

    private void setupTouchControls() {
        if (touchControlsBar == null) {
            return;
        }
        if (!touchDeviceMode) {
            touchControlsBar.setVisibility(View.GONE);
            if (timeshiftBarContainer != null) {
                timeshiftBarContainer.setVisibility(View.GONE);
            }
            updateVodTouchControlsState();
            if (touchPrevFilterButton != null) {
                touchPrevFilterButton.setVisibility(View.GONE);
            }
            if (touchNextFilterButton != null) {
                touchNextFilterButton.setVisibility(View.GONE);
            }
            return;
        }
        touchControlsBar.setVisibility(View.VISIBLE);
        updateVodTouchControlsState();
        updateTouchHomeHub();
        updateTimeshiftBar();
        scheduleTouchControlsAutoHide();
        if (touchPrevFilterButton != null) {
            touchPrevFilterButton.setVisibility(View.VISIBLE);
            touchPrevFilterButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                cycleFilter(-1);
            });
        }
        if (touchNextFilterButton != null) {
            touchNextFilterButton.setVisibility(View.VISIBLE);
            touchNextFilterButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                cycleFilter(1);
            });
        }
        if (filterText != null) {
            filterText.setClickable(true);
            filterText.setFocusable(true);
            filterText.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                cycleFilter(1);
            });
            filterText.setOnLongClickListener(v -> {
                showTouchControlsTemporarily();
                cycleFilter(-1);
                return true;
            });
        }
        if (touchSearchButton != null) {
            touchSearchButton.setVisibility(View.VISIBLE);
            touchSearchButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                focusOverlaySearchInput();
            });
        }
        if (touchRecentButton != null) {
            touchRecentButton.setVisibility(View.VISIBLE);
            touchRecentButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                showRecentChannelsQuickDialog();
            });
        }
        if (touchFavoritesButton != null) {
            touchFavoritesButton.setVisibility(View.VISIBLE);
            touchFavoritesButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                showFavoriteChannelsQuickDialog();
            });
            touchFavoritesButton.setOnLongClickListener(v -> {
                showTouchControlsTemporarily();
                toggleFavoritesOnlyMode();
                return true;
            });
        }
        if (quickTvButton != null) {
            quickTvButton.setOnClickListener(v -> applyQuickOverlayTarget("tv"));
        }
        if (quickVodButton != null) {
            quickVodButton.setOnClickListener(v -> applyQuickOverlayTarget("vod"));
        }
        if (quickAdultButton != null) {
            quickAdultButton.setOnClickListener(v -> applyQuickOverlayTarget("vod-adult"));
        }
        if (quickGrabButton != null) {
            quickGrabButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                openRecordingsBrowser();
            });
        }
        if (touchHomeTvButton != null) {
            touchHomeTvButton.setOnClickListener(v -> applyQuickOverlayTarget("tv"));
        }
        if (touchHomeVodButton != null) {
            touchHomeVodButton.setOnClickListener(v -> applyQuickOverlayTarget("vod"));
        }
        if (touchHomeAdultButton != null) {
            touchHomeAdultButton.setOnClickListener(v -> applyQuickOverlayTarget("vod-adult"));
        }
        if (touchHomeGrabButton != null) {
            touchHomeGrabButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                openRecordingsBrowser();
            });
        }
        if (touchHomeRecentButton != null) {
            touchHomeRecentButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                showRecentChannelsQuickDialog();
            });
        }
        if (touchHomeFavoritesButton != null) {
            touchHomeFavoritesButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                applyQuickOverlayTarget("favorites");
            });
            touchHomeFavoritesButton.setOnLongClickListener(v -> {
                showTouchControlsTemporarily();
                showFavoriteChannelsQuickDialog();
                return true;
            });
        }
        if (touchHomeListButton != null) {
            touchHomeListButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                showGlobalSearchDialog();
            });
            touchHomeListButton.setOnLongClickListener(v -> {
                showTouchControlsTemporarily();
                showOverlay();
                return true;
            });
        }
        if (touchHomeMultiButton != null) {
            touchHomeMultiButton.setOnClickListener(v -> openMultiView());
        }
        if (multiViewCloseButton != null) {
            multiViewCloseButton.setOnClickListener(v -> closeMultiView());
        }
        for (int i = 0; i < multiTiles.length; i++) {
            final int slot = i;
            if (multiTiles[i] != null) {
                multiTiles[i].setOnClickListener(v -> focusMultiViewSlot(slot));
                multiTiles[i].setOnLongClickListener(v -> {
                    showMultiViewChannelPicker(slot);
                    return true;
                });
            }
        }
        if (overlaySearchInput != null) {
            overlaySearchInput.setVisibility(View.VISIBLE);
            overlaySearchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyOverlaySearchQuery(s == null ? "" : s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            overlaySearchInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    showTouchControlsTemporarily();
                    uiHandler.removeCallbacks(hideOverlayRunnable);
                }
            });
        }

        if (touchListButton != null) {
            touchListButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                if (isOverlayVisible()) {
                    hideOverlay();
                } else {
                    showOverlay();
                }
            });
        }
        if (touchGuideButton != null) {
            touchGuideButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                ChannelItem current = getCurrentPlaybackChannelItem();
                if (current != null && current.isVod) {
                    showVodLibraryDialog();
                } else {
                    openTimelineGuideAroundSelection();
                }
            });
        }
        if (touchPreviousButton != null) {
            touchPreviousButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                ChannelItem current = getCurrentPlaybackChannelItem();
                if (current != null && current.isVod) {
                    showVodInfoDialog(current);
                } else {
                    tunePreviousChannel();
                }
            });
        }
        if (touchInfoButton != null) {
            touchInfoButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                openCurrentProgramInfoFromTouch();
            });
            touchInfoButton.setOnLongClickListener(v -> {
                showTouchControlsTemporarily();
                showPlaybackDiagnosticsDialog();
                return true;
            });
        }
        if (touchVodLibraryButton != null) {
            touchVodLibraryButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                showVodLibraryDialog();
            });
        }
        if (touchToolsButton != null) {
            touchToolsButton.setText("Grab");
            touchToolsButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                openRecordingsBrowser();
            });
            touchToolsButton.setOnLongClickListener(v -> {
                showTouchControlsTemporarily();
                showV12ToolsMenu();
                return true;
            });
        }
        if (touchRotateButton != null) {
            touchRotateButton.setVisibility(View.VISIBLE);
            touchRotateButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                toggleTabletOrientationLock();
            });
            updateTouchRotateButtonLabel();
        }
        if (touchPlayPauseButton != null) {
            touchPlayPauseButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                if (playerController != null) {
                    playerController.togglePlayback();
                }
            });
        }
        if (touchRewindButton != null) {
            touchRewindButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                if (playerController == null || !playerController.seekTimeshiftBack()) {
                    showStatus(getString(R.string.status_touch_seek_unavailable));
                }
            });
        }
        if (touchForwardButton != null) {
            touchForwardButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                if (playerController == null || !playerController.seekTimeshiftForward()) {
                    showStatus(getString(R.string.status_touch_seek_unavailable));
                }
            });
        }
        if (playbackGestureLayer != null) {
            playbackGestureLayer.setOnTouchListener((v, event) -> handlePlayerSurfaceTouch(event));
        }
        if (timeshiftLiveButton != null) {
            timeshiftLiveButton.setOnClickListener(v -> {
                showTouchControlsTemporarily();
                if (playerController == null || !playerController.resumeTimeshiftLive()) {
                    showStatus(getString(R.string.timeshift_status_unavailable));
                }
                updateTimeshiftBar();
            });
        }
        if (timeshiftSeekBar != null) {
            timeshiftSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser || playerController == null) {
                        return;
                    }
                    PlayerController.PlaybackSeekState state = playerController.getPlaybackSeekState();
                    if (state == null || timeshiftStatusText == null) {
                        return;
                    }
                    long range = Math.max(1L, state.endMs - state.startMs);
                    long target = state.startMs + Math.round((progress / 1000f) * range);
                    timeshiftStatusText.setText(formatPlaybackPreviewLabel(state, target));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    timeshiftSeekUserDragging = true;
                    showTouchControlsTemporarily();
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (playerController != null) {
                        PlayerController.PlaybackSeekState state = playerController.getPlaybackSeekState();
                        if (state != null) {
                            long range = Math.max(1L, state.endMs - state.startMs);
                            long target = state.startMs + Math.round((seekBar.getProgress() / 1000f) * range);
                            playerController.seekTimeshiftTo(target);
                        }
                    }
                    timeshiftSeekUserDragging = false;
                    updateTimeshiftBar();
                    scheduleTouchControlsAutoHide();
                }
            });
        }
    }

    private void updateTimeshiftBar() {
        if (timeshiftBarContainer == null || timeshiftSeekBar == null || timeshiftStatusText == null || playerController == null) {
            updatePlaybackStateBadge(null);
            return;
        }
        updateVodTouchControlsState();
        if (timeshiftLiveButton != null) {
            timeshiftLiveButton.setVisibility(View.GONE);
        }
        boolean showForTouch = touchDeviceMode
                && touchControlsBar != null
                && touchControlsBar.getVisibility() == View.VISIBLE;
        boolean showForTv = !touchDeviceMode && touchControlsController != null && touchControlsController.isTvTimeshiftHudVisible();
        if ((!showForTouch && !showForTv) || isOverlayVisible() || isRecordingsPanelVisible() || isMultiViewVisible()) {
            timeshiftBarContainer.setVisibility(View.GONE);
            updatePlaybackStateBadge(playerController.getTimeshiftState());
            return;
        }
        PlayerController.PlaybackSeekState state = playerController.getPlaybackSeekState();
        if (state == null) {
            timeshiftBarContainer.setVisibility(View.GONE);
            updatePlaybackStateBadge(null);
            return;
        }
        timeshiftBarContainer.setVisibility(View.VISIBLE);
        if (timeshiftLiveButton != null) {
            timeshiftLiveButton.setVisibility(state.liveCapable ? View.VISIBLE : View.GONE);
        }
        if (!timeshiftSeekUserDragging) {
            long range = Math.max(1L, state.endMs - state.startMs);
            int progress = (int) Math.max(0L, Math.min(1000L, Math.round(((state.currentMs - state.startMs) * 1000f) / range)));
            timeshiftSeekBar.setProgress(progress);
            timeshiftStatusText.setText(buildPlaybackSeekLabel(state));
        }
        updatePlaybackStateBadge(playerController.getTimeshiftState());
    }

    private String buildPlaybackSeekLabel(PlayerController.PlaybackSeekState state) {
        if (state == null) {
            return "";
        }
        ChannelItem current = getCurrentPlaybackChannelItem();
        if (current != null && current.isVod && !state.liveCapable) {
            return getString(R.string.vod_playback_seek_label, displayName(current), formatDurationLabel(state.currentMs), formatDurationLabel(state.endMs));
        }
        return state.label;
    }

    private void updateVodTouchControlsState() {
        ChannelItem current = getCurrentPlaybackChannelItem();
        boolean vod = current != null && current.isVod;
        if (touchVodLibraryButton != null) {
            touchVodLibraryButton.setVisibility(vod ? View.VISIBLE : View.GONE);
        }
        if (touchGuideButton != null) {
            touchGuideButton.setText(vod ? R.string.touch_button_vod_library : R.string.touch_button_guide);
        }
        if (touchPreviousButton != null) {
            touchPreviousButton.setText(vod ? R.string.touch_button_vod_detail : R.string.touch_button_previous);
        }
        if (touchInfoButton != null) {
            touchInfoButton.setText(vod ? R.string.touch_button_vod_detail : R.string.touch_button_info);
        }
    }

    private String formatPlaybackPreviewLabel(PlayerController.PlaybackSeekState state, long targetMs) {
        if (state == null) {
            return getString(R.string.timeshift_status_unavailable);
        }
        ChannelItem current = getCurrentPlaybackChannelItem();
        if (current != null && current.isVod && !state.liveCapable) {
            return getString(R.string.vod_playback_seek_label, displayName(current), formatDurationLabel(targetMs), formatDurationLabel(state.endMs));
        }
        if (state.liveCapable) {
            long offsetMs = Math.max(0L, state.endMs - targetMs);
            if (offsetMs < LIVE_BADGE_THRESHOLD_MS) {
                return getString(R.string.timeshift_status_live);
            }
            long totalSeconds = Math.max(0L, Math.round(offsetMs / 1000f));
            long mins = totalSeconds / 60L;
            long secs = totalSeconds % 60L;
            return getString(R.string.timeshift_status_delayed, mins, secs);
        }
        return formatDurationLabel(targetMs) + " / " + formatDurationLabel(state.endMs);
    }

    private String formatDurationLabel(long valueMs) {
        long totalSeconds = Math.max(0L, Math.round(valueMs / 1000f));
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private String formatDurationShort(long valueMs) {
        return formatDurationLabel(valueMs);
    }

    private void showTouchControlsTemporarily() {
        if (touchControlsController != null) {
            touchControlsController.showTouchControlsTemporarily();
        }
    }

    private void showTimeshiftHudTemporarily() {
        if (touchControlsController != null) {
            touchControlsController.showTimeshiftHudTemporarily();
        }
    }

    private boolean isTvTimeshiftHudActive() {
        return touchControlsController != null && touchControlsController.isTvTimeshiftHudActive();
    }

    private void hideTvTimeshiftHud() {
        if (touchControlsController != null) {
            touchControlsController.hideTvTimeshiftHud();
        }
    }

    private void scheduleTouchControlsAutoHide() {
        if (touchControlsController != null) {
            touchControlsController.scheduleTouchControlsAutoHide();
        }
    }

    private void scheduleTvTimeshiftHudAutoHide() {
        if (touchControlsController != null) {
            touchControlsController.scheduleTvTimeshiftHudAutoHide();
        }
    }

    private boolean handlePlayerSurfaceTouch(MotionEvent event) {
        if (!touchDeviceMode || event == null) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (isMultiViewVisible()) {
                    closeMultiView();
                    touchGestureDownX = Float.NaN;
                    touchGestureDownY = Float.NaN;
                    touchGestureLastY = Float.NaN;
                    touchGestureVerticalHandled = false;
                    return true;
                }
                if (isOverlayVisible()) {
                    hideOverlay();
                    touchGestureDownX = Float.NaN;
                    touchGestureDownY = Float.NaN;
                    touchGestureLastY = Float.NaN;
                    touchGestureVerticalHandled = false;
                    return true;
                }
                if (isRecordingsPanelVisible()) {
                    hideRecordingsPanel();
                    showTouchControlsTemporarily();
                    touchGestureDownX = Float.NaN;
                    touchGestureDownY = Float.NaN;
                    touchGestureLastY = Float.NaN;
                    touchGestureVerticalHandled = false;
                    return true;
                }
                if (quickSearchOverlay != null && quickSearchOverlay.getVisibility() == View.VISIBLE) {
                    touchGestureDownX = Float.NaN;
                    touchGestureDownY = Float.NaN;
                    touchGestureLastY = Float.NaN;
                    touchGestureVerticalHandled = false;
                    return false;
                }
                touchGestureDownX = event.getX();
                touchGestureDownY = event.getY();
                touchGestureLastY = event.getY();
                touchGestureVerticalHandled = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Float.isNaN(touchGestureDownX) || Float.isNaN(touchGestureDownY) || Float.isNaN(touchGestureLastY)) {
                    return true;
                }
                float totalDeltaX = event.getX() - touchGestureDownX;
                float totalDeltaY = event.getY() - touchGestureDownY;
                float stepDeltaY = event.getY() - touchGestureLastY;
                touchGestureLastY = event.getY();
                float moveMinSwipeDistancePx = dpToPx(28f);
                float moveMaxOffAxisPx = dpToPx(96f);
                if (Math.abs(totalDeltaY) >= moveMinSwipeDistancePx && Math.abs(totalDeltaX) <= moveMaxOffAxisPx && Math.abs(totalDeltaY) > Math.abs(totalDeltaX)) {
                    handleVerticalGesture(touchGestureDownX, stepDeltaY, getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
                    touchGestureVerticalHandled = true;
                    return true;
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                touchGestureDownX = Float.NaN;
                touchGestureDownY = Float.NaN;
                touchGestureLastY = Float.NaN;
                touchGestureVerticalHandled = false;
                return true;
            case MotionEvent.ACTION_UP:
                if (Float.isNaN(touchGestureDownX) || Float.isNaN(touchGestureDownY)) {
                    return true;
                }
                float deltaX = event.getX() - touchGestureDownX;
                float deltaY = event.getY() - touchGestureDownY;
                boolean verticalHandled = touchGestureVerticalHandled;
                touchGestureDownX = Float.NaN;
                touchGestureDownY = Float.NaN;
                touchGestureLastY = Float.NaN;
                touchGestureVerticalHandled = false;

                float minSwipeDistancePx = dpToPx(72f);
                float maxOffAxisPx = dpToPx(56f);
                float tapSlopPx = dpToPx(18f);
                if (Math.abs(deltaX) <= tapSlopPx && Math.abs(deltaY) <= tapSlopPx) {
                    showTouchControlsTemporarily();
                    return true;
                }
                if (Math.abs(deltaX) >= minSwipeDistancePx && Math.abs(deltaY) <= maxOffAxisPx && Math.abs(deltaX) > Math.abs(deltaY)) {
                    tuneRelative(deltaX > 0 ? 1 : -1);
                    return true;
                }
                if (!verticalHandled && Math.abs(deltaY) >= minSwipeDistancePx && Math.abs(deltaX) <= maxOffAxisPx && Math.abs(deltaY) > Math.abs(deltaX)) {
                    handleVerticalGesture(event.getX(), deltaY, getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
                    return true;
                }
                return true;
            default:
                return true;
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private void handleVerticalGesture(float startX, float deltaY, int widthPx, int heightPx) {
        if (Float.isNaN(startX) || widthPx <= 0 || heightPx <= 0) {
            return;
        }
        float gestureSpanPx = Math.max(dpToPx(140f), heightPx * 0.35f);
        float normalized = Math.max(-1f, Math.min(1f, (-deltaY) / gestureSpanPx));
        if (Math.abs(normalized) < 0.02f) {
            return;
        }
        if (startX < widthPx * 0.5f) {
            adjustTabletBrightness(normalized);
            return;
        }
        if (startX >= widthPx * 0.5f) {
            adjustTabletVolume(normalized);
        }
    }

    private void initializeTabletBrightness() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        if (params != null && params.screenBrightness >= 0f) {
            tabletBrightnessLevel = params.screenBrightness;
        } else {
            tabletBrightnessLevel = 0.5f;
        }
    }

    private void adjustTabletBrightness(float deltaNormalized) {
        tabletBrightnessLevel = Math.max(0.1f, Math.min(1f, tabletBrightnessLevel + (deltaNormalized * 0.9f)));
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = tabletBrightnessLevel;
        getWindow().setAttributes(params);
        showStatus(getString(R.string.status_brightness_level, Math.round(tabletBrightnessLevel * 100f)));
        Log.d(TAG, "tablet brightness gesture level=" + tabletBrightnessLevel);
    }

    private void adjustTabletVolume(float deltaNormalized) {
        if (audioManager == null) {
            return;
        }
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (maxVolume <= 0) {
            return;
        }
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int deltaSteps = Math.max(1, Math.round(Math.abs(deltaNormalized) * maxVolume));
        int updated = currentVolume + (deltaNormalized >= 0f ? deltaSteps : -deltaSteps);
        updated = Math.max(0, Math.min(maxVolume, updated));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, updated, 0);
        showStatus(getString(R.string.status_volume_level, Math.round((updated * 100f) / (float) maxVolume)));
        Log.d(TAG, "tablet volume gesture level=" + updated + "/" + maxVolume);
    }

    private void toggleTabletOrientationLock() {
        tabletOrientationLocked = !tabletOrientationLocked;
        if (prefs != null) {
            prefs.edit().putBoolean(PREF_TABLET_ORIENTATION_LOCK, tabletOrientationLocked).apply();
        }
        applyTabletOrientationMode();
        updateTouchRotateButtonLabel();
        showStatus(getString(tabletOrientationLocked ? R.string.status_orientation_locked : R.string.status_orientation_unlocked));
    }

    private void applyTabletOrientationMode() {
        if (!touchDeviceMode) {
            return;
        }
        if (tabletOrientationLocked) {
            setRequestedOrientation(resolveCurrentLandscapeOrientation());
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    private int resolveCurrentLandscapeOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        if (rotation == Surface.ROTATION_270) {
            return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        }
        return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    }

    private void updateTouchRotateButtonLabel() {
        if (touchRotateButton == null) {
            return;
        }
        touchRotateButton.setText(getString(tabletOrientationLocked ? R.string.touch_button_rotate_locked : R.string.touch_button_rotate_free));
    }

    private void updatePlaybackStateBadge(PlayerController.TimeshiftState state) {
        if (liveStateBadgeText == null || !touchDeviceMode) {
            return;
        }
        if (state == null) {
            liveStateBadgeText.setText(getString(R.string.playback_state_live));
            liveStateBadgeText.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xCC1F7A3C));
            liveStateBadgeText.setVisibility(View.VISIBLE);
            return;
        }
        long offsetMs = Math.max(0L, state.endMs - state.currentMs);
        if (offsetMs < LIVE_BADGE_THRESHOLD_MS) {
            liveStateBadgeText.setText(getString(R.string.playback_state_live));
            liveStateBadgeText.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xCC1F7A3C));
        } else {
            long totalSeconds = Math.round(offsetMs / 1000f);
            long mins = totalSeconds / 60L;
            long secs = totalSeconds % 60L;
            liveStateBadgeText.setText(getString(R.string.playback_state_timeshift, mins, secs));
            liveStateBadgeText.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xCC9A5A00));
        }
        liveStateBadgeText.setVisibility(View.VISIBLE);
    }

    private void loadChannels() {
        showStatus(getString(R.string.status_loading_channels));
        long startMs = System.currentTimeMillis();
        ioExecutor.execute(() -> {
            try {
                CatalogLoadResult result = catalogRepository.fetchCatalogChannels();
                long durationMs = System.currentTimeMillis() - startMs;
                uiHandler.post(() -> {
                    lastCatalogLoadDurationMs = durationMs;
                    applyLoadedChannels(result);
                    refreshStandaloneCatalogInBackgroundIfPossible();
                });
            } catch (Exception catalogErr) {
                if (BuildConfig.STANDALONE_MODE) {
                    Log.w(TAG, "local catalog load failed in standalone mode", catalogErr);
                    try {
                        lastOfflineCatalogRefreshAttemptMs = System.currentTimeMillis();
                        CatalogLoadResult refreshed = catalogRepository.refreshSnapshotFromConfiguredUrl(BuildConfig.CATALOG_SNAPSHOT_URL);
                        long durationMs = System.currentTimeMillis() - startMs;
                        uiHandler.post(() -> {
                            lastCatalogLoadDurationMs = durationMs;
                            lastOfflineCatalogRefreshSuccessMs = System.currentTimeMillis();
                            lastOfflineCatalogRefreshError = "";
                            showStatus(getString(R.string.catalog_snapshot_refresh_ready));
                            applyLoadedChannels(refreshed);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "standalone catalog load failed", e);
                        try {
                            CatalogLoadResult fallback = catalogRepository.fetchLastKnownGoodSnapshotCatalog();
                            long durationMs = System.currentTimeMillis() - startMs;
                            uiHandler.post(() -> {
                                lastCatalogLoadDurationMs = durationMs;
                                lastOfflineCatalogRefreshError = e.getMessage();
                                showStatus(getString(R.string.offline_catalog_status_using_last_good));
                                applyLoadedChannels(fallback);
                            });
                        } catch (Exception fallbackErr) {
                            uiHandler.post(() -> {
                                lastOfflineCatalogRefreshError = e.getMessage();
                                if (!showOfflineCatalogRecoveryDialogIfNeeded(e)) {
                                    showError(getString(R.string.error_load_channels, e.getMessage()));
                                    showCatalogRecoveryDialog(e.getMessage());
                                }
                            });
                        }
                    }
                    return;
                }
                Log.w(TAG, "catalog load failed, fallback to /api/channels", catalogErr);
                try {
                    CatalogLoadResult fallback = catalogRepository.fetchActiveChannels();
                    long durationMs = System.currentTimeMillis() - startMs;
                    uiHandler.post(() -> {
                        lastCatalogLoadDurationMs = durationMs;
                        applyLoadedChannels(fallback);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "load channels failed", e);
                    uiHandler.post(() -> {
                        showError(getString(R.string.error_load_channels, e.getMessage()));
                        showCatalogRecoveryDialog(e.getMessage());
                    });
                }
            }
        });
    }

    private void showCatalogRecoveryDialog(String reason) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.startup_recovery_title)
                .setMessage(getString(R.string.startup_recovery_message, fallbackUnknown(reason), BuildConfig.VERSION_NAME))
                .setPositiveButton(R.string.startup_recovery_retry, (dialog, which) -> loadChannels())
                .setNeutralButton(R.string.tools_menu_install_status, (dialog, which) -> showInstallStatusDialog())
                .setNegativeButton(R.string.dialog_close, null)
                .show();
    }

    private void applyLoadedChannels(CatalogLoadResult result) {
        long startMs = System.currentTimeMillis();
        syncOverlayCoordinator();
        epgFullCatalogLoaded = false;
        epgFullCatalogLoadRequested = false;
        epgFullLoadScheduledForChannelId = "";
        channelOverlayCoordinator.applyLoadedChannels(result, lastChannelId);
        syncOverlayStateFromCoordinator();
        persistNavigationState();
        channelAdapter.notifyDataSetChanged();
        updateFilterText();
        updateOverlaySearchState();

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
        lastApplyChannelsDurationMs = System.currentTimeMillis() - startMs;
        int visibleCount = channels.size();
        int totalCount = allChannels.size();
        showStatus(visibleCount == totalCount
                ? getString(R.string.status_channels_ready, visibleCount, lastCatalogLoadDurationMs)
                : getString(R.string.status_channels_ready_filtered, visibleCount, totalCount, lastCatalogLoadDurationMs));
        prefetchCurrentChannelLogos();
        if (!BuildConfig.STANDALONE_MODE) {
            uiHandler.postDelayed(() -> loadEpgNow(false), 450L);
        }
        uiHandler.postDelayed(this::maybeShowStartupHub, 700L);
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
        playChannelItem(channels.get(index), autoPlay);
    }

    private void playChannelItem(ChannelItem ch, boolean autoPlay) {
        if (ch == null) {
            return;
        }
        rememberCurrentVodPosition();
        if (ch.isVod) {
            playVodItem(ch, autoPlay);
            return;
        }
        currentPlaybackVodId = null;
        playChannelItemInternal(ch, autoPlay, 0L);
    }

    private void playVodItem(ChannelItem ch, boolean autoPlay) {
        if (ch == null) {
            return;
        }
        long resumePositionMs = getVodResumePosition(ch.id);
        Runnable startFromBeginning = () -> {
            clearVodResumePosition(ch.id);
            playChannelItemInternal(ch, autoPlay, 0L);
        };
        Runnable resumeFromSaved = () -> playChannelItemInternal(ch, autoPlay, resumePositionMs);
        if (resumePositionMs > 30_000L) {
            new AlertDialog.Builder(this)
                    .setTitle(displayName(ch))
                    .setMessage(getString(R.string.vod_continue_prompt, formatDurationShort(resumePositionMs)))
                    .setPositiveButton(R.string.vod_action_continue, (dialog, which) -> resumeFromSaved.run())
                    .setNegativeButton(R.string.vod_action_start_over, (dialog, which) -> startFromBeginning.run())
                    .show();
            return;
        }
        startFromBeginning.run();
    }

    private void playChannelItemInternal(ChannelItem ch, boolean autoPlay, long resumePositionMs) {
        saveLastChannelId(ch.id);
        if (recentChannelsStore != null) {
            recentChannelsStore.add(ch.id, displayName(ch));
        }
        if (ch.isVod) {
            currentPlaybackVodId = ch.id;
            lastVodId = ch.id;
            if (prefs != null) {
                prefs.edit().putString(PREF_LAST_VOD_ID, ch.id).apply();
            }
            showStatus(getString(R.string.vod_status_preparing, displayName(ch)));
        }
        playerController.resetFallbackState();
        updateTimeshiftBar();
        PlayerController.StreamInfo cachedStreamInfo = streamInfoByChannelId.get(ch.id);
        PlayerController.PlaybackRequest playbackRequest = toPlaybackRequest(ch);
        boolean resolveBeforePlayback = BuildConfig.STANDALONE_MODE
                && playbackRequest != null
                && !playbackRequest.directPlayback
                && !ch.isVod;
        if (resolveBeforePlayback) {
            showStatus(getString(R.string.status_buffering));
            playerController.playChannelAfterResolvingStreamInfo(playbackRequest, autoPlay, streamInfoByChannelId, resumePositionMs);
        } else {
            playerController.playChannel(playbackRequest, autoPlay, cachedStreamInfo, resumePositionMs);
        }
        if (!resolveBeforePlayback && playbackRequest != null && !playbackRequest.directPlayback) {
            playerController.resolveStreamInfoAndReplayIfNeeded(playbackRequest, autoPlay, streamInfoByChannelId, resumePositionMs);
        }
        scheduleLearnCurrentPlaybackRoute(ch.id, playbackRequest == null ? PlaybackModeStore.MODE_AUTO : playbackRequest.playbackMode);

        hideError();
        if (ch.isVod) {
            showStatus(getString(R.string.vod_status_preparing, displayName(ch)));
        } else {
            showStatus(displayName(ch));
        }
        updateOverlayPanel();
        showZapBanner(ch);
    }

    private String displayName(ChannelItem channelItem) {
        if (channelItem == null) {
            return "";
        }
        return channelProfileStore == null ? channelItem.name : channelProfileStore.getDisplayName(channelItem.id, channelItem.name);
    }

    private String getCurrentChannelName() {
        ChannelItem channel = null;
        if (currentIndex >= 0 && currentIndex < channels.size()) {
            channel = channels.get(currentIndex);
        }
        if (channel == null && lastChannelId != null && !lastChannelId.trim().isEmpty()) {
            channel = findChannelItemById(lastChannelId);
        }
        return cleanText(displayName(channel));
    }

    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }

    private String profileTag(ChannelItem channelItem) {
        if (channelItem == null || channelProfileStore == null) {
            return "";
        }
        return channelProfileStore.getTag(channelItem.id);
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String safeSearchText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private static boolean matchesSearch(String haystack, String query) {
        String normalizedHaystack = safeSearchText(haystack);
        String normalizedQuery = safeSearchText(query);
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        String[] tokens = normalizedQuery.split("\\s+");
        for (String token : tokens) {
            if (token == null || token.isEmpty()) {
                continue;
            }
            if (!normalizedHaystack.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private void scheduleFullEpgLoadAfterFirstFrame(String channelId) {
        if (!BuildConfig.STANDALONE_MODE) {
            return;
        }
        if (epgFullCatalogLoaded || epgFullCatalogLoadRequested) {
            return;
        }
        String cleanChannelId = channelId == null ? "" : channelId.trim();
        if (cleanChannelId.isEmpty() || cleanChannelId.equals(epgFullLoadScheduledForChannelId)) {
            return;
        }
        epgFullLoadScheduledForChannelId = cleanChannelId;
        epgFullCatalogLoadRequested = true;
        uiHandler.postDelayed(() -> loadEpgNow(true), 1500L);
    }

    private void loadEpgNow(boolean fullCatalog) {
        if (epgLoadInFlight) {
            Log.d(TAG, "skip loadEpgNow because a load is already in progress");
            return;
        }
        final List<ChannelItem> allChannelsSnapshot = new ArrayList<>(allChannels);
        final List<ChannelItem> visibleChannelsSnapshot = new ArrayList<>(channels);
        final List<ChannelItem> epgChannelsSnapshot = BuildConfig.STANDALONE_MODE
                ? (fullCatalog ? allChannelsSnapshot : visibleChannelsSnapshot)
                : allChannelsSnapshot;
        epgLoadInFlight = true;
        long startMs = System.currentTimeMillis();
        epgExecutor.execute(() -> {
            try {
                Map<String, String> updates;
                Map<String, EpgRepository.EpgProgramPair> pairs = new HashMap<>();
                if (BuildConfig.STANDALONE_MODE) {
                    pairs = epgRepository.fetchProgramPairsForChannels(epgChannelsSnapshot);
                    updates = buildNowProgramUpdatesFromPairs(pairs);
                } else {
                    updates = epgRepository.fetchNowPrograms();
                }
                Map<String, EpgRepository.EpgProgramPair> finalPairs = pairs;

                uiHandler.post(() -> {
                    epgLoadInFlight = false;
                    lastEpgNowLoadDurationMs = System.currentTimeMillis() - startMs;
                    if (fullCatalog) {
                        epgFullCatalogLoaded = true;
                    }
                    epgFullCatalogLoadRequested = false;
                    epgNowByChannelId.clear();
                    epgNowByChannelId.putAll(updates);
                    epgProgramPairByChannelId.clear();
                    epgProgramPairByChannelId.putAll(finalPairs);
                    int filled = applyProgramPairUpdates(allChannels, epgNowByChannelId, epgProgramPairByChannelId);
                    applyProgramPairUpdates(channels, epgNowByChannelId, epgProgramPairByChannelId);
                    Log.i(TAG, "EPG now loaded updates=" + updates.size()
                            + " filledChannels=" + filled
                            + " totalChannels=" + allChannels.size()
                            + " snapshotChannels=" + epgChannelsSnapshot.size()
                            + " visibleSnapshotChannels=" + visibleChannelsSnapshot.size()
                            + " fullCatalog=" + fullCatalog
                            + " standalone=" + BuildConfig.STANDALONE_MODE
                            + " durationMs=" + lastEpgNowLoadDurationMs);
                    channelAdapter.notifyDataSetChanged();
                    updateOverlayPanel();
                    ChannelItem currentChannel = getCurrentPlaybackChannelItem();
                    if (currentChannel != null && zapBanner != null && zapBanner.getVisibility() == View.VISIBLE) {
                        showZapBanner(currentChannel);
                    }
                });
            } catch (Exception e) {
                epgLoadInFlight = false;
                epgFullCatalogLoadRequested = false;
                lastEpgNowLoadDurationMs = System.currentTimeMillis() - startMs;
                Log.w(TAG, "load epg now failed", e);
            }
        });
    }

    private Map<String, String> buildNowProgramUpdatesFromPairs(Map<String, EpgRepository.EpgProgramPair> pairs) {
        Map<String, String> updates = new HashMap<>();
        if (pairs == null || pairs.isEmpty()) {
            return updates;
        }
        for (Map.Entry<String, EpgRepository.EpgProgramPair> entry : pairs.entrySet()) {
            EpgRepository.EpgProgram current = entry.getValue() == null ? null : entry.getValue().current;
            if (current == null || current.title == null || current.title.trim().isEmpty()) {
                continue;
            }
            String title = current.title.trim();
            if (current.progress >= 0) {
                title = title + " (" + current.progress + "%)";
            }
            updates.put(entry.getKey(), title);
        }
        return updates;
    }

    private int applyProgramPairUpdates(List<ChannelItem> items, Map<String, String> updates, Map<String, EpgRepository.EpgProgramPair> pairs) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int filled = 0;
        for (ChannelItem item : items) {
            if (item == null) {
                continue;
            }
            item.nowProgram = updates == null ? "" : updates.getOrDefault(item.id, "");
            EpgRepository.EpgProgramPair pair = pairs == null ? null : pairs.get(item.id);
            EpgRepository.EpgProgram next = pair == null ? null : pair.next;
            item.nextProgram = next == null || next.title == null ? "" : next.title.trim();
            if (item.nowProgram != null && !item.nowProgram.trim().isEmpty()) {
                filled++;
            }
        }
        return filled;
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
                List<EpgRepository.EpgProgram> items = epgRepository.fetchChannelPrograms(ch, 8);
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
        int anchorIndex = findChannelIndexById(lastTimelineAnchorChannelId);
        if (anchorIndex < 0) {
            anchorIndex = selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size()
                    ? selectedOverlayIndex
                    : (currentIndex >= 0 && currentIndex < channels.size() ? currentIndex : 0);
        }
        long windowStartMs = lastTimelineWindowStartMs > 0L ? lastTimelineWindowStartMs : System.currentTimeMillis();
        openTimelineGuide(anchorIndex, windowStartMs);
    }

    private void openTimelineGuideNow() {
        if (channels.isEmpty()) {
            return;
        }
        int anchorIndex = selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size()
                ? selectedOverlayIndex
                : (currentIndex >= 0 && currentIndex < channels.size() ? currentIndex : 0);
        lastTimelineFocusedCenterMinute = -1;
        openTimelineGuide(anchorIndex, System.currentTimeMillis());
    }

    private void openTimelineGuideForChannel(ChannelItem channel) {
        int anchorIndex = channel == null ? -1 : findChannelIndexById(channel.id);
        if (anchorIndex < 0) {
            anchorIndex = currentIndex;
        }
        openTimelineGuide(anchorIndex, System.currentTimeMillis());
    }

    private void openTimelineGuideNextForAnchor() {
        if (channels.isEmpty()) {
            return;
        }
        String anchorChannelId = activeTimelineAnchorChannelId != null ? activeTimelineAnchorChannelId : lastTimelineAnchorChannelId;
        int anchorIndex = findChannelIndexById(anchorChannelId);
        if (anchorIndex < 0) {
            anchorIndex = selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size()
                    ? selectedOverlayIndex
                    : (currentIndex >= 0 && currentIndex < channels.size() ? currentIndex : 0);
        }
        long referenceMs = System.currentTimeMillis();
        if (activeTimelineWindowStartMs > 0L && activeTimelineFocusedCenterMinute >= 0) {
            referenceMs = activeTimelineWindowStartMs + (activeTimelineFocusedCenterMinute * 60_000L);
        } else if (lastTimelineWindowStartMs > 0L && lastTimelineFocusedCenterMinute >= 0) {
            referenceMs = lastTimelineWindowStartMs + (lastTimelineFocusedCenterMinute * 60_000L);
        }
        EpgRepository.EpgProgram nextProgram = null;
        for (TimelineChannelPrograms row : activeTimelineRows) {
            if (row == null || row.channel == null || row.programs == null) {
                continue;
            }
            if (anchorChannelId == null || !row.channel.id.equals(anchorChannelId)) {
                continue;
            }
            for (EpgRepository.EpgProgram program : row.programs) {
                long startMs = parseIsoMillis(program.startTime);
                long endMs = parseIsoMillis(program.endTime);
                if (endMs <= startMs) {
                    continue;
                }
                if (startMs > referenceMs) {
                    if (nextProgram == null || startMs < parseIsoMillis(nextProgram.startTime)) {
                        nextProgram = program;
                    }
                }
            }
            break;
        }
        long targetWindowStartMs;
        if (nextProgram != null) {
            long nextStartMs = parseIsoMillis(nextProgram.startTime);
            targetWindowStartMs = Math.max(0L, (nextStartMs / TIMELINE_SHIFT_MS) * TIMELINE_SHIFT_MS);
            lastTimelineFocusedCenterMinute = (int) Math.max(0L, (nextStartMs - targetWindowStartMs) / 60000L);
        } else {
            targetWindowStartMs = (activeTimelineWindowStartMs > 0L ? activeTimelineWindowStartMs : System.currentTimeMillis()) + TIMELINE_SHIFT_MS;
            lastTimelineFocusedCenterMinute = -1;
        }
        if (activeTimelineDialog != null && activeTimelineDialog.isShowing() && !activeTimelineRows.isEmpty()) {
            android.app.Dialog dialog = activeTimelineDialog;
            List<TimelineChannelPrograms> rows = new ArrayList<>(activeTimelineRows);
            List<RecordingsRepository.RecordingItem> scheduled = new ArrayList<>(activeTimelineScheduledItems);
            refreshingTimelineDialog = true;
            dialog.dismiss();
            refreshingTimelineDialog = false;
            showTimelineGuideDialog(rows, targetWindowStartMs, channels.get(anchorIndex).id, scheduled);
            return;
        }
        openTimelineGuide(anchorIndex, targetWindowStartMs);
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
                    List<EpgRepository.EpgProgram> programs = epgRepository.fetchChannelPrograms(channel, 12);
                    rows.add(new TimelineChannelPrograms(channel, programs));
                }
                List<RecordingsRepository.RecordingItem> scheduledItems = new ArrayList<>();
                if (!isOfflineRecordingsDisabled()) {
                    try {
                        RecordingsRepository.RecordingsResult scheduledResult = recordingsRepository.fetchScheduledRecordings();
                        if (scheduledResult != null && scheduledResult.items != null) {
                            scheduledItems.addAll(scheduledResult.items);
                        }
                    } catch (Exception scheduledErr) {
                        Log.w(TAG, "timeline scheduled recordings fetch failed", scheduledErr);
                    }
                }
                uiHandler.post(() -> {
                    if (rows.isEmpty()) {
                        showStatus(getString(R.string.status_no_epg_for_channel));
                        return;
                    }
                    showTimelineGuideDialog(rows, selectedWindowStartMs, channels.get(selectedIndex).id, scheduledItems);
                });
            } catch (Exception e) {
                Log.w(TAG, "timeline guide failed", e);
                uiHandler.post(() -> showStatus(getString(R.string.status_failed_load_guide)));
            }
        });
    }


    private void openVisualEpgAroundSelection() {
        if (channels.isEmpty()) {
            return;
        }
        int visualAnchorIndex = findChannelIndexById(lastVisualEpgChannelId);
        final ChannelItem anchorChannel = (visualAnchorIndex >= 0 && visualAnchorIndex < channels.size())
                ? channels.get(visualAnchorIndex)
                : ((selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size())
                ? channels.get(selectedOverlayIndex)
                : ((currentIndex >= 0 && currentIndex < channels.size()) ? channels.get(currentIndex) : channels.get(0)));
        final String anchorChannelId = anchorChannel.id;
        final String platformLabel = (anchorChannel.platformName == null || anchorChannel.platformName.trim().isEmpty())
                ? getString(R.string.visual_epg_platform_visible)
                : anchorChannel.platformName.trim();
        showStatus(getString(R.string.status_loading_visual_epg));
        ioExecutor.execute(() -> {
            try {
                List<ChannelItem> platformChannels = new ArrayList<>();
                java.util.Map<String, ChannelItem> byId = new java.util.HashMap<>();
                java.util.Map<String, ChannelItem> byName = new java.util.HashMap<>();
                for (ChannelItem channel : channels) {
                    if (channel == null || channel.isVod || !matchesVisualEpgPlatform(anchorChannel, channel)) {
                        continue;
                    }
                    platformChannels.add(channel);
                    byId.put(channel.id, channel);
                    byName.put(String.valueOf(channel.name).trim().toLowerCase(java.util.Locale.ROOT), channel);
                }

                List<EpgRepository.EpgProgram> nowPrograms = epgRepository.fetchNowProgramsDetailed();
                List<EpgRepository.EpgProgram> moviePrograms = epgRepository.fetchCategoryPrograms("movies", 24);
                List<EpgRepository.EpgProgram> seriesPrograms = epgRepository.fetchCategoryPrograms("series", 24);
                List<EpgRepository.EpgProgram> sportsPrograms = epgRepository.fetchCategoryPrograms("sports", 24);

                List<VisualEpgSection> sections = new ArrayList<>();
                List<VisualEpgEntry> liveEntries = bindVisualEpgEntries(nowPrograms, byId, byName);
                List<VisualEpgEntry> movieEntries = bindVisualEpgEntries(moviePrograms, byId, byName);
                List<VisualEpgEntry> seriesEntries = bindVisualEpgEntries(seriesPrograms, byId, byName);
                List<VisualEpgEntry> sportsEntries = bindVisualEpgEntries(sportsPrograms, byId, byName);
                liveEntries.removeIf(entry -> containsVisualEpgProgram(sportsEntries, entry));
                sortVisualEpgEntries(liveEntries);
                sortVisualEpgEntries(movieEntries);
                sortVisualEpgEntries(seriesEntries);
                sortVisualEpgEntries(sportsEntries);
                if (!liveEntries.isEmpty()) sections.add(new VisualEpgSection(getString(R.string.visual_epg_section_live), liveEntries));
                if (!movieEntries.isEmpty()) sections.add(new VisualEpgSection(getString(R.string.visual_epg_section_movies), movieEntries));
                if (!seriesEntries.isEmpty()) sections.add(new VisualEpgSection(getString(R.string.visual_epg_section_series), seriesEntries));
                if (!sportsEntries.isEmpty()) sections.add(new VisualEpgSection(getString(R.string.visual_epg_section_sports), sportsEntries));

                List<RecordingsRepository.RecordingItem> scheduledItems = new ArrayList<>();
                if (!isOfflineRecordingsDisabled()) {
                    try {
                        RecordingsRepository.RecordingsResult scheduledResult = recordingsRepository.fetchScheduledRecordings();
                        if (scheduledResult != null && scheduledResult.items != null) {
                            scheduledItems.addAll(scheduledResult.items);
                        }
                    } catch (Exception scheduledErr) {
                        Log.w(TAG, "visual epg scheduled recordings fetch failed", scheduledErr);
                    }
                }
                uiHandler.post(() -> {
                    if (sections.isEmpty()) {
                        showStatus(getString(R.string.visual_epg_empty));
                        return;
                    }
                    showVisualEpgDialog(sections, anchorChannelId, platformLabel, scheduledItems);
                });
            } catch (Exception e) {
                Log.w(TAG, "visual epg failed", e);
                uiHandler.post(() -> showStatus(getString(R.string.status_failed_load_guide)));
            }
        });
    }

    private boolean matchesVisualEpgPlatform(ChannelItem anchorChannel, ChannelItem candidate) {
        if (anchorChannel == null || candidate == null) {
            return false;
        }
        if (anchorChannel.platformId > 0 && candidate.platformId > 0) {
            return anchorChannel.platformId == candidate.platformId;
        }
        String anchorPlatform = anchorChannel.platformName == null ? "" : anchorChannel.platformName.trim();
        String candidatePlatform = candidate.platformName == null ? "" : candidate.platformName.trim();
        if (!anchorPlatform.isEmpty() && !candidatePlatform.isEmpty()) {
            return anchorPlatform.equalsIgnoreCase(candidatePlatform);
        }
        return true;
    }

    private List<VisualEpgEntry> bindVisualEpgEntries(List<EpgRepository.EpgProgram> programs, java.util.Map<String, ChannelItem> byId, java.util.Map<String, ChannelItem> byName) {
        List<VisualEpgEntry> out = new ArrayList<>();
        if (programs == null || programs.isEmpty()) {
            return out;
        }
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (EpgRepository.EpgProgram program : programs) {
            if (program == null) continue;
            ChannelItem channel = null;
            if (program.channelId != null && !program.channelId.trim().isEmpty()) {
                channel = byId.get(program.channelId.trim());
            }
            if (channel == null && program.channelName != null) {
                channel = byName.get(program.channelName.trim().toLowerCase(java.util.Locale.ROOT));
            }
            if (channel == null) continue;
            String key = channel.id + "|" + String.valueOf(program.title).trim() + "|" + String.valueOf(program.startTime).trim();
            if (!seen.add(key)) continue;
            out.add(new VisualEpgEntry(channel, program));
        }
        return out;
    }

    private void sortVisualEpgEntries(List<VisualEpgEntry> entries) {
        if (entries == null) return;
        entries.sort((left, right) -> Long.compare(parseIsoMillis(left.program.startTime), parseIsoMillis(right.program.startTime)));
    }

    private boolean containsVisualEpgProgram(List<VisualEpgEntry> haystack, VisualEpgEntry needle) {
        if (haystack == null || needle == null || needle.channel == null || needle.program == null) {
            return false;
        }
        String needleKey = needle.channel.id + "|" + String.valueOf(needle.program.title).trim() + "|" + String.valueOf(needle.program.startTime).trim();
        for (VisualEpgEntry entry : haystack) {
            if (entry == null || entry.channel == null || entry.program == null) continue;
            String key = entry.channel.id + "|" + String.valueOf(entry.program.title).trim() + "|" + String.valueOf(entry.program.startTime).trim();
            if (needleKey.equals(key)) return true;
        }
        return false;
    }

    private List<VisualEpgSection> buildVisualEpgSections(List<VisualEpgEntry> entries) {
        List<VisualEpgSection> sections = new ArrayList<>();
        if (entries == null || entries.isEmpty()) {
            return sections;
        }

        List<VisualEpgEntry> live = new ArrayList<>();
        List<VisualEpgEntry> movies = new ArrayList<>();
        List<VisualEpgEntry> series = new ArrayList<>();
        List<VisualEpgEntry> sports = new ArrayList<>();
        for (VisualEpgEntry entry : entries) {
            if (looksSports(entry)) {
                sports.add(entry);
            } else if (looksMovie(entry)) {
                movies.add(entry);
            } else if (looksSeries(entry)) {
                series.add(entry);
            } else {
                live.add(entry);
            }
        }
        java.util.Comparator<VisualEpgEntry> byStartTime = java.util.Comparator.comparingLong(entry -> {
            if (entry == null || entry.program == null || entry.program.startTime == null) {
                return Long.MAX_VALUE;
            }
            try {
                return java.time.Instant.parse(entry.program.startTime).toEpochMilli();
            } catch (Exception ignored) {
                return Long.MAX_VALUE;
            }
        });
        live.sort(byStartTime);
        movies.sort(byStartTime);
        series.sort(byStartTime);
        sports.sort(byStartTime);

        if (!live.isEmpty()) {
            sections.add(new VisualEpgSection(getString(R.string.visual_epg_section_live), live));
        }
        if (!movies.isEmpty()) {
            sections.add(new VisualEpgSection(getString(R.string.visual_epg_section_movies), movies));
        }
        if (!series.isEmpty()) {
            sections.add(new VisualEpgSection(getString(R.string.visual_epg_section_series), series));
        }
        if (!sports.isEmpty()) {
            sections.add(new VisualEpgSection(getString(R.string.visual_epg_section_sports), sports));
        }
        return sections;
    }

    private boolean looksMovie(VisualEpgEntry entry) {
        String haystack = visualEpgText(entry);
        return haystack.contains("cine") || haystack.contains("pelicula") || haystack.contains("pelicula") || haystack.contains("film") || haystack.contains("accion") || haystack.contains("thriller");
    }

    private boolean looksSeries(VisualEpgEntry entry) {
        String haystack = visualEpgText(entry);
        return haystack.contains("serie") || haystack.contains("series") || haystack.contains("episodio") || haystack.contains("capitulo") || haystack.contains("temporada") || haystack.matches(".*t\\d+.*e\\d+.*");
    }

    private boolean looksSports(VisualEpgEntry entry) {
        String haystack = visualEpgText(entry);
        return haystack.contains("deporte") || haystack.contains("futbol") || haystack.contains("football") || haystack.contains("liga") || haystack.contains("champions") || haystack.contains("nba") || haystack.contains("tenis") || haystack.contains("formula 1") || haystack.contains("motogp") || haystack.contains("golf") || haystack.contains("boxing") || haystack.contains("ufc") || haystack.contains("baloncesto") || haystack.contains("arena sport") || haystack.contains("dazn") || haystack.contains("ziggo sport");
    }

    private String visualEpgText(VisualEpgEntry entry) {
        StringBuilder sb = new StringBuilder();
        if (entry.channel.name != null) {
            sb.append(entry.channel.name).append(' ');
        }
        if (entry.channel.group != null) {
            sb.append(entry.channel.group).append(' ');
        }
        if (entry.program.title != null) {
            sb.append(entry.program.title).append(' ');
        }
        if (entry.program.description != null) {
            sb.append(entry.program.description);
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private void showVisualEpgDialog(List<VisualEpgSection> sections, String anchorChannelId, String platformLabel, List<RecordingsRepository.RecordingItem> scheduledItems) {
        if (touchControlsBar != null) {
            touchControlsBar.setVisibility(View.GONE);
        }
        if (touchHomeHub != null) {
            touchHomeHub.setVisibility(View.GONE);
        }
        if (timeshiftBarContainer != null) {
            timeshiftBarContainer.setVisibility(View.GONE);
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_visual_epg_guide, null, false);
        TextView subtitleText = dialogView.findViewById(R.id.visualEpgSubtitleText);
        TextView refreshButton = dialogView.findViewById(R.id.visualEpgRefreshButton);
        TextView closeButton = dialogView.findViewById(R.id.visualEpgCloseButton);
        android.widget.ScrollView verticalScroll = dialogView.findViewById(R.id.visualEpgScroll);
        LinearLayout sectionsContainer = dialogView.findViewById(R.id.visualEpgSectionsContainer);
        ImageView posterImage = dialogView.findViewById(R.id.visualEpgPosterImage);
        TextView titleText = dialogView.findViewById(R.id.visualEpgDetailTitleText);
        TextView metaText = dialogView.findViewById(R.id.visualEpgMetaText);
        TextView descText = dialogView.findViewById(R.id.visualEpgDescText);
        final View[] initialFocus = new View[1];
        final List<List<View>> focusRows = new ArrayList<>();
        final Map<View, Integer> focusCenters = new HashMap<>();
        final Map<View, View> focusAnchors = new HashMap<>();

        int totalItems = 0;
        for (VisualEpgSection section : sections) {
            totalItems += section.entries.size();
        }
        activeProgramScheduledItems = scheduledItems == null ? new ArrayList<>() : new ArrayList<>(scheduledItems);
        subtitleText.setText(getString(R.string.visual_epg_subtitle, platformLabel, totalItems));
        posterImage.setVisibility(View.GONE);
        titleText.setText(getString(R.string.title_visual_epg));
        metaText.setText(getString(R.string.visual_epg_detail_hint));
        descText.setText(getString(R.string.timeline_program_desc_empty));
        sectionsContainer.removeAllViews();

        int cardWidth = dp(164);
        int cardHeight = dp(208);
        int posterHeight = dp(104);
        int cardGap = dp(10);

        for (VisualEpgSection section : sections) {
            if (section.entries == null || section.entries.isEmpty()) {
                continue;
            }
            final int rowIndex = focusRows.size();
            final List<View> rowFocusables = new ArrayList<>();

            TextView sectionTitle = new TextView(this);
            sectionTitle.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            sectionTitle.setText(section.title);
            sectionTitle.setTextColor(0xFFFFFFFF);
            sectionTitle.setTextSize(18f);
            sectionTitle.setTypeface(Typeface.DEFAULT_BOLD);
            sectionTitle.setPadding(dp(2), rowIndex == 0 ? dp(2) : dp(14), dp(2), dp(8));
            sectionsContainer.addView(sectionTitle);

            android.widget.HorizontalScrollView horizontalScroll = new android.widget.HorizontalScrollView(this);
            horizontalScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            horizontalScroll.setHorizontalScrollBarEnabled(false);
            horizontalScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            horizontalScroll.setFocusable(false);
            horizontalScroll.setFocusableInTouchMode(false);

            LinearLayout row = new LinearLayout(this);
            row.setFocusable(false);
            row.setFocusableInTouchMode(false);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            horizontalScroll.addView(row);
            sectionsContainer.addView(horizontalScroll);

            for (VisualEpgEntry entry : section.entries) {
                ChannelItem channel = entry.channel;
                EpgRepository.EpgProgram program = entry.program;
                boolean scheduled = isProgramScheduled(channel, program, scheduledItems);
                boolean live = program.progress >= 0;

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setFocusable(true);
                card.setFocusableInTouchMode(true);
                card.setClickable(true);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(cardWidth, cardHeight);
                cardParams.rightMargin = cardGap;
                card.setLayoutParams(cardParams);
                card.setPadding(dp(8), dp(8), dp(8), dp(8));
                card.setGravity(Gravity.TOP);

                android.widget.FrameLayout posterFrame = new android.widget.FrameLayout(this);
                posterFrame.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, posterHeight));
                posterFrame.setBackgroundColor(0xFF0E1820);

                ImageView posterView = new ImageView(this);
                posterView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                posterView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                String heroPoster = program.icon == null || program.icon.trim().isEmpty() ? channel.logoUrl : program.icon.trim();
                bindRecordingPoster(posterView, heroPoster);
                posterFrame.addView(posterView);

                TextView topBadge = new TextView(this);
                android.widget.FrameLayout.LayoutParams topBadgeParams = new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
                topBadgeParams.setMargins(dp(6), dp(6), dp(6), dp(6));
                topBadge.setLayoutParams(topBadgeParams);
                topBadge.setPadding(dp(7), dp(3), dp(7), dp(3));
                topBadge.setText(scheduled ? getString(R.string.timeline_program_scheduled_short) : (live ? getString(R.string.guide_program_now) : channel.name));
                topBadge.setTextColor(0xFFFFFFFF);
                topBadge.setTextSize(10f);
                topBadge.setTypeface(Typeface.DEFAULT_BOLD);
                android.graphics.drawable.GradientDrawable topBadgeBg = new android.graphics.drawable.GradientDrawable();
                topBadgeBg.setCornerRadius(dp(12));
                topBadgeBg.setColor(scheduled ? 0xCC8E5B16 : 0xCC214A72);
                topBadge.setBackground(topBadgeBg);
                posterFrame.addView(topBadge);
                card.addView(posterFrame);

                TextView programTitle = new TextView(this);
                LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                titleParams.topMargin = dp(8);
                programTitle.setLayoutParams(titleParams);
                programTitle.setText(program.title == null || program.title.trim().isEmpty() ? getString(R.string.label_program_default) : program.title.trim());
                programTitle.setTextColor(0xFFFFFFFF);
                programTitle.setTypeface(Typeface.DEFAULT_BOLD);
                programTitle.setTextSize(12f);
                programTitle.setMaxLines(2);
                programTitle.setMinLines(2);
                card.addView(programTitle);

                TextView programMeta = new TextView(this);
                LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                metaParams.topMargin = dp(4);
                programMeta.setLayoutParams(metaParams);
                programMeta.setText(shortTime(program.startTime) + " - " + shortTime(program.endTime));
                programMeta.setTextColor(0xFFC9D8E8);
                programMeta.setTextSize(10f);
                programMeta.setMaxLines(1);
                programMeta.setMinLines(1);
                card.addView(programMeta);

                Runnable applyState = () -> {
                    boolean focused = card.hasFocus();
                    android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                    bg.setCornerRadius(dp(18));
                    bg.setColor(focused ? 0xFF213447 : 0xFF17232F);
                    bg.setStroke(dp(2), focused ? 0xFF68B6FF : (scheduled ? 0xFFAF7A21 : 0xFF284156));
                    card.setBackground(bg);
                    card.setScaleX(focused ? 1.03f : 1f);
                    card.setScaleY(focused ? 1.03f : 1f);
                };
                applyState.run();

                final int itemIndex = rowFocusables.size();
                final int focusCenter = itemIndex * (cardWidth + cardGap) + (cardWidth / 2);
                card.setTag(focusCenter);
                card.setOnFocusChangeListener((v, hasFocus) -> {
                    applyState.run();
                    if (!hasFocus) {
                        return;
                    }
                    lastVisualEpgChannelId = channel == null ? lastVisualEpgChannelId : channel.id;
                    lastVisualEpgProgramStartTime = program == null ? lastVisualEpgProgramStartTime : program.startTime;
                    titleText.setText(program.title == null || program.title.trim().isEmpty()
                            ? getString(R.string.label_program_default)
                            : program.title.trim());
                    String detailMeta = channel.name + "  ·  " + shortTime(program.startTime) + " - " + shortTime(program.endTime);
                    if (live) {
                        detailMeta = detailMeta + "  ·  " + getString(R.string.guide_program_now);
                    }
                    if (scheduled) {
                        detailMeta = detailMeta + "  ·  " + getString(R.string.timeline_program_scheduled_short);
                    }
                    metaText.setText(detailMeta);
                    descText.setText(program.description == null || program.description.trim().isEmpty()
                            ? getString(R.string.timeline_program_desc_empty)
                            : program.description.trim());
                    if (heroPoster == null || heroPoster.trim().isEmpty()) {
                        posterImage.setVisibility(View.GONE);
                        Glide.with(this).clear(posterImage);
                    } else {
                        posterImage.setVisibility(View.VISIBLE);
                        Glide.with(this).load(heroPoster.trim()).fitCenter().into(posterImage);
                    }
                    horizontalScroll.post(() -> horizontalScroll.smoothScrollTo(Math.max(0, card.getLeft() - dp(24)), 0));
                    if (verticalScroll != null) {
                        View anchor = focusAnchors.get(card);
                        verticalScroll.postDelayed(() -> scrollVisualEpgSectionIntoPlace(verticalScroll, anchor != null ? anchor : card), 24L);
                    }
                });
                card.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) {
                        return false;
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (itemIndex > 0) {
                            rowFocusables.get(itemIndex - 1).requestFocus();
                            return true;
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (itemIndex + 1 < rowFocusables.size()) {
                            rowFocusables.get(itemIndex + 1).requestFocus();
                            return true;
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        return moveVisualEpgFocus(focusRows, rowIndex, -1, focusCenter);
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        return moveVisualEpgFocus(focusRows, rowIndex, 1, focusCenter);
                    }
                    return false;
                });
                card.setOnClickListener(v -> channelActionsCoordinator.showProgramActionMenu(channel, program));
                rowFocusables.add(card);
                focusCenters.put(card, focusCenter);
                focusAnchors.put(card, sectionTitle);
                boolean preferredVisualCard = lastVisualEpgChannelId != null && lastVisualEpgChannelId.equals(channel.id)
                        && lastVisualEpgProgramStartTime != null && lastVisualEpgProgramStartTime.equals(program.startTime);
                if (preferredVisualCard) {
                    initialFocus[0] = card;
                } else if (anchorChannelId != null && anchorChannelId.equals(channel.id) && initialFocus[0] == null) {
                    initialFocus[0] = card;
                } else if (initialFocus[0] == null) {
                    initialFocus[0] = card;
                }
                row.addView(card);
            }
            focusRows.add(rowFocusables);
        }

        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(dialogView);
        dialog.setCancelable(true);
        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
            if (initialFocus[0] != null) {
                initialFocus[0].requestFocus();
            }
        });
        dialog.setOnDismissListener(d -> enableImmersiveMode());
        refreshButton.setOnClickListener(v -> {
            dialog.dismiss();
            openVisualEpgAroundSelection();
        });
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void openCurrentProgramInfoFromTouch() {
        ChannelItem channel = (currentIndex >= 0 && currentIndex < channels.size()) ? channels.get(currentIndex) : null;
        if (channel == null) {
            showStatus(getString(R.string.status_no_program_in_epg));
            return;
        }
        if (channel.isVod) {
            showVodInfoDialog(channel);
            return;
        }
        showStatus(getString(R.string.status_searching_current_program));
        ioExecutor.execute(() -> {
            try {
                EpgRepository.EpgProgram program = epgRepository.fetchProgramForChannel(channel, false);
                if (program == null) {
                    uiHandler.post(() -> showStatus(getString(R.string.status_no_program_in_epg)));
                    return;
                }
                uiHandler.post(() -> showCurrentProgramInfoDialog(channel, program));
            } catch (Exception e) {
                Log.w(TAG, "touch info current program failed", e);
                uiHandler.post(() -> showStatus(getString(R.string.status_failed_get_program)));
            }
        });
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_about_app, BuildConfig.VERSION_NAME))
                .setMessage(getString(R.string.message_about_app))
                .setPositiveButton(R.string.dialog_close, null)
                .show();
    }

    private void showVodInfoDialog(ChannelItem channel) {
        if (channel == null) {
            return;
        }
        rememberCurrentVodPosition();
        prepareModalSurface();

        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        int panelWidth = Math.min(dp(880), Math.max(dp(720), metrics.widthPixels - dp(140)));
        int panelMaxHeight = Math.max(dp(420), metrics.heightPixels - dp(120));
        android.widget.ScrollView panelScrollView = new android.widget.ScrollView(this);
        panelScrollView.setFillViewport(false);
        panelScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        panelScrollView.setLayoutParams(new ViewGroup.LayoutParams(panelWidth, panelMaxHeight));
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16);
        panel.setPadding(padding, padding, padding, padding);
        panel.setBackgroundColor(0xF0181E28);
        panel.setLayoutParams(new ViewGroup.LayoutParams(panelWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.TOP);
        panel.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView posterView = new ImageView(this);
        LinearLayout.LayoutParams posterParams = new LinearLayout.LayoutParams(dp(150), dp(202));
        posterParams.setMarginEnd(dp(18));
        posterView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        posterView.setBackgroundColor(0xFF0E1820);
        header.addView(posterView, posterParams);
        bindRecordingPoster(posterView, channel.logoUrl);

        LinearLayout infoColumn = new LinearLayout(this);
        infoColumn.setOrientation(LinearLayout.VERTICAL);
        header.addView(infoColumn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView titleView = new TextView(this);
        titleView.setText(channel.name == null || channel.name.trim().isEmpty() ? getString(R.string.label_program_default) : channel.name.trim());
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(23f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setMaxLines(2);
        infoColumn.addView(titleView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView metaView = new TextView(this);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaParams.topMargin = dp(8);
        metaView.setText(buildVodInfoMeta(channel));
        metaView.setTextColor(0xFF9BD0FF);
        metaView.setTextSize(15f);
        infoColumn.addView(metaView, metaParams);

        TextView descView = new TextView(this);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = dp(14);
        descView.setText(buildVodDescription(channel));
        descView.setTextColor(0xFFD5E6F8);
        descView.setTextSize(14f);
        descView.setMaxLines(3);
        infoColumn.addView(descView, descParams);

        long resumeMs = getVodResumePosition(channel.id);
        if (resumeMs > 0L) {
            TextView resumeView = new TextView(this);
            LinearLayout.LayoutParams resumeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            resumeParams.topMargin = dp(14);
            resumeView.setText(buildVodProgressLabel(channel, resumeMs));
            resumeView.setTextColor(0xFFFFD082);
            resumeView.setTextSize(14f);
            resumeView.setTypeface(Typeface.DEFAULT_BOLD);
            resumeView.setGravity(Gravity.CENTER_VERTICAL);
            resumeView.setMinHeight(dp(38));
            resumeView.setPadding(dp(12), 0, dp(12), 0);
            resumeView.setBackground(makeRoundedBackground(0xFF2F3A25, 0xFFFFD782, dp(1), dp(8)));
            infoColumn.addView(resumeView, resumeParams);
        }

        TextView primaryTitle = new TextView(this);
        LinearLayout.LayoutParams primaryTitleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        primaryTitleParams.topMargin = dp(18);
        primaryTitle.setText(R.string.vod_detail_primary_actions);
        primaryTitle.setTextColor(0xFFFFFFFF);
        primaryTitle.setTextSize(15f);
        primaryTitle.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(primaryTitle, primaryTitleParams);

        LinearLayout primaryActionsRow = new LinearLayout(this);
        primaryActionsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams primaryActionsParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        primaryActionsParams.topMargin = dp(8);
        panel.addView(primaryActionsRow, primaryActionsParams);

        final Dialog[] dialogHolder = new Dialog[1];
        List<TextView> primaryActions = new ArrayList<>();
        addVodDetailPrimaryAction(primaryActionsRow, primaryActions, getString(R.string.vod_action_play), () -> {
            Dialog activeDialog = dialogHolder[0];
            if (activeDialog != null && activeDialog.isShowing()) {
                activeDialog.dismiss();
            }
            playVodItem(channel, true);
        });
        if (resumeMs > 30_000L) {
            addVodDetailPrimaryAction(primaryActionsRow, primaryActions, getString(R.string.vod_action_continue), () -> {
                Dialog activeDialog = dialogHolder[0];
                if (activeDialog != null && activeDialog.isShowing()) {
                    activeDialog.dismiss();
                }
                playChannelItemInternal(channel, true, getVodResumePosition(channel.id));
            });
            addVodDetailPrimaryAction(primaryActionsRow, primaryActions, getString(R.string.vod_action_start_over), () -> {
                Dialog activeDialog = dialogHolder[0];
                if (activeDialog != null && activeDialog.isShowing()) {
                    activeDialog.dismiss();
                }
                clearVodResumePosition(channel.id);
                playChannelItemInternal(channel, true, 0L);
            });
        }

        TextView secondaryTitle = new TextView(this);
        LinearLayout.LayoutParams secondaryTitleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        secondaryTitleParams.topMargin = dp(16);
        secondaryTitle.setText(R.string.vod_detail_secondary_actions);
        secondaryTitle.setTextColor(0xFFB7C4D6);
        secondaryTitle.setTextSize(13f);
        secondaryTitle.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(secondaryTitle, secondaryTitleParams);

        TextView hintView = new TextView(this);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(6);
        hintView.setText(R.string.vod_detail_action_hint);
        hintView.setTextColor(0xFFB7C4D6);
        hintView.setTextSize(12f);
        panel.addView(hintView, hintParams);

        LinearLayout actionsColumn = new LinearLayout(this);
        actionsColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsParams.topMargin = dp(8);
        panel.addView(actionsColumn, actionsParams);

        List<TextView> secondaryActions = new ArrayList<>();
        addVodDetailAction(actionsColumn, secondaryActions, getString(R.string.vod_action_more_vod), () -> {
            Dialog activeDialog = dialogHolder[0];
            if (activeDialog != null && activeDialog.isShowing()) {
                activeDialog.dismiss();
            }
            showVodActionsDialog(channel);
        });
        addVodDetailAction(actionsColumn, secondaryActions, getString(favoriteChannelIds.contains(channel.id) ? R.string.vod_action_remove_favorite : R.string.vod_action_add_favorite), () -> toggleFavoriteForChannel(channel));
        if (resumeMs > 30_000L) {
            addVodDetailAction(actionsColumn, secondaryActions, getString(R.string.vod_action_clear_progress_vod), () -> {
                clearVodResumePosition(channel.id);
                showStatus(getString(R.string.vod_status_progress_cleared));
            });
        }
        wireVodDetailActions(primaryActions, secondaryActions, panelScrollView);
        panelScrollView.addView(panel, new android.widget.ScrollView.LayoutParams(panelWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

        Dialog dialog = showCenteredTvPanelDialog(panelScrollView, () -> Glide.with(this).clear(posterView));
        dialogHolder[0] = dialog;
        if (!primaryActions.isEmpty()) {
            primaryActions.get(0).post(() -> {
                panelScrollView.scrollTo(0, 0);
                primaryActions.get(0).requestFocus();
            });
        }
    }

    private Dialog showCenteredTvPanelDialog(View contentView, Runnable onDismiss) {
        android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        root.setBackgroundColor(0xCC000000);
        android.widget.FrameLayout.LayoutParams contentParams = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        root.addView(contentView, contentParams);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(root);
        dialog.setOnDismissListener(d -> {
            if (onDismiss != null) {
                onDismiss.run();
            }
            enableImmersiveMode();
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setDimAmount(0f);
        }
        return dialog;
    }

    private String buildVodProgressLabel(ChannelItem channel, long resumeMs) {
        String progress = getString(R.string.vod_resume_meta, formatDurationShort(resumeMs));
        if (channel != null && channel.vodDurationSeconds > 0L) {
            long durationMs = channel.vodDurationSeconds * 1000L;
            int percent = (int) Math.max(1L, Math.min(99L, (resumeMs * 100L) / Math.max(1L, durationMs)));
            return getString(R.string.vod_resume_meta_percent, formatDurationShort(resumeMs), percent);
        }
        return progress;
    }

    private Drawable makeRoundedBackground(int fillColor, int strokeColor, int strokeWidth, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(radius);
        background.setColor(fillColor);
        background.setStroke(strokeWidth, strokeColor);
        return background;
    }

    private void addVodDetailPrimaryAction(LinearLayout parent, List<TextView> actionRows, String label, Runnable action) {
        if (parent == null || label == null) {
            return;
        }
        TextView row = new TextView(this);
        row.setText(label);
        row.setTextColor(0xFF111820);
        row.setTextSize(15f);
        row.setTypeface(Typeface.DEFAULT_BOLD);
        row.setGravity(Gravity.CENTER);
        row.setSingleLine(true);
        row.setEllipsize(TextUtils.TruncateAt.END);
        row.setMinHeight(dp(54));
        row.setPadding(dp(12), 0, dp(12), 0);
        row.setBackground(makeRoundedBackground(0xFFFFD782, 0xFFFFFFFF, dp(1), dp(10)));
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);
        row.setClickable(true);
        row.setOnFocusChangeListener((v, hasFocus) -> row.setBackground(hasFocus
                ? makeRoundedBackground(0xFFFFFFFF, 0xFFFFD782, dp(3), dp(10))
                : makeRoundedBackground(0xFFFFD782, 0xFFFFFFFF, dp(1), dp(10))));
        row.setOnClickListener(v -> {
            if (action != null) {
                action.run();
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(54), 1f);
        if (actionRows != null && !actionRows.isEmpty()) {
            params.setMarginStart(dp(8));
        }
        parent.addView(row, params);
        if (actionRows != null) {
            actionRows.add(row);
        }
    }

    private void addVodDetailAction(LinearLayout parent, List<TextView> actionRows, String label, Runnable action) {
        if (parent == null || label == null) {
            return;
        }
        TextView row = new TextView(this);
        row.setText(label);
        row.setTextColor(0xFFFFFFFF);
        row.setTextSize(16f);
        row.setTypeface(Typeface.DEFAULT_BOLD);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinHeight(dp(48));
        row.setPadding(dp(14), 0, dp(14), 0);
        row.setBackgroundResource(R.drawable.search_channel_item_bg);
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);
        row.setClickable(true);
        row.setOnClickListener(v -> {
            if (action != null) {
                action.run();
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        params.topMargin = actionRows == null || actionRows.isEmpty() ? 0 : dp(8);
        parent.addView(row, params);
        if (actionRows != null) {
            actionRows.add(row);
        }
    }

    private void wireVodDetailActions(List<TextView> primaryActions, List<TextView> secondaryActions, android.widget.ScrollView scrollView) {
        if (primaryActions != null) {
            for (int i = 0; i < primaryActions.size(); i++) {
                final int index = i;
                TextView action = primaryActions.get(i);
                action.setOnFocusChangeListener((v, hasFocus) -> {
                    action.setBackground(hasFocus
                            ? makeRoundedBackground(0xFFFFFFFF, 0xFFFFD782, dp(3), dp(10))
                            : makeRoundedBackground(0xFFFFD782, 0xFFFFFFFF, dp(1), dp(10)));
                    if (hasFocus) {
                        ensureVodVisualItemVisible(scrollView, action);
                    }
                });
                action.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) {
                        return false;
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && index > 0) {
                        return primaryActions.get(index - 1).requestFocus();
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && index + 1 < primaryActions.size()) {
                        return primaryActions.get(index + 1).requestFocus();
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && secondaryActions != null && !secondaryActions.isEmpty()) {
                        return secondaryActions.get(Math.min(index, secondaryActions.size() - 1)).requestFocus();
                    }
                    return false;
                });
            }
        }
        if (secondaryActions != null) {
            for (int i = 0; i < secondaryActions.size(); i++) {
                final int index = i;
                TextView action = secondaryActions.get(i);
                action.setOnFocusChangeListener((v, hasFocus) -> {
                    action.setSelected(hasFocus);
                    if (hasFocus) {
                        ensureVodVisualItemVisible(scrollView, action);
                    }
                });
                action.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) {
                        return false;
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if (index == 0 && primaryActions != null && !primaryActions.isEmpty()) {
                            return primaryActions.get(0).requestFocus();
                        }
                        if (index > 0) {
                            return secondaryActions.get(index - 1).requestFocus();
                        }
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && index + 1 < secondaryActions.size()) {
                        return secondaryActions.get(index + 1).requestFocus();
                    }
                    return false;
                });
            }
        }
    }

    private void showVodActionsDialog(ChannelItem channel) {
        if (channel == null) {
            return;
        }
        prepareModalSurface();
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        int panelWidth = Math.min(dp(720), Math.max(dp(620), metrics.widthPixels - dp(220)));
        int panelMaxHeight = Math.max(dp(420), metrics.heightPixels - dp(120));
        android.widget.ScrollView panelScrollView = new android.widget.ScrollView(this);
        panelScrollView.setFillViewport(false);
        panelScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        panelScrollView.setLayoutParams(new ViewGroup.LayoutParams(panelWidth, panelMaxHeight));
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16);
        panel.setPadding(padding, padding, padding, padding);
        panel.setBackgroundColor(0xF0181E28);
        panel.setLayoutParams(new ViewGroup.LayoutParams(panelWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView titleView = new TextView(this);
        titleView.setText(getString(R.string.vod_actions_title, displayName(channel)));
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(20f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setMaxLines(2);
        panel.addView(titleView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView metaView = new TextView(this);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaParams.topMargin = dp(6);
        metaView.setText(buildVodInfoMeta(channel));
        metaView.setTextColor(0xFFB7C4D6);
        metaView.setTextSize(13f);
        panel.addView(metaView, metaParams);

        LinearLayout actionsColumn = new LinearLayout(this);
        actionsColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsParams.topMargin = dp(12);
        panel.addView(actionsColumn, actionsParams);

        final Dialog[] dialogHolder = new Dialog[1];
        List<TextView> actionRows = new ArrayList<>();
        addVodDetailAction(actionsColumn, actionRows, getString(R.string.vod_action_play_vod), () -> {
            dismissDialog(dialogHolder[0]);
            clearVodResumePosition(channel.id);
            playChannelItemInternal(channel, true, 0L);
        });
        if (getVodResumePosition(channel.id) > 30_000L) {
            addVodDetailAction(actionsColumn, actionRows, getString(R.string.vod_action_continue_vod), () -> {
                dismissDialog(dialogHolder[0]);
                playChannelItemInternal(channel, true, getVodResumePosition(channel.id));
            });
            addVodDetailAction(actionsColumn, actionRows, getString(R.string.vod_action_start_over_vod), () -> {
                dismissDialog(dialogHolder[0]);
                clearVodResumePosition(channel.id);
                playChannelItemInternal(channel, true, 0L);
            });
        }
        addVodDetailAction(actionsColumn, actionRows, getString(R.string.vod_action_diagnostics), () -> showVodDiagnosticsDialog(channel));
        addVodDetailAction(actionsColumn, actionRows, getString(R.string.vod_action_retry_route), () -> {
            dismissDialog(dialogHolder[0]);
            retryCurrentPlaybackWithNextRoute(channel);
        });
        addVodDetailAction(actionsColumn, actionRows, getString(R.string.vod_action_temporary_mode), () -> {
            dismissDialog(dialogHolder[0]);
            showTemporaryPlaybackModeDialog(channel);
        });
        addVodDetailAction(actionsColumn, actionRows, getString(favoriteChannelIds.contains(channel.id) ? R.string.vod_action_remove_favorite : R.string.vod_action_add_favorite), () -> toggleFavoriteForChannel(channel));
        addVodDetailAction(actionsColumn, actionRows, getString(R.string.vod_action_personal_lists), () -> {
            dismissDialog(dialogHolder[0]);
            showPersonalListsDialog(channel);
        });
        addVodDetailAction(actionsColumn, actionRows, getString(R.string.vod_action_playback_diagnostics), () -> {
            dismissDialog(dialogHolder[0]);
            currentPlaybackVodId = channel.id;
            showPlaybackDiagnosticsDialog();
        });
        addVodDetailAction(actionsColumn, actionRows, getString(R.string.vod_action_clear_progress_vod), () -> {
            clearVodResumePosition(channel.id);
            showStatus(getString(R.string.vod_status_progress_cleared));
        });
        wireVodActionRows(actionRows, panelScrollView);
        panelScrollView.addView(panel, new android.widget.ScrollView.LayoutParams(panelWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
        Dialog dialog = showCenteredTvPanelDialog(panelScrollView, null);
        dialogHolder[0] = dialog;
        if (!actionRows.isEmpty()) {
            actionRows.get(0).post(() -> {
                panelScrollView.scrollTo(0, 0);
                actionRows.get(0).requestFocus();
            });
        }
    }

    private void wireVodActionRows(List<TextView> actionRows, android.widget.ScrollView scrollView) {
        if (actionRows == null) {
            return;
        }
        for (int i = 0; i < actionRows.size(); i++) {
            final int index = i;
            TextView action = actionRows.get(i);
            action.setOnFocusChangeListener((v, hasFocus) -> {
                action.setSelected(hasFocus);
                if (hasFocus) {
                    ensureVodVisualItemVisible(scrollView, action);
                }
            });
            action.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP && index > 0) {
                    return actionRows.get(index - 1).requestFocus();
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && index + 1 < actionRows.size()) {
                    return actionRows.get(index + 1).requestFocus();
                }
                return false;
            });
        }
    }

    private void dismissDialog(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private String buildVodInfoMeta(ChannelItem channel) {
        List<String> parts = new ArrayList<>();
        parts.add(getString(channel.isAdultVod ? R.string.channel_badge_vod_adult : R.string.channel_badge_vod));
        if (channel.platformName != null && !channel.platformName.trim().isEmpty()) {
            parts.add(channel.platformName.trim());
        }
        if (channel.group != null && !channel.group.trim().isEmpty()) {
            parts.add(channel.group.trim());
        }
        if (channel.vodYear != null && !channel.vodYear.trim().isEmpty()) {
            parts.add(channel.vodYear.trim());
        }
        if (channel.vodDurationSeconds > 0L) {
            parts.add(formatDurationShort(channel.vodDurationSeconds * 1000L));
        }
        long resumeMs = getVodResumePosition(channel.id);
        if (resumeMs > 30_000L) {
            parts.add(getString(R.string.vod_resume_meta, formatDurationShort(resumeMs)));
        }
        return TextUtils.join("  ·  ", parts);
    }

    private String buildVodDescription(ChannelItem channel) {
        if (channel == null) {
            return "";
        }
        if (channel.vodDescription != null && !channel.vodDescription.trim().isEmpty()) {
            return channel.vodDescription.trim();
        }
        return channel.isAdultVod ? getString(R.string.vod_info_desc_adult) : getString(R.string.vod_info_desc);
    }

    private void showVodDiagnosticsDialog(ChannelItem channel) {
        if (channel == null) {
            return;
        }
        PlayerController.PlaybackRequest request = toPlaybackRequest(channel);
        PlaybackRouteResolver.Decision decision = request == null
                ? null
                : new PlaybackRouteResolver(baseUrl).buildDecision(request, false, streamInfoByChannelId.get(channel.id));
        StringBuilder message = new StringBuilder();
        appendDiagnosticLine(message, getString(R.string.diagnostics_channel, displayName(channel)));
        appendDiagnosticLine(message, getString(R.string.vod_diagnostics_source, fallbackUnknown(channel.platformName)));
        appendDiagnosticLine(message, getString(R.string.diagnostics_playback_mode, formatPlaybackModeLabel(resolvePlaybackModeForRequest(channel))));
        appendDiagnosticLine(message, getString(R.string.diagnostics_target, decision == null ? fallbackUnknown(channel.playUrl) : fallbackUnknown(decision.targetUrl)));
        appendDiagnosticLine(message, getString(R.string.diagnostics_mime, decision == null ? fallbackUnknown(PlaybackRouteResolver.inferMimeType(channel.playUrl)) : fallbackUnknown(decision.mimeType)));
        appendDiagnosticLine(message, getString(R.string.diagnostics_drm, fallbackUnknown(channel.drmScheme)));
        appendDiagnosticLine(message, getString(R.string.vod_diagnostics_direct, getString(channel.directPlayback ? R.string.diagnostics_value_yes : R.string.diagnostics_value_no)));
        if (channel.vodFilterKey != null && !channel.vodFilterKey.trim().isEmpty()) {
            appendDiagnosticLine(message, getString(R.string.vod_diagnostics_filter, channel.vodFilterKey));
        }
        if (channel.playUrl != null && channel.playUrl.contains("/api/vod/runtime/stream/")) {
            appendDiagnosticLine(message, getString(R.string.vod_diagnostics_runtime_hls));
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.vod_action_diagnostics)
                .setMessage(message.toString().trim())
                .setPositiveButton(R.string.vod_action_play, (d, which) -> playVodItem(channel, true))
                .setNeutralButton(R.string.diagnostics_action_temporary_mode, (d, which) -> showTemporaryPlaybackModeDialog(channel))
                .setNegativeButton(R.string.dialog_close, null)
                .show();
    }

    private void showCurrentProgramInfoDialog(ChannelItem channel, EpgRepository.EpgProgram program) {
        if (channel == null || program == null) {
            return;
        }
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(18);
        container.setPadding(padding, padding, padding, padding);

        ImageView posterView = new ImageView(this);
        LinearLayout.LayoutParams posterParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220));
        posterView.setLayoutParams(posterParams);
        posterView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        container.addView(posterView);
        bindProgramPoster(posterView, program.icon);

        TextView titleView = new TextView(this);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(14);
        titleView.setLayoutParams(titleParams);
        titleView.setText(program.title == null || program.title.trim().isEmpty() ? channel.name : program.title);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(20f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        container.addView(titleView);

        TextView metaView = new TextView(this);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaParams.topMargin = dp(8);
        metaView.setLayoutParams(metaParams);
        metaView.setText(channel.name + "  ·  " + shortTime(program.startTime) + " - " + shortTime(program.endTime));
        metaView.setTextColor(0xFF9BD0FF);
        metaView.setTextSize(14f);
        container.addView(metaView);

        TextView descView = new TextView(this);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = dp(12);
        descView.setLayoutParams(descParams);
        String description = program.description == null || program.description.trim().isEmpty()
                ? getString(R.string.timeline_program_desc_empty)
                : program.description.trim();
        descView.setText(description);
        descView.setTextColor(0xFFD5E6F8);
        descView.setTextSize(15f);
        container.addView(descView);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_touch_program_info))
                .setView(container)
                .setPositiveButton(isOfflineRecordingsDisabled() ? R.string.dialog_close : R.string.menu_record, (d, which) -> {
                    if (!isOfflineRecordingsDisabled()) {
                        scheduleProgram(channel, program);
                    }
                })
                .setNegativeButton(R.string.dialog_close, null)
                .create();
        dialog.setOnDismissListener(d -> Glide.with(this).clear(posterView));
        dialog.show();
    }

    private void createScheduleFromEndpoint(ChannelItem ch, boolean next) {
        if (showOfflineRecordingsUnavailableIfNeeded()) {
            return;
        }
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
                EpgRepository.EpgProgram program = epgRepository.fetchProgramForChannel(ch, next);
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
        if (showOfflineRecordingsUnavailableIfNeeded()) {
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
                uiHandler.post(() -> {
                    showStatus(getString(R.string.status_recording_scheduled));
                    markScheduledProgramInOpenTimeline(ch, program);
                });
            } catch (Exception e) {
                Log.w(TAG, "schedule program failed", e);
                uiHandler.post(() -> showStatus(getString(R.string.status_failed_schedule_recording)));
            }
        });
    }

    private void markScheduledProgramInOpenTimeline(ChannelItem channel, EpgRepository.EpgProgram program) {
        if (channel == null || program == null || activeTimelineDialog == null || !activeTimelineDialog.isShowing()) {
            return;
        }
        activeTimelineScheduledItems.add(new RecordingsRepository.RecordingItem(
                "timeline-" + System.currentTimeMillis(),
                program.title == null || program.title.trim().isEmpty() ? channel.name : program.title,
                "",
                0L,
                "",
                channel.name,
                program.title == null ? "" : program.title,
                program.icon == null || program.icon.trim().isEmpty() ? channel.logoUrl : program.icon,
                "scheduled",
                program.startTime == null ? "" : program.startTime,
                program.endTime == null ? "" : program.endTime,
                false
        ));
        refreshTimelineGuideDialog();
    }

    private void refreshTimelineGuideDialog() {
        if (activeTimelineDialog == null || !activeTimelineDialog.isShowing() || activeTimelineRows.isEmpty()) {
            return;
        }
        android.app.Dialog dialog = activeTimelineDialog;
        List<TimelineChannelPrograms> rows = new ArrayList<>(activeTimelineRows);
        List<RecordingsRepository.RecordingItem> scheduled = new ArrayList<>(activeTimelineScheduledItems);
        long windowStartMs = activeTimelineWindowStartMs;
        String anchorChannelId = activeTimelineAnchorChannelId;
        refreshingTimelineDialog = true;
        dialog.dismiss();
        refreshingTimelineDialog = false;
        showTimelineGuideDialog(rows, windowStartMs, anchorChannelId, scheduled);
    }

    private void cancelScheduledProgram(ChannelItem ch, EpgRepository.EpgProgram program) {
        if (showOfflineRecordingsUnavailableIfNeeded()) {
            return;
        }
        RecordingsRepository.RecordingItem scheduled = findScheduledProgramRecording(ch, program, activeProgramScheduledItems);
        if (scheduled == null) {
            showStatus(getString(R.string.status_failed_cancel_scheduled_recording));
            return;
        }
        showStatus(getString(R.string.status_canceling_scheduled_recording));
        ioExecutor.execute(() -> {
            try {
                recordingsRepository.deleteScheduledRecording(scheduled.id);
                uiHandler.post(() -> {
                    activeProgramScheduledItems.remove(scheduled);
                    activeTimelineScheduledItems.removeIf(item -> item != null && scheduled.id.equals(item.id));
                    showStatus(getString(R.string.status_scheduled_recording_canceled));
                    if (activeTimelineDialog != null && activeTimelineDialog.isShowing()) {
                        refreshTimelineGuideDialog();
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "cancel scheduled program failed", e);
                uiHandler.post(() -> showStatus(getString(R.string.status_failed_cancel_scheduled_recording)));
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
        if (showOfflineRecordingsUnavailableIfNeeded()) {
            return;
        }
        Log.d(TAG, "openRecordingsBrowser scheduledMode=" + recordingsController.isScheduledMode());
        loadRecordingsPanel(recordingsController.isScheduledMode(), recordingsController.getLastSelectedId());
    }

    private void loadRecordingsPanel(boolean scheduledMode, String preferredId) {
        recordingsController.setScheduledMode(scheduledMode);
        showStatus(getString(scheduledMode ? R.string.status_loading_scheduled_recordings : R.string.status_loading_recordings));
        final String desiredId = preferredId;
        ioExecutor.execute(() -> {
            try {
                RecordingsRepository.RecordingsResult fetchedPrimaryResult = scheduledMode
                        ? recordingsRepository.fetchScheduledRecordings()
                        : recordingsRepository.fetchCompletedRecordings();
                RecordingsRepository.RecordingsResult fetchedAlternateResult = scheduledMode
                        ? recordingsRepository.fetchCompletedRecordings()
                        : recordingsRepository.fetchScheduledRecordings();
                final RecordingsRepository.RecordingsResult primaryResult = filterRecordingsResult(fetchedPrimaryResult);
                final RecordingsRepository.RecordingsResult alternateResult = filterRecordingsResult(fetchedAlternateResult);
                if (!primaryResult.items.isEmpty()) {
                    Log.d(TAG, "loadRecordingsPanel primary scheduled=" + primaryResult.scheduledMode + " count=" + primaryResult.items.size());
                    uiHandler.post(() -> showRecordingsPanel(primaryResult, desiredId));
                    return;
                }
                if (!alternateResult.items.isEmpty()) {
                    Log.d(TAG, "loadRecordingsPanel alternate scheduled=" + alternateResult.scheduledMode + " count=" + alternateResult.items.size());
                    uiHandler.post(() -> {
                        showStatus(getString(scheduledMode
                                ? R.string.status_recordings_showing_completed
                                : R.string.status_recordings_showing_scheduled));
                        showRecordingsPanel(alternateResult, desiredId);
                    });
                    return;
                }
                Log.d(TAG, "loadRecordingsPanel both empty scheduledMode=" + scheduledMode);
                uiHandler.post(() -> {
                    showRecordingsPanel(primaryResult, desiredId);
                    showStatus(getString(scheduledMode ? R.string.status_no_scheduled_recordings : R.string.status_no_recordings));
                });
            } catch (Exception e) {
                Log.w(TAG, scheduledMode ? "open scheduled recordings failed" : "open recordings failed", e);
                uiHandler.post(() -> showStatus(getString(scheduledMode ? R.string.status_failed_load_scheduled_recordings : R.string.status_failed_load_recordings)));
            }
        });
    }

    private void refreshRecordingsPanel() {
        RecordingsRepository.RecordingItem selected = getSelectedRecordingItem();
        loadRecordingsPanel(recordingsController.isScheduledMode(), selected == null ? null : selected.id);
    }

    private void switchRecordingsMode(boolean scheduledMode) {
        if (recordingsController.isScheduledMode() == scheduledMode && isRecordingsPanelVisible()) {
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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_recording_cancel_confirm)
                .setMessage(getString(R.string.recording_cancel_confirm_message, buildRecordingTitle(item), buildRecordingMeta(item)))
                .setPositiveButton(R.string.recording_action_cancel_confirm, (unused, which) -> cancelScheduledRecording(item))
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        showTvDialog(dialog);
    }

    private void cancelScheduledRecording(RecordingsRepository.RecordingItem item) {
        if (item == null || item.playable) {
            return;
        }
        if (showOfflineRecordingsUnavailableIfNeeded()) {
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
        if (showOfflineRecordingsUnavailableIfNeeded()) {
            return;
        }
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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_recording_edit_time)
                .setItems(options, (unused, which) -> {
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
                .create();
        showTvDialog(dialog);
    }

    private void adjustSelectedScheduledRecording(long startDeltaMs, long endDeltaMs) {
        if (showOfflineRecordingsUnavailableIfNeeded()) {
            return;
        }
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
        if (showOfflineRecordingsUnavailableIfNeeded()) {
            return;
        }
        if (item == null || !item.playable) {
            showStatus(getString(R.string.status_recording_not_playable));
            return;
        }
        rememberCurrentVodPosition();
        currentPlaybackVodId = null;
        String url = recordingsRepository.buildPlaybackUrl(item, basePath);
        currentPlaybackRecordingId = item.id;
        currentPlaybackReturnChannelId = getCurrentChannelId();
        long resumePositionMs = getRecordingResumePosition(item.id);
        Runnable startFromBeginning = () -> {
            clearRecordingResumePosition(item.id);
            playerController.playRecording(buildRecordingTitle(item), url, 0L);
            hideRecordingsPanel();
            hideOverlay();
        };
        Runnable resumeFromSaved = () -> {
            playerController.playRecording(buildRecordingTitle(item), url, resumePositionMs);
            hideRecordingsPanel();
            hideOverlay();
        };
        if (resumePositionMs > 30_000L) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.title_recordings_visual)
                    .setMessage(getString(R.string.recording_resume_prompt, formatPlaybackPosition(resumePositionMs)))
                    .setPositiveButton(R.string.recording_resume_continue, (unused, which) -> resumeFromSaved.run())
                    .setNegativeButton(R.string.recording_resume_restart, (unused, which) -> startFromBeginning.run())
                    .create();
            showTvDialog(dialog);
            return;
        }
        startFromBeginning.run();
    }

    private RecordingsRepository.RecordingItem getSelectedRecordingItem() {
        return recordingsController.getSelectedItem();
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
            if (getRecordingResumePosition(item.id) > 30_000L) {
                options.add(getString(R.string.recording_action_clear_progress));
                actions.add(() -> clearSelectedRecordingProgress(item));
            }
        } else {
            options.add(getString(R.string.recording_action_edit_time));
            actions.add(this::showScheduledRecordingEditDialog);
            options.add(getString(R.string.recording_action_cancel));
            actions.add(this::cancelSelectedScheduledRecording);
        }
        options.add(getString(R.string.recording_action_refresh));
        actions.add(this::refreshRecordingsPanel);
        options.add(getString(recordingsAutoRefreshEnabled ? R.string.recording_action_auto_refresh_on : R.string.recording_action_auto_refresh_off));
        actions.add(this::toggleRecordingsAutoRefresh);
        options.add(getString(recordingsController.isScheduledMode() ? R.string.recording_action_switch_completed : R.string.recording_action_switch_scheduled));
        actions.add(() -> switchRecordingsMode(!recordingsController.isScheduledMode()));
        addRecordingFilterActions(options, actions, item);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_recording_actions)
                .setItems(options.toArray(new String[0]), (unused, which) -> actions.get(which).run())
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        showTvDialog(dialog);
    }

    private void clearSelectedRecordingProgress(RecordingsRepository.RecordingItem item) {
        if (item == null) {
            return;
        }
        clearRecordingResumePosition(item.id);
        if (recordingsAdapter != null) {
            recordingsAdapter.notifyDataSetChanged();
        }
        updateRecordingsDetailPanel();
        showStatus(getString(R.string.status_recording_progress_cleared));
    }

    private void addRecordingFilterActions(List<String> options, List<Runnable> actions, RecordingsRepository.RecordingItem item) {
        String itemChannel = item == null ? "" : cleanText(item.channelName);
        if (!itemChannel.isEmpty() && !itemChannel.equalsIgnoreCase(recordingsChannelFilter)) {
            options.add(getString(R.string.recording_action_filter_channel, itemChannel));
            actions.add(() -> applyRecordingsChannelFilter(itemChannel));
        }
        String currentChannel = cleanText(getCurrentChannelName());
        if (!currentChannel.isEmpty() && !currentChannel.equalsIgnoreCase(itemChannel) && !currentChannel.equalsIgnoreCase(recordingsChannelFilter)) {
            options.add(getString(R.string.recording_action_filter_current_channel, currentChannel));
            actions.add(() -> applyRecordingsChannelFilter(currentChannel));
        }
        if (!recordingsChannelFilter.trim().isEmpty() || !RECORDINGS_DAY_ALL.equals(recordingsDayFilter)) {
            options.add(getString(R.string.recording_action_clear_filters));
            actions.add(this::clearRecordingsFilters);
        }
        options.add(getString(R.string.recording_action_filter_today));
        actions.add(() -> applyRecordingsDayFilter(RECORDINGS_DAY_TODAY));
        options.add(getString(R.string.recording_action_filter_tomorrow));
        actions.add(() -> applyRecordingsDayFilter(RECORDINGS_DAY_TOMORROW));
        options.add(getString(R.string.recording_action_filter_week));
        actions.add(() -> applyRecordingsDayFilter(RECORDINGS_DAY_WEEK));
        if (!RECORDINGS_DAY_ALL.equals(recordingsDayFilter)) {
            options.add(getString(R.string.recording_action_filter_all_days));
            actions.add(() -> applyRecordingsDayFilter(RECORDINGS_DAY_ALL));
        }
    }

    private void applyRecordingsChannelFilter(String channelName) {
        recordingsChannelFilter = cleanText(channelName);
        showStatus(getString(R.string.status_recordings_filter_channel, recordingsChannelFilter));
        refreshRecordingsPanel();
    }

    private void applyRecordingsDayFilter(String dayFilter) {
        recordingsDayFilter = dayFilter == null || dayFilter.trim().isEmpty() ? RECORDINGS_DAY_ALL : dayFilter;
        showStatus(getString(R.string.status_recordings_filter_day, recordingsDayFilterLabel(recordingsDayFilter)));
        refreshRecordingsPanel();
    }

    private void clearRecordingsFilters() {
        recordingsChannelFilter = "";
        recordingsDayFilter = RECORDINGS_DAY_ALL;
        showStatus(getString(R.string.status_recordings_filters_cleared));
        refreshRecordingsPanel();
    }

    private void toggleRecordingsAutoRefresh() {
        recordingsAutoRefreshEnabled = !recordingsAutoRefreshEnabled;
        showStatus(getString(recordingsAutoRefreshEnabled ? R.string.recordings_panel_auto_refresh_on : R.string.recordings_panel_auto_refresh_off));
        scheduleRecordingsAutoRefresh();
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
        int size = channels.size();
        int anchor = currentIndex < 0 ? 0 : currentIndex;
        int next = ((anchor + delta) % size + size) % size;
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

    private ChannelItem findChannelItemById(String channelID) {
        if (channelID == null || channelID.trim().isEmpty()) {
            return null;
        }
        for (ChannelItem item : allChannels) {
            if (item != null && channelID.equals(item.id)) {
                return item;
            }
        }
        return null;
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

    private void persistNavigationState() {
        if (prefs == null) {
            return;
        }
        prefs.edit()
                .putString(PREF_LAST_FILTER_KEY, selectedFilterKey == null || selectedFilterKey.trim().isEmpty() ? "all" : selectedFilterKey)
                .putBoolean(PREF_FAVORITES_ONLY, favoritesOnly)
                .apply();
    }

    private void cycleFilter(int delta) {
        syncOverlayCoordinator();
        ChannelFilter filter = channelOverlayCoordinator.cycleFilter(delta);
        syncOverlayStateFromCoordinator();
        persistNavigationState();
        channelAdapter.notifyDataSetChanged();
        updateFilterText();
        updateOverlaySearchState();

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
        updateOverlaySearchState();
        if (selectedOverlayIndex >= 0) {
            channelList.scrollToPosition(selectedOverlayIndex);
        }
        showOverlay();
    }

    private void toggleFavoriteForChannel(ChannelItem channelItem) {
        if (channelItem == null || channelItem.id == null || channelItem.id.trim().isEmpty()) {
            showStatus(getString(R.string.diagnostics_none));
            return;
        }
        int index = findChannelIndexById(channelItem.id);
        if (index >= 0) {
            selectedOverlayIndex = index;
            toggleFavoriteSelected();
            return;
        }
        boolean added;
        if (favoriteChannelIds.contains(channelItem.id)) {
            favoriteChannelIds.remove(channelItem.id);
            channelItem.favorite = false;
            favoriteOrderStore.remove(channelItem.id);
            added = false;
        } else {
            favoriteChannelIds.add(channelItem.id);
            channelItem.favorite = true;
            favoriteOrderStore.addIfMissing(channelItem.id);
            added = true;
        }
        for (ChannelItem item : allChannels) {
            if (item != null && channelItem.id.equals(item.id)) {
                item.favorite = channelItem.favorite;
            }
        }
        saveFavorites();
        channelAdapter.notifyDataSetChanged();
        updateOverlaySearchState();
        showStatus(getString(added ? R.string.status_favorite_added : R.string.status_favorite_removed));
    }

    private void toggleFavoriteChannel(ChannelItem channel) {
        if (channel == null || channel.id == null || channel.id.trim().isEmpty()) {
            return;
        }
        boolean added;
        if (favoriteChannelIds.contains(channel.id)) {
            favoriteChannelIds.remove(channel.id);
            favoriteOrderStore.remove(channel.id);
            added = false;
        } else {
            favoriteChannelIds.add(channel.id);
            favoriteOrderStore.addIfMissing(channel.id);
            added = true;
        }
        saveFavorites();
        if (channelAdapter != null) {
            channelAdapter.notifyDataSetChanged();
        }
        updateOverlaySearchState();
        showStatus(getString(added ? R.string.status_favorite_added : R.string.status_favorite_removed));
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
        updateOverlaySearchState();

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

    private boolean isMultiViewVisible() {
        return multiViewContainer != null && multiViewContainer.getVisibility() == View.VISIBLE;
    }

    private void showOverlay() {
        clearQuickSearchOverlay();
        hideRecordingsPanel();
        closeMultiView();
        if (touchDeviceMode) {
            if (touchControlsController != null) {
                touchControlsController.cancelTimers();
            }
            if (touchControlsBar != null) touchControlsBar.setVisibility(View.GONE);
            if (touchHomeHub != null) touchHomeHub.setVisibility(View.GONE);
            if (timeshiftBarContainer != null) timeshiftBarContainer.setVisibility(View.GONE);
        }
        updateOverlayPanel();
        updateOverlaySearchState();
        channelOverlayCoordinator.showOverlay(channelOverlay, uiHandler, hideOverlayRunnable, touchDeviceMode ? 0L : OVERLAY_HIDE_MS);
    }

    private void hideOverlay() {
        uiHandler.removeCallbacks(hideOverlayRunnable);
        clearOverlaySearchQuery();
        channelOverlayCoordinator.hideOverlay(channelOverlay);
        if (touchDeviceMode) {
            if (touchControlsController != null) {
                touchControlsController.cancelTimers();
            }
            if (touchControlsBar != null) touchControlsBar.setVisibility(View.GONE);
            if (touchHomeHub != null) touchHomeHub.setVisibility(View.GONE);
            if (timeshiftBarContainer != null) timeshiftBarContainer.setVisibility(View.GONE);
        }
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
        closeMultiView();
        if (touchControlsBar != null) {
            touchControlsBar.setVisibility(View.GONE);
        }
        if (touchHomeHub != null) {
            touchHomeHub.setVisibility(View.GONE);
        }
        if (timeshiftBarContainer != null) {
            timeshiftBarContainer.setVisibility(View.GONE);
        }
        recordingsController.applyResult(result, preferredId);
        Log.d(TAG, "showRecordingsPanel scheduled=" + result.scheduledMode + " count=" + result.items.size());
        recordingsAdapter = new RecordingsAdapter(result);
        recordingsRecyclerView.setAdapter(recordingsAdapter);
        recordingsRecyclerView.scrollToPosition(recordingsController.getSelectedIndex());
        updateRecordingsDetailPanel();
        recordingsPanel.setVisibility(View.VISIBLE);
        scheduleRecordingsAutoRefresh();
        Log.d(TAG, "recordingsPanel visible=" + (recordingsPanel.getVisibility() == View.VISIBLE));
    }

    private void hideRecordingsPanel() {
        uiHandler.removeCallbacks(recordingsAutoRefreshRunnable);
        recordingsController.clearCurrentResult();
        if (recordingsPanel != null) {
            recordingsPanel.setVisibility(View.GONE);
        }
        updateRecordingsDetailPanel();
    }

    private void scheduleRecordingsAutoRefresh() {
        uiHandler.removeCallbacks(recordingsAutoRefreshRunnable);
        if (recordingsAutoRefreshEnabled && isRecordingsPanelVisible()) {
            uiHandler.postDelayed(recordingsAutoRefreshRunnable, RECORDINGS_AUTO_REFRESH_MS);
        }
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
        List<ChannelItem> results = new ArrayList<>();
        for (ChannelItem item : allChannels) {
            if (query == null || query.trim().isEmpty()) {
                results.add(item);
            } else {
                String haystack = displayName(item) + " " + item.group + " " + item.platformName + " " + joinLabels(item.customGroups);
                if (matchesSearch(haystack, query)) {
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
        if (recordingsController.moveSelection(delta) == null) {
            return;
        }
        if (recordingsAdapter != null) {
            recordingsAdapter.notifyDataSetChanged();
        }
        if (recordingsRecyclerView != null) {
            recordingsRecyclerView.scrollToPosition(recordingsController.getSelectedIndex());
        }
        updateRecordingsDetailPanel();
    }

    private void playSelectedRecording() {
        RecordingsRepository.RecordingsResult result = recordingsController.getCurrentResult();
        RecordingsRepository.RecordingItem item = recordingsController.getSelectedItem();
        if (result == null || item == null) {
            return;
        }
        if (!item.playable) {
            showRecordingActionsDialog();
            return;
        }
        playRecording(item, result.basePath);
    }

    private void updateRecordingsDetailPanel() {
        if (recordingsSectionText == null || recordingsSummaryText == null || recordingDetailPosterImage == null || recordingDetailTitleText == null || recordingDetailMetaText == null || recordingDetailPathText == null || recordingDetailActionText == null) {
            return;
        }
        updateRecordingsPanelButtons();
        RecordingsRepository.RecordingsResult result = recordingsController.getCurrentResult();
        if (result == null || result.items.isEmpty()) {
            recordingsSectionText.setText(getString(recordingsController.isScheduledMode() ? R.string.title_recordings_scheduled : R.string.title_recordings_completed));
            recordingsSummaryText.setText(buildRecordingsSummary(result));
            recordingDetailTitleText.setText(getString(R.string.recordings_detail_empty));
            recordingDetailMetaText.setText("");
            recordingDetailMetaText.setTextColor(0xFFF2D5AF);
            recordingDetailPathText.setText("");
            recordingDetailActionText.setText(getString(recordingsController.isScheduledMode() ? R.string.recordings_panel_action_hint_scheduled : R.string.recordings_panel_action_hint));
            recordingDetailPathText.setVisibility(View.GONE);
            recordingDetailPosterImage.setVisibility(View.GONE);
            Glide.with(this).clear(recordingDetailPosterImage);
            return;
        }
        RecordingsRepository.RecordingItem item = recordingsController.getSelectedItem();
        recordingsSectionText.setText(getString(result.scheduledMode ? R.string.title_recordings_scheduled : R.string.title_recordings_completed));
        recordingsSummaryText.setText(buildRecordingsSummary(result));
        recordingDetailTitleText.setText(buildRecordingTitle(item));
        recordingDetailMetaText.setText(buildRecordingMeta(item));
        recordingDetailMetaText.setTextColor(recordingMetaColor(item));
        if (item.playable) {
            recordingDetailPathText.setVisibility(View.VISIBLE);
            recordingDetailPathText.setText(getString(R.string.recordings_path, item.path == null ? "" : item.path));
        } else {
            recordingDetailPathText.setVisibility(View.GONE);
            recordingDetailPathText.setText("");
        }
        recordingDetailActionText.setText(getString(result.scheduledMode ? R.string.recordings_panel_action_hint_scheduled : R.string.recordings_panel_action_hint));
        bindRecordingPoster(recordingDetailPosterImage, item.poster);
    }

    private void updateRecordingsPanelButtons() {
        if (recordingsCompletedButton != null) {
            recordingsCompletedButton.setBackgroundTintList(ColorStateList.valueOf(recordingsController.isScheduledMode() ? 0xFF2B3642 : 0xFF2A7C86));
        }
        if (recordingsScheduledButton != null) {
            recordingsScheduledButton.setBackgroundTintList(ColorStateList.valueOf(recordingsController.isScheduledMode() ? 0xFF2A7C86 : 0xFF2B3642));
        }
        if (recordingsRefreshButton != null) {
            recordingsRefreshButton.setBackgroundTintList(ColorStateList.valueOf(0xFF2B3642));
        }
        if (recordingsHintText != null) {
            recordingsHintText.setText(buildRecordingsHint());
        }
    }

    private String buildRecordingsHint() {
        String base = touchDeviceMode ? getString(R.string.recordings_panel_hint_touch) : getString(R.string.recordings_panel_hint);
        String filterLabel = buildRecordingsFilterLabel();
        if (filterLabel.isEmpty()) {
            return base;
        }
        return base + "\n" + filterLabel;
    }

    private String buildRecordingsFilterLabel() {
        List<String> labels = new ArrayList<>();
        if (!recordingsChannelFilter.trim().isEmpty()) {
            labels.add(getString(R.string.recordings_filter_channel_label, recordingsChannelFilter));
        }
        if (!RECORDINGS_DAY_ALL.equals(recordingsDayFilter)) {
            labels.add(getString(R.string.recordings_filter_day_label, recordingsDayFilterLabel(recordingsDayFilter)));
        }
        if (labels.isEmpty()) {
            return "";
        }
        return TextUtils.join("  ·  ", labels);
    }

    private RecordingsRepository.RecordingsResult filterRecordingsResult(RecordingsRepository.RecordingsResult result) {
        if (result == null || result.items == null || result.items.isEmpty()) {
            return result;
        }
        boolean hasChannelFilter = !recordingsChannelFilter.trim().isEmpty();
        boolean hasDayFilter = !RECORDINGS_DAY_ALL.equals(recordingsDayFilter);
        long nowMs = System.currentTimeMillis();
        List<RecordingsRepository.RecordingItem> filtered = new ArrayList<>();
        for (RecordingsRepository.RecordingItem item : result.items) {
            if (item == null) {
                continue;
            }
            if (hasChannelFilter && !channelMatchesRecordingFilter(item)) {
                continue;
            }
            if (hasDayFilter && !dayMatchesRecordingFilter(item, recordingsDayFilter, nowMs)) {
                continue;
            }
            filtered.add(item);
        }
        filtered.sort((left, right) -> {
            int leftBucket = recordingStatusSortBucket(left);
            int rightBucket = recordingStatusSortBucket(right);
            if (leftBucket != rightBucket) {
                return Integer.compare(leftBucket, rightBucket);
            }
            long leftTime = recordingTimeMillis(left);
            long rightTime = recordingTimeMillis(right);
            if (leftTime <= 0L && rightTime <= 0L) {
                return buildRecordingTitle(left).compareToIgnoreCase(buildRecordingTitle(right));
            }
            if (leftTime <= 0L) {
                return 1;
            }
            if (rightTime <= 0L) {
                return -1;
            }
            return result.scheduledMode ? Long.compare(leftTime, rightTime) : Long.compare(rightTime, leftTime);
        });
        return new RecordingsRepository.RecordingsResult(result.basePath, filtered, result.scheduledMode);
    }

    private int recordingStatusSortBucket(RecordingsRepository.RecordingItem item) {
        if (item == null || item.playable) {
            return 0;
        }
        switch (safeLower(item.status)) {
            case "recording":
            case "running":
            case "in_progress":
                return 0;
            case "scheduled":
            case "pending":
                return 1;
            case "failed":
            case "error":
            case "cancelled":
            case "canceled":
                return 2;
            default:
                return 3;
        }
    }

    private boolean channelMatchesRecordingFilter(RecordingsRepository.RecordingItem item) {
        String channel = cleanText(item == null ? "" : item.channelName);
        if (channel.isEmpty()) {
            return false;
        }
        return channel.equalsIgnoreCase(recordingsChannelFilter.trim());
    }

    private boolean dayMatchesRecordingFilter(RecordingsRepository.RecordingItem item, String filter, long nowMs) {
        long itemTime = recordingTimeMillis(item);
        if (itemTime <= 0L) {
            return false;
        }
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMs);
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(itemTime);
        if (RECORDINGS_DAY_TODAY.equals(filter)) {
            return isSameDay(now, target);
        }
        if (RECORDINGS_DAY_TOMORROW.equals(filter)) {
            now.add(Calendar.DAY_OF_YEAR, 1);
            return isSameDay(now, target);
        }
        if (RECORDINGS_DAY_WEEK.equals(filter)) {
            long weekEndMs = nowMs + (7L * 24L * 60L * 60L * 1000L);
            return itemTime >= startOfDayMillis(nowMs) && itemTime <= weekEndMs;
        }
        return true;
    }

    private boolean isSameDay(Calendar left, Calendar right) {
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR)
                && left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR);
    }

    private long recordingTimeMillis(RecordingsRepository.RecordingItem item) {
        if (item == null) {
            return 0L;
        }
        long start = parseIsoMillis(item.startTime);
        if (start > 0L) {
            return start;
        }
        return parseIsoMillis(item.modified);
    }

    private long startOfDayMillis(long value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(value);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private String recordingsDayFilterLabel(String filter) {
        if (RECORDINGS_DAY_TODAY.equals(filter)) {
            return getString(R.string.recordings_day_today);
        }
        if (RECORDINGS_DAY_TOMORROW.equals(filter)) {
            return getString(R.string.recordings_day_tomorrow);
        }
        if (RECORDINGS_DAY_WEEK.equals(filter)) {
            return getString(R.string.recordings_day_week);
        }
        return getString(R.string.recordings_day_all);
    }

    private void showHdrBadge(String label) {
        if (hdrBadgeText == null) {
            return;
        }
        hdrBadgeText.setText(label == null || label.trim().isEmpty() ? getString(R.string.status_hdr_detected) : label.trim());
        hdrBadgeText.setVisibility(View.VISIBLE);
        uiHandler.removeCallbacks(hideHdrBadgeRunnable);
        uiHandler.postDelayed(hideHdrBadgeRunnable, 2000L);
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
        ChannelItem current = getCurrentPlaybackChannelItem();
        if (current != null && current.isVod) {
            errorText.setVisibility(View.VISIBLE);
            errorText.setText(getString(
                    R.string.error_vod_playback_details,
                    reason == null ? getString(R.string.error_unknown_reason) : reason,
                    displayName(current)
            ));
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

    private RemoteInputRouter.Host createRemoteInputHost() {
        return new RemoteInputRouter.Host() {
            @Override
            public boolean isRecordingsPanelVisible() {
                return MainActivity.this.isRecordingsPanelVisible();
            }

            @Override
            public boolean isQuickSearchVisible() {
                return MainActivity.this.isQuickSearchVisible();
            }

            @Override
            public boolean isMultiViewVisible() {
                return MainActivity.this.isMultiViewVisible();
            }

            @Override
            public boolean isOverlayVisible() {
                return MainActivity.this.isOverlayVisible();
            }

            @Override
            public boolean isTvTimeshiftHudActive() {
                return MainActivity.this.isTvTimeshiftHudActive();
            }

            @Override
            public boolean canResumeTimeshiftLive() {
                return playerController != null && playerController.resumeTimeshiftLive();
            }

            @Override
            public boolean canSeekTimeshiftBack() {
                return playerController != null && playerController.seekTimeshiftBack();
            }

            @Override
            public boolean canSeekTimeshiftForward() {
                return playerController != null && playerController.seekTimeshiftForward();
            }

            @Override
            public boolean isPlayingRecordingWithReturnTarget() {
                return playerController != null && playerController.isPlayingRecording() && currentPlaybackRecordingId != null;
            }

            @Override
            public boolean hasSeekablePlayback() {
                return playerController != null && playerController.getPlaybackSeekState() != null;
            }

            @Override
            public boolean isTouchDeviceMode() {
                return touchDeviceMode;
            }

            @Override
            public boolean hasSelectedOverlayChannel() {
                return selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size();
            }

            @Override
            public boolean hasCurrentChannel() {
                return currentIndex >= 0 && currentIndex < channels.size();
            }

            @Override
            public int getMultiViewActiveIndex() {
                return multiViewActiveIndex;
            }

            @Override
            public void handleQuickSearchCharacter(char value) {
                MainActivity.this.handleQuickSearchCharacter(value);
            }

            @Override
            public void clearQuickSearchOverlay() {
                MainActivity.this.clearQuickSearchOverlay();
            }

            @Override
            public void switchRecordingsMode(boolean scheduledMode) {
                MainActivity.this.switchRecordingsMode(scheduledMode);
            }

            @Override
            public void showChannelActionMenu() {
                MainActivity.this.showChannelActionMenu();
            }

            @Override
            public void openTimelineGuideAroundSelection() {
                MainActivity.this.openTimelineGuideAroundSelection();
            }

            @Override
            public void openTimelineGuideForCurrentChannel() {
                MainActivity.this.openTimelineGuide(currentIndex, System.currentTimeMillis());
            }

            @Override
            public void showOverlay() {
                MainActivity.this.showOverlay();
            }

            @Override
            public void showV12ToolsMenu() {
                MainActivity.this.showV12ToolsMenu();
            }

            @Override
            public void hideRecordingsPanel() {
                MainActivity.this.hideRecordingsPanel();
            }

            @Override
            public void hideTvTimeshiftHud() {
                MainActivity.this.hideTvTimeshiftHud();
            }

            @Override
            public void showLeaveRecordingPrompt() {
                MainActivity.this.showLeaveRecordingPrompt();
            }

            @Override
            public void hideOverlay() {
                MainActivity.this.hideOverlay();
            }

            @Override
            public void finishActivity() {
                MainActivity.this.finish();
            }

            @Override
            public void moveQuickSearchSelection(int delta) {
                MainActivity.this.moveQuickSearchSelection(delta);
            }

            @Override
            public void moveRecordingsSelection(int delta) {
                MainActivity.this.moveRecordingsSelection(delta);
            }

            @Override
            public void tuneRelative(int delta) {
                MainActivity.this.tuneRelative(delta);
            }

            @Override
            public void showTimeshiftHudTemporarily() {
                MainActivity.this.showTimeshiftHudTemporarily();
            }

            @Override
            public void moveOverlaySelection(int delta) {
                MainActivity.this.moveOverlaySelection(delta);
            }

            @Override
            public void cycleFilter(int delta) {
                MainActivity.this.cycleFilter(delta);
            }

            @Override
            public void tuneQuickSearchSelection() {
                MainActivity.this.tuneQuickSearchSelection();
            }

            @Override
            public void playSelectedRecording() {
                MainActivity.this.playSelectedRecording();
            }

            @Override
            public void tuneOverlaySelectionAndHide() {
                MainActivity.this.tuneToIndex(selectedOverlayIndex, true);
                MainActivity.this.hideOverlay();
            }

            @Override
            public void togglePlayback() {
                if (playerController != null) {
                    playerController.togglePlayback();
                }
            }

            @Override
            public void showChannelSearchDialog() {
                MainActivity.this.showChannelSearchDialog();
            }

            @Override
            public void showRecordingActionsDialog() {
                MainActivity.this.showRecordingActionsDialog();
            }

            @Override
            public void deleteQuickSearchCharacter() {
                MainActivity.this.deleteQuickSearchCharacter();
            }

            @Override
            public void scheduleSelectedOrCurrentProgram() {
                if (MainActivity.this.isOverlayVisible() && selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size()) {
                    MainActivity.this.createScheduleFromEndpoint(channels.get(selectedOverlayIndex), false);
                } else if (currentIndex >= 0 && currentIndex < channels.size()) {
                    MainActivity.this.createScheduleFromEndpoint(channels.get(currentIndex), false);
                }
            }

            @Override
            public void closeMultiView() {
                MainActivity.this.closeMultiView();
            }

            @Override
            public void moveMultiViewSelection(int columnDelta, int rowDelta) {
                MainActivity.this.moveMultiViewSelection(columnDelta, rowDelta);
            }

            @Override
            public void focusMultiViewSlot(int slot) {
                MainActivity.this.focusMultiViewSlot(slot);
            }

            @Override
            public void showMultiViewChannelPicker(int slot) {
                MainActivity.this.showMultiViewChannelPicker(slot);
            }
        };
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (remoteInputRouter != null && remoteInputRouter.dispatchKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, @NonNull KeyEvent event) {
        if (remoteInputRouter != null && remoteInputRouter.onKeyLongPress(keyCode)) {
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
        rememberCurrentVodPosition();
        rememberCurrentRecordingPosition();
        if (touchControlsController != null) {
            touchControlsController.cancelTimers();
        }
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
                displayName(channelItem),
                channelItem.platformName,
                channelItem.playUrl,
                channelItem.fallbackPlayUrl,
                resolvePlaybackModeForRequest(channelItem),
                channelItem.drmScheme,
                channelItem.drmLicenseUrl,
                channelItem.directPlayback
        );
    }

    private String resolvePlaybackModeForRequest(ChannelItem channelItem) {
        if (channelItem == null || channelItem.id == null) {
            return PlaybackModeStore.MODE_AUTO;
        }
        if (temporaryPlaybackModesByChannelId.containsKey(channelItem.id)) {
            return sanitizePlaybackMode(temporaryPlaybackModesByChannelId.get(channelItem.id));
        }
        String permanentMode = playbackModeStore == null ? PlaybackModeStore.MODE_AUTO : playbackModeStore.getMode(channelItem.id);
        if (!PlaybackModeStore.MODE_AUTO.equals(permanentMode)) {
            return permanentMode;
        }
        if (BuildConfig.STANDALONE_MODE) {
            return PlaybackModeStore.MODE_AUTO;
        }
        if (!playbackRepairEnabled) {
            return PlaybackModeStore.MODE_AUTO;
        }
        return sanitizePlaybackMode(learnedPlaybackModesByChannelId.get(channelItem.id));
    }

    private ChannelItem getCurrentPlaybackChannelItem() {
        if (currentIndex >= 0 && currentIndex < channels.size()) {
            return channels.get(currentIndex);
        }
        return findChannelItemById(lastChannelId);
    }

    private void retryCurrentPlayback() {
        ChannelItem channelItem = getCurrentPlaybackChannelItem();
        if (channelItem == null) {
            showStatus(getString(R.string.diagnostics_none));
            return;
        }
        showStatus(getString(R.string.status_retry_channel, displayName(channelItem)));
        currentIndex = findChannelIndexById(channelItem.id);
        if (currentIndex >= 0) {
            selectedOverlayIndex = currentIndex;
            channelAdapter.notifyDataSetChanged();
            tuneToIndex(currentIndex, true);
            return;
        }
        playChannelItem(channelItem, true);
    }

    private void showCurrentChannelPlaybackModeDialog() {
        ChannelItem channelItem = getCurrentPlaybackChannelItem();
        if (channelItem == null) {
            showStatus(getString(R.string.diagnostics_none));
            return;
        }
        showPlaybackModeDialog(channelItem);
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
                    ChannelItem currentPlaybackChannel = getCurrentPlaybackChannelItem();
                    if (currentPlaybackChannel != null && channelItem.id.equals(currentPlaybackChannel.id)) {
                        retryCurrentPlayback();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showTemporaryPlaybackModeDialog(ChannelItem channelItem) {
        if (channelItem == null) {
            return;
        }
        String currentMode = temporaryPlaybackModesByChannelId.getOrDefault(channelItem.id, PlaybackModeStore.MODE_AUTO);
        int checkedItem = PlaybackModeStore.MODE_DIRECT.equals(currentMode)
                ? 1
                : (PlaybackModeStore.MODE_PROXY.equals(currentMode) ? 2 : 0);
        String[] options = getResources().getStringArray(R.array.playback_mode_options);
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_playback_mode_temporary)
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    String selectedMode = which == 1
                            ? PlaybackModeStore.MODE_DIRECT
                            : (which == 2 ? PlaybackModeStore.MODE_PROXY : PlaybackModeStore.MODE_AUTO);
                    if (PlaybackModeStore.MODE_AUTO.equals(selectedMode)) {
                        temporaryPlaybackModesByChannelId.remove(channelItem.id);
                    } else {
                        temporaryPlaybackModesByChannelId.put(channelItem.id, selectedMode);
                    }
                    showStatus(getString(R.string.status_playback_mode_temporary_changed, options[which]));
                    dialog.dismiss();
                    ChannelItem currentPlaybackChannel = getCurrentPlaybackChannelItem();
                    if (currentPlaybackChannel != null && channelItem.id.equals(currentPlaybackChannel.id)) {
                        retryCurrentPlayback();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showPersonalListsDialog(ChannelItem channelItem) {
        if (channelItem == null || channelCollectionStore == null) {
            return;
        }
        List<ChannelCollectionStore.ChannelCollection> collections = channelCollectionStore.getCollections();
        String[] labels = new String[collections.size()];
        boolean[] checked = new boolean[collections.size()];
        for (int i = 0; i < collections.size(); i++) {
            ChannelCollectionStore.ChannelCollection collection = collections.get(i);
            labels[i] = collection.label;
            checked[i] = channelCollectionStore.contains(collection.key, channelItem.id);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_personal_lists)
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    for (int i = 0; i < collections.size(); i++) {
                        channelCollectionStore.setMembership(collections.get(i).key, channelItem.id, checked[i]);
                    }
                    refreshLocalChannelFilters(channelItem.id);
                    showStatus(getString(R.string.status_personal_lists_updated));
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showChannelProfileDialog(ChannelItem channelItem) {
        if (channelItem == null || channelProfileStore == null) {
            return;
        }
        boolean hasAlias = channelProfileStore.hasAlias(channelItem.id);
        boolean hidden = channelProfileStore.isHidden(channelItem.id);
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.channel_profile_alias));
        actions.add(() -> showChannelAliasDialog(channelItem));
        if (hasAlias) {
            options.add(getString(R.string.channel_profile_clear_alias));
            actions.add(() -> {
                channelProfileStore.setAlias(channelItem.id, "");
                refreshLocalChannelFilters(channelItem.id);
                showStatus(getString(R.string.status_channel_alias_cleared));
            });
        }
        options.add(getString(R.string.channel_profile_tag));
        actions.add(() -> showChannelTagDialog(channelItem));
        if (channelProfileStore.hasTag(channelItem.id)) {
            options.add(getString(R.string.channel_profile_clear_tag));
            actions.add(() -> {
                channelProfileStore.setTag(channelItem.id, "");
                refreshLocalChannelFilters(channelItem.id);
                showStatus(getString(R.string.status_channel_tag_cleared));
            });
        }
        options.add(getString(hidden ? R.string.channel_profile_unhide : R.string.channel_profile_hide));
        actions.add(() -> {
            channelProfileStore.setHidden(channelItem.id, !hidden);
            refreshLocalChannelFilters(channelItem.id);
            showStatus(getString(hidden ? R.string.status_channel_unhidden : R.string.status_channel_hidden));
        });
        options.add(getString(R.string.channel_profile_startup));
        actions.add(() -> {
            saveLastChannelId(channelItem.id);
            showStatus(getString(R.string.status_channel_startup_set));
        });
        options.add(getString(R.string.menu_playback_mode_temporary));
        actions.add(() -> showTemporaryPlaybackModeDialog(channelItem));
        new AlertDialog.Builder(this)
                .setTitle(displayName(channelItem))
                .setItems(options.toArray(new String[0]), (dialog, which) -> actions.get(which).run())
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showChannelTagDialog(ChannelItem channelItem) {
        if (channelItem == null || channelProfileStore == null) {
            return;
        }
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(R.string.channel_profile_tag_hint);
        input.setText(channelProfileStore.getTag(channelItem.id));
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this)
                .setTitle(R.string.channel_profile_tag)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String value = input.getText() == null ? "" : input.getText().toString();
                    channelProfileStore.setTag(channelItem.id, value);
                    refreshLocalChannelFilters(channelItem.id);
                    showStatus(getString(value.trim().isEmpty() ? R.string.status_channel_tag_cleared : R.string.status_channel_tag_updated));
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showChannelAliasDialog(ChannelItem channelItem) {
        if (channelItem == null || channelProfileStore == null) {
            return;
        }
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(R.string.channel_profile_alias_hint);
        input.setText(channelProfileStore.getDisplayName(channelItem.id, channelItem.name));
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this)
                .setTitle(R.string.channel_profile_alias)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String value = input.getText() == null ? "" : input.getText().toString();
                    channelProfileStore.setAlias(channelItem.id, value);
                    refreshLocalChannelFilters(channelItem.id);
                    showStatus(getString(value.trim().isEmpty() ? R.string.status_channel_alias_cleared : R.string.status_channel_alias_updated));
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void refreshLocalChannelFilters(String selectedId) {
        if (channelOverlayCoordinator == null) {
            return;
        }
        syncOverlayCoordinator();
        channelOverlayCoordinator.refreshLocalFilters();
        String currentId = (currentIndex >= 0 && currentIndex < channels.size()) ? channels.get(currentIndex).id : lastChannelId;
        channelOverlayCoordinator.refreshVisibleChannels(currentId, selectedId == null ? currentId : selectedId);
        syncOverlayStateFromCoordinator();
        channelAdapter.notifyDataSetChanged();
        updateFilterText();
        updateOverlaySearchState();
        if (!channels.isEmpty() && selectedOverlayIndex >= 0) {
            channelList.scrollToPosition(selectedOverlayIndex);
        }
        showOverlay();
    }

    private void focusOverlaySearchInput() {
        if (overlaySearchInput == null) {
            return;
        }
        overlaySearchInput.setVisibility(View.VISIBLE);
        overlaySearchInput.requestFocus();
        overlaySearchInput.setSelection(overlaySearchInput.getText() == null ? 0 : overlaySearchInput.getText().length());
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(overlaySearchInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideOverlaySearchKeyboard() {
        if (overlaySearchInput == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(overlaySearchInput.getWindowToken(), 0);
        }
        overlaySearchInput.clearFocus();
    }

    private void clearOverlaySearchQuery() {
        if (overlaySearchInput == null) {
            syncOverlayCoordinator();
            channelOverlayCoordinator.setSearchQuery("");
            return;
        }
        if (overlaySearchInput.getText() != null && overlaySearchInput.getText().length() > 0) {
            overlaySearchInput.setText("");
        } else {
            syncOverlayCoordinator();
            channelOverlayCoordinator.setSearchQuery("");
        }
        hideOverlaySearchKeyboard();
    }

    private void applyOverlaySearchQuery(String query) {
        syncOverlayCoordinator();
        channelOverlayCoordinator.setSearchQuery(query);
        String currentId = (currentIndex >= 0 && currentIndex < channels.size()) ? channels.get(currentIndex).id : lastChannelId;
        String selectedId = (selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size()) ? channels.get(selectedOverlayIndex).id : currentId;
        channelOverlayCoordinator.refreshVisibleChannels(currentId, selectedId);
        syncOverlayStateFromCoordinator();
        channelAdapter.notifyDataSetChanged();
        updateFilterText();
        updateOverlaySearchState();
        if (!channels.isEmpty() && selectedOverlayIndex >= 0) {
            channelList.scrollToPosition(selectedOverlayIndex);
        }
    }

    private void updateOverlaySearchState() {
        if (overlayEmptyText != null) {
            String query = channelOverlayCoordinator == null ? "" : channelOverlayCoordinator.getSearchQuery();
            boolean hasQuery = query != null && !query.trim().isEmpty();
            overlayEmptyText.setVisibility(channels.isEmpty() ? View.VISIBLE : View.GONE);
            overlayEmptyText.setText(hasQuery
                    ? getString(R.string.overlay_no_results_search, query.trim())
                    : getString(R.string.overlay_no_results));
        }
        if (touchRecentButton != null) {
            touchRecentButton.setText(getString(R.string.overlay_recent_button_count, buildRecentQuickChannels().size()));
        }
        if (touchFavoritesButton != null) {
            int favoriteCount = buildFavoriteQuickChannels().size();
            touchFavoritesButton.setText(getString(
                    favoritesOnly ? R.string.overlay_favorites_button_on_count : R.string.overlay_favorites_button_off_count,
                    favoriteCount));
        }
        updateQuickAccessButtons();
        updateTouchHomeHub();
    }

    private void applyQuickOverlayTarget(String targetKey) {
        showTouchControlsTemporarily();
        if ("grab".equals(targetKey)) {
            openRecordingsBrowser();
            return;
        }
        syncOverlayCoordinator();
        channelOverlayCoordinator.setSearchQuery("");
        channelOverlayCoordinator.setFavoritesOnly(false);
        String filterKey = targetKey;
        if ("tv".equals(targetKey)) {
            filterKey = findPreferredTvFilterKey();
        } else if ("vod".equals(targetKey)) {
            filterKey = findPreferredVodFilterKey(false);
        } else if ("vod-adult".equals(targetKey)) {
            filterKey = findPreferredVodFilterKey(true);
        }
        channelOverlayCoordinator.setSelectedFilterKey(filterKey);
        String currentId = lastChannelId == null ? "" : lastChannelId;
        channelOverlayCoordinator.refreshVisibleChannels(currentId, currentId);
        syncOverlayStateFromCoordinator();
        clearOverlaySearchQuery();
        channelAdapter.notifyDataSetChanged();
        updateFilterText();
        updateOverlaySearchState();
        showOverlay();
    }

    private String findPreferredTvFilterKey() {
        for (ChannelFilter filter : filters) {
            if (filter != null && filter.type == 1) {
                return filter.key;
            }
        }
        for (ChannelFilter filter : filters) {
            if (filter != null && filter.type == 2) {
                return filter.key;
            }
        }
        return "all";
    }

    private String findPreferredVodFilterKey(boolean adult) {
        String fallback = adult ? "vod-adult" : "vod";
        for (ChannelFilter filter : filters) {
            if (filter == null) {
                continue;
            }
            if (adult && filter.type == FILTER_VOD_ADULT) {
                return filter.key;
            }
            if (!adult && filter.type == FILTER_VOD) {
                return filter.key;
            }
        }
        return fallback;
    }

    private boolean shouldShowGenericVodQuickTarget(boolean adult) {
        for (ChannelFilter filter : filters) {
            if (filter == null || filter.key == null) {
                continue;
            }
            if (adult) {
                if (filter.type == FILTER_VOD_ADULT && !"vod-adult".equals(filter.key)) {
                    return false;
                }
            } else {
                if (filter.type == FILTER_VOD && !"vod".equals(filter.key)) {
                    return false;
                }
            }
        }
        return true;
    }

    private int countItemsForQuickTarget(String targetKey) {
        if ("grab".equals(targetKey)) {
            RecordingsRepository.RecordingsResult result = recordingsController.getCurrentResult();
            return result == null || result.items == null ? 0 : result.items.size();
        }
        int total = 0;
        for (ChannelItem item : allChannels) {
            if (item == null) {
                continue;
            }
            if ("vod".equals(targetKey) && item.isVod && !item.isAdultVod) {
                total++;
            } else if ("vod-adult".equals(targetKey) && item.isAdultVod) {
                total++;
            } else if ("tv".equals(targetKey) && !item.isVod) {
                total++;
            } else if ("favorites".equals(targetKey) && item.favorite && !item.isVod) {
                total++;
            }
        }
        return total;
    }

    private void styleQuickAccessButton(TextView view, boolean active, String label) {
        if (view == null) {
            return;
        }
        view.setText(label);
        view.setBackgroundTintList(ColorStateList.valueOf(active ? 0xFF2A7C86 : 0xFF2A3440));
        view.setTextColor(0xFFFFFFFF);
    }

    private void updateQuickAccessButtons() {
        if (!touchDeviceMode) {
            return;
        }
        String activeTvKey = findPreferredTvFilterKey();
        boolean tvActive = !favoritesOnly && (selectedFilterKey == null || selectedFilterKey.equals(activeTvKey) || ("all".equals(selectedFilterKey) && "all".equals(activeTvKey)));
        boolean vodActive = !favoritesOnly && isVodFilterSelected(false);
        boolean adultActive = !favoritesOnly && isVodFilterSelected(true);
        styleQuickAccessButton(quickTvButton, tvActive, getString(R.string.overlay_quick_tv, countItemsForQuickTarget("tv")));
        if (quickVodButton != null) {
            if (shouldShowGenericVodQuickTarget(false)) {
                quickVodButton.setVisibility(View.VISIBLE);
                styleQuickAccessButton(quickVodButton, vodActive, getString(R.string.overlay_quick_vod, countItemsForQuickTarget("vod")));
            } else {
                quickVodButton.setVisibility(View.GONE);
            }
        }
        if (quickAdultButton != null) {
            if (shouldShowGenericVodQuickTarget(true)) {
                quickAdultButton.setVisibility(View.VISIBLE);
                styleQuickAccessButton(quickAdultButton, adultActive, getString(R.string.overlay_quick_adult, countItemsForQuickTarget("vod-adult")));
            } else {
                quickAdultButton.setVisibility(View.GONE);
            }
        }
        styleQuickAccessButton(quickGrabButton, isRecordingsPanelVisible(), getString(R.string.overlay_quick_grab));
    }

    private void updateTouchHomeHub() {
        if (touchHomeHub == null) {
            return;
        }
        boolean visible = touchDeviceMode && touchControlsBar != null && touchControlsBar.getVisibility() == View.VISIBLE && !isOverlayVisible() && !isRecordingsPanelVisible() && !isMultiViewVisible();
        touchHomeHub.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            return;
        }
        if (touchHomeTitleText != null) {
            touchHomeTitleText.setText(getString(R.string.touch_home_title));
        }
        if (touchHomeSubtitleText != null) {
            String label = buildTouchHomeFilterLabel();
            int count = (favoritesOnly || "favorites".equals(selectedFilterKey)) ? buildFavoriteQuickChannels().size() : channels.size();
            String continueLabel = buildTouchHomeContinueLabel();
            String subtitle = getString(R.string.touch_home_subtitle, label, count);
            if (!continueLabel.isEmpty()) {
                subtitle = subtitle + "\n" + continueLabel;
            }
            touchHomeSubtitleText.setText(subtitle);
        }
        styleHomeHubPrimaryButton(touchHomeTvButton, !favoritesOnly && isTvHubActive(), getString(R.string.touch_home_button_tv, countItemsForQuickTarget("tv")));
        if (touchHomeVodButton != null) {
            if (shouldShowGenericVodQuickTarget(false)) {
                touchHomeVodButton.setVisibility(View.VISIBLE);
                styleHomeHubPrimaryButton(touchHomeVodButton, !favoritesOnly && isVodFilterSelected(false), getString(R.string.touch_home_button_vod, countItemsForQuickTarget("vod")));
            } else {
                touchHomeVodButton.setVisibility(View.GONE);
            }
        }
        if (touchHomeAdultButton != null) {
            if (shouldShowGenericVodQuickTarget(true)) {
                touchHomeAdultButton.setVisibility(View.VISIBLE);
                styleHomeHubPrimaryButton(touchHomeAdultButton, !favoritesOnly && isVodFilterSelected(true), getString(R.string.touch_home_button_adult, countItemsForQuickTarget("vod-adult")));
            } else {
                touchHomeAdultButton.setVisibility(View.GONE);
            }
        }
        styleHomeHubPrimaryButton(touchHomeGrabButton, false, getString(R.string.touch_home_button_grab));
        styleHomeHubSecondaryButton(touchHomeRecentButton, false, getString(R.string.touch_home_button_recent, buildRecentQuickChannels().size()));
        styleHomeHubSecondaryButton(touchHomeFavoritesButton, favoritesOnly || "favorites".equals(selectedFilterKey), getString(R.string.touch_home_button_favorites, buildFavoriteQuickChannels().size()));
        styleHomeHubSecondaryButton(touchHomeListButton, false, getString(R.string.touch_home_button_list));
        styleHomeHubSecondaryButton(touchHomeMultiButton, isMultiViewVisible(), getString(R.string.touch_home_button_multi));
    }

    private String buildTouchHomeContinueLabel() {
        ChannelItem lastVod = findChannelItemById(lastVodId);
        if (lastVod != null && lastVod.isVod) {
            long resumeMs = getVodResumePosition(lastVod.id);
            if (resumeMs > 30_000L) {
                return getString(R.string.touch_home_continue_vod, displayName(lastVod), formatDurationShort(resumeMs));
            }
        }
        if (!recordingResumePositions.isEmpty()) {
            return getString(R.string.touch_home_continue_recording_count, recordingResumePositions.size());
        }
        return "";
    }

    private boolean isTvHubActive() {
        String activeTvKey = findPreferredTvFilterKey();
        return selectedFilterKey == null || selectedFilterKey.equals(activeTvKey) || ("all".equals(selectedFilterKey) && "all".equals(activeTvKey));
    }

    private boolean isVodFilterSelected(boolean adult) {
        if (selectedFilterKey == null || selectedFilterKey.trim().isEmpty()) {
            return false;
        }
        for (ChannelFilter filter : filters) {
            if (filter == null || !selectedFilterKey.equals(filter.key)) {
                continue;
            }
            return adult ? filter.type == FILTER_VOD_ADULT : filter.type == FILTER_VOD;
        }
        return adult ? "vod-adult".equals(selectedFilterKey) : "vod".equals(selectedFilterKey);
    }

    private String buildTouchHomeFilterLabel() {
        if (favoritesOnly || "favorites".equals(selectedFilterKey)) {
            return getString(R.string.touch_home_filter_favorites);
        }
        if (selectedFilterKey == null || selectedFilterKey.trim().isEmpty() || "all".equals(selectedFilterKey)) {
            return getString(R.string.touch_home_filter_all);
        }
        for (ChannelFilter filter : filters) {
            if (filter != null && selectedFilterKey.equals(filter.key) && filter.label != null && !filter.label.trim().isEmpty()) {
                return filter.label.trim();
            }
        }
        return getString(R.string.touch_home_filter_all);
    }

    private void styleHomeHubPrimaryButton(TextView view, boolean active, String label) {
        styleHomeHubButton(view, active, label, 0xFF244252, 0xFF1F90A2);
    }

    private void styleHomeHubSecondaryButton(TextView view, boolean active, String label) {
        styleHomeHubButton(view, active, label, 0xFF4C3427, 0xFFB46B29);
    }

    private void styleHomeHubButton(TextView view, boolean active, String label, int inactiveColor, int activeColor) {
        if (view == null) {
            return;
        }
        view.setText(label);
        view.setBackgroundTintList(ColorStateList.valueOf(active ? activeColor : inactiveColor));
        view.setTextColor(0xFFFFFFFF);
    }

    private void openMultiView() {
        openMultiView(buildMultiViewChannels());
    }

    private void openMultiView(List<ChannelItem> selected) {
        if (selected == null || selected.size() < 2) {
            showStatus(getString(R.string.status_multiview_not_enough_channels));
            return;
        }
        hideOverlay();
        hideRecordingsPanel();
        clearQuickSearchOverlay();
        if (touchControlsBar != null) {
            touchControlsBar.setVisibility(View.GONE);
        }
        if (touchHomeHub != null) {
            touchHomeHub.setVisibility(View.GONE);
        }
        if (timeshiftBarContainer != null) {
            timeshiftBarContainer.setVisibility(View.GONE);
        }
        if (multiViewHintText != null) {
            multiViewHintText.setText(touchDeviceMode
                    ? getString(R.string.multiview_hint_touch)
                    : getString(R.string.multiview_hint_tv));
        }
        mainWasPlayingBeforeMultiView = playerController != null && playerController.isPlaying();
        if (playerController != null) {
            playerController.setMuted(true);
            playerController.setPlayWhenReady(false);
        }
        ensureMultiViewControllers();
        multiViewChannels.clear();
        multiViewChannels.addAll(selected);
        for (int i = 0; i < multiViewChannelIds.length; i++) {
            multiViewChannelIds[i] = i < selected.size() ? selected.get(i).id : null;
        }
        multiViewActiveIndex = 0;
        for (int i = 0; i < multiTiles.length; i++) {
            if (multiTiles[i] == null || multiLabels[i] == null) {
                continue;
            }
            if (i < selected.size()) {
                ChannelItem item = selected.get(i);
                multiTiles[i].setVisibility(View.VISIBLE);
                multiLabels[i].setText(item.name);
                PlayerController controller = multiPlayerControllers.get(i);
                controller.setMuted(i != multiViewActiveIndex);
                PlayerController.PlaybackRequest request = toPlaybackRequest(item);
                PlayerController.StreamInfo cachedStreamInfo = streamInfoByChannelId.get(item.id);
                controller.playChannel(request, true, cachedStreamInfo);
                if (request != null && !request.directPlayback) {
                    controller.resolveStreamInfoAndReplayIfNeeded(request, true, streamInfoByChannelId);
                }
            } else {
                multiTiles[i].setVisibility(View.INVISIBLE);
                multiLabels[i].setText("");
            }
        }
        updateMultiViewFocus();
        if (multiViewContainer != null) {
            multiViewContainer.setVisibility(View.VISIBLE);
        }
        showStatus(getString(R.string.multiview_title));
    }

    private void closeMultiView() {
        if (multiViewContainer != null && multiViewContainer.getVisibility() != View.VISIBLE) {
            return;
        }
        if (multiViewContainer != null) {
            multiViewContainer.setVisibility(View.GONE);
        }
        releaseMultiViewControllers();
        multiViewChannels.clear();
        for (int i = 0; i < multiViewChannelIds.length; i++) {
            multiViewChannelIds[i] = null;
        }
        if (playerController != null) {
            playerController.setMuted(false);
            playerController.setPlayWhenReady(mainWasPlayingBeforeMultiView);
        }
        mainWasPlayingBeforeMultiView = false;
        updateTouchHomeHub();
    }

    private void ensureMultiViewControllers() {
        if (!multiPlayerControllers.isEmpty()) {
            return;
        }
        for (int i = 0; i < multiPlayerViews.length; i++) {
            final int slot = i;
            PlayerView mv = multiPlayerViews[i];
            if (mv == null) {
                continue;
            }
            PlayerController controller = new PlayerController(this, mv, baseUrl, ioExecutor, uiHandler, new PlayerController.Host() {
                @Override
                public void showStatus(String text) {
                }

                @Override
                public void showError(String text) {
                }

                @Override
                public void hideError() {
                }

                @Override
                public boolean isChannelCurrent(String channelId) {
                    return channelId != null && channelId.equals(multiViewChannelIds[slot]);
                }

                @Override
                public void showHdrBadge(String label) {
                }

                @Override
                public boolean isPlaybackRepairEnabled() {
                    return MainActivity.this.playbackRepairEnabled;
                }

                @Override
                public void recordPlaybackError(PlayerController.PlaybackRequest request, PlayerController.PlaybackDiagnostics diagnostics) {
                    MainActivity.this.recordPlaybackError(request, diagnostics);
                }

                @Override
                public void onFirstVideoFrameRendered(String channelId) {
                }
            });
            controller.initialize();
            multiPlayerControllers.add(controller);
        }
    }

    private void releaseMultiViewControllers() {
        for (PlayerController controller : multiPlayerControllers) {
            if (controller != null) {
                controller.release();
            }
        }
        multiPlayerControllers.clear();
    }

    private List<ChannelItem> buildMultiViewChannels() {
        List<ChannelItem> selected = new ArrayList<>();
        Set<String> added = new HashSet<>();
        if (currentIndex >= 0 && currentIndex < channels.size()) {
            ChannelItem current = channels.get(currentIndex);
            if (current != null && !current.isVod && current.id != null && added.add(current.id)) {
                selected.add(current);
            }
        }
        for (ChannelItem item : channels) {
            if (selected.size() >= 4) {
                break;
            }
            if (item == null || item.isVod || item.id == null || !added.add(item.id)) {
                continue;
            }
            selected.add(item);
        }
        if (selected.size() < 4) {
            for (ChannelItem item : allChannels) {
                if (selected.size() >= 4) {
                    break;
                }
                if (item == null || item.isVod || item.id == null || !added.add(item.id)) {
                    continue;
                }
                selected.add(item);
            }
        }
        return selected;
    }

    private List<ChannelItem> buildCurrentMultiViewSelectionForSave() {
        List<ChannelItem> selected = new ArrayList<>();
        Set<String> added = new HashSet<>();
        if (isMultiViewVisible()) {
            for (ChannelItem item : multiViewChannels) {
                if (item != null && item.id != null && added.add(item.id)) {
                    selected.add(item);
                }
            }
        }
        if (selected.size() >= 2) {
            return selected;
        }
        return buildMultiViewChannels();
    }

    private String getMultiViewPresetKey(int presetIndex) {
        return PREF_MULTIVIEW_PRESET_PREFIX + presetIndex;
    }

    private List<String> getMultiViewPresetIds(int presetIndex) {
        List<String> ids = new ArrayList<>();
        if (prefs == null) {
            return ids;
        }
        String raw = prefs.getString(getMultiViewPresetKey(presetIndex), "");
        if (raw == null || raw.trim().isEmpty()) {
            return ids;
        }
        for (String part : raw.split("\\|")) {
            if (part == null) {
                continue;
            }
            String id = part.trim();
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return ids;
    }

    private List<ChannelItem> resolveMultiViewPreset(int presetIndex) {
        List<ChannelItem> result = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (String id : getMultiViewPresetIds(presetIndex)) {
            ChannelItem item = findChannelItemById(id);
            if (item != null && !item.isVod && item.id != null && added.add(item.id)) {
                result.add(item);
            }
        }
        return result;
    }

    private void saveMultiViewPreset(int presetIndex, List<ChannelItem> items) {
        if (prefs == null) {
            return;
        }
        List<String> ids = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (ChannelItem item : items) {
            if (item != null && item.id != null && !item.id.trim().isEmpty() && added.add(item.id)) {
                ids.add(item.id);
            }
        }
        prefs.edit().putString(getMultiViewPresetKey(presetIndex), String.join("|", ids)).apply();
    }

    private String buildMultiViewPresetLabel(int presetIndex) {
        int count = resolveMultiViewPreset(presetIndex).size();
        return getString(R.string.multiview_preset_label, presetIndex + 1, count);
    }

    private void showSaveMultiViewPresetDialog() {
        List<ChannelItem> source = buildCurrentMultiViewSelectionForSave();
        if (source.size() < 2) {
            showStatus(getString(R.string.status_multiview_not_enough_channels));
            return;
        }
        String[] labels = new String[MULTIVIEW_PRESET_COUNT];
        for (int i = 0; i < MULTIVIEW_PRESET_COUNT; i++) {
            labels[i] = buildMultiViewPresetLabel(i);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.multiview_save_preset_title)
                .setItems(labels, (dialog, which) -> {
                    saveMultiViewPreset(which, source);
                    showStatus(getString(R.string.status_multiview_preset_saved, which + 1));
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showOpenMultiViewPresetDialog() {
        String[] labels = new String[MULTIVIEW_PRESET_COUNT];
        for (int i = 0; i < MULTIVIEW_PRESET_COUNT; i++) {
            labels[i] = buildMultiViewPresetLabel(i);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.multiview_open_preset_title)
                .setItems(labels, (dialog, which) -> {
                    List<ChannelItem> preset = resolveMultiViewPreset(which);
                    if (preset.size() < 2) {
                        showStatus(getString(R.string.status_multiview_preset_empty));
                        return;
                    }
                    openMultiView(preset);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showMultiViewChannelPicker(int slot) {
        if (slot < 0 || slot >= multiViewChannels.size()) {
            return;
        }
        List<ChannelItem> items = buildSelectableMultiViewChannels(slot);
        if (items.isEmpty()) {
            showStatus(getString(R.string.status_multiview_not_enough_channels));
            return;
        }
        showQuickChannelListDialog(
                getString(R.string.multiview_select_channel_title),
                items,
                getString(R.string.overlay_no_results),
                item -> tuneMultiViewSlotChannel(slot, item)
        );
    }

    private List<ChannelItem> buildSelectableMultiViewChannels(int slot) {
        List<ChannelItem> items = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (int i = 0; i < multiViewChannels.size(); i++) {
            if (i == slot) {
                continue;
            }
            ChannelItem existing = multiViewChannels.get(i);
            if (existing != null && existing.id != null) {
                used.add(existing.id);
            }
        }
        for (ChannelItem item : channels) {
            if (item == null || item.isVod || item.id == null || used.contains(item.id)) {
                continue;
            }
            items.add(item);
        }
        return items;
    }

    private void tuneMultiViewSlotChannel(int slot, ChannelItem item) {
        if (slot < 0 || slot >= multiPlayerControllers.size() || item == null || item.id == null) {
            return;
        }
        while (multiViewChannels.size() <= slot) {
            multiViewChannels.add(null);
        }
        multiViewChannels.set(slot, item);
        multiViewChannelIds[slot] = item.id;
        PlayerController controller = multiPlayerControllers.get(slot);
        controller.setMuted(slot != multiViewActiveIndex);
        PlayerController.PlaybackRequest request = toPlaybackRequest(item);
        PlayerController.StreamInfo cachedStreamInfo = streamInfoByChannelId.get(item.id);
        controller.playChannel(request, true, cachedStreamInfo);
        if (request != null && !request.directPlayback) {
            controller.resolveStreamInfoAndReplayIfNeeded(request, true, streamInfoByChannelId);
        }
        updateMultiViewFocus();
    }

    private void focusMultiViewSlot(int slot) {
        if (slot < 0 || slot >= multiViewChannels.size()) {
            return;
        }
        if (slot == multiViewActiveIndex) {
            openMultiViewSlotFullscreen(slot);
            return;
        }
        multiViewActiveIndex = slot;
        for (int i = 0; i < multiPlayerControllers.size(); i++) {
            multiPlayerControllers.get(i).setMuted(i != multiViewActiveIndex);
        }
        updateMultiViewFocus();
    }

    private void moveMultiViewSelection(int columnDelta, int rowDelta) {
        if (!isMultiViewVisible() || multiViewChannels.isEmpty()) {
            return;
        }
        int currentSlot = Math.max(0, Math.min(multiViewActiveIndex, multiViewChannels.size() - 1));
        int currentColumn = currentSlot % 2;
        int currentRow = currentSlot / 2;
        int nextColumn = Math.max(0, Math.min(1, currentColumn + columnDelta));
        int nextRow = Math.max(0, Math.min(1, currentRow + rowDelta));
        int nextSlot = (nextRow * 2) + nextColumn;
        if (nextSlot >= multiViewChannels.size() || nextSlot == currentSlot) {
            return;
        }
        multiViewActiveIndex = nextSlot;
        for (int i = 0; i < multiPlayerControllers.size(); i++) {
            multiPlayerControllers.get(i).setMuted(i != multiViewActiveIndex);
        }
        updateMultiViewFocus();
    }

    private void openMultiViewSlotFullscreen(int slot) {
        if (slot < 0 || slot >= multiViewChannels.size()) {
            return;
        }
        ChannelItem item = multiViewChannels.get(slot);
        if (item == null) {
            return;
        }
        closeMultiView();
        showStatus(item.name == null || item.name.trim().isEmpty()
                ? getString(R.string.status_ready)
                : item.name.trim());
        tuneChannelById(item.id);
    }

    private void updateMultiViewFocus() {
        for (int i = 0; i < multiTiles.length; i++) {
            if (multiTiles[i] == null) {
                continue;
            }
            boolean hasChannel = i < multiViewChannels.size();
            boolean active = i == multiViewActiveIndex && hasChannel;
            GradientDrawable tileBackground = new GradientDrawable();
            tileBackground.setColor(Color.parseColor("#FF1A2430"));
            tileBackground.setStroke(active ? dpToPx(4) : dpToPx(2), active ? Color.parseColor("#FFCC7A00") : Color.parseColor("#55384B5E"));
            multiTiles[i].setBackground(tileBackground);
            multiTiles[i].setAlpha(hasChannel ? 1f : 0.7f);
            if (multiLabels[i] != null) {
                multiLabels[i].setText(hasChannel ? multiViewChannels.get(i).name : "");
                multiLabels[i].setBackgroundTintList(ColorStateList.valueOf(active ? 0xCC0E3E46 : 0xCC243447));
            }
            if (multiAudioBadges[i] != null) {
                multiAudioBadges[i].setVisibility(View.GONE);
            }
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private CharSequence buildHighlightedText(String value, String query, boolean favorite) {
        String base = value == null ? "" : value;
        String prefix = favorite ? "★ " : "";
        SpannableStringBuilder builder = new SpannableStringBuilder(prefix + base);
        if (query == null || query.trim().isEmpty() || base.isEmpty()) {
            return builder;
        }
        String lowerBase = base.toLowerCase(Locale.ROOT);
        String lowerQuery = query.trim().toLowerCase(Locale.ROOT);
        int start = 0;
        while (start < lowerBase.length()) {
            int index = lowerBase.indexOf(lowerQuery, start);
            if (index < 0) {
                break;
            }
            int spanStart = prefix.length() + index;
            int spanEnd = spanStart + lowerQuery.length();
            builder.setSpan(new ForegroundColorSpan(0xFF9BD0FF), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new StyleSpan(Typeface.BOLD), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = index + lowerQuery.length();
        }
        return builder;
    }

    private void syncOverlayCoordinator() {
        channelOverlayCoordinator.syncState(currentIndex, selectedOverlayIndex, favoritesOnly, selectedFilterKey);
    }

    private void syncOverlayStateFromCoordinator() {
        currentIndex = channelOverlayCoordinator.getCurrentIndex();
        selectedOverlayIndex = channelOverlayCoordinator.getSelectedOverlayIndex();
        favoritesOnly = channelOverlayCoordinator.isFavoritesOnly();
        selectedFilterKey = channelOverlayCoordinator.getSelectedFilterKey();
        persistNavigationState();
    }

    private void updateOverlayPanel() {
        if (overlayCurrentChannelText == null || overlayCurrentMetaText == null || overlayPlaybackRouteText == null || overlayRecentText == null) {
            return;
        }
        ChannelItem currentChannel = (currentIndex >= 0 && currentIndex < channels.size()) ? channels.get(currentIndex) : findChannelItemById(lastChannelId);
        if (currentChannel == null) {
            overlayCurrentChannelText.setText(getString(R.string.status_ready));
            overlayCurrentMetaText.setVisibility(View.VISIBLE);
            overlayCurrentMetaText.setText(getString(R.string.overlay_current_program_empty));
        } else {
            overlayCurrentChannelText.setText(displayName(currentChannel));
            String currentProgram = buildOverlayProgramSummary(currentChannel);
            String tag = profileTag(currentChannel);
            if (!tag.isEmpty()) {
                currentProgram = tag + "  ·  " + currentProgram;
            }
            overlayCurrentMetaText.setVisibility(View.VISIBLE);
            overlayCurrentMetaText.setText(currentProgram);
        }

        PlayerController.PlaybackDiagnostics diagnostics = playerController == null ? null : playerController.getPlaybackDiagnostics();
        String routeLabel = diagnostics == null || diagnostics.routeLabel == null || diagnostics.routeLabel.trim().isEmpty()
                ? getString(R.string.diagnostics_state_idle)
                : diagnostics.routeLabel;
        overlayPlaybackRouteText.setText(getString(R.string.overlay_playback_route, routeLabel));

        List<RecentChannelsStore.RecentChannelItem> items = recentChannelsStore == null ? new ArrayList<>() : recentChannelsStore.getItems();
        overlayRecentText.setVisibility(View.VISIBLE);
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

    private String buildOverlayProgramSummary(ChannelItem channel) {
        String currentLine = channel == null || channel.nowProgram == null || channel.nowProgram.trim().isEmpty()
                ? getString(R.string.overlay_current_program_empty)
                : getString(R.string.overlay_current_program, channel.nowProgram.trim());
        String nextLine = channel == null || channel.nextProgram == null || channel.nextProgram.trim().isEmpty()
                ? getString(R.string.overlay_next_program_empty)
                : getString(R.string.overlay_next_program, channel.nextProgram.trim());
        return currentLine + "\n" + nextLine;
    }

    private String buildZapProgramSummary(ChannelItem channel) {
        if (channel != null && channel.nowProgram != null && !channel.nowProgram.trim().isEmpty()) {
            if (channel.nextProgram != null && !channel.nextProgram.trim().isEmpty()) {
                return channel.nowProgram.trim() + "  ·  " + getString(R.string.overlay_next_program_short, channel.nextProgram.trim());
            }
            return channel.nowProgram.trim();
        }
        return getString(R.string.zap_banner_empty_meta);
    }

    private void showV12ToolsMenu() {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.tools_section_current_channel));
        actions.add(this::showCurrentChannelQuickActionsDialog);
        options.add(getString(R.string.tools_section_playback));
        actions.add(this::showPlaybackToolsDialog);
        options.add(getString(R.string.tools_section_navigation));
        actions.add(this::showNavigationToolsDialog);
        options.add(getString(R.string.tools_section_vod));
        actions.add(this::showVodLibraryDialog);
        options.add(getString(R.string.tools_section_lists));
        actions.add(this::showListsToolsDialog);
        if (!isOfflineRecordingsDisabled()) {
            options.add(getString(R.string.tools_section_recordings));
            actions.add(this::showRecordingsToolsDialog);
        }
        options.add(getString(R.string.tools_section_multiview));
        actions.add(this::showMultiviewToolsDialog);
        options.add(getString(R.string.tools_section_settings));
        actions.add(this::showSettingsAndDiagnosticsToolsDialog);
        showTvOptionsDialog(R.string.tools_menu_title_short, null, options, actions);
    }

    private void showPlaybackToolsDialog() {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.diagnostics_action_retry_next_route));
        actions.add(() -> retryCurrentPlaybackWithNextRoute(getCurrentPlaybackChannelItem()));
        options.add(getString(R.string.diagnostics_action_retry));
        actions.add(this::retryCurrentPlayback);
        options.add(getString(R.string.tools_menu_playback_mode_temporary));
        actions.add(this::openCurrentTemporaryPlaybackMode);
        options.add(getString(R.string.tools_menu_playback_diagnostics));
        actions.add(this::showPlaybackDiagnosticsDialog);
        showTvOptionsDialog(R.string.tools_section_playback, null, options, actions);
    }

    private void showNavigationToolsDialog() {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.tools_menu_quick_hub));
        actions.add(this::showQuickHubDialog);
        options.add(getString(R.string.tools_menu_search_channels));
        actions.add(this::showChannelSearchDialog);
        options.add(getString(R.string.quick_hub_global_search));
        actions.add(this::showGlobalSearchDialog);
        options.add(getString(R.string.tools_menu_timeline_guide));
        actions.add(this::openTimelineGuideAroundSelection);
        options.add(getString(R.string.tools_menu_visual_epg));
        actions.add(this::openVisualEpgAroundSelection);
        options.add(getString(R.string.tools_menu_epg_search));
        actions.add(this::showEpgSearchDialog);
        options.add(getString(R.string.tools_menu_recent_channels));
        actions.add(this::showRecentChannelsDialog);
        showTvOptionsDialog(R.string.tools_section_navigation, null, options, actions);
    }

    private void showVodLibraryDialog() {
        showVodVisualLibraryDialog();
    }

    private void showVodVisualLibraryDialog() {
        showVodVisualLibraryDialog(VodVisualTypeFilter.GENERAL, VodVisualPlatformFilter.ALL, VodVisualStatusFilter.ALL, VodVisualSortFilter.SMART, "");
    }

    private void showVodLibraryMenuDialog() {
        rememberCurrentVodPosition();
        List<ChannelItem> continueItems = buildVodContinueItems();
        List<ChannelItem> recentItems = buildRecentVodItems();
        List<ChannelItem> tivifyItems = buildVodItemsByFilter("vod:tivify:general", false);
        List<ChannelItem> tivifyAdultItems = buildVodItemsByFilter("vod:tivify:adult", true);
        List<ChannelItem> runtimeItems = buildVodItemsByFilter("vod:runtime:movies", false);
        List<ChannelItem> progressItems = buildVodProgressItems();
        List<ChannelItem> notStartedItems = buildVodNotStartedItems();
        List<ChannelItem> allVodItems = buildAllVodLibraryItems(false);
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(buildVodLibraryOptionLabel(R.string.vod_library_continue, continueItems));
        actions.add(() -> showVodLibraryList(R.string.vod_library_continue, continueItems, true));
        options.add(buildVodLibraryOptionLabel(R.string.vod_library_recent, recentItems));
        actions.add(() -> showVodLibraryList(R.string.vod_library_recent, recentItems, false));
        options.add(buildVodLibraryOptionLabel(R.string.vod_library_tivify, tivifyItems));
        actions.add(() -> showVodLibraryList(R.string.vod_library_tivify, tivifyItems, false));
        options.add(buildVodLibraryOptionLabel(R.string.vod_library_tivify_adult, tivifyAdultItems));
        actions.add(() -> showVodLibraryList(R.string.vod_library_tivify_adult, tivifyAdultItems, false));
        options.add(buildVodLibraryOptionLabel(R.string.vod_library_runtime, runtimeItems));
        actions.add(() -> showVodLibraryList(R.string.vod_library_runtime, runtimeItems, false));
        options.add(buildVodLibraryOptionLabel(R.string.vod_library_with_progress, progressItems));
        actions.add(() -> showVodLibraryList(R.string.vod_library_with_progress, progressItems, true));
        options.add(buildVodLibraryOptionLabel(R.string.vod_library_not_started, notStartedItems));
        actions.add(() -> showVodLibraryList(R.string.vod_library_not_started, notStartedItems, false));
        options.add(getString(R.string.vod_library_categories));
        actions.add(this::showVodCategoriesDialog);
        options.add(buildVodLibraryOptionLabel(R.string.vod_library_all_alpha, allVodItems));
        actions.add(() -> showVodLibraryList(R.string.vod_library_all_alpha, buildVodSortedItems(VodSortMode.ALPHA), false));
        options.add(getString(R.string.vod_library_sort_year));
        actions.add(() -> showVodLibraryList(R.string.vod_library_sort_year, buildVodSortedItems(VodSortMode.YEAR_DESC), false));
        options.add(getString(R.string.vod_library_sort_duration));
        actions.add(() -> showVodLibraryList(R.string.vod_library_sort_duration, buildVodSortedItems(VodSortMode.DURATION_DESC), false));
        options.add(getString(R.string.quick_hub_search_vod));
        actions.add(this::showVodSearchDialog);
        options.add(buildVodLibraryOptionLabel(R.string.vod_library_manage_progress, progressItems));
        actions.add(this::showVodProgressManagerDialog);
        showTvOptionsDialog(R.string.tools_section_vod, null, options, actions);
    }

    private void showVodVisualLibraryDialog(VodVisualTypeFilter typeFilter, VodVisualPlatformFilter platformFilter, VodVisualStatusFilter statusFilter, VodVisualSortFilter sortFilter) {
        showVodVisualLibraryDialog(typeFilter, platformFilter, statusFilter, sortFilter, "");
    }

    private void showVodVisualLibraryDialog(VodVisualTypeFilter typeFilter, VodVisualPlatformFilter platformFilter, VodVisualStatusFilter statusFilter, VodVisualSortFilter sortFilter, String searchQuery) {
        rememberCurrentVodPosition();
        prepareModalSurface();
        final Dialog[] dialogHolder = new Dialog[1];
        String trimmedSearchQuery = searchQuery == null ? "" : searchQuery.trim();
        boolean searchMode = !trimmedSearchQuery.isEmpty();

        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        int dialogWidth = Math.max(dp(900), metrics.widthPixels - dp(56));
        int dialogHeight = Math.max(dp(560), metrics.heightPixels - dp(80));
        int contentWidth = dialogWidth;
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(dialogWidth, dialogHeight));
        scrollView.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setMinimumWidth(contentWidth);
        content.setLayoutParams(new android.widget.ScrollView.LayoutParams(contentWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
        int padding = dp(16);
        content.setPadding(padding, padding, padding, padding);
        content.setBackgroundColor(0xF0181E28);

        TextView titleView = new TextView(this);
        titleView.setText(searchMode ? getString(R.string.vod_search_results_title, trimmedSearchQuery) : getString(R.string.tools_section_vod));
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(24f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(titleView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView subtitleView = new TextView(this);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(4);
        subtitleView.setText(searchMode ? buildVodSearchSummary(trimmedSearchQuery) : buildVodLibrarySummary());
        subtitleView.setTextColor(0xFFB7C4D6);
        subtitleView.setTextSize(13f);
        content.addView(subtitleView, subtitleParams);

        TextView helpView = new TextView(this);
        LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        helpParams.topMargin = dp(8);
        helpView.setText(R.string.vod_visual_help);
        helpView.setTextColor(0xFFFFD082);
        helpView.setTextSize(12f);
        helpView.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(helpView, helpParams);

        LinearLayout filtersRow = new LinearLayout(this);
        filtersRow.setOrientation(LinearLayout.HORIZONTAL);
        filtersRow.setGravity(Gravity.CENTER_VERTICAL);
        filtersRow.setFocusable(false);
        LinearLayout.LayoutParams filtersParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        filtersParams.topMargin = dp(12);
        content.addView(filtersRow, filtersParams);
        activeVodVisualFilterRow = filtersRow;

        if (searchMode) {
            addVodVisualAction(filtersRow, getString(R.string.vod_visual_filter_edit_search), false, () -> {
                dismissVodVisualDialog(dialogHolder[0]);
                uiHandler.post(() -> showVodSearchDialog(trimmedSearchQuery));
            });
        }
        addVodVisualAction(filtersRow, getString(R.string.vod_visual_filter_type, typeFilter.label), true, () -> {
            dismissVodVisualDialog(dialogHolder[0]);
            uiHandler.post(() -> showVodVisualLibraryDialog(typeFilter.next(), platformFilter, statusFilter, sortFilter, trimmedSearchQuery));
        });
        addVodVisualAction(filtersRow, getString(R.string.vod_visual_filter_platform, platformFilter.label), true, () -> {
            dismissVodVisualDialog(dialogHolder[0]);
            uiHandler.post(() -> showVodVisualLibraryDialog(typeFilter, platformFilter.next(), statusFilter, sortFilter, trimmedSearchQuery));
        });
        addVodVisualAction(filtersRow, getString(R.string.vod_visual_filter_status, statusFilter.label), true, () -> {
            dismissVodVisualDialog(dialogHolder[0]);
            uiHandler.post(() -> showVodVisualLibraryDialog(typeFilter, platformFilter, statusFilter.next(), sortFilter, trimmedSearchQuery));
        });
        addVodVisualAction(filtersRow, getString(R.string.vod_visual_filter_sort, sortFilter.label), true, () -> {
            dismissVodVisualDialog(dialogHolder[0]);
            uiHandler.post(() -> showVodVisualLibraryDialog(typeFilter, platformFilter, statusFilter, sortFilter.next(), trimmedSearchQuery));
        });
        if (searchMode) {
            addVodVisualAction(filtersRow, getString(R.string.vod_visual_filter_clear_search), false, () -> {
                dismissVodVisualDialog(dialogHolder[0]);
                uiHandler.post(this::showVodVisualLibraryDialog);
            });
        } else {
            addVodVisualAction(filtersRow, getString(R.string.vod_visual_filter_search), false, () -> {
                dismissVodVisualDialog(dialogHolder[0]);
                uiHandler.post(this::showVodSearchDialog);
            });
            addVodVisualAction(filtersRow, getString(R.string.vod_visual_filter_list_view), false, () -> {
                dismissVodVisualDialog(dialogHolder[0]);
                uiHandler.post(this::showVodLibraryMenuDialog);
            });
        }

        List<RecyclerView> shelfRows = new ArrayList<>();
        if (searchMode) {
            addVodShelf(content, scrollView, shelfRows, contentWidth - (padding * 2), getString(R.string.vod_visual_results), buildVodVisualFilteredItems(typeFilter, platformFilter, statusFilter, sortFilter, trimmedSearchQuery), false);
        } else if (isDefaultVodVisualFilter(typeFilter, platformFilter, statusFilter, sortFilter)) {
            addVodShelf(content, scrollView, shelfRows, contentWidth - (padding * 2), getString(R.string.vod_library_continue), buildVodContinueItems(), true);
            addVodShelf(content, scrollView, shelfRows, contentWidth - (padding * 2), getString(R.string.vod_library_recent), buildRecentVodItems(), false);
            addVodShelf(content, scrollView, shelfRows, contentWidth - (padding * 2), getString(R.string.vod_library_runtime), buildVodItemsByFilter("vod:runtime:movies", false), false);
            addVodShelf(content, scrollView, shelfRows, contentWidth - (padding * 2), getString(R.string.vod_library_tivify), buildVodItemsByFilter("vod:tivify:general", false), false);
            addVodShelf(content, scrollView, shelfRows, contentWidth - (padding * 2), getString(R.string.vod_library_with_progress), buildVodProgressItems(), true);
            addVodShelf(content, scrollView, shelfRows, contentWidth - (padding * 2), getString(R.string.vod_library_all_alpha), buildVodSortedItems(VodSortMode.ALPHA), false);
        } else {
            addVodShelf(content, scrollView, shelfRows, contentWidth - (padding * 2), getString(R.string.vod_visual_results), buildVodVisualFilteredItems(typeFilter, platformFilter, statusFilter, sortFilter), false);
        }
        if (shelfRows.isEmpty()) {
            TextView emptyView = new TextView(this);
            LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            emptyParams.topMargin = dp(22);
            emptyView.setText(R.string.vod_library_empty);
            emptyView.setTextColor(0xFFB7C4D6);
            emptyView.setTextSize(15f);
            content.addView(emptyView, emptyParams);
        }

        scrollView.addView(content, new android.widget.ScrollView.LayoutParams(contentWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

        android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        root.setBackgroundColor(0xCC000000);
        android.widget.FrameLayout.LayoutParams scrollParams = new android.widget.FrameLayout.LayoutParams(dialogWidth, dialogHeight, Gravity.CENTER);
        root.addView(scrollView, scrollParams);

        Dialog dialog = new Dialog(this);
        dialogHolder[0] = dialog;
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(root);
        dialog.setOnDismissListener(d -> {
            if (activeVodVisualFilterRow == filtersRow) {
                activeVodVisualFilterRow = null;
            }
            enableImmersiveMode();
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setDimAmount(0f);
        }
        wireVodVisualHeaderNavigation(filtersRow, shelfRows, scrollView);
        if (!focusVodShelfItem(shelfRows, 0, 0, scrollView)) {
            focusVodVisualRowButton(filtersRow, 0, scrollView);
        }
    }

    private void dismissVodVisualDialog(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private TextView addVodVisualAction(LinearLayout parent, String label, Runnable action) {
        return addVodVisualAction(parent, label, false, action);
    }

    private TextView addVodVisualAction(LinearLayout parent, String label, boolean filterButton, Runnable action) {
        if (parent == null || label == null) {
            return null;
        }
        TextView button = new TextView(this);
        button.setText(label);
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(13f);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setMinHeight(dp(42));
        button.setPadding(dp(12), 0, dp(12), 0);
        applyVodVisualActionStyle(button, filterButton, false);
        button.setOnFocusChangeListener((v, hasFocus) -> applyVodVisualActionStyle(button, filterButton, hasFocus));
        button.setFocusable(true);
        button.setFocusableInTouchMode(true);
        button.setClickable(true);
        if (button.getId() == View.NO_ID) {
            button.setId(View.generateViewId());
        }
        button.setOnClickListener(v -> {
            if (action != null) {
                action.run();
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
        params.setMarginEnd(dp(8));
        parent.addView(button, params);
        return button;
    }

    private void applyVodVisualActionStyle(TextView button, boolean filterButton, boolean focused) {
        if (button == null) {
            return;
        }
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(8));
        if (focused) {
            background.setColor(0xFFFFD782);
            background.setStroke(dp(2), 0xFFFFFFFF);
            button.setTextColor(0xFF111820);
        } else if (filterButton) {
            background.setColor(0xFF235D78);
            background.setStroke(dp(1), 0xFF7FD8FF);
            button.setTextColor(0xFFFFFFFF);
        } else {
            background.setColor(0xFF263645);
            background.setStroke(dp(1), 0xFF54677A);
            button.setTextColor(0xFFD8E4F2);
        }
        button.setBackground(background);
    }

    private void addVodShelf(LinearLayout parent, android.widget.ScrollView scrollView, List<RecyclerView> shelfRows, int shelfWidth, String title, List<ChannelItem> items, boolean progressFirst) {
        if (parent == null || items == null || items.isEmpty()) {
            return;
        }
        if (progressFirst) {
            sortVodLibraryItems(items);
        }
        TextView titleView = new TextView(this);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(18);
        titleView.setText(getString(R.string.vod_visual_section_title, title, items.size()));
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(17f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        parent.addView(titleView, titleParams);

        int rowIndex = shelfRows == null ? 0 : shelfRows.size();
        RecyclerView recyclerView = new VodShelfRecyclerView(shelfRows, rowIndex, scrollView);
        if (shelfRows != null) {
            shelfRows.add(recyclerView);
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(8);
        recyclerView.setFocusable(true);
        recyclerView.setFocusableInTouchMode(true);
        recyclerView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        recyclerView.setMinimumWidth(shelfWidth);
        recyclerView.setAdapter(new VodPosterAdapter(items, shelfRows, rowIndex, scrollView));
        recyclerView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_UP && rowIndex == 0) {
                return false;
            }
            return false;
        });
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(shelfWidth, dp(248));
        listParams.topMargin = dp(8);
        parent.addView(recyclerView, listParams);
    }

    private void wireVodVisualHeaderNavigation(LinearLayout actionsRow, List<RecyclerView> shelfRows, android.widget.ScrollView scrollView) {
        wireVodVisualRowNavigation(actionsRow, shelfRows, scrollView);
    }

    private void wireVodVisualRowNavigation(LinearLayout row, List<RecyclerView> shelfRows, android.widget.ScrollView scrollView) {
        if (row == null) {
            return;
        }
        for (int i = 0; i < row.getChildCount(); i++) {
            final int actionIndex = i;
            View child = row.getChildAt(i);
            child.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    return focusVodShelfItem(shelfRows, 0, actionIndex, scrollView);
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    return focusVodVisualRowButton(row, actionIndex - 1, scrollView);
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    return focusVodVisualRowButton(row, actionIndex + 1, scrollView);
                }
                return false;
            });
        }
    }

    private boolean focusVodVisualRowButton(LinearLayout row, int index, android.widget.ScrollView scrollView) {
        if (row == null || row.getChildCount() == 0) {
            return false;
        }
        int targetIndex = Math.max(0, Math.min(index, row.getChildCount() - 1));
        View target = row.getChildAt(targetIndex);
        if (target == null) {
            return false;
        }
        target.post(() -> {
            target.requestFocus();
            ensureVodVisualItemVisible(scrollView, target);
        });
        return true;
    }

    private boolean isFocusInsideView(View container) {
        return isFocusInsideView(container, getCurrentFocus());
    }

    private boolean isFocusInsideView(View container, View focused) {
        if (container == null) {
            return false;
        }
        while (focused != null) {
            if (focused == container) {
                return true;
            }
            Object parent = focused.getParent();
            focused = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    private int findFocusedChildIndex(LinearLayout row) {
        return findFocusedChildIndex(row, getCurrentFocus());
    }

    private int findFocusedChildIndex(LinearLayout row, View focused) {
        if (row == null || row.getChildCount() == 0) {
            return 0;
        }
        for (int i = 0; i < row.getChildCount(); i++) {
            if (row.getChildAt(i) == focused) {
                return i;
            }
        }
        return 0;
    }

    private int findFocusedVodShelfPosition(RecyclerView recyclerView) {
        return findFocusedVodShelfPosition(recyclerView, getCurrentFocus());
    }

    private int findFocusedVodShelfPosition(RecyclerView recyclerView, View focused) {
        if (recyclerView == null) {
            return 0;
        }
        if (focused == null) {
            return 0;
        }
        View child = focused;
        while (child != null && child.getParent() != recyclerView) {
            Object parent = child.getParent();
            child = parent instanceof View ? (View) parent : null;
        }
        if (child == null) {
            return 0;
        }
        int position = recyclerView.getChildAdapterPosition(child);
        return position == RecyclerView.NO_POSITION ? 0 : position;
    }

    private boolean focusVodShelfItem(List<RecyclerView> shelfRows, int rowIndex, int itemIndex, android.widget.ScrollView scrollView) {
        if (rowIndex < 0) {
            return focusVodVisualRowButton(activeVodVisualFilterRow, itemIndex, scrollView);
        }
        if (shelfRows == null || rowIndex < 0 || rowIndex >= shelfRows.size()) {
            return false;
        }
        RecyclerView recyclerView = shelfRows.get(rowIndex);
        if (recyclerView == null || recyclerView.getAdapter() == null || recyclerView.getAdapter().getItemCount() == 0) {
            return false;
        }
        int targetIndex = Math.max(0, Math.min(itemIndex, recyclerView.getAdapter().getItemCount() - 1));
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(targetIndex, dp(8));
        } else {
            recyclerView.scrollToPosition(targetIndex);
        }
        recyclerView.post(() -> {
            View target = null;
            RecyclerView.LayoutManager updatedLayoutManager = recyclerView.getLayoutManager();
            if (updatedLayoutManager != null) {
                target = updatedLayoutManager.findViewByPosition(targetIndex);
            }
            if (target != null) {
                target.requestFocus();
                ensureVodVisualItemVisible(scrollView, target);
            } else {
                recyclerView.requestFocus();
                ensureVodVisualItemVisible(scrollView, recyclerView);
            }
        });
        return true;
    }

    private void ensureVodVisualItemVisible(android.widget.ScrollView scrollView, View target) {
        if (scrollView == null || target == null) {
            return;
        }
        scrollView.post(() -> {
            Rect rect = new Rect();
            target.getDrawingRect(rect);
            scrollView.offsetDescendantRectToMyCoords(target, rect);
            int viewportTop = scrollView.getScrollY();
            int viewportBottom = viewportTop + scrollView.getHeight();
            int topPadding = dp(56);
            int bottomPadding = dp(44);
            if (rect.top < viewportTop + topPadding) {
                scrollView.smoothScrollTo(0, Math.max(0, rect.top - topPadding));
            } else if (rect.bottom > viewportBottom - bottomPadding) {
                scrollView.smoothScrollTo(0, Math.max(0, rect.bottom - scrollView.getHeight() + bottomPadding));
            }
        });
    }

    private void showListsToolsDialog() {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.tools_menu_favorite_channels));
        actions.add(this::showFavoriteChannelsQuickDialog);
        options.add(getString(R.string.tools_menu_manage_personal_lists));
        actions.add(this::showPersonalListsManagerDialog);
        options.add(getString(R.string.tools_menu_personal_lists_current));
        actions.add(this::openCurrentChannelPersonalLists);
        options.add(getString(R.string.tools_menu_channel_profile_current));
        actions.add(this::openCurrentChannelProfile);
        showTvOptionsDialog(R.string.tools_section_lists, null, options, actions);
    }

    private boolean isOfflineRecordingsDisabled() {
        return BuildConfig.STANDALONE_MODE;
    }

    private boolean showOfflineRecordingsUnavailableIfNeeded() {
        if (!isOfflineRecordingsDisabled()) {
            return false;
        }
        hideRecordingsPanel();
        showStatus(getString(R.string.status_recordings_unavailable_offline));
        return true;
    }

    private void showRecordingsToolsDialog() {
        if (showOfflineRecordingsUnavailableIfNeeded()) {
            return;
        }
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.tools_menu_recordings_panel));
        actions.add(this::openRecordingsBrowser);
        options.add(getString(R.string.menu_record_current_program));
        actions.add(() -> createScheduleFromEndpoint(getCurrentPlaybackChannelItem(), false));
        options.add(getString(R.string.menu_record_next_program));
        actions.add(() -> createScheduleFromEndpoint(getCurrentPlaybackChannelItem(), true));
        showTvOptionsDialog(R.string.tools_section_recordings, null, options, actions);
    }

    private void showMultiviewToolsDialog() {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.tools_menu_multiview));
        actions.add(this::openMultiView);
        options.add(getString(R.string.tools_menu_multiview_open_preset));
        actions.add(this::showOpenMultiViewPresetDialog);
        options.add(getString(R.string.tools_menu_multiview_save_preset));
        actions.add(this::showSaveMultiViewPresetDialog);
        showTvOptionsDialog(R.string.tools_section_multiview, null, options, actions);
    }

    private void showSettingsAndDiagnosticsToolsDialog() {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.tools_menu_settings_center));
        actions.add(this::showSettingsCenterDialog);
        options.add(getString(R.string.settings_section_diagnostics));
        actions.add(this::showSettingsDiagnosticsDialog);
        options.add(getString(R.string.tools_menu_install_status));
        actions.add(this::showInstallStatusDialog);
        showTvOptionsDialog(R.string.tools_section_settings, null, options, actions);
    }

    private void showCurrentChannelQuickActionsDialog() {
        ChannelItem channelItem = getCurrentPlaybackChannelItem();
        if (channelItem == null) {
            showStatus(getString(R.string.diagnostics_none));
            return;
        }
        boolean favorite = favoriteChannelIds.contains(channelItem.id);
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.diagnostics_action_retry_next_route));
        actions.add(() -> retryCurrentPlaybackWithNextRoute(channelItem));
        options.add(getString(R.string.diagnostics_action_temporary_mode));
        actions.add(() -> showTemporaryPlaybackModeDialog(channelItem));
        options.add(getString(favorite ? R.string.menu_remove_favorite : R.string.menu_add_favorite));
        actions.add(() -> toggleFavoriteForChannel(channelItem));
        options.add(getString(R.string.menu_personal_lists));
        actions.add(() -> showPersonalListsDialog(channelItem));
        options.add(getString(R.string.menu_channel_profile));
        actions.add(() -> showChannelProfileDialog(channelItem));
        options.add(getString(R.string.menu_mini_guide));
        actions.add(() -> openMiniGuideForChannel(channelItem));
        options.add(getString(R.string.tools_menu_playback_diagnostics));
        actions.add(this::showPlaybackDiagnosticsDialog);
        showTvOptionsDialog(R.string.tools_section_current_channel, displayName(channelItem), options, actions);
    }

    private String buildCurrentChannelToolsMessage() {
        ChannelItem channelItem = getCurrentPlaybackChannelItem();
        if (channelItem == null) {
            return getString(R.string.diagnostics_none);
        }
        String meta = channelItem.isVod ? buildVodInfoMeta(channelItem) : fallbackUnknown(channelItem.group);
        return getString(R.string.tools_current_channel_message, displayName(channelItem), meta);
    }

    private void showInstallStatusDialog() {
        prepareModalSurface();
        String packageName = getPackageName();
        String versionName = BuildConfig.VERSION_NAME;
        int versionCode = BuildConfig.VERSION_CODE;
        String updatedAt = getString(R.string.diagnostics_value_unknown);
        try {
            PackageInfo info = getPackageManager().getPackageInfo(packageName, 0);
            if (info != null && info.lastUpdateTime > 0L) {
                updatedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(info.lastUpdateTime));
            }
        } catch (Exception ignored) {
        }
        String message = getString(
                R.string.install_status_message,
                packageName,
                versionName,
                versionCode,
                ".MainActivity",
                updatedAt,
                "com.drbep.tvplayer.firetv, com.drbep.tvplayer, com.drbep.tv.v2.fixed"
        );
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_install_status)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_close, null)
                .create();
        showTvDialog(dialog);
    }

    private void showSettingsCenterDialog() {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.settings_section_startup));
        actions.add(this::showStartupSettingsDialog);
        options.add(getString(R.string.settings_section_playback));
        actions.add(this::showPlaybackSettingsDialog);
        options.add(getString(R.string.settings_section_search));
        actions.add(this::showSearchSettingsDialog);
        options.add(getString(R.string.settings_section_recordings));
        actions.add(this::showRecordingSettingsDialog);
        options.add(getString(R.string.settings_section_local_data));
        actions.add(this::showLocalDataSettingsDialog);
        options.add(getString(R.string.settings_section_offline_system));
        actions.add(this::showOfflineSystemDialog);
        options.add(getString(R.string.settings_offline_full_sync));
        actions.add(this::runManualOfflineFullSync);
        options.add(getString(R.string.settings_section_offline_catalog));
        actions.add(this::showOfflineCatalogSettingsDialog);
        options.add(getString(R.string.app_update_action_check));
        actions.add(this::checkAppUpdateManually);
        options.add(getString(R.string.settings_section_diagnostics));
        actions.add(this::showSettingsDiagnosticsDialog);
        options.add(getString(R.string.settings_section_reset));
        actions.add(this::showResetSettingsDialog);
        showTvOptionsDialog(R.string.title_settings_center, null, options, actions);
    }

    private String buildSettingsSummary() {
        return getString(
                R.string.settings_summary,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                prefs != null && prefs.getBoolean(PREF_STARTUP_HUB_DISABLED, false) ? getString(R.string.diagnostics_value_no) : getString(R.string.diagnostics_value_yes),
                recordingsAutoRefreshEnabled ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
                favoritesOnly ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
                globalSearchRecents.size(),
                vodResumePositions.size(),
                recordingResumePositions.size()
        );
    }

    private void showStartupSettingsDialog() {
        boolean startupEnabled = prefs == null || !prefs.getBoolean(PREF_STARTUP_HUB_DISABLED, false);
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(startupEnabled ? R.string.settings_startup_disable : R.string.settings_startup_enable));
        actions.add(() -> {
            if (startupEnabled) {
                disableStartupHub();
            } else {
                enableStartupHub();
            }
        });
        options.add(getString(R.string.settings_startup_show_now));
        actions.add(this::loadStartupHubStateAndShow);
        options.add(getString(R.string.settings_startup_set_current_channel));
        actions.add(() -> {
            ChannelItem current = getCurrentPlaybackChannelItem();
            if (current != null) {
                saveLastChannelId(current.id);
                showStatus(getString(R.string.status_channel_startup_set));
            }
        });
        options.add(getString(R.string.settings_startup_clear_last_vod));
        actions.add(() -> {
            lastVodId = "";
            if (prefs != null) {
                prefs.edit().remove(PREF_LAST_VOD_ID).apply();
            }
            showStatus(getString(R.string.settings_status_last_vod_cleared));
        });
        options.add(getString(R.string.settings_action_view_summary));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_startup, buildStartupSettingsSummary()));
        showTvOptionsDialog(R.string.settings_section_startup, null, options, actions);
    }

    private String buildStartupSettingsSummary() {
        ChannelItem current = getCurrentPlaybackChannelItem();
        ChannelItem lastVod = findChannelItemById(lastVodId);
        return getString(
                R.string.settings_startup_summary,
                prefs != null && prefs.getBoolean(PREF_STARTUP_HUB_DISABLED, false) ? getString(R.string.diagnostics_value_no) : getString(R.string.diagnostics_value_yes),
                current == null ? getString(R.string.diagnostics_value_unknown) : displayName(current),
                lastVod == null ? getString(R.string.diagnostics_value_unknown) : displayName(lastVod)
        );
    }

    private void showPlaybackSettingsDialog() {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(playbackRepairEnabled ? R.string.settings_playback_repair_disable : R.string.settings_playback_repair_enable));
        actions.add(this::togglePlaybackRepair);
        options.add(getString(R.string.settings_playback_current_mode));
        actions.add(this::openCurrentTemporaryPlaybackMode);
        options.add(getString(R.string.settings_playback_diagnostics));
        actions.add(this::showPlaybackDiagnosticsDialog);
        options.add(getString(R.string.settings_playback_clear_learned));
        actions.add(() -> confirmSettingsAction(R.string.settings_playback_clear_learned, R.string.settings_confirm_clear_learned_routes, this::clearLearnedPlaybackModes));
        options.add(getString(R.string.settings_playback_clear_modes));
        actions.add(() -> confirmSettingsAction(R.string.settings_playback_clear_modes, R.string.settings_confirm_clear_modes, this::clearPlaybackModes));
        options.add(getString(R.string.settings_playback_clear_diagnostics));
        actions.add(() -> confirmSettingsAction(R.string.settings_playback_clear_diagnostics, R.string.settings_confirm_clear_diagnostics, this::clearAllPlaybackDiagnostics));
        options.add(getString(R.string.settings_action_view_summary));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_playback, buildPlaybackSettingsSummary()));
        showTvOptionsDialog(R.string.settings_section_playback, null, options, actions);
    }

    private String buildPlaybackSettingsSummary() {
        ChannelItem current = getCurrentPlaybackChannelItem();
        String currentMode = current == null ? getString(R.string.diagnostics_value_unknown) : formatPlaybackModeLabel(resolvePlaybackModeForRequest(current));
        int errors = playbackDiagnosticsStore == null ? 0 : playbackDiagnosticsStore.getRecentErrors(100).size();
        return getString(
                R.string.settings_playback_summary,
                currentMode,
                temporaryPlaybackModesByChannelId.size(),
                learnedPlaybackModesByChannelId.size(),
                playbackRepairEnabled ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
                errors
        );
    }

    private void showSearchSettingsDialog() {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.quick_hub_global_search));
        actions.add(this::showGlobalSearchDialog);
        options.add(getString(R.string.settings_search_clear_recent));
        actions.add(() -> confirmSettingsAction(R.string.settings_search_clear_recent, R.string.settings_confirm_clear_searches, this::clearGlobalSearchRecents));
        options.add(getString(R.string.settings_action_view_summary));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_search, getString(R.string.settings_search_summary, globalSearchRecents.size())));
        showTvOptionsDialog(R.string.settings_section_search, null, options, actions);
    }

    private void showRecordingSettingsDialog() {
        if (isOfflineRecordingsDisabled()) {
            List<String> options = new ArrayList<>();
            List<Runnable> actions = new ArrayList<>();
            options.add(getString(R.string.settings_recordings_clear_progress));
            actions.add(() -> confirmSettingsAction(R.string.settings_recordings_clear_progress, R.string.settings_confirm_clear_recording_progress, this::clearAllRecordingProgress));
            options.add(getString(R.string.settings_action_view_summary));
            actions.add(() -> showSettingsInfoDialog(R.string.settings_section_recordings, getString(R.string.settings_recordings_offline_summary, recordingResumePositions.size())));
            showTvOptionsDialog(R.string.settings_section_recordings, getString(R.string.settings_recordings_offline_summary, recordingResumePositions.size()), options, actions);
            return;
        }
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(recordingsAutoRefreshEnabled ? R.string.settings_recordings_auto_off : R.string.settings_recordings_auto_on));
        actions.add(this::toggleRecordingsAutoRefresh);
        options.add(getString(R.string.tools_menu_recordings_panel));
        actions.add(this::openRecordingsBrowser);
        options.add(getString(R.string.settings_recordings_clear_progress));
        actions.add(() -> confirmSettingsAction(R.string.settings_recordings_clear_progress, R.string.settings_confirm_clear_recording_progress, this::clearAllRecordingProgress));
        options.add(getString(R.string.settings_action_view_summary));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_recordings, getString(R.string.settings_recordings_summary, recordingsAutoRefreshEnabled ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no), recordingResumePositions.size())));
        showTvOptionsDialog(R.string.settings_section_recordings, null, options, actions);
    }

    private void showLocalDataSettingsDialog() {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.settings_data_clear_vod_progress));
        actions.add(() -> confirmSettingsAction(R.string.settings_data_clear_vod_progress, R.string.settings_confirm_clear_vod_progress, this::clearAllVodProgress));
        options.add(getString(R.string.settings_data_clear_recording_progress));
        actions.add(() -> confirmSettingsAction(R.string.settings_data_clear_recording_progress, R.string.settings_confirm_clear_recording_progress, this::clearAllRecordingProgress));
        options.add(getString(R.string.settings_data_clear_recent_channels));
        actions.add(() -> confirmSettingsAction(R.string.settings_data_clear_recent_channels, R.string.settings_confirm_clear_recent_channels, this::clearRecentChannels));
        options.add(getString(R.string.settings_data_clear_favorites));
        actions.add(() -> confirmSettingsAction(R.string.settings_data_clear_favorites, R.string.settings_confirm_clear_favorites, this::clearFavorites));
        options.add(getString(R.string.settings_data_reset_lists_profiles));
        actions.add(() -> confirmSettingsAction(R.string.settings_data_reset_lists_profiles, R.string.settings_confirm_reset_lists_profiles, this::resetListsAndProfiles));
        options.add(getString(R.string.settings_action_view_summary));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_local_data, buildLocalDataSummary()));
        showTvOptionsDialog(R.string.settings_section_local_data, null, options, actions);
    }

    private String buildLocalDataSummary() {
        return getString(
                R.string.settings_data_summary,
                buildRecentQuickChannels().size(),
                favoriteChannelIds.size(),
                globalSearchRecents.size(),
                vodResumePositions.size(),
                recordingResumePositions.size(),
                channelCollectionStore == null ? 0 : channelCollectionStore.getCollections().size()
        );
    }

    private void showOfflineSystemDialog() {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.settings_offline_system_status));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_offline_system, buildOfflineSystemSummary()));
        options.add(getString(R.string.settings_offline_full_sync));
        actions.add(this::runManualOfflineFullSync);
        options.add(getString(R.string.offline_catalog_action_repair));
        actions.add(this::repairOfflineCatalog);
        options.add(getString(R.string.settings_offline_sync_history));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_offline_sync_history, buildOfflineSyncHistorySummary()));
        options.add(getString(R.string.offline_catalog_action_refresh));
        actions.add(this::refreshOfflineCatalogFromSettings);
        options.add(getString(R.string.app_update_action_check));
        actions.add(this::checkAppUpdateManually);
        options.add(getString(R.string.offline_catalog_action_activate_code));
        actions.add(this::startOfflineActivationCodeFlow);
        options.add(getString(R.string.settings_playback_diagnostics));
        actions.add(this::showPlaybackDiagnosticsDialog);
        showTvOptionsDialog(R.string.settings_section_offline_system, buildOfflineSystemSummary(), options, actions);
    }

    private String buildOfflineSystemSummary() {
        CatalogSnapshotStore.SnapshotStatus status = catalogSnapshotStore == null
                ? new CatalogSnapshotStore.SnapshotStatus(false, 0L, 0L, 0L, false, 0, 0, "", "", "", "", false)
                : catalogSnapshotStore.getStatus(BuildConfig.CATALOG_SNAPSHOT_URL);
        CatalogSnapshotStore.VerificationReport verification = catalogSnapshotStore == null
                ? new CatalogSnapshotStore.VerificationReport(false, "error", getString(R.string.settings_offline_verify_missing), status)
                : catalogSnapshotStore.verifyStoredSnapshot(BuildConfig.CATALOG_SNAPSHOT_URL);
        String catalogHealth = buildOfflineCatalogHealth(status, verification);
        String updateState = buildAppUpdateStateSummary();
        String lastCatalogAttempt = lastOfflineCatalogRefreshAttemptMs <= 0L
                ? getString(R.string.diagnostics_value_unknown)
                : formatDateTime(lastOfflineCatalogRefreshAttemptMs);
        String lastCatalogSuccess = lastOfflineCatalogRefreshSuccessMs <= 0L
                ? getString(R.string.diagnostics_value_unknown)
                : formatDateTime(lastOfflineCatalogRefreshSuccessMs);
        String lastCatalogError = lastOfflineCatalogRefreshError == null || lastOfflineCatalogRefreshError.trim().isEmpty()
                ? getString(R.string.diagnostics_value_no)
                : classifyOperationalError(lastOfflineCatalogRefreshError) + ": " + lastOfflineCatalogRefreshError;
        String lastMaintenance = lastOfflineMaintenanceMs <= 0L
                ? getString(R.string.diagnostics_value_unknown)
                : formatDateTime(lastOfflineMaintenanceMs);
        String maintenanceError = lastOfflineMaintenanceError == null || lastOfflineMaintenanceError.trim().isEmpty()
                ? getString(R.string.diagnostics_value_no)
                : classifyOperationalError(lastOfflineMaintenanceError) + ": " + lastOfflineMaintenanceError;
        return getString(
                R.string.settings_offline_system_summary,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                BuildConfig.STANDALONE_MODE ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
                catalogHealth,
                buildOfflineVerificationSummary(verification),
                buildOfflineEpgStateSummary(status),
                status.hasAccessToken ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
                status.deviceId == null || status.deviceId.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : status.deviceId,
                buildOfflineRecordingsStateSummary(),
                lastCatalogAttempt,
                lastCatalogSuccess,
                lastCatalogError,
                updateState,
                buildRecentDiagnosticsSummary(),
                lastMaintenance,
                maintenanceError,
                buildNextOfflineSyncSummary(status)
        );
    }

    private String buildOfflineEpgStateSummary(CatalogSnapshotStore.SnapshotStatus status) {
        if (status == null || status.epgProgramCount <= 0) {
            return getString(R.string.settings_offline_epg_missing);
        }
        String until = status.epgUntilMs <= 0L
                ? getString(R.string.diagnostics_value_unknown)
                : formatDateTime(status.epgUntilMs);
        return getString(R.string.settings_offline_epg_summary, status.epgChannelCount, status.epgProgramCount, until);
    }

    private String buildOfflineRecordingsStateSummary() {
        return isOfflineRecordingsDisabled()
                ? getString(R.string.settings_offline_recordings_disabled)
                : getString(R.string.settings_offline_recordings_enabled);
    }

    private String buildOfflineCatalogHealth(CatalogSnapshotStore.SnapshotStatus status, CatalogSnapshotStore.VerificationReport verification) {
        if (status == null || !status.available) {
            if (status != null && status.hasLastGoodBackup) {
                return getString(R.string.settings_offline_health_backup_available);
            }
            return getString(R.string.settings_offline_health_missing);
        }
        if (verification != null && !verification.valid) {
            return getString(R.string.settings_offline_health_invalid);
        }
        if (status.expired) {
            return getString(R.string.settings_offline_health_expired);
        }
        if (!status.hasAccessToken) {
            return getString(R.string.settings_offline_health_no_token);
        }
        if (verification != null && "warning".equalsIgnoreCase(verification.state)) {
            return getString(R.string.settings_offline_health_warning);
        }
        if (status.expiresAtMs > 0L) {
            long remainingMs = status.expiresAtMs - System.currentTimeMillis();
            if (remainingMs <= 24L * 60L * 60L * 1000L) {
                return getString(R.string.settings_offline_health_expiring, formatDurationShort(remainingMs));
            }
        }
        return getString(R.string.settings_offline_health_ok);
    }

    private String buildAppUpdateStateSummary() {
        String checked = lastAppUpdateCheckMs <= 0L
                ? getString(R.string.diagnostics_value_unknown)
                : formatDateTime(lastAppUpdateCheckMs);
        if (lastAppUpdateError != null && !lastAppUpdateError.trim().isEmpty()) {
            return getString(R.string.settings_update_state_error, checked, classifyOperationalError(lastAppUpdateError), lastAppUpdateError);
        }
        if (lastKnownAppUpdateInfo != null && lastKnownAppUpdateInfo.isNewerThanCurrent()) {
            return getString(R.string.settings_update_state_available, checked, safeUpdateVersionName(lastKnownAppUpdateInfo), lastKnownAppUpdateInfo.versionCode);
        }
        if (lastKnownAppUpdateInfo != null) {
            return getString(R.string.settings_update_state_current, checked, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        }
        return getString(R.string.settings_update_state_unknown, checked);
    }

    private String buildNextOfflineSyncSummary(CatalogSnapshotStore.SnapshotStatus status) {
        if (!BuildConfig.STANDALONE_MODE) {
            return getString(R.string.diagnostics_value_no);
        }
        if (status == null || !status.hasAccessToken || status.sourceUrl.trim().isEmpty()) {
            return getString(R.string.settings_offline_next_sync_blocked);
        }
        long now = System.currentTimeMillis();
        long byAge = status.updatedAtMs <= 0L ? now : status.updatedAtMs + OFFLINE_CATALOG_AUTO_REFRESH_MS;
        long byExpiry = status.expiresAtMs <= 0L ? Long.MAX_VALUE : status.expiresAtMs - OFFLINE_CATALOG_EXPIRY_REFRESH_MS;
        long next = Math.max(now, Math.min(byAge, byExpiry));
        if (next <= now + 60_000L) {
            return getString(R.string.settings_offline_next_sync_ready);
        }
        return formatDateTime(next);
    }

    private void runManualOfflineFullSync() {
        showStatus(getString(R.string.settings_offline_full_sync_running));
        runOfflineMaintenance(true);
    }

    private void showOfflineCatalogSettingsDialog() {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.settings_offline_system_status));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_offline_system, buildOfflineSystemSummary()));
        options.add(getString(R.string.settings_offline_full_sync));
        actions.add(this::runManualOfflineFullSync);
        options.add(getString(R.string.offline_catalog_action_repair));
        actions.add(this::repairOfflineCatalog);
        options.add(getString(R.string.offline_catalog_action_activate_code));
        actions.add(this::startOfflineActivationCodeFlow);
        options.add(getString(R.string.offline_catalog_action_refresh));
        actions.add(this::refreshOfflineCatalogFromSettings);
        options.add(getString(R.string.offline_catalog_action_verify));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_offline_catalog, buildOfflineCatalogVerificationSummary()));
        options.add(getString(R.string.offline_catalog_action_set_url));
        actions.add(this::showOfflineCatalogUrlDialog);
        options.add(getString(R.string.offline_catalog_action_set_token));
        actions.add(this::showOfflineCatalogTokenDialog);
        options.add(getString(R.string.offline_catalog_action_clear));
        actions.add(() -> confirmSettingsAction(R.string.offline_catalog_action_clear, R.string.offline_catalog_confirm_clear, this::clearOfflineCatalog));
        options.add(getString(R.string.settings_action_view_summary));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_offline_catalog, buildOfflineCatalogSummary()));
        showTvOptionsDialog(R.string.settings_section_offline_catalog, null, options, actions);
    }

    private void startOfflineActivationCodeFlow() {
        if (catalogSnapshotStore == null) {
            return;
        }
        showStatus(getString(R.string.offline_catalog_activation_waiting));
        ioExecutor.execute(() -> {
            try {
                JSONObject payload = catalogSnapshotStore.startActivation(BuildConfig.OFFLINE_BASE_URL, "Fire Stick offline");
                String code = payload.optString("code", "").trim();
                uiHandler.post(() -> showOfflineActivationCodeDialog(code));
            } catch (Exception e) {
                Log.e(TAG, "offline activation start failed", e);
                uiHandler.post(() -> showError(getString(R.string.offline_catalog_activation_error, e.getMessage())));
            }
        });
    }

    private void showOfflineActivationCodeDialog(String code) {
        if (code == null || code.trim().isEmpty()) {
            showError(getString(R.string.offline_catalog_activation_error, "codigo vacio"));
            return;
        }
        TextView codeView = new TextView(this);
        codeView.setText(getString(R.string.offline_catalog_activation_message, formatActivationCode(code)));
        codeView.setTextSize(22f);
        codeView.setTypeface(Typeface.DEFAULT_BOLD);
        codeView.setGravity(Gravity.CENTER);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        codeView.setPadding(pad, pad, pad, pad);
        final boolean[] active = {true};
        final int[] attempts = {0};
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.offline_catalog_activation_title)
                .setView(codeView)
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        dialog.setOnDismissListener(unused -> active[0] = false);
        showTvDialog(dialog);
        pollOfflineActivationCode(code, active, attempts, dialog);
    }

    private void pollOfflineActivationCode(String code, boolean[] active, int[] attempts, Dialog dialog) {
        if (active == null || !active[0]) {
            return;
        }
        if (attempts == null || attempts.length == 0 || attempts[0] >= 120) {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            showError(getString(R.string.offline_catalog_activation_expired));
            return;
        }
        attempts[0]++;
        ioExecutor.execute(() -> {
            try {
                JSONObject payload = catalogSnapshotStore == null ? null : catalogSnapshotStore.pollActivation(BuildConfig.OFFLINE_BASE_URL, code);
                String status = payload == null ? "" : payload.optString("status", "");
                if ("approved".equalsIgnoreCase(status)) {
                    if (catalogSnapshotStore != null) {
                        catalogSnapshotStore.applyActivationPayload(payload, BuildConfig.OFFLINE_BASE_URL);
                    }
                    uiHandler.post(() -> {
                        active[0] = false;
                        if (dialog != null && dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        showStatus(getString(R.string.offline_catalog_activation_approved));
                        refreshOfflineCatalogFromSettings();
                    });
                    return;
                }
                uiHandler.postDelayed(() -> pollOfflineActivationCode(code, active, attempts, dialog), 3000L);
            } catch (Exception e) {
                Log.e(TAG, "offline activation poll failed", e);
                uiHandler.postDelayed(() -> pollOfflineActivationCode(code, active, attempts, dialog), 3000L);
            }
        });
    }

    private static String formatActivationCode(String code) {
        String clean = code == null ? "" : code.replaceAll("\\D", "");
        if (clean.length() == 6) {
            return clean.substring(0, 3) + " " + clean.substring(3);
        }
        return code == null ? "" : code;
    }

    private String formatDateTime(long timestampMs) {
        if (timestampMs <= 0L) {
            return getString(R.string.diagnostics_value_unknown);
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(timestampMs));
    }

    private String buildOfflineCatalogSummary() {
        CatalogSnapshotStore.SnapshotStatus status = catalogSnapshotStore == null
                ? new CatalogSnapshotStore.SnapshotStatus(false, 0L, 0L, 0L, false, 0, 0, "", "", "", "", false)
                : catalogSnapshotStore.getStatus(BuildConfig.CATALOG_SNAPSHOT_URL);
        CatalogSnapshotStore.VerificationReport verification = catalogSnapshotStore == null
                ? new CatalogSnapshotStore.VerificationReport(false, "error", getString(R.string.settings_offline_verify_missing), status)
                : catalogSnapshotStore.verifyStoredSnapshot(BuildConfig.CATALOG_SNAPSHOT_URL);
        String updatedAt = status.updatedAtMs <= 0L
                ? getString(R.string.diagnostics_value_unknown)
                : formatDateTime(status.updatedAtMs);
        String generatedAt = status.generatedAtMs <= 0L
                ? getString(R.string.diagnostics_value_unknown)
                : formatDateTime(status.generatedAtMs);
        String expiresAt = status.expiresAtMs <= 0L
                ? getString(R.string.diagnostics_value_unknown)
                : formatDateTime(status.expiresAtMs);
        String permissionsChangedAt = status.permissionsChangedAtMs <= 0L
                ? getString(R.string.diagnostics_value_no)
                : formatDateTime(status.permissionsChangedAtMs);
        return getString(
                R.string.offline_catalog_summary,
                BuildConfig.STANDALONE_MODE ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
                status.available ? status.expired ? getString(R.string.offline_catalog_value_expired) : getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
                updatedAt,
                generatedAt,
                expiresAt,
                status.channelCount,
                status.vodCount,
                status.epgChannelCount,
                status.epgProgramCount,
                status.epgUntilMs <= 0L ? getString(R.string.diagnostics_value_unknown) : formatDateTime(status.epgUntilMs),
                humanReadableSize(status.sizeBytes),
                status.schema == null || status.schema.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : status.schema,
                status.sourceUrl == null || status.sourceUrl.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : status.sourceUrl,
                status.sourceBaseUrl == null || status.sourceBaseUrl.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : status.sourceBaseUrl,
                status.deviceId == null || status.deviceId.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : status.deviceId,
                status.hasAccessToken ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
                status.subject == null || status.subject.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : status.subject,
                status.permissions == null || status.permissions.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : status.permissions,
                permissionsChangedAt,
                status.hasLastGoodBackup ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
                buildOfflineVerificationSummary(verification),
                buildShortFingerprint(status.payloadFingerprint)
        );
    }

    private String buildOfflineCatalogVerificationSummary() {
        CatalogSnapshotStore.VerificationReport report = catalogSnapshotStore == null
                ? new CatalogSnapshotStore.VerificationReport(false, "error", getString(R.string.settings_offline_verify_missing), null)
                : catalogSnapshotStore.verifyStoredSnapshot(BuildConfig.CATALOG_SNAPSHOT_URL);
        CatalogSnapshotStore.SnapshotStatus status = report == null ? null : report.status;
        String schema = status == null || status.schema == null || status.schema.trim().isEmpty()
                ? getString(R.string.diagnostics_value_unknown)
                : status.schema;
        String generatedAt = status == null || status.generatedAtMs <= 0L
                ? getString(R.string.diagnostics_value_unknown)
                : formatDateTime(status.generatedAtMs);
        String expiresAt = status == null || status.expiresAtMs <= 0L
                ? getString(R.string.diagnostics_value_unknown)
                : formatDateTime(status.expiresAtMs);
        String detail = report == null || report.message == null || report.message.trim().isEmpty()
                ? getString(R.string.diagnostics_value_unknown)
                : report.message.trim();
        return getString(
                R.string.offline_catalog_verify_summary,
                buildOfflineVerificationSummary(report),
                schema,
                generatedAt,
                expiresAt,
                status == null || status.sourceBaseUrl == null || status.sourceBaseUrl.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : status.sourceBaseUrl,
                status == null ? getString(R.string.diagnostics_value_unknown) : buildShortFingerprint(status.payloadFingerprint),
                detail
        );
    }

    private String buildOfflineVerificationSummary(CatalogSnapshotStore.VerificationReport report) {
        if (report == null) {
            return getString(R.string.diagnostics_value_unknown);
        }
        if ("ok".equalsIgnoreCase(report.state)) {
            return getString(R.string.settings_offline_verify_ok);
        }
        String detail = report.message == null || report.message.trim().isEmpty()
                ? getString(R.string.diagnostics_value_unknown)
                : report.message.trim();
        if ("warning".equalsIgnoreCase(report.state)) {
            return getString(R.string.settings_offline_verify_warning, detail);
        }
        return getString(R.string.settings_offline_verify_error, detail);
    }

    private String buildShortFingerprint(String fingerprint) {
        String clean = fingerprint == null ? "" : fingerprint.trim();
        if (clean.isEmpty()) {
            return getString(R.string.diagnostics_value_unknown);
        }
        if (clean.length() <= 12) {
            return clean;
        }
        return clean.substring(0, 12);
    }

    private void refreshOfflineCatalogFromSettings() {
        refreshOfflineCatalog(true, true);
    }

    private void refreshOfflineCatalog(boolean manual, boolean force) {
        refreshOfflineCatalog(manual, force, false);
    }

    private void refreshOfflineCatalog(boolean manual, boolean force, boolean preferFallbackOnFailure) {
        if (!BuildConfig.STANDALONE_MODE && !manual) {
            return;
        }
        if (catalogRepository == null || catalogSnapshotStore == null || offlineCatalogRefreshRunning) {
            return;
        }
        CatalogSnapshotStore.SnapshotStatus status = catalogSnapshotStore.getStatus(BuildConfig.CATALOG_SNAPSHOT_URL);
        CatalogSnapshotStore.SnapshotStatus statusBeforeRefresh = status;
        if (!status.hasAccessToken || status.sourceUrl.trim().isEmpty()) {
            if (manual) {
                showOfflineCatalogRecoveryDialogIfNeeded(new IllegalStateException(getString(R.string.settings_offline_next_sync_blocked)));
            }
            return;
        }
        if (!force && !shouldRefreshOfflineCatalog(status)) {
            if (manual) {
                showStatus(getString(R.string.offline_catalog_status_already_fresh));
            }
            return;
        }
        offlineCatalogRefreshRunning = true;
        if (manual) {
            showStatus(getString(R.string.offline_catalog_status_refreshing));
        }
        long startMs = System.currentTimeMillis();
        lastOfflineCatalogRefreshAttemptMs = startMs;
        lastOfflineCatalogRefreshError = "";
        boolean shouldFallbackOnFailure = preferFallbackOnFailure || allChannels.isEmpty();
        ioExecutor.execute(() -> {
            try {
                CatalogLoadResult result = catalogRepository.refreshSnapshotFromConfiguredUrl(BuildConfig.CATALOG_SNAPSHOT_URL);
                long durationMs = System.currentTimeMillis() - startMs;
                uiHandler.post(() -> {
                    offlineCatalogRefreshRunning = false;
                    lastCatalogLoadDurationMs = durationMs;
                    lastOfflineCatalogRefreshSuccessMs = System.currentTimeMillis();
                    lastOfflineCatalogRefreshError = "";
                    offlineCatalogRetryCount = 0;
                    uiHandler.removeCallbacks(offlineCatalogRetryRunnable);
                    CatalogSnapshotStore.SnapshotStatus afterStatus = catalogSnapshotStore == null
                            ? statusBeforeRefresh
                            : catalogSnapshotStore.getStatus(BuildConfig.CATALOG_SNAPSHOT_URL);
                    String refreshDetail = buildOfflineCatalogRefreshDetail(statusBeforeRefresh, afterStatus);
                    recordOfflineSyncEvent(getString(R.string.settings_offline_sync_catalog), true, durationMs, refreshDetail);
                    applyLoadedChannels(result);
                    if (manual) {
                        showStatus(refreshDetail);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "offline catalog refresh failed", e);
                long durationMs = System.currentTimeMillis() - startMs;
                CatalogLoadResult fallback = null;
                String fallbackError = "";
                if (shouldFallbackOnFailure) {
                    try {
                        fallback = catalogRepository.fetchLastKnownGoodSnapshotCatalog();
                    } catch (Exception fallbackErr) {
                        fallbackError = fallbackErr.getMessage();
                    }
                }
                CatalogLoadResult finalFallback = fallback;
                String finalFallbackError = fallbackError;
                uiHandler.post(() -> {
                    offlineCatalogRefreshRunning = false;
                    lastOfflineCatalogRefreshError = e.getMessage();
                    lastOfflineMaintenanceError = e.getMessage();
                    recordOfflineSyncEvent(getString(R.string.settings_offline_sync_catalog), false, durationMs, e.getMessage());
                    if (finalFallback != null) {
                        lastCatalogLoadDurationMs = durationMs;
                        applyLoadedChannels(finalFallback);
                        showStatus(getString(R.string.offline_catalog_status_using_last_good));
                        recordOfflineSyncEvent(getString(R.string.settings_offline_sync_catalog), true, durationMs, getString(R.string.offline_catalog_status_using_last_good));
                        return;
                    }
                    if (manual && shouldFallbackOnFailure && finalFallbackError != null && !finalFallbackError.trim().isEmpty()) {
                        lastOfflineCatalogRefreshError = e.getMessage() + " · " + finalFallbackError;
                    }
                    if (manual && !showOfflineCatalogRecoveryDialogIfNeeded(e)) {
                        showError(getString(R.string.error_load_channels, e.getMessage()));
                    } else if (!manual) {
                        scheduleOfflineCatalogRetryIfUseful(e);
                    }
                });
            }
        });
    }

    private String buildOfflineCatalogRefreshDetail(CatalogSnapshotStore.SnapshotStatus before, CatalogSnapshotStore.SnapshotStatus after) {
        if (after == null) {
            return getString(R.string.offline_catalog_status_refreshed);
        }
        boolean sameFingerprint = before != null
                && before.payloadFingerprint != null
                && !before.payloadFingerprint.trim().isEmpty()
                && before.payloadFingerprint.equals(after.payloadFingerprint);
        if (sameFingerprint) {
            return getString(R.string.offline_catalog_status_unchanged);
        }
        boolean permissionsChanged = before != null
                && before.permissionsFingerprint != null
                && !before.permissionsFingerprint.trim().isEmpty()
                && after.permissionsFingerprint != null
                && !after.permissionsFingerprint.trim().isEmpty()
                && !before.permissionsFingerprint.equals(after.permissionsFingerprint);
        if (permissionsChanged) {
            return getString(R.string.offline_catalog_status_permissions_changed);
        }
        if (before != null && before.expired && !after.expired) {
            return getString(R.string.offline_catalog_status_reactivated);
        }
        return getString(R.string.offline_catalog_status_refreshed);
    }

    private void repairOfflineCatalog() {
        if (!BuildConfig.STANDALONE_MODE) {
            showStatus(getString(R.string.offline_catalog_status_repair_not_needed));
            return;
        }
        showStatus(getString(R.string.offline_catalog_status_repairing));
        refreshOfflineCatalog(true, true, true);
    }

    private void refreshStandaloneCatalogInBackgroundIfPossible() {
        if (BuildConfig.STANDALONE_MODE && !allChannels.isEmpty() && isWithinStartupMaintenanceGrace()) {
            CatalogSnapshotStore.SnapshotStatus status = catalogSnapshotStore == null
                    ? null
                    : catalogSnapshotStore.getStatus(BuildConfig.CATALOG_SNAPSHOT_URL);
            if (shouldRefreshOfflineCatalog(status)) {
                uiHandler.removeCallbacks(offlineCatalogAutoRefreshRunnable);
                uiHandler.postDelayed(offlineCatalogAutoRefreshRunnable, startupMaintenanceGraceRemainingMs());
            }
            return;
        }
        refreshOfflineCatalog(false, false);
    }

    private boolean isWithinStartupMaintenanceGrace() {
        return startupMaintenanceGraceRemainingMs() > 0L;
    }

    private long startupMaintenanceGraceRemainingMs() {
        if (activityCreatedAtMs <= 0L) {
            return 0L;
        }
        return Math.max(0L, activityCreatedAtMs + OFFLINE_STARTUP_MAINTENANCE_GRACE_MS - System.currentTimeMillis());
    }

    private boolean showOfflineCatalogRecoveryDialogIfNeeded(Throwable error) {
        if (!BuildConfig.STANDALONE_MODE || !isAuthRelatedError(error)) {
            return false;
        }
        String message = error == null || error.getMessage() == null ? getString(R.string.error_unknown_reason) : error.getMessage();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.offline_catalog_reactivation_title)
                .setMessage(getString(R.string.offline_catalog_reactivation_message, classifyOperationalError(message), message))
                .setPositiveButton(R.string.offline_catalog_action_activate_code, (unused, which) -> startOfflineActivationCodeFlow())
                .setNeutralButton(R.string.offline_catalog_action_set_token, (unused, which) -> showOfflineCatalogTokenDialog())
                .setNegativeButton(R.string.dialog_close, null)
                .create();
        showTvDialog(dialog);
        return true;
    }

    private boolean isAuthRelatedError(Throwable error) {
        return isAuthRelatedMessage(error == null ? "" : error.getMessage());
    }

    private void recordOfflineSyncEvent(String type, boolean success, long durationMs, String detail) {
        if (prefs == null) {
            return;
        }
        try {
            org.json.JSONArray history = new org.json.JSONArray(prefs.getString(PREF_OFFLINE_SYNC_HISTORY, "[]"));
            org.json.JSONObject event = new org.json.JSONObject()
                    .put("ts", System.currentTimeMillis())
                    .put("type", type == null ? "" : type)
                    .put("success", success)
                    .put("duration_ms", Math.max(0L, durationMs))
                    .put("detail", detail == null ? "" : detail);
            org.json.JSONArray next = new org.json.JSONArray();
            next.put(event);
            for (int i = 0; i < history.length() && next.length() < OFFLINE_SYNC_HISTORY_LIMIT; i++) {
                next.put(history.optJSONObject(i));
            }
            prefs.edit().putString(PREF_OFFLINE_SYNC_HISTORY, next.toString()).apply();
        } catch (Exception e) {
            Log.d(TAG, "offline sync history write failed", e);
        }
    }

    private String buildOfflineSyncHistorySummary() {
        if (prefs == null) {
            return getString(R.string.settings_offline_sync_history_empty);
        }
        try {
            org.json.JSONArray history = new org.json.JSONArray(prefs.getString(PREF_OFFLINE_SYNC_HISTORY, "[]"));
            if (history.length() == 0) {
                return getString(R.string.settings_offline_sync_history_empty);
            }
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < history.length(); i++) {
                org.json.JSONObject event = history.optJSONObject(i);
                if (event == null) {
                    continue;
                }
                String type = event.optString("type", getString(R.string.diagnostics_value_unknown));
                boolean success = event.optBoolean("success", false);
                String state = success ? getString(R.string.settings_offline_sync_ok) : getString(R.string.settings_offline_sync_failed);
                String detail = event.optString("detail", "").trim();
                if (detail.length() > 120) {
                    detail = detail.substring(0, 117) + "...";
                }
                appendDiagnosticLine(out, getString(
                        R.string.settings_offline_sync_history_item,
                        formatDateTime(event.optLong("ts", 0L)),
                        type,
                        state,
                        event.optLong("duration_ms", 0L),
                        detail.isEmpty() ? getString(R.string.diagnostics_value_unknown) : detail
                ));
            }
            return out.length() == 0 ? getString(R.string.settings_offline_sync_history_empty) : out.toString();
        } catch (Exception e) {
            return getString(R.string.settings_offline_sync_history_error, e.getMessage());
        }
    }

    private boolean shouldRefreshOfflineCatalog(CatalogSnapshotStore.SnapshotStatus status) {
        if (status == null || !status.available || status.expired) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (status.updatedAtMs <= 0L || now - status.updatedAtMs >= OFFLINE_CATALOG_AUTO_REFRESH_MS) {
            return true;
        }
        return status.expiresAtMs > 0L && status.expiresAtMs - now <= OFFLINE_CATALOG_EXPIRY_REFRESH_MS;
    }

    private void runOfflineMaintenance(boolean manual) {
        if (!BuildConfig.STANDALONE_MODE) {
            return;
        }
        lastOfflineMaintenanceMs = System.currentTimeMillis();
        lastOfflineMaintenanceError = "";
        if (manual) {
            refreshOfflineCatalog(true, true);
            checkAppUpdateManually();
            return;
        }
        refreshStandaloneCatalogInBackgroundIfPossible();
        checkAppUpdate(false);
    }

    private void scheduleOfflineCatalogRetryIfUseful(Throwable error) {
        if (isAuthRelatedError(error)) {
            return;
        }
        offlineCatalogRetryCount = Math.min(offlineCatalogRetryCount + 1, 4);
        long delayMs = Math.min(OFFLINE_CATALOG_RETRY_MAX_MS, OFFLINE_CATALOG_RETRY_BASE_MS * offlineCatalogRetryCount);
        uiHandler.removeCallbacks(offlineCatalogRetryRunnable);
        uiHandler.postDelayed(offlineCatalogRetryRunnable, delayMs);
    }

    private void scheduleOfflineCatalogAutoRefresh() {
        uiHandler.removeCallbacks(offlineCatalogAutoRefreshRunnable);
        if (BuildConfig.STANDALONE_MODE) {
            long delayMs = OFFLINE_CATALOG_AUTO_REFRESH_MS;
            delayMs = Math.max(delayMs, startupMaintenanceGraceRemainingMs());
            uiHandler.postDelayed(offlineCatalogAutoRefreshRunnable, delayMs);
        }
    }

    private void showOfflineCatalogUrlDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(R.string.offline_catalog_url_hint);
        input.setText(catalogSnapshotStore == null ? BuildConfig.CATALOG_SNAPSHOT_URL : catalogSnapshotStore.getSourceUrl(BuildConfig.CATALOG_SNAPSHOT_URL));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.offline_catalog_action_set_url)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (unused, which) -> {
                    if (catalogSnapshotStore != null) {
                        catalogSnapshotStore.setSourceUrl(input.getText() == null ? "" : input.getText().toString());
                    }
                    showStatus(getString(R.string.offline_catalog_status_url_saved));
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        showTvDialog(dialog);
    }

    private void showOfflineCatalogTokenDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(R.string.offline_catalog_token_hint);
        String current = catalogSnapshotStore == null ? "" : catalogSnapshotStore.getAccessToken();
        input.setText(current == null || current.trim().isEmpty() ? "" : current);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.offline_catalog_action_set_token)
                .setMessage(catalogSnapshotStore == null ? "" : getString(R.string.offline_catalog_device_message, catalogSnapshotStore.getDeviceId()))
                .setView(input)
                .setPositiveButton(android.R.string.ok, (unused, which) -> {
                    if (catalogSnapshotStore != null) {
                        catalogSnapshotStore.setAccessToken(input.getText() == null ? "" : input.getText().toString());
                    }
                    showStatus(getString(R.string.offline_catalog_status_token_saved));
                })
                .setNeutralButton(R.string.offline_catalog_action_clear_token, (unused, which) -> {
                    if (catalogSnapshotStore != null) {
                        catalogSnapshotStore.setAccessToken("");
                    }
                    showStatus(getString(R.string.offline_catalog_status_token_cleared));
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        showTvDialog(dialog);
    }

    private void clearOfflineCatalog() {
        if (catalogSnapshotStore != null) {
            catalogSnapshotStore.clear();
        }
        showStatus(getString(R.string.offline_catalog_status_cleared));
    }

    private void showSettingsDiagnosticsDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.settings_section_diagnostics)
                .setMessage(buildSettingsDiagnosticsMessage())
                .setNeutralButton(R.string.settings_performance_clear_caches, (dialogInterface, which) -> clearRuntimeCaches())
                .setPositiveButton(R.string.dialog_close, null)
                .create();
        showTvDialog(dialog);
    }

    private String buildSettingsDiagnosticsMessage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        return getString(
                R.string.settings_diagnostics_message,
                getPackageName(),
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                baseUrl == null ? "" : baseUrl,
                selectedFilterKey == null ? "all" : selectedFilterKey,
                favoritesOnly ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
                prefs != null && prefs.getBoolean(PREF_STARTUP_HUB_DISABLED, false) ? getString(R.string.diagnostics_value_no) : getString(R.string.diagnostics_value_yes),
                recordingsAutoRefreshEnabled ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
                channels.size(),
                allChannels.size(),
                temporaryPlaybackModesByChannelId.size(),
                humanReadableSize(usedMemory),
                humanReadableSize(maxMemory),
                channelLogoCache.size(),
                streamInfoByChannelId.size(),
                lastCatalogLoadDurationMs,
                lastApplyChannelsDurationMs,
                lastEpgNowLoadDurationMs,
                lastImageCacheClearMs <= 0L ? getString(R.string.diagnostics_value_unknown) : new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(lastImageCacheClearMs))
        );
    }

    private void showResetSettingsDialog() {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.settings_reset_search));
        actions.add(() -> confirmSettingsAction(R.string.settings_reset_search, R.string.settings_confirm_reset_search, this::clearGlobalSearchRecents));
        options.add(getString(R.string.settings_reset_playback));
        actions.add(() -> confirmSettingsAction(R.string.settings_reset_playback, R.string.settings_confirm_reset_playback, this::resetPlaybackSettings));
        options.add(getString(R.string.settings_reset_startup));
        actions.add(() -> confirmSettingsAction(R.string.settings_reset_startup, R.string.settings_confirm_reset_startup, this::resetStartupSettings));
        options.add(getString(R.string.settings_reset_local_data));
        actions.add(() -> confirmSettingsAction(R.string.settings_reset_local_data, R.string.settings_confirm_reset_local_data, this::resetLocalData));
        options.add(getString(R.string.settings_action_view_summary));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_reset, getString(R.string.settings_reset_summary)));
        showTvOptionsDialog(R.string.settings_section_reset, null, options, actions);
    }

    private void checkAppUpdateOnStartup() {
        checkAppUpdate(false);
    }

    private void scheduleAppUpdateCheckOnStartup() {
        if (BuildConfig.STANDALONE_MODE) {
            uiHandler.postDelayed(this::checkAppUpdateOnStartup, OFFLINE_APP_UPDATE_STARTUP_DELAY_MS);
        } else {
            checkAppUpdateOnStartup();
        }
    }

    private void checkAppUpdateManually() {
        checkAppUpdate(true);
    }

    private void checkAppUpdate(boolean manual) {
        if (appUpdateManager == null || appUpdateCheckRunning) {
            return;
        }
        appUpdateCheckRunning = true;
        if (manual) {
            showStatus(getString(R.string.app_update_status_checking));
        }
        long startMs = System.currentTimeMillis();
        ioExecutor.execute(() -> {
            try {
                AppUpdateManager.UpdateInfo info = appUpdateManager.fetchLatest(BuildConfig.OFFLINE_BASE_URL);
                long durationMs = System.currentTimeMillis() - startMs;
                uiHandler.post(() -> {
                    appUpdateCheckRunning = false;
                    lastKnownAppUpdateInfo = info;
                    lastAppUpdateCheckMs = System.currentTimeMillis();
                    lastAppUpdateError = "";
                    recordOfflineSyncEvent(
                            getString(R.string.settings_offline_sync_app_update),
                            true,
                            durationMs,
                            info.isNewerThanCurrent()
                                    ? getString(R.string.settings_update_state_available_short, safeUpdateVersionName(info), info.versionCode)
                                    : getString(R.string.app_update_none, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
                    );
                    if (info.isNewerThanCurrent()) {
                        int lastPrompted = prefs == null ? 0 : prefs.getInt(PREF_LAST_UPDATE_PROMPT_VERSION_CODE, 0);
                        if (!manual && !info.required && lastPrompted >= info.versionCode) {
                            return;
                        }
                        if (prefs != null) {
                            prefs.edit().putInt(PREF_LAST_UPDATE_PROMPT_VERSION_CODE, info.versionCode).apply();
                        }
                        showAppUpdateAvailableDialog(info);
                    } else if (manual) {
                        showSettingsInfoDialog(R.string.app_update_action_check, buildAppUpdateStateSummary());
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "app update check failed", e);
                long durationMs = System.currentTimeMillis() - startMs;
                uiHandler.post(() -> {
                    appUpdateCheckRunning = false;
                    lastAppUpdateCheckMs = System.currentTimeMillis();
                    lastAppUpdateError = e.getMessage();
                    lastOfflineMaintenanceError = e.getMessage();
                    recordOfflineSyncEvent(getString(R.string.settings_offline_sync_app_update), false, durationMs, e.getMessage());
                    if (manual) {
                        showError(getString(R.string.app_update_error, e.getMessage()));
                    }
                });
            }
        });
    }

    private void showAppUpdateAvailableDialog(AppUpdateManager.UpdateInfo info) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_update_available_title, safeUpdateVersionName(info)))
                .setMessage(buildAppUpdateMessage(info))
                .setPositiveButton(R.string.app_update_action_install, (unused, which) -> downloadAndInstallAppUpdate(info));
        if (info.required) {
            builder.setNegativeButton(R.string.dialog_close, null);
        } else {
            builder.setNegativeButton(R.string.app_update_action_later, null);
        }
        showTvDialog(builder.create());
    }

    private void downloadAndInstallAppUpdate(AppUpdateManager.UpdateInfo info) {
        showStatus(getString(R.string.app_update_status_downloading));
        ioExecutor.execute(() -> {
            try {
                File apk = appUpdateManager.downloadApk(info, (done, total) -> {
                    if (total > 0L) {
                        int pct = (int) Math.max(0L, Math.min(100L, (done * 100L) / total));
                        uiHandler.post(() -> showStatus(getString(R.string.app_update_status_downloading_pct, pct)));
                    }
                });
                uiHandler.post(() -> {
                    showStatus(getString(R.string.app_update_status_installing));
                    try {
                        appUpdateManager.installApk(apk);
                    } catch (Exception e) {
                        Log.e(TAG, "app update install failed", e);
                        showError(getString(R.string.app_update_error, e.getMessage()));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "app update download failed", e);
                uiHandler.post(() -> showError(getString(R.string.app_update_error, e.getMessage())));
            }
        });
    }

    private void showPostUpdateNotesIfNeeded() {
        if (prefs == null || appUpdateManager == null) {
            return;
        }
        int lastSeen = prefs.getInt(PREF_LAST_SEEN_APP_VERSION_CODE, 0);
        if (lastSeen >= BuildConfig.VERSION_CODE) {
            return;
        }
        prefs.edit().putInt(PREF_LAST_SEEN_APP_VERSION_CODE, BuildConfig.VERSION_CODE).apply();
        ioExecutor.execute(() -> {
            try {
                AppUpdateManager.UpdateInfo info = appUpdateManager.fetchLatest(BuildConfig.OFFLINE_BASE_URL);
                if (info.versionCode == BuildConfig.VERSION_CODE && !info.changelog.isEmpty()) {
                    uiHandler.post(() -> showSettingsInfoDialog(R.string.app_update_installed_title, buildAppInstalledMessage(info)));
                }
            } catch (Exception e) {
                Log.d(TAG, "post-update notes unavailable", e);
            }
        });
    }

    private String buildAppUpdateMessage(AppUpdateManager.UpdateInfo info) {
        return getString(
                R.string.app_update_available_message,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                safeUpdateVersionName(info),
                info.versionCode,
                buildChangelogText(info)
        );
    }

    private String buildAppInstalledMessage(AppUpdateManager.UpdateInfo info) {
        return getString(R.string.app_update_installed_message, safeUpdateVersionName(info), buildChangelogText(info));
    }

    private String buildChangelogText(AppUpdateManager.UpdateInfo info) {
        if (info == null || info.changelog == null || info.changelog.isEmpty()) {
            return getString(R.string.diagnostics_value_unknown);
        }
        StringBuilder out = new StringBuilder();
        for (String item : info.changelog) {
            if (item == null || item.trim().isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append("- ").append(item.trim());
        }
        return out.length() == 0 ? getString(R.string.diagnostics_value_unknown) : out.toString();
    }

    private String safeUpdateVersionName(AppUpdateManager.UpdateInfo info) {
        if (info == null || info.versionName == null || info.versionName.trim().isEmpty()) {
            return String.valueOf(info == null ? 0 : info.versionCode);
        }
        return info.versionName.trim();
    }

    private void showSettingsInfoDialog(int titleResId, String message) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(titleResId)
                .setMessage(message == null || message.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : message)
                .setPositiveButton(R.string.dialog_close, null)
                .create();
        showTvDialog(dialog);
    }

    private void confirmSettingsAction(int titleResId, int messageResId, Runnable action) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(titleResId)
                .setMessage(messageResId)
                .setPositiveButton(android.R.string.ok, (unused, which) -> {
                    if (action != null) {
                        action.run();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        showTvDialog(dialog);
    }

    private void clearPlaybackModes() {
        temporaryPlaybackModesByChannelId.clear();
        if (prefs != null) {
            prefs.edit().remove(PREF_PLAYBACK_MODES).apply();
        }
        if (playbackModeStore != null) {
            playbackModeStore.load();
        }
        showStatus(getString(R.string.settings_status_playback_modes_cleared));
    }

    private void togglePlaybackRepair() {
        playbackRepairEnabled = !playbackRepairEnabled;
        if (prefs != null) {
            prefs.edit().putBoolean(PREF_PLAYBACK_REPAIR_ENABLED, playbackRepairEnabled).apply();
        }
        showStatus(getString(playbackRepairEnabled ? R.string.status_playback_repair_enabled : R.string.status_playback_repair_disabled));
    }

    private void clearLearnedPlaybackModes() {
        learnedPlaybackModesByChannelId.clear();
        if (prefs != null) {
            prefs.edit().remove(PREF_PLAYBACK_LEARNED_MODES).apply();
        }
        showStatus(getString(R.string.settings_status_learned_routes_cleared));
    }

    private void clearRuntimeCaches() {
        streamInfoByChannelId.clear();
        epgNowByChannelId.clear();
        channelLogoCache.evictAll();
        lastImageCacheClearMs = System.currentTimeMillis();
        try {
            Glide.get(this).clearMemory();
            ioExecutor.execute(() -> {
                try {
                    Glide.get(getApplicationContext()).clearDiskCache();
                } catch (Exception e) {
                    Log.w(TAG, "failed to clear glide disk cache", e);
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "failed to clear glide memory cache", e);
        }
        showStatus(getString(R.string.settings_status_runtime_caches_cleared));
    }

    private void clearAllPlaybackDiagnostics() {
        if (playbackDiagnosticsStore != null) {
            playbackDiagnosticsStore.clearAll();
        }
        if (playerController != null) {
            playerController.clearLastError();
        }
        showStatus(getString(R.string.status_diagnostics_history_cleared));
    }

    private void clearGlobalSearchRecents() {
        globalSearchRecents.clear();
        if (prefs != null) {
            prefs.edit().remove(PREF_GLOBAL_SEARCH_RECENTS).apply();
        }
        showStatus(getString(R.string.settings_status_searches_cleared));
    }

    private void removeGlobalSearchRecent(String query) {
        String value = cleanText(query);
        if (value.isEmpty()) {
            return;
        }
        if (globalSearchRecents.remove(value)) {
            saveGlobalSearchRecents();
            showStatus(getString(R.string.global_search_history_deleted));
        }
    }

    private void clearAllVodProgress() {
        rememberCurrentVodPosition();
        vodResumePositions.clear();
        if (prefs != null) {
            prefs.edit().remove(PREF_VOD_RESUME_POSITIONS).apply();
        }
        showStatus(getString(R.string.settings_status_vod_progress_cleared));
    }

    private void clearAllRecordingProgress() {
        rememberCurrentRecordingPosition();
        recordingResumePositions.clear();
        if (prefs != null) {
            prefs.edit().remove(PREF_RECORDING_RESUME_POSITIONS).apply();
        }
        if (recordingsAdapter != null) {
            recordingsAdapter.notifyDataSetChanged();
        }
        updateRecordingsDetailPanel();
        showStatus(getString(R.string.settings_status_recording_progress_cleared));
    }

    private void clearRecentChannels() {
        if (prefs != null) {
            prefs.edit().remove(PREF_RECENT_CHANNELS).apply();
        }
        if (recentChannelsStore != null) {
            recentChannelsStore.load();
        }
        updateQuickAccessButtons();
        updateTouchHomeHub();
        showStatus(getString(R.string.settings_status_recent_channels_cleared));
    }

    private void clearFavorites() {
        favoriteChannelIds.clear();
        if (prefs != null) {
            prefs.edit().remove(PREF_FAVORITES).remove(PREF_FAVORITE_ORDER).putBoolean(PREF_FAVORITES_ONLY, false).apply();
        }
        favoritesOnly = false;
        if (favoriteOrderStore != null) {
            favoriteOrderStore.load();
        }
        for (ChannelItem item : allChannels) {
            if (item != null) {
                item.favorite = false;
            }
        }
        refreshLocalChannelFilters(lastChannelId);
        channelAdapter.notifyDataSetChanged();
        updateQuickAccessButtons();
        updateTouchHomeHub();
        showStatus(getString(R.string.settings_status_favorites_cleared));
    }

    private void resetListsAndProfiles() {
        if (prefs != null) {
            prefs.edit().remove(PREF_CHANNEL_COLLECTIONS).remove(PREF_CHANNEL_PROFILES).apply();
        }
        if (channelCollectionStore != null) {
            channelCollectionStore.load();
        }
        if (channelProfileStore != null) {
            channelProfileStore.load();
        }
        refreshLocalChannelFilters(lastChannelId);
        channelAdapter.notifyDataSetChanged();
        updateFilterText();
        updateTouchHomeHub();
        showStatus(getString(R.string.settings_status_lists_profiles_reset));
    }

    private void resetPlaybackSettings() {
        clearPlaybackModes();
        clearLearnedPlaybackModes();
        playbackRepairEnabled = true;
        if (prefs != null) {
            prefs.edit().putBoolean(PREF_PLAYBACK_REPAIR_ENABLED, true).apply();
        }
        clearAllPlaybackDiagnostics();
    }

    private void resetStartupSettings() {
        if (prefs != null) {
            prefs.edit()
                    .remove(PREF_STARTUP_HUB_DISABLED)
                    .remove(PREF_LAST_CHANNEL_ID)
                    .remove(PREF_LAST_FILTER_KEY)
                    .remove(PREF_FAVORITES_ONLY)
                    .remove(PREF_LAST_VOD_ID)
                    .apply();
        }
        lastVodId = "";
        selectedFilterKey = "all";
        favoritesOnly = false;
        persistNavigationState();
        updateFilterText();
        updateTouchHomeHub();
        showStatus(getString(R.string.settings_status_startup_reset));
    }

    private void resetLocalData() {
        clearAllVodProgress();
        clearAllRecordingProgress();
        clearRecentChannels();
        clearGlobalSearchRecents();
    }

    private void prepareModalSurface() {
        clearQuickSearchOverlay();
        hideOverlay();
        hideRecordingsPanel();
        if (touchHomeHub != null) {
            touchHomeHub.setVisibility(View.GONE);
        }
        if (timeshiftBarContainer != null) {
            timeshiftBarContainer.setVisibility(View.GONE);
        }
    }

    private void showTvDialog(AlertDialog dialog) {
        if (dialog == null) {
            return;
        }
        dialog.setOnDismissListener(d -> enableImmersiveMode());
        dialog.setOnShowListener(d -> {
            android.widget.ListView listView = dialog.getListView();
            if (listView != null) {
                listView.setFocusable(true);
                listView.setFocusableInTouchMode(true);
                listView.requestFocus();
                if (listView.getCount() > 0) {
                    listView.setSelection(0);
                }
            }
        });
        dialog.show();
    }

    private void showTvOptionsDialog(int titleResId, String message, List<String> options, List<Runnable> actions) {
        prepareModalSurface();
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(titleResId)
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (which >= 0 && which < actions.size()) {
                        actions.get(which).run();
                    }
                })
                .setNegativeButton(R.string.dialog_close, null);
        if (message != null && !message.trim().isEmpty()) {
            builder.setMessage(message);
        }
        showTvDialog(builder.create());
    }

    private void maybeShowStartupHub() {
        if (startupHubShown || (prefs != null && prefs.getBoolean(PREF_STARTUP_HUB_DISABLED, false))) {
            return;
        }
        startupHubShown = true;
        uiHandler.postDelayed(this::loadStartupHubStateAndShow, 700L);
    }

    private void loadStartupHubStateAndShow() {
        if (isFinishing()) {
            return;
        }
        ChannelItem current = getCurrentPlaybackChannelItem();
        ChannelItem lastVod = findChannelItemById(lastVodId);
        ioExecutor.execute(() -> {
            RecordingsRepository.RecordingItem resumeRecording = null;
            String recordingBasePath = "";
            int completedCount = 0;
            int scheduledCount = 0;
            if (!isOfflineRecordingsDisabled()) {
                try {
                    RecordingsRepository.RecordingsResult completed = recordingsRepository.fetchCompletedRecordings();
                    recordingBasePath = completed == null ? "" : completed.basePath;
                    completedCount = completed == null || completed.items == null ? 0 : completed.items.size();
                    resumeRecording = findResumeRecording(completed);
                } catch (Exception e) {
                    Log.w(TAG, "startup completed recordings summary failed", e);
                }
                try {
                    RecordingsRepository.RecordingsResult scheduled = recordingsRepository.fetchScheduledRecordings();
                    scheduledCount = scheduled == null || scheduled.items == null ? 0 : scheduled.items.size();
                } catch (Exception e) {
                    Log.w(TAG, "startup scheduled recordings summary failed", e);
                }
            }
            StartupHubState state = new StartupHubState(current, lastVod, resumeRecording, recordingBasePath, completedCount, scheduledCount);
            uiHandler.post(() -> showStartupHubDialog(state));
        });
    }

    private RecordingsRepository.RecordingItem findResumeRecording(RecordingsRepository.RecordingsResult completed) {
        if (completed == null || completed.items == null || completed.items.isEmpty() || recordingResumePositions.isEmpty()) {
            return null;
        }
        RecordingsRepository.RecordingItem best = null;
        long bestPosition = 0L;
        for (RecordingsRepository.RecordingItem item : completed.items) {
            if (item == null || !item.playable) {
                continue;
            }
            long position = getRecordingResumePosition(item.id);
            if (position > bestPosition && position > 30_000L) {
                best = item;
                bestPosition = position;
            }
        }
        return best;
    }

    private RecordingsRepository.RecordingItem findLocalResumeRecording() {
        RecordingsRepository.RecordingsResult result = recordingsController.getCurrentResult();
        return findResumeRecording(result);
    }

    private void showStartupHubDialog(StartupHubState state) {
        if (isFinishing()) {
            return;
        }
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        ChannelItem current = state == null ? getCurrentPlaybackChannelItem() : state.currentChannel;
        options.add(getString(R.string.startup_hub_continue_channel, current == null ? getString(R.string.diagnostics_value_unknown) : displayName(current)));
        actions.add(() -> {
            if (current != null) {
                tuneChannelById(current.id);
            }
        });
        ChannelItem lastVod = state == null ? findChannelItemById(lastVodId) : state.lastVod;
        if (lastVod != null && lastVod.isVod) {
            long resumeMs = getVodResumePosition(lastVod.id);
            options.add(resumeMs > 30_000L
                    ? getString(R.string.startup_hub_continue_vod_progress, displayName(lastVod), formatDurationShort(resumeMs))
                    : getString(R.string.startup_hub_continue_vod, displayName(lastVod)));
            actions.add(() -> showVodInfoDialog(lastVod));
        }
        RecordingsRepository.RecordingItem resumeRecording = state == null ? null : state.resumeRecording;
        if (resumeRecording != null) {
            long resumeMs = getRecordingResumePosition(resumeRecording.id);
            String basePath = state.resumeRecordingBasePath == null ? "" : state.resumeRecordingBasePath;
            options.add(getString(R.string.startup_hub_continue_recording, buildRecordingTitle(resumeRecording), formatPlaybackPosition(resumeMs)));
            actions.add(() -> playRecording(resumeRecording, basePath));
        }
        options.add(getString(R.string.quick_hub_global_search));
        actions.add(this::showGlobalSearchDialog);
        options.add(getString(R.string.touch_home_button_tv, countItemsForQuickTarget("tv")));
        actions.add(() -> applyQuickOverlayTarget("tv"));
        if (shouldShowGenericVodQuickTarget(false)) {
            options.add(getString(R.string.touch_home_button_vod, countItemsForQuickTarget("vod")));
            actions.add(() -> applyQuickOverlayTarget("vod"));
        }
        if (shouldShowGenericVodQuickTarget(true)) {
            options.add(getString(R.string.touch_home_button_adult, countItemsForQuickTarget("vod-adult")));
            actions.add(() -> applyQuickOverlayTarget("vod-adult"));
        }
        if (!isOfflineRecordingsDisabled()) {
            options.add(getString(R.string.quick_hub_recordings));
            actions.add(this::openRecordingsBrowser);
        }
        options.add(getString(R.string.quick_hub_recent));
        actions.add(this::showRecentChannelsQuickDialog);
        options.add(getString(R.string.quick_hub_favorites));
        actions.add(this::showFavoriteChannelsQuickDialog);
        options.add(getString(R.string.quick_hub_lists));
        actions.add(this::showPersonalListsManagerDialog);
        options.add(getString(R.string.startup_hub_disable));
        actions.add(this::disableStartupHub);
        options.add(getString(R.string.tools_menu_install_status));
        actions.add(this::showInstallStatusDialog);

        showTvOptionsDialog(
                R.string.startup_hub_title,
                buildStartupHubMessage(state),
                options,
                actions
        );
    }

    private String buildStartupHubMessage(StartupHubState state) {
        int vodCount = countItemsForQuickTarget("vod");
        int adultCount = countItemsForQuickTarget("vod-adult");
        int favoriteCount = buildFavoriteQuickChannels().size();
        int recentCount = buildRecentQuickChannels().size();
        int completed = state == null ? 0 : state.completedRecordings;
        int scheduled = state == null ? 0 : state.scheduledRecordings;
        if (isOfflineRecordingsDisabled()) {
            return getString(
                    R.string.startup_hub_message_offline,
                    BuildConfig.VERSION_NAME,
                    buildCurrentFilterLabel(),
                    channels.size(),
                    vodCount,
                    adultCount,
                    favoriteCount,
                    recentCount
            );
        }
        return getString(R.string.startup_hub_message_v2, BuildConfig.VERSION_NAME, buildCurrentFilterLabel(), channels.size(), vodCount, adultCount, favoriteCount, recentCount, completed, scheduled);
    }

    private void disableStartupHub() {
        if (prefs != null) {
            prefs.edit().putBoolean(PREF_STARTUP_HUB_DISABLED, true).apply();
        }
        showStatus(getString(R.string.status_startup_hub_disabled));
    }

    private void enableStartupHub() {
        if (prefs != null) {
            prefs.edit().putBoolean(PREF_STARTUP_HUB_DISABLED, false).apply();
        }
        showStatus(getString(R.string.status_startup_hub_enabled));
    }

    private String buildCurrentFilterLabel() {
        if (favoritesOnly || "favorites".equals(selectedFilterKey)) {
            return getString(R.string.touch_home_filter_favorites);
        }
        for (ChannelFilter filter : filters) {
            if (filter != null && selectedFilterKey != null && selectedFilterKey.equals(filter.key) && filter.label != null && !filter.label.trim().isEmpty()) {
                return filter.label.trim();
            }
        }
        return getString(R.string.touch_home_filter_all);
    }

    private void showQuickHubDialog() {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.tools_section_current_channel));
        actions.add(this::showCurrentChannelQuickActionsDialog);
        options.add(getString(R.string.quick_hub_continue));
        actions.add(() -> {
            ChannelItem current = getCurrentPlaybackChannelItem();
            if (current != null) {
                tuneChannelById(current.id);
            }
        });
        options.add(getString(R.string.quick_hub_continue_vod));
        actions.add(this::openLastVod);
        RecordingsRepository.RecordingItem resumeRecording = isOfflineRecordingsDisabled() ? null : findLocalResumeRecording();
        if (resumeRecording != null) {
            options.add(getString(R.string.quick_hub_continue_recording, buildRecordingTitle(resumeRecording)));
            actions.add(() -> openRecordingsBrowser());
        }
        options.add(getString(R.string.quick_hub_global_search));
        actions.add(this::showGlobalSearchDialog);
        options.add(getString(R.string.tools_section_vod));
        actions.add(this::showVodLibraryDialog);
        options.add(getString(R.string.tools_section_navigation));
        actions.add(this::showNavigationToolsDialog);
        options.add(getString(R.string.quick_hub_recent));
        actions.add(this::showRecentChannelsQuickDialog);
        options.add(getString(R.string.quick_hub_favorites));
        actions.add(this::showFavoriteChannelsQuickDialog);
        options.add(getString(R.string.quick_hub_lists));
        actions.add(this::showPersonalListsManagerDialog);
        if (!isOfflineRecordingsDisabled()) {
            options.add(getString(R.string.quick_hub_recordings));
            actions.add(this::openRecordingsBrowser);
        }
        options.add(getString(R.string.quick_hub_timeline));
        actions.add(this::openTimelineGuideAroundSelection);
        options.add(getString(R.string.quick_hub_epg_search));
        actions.add(this::showEpgSearchDialog);
        options.add(getString(R.string.tools_section_playback));
        actions.add(this::showPlaybackToolsDialog);
        if (prefs != null && prefs.getBoolean(PREF_STARTUP_HUB_DISABLED, false)) {
            options.add(getString(R.string.quick_hub_enable_startup));
            actions.add(this::enableStartupHub);
        }
        options.add(getString(R.string.tools_menu_title_short));
        actions.add(this::showV12ToolsMenu);
        showTvOptionsDialog(R.string.title_quick_hub, null, options, actions);
    }

    private void showVodSearchDialog() {
        showVodSearchDialog("");
    }

    private void showVodSearchDialog(String initialQuery) {
        clearQuickSearchOverlay();
        hideOverlay();
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(R.string.vod_search_hint);
        if (initialQuery != null && !initialQuery.trim().isEmpty()) {
            input.setText(initialQuery.trim());
            input.setSelectAllOnFocus(true);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_vod_search)
                .setView(input)
                .setPositiveButton(R.string.search_channel_dialog_action, (dialog, which) -> {
                    String query = input.getText() == null ? "" : input.getText().toString();
                    showVodSearchResults(query);
                })
                .setNeutralButton(R.string.vod_search_all, (dialog, which) -> showVodSearchResults(""))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showVodSearchResults(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            showVodVisualLibraryDialog();
            return;
        }
        showVodVisualLibraryDialog(VodVisualTypeFilter.ALL, VodVisualPlatformFilter.ALL, VodVisualStatusFilter.ALL, VodVisualSortFilter.SMART, trimmed);
    }

    private void showVodLibraryList(int titleResId, List<ChannelItem> items, boolean progressFirst) {
        if (progressFirst) {
            sortVodLibraryItems(items);
        }
        showQuickChannelListDialog(
                getString(titleResId),
                items,
                getString(R.string.vod_library_empty),
                item -> showVodInfoDialog(item)
        );
    }

    private String buildVodLibrarySummary() {
        int total = 0;
        int adult = 0;
        int progress = 0;
        for (ChannelItem item : allChannels) {
            if (item == null || !item.isVod) {
                continue;
            }
            total++;
            if (item.isAdultVod) {
                adult++;
            }
            if (getVodResumePosition(item.id) > 30_000L) {
                progress++;
            }
        }
        return getString(R.string.vod_library_summary, total, adult, progress);
    }

    private String buildVodLibraryOptionLabel(int titleResId, List<ChannelItem> items) {
        int count = items == null ? 0 : items.size();
        return getString(titleResId) + " (" + count + ")";
    }

    private enum VodSortMode {
        ALPHA,
        YEAR_DESC,
        DURATION_DESC
    }

    private enum VodVisualTypeFilter {
        GENERAL("General"),
        ADULT("Adulto"),
        ALL("Todo");

        final String label;

        VodVisualTypeFilter(String label) {
            this.label = label;
        }

        VodVisualTypeFilter next() {
            VodVisualTypeFilter[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private enum VodVisualPlatformFilter {
        ALL("Todo"),
        TIVIFY("Tivify"),
        RUNTIME("Runtime"),
        OTHER("Otros");

        final String label;

        VodVisualPlatformFilter(String label) {
            this.label = label;
        }

        VodVisualPlatformFilter next() {
            VodVisualPlatformFilter[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private enum VodVisualStatusFilter {
        ALL("Todo"),
        CONTINUE("Continuar"),
        PROGRESS("Con progreso"),
        NOT_STARTED("Sin empezar");

        final String label;

        VodVisualStatusFilter(String label) {
            this.label = label;
        }

        VodVisualStatusFilter next() {
            VodVisualStatusFilter[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private enum VodVisualSortFilter {
        SMART("Recomendado"),
        ALPHA("A-Z"),
        RECENT("Recientes"),
        YEAR("Ano"),
        DURATION("Duracion");

        final String label;

        VodVisualSortFilter(String label) {
            this.label = label;
        }

        VodVisualSortFilter next() {
            VodVisualSortFilter[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private boolean isDefaultVodVisualFilter(VodVisualTypeFilter typeFilter, VodVisualPlatformFilter platformFilter, VodVisualStatusFilter statusFilter, VodVisualSortFilter sortFilter) {
        return typeFilter == VodVisualTypeFilter.GENERAL
                && platformFilter == VodVisualPlatformFilter.ALL
                && statusFilter == VodVisualStatusFilter.ALL
                && sortFilter == VodVisualSortFilter.SMART;
    }

    private List<ChannelItem> buildVodVisualFilteredItems(VodVisualTypeFilter typeFilter, VodVisualPlatformFilter platformFilter, VodVisualStatusFilter statusFilter, VodVisualSortFilter sortFilter) {
        return buildVodVisualFilteredItems(typeFilter, platformFilter, statusFilter, sortFilter, "");
    }

    private List<ChannelItem> buildVodVisualFilteredItems(VodVisualTypeFilter typeFilter, VodVisualPlatformFilter platformFilter, VodVisualStatusFilter statusFilter, VodVisualSortFilter sortFilter, String query) {
        String trimmedQuery = query == null ? "" : query.trim();
        List<ChannelItem> items = new ArrayList<>();
        for (ChannelItem item : allChannels) {
            if (item == null || !item.isVod || !matchesVodVisualType(item, typeFilter) || !matchesVodVisualPlatform(item, platformFilter) || !matchesVodVisualStatus(item, statusFilter)) {
                continue;
            }
            if (!matchesVodSearchQuery(item, trimmedQuery)) {
                continue;
            }
            items.add(item);
        }
        sortVodVisualFilteredItems(items, sortFilter);
        return items;
    }

    private boolean matchesVodSearchQuery(ChannelItem item, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        if (item == null) {
            return false;
        }
        String haystack = displayName(item) + " " + item.group + " " + item.platformName + " " + item.vodYear;
        return matchesSearch(haystack, query);
    }

    private String buildVodSearchSummary(String query) {
        List<ChannelItem> all = buildVodVisualFilteredItems(VodVisualTypeFilter.ALL, VodVisualPlatformFilter.ALL, VodVisualStatusFilter.ALL, VodVisualSortFilter.SMART, query);
        int tivify = 0;
        int runtime = 0;
        int adult = 0;
        int progress = 0;
        for (ChannelItem item : all) {
            if (item == null) {
                continue;
            }
            if (item.isAdultVod) {
                adult++;
            }
            if (matchesVodVisualPlatform(item, VodVisualPlatformFilter.TIVIFY)) {
                tivify++;
            } else if (matchesVodVisualPlatform(item, VodVisualPlatformFilter.RUNTIME)) {
                runtime++;
            }
            if (getVodResumePosition(item.id) > 30_000L) {
                progress++;
            }
        }
        return getString(R.string.vod_search_visual_summary, all.size(), tivify, runtime, adult, progress);
    }

    private boolean matchesVodVisualType(ChannelItem item, VodVisualTypeFilter typeFilter) {
        if (typeFilter == VodVisualTypeFilter.ALL) {
            return true;
        }
        return typeFilter == VodVisualTypeFilter.ADULT ? item.isAdultVod : !item.isAdultVod;
    }

    private boolean matchesVodVisualPlatform(ChannelItem item, VodVisualPlatformFilter platformFilter) {
        if (platformFilter == VodVisualPlatformFilter.ALL) {
            return true;
        }
        String filterKey = item.vodFilterKey == null ? "" : item.vodFilterKey.toLowerCase(Locale.ROOT);
        String platform = item.platformName == null ? "" : item.platformName.toLowerCase(Locale.ROOT);
        boolean isTivify = filterKey.contains("tivify") || platform.contains("tivify");
        boolean isRuntime = filterKey.contains("runtime") || platform.contains("runtime");
        if (platformFilter == VodVisualPlatformFilter.TIVIFY) {
            return isTivify;
        }
        if (platformFilter == VodVisualPlatformFilter.RUNTIME) {
            return isRuntime;
        }
        return !isTivify && !isRuntime;
    }

    private boolean matchesVodVisualStatus(ChannelItem item, VodVisualStatusFilter statusFilter) {
        if (statusFilter == VodVisualStatusFilter.ALL) {
            return true;
        }
        long progress = getVodResumePosition(item == null ? null : item.id);
        if (statusFilter == VodVisualStatusFilter.CONTINUE || statusFilter == VodVisualStatusFilter.PROGRESS) {
            return progress > 30_000L;
        }
        return progress <= 0L;
    }

    private void sortVodVisualFilteredItems(List<ChannelItem> items, VodVisualSortFilter sortFilter) {
        if (items == null) {
            return;
        }
        if (sortFilter == VodVisualSortFilter.ALPHA) {
            items.sort((left, right) -> displayName(left).compareToIgnoreCase(displayName(right)));
        } else if (sortFilter == VodVisualSortFilter.YEAR) {
            items.sort((left, right) -> {
                int yearCompare = Integer.compare(parseVodYear(right), parseVodYear(left));
                return yearCompare != 0 ? yearCompare : displayName(left).compareToIgnoreCase(displayName(right));
            });
        } else if (sortFilter == VodVisualSortFilter.DURATION) {
            items.sort((left, right) -> {
                int durationCompare = Long.compare(right == null ? 0L : right.vodDurationSeconds, left == null ? 0L : left.vodDurationSeconds);
                return durationCompare != 0 ? durationCompare : displayName(left).compareToIgnoreCase(displayName(right));
            });
        } else if (sortFilter == VodVisualSortFilter.RECENT) {
            Map<String, Integer> recentRanks = buildRecentVodRankMap();
            items.sort((left, right) -> {
                int leftRank = recentRanks.getOrDefault(left == null ? "" : left.id, Integer.MAX_VALUE);
                int rightRank = recentRanks.getOrDefault(right == null ? "" : right.id, Integer.MAX_VALUE);
                int rankCompare = Integer.compare(leftRank, rightRank);
                return rankCompare != 0 ? rankCompare : displayName(left).compareToIgnoreCase(displayName(right));
            });
        } else {
            sortVodLibraryItems(items);
        }
    }

    private Map<String, Integer> buildRecentVodRankMap() {
        Map<String, Integer> ranks = new HashMap<>();
        List<RecentChannelsStore.RecentChannelItem> recents = recentChannelsStore == null ? new ArrayList<>() : recentChannelsStore.getItems();
        int rank = 0;
        for (RecentChannelsStore.RecentChannelItem recent : recents) {
            if (recent == null || recent.channelId == null || ranks.containsKey(recent.channelId)) {
                continue;
            }
            ranks.put(recent.channelId, rank++);
        }
        return ranks;
    }

    private List<ChannelItem> buildAllVodLibraryItems(boolean includeAdult) {
        List<ChannelItem> items = new ArrayList<>();
        for (ChannelItem item : allChannels) {
            if (item == null || !item.isVod) {
                continue;
            }
            if (!includeAdult && item.isAdultVod) {
                continue;
            }
            items.add(item);
        }
        sortVodLibraryItems(items);
        return items;
    }

    private List<ChannelItem> buildVodSortedItems(VodSortMode sortMode) {
        List<ChannelItem> items = buildAllVodLibraryItems(false);
        if (sortMode == VodSortMode.YEAR_DESC) {
            items.sort((left, right) -> {
                int yearCompare = Integer.compare(parseVodYear(right), parseVodYear(left));
                if (yearCompare != 0) {
                    return yearCompare;
                }
                return displayName(left).compareToIgnoreCase(displayName(right));
            });
        } else if (sortMode == VodSortMode.DURATION_DESC) {
            items.sort((left, right) -> {
                int durationCompare = Long.compare(right == null ? 0L : right.vodDurationSeconds, left == null ? 0L : left.vodDurationSeconds);
                if (durationCompare != 0) {
                    return durationCompare;
                }
                return displayName(left).compareToIgnoreCase(displayName(right));
            });
        }
        return items;
    }

    private int parseVodYear(ChannelItem item) {
        if (item == null || item.vodYear == null || item.vodYear.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(item.vodYear.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void showVodCategoriesDialog() {
        LinkedHashMap<String, List<ChannelItem>> categories = new LinkedHashMap<>();
        for (ChannelItem item : buildAllVodLibraryItems(false)) {
            String category = item.group == null || item.group.trim().isEmpty() ? getString(R.string.vod_library_uncategorized) : item.group.trim();
            List<ChannelItem> bucket = categories.get(category);
            if (bucket == null) {
                bucket = new ArrayList<>();
                categories.put(category, bucket);
            }
            bucket.add(item);
        }
        if (categories.isEmpty()) {
            showStatus(getString(R.string.vod_library_empty));
            return;
        }
        List<Map.Entry<String, List<ChannelItem>>> entries = new ArrayList<>(categories.entrySet());
        entries.sort((left, right) -> {
            int countCompare = Integer.compare(right.getValue().size(), left.getValue().size());
            if (countCompare != 0) {
                return countCompare;
            }
            return left.getKey().compareToIgnoreCase(right.getKey());
        });
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        for (Map.Entry<String, List<ChannelItem>> entry : entries) {
            List<ChannelItem> categoryItems = new ArrayList<>(entry.getValue());
            sortVodLibraryItems(categoryItems);
            options.add(entry.getKey() + " (" + categoryItems.size() + ")");
            actions.add(() -> showQuickChannelListDialog(entry.getKey(), categoryItems, getString(R.string.vod_library_empty), this::showVodInfoDialog));
        }
        showTvOptionsDialog(R.string.vod_library_categories, null, options, actions);
    }

    private List<ChannelItem> buildVodContinueItems() {
        List<ChannelItem> items = new ArrayList<>();
        Set<String> added = new HashSet<>();
        ChannelItem last = findChannelItemById(lastVodId);
        if (last != null && last.isVod && getVodResumePosition(last.id) > 30_000L && added.add(last.id)) {
            items.add(last);
        }
        for (ChannelItem item : buildRecentVodItems()) {
            if (item != null && getVodResumePosition(item.id) > 30_000L && added.add(item.id)) {
                items.add(item);
            }
        }
        for (ChannelItem item : buildVodProgressItems()) {
            if (item != null && added.add(item.id)) {
                items.add(item);
            }
        }
        sortVodLibraryItems(items);
        return items;
    }

    private List<ChannelItem> buildRecentVodItems() {
        List<ChannelItem> items = new ArrayList<>();
        Set<String> added = new HashSet<>();
        List<RecentChannelsStore.RecentChannelItem> recents = recentChannelsStore == null ? new ArrayList<>() : recentChannelsStore.getItems();
        for (RecentChannelsStore.RecentChannelItem recent : recents) {
            if (recent == null || recent.channelId == null || !added.add(recent.channelId)) {
                continue;
            }
            ChannelItem item = findChannelItemById(recent.channelId);
            if (item != null && item.isVod) {
                items.add(item);
            }
            if (items.size() >= 80) {
                break;
            }
        }
        return items;
    }

    private List<ChannelItem> buildVodItemsByFilter(String vodFilterKey, boolean includeAdult) {
        List<ChannelItem> items = new ArrayList<>();
        for (ChannelItem item : allChannels) {
            if (item == null || !item.isVod) {
                continue;
            }
            if (!includeAdult && item.isAdultVod) {
                continue;
            }
            if (vodFilterKey != null && !vodFilterKey.equals(item.vodFilterKey)) {
                continue;
            }
            items.add(item);
        }
        sortVodLibraryItems(items);
        return items;
    }

    private List<ChannelItem> buildVodProgressItems() {
        List<ChannelItem> items = new ArrayList<>();
        for (Map.Entry<String, Long> entry : vodResumePositions.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 30_000L) {
                continue;
            }
            ChannelItem item = findChannelItemById(entry.getKey());
            if (item != null && item.isVod) {
                items.add(item);
            }
        }
        sortVodLibraryItems(items);
        return items;
    }

    private List<ChannelItem> buildVodNotStartedItems() {
        List<ChannelItem> items = new ArrayList<>();
        for (ChannelItem item : allChannels) {
            if (item == null || !item.isVod || item.isAdultVod || getVodResumePosition(item.id) > 0L) {
                continue;
            }
            items.add(item);
            if (items.size() >= 120) {
                break;
            }
        }
        sortVodLibraryItems(items);
        return items;
    }

    private void sortVodLibraryItems(List<ChannelItem> items) {
        if (items == null) {
            return;
        }
        items.sort((left, right) -> {
            long leftProgress = getVodResumePosition(left == null ? null : left.id);
            long rightProgress = getVodResumePosition(right == null ? null : right.id);
            int progressCompare = Long.compare(rightProgress, leftProgress);
            if (progressCompare != 0) {
                return progressCompare;
            }
            String leftPlatform = left == null || left.platformName == null ? "" : left.platformName;
            String rightPlatform = right == null || right.platformName == null ? "" : right.platformName;
            int platformCompare = leftPlatform.compareToIgnoreCase(rightPlatform);
            if (platformCompare != 0) {
                return platformCompare;
            }
            return displayName(left).compareToIgnoreCase(displayName(right));
        });
    }

    private void showVodProgressManagerDialog() {
        showQuickChannelListDialog(
                getString(R.string.vod_library_manage_progress),
                buildVodProgressItems(),
                getString(R.string.vod_continue_empty),
                this::showVodProgressActionsDialog
        );
    }

    private void showVodProgressActionsDialog(ChannelItem item) {
        if (item == null) {
            return;
        }
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.vod_action_continue));
        actions.add(() -> playChannelItemInternal(item, true, getVodResumePosition(item.id)));
        options.add(getString(R.string.vod_action_start_over));
        actions.add(() -> {
            clearVodResumePosition(item.id);
            playChannelItemInternal(item, true, 0L);
        });
        options.add(getString(R.string.vod_action_clear_progress));
        actions.add(() -> {
            clearVodResumePosition(item.id);
            showStatus(getString(R.string.vod_status_progress_cleared));
        });
        showTvOptionsDialog(R.string.vod_library_manage_progress, displayName(item), options, actions);
    }

    private List<ChannelItem> buildVodSearchResults(String query, boolean includeAdult) {
        List<ChannelItem> results = new ArrayList<>();
        for (ChannelItem item : allChannels) {
            if (item == null || !item.isVod || (!includeAdult && item.isAdultVod)) {
                continue;
            }
            String haystack = displayName(item) + " " + item.group + " " + item.platformName;
            if (!matchesSearch(haystack, query)) {
                continue;
            }
            results.add(item);
            if (results.size() >= 80) {
                break;
            }
        }
        sortVodLibraryItems(results);
        return results;
    }

    private void openLastVod() {
        rememberCurrentVodPosition();
        ChannelItem item = findChannelItemById(lastVodId);
        if (item == null || !item.isVod) {
            showStatus(getString(R.string.vod_continue_empty));
            return;
        }
        showVodInfoDialog(item);
    }

    private void showPersonalListsManagerDialog() {
        if (channelCollectionStore == null) {
            return;
        }
        List<ChannelCollectionStore.ChannelCollection> collections = channelCollectionStore.getCollections();
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_list_panel, null, false);
        TextView panelTitle = dialogView.findViewById(R.id.dialogPanelTitleText);
        TextView panelSubtitle = dialogView.findViewById(R.id.dialogPanelSubtitleText);
        RecyclerView recyclerView = dialogView.findViewById(R.id.dialogRecyclerView);
        if (panelTitle != null) {
            panelTitle.setText(R.string.title_manage_personal_lists);
            panelTitle.setVisibility(View.VISIBLE);
        }
        if (panelSubtitle != null) {
            panelSubtitle.setText(getString(R.string.personal_list_manager_hint, collections.size()));
            panelSubtitle.setVisibility(View.VISIBLE);
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        PersonalListAdapter adapter = new PersonalListAdapter(
                collections,
                this::showPersonalListChannelsPanel,
                this::showPersonalListActionsDialog
        );
        recyclerView.setAdapter(adapter);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.personal_list_create, (d, which) -> showCreatePersonalListDialog())
                .setNegativeButton(R.string.dialog_close, null)
                .create();
        dialog.setOnDismissListener(d -> enableImmersiveMode());
        dialog.show();
        recyclerView.post(() -> {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager != null) {
                View firstItem = layoutManager.findViewByPosition(0);
                if (firstItem != null) {
                    firstItem.requestFocus();
                    return;
                }
            }
            recyclerView.requestFocus();
        });
    }

    private void showPersonalListActionsDialog(ChannelCollectionStore.ChannelCollection collection) {
        if (collection == null) {
            return;
        }
        String[] options = new String[]{
                getString(R.string.personal_list_view_channels),
                getString(R.string.personal_list_open),
                getString(R.string.personal_list_rename),
                getString(R.string.personal_list_delete)
        };
        new AlertDialog.Builder(this)
                .setTitle(collection.label)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showPersonalListChannelsPanel(collection);
                    } else if (which == 1) {
                        applyPersonalListFilter(collection.key);
                    } else if (which == 2) {
                        showRenamePersonalListDialog(collection);
                    } else if (which == 3) {
                        channelCollectionStore.deleteCollection(collection.key);
                        refreshLocalChannelFilters(lastChannelId);
                        showStatus(getString(R.string.status_personal_list_deleted));
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showCreatePersonalListDialog() {
        showPersonalListNameDialog("", value -> {
            ChannelCollectionStore.ChannelCollection created = channelCollectionStore == null ? null : channelCollectionStore.createCollection(value);
            if (created == null) {
                showStatus(getString(R.string.status_personal_list_empty_name));
                return;
            }
            refreshLocalChannelFilters(lastChannelId);
            showStatus(getString(R.string.status_personal_list_created));
            uiHandler.post(() -> showPersonalListChannelsPanel(created));
        });
    }

    private void showRenamePersonalListDialog(ChannelCollectionStore.ChannelCollection collection) {
        if (collection == null) {
            return;
        }
        showPersonalListNameDialog(collection.label, value -> {
            if (channelCollectionStore == null || !channelCollectionStore.renameCollection(collection.key, value)) {
                showStatus(getString(R.string.status_personal_list_empty_name));
                return;
            }
            refreshLocalChannelFilters(lastChannelId);
            showStatus(getString(R.string.status_personal_list_renamed));
        });
    }

    private void showPersonalListNameDialog(String initialValue, PersonalListNameAction action) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(R.string.personal_list_name_hint);
        input.setText(initialValue == null ? "" : initialValue);
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_manage_personal_lists)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String value = input.getText() == null ? "" : input.getText().toString().trim();
                    if (value.isEmpty()) {
                        showStatus(getString(R.string.status_personal_list_empty_name));
                        return;
                    }
                    action.apply(value);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void applyPersonalListFilter(String collectionKey) {
        syncOverlayCoordinator();
        channelOverlayCoordinator.refreshLocalFilters();
        channelOverlayCoordinator.setSearchQuery("");
        channelOverlayCoordinator.setFavoritesOnly(false);
        channelOverlayCoordinator.setSelectedFilterKey("collection:" + collectionKey);
        String currentId = lastChannelId == null ? "" : lastChannelId;
        channelOverlayCoordinator.refreshVisibleChannels(currentId, currentId);
        syncOverlayStateFromCoordinator();
        clearOverlaySearchQuery();
        channelAdapter.notifyDataSetChanged();
        updateFilterText();
        updateOverlaySearchState();
        showOverlay();
    }

    private void showPersonalListChannelsPanel(ChannelCollectionStore.ChannelCollection collection) {
        if (collection == null || channelCollectionStore == null) {
            return;
        }
        ChannelCollectionStore.ChannelCollection currentCollection = channelCollectionStore.getCollection(collection.key);
        if (currentCollection == null) {
            showStatus(getString(R.string.personal_list_channels_empty));
            return;
        }
        List<ChannelItem> items = buildPersonalListChannels(currentCollection);
        if (items.isEmpty()) {
            showStatus(getString(R.string.personal_list_channels_empty));
            showPersonalListActionsDialog(currentCollection);
            return;
        }
        clearQuickSearchOverlay();
        hideOverlay();
        hideRecordingsPanel();
        if (touchControlsBar != null) {
            touchControlsBar.setVisibility(View.GONE);
        }
        if (touchHomeHub != null) {
            touchHomeHub.setVisibility(View.GONE);
        }
        prefetchChannelLogos(items, SEARCH_LOGO_PREFETCH_LIMIT, 42, 42);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_list_panel, null, false);
        TextView panelTitle = dialogView.findViewById(R.id.dialogPanelTitleText);
        TextView panelSubtitle = dialogView.findViewById(R.id.dialogPanelSubtitleText);
        RecyclerView recyclerView = dialogView.findViewById(R.id.dialogRecyclerView);
        if (panelTitle != null) {
            panelTitle.setText(getString(R.string.personal_list_channels_title, currentCollection.label));
            panelTitle.setVisibility(View.VISIBLE);
        }
        if (panelSubtitle != null) {
            panelSubtitle.setText(getString(R.string.personal_list_channels_hint, items.size()));
            panelSubtitle.setVisibility(View.VISIBLE);
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        final AlertDialog[] dialogHolder = new AlertDialog[1];
        SearchChannelAdapter adapter = new SearchChannelAdapter(items, item -> {
            AlertDialog activeDialog = dialogHolder[0];
            if (activeDialog != null && activeDialog.isShowing()) {
                activeDialog.dismiss();
            }
            uiHandler.post(() -> showPersonalListChannelActionsDialog(currentCollection, item));
        });
        recyclerView.setAdapter(adapter);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.personal_list_open, (d, which) -> applyPersonalListFilter(currentCollection.key))
                .setNeutralButton(R.string.personal_list_rename, (d, which) -> showRenamePersonalListDialog(currentCollection))
                .setNegativeButton(R.string.dialog_close, null)
                .create();
        dialogHolder[0] = dialog;
        dialog.setOnDismissListener(d -> enableImmersiveMode());
        dialog.show();
        recyclerView.post(() -> {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager != null) {
                View firstItem = layoutManager.findViewByPosition(0);
                if (firstItem != null) {
                    firstItem.requestFocus();
                    return;
                }
            }
            recyclerView.requestFocus();
        });
    }

    private void showPersonalListChannelActionsDialog(ChannelCollectionStore.ChannelCollection collection, ChannelItem item) {
        if (collection == null || item == null) {
            return;
        }
        String[] options = new String[]{
                getString(R.string.personal_list_channel_action_tune),
                getString(R.string.personal_list_channel_action_remove),
                getString(R.string.personal_list_channel_action_profile)
        };
        new AlertDialog.Builder(this)
                .setTitle(displayName(item))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        tuneQuickAccessChannel(item);
                    } else if (which == 1) {
                        if (channelCollectionStore != null) {
                            channelCollectionStore.setMembership(collection.key, item.id, false);
                        }
                        refreshLocalChannelFilters(item.id);
                        showStatus(getString(R.string.status_personal_list_channel_removed));
                        ChannelCollectionStore.ChannelCollection refreshed = channelCollectionStore == null ? null : channelCollectionStore.getCollection(collection.key);
                        if (refreshed != null && !refreshed.channelIds.isEmpty()) {
                            uiHandler.post(() -> showPersonalListChannelsPanel(refreshed));
                        }
                    } else if (which == 2) {
                        showChannelProfileDialog(item);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private List<ChannelItem> buildPersonalListChannels(ChannelCollectionStore.ChannelCollection collection) {
        List<ChannelItem> items = new ArrayList<>();
        if (collection == null || collection.channelIds.isEmpty()) {
            return items;
        }
        Set<String> added = new HashSet<>();
        for (ChannelItem item : allChannels) {
            if (item != null && item.id != null && collection.channelIds.contains(item.id)) {
                items.add(item);
                added.add(item.id);
            }
        }
        for (String channelId : collection.channelIds) {
            if (channelId == null || added.contains(channelId)) {
                continue;
            }
            ChannelItem item = findChannelItemById(channelId);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private String buildPersonalListPreview(ChannelCollectionStore.ChannelCollection collection) {
        List<ChannelItem> items = buildPersonalListChannels(collection);
        if (items.isEmpty()) {
            return getString(R.string.personal_list_empty_preview);
        }
        List<String> names = new ArrayList<>();
        int max = Math.min(3, items.size());
        for (int i = 0; i < max; i++) {
            names.add(displayName(items.get(i)));
        }
        String joined = joinLabels(names);
        int remaining = items.size() - max;
        if (remaining > 0) {
            return getString(R.string.personal_list_more_preview, joined, remaining);
        }
        return joined;
    }

    private String buildChannelMembershipLabel(ChannelItem channelItem, int maxLabels) {
        if (channelItem == null || channelCollectionStore == null) {
            return "";
        }
        List<String> labels = channelCollectionStore.getMembershipLabels(channelItem.id, maxLabels);
        if (labels.isEmpty()) {
            return "";
        }
        return getString(R.string.channel_list_membership_label, joinLabels(labels));
    }

    private void openCurrentChannelPersonalLists() {
        ChannelItem channelItem = getCurrentPlaybackChannelItem();
        if (channelItem == null) {
            showStatus(getString(R.string.diagnostics_none));
            return;
        }
        showPersonalListsDialog(channelItem);
    }

    private void openCurrentChannelProfile() {
        ChannelItem channelItem = getCurrentPlaybackChannelItem();
        if (channelItem == null) {
            showStatus(getString(R.string.diagnostics_none));
            return;
        }
        showChannelProfileDialog(channelItem);
    }

    private void openCurrentTemporaryPlaybackMode() {
        ChannelItem channelItem = getCurrentPlaybackChannelItem();
        if (channelItem == null) {
            showStatus(getString(R.string.diagnostics_none));
            return;
        }
        showTemporaryPlaybackModeDialog(channelItem);
    }

    private interface PersonalListNameAction {
        void apply(String value);
    }

    private void showChannelSearchDialog() {
        showGlobalSearchDialog();
    }

    private void showGlobalSearchDialog() {
        showGlobalSearchDialog("");
    }

    private void showGlobalSearchDialog(String initialQuery) {
        clearQuickSearchOverlay();
        hideOverlay();
        hideRecordingsPanel();
        closeMultiView();
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_channel_search, null, false);
        EditText input = dialogView.findViewById(R.id.channelSearchInput);
        LinearLayout filterRow = dialogView.findViewById(R.id.globalSearchFilterRow);
        RecyclerView recyclerView = dialogView.findViewById(R.id.channelSearchResults);
        input.setHint(R.string.global_search_hint);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        globalSearchFilter = GLOBAL_SEARCH_FILTER_ALL;

        final AlertDialog[] dialogHolder = new AlertDialog[1];
        GlobalSearchAdapter adapter = new GlobalSearchAdapter(buildGlobalSearchLocalResults("", globalSearchFilter), result -> {
            if (result == null) {
                return;
            }
            if (result.type == GLOBAL_SEARCH_HISTORY) {
                input.setText(result.title);
                input.setSelection(input.getText() == null ? 0 : input.getText().length());
                return;
            }
            String query = input.getText() == null ? "" : input.getText().toString();
            rememberGlobalSearchQuery(query);
            if (dialogHolder[0] != null) {
                dialogHolder[0].dismiss();
            }
            handleGlobalSearchResult(result);
        });
        recyclerView.setAdapter(adapter);
        bindGlobalSearchFilterRow(filterRow, () -> updateGlobalSearchResults(adapter, input.getText() == null ? "" : input.getText().toString()));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_global_search)
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
                updateGlobalSearchResults(adapter, s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        dialog.setOnShowListener(d -> {
            input.requestFocus();
            String initial = initialQuery == null ? "" : initialQuery.trim();
            if (!initial.isEmpty()) {
                input.setText(initial);
                input.setSelection(input.getText() == null ? 0 : input.getText().length());
            }
            updateGlobalSearchResults(adapter, initial);
        });
        dialog.show();
    }

    private void bindGlobalSearchFilterRow(LinearLayout filterRow, Runnable onChanged) {
        if (filterRow == null) {
            return;
        }
        filterRow.removeAllViews();
        int[] filters = new int[]{
                GLOBAL_SEARCH_FILTER_ALL,
                GLOBAL_SEARCH_FILTER_TV,
                GLOBAL_SEARCH_FILTER_VOD,
                GLOBAL_SEARCH_FILTER_FAVORITES,
                GLOBAL_SEARCH_FILTER_EPG,
                GLOBAL_SEARCH_FILTER_RECORDINGS
        };
        for (int filter : filters) {
            if (filter == GLOBAL_SEARCH_FILTER_RECORDINGS && isOfflineRecordingsDisabled()) {
                continue;
            }
            TextView chip = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.rightMargin = dp(8);
            chip.setLayoutParams(params);
            chip.setMinHeight(dp(36));
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            chip.setText(globalSearchFilterLabel(filter));
            chip.setTextSize(13f);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setTextColor(filter == globalSearchFilter ? 0xFFFFFFFF : 0xFFC7D2E2);
            chip.setBackgroundColor(filter == globalSearchFilter ? 0xFF2A7C86 : 0xFF223249);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> {
                globalSearchFilter = filter;
                bindGlobalSearchFilterRow(filterRow, onChanged);
                if (onChanged != null) {
                    onChanged.run();
                }
            });
            filterRow.addView(chip);
        }
    }

    private String globalSearchFilterLabel(int filter) {
        switch (filter) {
            case GLOBAL_SEARCH_FILTER_TV:
                return getString(R.string.global_search_filter_tv);
            case GLOBAL_SEARCH_FILTER_VOD:
                return getString(R.string.global_search_filter_vod);
            case GLOBAL_SEARCH_FILTER_FAVORITES:
                return getString(R.string.global_search_filter_favorites);
            case GLOBAL_SEARCH_FILTER_EPG:
                return getString(R.string.global_search_filter_epg);
            case GLOBAL_SEARCH_FILTER_RECORDINGS:
                return getString(R.string.global_search_filter_recordings);
            default:
                return getString(R.string.global_search_filter_all);
        }
    }

    private void updateGlobalSearchResults(GlobalSearchAdapter adapter, String query) {
        if (adapter == null) {
            return;
        }
        int generation = ++globalSearchGeneration;
        if (pendingGlobalSearchRunnable != null) {
            uiHandler.removeCallbacks(pendingGlobalSearchRunnable);
            pendingGlobalSearchRunnable = null;
        }
        adapter.submitList(buildGlobalSearchLocalResults(query, globalSearchFilter));
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() < 2) {
            return;
        }
        int requestedFilter = globalSearchFilter;
        pendingGlobalSearchRunnable = () -> fetchGlobalSearchRemoteResults(adapter, trimmed, generation, requestedFilter);
        uiHandler.postDelayed(pendingGlobalSearchRunnable, 450L);
    }

    private void fetchGlobalSearchRemoteResults(GlobalSearchAdapter adapter, String query, int generation, int requestedFilter) {
        ioExecutor.execute(() -> {
            List<GlobalSearchResult> remoteResults = new ArrayList<>();
            try {
                if (requestedFilter != GLOBAL_SEARCH_FILTER_ALL && requestedFilter != GLOBAL_SEARCH_FILTER_EPG) {
                    throw new IllegalStateException("EPG search skipped by filter");
                }
                List<EpgSearchResult> epgResults = buildEpgSearchResults(query);
                if (!epgResults.isEmpty()) {
                    appendGroupedGlobalEpgResults(remoteResults, epgResults, 12);
                }
            } catch (IllegalStateException ignored) {
            } catch (Exception e) {
                Log.w(TAG, "global EPG search failed", e);
            }
            try {
                if (!isOfflineRecordingsDisabled() && (requestedFilter == GLOBAL_SEARCH_FILTER_ALL || requestedFilter == GLOBAL_SEARCH_FILTER_RECORDINGS)) {
                    remoteResults.addAll(buildGlobalRecordingResults(query));
                }
            } catch (Exception e) {
                Log.w(TAG, "global recording search failed", e);
            }
            uiHandler.post(() -> {
                if (generation != globalSearchGeneration) {
                    return;
                }
                List<GlobalSearchResult> merged = new ArrayList<>(buildGlobalSearchLocalResults(query, requestedFilter));
                merged.addAll(remoteResults);
                adapter.submitList(merged);
            });
        });
    }

    private void appendGroupedGlobalEpgResults(List<GlobalSearchResult> out, List<EpgSearchResult> epgResults, int limit) {
        if (out == null || epgResults == null || epgResults.isEmpty()) {
            return;
        }
        List<GlobalSearchResult> now = new ArrayList<>();
        List<GlobalSearchResult> today = new ArrayList<>();
        List<GlobalSearchResult> upcoming = new ArrayList<>();
        long nowMs = System.currentTimeMillis();
        java.util.Calendar todayStart = java.util.Calendar.getInstance();
        todayStart.set(java.util.Calendar.HOUR_OF_DAY, 0);
        todayStart.set(java.util.Calendar.MINUTE, 0);
        todayStart.set(java.util.Calendar.SECOND, 0);
        todayStart.set(java.util.Calendar.MILLISECOND, 0);
        long tomorrowStartMs = todayStart.getTimeInMillis() + 24L * 60L * 60L * 1000L;
        int max = Math.min(limit, epgResults.size());
        for (int i = 0; i < max; i++) {
            EpgSearchResult result = epgResults.get(i);
            EpgRepository.EpgProgram program = result == null ? null : result.program;
            ChannelItem channel = result == null ? null : result.channel;
            String title = program == null || program.title == null || program.title.trim().isEmpty()
                    ? getString(R.string.label_program_default)
                    : program.title.trim();
            String meta = (channel == null ? "" : displayName(channel)) + "  ·  " + shortTime(program == null ? "" : program.startTime) + " - " + shortTime(program == null ? "" : program.endTime);
            boolean live = program != null && program.progress >= 0;
            GlobalSearchResult row = new GlobalSearchResult(GLOBAL_SEARCH_EPG, title, meta.trim(), live ? getString(R.string.epg_search_badge_live) : getString(R.string.epg_search_badge_next), null, result, null, "");
            long startMs = program == null ? Long.MAX_VALUE : parseIsoMillis(program.startTime);
            if (live || (startMs <= nowMs && (program == null || parseIsoMillis(program.endTime) >= nowMs))) {
                now.add(row);
            } else if (startMs < tomorrowStartMs) {
                today.add(row);
            } else {
                upcoming.add(row);
            }
        }
        appendGlobalSearchGroup(out, R.string.global_search_section_epg_now, now);
        appendGlobalSearchGroup(out, R.string.global_search_section_epg_today, today);
        appendGlobalSearchGroup(out, R.string.global_search_section_epg_upcoming, upcoming);
    }

    private void appendGlobalSearchGroup(List<GlobalSearchResult> out, int titleResId, List<GlobalSearchResult> rows) {
        if (out == null || rows == null || rows.isEmpty()) {
            return;
        }
        out.add(globalSearchHeader(getString(titleResId)));
        out.addAll(rows);
    }

    private List<GlobalSearchResult> buildGlobalSearchLocalResults(String query, int filter) {
        String trimmed = query == null ? "" : query.trim();
        List<GlobalSearchResult> results = new ArrayList<>();
        if (trimmed.isEmpty()) {
            appendGlobalSuggestions(results, filter);
            return results;
        }
        List<GlobalSearchResult> channels = new ArrayList<>();
        List<GlobalSearchResult> vod = new ArrayList<>();
        List<ChannelItem> channelMatches = new ArrayList<>();
        List<ChannelItem> vodMatches = new ArrayList<>();
        for (ChannelItem item : allChannels) {
            if (item == null) {
                continue;
            }
            if (!globalSearchIncludesItem(filter, item)) {
                continue;
            }
            String haystack = displayName(item) + " " + item.group + " " + item.platformName + " " + joinLabels(item.customGroups);
            if (!matchesSearch(haystack, trimmed)) {
                continue;
            }
            if (item.isVod) {
                vodMatches.add(item);
            } else {
                channelMatches.add(item);
            }
        }
        sortSearchChannelMatches(channelMatches, trimmed);
        sortSearchChannelMatches(vodMatches, trimmed);
        for (int i = 0; i < Math.min(12, channelMatches.size()); i++) {
            ChannelItem item = channelMatches.get(i);
            channels.add(new GlobalSearchResult(GLOBAL_SEARCH_CHANNEL, displayName(item), buildGlobalChannelMeta(item), getString(R.string.channel_badge_live), item, null, null, ""));
        }
        for (int i = 0; i < Math.min(12, vodMatches.size()); i++) {
            ChannelItem item = vodMatches.get(i);
            vod.add(new GlobalSearchResult(GLOBAL_SEARCH_VOD, displayName(item), buildGlobalVodMeta(item), item.isAdultVod ? getString(R.string.channel_badge_vod_adult) : getString(R.string.channel_badge_vod), item, null, null, ""));
        }
        if (!channels.isEmpty()) {
            results.add(globalSearchHeader(getString(R.string.global_search_section_channels)));
            results.addAll(channels);
        }
        if (!vod.isEmpty()) {
            results.add(globalSearchHeader(getString(R.string.global_search_section_vod)));
            results.addAll(vod);
        }
        if (results.isEmpty()) {
            results.add(globalSearchHeader(getString(R.string.global_search_no_local_results)));
        }
        return results;
    }

    private boolean globalSearchIncludesItem(int filter, ChannelItem item) {
        if (item == null) {
            return false;
        }
        switch (filter) {
            case GLOBAL_SEARCH_FILTER_TV:
                return !item.isVod;
            case GLOBAL_SEARCH_FILTER_VOD:
                return item.isVod;
            case GLOBAL_SEARCH_FILTER_FAVORITES:
                return !item.isVod && favoriteChannelIds.contains(item.id);
            case GLOBAL_SEARCH_FILTER_EPG:
            case GLOBAL_SEARCH_FILTER_RECORDINGS:
                return false;
            default:
                return true;
        }
    }

    private void sortSearchChannelMatches(List<ChannelItem> items, String query) {
        if (items == null) {
            return;
        }
        items.sort((left, right) -> Integer.compare(searchScore(right, query), searchScore(left, query)));
    }

    private int searchScore(ChannelItem item, String query) {
        if (item == null) {
            return 0;
        }
        String normalizedQuery = safeSearchText(query);
        String name = safeSearchText(displayName(item));
        String group = safeSearchText(item.group);
        String platform = safeSearchText(item.platformName);
        int score = 0;
        if (name.equals(normalizedQuery)) {
            score += 120;
        }
        if (name.startsWith(normalizedQuery)) {
            score += 70;
        }
        if (name.contains(normalizedQuery)) {
            score += 40;
        }
        if (platform.contains(normalizedQuery)) {
            score += 18;
        }
        if (group.contains(normalizedQuery)) {
            score += 12;
        }
        if (favoriteChannelIds.contains(item.id)) {
            score += 8;
        }
        if (item.id != null && item.id.equals(lastChannelId)) {
            score += 6;
        }
        return score;
    }

    private String buildGlobalChannelMeta(ChannelItem item) {
        if (item == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (item.platformName != null && !item.platformName.trim().isEmpty()) {
            parts.add(item.platformName.trim());
        }
        if (item.group != null && !item.group.trim().isEmpty()) {
            parts.add(item.group.trim());
        }
        String membership = buildChannelMembershipLabel(item, 2);
        if (!membership.isEmpty()) {
            parts.add(membership);
        }
        if (favoriteChannelIds.contains(item.id)) {
            parts.add(getString(R.string.global_search_meta_favorite));
        }
        if (item.nowProgram != null && !item.nowProgram.trim().isEmpty()) {
            parts.add(item.nowProgram.trim());
        }
        return joinLabels(parts);
    }

    private String buildGlobalVodMeta(ChannelItem item) {
        if (item == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        parts.add(buildVodRowMeta(item));
        String membership = buildChannelMembershipLabel(item, 2);
        if (!membership.isEmpty()) {
            parts.add(membership);
        }
        long resumeMs = getVodResumePosition(item.id);
        if (resumeMs > 30_000L) {
            parts.add(getString(R.string.global_search_meta_progress, formatDurationShort(resumeMs)));
        }
        return joinLabels(parts);
    }

    private void appendGlobalSuggestions(List<GlobalSearchResult> results, int filter) {
        if (!globalSearchRecents.isEmpty() && filter == GLOBAL_SEARCH_FILTER_ALL) {
            results.add(globalSearchHeader(getString(R.string.global_search_section_recent_searches)));
            int maxSearches = Math.min(5, globalSearchRecents.size());
            for (int i = 0; i < maxSearches; i++) {
                String value = globalSearchRecents.get(i);
                results.add(new GlobalSearchResult(GLOBAL_SEARCH_HISTORY, value, getString(R.string.global_search_history_hint), getString(R.string.global_search_badge_history), null, null, null, ""));
            }
        }
        ChannelItem current = getCurrentPlaybackChannelItem();
        boolean suggestionHeaderAdded = false;
        if (current != null && globalSearchIncludesItem(filter, current)) {
            results.add(globalSearchHeader(getString(R.string.global_search_section_suggestions)));
            suggestionHeaderAdded = true;
            results.add(new GlobalSearchResult(current.isVod ? GLOBAL_SEARCH_VOD : GLOBAL_SEARCH_CHANNEL, displayName(current), current.isVod ? buildVodRowMeta(current) : getString(R.string.quick_hub_continue), current.isVod ? getString(R.string.channel_badge_vod) : getString(R.string.channel_badge_live), current, null, null, ""));
        }
        ChannelItem lastVod = findChannelItemById(lastVodId);
        if (lastVod != null && lastVod.isVod && globalSearchIncludesItem(filter, lastVod) && (current == null || !lastVod.id.equals(current.id))) {
            if (!suggestionHeaderAdded) {
                results.add(globalSearchHeader(getString(R.string.global_search_section_suggestions)));
                suggestionHeaderAdded = true;
            }
            results.add(new GlobalSearchResult(GLOBAL_SEARCH_VOD, displayName(lastVod), getString(R.string.quick_hub_continue_vod, buildVodInfoMeta(lastVod)), getString(R.string.channel_badge_vod), lastVod, null, null, ""));
        }
        List<ChannelItem> recentChannels = buildRecentQuickChannels();
        if (!recentChannels.isEmpty() && (filter == GLOBAL_SEARCH_FILTER_ALL || filter == GLOBAL_SEARCH_FILTER_TV || filter == GLOBAL_SEARCH_FILTER_FAVORITES)) {
            results.add(globalSearchHeader(getString(R.string.global_search_section_recent_channels)));
            int max = Math.min(6, recentChannels.size());
            for (int i = 0; i < max; i++) {
                ChannelItem item = recentChannels.get(i);
                if (item == null) {
                    continue;
                }
                if (!globalSearchIncludesItem(filter, item)) {
                    continue;
                }
                results.add(new GlobalSearchResult(item.isVod ? GLOBAL_SEARCH_VOD : GLOBAL_SEARCH_CHANNEL, displayName(item), item.isVod ? buildVodRowMeta(item) : item.group, item.isVod ? getString(R.string.channel_badge_vod) : getString(R.string.channel_badge_live), item, null, null, ""));
            }
        }
        if (results.isEmpty()) {
            results.add(globalSearchHeader(getString(R.string.global_search_empty_hint)));
        }
    }

    private List<GlobalSearchResult> buildGlobalRecordingResults(String query) throws Exception {
        if (isOfflineRecordingsDisabled()) {
            return new ArrayList<>();
        }
        List<GlobalSearchResult> results = new ArrayList<>();
        List<GlobalSearchResult> matches = new ArrayList<>();
        RecordingsRepository.RecordingsResult completed = recordingsRepository.fetchCompletedRecordings();
        appendGlobalRecordingMatches(matches, completed, query, 8);
        RecordingsRepository.RecordingsResult scheduled = recordingsRepository.fetchScheduledRecordings();
        appendGlobalRecordingMatches(matches, scheduled, query, 8);
        if (!matches.isEmpty()) {
            results.add(globalSearchHeader(getString(R.string.global_search_section_recordings)));
            results.addAll(matches);
        }
        return results;
    }

    private void appendGlobalRecordingMatches(List<GlobalSearchResult> out, RecordingsRepository.RecordingsResult result, String query, int limit) {
        if (out == null || result == null || result.items == null || out.size() >= limit) {
            return;
        }
        for (RecordingsRepository.RecordingItem item : result.items) {
            if (item == null) {
                continue;
            }
            String haystack = buildRecordingTitle(item) + " " + item.channelName + " " + item.name + " " + item.path + " " + item.status;
            if (!matchesSearch(haystack, query)) {
                continue;
            }
            out.add(new GlobalSearchResult(GLOBAL_SEARCH_RECORDING, buildRecordingTitle(item), buildRecordingMeta(item), item.playable ? getString(R.string.recording_status_completed_short) : buildRecordingStatusLabel(item), null, null, item, result.basePath));
            if (out.size() >= limit) {
                return;
            }
        }
    }

    private GlobalSearchResult globalSearchHeader(String title) {
        return new GlobalSearchResult(GLOBAL_SEARCH_HEADER, title, "", "", null, null, null, "");
    }

    private void handleGlobalSearchResult(GlobalSearchResult result) {
        if (result == null) {
            return;
        }
        if ((result.type == GLOBAL_SEARCH_CHANNEL || result.type == GLOBAL_SEARCH_VOD) && result.channel != null) {
            if (result.channel.isVod) {
                showVodInfoDialog(result.channel);
            } else {
                tuneChannelById(result.channel.id);
            }
            return;
        }
        if (result.type == GLOBAL_SEARCH_EPG && result.epgResult != null) {
            channelActionsCoordinator.showProgramActionMenu(result.epgResult.channel, result.epgResult.program);
            return;
        }
        if (result.type == GLOBAL_SEARCH_RECORDING && result.recording != null) {
            if (result.recording.playable) {
                playRecording(result.recording, result.recordingBasePath);
            } else {
                loadRecordingsPanel(true, result.recording.id);
            }
        }
    }

    private void showGlobalSearchActions(GlobalSearchResult result) {
        if (result == null || result.type == GLOBAL_SEARCH_HEADER) {
            return;
        }
        if (result.type == GLOBAL_SEARCH_HISTORY) {
            showGlobalSearchHistoryActions(result.title);
            return;
        }
        if (result.channel != null) {
            if (result.channel.isVod) {
                showVodActionsDialog(result.channel);
            } else {
                showGlobalChannelActions(result.channel);
            }
            return;
        }
        if (result.epgResult != null) {
            channelActionsCoordinator.showProgramActionMenu(result.epgResult.channel, result.epgResult.program);
            return;
        }
        if (result.recording != null) {
            if (result.recording.playable) {
                playRecording(result.recording, result.recordingBasePath);
            } else {
                loadRecordingsPanel(true, result.recording.id);
            }
        }
    }

    private void showGlobalSearchHistoryActions(String query) {
        String value = query == null ? "" : query.trim();
        if (value.isEmpty()) {
            return;
        }
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.global_search_history_search_again));
        actions.add(() -> showGlobalSearchDialogWithQuery(value));
        options.add(getString(R.string.global_search_history_delete_one));
        actions.add(() -> removeGlobalSearchRecent(value));
        showTvOptionsDialog(R.string.global_search_section_recent_searches, value, options, actions);
    }

    private void showGlobalSearchDialogWithQuery(String query) {
        showGlobalSearchDialog(query);
    }

    private void showGlobalChannelActions(ChannelItem channel) {
        if (channel == null) {
            return;
        }
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.menu_tune_channel));
        actions.add(() -> tuneChannelById(channel.id));
        options.add(getString(favoriteChannelIds.contains(channel.id) ? R.string.menu_remove_favorite : R.string.menu_add_favorite));
        actions.add(() -> toggleFavoriteChannel(channel));
        options.add(getString(R.string.menu_personal_lists));
        actions.add(() -> showPersonalListsDialog(channel));
        options.add(getString(R.string.menu_channel_profile));
        actions.add(() -> showChannelProfileDialog(channel));
        options.add(getString(R.string.menu_mini_guide));
        actions.add(() -> openMiniGuideForChannel(channel));
        options.add(getString(R.string.diagnostics_action_temporary_mode));
        actions.add(() -> showTemporaryPlaybackModeDialog(channel));
        options.add(getString(R.string.tools_menu_playback_diagnostics));
        actions.add(() -> {
            currentIndex = findChannelIndexById(channel.id);
            lastChannelId = channel.id;
            showPlaybackDiagnosticsDialog();
        });
        showTvOptionsDialog(R.string.title_global_search, displayName(channel), options, actions);
    }

    private void filterSearchResults(SearchChannelAdapter adapter, String query) {
        if (adapter == null) {
            return;
        }
        adapter.submitList(searchChannels(query, 25));
    }

    private void showEpgSearchDialog() {
        clearQuickSearchOverlay();
        hideOverlay();
        hideRecordingsPanel();
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(R.string.epg_search_hint);
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_epg_search)
                .setMessage(R.string.epg_search_scanned_hint)
                .setView(input)
                .setPositiveButton(R.string.search_channel_dialog_action, (dialog, which) -> {
                    String query = input.getText() == null ? "" : input.getText().toString();
                    searchEpgPrograms(query);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void searchEpgPrograms(String query) {
        String trimmedQuery = query == null ? "" : query.trim();
        if (trimmedQuery.length() < 2) {
            showStatus(getString(R.string.epg_search_empty));
            return;
        }
        showStatus(getString(R.string.status_searching_epg));
        ioExecutor.execute(() -> {
            try {
                List<EpgSearchResult> results = buildEpgSearchResults(trimmedQuery);
                uiHandler.post(() -> showEpgSearchResultsDialog(trimmedQuery, results));
            } catch (Exception e) {
                Log.w(TAG, "EPG search failed", e);
                uiHandler.post(() -> showStatus(getString(R.string.status_failed_load_guide)));
            }
        });
    }

    private List<EpgSearchResult> buildEpgSearchResults(String query) throws Exception {
        List<EpgSearchResult> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<ChannelItem> searchScope = new ArrayList<>();
        for (ChannelItem item : channels) {
            if (item != null && !item.isVod && item.id != null && !item.id.trim().isEmpty()) {
                searchScope.add(item);
            }
        }
        if (searchScope.isEmpty()) {
            for (ChannelItem item : allChannels) {
                if (item != null && !item.isVod && item.id != null && !item.id.trim().isEmpty()) {
                    searchScope.add(item);
                }
            }
        }
        int maxChannels = Math.min(80, searchScope.size());
        for (int i = 0; i < maxChannels; i++) {
            ChannelItem channel = searchScope.get(i);
            List<EpgRepository.EpgProgram> programs = epgRepository.fetchChannelPrograms(channel, 18);
            for (EpgRepository.EpgProgram program : programs) {
                if (program == null) {
                    continue;
                }
                String haystack = program.title + " " + program.description + " " + displayName(channel);
                if (!matchesSearch(haystack, query)) {
                    continue;
                }
                String key = channel.id + "|" + String.valueOf(program.title).trim() + "|" + String.valueOf(program.startTime).trim();
                if (seen.add(key)) {
                    results.add(new EpgSearchResult(channel, program));
                }
                if (results.size() >= 60) {
                    return sortEpgSearchResults(results);
                }
            }
        }
        return sortEpgSearchResults(results);
    }

    private List<EpgSearchResult> sortEpgSearchResults(List<EpgSearchResult> results) {
        results.sort((left, right) -> Long.compare(
                left == null || left.program == null ? Long.MAX_VALUE : parseIsoMillis(left.program.startTime),
                right == null || right.program == null ? Long.MAX_VALUE : parseIsoMillis(right.program.startTime)
        ));
        return results;
    }

    private void showEpgSearchResultsDialog(String query, List<EpgSearchResult> results) {
        if (results == null || results.isEmpty()) {
            showStatus(getString(R.string.epg_search_empty));
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_list_panel, null, false);
        TextView panelTitle = dialogView.findViewById(R.id.dialogPanelTitleText);
        TextView panelSubtitle = dialogView.findViewById(R.id.dialogPanelSubtitleText);
        RecyclerView recyclerView = dialogView.findViewById(R.id.dialogRecyclerView);
        if (panelTitle != null) {
            panelTitle.setText(getString(R.string.epg_search_results_title, query));
            panelTitle.setVisibility(View.VISIBLE);
        }
        if (panelSubtitle != null) {
            panelSubtitle.setText(getString(R.string.epg_search_results_hint, results.size()));
            panelSubtitle.setVisibility(View.VISIBLE);
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        final AlertDialog[] dialogHolder = new AlertDialog[1];
        EpgSearchResultAdapter adapter = new EpgSearchResultAdapter(results, result -> {
            AlertDialog activeDialog = dialogHolder[0];
            if (activeDialog != null && activeDialog.isShowing()) {
                activeDialog.dismiss();
            }
            if (result != null) {
                uiHandler.post(() -> channelActionsCoordinator.showProgramActionMenu(result.channel, result.program));
            }
        });
        recyclerView.setAdapter(adapter);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_close, null)
                .create();
        dialogHolder[0] = dialog;
        dialog.setOnDismissListener(d -> enableImmersiveMode());
        dialog.show();
        recyclerView.post(() -> {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager != null) {
                View firstItem = layoutManager.findViewByPosition(0);
                if (firstItem != null) {
                    firstItem.requestFocus();
                    return;
                }
            }
            recyclerView.requestFocus();
        });
    }

    private void showMiniGuideDialog(ChannelItem channel, List<EpgRepository.EpgProgram> items) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_list_panel, null, false);
        TextView panelTitle = dialogView.findViewById(R.id.dialogPanelTitleText);
        TextView panelSubtitle = dialogView.findViewById(R.id.dialogPanelSubtitleText);
        RecyclerView recyclerView = dialogView.findViewById(R.id.dialogRecyclerView);
        if (panelTitle != null) {
            panelTitle.setText(getString(R.string.title_guide, displayName(channel)));
            panelTitle.setVisibility(View.VISIBLE);
        }
        if (panelSubtitle != null) {
            panelSubtitle.setText(getString(R.string.mini_guide_hint, items == null ? 0 : items.size()));
            panelSubtitle.setVisibility(View.VISIBLE);
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new GuideProgramAdapter(channel, items));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.quick_hub_timeline, (d, which) -> openTimelineGuideForChannel(channel))
                .setNeutralButton(R.string.quick_hub_epg_search, (d, which) -> showEpgSearchDialog())
                .setNegativeButton(R.string.dialog_close, null)
                .create();
        dialog.setOnDismissListener(d -> enableImmersiveMode());
        dialog.show();
    }

    private void showTimelineGuideDialog(List<TimelineChannelPrograms> rows, long windowStartMs, String anchorChannelId, List<RecordingsRepository.RecordingItem> scheduledItems) {
        activeTimelineRows = new ArrayList<>(rows);
        activeTimelineScheduledItems = scheduledItems == null ? new ArrayList<>() : new ArrayList<>(scheduledItems);
        activeProgramScheduledItems = new ArrayList<>(activeTimelineScheduledItems);
        activeTimelineWindowStartMs = windowStartMs;
        activeTimelineAnchorChannelId = anchorChannelId;
        lastTimelineWindowStartMs = windowStartMs;
        lastTimelineAnchorChannelId = anchorChannelId;
        if (touchControlsBar != null) {
            touchControlsBar.setVisibility(View.GONE);
        }
        if (touchHomeHub != null) {
            touchHomeHub.setVisibility(View.GONE);
        }
        if (timeshiftBarContainer != null) {
            timeshiftBarContainer.setVisibility(View.GONE);
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_timeline_guide, null, false);
        android.widget.ScrollView timelineVerticalScroll = dialogView.findViewById(R.id.timelineVerticalScroll);
        TextView timelineNowButton = dialogView.findViewById(R.id.timelineNowButton);
        TextView timelinePrevButton = dialogView.findViewById(R.id.timelinePrevButton);
        TextView timelineChannelNextButton = dialogView.findViewById(R.id.timelineChannelNextButton);
        TextView timelineNextButton = dialogView.findViewById(R.id.timelineNextButton);
        TextView timelineCloseButton = dialogView.findViewById(R.id.timelineCloseButton);
        TextView windowText = dialogView.findViewById(R.id.timelineWindowText);
        final List<TextView> timelineHeaderButtons = java.util.Arrays.asList(timelineNowButton, timelinePrevButton, timelineChannelNextButton, timelineNextButton, timelineCloseButton);
        LinearLayout headerRow = dialogView.findViewById(R.id.timelineHeaderRow);
        LinearLayout rowsContainer = dialogView.findViewById(R.id.timelineRowsContainer);
        ImageView timelineProgramPosterImage = dialogView.findViewById(R.id.timelineProgramPosterImage);
        TextView timelineProgramTitleText = dialogView.findViewById(R.id.timelineProgramTitleText);
        TextView timelineProgramMetaText = dialogView.findViewById(R.id.timelineProgramMetaText);
        TextView timelineProgramDescText = dialogView.findViewById(R.id.timelineProgramDescText);
        final View[] initialFocus = new View[1];
        final View[] anchorLiveFocus = new View[1];
        final View[] anchorRememberedFocus = new View[1];
        final View[] anchorFirstFocus = new View[1];
        final View[] anchorEmptyFocus = new View[1];
        final View[] anyFirstFocus = new View[1];
        final View[] anyEmptyFocus = new View[1];
        final int[] anchorRememberedDelta = new int[]{Integer.MAX_VALUE};
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
        final java.util.function.Consumer<TextView> styleTimelineHeaderButton = button -> {
            if (button == null) {
                return;
            }
            button.setFocusable(true);
            button.setFocusableInTouchMode(true);
            button.setClickable(true);
            button.setOnFocusChangeListener((v, hasFocus) -> {
                v.setScaleX(hasFocus ? 1.08f : 1f);
                v.setScaleY(hasFocus ? 1.08f : 1f);
                v.setAlpha(hasFocus ? 1f : 0.9f);
                v.setBackgroundColor(hasFocus ? 0xFF2F89C5 : 0x264F86A8);
            });
        };
        for (TextView headerButton : timelineHeaderButtons) {
            styleTimelineHeaderButton.accept(headerButton);
        }

        long windowEndMs = windowStartMs + TIMELINE_WINDOW_MS;
        final long nowMs = System.currentTimeMillis();
        final boolean rememberFocusedCenter = lastTimelineFocusedCenterMinute >= 0 && lastTimelineWindowStartMs == windowStartMs;
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
            bindChannelLogo(channelLogo, row.channel.logoUrl, row.channel.name, 26, 26);
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
                String titleText = program.title == null || program.title.trim().isEmpty() ? getString(R.string.label_program_default) : program.title;
                boolean scheduled = isProgramScheduled(row.channel, program, scheduledItems);
                if (scheduled) {
                    titleText = getString(R.string.timeline_program_scheduled_prefix) + " " + titleText;
                }
                block.setText(titleText + "\n" + shortTime(program.startTime) + " - " + shortTime(program.endTime));
                block.setTextColor(0xFFFFFFFF);
                block.setTextSize(11f);
                block.setMaxLines(3);
                boolean live = program.progress >= 0;
                applyTimelineBlockState(block, live, scheduled, false);
                block.setOnFocusChangeListener((v, hasFocus) -> {
                    applyTimelineBlockState(block, live, scheduled, hasFocus);
                    if (hasFocus) {
                        activeTimelineAnchorChannelId = row.channel == null ? activeTimelineAnchorChannelId : row.channel.id;
                        activeTimelineWindowStartMs = windowStartMs;
                        activeTimelineFocusedCenterMinute = centerMinute;
                        lastTimelineFocusedCenterMinute = centerMinute;
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
                            if (scheduled) {
                                timelineMeta = timelineMeta + "  ·  " + getString(R.string.timeline_program_scheduled_short);
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
                        if (rowIndex == 0 && timelineNowButton != null) {
                            timelineNowButton.requestFocus();
                            return true;
                        }
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
                boolean anchorMatch = anchorChannelId != null && anchorChannelId.equals(row.channel.id);
                if (anchorMatch && anchorFirstFocus[0] == null) {
                    anchorFirstFocus[0] = block;
                }
                if (anyFirstFocus[0] == null) {
                    anyFirstFocus[0] = block;
                }
                if (anchorMatch && nowMs >= startMs && nowMs < endMs) {
                    anchorLiveFocus[0] = block;
                }
                if (anchorMatch && rememberFocusedCenter) {
                    int delta = Math.abs(centerMinute - lastTimelineFocusedCenterMinute);
                    if (delta < anchorRememberedDelta[0]) {
                        anchorRememberedDelta[0] = delta;
                        anchorRememberedFocus[0] = block;
                    }
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
                empty.setFocusable(true);
                empty.setFocusableInTouchMode(true);
                empty.setOnFocusChangeListener((v, hasFocus) -> {
                    v.setAlpha(hasFocus ? 1f : 0.82f);
                    v.setBackgroundColor(hasFocus ? 0xFF2A3950 : 0xFF1E2630);
                    if (hasFocus) {
                        activeTimelineAnchorChannelId = row.channel == null ? activeTimelineAnchorChannelId : row.channel.id;
                        activeTimelineWindowStartMs = windowStartMs;
                        activeTimelineFocusedCenterMinute = -1;
                        lastTimelineFocusedCenterMinute = -1;
                        if (timelineProgramPosterImage != null) {
                            timelineProgramPosterImage.setVisibility(View.GONE);
                            Glide.with(this).clear(timelineProgramPosterImage);
                        }
                        if (timelineProgramTitleText != null) {
                            timelineProgramTitleText.setText(row.channel == null ? getString(R.string.timeline_no_epg) : row.channel.name);
                        }
                        if (timelineProgramMetaText != null) {
                            timelineProgramMetaText.setText(getString(R.string.timeline_no_epg));
                        }
                        if (timelineProgramDescText != null) {
                            timelineProgramDescText.setText(getString(R.string.timeline_program_desc_empty));
                        }
                    }
                });
                empty.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) {
                        return false;
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if (rowIndex == 0 && timelineNowButton != null) {
                            timelineNowButton.requestFocus();
                            return true;
                        }
                        return moveTimelineFocus(timelineVerticalScroll, focusRows, focusCenters, rowIndex, -1, 0);
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        return moveTimelineFocus(timelineVerticalScroll, focusRows, focusCenters, rowIndex, 1, 0);
                    }
                    return false;
                });
                rowFocusables.add(empty);
                boolean anchorMatch = anchorChannelId != null && row.channel != null && anchorChannelId.equals(row.channel.id);
                if (anchorMatch && anchorEmptyFocus[0] == null) {
                    anchorEmptyFocus[0] = empty;
                }
                if (anyEmptyFocus[0] == null) {
                    anyEmptyFocus[0] = empty;
                }
                strip.addView(empty);
            }

            focusRows.add(rowFocusables);
            rowLayout.addView(strip);
            rowsContainer.addView(rowLayout);
        }

        if (anchorLiveFocus[0] != null) {
            initialFocus[0] = anchorLiveFocus[0];
        } else if (anchorRememberedFocus[0] != null) {
            initialFocus[0] = anchorRememberedFocus[0];
        } else if (anchorFirstFocus[0] != null) {
            initialFocus[0] = anchorFirstFocus[0];
        } else if (anchorEmptyFocus[0] != null) {
            initialFocus[0] = anchorEmptyFocus[0];
        } else if (anyFirstFocus[0] != null) {
            initialFocus[0] = anyFirstFocus[0];
        } else {
            initialFocus[0] = anyEmptyFocus[0];
        }

        clearTimelineProgramDetail.run();

        android.app.Dialog timelineDialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        activeTimelineDialog = timelineDialog;
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
        timelineDialog.setOnDismissListener(d -> {
            if (!refreshingTimelineDialog) {
                lastTimelineAnchorChannelId = activeTimelineAnchorChannelId;
                lastTimelineWindowStartMs = activeTimelineWindowStartMs;
                lastTimelineFocusedCenterMinute = activeTimelineFocusedCenterMinute;
                activeTimelineDialog = null;
                activeTimelineRows = new ArrayList<>();
                activeTimelineScheduledItems = new ArrayList<>();
                activeProgramScheduledItems = new ArrayList<>();
                activeTimelineAnchorChannelId = null;
                activeTimelineWindowStartMs = 0L;
                activeTimelineFocusedCenterMinute = -1;
            }
            enableImmersiveMode();
        });
        timelineNowButton.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && initialFocus[0] != null) {
                initialFocus[0].requestFocus();
                return true;
            }
            return false;
        });
        timelinePrevButton.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && initialFocus[0] != null) {
                initialFocus[0].requestFocus();
                return true;
            }
            return false;
        });
        timelineChannelNextButton.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && initialFocus[0] != null) {
                initialFocus[0].requestFocus();
                return true;
            }
            return false;
        });
        timelineNextButton.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && initialFocus[0] != null) {
                initialFocus[0].requestFocus();
                return true;
            }
            return false;
        });
        timelineCloseButton.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && initialFocus[0] != null) {
                initialFocus[0].requestFocus();
                return true;
            }
            return false;
        });
        timelineNowButton.setOnClickListener(v -> {
            timelineDialog.dismiss();
            openTimelineGuideNow();
        });
        timelinePrevButton.setOnClickListener(v -> {
            timelineDialog.dismiss();
            int anchorIndex = selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size() ? selectedOverlayIndex : Math.max(0, currentIndex);
            openTimelineGuide(anchorIndex, Math.max(0L, windowStartMs - TIMELINE_SHIFT_MS));
        });
        timelineChannelNextButton.setOnClickListener(v -> openTimelineGuideNextForAnchor());
        timelineNextButton.setOnClickListener(v -> {
            timelineDialog.dismiss();
            int anchorIndex = selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size() ? selectedOverlayIndex : Math.max(0, currentIndex);
            openTimelineGuide(anchorIndex, windowStartMs + TIMELINE_SHIFT_MS);
        });
        timelineCloseButton.setOnClickListener(v -> timelineDialog.dismiss());
        timelineDialog.show();
    }

    private boolean moveVisualEpgFocus(List<List<View>> focusRows, int fromRowIndex, int direction, int preferredCenterMinute) {
        int rowIndex = fromRowIndex + direction;
        while (rowIndex >= 0 && rowIndex < focusRows.size()) {
            List<View> targetRow = focusRows.get(rowIndex);
            if (targetRow != null && !targetRow.isEmpty()) {
                View best = null;
                int bestDistance = Integer.MAX_VALUE;
                for (View candidate : targetRow) {
                    if (candidate == null) continue;
                    Object tag = candidate.getTag();
                    int center = preferredCenterMinute;
                    if (tag instanceof Integer) {
                        center = (Integer) tag;
                    }
                    int distance = Math.abs(center - preferredCenterMinute);
                    if (best == null || distance < bestDistance) {
                        best = candidate;
                        bestDistance = distance;
                    }
                }
                return best != null && best.requestFocus();
            }
            rowIndex += direction;
        }
        return false;
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

    private void scrollVisualEpgSectionIntoPlace(android.widget.ScrollView verticalScroll, View target) {
        if (verticalScroll == null || target == null) {
            return;
        }
        Rect rect = new Rect();
        target.getDrawingRect(rect);
        verticalScroll.offsetDescendantRectToMyCoords(target, rect);
        int desiredTop = Math.max(0, rect.top - dp(4));
        verticalScroll.scrollTo(0, desiredTop);
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
        showRecentChannelsQuickDialog();
    }

    private void showRecentChannelsQuickDialog() {
        showQuickChannelListDialog(
                getString(R.string.title_recent_channels),
                buildRecentQuickChannels(),
                getString(R.string.overlay_recent_channels_empty)
        );
    }

    private void showFavoriteChannelsQuickDialog() {
        List<ChannelItem> favorites = buildFavoriteQuickChannels();
        if (favorites.isEmpty()) {
            showStatus(getString(R.string.status_no_favorites_saved));
            return;
        }
        showQuickChannelListDialog(
                getString(R.string.title_favorite_channels),
                favorites,
                getString(R.string.status_no_favorites_saved)
        );
    }

    private interface QuickChannelSelectionAction {
        void onChannelChosen(ChannelItem item);
    }

    private void showQuickChannelListDialog(String title, List<ChannelItem> items, String emptyMessage) {
        showQuickChannelListDialog(title, items, emptyMessage, this::tuneQuickAccessChannel);
    }

    private void showQuickChannelListDialog(String title, List<ChannelItem> items, String emptyMessage, QuickChannelSelectionAction action) {
        if (items == null || items.isEmpty()) {
            showStatus(emptyMessage == null || emptyMessage.trim().isEmpty()
                    ? getString(R.string.overlay_no_results)
                    : emptyMessage);
            return;
        }
        clearQuickSearchOverlay();
        hideOverlay();
        hideRecordingsPanel();
        if (touchControlsBar != null) {
            touchControlsBar.setVisibility(View.GONE);
        }
        if (touchHomeHub != null) {
            touchHomeHub.setVisibility(View.GONE);
        }
        if (timeshiftBarContainer != null) {
            timeshiftBarContainer.setVisibility(View.GONE);
        }
        prefetchChannelLogos(items, SEARCH_LOGO_PREFETCH_LIMIT, 42, 42);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_list_panel, null, false);
        TextView panelTitle = dialogView.findViewById(R.id.dialogPanelTitleText);
        TextView panelSubtitle = dialogView.findViewById(R.id.dialogPanelSubtitleText);
        RecyclerView recyclerView = dialogView.findViewById(R.id.dialogRecyclerView);
        if (panelTitle != null) {
            panelTitle.setText(title);
            panelTitle.setVisibility(View.VISIBLE);
        }
        if (panelSubtitle != null) {
            panelSubtitle.setText(getString(R.string.quick_channel_count, items.size()) + "  ·  " + getString(R.string.quick_channel_hint));
            panelSubtitle.setVisibility(View.VISIBLE);
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        final AlertDialog[] dialogHolder = new AlertDialog[1];
        SearchChannelAdapter adapter = new SearchChannelAdapter(items, item -> {
            AlertDialog activeDialog = dialogHolder[0];
            if (activeDialog != null && activeDialog.isShowing()) {
                activeDialog.dismiss();
            }
            if (action != null) {
                uiHandler.post(() -> action.onChannelChosen(item));
            }
        });
        recyclerView.setAdapter(adapter);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_close, null)
                .create();
        dialogHolder[0] = dialog;
        dialog.setOnDismissListener(d -> enableImmersiveMode());
        dialog.show();
        recyclerView.post(() -> {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager != null) {
                View firstItem = layoutManager.findViewByPosition(0);
                if (firstItem != null) {
                    firstItem.requestFocus();
                    return;
                }
            }
            recyclerView.requestFocus();
        });
    }

    private void tuneQuickAccessChannel(ChannelItem item) {
        Log.d(TAG, "tuneQuickAccessChannel item=" + (item == null ? "null" : item.id + "/" + item.name));
        if (item == null || item.id == null || item.id.trim().isEmpty()) {
            return;
        }
        showStatus(item.name == null || item.name.trim().isEmpty() ? getString(R.string.status_ready) : item.name.trim());
        tuneChannelById(item.id);
    }

    private List<ChannelItem> buildRecentQuickChannels() {
        List<ChannelItem> recentItems = new ArrayList<>();
        List<RecentChannelsStore.RecentChannelItem> items = recentChannelsStore == null ? new ArrayList<>() : recentChannelsStore.getItems();
        if (items.isEmpty()) {
            return recentItems;
        }
        Map<String, ChannelItem> byId = new LinkedHashMap<>();
        for (ChannelItem item : allChannels) {
            if (item != null && item.id != null && !item.id.trim().isEmpty() && !byId.containsKey(item.id)) {
                byId.put(item.id, item);
            }
        }
        for (RecentChannelsStore.RecentChannelItem recent : items) {
            if (recent == null || recent.channelId == null || recent.channelId.trim().isEmpty()) {
                continue;
            }
            ChannelItem match = byId.get(recent.channelId);
            if (match != null) {
                recentItems.add(match);
            }
        }
        return recentItems;
    }

    private List<ChannelItem> buildFavoriteQuickChannels() {
        List<ChannelItem> favorites = new ArrayList<>();
        if (favoriteChannelIds.isEmpty()) {
            return favorites;
        }
        Map<String, ChannelItem> byId = new LinkedHashMap<>();
        for (ChannelItem item : allChannels) {
            if (item != null && item.id != null && !item.id.trim().isEmpty() && !byId.containsKey(item.id)) {
                byId.put(item.id, item);
            }
        }
        List<String> orderedIds = favoriteOrderStore == null ? new ArrayList<>() : favoriteOrderStore.getOrderedIds();
        Set<String> addedIds = new HashSet<>();
        for (String favoriteId : orderedIds) {
            ChannelItem match = byId.get(favoriteId);
            if (match != null && favoriteChannelIds.contains(favoriteId) && addedIds.add(favoriteId)) {
                favorites.add(match);
            }
        }
        for (ChannelItem item : allChannels) {
            if (item != null && item.id != null && favoriteChannelIds.contains(item.id) && addedIds.add(item.id)) {
                favorites.add(item);
            }
        }
        return favorites;
    }

    private void tunePreviousChannel() {
        List<RecentChannelsStore.RecentChannelItem> items = recentChannelsStore == null ? new ArrayList<>() : recentChannelsStore.getItems();
        if (items.isEmpty()) {
            showStatus(getString(R.string.touch_previous_unavailable));
            return;
        }
        String currentId = (currentIndex >= 0 && currentIndex < channels.size()) ? channels.get(currentIndex).id : lastChannelId;
        for (RecentChannelsStore.RecentChannelItem item : items) {
            if (item == null || item.channelId == null || item.channelId.trim().isEmpty()) {
                continue;
            }
            if (currentId != null && currentId.equals(item.channelId)) {
                continue;
            }
            tuneRecentChannel(item.channelId);
            return;
        }
        showStatus(getString(R.string.touch_previous_unavailable));
    }

    private void tuneRecentChannel(String channelId) {
        tuneChannelById(channelId);
    }

    private void tuneChannelById(String channelId) {
        Log.d(TAG, "tuneChannelById channelId=" + channelId + " currentFilter=" + selectedFilterKey + " favoritesOnly=" + favoritesOnly);
        if (channelId == null || channelId.trim().isEmpty()) {
            return;
        }
        int index = findChannelIndexById(channelId);
        Log.d(TAG, "tuneChannelById initialIndex=" + index + " visibleSize=" + channels.size() + " allSize=" + allChannels.size());
        if (index < 0) {
            syncOverlayCoordinator();
            channelOverlayCoordinator.setSearchQuery("");
            channelOverlayCoordinator.setSelectedFilterKey("all");
            channelOverlayCoordinator.setFavoritesOnly(false);
            channelOverlayCoordinator.refreshVisibleChannels(channelId, channelId);
            syncOverlayStateFromCoordinator();
            clearOverlaySearchQuery();
            channelAdapter.notifyDataSetChanged();
            updateFilterText();
            updateOverlaySearchState();
            index = findChannelIndexById(channelId);
            Log.d(TAG, "tuneChannelById afterRefresh index=" + index + " visibleSize=" + channels.size() + " selectedFilter=" + selectedFilterKey + " favoritesOnly=" + favoritesOnly);
        }
        if (index >= 0) {
            Log.d(TAG, "tuneChannelById finalIndex=" + index + " -> tuneToIndex");
            tuneToIndex(index, true);
        } else {
            ChannelItem directItem = findChannelItemById(channelId);
            if (directItem != null) {
                Log.d(TAG, "tuneChannelById directFallback id=" + directItem.id + " name=" + directItem.name);
                currentIndex = -1;
                selectedOverlayIndex = -1;
                channelAdapter.notifyDataSetChanged();
                playChannelItem(directItem, true);
            } else {
                Log.d(TAG, "tuneChannelById unresolved channelId=" + channelId);
            }
        }
    }

    private void showPlaybackDiagnosticsDialog() {
        PlayerController.PlaybackDiagnostics diagnostics = playerController == null ? null : playerController.getPlaybackDiagnostics();
        ChannelItem currentChannel = getCurrentPlaybackChannelItem();
        PlaybackDiagnosticsStore.ErrorRecord storedError = currentChannel == null || playbackDiagnosticsStore == null
                ? null
                : playbackDiagnosticsStore.getLastError(currentChannel.id);
        if (diagnostics == null || (diagnostics.channelName == null || diagnostics.channelName.trim().isEmpty()) && (diagnostics.targetUrl == null || diagnostics.targetUrl.trim().isEmpty())) {
            String message = getString(R.string.diagnostics_none);
            if (storedError != null) {
                message = message + "\n\n" + getString(R.string.diagnostics_persistent_error, storedError.shortLabel());
            }
            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_playback_diagnostics)
                    .setMessage(message)
                    .setPositiveButton(currentChannel == null ? R.string.diagnostics_action_retry : R.string.diagnostics_action_retry_next_route, (dialog, which) -> {
                        if (currentChannel == null) {
                            retryCurrentPlayback();
                        } else {
                            retryCurrentPlaybackWithNextRoute(currentChannel);
                        }
                    })
                    .setNeutralButton(currentChannel == null ? R.string.dialog_close : R.string.diagnostics_action_more, (dialog, which) -> {
                        if (currentChannel != null) {
                            showPlaybackDiagnosticsActionsDialog(currentChannel);
                        }
                    })
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
        String diagnosticErrorText = "";
        if (diagnostics.lastError != null && !diagnostics.lastError.trim().isEmpty()) {
            diagnosticErrorText = diagnostics.lastError;
            appendDiagnosticLine(message, getString(R.string.diagnostics_last_error, diagnostics.lastError));
        }
        if (storedError != null) {
            if (diagnosticErrorText.trim().isEmpty()) {
                diagnosticErrorText = storedError.message;
            }
            appendDiagnosticLine(message, getString(R.string.diagnostics_persistent_error, storedError.shortLabel()));
            if (!storedError.routeLabel.isEmpty()) {
                appendDiagnosticLine(message, getString(R.string.diagnostics_persistent_route, storedError.routeLabel));
            }
        }
        if (!diagnosticErrorText.trim().isEmpty()) {
            appendDiagnosticLine(message, getString(R.string.diagnostics_error_type, classifyOperationalError(diagnosticErrorText)));
        }
        appendDiagnosticLine(message, getString(R.string.diagnostics_recommendation, buildPlaybackDiagnosticsRecommendation(diagnostics, storedError)));
        if (currentChannel != null && temporaryPlaybackModesByChannelId.containsKey(currentChannel.id)) {
            appendDiagnosticLine(message, getString(R.string.diagnostics_temporary_mode, formatPlaybackModeLabel(temporaryPlaybackModesByChannelId.get(currentChannel.id))));
        }
        if (currentChannel != null && learnedPlaybackModesByChannelId.containsKey(currentChannel.id)) {
            appendDiagnosticLine(message, getString(R.string.diagnostics_learned_mode, formatPlaybackModeLabel(learnedPlaybackModesByChannelId.get(currentChannel.id))));
        }
        appendDiagnosticLine(message, getString(R.string.diagnostics_recent, buildRecentDiagnosticsSummary()));
        appendDiagnosticLine(message, getString(R.string.diagnostics_actions_hint));

        new AlertDialog.Builder(this)
                .setTitle(R.string.title_playback_diagnostics)
                .setMessage(message.toString().trim())
                .setPositiveButton(currentChannel == null ? R.string.diagnostics_action_retry : R.string.diagnostics_action_retry_next_route, (dialog, which) -> {
                    if (currentChannel == null) {
                        retryCurrentPlayback();
                    } else {
                        retryCurrentPlaybackWithNextRoute(currentChannel);
                    }
                })
                .setNeutralButton(currentChannel == null ? R.string.dialog_close : R.string.diagnostics_action_more, (dialog, which) -> {
                    if (currentChannel != null) {
                        showPlaybackDiagnosticsActionsDialog(currentChannel);
                    }
                })
                .setNegativeButton(R.string.dialog_close, null)
                .show();
    }

    private void showPlaybackDiagnosticsActionsDialog(ChannelItem channelItem) {
        if (channelItem == null) {
            return;
        }
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.diagnostics_action_retry_next_route));
        actions.add(() -> retryCurrentPlaybackWithNextRoute(channelItem));
        options.add(getString(R.string.diagnostics_action_retry));
        actions.add(this::retryCurrentPlayback);
        options.add(getString(R.string.diagnostics_action_test_auto));
        actions.add(() -> testPlaybackModeNow(channelItem, PlaybackModeStore.MODE_AUTO));
        options.add(getString(R.string.diagnostics_action_test_direct));
        actions.add(() -> testPlaybackModeNow(channelItem, PlaybackModeStore.MODE_DIRECT));
        options.add(getString(R.string.diagnostics_action_test_proxy));
        actions.add(() -> testPlaybackModeNow(channelItem, PlaybackModeStore.MODE_PROXY));
        options.add(getString(R.string.diagnostics_action_temporary_mode));
        actions.add(() -> showTemporaryPlaybackModeDialog(channelItem));
        options.add(getString(R.string.diagnostics_action_permanent_mode));
        actions.add(() -> showPlaybackModeDialog(channelItem));
        options.add(getString(R.string.diagnostics_action_save_learned));
        actions.add(() -> saveCurrentRouteAsLearned(channelItem));
        options.add(getString(R.string.diagnostics_action_clear_learned));
        actions.add(() -> clearLearnedPlaybackMode(channelItem));
        options.add(getString(R.string.diagnostics_action_history));
        actions.add(this::showPlaybackDiagnosticsHistoryDialog);
        options.add(getString(R.string.diagnostics_action_clear_error));
        actions.add(() -> clearPlaybackDiagnosticsError(channelItem));
        showTvOptionsDialog(R.string.title_playback_diagnostics, displayName(channelItem), options, actions);
    }

    private void testPlaybackModeNow(ChannelItem channelItem, String playbackMode) {
        if (channelItem == null) {
            return;
        }
        if (PlaybackModeStore.MODE_AUTO.equals(playbackMode)) {
            temporaryPlaybackModesByChannelId.remove(channelItem.id);
        } else {
            temporaryPlaybackModesByChannelId.put(channelItem.id, playbackMode);
        }
        showStatus(getString(R.string.status_playback_mode_temporary_changed, formatPlaybackModeLabel(playbackMode)));
        scheduleLearnCurrentPlaybackRoute(channelItem.id, playbackMode);
        retryCurrentPlayback();
    }

    private void retryCurrentPlaybackWithNextRoute(ChannelItem channelItem) {
        if (channelItem == null) {
            showStatus(getString(R.string.diagnostics_none));
            return;
        }
        if (channelItem.isVod && channelItem.directPlayback) {
            showStatus(channelItem.playUrl != null && channelItem.playUrl.contains("/api/vod/runtime/stream/")
                    ? getString(R.string.vod_status_refreshing_runtime)
                    : getString(R.string.vod_status_retrying));
            streamInfoByChannelId.remove(channelItem.id);
            playVodItem(channelItem, true);
            return;
        }
        String nextMode = nextPlaybackMode(resolvePlaybackModeForRequest(channelItem));
        if (PlaybackModeStore.MODE_AUTO.equals(nextMode)) {
            temporaryPlaybackModesByChannelId.remove(channelItem.id);
        } else {
            temporaryPlaybackModesByChannelId.put(channelItem.id, nextMode);
        }
        showStatus(getString(R.string.status_playback_repair_trying, formatPlaybackModeLabel(nextMode)));
        scheduleLearnCurrentPlaybackRoute(channelItem.id, nextMode);
        retryCurrentPlayback();
    }

    private String nextPlaybackMode(String currentMode) {
        if (PlaybackModeStore.MODE_AUTO.equals(currentMode)) {
            return PlaybackModeStore.MODE_DIRECT;
        }
        if (PlaybackModeStore.MODE_DIRECT.equals(currentMode)) {
            return PlaybackModeStore.MODE_PROXY;
        }
        return PlaybackModeStore.MODE_AUTO;
    }

    private void showPlaybackDiagnosticsHistoryDialog() {
        if (playbackDiagnosticsStore == null) {
            showStatus(getString(R.string.diagnostics_history_empty));
            return;
        }
        List<PlaybackDiagnosticsStore.ErrorRecord> records = playbackDiagnosticsStore.getRecentErrors(10);
        if (records.isEmpty()) {
            showStatus(getString(R.string.diagnostics_history_empty));
            return;
        }
        StringBuilder message = new StringBuilder();
        for (PlaybackDiagnosticsStore.ErrorRecord record : records) {
            appendDiagnosticLine(message, formatPlaybackDiagnosticsHistoryItem(record));
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.diagnostics_action_history)
                .setMessage(message.toString())
                .setPositiveButton(R.string.diagnostics_action_clear_history, (dialog, which) -> {
                    playbackDiagnosticsStore.clearAll();
                    if (playerController != null) {
                        playerController.clearLastError();
                    }
                    showStatus(getString(R.string.status_diagnostics_history_cleared));
                })
                .setNegativeButton(R.string.dialog_close, null)
                .show();
    }

    private void clearPlaybackDiagnosticsError(ChannelItem channelItem) {
        if (channelItem == null) {
            return;
        }
        if (playbackDiagnosticsStore != null) {
            playbackDiagnosticsStore.clear(channelItem.id);
        }
        if (playerController != null) {
            playerController.clearLastError();
        }
        showStatus(getString(R.string.status_diagnostics_error_cleared));
    }

    private void saveCurrentRouteAsLearned(ChannelItem channelItem) {
        if (channelItem == null) {
            return;
        }
        PlayerController.PlaybackDiagnostics diagnostics = playerController == null ? null : playerController.getPlaybackDiagnostics();
        String mode = diagnostics == null ? resolvePlaybackModeForRequest(channelItem) : inferPlaybackModeFromDiagnostics(diagnostics);
        if (PlaybackModeStore.MODE_AUTO.equals(mode)) {
            mode = nextPlaybackMode(mode);
        }
        setLearnedPlaybackMode(channelItem.id, mode, true);
    }

    private void clearLearnedPlaybackMode(ChannelItem channelItem) {
        if (channelItem == null || channelItem.id == null) {
            return;
        }
        if (learnedPlaybackModesByChannelId.remove(channelItem.id) != null) {
            saveLearnedPlaybackModes();
        }
        showStatus(getString(R.string.status_playback_learned_cleared));
    }

    private void recordPlaybackError(PlayerController.PlaybackRequest request, PlayerController.PlaybackDiagnostics diagnostics) {
        if (playbackDiagnosticsStore == null || request == null || diagnostics == null) {
            return;
        }
        playbackDiagnosticsStore.recordError(
                request.channelId,
                request.channelName,
                diagnostics.lastError,
                diagnostics.routeLabel,
                diagnostics.playbackMode
        );
        ChannelItem item = findChannelItemById(request.channelId);
        if (item != null && item.isVod) {
            rememberCurrentVodPosition();
            showStatus(getString(R.string.vod_status_failed));
            uiHandler.postDelayed(() -> {
                ChannelItem current = getCurrentPlaybackChannelItem();
                if (current != null && request.channelId.equals(current.id)) {
                    showVodPlaybackRecoveryPanel(current, diagnostics);
                }
            }, 500L);
            return;
        }
        maybeRepairPlaybackAfterError(request);
    }

    private void showVodPlaybackRecoveryPanel(ChannelItem channel, PlayerController.PlaybackDiagnostics diagnostics) {
        if (channel == null) {
            return;
        }
        prepareModalSurface();
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16);
        panel.setPadding(padding, padding, padding, padding);
        panel.setBackgroundColor(0xF0181E28);

        TextView titleView = new TextView(this);
        titleView.setText(getString(R.string.vod_recovery_title, displayName(channel)));
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(20f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setMaxLines(2);
        panel.addView(titleView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView messageView = new TextView(this);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        messageParams.topMargin = dp(8);
        String error = diagnostics == null || diagnostics.lastError == null || diagnostics.lastError.trim().isEmpty()
                ? getString(R.string.error_unknown_reason)
                : diagnostics.lastError.trim();
        messageView.setText(getString(R.string.vod_recovery_message, error));
        messageView.setTextColor(0xFFD5E6F8);
        messageView.setTextSize(14f);
        panel.addView(messageView, messageParams);

        LinearLayout actionsColumn = new LinearLayout(this);
        actionsColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsParams.topMargin = dp(12);
        panel.addView(actionsColumn, actionsParams);

        final AlertDialog[] dialogHolder = new AlertDialog[1];
        List<TextView> actionRows = new ArrayList<>();
        addVodDetailAction(actionsColumn, actionRows, getString(R.string.vod_recovery_retry), () -> {
            dismissDialog(dialogHolder[0]);
            playVodItem(channel, true);
        });
        addVodDetailAction(actionsColumn, actionRows, getString(R.string.vod_action_retry_route), () -> {
            dismissDialog(dialogHolder[0]);
            retryCurrentPlaybackWithNextRoute(channel);
        });
        addVodDetailAction(actionsColumn, actionRows, getString(R.string.vod_action_diagnostics), () -> showVodDiagnosticsDialog(channel));
        addVodDetailAction(actionsColumn, actionRows, getString(R.string.vod_recovery_library), () -> {
            dismissDialog(dialogHolder[0]);
            showVodLibraryDialog();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(panel)
                .setNegativeButton(R.string.dialog_close, null)
                .create();
        dialogHolder[0] = dialog;
        showTvDialog(dialog);
        if (!actionRows.isEmpty()) {
            actionRows.get(0).post(() -> actionRows.get(0).requestFocus());
        }
    }

    private void maybeRepairPlaybackAfterError(PlayerController.PlaybackRequest request) {
        if (!playbackRepairEnabled || request == null || request.channelId == null || request.channelId.trim().isEmpty() || request.directPlayback) {
            return;
        }
        String currentMode = sanitizePlaybackMode(request.playbackMode);
        if (PlaybackModeStore.MODE_AUTO.equals(currentMode) || PlaybackModeStore.MODE_PROXY.equals(currentMode)) {
            return;
        }
        String nextMode = nextPlaybackMode(currentMode);
        if (PlaybackModeStore.MODE_AUTO.equals(nextMode)) {
            return;
        }
        Set<String> attempts = playbackRepairAttemptsByChannelId.get(request.channelId);
        if (attempts == null) {
            attempts = new HashSet<>();
            playbackRepairAttemptsByChannelId.put(request.channelId, attempts);
        }
        if (!attempts.add(nextMode)) {
            return;
        }
        temporaryPlaybackModesByChannelId.put(request.channelId, nextMode);
        showStatus(getString(R.string.status_playback_repair_trying, formatPlaybackModeLabel(nextMode)));
        uiHandler.postDelayed(() -> {
            ChannelItem current = getCurrentPlaybackChannelItem();
            if (current != null && request.channelId.equals(current.id)) {
                retryCurrentPlayback();
            }
        }, 700L);
    }

    private void scheduleLearnCurrentPlaybackRoute(String channelId, String playbackMode) {
        String mode = sanitizePlaybackMode(playbackMode);
        if (!playbackRepairEnabled || PlaybackModeStore.MODE_AUTO.equals(mode) || channelId == null || channelId.trim().isEmpty()) {
            return;
        }
        uiHandler.postDelayed(() -> learnPlaybackModeIfStillCurrent(channelId, mode), 18_000L);
    }

    private void learnPlaybackModeIfStillCurrent(String channelId, String playbackMode) {
        ChannelItem current = getCurrentPlaybackChannelItem();
        if (current == null || !channelId.equals(current.id)) {
            return;
        }
        PlayerController.PlaybackDiagnostics diagnostics = playerController == null ? null : playerController.getPlaybackDiagnostics();
        if (diagnostics == null || !PlaybackModeStore.MODE_AUTO.equals(sanitizePlaybackMode(diagnostics.playbackMode)) && !playbackMode.equals(sanitizePlaybackMode(diagnostics.playbackMode))) {
            return;
        }
        if (diagnostics.lastError != null && !diagnostics.lastError.trim().isEmpty()) {
            return;
        }
        String state = diagnostics.playbackState == null ? "" : diagnostics.playbackState.trim();
        if (!"READY".equals(state)) {
            return;
        }
        playbackRepairAttemptsByChannelId.remove(channelId);
        setLearnedPlaybackMode(channelId, playbackMode, false);
    }

    private void setLearnedPlaybackMode(String channelId, String playbackMode, boolean announce) {
        String mode = sanitizePlaybackMode(playbackMode);
        if (channelId == null || channelId.trim().isEmpty() || PlaybackModeStore.MODE_AUTO.equals(mode)) {
            return;
        }
        learnedPlaybackModesByChannelId.put(channelId, mode);
        saveLearnedPlaybackModes();
        if (announce) {
            showStatus(getString(R.string.status_playback_learned_saved, formatPlaybackModeLabel(mode)));
        }
    }

    private String formatPlaybackDiagnosticsHistoryItem(PlaybackDiagnosticsStore.ErrorRecord record) {
        if (record == null) {
            return "";
        }
        return getString(
                R.string.diagnostics_history_item,
                record.shortLabel(),
                fallbackUnknown(record.channelName),
                fallbackUnknown(record.routeLabel),
                formatPlaybackModeLabel(record.playbackMode),
                fallbackUnknown(record.message)
        );
    }

    private String buildPlaybackDiagnosticsRecommendation(PlayerController.PlaybackDiagnostics diagnostics, PlaybackDiagnosticsStore.ErrorRecord storedError) {
        String route = diagnostics == null ? "" : safeLower(diagnostics.routeLabel);
        String mode = diagnostics == null ? "" : safeLower(diagnostics.playbackMode);
        String error = diagnostics == null ? "" : safeLower(diagnostics.lastError);
        if (error.isEmpty() && storedError != null) {
            error = safeLower(storedError.message);
            route = safeLower(storedError.routeLabel);
            mode = safeLower(storedError.playbackMode);
        }
        if (isAuthRelatedMessage(error)) {
            return getString(R.string.diagnostics_recommend_reactivate);
        }
        if (isLicenseRelatedMessage(error)) {
            return getString(R.string.diagnostics_recommend_license);
        }
        if (isDecoderRelatedMessage(error)) {
            return getString(R.string.diagnostics_recommend_decoder);
        }
        if (route.contains("proxy") || mode.contains(PlaybackModeStore.MODE_PROXY) || error.contains("proxy")) {
            return getString(R.string.diagnostics_recommend_direct);
        }
        if (error.contains("drm") || error.contains("403") || error.contains("401") || error.contains("mime") || route.contains("direct")) {
            return getString(R.string.diagnostics_recommend_proxy);
        }
        return getString(R.string.diagnostics_recommend_auto);
    }

    private String classifyOperationalError(String message) {
        String error = safeLower(message);
        if (isAuthRelatedMessage(error)) {
            return getString(R.string.diagnostics_error_type_token);
        }
        if (isLicenseRelatedMessage(error)) {
            return getString(R.string.diagnostics_error_type_license);
        }
        if (isDecoderRelatedMessage(error)) {
            return getString(R.string.diagnostics_error_type_decoder);
        }
        if (error.contains("manifest") || error.contains("source") || error.contains("m3u8") || error.contains("mpd") || error.contains("404")) {
            return getString(R.string.diagnostics_error_type_manifest);
        }
        if (error.contains("timeout") || error.contains("timed out") || error.contains("network") || error.contains("connect") || error.contains("dns") || error.contains("unreachable")) {
            return getString(R.string.diagnostics_error_type_network);
        }
        if (error.contains("500") || error.contains("502") || error.contains("503") || error.contains("504") || error.contains("server")) {
            return getString(R.string.diagnostics_error_type_server);
        }
        return getString(R.string.diagnostics_error_type_unknown);
    }

    private boolean isAuthRelatedMessage(String message) {
        String error = safeLower(message);
        return error.contains("401")
                || error.contains("403")
                || error.contains("unauthorized")
                || error.contains("forbidden")
                || error.contains("token")
                || error.contains("session")
                || error.contains("sesion")
                || error.contains("expired")
                || error.contains("caduc");
    }

    private boolean isLicenseRelatedMessage(String message) {
        String error = safeLower(message);
        return error.contains("drm")
                || error.contains("widevine")
                || error.contains("license")
                || error.contains("licence")
                || error.contains("licencia")
                || error.contains("clearkey");
    }

    private boolean isDecoderRelatedMessage(String message) {
        String error = safeLower(message);
        return error.contains("decoder")
                || error.contains("mediacodec")
                || error.contains("h265")
                || error.contains("hevc")
                || error.contains("avc")
                || error.contains("codec");
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

    private String sanitizePlaybackMode(String playbackMode) {
        if (PlaybackModeStore.MODE_DIRECT.equals(playbackMode)) {
            return PlaybackModeStore.MODE_DIRECT;
        }
        if (PlaybackModeStore.MODE_PROXY.equals(playbackMode)) {
            return PlaybackModeStore.MODE_PROXY;
        }
        return PlaybackModeStore.MODE_AUTO;
    }

    private String inferPlaybackModeFromDiagnostics(PlayerController.PlaybackDiagnostics diagnostics) {
        String mode = diagnostics == null ? PlaybackModeStore.MODE_AUTO : sanitizePlaybackMode(diagnostics.playbackMode);
        if (!PlaybackModeStore.MODE_AUTO.equals(mode)) {
            return mode;
        }
        String route = diagnostics == null ? "" : safeLower(diagnostics.routeLabel);
        if (route.contains("proxy")) {
            return PlaybackModeStore.MODE_PROXY;
        }
        if (route.contains("direct")) {
            return PlaybackModeStore.MODE_DIRECT;
        }
        return PlaybackModeStore.MODE_AUTO;
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
        String meta = buildZapProgramSummary(channelItem);
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
        String dayLabel = recordingDayLabel(item);
        if (!item.playable) {
            String start = shortTime(item.startTime);
            String end = shortTime(item.endTime);
            String status = humanizeRecordingStatus(item.status);
            String baseMeta = getString(R.string.recording_meta_scheduled, start, end, status);
            if (hasRecordingConflict(item, recordingsController.getCurrentResult())) {
                baseMeta = baseMeta + "  ·  " + getString(R.string.recording_status_conflict);
            }
            if (!dayLabel.isEmpty()) {
                baseMeta = dayLabel + "  ·  " + baseMeta;
            }
            if (item.channelName != null && !item.channelName.trim().isEmpty()) {
                return item.channelName.trim() + "  ·  " + baseMeta;
            }
            return baseMeta;
        }
        String modified = item.modified == null || item.modified.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : item.modified;
        String sizeLabel = item.size <= 0L ? getString(R.string.recording_size_unknown) : humanReadableSize(item.size);
        String baseMeta = getString(R.string.recording_meta, modified, sizeLabel);
        long resumePositionMs = getRecordingResumePosition(item.id);
        if (resumePositionMs > 30_000L) {
            baseMeta = baseMeta + "  ·  " + getString(R.string.recording_progress_label, formatPlaybackPosition(resumePositionMs));
        }
        if (!dayLabel.isEmpty()) {
            baseMeta = dayLabel + "  ·  " + baseMeta;
        }
        if (item.channelName != null && !item.channelName.trim().isEmpty()) {
            return item.channelName.trim() + "  ·  " + baseMeta;
        }
        return baseMeta;
    }

    private String recordingDayLabel(RecordingsRepository.RecordingItem item) {
        long timeMs = recordingTimeMillis(item);
        if (timeMs <= 0L) {
            return "";
        }
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(timeMs);
        if (isSameDay(now, target)) {
            return getString(R.string.recordings_day_today);
        }
        now.add(Calendar.DAY_OF_YEAR, 1);
        if (isSameDay(now, target)) {
            return getString(R.string.recordings_day_tomorrow);
        }
        return new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(new Date(timeMs));
    }

    private String formatPlaybackPosition(long positionMs) {
        long totalSeconds = Math.max(0L, positionMs / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private boolean hasRecordingConflict(RecordingsRepository.RecordingItem item, RecordingsRepository.RecordingsResult result) {
        if (item == null || result == null || result.items == null || !result.scheduledMode) {
            return false;
        }
        long start = parseIsoMillis(item.startTime);
        long end = parseIsoMillis(item.endTime);
        if (start <= 0L || end <= start) {
            return false;
        }
        for (RecordingsRepository.RecordingItem other : result.items) {
            if (other == null || item == other || (item.id != null && item.id.equals(other.id))) {
                continue;
            }
            long otherStart = parseIsoMillis(other.startTime);
            long otherEnd = parseIsoMillis(other.endTime);
            if (otherStart <= 0L || otherEnd <= otherStart) {
                continue;
            }
            if (start < otherEnd && otherStart < end) {
                return true;
            }
        }
        return false;
    }

    private String getCurrentChannelId() {
        if (currentIndex >= 0 && currentIndex < channels.size()) {
            ChannelItem item = channels.get(currentIndex);
            if (item != null && item.id != null && !item.id.trim().isEmpty()) {
                return item.id;
            }
        }
        return lastChannelId;
    }

    private void showLeaveRecordingPrompt() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_recordings_visual)
                .setMessage("¿Salir de la grabacion y volver al canal anterior?")
                .setPositiveButton("Salir", (dialog, which) -> exitRecordingToPreviousChannel())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void exitRecordingToPreviousChannel() {
        rememberCurrentRecordingPosition();
        String channelId = currentPlaybackReturnChannelId;
        currentPlaybackRecordingId = null;
        currentPlaybackReturnChannelId = null;
        if (channelId != null && !channelId.trim().isEmpty()) {
            ChannelItem returnChannel = findChannelItemById(channelId);
            if (returnChannel != null) {
                int visibleIndex = findChannelIndexById(channelId);
                if (visibleIndex >= 0) {
                    tuneToIndex(visibleIndex, true);
                    return;
                }
                playChannelItem(returnChannel, true);
                return;
            }
        }
        openRecordingsBrowser();
    }

    private void rememberCurrentRecordingPosition() {
        if (playerController == null || currentPlaybackRecordingId == null || currentPlaybackRecordingId.trim().isEmpty()) {
            return;
        }
        long positionMs = playerController.getCurrentPlaybackPosition();
        if (positionMs <= 0L) {
            return;
        }
        recordingResumePositions.put(currentPlaybackRecordingId, positionMs);
        saveRecordingResumePositions();
    }

    private long getRecordingResumePosition(String recordingId) {
        if (recordingId == null || recordingId.trim().isEmpty()) {
            return 0L;
        }
        Long value = recordingResumePositions.get(recordingId);
        return value == null ? 0L : Math.max(0L, value);
    }

    private void clearRecordingResumePosition(String recordingId) {
        if (recordingId == null || recordingId.trim().isEmpty()) {
            return;
        }
        if (recordingResumePositions.remove(recordingId) != null) {
            saveRecordingResumePositions();
        }
    }

    private void rememberCurrentVodPosition() {
        if (playerController == null || currentPlaybackVodId == null || currentPlaybackVodId.trim().isEmpty()) {
            return;
        }
        long positionMs = playerController.getCurrentPlaybackPosition();
        if (positionMs <= 0L) {
            return;
        }
        vodResumePositions.put(currentPlaybackVodId, positionMs);
        saveVodResumePositions();
    }

    private long getVodResumePosition(String vodId) {
        if (vodId == null || vodId.trim().isEmpty()) {
            return 0L;
        }
        Long value = vodResumePositions.get(vodId);
        return value == null ? 0L : Math.max(0L, value);
    }

    private void clearVodResumePosition(String vodId) {
        if (vodId == null || vodId.trim().isEmpty()) {
            return;
        }
        if (vodResumePositions.remove(vodId) != null) {
            saveVodResumePositions();
        }
    }

    private void loadVodResumePositions() {
        vodResumePositions.clear();
        if (prefs == null) {
            return;
        }
        String raw = prefs.getString(PREF_VOD_RESUME_POSITIONS, "");
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        try {
            JSONObject json = new JSONObject(raw);
            java.util.Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                vodResumePositions.put(key, Math.max(0L, json.optLong(key, 0L)));
            }
        } catch (Exception e) {
            Log.w(TAG, "failed to load vod resume positions", e);
        }
    }

    private void saveVodResumePositions() {
        if (prefs == null) {
            return;
        }
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, Long> entry : vodResumePositions.entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    continue;
                }
                long value = entry.getValue() == null ? 0L : Math.max(0L, entry.getValue());
                if (value > 0L) {
                    json.put(entry.getKey(), value);
                }
            }
            prefs.edit().putString(PREF_VOD_RESUME_POSITIONS, json.toString()).apply();
        } catch (Exception e) {
            Log.w(TAG, "failed to save vod resume positions", e);
        }
    }

    private void loadGlobalSearchRecents() {
        globalSearchRecents.clear();
        if (prefs == null) {
            return;
        }
        String raw = prefs.getString(PREF_GLOBAL_SEARCH_RECENTS, "");
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        try {
            org.json.JSONArray array = new org.json.JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                String value = cleanText(array.optString(i, ""));
                if (!value.isEmpty() && !globalSearchRecents.contains(value)) {
                    globalSearchRecents.add(value);
                }
                if (globalSearchRecents.size() >= 8) {
                    break;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "failed to load global search recents", e);
        }
    }

    private void rememberGlobalSearchQuery(String query) {
        String value = cleanText(query);
        if (value.length() < 2 || prefs == null) {
            return;
        }
        globalSearchRecents.remove(value);
        globalSearchRecents.add(0, value);
        while (globalSearchRecents.size() > 8) {
            globalSearchRecents.remove(globalSearchRecents.size() - 1);
        }
        saveGlobalSearchRecents();
    }

    private void saveGlobalSearchRecents() {
        if (prefs == null) {
            return;
        }
        try {
            org.json.JSONArray array = new org.json.JSONArray();
            for (String item : globalSearchRecents) {
                if (item != null && !item.trim().isEmpty()) {
                    array.put(item.trim());
                }
            }
            prefs.edit().putString(PREF_GLOBAL_SEARCH_RECENTS, array.toString()).apply();
        } catch (Exception e) {
            Log.w(TAG, "failed to save global search recents", e);
        }
    }

    private void loadLearnedPlaybackModes() {
        learnedPlaybackModesByChannelId.clear();
        if (prefs == null || BuildConfig.STANDALONE_MODE) {
            return;
        }
        String raw = prefs.getString(PREF_PLAYBACK_LEARNED_MODES, "");
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        try {
            JSONObject json = new JSONObject(raw);
            java.util.Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String mode = sanitizePlaybackMode(json.optString(key, PlaybackModeStore.MODE_AUTO));
                if (!PlaybackModeStore.MODE_AUTO.equals(mode)) {
                    learnedPlaybackModesByChannelId.put(key, mode);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "failed to load learned playback modes", e);
        }
    }

    private void saveLearnedPlaybackModes() {
        if (prefs == null || BuildConfig.STANDALONE_MODE) {
            return;
        }
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, String> entry : learnedPlaybackModesByChannelId.entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    continue;
                }
                String mode = sanitizePlaybackMode(entry.getValue());
                if (!PlaybackModeStore.MODE_AUTO.equals(mode)) {
                    json.put(entry.getKey(), mode);
                }
            }
            prefs.edit().putString(PREF_PLAYBACK_LEARNED_MODES, json.toString()).apply();
        } catch (Exception e) {
            Log.w(TAG, "failed to save learned playback modes", e);
        }
    }

    private void loadRecordingResumePositions() {
        recordingResumePositions.clear();
        if (prefs == null) {
            return;
        }
        String raw = prefs.getString(PREF_RECORDING_RESUME_POSITIONS, "");
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        try {
            JSONObject json = new JSONObject(raw);
            java.util.Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                recordingResumePositions.put(key, Math.max(0L, json.optLong(key, 0L)));
            }
        } catch (Exception e) {
            Log.w(TAG, "failed to load recording resume positions", e);
        }
    }

    private void saveRecordingResumePositions() {
        if (prefs == null) {
            return;
        }
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, Long> entry : recordingResumePositions.entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    continue;
                }
                long value = entry.getValue() == null ? 0L : Math.max(0L, entry.getValue());
                if (value > 0L) {
                    json.put(entry.getKey(), value);
                }
            }
            prefs.edit().putString(PREF_RECORDING_RESUME_POSITIONS, json.toString()).apply();
        } catch (Exception e) {
            Log.w(TAG, "failed to save recording resume positions", e);
        }
    }

    private String buildRecordingsSummary(RecordingsRepository.RecordingsResult result) {
        if (result == null || result.items == null || result.items.isEmpty()) {
            if (result != null && result.scheduledMode) {
                return appendRecordingsSummaryFilters(getString(R.string.recordings_summary_scheduled, 0, 0, 0, 0, 0));
            }
            return appendRecordingsSummaryFilters(getString(R.string.recordings_summary_completed, 0));
        }
        if (!result.scheduledMode) {
            return appendRecordingsSummaryFilters(getString(R.string.recordings_summary_completed, result.items.size()));
        }
        RecordingsController.SummaryStats stats = RecordingsController.buildSummaryStats(result);
        return appendRecordingsSummaryFilters(getString(R.string.recordings_summary_scheduled, stats.total, stats.scheduled, stats.recording, stats.issue, stats.conflict));
    }

    private String appendRecordingsSummaryFilters(String summary) {
        String label = buildRecordingsFilterLabel();
        if (label.isEmpty()) {
            return summary;
        }
        return summary + "  ·  " + label;
    }

    private String humanizeRecordingStatus(String status) {
        switch (safeLower(status)) {
            case "scheduled":
            case "pending":
                return getString(R.string.recording_status_scheduled);
            case "recording":
            case "running":
            case "in_progress":
                return getString(R.string.recording_status_recording);
            case "completed":
            case "done":
                return getString(R.string.recording_status_completed);
            case "failed":
            case "error":
                return getString(R.string.recording_status_failed);
            case "cancelled":
            case "canceled":
                return getString(R.string.recording_status_canceled);
            default:
                return getString(R.string.recording_status_unknown);
        }
    }

    private String buildRecordingStatusLabel(RecordingsRepository.RecordingItem item) {
        if (item == null || item.status == null || item.status.trim().isEmpty()) {
            return getString(R.string.recording_status_ready);
        }
        if (hasRecordingConflict(item, recordingsController.getCurrentResult())) {
            return getString(R.string.recording_status_conflict_short);
        }
        String status = item.status.trim().toLowerCase(Locale.US);
        switch (status) {
            case "completed":
                return getString(R.string.recording_status_completed_short);
            case "recording":
                return getString(R.string.recording_status_recording_short);
            case "failed":
            case "error":
                return getString(R.string.recording_status_issue_short);
            case "scheduled":
                return getString(R.string.recording_status_scheduled_short);
            default:
                return status.toUpperCase(Locale.US);
        }
    }

    private int recordingStatusBadgeColor(RecordingsRepository.RecordingItem item) {
        if (item == null || item.status == null) {
            return 0xFF4F3A23;
        }
        if (hasRecordingConflict(item, recordingsController.getCurrentResult())) {
            return 0xFF9A6B28;
        }
        String status = item.status.trim().toLowerCase(Locale.US);
        switch (status) {
            case "completed":
                return 0xFF2E6A57;
            case "recording":
                return 0xFF8B3D2F;
            case "scheduled":
                return 0xFF3F5877;
            case "failed":
            case "error":
                return 0xFF7A3340;
            default:
                return 0xFF4F3A23;
        }
    }

    private int recordingMetaColor(RecordingsRepository.RecordingItem item) {
        if (item == null || item.playable) {
            return 0xFFF2D5AF;
        }
        switch (safeLower(item.status)) {
            case "recording":
            case "running":
            case "in_progress":
                return 0xFF8DE1A5;
            case "failed":
            case "error":
                return 0xFFFF9C9C;
            case "cancelled":
            case "canceled":
                return 0xFFC7D2E2;
            default:
                return 0xFF9BD0FF;
        }
    }

    private void prefetchChannelLogos(List<ChannelItem> items, int maxItems, int widthDp, int heightDp) {
        if (items == null || items.isEmpty()) {
            return;
        }
        int count = 0;
        for (ChannelItem item : items) {
            if (item == null || item.logoUrl == null || item.logoUrl.trim().isEmpty()) {
                continue;
            }
            String trimmedLogoUrl = item.logoUrl.trim();
            if (isSvgLogoUrl(trimmedLogoUrl)) {
                if (channelLogoCache.get(trimmedLogoUrl) == null) {
                    ioExecutor.execute(() -> {
                        Drawable loaded = loadSvgDrawable(trimmedLogoUrl, widthDp, heightDp);
                        if (loaded != null) {
                            channelLogoCache.put(trimmedLogoUrl, loaded);
                        }
                    });
                }
            } else {
                Glide.with(getApplicationContext())
                        .load(trimmedLogoUrl)
                        .fitCenter()
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .preload(dp(widthDp), dp(heightDp));
            }
            count++;
            if (count >= maxItems) {
                break;
            }
        }
    }

    private void prefetchCurrentChannelLogos() {
        if (channels.isEmpty()) {
            return;
        }
        List<ChannelItem> warmList = new ArrayList<>();
        int start = currentIndex >= 0 ? currentIndex : 0;
        for (int offset = 0; offset < channels.size() && warmList.size() < CHANNEL_LOGO_PREFETCH_LIMIT; offset++) {
            int forward = start + offset;
            if (forward >= 0 && forward < channels.size()) {
                warmList.add(channels.get(forward));
            }
            int backward = start - offset - 1;
            if (backward >= 0 && warmList.size() < CHANNEL_LOGO_PREFETCH_LIMIT) {
                warmList.add(channels.get(backward));
            }
        }
        prefetchChannelLogos(warmList, CHANNEL_LOGO_PREFETCH_LIMIT, 42, 42);
    }

    private void bindChannelLogo(ImageView imageView, String logoUrl, String channelName, int widthDp, int heightDp) {
        if (imageView == null) {
            return;
        }
        Drawable fallback = buildChannelLogoFallback(channelName, widthDp, heightDp);
        String trimmedLogoUrl = logoUrl == null ? "" : logoUrl.trim();
        if (trimmedLogoUrl.isEmpty()) {
            Glide.with(imageView.getContext()).clear(imageView);
            imageView.setTag(null);
            imageView.setImageDrawable(fallback);
            return;
        }
        imageView.setTag(trimmedLogoUrl);
        if (isSvgLogoUrl(trimmedLogoUrl)) {
            bindSvgChannelLogo(imageView, trimmedLogoUrl, fallback, widthDp, heightDp);
            return;
        }
        Glide.with(imageView.getContext())
                .load(trimmedLogoUrl)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(fallback)
                .error(fallback)
                .into(imageView);
    }

    private boolean isSvgLogoUrl(String logoUrl) {
        if (logoUrl == null) {
            return false;
        }
        String normalized = logoUrl.trim().toLowerCase(Locale.US);
        if (normalized.contains("format=svg")) {
            return true;
        }
        if (normalized.contains(".svg.png") || normalized.contains(".svg.jpg") || normalized.contains(".svg.jpeg") || normalized.contains(".svg.webp")) {
            return false;
        }
        return normalized.endsWith(".svg")
                || normalized.contains(".svg?")
                || normalized.contains(".svg&")
                || normalized.contains("/svg/");
    }

    private void bindSvgChannelLogo(ImageView imageView, String logoUrl, Drawable fallback, int widthDp, int heightDp) {
        Glide.with(imageView.getContext()).clear(imageView);
        Drawable cached = channelLogoCache.get(logoUrl);
        if (cached != null) {
            imageView.setImageDrawable(cached);
            return;
        }
        imageView.setImageDrawable(fallback);
        ioExecutor.execute(() -> {
            Drawable loaded = loadSvgDrawable(logoUrl, widthDp, heightDp);
            if (loaded != null) {
                channelLogoCache.put(logoUrl, loaded);
            }
            uiHandler.post(() -> {
                Object tag = imageView.getTag();
                if (!(tag instanceof String) || !logoUrl.equals(tag)) {
                    return;
                }
                imageView.setImageDrawable(loaded != null ? loaded : fallback);
            });
        });
    }

    private Drawable loadSvgDrawable(String logoUrl, int widthDp, int heightDp) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(logoUrl).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "DRBEP-TVPlayer/1.3.2");
            connection.connect();
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                return null;
            }
            try (InputStream stream = connection.getInputStream()) {
                SVG svg = SVG.getFromInputStream(stream);
                int widthPx = Math.max(1, dp(widthDp));
                int heightPx = Math.max(1, dp(heightDp));
                svg.setDocumentWidth(widthPx);
                svg.setDocumentHeight(heightPx);
                Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                svg.renderToCanvas(canvas);
                return new BitmapDrawable(getResources(), bitmap);
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to render SVG logo: " + logoUrl, e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private Drawable buildChannelLogoFallback(String channelName, int widthDp, int heightDp) {
        float density = getResources().getDisplayMetrics().density;
        int widthPx = Math.max(1, Math.round(widthDp * density));
        int heightPx = Math.max(1, Math.round(heightDp * density));
        Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(0xFF243547);
        canvas.drawRect(0, 0, widthPx, heightPx, bgPaint);

        String initials = buildChannelInitials(channelName);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFF7FAFF);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(heightPx * (initials.length() >= 3 ? 0.34f : 0.46f));

        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float centerY = (heightPx - metrics.ascent - metrics.descent) / 2f;
        canvas.drawText(initials, widthPx / 2f, centerY, textPaint);

        return new BitmapDrawable(getResources(), bitmap);
    }

    private String buildChannelInitials(String channelName) {
        String normalized = channelName == null ? "" : channelName.trim().toUpperCase(Locale.getDefault());
        if (normalized.isEmpty()) {
            return "TV";
        }
        String[] rawTokens = normalized.split("[^A-Z0-9]+");
        List<String> tokens = new ArrayList<>();
        for (String token : rawTokens) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        if (tokens.isEmpty()) {
            return "TV";
        }

        StringBuilder out = new StringBuilder();
        for (String token : tokens) {
            if (token.chars().allMatch(Character::isDigit)) {
                out.append(token.charAt(0));
                break;
            }
            if (out.length() >= 2) {
                break;
            }
            out.append(token.charAt(0));
        }
        if (out.length() == 0) {
            out.append(tokens.get(0).charAt(0));
        }
        if (out.length() == 1 && tokens.size() > 1 && !tokens.get(1).chars().allMatch(Character::isDigit)) {
            out.append(tokens.get(1).charAt(0));
        }
        return out.length() > 3 ? out.substring(0, 3) : out.toString();
    }

    private void bindProgramPoster(ImageView imageView, String posterUrl) {
        bindPoster(imageView, posterUrl, true);
    }

    private void bindRecordingPoster(ImageView imageView, String posterUrl) {
        bindPoster(imageView, posterUrl, false);
    }

    private void bindPoster(ImageView imageView, String posterUrl, boolean fitInside) {
        if (imageView == null) {
            return;
        }
        if (posterUrl == null || posterUrl.trim().isEmpty()) {
            imageView.setVisibility(View.GONE);
            Glide.with(imageView.getContext()).clear(imageView);
            imageView.setTag(null);
            return;
        }
        String trimmedPosterUrl = posterUrl.trim();
        imageView.setVisibility(View.VISIBLE);
        imageView.setTag(trimmedPosterUrl);
        if (fitInside) {
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Glide.with(imageView.getContext())
                    .load(trimmedPosterUrl)
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(imageView);
            return;
        }
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(imageView.getContext())
                .load(trimmedPosterUrl)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
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

    private void applyTimelineBlockState(TextView block, boolean live, boolean scheduled, boolean focused) {
        if (block == null) {
            return;
        }
        int bgColor;
        if (focused) {
            if (scheduled) {
                bgColor = 0xFF9A6A1F;
            } else {
                bgColor = live ? 0xFF49A06E : 0xFF4A6F98;
            }
            block.setScaleX(1.03f);
            block.setScaleY(1.03f);
        } else {
            if (scheduled) {
                bgColor = 0xFF6E4A16;
            } else {
                bgColor = live ? 0xFF276B49 : 0xFF2B4056;
            }
            block.setScaleX(1.0f);
            block.setScaleY(1.0f);
        }
        block.setBackgroundColor(bgColor);
    }

    private String buildVodRowMeta(ChannelItem channel) {
        List<String> parts = new ArrayList<>();
        if (channel == null) {
            return getString(R.string.search_channel_action_hint);
        }
        if (channel.platformName != null && !channel.platformName.trim().isEmpty()) {
            parts.add(channel.platformName.trim());
        }
        if (channel.group != null && !channel.group.trim().isEmpty()) {
            parts.add(channel.group.trim());
        }
        if (channel.vodYear != null && !channel.vodYear.trim().isEmpty()) {
            parts.add(channel.vodYear.trim());
        }
        if (channel.vodDurationSeconds > 0L) {
            parts.add(formatDurationShort(channel.vodDurationSeconds * 1000L));
        }
        long resumeMs = getVodResumePosition(channel.id);
        if (resumeMs > 30_000L) {
            parts.add(getString(R.string.vod_resume_meta, formatDurationShort(resumeMs)));
        }
        parts.add(getString(R.string.vod_row_hint));
        return TextUtils.join("  ·  ", parts);
    }

    private boolean isProgramScheduled(ChannelItem channel, EpgRepository.EpgProgram program, List<RecordingsRepository.RecordingItem> scheduledItems) {
        return findScheduledProgramRecording(channel, program, scheduledItems) != null;
    }

    private RecordingsRepository.RecordingItem findScheduledProgramRecording(ChannelItem channel, EpgRepository.EpgProgram program, List<RecordingsRepository.RecordingItem> scheduledItems) {
        if (channel == null || program == null || scheduledItems == null || scheduledItems.isEmpty()) {
            return null;
        }
        long programStartMs = parseIsoMillis(program.startTime);
        long programEndMs = parseIsoMillis(program.endTime);
        String normalizedChannel = normalizeScheduledText(channel.name);
        String normalizedTitle = normalizeScheduledText(program.title);
        for (RecordingsRepository.RecordingItem item : scheduledItems) {
            if (item == null) {
                continue;
            }
            String itemChannel = normalizeScheduledText(item.channelName);
            String itemTitle = normalizeScheduledText(item.programTitle == null || item.programTitle.trim().isEmpty() ? item.name : item.programTitle);
            if (!normalizedChannel.isEmpty() && !itemChannel.isEmpty() && !normalizedChannel.equals(itemChannel)) {
                continue;
            }
            if (!normalizedTitle.isEmpty() && !itemTitle.isEmpty() && !normalizedTitle.equals(itemTitle)) {
                continue;
            }
            long itemStartMs = parseIsoMillis(item.startTime);
            long itemEndMs = parseIsoMillis(item.endTime);
            boolean timeMatches = programStartMs > 0L && itemStartMs > 0L && Math.abs(programStartMs - itemStartMs) < 120000L;
            if (!timeMatches && programEndMs > 0L && itemEndMs > 0L) {
                timeMatches = Math.abs(programEndMs - itemEndMs) < 120000L;
            }
            if (timeMatches || (!normalizedChannel.isEmpty() && normalizedChannel.equals(itemChannel) && !normalizedTitle.isEmpty() && normalizedTitle.equals(itemTitle))) {
                return item;
            }
        }
        return null;
    }

    private String normalizeScheduledText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
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
            holder.logo.setTag(null);
            String query = channelOverlayCoordinator == null ? "" : channelOverlayCoordinator.getSearchQuery();
            holder.name.setText(buildHighlightedText(displayName(ch), query, ch.favorite));
            if (ch.isVod) {
                String vodMeta = buildVodRowMeta(ch);
                String listLabel = buildChannelMembershipLabel(ch, 2);
                if (!listLabel.isEmpty()) {
                    vodMeta = listLabel + "  ·  " + vodMeta;
                }
                holder.meta.setText(buildHighlightedText(vodMeta, query, false));
                holder.typeBadge.setVisibility(View.VISIBLE);
                holder.typeBadge.setText(ch.isAdultVod ? getString(R.string.channel_badge_vod_adult) : getString(R.string.channel_badge_vod));
                holder.typeBadge.setTextColor(ch.isAdultVod ? 0xFFFFD6D6 : 0xFFDDE8F6);
                ViewGroup.LayoutParams plateParams = holder.logoPlate.getLayoutParams();
                plateParams.width = dp(54);
                plateParams.height = dp(72);
                holder.logoPlate.setLayoutParams(plateParams);
                holder.logoPlate.setPadding(0, 0, 0, 0);
                bindRecordingPoster(holder.logo, ch.logoUrl);
            } else {
                String tag = profileTag(ch);
                String listLabel = buildChannelMembershipLabel(ch, 2);
                String metaText = "";
                if (ch.nowProgram != null && !ch.nowProgram.trim().isEmpty()) {
                    metaText = tag.isEmpty() ? ch.nowProgram : tag + "  ·  " + ch.nowProgram;
                } else if (ch.group != null && !ch.group.trim().isEmpty()) {
                    metaText = tag.isEmpty() ? ch.group : tag + "  ·  " + ch.group;
                } else if (!tag.isEmpty()) {
                    metaText = tag;
                }
                if (!listLabel.isEmpty()) {
                    metaText = metaText.isEmpty() ? listLabel : listLabel + "  ·  " + metaText;
                }
                holder.meta.setText(buildHighlightedText(metaText, query, false));
                holder.typeBadge.setVisibility(View.GONE);
                ViewGroup.LayoutParams plateParams = holder.logoPlate.getLayoutParams();
                plateParams.width = getResources().getDimensionPixelSize(R.dimen.channel_logo_plate_size);
                plateParams.height = getResources().getDimensionPixelSize(R.dimen.channel_logo_plate_size);
                holder.logoPlate.setLayoutParams(plateParams);
                int logoPadding = getResources().getDimensionPixelSize(R.dimen.channel_logo_plate_padding);
                holder.logoPlate.setPadding(logoPadding, logoPadding, logoPadding, logoPadding);
                bindChannelLogo(holder.logo, ch.logoUrl, displayName(ch), 38, 38);
            }
            if (touchDeviceMode) {
                holder.favoriteToggle.setVisibility(View.VISIBLE);
                holder.favoriteToggle.setText(getString(ch.favorite ? R.string.overlay_favorite_toggle_on : R.string.overlay_favorite_toggle_off));
                holder.favoriteToggle.setTextColor(ch.favorite ? 0xFFFFD54F : 0xFFFFFFFF);
                holder.favoriteToggle.setOnClickListener(v -> {
                    selectedOverlayIndex = position;
                    toggleFavoriteSelected();
                });
            } else {
                holder.favoriteToggle.setVisibility(View.GONE);
                holder.favoriteToggle.setOnClickListener(null);
            }

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
            TextView meta;
            TextView favoriteToggle;
            TextView typeBadge;
            ImageView logo;
            ViewGroup logoPlate;

            ChannelVH(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.channelCard);
                name = itemView.findViewById(R.id.channelName);
                meta = itemView.findViewById(R.id.channelMeta);
                favoriteToggle = itemView.findViewById(R.id.channelFavoriteToggle);
                typeBadge = itemView.findViewById(R.id.channelTypeBadge);
                logo = itemView.findViewById(R.id.channelLogo);
                logoPlate = itemView.findViewById(R.id.channelLogoPlate);
            }
        }
    }

    private final class PersonalListAdapter extends RecyclerView.Adapter<PersonalListAdapter.PersonalListVH> {
        interface OnPersonalListChosenListener {
            void onPersonalListChosen(ChannelCollectionStore.ChannelCollection collection);
        }

        private final List<ChannelCollectionStore.ChannelCollection> items = new ArrayList<>();
        private final OnPersonalListChosenListener clickListener;
        private final OnPersonalListChosenListener actionsListener;

        PersonalListAdapter(List<ChannelCollectionStore.ChannelCollection> initialItems, OnPersonalListChosenListener clickListener, OnPersonalListChosenListener actionsListener) {
            this.clickListener = clickListener;
            this.actionsListener = actionsListener;
            if (initialItems != null) {
                items.addAll(initialItems);
            }
        }

        @NonNull
        @Override
        public PersonalListVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_personal_list, parent, false);
            return new PersonalListVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PersonalListVH holder, int position) {
            ChannelCollectionStore.ChannelCollection collection = items.get(position);
            holder.badge.setText(String.valueOf(Math.min(99, collection.channelIds.size())));
            holder.name.setText(collection.label);
            holder.preview.setText(getString(R.string.personal_list_count, collection.channelIds.size()) + "  ·  " + buildPersonalListPreview(collection));
            holder.action.setText(R.string.personal_list_action_badge);
            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onPersonalListChosen(collection);
                }
            });
            holder.itemView.setOnLongClickListener(v -> {
                if (actionsListener != null) {
                    actionsListener.onPersonalListChosen(collection);
                }
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        final class PersonalListVH extends RecyclerView.ViewHolder {
            final TextView badge;
            final TextView name;
            final TextView preview;
            final TextView action;

            PersonalListVH(@NonNull View itemView) {
                super(itemView);
                badge = itemView.findViewById(R.id.personalListBadgeText);
                name = itemView.findViewById(R.id.personalListNameText);
                preview = itemView.findViewById(R.id.personalListPreviewText);
                action = itemView.findViewById(R.id.personalListActionText);
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

    private final class EpgSearchResultAdapter extends RecyclerView.Adapter<EpgSearchResultAdapter.EpgSearchResultVH> {
        interface OnEpgSearchResultChosenListener {
            void onEpgSearchResultChosen(EpgSearchResult result);
        }

        private final List<EpgSearchResult> items = new ArrayList<>();
        private final OnEpgSearchResultChosenListener listener;

        EpgSearchResultAdapter(List<EpgSearchResult> initialItems, OnEpgSearchResultChosenListener listener) {
            this.listener = listener;
            if (initialItems != null) {
                items.addAll(initialItems);
            }
        }

        @NonNull
        @Override
        public EpgSearchResultVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_epg_search_result, parent, false);
            return new EpgSearchResultVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EpgSearchResultVH holder, int position) {
            EpgSearchResult result = items.get(position);
            ChannelItem channel = result == null ? null : result.channel;
            EpgRepository.EpgProgram program = result == null ? null : result.program;
            String title = program == null || program.title == null || program.title.trim().isEmpty()
                    ? getString(R.string.label_program_default)
                    : program.title.trim();
            holder.title.setText(title);
            String channelName = channel == null ? "" : displayName(channel);
            String time = program == null ? "" : shortTime(program.startTime) + " - " + shortTime(program.endTime);
            holder.meta.setText((channelName + "  ·  " + time).trim());
            boolean live = program != null && program.progress >= 0;
            holder.badge.setText(live ? R.string.epg_search_badge_live : R.string.epg_search_badge_next);
            holder.badge.setBackgroundTintList(ColorStateList.valueOf(live ? 0xFF276B49 : 0xFF1E2D3E));
            String poster = program == null || program.icon == null || program.icon.trim().isEmpty()
                    ? (channel == null ? "" : channel.logoUrl)
                    : program.icon.trim();
            bindProgramPoster(holder.poster, poster);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEpgSearchResultChosen(result);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        final class EpgSearchResultVH extends RecyclerView.ViewHolder {
            final ImageView poster;
            final TextView title;
            final TextView meta;
            final TextView badge;

            EpgSearchResultVH(@NonNull View itemView) {
                super(itemView);
                poster = itemView.findViewById(R.id.epgSearchPosterImage);
                title = itemView.findViewById(R.id.epgSearchTitleText);
                meta = itemView.findViewById(R.id.epgSearchMetaText);
                badge = itemView.findViewById(R.id.epgSearchBadgeText);
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
            holder.meta.setTextColor(recordingMetaColor(item));
            holder.status.setText(buildRecordingStatusLabel(item));
            holder.status.setBackgroundTintList(ColorStateList.valueOf(recordingStatusBadgeColor(item)));
            bindRecordingPoster(holder.poster, item.poster);
            boolean selected = position == recordingsController.getSelectedIndex();
            holder.itemView.setBackgroundColor(selected ? 0xFF80542A : 0xFF2C2419);
            holder.itemView.setOnClickListener(v -> {
                recordingsController.selectIndex(position);
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
            final TextView status;
            final ImageView poster;

            RecordingVH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.recordingNameText);
                meta = itemView.findViewById(R.id.recordingMetaText);
                status = itemView.findViewById(R.id.recordingStatusBadge);
                poster = itemView.findViewById(R.id.recordingPosterThumb);
            }
        }
    }

    private final class GlobalSearchAdapter extends RecyclerView.Adapter<GlobalSearchAdapter.GlobalSearchVH> {
        interface OnGlobalSearchResultChosenListener {
            void onGlobalSearchResultChosen(GlobalSearchResult result);
        }

        private final List<GlobalSearchResult> items = new ArrayList<>();
        private final OnGlobalSearchResultChosenListener listener;

        GlobalSearchAdapter(List<GlobalSearchResult> initialItems, OnGlobalSearchResultChosenListener listener) {
            this.listener = listener;
            submitList(initialItems);
        }

        void submitList(List<GlobalSearchResult> newItems) {
            items.clear();
            if (newItems != null) {
                items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public GlobalSearchVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_search_channel, parent, false);
            return new GlobalSearchVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GlobalSearchVH holder, int position) {
            GlobalSearchResult result = items.get(position);
            boolean header = result != null && result.type == GLOBAL_SEARCH_HEADER;
            holder.logo.setVisibility(header ? View.GONE : View.VISIBLE);
            holder.meta.setVisibility(header || result == null || result.meta == null || result.meta.trim().isEmpty() ? View.GONE : View.VISIBLE);
            holder.type.setVisibility(header || result == null || result.badge == null || result.badge.trim().isEmpty() ? View.GONE : View.VISIBLE);
            holder.name.setText(result == null ? "" : result.title);
            holder.name.setTextColor(header ? 0xFF9BD0FF : 0xFFFFFFFF);
            holder.name.setTextSize(header ? 13f : 18f);
            holder.name.setTypeface(Typeface.DEFAULT, header ? Typeface.BOLD : Typeface.BOLD);
            holder.meta.setText(result == null ? "" : result.meta);
            holder.type.setText(result == null ? "" : result.badge);
            holder.itemView.setFocusable(!header);
            holder.itemView.setClickable(!header);
            holder.itemView.setBackgroundColor(header ? 0x00000000 : 0xFF182638);
            if (!header && result != null) {
                bindGlobalSearchImage(holder.logo, result);
                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onGlobalSearchResultChosen(result);
                    }
                });
                holder.itemView.setOnLongClickListener(v -> {
                    showGlobalSearchActions(result);
                    return true;
                });
            } else {
                holder.itemView.setOnClickListener(null);
                holder.itemView.setOnLongClickListener(null);
            }
        }

        private void bindGlobalSearchImage(ImageView logo, GlobalSearchResult result) {
            if (result == null || logo == null) {
                return;
            }
            if (result.channel != null) {
                bindChannelLogo(logo, result.channel.logoUrl, displayName(result.channel), 42, 42);
                return;
            }
            if (result.epgResult != null) {
                EpgRepository.EpgProgram program = result.epgResult.program;
                ChannelItem channel = result.epgResult.channel;
                String poster = program == null || program.icon == null || program.icon.trim().isEmpty()
                        ? (channel == null ? "" : channel.logoUrl)
                        : program.icon.trim();
                bindProgramPoster(logo, poster);
                return;
            }
            if (result.recording != null) {
                bindRecordingPoster(logo, result.recording.poster);
                return;
            }
            logo.setImageDrawable(null);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        final class GlobalSearchVH extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView meta;
            final TextView type;
            final ImageView logo;

            GlobalSearchVH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.searchChannelNameText);
                meta = itemView.findViewById(R.id.searchChannelMetaText);
                type = itemView.findViewById(R.id.searchChannelTypeText);
                logo = itemView.findViewById(R.id.searchChannelLogo);
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
            holder.name.setText(item.favorite ? "★ " + displayName(item) : displayName(item));
            String primaryMeta = item.isVod ? buildVodRowMeta(item) : (item.nowProgram != null && !item.nowProgram.trim().isEmpty() ? item.nowProgram : item.group);
            if (primaryMeta == null || primaryMeta.trim().isEmpty()) {
                primaryMeta = getString(R.string.search_channel_action_hint);
            }
            String listLabel = buildChannelMembershipLabel(item, 2);
            if (!listLabel.isEmpty()) {
                primaryMeta = listLabel + "  ·  " + primaryMeta;
            }
            holder.meta.setText(primaryMeta);
            String typeLabel;
            if (item.isAdultVod) {
                typeLabel = getString(R.string.channel_badge_vod_adult);
            } else if (item.isVod) {
                typeLabel = getString(R.string.channel_badge_vod);
            } else {
                typeLabel = getString(R.string.channel_badge_live);
            }
            holder.type.setText(typeLabel);
            bindChannelLogo(holder.logo, item.logoUrl, displayName(item), 42, 42);
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
            final TextView type;
            final ImageView logo;

            SearchChannelVH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.searchChannelNameText);
                meta = itemView.findViewById(R.id.searchChannelMetaText);
                type = itemView.findViewById(R.id.searchChannelTypeText);
                logo = itemView.findViewById(R.id.searchChannelLogo);
            }
        }
    }

    private final class VodShelfRecyclerView extends RecyclerView {
        private final List<RecyclerView> shelfRows;
        private final int rowIndex;
        private final android.widget.ScrollView scrollView;

        VodShelfRecyclerView(List<RecyclerView> shelfRows, int rowIndex, android.widget.ScrollView scrollView) {
            super(MainActivity.this);
            this.shelfRows = shelfRows;
            this.rowIndex = rowIndex;
            this.scrollView = scrollView;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                int focusedPosition = findFocusedVodShelfPosition(this, findFocus());
                if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                    return focusVodShelfItem(shelfRows, rowIndex + 1, focusedPosition, scrollView);
                }
            }
            return super.dispatchKeyEvent(event);
        }
    }

    private final class VodPosterAdapter extends RecyclerView.Adapter<VodPosterAdapter.VodPosterVH> {
        private final List<ChannelItem> items = new ArrayList<>();
        private final List<RecyclerView> shelfRows;
        private final int rowIndex;
        private final android.widget.ScrollView scrollView;

        VodPosterAdapter(List<ChannelItem> initialItems, List<RecyclerView> shelfRows, int rowIndex, android.widget.ScrollView scrollView) {
            if (initialItems != null) {
                items.addAll(initialItems);
            }
            this.shelfRows = shelfRows;
            this.rowIndex = rowIndex;
            this.scrollView = scrollView;
        }

        @NonNull
        @Override
        public VodPosterVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_vod_poster, parent, false);
            return new VodPosterVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VodPosterVH holder, int position) {
            ChannelItem item = items.get(position);
            holder.title.setText(displayName(item));
            holder.meta.setText(buildVodPosterMeta(item));
            long progressMs = getVodResumePosition(item == null ? null : item.id);
            if (progressMs > 30_000L) {
                holder.progress.setText(formatDurationShort(progressMs));
                holder.progress.setVisibility(View.VISIBLE);
            } else {
                holder.progress.setVisibility(View.GONE);
            }
            bindRecordingPoster(holder.poster, item == null ? "" : item.logoUrl);
            holder.itemView.setNextFocusUpId(View.NO_ID);
            holder.itemView.setOnClickListener(v -> showVodInfoDialog(item));
            holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    ensureVodVisualItemVisible(scrollView, v);
                }
            });
            holder.itemView.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_MENU) {
                    showVodActionsDialog(item);
                    return true;
                }
                int currentIndex = holder.getBindingAdapterPosition();
                if (currentIndex == RecyclerView.NO_POSITION) {
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (currentIndex > 0) {
                        return focusVodShelfItem(shelfRows, rowIndex, currentIndex - 1, scrollView);
                    }
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (currentIndex + 1 < getItemCount()) {
                        return focusVodShelfItem(shelfRows, rowIndex, currentIndex + 1, scrollView);
                    }
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    return focusVodShelfItem(shelfRows, rowIndex - 1, currentIndex, scrollView);
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    return focusVodShelfItem(shelfRows, rowIndex + 1, currentIndex, scrollView);
                }
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private String buildVodPosterMeta(ChannelItem item) {
            if (item == null) {
                return "";
            }
            List<String> parts = new ArrayList<>();
            if (item.vodYear != null && !item.vodYear.trim().isEmpty()) {
                parts.add(item.vodYear.trim());
            }
            if (item.platformName != null && !item.platformName.trim().isEmpty()) {
                parts.add(item.platformName.trim());
            }
            if (parts.isEmpty() && item.group != null && !item.group.trim().isEmpty()) {
                parts.add(item.group.trim());
            }
            return TextUtils.join("  ·  ", parts);
        }

        final class VodPosterVH extends RecyclerView.ViewHolder {
            final ImageView poster;
            final TextView title;
            final TextView meta;
            final TextView progress;

            VodPosterVH(@NonNull View itemView) {
                super(itemView);
                poster = itemView.findViewById(R.id.vodPosterImage);
                title = itemView.findViewById(R.id.vodPosterTitleText);
                meta = itemView.findViewById(R.id.vodPosterMetaText);
                progress = itemView.findViewById(R.id.vodProgressBadgeText);
            }
        }
    }
}
