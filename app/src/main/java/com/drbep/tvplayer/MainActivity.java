package com.drbep.tvplayer;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Build;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.LruCache;
import android.util.Log;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.compose.ui.platform.ComposeView;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.lifecycle.ViewTreeViewModelStoreOwner;
import androidx.media3.ui.PlayerView;
import androidx.media3.common.util.UnstableApi;
import androidx.savedstate.ViewTreeSavedStateRegistryOwner;

import org.json.JSONArray;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.HashMap;
import java.util.Map;

@OptIn(markerClass = UnstableApi.class)
public class MainActivity extends FragmentActivity {
    private static final String TAG = "DRBEP-TV-Native";
    private static final long OVERLAY_HIDE_MS = 6000L;
    private static final long OVERLAY_RENDER_COALESCE_MS = 32L;
    private static final long PLAYBACK_QUALITY_UI_COALESCE_MS = 120L;
    private static final long TOUCH_CONTROLS_HIDE_MS = 3000L;
    private static final long TV_TIMESHIFT_HUD_HIDE_MS = 3500L;
    private static final long MENU_DOUBLE_PRESS_MS = 450L;
    private static final long LIVE_BADGE_THRESHOLD_MS = 15000L;
    private static final long OFFLINE_CATALOG_AUTO_REFRESH_MS = 30L * 60L * 1000L;
    private static final long OFFLINE_CATALOG_EXPIRY_REFRESH_MS = 12L * 60L * 60L * 1000L;
    private static final long OFFLINE_CATALOG_RETRY_BASE_MS = 15L * 60L * 1000L;
    private static final long OFFLINE_CATALOG_RETRY_MAX_MS = 60L * 60L * 1000L;
    private static final long OFFLINE_STARTUP_MAINTENANCE_GRACE_MS = 5L * 60L * 1000L;
    private static final long OFFLINE_APP_UPDATE_STARTUP_DELAY_MS = 2L * 60L * 1000L;
    private static final long OFFLINE_APP_UPDATE_RESUME_CHECK_MS = 15L * 60L * 1000L;
    private static final long OFFLINE_EPG_INITIAL_DELAY_MS = 20L * 1000L;
    private static final long OFFLINE_EPG_PROGRESSIVE_DELAY_MS = 8L * 1000L;
    private static final long OFFLINE_EPG_PRIORITY_DELAY_MS = 2L * 1000L;
    private static final long OFFLINE_EPG_BUSY_RETRY_MS = 5L * 1000L;
    private static final long OFFLINE_EPG_LOAD_TIMEOUT_MS = 25L * 1000L;
    private static final int OFFLINE_EPG_VISIBLE_BATCH_LIMIT = 48;
    private static final int OFFLINE_EPG_COMPACT_BATCH_LIMIT = 12;
    private static final long PLAYBACK_HEARTBEAT_INTERVAL_MS = 30L * 1000L;
    private static final int OFFLINE_SYNC_HISTORY_LIMIT = 8;
    private static final int CHANNEL_LOGO_PREFETCH_LIMIT = 36;
    private static final int SEARCH_LOGO_PREFETCH_LIMIT = 18;
    private static final int VOD_DENSE_PAGE_SIZE = 160;
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
    private static final String PREF_PLAYBACK_QUALITY_MODE = "playback_quality_mode";
    private static final String PREF_PLAYBACK_LEARNED_MODES = "playback_learned_modes";
    private static final String PREF_OFFLINE_SYNC_HISTORY = "offline_sync_history";
    private static final String PREF_APP_UPDATE_DIAGNOSTIC = "app_update_diagnostic";
    private static final String PREF_MULTIVIEW_PRESET_PREFIX = "multiview_preset_";
    private static final String PREF_LAST_UPDATE_PROMPT_VERSION_CODE = "last_update_prompt_version_code";
    private static final String PREF_UPDATE_CHANNEL = "update_channel";
    private static final String PREF_LAST_SEEN_APP_VERSION_CODE = "last_seen_app_version_code";
    private static final String PREF_PENDING_UPDATE_HEALTH_VERSION_CODE = "pending_update_health_version_code";
    private static final String PREF_LAST_GOOD_APP_VERSION_CODE = "last_good_app_version_code";
    private static final String PREF_LAST_GOOD_APP_VERSION_NAME = "last_good_app_version_name";
    private static final String PREF_UPDATE_HEALTH_STATE = "update_health_state";
    private static final String PREF_LAST_UPDATE_HEALTH_AT_MS = "last_update_health_at_ms";
    private static final String PREF_LAST_UPDATE_HEALTH_ERROR = "last_update_health_error";
    private static final String UPDATE_HEALTH_PENDING = "pending";
    private static final String UPDATE_HEALTH_CATALOG_OK = "catalog_ok";
    private static final String UPDATE_HEALTH_GOOD = "good";
    private static final String UPDATE_HEALTH_FAILED = "failed";
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
    private static final int TIMELINE_MAX_RENDERED_CHANNELS = 48;
    private static final int VISUAL_EPG_MAX_ITEMS_PER_SECTION = 40;
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
    private static final String PARENTAL_PREF_PREFIX = "parental_control";

    private PlayerView playerView;
    private OverlayUiController overlayUiController;
    private ComposeView overlayControlsComposeView;
    private View touchHomeHub;
    private ComposeView touchHomeComposeView;
    private View multiViewContainer;
    private ComposeView multiViewHeaderComposeView;
    private final PlayerView[] multiPlayerViews = new PlayerView[4];
    private final View[] multiTiles = new View[4];
    private final ComposeView[] multiOverlayViews = new ComposeView[4];
    private final List<PlayerController> multiPlayerControllers = new ArrayList<>();
    private final List<ChannelItem> multiViewChannels = new ArrayList<>();
    private final String[] multiViewChannelIds = new String[4];
    private int multiViewActiveIndex = 0;
    private boolean mainWasPlayingBeforeMultiView;
    private ComposeView overlayNowPlayingComposeView;
    private View overlayNowPlayingSection;
    private View overlayExploreSection;
    private View overlayListSection;
    private ComposeView hdrBadgeText;
    private ComposeView liveStateBadgeText;
    private View touchControlsBar;
    private View timeshiftBarContainer;
    private ComposeView touchControlsComposeView;
    private ComposeView timeshiftComposeView;
    private boolean touchSurfaceHudVisible;
    private TouchControlsBarUiModel currentTouchControlsBarModel;
    private final TouchControlsFocusState touchControlsFocusState = new TouchControlsFocusState();
    private View playbackGestureLayer;
    private android.app.Dialog activeTimelineDialog;
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
    private ChannelItem currentPlaybackTransientItem;
    private ChannelItem currentPlaybackU7dItem;
    private String currentPlaybackU7dBaseUrl;
    private long currentPlaybackU7dDurationMs;
    private long currentPlaybackU7dOffsetMs;
    private String lastVodId;
    private final Map<String, Long> recordingResumePositions = new HashMap<>();
    private final Map<String, Long> vodResumePositions = new HashMap<>();
    private boolean refreshingTimelineDialog;
    private View channelOverlay;
    private ComposeView zapBanner;
    private ComposeView quickSearchOverlay;
    private ComposeView recordingsPanel;
    private ComposeView channelListComposeView;

    private PlayerController playerController;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService epgExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService interactiveExecutor = Executors.newCachedThreadPool();
    // Telemetria y comandos remotos no deben bloquear la resolucion de una reproduccion.
    private final ExecutorService controlExecutor = Executors.newSingleThreadExecutor();
    // Executor dedicado a la carga inicial del catalogo para que NO espere en cola
    // detras del arranque del reproductor (que comparte ioExecutor single-thread).
    private final ExecutorService catalogLoadExecutor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private volatile boolean activityDestroyed;
    private final Runnable channelOverlayRenderRunnable = new Runnable() {
        @Override
        public void run() {
            if (!activityDestroyed) {
                renderChannelOverlaySurface();
            }
        }
    };
    private final Runnable playbackQualityUiRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (activityDestroyed) {
                return;
            }
            if (isOverlayVisible()) {
                renderOverlayNowPlayingSurface();
            }
            ChannelItem currentChannel = getCurrentPlaybackChannelItem();
            if (currentChannel != null && zapBanner != null && zapBanner.getVisibility() == View.VISIBLE) {
                updateZapBannerContent(currentChannel);
            }
        }
    };
    private final Runnable vodProgressSaveRunnable = new Runnable() {
        @Override
        public void run() {
            rememberCurrentVodPosition();
            postUiDelayedIfAlive(this, 15_000L);
        }
    };
    private String vodLoadingChannelId = "";
    private String vodLoadingKind = "";
    private String vodLoadingTitle = "";
    private String vodLoadingStep = "";
    private String vodLoadingDetail = "";
    private long vodLoadingStartedAtMs;
    private final Runnable vodLoadingProgressRunnable = new Runnable() {
        @Override
        public void run() {
            updateVodLoadingOverlay();
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
    private final Runnable playbackHeartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            sendPlaybackHeartbeat("heartbeat");
            postUiDelayedIfAlive(this, PLAYBACK_HEARTBEAT_INTERVAL_MS);
        }
    };
    private final Runnable progressiveEpgRunnable = new Runnable() {
        @Override
        public void run() {
            loadNextProgressiveEpgFilter();
        }
    };
    private final List<ChannelItem> channels = new ArrayList<>();
    private final List<ChannelItem> allChannels = new ArrayList<>();
    private final List<ChannelItem> cachedMovistarVodItems = new ArrayList<>();
    private boolean cachedMovistarVodItemsValid = false;
    private final List<ChannelFilter> filters = new ArrayList<>();
    private final Map<String, String> epgNowByChannelId = new HashMap<>();
    private final Map<String, EpgRepository.EpgProgramPair> epgProgramPairByChannelId = new HashMap<>();
    private final Set<String> touchControlsEpgFetchInFlight = new HashSet<>();
    private volatile boolean epgLoadInFlight = false;
    private volatile boolean epgWorkerBusy = false;
    private int epgLoadGeneration;
    private long epgLoadStartedAtMs;
    // Cache de logos dimensionada por memoria real (KB) en vez de por numero fijo de
    // entradas, evitando OOM en Fire Stick de gama baja con bitmaps grandes.
    private final LruCache<String, Drawable> channelLogoCache = new LruCache<String, Drawable>(
            Math.max(4 * 1024, (int) (Runtime.getRuntime().maxMemory() / 1024 / 8))) {
        @Override
        protected int sizeOf(String key, Drawable value) {
            if (value instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) value).getBitmap();
                if (bitmap != null) {
                    return Math.max(1, bitmap.getByteCount() / 1024);
                }
            }
            return 1;
        }
    };
    private CatalogRepository catalogRepository;
    private CatalogSnapshotStore catalogSnapshotStore;
    private boolean u7dProgramsLoading;
    private EpgRepository epgRepository;
    private RecordingsRepository recordingsRepository;
    private ReminderStore reminderStore;
    private RecentChannelsStore recentChannelsStore;
    private FavoriteOrderStore favoriteOrderStore;
    private PlaybackModeStore playbackModeStore;
    private ChannelCollectionStore channelCollectionStore;
    private ChannelProfileStore channelProfileStore;
    private PlaybackDiagnosticsStore playbackDiagnosticsStore;
    private ParentalControlStore parentalControlStore;
    private ChannelActionsCoordinator channelActionsCoordinator;
    private ChannelOverlayCoordinator channelOverlayCoordinator;
    private RemoteInputRouter remoteInputRouter;
    private TouchControlsController touchControlsController;
    private HttpClient httpClient;
    private AppUpdateManager appUpdateManager;
    private AudioManager audioManager;
    private int pendingOverlayListScrollIndex = -1;
    private int overlayListScrollRequestToken;
    private String baseUrl;
    private SharedPreferences prefs;
    private String playbackHeartbeatSessionId;
    private ChannelItem playbackHeartbeatChannel;
    private long playbackHeartbeatStartedAtMs;

    private final OverlayNavigationState overlayNavigationState = new OverlayNavigationState();
    private final OfflineOverlayState overlaySurfaceState = new OfflineOverlayState();
    private final OfflineComposeSurfaceRenderer composeSurfaceRenderer = new OfflineComposeSurfaceRenderer();
    private int overlaySearchFocusRequestToken;
    private int overlaySearchClearFocusRequestToken;
    private boolean startupHubShown;
    private boolean startupFastPlaybackStarted;
    private boolean startupCatalogHydrationRunning;
    private String startupFastPlaybackChannelId = "";
    private String lastChannelId;
    private final List<String> globalSearchRecents = new ArrayList<>();
    private final Set<String> favoriteChannelIds = new HashSet<>();
    private final Map<String, String> temporaryPlaybackModesByChannelId = new HashMap<>();
    private final Map<String, String> learnedPlaybackModesByChannelId = new HashMap<>();
    private final Map<String, Set<String>> playbackRepairAttemptsByChannelId = new HashMap<>();
    private final PlaybackRecoveryCoordinator playbackRecoveryCoordinator = new PlaybackRecoveryCoordinator(temporaryPlaybackModesByChannelId, learnedPlaybackModesByChannelId, playbackRepairAttemptsByChannelId);
    private final Map<String, PlayerController.StreamInfo> streamInfoByChannelId = new HashMap<>();
    private final RecordingsController recordingsController = new RecordingsController();
    private final RecordingsPanelController recordingsPanelController = new RecordingsPanelController(uiHandler, recordingsController, composeSurfaceRenderer, createRecordingsPanelHost());
    private final ZapBannerController zapBannerController = new ZapBannerController(uiHandler, createZapBannerHost());
    private final QuickSearchController quickSearchController = new QuickSearchController(uiHandler, createQuickSearchHost());
    private OfflinePermissions currentOfflinePermissions = new OfflinePermissions();
    private String recordingsChannelFilter = "";
    private String recordingsDayFilter = RECORDINGS_DAY_ALL;
    private boolean touchDeviceMode;
    private boolean playbackRepairEnabled = true;
    private String playbackQualityMode = PlaybackQualityPolicy.AUTO;
    private DevicePerformanceProfile devicePerformanceProfile;
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
    private boolean offlineFirstRunDialogShowing;
    private boolean showOfflineActivationSummaryAfterRefresh;
    private AppUpdateManager.UpdateInfo lastKnownAppUpdateInfo;
    private long lastAppUpdateCheckMs;
    private String lastAppUpdateError = "";
    private long lastOfflineCatalogRefreshAttemptMs;
    private long lastOfflineCatalogRefreshSuccessMs;
    private String lastOfflineCatalogRefreshError = "";
    private long lastOfflinePlaybackRecoveryRefreshMs;
    private long lastOfflineMaintenanceMs;
    private String lastOfflineMaintenanceError = "";
    private long activityCreatedAtMs;
    private long lastResumeAppUpdateCheckMs;
    private long lastResumeOfflineCatalogCheckMs;
    private String epgFullLoadScheduledForChannelId = "";
    private Runnable pendingVisibleEpgLoadRunnable;
    private String pendingVisibleEpgLoadFilterKey = "";
    private long pendingVisibleEpgLoadAtMs;
    private boolean epgFullCatalogLoaded;
    private boolean epgFullCatalogLoadRequested;
    private boolean startupEpgLoadsScheduled;
    private String epgProgressState = "idle";
    private String epgProgressFilterKey = "";
    private String epgProgressLabel = "";
    private String epgProgressLastError = "";
    private int epgProgressLoadedChannels;
    private int epgProgressTotalChannels;
    private int epgProgressLastBatchChannels;
    private int epgProgressLastBatchUpdates;
    private int epgProgressCompletedFilters;
    private long epgProgressStartedAtMs;
    private long epgProgressCompletedAtMs;
    private final Set<String> epgLoadedFilterKeys = new HashSet<>();
    private final Set<String> epgQueuedFilterKeys = new HashSet<>();
    private final Map<String, Integer> epgFilterOffsets = new HashMap<>();
    private int offlineCatalogRetryCount;
    private int globalSearchGeneration;
    private int globalSearchFilter = GLOBAL_SEARCH_FILTER_ALL;
    private Runnable pendingGlobalSearchRunnable;
    private boolean modalTransitionInProgress;
    private Runnable modalReturnAction;
    private boolean playbackHiddenBehindModal;

    static final class TimelineChannelPrograms {
        final ChannelItem channel;
        final List<EpgRepository.EpgProgram> programs;

        TimelineChannelPrograms(ChannelItem channel, List<EpgRepository.EpgProgram> programs) {
            this.channel = channel;
            this.programs = programs;
        }
    }

    private static final class EpgBatchSnapshot {
        final List<ChannelItem> items;
        final int nextOffset;
        final int totalLiveChannels;
        final boolean complete;

        EpgBatchSnapshot(List<ChannelItem> items, int nextOffset, int totalLiveChannels, boolean complete) {
            this.items = items == null ? new ArrayList<>() : items;
            this.nextOffset = nextOffset;
            this.totalLiveChannels = totalLiveChannels;
            this.complete = complete;
        }
    }

    static final class TimelineVisibleBlock {
        final EpgRepository.EpgProgram program;
        final boolean scheduled;
        final boolean live;
        final boolean activeNow;
        final int spacerWidth;
        final int blockWidth;
        final int centerMinute;

        TimelineVisibleBlock(EpgRepository.EpgProgram program, boolean scheduled, boolean live, boolean activeNow, int spacerWidth, int blockWidth, int centerMinute) {
            this.program = program;
            this.scheduled = scheduled;
            this.live = live;
            this.activeNow = activeNow;
            this.spacerWidth = spacerWidth;
            this.blockWidth = blockWidth;
            this.centerMinute = centerMinute;
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

    static final class EpgSearchResult {
        final ChannelItem channel;
        final EpgRepository.EpgProgram program;

        EpgSearchResult(ChannelItem channel, EpgRepository.EpgProgram program) {
            this.channel = channel;
            this.program = program;
        }
    }

    static final class GlobalSearchResult {
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
    private final Runnable reminderTickRunnable = new Runnable() {
        @Override
        public void run() {
            checkReminderNotifications();
            postUiDelayedIfAlive(this, 30000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityCreatedAtMs = System.currentTimeMillis();
        setContentView(R.layout.activity_main);
        touchDeviceMode = detectTouchDeviceMode();
        devicePerformanceProfile = DevicePerformanceProfile.detect(this);

        playerView = findViewById(R.id.playerView);
        ComposeView errorText = findViewById(R.id.errorText);
        ComposeView statusText = findViewById(R.id.statusText);
        ComposeView startupLoadingOverlay = findViewById(R.id.startupLoadingOverlay);
        overlayControlsComposeView = findViewById(R.id.overlayExploreSection);
        touchHomeHub = findViewById(R.id.touchHomeHub);
        touchHomeComposeView = (ComposeView) touchHomeHub;
        multiViewContainer = findViewById(R.id.multiViewContainer);
        multiViewHeaderComposeView = findViewById(R.id.multiViewHeaderComposeView);
        multiPlayerViews[0] = findViewById(R.id.multiPlayerView1);
        multiPlayerViews[1] = findViewById(R.id.multiPlayerView2);
        multiPlayerViews[2] = findViewById(R.id.multiPlayerView3);
        multiPlayerViews[3] = findViewById(R.id.multiPlayerView4);
        multiTiles[0] = findViewById(R.id.multiTile1);
        multiTiles[1] = findViewById(R.id.multiTile2);
        multiTiles[2] = findViewById(R.id.multiTile3);
        multiTiles[3] = findViewById(R.id.multiTile4);
        multiOverlayViews[0] = findViewById(R.id.multiOverlay1);
        multiOverlayViews[1] = findViewById(R.id.multiOverlay2);
        multiOverlayViews[2] = findViewById(R.id.multiOverlay3);
        multiOverlayViews[3] = findViewById(R.id.multiOverlay4);
        overlayNowPlayingComposeView = findViewById(R.id.overlayNowPlayingSection);
        overlayNowPlayingSection = overlayNowPlayingComposeView;
        overlayExploreSection = overlayControlsComposeView;
        overlayListSection = findViewById(R.id.overlayListSection);
        zapBanner = findViewById(R.id.zapBanner);
        zapBannerController.attachBanner(zapBanner);
        quickSearchOverlay = findViewById(R.id.quickSearchOverlay);
        quickSearchController.attachOverlay(quickSearchOverlay);
        ComposeView hdrBadgeText = findViewById(R.id.hdrBadgeText);
        overlayUiController = new OverlayUiController(this, uiHandler, createOverlayUiHost());
        overlayUiController.attachViews(statusText, errorText, hdrBadgeText, startupLoadingOverlay);
        showStartupLoading(
                getString(R.string.startup_loading_local_catalog),
                getString(R.string.startup_loading_local_catalog_detail)
        );
        liveStateBadgeText = findViewById(R.id.liveStateBadgeText);
        touchControlsBar = findViewById(R.id.touchControlsBar);
        timeshiftBarContainer = findViewById(R.id.timeshiftBarContainer);
        touchControlsComposeView = (ComposeView) touchControlsBar;
        timeshiftComposeView = (ComposeView) timeshiftBarContainer;
        playbackGestureLayer = findViewById(R.id.playbackGestureLayer);
        channelOverlay = findViewById(R.id.channelOverlay);
        recordingsPanel = findViewById(R.id.recordingsPanel);
        recordingsPanelController.attachPanel(recordingsPanel);
        channelListComposeView = (ComposeView) overlayListSection;
        if (channelOverlay != null) {
            channelOverlay.setClickable(true);
            channelOverlay.setOnTouchListener((v, event) -> {
                if (touchDeviceMode) {
                    uiHandler.removeCallbacks(hideOverlayRunnable);
                }
                if (event != null && event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
                return false;
            });
        }
        applyResponsiveSurfaceLayout();
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
        recordingsRepository = new RecordingsRepository(baseUrl, catalogSnapshotStore);
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
        parentalControlStore = new ParentalControlStore(prefs, PARENTAL_PREF_PREFIX);
        loadRecordingResumePositions();
        loadVodResumePositions();
        loadGlobalSearchRecents();
        loadLearnedPlaybackModes();
        channelOverlayCoordinator = new ChannelOverlayCoordinator(channels, allChannels, filters, favoriteChannelIds, favoriteOrderStore, channelCollectionStore, channelProfileStore, parentalControlStore);
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

            @Override
            public void showActionMenu(String title, List<String> options, List<Runnable> actions) {
                MainActivity.this.showTvOptionsDialog(title, null, options, actions);
            }
        });
        lastChannelId = prefs.getString(PREF_LAST_CHANNEL_ID, "");
        overlayNavigationState.selectedFilterKey = prefs.getString(PREF_LAST_FILTER_KEY, "all");
        overlayNavigationState.favoritesOnly = prefs.getBoolean(PREF_FAVORITES_ONLY, false);
        playbackRepairEnabled = prefs.getBoolean(PREF_PLAYBACK_REPAIR_ENABLED, true);
        playbackQualityMode = PlaybackQualityPolicy.normalize(prefs.getString(PREF_PLAYBACK_QUALITY_MODE, PlaybackQualityPolicy.AUTO));
        lastVodId = prefs.getString(PREF_LAST_VOD_ID, "");
        overlayNavigationState.favoritesOnly = false;
        remoteInputRouter = new RemoteInputRouter(createRemoteInputHost(), MENU_DOUBLE_PRESS_MS);
        Set<String> storedFavorites = prefs.getStringSet(PREF_FAVORITES, new HashSet<>());
        if (storedFavorites != null) {
            favoriteChannelIds.addAll(storedFavorites);
        }
        reminderStore.load();
        // Reprograma en WorkManager los recordatorios pendientes (sobreviven a cierres
        // de la app y reinicios del dispositivo). Politica KEEP evita duplicados.
        ReminderScheduler.reschedulePending(this, reminderStore);
        recentChannelsStore.load();
        favoriteOrderStore.load();
        playbackModeStore.load();
        channelCollectionStore.load();
        channelProfileStore.load();
        playbackDiagnosticsStore.load();
        favoriteOrderStore.syncToFavorites(favoriteChannelIds);
        touchControlsController = new TouchControlsController(uiHandler, createTouchControlsHost(), TOUCH_CONTROLS_HIDE_MS, TV_TIMESHIFT_HUD_HIDE_MS);
        tabletOrientationLocked = prefs.getBoolean(PREF_TABLET_ORIENTATION_LOCK, false);
        initializeTabletBrightness();
        applyTabletOrientationMode();

        setupPlayer();
        setupChannelList();
        setupRecordingsPanel();
        setupTouchControls();
        enableImmersiveMode();
        tryFastStartupPlaybackFromCache();
        loadChannels();
        detectUnfinishedAppUpdateIfNeeded();
        showPostUpdateNotesIfNeeded();
        scheduleAppUpdateCheckOnStartup();
        scheduleOfflineCatalogAutoRefresh();
        postUiDelayedIfAlive(reminderTickRunnable, 30000L);
        postUiDelayedIfAlive(vodProgressSaveRunnable, 15_000L);
    }

    @Override
    protected void onResume() {
        super.onResume();
        maybeCheckAppUpdateOnResume();
        maybeRefreshOfflineCatalogOnResume();
        if (playbackHeartbeatChannel != null) {
            uiHandler.removeCallbacks(playbackHeartbeatRunnable);
            postUiDelayedIfAlive(playbackHeartbeatRunnable, PLAYBACK_HEARTBEAT_INTERVAL_MS);
        }
    }

    @Override
    protected void onPause() {
        stopPlaybackHeartbeat("stop");
        super.onPause();
    }

    private String resolveBaseUrl() {
        if (BuildConfig.STANDALONE_MODE && BuildConfig.OFFLINE_BASE_URL != null && !BuildConfig.OFFLINE_BASE_URL.trim().isEmpty()) {
            return normalizeBaseUrl(BuildConfig.OFFLINE_BASE_URL);
        }
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

    private static String normalizeBaseUrl(String raw) {
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
                if (startupFastPlaybackStarted
                        && channelId != null
                        && channelId.equals(startupFastPlaybackChannelId)) {
                    return true;
                }
                ChannelItem current = (overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size()) ? channels.get(overlayNavigationState.currentIndex) : null;
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
            public boolean isCompactTouchDeviceMode() {
                return MainActivity.this.useCompactTouchEpgMode();
            }

            @Override
            public String playbackQualityMode() {
                return MainActivity.this.playbackQualityMode;
            }

            @Override
            public void recordPlaybackError(PlayerController.PlaybackRequest request, PlayerController.PlaybackDiagnostics diagnostics) {
                MainActivity.this.recordPlaybackError(request, diagnostics);
            }

            @Override
            public void onPlaybackReady(PlayerController.PlaybackRequest request) {
                MainActivity.this.stopVodLoadingOverlay(request == null ? "" : request.channelId);
                MainActivity.this.hideStartupLoading();
                MainActivity.this.markPostUpdatePlaybackHealthy(request == null ? "" : request.channelId);
                MainActivity.this.sendPlaybackHeartbeat("ready");
            }

            @Override
            public void onFirstVideoFrameRendered(String channelId) {
                MainActivity.this.stopVodLoadingOverlay(channelId);
                MainActivity.this.hideStartupLoading();
                MainActivity.this.scheduleFullEpgLoadAfterFirstFrame(channelId);
                MainActivity.this.markPostUpdatePlaybackHealthy(channelId);
            }

            @Override
            public void onPlaybackQualityChanged(PlayerController.PlaybackDiagnostics diagnostics) {
                MainActivity.this.schedulePlaybackQualityUiRefresh();
            }

            @Override
            public void onPlaybackAutoRecoveryReady(PlayerController.PlaybackRequest request, PlayerController.PlaybackDiagnostics diagnostics, String reason) {
                MainActivity.this.handlePlaybackAutoRecoveryReady(request, diagnostics, reason);
            }
        });
        playerController.initialize();
    }

    private void setupChannelList() {
        refreshOverlayChannelList();
    }

    private void setupRecordingsPanel() {
        refreshRecordingsPanelSurface();
    }

    private void applyResponsiveSurfaceLayout() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        applyBoundedPanelWidth(zapBanner, screenWidth, R.dimen.zap_banner_width, getResources().getDimensionPixelSize(R.dimen.player_edge_margin) * 2);
        applyBoundedPanelWidth(quickSearchOverlay, screenWidth, R.dimen.quick_search_width, getResources().getDimensionPixelSize(R.dimen.player_edge_margin) * 2);
        applyChannelOverlayWidth(screenWidth);
        applyBoundedPanelWidth(recordingsPanel, screenWidth, R.dimen.recordings_panel_width, getResources().getDimensionPixelSize(R.dimen.player_edge_margin));
        applyBoundedPanelWidth(timeshiftBarContainer, screenWidth, R.dimen.touch_surface_panel_max_width, getResources().getDimensionPixelSize(R.dimen.touch_surface_panel_side_margin) * 2);
        applyBoundedPanelWidth(touchHomeHub, screenWidth, R.dimen.touch_home_hub_max_width, getResources().getDimensionPixelSize(R.dimen.touch_home_hub_side_margin) * 2);
        applyOverlayPanelMode();
    }

    private void applyOverlayPanelMode() {
        if (overlayNowPlayingSection != null) {
            overlayNowPlayingSection.setVisibility(View.VISIBLE);
        }
        if (overlayExploreSection != null) {
            overlayExploreSection.setVisibility(View.GONE);
        }
        if (overlayListSection != null) {
            ViewGroup.LayoutParams params = overlayListSection.getLayoutParams();
            if (params instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams linearParams = (LinearLayout.LayoutParams) params;
                linearParams.height = 0;
                linearParams.weight = 1f;
                overlayListSection.setLayoutParams(linearParams);
            }
        }
    }

    private void applyChannelOverlayWidth(int screenWidthPx) {
        if (channelOverlay == null) {
            return;
        }
        if (!touchDeviceMode) {
            applyBoundedPanelWidth(channelOverlay, screenWidthPx, R.dimen.channel_overlay_width, getResources().getDimensionPixelSize(R.dimen.player_edge_margin));
            return;
        }
        int maxWidth = getResources().getDimensionPixelSize(R.dimen.channel_overlay_width);
        int preferredWidth = Math.round(screenWidthPx * (isLargeTouchScreen() ? 0.40f : 0.48f));
        int minWidth = Math.min(dp(320), Math.max(dp(260), screenWidthPx - dp(48)));
        int targetWidth = Math.max(minWidth, Math.min(maxWidth, preferredWidth));
        ViewGroup.LayoutParams params = channelOverlay.getLayoutParams();
        if (params != null && params.width != targetWidth) {
            params.width = targetWidth;
            channelOverlay.setLayoutParams(params);
        }
    }

    private boolean isLargeTouchScreen() {
        return touchDeviceMode && getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    private boolean useCompactTouchEpgMode() {
        return BuildConfig.STANDALONE_MODE && touchDeviceMode && !isLargeTouchScreen();
    }

    private void applyBoundedPanelWidth(View view, int screenWidthPx, int maxWidthDimenRes, int reservedHorizontalPx) {
        if (view == null) {
            return;
        }
        int maxWidth = getResources().getDimensionPixelSize(maxWidthDimenRes);
        int availableWidth = Math.max(dp(220), screenWidthPx - Math.max(0, reservedHorizontalPx));
        int targetWidth = Math.min(maxWidth, availableWidth);
        ViewGroup.LayoutParams rawParams = view.getLayoutParams();
        if (rawParams == null) {
            return;
        }
        if (rawParams.width != targetWidth) {
            rawParams.width = targetWidth;
            view.setLayoutParams(rawParams);
        }
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
                return touchDeviceMode ? touchSurfaceHudVisible : touchControlsBar != null && touchControlsBar.getVisibility() == View.VISIBLE;
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
                return playerController != null && (playerController.getPlaybackSeekState() != null || getCurrentU7dSeekState() != null);
            }

            @Override
            public boolean isTimeshiftSeekInProgress() {
                return timeshiftSeekUserDragging;
            }

            @Override
            public void setTouchControlsVisible(boolean visible) {
                if (touchDeviceMode) {
                    touchSurfaceHudVisible = visible;
                    if (touchControlsBar != null) {
                        touchControlsBar.setVisibility(visible ? View.VISIBLE : View.GONE);
                    }
                } else if (touchControlsBar != null) {
                    touchControlsBar.setVisibility(visible ? View.VISIBLE : View.GONE);
                }
                overlaySurfaceState.setVisible(OfflineOverlayState.Surface.TOUCH_CONTROLS, visible);
                if (visible) {
                    overlaySurfaceState.focusSurface(OfflineOverlayState.Surface.TOUCH_CONTROLS);
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
                overlaySurfaceState.setVisible(OfflineOverlayState.Surface.TIMESHIFT, false);
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
            return;
        }
        touchSurfaceHudVisible = false;
        touchControlsBar.setVisibility(View.GONE);
        updateVodTouchControlsState();
        updateTimeshiftBar();
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
        refreshTouchControlsBar();
        if (playbackGestureLayer != null) {
            playbackGestureLayer.setOnTouchListener((v, event) -> {
                boolean handled = handlePlayerSurfaceTouch(event);
                if (handled && event != null && event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
                return handled;
            });
        }
    }

    private void updateTimeshiftBar() {
        if (timeshiftBarContainer == null || timeshiftComposeView == null || playerController == null) {
            updatePlaybackStateBadge(null);
            return;
        }
        updateVodTouchControlsState();
        boolean showForTouch = touchDeviceMode
                && touchSurfaceHudVisible;
        boolean showForTv = !touchDeviceMode
                && ((touchControlsController != null && touchControlsController.isTvTimeshiftHudVisible())
                || (touchControlsBar != null && touchControlsBar.getVisibility() == View.VISIBLE));
        if ((!showForTouch && !showForTv) || hasBlockingOverlaySurfaceVisible()) {
            timeshiftBarContainer.setVisibility(View.GONE);
            overlaySurfaceState.setVisible(OfflineOverlayState.Surface.TIMESHIFT, false);
            updatePlaybackStateBadge(playerController.getTimeshiftState());
            return;
        }
        PlayerController.PlaybackSeekState state = playerController.getPlaybackSeekState();
        if (state == null) {
            state = getCurrentU7dSeekState();
        }
        if (state == null) {
            timeshiftBarContainer.setVisibility(View.GONE);
            overlaySurfaceState.setVisible(OfflineOverlayState.Surface.TIMESHIFT, false);
            updatePlaybackStateBadge(null);
            return;
        }
        timeshiftBarContainer.setVisibility(View.VISIBLE);
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.TIMESHIFT, true, touchControlsFocusState.timeshiftFocused());
        if (touchControlsFocusState.timeshiftFocused()) {
            overlaySurfaceState.focusSurface(OfflineOverlayState.Surface.TIMESHIFT);
        } else if (touchControlsBar != null && touchControlsBar.getVisibility() == View.VISIBLE) {
            overlaySurfaceState.focusSurface(OfflineOverlayState.Surface.TOUCH_CONTROLS);
        }
        composeSurfaceRenderer.bindTimeshift(timeshiftComposeView, buildTimeshiftBarUiModel(state));
        updatePlaybackStateBadge(playerController.getTimeshiftState());
    }

    private TimeshiftBarUiModel buildTimeshiftBarUiModel(PlayerController.PlaybackSeekState state) {
        TimeshiftBarUiModel model = TimeshiftUiFactory.build(state, new TimeshiftUiFactory.Host() {
            @Override
            public String statusLabel(PlayerController.PlaybackSeekState playbackSeekState) {
                return buildPlaybackSeekLabel(playbackSeekState);
            }

            @Override
            public String previewLabel(PlayerController.PlaybackSeekState playbackSeekState, long previewTargetMs) {
                return formatPlaybackPreviewLabel(playbackSeekState, previewTargetMs);
            }

            @Override
            public void showControls() {
                showTouchControlsTemporarily();
            }

            @Override
            public boolean resumeLive() {
                return playerController != null && playerController.resumeTimeshiftLive();
            }

            @Override
            public void showUnavailable() {
                showStatus(getString(R.string.timeshift_status_unavailable));
            }

            @Override
            public void update() {
                updateTimeshiftBar();
            }

            @Override
            public void markDragging(boolean dragging) {
                timeshiftSeekUserDragging = dragging;
            }

            @Override
            public void seekTo(long targetMs) {
                if (isCurrentU7dPlayback()) {
                    seekCurrentU7dPlaybackTo(targetMs);
                } else if (playerController != null) {
                    playerController.seekTimeshiftTo(targetMs);
                }
            }

            @Override
            public void scheduleAutoHide() {
                scheduleTouchControlsAutoHide();
            }
        });
        if (!touchDeviceMode || model == null || !model.liveVisible) {
            if (model == null || !touchControlsFocusState.timeshiftFocused()) {
                return model;
            }
            return new TimeshiftBarUiModel(
                    model.statusLabel,
                    model.progress,
                    model.liveVisible,
                    model.onLiveClick,
                    model.onSeekStart,
                    model.previewLabelProvider,
                    model.seekCommitHandler,
                    true
            );
        }
        return new TimeshiftBarUiModel(
                model.statusLabel,
                model.progress,
                false,
                null,
                model.onSeekStart,
                model.previewLabelProvider,
                model.seekCommitHandler,
                touchControlsFocusState.timeshiftFocused()
        );
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

    private boolean isCurrentU7dPlayback() {
        return currentPlaybackU7dItem != null
                && currentPlaybackU7dDurationMs > 0L
                && currentPlaybackU7dBaseUrl != null
                && !currentPlaybackU7dBaseUrl.trim().isEmpty();
    }

    private boolean isU7dReplayItem(ChannelItem item) {
        return item != null && "u7d_proxy".equals(safeLower(item.playbackProfile));
    }

    private void clearCurrentU7dPlayback() {
        currentPlaybackU7dItem = null;
        currentPlaybackU7dBaseUrl = "";
        currentPlaybackU7dDurationMs = 0L;
        currentPlaybackU7dOffsetMs = 0L;
    }

    private PlayerController.PlaybackSeekState getCurrentU7dSeekState() {
        if (!isCurrentU7dPlayback()) {
            return null;
        }
        long localPositionMs = playerController == null ? 0L : Math.max(0L, playerController.getCurrentPlaybackPosition());
        long currentMs = Math.max(0L, Math.min(currentPlaybackU7dDurationMs, currentPlaybackU7dOffsetMs + localPositionMs));
        return new PlayerController.PlaybackSeekState(
                0L,
                currentPlaybackU7dDurationMs,
                currentMs,
                formatDurationLabel(currentMs) + " / " + formatDurationLabel(currentPlaybackU7dDurationMs),
                false
        );
    }

    private void seekCurrentU7dPlaybackTo(long targetMs) {
        if (!isCurrentU7dPlayback()) {
            showStatus(getString(R.string.timeshift_status_unavailable));
            return;
        }
        long offsetMs = Math.max(0L, Math.min(currentPlaybackU7dDurationMs, targetMs));
        currentPlaybackU7dOffsetMs = offsetMs;
        ChannelItem source = currentPlaybackU7dItem;
        String seekUrl = buildU7dUrlWithOffset(currentPlaybackU7dBaseUrl, offsetMs);
        ChannelItem seekItem = clonePlaybackItemWithUrl(source, seekUrl);
        currentPlaybackU7dItem = seekItem;
        currentPlaybackTransientItem = seekItem;
        showStatus(formatDurationLabel(offsetMs) + " / " + formatDurationLabel(currentPlaybackU7dDurationMs));
        playChannelItemInternal(seekItem, true, 0L);
    }

    private String buildU7dUrlWithOffset(String baseReplayUrl, long offsetMs) {
        String clean = baseReplayUrl == null ? "" : baseReplayUrl.trim();
        if (clean.isEmpty() || offsetMs <= 0L) {
            return clean;
        }
        return Uri.parse(clean).buildUpon()
                .appendQueryParameter("offset_ms", String.valueOf(offsetMs))
                .build()
                .toString();
    }

    private ChannelItem clonePlaybackItemWithUrl(ChannelItem source, String playUrl) {
        if (source == null) {
            return null;
        }
        return new ChannelItem(
                source.id,
                source.name,
                source.tvgId,
                source.logoUrl,
                source.group,
                playUrl,
                source.fallbackPlayUrl,
                source.originalOrder,
                source.dashboardOrder,
                source.isVod,
                source.isAdultVod,
                source.platformId,
                source.platformName,
                source.customGroups == null ? new ArrayList<>() : new ArrayList<>(source.customGroups),
                source.drmScheme,
                source.drmLicenseUrl,
                source.vodFilterKey,
                source.directPlayback,
                source.vodDescription,
                source.vodYear,
                source.vodDurationSeconds,
                source.playbackProfile
        );
    }

    private void updateVodTouchControlsState() {
        if (touchControlsComposeView != null) {
            refreshTouchControlsBar();
        }
    }

    private void refreshTouchControlsBar() {
        if (touchControlsComposeView == null) {
            return;
        }
        currentTouchControlsBarModel = buildTouchControlsBarUiModel();
        composeSurfaceRenderer.bindTouchControls(touchControlsComposeView, currentTouchControlsBarModel, new TouchControlsArtworkBinder() {
            @Override
            public void bindLogo(ImageView imageView, String logoUrl, String channelName, int widthDp, int heightDp) {
                bindChannelLogo(imageView, logoUrl, channelName, widthDp, heightDp);
            }

            @Override
            public void bindPoster(ImageView imageView, String posterUrl) {
                bindProgramPoster(imageView, posterUrl);
            }
        });
    }

    private TouchControlsBarUiModel buildTouchControlsBarUiModel() {
        TouchControlsNowPlayingUiModel nowPlaying = buildTouchControlsNowPlayingUiModel(getCurrentPlaybackChannelItem());
        return TouchControlsUiFactory.build(new TouchControlsUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String currentFilterLabel() {
                return buildTouchHomeFilterLabel();
            }

            @Override
            public ChannelItem currentChannel() {
                return getCurrentPlaybackChannelItem();
            }

            @Override
            public boolean isOverlayVisible() {
                return MainActivity.this.isOverlayVisible();
            }

            @Override
            public boolean isTabletOrientationLocked() {
                return tabletOrientationLocked;
            }

            @Override
            public boolean supportsOrientationLock() {
                return touchDeviceMode;
            }

            @Override
            public void keepVisible() {
                showTouchControlsTemporarily();
            }

            @Override
            public void hideOverlay() {
                MainActivity.this.hideOverlay();
            }

            @Override
            public void showOverlay() {
                MainActivity.this.showOverlay();
            }

            @Override
            public void showFilterPicker() {
                showFilterPickerDialog();
            }

            @Override
            public void showVodLibrary() {
                showVodLibraryDialog();
            }

            @Override
            public void openTimelineGuide() {
                hideTouchControlsForRemote();
                hideZapBanner();
                openTimelineGuideForCurrentPlayback();
            }

            @Override
            public boolean supportsU7d(ChannelItem item) {
                return isMovistarIsmChannel(item);
            }

            @Override
            public void openU7d(ChannelItem item) {
                openMovistarIsmU7d(item);
            }

            @Override
            public void showVodInfo(ChannelItem item) {
                showVodInfoDialog(item);
            }

            @Override
            public void tunePreviousChannel() {
                MainActivity.this.tunePreviousChannel();
            }

            @Override
            public void openProgramInfo() {
                hideTouchControlsForRemote();
                hideZapBanner();
                openCurrentProgramInfoFromTouch();
            }

            @Override
            public void showPlaybackDiagnostics() {
                showPlaybackDiagnosticsDialog();
            }

            @Override
            public void openRecordings() {
                hideTouchControlsForRemote();
                hideZapBanner();
                openRecordingsBrowser();
            }

            @Override
            public void showToolsMenu() {
                showV12ToolsMenu();
            }

            @Override
            public void toggleTabletOrientationLock() {
                MainActivity.this.toggleTabletOrientationLock();
            }

            @Override
            public boolean seekBack() {
                if (isCurrentU7dPlayback()) {
                    PlayerController.PlaybackSeekState state = getCurrentU7dSeekState();
                    if (state == null) {
                        return false;
                    }
                    seekCurrentU7dPlaybackTo(state.currentMs - 30_000L);
                    return true;
                }
                return playerController != null && playerController.seekTimeshiftBack();
            }

            @Override
            public boolean seekForward() {
                if (isCurrentU7dPlayback()) {
                    PlayerController.PlaybackSeekState state = getCurrentU7dSeekState();
                    if (state == null) {
                        return false;
                    }
                    seekCurrentU7dPlaybackTo(state.currentMs + 30_000L);
                    return true;
                }
                return playerController != null && playerController.seekTimeshiftForward();
            }

            @Override
            public void showSeekUnavailable() {
                showStatus(getString(R.string.status_touch_seek_unavailable));
            }

            @Override
            public void togglePlayback() {
                if (playerController != null) {
                    playerController.togglePlayback();
                }
            }
        }, touchControlsFocusState.actionIndex(), nowPlaying);
    }

    private TouchControlsNowPlayingUiModel buildTouchControlsNowPlayingUiModel(ChannelItem channel) {
        if (channel == null || channel.isVod) {
            return TouchControlsNowPlayingUiModel.EMPTY;
        }
        ensureTouchControlsEpgPair(channel);
        EpgRepository.EpgProgramPair pair = epgProgramPairByChannelId.get(channel.id);
        EpgRepository.EpgProgram currentProgram = pair == null ? null : pair.current;
        EpgRepository.EpgProgram nextProgram = pair == null ? null : pair.next;
        String currentTitle = currentProgram != null && currentProgram.title != null && !currentProgram.title.trim().isEmpty()
                ? currentProgram.title.trim()
                : (channel.nowProgram == null ? "" : channel.nowProgram.trim());
        String nextTitle = nextProgram != null && nextProgram.title != null && !nextProgram.title.trim().isEmpty()
                ? nextProgram.title.trim()
                : (channel.nextProgram == null ? "" : channel.nextProgram.trim());
        int progress = currentProgram == null ? 0 : Math.max(0, Math.min(100, currentProgram.progress));
        boolean progressVisible = currentProgram != null && currentProgram.progress >= 0;
        long endMs = currentProgram == null ? 0L : parseIsoMillis(currentProgram.endTime);
        long nowMs = System.currentTimeMillis();
        String remainingText = progressVisible && endMs > nowMs
                ? getString(R.string.zap_banner_remaining, formatDurationShort(endMs - nowMs))
                : "";
        String endTimeText = currentProgram == null ? "" : shortTime(currentProgram.endTime);
        String nextLabel = nextTitle.isEmpty() ? "" : getString(R.string.zap_banner_next_prefix) + ": " + nextTitle;
        return new TouchControlsNowPlayingUiModel(
                true,
                channel.logoUrl,
                buildZapChannelBadge(channel),
                displayName(channel),
                currentTitle.isEmpty() ? getString(R.string.zap_banner_epg_missing) : currentTitle,
                buildZapProgramMeta(channel, currentProgram),
                nextLabel,
                !nextLabel.isEmpty(),
                ProgramArtworkResolver.resolve(currentProgram, channel),
                remainingText,
                progress,
                progressVisible,
                endTimeText
        );
    }

    private void ensureTouchControlsEpgPair(ChannelItem channel) {
        if (channel == null || channel.isVod || epgRepository == null) {
            return;
        }
        String channelId = channel.id == null ? "" : channel.id.trim();
        EpgRepository.EpgProgramPair existingPair = epgProgramPairByChannelId.get(channelId);
        boolean hasCurrent = hasProgramTitle(existingPair == null ? null : existingPair.current)
                || (channel.nowProgram != null && !channel.nowProgram.trim().isEmpty());
        boolean hasNext = hasProgramTitle(existingPair == null ? null : existingPair.next)
                || (channel.nextProgram != null && !channel.nextProgram.trim().isEmpty());
        boolean hasRichCurrent = hasRichProgramDetails(existingPair == null ? null : existingPair.current);
        if (channelId.isEmpty() || (hasCurrent && hasNext && hasRichCurrent) || touchControlsEpgFetchInFlight.contains(channelId)) {
            return;
        }
        touchControlsEpgFetchInFlight.add(channelId);
        submitEpgTask("touch-hud-epg", () -> {
            EpgRepository.EpgProgram current = null;
            EpgRepository.EpgProgram next = null;
            try {
                EpgRepository.EpgProgramPair pair = epgRepository
                        .fetchProgramPairsForChannels(Collections.singletonList(channel), true, true, false)
                        .get(channelId);
                if (pair != null) {
                    current = pair.current;
                    next = pair.next;
                }
            } catch (Exception e) {
                Log.w(TAG, "Touch HUD EPG hydrate failed for " + channelId + " " + displayName(channel), e);
            }
            EpgRepository.EpgProgram finalCurrent = current;
            EpgRepository.EpgProgram finalNext = next;
            postUiIfAlive(() -> {
                touchControlsEpgFetchInFlight.remove(channelId);
                EpgRepository.EpgProgramPair previousPair = epgProgramPairByChannelId.get(channelId);
                EpgRepository.EpgProgram mergedCurrent = finalCurrent != null ? finalCurrent : (previousPair == null ? null : previousPair.current);
                EpgRepository.EpgProgram mergedNext = finalNext != null ? finalNext : (previousPair == null ? null : previousPair.next);
                if (mergedCurrent != null || mergedNext != null) {
                    epgProgramPairByChannelId.put(channelId, new EpgRepository.EpgProgramPair(mergedCurrent, mergedNext));
                    applySingleProgramPairToChannelLists(channelId, mergedCurrent, mergedNext);
                    Log.w(TAG, "Touch HUD EPG hydrated channel=" + channelId
                            + " current=" + (mergedCurrent == null ? "" : mergedCurrent.title)
                            + " next=" + (mergedNext == null ? "" : mergedNext.title));
                }
                ChannelItem active = getCurrentPlaybackChannelItem();
                boolean stillActive = active != null && channelId.equals(active.id);
                boolean hudVisible = touchControlsBar != null && touchControlsBar.getVisibility() == View.VISIBLE;
                if (stillActive && hudVisible) {
                    refreshTouchControlsBar();
                }
                if (stillActive && isOverlayVisible()) {
                    refreshOverlayChannelList();
                    updateOverlayPanel();
                }
            });
        });
    }

    private boolean hasProgramTitle(EpgRepository.EpgProgram program) {
        return program != null && program.title != null && !program.title.trim().isEmpty();
    }

    private boolean hasRichProgramDetails(EpgRepository.EpgProgram program) {
        if (!hasProgramTitle(program)) {
            return false;
        }
        boolean hasTiming = program.startTime != null && !program.startTime.trim().isEmpty()
                && program.endTime != null && !program.endTime.trim().isEmpty();
        boolean hasArtwork = program.icon != null && !program.icon.trim().isEmpty();
        return hasTiming || hasArtwork;
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
        hideZapBanner();
        if (touchControlsController != null) {
            touchControlsController.showTouchControlsTemporarily();
            ensureTouchControlsEpgPair(getCurrentPlaybackChannelItem());
            if (!touchDeviceMode) {
                resetTouchControlsFocus();
            }
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
        if (touchControlsComposeView == null) {
            return;
        }
        refreshTouchControlsBar();
    }

    private void updatePlaybackStateBadge(PlayerController.TimeshiftState state) {
        if (liveStateBadgeText == null || !touchDeviceMode) {
            return;
        }
        liveStateBadgeText.setVisibility(View.GONE);
    }

    private void loadChannels() {
        showStatus(getString(R.string.status_loading_channels));
        showStartupLoading(
                getString(R.string.startup_loading_validate_catalog),
                getString(R.string.startup_loading_validate_catalog_detail)
        );
        long startMs = System.currentTimeMillis();
        if (maybeShowOfflineFirstRunOnboarding(null)) {
            return;
        }
        submitCatalogTask("load-channels", () -> {
            try {
                CatalogLoadResult result = BuildConfig.STANDALONE_MODE
                        ? catalogRepository.fetchStartupLiveCatalogChannels()
                        : catalogRepository.fetchCatalogChannels();
                long durationMs = System.currentTimeMillis() - startMs;
                logCatalogStartupMetrics("startup-load", result, durationMs);
                postUiIfAlive(() -> {
                    lastCatalogLoadDurationMs = durationMs;
                    applyLoadedChannels(result);
                    maybeHydrateFullStartupCatalog(result);
                    maybeShowStartupCatalogCacheValidated();
                    runPostUpdateStartupHealthCheck("catalog-load", result);
                    refreshStandaloneCatalogInBackgroundIfPossible();
                });
            } catch (Exception catalogErr) {
                if (activityDestroyed || Thread.currentThread().isInterrupted()) {
                    Log.i(TAG, "catalog load cancelled because activity was destroyed");
                    return;
                }
                if (BuildConfig.STANDALONE_MODE) {
                    Log.w(TAG, "local catalog load failed in standalone mode", catalogErr);
                    try {
                        postUiIfAlive(() -> updateStartupLoading(
                                getString(R.string.startup_loading_refresh_catalog),
                                getString(R.string.startup_loading_refresh_catalog_detail)
                        ));
                        lastOfflineCatalogRefreshAttemptMs = System.currentTimeMillis();
                        CatalogLoadResult refreshed = catalogRepository.refreshSnapshotFromConfiguredUrl(BuildConfig.CATALOG_SNAPSHOT_URL);
                        long durationMs = System.currentTimeMillis() - startMs;
                        postUiIfAlive(() -> {
                            lastCatalogLoadDurationMs = durationMs;
                            lastOfflineCatalogRefreshSuccessMs = System.currentTimeMillis();
                            lastOfflineCatalogRefreshError = "";
                            showStatus(getString(R.string.catalog_snapshot_refresh_ready));
                            updateStartupLoading(
                                    getString(R.string.startup_loading_prepare_list),
                                    getString(R.string.startup_loading_prepare_list_detail)
                            );
                            logCatalogStartupMetrics("startup-refresh", refreshed, durationMs);
                            applyLoadedChannels(refreshed);
                            runPostUpdateStartupHealthCheck("catalog-refresh", refreshed);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "standalone catalog load failed", e);
                        try {
                            postUiIfAlive(() -> updateStartupLoading(
                                    getString(R.string.startup_loading_last_good),
                                    getString(R.string.startup_loading_last_good_detail)
                            ));
                            CatalogLoadResult fallback = catalogRepository.fetchLastKnownGoodSnapshotCatalog();
                            long durationMs = System.currentTimeMillis() - startMs;
                            postUiIfAlive(() -> {
                                lastCatalogLoadDurationMs = durationMs;
                                lastOfflineCatalogRefreshError = e.getMessage();
                                showStatus(getString(R.string.offline_catalog_status_using_last_good));
                                updateStartupLoading(
                                        getString(R.string.startup_loading_prepare_list),
                                        getString(R.string.startup_loading_prepare_list_detail)
                                );
                                logCatalogStartupMetrics("startup-last-good", fallback, durationMs);
                                applyLoadedChannels(fallback);
                                runPostUpdateStartupHealthCheck("last-good-catalog", fallback);
                            });
                        } catch (Exception fallbackErr) {
                            postUiIfAlive(() -> {
                                hideStartupLoading();
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
                    postUiIfAlive(() -> {
                        lastCatalogLoadDurationMs = durationMs;
                        updateStartupLoading(
                                getString(R.string.startup_loading_prepare_list),
                                getString(R.string.startup_loading_prepare_list_detail)
                        );
                        logCatalogStartupMetrics("startup-api-fallback", fallback, durationMs);
                        applyLoadedChannels(fallback);
                        runPostUpdateStartupHealthCheck("api-fallback", fallback);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "load channels failed", e);
                    postUiIfAlive(() -> {
                        hideStartupLoading();
                        showError(getString(R.string.error_load_channels, e.getMessage()));
                        showCatalogRecoveryDialog(e.getMessage());
                    });
                }
            }
        });
    }

    private void logCatalogStartupMetrics(String stage, CatalogLoadResult result, long durationMs) {
        int channelCount = result == null || result.channels == null ? 0 : result.channels.size();
        int filterCount = result == null || result.filters == null ? 0 : result.filters.size();
        Log.w(TAG, "startup catalog metrics stage=" + fallbackUnknown(stage)
                + " source=" + (result == null ? "" : result.loadSource)
                + " liveOnly=" + (result != null && result.liveOnly)
                + " channels=" + channelCount
                + " liveItems=" + (result == null ? 0 : result.liveItems)
                + " vodItems=" + (result == null ? 0 : result.vodItems)
                + " filters=" + filterCount
                + " normalizeMs=" + (result == null ? 0L : result.normalizeMs)
                + " permissionsMs=" + (result == null ? 0L : result.permissionsMs)
                + " liveParseMs=" + (result == null ? 0L : result.liveParseMs)
                + " vodParseMs=" + (result == null ? 0L : result.vodParseMs)
                + " filtersMs=" + (result == null ? 0L : result.filtersMs)
                + " parseTotalMs=" + (result == null ? 0L : result.totalParseMs)
                + " durationMs=" + durationMs);
    }

    private void maybeHydrateFullStartupCatalog(CatalogLoadResult startupResult) {
        if (!BuildConfig.STANDALONE_MODE
                || startupResult == null
                || !startupResult.liveOnly
                || startupCatalogHydrationRunning
                || catalogRepository == null
                || ioExecutor == null) {
            return;
        }
        startupCatalogHydrationRunning = true;
        long startMs = System.currentTimeMillis();
        Log.w(TAG, "startup catalog hydration scheduled after live-first load channels="
                + (startupResult.channels == null ? 0 : startupResult.channels.size()));
        ioExecutor.execute(() -> {
            try {
                CatalogLoadResult hydrated = catalogRepository.hydrateFullStartupCatalog();
                long durationMs = System.currentTimeMillis() - startMs;
                postUiIfAlive(() -> {
                    startupCatalogHydrationRunning = false;
                    logCatalogStartupMetrics("startup-hydrate", hydrated, durationMs);
                    applyHydratedStartupCatalog(hydrated);
                    showStatus("Catalogo completo preparado: TV "
                            + (hydrated == null ? 0 : hydrated.liveItems)
                            + " · VOD "
                            + (hydrated == null ? 0 : hydrated.vodItems));
                });
            } catch (Exception e) {
                long durationMs = System.currentTimeMillis() - startMs;
                Log.w(TAG, "startup catalog hydration failed durationMs=" + durationMs, e);
                postUiIfAlive(() -> startupCatalogHydrationRunning = false);
            }
        });
    }

    private void applyHydratedStartupCatalog(CatalogLoadResult result) {
        if (!isActivityReadyForUiWork() || result == null || result.channels == null || result.channels.isEmpty()) {
            return;
        }
        long startMs = System.currentTimeMillis();
        ChannelItem current = getCurrentPlaybackChannelItem();
        String keepChannelId = current == null ? lastChannelId : current.id;
        currentOfflinePermissions = result.offlinePermissions == null ? new OfflinePermissions() : result.offlinePermissions;
        syncOverlayCoordinator();
        invalidateVodDerivedCaches();
        channelOverlayCoordinator.applyLoadedChannels(result, keepChannelId);
        syncOverlayStateFromCoordinator();
        persistNavigationState();
        refreshOverlayChannelList();
        updateFilterText();
        updateOverlaySearchState();
        refreshTouchControlsBar();
        if (current != null && zapBanner != null && zapBanner.getVisibility() == View.VISIBLE) {
            updateZapBannerContent(getCurrentPlaybackChannelItem());
        }
        Log.w(TAG, "startup catalog hydrated applied total=" + allChannels.size()
                + " visible=" + channels.size()
                + " keepChannel=" + fallbackUnknown(keepChannelId)
                + " applyMs=" + (System.currentTimeMillis() - startMs));
    }

    private void tryFastStartupPlaybackFromCache() {
        if (!BuildConfig.STANDALONE_MODE || catalogSnapshotStore == null || playerController == null) {
            return;
        }
        long startMs = System.currentTimeMillis();
        try {
            ChannelItem cached = catalogSnapshotStore.loadStartupPlaybackChannel(BuildConfig.CATALOG_SNAPSHOT_URL);
            if (cached == null) {
                return;
            }
            if (lastChannelId != null && !lastChannelId.trim().isEmpty() && !lastChannelId.equals(cached.id)) {
                return;
            }
            if (isProtectedItem(cached) && isProtectedContentLocked()) {
                Log.w(TAG, "startup fast playback skipped protected channel=" + cached.id);
                return;
            }
            startupFastPlaybackStarted = true;
            startupFastPlaybackChannelId = cached.id;
            updateStartupLoading(
                    getString(R.string.startup_loading_fast_playback),
                    getString(R.string.startup_loading_fast_playback_detail)
            );
            Log.w(TAG, "startup fast playback start channel=" + cached.id
                    + " loadMs=" + (System.currentTimeMillis() - startMs));
            playChannelItemInternal(cached, true, 0L);
        } catch (Exception e) {
            Log.w(TAG, "startup fast playback failed", e);
            startupFastPlaybackStarted = false;
            startupFastPlaybackChannelId = "";
        }
    }

    private void showCatalogRecoveryDialog(String reason) {
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        actions.add(new TvMessageActionUiModel(getString(R.string.startup_recovery_retry), false, this::loadChannels));
        actions.add(new TvMessageActionUiModel(getString(R.string.tools_menu_install_status), false, this::showInstallStatusDialog));
        actions.add(new TvMessageActionUiModel(getString(R.string.dialog_close), false, null));
        showTvMessagePanel(
                getString(R.string.startup_recovery_title),
                getString(R.string.startup_recovery_message, fallbackUnknown(reason), BuildConfig.VERSION_NAME),
                actions,
                null
        );
    }

    private void applyLoadedChannels(CatalogLoadResult result) {
        if (!isActivityReadyForUiWork()) {
            Log.w(TAG, "Ignoring catalog result because activity is no longer active");
            return;
        }
        long startMs = System.currentTimeMillis();
        currentOfflinePermissions = result == null || result.offlinePermissions == null ? new OfflinePermissions() : result.offlinePermissions;
        syncOverlayCoordinator();
        epgFullCatalogLoaded = false;
        epgFullCatalogLoadRequested = false;
        startupEpgLoadsScheduled = false;
        epgFullLoadScheduledForChannelId = "";
        epgLoadedFilterKeys.clear();
        epgQueuedFilterKeys.clear();
        epgFilterOffsets.clear();
        invalidateVodDerivedCaches();
        uiHandler.removeCallbacks(progressiveEpgRunnable);
        long coordinatorStartMs = System.currentTimeMillis();
        channelOverlayCoordinator.applyLoadedChannels(result, lastChannelId);
        long coordinatorMs = System.currentTimeMillis() - coordinatorStartMs;
        long overlayStartMs = System.currentTimeMillis();
        syncOverlayStateFromCoordinator();
        persistNavigationState();
        refreshOverlayChannelList();
        updateFilterText();
        updateOverlaySearchState();
        long overlayMs = System.currentTimeMillis() - overlayStartMs;

        if (channels.isEmpty()) {
            updateStartupLoading(
                    getString(R.string.startup_loading_empty_filter),
                    getString(R.string.startup_loading_empty_filter_detail)
            );
            showError(getString(R.string.error_no_channels_for_filter));
            return;
        }

        int startIndex = resolveStartupPlaybackIndex();
        selectChannelIndex(startIndex);
        scheduleStartupEpgLoads();
        lastApplyChannelsDurationMs = System.currentTimeMillis() - startMs;
        int visibleCount = channels.size();
        int totalCount = allChannels.size();
        showStatus(visibleCount == totalCount
                ? getString(R.string.status_channels_ready, visibleCount, lastCatalogLoadDurationMs)
                : getString(R.string.status_channels_ready_filtered, visibleCount, totalCount, lastCatalogLoadDurationMs));
        Log.w(TAG, "startup catalog applied visible=" + visibleCount
                + " total=" + totalCount
                + " coordinatorMs=" + coordinatorMs
                + " overlayMs=" + overlayMs
                + " totalApplyMs=" + lastApplyChannelsDurationMs);
        if (BuildConfig.STANDALONE_MODE) {
            updateStartupLoading(
                    getString(R.string.startup_loading_open_channel),
                    getString(R.string.startup_loading_open_channel_detail)
            );
            final int deferredStartIndex = startIndex;
            ChannelItem startupChannel = channels.get(Math.max(0, Math.min(deferredStartIndex, channels.size() - 1)));
            if (catalogSnapshotStore != null && startupChannel != null && !isProtectedItem(startupChannel)) {
                catalogSnapshotStore.saveStartupPlaybackChannel(BuildConfig.CATALOG_SNAPSHOT_URL, startupChannel);
            }
            postUiDelayedIfAlive(() -> {
                if (!isActivityReadyForUiWork() || channels.isEmpty()) {
                    return;
                }
                int index = Math.max(0, Math.min(deferredStartIndex, channels.size() - 1));
                ChannelItem channel = channels.get(index);
                if (startupFastPlaybackStarted
                        && channel != null
                        && channel.id != null
                        && channel.id.equals(startupFastPlaybackChannelId)) {
                    Log.w(TAG, "startup full catalog playback skipped because fast channel is already playing id=" + channel.id);
                    return;
                }
                playChannelItem(channel, true);
            }, 120L);
            postUiDelayedIfAlive(this::prefetchCurrentChannelLogos, 2400L);
        } else {
            tuneToIndex(startIndex, true);
            postUiDelayedIfAlive(this::prefetchCurrentChannelLogos, 1800L);
            postUiDelayedIfAlive(() -> loadEpgNow(false), 450L);
        }
        postUiDelayedIfAlive(this::maybeShowStartupHub, 700L);
    }

    private void selectChannelIndex(int index) {
        if (channels.isEmpty()) {
            return;
        }
        if (index < 0) {
            index = channels.size() - 1;
        }
        if (index >= channels.size()) {
            index = 0;
        }
        overlayNavigationState.currentIndex = index;
        overlayNavigationState.selectedOverlayIndex = index;
        pendingOverlayListScrollIndex = index;
        overlayListScrollRequestToken++;
        if (isOverlayVisible()) {
            requestChannelOverlaySurfaceRender();
        }
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

        selectChannelIndex(index);
        playChannelItem(channels.get(index), autoPlay);
    }

    private void playChannelItem(ChannelItem ch, boolean autoPlay) {
        if (ch == null) {
            return;
        }
        if (isProtectedItem(ch) && isProtectedContentLocked()) {
            ensureParentalAccessForItem(ch, () -> playChannelItem(ch, autoPlay));
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
            List<TvMessageActionUiModel> actions = new ArrayList<>();
            actions.add(new TvMessageActionUiModel(getString(R.string.vod_action_continue), false, resumeFromSaved));
            actions.add(new TvMessageActionUiModel(getString(R.string.vod_action_start_over), true, startFromBeginning));
            showTvMessagePanel(displayName(ch), getString(R.string.vod_continue_prompt, formatDurationShort(resumePositionMs)), actions, null);
            return;
        }
        startFromBeginning.run();
    }

    private void playChannelItemInternal(ChannelItem ch, boolean autoPlay, long resumePositionMs) {
        if (!isActivityReadyForUiWork()) {
            Log.w(TAG, "Ignoring playback request because activity is no longer active channel=" + (ch == null ? "" : ch.id));
            return;
        }
        if (playerController == null) {
            Log.w(TAG, "Reinitializing player before playback channel=" + (ch == null ? "" : ch.id));
            setupPlayer();
        }
        if (playerController == null) {
            showError(getString(R.string.error_playback_message, "Player no inicializado"));
            return;
        }
        stopPlaybackHeartbeat("stop");
        saveLastChannelId(ch.id);
        currentPlaybackTransientItem = findChannelIndexById(ch.id) < 0 ? ch : null;
        if (!isU7dReplayItem(ch)) {
            clearCurrentU7dPlayback();
        }
        if (startupFastPlaybackStarted
                && ch.id != null
                && !ch.id.equals(startupFastPlaybackChannelId)) {
            startupFastPlaybackStarted = false;
            startupFastPlaybackChannelId = "";
        }
        if (recentChannelsStore != null) {
            recentChannelsStore.add(ch.id, displayName(ch));
        }
        if (ch.isVod) {
            currentPlaybackVodId = ch.id;
            lastVodId = ch.id;
            if (prefs != null) {
                prefs.edit().putString(PREF_LAST_VOD_ID, ch.id).apply();
            }
            startVodLoadingOverlay(ch);
        } else {
            stopVodLoadingOverlay("");
            hideStartupLoading();
        }
        playerController.resetFallbackState();
        updateTimeshiftBar();
        PlayerController.StreamInfo cachedStreamInfo = streamInfoByChannelId.get(ch.id);
        PlayerController.PlaybackRequest playbackRequest = toPlaybackRequest(ch);
        boolean resolveBeforePlayback = shouldResolveStreamInfoBeforePlayback(ch, playbackRequest);
        if (resolveBeforePlayback) {
            playerController.playChannelAfterResolvingStreamInfo(playbackRequest, autoPlay, streamInfoByChannelId, resumePositionMs);
        } else {
            playerController.playChannel(playbackRequest, autoPlay, cachedStreamInfo, resumePositionMs);
        }
        if (!resolveBeforePlayback && playbackRequest != null && !playbackRequest.directPlayback) {
            playerController.resolveStreamInfoAndReplayIfNeeded(playbackRequest, autoPlay, streamInfoByChannelId, resumePositionMs);
        }
        scheduleLearnCurrentPlaybackRoute(ch.id, playbackRequest == null ? PlaybackModeStore.MODE_AUTO : playbackRequest.playbackMode);

        hideError();
        if (isOverlayVisible()) {
            requestChannelOverlaySurfaceRender();
        }
        showZapBanner(ch);
        startPlaybackHeartbeat(ch);
    }

    private boolean isActivityReadyForUiWork() {
        return !isFinishing() && (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !isDestroyed());
    }

    private boolean shouldResolveStreamInfoBeforePlayback(ChannelItem channel, PlayerController.PlaybackRequest request) {
        return PlaybackStreamInfoPolicy.shouldResolveBeforePlayback(BuildConfig.STANDALONE_MODE, channel, request, displayName(channel));
    }

    private boolean isMovistarIsmChannel(ChannelItem channel) {
        if (channel == null || channel.isVod) {
            return false;
        }
        String platform = safeLower(channel.platformName);
        String group = safeLower(channel.group);
        String name = safeLower(displayName(channel));
        String playUrl = safeLower(channel.playUrl);
        String fallbackUrl = safeLower(channel.fallbackPlayUrl);
        boolean movistar = platform.contains("movistar ism")
                || (platform.contains("movistar") && group.contains("movistar"))
                || playUrl.contains("movistarplus")
                || isMovistarIsmBackendUrl(fallbackUrl);
        boolean smooth = platform.contains("ism")
                || playUrl.contains(".isml/manifest")
                || playUrl.contains(".ism/manifest")
                || isMovistarIsmBackendUrl(fallbackUrl);
        return movistar && smooth && !name.trim().isEmpty();
    }

    private boolean isMovistarIsmBackendUrl(String url) {
        return url != null
                && (url.contains("/hls/ism/")
                || url.contains("/hls/ism-mux/"));
    }

    private String displayName(ChannelItem channelItem) {
        if (channelItem == null) {
            return "";
        }
        return channelProfileStore == null ? channelItem.name : channelProfileStore.getDisplayName(channelItem.id, channelItem.name);
    }

    private String getCurrentChannelName() {
        ChannelItem channel = null;
        if (overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size()) {
            channel = channels.get(overlayNavigationState.currentIndex);
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
        if (catalogSnapshotStore != null && catalogSnapshotStore.getStatus(BuildConfig.OFFLINE_BASE_URL).epgProgramCount <= 0) {
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
        scheduleVisibleEpgLoad(OFFLINE_EPG_INITIAL_DELAY_MS);
    }

    private void loadEpgNow(boolean fullCatalog) {
        if (BuildConfig.STANDALONE_MODE && !fullCatalog) {
            loadEpgForChannels(currentEpgFilterKey(), currentEpgFilterLabel(), buildPriorityVisibleEpgSnapshot(), false, true);
            return;
        }
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

                postUiIfAlive(() -> {
                    epgLoadInFlight = false;
                    lastEpgNowLoadDurationMs = System.currentTimeMillis() - startMs;
                    if (fullCatalog) {
                        epgFullCatalogLoaded = true;
                    }
                    epgFullCatalogLoadRequested = false;
                    if (fullCatalog) {
                        epgNowByChannelId.clear();
                        epgProgramPairByChannelId.clear();
                    }
                    epgNowByChannelId.putAll(updates);
                    mergeEpgProgramPairs(epgProgramPairByChannelId, finalPairs);
                    int filled = fullCatalog
                            ? applyProgramPairUpdates(allChannels, epgNowByChannelId, epgProgramPairByChannelId)
                            : applyProgramPairUpdates(epgChannelsSnapshot, updates, finalPairs);
                    applyProgramPairUpdates(channels, epgNowByChannelId, epgProgramPairByChannelId);
                    Log.i(TAG, "EPG now loaded updates=" + updates.size()
                            + " filledChannels=" + filled
                            + " totalChannels=" + allChannels.size()
                            + " snapshotChannels=" + epgChannelsSnapshot.size()
                            + " visibleSnapshotChannels=" + visibleChannelsSnapshot.size()
                            + " fullCatalog=" + fullCatalog
                            + " standalone=" + BuildConfig.STANDALONE_MODE
                            + " durationMs=" + lastEpgNowLoadDurationMs);
                    refreshOverlayChannelList();
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

    private void scheduleVisibleEpgLoad(long delayMs) {
        if (!BuildConfig.STANDALONE_MODE || channels.isEmpty()) {
            return;
        }
        long safeDelay = Math.max(0L, delayMs);
        String scheduledFilterKey = currentEpgFilterKey();
        long scheduledAtMs = System.currentTimeMillis() + safeDelay;
        if (pendingVisibleEpgLoadRunnable != null) {
            if (scheduledFilterKey.equals(pendingVisibleEpgLoadFilterKey)
                    && pendingVisibleEpgLoadAtMs <= scheduledAtMs) {
                Log.d(TAG, "EPG visible schedule coalesced filter=" + scheduledFilterKey
                        + " existingDelayMs=" + Math.max(0L, pendingVisibleEpgLoadAtMs - System.currentTimeMillis())
                        + " requestedDelayMs=" + safeDelay);
                return;
            }
            uiHandler.removeCallbacks(pendingVisibleEpgLoadRunnable);
        }
        List<ChannelItem> scheduledSnapshot = buildPriorityVisibleEpgSnapshot();
        Log.w(TAG, "EPG visible scheduled delayMs=" + safeDelay
                + " channels=" + scheduledSnapshot.size()
                + " totalVisible=" + channels.size()
                + " filter=" + scheduledFilterKey);
        pendingVisibleEpgLoadFilterKey = scheduledFilterKey;
        pendingVisibleEpgLoadAtMs = scheduledAtMs;
        pendingVisibleEpgLoadRunnable = () -> {
            if (!scheduledFilterKey.equals(pendingVisibleEpgLoadFilterKey)) {
                return;
            }
            pendingVisibleEpgLoadRunnable = null;
            pendingVisibleEpgLoadFilterKey = "";
            pendingVisibleEpgLoadAtMs = 0L;
            List<ChannelItem> snapshot = buildPriorityVisibleEpgSnapshot();
            Log.w(TAG, "EPG visible trigger channels=" + snapshot.size()
                    + " totalVisible=" + channels.size()
                    + " filter=" + currentEpgFilterKey());
            loadEpgForChannels(currentEpgFilterKey(), currentEpgFilterLabel(), snapshot, false, true);
        };
        postUiDelayedIfAlive(pendingVisibleEpgLoadRunnable, safeDelay);
    }

    private List<ChannelItem> buildPriorityVisibleEpgSnapshot() {
        List<ChannelItem> source = resolvePriorityEpgSourceChannels();
        List<ChannelItem> out = new ArrayList<>();
        if (source.isEmpty()) {
            return out;
        }
        if (!useCompactTouchEpgMode()) {
            return new ArrayList<>(source);
        }
        int limit = Math.max(1, OFFLINE_EPG_COMPACT_BATCH_LIMIT);
        if (source.size() <= limit) {
            return new ArrayList<>(source);
        }
        int center = resolveCurrentVisibleChannelIndex(source);
        int before = limit / 3;
        int start = Math.max(0, center - before);
        int end = Math.min(source.size(), start + limit);
        start = Math.max(0, end - limit);
        for (int i = start; i < end; i++) {
            ChannelItem channel = source.get(i);
            if (channel != null && !channel.isVod) {
                out.add(channel);
            }
        }
        return out;
    }

    private List<ChannelItem> resolvePriorityEpgSourceChannels() {
        ChannelItem current = getCurrentPlaybackChannelItem();
        if (current == null || current.id == null || current.id.trim().isEmpty()) {
            return new ArrayList<>(channels);
        }
        for (ChannelItem item : channels) {
            if (item != null && current.id.equals(item.id)) {
                return new ArrayList<>(channels);
            }
        }
        ChannelFilter currentFilter = resolveEpgFilterForChannel(current);
        List<ChannelItem> filtered = channelsForEpgFilter(currentFilter);
        return filtered.isEmpty() ? new ArrayList<>(channels) : filtered;
    }

    private List<ChannelItem> limitEpgSnapshot(List<ChannelItem> source, int limit) {
        return buildEpgBatchSnapshot(source, limit, 0).items;
    }

    private EpgBatchSnapshot buildEpgBatchSnapshot(List<ChannelItem> source, int limit, int offset) {
        List<ChannelItem> out = new ArrayList<>();
        if (source == null || source.isEmpty()) {
            return new EpgBatchSnapshot(out, 0, 0, true);
        }
        int liveTotal = 0;
        for (ChannelItem channel : source) {
            if (channel != null && !channel.isVod) {
                liveTotal++;
            }
        }
        int max = Math.max(1, limit);
        int safeOffset = Math.max(0, Math.min(offset, liveTotal));
        int liveIndex = 0;
        for (ChannelItem channel : source) {
            if (channel == null || channel.isVod) {
                continue;
            }
            if (liveIndex++ < safeOffset) {
                continue;
            }
            out.add(channel);
            if (out.size() >= max) {
                break;
            }
        }
        int nextOffset = Math.min(liveTotal, safeOffset + out.size());
        return new EpgBatchSnapshot(out, nextOffset, liveTotal, nextOffset >= liveTotal);
    }

    private int resolveCurrentVisibleChannelIndex() {
        return resolveCurrentVisibleChannelIndex(channels);
    }

    private int resolveCurrentVisibleChannelIndex(List<ChannelItem> source) {
        List<ChannelItem> safeSource = source == null ? channels : source;
        ChannelItem current = getCurrentPlaybackChannelItem();
        if (current != null && current.id != null) {
            for (int i = 0; i < safeSource.size(); i++) {
                ChannelItem item = safeSource.get(i);
                if (item != null && current.id.equals(item.id)) {
                    return i;
                }
            }
        }
        if (safeSource == channels && overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size()) {
            return overlayNavigationState.currentIndex;
        }
        return 0;
    }

    private void scheduleStartupEpgLoads() {
        if (!BuildConfig.STANDALONE_MODE || startupEpgLoadsScheduled || channels.isEmpty()) {
            return;
        }
        startupEpgLoadsScheduled = true;
        Log.w(TAG, "EPG startup priority scheduled visibleChannels=" + channels.size()
                + " totalChannels=" + allChannels.size()
                + " filter=" + currentEpgFilterKey()
                + " currentIndex=" + overlayNavigationState.currentIndex);
        scheduleCurrentChannelEpgLoad(800L);
        scheduleVisibleEpgLoad(3500L);
    }

    private void scheduleCurrentChannelEpgLoad(long delayMs) {
        if (!BuildConfig.STANDALONE_MODE || channels.isEmpty()) {
            return;
        }
        long safeDelay = Math.max(0L, delayMs);
        Log.w(TAG, "EPG current scheduled delayMs=" + safeDelay + " currentIndex=" + overlayNavigationState.currentIndex);
        postUiDelayedIfAlive(() -> {
            ChannelItem current = getCurrentPlaybackChannelItem();
            if (current == null && overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size()) {
                current = channels.get(overlayNavigationState.currentIndex);
            }
            if (current == null || current.isVod) {
                Log.w(TAG, "EPG current skipped no live channel");
                return;
            }
            Log.w(TAG, "EPG current trigger channel=" + current.id + " name=" + displayName(current));
            loadEpgForChannels("current:" + current.id, displayName(current), java.util.Collections.singletonList(current), false, false);
        }, safeDelay);
    }

    private void scheduleNextProgressiveEpgLoad(long delayMs) {
        if (!BuildConfig.STANDALONE_MODE || allChannels.isEmpty()) {
            return;
        }
        uiHandler.removeCallbacks(progressiveEpgRunnable);
        postUiDelayedIfAlive(progressiveEpgRunnable, Math.max(0L, delayMs));
    }

    private void markEpgProgressStarted(String filterKey, String label, int loadedChannels, int totalChannels, int batchChannels) {
        epgProgressState = "loading";
        epgProgressFilterKey = filterKey == null ? "" : filterKey.trim();
        epgProgressLabel = label == null || label.trim().isEmpty() ? epgProgressFilterKey : label.trim();
        epgProgressLastError = "";
        epgProgressLoadedChannels = Math.max(0, loadedChannels);
        epgProgressTotalChannels = Math.max(epgProgressLoadedChannels, totalChannels);
        epgProgressLastBatchChannels = Math.max(0, batchChannels);
        epgProgressLastBatchUpdates = 0;
        epgProgressStartedAtMs = System.currentTimeMillis();
        showStatus(getString(
                R.string.status_epg_loading_filter,
                epgProgressLabel,
                epgProgressLoadedChannels,
                epgProgressTotalChannels
        ));
    }

    private void markEpgProgressLoaded(String filterKey, String label, int loadedChannels, int totalChannels, int batchChannels, int updates, boolean complete) {
        epgProgressState = complete ? "ready" : "partial";
        epgProgressFilterKey = filterKey == null ? "" : filterKey.trim();
        epgProgressLabel = label == null || label.trim().isEmpty() ? epgProgressFilterKey : label.trim();
        epgProgressLoadedChannels = Math.max(0, loadedChannels);
        epgProgressTotalChannels = Math.max(epgProgressLoadedChannels, totalChannels);
        epgProgressLastBatchChannels = Math.max(0, batchChannels);
        epgProgressLastBatchUpdates = Math.max(0, updates);
        epgProgressCompletedAtMs = System.currentTimeMillis();
        if (complete) {
            epgProgressCompletedFilters = epgLoadedFilterKeys.size();
            showStatus(getString(
                    R.string.status_epg_loaded_filter,
                    epgProgressLabel,
                    epgProgressLoadedChannels,
                    epgProgressTotalChannels,
                    epgProgressLastBatchUpdates
            ));
        } else {
            showStatus(getString(
                    R.string.status_epg_partial_filter,
                    epgProgressLabel,
                    epgProgressLoadedChannels,
                    epgProgressTotalChannels
            ));
        }
    }

    private void markEpgProgressFailed(String filterKey, String label, String error, boolean timeout) {
        epgProgressState = timeout ? "timeout" : "error";
        epgProgressFilterKey = filterKey == null ? "" : filterKey.trim();
        epgProgressLabel = label == null || label.trim().isEmpty() ? epgProgressFilterKey : label.trim();
        epgProgressLastError = error == null ? "" : error.trim();
        epgProgressCompletedAtMs = System.currentTimeMillis();
        showStatus(getString(
                timeout ? R.string.status_epg_timeout_filter : R.string.status_epg_failed_filter,
                epgProgressLabel
        ));
    }

    private void markEpgProgressAllReadyIfNeeded() {
        if (!BuildConfig.STANDALONE_MODE || epgLoadInFlight || !epgQueuedFilterKeys.isEmpty()) {
            return;
        }
        if (nextProgressiveEpgFilter() != null) {
            return;
        }
        epgProgressState = "complete";
        epgProgressCompletedFilters = epgLoadedFilterKeys.size();
        epgProgressCompletedAtMs = System.currentTimeMillis();
        showStatus(getString(R.string.status_epg_all_ready));
    }

    private void loadNextProgressiveEpgFilter() {
        if (!BuildConfig.STANDALONE_MODE || allChannels.isEmpty()) {
            return;
        }
        if (useCompactTouchEpgMode()) {
            return;
        }
        if (epgLoadInFlight || epgWorkerBusy) {
            scheduleNextProgressiveEpgLoad(OFFLINE_EPG_BUSY_RETRY_MS);
            return;
        }
        ChannelFilter nextFilter = nextProgressiveEpgFilter();
        if (nextFilter == null) {
            markEpgProgressAllReadyIfNeeded();
            return;
        }
        String key = epgFilterKey(nextFilter);
        List<ChannelItem> items = channelsForEpgFilter(nextFilter);
        if (items.isEmpty()) {
            epgLoadedFilterKeys.add(key);
            scheduleNextProgressiveEpgLoad(1000L);
            return;
        }
        loadEpgForChannels(key, nextFilter.label, items, false, true);
    }

    private void loadEpgForChannels(String filterKey, String label, List<ChannelItem> snapshot, boolean fullCatalog, boolean continueProgressive) {
        if (epgLoadInFlight || epgWorkerBusy) {
            Log.w(TAG, "EPG partial skipped inFlight filter=" + filterKey
                    + " workerBusy=" + epgWorkerBusy
                    + " continueProgressive=" + continueProgressive);
            if (continueProgressive) {
                scheduleNextProgressiveEpgLoad(OFFLINE_EPG_BUSY_RETRY_MS);
            }
            return;
        }
        if (snapshot == null || snapshot.isEmpty()) {
            Log.w(TAG, "EPG partial skipped empty snapshot filter=" + filterKey);
            if (continueProgressive) {
                scheduleNextProgressiveEpgLoad(OFFLINE_EPG_PROGRESSIVE_DELAY_MS);
            }
            return;
        }
        final String cleanFilterKey = filterKey == null || filterKey.trim().isEmpty() ? "visible" : filterKey.trim();
        final String cleanLabel = label == null || label.trim().isEmpty() ? cleanFilterKey : label.trim();
        if (continueProgressive) {
            pruneEpgProgressToActiveFilter(cleanFilterKey);
        }
        final boolean compactEpgMode = useCompactTouchEpgMode();
        final int batchLimit = compactEpgMode ? OFFLINE_EPG_COMPACT_BATCH_LIMIT : OFFLINE_EPG_VISIBLE_BATCH_LIMIT;
        final List<ChannelItem> sourceSnapshot = new ArrayList<>(snapshot);
        final int batchOffset = continueProgressive ? Math.max(0, epgFilterOffsets.getOrDefault(cleanFilterKey, 0)) : 0;
        final EpgBatchSnapshot batchSnapshot = buildEpgBatchSnapshot(sourceSnapshot, batchLimit, batchOffset);
        final List<ChannelItem> epgChannelsSnapshot = batchSnapshot.items;
        if (epgChannelsSnapshot.isEmpty()) {
            Log.w(TAG, "EPG partial skipped empty batch filter=" + cleanFilterKey
                    + " offset=" + batchOffset
                    + " totalLive=" + batchSnapshot.totalLiveChannels);
            epgQueuedFilterKeys.remove(cleanFilterKey);
            epgFilterOffsets.remove(cleanFilterKey);
            epgLoadedFilterKeys.add(cleanFilterKey);
            if (continueProgressive && !compactEpgMode) {
                scheduleNextProgressiveEpgLoad(OFFLINE_EPG_PROGRESSIVE_DELAY_MS);
            }
            return;
        }
        epgQueuedFilterKeys.add(cleanFilterKey);
        epgLoadInFlight = true;
        epgWorkerBusy = true;
        epgLoadStartedAtMs = System.currentTimeMillis();
        final int loadGeneration = ++epgLoadGeneration;
        long startMs = System.currentTimeMillis();
        markEpgProgressStarted(
                cleanFilterKey,
                cleanLabel,
                batchOffset,
                batchSnapshot.totalLiveChannels,
                epgChannelsSnapshot.size()
        );
        Log.w(TAG, "EPG partial start filter=" + cleanFilterKey
                + " label=" + cleanLabel
                + " channels=" + epgChannelsSnapshot.size()
                + " offset=" + batchOffset
                + " nextOffset=" + batchSnapshot.nextOffset
                + " totalLive=" + batchSnapshot.totalLiveChannels);
        postUiDelayedIfAlive(() -> {
            if (!epgLoadInFlight || loadGeneration != epgLoadGeneration) {
                return;
            }
            long elapsedMs = System.currentTimeMillis() - epgLoadStartedAtMs;
            if (elapsedMs < OFFLINE_EPG_LOAD_TIMEOUT_MS) {
                return;
            }
            epgLoadInFlight = false;
            epgLoadGeneration++;
            epgQueuedFilterKeys.remove(cleanFilterKey);
            markEpgProgressFailed(cleanFilterKey, cleanLabel, "timeout", true);
            Log.w(TAG, "EPG partial timeout filter=" + cleanFilterKey
                    + " label=" + cleanLabel
                    + " channels=" + epgChannelsSnapshot.size()
                    + " elapsedMs=" + elapsedMs
                    + " workerStillBusy=" + epgWorkerBusy);
        }, OFFLINE_EPG_LOAD_TIMEOUT_MS + 500L);
        if (!submitEpgTask("load-epg-partial", () -> {
            try {
                Map<String, EpgRepository.EpgProgramPair> pairs = BuildConfig.STANDALONE_MODE && !compactEpgMode
                        ? epgRepository.fetchProgramPairsForChannels(epgChannelsSnapshot, false, false, false)
                        : epgRepository.fetchProgramPairsForChannels(epgChannelsSnapshot, true, compactEpgMode, false);
                Map<String, String> updates = buildNowProgramUpdatesFromPairs(pairs);
                postUiIfAlive(() -> {
                    epgWorkerBusy = false;
                    if (loadGeneration != epgLoadGeneration) {
                        Log.w(TAG, "EPG partial stale result ignored filter=" + cleanFilterKey
                                + " label=" + cleanLabel
                                + " updates=" + updates.size()
                                + " durationMs=" + (System.currentTimeMillis() - startMs));
                        return;
                    }
                    epgLoadInFlight = false;
                    lastEpgNowLoadDurationMs = System.currentTimeMillis() - startMs;
                    epgFullCatalogLoadRequested = false;
                    if (fullCatalog) {
                        epgFullCatalogLoaded = true;
                        epgNowByChannelId.clear();
                        epgProgramPairByChannelId.clear();
                    }
                    epgNowByChannelId.putAll(updates);
                    mergeEpgProgramPairs(epgProgramPairByChannelId, pairs);
                    epgQueuedFilterKeys.remove(cleanFilterKey);
                    if (batchSnapshot.complete || !continueProgressive) {
                        epgFilterOffsets.remove(cleanFilterKey);
                        epgLoadedFilterKeys.add(cleanFilterKey);
                    } else {
                        epgFilterOffsets.put(cleanFilterKey, batchSnapshot.nextOffset);
                    }
                    int filled = applyProgramPairUpdates(epgChannelsSnapshot, updates, pairs);
                    applyProgramPairUpdates(channels, epgNowByChannelId, epgProgramPairByChannelId);
                    boolean filterComplete = batchSnapshot.complete || !continueProgressive;
                    markEpgProgressLoaded(
                            cleanFilterKey,
                            cleanLabel,
                            filterComplete ? batchSnapshot.totalLiveChannels : batchSnapshot.nextOffset,
                            batchSnapshot.totalLiveChannels,
                            epgChannelsSnapshot.size(),
                            updates.size(),
                            filterComplete
                    );
                    Log.w(TAG, "EPG partial loaded filter=" + cleanFilterKey
                            + " label=" + cleanLabel
                            + " updates=" + updates.size()
                            + " filledChannels=" + filled
                            + " snapshotChannels=" + epgChannelsSnapshot.size()
                            + " nextOffset=" + batchSnapshot.nextOffset
                            + " totalLive=" + batchSnapshot.totalLiveChannels
                            + " complete=" + batchSnapshot.complete
                            + " durationMs=" + lastEpgNowLoadDurationMs);
                    refreshOverlayChannelList();
                    updateOverlayPanel();
                    ChannelItem currentChannel = getCurrentPlaybackChannelItem();
                    if (currentChannel != null && zapBanner != null && zapBanner.getVisibility() == View.VISIBLE) {
                        showZapBanner(currentChannel);
                    }
                    if (continueProgressive && !compactEpgMode) {
                        scheduleNextProgressiveEpgLoad(batchSnapshot.complete
                                ? OFFLINE_EPG_PROGRESSIVE_DELAY_MS
                                : OFFLINE_EPG_BUSY_RETRY_MS);
                    } else {
                        markEpgProgressAllReadyIfNeeded();
                    }
                });
            } catch (Exception e) {
                epgLoadInFlight = false;
                epgWorkerBusy = false;
                epgFullCatalogLoadRequested = false;
                lastEpgNowLoadDurationMs = System.currentTimeMillis() - startMs;
                epgQueuedFilterKeys.remove(cleanFilterKey);
                postUiIfAlive(() -> markEpgProgressFailed(cleanFilterKey, cleanLabel, e.getMessage(), false));
                Log.w(TAG, "load epg partial failed filter=" + cleanFilterKey, e);
                if (continueProgressive && !compactEpgMode) {
                    scheduleNextProgressiveEpgLoad(OFFLINE_EPG_PROGRESSIVE_DELAY_MS);
                }
            }
        })) {
            epgLoadInFlight = false;
            epgWorkerBusy = false;
            epgFullCatalogLoadRequested = false;
            epgQueuedFilterKeys.remove(cleanFilterKey);
        }
    }

    private void pruneEpgProgressToActiveFilter(String activeFilterKey) {
        String cleanActive = activeFilterKey == null ? "" : activeFilterKey.trim();
        if (cleanActive.isEmpty()) {
            return;
        }
        epgQueuedFilterKeys.removeIf(key -> key == null || !cleanActive.equals(key));
        epgFilterOffsets.keySet().removeIf(key -> key == null || !cleanActive.equals(key));
    }

    private ChannelFilter nextProgressiveEpgFilter() {
        ChannelFilter currentFilter = resolveCurrentPlaybackEpgFilter();
        if (currentFilter == null || !isProgressiveEpgFilter(currentFilter)) {
            return null;
        }
        String key = epgFilterKey(currentFilter);
        return !epgLoadedFilterKeys.contains(key) && !epgQueuedFilterKeys.contains(key)
                ? currentFilter
                : null;
    }

    private boolean isProgressiveEpgFilter(ChannelFilter filter) {
        if (filter == null || filter.key == null || filter.key.trim().isEmpty()) {
            return false;
        }
        if (filter.type != FILTER_PLATFORM && filter.type != FILTER_CUSTOM_GROUP) {
            return false;
        }
        return !isProtectedFilter(filter) || !isProtectedContentLocked();
    }

    private String currentEpgFilterKey() {
        ChannelFilter filter = resolveCurrentPlaybackEpgFilter();
        return filter == null ? "visible" : epgFilterKey(filter);
    }

    private String currentEpgFilterLabel() {
        ChannelFilter filter = resolveCurrentPlaybackEpgFilter();
        return filter == null || filter.label == null || filter.label.trim().isEmpty()
                ? "visible"
                : stripOverlayFilterPrefix(filter.label);
    }

    private ChannelFilter resolveCurrentPlaybackEpgFilter() {
        ChannelItem current = getCurrentPlaybackChannelItem();
        ChannelFilter selected = selectedOverlayFilter();
        if (current == null) {
            return selected;
        }
        if (filterContainsChannel(selected, current)) {
            return selected;
        }
        ChannelFilter currentFilter = resolveEpgFilterForChannel(current);
        return currentFilter == null ? selected : currentFilter;
    }

    private ChannelFilter resolveEpgFilterForChannel(ChannelItem channel) {
        if (channel == null) {
            return null;
        }
        for (ChannelFilter filter : filters) {
            if (filterContainsChannel(filter, channel)) {
                return filter;
            }
        }
        return null;
    }

    private boolean filterContainsChannel(ChannelFilter filter, ChannelItem channel) {
        if (filter == null || channel == null) {
            return false;
        }
        if (filter.type == FILTER_PLATFORM) {
            return channel.platformId == filter.platformId;
        }
        if (filter.type == FILTER_CUSTOM_GROUP && channel.customGroups != null) {
            for (String groupName : channel.customGroups) {
                if (groupName != null && groupName.equalsIgnoreCase(filter.groupName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String epgFilterKey(ChannelFilter filter) {
        return filter == null || filter.key == null || filter.key.trim().isEmpty()
                ? "visible"
                : filter.key.trim();
    }

    private List<ChannelItem> channelsForEpgFilter(ChannelFilter filter) {
        List<ChannelItem> out = new ArrayList<>();
        if (filter == null) {
            return out;
        }
        for (ChannelItem item : allChannels) {
            if (item == null || item.isVod || shouldHideProtectedItem(item)) {
                continue;
            }
            if (filter.type == FILTER_PLATFORM && item.platformId == filter.platformId) {
                out.add(item);
            } else if (filter.type == FILTER_CUSTOM_GROUP && item.customGroups != null) {
                for (String groupName : item.customGroups) {
                    if (groupName != null && groupName.equalsIgnoreCase(filter.groupName)) {
                        out.add(item);
                        break;
                    }
                }
            }
        }
        return out;
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
            String previousNow = item.nowProgram == null ? "" : item.nowProgram.trim();
            String previousNext = item.nextProgram == null ? "" : item.nextProgram.trim();
            String updatedNow = updates == null ? "" : updates.getOrDefault(item.id, "");
            EpgRepository.EpgProgramPair pair = pairs == null ? null : pairs.get(item.id);
            EpgRepository.EpgProgram next = pair == null ? null : pair.next;
            String updatedNext = next == null || next.title == null ? "" : next.title.trim();
            item.nowProgram = updatedNow == null || updatedNow.trim().isEmpty() ? previousNow : updatedNow.trim();
            item.nextProgram = updatedNext.isEmpty() ? previousNext : updatedNext;
            if (item.nowProgram != null && !item.nowProgram.trim().isEmpty()) {
                filled++;
            }
        }
        return filled;
    }

    private void mergeEpgProgramPairs(Map<String, EpgRepository.EpgProgramPair> target, Map<String, EpgRepository.EpgProgramPair> incoming) {
        if (target == null || incoming == null || incoming.isEmpty()) {
            return;
        }
        for (Map.Entry<String, EpgRepository.EpgProgramPair> entry : incoming.entrySet()) {
            String channelId = entry.getKey() == null ? "" : entry.getKey().trim();
            EpgRepository.EpgProgramPair nextPair = entry.getValue();
            if (channelId.isEmpty() || nextPair == null) {
                continue;
            }
            EpgRepository.EpgProgramPair previous = target.get(channelId);
            if (previous == null) {
                target.put(channelId, nextPair);
                continue;
            }
            target.put(channelId, new EpgRepository.EpgProgramPair(
                    chooseBetterProgram(previous.current, nextPair.current),
                    chooseBetterProgram(previous.next, nextPair.next)
            ));
        }
    }

    private EpgRepository.EpgProgram chooseBetterProgram(EpgRepository.EpgProgram previous, EpgRepository.EpgProgram incoming) {
        if (incoming == null) {
            return previous;
        }
        if (previous == null) {
            return incoming;
        }
        boolean incomingRich = hasRichProgramDetails(incoming);
        boolean previousRich = hasRichProgramDetails(previous);
        long previousEndMs = parseIsoMillis(previous.endTime);
        if (previousEndMs > 0L && previousEndMs <= System.currentTimeMillis() && hasProgramTitle(incoming)) {
            return incoming;
        }
        if (incomingRich && !previousRich) {
            return incoming;
        }
        if (!incomingRich && previousRich) {
            return previous;
        }
        if (hasProgramTitle(incoming) && !hasProgramTitle(previous)) {
            return incoming;
        }
        return incomingRich == previousRich ? incoming : previous;
    }

    private void applySingleProgramPairToChannelLists(String channelId, EpgRepository.EpgProgram current, EpgRepository.EpgProgram next) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return;
        }
        applySingleProgramPairToChannelList(channels, channelId.trim(), current, next);
        applySingleProgramPairToChannelList(allChannels, channelId.trim(), current, next);
    }

    private void applySingleProgramPairToChannelList(List<ChannelItem> items, String channelId, EpgRepository.EpgProgram current, EpgRepository.EpgProgram next) {
        if (items == null || items.isEmpty()) {
            return;
        }
        String currentTitle = hasProgramTitle(current) ? current.title.trim() : "";
        String nextTitle = hasProgramTitle(next) ? next.title.trim() : "";
        for (ChannelItem item : items) {
            if (item == null || item.id == null || !channelId.equals(item.id.trim())) {
                continue;
            }
            if (!currentTitle.isEmpty()) {
                item.nowProgram = currentTitle;
            }
            if (!nextTitle.isEmpty()) {
                item.nextProgram = nextTitle;
            }
        }
    }

    private void showChannelActionMenu() {
        if (channels.isEmpty() || overlayNavigationState.selectedOverlayIndex < 0 || overlayNavigationState.selectedOverlayIndex >= channels.size()) {
            return;
        }
        ChannelItem ch = channels.get(overlayNavigationState.selectedOverlayIndex);
        boolean fav = favoriteChannelIds.contains(ch.id);
        channelActionsCoordinator.showChannelActionMenu(ch, fav);
    }

    private void openMiniGuideForChannel(ChannelItem ch) {
        if (ch == null) {
            return;
        }
        showStatus(getString(R.string.status_loading_guide));
        interactiveExecutor.execute(() -> {
            try {
                List<EpgRepository.EpgProgram> items = epgRepository.fetchChannelPrograms(ch, 8);
                postUiIfAlive(() -> {
                    if (items.isEmpty()) {
                        showStatus(getString(R.string.status_no_epg_for_channel));
                        return;
                    }
                    showMiniGuideDialog(ch, items);
                });
            } catch (Exception e) {
                Log.w(TAG, "mini guide failed", e);
                postUiIfAlive(() -> showStatus(getString(R.string.status_failed_load_guide)));
            }
        });
    }

    private void openTimelineGuideAroundSelection() {
        if (channels.isEmpty()) {
            return;
        }
        int anchorIndex = findChannelIndexById(lastTimelineAnchorChannelId);
        if (anchorIndex < 0) {
            anchorIndex = overlayNavigationState.selectedOverlayIndex >= 0 && overlayNavigationState.selectedOverlayIndex < channels.size()
                    ? overlayNavigationState.selectedOverlayIndex
                    : (overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size() ? overlayNavigationState.currentIndex : 0);
        }
        long windowStartMs = lastTimelineWindowStartMs > 0L ? lastTimelineWindowStartMs : System.currentTimeMillis();
        openTimelineGuide(anchorIndex, windowStartMs);
    }

    private void openTimelineGuideNow() {
        if (channels.isEmpty()) {
            return;
        }
        int anchorIndex = overlayNavigationState.selectedOverlayIndex >= 0 && overlayNavigationState.selectedOverlayIndex < channels.size()
                ? overlayNavigationState.selectedOverlayIndex
                : (overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size() ? overlayNavigationState.currentIndex : 0);
        lastTimelineFocusedCenterMinute = -1;
        openTimelineGuide(anchorIndex, System.currentTimeMillis());
    }

    private void openTimelineGuideForChannel(ChannelItem channel) {
        int anchorIndex = channel == null ? -1 : findChannelIndexById(channel.id);
        if (anchorIndex < 0) {
            openTimelineGuideForExplicitChannel(channel, System.currentTimeMillis());
            return;
        }
        openTimelineGuide(anchorIndex, System.currentTimeMillis());
    }

    private void openTimelineGuideForExplicitChannel(ChannelItem anchorChannel, long windowStartMs) {
        if (anchorChannel == null) {
            showOverlay();
            return;
        }
        final long selectedWindowStartMs = windowStartMs;
        showStatus(getString(R.string.status_loading_guide));
        Log.w(TAG, "timeline guide explicit start channel=" + anchorChannel.id + " name=" + displayName(anchorChannel));
        List<ChannelItem> timelineChannels = selectTimelineChannelsAroundChannel(anchorChannel);
        loadTimelineGuideRowsAsync(timelineChannels, anchorChannel.id, selectedWindowStartMs, "timeline explicit", true);
    }

    private void openTimelineGuideNextForAnchor() {
        if (channels.isEmpty()) {
            return;
        }
        String anchorChannelId = activeTimelineAnchorChannelId != null ? activeTimelineAnchorChannelId : lastTimelineAnchorChannelId;
        int anchorIndex = findChannelIndexById(anchorChannelId);
        if (anchorIndex < 0) {
            anchorIndex = overlayNavigationState.selectedOverlayIndex >= 0 && overlayNavigationState.selectedOverlayIndex < channels.size()
                    ? overlayNavigationState.selectedOverlayIndex
                    : (overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size() ? overlayNavigationState.currentIndex : 0);
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
        Log.w(TAG, "timeline guide start selectedIndex=" + selectedIndex
                + " channel=" + (channels.get(selectedIndex) == null ? "" : channels.get(selectedIndex).id)
                + " name=" + (channels.get(selectedIndex) == null ? "" : displayName(channels.get(selectedIndex))));
        List<ChannelItem> timelineChannels = selectTimelineChannels(selectedIndex);
        String selectedChannelId = channels.get(selectedIndex).id;
        loadTimelineGuideRowsAsync(timelineChannels, selectedChannelId, selectedWindowStartMs, "timeline", true);
    }

    private void loadTimelineGuideRowsAsync(List<ChannelItem> timelineChannels, String selectedChannelId, long selectedWindowStartMs, String source, boolean showWhenReady) {
        final List<ChannelItem> channelsSnapshot = timelineChannels == null ? new ArrayList<>() : new ArrayList<>(timelineChannels);
        final String anchorId = selectedChannelId == null ? "" : selectedChannelId;
        Log.w(TAG, source + " enriched queued selectedChannel=" + anchorId
                + " channels=" + channelsSnapshot.size()
                + " showWhenReady=" + showWhenReady
                + " activeDialog=" + (activeTimelineDialog != null && activeTimelineDialog.isShowing()));
        interactiveExecutor.execute(() -> {
            try {
                List<RecordingsRepository.RecordingItem> scheduledItems = fetchScheduledRecordingsSafely(source);
                loadTimelineGuideRowsFromSnapshot(channelsSnapshot, anchorId, selectedWindowStartMs, source, showWhenReady, scheduledItems);
            } catch (Exception e) {
                Log.w(TAG, source + " enriched failed", e);
                if (showWhenReady) {
                    postUiIfAlive(() -> showStatus(getString(R.string.status_failed_load_guide)));
                }
            }
        });
    }

    private void loadTimelineGuideRowsFromSnapshot(
            List<ChannelItem> channelsSnapshot,
            String anchorId,
            long selectedWindowStartMs,
            String source,
            boolean showWhenReady,
            List<RecordingsRepository.RecordingItem> scheduledItems
    ) {
        try {
            Log.w(TAG, source + " full guide scan start selectedChannel=" + anchorId
                    + " channels=" + channelsSnapshot.size());
            java.util.Map<String, List<EpgRepository.EpgProgram>> programsByChannel =
                    epgRepository.fetchChannelProgramsForChannelsDirect(channelsSnapshot, 18);
            List<TimelineChannelPrograms> rows = buildTimelineRowsFromPrograms(channelsSnapshot, programsByChannel, anchorId);
            final int selectedRowIndex = findTimelineRowIndex(rows, anchorId);
            postUiIfAlive(() -> {
                Log.w(TAG, source + " full guide scan ready selectedChannel=" + anchorId
                        + " rows=" + rows.size()
                        + " selectedRow=" + selectedRowIndex
                        + " selectedPrograms=" + (rows.isEmpty() || rows.get(selectedRowIndex).programs == null ? 0 : rows.get(selectedRowIndex).programs.size()));
                if (rows.isEmpty()) {
                    return;
                }
                boolean canRefreshActiveDialog = activeTimelineDialog != null
                        && activeTimelineDialog.isShowing()
                        && activeTimelineWindowStartMs == selectedWindowStartMs;
                if (!showWhenReady && !canRefreshActiveDialog) {
                    return;
                }
                ChannelItem selectedChannel = rows.get(Math.max(0, Math.min(selectedRowIndex, rows.size() - 1))).channel;
                if (canRefreshActiveDialog) {
                    refreshingTimelineDialog = true;
                    activeTimelineDialog.dismiss();
                    refreshingTimelineDialog = false;
                }
                showTimelineGuideDialog(rows, selectedWindowStartMs, selectedChannel == null ? anchorId : selectedChannel.id, scheduledItems);
            });
        } catch (Exception e) {
            Log.w(TAG, source + " full guide scan failed", e);
        }
    }

    private List<TimelineChannelPrograms> buildTimelineRowsFromPrograms(
            List<ChannelItem> channelsSnapshot,
            java.util.Map<String, List<EpgRepository.EpgProgram>> programsByChannel,
            String anchorId
    ) {
        List<TimelineChannelPrograms> rows = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (ChannelItem channel : channelsSnapshot) {
            List<EpgRepository.EpgProgram> programs = channel == null || channel.id == null
                    ? new ArrayList<>()
                    : programsByChannel.get(channel.id.trim());
            if (programs == null || programs.isEmpty()) {
                programs = channel == null ? new ArrayList<>() : buildInlineTimelinePrograms(channel, now);
            }
            rows.add(new TimelineChannelPrograms(channel, programs));
        }
        return rows;
    }

    private int findTimelineRowIndex(List<TimelineChannelPrograms> rows, String anchorId) {
        if (rows == null || rows.isEmpty() || anchorId == null || anchorId.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < rows.size(); i++) {
            TimelineChannelPrograms row = rows.get(i);
            if (row != null && row.channel != null && anchorId.equals(row.channel.id)) {
                return i;
            }
        }
        return 0;
    }

    private List<TimelineChannelPrograms> buildFastTimelineRows(List<ChannelItem> timelineChannels) {
        List<TimelineChannelPrograms> rows = new ArrayList<>();
        if (timelineChannels == null || timelineChannels.isEmpty()) {
            return rows;
        }
        long now = System.currentTimeMillis();
        for (ChannelItem channel : timelineChannels) {
            if (channel != null && !channel.isVod) {
                List<EpgRepository.EpgProgram> programs = buildCachedTimelinePrograms(channel, now);
                if (programs.isEmpty()) {
                    programs = buildInlineTimelinePrograms(channel, now);
                }
                rows.add(new TimelineChannelPrograms(channel, programs));
            }
        }
        return rows;
    }

    private List<EpgRepository.EpgProgram> buildCachedTimelinePrograms(ChannelItem channel, long now) {
        List<EpgRepository.EpgProgram> programs = new ArrayList<>();
        if (channel == null || channel.id == null) {
            return programs;
        }
        EpgRepository.EpgProgramPair pair = epgProgramPairByChannelId.get(channel.id);
        if (pair == null) {
            return programs;
        }
        addCachedTimelineProgram(programs, channel, pair.current, now, false);
        addCachedTimelineProgram(programs, channel, pair.next, now, true);
        return programs;
    }

    private void addCachedTimelineProgram(List<EpgRepository.EpgProgram> programs, ChannelItem channel, EpgRepository.EpgProgram program, long now, boolean next) {
        if (program == null || program.title == null || program.title.trim().isEmpty()) {
            return;
        }
        long startMs = parseIsoMillis(program.startTime);
        long endMs = parseIsoMillis(program.endTime);
        if (startMs <= 0L || endMs <= startMs) {
            long roundedNow = Math.max(0L, (now / 60000L) * 60000L);
            startMs = next ? roundedNow + 30L * 60L * 1000L : Math.max(0L, roundedNow - 30L * 60L * 1000L);
            endMs = startMs + 60L * 60L * 1000L;
        }
        programs.add(new EpgRepository.EpgProgram(
                channel.id,
                channel.name,
                channel.tvgId,
                program.title,
                ProgramArtworkResolver.resolve(program, channel),
                program.description,
                java.time.Instant.ofEpochMilli(startMs).toString(),
                java.time.Instant.ofEpochMilli(endMs).toString(),
                program.category,
                next ? -1 : Math.max(program.progress, 0)
        ));
    }

    private List<EpgRepository.EpgProgram> buildInlineTimelinePrograms(ChannelItem channel, long now) {
        List<EpgRepository.EpgProgram> programs = new ArrayList<>();
        if (channel == null) {
            return programs;
        }
        String currentTitle = channel.nowProgram == null ? "" : channel.nowProgram.trim();
        String nextTitle = channel.nextProgram == null ? "" : channel.nextProgram.trim();
        if (!currentTitle.isEmpty()) {
            programs.add(new EpgRepository.EpgProgram(
                    channel.id,
                    displayName(channel),
                    channel.tvgId,
                    currentTitle,
                    channel.logoUrl,
                    "",
                    formatIsoMillis(now - 30L * 60L * 1000L),
                    formatIsoMillis(now + 30L * 60L * 1000L),
                    "",
                    -1
            ));
        }
        if (!nextTitle.isEmpty()) {
            programs.add(new EpgRepository.EpgProgram(
                    channel.id,
                    displayName(channel),
                    channel.tvgId,
                    nextTitle,
                    channel.logoUrl,
                    "",
                    formatIsoMillis(now + 30L * 60L * 1000L),
                    formatIsoMillis(now + 90L * 60L * 1000L),
                    "",
                    -1
            ));
        }
        return programs;
    }

    private List<ChannelItem> selectTimelineChannels(int anchorIndex) {
        List<ChannelItem> liveChannels = new ArrayList<>();
        for (ChannelItem channel : channels) {
            if (channel != null && !channel.isVod) {
                liveChannels.add(channel);
            }
        }
        if (liveChannels.size() <= TIMELINE_MAX_RENDERED_CHANNELS) {
            return liveChannels;
        }
        String anchorId = anchorIndex >= 0 && anchorIndex < channels.size() && channels.get(anchorIndex) != null
                ? channels.get(anchorIndex).id
                : null;
        int liveAnchorIndex = 0;
        if (anchorId != null) {
            for (int i = 0; i < liveChannels.size(); i++) {
                if (anchorId.equals(liveChannels.get(i).id)) {
                    liveAnchorIndex = i;
                    break;
                }
            }
        }
        int half = TIMELINE_MAX_RENDERED_CHANNELS / 2;
        int start = Math.max(0, liveAnchorIndex - half);
        int end = Math.min(liveChannels.size(), start + TIMELINE_MAX_RENDERED_CHANNELS);
        start = Math.max(0, end - TIMELINE_MAX_RENDERED_CHANNELS);
        return new ArrayList<>(liveChannels.subList(start, end));
    }

    private List<ChannelItem> selectTimelineChannelsAroundChannel(ChannelItem anchorChannel) {
        List<ChannelItem> liveChannels = new ArrayList<>();
        if (anchorChannel == null || anchorChannel.isVod) {
            return liveChannels;
        }
        String anchorPlatform = anchorChannel.platformName == null ? "" : anchorChannel.platformName.trim();
        String anchorGroup = anchorChannel.group == null ? "" : anchorChannel.group.trim();
        for (ChannelItem channel : allChannels) {
            if (channel == null || channel.isVod || channel.id == null || channel.id.trim().isEmpty()) {
                continue;
            }
            boolean samePlatform = anchorChannel.platformId > 0 && channel.platformId == anchorChannel.platformId;
            if (!samePlatform && !anchorPlatform.isEmpty()) {
                samePlatform = anchorPlatform.equalsIgnoreCase(channel.platformName == null ? "" : channel.platformName.trim());
            }
            boolean sameGroup = !anchorGroup.isEmpty() && anchorGroup.equalsIgnoreCase(channel.group == null ? "" : channel.group.trim());
            if (anchorChannel.id.equals(channel.id) || samePlatform || sameGroup) {
                liveChannels.add(channel);
            }
        }
        if (liveChannels.isEmpty()) {
            liveChannels.add(anchorChannel);
            return liveChannels;
        }
        int anchorIndex = 0;
        for (int i = 0; i < liveChannels.size(); i++) {
            if (anchorChannel.id.equals(liveChannels.get(i).id)) {
                anchorIndex = i;
                break;
            }
        }
        int half = TIMELINE_MAX_RENDERED_CHANNELS / 2;
        int start = Math.max(0, anchorIndex - half);
        int end = Math.min(liveChannels.size(), start + TIMELINE_MAX_RENDERED_CHANNELS);
        start = Math.max(0, end - TIMELINE_MAX_RENDERED_CHANNELS);
        return new ArrayList<>(liveChannels.subList(start, end));
    }

    private List<RecordingsRepository.RecordingItem> fetchScheduledRecordingsSafely(String source) {
        List<RecordingsRepository.RecordingItem> scheduledItems = new ArrayList<>();
        if (isOfflineRecordingsDisabled()) {
            return scheduledItems;
        }
        try {
            RecordingsRepository.RecordingsResult scheduledResult = recordingsRepository.fetchScheduledRecordings();
            if (scheduledResult != null && scheduledResult.items != null) {
                scheduledItems.addAll(scheduledResult.items);
            }
        } catch (Exception scheduledErr) {
            Log.w(TAG, source + " scheduled recordings fetch failed", scheduledErr);
        }
        return scheduledItems;
    }


    private void openVisualEpgAroundSelection() {
        if (channels.isEmpty()) {
            return;
        }
        int visualAnchorIndex = findChannelIndexById(lastVisualEpgChannelId);
        final ChannelItem anchorChannel = (visualAnchorIndex >= 0 && visualAnchorIndex < channels.size())
                ? channels.get(visualAnchorIndex)
                : ((overlayNavigationState.selectedOverlayIndex >= 0 && overlayNavigationState.selectedOverlayIndex < channels.size())
                ? channels.get(overlayNavigationState.selectedOverlayIndex)
                : ((overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size()) ? channels.get(overlayNavigationState.currentIndex) : channels.get(0)));
        final String anchorChannelId = anchorChannel.id;
        final String platformLabel = (anchorChannel.platformName == null || anchorChannel.platformName.trim().isEmpty())
                ? getString(R.string.visual_epg_platform_visible)
                : anchorChannel.platformName.trim();
        showStatus(getString(R.string.status_loading_visual_epg));
        interactiveExecutor.execute(() -> {
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
                final List<VisualEpgEntry> sportsEntriesForLiveFilter = sportsEntries;
                liveEntries.removeIf(entry -> containsVisualEpgProgram(sportsEntriesForLiveFilter, entry));
                sortVisualEpgEntries(liveEntries);
                sortVisualEpgEntries(movieEntries);
                sortVisualEpgEntries(seriesEntries);
                sortVisualEpgEntries(sportsEntries);
                liveEntries = trimVisualEpgEntries(liveEntries, anchorChannelId, VISUAL_EPG_MAX_ITEMS_PER_SECTION);
                movieEntries = trimVisualEpgEntries(movieEntries, anchorChannelId, VISUAL_EPG_MAX_ITEMS_PER_SECTION);
                seriesEntries = trimVisualEpgEntries(seriesEntries, anchorChannelId, VISUAL_EPG_MAX_ITEMS_PER_SECTION);
                sportsEntries = trimVisualEpgEntries(sportsEntries, anchorChannelId, VISUAL_EPG_MAX_ITEMS_PER_SECTION);
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
                postUiIfAlive(() -> {
                    if (sections.isEmpty()) {
                        showStatus(getString(R.string.visual_epg_empty));
                        return;
                    }
                    showVisualEpgDialog(sections, anchorChannelId, platformLabel, scheduledItems);
                });
            } catch (Exception e) {
                Log.w(TAG, "visual epg failed", e);
                postUiIfAlive(() -> showStatus(getString(R.string.status_failed_load_guide)));
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

    private List<VisualEpgEntry> trimVisualEpgEntries(List<VisualEpgEntry> entries, String anchorChannelId, int maxItems) {
        if (entries == null || entries.isEmpty()) {
            return new ArrayList<>();
        }
        if (entries.size() <= maxItems) {
            return entries;
        }
        List<VisualEpgEntry> trimmed = new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        if (anchorChannelId != null) {
            for (VisualEpgEntry entry : entries) {
                if (entry != null && entry.channel != null && anchorChannelId.equals(entry.channel.id) && seen.add(visualEpgEntryKey(entry))) {
                    trimmed.add(entry);
                    break;
                }
            }
        }
        for (VisualEpgEntry entry : entries) {
            if (trimmed.size() >= maxItems) {
                break;
            }
            if (entry != null && seen.add(visualEpgEntryKey(entry))) {
                trimmed.add(entry);
            }
        }
        return trimmed;
    }

    private String visualEpgEntryKey(VisualEpgEntry entry) {
        if (entry == null) {
            return "";
        }
        String channelId = entry.channel == null ? "" : String.valueOf(entry.channel.id);
        String title = entry.program == null ? "" : String.valueOf(entry.program.title);
        String start = entry.program == null ? "" : String.valueOf(entry.program.startTime);
        return channelId + "|" + title + "|" + start;
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
        ComposeView visualEpgPanelComposeView = new ComposeView(this);
        visualEpgPanelComposeView.setFocusable(true);
        visualEpgPanelComposeView.setFocusableInTouchMode(true);
        final android.app.Dialog[] visualEpgDialogRef = new android.app.Dialog[1];
        final Runnable focusInitialVisualEpgCard = () -> {
            visualEpgPanelComposeView.requestFocus();
        };
        final java.util.function.Consumer<TimelineProgramDetailUiModel> renderVisualEpgDetail = model ->
                VisualEpgPanelComposeBinder.updateDetail(visualEpgPanelComposeView, model);
        int totalItems = 0;
        for (VisualEpgSection section : sections) {
            totalItems += section.entries.size();
        }
        activeProgramScheduledItems = scheduledItems == null ? new ArrayList<>() : new ArrayList<>(scheduledItems);
        VisualEpgHeaderUiModel visualEpgHeaderModel = new VisualEpgHeaderUiModel(
                getString(R.string.title_visual_epg),
                getString(R.string.visual_epg_subtitle, platformLabel, totalItems),
                Arrays.asList(
                        new VisualEpgHeaderUiModel.VisualEpgHeaderActionUiModel(getString(R.string.recording_action_refresh), () -> {
                            if (visualEpgDialogRef[0] != null) {
                                visualEpgDialogRef[0].dismiss();
                            }
                            openVisualEpgAroundSelection();
                        }, focusInitialVisualEpgCard),
                        new VisualEpgHeaderUiModel.VisualEpgHeaderActionUiModel(getString(R.string.dialog_close), () -> {
                            if (visualEpgDialogRef[0] != null) {
                                visualEpgDialogRef[0].dismiss();
                            }
                        }, focusInitialVisualEpgCard)
                )
        );
        TimelineProgramDetailUiModel initialVisualEpgDetail = new TimelineProgramDetailUiModel(
                getString(R.string.title_visual_epg),
                getString(R.string.visual_epg_detail_hint),
                getString(R.string.timeline_program_desc_empty),
                "",
                "",
                getString(R.string.timeline_program_action_hint)
        );
        List<VisualEpgSectionUiModel> sectionModels = new ArrayList<>();
        final boolean[] preferredAssigned = new boolean[]{false};
        for (VisualEpgSection section : sections) {
            if (section.entries == null || section.entries.isEmpty()) {
                continue;
            }
            List<VisualEpgEntryUiModel> entryModels = new ArrayList<>();
            for (VisualEpgEntry entry : section.entries) {
                ChannelItem channel = entry.channel;
                EpgRepository.EpgProgram program = entry.program;
                boolean scheduled = isProgramScheduled(channel, program, scheduledItems);
                boolean live = program.progress >= 0;
                String heroPoster = ProgramArtworkResolver.resolve(program, channel);
                String cardTitle = program.title == null || program.title.trim().isEmpty() ? getString(R.string.label_program_default) : program.title.trim();
                String cardTime = shortTime(program.startTime) + " - " + shortTime(program.endTime);
                String cardBadge = scheduled ? getString(R.string.timeline_program_scheduled_short) : (live ? getString(R.string.guide_program_now) : channel.name);
                boolean preferredVisualCard = lastVisualEpgChannelId != null && lastVisualEpgChannelId.equals(channel.id)
                        && lastVisualEpgProgramStartTime != null && lastVisualEpgProgramStartTime.equals(program.startTime);
                boolean preferredAnchorCard = anchorChannelId != null && anchorChannelId.equals(channel.id);
                boolean preferred = false;
                if (!preferredAssigned[0] && (preferredVisualCard || preferredAnchorCard || sectionModels.isEmpty() && entryModels.isEmpty())) {
                    preferred = true;
                    preferredAssigned[0] = true;
                }
                Runnable onFocus = () -> {
                    lastVisualEpgChannelId = channel == null ? lastVisualEpgChannelId : channel.id;
                    lastVisualEpgProgramStartTime = program == null ? lastVisualEpgProgramStartTime : program.startTime;
                    String detailMeta = channel.name + "  ·  " + shortTime(program.startTime) + " - " + shortTime(program.endTime);
                    if (live) {
                        detailMeta = detailMeta + "  ·  " + getString(R.string.guide_program_now);
                    }
                    if (scheduled) {
                        detailMeta = detailMeta + "  ·  " + getString(R.string.timeline_program_scheduled_short);
                    }
                    renderVisualEpgDetail.accept(new TimelineProgramDetailUiModel(
                            program.title == null || program.title.trim().isEmpty() ? getString(R.string.label_program_default) : program.title.trim(),
                            detailMeta,
                            program.description == null || program.description.trim().isEmpty() ? getString(R.string.timeline_program_desc_empty) : program.description.trim(),
                            heroPoster,
                            scheduled ? getString(R.string.timeline_program_scheduled_short) : live ? getString(R.string.guide_program_now) : "",
                            getString(R.string.timeline_program_action_hint)
                    ));
                };
                entryModels.add(new VisualEpgEntryUiModel(
                        new VisualEpgCardUiModel(cardTitle, cardTime, cardBadge, heroPoster, scheduled, false),
                        preferred,
                        onFocus,
                        () -> channelActionsCoordinator.showProgramActionMenu(channel, program)
                ));
            }
            sectionModels.add(new VisualEpgSectionUiModel(section.title, entryModels));
        }
        attachDialogViewTreeOwners(visualEpgPanelComposeView);
        VisualEpgPanelComposeBinder.bind(
                visualEpgPanelComposeView,
                new VisualEpgPanelUiModel(visualEpgHeaderModel, sectionModels, initialVisualEpgDetail),
                (imageView, item) -> bindRecordingPoster(imageView, item == null ? "" : item.posterUrl),
                (imageView, item) -> {
                    if (imageView == null || item == null || item.imageUrl == null || item.imageUrl.trim().isEmpty()) {
                        if (imageView != null) {
                            Glide.with(this).clear(imageView);
                            imageView.setImageDrawable(null);
                        }
                        return;
                    }
                    Glide.with(this).load(item.imageUrl.trim()).fitCenter()
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .override(dp(320), dp(200)).into(imageView);
                }
        );

        Dialog dialog = ComposeDialogHost.showFullscreen(this, visualEpgPanelComposeView, () -> {
            VisualEpgPanelComposeBinder.clear(visualEpgPanelComposeView);
            handleModalDismissed();
        });
        visualEpgDialogRef[0] = dialog;
        dialog.setCancelable(true);
        handleModalShown();
        visualEpgPanelComposeView.requestFocus();
    }

    private void openCurrentProgramInfoFromTouch() {
        ChannelItem channel = getCurrentPlaybackChannelItem();
        if (channel == null && overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size()) {
            channel = channels.get(overlayNavigationState.currentIndex);
        }
        if (channel == null) {
            showStatus(getString(R.string.status_no_program_in_epg));
            return;
        }
        if (channel.isVod) {
            showVodInfoDialog(channel);
            return;
        }
        final ChannelItem targetChannel = channel;
        Log.w(TAG, "touch info start channel=" + targetChannel.id + " name=" + displayName(targetChannel));
        EpgRepository.EpgProgram cachedProgram = findCachedProgramForChannel(targetChannel, false);
        if (cachedProgram != null) {
            Log.w(TAG, "touch info cached channel=" + targetChannel.id
                    + " program=" + cachedProgram.title
                    + " start=" + cachedProgram.startTime
                    + " end=" + cachedProgram.endTime);
            showCurrentProgramInfoDialog(targetChannel, cachedProgram);
            return;
        }
        showStatus(getString(R.string.status_searching_current_program));
        interactiveExecutor.execute(() -> {
            try {
                EpgRepository.EpgProgram program = epgRepository.fetchProgramForChannel(targetChannel, false);
                Log.w(TAG, "touch info result channel=" + targetChannel.id
                        + " program=" + (program == null ? "" : program.title)
                        + " start=" + (program == null ? "" : program.startTime)
                        + " end=" + (program == null ? "" : program.endTime));
                if (program == null) {
                    postUiIfAlive(() -> showStatus(getString(R.string.status_no_program_in_epg)));
                    return;
                }
                postUiIfAlive(() -> showCurrentProgramInfoDialog(targetChannel, program));
            } catch (Exception e) {
                Log.w(TAG, "touch info current program failed", e);
                postUiIfAlive(() -> showStatus(getString(R.string.status_failed_get_program)));
            }
        });
    }

    private void openMovistarIsmU7d(ChannelItem channel) {
        if (!isMovistarIsmChannel(channel)) {
            showStatus(getString(R.string.status_u7d_unavailable));
            return;
        }
        if (u7dProgramsLoading) {
            showStatus(getString(R.string.status_loading_u7d));
            return;
        }
        u7dProgramsLoading = true;
        showLoading(
                getString(R.string.u7d_menu_title, displayName(channel)),
                getString(R.string.u7d_loading_step_programs),
                getString(R.string.u7d_loading_detail_programs)
        );
        interactiveExecutor.execute(() -> {
            try {
                List<EpgRepository.EpgProgram> programs = fetchMovistarIsmU7dPrograms(channel);
                postUiIfAlive(() -> {
                    u7dProgramsLoading = false;
                    hideStartupLoading();
                    showMovistarIsmU7dMenu(channel, programs);
                });
            } catch (Exception e) {
                Log.w(TAG, "failed to load Movistar ISM U7D programs channel=" + channel.id, e);
                postUiIfAlive(() -> {
                    u7dProgramsLoading = false;
                    hideStartupLoading();
                    showError(getString(R.string.status_u7d_load_failed));
                });
            }
        });
    }

    private List<EpgRepository.EpgProgram> fetchMovistarIsmU7dPrograms(ChannelItem channel) throws Exception {
        List<EpgRepository.EpgProgram> programs = fetchMovistarIsmU7dProgramsFromBackend(channel);
        if (!programs.isEmpty() || epgRepository == null) {
            return programs;
        }
        Log.i(TAG, "Movistar ISM U7D backend returned no matches, falling back to offline EPG channel=" + channel.id);
        return epgRepository.fetchPastChannelPrograms(channel, 80, 7);
    }

    private List<EpgRepository.EpgProgram> fetchMovistarIsmU7dProgramsFromBackend(ChannelItem channel) throws Exception {
        List<EpgRepository.EpgProgram> result = new ArrayList<>();
        if (channel == null || httpClient == null || baseUrl == null || baseUrl.trim().isEmpty()) {
            return result;
        }
        List<String> candidates = new ArrayList<>();
        addUniqueU7dBaseUrl(candidates, baseUrl);
        addUniqueU7dBaseUrl(candidates, "https://iptv.bepllorens.com");
        String token = catalogSnapshotStore == null ? "" : catalogSnapshotStore.getAccessToken();
        String deviceId = catalogSnapshotStore == null ? "" : catalogSnapshotStore.getDeviceId();
        Exception lastError = null;
        for (String candidateBaseUrl : candidates) {
            String url = buildMovistarU7dProgramsUrl(candidateBaseUrl, token, deviceId, channel);
            try {
                HttpClient.Response response = httpClient.get(url, 10000, 25000, buildAuthenticatedJsonHeaders());
                if (response == null || !response.isSuccessful()) {
                    int code = response == null ? 0 : response.code;
                    Log.w(TAG, "Movistar ISM U7D programs endpoint failed host=" + safeLogHost(candidateBaseUrl) + " http=" + code);
                    lastError = new IllegalStateException("cargando U7D Movistar: HTTP " + code);
                    continue;
                }
                Log.i(TAG, "Movistar ISM U7D programs endpoint ok host=" + safeLogHost(candidateBaseUrl));
                return parseMovistarU7dProgramsPayload(channel, response.body);
            } catch (Exception e) {
                Log.w(TAG, "Movistar ISM U7D programs endpoint exception host=" + safeLogHost(candidateBaseUrl), e);
                lastError = e;
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        return result;
    }

    private void addUniqueU7dBaseUrl(List<String> candidates, String value) {
        String normalized = normalizeBaseUrl(value);
        if (normalized.isEmpty() || candidates.contains(normalized)) {
            return;
        }
        candidates.add(normalized);
    }

    private String buildMovistarU7dProgramsUrl(String candidateBaseUrl, String token, String deviceId, ChannelItem channel) {
        Uri.Builder uriBuilder = Uri.parse(candidateBaseUrl).buildUpon()
                .appendPath("api")
                .appendPath("offline")
                .appendPath("u7d")
                .appendPath("movistar-ism")
                .appendPath("programs");
        if (channel != null) {
            if (channel.id != null && !channel.id.trim().isEmpty()) {
                uriBuilder.appendQueryParameter("channel_id", channel.id.trim());
            }
            if (channel.tvgId != null && !channel.tvgId.trim().isEmpty()) {
                uriBuilder.appendQueryParameter("tvg_id", channel.tvgId.trim());
            }
            String channelName = displayName(channel);
            if (channelName != null && !channelName.trim().isEmpty()) {
                uriBuilder.appendQueryParameter("channel_name", channelName.trim());
            }
        }
        if (token != null && !token.trim().isEmpty()) {
            uriBuilder.appendQueryParameter("access_token", token.trim());
        }
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            uriBuilder.appendQueryParameter("device_id", deviceId.trim());
        }
        return uriBuilder.build().toString();
    }

    private List<EpgRepository.EpgProgram> parseMovistarU7dProgramsPayload(ChannelItem channel, String responseBody) throws Exception {
        List<EpgRepository.EpgProgram> result = new ArrayList<>();
        String body = responseBody == null ? "" : responseBody.trim();
        if (body.startsWith("[")) {
            return parseFlatMovistarU7dPrograms(channel, new JSONArray(body));
        }
        JSONObject payload = new JSONObject(body);
        JSONArray channelRows = payload.optJSONArray("channels");
        JSONArray flatRows = payload.optJSONArray("programs");
        if (flatRows != null) {
            return parseFlatMovistarU7dPrograms(channel, flatRows);
        }
        if (channelRows == null) {
            return result;
        }
        for (int i = 0; i < channelRows.length(); i++) {
            JSONObject row = channelRows.optJSONObject(i);
            if (!matchesMovistarU7dChannel(channel, row)) {
                continue;
            }
            JSONArray programRows = row.optJSONArray("programs");
            if (programRows == null) {
                continue;
            }
            for (int j = 0; j < programRows.length(); j++) {
                JSONObject program = programRows.optJSONObject(j);
                if (program == null) {
                    continue;
                }
                String start = firstNonEmpty(program.optString("start_dt", ""), program.optString("start_iso", ""));
                String end = firstNonEmpty(program.optString("end_iso", ""), program.optString("end_dt", ""));
                if (parseIsoMillis(start) <= 0L || parseIsoMillis(end) <= 0L) {
                    continue;
                }
                String title = firstNonEmpty(program.optString("display_name", ""), program.optString("title", ""));
                String icon = firstNonEmpty(program.optString("poster", ""), program.optString("logo", ""), row.optString("logo", ""), channel.logoUrl);
                String description = firstNonEmpty(program.optString("description", ""), program.optString("genre", ""));
                result.add(new EpgRepository.EpgProgram(
                        channel.id,
                        displayName(channel),
                        channel.tvgId,
                        title,
                        icon,
                        description,
                        start,
                        end,
                        program.optString("genre", ""),
                        0
                ));
            }
            Log.i(TAG, "Movistar ISM U7D matched channel=" + displayName(channel) + " backend=" + row.optString("name", "") + " programs=" + result.size());
            return result;
        }
        return result;
    }

    private String safeLogHost(String value) {
        try {
            Uri uri = Uri.parse(value == null ? "" : value.trim());
            String host = uri.getHost();
            return host == null ? "" : host;
        } catch (Exception e) {
            return "";
        }
    }

    private List<EpgRepository.EpgProgram> parseFlatMovistarU7dPrograms(ChannelItem channel, JSONArray programRows) {
        List<EpgRepository.EpgProgram> result = new ArrayList<>();
        if (channel == null || programRows == null) {
            return result;
        }
        for (int i = 0; i < programRows.length(); i++) {
            JSONObject program = programRows.optJSONObject(i);
            if (program == null || !matchesFlatMovistarU7dProgram(channel, program)) {
                continue;
            }
            String start = firstNonEmpty(program.optString("start_dt", ""), program.optString("start_iso", ""));
            String end = firstNonEmpty(program.optString("end_iso", ""), program.optString("end_dt", ""));
            if (parseIsoMillis(start) <= 0L || parseIsoMillis(end) <= 0L) {
                continue;
            }
            String title = firstNonEmpty(program.optString("display_name", ""), program.optString("title", ""));
            String icon = firstNonEmpty(program.optString("poster", ""), program.optString("logo", ""), channel.logoUrl);
            String description = firstNonEmpty(program.optString("description", ""), program.optString("genre", ""));
            result.add(new EpgRepository.EpgProgram(
                    channel.id,
                    displayName(channel),
                    channel.tvgId,
                    title,
                    icon,
                    description,
                    start,
                    end,
                    program.optString("genre", ""),
                    0
            ));
        }
        Log.i(TAG, "Movistar ISM U7D flat report matched channel=" + displayName(channel) + " programs=" + result.size());
        return result;
    }

    private boolean matchesFlatMovistarU7dProgram(ChannelItem channel, JSONObject program) {
        if (channel == null || program == null) {
            return false;
        }
        String backendCode = normalizeU7dMatchText(program.optString("channel_code", ""));
        String backendName = normalizeU7dMatchText(program.optString("channel_name", ""));
        String channelTvg = normalizeU7dMatchText(channel.tvgId);
        String channelName = normalizeU7dMatchText(displayName(channel));
        String rawName = normalizeU7dMatchText(channel.name);
        return u7dTextMatches(channelTvg, backendCode)
                || u7dTextMatches(channelTvg, backendName)
                || u7dTextMatches(channelName, backendName)
                || u7dTextMatches(rawName, backendName)
                || u7dTextMatches(channelName, backendCode)
                || u7dTextMatches(rawName, backendCode);
    }

    private boolean matchesMovistarU7dChannel(ChannelItem channel, JSONObject row) {
        if (channel == null || row == null) {
            return false;
        }
        String backendCode = normalizeU7dMatchText(row.optString("code", ""));
        String backendName = normalizeU7dMatchText(row.optString("name", ""));
        String channelTvg = normalizeU7dMatchText(channel.tvgId);
        String channelName = normalizeU7dMatchText(displayName(channel));
        String rawName = normalizeU7dMatchText(channel.name);
        return u7dTextMatches(channelTvg, backendCode)
                || u7dTextMatches(channelTvg, backendName)
                || u7dTextMatches(channelName, backendName)
                || u7dTextMatches(rawName, backendName)
                || u7dTextMatches(channelName, backendCode)
                || u7dTextMatches(rawName, backendCode);
    }

    private boolean u7dTextMatches(String left, String right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return false;
        }
        return left.equals(right) || left.contains(right) || right.contains(left);
    }

    private String normalizeU7dMatchText(String value) {
        String normalized = safeSearchText(value)
                .replaceAll("\\b(uhd|fhd|hd|sd|movistar|ism|m|plus)\\b", " ")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private void showMovistarIsmU7dMenu(ChannelItem channel, List<EpgRepository.EpgProgram> programs) {
        if (channel == null || programs == null || programs.isEmpty()) {
            showStatus(getString(R.string.status_u7d_empty));
            return;
        }
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        for (EpgRepository.EpgProgram program : programs) {
            if (program == null || parseIsoMillis(program.startTime) <= 0L || parseIsoMillis(program.endTime) <= 0L) {
                continue;
            }
            options.add(formatU7dProgramLabel(program));
            actions.add(() -> playMovistarIsmU7dProgram(channel, program));
        }
        if (options.isEmpty()) {
            showStatus(getString(R.string.status_u7d_empty));
            return;
        }
        showTvOptionsDialog(
                getString(R.string.u7d_menu_title, displayName(channel)),
                getString(R.string.u7d_menu_message),
                options,
                actions
        );
    }

    private boolean isTouchControlsVisibleForRemote() {
        return !touchDeviceMode && touchControlsBar != null && touchControlsBar.getVisibility() == View.VISIBLE;
    }

    private void hideTouchControlsForRemote() {
        if (touchControlsController != null) {
            touchControlsController.hideAllTransientControls();
        } else if (touchControlsBar != null) {
            touchControlsBar.setVisibility(View.GONE);
        }
        touchControlsFocusState.clear();
        currentTouchControlsBarModel = null;
    }

    private void resetTouchControlsFocus() {
        touchControlsFocusState.reset(firstEnabledTouchActionIndex());
        refreshTouchControlsBar();
        updateTimeshiftBar();
    }

    private int firstEnabledTouchActionIndex() {
        TouchControlsBarUiModel model = currentTouchControlsBarModel;
        if (model == null || model.actions == null || model.actions.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < model.actions.size(); i++) {
            ZapActionItem item = model.actions.get(i);
            if (item != null && item.enabled) {
                return i;
            }
        }
        return 0;
    }

    private void moveTouchControlsFocus(int delta) {
        TouchControlsBarUiModel model = currentTouchControlsBarModel;
        if (model == null || model.actions == null || model.actions.isEmpty()) {
            refreshTouchControlsBar();
            return;
        }
        boolean moved = touchControlsFocusState.moveToNextEnabledAction(delta, model.actions.size(), index -> {
            ZapActionItem item = model.actions.get(index);
            return item != null && item.enabled;
        });
        if (moved) {
            refreshTouchControlsBar();
            scheduleTouchControlsAutoHide();
        }
    }

    private boolean isTouchControlsTimeshiftFocused() {
        return touchControlsFocusState.timeshiftFocused();
    }

    private boolean canSeekPlaybackBack() {
        PlayerController.PlaybackSeekState state = getCurrentU7dSeekState();
        if (state == null && playerController != null) {
            state = playerController.getPlaybackSeekState();
        }
        return state != null && state.currentMs > state.startMs;
    }

    private boolean canSeekPlaybackForward() {
        PlayerController.PlaybackSeekState state = getCurrentU7dSeekState();
        if (state == null && playerController != null) {
            state = playerController.getPlaybackSeekState();
        }
        return state != null && state.currentMs < state.endMs;
    }

    private boolean canResumeLivePlayback() {
        if (playerController == null) {
            return false;
        }
        PlayerController.PlaybackSeekState state = playerController.getPlaybackSeekState();
        return state != null && state.liveCapable && state.currentMs < state.endMs;
    }

    private boolean seekTouchControlsBack() {
        if (isCurrentU7dPlayback()) {
            PlayerController.PlaybackSeekState state = getCurrentU7dSeekState();
            if (state == null) {
                return false;
            }
            seekCurrentU7dPlaybackTo(state.currentMs - 30_000L);
            return true;
        }
        return playerController != null && playerController.seekTimeshiftBack();
    }

    private boolean seekTouchControlsForward() {
        if (isCurrentU7dPlayback()) {
            PlayerController.PlaybackSeekState state = getCurrentU7dSeekState();
            if (state == null) {
                return false;
            }
            seekCurrentU7dPlaybackTo(state.currentMs + 30_000L);
            return true;
        }
        return playerController != null && playerController.seekTimeshiftForward();
    }

    private void focusTouchControlsTimeshift() {
        if (playerController == null || (playerController.getPlaybackSeekState() == null && getCurrentU7dSeekState() == null)) {
            scheduleTouchControlsAutoHide();
            return;
        }
        touchControlsFocusState.focusTimeshift();
        updateTimeshiftBar();
        scheduleTouchControlsAutoHide();
    }

    private void focusTouchControlsActions() {
        if (!touchControlsFocusState.focusActionsIfNeeded()) {
            scheduleTouchControlsAutoHide();
            return;
        }
        refreshTouchControlsBar();
        updateTimeshiftBar();
        scheduleTouchControlsAutoHide();
    }

    private void activateTouchControlsFocus() {
        TouchControlsBarUiModel model = currentTouchControlsBarModel;
        if (model == null || model.actions == null || model.actions.isEmpty()) {
            refreshTouchControlsBar();
            return;
        }
        int index = Math.max(0, Math.min(model.actions.size() - 1, touchControlsFocusState.actionIndex()));
        ZapActionItem item = model.actions.get(index);
        Log.w(TAG, "touch controls activate index=" + index
                + " label=" + (item == null ? "null" : item.label)
                + " enabled=" + (item != null && item.enabled));
        if (item != null && item.enabled && item.onClick != null) {
            item.onClick.run();
        } else {
            scheduleTouchControlsAutoHide();
        }
    }

    private String formatU7dProgramLabel(EpgRepository.EpgProgram program) {
        long startMs = parseIsoMillis(program.startTime);
        long endMs = parseIsoMillis(program.endTime);
        String day = startMs <= 0L ? "" : new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(new Date(startMs));
        String title = program.title == null || program.title.trim().isEmpty()
                ? getString(R.string.diagnostics_value_unknown)
                : program.title.trim();
        String duration = endMs > startMs ? " · " + formatDurationShort(endMs - startMs) : "";
        return day + " · " + shortTime(program.startTime) + "  " + title + duration;
    }

    private void playMovistarIsmU7dProgram(ChannelItem channel, EpgRepository.EpgProgram program) {
        if (channel == null || program == null) {
            showStatus(getString(R.string.status_u7d_unavailable));
            return;
        }
        String replayUrl = buildMovistarIsmU7dUrl(channel, program);
        if (replayUrl.isEmpty()) {
            showStatus(getString(R.string.status_u7d_unavailable));
            return;
        }
        long startMs = parseIsoMillis(program.startTime);
        long endMs = parseIsoMillis(program.endTime);
        String programTitle = program.title == null || program.title.trim().isEmpty()
                ? displayName(channel)
                : program.title.trim();
        ChannelItem replayItem = new ChannelItem(
                "u7d:" + channel.id + ":" + Math.max(0L, startMs),
                programTitle,
                channel.tvgId,
                ProgramArtworkResolver.resolve(program, channel),
                getString(R.string.u7d_replay_group),
                replayUrl,
                "",
                channel.originalOrder,
                channel.dashboardOrder,
                true,
                false,
                channel.platformId,
                getString(R.string.u7d_replay_platform),
                new ArrayList<>(),
                "",
                "",
                "",
                true,
                program.description,
                "",
                endMs > startMs ? (endMs - startMs) / 1000L : 0L,
                "u7d_proxy"
        );
        currentPlaybackU7dBaseUrl = replayUrl;
        currentPlaybackU7dDurationMs = Math.max(0L, endMs - startMs);
        currentPlaybackU7dOffsetMs = 0L;
        currentPlaybackU7dItem = replayItem;
        currentPlaybackTransientItem = replayItem;
        String previousLastChannelId = lastChannelId;
        showStatus(getString(R.string.status_u7d_opening, programTitle));
        playChannelItemInternal(replayItem, true, 0L);
        if (previousLastChannelId != null && !previousLastChannelId.trim().isEmpty()) {
            saveLastChannelId(previousLastChannelId);
        } else if (channel.id != null && !channel.id.trim().isEmpty()) {
            saveLastChannelId(channel.id);
        }
        updateVodLoadingState(
                replayItem,
                getString(R.string.u7d_loading_title),
                getString(R.string.u7d_loading_step_manifest),
                getString(R.string.u7d_loading_detail_manifest)
        );
    }

    private String buildMovistarIsmU7dUrl(ChannelItem channel, EpgRepository.EpgProgram program) {
        if (channel == null || program == null || baseUrl == null || baseUrl.trim().isEmpty()) {
            return "";
        }
        try {
            Uri.Builder builder = Uri.parse(resolveMovistarU7dStreamBaseUrl()).buildUpon()
                    .appendPath("api")
                    .appendPath("offline")
                    .appendPath("u7d")
                    .appendPath("movistar-ism")
                    .appendPath("stream")
                    .appendQueryParameter("channel_id", channel.id)
                    .appendQueryParameter("tvg_id", channel.tvgId)
                    .appendQueryParameter("channel_name", displayName(channel))
                    .appendQueryParameter("start_time", program.startTime)
                    .appendQueryParameter("end_time", program.endTime)
                    .appendQueryParameter("title", program.title);
            String token = catalogSnapshotStore == null ? "" : catalogSnapshotStore.getAccessToken();
            if (token != null && !token.trim().isEmpty()) {
                builder.appendQueryParameter("access_token", token.trim());
            }
            String deviceId = catalogSnapshotStore == null ? "" : catalogSnapshotStore.getDeviceId();
            if (deviceId != null && !deviceId.trim().isEmpty()) {
                builder.appendQueryParameter("device_id", deviceId.trim());
            }
            return builder.build().toString();
        } catch (Exception e) {
            Log.w(TAG, "failed to build Movistar ISM U7D url", e);
            return "";
        }
    }

    private String resolveMovistarU7dStreamBaseUrl() {
        String normalized = normalizeBaseUrl(baseUrl);
        try {
            String host = Uri.parse(normalized).getHost();
            if ("fire.tvbep.com".equalsIgnoreCase(host)) {
                return "https://iptv.bepllorens.com";
            }
        } catch (Exception ignored) {
        }
        return normalized.isEmpty() ? "https://iptv.bepllorens.com" : normalized;
    }

    private void showAboutDialog() {
        showTvMessagePanel(
                getString(R.string.title_about_app, BuildConfig.VERSION_NAME),
                getString(R.string.message_about_app),
                java.util.Collections.singletonList(new TvMessageActionUiModel(getString(R.string.dialog_close), false, null)),
                null
        );
    }

    private void showVodInfoDialog(ChannelItem channel) {
        showVodInfoDialog(channel, null);
    }

    private void showVodInfoDialog(ChannelItem channel, Runnable onBack) {
        if (channel == null) {
            return;
        }
        if (isProtectedItem(channel) && isProtectedContentLocked()) {
            ensureParentalAccessForItem(channel, () -> showVodInfoDialog(channel, onBack));
            return;
        }
        rememberCurrentVodPosition();
        prepareModalSurface();
        long resumeMs = getVodResumePosition(channel.id);
        final Dialog[] dialogHolder = new Dialog[1];
        final boolean[] navigationHandled = {false};
        Runnable panelBackAction = onBack == null ? null : () -> {
            navigationHandled[0] = true;
            dismissModalForNextAction(dialogHolder[0], onBack);
        };
        List<VodPanelActionUiModel> primaryActions = new ArrayList<>();
        primaryActions.add(new VodPanelActionUiModel(getString(R.string.vod_action_play), true, () -> {
            Dialog activeDialog = dialogHolder[0];
            if (activeDialog != null && activeDialog.isShowing()) {
                activeDialog.dismiss();
            }
            playVodItem(channel, true);
        }));
        if (resumeMs > 30_000L) {
            primaryActions.add(new VodPanelActionUiModel(getString(R.string.vod_action_continue), true, () -> {
                Dialog activeDialog = dialogHolder[0];
                if (activeDialog != null && activeDialog.isShowing()) {
                    activeDialog.dismiss();
                }
                playChannelItemInternal(channel, true, getVodResumePosition(channel.id));
            }));
            primaryActions.add(new VodPanelActionUiModel(getString(R.string.vod_action_start_over), true, () -> {
                Dialog activeDialog = dialogHolder[0];
                if (activeDialog != null && activeDialog.isShowing()) {
                    activeDialog.dismiss();
                }
                clearVodResumePosition(channel.id);
                playChannelItemInternal(channel, true, 0L);
            }));
        }
        List<VodPanelActionUiModel> secondaryActions = new ArrayList<>();
        secondaryActions.add(new VodPanelActionUiModel(getString(R.string.vod_action_more_vod), false, () -> {
            dismissModalForNextAction(dialogHolder[0], () -> showVodActionsDialog(channel, () -> showVodInfoDialog(channel, onBack)));
        }));
        secondaryActions.add(new VodPanelActionUiModel(getString(favoriteChannelIds.contains(channel.id) ? R.string.vod_action_remove_favorite : R.string.vod_action_add_favorite), false, () -> toggleFavoriteForChannel(channel)));
        if (resumeMs > 30_000L) {
            secondaryActions.add(new VodPanelActionUiModel(getString(R.string.vod_action_clear_progress_vod), false, () -> {
                clearVodResumePosition(channel.id);
                showStatus(getString(R.string.vod_status_progress_cleared));
            }));
        }
        ComposeView composeView = new ComposeView(this);
        attachDialogViewTreeOwners(composeView);
        VodDetailPanelComposeBinder.bind(
                composeView,
                new VodDetailPanelUiModel(
                        channel.name == null || channel.name.trim().isEmpty() ? getString(R.string.label_program_default) : channel.name.trim(),
                        buildVodInfoMeta(channel),
                        buildVodDescription(channel),
                        resumeMs > 0L ? buildVodProgressLabel(channel, resumeMs) : "",
                        channel.logoUrl,
                        getString(R.string.vod_detail_primary_actions),
                        getString(R.string.vod_detail_secondary_actions),
                        getString(R.string.vod_detail_action_hint),
                        primaryActions,
                        secondaryActions,
                        panelBackAction
                ),
                (imageView, item) -> bindRecordingPoster(imageView, item == null ? "" : item.posterUrl)
        );
        Dialog dialog = ComposeDialogHost.showFullscreen(this, composeView, () -> {
            if (onBack != null) {
                modalReturnAction = onBack;
            }
        }, this::handleModalDismissed);
        dialogHolder[0] = dialog;
        if (onBack != null) {
            dialog.setOnKeyListener((ignored, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP && !navigationHandled[0]) {
                    panelBackAction.run();
                    return true;
                }
                return false;
            });
        }
        handleModalShown();
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

    private void showVodActionsDialog(ChannelItem channel) {
        showVodActionsDialog(channel, null);
    }

    private void showVodActionsDialog(ChannelItem channel, Runnable onBack) {
        if (channel == null) {
            return;
        }
        prepareModalSurface();
        final Dialog[] dialogHolder = new Dialog[1];
        final boolean[] navigationHandled = {false};
        Runnable panelBackAction = onBack == null ? null : () -> {
            navigationHandled[0] = true;
            dismissModalForNextAction(dialogHolder[0], onBack);
        };
        List<VodPanelActionUiModel> actionRows = new ArrayList<>();
        actionRows.add(new VodPanelActionUiModel(getString(R.string.vod_action_play_vod), false, () -> {
            dismissDialog(dialogHolder[0]);
            clearVodResumePosition(channel.id);
            playChannelItemInternal(channel, true, 0L);
        }));
        if (getVodResumePosition(channel.id) > 30_000L) {
            actionRows.add(new VodPanelActionUiModel(getString(R.string.vod_action_continue_vod), false, () -> {
                dismissDialog(dialogHolder[0]);
                playChannelItemInternal(channel, true, getVodResumePosition(channel.id));
            }));
            actionRows.add(new VodPanelActionUiModel(getString(R.string.vod_action_start_over_vod), false, () -> {
                dismissDialog(dialogHolder[0]);
                clearVodResumePosition(channel.id);
                playChannelItemInternal(channel, true, 0L);
            }));
        }
        actionRows.add(new VodPanelActionUiModel(getString(R.string.vod_action_diagnostics), false, () -> showVodDiagnosticsDialog(channel)));
        actionRows.add(new VodPanelActionUiModel(getString(R.string.vod_action_retry_route), false, () -> {
            dismissDialog(dialogHolder[0]);
            retryCurrentPlaybackWithNextRoute(channel);
        }));
        actionRows.add(new VodPanelActionUiModel(getString(R.string.vod_action_temporary_mode), false, () -> {
            dismissDialog(dialogHolder[0]);
            showTemporaryPlaybackModeDialog(channel);
        }));
        actionRows.add(new VodPanelActionUiModel(getString(favoriteChannelIds.contains(channel.id) ? R.string.vod_action_remove_favorite : R.string.vod_action_add_favorite), false, () -> toggleFavoriteForChannel(channel)));
        actionRows.add(new VodPanelActionUiModel(getString(R.string.vod_action_personal_lists), false, () -> {
            dismissDialog(dialogHolder[0]);
            showPersonalListsDialog(channel);
        }));
        actionRows.add(new VodPanelActionUiModel(getString(R.string.vod_action_playback_diagnostics), false, () -> {
            dismissDialog(dialogHolder[0]);
            currentPlaybackVodId = channel.id;
            showPlaybackDiagnosticsDialog();
        }));
        actionRows.add(new VodPanelActionUiModel(getString(R.string.vod_action_clear_progress_vod), false, () -> {
            clearVodResumePosition(channel.id);
            showStatus(getString(R.string.vod_status_progress_cleared));
        }));
        ComposeView composeView = new ComposeView(this);
        attachDialogViewTreeOwners(composeView);
        VodDetailPanelComposeBinder.bind(
                composeView,
                new VodDetailPanelUiModel(
                        getString(R.string.vod_actions_title, displayName(channel)),
                        buildVodInfoMeta(channel),
                        "",
                        "",
                        channel.logoUrl,
                        "",
                        getString(R.string.vod_detail_secondary_actions),
                        getString(R.string.vod_detail_action_hint),
                        new ArrayList<>(),
                        actionRows,
                        panelBackAction
                ),
                (imageView, item) -> bindRecordingPoster(imageView, item == null ? "" : item.posterUrl)
        );
        Dialog dialog = ComposeDialogHost.showFullscreen(this, composeView, () -> {
            if (onBack != null) {
                modalReturnAction = onBack;
            }
        }, this::handleModalDismissed);
        dialogHolder[0] = dialog;
        if (onBack != null) {
            dialog.setOnKeyListener((ignored, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP && !navigationHandled[0]) {
                    panelBackAction.run();
                    return true;
                }
                return false;
            });
        }
        handleModalShown();
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
        appendDiagnosticLine(message, getString(R.string.diagnostics_target, fallbackUnknown(DiagnosticRedactor.sanitizeUrl(decision == null ? channel.playUrl : decision.targetUrl))));
        appendDiagnosticLine(message, getString(R.string.diagnostics_mime, decision == null ? fallbackUnknown(PlaybackRouteResolver.inferMimeType(channel.playUrl)) : fallbackUnknown(decision.mimeType)));
        appendDiagnosticLine(message, getString(R.string.diagnostics_drm, fallbackUnknown(channel.drmScheme)));
        appendDiagnosticLine(message, getString(R.string.vod_diagnostics_direct, getString(channel.directPlayback ? R.string.diagnostics_value_yes : R.string.diagnostics_value_no)));
        if (channel.vodFilterKey != null && !channel.vodFilterKey.trim().isEmpty()) {
            appendDiagnosticLine(message, getString(R.string.vod_diagnostics_filter, channel.vodFilterKey));
        }
        if (channel.playUrl != null && channel.playUrl.contains("/api/vod/runtime/stream/")) {
            appendDiagnosticLine(message, getString(R.string.vod_diagnostics_runtime_hls));
        }
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        actions.add(new TvMessageActionUiModel(getString(R.string.vod_action_play), false, () -> playVodItem(channel, true)));
        actions.add(new TvMessageActionUiModel(getString(R.string.diagnostics_action_temporary_mode), false, () -> showTemporaryPlaybackModeDialog(channel)));
        actions.add(new TvMessageActionUiModel(getString(R.string.dialog_close), false, null));
        showTvMessagePanel(getString(R.string.vod_action_diagnostics), message.toString().trim(), actions, null);
    }

    private void showCurrentProgramInfoDialog(ChannelItem channel, EpgRepository.EpgProgram program) {
        if (channel == null || program == null) {
            return;
        }
        String title = program.title == null || program.title.trim().isEmpty() ? channel.name : program.title;
        String description = program.description == null || program.description.trim().isEmpty()
                ? getString(R.string.timeline_program_desc_empty)
                : program.description.trim();
        String imageUrl = ProgramArtworkResolver.resolve(program, channel);
        String meta = channel.name + "  ·  " + shortTime(program.startTime) + " - " + shortTime(program.endTime);
        final Dialog[] dialogHolder = new Dialog[1];
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        if (!isOfflineRecordingsDisabled()) {
            actions.add(new TvMessageActionUiModel(getString(R.string.menu_record), false, () -> {
                if (dialogHolder[0] != null) {
                    dialogHolder[0].dismiss();
                }
                scheduleProgram(channel, program);
            }));
        }
        actions.add(new TvMessageActionUiModel(getString(R.string.dialog_close), false, () -> {
            if (dialogHolder[0] != null) {
                dialogHolder[0].dismiss();
            }
        }));
        ComposeView composeView = new ComposeView(this);
        attachDialogViewTreeOwners(composeView);
        ProgramInfoPanelComposeBinder.bind(
                composeView,
                new ProgramInfoPanelUiModel(
                        getString(R.string.title_touch_program_info),
                        new TimelineProgramDetailUiModel(
                                title,
                                meta,
                                description,
                                imageUrl,
                                program.progress >= 0 ? getString(R.string.guide_program_now) : "",
                                getString(R.string.timeline_program_action_hint)
                        ),
                        actions
                ),
                (imageView, item) -> bindProgramPoster(imageView, item == null ? "" : item.imageUrl)
        );
        Dialog dialog = ComposeDialogHost.showFullscreen(this, composeView, this::handleModalDismissed);
        dialogHolder[0] = dialog;
        handleModalShown();
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
        EpgRepository.EpgProgram cachedProgram = findCachedProgramForChannel(ch, next);
        if (cachedProgram != null) {
            if (reminderOnly) {
                createReminder(ch, cachedProgram);
            } else {
                scheduleProgram(ch, cachedProgram);
            }
            return;
        }
        showStatus(getString(next ? R.string.status_searching_next_program : R.string.status_searching_current_program));
        interactiveExecutor.execute(() -> {
            try {
                EpgRepository.EpgProgram program = epgRepository.fetchProgramForChannel(ch, next);
                if (program == null) {
                    postUiIfAlive(() -> showStatus(getString(R.string.status_no_program_in_epg)));
                    return;
                }
                postUiIfAlive(() -> {
                    if (reminderOnly) {
                        createReminder(ch, program);
                    } else {
                        scheduleProgram(ch, program);
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "fetch program failed", e);
                postUiIfAlive(() -> showStatus(getString(R.string.status_failed_get_program)));
            }
        });
    }

    private EpgRepository.EpgProgram findCachedProgramForChannel(ChannelItem channel, boolean next) {
        if (channel == null || channel.isVod) {
            return null;
        }
        EpgRepository.EpgProgramPair pair = channel.id == null ? null : epgProgramPairByChannelId.get(channel.id);
        if (pair != null) {
            EpgRepository.EpgProgram program = next ? pair.next : pair.current;
            if (program != null && program.title != null && !program.title.trim().isEmpty()) {
                return program;
            }
        }
        List<EpgRepository.EpgProgram> inlinePrograms = buildInlineTimelinePrograms(channel, System.currentTimeMillis());
        if (inlinePrograms.isEmpty()) {
            return null;
        }
        if (next) {
            return inlinePrograms.size() > 1 ? inlinePrograms.get(1) : null;
        }
        return inlinePrograms.get(0);
    }

    private void scheduleProgram(ChannelItem ch, EpgRepository.EpgProgram program) {
        if (ch == null || program == null) {
            return;
        }
        if (showOfflineRecordingsUnavailableIfNeeded()) {
            return;
        }
        if (!canScheduleRecordings()) {
            showStatus(getString(R.string.status_recording_schedule_permission_denied));
            return;
        }
        showStatus(getString(R.string.status_scheduling_recording));
        ioExecutor.execute(() -> {
            try {
                JSONObject req = new JSONObject();
                req.put("channel_id", Long.parseLong(ch.id));
                req.put("channel_name", ch.name);
                req.put("tvg_id", "");
                req.put("program_title", program.title == null || program.title.trim().isEmpty() ? ch.name : program.title);
                req.put("poster", ProgramArtworkResolver.resolve(program, ch));
                req.put("start_time", program.startTime == null ? "" : program.startTime);
                req.put("end_time", program.endTime == null ? "" : program.endTime);

                HttpClient.Response response = httpClient.postJson(
                        baseUrl + "/api/recordings/schedule",
                        req,
                        10000,
                        15000,
                        buildAuthenticatedJsonHeaders()
                );
                if (!response.isSuccessful()) {
                    throw new IllegalStateException("schedule HTTP " + response.code + ": " + response.body);
                }
                postUiIfAlive(() -> {
                    showStatus(getString(R.string.status_recording_scheduled));
                    markScheduledProgramInOpenGuides(ch, program);
                });
            } catch (Exception e) {
                Log.w(TAG, "schedule program failed", e);
                postUiIfAlive(() -> showStatus(getString(R.string.status_failed_schedule_recording)));
            }
        });
    }

    private java.util.Map<String, String> buildAuthenticatedJsonHeaders() {
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        String token = catalogSnapshotStore == null ? "" : catalogSnapshotStore.getAccessToken();
        if (token != null && !token.trim().isEmpty()) {
            headers.put("Authorization", "Bearer " + token.trim());
            headers.put("X-DRBEP-Access-Token", token.trim());
        }
        String deviceId = catalogSnapshotStore == null ? "" : catalogSnapshotStore.getDeviceId();
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            headers.put("X-DRBEP-Device-Id", deviceId.trim());
        }
        return headers;
    }

    private boolean canScheduleRecordings() {
        return currentOfflinePermissions == null || currentOfflinePermissions.canScheduleRecordings;
    }

    private boolean canDeleteRecordings() {
        return currentOfflinePermissions == null || currentOfflinePermissions.canDeleteRecordings;
    }

    private void markScheduledProgramInOpenGuides(ChannelItem channel, EpgRepository.EpgProgram program) {
        if (channel == null || program == null) {
            return;
        }
        RecordingsRepository.RecordingItem item = new RecordingsRepository.RecordingItem(
                "timeline-" + System.currentTimeMillis(),
                program.title == null || program.title.trim().isEmpty() ? channel.name : program.title,
                "",
                0L,
                "",
                channel.name,
                program.title == null ? "" : program.title,
                ProgramArtworkResolver.resolve(program, channel),
                "scheduled",
                program.startTime == null ? "" : program.startTime,
                program.endTime == null ? "" : program.endTime,
                false
        );
        activeProgramScheduledItems.add(item);
        if (activeTimelineDialog != null && activeTimelineDialog.isShowing()) {
            activeTimelineScheduledItems.add(item);
            refreshTimelineGuideDialog();
        }
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
        if (!canDeleteRecordings()) {
            showStatus(getString(R.string.status_recording_delete_permission_denied));
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
                postUiIfAlive(() -> {
                    activeProgramScheduledItems.remove(scheduled);
                    activeTimelineScheduledItems.removeIf(item -> item != null && scheduled.id.equals(item.id));
                    showStatus(getString(R.string.status_scheduled_recording_canceled));
                    if (activeTimelineDialog != null && activeTimelineDialog.isShowing()) {
                        refreshTimelineGuideDialog();
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "cancel scheduled program failed", e);
                postUiIfAlive(() -> showStatus(getString(R.string.status_failed_cancel_scheduled_recording)));
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
        ensureNotificationPermission();
        ReminderScheduler.schedule(this, item);
        showStatus(getString(R.string.status_reminder_created));
    }

    /** Solicita permiso de notificaciones en Android 13+ para que los recordatorios avisen. */
    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        try {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 4501);
            }
        } catch (Exception ignored) {
        }
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
                    postUiIfAlive(() -> showRecordingsPanel(primaryResult, desiredId));
                    return;
                }
                if (!alternateResult.items.isEmpty()) {
                    Log.d(TAG, "loadRecordingsPanel alternate scheduled=" + alternateResult.scheduledMode + " count=" + alternateResult.items.size());
                    postUiIfAlive(() -> {
                        showStatus(getString(scheduledMode
                                ? R.string.status_recordings_showing_completed
                                : R.string.status_recordings_showing_scheduled));
                        showRecordingsPanel(alternateResult, desiredId);
                    });
                    return;
                }
                Log.d(TAG, "loadRecordingsPanel both empty scheduledMode=" + scheduledMode);
                postUiIfAlive(() -> {
                    showRecordingsPanel(primaryResult, desiredId);
                    showStatus(getString(scheduledMode ? R.string.status_no_scheduled_recordings : R.string.status_no_recordings));
                });
            } catch (Exception e) {
                Log.w(TAG, scheduledMode ? "open scheduled recordings failed" : "open recordings failed", e);
                postUiIfAlive(() -> showStatus(getString(scheduledMode ? R.string.status_failed_load_scheduled_recordings : R.string.status_failed_load_recordings)));
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
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        actions.add(new TvMessageActionUiModel(getString(R.string.recording_action_cancel_confirm), true, () -> cancelScheduledRecording(item)));
        actions.add(new TvMessageActionUiModel(getString(R.string.dialog_cancel), false, null));
        showTvMessagePanel(
                getString(R.string.title_recording_cancel_confirm),
                getString(R.string.recording_cancel_confirm_message, buildRecordingTitle(item), buildRecordingMeta(item)),
                actions,
                null
        );
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
                postUiIfAlive(() -> {
                    showStatus(getString(R.string.status_scheduled_recording_canceled));
                    refreshRecordingsPanel();
                });
            } catch (Exception e) {
                Log.w(TAG, "cancel scheduled recording failed", e);
                postUiIfAlive(() -> showStatus(getString(R.string.status_failed_cancel_scheduled_recording)));
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
        List<String> labels = Arrays.asList(options);
        List<Runnable> actions = new ArrayList<>();
        actions.add(() -> adjustSelectedScheduledRecording(-15L * 60L * 1000L, -15L * 60L * 1000L));
        actions.add(() -> adjustSelectedScheduledRecording(15L * 60L * 1000L, 15L * 60L * 1000L));
        actions.add(() -> adjustSelectedScheduledRecording(0L, 15L * 60L * 1000L));
        actions.add(() -> adjustSelectedScheduledRecording(0L, -15L * 60L * 1000L));
        showTvOptionsDialog(R.string.title_recording_edit_time, null, labels, actions);
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
                postUiIfAlive(() -> {
                    showStatus(getString(R.string.status_scheduled_recording_updated));
                    refreshRecordingsPanel();
                });
            } catch (Exception e) {
                Log.w(TAG, "update scheduled recording failed", e);
                postUiIfAlive(() -> showStatus(getString(R.string.status_failed_update_scheduled_recording)));
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
            List<TvMessageActionUiModel> actions = new ArrayList<>();
            actions.add(new TvMessageActionUiModel(getString(R.string.recording_resume_continue), false, resumeFromSaved));
            actions.add(new TvMessageActionUiModel(getString(R.string.recording_resume_restart), true, startFromBeginning));
            showTvMessagePanel(getString(R.string.title_recordings_visual), getString(R.string.recording_resume_prompt, formatPlaybackPosition(resumePositionMs)), actions, null);
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
        options.add(getString(recordingsPanelController.isAutoRefreshEnabled() ? R.string.recording_action_auto_refresh_on : R.string.recording_action_auto_refresh_off));
        actions.add(this::toggleRecordingsAutoRefresh);
        options.add(getString(recordingsController.isScheduledMode() ? R.string.recording_action_switch_completed : R.string.recording_action_switch_scheduled));
        actions.add(() -> switchRecordingsMode(!recordingsController.isScheduledMode()));
        addRecordingFilterActions(options, actions, item);
        showTvOptionsDialog(R.string.title_recording_actions, null, options, actions);
    }

    private void clearSelectedRecordingProgress(RecordingsRepository.RecordingItem item) {
        if (item == null) {
            return;
        }
        clearRecordingResumePosition(item.id);
        refreshRecordingsPanelSurface();
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
        boolean enabled = recordingsPanelController.toggleAutoRefresh();
        showStatus(getString(enabled ? R.string.recordings_panel_auto_refresh_on : R.string.recordings_panel_auto_refresh_off));
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
        SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        String formatted = out.format(new Date(value));
        return formatted.length() > 2
                ? formatted.substring(0, formatted.length() - 2) + ":" + formatted.substring(formatted.length() - 2)
                : formatted;
    }

    private void tuneRelative(int delta) {
        if (channels.isEmpty()) {
            return;
        }
        int size = channels.size();
        int anchor = overlayNavigationState.currentIndex < 0 ? 0 : overlayNavigationState.currentIndex;
        int next = ((anchor + delta) % size + size) % size;
        tuneToIndex(next, true);
    }

    private void tuneSelectedChannel() {
        tuneToIndex(overlayNavigationState.selectedOverlayIndex, true);
        hideOverlay();
    }

    private void moveOverlaySelection(int delta) {
        syncOverlayCoordinator();
        channelOverlayCoordinator.moveOverlaySelection(delta);
        syncOverlayStateFromCoordinator();
        refreshOverlayChannelList();
        scrollOverlayChannelListToPosition(overlayNavigationState.selectedOverlayIndex);
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

    private int resolveStartupPlaybackIndex() {
        if (channels.isEmpty()) {
            return 0;
        }
        if (lastChannelId != null && !lastChannelId.trim().isEmpty()) {
            int found = findChannelIndexById(lastChannelId);
            if (found >= 0 && isLinearStartupChannel(channels.get(found))) {
                return found;
            }
        }
        for (int i = 0; i < channels.size(); i++) {
            if (isLinearStartupChannel(channels.get(i))) {
                return i;
            }
        }
        return 0;
    }

    private boolean isLinearStartupChannel(ChannelItem item) {
        return item != null
                && !item.isVod
                && item.playUrl != null
                && !item.playUrl.trim().isEmpty();
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
                .putString(PREF_LAST_FILTER_KEY, overlayNavigationState.selectedFilterKey == null || overlayNavigationState.selectedFilterKey.trim().isEmpty() ? "all" : overlayNavigationState.selectedFilterKey)
                .putBoolean(PREF_FAVORITES_ONLY, overlayNavigationState.favoritesOnly)
                .apply();
    }

    private void cycleFilter(int delta) {
        ChannelFilter targetFilter = findAdjacentFilter(delta);
        if (targetFilter != null && isProtectedFilter(targetFilter) && isProtectedContentLocked()) {
            ensureParentalAccessForFilterKey(targetFilter.key, () -> cycleFilter(delta));
            return;
        }
        syncOverlayCoordinator();
        ChannelFilter filter = channelOverlayCoordinator.cycleFilter(delta);
        syncOverlayStateFromCoordinator();
        persistNavigationState();
        refreshOverlayChannelList();
        updateFilterText();
        updateOverlaySearchState();

        if (channels.isEmpty()) {
            showStatus(getString(R.string.status_no_channels_for_filter));
            showOverlay();
            return;
        }

        if (overlayNavigationState.currentIndex < 0) {
            tuneToIndex(0, true);
        } else if (overlayNavigationState.selectedOverlayIndex >= 0) {
            scrollOverlayChannelListToPosition(overlayNavigationState.selectedOverlayIndex);
        }

        if (filter != null) {
            showStatus(getString(R.string.status_filter_changed, decorateProtectedFilterLabel(filter)));
        }
        scheduleVisibleEpgLoad(OFFLINE_EPG_PRIORITY_DELAY_MS);
        showOverlay();
    }

    private void showFilterPickerDialog() {
        if (filters.isEmpty()) {
            showStatus(getString(R.string.status_no_channels_for_filter));
            return;
        }
        List<ChannelFilter> selectableFilters = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int checkedIndex = -1;
        boolean hasUserVisibleFilters = hasUserVisibleOverlayFilters();
        for (ChannelFilter filter : filters) {
            if (filter == null || filter.key == null || filter.key.trim().isEmpty()) {
                continue;
            }
            if (hasUserVisibleFilters && !isUserVisibleOverlayFilter(filter)) {
                continue;
            }
            selectableFilters.add(filter);
            labels.add(decorateProtectedFilterLabel(filter));
            if (filter.key.equals(overlayNavigationState.selectedFilterKey)) {
                checkedIndex = selectableFilters.size() - 1;
            }
        }
        if (selectableFilters.isEmpty()) {
            showStatus(getString(R.string.status_no_channels_for_filter));
            return;
        }
        List<Runnable> actions = new ArrayList<>();
        for (int i = 0; i < selectableFilters.size(); i++) {
            final ChannelFilter selected = selectableFilters.get(i);
            if (i == checkedIndex) {
                labels.set(i, getString(R.string.settings_selected_prefix, labels.get(i)));
            }
            actions.add(() -> applySelectedFilterFromPicker(selected));
        }
        if (touchDeviceMode) {
            showTouchFilterPickerDialog(selectableFilters, checkedIndex);
            return;
        }
        showTvOptionsDialog(R.string.filter_navigation_hint, null, labels, actions);
    }

    private void showTouchFilterPickerDialog(List<ChannelFilter> selectableFilters, int checkedIndex) {
        if (selectableFilters == null || selectableFilters.isEmpty()) {
            showStatus(getString(R.string.status_no_channels_for_filter));
            return;
        }
        prepareModalSurface();
        if (touchControlsBar != null) {
            touchControlsBar.setVisibility(View.GONE);
        }
        ComposeView composeView = new ComposeView(this);
        attachDialogViewTreeOwners(composeView);
        final Dialog[] dialogHolder = new Dialog[1];
        List<TouchFilterPickerRowUiModel> rows = new ArrayList<>();
        for (int i = 0; i < selectableFilters.size(); i++) {
            final ChannelFilter filter = selectableFilters.get(i);
            boolean selected = i == checkedIndex;
            rows.add(new TouchFilterPickerRowUiModel(
                    cleanFilterLabel(filter),
                    getString(R.string.touch_filter_picker_count, countChannelsForFilter(filter)),
                    selected,
                    isProtectedFilter(filter),
                    () -> {
                        Dialog dialog = dialogHolder[0];
                        if (dialog != null) {
                            dismissModalForNextAction(dialog, () -> applySelectedFilterFromPicker(filter));
                        } else {
                            applySelectedFilterFromPicker(filter);
                        }
                    }
            ));
        }
        Runnable close = () -> {
            Dialog dialog = dialogHolder[0];
            if (dialog != null) {
                dialog.dismiss();
            }
        };
        TouchFilterPickerComposeBinder.bind(composeView, new TouchFilterPickerUiModel(
                getString(R.string.touch_filter_picker_title),
                getString(R.string.touch_filter_picker_subtitle),
                getString(R.string.touch_filter_picker_selected, buildCurrentFilterLabel()),
                checkedIndex,
                rows,
                close
        ));
        Dialog dialog = ComposeDialogHost.showFullscreen(this, composeView, this::handleModalDismissed);
        dialogHolder[0] = dialog;
        handleModalShown();
    }

    private String cleanFilterLabel(ChannelFilter filter) {
        String label = decorateProtectedFilterLabel(filter);
        if (label == null || label.trim().isEmpty()) {
            return getString(R.string.filter_all_label);
        }
        return label
                .replace("Plataforma activa: ", "")
                .replace("Plataforma: ", "")
                .replace("Grupo: ", "")
                .trim();
    }

    private int countChannelsForFilter(ChannelFilter filter) {
        if (filter == null) {
            return allChannels.size();
        }
        if ("favorites".equals(filter.key) || filter.type == FILTER_FAVORITES) {
            return buildFavoriteQuickChannels().size();
        }
        int count = 0;
        for (ChannelItem item : allChannels) {
            if (item == null) {
                continue;
            }
            if (filter.type == FILTER_ALL || "all".equals(filter.key)) {
                count++;
            } else if (filter.type == FILTER_PLATFORM && item.platformId == filter.platformId && !item.isVod) {
                count++;
            } else if (filter.type == FILTER_CUSTOM_GROUP && item.customGroups != null) {
                for (String group : item.customGroups) {
                    if (group != null && group.equalsIgnoreCase(filter.groupName)) {
                        count++;
                        break;
                    }
                }
            } else if (filter.type == FILTER_VOD && item.isVod && !item.isAdultVod) {
                count++;
            } else if (filter.type == FILTER_VOD_ADULT && item.isVod && item.isAdultVod) {
                count++;
            }
        }
        return count;
    }

    private void applySelectedFilterFromPicker(ChannelFilter filter) {
        if (filter == null || filter.key == null || filter.key.trim().isEmpty()) {
            return;
        }
        if (isProtectedFilter(filter) && isProtectedContentLocked()) {
            ensureParentalAccessForFilterKey(filter.key, () -> applySelectedFilterFromPicker(filter));
            return;
        }
        syncOverlayCoordinator();
        channelOverlayCoordinator.setSearchQuery("");
        channelOverlayCoordinator.setFavoritesOnly("favorites".equals(filter.key));
        channelOverlayCoordinator.setSelectedFilterKey(filter.key);
        String currentId = overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size()
                ? channels.get(overlayNavigationState.currentIndex).id
                : lastChannelId == null ? "" : lastChannelId;
        channelOverlayCoordinator.refreshVisibleChannels(currentId, currentId);
        syncOverlayStateFromCoordinator();
        persistNavigationState();
        clearOverlaySearchQuery();
        refreshOverlayChannelList();
        updateFilterText();
        updateOverlaySearchState();
        if (channels.isEmpty()) {
            showStatus(getString(R.string.status_no_channels_for_filter));
        } else if (overlayNavigationState.currentIndex < 0) {
            tuneToIndex(overlayNavigationState.selectedOverlayIndex >= 0 ? overlayNavigationState.selectedOverlayIndex : 0, true);
        } else if (overlayNavigationState.selectedOverlayIndex >= 0) {
            scrollOverlayChannelListToPosition(overlayNavigationState.selectedOverlayIndex);
        }
        showStatus(getString(R.string.status_filter_changed, decorateProtectedFilterLabel(filter)));
        scheduleVisibleEpgLoad(OFFLINE_EPG_PRIORITY_DELAY_MS);
        showOverlay();
    }

    private ChannelFilter findAdjacentFilter(int delta) {
        if (filters.isEmpty()) {
            return null;
        }
        int currentFilterIndex = 0;
        for (int i = 0; i < filters.size(); i++) {
            ChannelFilter filter = filters.get(i);
            if (filter != null && filter.key != null && filter.key.equals(overlayNavigationState.selectedFilterKey)) {
                currentFilterIndex = i;
                break;
            }
        }
        boolean hasUserVisibleFilters = hasUserVisibleOverlayFilters();
        int next = currentFilterIndex;
        for (int attempts = 0; attempts < filters.size(); attempts++) {
            next += delta;
            if (next < 0) {
                next = filters.size() - 1;
            }
            if (next >= filters.size()) {
                next = 0;
            }
            ChannelFilter candidate = filters.get(next);
            if (!hasUserVisibleFilters || isUserVisibleOverlayFilter(candidate)) {
                return candidate;
            }
        }
        return filters.get(currentFilterIndex);
    }

    private boolean hasUserVisibleOverlayFilters() {
        for (ChannelFilter filter : filters) {
            if (isUserVisibleOverlayFilter(filter)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUserVisibleOverlayFilter(ChannelFilter filter) {
        return filter != null
                && filter.key != null
                && !filter.key.trim().isEmpty()
                && !"all".equals(filter.key)
                && filter.type != FILTER_ALL;
    }

    private void updateFilterText() {
        updateQuickAccessButtons();
    }

    private void saveFavorites() {
        if (prefs != null) {
            prefs.edit().putStringSet(PREF_FAVORITES, new HashSet<>(favoriteChannelIds)).apply();
        }
        favoriteOrderStore.syncToFavorites(favoriteChannelIds);
    }

    private void toggleFavoriteSelected() {
        if (channels.isEmpty() || overlayNavigationState.selectedOverlayIndex < 0 || overlayNavigationState.selectedOverlayIndex >= channels.size()) {
            return;
        }
        syncOverlayCoordinator();
        boolean added = channelOverlayCoordinator.toggleFavoriteSelected();
        syncOverlayStateFromCoordinator();
        showStatus(getString(added ? R.string.status_favorite_added : R.string.status_favorite_removed));
        String selectedId = overlayNavigationState.selectedOverlayIndex >= 0 && overlayNavigationState.selectedOverlayIndex < channels.size() ? channels.get(overlayNavigationState.selectedOverlayIndex).id : null;
        if (added) {
            favoriteOrderStore.addIfMissing(selectedId);
        } else {
            favoriteOrderStore.remove(selectedId);
        }
        saveFavorites();
        refreshOverlayChannelList();
        updateOverlaySearchState();
        if (overlayNavigationState.selectedOverlayIndex >= 0) {
            scrollOverlayChannelListToPosition(overlayNavigationState.selectedOverlayIndex);
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
            overlayNavigationState.selectedOverlayIndex = index;
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
        refreshOverlayChannelList();
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
        refreshOverlayChannelList();
        updateOverlaySearchState();
        showStatus(getString(added ? R.string.status_favorite_added : R.string.status_favorite_removed));
    }

    private void moveFavoriteSelected(int delta) {
        if (channels.isEmpty() || overlayNavigationState.selectedOverlayIndex < 0 || overlayNavigationState.selectedOverlayIndex >= channels.size()) {
            return;
        }
        ChannelItem selected = channels.get(overlayNavigationState.selectedOverlayIndex);
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
        String currentId = overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size() ? channels.get(overlayNavigationState.currentIndex).id : "";
        channelOverlayCoordinator.refreshVisibleChannels(currentId, selected.id);
        syncOverlayStateFromCoordinator();
        refreshOverlayChannelList();
        if (overlayNavigationState.selectedOverlayIndex >= 0) {
            scrollOverlayChannelListToPosition(overlayNavigationState.selectedOverlayIndex);
        }
        showStatus(getString(delta < 0 ? R.string.status_favorite_moved_up : R.string.status_favorite_moved_down));
        showOverlay();
    }

    private void toggleFavoritesOnlyMode() {
        syncOverlayCoordinator();
        boolean nowFavoritesOnly = channelOverlayCoordinator.toggleFavoritesOnlyMode();
        syncOverlayStateFromCoordinator();
        refreshOverlayChannelList();
        updateOverlaySearchState();

        if (channels.isEmpty() && nowFavoritesOnly) {
            showStatus(getString(R.string.status_favorites_only_empty));
            return;
        }

        if (overlayNavigationState.selectedOverlayIndex >= 0) {
            scrollOverlayChannelListToPosition(overlayNavigationState.selectedOverlayIndex);
        }
        showStatus(getString(nowFavoritesOnly ? R.string.status_favorites_only_on : R.string.status_favorites_only_off));
        showOverlay();
    }

    private boolean isOverlayVisible() {
        boolean visible = channelOverlayCoordinator.isOverlayVisible(channelOverlay);
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.CHANNEL_LIST, visible);
        return visible;
    }

    private boolean isRecordingsPanelVisible() {
        boolean visible = recordingsPanelController.isVisible();
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.RECORDINGS, visible);
        return visible;
    }

    private boolean isMultiViewVisible() {
        boolean visible = multiViewContainer != null && multiViewContainer.getVisibility() == View.VISIBLE;
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.MULTIVIEW, visible);
        return visible;
    }

    private boolean isZapBannerVisible() {
        return zapBannerController.isVisible();
    }

    private void syncOverlaySurfaceVisibilityFromViews() {
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.CHANNEL_LIST, channelOverlayCoordinator.isOverlayVisible(channelOverlay));
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.RECORDINGS, recordingsPanelController.isVisible());
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.MULTIVIEW, multiViewContainer != null && multiViewContainer.getVisibility() == View.VISIBLE);
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.TOUCH_CONTROLS, touchControlsBar != null && touchControlsBar.getVisibility() == View.VISIBLE, false);
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.TIMESHIFT, timeshiftBarContainer != null && timeshiftBarContainer.getVisibility() == View.VISIBLE, false);
        if (touchControlsBar != null && touchControlsBar.getVisibility() == View.VISIBLE) {
            overlaySurfaceState.focusSurface(touchControlsFocusState.timeshiftFocused() ? OfflineOverlayState.Surface.TIMESHIFT : OfflineOverlayState.Surface.TOUCH_CONTROLS);
        }
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.ZAP_BANNER, isZapBannerVisible());
    }

    private boolean hasBlockingOverlaySurfaceVisible() {
        syncOverlaySurfaceVisibilityFromViews();
        return overlaySurfaceState.hasBlockingPanelVisible();
    }

    private void showOverlay() {
        clearQuickSearchOverlay();
        hideZapBanner();
        hideRecordingsPanel();
        closeMultiView();
        if (touchDeviceMode) {
            if (touchControlsController != null) {
                touchControlsController.cancelTimers();
            }
            if (touchControlsBar != null) touchControlsBar.setVisibility(View.GONE);
            if (touchHomeHub != null) touchHomeHub.setVisibility(View.GONE);
            if (timeshiftBarContainer != null) timeshiftBarContainer.setVisibility(View.GONE);
            overlaySurfaceState.clearTransientPlaybackSurfaces();
        }
        updateOverlayPanel();
        updateOverlaySearchState();
        channelOverlayCoordinator.showOverlay(channelOverlay, uiHandler, hideOverlayRunnable, touchDeviceMode ? 0L : OVERLAY_HIDE_MS);
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.CHANNEL_LIST, true);
    }

    private void hideOverlay() {
        uiHandler.removeCallbacks(hideOverlayRunnable);
        clearOverlaySearchQuery();
        channelOverlayCoordinator.hideOverlay(channelOverlay);
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.CHANNEL_LIST, false);
        if (touchDeviceMode) {
            if (touchControlsController != null) {
                touchControlsController.cancelTimers();
            }
            if (touchControlsBar != null) touchControlsBar.setVisibility(View.GONE);
            if (touchHomeHub != null) touchHomeHub.setVisibility(View.GONE);
            if (timeshiftBarContainer != null) timeshiftBarContainer.setVisibility(View.GONE);
            overlaySurfaceState.clearTransientPlaybackSurfaces();
        }
    }

    private void hideZapBanner() {
        zapBannerController.hide();
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.ZAP_BANNER, false);
    }

    private void showRecordingsPanel(RecordingsRepository.RecordingsResult result) {
        recordingsPanelController.show(result);
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.RECORDINGS, true);
    }

    private void showRecordingsPanel(RecordingsRepository.RecordingsResult result, String preferredId) {
        recordingsPanelController.show(result, preferredId);
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.RECORDINGS, true);
    }

    private void hideRecordingsPanel() {
        recordingsPanelController.hide();
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.RECORDINGS, false);
    }

    private void scheduleRecordingsAutoRefresh() {
        recordingsPanelController.scheduleAutoRefresh();
    }

    private RecordingsPanelController.Host createRecordingsPanelHost() {
        return new RecordingsPanelController.Host() {
            @Override
            public String string(int resId) {
                return getString(resId);
            }

            @Override
            public String string(int resId, Object... args) {
                return getString(resId, args);
            }

            @Override
            public String summary(RecordingsRepository.RecordingsResult result) {
                return buildRecordingsSummary(result);
            }

            @Override
            public String hint() {
                return buildRecordingsHint();
            }

            @Override
            public String title(RecordingsRepository.RecordingItem item) {
                return buildRecordingTitle(item);
            }

            @Override
            public String meta(RecordingsRepository.RecordingItem item) {
                return buildRecordingMeta(item);
            }

            @Override
            public int metaColor(RecordingsRepository.RecordingItem item) {
                return recordingMetaColor(item);
            }

            @Override
            public String statusLabel(RecordingsRepository.RecordingItem item) {
                return buildRecordingStatusLabel(item);
            }

            @Override
            public int statusBadgeColor(RecordingsRepository.RecordingItem item) {
                return recordingStatusBadgeColor(item);
            }

            @Override
            public void bindPoster(ImageView imageView, String posterUrl) {
                bindRecordingPoster(imageView, posterUrl);
            }

            @Override
            public void switchMode(boolean scheduledMode) {
                switchRecordingsMode(scheduledMode);
            }

            @Override
            public void refreshData() {
                refreshRecordingsPanel();
            }

            @Override
            public void playRecording(RecordingsRepository.RecordingItem item, String basePath) {
                MainActivity.this.playRecording(item, basePath);
            }

            @Override
            public void showRecordingActionsDialog() {
                MainActivity.this.showRecordingActionsDialog();
            }

            @Override
            public void showRecordingsDialog(RecordingsRepository.RecordingsResult result) {
                MainActivity.this.showRecordingsDialog(result);
            }

            @Override
            public void onBeforeShowPanel() {
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
            }
        };
    }

    private boolean isQuickSearchVisible() {
        return quickSearchController.isVisible();
    }

    private void handleQuickSearchCharacter(char value) {
        quickSearchController.handleCharacter(value);
    }

    private void deleteQuickSearchCharacter() {
        quickSearchController.deleteCharacter();
    }

    private void moveQuickSearchSelection(int delta) {
        quickSearchController.moveSelection(delta);
    }

    private void tuneQuickSearchSelection() {
        quickSearchController.tuneSelection();
    }

    private void clearQuickSearchOverlay() {
        quickSearchController.clear();
    }

    private QuickSearchController.Host createQuickSearchHost() {
        return new QuickSearchController.Host() {
            @Override
            public String string(int resId) {
                return getString(resId);
            }

            @Override
            public String string(int resId, Object... args) {
                return getString(resId, args);
            }

            @Override
            public List<ChannelItem> searchChannels(String query, int limit) {
                return MainActivity.this.searchChannels(query, limit);
            }

            @Override
            public void tuneChannelById(String channelId) {
                MainActivity.this.tuneChannelById(channelId);
            }
        };
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
        recordingsPanelController.moveSelection(delta);
    }

    private void moveRecordingsHeaderFocus(int delta) {
        recordingsPanelController.moveHeaderFocus(delta);
    }

    private boolean activateRecordingsHeaderFocus() {
        return recordingsPanelController.activateHeaderFocus();
    }

    private void playSelectedRecording() {
        recordingsPanelController.playSelected();
    }

    private void updateRecordingsDetailPanel() {
        recordingsPanelController.refreshSurface();
    }

    private void refreshRecordingsPanelList() {
        recordingsPanelController.refreshSurface();
    }

    private void refreshRecordingsPanelSurface() {
        recordingsPanelController.refreshSurface();
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
        overlayUiController.showHdrBadge(label);
    }

    private void showStartupLoading(String step, String detail) {
        overlayUiController.showStartupLoading(step, detail);
    }

    private void updateStartupLoading(String step, String detail) {
        overlayUiController.updateStartupLoading(step, detail);
    }

    private void showLoading(String title, String step, String detail) {
        overlayUiController.showLoading(title, step, detail);
    }

    private void updateLoading(String title, String step, String detail) {
        overlayUiController.updateLoading(title, step, detail);
    }

    private void hideStartupLoading() {
        overlayUiController.hideStartupLoading();
    }

    private void startVodLoadingOverlay(ChannelItem item) {
        if (item == null || !item.isVod || item.id == null || item.id.trim().isEmpty()) {
            return;
        }
        vodLoadingChannelId = item.id.trim();
        vodLoadingStartedAtMs = System.currentTimeMillis();
        uiHandler.removeCallbacks(vodLoadingProgressRunnable);
        updateVodLoadingState(
                item,
                getVodLoadingTitle(item),
                isU7dReplayItem(item) ? getString(R.string.u7d_loading_step_manifest) : getString(R.string.vod_loading_step_preparing),
                isU7dReplayItem(item) ? getString(R.string.u7d_loading_detail_manifest) : getString(R.string.vod_loading_detail_preparing, displayName(item))
        );
        postUiDelayedIfAlive(vodLoadingProgressRunnable, 4_000L);
    }

    private void updateVodLoadingOverlay() {
        ChannelItem current = getCurrentPlaybackChannelItem();
        if (current == null || current.id == null || vodLoadingChannelId == null
                || !current.id.equals(vodLoadingChannelId) || !current.isVod) {
            stopVodLoadingOverlay("");
            return;
        }
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - vodLoadingStartedAtMs);
        boolean u7dReplay = isU7dReplayItem(current);
        boolean movistarVod = isMovistarVodItem(current);
        String title = getVodLoadingTitle(current);
        String step;
        String detail;
        long nextDelayMs;
        if (elapsedMs < 12_000L) {
            step = u7dReplay
                    ? getString(R.string.u7d_loading_step_manifest)
                    : movistarVod
                    ? getString(R.string.vod_loading_step_manifest)
                    : getString(R.string.vod_loading_step_stream);
            detail = u7dReplay
                    ? getString(R.string.u7d_loading_detail_manifest)
                    : movistarVod
                    ? getString(R.string.vod_loading_detail_manifest)
                    : getString(R.string.vod_loading_detail_stream);
            nextDelayMs = 8_000L;
        } else if (elapsedMs < 30_000L) {
            step = u7dReplay
                    ? getString(R.string.u7d_loading_step_drm)
                    : movistarVod
                    ? getString(R.string.vod_loading_step_drm)
                    : getString(R.string.vod_loading_step_buffering);
            detail = u7dReplay
                    ? getString(R.string.u7d_loading_detail_drm)
                    : movistarVod
                    ? getString(R.string.vod_loading_detail_drm)
                    : getString(R.string.vod_loading_detail_buffering);
            nextDelayMs = 10_000L;
        } else if (elapsedMs < 55_000L) {
            step = u7dReplay
                    ? getString(R.string.u7d_loading_step_backend)
                    : movistarVod
                    ? getString(R.string.vod_loading_step_backend)
                    : getString(R.string.vod_loading_step_waiting);
            detail = u7dReplay
                    ? getString(R.string.u7d_loading_detail_backend)
                    : movistarVod
                    ? getString(R.string.vod_loading_detail_backend)
                    : getString(R.string.vod_loading_detail_waiting);
            nextDelayMs = 12_000L;
        } else {
            step = u7dReplay ? getString(R.string.u7d_loading_step_slow) : getString(R.string.vod_loading_step_slow);
            detail = u7dReplay ? getString(R.string.u7d_loading_detail_slow) : getString(R.string.vod_loading_detail_slow);
            nextDelayMs = 15_000L;
        }
        updateVodLoadingState(current, title, step, detail);
        postUiDelayedIfAlive(vodLoadingProgressRunnable, nextDelayMs);
    }

    private void updateVodLoadingState(ChannelItem item, String title, String step, String detail) {
        vodLoadingKind = isU7dReplayItem(item) ? "u7d" : isMovistarVodItem(item) ? "movistar_vod" : "vod";
        vodLoadingTitle = title == null ? "" : title.trim();
        vodLoadingStep = step == null ? "" : step.trim();
        vodLoadingDetail = detail == null ? "" : detail.trim();
        updateLoading(vodLoadingTitle, vodLoadingStep, vodLoadingDetail);
    }

    private void stopVodLoadingOverlay(String channelId) {
        boolean hadVodLoading = vodLoadingChannelId != null && !vodLoadingChannelId.isEmpty();
        if (channelId != null && !channelId.trim().isEmpty()
                && vodLoadingChannelId != null && !vodLoadingChannelId.isEmpty()
                && !channelId.trim().equals(vodLoadingChannelId)) {
            return;
        }
        uiHandler.removeCallbacks(vodLoadingProgressRunnable);
        vodLoadingChannelId = "";
        vodLoadingKind = "";
        vodLoadingTitle = "";
        vodLoadingStep = "";
        vodLoadingDetail = "";
        vodLoadingStartedAtMs = 0L;
        if (hadVodLoading) {
            hideStartupLoading();
        }
    }

    private boolean isVodLoadingActive() {
        return vodLoadingChannelId != null && !vodLoadingChannelId.trim().isEmpty() && vodLoadingStartedAtMs > 0L;
    }

    private long currentVodLoadingElapsedMs() {
        return isVodLoadingActive() ? Math.max(0L, System.currentTimeMillis() - vodLoadingStartedAtMs) : 0L;
    }

    private String getVodLoadingTitle(ChannelItem item) {
        if (isU7dReplayItem(item)) {
            return getString(R.string.u7d_loading_title);
        }
        if (isMovistarVodItem(item)) {
            return getString(R.string.vod_loading_title_movistar);
        }
        return getString(R.string.vod_loading_title);
    }

    private boolean isMovistarVodItem(ChannelItem item) {
        if (item == null || !item.isVod) {
            return false;
        }
        String source = safeLower(item.platformName) + " " + safeLower(item.vodFilterKey) + " " + safeLower(item.playUrl);
        return source.contains("movistar");
    }

    private void showStatus(String text) {
        overlayUiController.showStatus(text);
    }

    private boolean isRedundantPlaybackStatus(String text) {
        ChannelItem current = getCurrentPlaybackChannelItem();
        if (current == null || current.isVod || text == null) {
            return false;
        }
        String value = text.trim();
        String channelName = displayName(current).trim();
        if (value.equals(channelName)) {
            return true;
        }
        return value.equals(getString(R.string.status_channel_widevine, channelName))
                || value.equals(getString(R.string.status_channel_compat, channelName));
    }

    private void showError(String reason) {
        overlayUiController.showError(reason);
    }

    private OverlayUiController.Host createOverlayUiHost() {
        return new OverlayUiController.Host() {
            @Override
            public boolean isRedundantPlaybackStatus(String text) {
                return MainActivity.this.isRedundantPlaybackStatus(text);
            }

            @Override
            public void onOverlayPanelInvalidated() {
                MainActivity.this.updateOverlayPanel();
            }

            @Override
            public ChannelItem currentPlaybackChannelItem() {
                return MainActivity.this.getCurrentPlaybackChannelItem();
            }

            @Override
            public String displayName(ChannelItem item) {
                return MainActivity.this.displayName(item);
            }

            @Override
            public String baseUrl() {
                return baseUrl;
            }
        };
    }

    private void hideError() {
        overlayUiController.hideError();
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
            public boolean isZapBannerVisible() {
                return MainActivity.this.isZapBannerVisible();
            }

            @Override
            public boolean isTvTimeshiftHudActive() {
                return MainActivity.this.isTvTimeshiftHudActive();
            }

            @Override
            public boolean isTouchControlsVisible() {
                return MainActivity.this.isTouchControlsVisibleForRemote();
            }

            @Override
            public boolean isTouchControlsTimeshiftFocused() {
                return MainActivity.this.isTouchControlsTimeshiftFocused();
            }

            @Override
            public boolean canResumeTimeshiftLive() {
                return MainActivity.this.canResumeLivePlayback();
            }

            @Override
            public boolean resumeTimeshiftLive() {
                return playerController != null && playerController.resumeTimeshiftLive();
            }

            @Override
            public boolean canSeekTimeshiftBack() {
                return MainActivity.this.canSeekPlaybackBack();
            }

            @Override
            public boolean canSeekTimeshiftForward() {
                return MainActivity.this.canSeekPlaybackForward();
            }

            @Override
            public boolean seekTimeshiftBack() {
                return MainActivity.this.seekTouchControlsBack();
            }

            @Override
            public boolean seekTimeshiftForward() {
                return MainActivity.this.seekTouchControlsForward();
            }

            @Override
            public boolean isPlayingRecordingWithReturnTarget() {
                return playerController != null && playerController.isPlayingRecording() && currentPlaybackRecordingId != null;
            }

            @Override
            public boolean hasSeekablePlayback() {
                return playerController != null && (playerController.getPlaybackSeekState() != null || getCurrentU7dSeekState() != null);
            }

            @Override
            public boolean isTouchDeviceMode() {
                return touchDeviceMode;
            }

            @Override
            public boolean hasSelectedOverlayChannel() {
                return overlayNavigationState.selectedOverlayIndex >= 0 && overlayNavigationState.selectedOverlayIndex < channels.size();
            }

            @Override
            public boolean hasCurrentChannel() {
                return getCurrentPlaybackChannelItem() != null
                        || (overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size());
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
            public void moveRecordingsHeaderFocus(int delta) {
                MainActivity.this.moveRecordingsHeaderFocus(delta);
            }

            @Override
            public boolean activateRecordingsHeaderFocus() {
                return MainActivity.this.activateRecordingsHeaderFocus();
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
                MainActivity.this.openTimelineGuideForCurrentPlayback();
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
            public void hideTouchControls() {
                MainActivity.this.hideTouchControlsForRemote();
            }

            @Override
            public void moveTouchControlsFocus(int delta) {
                MainActivity.this.moveTouchControlsFocus(delta);
            }

            @Override
            public void focusTouchControlsTimeshift() {
                MainActivity.this.focusTouchControlsTimeshift();
            }

            @Override
            public void focusTouchControlsActions() {
                MainActivity.this.focusTouchControlsActions();
            }

            @Override
            public void activateTouchControlsFocus() {
                MainActivity.this.activateTouchControlsFocus();
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
            public void hideZapBanner() {
                MainActivity.this.hideZapBanner();
            }

            @Override
            public void moveZapBannerSelection(int delta) {
                MainActivity.this.moveZapBannerSelection(delta);
            }

            @Override
            public void activateZapBannerSelection() {
                MainActivity.this.activateZapBannerSelection();
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
            public void showTouchControlsTemporarily() {
                MainActivity.this.showTouchControlsTemporarily();
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
                MainActivity.this.tuneToIndex(overlayNavigationState.selectedOverlayIndex, true);
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
                if (MainActivity.this.isOverlayVisible() && overlayNavigationState.selectedOverlayIndex >= 0 && overlayNavigationState.selectedOverlayIndex < channels.size()) {
                    MainActivity.this.createScheduleFromEndpoint(channels.get(overlayNavigationState.selectedOverlayIndex), false);
                } else if (overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size()) {
                    MainActivity.this.createScheduleFromEndpoint(channels.get(overlayNavigationState.currentIndex), false);
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
    @SuppressLint("RestrictedApi")
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

    private boolean submitIoTask(String label, Runnable task) {
        return submitExecutorTask(ioExecutor, label, task);
    }

    private boolean submitEpgTask(String label, Runnable task) {
        return submitExecutorTask(epgExecutor, label, task);
    }

    private boolean submitCatalogTask(String label, Runnable task) {
        return submitExecutorTask(catalogLoadExecutor, label, task);
    }

    private boolean submitControlTask(String label, Runnable task) {
        return submitExecutorTask(controlExecutor, label, task);
    }

    private boolean postUiIfAlive(Runnable task) {
        if (task == null || activityDestroyed) {
            return false;
        }
        return uiHandler.post(() -> {
            if (!activityDestroyed) {
                task.run();
            }
        });
    }

    private boolean postUiDelayedIfAlive(Runnable task, long delayMs) {
        if (task == null || activityDestroyed) {
            return false;
        }
        // Keep the original Runnable identity so removeCallbacks(task) continues to work.
        // onDestroy() clears the handler queue and prevents any later scheduling.
        return uiHandler.postDelayed(task, delayMs);
    }

    private boolean submitExecutorTask(ExecutorService executor, String label, Runnable task) {
        if (executor == null || task == null) {
            return false;
        }
        if (executor.isShutdown() || executor.isTerminated()) {
            Log.w(TAG, "executor task skipped after shutdown label=" + label);
            return false;
        }
        try {
            executor.execute(() -> {
                if (!activityDestroyed && !Thread.currentThread().isInterrupted()) {
                    task.run();
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            Log.w(TAG, "executor task rejected label=" + label, e);
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        activityDestroyed = true;
        rememberCurrentVodPosition();
        rememberCurrentRecordingPosition();
        stopPlaybackHeartbeat("stop");
        if (touchControlsController != null) {
            touchControlsController.cancelTimers();
        }
        uiHandler.removeCallbacksAndMessages(null);
        ioExecutor.shutdownNow();
        epgExecutor.shutdownNow();
        interactiveExecutor.shutdownNow();
        controlExecutor.shutdownNow();
        catalogLoadExecutor.shutdownNow();
        if (playerController != null) {
            playerController.release();
            playerController = null;
        }
        super.onDestroy();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.get(this).trimMemory(level);
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            channelLogoCache.evictAll();
            streamInfoByChannelId.clear();
        }
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL && isMultiViewVisible()) {
            closeMultiView();
            showStatus(getString(R.string.status_multiview_closed_low_memory));
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        channelLogoCache.evictAll();
        streamInfoByChannelId.clear();
        Glide.get(this).clearMemory();
        if (isMultiViewVisible()) {
            closeMultiView();
        }
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
                channelItem.directPlayback,
                channelItem.isVod,
                channelItem.playbackProfile
        );
    }

    private String resolvePlaybackModeForRequest(ChannelItem channelItem) {
        return PlaybackModeResolver.resolve(channelItem, playbackModeStore, temporaryPlaybackModesByChannelId, learnedPlaybackModesByChannelId, new PlaybackModeResolver.Host() {
            @Override
            public boolean standaloneMode() {
                return BuildConfig.STANDALONE_MODE;
            }

            @Override
            public boolean playbackRepairEnabled() {
                return MainActivity.this.playbackRepairEnabled;
            }

            @Override
            public boolean proxyManifestProfile(ChannelItem item) {
                return isProxyManifestProfile(item);
            }

            @Override
            public void saveLearnedModes() {
                saveLearnedPlaybackModes();
            }
        });
    }

    private boolean isProxyManifestProfile(ChannelItem channelItem) {
        return channelItem != null && "proxy_manifest".equals(safeLower(channelItem.playbackProfile));
    }

    private ChannelItem getCurrentPlaybackChannelItem() {
        if (currentPlaybackTransientItem != null) {
            return currentPlaybackTransientItem;
        }
        String playbackChannelId = playerController == null ? "" : playerController.getCurrentRequestChannelId();
        if (!playbackChannelId.isEmpty()) {
            ChannelItem playbackChannel = findChannelItemById(playbackChannelId);
            if (playbackChannel != null) {
                return playbackChannel;
            }
        }
        if (overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size()) {
            return channels.get(overlayNavigationState.currentIndex);
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
        overlayNavigationState.currentIndex = findChannelIndexById(channelItem.id);
        if (overlayNavigationState.currentIndex >= 0) {
            overlayNavigationState.selectedOverlayIndex = overlayNavigationState.currentIndex;
            refreshOverlayChannelList();
            tuneToIndex(overlayNavigationState.currentIndex, true);
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
        TvOptionsMenuModel menu = PlaybackModeUiFactory.buildPermanent(channelItem, buildPlaybackModeUiHost());
        showTvOptionsDialog(R.string.title_playback_mode, displayName(channelItem), menu.options, menu.actions);
    }

    private void showTemporaryPlaybackModeDialog(ChannelItem channelItem) {
        if (channelItem == null) {
            return;
        }
        TvOptionsMenuModel menu = PlaybackModeUiFactory.buildTemporary(channelItem, buildPlaybackModeUiHost());
        showTvOptionsDialog(R.string.title_playback_mode_temporary, displayName(channelItem), menu.options, menu.actions);
    }

    private PlaybackModeUiFactory.Host buildPlaybackModeUiHost() {
        return new PlaybackModeUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String text(int resId, Object... args) {
                return getString(resId, args);
            }

            @Override
            public String[] modeOptions() {
                return getResources().getStringArray(R.array.playback_mode_options);
            }

            @Override
            public String currentPermanentMode(ChannelItem item) {
                return playbackModeStore == null || item == null ? PlaybackModeStore.MODE_AUTO : playbackModeStore.getMode(item.id);
            }

            @Override
            public String currentTemporaryMode(ChannelItem item) {
                return item == null ? PlaybackModeStore.MODE_AUTO : temporaryPlaybackModesByChannelId.getOrDefault(item.id, PlaybackModeStore.MODE_AUTO);
            }

            @Override
            public void setPermanentMode(ChannelItem item, String mode, String label) {
                if (playbackModeStore == null || item == null) {
                    return;
                }
                playbackModeStore.setMode(item.id, mode);
                showStatus(getString(R.string.status_playback_mode_changed, label));
                retryIfCurrentPlayback(item);
            }

            @Override
            public void setTemporaryMode(ChannelItem item, String mode, String label) {
                if (item == null) {
                    return;
                }
                playbackRecoveryCoordinator.setTemporaryMode(item.id, mode);
                showStatus(getString(R.string.status_playback_mode_temporary_changed, label));
                retryIfCurrentPlayback(item);
            }
        };
    }

    private void retryIfCurrentPlayback(ChannelItem channelItem) {
        ChannelItem currentPlaybackChannel = getCurrentPlaybackChannelItem();
        if (currentPlaybackChannel != null && channelItem != null && channelItem.id.equals(currentPlaybackChannel.id)) {
            retryCurrentPlayback();
        }
    }

    private void showPersonalListsDialog(ChannelItem channelItem) {
        if (channelItem == null || channelCollectionStore == null) {
            return;
        }
        List<ChannelCollectionStore.ChannelCollection> collections = channelCollectionStore.getCollections();
        boolean[] checked = new boolean[collections.size()];
        showPersonalListsDialog(channelItem, collections, checked, true);
    }

    private void showPersonalListsDialog(ChannelItem channelItem, List<ChannelCollectionStore.ChannelCollection> collections, boolean[] checked, boolean initialize) {
        if (channelItem == null || channelCollectionStore == null || collections == null || checked == null) {
            return;
        }
        TvOptionsMenuModel model = ChannelProfileUiFactory.buildPersonalLists(channelItem, collections, checked, initialize, buildChannelProfileUiHost());
        showTvOptionsDialog(R.string.title_personal_lists, model.message, model.options, model.actions);
    }

    private void showChannelProfileDialog(ChannelItem channelItem) {
        if (channelItem == null || channelProfileStore == null) {
            return;
        }
        TvOptionsMenuModel model = ChannelProfileUiFactory.buildProfile(channelItem, buildChannelProfileUiHost());
        showTvOptionsDialog(displayName(channelItem), model.message, model.options, model.actions);
    }

    private ChannelProfileUiFactory.Host buildChannelProfileUiHost() {
        return new ChannelProfileUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String displayName(ChannelItem item) {
                return MainActivity.this.displayName(item);
            }

            @Override
            public boolean contains(ChannelCollectionStore.ChannelCollection collection, ChannelItem item) {
                return channelCollectionStore != null && collection != null && item != null && channelCollectionStore.contains(collection.key, item.id);
            }

            @Override
            public boolean hasAlias(ChannelItem item) {
                return channelProfileStore != null && item != null && channelProfileStore.hasAlias(item.id);
            }

            @Override
            public boolean hasTag(ChannelItem item) {
                return channelProfileStore != null && item != null && channelProfileStore.hasTag(item.id);
            }

            @Override
            public boolean isHidden(ChannelItem item) {
                return channelProfileStore != null && item != null && channelProfileStore.isHidden(item.id);
            }

            @Override
            public void openPersonalLists(ChannelItem item, List<ChannelCollectionStore.ChannelCollection> collections, boolean[] checked) {
                showPersonalListsDialog(item, collections, checked, false);
            }

            @Override
            public void savePersonalLists(ChannelItem item, List<ChannelCollectionStore.ChannelCollection> collections, boolean[] checked) {
                if (channelCollectionStore == null || item == null || collections == null || checked == null) {
                    return;
                }
                for (int i = 0; i < collections.size(); i++) {
                    ChannelCollectionStore.ChannelCollection collection = collections.get(i);
                    if (collection != null && i < checked.length) {
                        channelCollectionStore.setMembership(collection.key, item.id, checked[i]);
                    }
                }
                refreshLocalChannelFilters(item.id);
                showStatus(getString(R.string.status_personal_lists_updated));
            }

            @Override
            public void openAlias(ChannelItem item) {
                showChannelAliasDialog(item);
            }

            @Override
            public void clearAlias(ChannelItem item) {
                if (channelProfileStore == null || item == null) {
                    return;
                }
                channelProfileStore.setAlias(item.id, "");
                refreshLocalChannelFilters(item.id);
                showStatus(getString(R.string.status_channel_alias_cleared));
            }

            @Override
            public void openTag(ChannelItem item) {
                showChannelTagDialog(item);
            }

            @Override
            public void clearTag(ChannelItem item) {
                if (channelProfileStore == null || item == null) {
                    return;
                }
                channelProfileStore.setTag(item.id, "");
                refreshLocalChannelFilters(item.id);
                showStatus(getString(R.string.status_channel_tag_cleared));
            }

            @Override
            public void setHidden(ChannelItem item, boolean hidden) {
                if (channelProfileStore == null || item == null) {
                    return;
                }
                channelProfileStore.setHidden(item.id, hidden);
                refreshLocalChannelFilters(item.id);
                showStatus(getString(hidden ? R.string.status_channel_hidden : R.string.status_channel_unhidden));
            }

            @Override
            public void setStartup(ChannelItem item) {
                if (item == null) {
                    return;
                }
                saveLastChannelId(item.id);
                showStatus(getString(R.string.status_channel_startup_set));
            }

            @Override
            public void openTemporaryPlaybackMode(ChannelItem item) {
                showTemporaryPlaybackModeDialog(item);
            }
        };
    }

    private void showChannelTagDialog(ChannelItem channelItem) {
        if (channelItem == null || channelProfileStore == null) {
            return;
        }
        showTvTextInputPanel(new TvTextInputPanelUiModel(
                getString(R.string.channel_profile_tag),
                displayName(channelItem),
                getString(android.R.string.ok),
                getString(R.string.dialog_cancel),
                "",
                java.util.Collections.singletonList(new TvTextInputFieldUiModel(getString(R.string.channel_profile_tag_hint), channelProfileStore.getTag(channelItem.id), false, false)),
                values -> {
                    String value = values == null || values.isEmpty() ? "" : values.get(0);
                    channelProfileStore.setTag(channelItem.id, value);
                    refreshLocalChannelFilters(channelItem.id);
                    showStatus(getString(value.trim().isEmpty() ? R.string.status_channel_tag_cleared : R.string.status_channel_tag_updated));
                },
                null,
                null
        ));
    }

    private void showChannelAliasDialog(ChannelItem channelItem) {
        if (channelItem == null || channelProfileStore == null) {
            return;
        }
        showTvTextInputPanel(new TvTextInputPanelUiModel(
                getString(R.string.channel_profile_alias),
                displayName(channelItem),
                getString(android.R.string.ok),
                getString(R.string.dialog_cancel),
                "",
                java.util.Collections.singletonList(new TvTextInputFieldUiModel(getString(R.string.channel_profile_alias_hint), channelProfileStore.getDisplayName(channelItem.id, channelItem.name), false, false)),
                values -> {
                    String value = values == null || values.isEmpty() ? "" : values.get(0);
                    channelProfileStore.setAlias(channelItem.id, value);
                    refreshLocalChannelFilters(channelItem.id);
                    showStatus(getString(value.trim().isEmpty() ? R.string.status_channel_alias_cleared : R.string.status_channel_alias_updated));
                },
                null,
                null
        ));
    }

    private void refreshLocalChannelFilters(String selectedId) {
        if (channelOverlayCoordinator == null) {
            return;
        }
        syncOverlayCoordinator();
        channelOverlayCoordinator.refreshLocalFilters();
        String currentId = (overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size()) ? channels.get(overlayNavigationState.currentIndex).id : lastChannelId;
        channelOverlayCoordinator.refreshVisibleChannels(currentId, selectedId == null ? currentId : selectedId);
        syncOverlayStateFromCoordinator();
        refreshOverlayChannelList();
        updateFilterText();
        updateOverlaySearchState();
        if (!channels.isEmpty() && overlayNavigationState.selectedOverlayIndex >= 0) {
            scrollOverlayChannelListToPosition(overlayNavigationState.selectedOverlayIndex);
        }
        showOverlay();
    }

    private void focusOverlaySearchInput() {
        overlaySearchFocusRequestToken++;
        updateQuickAccessButtons();
        showTouchControlsTemporarily();
        uiHandler.removeCallbacks(hideOverlayRunnable);
    }

    private void hideOverlaySearchKeyboard() {
        overlaySearchClearFocusRequestToken++;
        updateQuickAccessButtons();
    }

    private void clearOverlaySearchQuery() {
        syncOverlayCoordinator();
        channelOverlayCoordinator.setSearchQuery("");
        hideOverlaySearchKeyboard();
        updateOverlaySearchState();
    }

    private void applyOverlaySearchQuery(String query) {
        syncOverlayCoordinator();
        channelOverlayCoordinator.setSearchQuery(query);
        String currentId = (overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size()) ? channels.get(overlayNavigationState.currentIndex).id : lastChannelId;
        String selectedId = (overlayNavigationState.selectedOverlayIndex >= 0 && overlayNavigationState.selectedOverlayIndex < channels.size()) ? channels.get(overlayNavigationState.selectedOverlayIndex).id : currentId;
        channelOverlayCoordinator.refreshVisibleChannels(currentId, selectedId);
        syncOverlayStateFromCoordinator();
        refreshOverlayChannelList();
        updateFilterText();
        updateOverlaySearchState();
        if (!channels.isEmpty() && overlayNavigationState.selectedOverlayIndex >= 0) {
            scrollOverlayChannelListToPosition(overlayNavigationState.selectedOverlayIndex);
        }
    }

    private void updateOverlaySearchState() {
        refreshOverlayChannelList();
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
        String finalFilterKey = filterKey;
        ensureParentalAccessForFilterKey(finalFilterKey, () -> {
            channelOverlayCoordinator.setSelectedFilterKey(finalFilterKey);
            String currentId = lastChannelId == null ? "" : lastChannelId;
            channelOverlayCoordinator.refreshVisibleChannels(currentId, currentId);
            syncOverlayStateFromCoordinator();
            clearOverlaySearchQuery();
            refreshOverlayChannelList();
            updateFilterText();
            updateOverlaySearchState();
            if (!channels.isEmpty() && overlayNavigationState.currentIndex < 0) {
                tuneToIndex(overlayNavigationState.selectedOverlayIndex >= 0 ? overlayNavigationState.selectedOverlayIndex : 0, true);
            }
            showOverlay();
        });
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

    private String formatOverlayCountLabel(int labelRes, int count) {
        return ChannelOverlayUi.buildQuickCountLabel(this, labelRes, count);
    }

    private OverlayControlsUiModel buildOverlayControlsModel(boolean tvActive, boolean vodActive, boolean adultActive) {
        return OverlayControlsUiFactory.build(new OverlayControlsUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String text(int resId, Object... args) {
                return getString(resId, args);
            }

            @Override
            public String quickCountLabel(int labelRes, int count) {
                return formatOverlayCountLabel(labelRes, count);
            }

            @Override
            public String currentFilterLabel() {
                return currentOverlayFilterLabel();
            }

            @Override
            public String searchQuery() {
                return channelOverlayCoordinator == null ? "" : channelOverlayCoordinator.getSearchQuery();
            }

            @Override
            public int searchFocusRequestToken() {
                return overlaySearchFocusRequestToken;
            }

            @Override
            public int searchClearFocusRequestToken() {
                return overlaySearchClearFocusRequestToken;
            }

            @Override
            public boolean tvActive() {
                return tvActive;
            }

            @Override
            public boolean vodActive() {
                return vodActive;
            }

            @Override
            public boolean adultActive() {
                return adultActive;
            }

            @Override
            public boolean showVodTarget() {
                return shouldShowGenericVodQuickTarget(false);
            }

            @Override
            public boolean showAdultTarget() {
                return shouldShowGenericVodQuickTarget(true);
            }

            @Override
            public boolean adultTargetProtected() {
                return currentOfflinePermissions != null && currentOfflinePermissions.protectAdultVod;
            }

            @Override
            public boolean recordingsEnabled() {
                return !isOfflineRecordingsDisabled();
            }

            @Override
            public boolean recordingsVisible() {
                return isRecordingsPanelVisible();
            }

            @Override
            public boolean favoritesSelected() {
                return overlayNavigationState.favoritesOnly || "favorites".equals(overlayNavigationState.selectedFilterKey);
            }

            @Override
            public int countForTarget(String targetKey) {
                return countItemsForQuickTarget(targetKey);
            }

            @Override
            public int recentCount() {
                return buildRecentQuickChannels().size();
            }

            @Override
            public int favoriteCount() {
                return buildFavoriteQuickChannels().size();
            }

            @Override
            public void keepVisible() {
                showTouchControlsTemporarily();
            }

            @Override
            public void cycleFilter(int delta) {
                MainActivity.this.cycleFilter(delta);
            }

            @Override
            public void focusSearch() {
                focusOverlaySearchInput();
            }

            @Override
            public void applyQuickTarget(String targetKey) {
                applyQuickOverlayTarget(targetKey);
            }

            @Override
            public void openRecordings() {
                openRecordingsBrowser();
            }

            @Override
            public void openRecentChannels() {
                showRecentChannelsQuickDialog();
            }

            @Override
            public void openFavoriteChannels() {
                showFavoriteChannelsQuickDialog();
            }

            @Override
            public void toggleFavoritesOnly() {
                toggleFavoritesOnlyMode();
            }

            @Override
            public void applySearchQuery(String query) {
                applyOverlaySearchQuery(query);
            }

            @Override
            public void onSearchFocused() {
                uiHandler.removeCallbacks(hideOverlayRunnable);
            }

            @Override
            public String decorateProtectedLabel(String label, boolean locked) {
                return MainActivity.this.decorateProtectedLabel(label, locked);
            }
        });
    }

    private OverlayChannelListUiModel buildOverlayChannelListUiModel() {
        return OverlayChannelListUiFactory.build(new OverlayChannelListUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String text(int resId, Object... args) {
                return getString(resId, args);
            }

            @Override
            public String searchQuery() {
                return channelOverlayCoordinator == null ? "" : channelOverlayCoordinator.getSearchQuery();
            }

            @Override
            public List<ChannelItem> channels() {
                return channels;
            }

            @Override
            public boolean touchMode() {
                return touchDeviceMode;
            }

            @Override
            public int selectedIndex() {
                return overlayNavigationState.selectedOverlayIndex;
            }

            @Override
            public int currentIndex() {
                return overlayNavigationState.currentIndex;
            }

            @Override
            public int scrollToIndex() {
                return pendingOverlayListScrollIndex;
            }

            @Override
            public int scrollRequestToken() {
                return overlayListScrollRequestToken;
            }

            @Override
            public String displayName(ChannelItem item) {
                return MainActivity.this.displayName(item);
            }

            @Override
            public String decorateProtectedTitle(ChannelItem item, String title) {
                return decorateProtectedItemTitle(item, title);
            }

            @Override
            public String decorateProtectedMeta(ChannelItem item, String meta) {
                return MainActivity.this.decorateProtectedMeta(item, meta);
            }

            @Override
            public String vodMeta(ChannelItem item) {
                return buildVodRowMeta(item);
            }

            @Override
            public String membershipLabel(ChannelItem item, int maxLabels) {
                return buildChannelMembershipLabel(item, maxLabels);
            }

            @Override
            public String protectedTypeBadge(ChannelItem item, String fallback) {
                return buildProtectedTypeBadge(item, fallback);
            }

            @Override
            public boolean isProtected(ChannelItem item) {
                return isProtectedItem(item);
            }

            @Override
            public String profileTag(ChannelItem item) {
                return MainActivity.this.profileTag(item);
            }

            @Override
            public void selectAndTune(int position) {
                overlayNavigationState.selectedOverlayIndex = position;
                tuneToIndex(position, true);
                hideOverlay();
            }

            @Override
            public void selectAndToggleFavorite(int position) {
                overlayNavigationState.selectedOverlayIndex = position;
                toggleFavoriteSelected();
            }

            @Override
            public void moveSelection(int delta) {
                moveOverlaySelection(delta);
            }
        });
    }

    private void refreshOverlayChannelList() {
        requestChannelOverlaySurfaceRender();
    }

    private void scrollOverlayChannelListToPosition(int position) {
        pendingOverlayListScrollIndex = position;
        overlayListScrollRequestToken++;
        requestChannelOverlaySurfaceRender();
    }

    private String currentOverlayFilterLabel() {
        if (overlayNavigationState.favoritesOnly || "favorites".equals(overlayNavigationState.selectedFilterKey)) {
            return getString(R.string.status_filter_changed, getString(R.string.touch_home_filter_favorites));
        }
        if (overlayNavigationState.selectedFilterKey == null || overlayNavigationState.selectedFilterKey.trim().isEmpty()) {
            return getString(R.string.filter_all_label);
        }
        for (ChannelFilter filter : filters) {
            if (filter != null && overlayNavigationState.selectedFilterKey.equals(filter.key)) {
                return getString(R.string.status_filter_changed, decorateProtectedFilterLabel(filter));
            }
        }
        return getString(R.string.filter_all_label);
    }

    private void updateQuickAccessButtons() {
        requestChannelOverlaySurfaceRender();
    }

    private void updateTouchHomeHub() {
        if (touchHomeHub == null || touchHomeComposeView == null) {
            return;
        }
        if (touchDeviceMode) {
            touchHomeHub.setVisibility(View.GONE);
            return;
        }
        boolean visible = touchDeviceMode && touchControlsBar != null && touchControlsBar.getVisibility() == View.VISIBLE && !isOverlayVisible() && !isRecordingsPanelVisible() && !isMultiViewVisible();
        touchHomeHub.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            return;
        }
        TouchHomeHubComposeBinder.bind(touchHomeComposeView, buildTouchHomeHubModel());
    }

    private String buildTouchHomeContinueLabel() {
        ChannelItem lastVod = findChannelItemById(lastVodId);
        if (lastVod != null && lastVod.isVod && !shouldHideProtectedItem(lastVod)) {
            long resumeMs = getVodResumePosition(lastVod.id);
            if (resumeMs > 30_000L) {
                return getString(R.string.touch_home_continue_vod, decorateProtectedItemTitle(lastVod, displayName(lastVod)), formatDurationShort(resumeMs));
            }
        }
        if (!recordingResumePositions.isEmpty()) {
            return getString(R.string.touch_home_continue_recording_count, recordingResumePositions.size());
        }
        return "";
    }

    private boolean isTvHubActive() {
        String activeTvKey = findPreferredTvFilterKey();
        return overlayNavigationState.selectedFilterKey == null || overlayNavigationState.selectedFilterKey.equals(activeTvKey) || ("all".equals(overlayNavigationState.selectedFilterKey) && "all".equals(activeTvKey));
    }

    private boolean isVodFilterSelected(boolean adult) {
        if (overlayNavigationState.selectedFilterKey == null || overlayNavigationState.selectedFilterKey.trim().isEmpty()) {
            return false;
        }
        for (ChannelFilter filter : filters) {
            if (filter == null || !overlayNavigationState.selectedFilterKey.equals(filter.key)) {
                continue;
            }
            return adult ? filter.type == FILTER_VOD_ADULT : filter.type == FILTER_VOD;
        }
        return adult ? "vod-adult".equals(overlayNavigationState.selectedFilterKey) : "vod".equals(overlayNavigationState.selectedFilterKey);
    }

    private String buildTouchHomeFilterLabel() {
        if (overlayNavigationState.favoritesOnly || "favorites".equals(overlayNavigationState.selectedFilterKey)) {
            return getString(R.string.touch_home_filter_favorites);
        }
        if (overlayNavigationState.selectedFilterKey == null || overlayNavigationState.selectedFilterKey.trim().isEmpty() || "all".equals(overlayNavigationState.selectedFilterKey)) {
            return getString(R.string.touch_home_filter_all);
        }
        for (ChannelFilter filter : filters) {
            if (filter != null && overlayNavigationState.selectedFilterKey.equals(filter.key) && filter.label != null && !filter.label.trim().isEmpty()) {
                return decorateProtectedFilterLabel(filter);
            }
        }
        return getString(R.string.touch_home_filter_all);
    }

    private TouchHomeHubUiModel buildTouchHomeHubModel() {
        String label = buildTouchHomeFilterLabel();
        int count = (overlayNavigationState.favoritesOnly || "favorites".equals(overlayNavigationState.selectedFilterKey))
                ? buildFavoriteQuickChannels().size()
                : channels.size();
        String continueLabel = buildTouchHomeContinueLabel();
        String subtitle = getString(R.string.touch_home_subtitle, label, count);
        if (!continueLabel.isEmpty()) {
            subtitle = subtitle + "\n" + continueLabel;
        }

        List<ZapActionItem> libraryActions = new ArrayList<>();
        libraryActions.add(new ZapActionItem(getString(R.string.touch_home_button_tv, countItemsForQuickTarget("tv")), true, false, !overlayNavigationState.favoritesOnly && isTvHubActive(), () -> applyQuickOverlayTarget("tv")));
        if (shouldShowGenericVodQuickTarget(false)) {
            libraryActions.add(new ZapActionItem(getString(R.string.touch_home_button_vod, countItemsForQuickTarget("vod")), true, false, !overlayNavigationState.favoritesOnly && isVodFilterSelected(false), () -> applyQuickOverlayTarget("vod")));
        }
        if (shouldShowGenericVodQuickTarget(true)) {
            libraryActions.add(new ZapActionItem(
                    decorateProtectedLabel(getString(R.string.touch_home_button_adult, countItemsForQuickTarget("vod-adult")), currentOfflinePermissions != null && currentOfflinePermissions.protectAdultVod),
                    true,
                    currentOfflinePermissions != null && currentOfflinePermissions.protectAdultVod,
                    !overlayNavigationState.favoritesOnly && isVodFilterSelected(true),
                    () -> applyQuickOverlayTarget("vod-adult")
            ));
        }
        if (!isOfflineRecordingsDisabled()) {
            libraryActions.add(new ZapActionItem(getString(R.string.touch_home_button_grab), true, false, false, () -> {
                showTouchControlsTemporarily();
                openRecordingsBrowser();
            }));
        }

        List<ZapActionItem> accessActions = new ArrayList<>();
        accessActions.add(new ZapActionItem(getString(R.string.touch_home_button_recent, buildRecentQuickChannels().size()), true, false, false, () -> {
            showTouchControlsTemporarily();
            showRecentChannelsQuickDialog();
        }));
        accessActions.add(new ZapActionItem(
                getString(R.string.touch_home_button_favorites, buildFavoriteQuickChannels().size()),
                true,
                false,
                overlayNavigationState.favoritesOnly || "favorites".equals(overlayNavigationState.selectedFilterKey),
                () -> {
                    showTouchControlsTemporarily();
                    applyQuickOverlayTarget("favorites");
                },
                () -> {
                    showTouchControlsTemporarily();
                    showFavoriteChannelsQuickDialog();
                }
        ));
        accessActions.add(new ZapActionItem(
                getString(R.string.touch_home_button_list),
                true,
                false,
                false,
                () -> {
                    showTouchControlsTemporarily();
                    showGlobalSearchDialog();
                },
                () -> {
                    showTouchControlsTemporarily();
                    showOverlay();
                }
        ));
        accessActions.add(new ZapActionItem(getString(R.string.touch_home_button_multi), true, false, isMultiViewVisible(), this::openMultiView));

        return new TouchHomeHubUiModel(
                getString(R.string.touch_home_title),
                subtitle,
                getString(R.string.touch_home_section_library),
                getString(R.string.touch_home_section_access),
                libraryActions,
                accessActions
        );
    }

    private void openMultiView() {
        openMultiView(buildMultiViewChannels());
    }

    private void openMultiView(List<ChannelItem> selected) {
        if (selected == null || selected.size() < 2) {
            showStatus(getString(R.string.status_multiview_not_enough_channels));
            return;
        }
        int supportedStreams = devicePerformanceProfile == null ? 2 : devicePerformanceProfile.maxMultiViewStreams;
        boolean limitedByDevice = false;
        if (selected.size() > supportedStreams) {
            selected = new ArrayList<>(selected.subList(0, supportedStreams));
            limitedByDevice = true;
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
            if (multiTiles[i] == null) {
                continue;
            }
            if (i < selected.size()) {
                ChannelItem item = selected.get(i);
                multiTiles[i].setVisibility(View.VISIBLE);
                PlayerController controller = multiPlayerControllers.get(i);
                controller.setMuted(i != multiViewActiveIndex);
                PlayerController.PlaybackRequest request = toPlaybackRequest(item);
                PlayerController.StreamInfo cachedStreamInfo = streamInfoByChannelId.get(item.id);
                if (shouldResolveStreamInfoBeforePlayback(item, request)) {
                    controller.playChannelAfterResolvingStreamInfo(request, true, streamInfoByChannelId, 0L);
                } else {
                    controller.playChannel(request, true, cachedStreamInfo);
                }
                if (request != null && !shouldResolveStreamInfoBeforePlayback(item, request) && !request.directPlayback) {
                    controller.resolveStreamInfoAndReplayIfNeeded(request, true, streamInfoByChannelId);
                }
            } else {
                multiTiles[i].setVisibility(View.INVISIBLE);
            }
        }
        refreshMultiViewHeader();
        updateMultiViewFocus();
        if (multiViewContainer != null) {
            multiViewContainer.setVisibility(View.VISIBLE);
        }
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.MULTIVIEW, true);
        showStatus(limitedByDevice
                ? getString(R.string.status_multiview_device_limit, supportedStreams)
                : getString(R.string.multiview_title));
    }

    private void closeMultiView() {
        if (multiViewContainer != null && multiViewContainer.getVisibility() != View.VISIBLE) {
            return;
        }
        if (multiViewContainer != null) {
            multiViewContainer.setVisibility(View.GONE);
        }
        overlaySurfaceState.setVisible(OfflineOverlayState.Surface.MULTIVIEW, false);
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
        int supportedStreams = devicePerformanceProfile == null ? 2 : devicePerformanceProfile.maxMultiViewStreams;
        for (int i = 0; i < Math.min(multiPlayerViews.length, supportedStreams); i++) {
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
                public boolean isMultiViewPlayback() {
                    return true;
                }

                @Override
                public void recordPlaybackError(PlayerController.PlaybackRequest request, PlayerController.PlaybackDiagnostics diagnostics) {
                    MainActivity.this.recordPlaybackError(request, diagnostics);
                }

                @Override
                public void onPlaybackReady(PlayerController.PlaybackRequest request) {
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
        int maxStreams = devicePerformanceProfile == null ? 2 : devicePerformanceProfile.maxMultiViewStreams;
        if (overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size()) {
            ChannelItem current = channels.get(overlayNavigationState.currentIndex);
            if (current != null && !current.isVod && current.id != null && added.add(current.id)) {
                selected.add(current);
            }
        }
        for (ChannelItem item : channels) {
            if (selected.size() >= maxStreams) {
                break;
            }
            if (item == null || item.isVod || item.id == null || !added.add(item.id)) {
                continue;
            }
            selected.add(item);
        }
        if (selected.size() < maxStreams) {
            for (ChannelItem item : allChannels) {
                if (selected.size() >= maxStreams) {
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
        List<String> options = Arrays.asList(labels);
        List<Runnable> actions = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            final int which = i;
            actions.add(() -> {
                    saveMultiViewPreset(which, source);
                    showStatus(getString(R.string.status_multiview_preset_saved, which + 1));
            });
        }
        showTvOptionsDialog(R.string.multiview_save_preset_title, null, options, actions);
    }

    private void showOpenMultiViewPresetDialog() {
        String[] labels = new String[MULTIVIEW_PRESET_COUNT];
        for (int i = 0; i < MULTIVIEW_PRESET_COUNT; i++) {
            labels[i] = buildMultiViewPresetLabel(i);
        }
        List<String> options = Arrays.asList(labels);
        List<Runnable> actions = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            final int which = i;
            actions.add(() -> {
                    List<ChannelItem> preset = resolveMultiViewPreset(which);
                    if (preset.size() < 2) {
                        showStatus(getString(R.string.status_multiview_preset_empty));
                        return;
                    }
                    openMultiView(preset);
            });
        }
        showTvOptionsDialog(R.string.multiview_open_preset_title, null, options, actions);
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
        if (shouldResolveStreamInfoBeforePlayback(item, request)) {
            controller.playChannelAfterResolvingStreamInfo(request, true, streamInfoByChannelId, 0L);
        } else {
            controller.playChannel(request, true, cachedStreamInfo);
        }
        if (request != null && !shouldResolveStreamInfoBeforePlayback(item, request) && !request.directPlayback) {
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

    private void refreshMultiViewHeader() {
        MultiViewHeaderComposeBinder.bind(
                multiViewHeaderComposeView,
                new MultiViewHeaderUiModel(
                        getString(R.string.multiview_title),
                        touchDeviceMode ? getString(R.string.multiview_hint_touch) : getString(R.string.multiview_hint_tv),
                        getString(R.string.multiview_close),
                        this::closeMultiView
                )
        );
    }

    private void refreshMultiViewOverlays() {
        for (int i = 0; i < multiOverlayViews.length; i++) {
            boolean hasChannel = i < multiViewChannels.size() && multiViewChannels.get(i) != null;
            ChannelItem item = hasChannel ? multiViewChannels.get(i) : null;
            MultiViewTileOverlayComposeBinder.bind(
                    multiOverlayViews[i],
                    new MultiViewTileOverlayUiModel(
                            item == null ? "" : item.name,
                            getString(R.string.multiview_slot_label, i + 1),
                            getString(R.string.multiview_active_audio),
                            touchDeviceMode ? getString(R.string.multiview_tile_hint_touch) : getString(R.string.multiview_tile_hint_tv),
                            hasChannel,
                            i == multiViewActiveIndex && hasChannel,
                            false,
                            ""
                    )
            );
        }
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
        }
        refreshMultiViewOverlays();
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
        channelOverlayCoordinator.syncState(overlayNavigationState.currentIndex, overlayNavigationState.selectedOverlayIndex, overlayNavigationState.favoritesOnly, overlayNavigationState.selectedFilterKey);
    }

    private void syncOverlayStateFromCoordinator() {
        overlayNavigationState.currentIndex = channelOverlayCoordinator.getCurrentIndex();
        overlayNavigationState.selectedOverlayIndex = channelOverlayCoordinator.getSelectedOverlayIndex();
        overlayNavigationState.favoritesOnly = channelOverlayCoordinator.isFavoritesOnly();
        overlayNavigationState.selectedFilterKey = channelOverlayCoordinator.getSelectedFilterKey();
        persistNavigationState();
    }

    private void updateOverlayPanel() {
        requestChannelOverlaySurfaceRender();
    }

    private void requestChannelOverlaySurfaceRender() {
        if (activityDestroyed) {
            return;
        }
        uiHandler.removeCallbacks(channelOverlayRenderRunnable);
        postUiDelayedIfAlive(channelOverlayRenderRunnable, OVERLAY_RENDER_COALESCE_MS);
    }

    private void schedulePlaybackQualityUiRefresh() {
        if (activityDestroyed) {
            return;
        }
        uiHandler.removeCallbacks(playbackQualityUiRefreshRunnable);
        postUiDelayedIfAlive(playbackQualityUiRefreshRunnable, PLAYBACK_QUALITY_UI_COALESCE_MS);
    }

    private ChannelOverlayUi.NowPlayingModel buildOverlayNowPlayingModel() {
        ChannelItem currentChannel = getCurrentPlaybackChannelItem();
        ensureTouchControlsEpgPair(currentChannel);
        PlayerController.PlaybackDiagnostics diagnostics = playerController == null ? null : playerController.getPlaybackDiagnostics();
        List<RecentChannelsStore.RecentChannelItem> items = recentChannelsStore == null ? new ArrayList<>() : recentChannelsStore.getItems();
        EpgRepository.EpgProgramPair epgPair = currentChannel == null ? null : epgProgramPairByChannelId.get(currentChannel.id);
        return ChannelOverlayUi.buildNowPlayingModel(
                this,
                currentChannel,
                currentChannel == null ? "" : displayName(currentChannel),
                profileTag(currentChannel),
                overlayContextLabel(currentChannel),
                diagnostics,
                formatPlaybackQualityCompact(diagnostics),
                epgPair,
                items
        );
    }

    private void renderOverlayNowPlayingSurface() {
        if (overlayNowPlayingComposeView == null) {
            return;
        }
        composeSurfaceRenderer.bindOverlayNowPlaying(
                overlayNowPlayingComposeView,
                buildOverlayNowPlayingModel()
        );
    }

    private ChannelOverlaySurfaceUiModel buildChannelOverlaySurfaceModel() {
        ChannelOverlayUi.NowPlayingModel nowPlayingModel = buildOverlayNowPlayingModel();
        String activeTvKey = findPreferredTvFilterKey();
        boolean tvActive = !overlayNavigationState.favoritesOnly && (overlayNavigationState.selectedFilterKey == null || overlayNavigationState.selectedFilterKey.equals(activeTvKey) || ("all".equals(overlayNavigationState.selectedFilterKey) && "all".equals(activeTvKey)));
        boolean vodActive = !overlayNavigationState.favoritesOnly && isVodFilterSelected(false);
        boolean adultActive = !overlayNavigationState.favoritesOnly && isVodFilterSelected(true);
        return new ChannelOverlaySurfaceUiModel(
                nowPlayingModel,
                buildOverlayControlsModel(tvActive, vodActive, adultActive),
                buildOverlayChannelListUiModel()
        );
    }

    private void renderChannelOverlaySurface() {
        if (overlayNowPlayingComposeView == null && overlayControlsComposeView == null && channelListComposeView == null) {
            return;
        }
        composeSurfaceRenderer.bindChannelOverlaySurface(
                overlayNowPlayingComposeView,
                overlayControlsComposeView,
                channelListComposeView,
                buildChannelOverlaySurfaceModel(),
                (imageView, item) -> {
                    if (item == null || imageView == null) {
                        return;
                    }
                    if (item.vod) {
                        bindVodPosterList(imageView, item.logoUrl);
                    } else {
                        bindChannelLogo(imageView, item.logoUrl, item.name, 38, 38);
                    }
                }
        );
    }

    private String overlayContextLabel(ChannelItem channel) {
        ChannelFilter filter = selectedOverlayFilter();
        if ((overlayNavigationState != null && overlayNavigationState.favoritesOnly)
                || (filter != null && "favorites".equals(filter.key))) {
            return getString(R.string.touch_home_filter_favorites);
        }
        if (filter != null && filter.type == FILTER_CUSTOM_GROUP && filter.groupName != null && !filter.groupName.trim().isEmpty()) {
            return filter.groupName.trim();
        }
        if (filter != null && filter.type == FILTER_PLATFORM && filter.label != null && !filter.label.trim().isEmpty()) {
            return stripOverlayFilterPrefix(filter.label);
        }
        if (channel != null && channel.platformName != null && !channel.platformName.trim().isEmpty()) {
            return channel.platformName.trim();
        }
        if (channel != null && channel.group != null && !channel.group.trim().isEmpty()) {
            return channel.group.trim();
        }
        return buildCurrentFilterLabel();
    }

    private ChannelFilter selectedOverlayFilter() {
        String selectedKey = overlayNavigationState == null ? "" : overlayNavigationState.selectedFilterKey;
        if (selectedKey == null || selectedKey.trim().isEmpty()) {
            return null;
        }
        for (ChannelFilter filter : filters) {
            if (filter != null && selectedKey.equals(filter.key)) {
                return filter;
            }
        }
        return null;
    }

    private String stripOverlayFilterPrefix(String label) {
        if (label == null) {
            return "";
        }
        String trimmed = label.trim();
        int separator = trimmed.indexOf(':');
        if (separator >= 0 && separator + 1 < trimmed.length()) {
            return trimmed.substring(separator + 1).trim();
        }
        return trimmed;
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

    private String buildZapChannelBadge(ChannelItem channelItem) {
        int channelNumber = channelItem == null ? 0 : Math.max(channelItem.dashboardOrder, channelItem.originalOrder);
        if (channelNumber <= 0 && overlayNavigationState.currentIndex >= 0) {
            channelNumber = overlayNavigationState.currentIndex + 1;
        }
        if (channelNumber <= 0) {
            return getString(R.string.zap_banner_channel_badge_unavailable);
        }
        return getString(R.string.zap_banner_channel_badge, String.valueOf(channelNumber));
    }

    private String buildZapProgramMeta(ChannelItem channelItem, EpgRepository.EpgProgram currentProgram) {
        List<String> parts = new ArrayList<>();
        if (currentProgram != null) {
            String start = shortTime(currentProgram.startTime);
            String end = shortTime(currentProgram.endTime);
            if (!start.isEmpty() && !end.isEmpty()) {
                parts.add(start + " - " + end);
            }
            long startMs = parseIsoMillis(currentProgram.startTime);
            long endMs = parseIsoMillis(currentProgram.endTime);
            long durationMs = endMs > startMs ? (endMs - startMs) : 0L;
            if (durationMs > 0L) {
                parts.add(formatDurationShort(durationMs));
            }
        } else {
            String fallback = buildZapProgramSummary(channelItem);
            if (!fallback.trim().isEmpty()) {
                parts.add(fallback);
            }
        }
        String tag = profileTag(channelItem);
        if (!tag.isEmpty()) {
            parts.add(tag);
        }
        return joinLabels(parts);
    }

    private void moveZapBannerSelection(int delta) {
        zapBannerController.moveSelection(delta);
    }

    private void activateZapBannerSelection() {
        zapBannerController.activateSelection();
    }

    private ZapBannerController.Host createZapBannerHost() {
        return new ZapBannerController.Host() {
            @Override
            public String string(int resId) {
                return getString(resId);
            }

            @Override
            public String string(int resId, Object... args) {
                return getString(resId, args);
            }

            @Override
            public boolean offlineRecordingsDisabled() {
                return isOfflineRecordingsDisabled();
            }

            @Override
            public boolean isProtectedItem(ChannelItem item) {
                return MainActivity.this.isProtectedItem(item);
            }

            @Override
            public boolean isFavorite(ChannelItem item) {
                return item != null && favoriteChannelIds.contains(item.id);
            }

            @Override
            public ChannelItem currentPlaybackChannel() {
                return getCurrentPlaybackChannelItem();
            }

            @Override
            public EpgRepository.EpgProgramPair epgPair(String channelId) {
                return epgProgramPairByChannelId.get(channelId);
            }

            @Override
            public PlayerController.PlaybackDiagnostics playbackDiagnostics() {
                return playerController == null ? null : playerController.getPlaybackDiagnostics();
            }

            @Override
            public void bindChannelLogo(ImageView imageView, String logoUrl, String channelName, int widthDp, int heightDp) {
                MainActivity.this.bindChannelLogo(imageView, logoUrl, channelName, widthDp, heightDp);
            }

            @Override
            public String displayName(ChannelItem item) {
                return MainActivity.this.displayName(item);
            }

            @Override
            public String channelBadge(ChannelItem item) {
                return buildZapChannelBadge(item);
            }

            @Override
            public String programMeta(ChannelItem item, EpgRepository.EpgProgram program) {
                return buildZapProgramMeta(item, program);
            }

            @Override
            public String playbackQuality(PlayerController.PlaybackDiagnostics diagnostics) {
                return formatPlaybackQualityCompact(diagnostics);
            }

            @Override
            public String durationShort(long durationMs) {
                return formatDurationShort(durationMs);
            }

            @Override
            public String shortTime(String isoTime) {
                return MainActivity.this.shortTime(isoTime);
            }

            @Override
            public long parseIsoMillis(String isoTime) {
                return MainActivity.this.parseIsoMillis(isoTime);
            }

            @Override
            public void showChannels() {
                showOverlay();
            }

            @Override
            public void openGuide() {
                openTimelineGuideForCurrentPlayback();
            }

            @Override
            public boolean supportsU7d(ChannelItem item) {
                return isMovistarIsmChannel(item);
            }

            @Override
            public void openU7d(ChannelItem item) {
                openMovistarIsmU7d(item);
            }

            @Override
            public void scheduleCurrentProgram() {
                scheduleCurrentProgramFromHud();
            }

            @Override
            public void showParentalSettings() {
                showParentalSettingsDialog();
            }

            @Override
            public void showAudioTrack() {
                showAudioTrackDialog();
            }

            @Override
            public void showPlaybackDiagnostics() {
                showPlaybackDiagnosticsFromHud();
            }

            @Override
            public void toggleCurrentFavorite() {
                toggleCurrentChannelFavoriteFromHud();
            }

            @Override
            public void showToolsMenu() {
                showSimpleOfflineToolsMenu();
            }
        };
    }

    private void openTimelineGuideForCurrentPlayback() {
        ChannelItem currentChannel = getCurrentPlaybackChannelItem();
        if (currentChannel == null) {
            showOverlay();
            return;
        }
        openTimelineGuideForChannel(currentChannel);
    }

    private void scheduleCurrentProgramFromHud() {
        if (isOverlayVisible() && overlayNavigationState.selectedOverlayIndex >= 0 && overlayNavigationState.selectedOverlayIndex < channels.size()) {
            createScheduleFromEndpoint(channels.get(overlayNavigationState.selectedOverlayIndex), false);
            return;
        }
        ChannelItem currentChannel = getCurrentPlaybackChannelItem();
        if (currentChannel == null && overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size()) {
            currentChannel = channels.get(overlayNavigationState.currentIndex);
        }
        if (currentChannel != null) {
            createScheduleFromEndpoint(currentChannel, false);
        }
    }

    private void showPlaybackDiagnosticsFromHud() {
        ChannelItem currentChannel = getCurrentPlaybackChannelItem();
        if (currentChannel != null) {
            showPlaybackDiagnosticsActionsDialog(currentChannel);
            return;
        }
        showPlaybackDiagnosticsDialog();
    }

    private void toggleCurrentChannelFavoriteFromHud() {
        ChannelItem currentChannel = getCurrentPlaybackChannelItem();
        if (currentChannel == null) {
            showOverlay();
            return;
        }
        toggleFavoriteForChannel(currentChannel);
        if (isZapBannerVisible()) {
            updateZapBannerContent(currentChannel);
            zapBannerController.refreshAutoHideTimer();
        }
    }

    private void showV12ToolsMenu() {
        showSimpleOfflineToolsMenu();
    }

    private void showSimpleOfflineToolsMenu() {
        TvOptionsMenuModel menu = ToolsMenuUiFactory.buildSimple(buildToolsMenuHost(null, this::showSimpleOfflineToolsMenu));
        showTvOptionsDialog(R.string.tools_menu_title_short, menu.message, menu.options, menu.actions);
    }

    private void showAdvancedToolsMenu() {
        showAdvancedToolsMenu(null);
    }

    private void showAdvancedToolsMenu(Runnable onBack) {
        Runnable currentMenu = () -> showAdvancedToolsMenu(onBack);
        TvOptionsMenuModel menu = ToolsMenuUiFactory.buildAdvanced(buildToolsMenuHost(onBack, currentMenu));
        showTvOptionsDialog(R.string.tools_section_advanced, menu.message, menu.options, menu.actions, onBack);
    }

    private void showTvAndGuideToolsDialog() {
        showTvAndGuideToolsDialog(null);
    }

    private void showTvAndGuideToolsDialog(Runnable onBack) {
        Runnable currentMenu = () -> showTvAndGuideToolsDialog(onBack);
        TvOptionsMenuModel menu = ToolsMenuUiFactory.buildTvGuide(buildToolsMenuHost(onBack, currentMenu));
        showTvOptionsDialog(R.string.tools_section_tv_guide, menu.message, menu.options, menu.actions, onBack);
    }

    private void showSearchAndRecentsToolsDialog() {
        showSearchAndRecentsToolsDialog(null);
    }

    private void showSearchAndRecentsToolsDialog(Runnable onBack) {
        TvOptionsMenuModel menu = ToolsMenuUiFactory.buildSearchRecents(buildToolsMenuHost(onBack, () -> showSearchAndRecentsToolsDialog(onBack)));
        showTvOptionsDialog(R.string.tools_section_search_recents, menu.message, menu.options, menu.actions, onBack);
    }

    private void showFamilyAndSecurityToolsDialog() {
        showFamilyAndSecurityToolsDialog(null);
    }

    private void showFamilyAndSecurityToolsDialog(Runnable onBack) {
        TvOptionsMenuModel menu = ToolsMenuUiFactory.buildFamily(buildToolsMenuHost(onBack, () -> showFamilyAndSecurityToolsDialog(onBack)));
        showTvOptionsDialog(R.string.tools_section_family, menu.message, menu.options, menu.actions, onBack);
    }

    private void showRecordingsSimpleToolsDialog() {
        showRecordingsSimpleToolsDialog(null);
    }

    private void showRecordingsSimpleToolsDialog(Runnable onBack) {
        if (showOfflineRecordingsUnavailableIfNeeded()) {
            return;
        }
        TvOptionsMenuModel menu = ToolsMenuUiFactory.buildRecordings(buildToolsMenuHost(onBack, () -> showRecordingsSimpleToolsDialog(onBack)));
        showTvOptionsDialog(R.string.tools_section_recordings, menu.message, menu.options, menu.actions, onBack);
    }

    private void showPlaybackToolsDialog() {
        showPlaybackToolsDialog(null);
    }

    private void showPlaybackToolsDialog(Runnable onBack) {
        TvOptionsMenuModel menu = ToolsMenuUiFactory.buildPlayback(buildToolsMenuHost(onBack, () -> showPlaybackToolsDialog(onBack)));
        showTvOptionsDialog(R.string.tools_section_playback, menu.message, menu.options, menu.actions, onBack);
    }

    private void showNavigationToolsDialog() {
        showNavigationToolsDialog(null);
    }

    private void showNavigationToolsDialog(Runnable onBack) {
        TvOptionsMenuModel menu = ToolsMenuUiFactory.buildNavigation(buildToolsMenuHost(onBack, () -> showNavigationToolsDialog(onBack)));
        showTvOptionsDialog(R.string.tools_section_navigation, menu.message, menu.options, menu.actions, onBack);
    }

    private void showVodLibraryDialog() {
        showVodLibraryDialog(null);
    }

    private void showVodLibraryDialog(Runnable onBack) {
        showVodVisualLibraryDialog(onBack);
    }

    private void showVodVisualLibraryDialog() {
        showVodVisualLibraryDialog(null);
    }

    private void showVodVisualLibraryDialog(Runnable onBack) {
        showVodVisualLibraryDialog(VodVisualTypeFilter.GENERAL, VodVisualPlatformFilter.ALL, VodVisualStatusFilter.ALL, VodVisualSortFilter.SMART, "", onBack);
    }

    private void showVodLibraryMenuDialog() {
        showVodLibraryMenuDialog(null);
    }

    private void showVodLibraryMenuDialog(Runnable onBack) {
        rememberCurrentVodPosition();
        List<ChannelItem> continueItems = buildVodContinueItems();
        List<ChannelItem> recentItems = buildRecentVodItems();
        List<ChannelItem> tivifyItems = buildVodItemsByFilter("vod:tivify:general", false);
        List<ChannelItem> tivifyAdultItems = buildVodItemsByFilter("vod:tivify:adult", true);
        List<ChannelItem> runtimeItems = buildVodItemsByFilter("vod:runtime:movies", false);
        List<ChannelItem> progressItems = buildVodProgressItems();
        List<ChannelItem> notStartedItems = buildVodNotStartedItems();
        List<ChannelItem> allVodItems = buildAllVodLibraryItems(false);
        Runnable returnToThisMenu = () -> showVodLibraryMenuDialog(onBack);
        TvOptionsMenuModel menu = VodLibraryMenuUiFactory.build(
                continueItems,
                recentItems,
                tivifyItems,
                tivifyAdultItems,
                runtimeItems,
                progressItems,
                notStartedItems,
                allVodItems,
                new VodLibraryMenuUiFactory.Host() {
                    @Override
                    public String text(int resId) {
                        return getString(resId);
                    }

                    @Override
                    public String optionLabel(int titleResId, List<ChannelItem> items) {
                        return buildVodLibraryOptionLabel(titleResId, items);
                    }

                    @Override
                    public String protectedLabel(String label, boolean protectedEntry) {
                        return decorateProtectedLabel(label, protectedEntry);
                    }

                    @Override
                    public boolean protectAdultVod() {
                        return currentOfflinePermissions != null && currentOfflinePermissions.protectAdultVod;
                    }

                    @Override
                    public void openContinue() {
                        showVodLibraryList(R.string.vod_library_continue, continueItems, true, returnToThisMenu);
                    }

                    @Override
                    public void openRecent() {
                        showVodLibraryList(R.string.vod_library_recent, recentItems, false, returnToThisMenu);
                    }

                    @Override
                    public void openTivify() {
                        showVodLibraryList(R.string.vod_library_tivify, tivifyItems, false, returnToThisMenu);
                    }

                    @Override
                    public void openTivifyAdult() {
                        ensureParentalAccessForFilterKey("vod:tivify:adult", () -> showVodLibraryList(R.string.vod_library_tivify_adult, tivifyAdultItems, false, returnToThisMenu));
                    }

                    @Override
                    public void openRuntime() {
                        showVodLibraryList(R.string.vod_library_runtime, runtimeItems, false, returnToThisMenu);
                    }

                    @Override
                    public void openProgress() {
                        showVodLibraryList(R.string.vod_library_with_progress, progressItems, true, returnToThisMenu);
                    }

                    @Override
                    public void openNotStarted() {
                        showVodLibraryList(R.string.vod_library_not_started, notStartedItems, false, returnToThisMenu);
                    }

                    @Override
                    public void openCategories() {
                        showVodCategoriesDialog(returnToThisMenu);
                    }

                    @Override
                    public void openAllAlpha() {
                        showVodLibraryList(R.string.vod_library_all_alpha, buildVodSortedItems(VodSortMode.ALPHA), false, returnToThisMenu);
                    }

                    @Override
                    public void openSortYear() {
                        showVodLibraryList(R.string.vod_library_sort_year, buildVodSortedItems(VodSortMode.YEAR_DESC), false, returnToThisMenu);
                    }

                    @Override
                    public void openSortDuration() {
                        showVodLibraryList(R.string.vod_library_sort_duration, buildVodSortedItems(VodSortMode.DURATION_DESC), false, returnToThisMenu);
                    }

                    @Override
                    public void openSearch() {
                        showVodSearchDialog("", returnToThisMenu);
                    }

                    @Override
                    public void openProgressManager() {
                        showVodProgressManagerDialog(returnToThisMenu);
                    }
                }
        );
        showTvOptionsDialog(R.string.tools_section_vod, menu.message, menu.options, menu.actions, onBack);
    }

    private void showVodVisualLibraryDialog(VodVisualTypeFilter typeFilter, VodVisualPlatformFilter platformFilter, VodVisualStatusFilter statusFilter, VodVisualSortFilter sortFilter) {
        showVodVisualLibraryDialog(typeFilter, platformFilter, statusFilter, sortFilter, "");
    }

    private void showVodVisualLibraryDialog(VodVisualTypeFilter typeFilter, VodVisualPlatformFilter platformFilter, VodVisualStatusFilter statusFilter, VodVisualSortFilter sortFilter, String searchQuery) {
        showVodVisualLibraryDialog(typeFilter, platformFilter, statusFilter, sortFilter, searchQuery, null);
    }

    private void showVodVisualLibraryDialog(VodVisualTypeFilter typeFilter, VodVisualPlatformFilter platformFilter, VodVisualStatusFilter statusFilter, VodVisualSortFilter sortFilter, String searchQuery, Runnable onBack) {
        rememberCurrentVodPosition();
        prepareModalSurface();
        final Dialog[] dialogHolder = new Dialog[1];
        String trimmedSearchQuery = searchQuery == null ? "" : searchQuery.trim();
        ComposeView composeView = new ComposeView(this);
        attachDialogViewTreeOwners(composeView);
        VodVisualPanelComposeBinder.bind(
                composeView,
                buildVodVisualPanelModel(typeFilter, platformFilter, statusFilter, sortFilter, trimmedSearchQuery, dialogHolder, onBack),
                (imageView, item) -> bindVodPosterThumbnail(imageView, item == null ? "" : item.posterUrl)
        );
        Dialog dialog = ComposeDialogHost.showFullscreen(this, composeView, () -> {
            if (onBack != null) {
                modalReturnAction = onBack;
            }
        }, () -> {
            handleModalDismissed();
        });
        dialogHolder[0] = dialog;
        handleModalShown();
    }

    private VodVisualPanelUiModel buildVodVisualPanelModel(VodVisualTypeFilter typeFilter, VodVisualPlatformFilter platformFilter, VodVisualStatusFilter statusFilter, VodVisualSortFilter sortFilter, String trimmedSearchQuery, Dialog[] dialogHolder, Runnable onBack) {
        return VodVisualUiFactory.build(typeFilter, platformFilter, statusFilter, sortFilter, trimmedSearchQuery, new VodVisualUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String text(int resId, Object... args) {
                return getString(resId, args);
            }

            @Override
            public boolean protectAdultVod() {
                return currentOfflinePermissions != null && currentOfflinePermissions.protectAdultVod;
            }

            @Override
            public boolean protectedContentLocked() {
                return isProtectedContentLocked();
            }

            @Override
            public boolean defaultFilter(VodVisualTypeFilter currentTypeFilter, VodVisualPlatformFilter currentPlatformFilter, VodVisualStatusFilter currentStatusFilter, VodVisualSortFilter currentSortFilter) {
                return isDefaultVodVisualFilter(currentTypeFilter, currentPlatformFilter, currentStatusFilter, currentSortFilter);
            }

            @Override
            public String librarySummary() {
                return buildVodLibrarySummary();
            }

            @Override
            public String searchSummary(String query) {
                return buildVodSearchSummary(query);
            }

            @Override
            public List<ChannelItem> filteredItems(VodVisualTypeFilter currentTypeFilter, VodVisualPlatformFilter currentPlatformFilter, VodVisualStatusFilter currentStatusFilter, VodVisualSortFilter currentSortFilter) {
                return buildVodVisualFilteredItems(currentTypeFilter, currentPlatformFilter, currentStatusFilter, currentSortFilter);
            }

            @Override
            public List<ChannelItem> filteredItems(VodVisualTypeFilter currentTypeFilter, VodVisualPlatformFilter currentPlatformFilter, VodVisualStatusFilter currentStatusFilter, VodVisualSortFilter currentSortFilter, String query) {
                return buildVodVisualFilteredItems(currentTypeFilter, currentPlatformFilter, currentStatusFilter, currentSortFilter, query);
            }

            @Override
            public List<ChannelItem> continueItems() {
                return buildVodContinueItems();
            }

            @Override
            public List<ChannelItem> recentItems() {
                return buildRecentVodItems();
            }

            @Override
            public List<ChannelItem> movistarItems() {
                return buildMovistarVodItems();
            }

            @Override
            public List<ChannelItem> runtimeItems() {
                return buildVodItemsByFilter("vod:runtime:movies", false);
            }

            @Override
            public List<ChannelItem> tivifyItems() {
                return buildVodItemsByFilter("vod:tivify:general", false);
            }

            @Override
            public List<ChannelItem> progressItems() {
                return buildVodProgressItems();
            }

            @Override
            public List<ChannelItem> alphaItems() {
                return buildVodSortedItems(VodSortMode.ALPHA);
            }

            @Override
            public String displayName(ChannelItem item) {
                return MainActivity.this.displayName(item);
            }

            @Override
            public String posterMeta(ChannelItem item) {
                return buildVodPosterMeta(item);
            }

            @Override
            public String protectedTitle(ChannelItem item, String title) {
                return decorateProtectedItemTitle(item, title);
            }

            @Override
            public String protectedMeta(ChannelItem item, String meta) {
                return decorateProtectedMeta(item, meta);
            }

            @Override
            public String progressLabel(ChannelItem item) {
                long progressMs = getVodResumePosition(item == null ? null : item.id);
                return progressMs > 30_000L ? formatDurationShort(progressMs) : "";
            }

            @Override
            public void editSearch(String query) {
                dismissModalForNextAction(dialogHolder[0], () -> showVodSearchDialog(query));
            }

            @Override
            public void openType(VodVisualTypeFilter nextTypeFilter, VodVisualPlatformFilter currentPlatformFilter, VodVisualStatusFilter currentStatusFilter, VodVisualSortFilter currentSortFilter, String query) {
                dismissModalForNextAction(dialogHolder[0], () -> showVodVisualLibraryDialog(nextTypeFilter, currentPlatformFilter, currentStatusFilter, currentSortFilter, query, onBack));
            }

            @Override
            public void openPlatform(VodVisualTypeFilter currentTypeFilter, VodVisualPlatformFilter nextPlatformFilter, VodVisualStatusFilter currentStatusFilter, VodVisualSortFilter currentSortFilter, String query) {
                dismissModalForNextAction(dialogHolder[0], () -> showVodVisualLibraryDialog(currentTypeFilter, nextPlatformFilter, currentStatusFilter, currentSortFilter, query, onBack));
            }

            @Override
            public void openStatus(VodVisualTypeFilter currentTypeFilter, VodVisualPlatformFilter currentPlatformFilter, VodVisualStatusFilter nextStatusFilter, VodVisualSortFilter currentSortFilter, String query) {
                dismissModalForNextAction(dialogHolder[0], () -> showVodVisualLibraryDialog(currentTypeFilter, currentPlatformFilter, nextStatusFilter, currentSortFilter, query, onBack));
            }

            @Override
            public void openSort(VodVisualTypeFilter currentTypeFilter, VodVisualPlatformFilter currentPlatformFilter, VodVisualStatusFilter currentStatusFilter, VodVisualSortFilter nextSortFilter, String query) {
                dismissModalForNextAction(dialogHolder[0], () -> showVodVisualLibraryDialog(currentTypeFilter, currentPlatformFilter, currentStatusFilter, nextSortFilter, query, onBack));
            }

            @Override
            public void clearSearch() {
                dismissModalForNextAction(dialogHolder[0], () -> showVodVisualLibraryDialog(onBack));
            }

            @Override
            public void openSearch() {
                dismissModalForNextAction(dialogHolder[0], () -> showVodSearchDialog("", () -> showVodVisualLibraryDialog(onBack)));
            }

            @Override
            public void openListView() {
                dismissModalForNextAction(dialogHolder[0], () -> showVodLibraryMenuDialog(() -> showVodVisualLibraryDialog(onBack)));
            }

            @Override
            public void unlockAdultAndOpen(VodVisualTypeFilter nextTypeFilter, VodVisualPlatformFilter currentPlatformFilter, VodVisualStatusFilter currentStatusFilter, VodVisualSortFilter currentSortFilter, String query) {
                ensureParentalAccessForFilterKey("vod:tivify:adult", () -> {
                    dismissModalForNextAction(dialogHolder[0], () -> showVodVisualLibraryDialog(nextTypeFilter, currentPlatformFilter, currentStatusFilter, currentSortFilter, query, onBack));
                });
            }

            @Override
            public void openInfo(ChannelItem item) {
                dismissModalForNextAction(dialogHolder[0], () -> showVodInfoDialog(item, () -> showVodVisualLibraryDialog(onBack)));
            }

            @Override
            public void openActions(ChannelItem item) {
                dismissModalForNextAction(dialogHolder[0], () -> showVodActionsDialog(item, () -> showVodVisualLibraryDialog(onBack)));
            }
        });
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

    private void dismissVodVisualDialog(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void showListsToolsDialog() {
        showListsToolsDialog(null);
    }

    private void showListsToolsDialog(Runnable onBack) {
        TvOptionsMenuModel menu = ToolsMenuUiFactory.buildLists(buildToolsMenuHost(onBack, () -> showListsToolsDialog(onBack)));
        showTvOptionsDialog(R.string.tools_section_lists, menu.message, menu.options, menu.actions, onBack);
    }

    private boolean isOfflineRecordingsDisabled() {
        return baseUrl == null || baseUrl.trim().isEmpty();
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
        TvOptionsMenuModel menu = ToolsMenuUiFactory.buildRecordings(buildToolsMenuHost(null, this::showRecordingsToolsDialog));
        showTvOptionsDialog(R.string.tools_section_recordings, menu.message, menu.options, menu.actions);
    }

    private void showMultiviewToolsDialog() {
        showMultiviewToolsDialog(null);
    }

    private void showMultiviewToolsDialog(Runnable onBack) {
        TvOptionsMenuModel menu = ToolsMenuUiFactory.buildMultiview(buildToolsMenuHost(onBack, () -> showMultiviewToolsDialog(onBack)));
        showTvOptionsDialog(R.string.tools_section_multiview, menu.message, menu.options, menu.actions, onBack);
    }

    private void showSettingsAndDiagnosticsToolsDialog() {
        showSettingsAndDiagnosticsToolsDialog(null);
    }

    private void showSettingsAndDiagnosticsToolsDialog(Runnable onBack) {
        TvOptionsMenuModel menu = ToolsMenuUiFactory.buildSettings(buildToolsMenuHost(onBack, () -> showSettingsAndDiagnosticsToolsDialog(onBack)));
        showTvOptionsDialog(R.string.tools_section_settings, menu.message, menu.options, menu.actions, onBack);
    }

    private ToolsMenuUiFactory.Host buildToolsMenuHost(Runnable onBack, Runnable currentMenu) {
        return new ToolsMenuUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String text(int resId, Object... args) {
                return getString(resId, args);
            }

            @Override
            public boolean recordingsAvailable() {
                return !isOfflineRecordingsDisabled();
            }

            @Override
            public boolean canScheduleRecordings() {
                return MainActivity.this.canScheduleRecordings();
            }

            @Override
            public String updateChannelLabel() {
                return currentUpdateChannelLabel();
            }

            @Override
            public void openTvGuide() {
                showTvAndGuideToolsDialog(currentMenu);
            }

            @Override
            public void openRecordings() {
                showRecordingsSimpleToolsDialog(currentMenu);
            }

            @Override
            public void openVod() {
                showVodLibraryDialog(currentMenu);
            }

            @Override
            public void refreshCatalog() {
                refreshOfflineCatalogFromSettings();
            }

            @Override
            public void openSearchRecents() {
                showSearchAndRecentsToolsDialog(currentMenu);
            }

            @Override
            public void openLists() {
                showListsToolsDialog(currentMenu);
            }

            @Override
            public void openFamily() {
                showFamilyAndSecurityToolsDialog(currentMenu);
            }

            @Override
            public void openAdvanced() {
                showAdvancedToolsMenu(currentMenu);
            }

            @Override
            public void openCurrentChannel() {
                showCurrentChannelQuickActionsDialog(currentMenu);
            }

            @Override
            public void openPlayback() {
                showPlaybackToolsDialog(currentMenu);
            }

            @Override
            public void openNavigation() {
                showNavigationToolsDialog(currentMenu);
            }

            @Override
            public void openMultiview() {
                showMultiviewToolsDialog(currentMenu);
            }

            @Override
            public void openSettings() {
                showSettingsAndDiagnosticsToolsDialog(currentMenu);
            }

            @Override
            public void openTimeline() {
                openTimelineGuideAroundSelection();
            }

            @Override
            public void openVisualEpg() {
                openVisualEpgAroundSelection();
            }

            @Override
            public void openEpgSearch() {
                showEpgSearchDialog();
            }

            @Override
            public void openChannelSearch() {
                showChannelSearchDialog();
            }

            @Override
            public void openGlobalSearch() {
                showGlobalSearchDialog();
            }

            @Override
            public void openRecentChannels() {
                showRecentChannelsDialog();
            }

            @Override
            public void openRecentQuick() {
                showRecentChannelsQuickDialog();
            }

            @Override
            public void openFavoritesQuick() {
                showFavoriteChannelsQuickDialog();
            }

            @Override
            public void openParental() {
                showParentalSettingsDialog(currentMenu);
            }

            @Override
            public void openPersonalListsManager() {
                showPersonalListsManagerDialog();
            }

            @Override
            public void openParentalStatus() {
                showSettingsInfoDialog(R.string.settings_section_parental, buildParentalSettingsSummary());
            }

            @Override
            public void openRecordingsBrowser() {
                MainActivity.this.openRecordingsBrowser();
            }

            @Override
            public void recordCurrentProgram() {
                createScheduleFromEndpoint(getCurrentPlaybackChannelItem(), false);
            }

            @Override
            public void recordNextProgram() {
                createScheduleFromEndpoint(getCurrentPlaybackChannelItem(), true);
            }

            @Override
            public void retryNextRoute() {
                retryCurrentPlaybackWithNextRoute(getCurrentPlaybackChannelItem());
            }

            @Override
            public void retryPlayback() {
                retryCurrentPlayback();
            }

            @Override
            public void openTemporaryPlaybackMode() {
                openCurrentTemporaryPlaybackMode();
            }

            @Override
            public void openAudioTracks() {
                showAudioTrackDialog();
            }

            @Override
            public void openPlaybackDiagnostics() {
                showPlaybackDiagnosticsDialog();
            }

            @Override
            public void openQuickHub() {
                showQuickHubDialog();
            }

            @Override
            public void openCurrentChannelPersonalLists() {
                MainActivity.this.openCurrentChannelPersonalLists();
            }

            @Override
            public void openCurrentChannelProfile() {
                MainActivity.this.openCurrentChannelProfile();
            }

            @Override
            public void openMultiView() {
                MainActivity.this.openMultiView();
            }

            @Override
            public void openMultiViewPreset() {
                showOpenMultiViewPresetDialog();
            }

            @Override
            public void saveMultiViewPreset() {
                showSaveMultiViewPresetDialog();
            }

            @Override
            public void openSettingsCenter() {
                showSettingsCenterDialog(onBack);
            }

            @Override
            public void openSettingsDiagnostics() {
                showSettingsDiagnosticsDialog(onBack);
            }

            @Override
            public void openInstallStatus() {
                showInstallStatusDialog();
            }

            @Override
            public void openUpdateChannel() {
                showUpdateChannelDialog(onBack);
            }
        };
    }

    private void showCurrentChannelQuickActionsDialog() {
        showCurrentChannelQuickActionsDialog(null);
    }

    private void showCurrentChannelQuickActionsDialog(Runnable onBack) {
        ChannelItem channelItem = getCurrentPlaybackChannelItem();
        if (channelItem == null) {
            showStatus(getString(R.string.diagnostics_none));
            return;
        }
        TvOptionsMenuModel menu = ChannelToolsUiFactory.buildCurrentChannel(channelItem, new ChannelToolsUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public boolean favorite(ChannelItem item) {
                return item != null && favoriteChannelIds.contains(item.id);
            }

            @Override
            public void retryRoute(ChannelItem item) {
                retryCurrentPlaybackWithNextRoute(item);
            }

            @Override
            public void temporaryMode(ChannelItem item) {
                showTemporaryPlaybackModeDialog(item);
            }

            @Override
            public void audioTracks() {
                showAudioTrackDialog();
            }

            @Override
            public void toggleFavorite(ChannelItem item) {
                toggleFavoriteForChannel(item);
            }

            @Override
            public void personalLists(ChannelItem item) {
                showPersonalListsDialog(item);
            }

            @Override
            public void profile(ChannelItem item) {
                showChannelProfileDialog(item);
            }

            @Override
            public void miniGuide(ChannelItem item) {
                openMiniGuideForChannel(item);
            }

            @Override
            public void diagnostics() {
                showPlaybackDiagnosticsDialog();
            }
        });
        showTvOptionsDialog(R.string.tools_section_current_channel, displayName(channelItem), menu.options, menu.actions, onBack);
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
        showTvMessagePanel(
                getString(R.string.title_install_status),
                message,
                java.util.Collections.singletonList(new TvMessageActionUiModel(getString(R.string.dialog_close), false, null)),
                null
        );
    }

    private void showSettingsCenterDialog() {
        showSettingsCenterDialog(null);
    }

    private void showSettingsCenterDialog(Runnable onBack) {
        TvOptionsMenuModel menu = SettingsUiFactory.buildCenter(buildSettingsUiHost(onBack));
        showTvOptionsDialog(R.string.title_settings_center, menu.message, menu.options, menu.actions, onBack);
    }

    private String buildSettingsSummary() {
        return getString(
                R.string.settings_summary,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                prefs != null && prefs.getBoolean(PREF_STARTUP_HUB_DISABLED, false) ? getString(R.string.diagnostics_value_no) : getString(R.string.diagnostics_value_yes),
                recordingsPanelController.isAutoRefreshEnabled() ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
                overlayNavigationState.favoritesOnly ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
                globalSearchRecents.size(),
                vodResumePositions.size(),
                recordingResumePositions.size()
        );
    }

    private SettingsUiFactory.Host buildSettingsUiHost(Runnable onBack) {
        return new SettingsUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String text(int resId, Object... args) {
                return getString(resId, args);
            }

            @Override
            public boolean startupEnabled() {
                return prefs == null || !prefs.getBoolean(PREF_STARTUP_HUB_DISABLED, false);
            }

            @Override
            public String updateChannelLabel() {
                return currentUpdateChannelLabel();
            }

            @Override
            public int recentSearchCount() {
                return globalSearchRecents.size();
            }

            @Override
            public boolean playbackRepairEnabled() {
                return MainActivity.this.playbackRepairEnabled;
            }

            @Override
            public String playbackQualityLabel() {
                return MainActivity.this.playbackQualityLabel();
            }

            @Override
            public void openStartup() {
                showStartupSettingsDialog(() -> showSettingsCenterDialog(onBack));
            }

            @Override
            public void openPlayback() {
                showPlaybackSettingsDialog(() -> showSettingsCenterDialog(onBack));
            }

            @Override
            public void openSearch() {
                showSearchSettingsDialog(() -> showSettingsCenterDialog(onBack));
            }

            @Override
            public void openRecordings() {
                showRecordingSettingsDialog(() -> showSettingsCenterDialog(onBack));
            }

            @Override
            public void openLocalData() {
                showLocalDataSettingsDialog(() -> showSettingsCenterDialog(onBack));
            }

            @Override
            public void openParental() {
                showParentalSettingsDialog(() -> showSettingsCenterDialog(onBack));
            }

            @Override
            public void openOfflineSystem() {
                showOfflineSystemDialog(() -> showSettingsCenterDialog(onBack));
            }

            @Override
            public void runFullSync() {
                runManualOfflineFullSync();
            }

            @Override
            public void openOfflineCatalog() {
                showOfflineCatalogSettingsDialog(() -> showSettingsCenterDialog(onBack));
            }

            @Override
            public void checkUpdate() {
                checkAppUpdateManually();
            }

            @Override
            public void openUpdateChannel() {
                showUpdateChannelDialog(() -> showSettingsCenterDialog(onBack));
            }

            @Override
            public void checkRescueUpdate() {
                checkRescueAppUpdateManually();
            }

            @Override
            public void openDiagnostics() {
                showSettingsDiagnosticsDialog(() -> showSettingsCenterDialog(onBack));
            }

            @Override
            public void openReset() {
                showResetSettingsDialog(() -> showSettingsCenterDialog(onBack));
            }

            @Override
            public void toggleStartup() {
                if (startupEnabled()) {
                    disableStartupHub();
                } else {
                    enableStartupHub();
                }
            }

            @Override
            public void showStartupNow() {
                loadStartupHubStateAndShow();
            }

            @Override
            public void setStartupChannel() {
                ChannelItem current = getCurrentPlaybackChannelItem();
                if (current != null) {
                    saveLastChannelId(current.id);
                    showStatus(getString(R.string.status_channel_startup_set));
                }
            }

            @Override
            public void clearLastVod() {
                lastVodId = "";
                if (prefs != null) {
                    prefs.edit().remove(PREF_LAST_VOD_ID).apply();
                }
                showStatus(getString(R.string.settings_status_last_vod_cleared));
            }

            @Override
            public void showStartupSummary() {
                showSettingsInfoDialog(R.string.settings_section_startup, buildStartupSettingsSummary(), () -> showStartupSettingsDialog(onBack));
            }

            @Override
            public void openGlobalSearch() {
                showGlobalSearchDialog();
            }

            @Override
            public void clearRecentSearches() {
                confirmSettingsAction(R.string.settings_search_clear_recent, R.string.settings_confirm_clear_searches, MainActivity.this::clearGlobalSearchRecents, () -> showSearchSettingsDialog(onBack));
            }

            @Override
            public void showSearchSummary() {
                showSettingsInfoDialog(R.string.settings_section_search, getString(R.string.settings_search_summary, globalSearchRecents.size()), () -> showSearchSettingsDialog(onBack));
            }

            @Override
            public void togglePlaybackRepair() {
                MainActivity.this.togglePlaybackRepair();
            }

            @Override
            public void openPlaybackQuality() {
                MainActivity.this.showPlaybackQualityDialog(() -> showPlaybackSettingsDialog(onBack));
            }

            @Override
            public void openTextTracks() {
                MainActivity.this.showTextTrackDialog(() -> showPlaybackSettingsDialog(onBack));
            }

            @Override
            public void openCurrentPlaybackMode() {
                openCurrentTemporaryPlaybackMode();
            }

            @Override
            public void openPlaybackDiagnostics() {
                showPlaybackDiagnosticsDialog();
            }

            @Override
            public void clearLearnedPlaybackModes() {
                confirmSettingsAction(R.string.settings_playback_clear_learned, R.string.settings_confirm_clear_learned_routes, MainActivity.this::clearLearnedPlaybackModes, () -> showPlaybackSettingsDialog(onBack));
            }

            @Override
            public void clearPlaybackModes() {
                confirmSettingsAction(R.string.settings_playback_clear_modes, R.string.settings_confirm_clear_modes, MainActivity.this::clearPlaybackModes, () -> showPlaybackSettingsDialog(onBack));
            }

            @Override
            public void clearPlaybackDiagnostics() {
                confirmSettingsAction(R.string.settings_playback_clear_diagnostics, R.string.settings_confirm_clear_diagnostics, MainActivity.this::clearAllPlaybackDiagnostics, () -> showPlaybackSettingsDialog(onBack));
            }

            @Override
            public void showPlaybackSummary() {
                showSettingsInfoDialog(R.string.settings_section_playback, buildPlaybackSettingsSummary(), () -> showPlaybackSettingsDialog(onBack));
            }

            @Override
            public boolean offlineRecordingsDisabled() {
                return isOfflineRecordingsDisabled();
            }

            @Override
            public boolean recordingsAutoRefreshEnabled() {
                return MainActivity.this.recordingsPanelController.isAutoRefreshEnabled();
            }

            @Override
            public boolean parentalPinConfigured() {
                return parentalControlStore != null && parentalControlStore.hasPinConfigured();
            }

            @Override
            public boolean parentalUnlocked() {
                return parentalControlStore != null && parentalControlStore.isUnlocked();
            }

            @Override
            public String recordingsOfflineSummary() {
                return getString(R.string.settings_recordings_offline_summary, recordingResumePositions.size());
            }

            @Override
            public void toggleRecordingsAutoRefresh() {
                MainActivity.this.toggleRecordingsAutoRefresh();
            }

            @Override
            public void openRecordingsBrowser() {
                MainActivity.this.openRecordingsBrowser();
            }

            @Override
            public void clearRecordingProgress() {
                confirmSettingsAction(R.string.settings_recordings_clear_progress, R.string.settings_confirm_clear_recording_progress, MainActivity.this::clearAllRecordingProgress, () -> showRecordingSettingsDialog(onBack));
            }

            @Override
            public void showRecordingsSummary() {
                if (isOfflineRecordingsDisabled()) {
                    showSettingsInfoDialog(R.string.settings_section_recordings, getString(R.string.settings_recordings_offline_summary, recordingResumePositions.size()), () -> showRecordingSettingsDialog(onBack));
                } else {
                    showSettingsInfoDialog(R.string.settings_section_recordings, getString(R.string.settings_recordings_summary, recordingsPanelController.isAutoRefreshEnabled() ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no), recordingResumePositions.size()), () -> showRecordingSettingsDialog(onBack));
                }
            }

            @Override
            public void clearVodProgress() {
                confirmSettingsAction(R.string.settings_data_clear_vod_progress, R.string.settings_confirm_clear_vod_progress, MainActivity.this::clearAllVodProgress, () -> showLocalDataSettingsDialog(onBack));
            }

            @Override
            public void clearRecentChannels() {
                confirmSettingsAction(R.string.settings_data_clear_recent_channels, R.string.settings_confirm_clear_recent_channels, MainActivity.this::clearRecentChannels, () -> showLocalDataSettingsDialog(onBack));
            }

            @Override
            public void clearFavorites() {
                confirmSettingsAction(R.string.settings_data_clear_favorites, R.string.settings_confirm_clear_favorites, MainActivity.this::clearFavorites, () -> showLocalDataSettingsDialog(onBack));
            }

            @Override
            public void resetListsAndProfiles() {
                confirmSettingsAction(R.string.settings_data_reset_lists_profiles, R.string.settings_confirm_reset_lists_profiles, MainActivity.this::resetListsAndProfiles, () -> showLocalDataSettingsDialog(onBack));
            }

            @Override
            public void showLocalDataSummary() {
                showSettingsInfoDialog(R.string.settings_section_local_data, buildLocalDataSummary(), () -> showLocalDataSettingsDialog(onBack));
            }

            @Override
            public void showParentalStatus() {
                showSettingsInfoDialog(R.string.settings_section_parental, buildParentalSettingsSummary(), () -> showParentalSettingsDialog(onBack));
            }

            @Override
            public void toggleParentalLock() {
                if (parentalControlStore == null) {
                    return;
                }
                if (parentalControlStore.isUnlocked()) {
                    parentalControlStore.lockSession();
                    refreshProtectedContentState();
                    showStatus(getString(R.string.settings_parental_locked));
                } else {
                    showParentalPinPrompt(getString(R.string.settings_section_parental), getString(R.string.parental_unlock_prompt), () -> showStatus(getString(R.string.settings_parental_unlocked)), () -> showParentalSettingsDialog(onBack));
                }
            }

            @Override
            public void changeParentalPin() {
                showParentalPinPrompt(getString(R.string.settings_parental_change_pin), getString(R.string.parental_change_pin_prompt), () -> showParentalPinSetupDialog(true, () -> showParentalSettingsDialog(onBack)), () -> showParentalSettingsDialog(onBack));
            }

            @Override
            public void clearParentalPin() {
                confirmSettingsAction(R.string.settings_parental_clear_pin, R.string.settings_parental_clear_pin_confirm, MainActivity.this::clearParentalPin, () -> showParentalSettingsDialog(onBack));
            }

            @Override
            public void setParentalPin() {
                showParentalPinSetupDialog(false, () -> showParentalSettingsDialog(onBack));
            }
        };
    }

    private void showStartupSettingsDialog() {
        showStartupSettingsDialog(null);
    }

    private void showStartupSettingsDialog(Runnable onBack) {
        TvOptionsMenuModel menu = SettingsUiFactory.buildStartup(buildSettingsUiHost(onBack));
        showTvOptionsDialog(R.string.settings_section_startup, menu.message, menu.options, menu.actions, onBack);
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
        showPlaybackSettingsDialog(null);
    }

    private void showPlaybackSettingsDialog(Runnable onBack) {
        TvOptionsMenuModel menu = SettingsUiFactory.buildPlayback(buildSettingsUiHost(onBack));
        showTvOptionsDialog(R.string.settings_section_playback, menu.message, menu.options, menu.actions, onBack);
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
                errors,
                playbackQualityLabel()
        );
    }

    private void showSearchSettingsDialog() {
        showSearchSettingsDialog(null);
    }

    private void showSearchSettingsDialog(Runnable onBack) {
        TvOptionsMenuModel menu = SettingsUiFactory.buildSearch(buildSettingsUiHost(onBack));
        showTvOptionsDialog(R.string.settings_section_search, menu.message, menu.options, menu.actions, onBack);
    }

    private void showRecordingSettingsDialog() {
        showRecordingSettingsDialog(null);
    }

    private void showRecordingSettingsDialog(Runnable onBack) {
        TvOptionsMenuModel menu = SettingsUiFactory.buildRecordings(buildSettingsUiHost(onBack));
        showTvOptionsDialog(R.string.settings_section_recordings, menu.message, menu.options, menu.actions, onBack);
    }

    private void showLocalDataSettingsDialog() {
        showLocalDataSettingsDialog(null);
    }

    private void showLocalDataSettingsDialog(Runnable onBack) {
        TvOptionsMenuModel menu = SettingsUiFactory.buildLocalData(buildSettingsUiHost(onBack));
        showTvOptionsDialog(R.string.settings_section_local_data, menu.message, menu.options, menu.actions, onBack);
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

    private void showParentalSettingsDialog() {
        showParentalSettingsDialog(null);
    }

    private void showParentalSettingsDialog(Runnable onBack) {
        TvOptionsMenuModel menu = SettingsUiFactory.buildParental(buildSettingsUiHost(onBack));
        showTvOptionsDialog(R.string.settings_section_parental, menu.message, menu.options, menu.actions, onBack);
    }

    private String buildParentalSettingsSummary() {
        int protectedFilters = 0;
        for (ChannelFilter filter : filters) {
            if (isProtectedFilter(filter)) {
                protectedFilters++;
            }
        }
        int protectedChannels = 0;
        int protectedVod = 0;
        for (ChannelItem item : allChannels) {
            if (!isProtectedItem(item)) {
                continue;
            }
            if (item.isVod) {
                protectedVod++;
            } else {
                protectedChannels++;
            }
        }
        String rulesState = hasParentalRules()
                ? getString(R.string.diagnostics_value_yes)
                : getString(R.string.diagnostics_value_no);
        String pinState = parentalControlStore != null && parentalControlStore.hasPinConfigured()
                ? getString(R.string.diagnostics_value_yes)
                : getString(R.string.diagnostics_value_no);
        String sessionState = parentalControlStore != null && parentalControlStore.isUnlocked()
                ? getString(R.string.settings_parental_unlocked_remaining, formatDurationShort(parentalControlStore.getUnlockedRemainingMs()))
                : getString(R.string.settings_parental_locked_short);
        return getString(
                R.string.settings_parental_summary,
                rulesState,
                pinState,
                sessionState,
                protectedFilters,
                protectedChannels,
                protectedVod
        );
    }

    private void showParentalPinSetupDialog(boolean replaceExisting) {
        showParentalPinSetupDialog(replaceExisting, null);
    }

    private void showParentalPinSetupDialog(boolean replaceExisting, Runnable onBack) {
        List<TvTextInputFieldUiModel> fields = new ArrayList<>();
        fields.add(new TvTextInputFieldUiModel(getString(R.string.parental_pin_hint), "", true, true));
        fields.add(new TvTextInputFieldUiModel(getString(R.string.parental_pin_confirm_hint), "", true, true));
        showTvTextInputPanel(new TvTextInputPanelUiModel(
                getString(replaceExisting ? R.string.settings_parental_change_pin : R.string.settings_parental_set_pin),
                getString(R.string.parental_pin_setup_message),
                getString(android.R.string.ok),
                getString(R.string.dialog_cancel),
                "",
                fields,
                values -> {
                    String pin = values == null || values.isEmpty() ? "" : values.get(0);
                    String confirm = values == null || values.size() < 2 ? "" : values.get(1);
                    if (!ParentalControlStore.isValidPin(pin)) {
                        showStatus(getString(R.string.parental_pin_invalid));
                        return;
                    }
                    if (!ParentalControlStore.normalizePin(pin).equals(ParentalControlStore.normalizePin(confirm))) {
                        showStatus(getString(R.string.parental_pin_mismatch));
                        return;
                    }
                    parentalControlStore.setPin(pin);
                    refreshProtectedContentState();
                    showStatus(getString(replaceExisting ? R.string.settings_parental_pin_changed : R.string.settings_parental_pin_set));
                    if (onBack != null) {
                        postUiIfAlive(onBack);
                    }
                },
                () -> {
                    if (onBack != null) {
                        postUiIfAlive(onBack);
                    }
                },
                null
        ));
    }

    private void clearParentalPin() {
        if (parentalControlStore == null) {
            return;
        }
        parentalControlStore.clearPin();
        refreshProtectedContentState();
        showStatus(getString(R.string.settings_parental_pin_cleared));
    }

    private void showParentalPinPrompt(String title, String message, Runnable onSuccess) {
        showParentalPinPrompt(title, message, onSuccess, null);
    }

    private void showParentalPinPrompt(String title, String message, Runnable onSuccess, Runnable onBack) {
        if (parentalControlStore == null) {
            if (onSuccess != null) {
                onSuccess.run();
            }
            return;
        }
        if (!parentalControlStore.hasPinConfigured()) {
            showParentalPinRequiredDialog(onBack);
            return;
        }
        showTvTextInputPanel(new TvTextInputPanelUiModel(
                title,
                message,
                getString(android.R.string.ok),
                getString(R.string.dialog_cancel),
                "",
                java.util.Collections.singletonList(new TvTextInputFieldUiModel(getString(R.string.parental_pin_hint), "", true, true)),
                values -> {
                    String value = values == null || values.isEmpty() ? "" : values.get(0);
                    if (!parentalControlStore.verifyPin(value)) {
                        showStatus(getString(R.string.parental_pin_wrong));
                        return;
                    }
                    parentalControlStore.unlockSession();
                    refreshProtectedContentState();
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                },
                () -> {
                    if (onBack != null) {
                        postUiIfAlive(onBack);
                    }
                },
                null
        ));
    }

    private void showParentalPinRequiredDialog() {
        showParentalPinRequiredDialog(null);
    }

    private void showParentalPinRequiredDialog(Runnable onBack) {
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        actions.add(new TvMessageActionUiModel(getString(R.string.settings_parental_set_pin), false, () -> showParentalPinSetupDialog(false, onBack)));
        actions.add(new TvMessageActionUiModel(getString(R.string.dialog_cancel), false, () -> {
            if (onBack != null) {
                postUiIfAlive(onBack);
            }
        }));
        showTvMessagePanel(getString(R.string.settings_section_parental), getString(R.string.parental_pin_required_setup), actions, onBack);
    }

    private boolean hasParentalRules() {
        return currentOfflinePermissions != null && currentOfflinePermissions.hasParentalRules();
    }

    private boolean isProtectedItem(ChannelItem item) {
        return item != null && currentOfflinePermissions != null && currentOfflinePermissions.isProtectedItem(item);
    }

    private boolean isProtectedFilter(ChannelFilter filter) {
        return filter != null && currentOfflinePermissions != null && currentOfflinePermissions.isProtectedFilter(filter);
    }

    private boolean isProtectedFilterKey(String filterKey) {
        if (filterKey == null || filterKey.trim().isEmpty()) {
            return false;
        }
        for (ChannelFilter filter : filters) {
            if (filter != null && filterKey.equals(filter.key)) {
                return isProtectedFilter(filter);
            }
        }
        return false;
    }

    private String protectedPrefix() {
        return getString(R.string.parental_lock_icon) + " ";
    }

    private String decorateProtectedLabel(String label, boolean protectedEntry) {
        String value = label == null ? "" : label.trim();
        if (!protectedEntry || value.isEmpty()) {
            return value;
        }
        return value.startsWith(getString(R.string.parental_lock_icon)) ? value : protectedPrefix() + value;
    }

    private String decorateProtectedFilterLabel(ChannelFilter filter) {
        if (filter == null) {
            return "";
        }
        return decorateProtectedLabel(filter.label, isProtectedFilter(filter));
    }

    private String decorateProtectedItemTitle(ChannelItem item, String title) {
        return decorateProtectedLabel(title, isProtectedItem(item));
    }

    private String decorateProtectedMeta(ChannelItem item, String meta) {
        String value = meta == null ? "" : meta.trim();
        if (!isProtectedItem(item)) {
            return value;
        }
        if (value.isEmpty()) {
            return getString(R.string.parental_lock_label);
        }
        return getString(R.string.parental_lock_label) + "  ·  " + value;
    }

    private String buildProtectedTypeBadge(ChannelItem item, String fallback) {
        if (isProtectedItem(item)) {
            return getString(R.string.parental_lock_pin_badge);
        }
        return fallback == null ? "" : fallback;
    }

    private boolean isProtectedContentLocked() {
        return hasParentalRules()
                && parentalControlStore != null
                && parentalControlStore.hasPinConfigured()
                && !parentalControlStore.isUnlocked();
    }

    private boolean shouldHideProtectedItem(ChannelItem item) {
        return isProtectedContentLocked() && isProtectedItem(item);
    }

    private void ensureParentalAccessForItem(ChannelItem item, Runnable onAllowed) {
        if (item == null || !isProtectedItem(item)) {
            if (onAllowed != null) {
                onAllowed.run();
            }
            return;
        }
        if (!hasParentalRules()) {
            if (onAllowed != null) {
                onAllowed.run();
            }
            return;
        }
        if (parentalControlStore == null || !parentalControlStore.hasPinConfigured()) {
            showParentalPinRequiredDialog();
            return;
        }
        if (!isProtectedContentLocked()) {
            if (onAllowed != null) {
                onAllowed.run();
            }
            return;
        }
        showParentalPinPrompt(getString(R.string.settings_section_parental), getString(R.string.parental_unlock_content_prompt), onAllowed);
    }

    private void ensureParentalAccessForFilterKey(String filterKey, Runnable onAllowed) {
        if (!isProtectedFilterKey(filterKey)) {
            if (onAllowed != null) {
                onAllowed.run();
            }
            return;
        }
        if (parentalControlStore == null || !parentalControlStore.hasPinConfigured()) {
            showParentalPinRequiredDialog();
            return;
        }
        if (!isProtectedContentLocked()) {
            if (onAllowed != null) {
                onAllowed.run();
            }
            return;
        }
        showParentalPinPrompt(getString(R.string.settings_section_parental), getString(R.string.parental_unlock_filter_prompt), onAllowed);
    }

    private void refreshProtectedContentState() {
        syncOverlayCoordinator();
        String keepCurrentId = overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size() ? channels.get(overlayNavigationState.currentIndex).id : lastChannelId;
        String keepSelectedId = overlayNavigationState.selectedOverlayIndex >= 0 && overlayNavigationState.selectedOverlayIndex < channels.size() ? channels.get(overlayNavigationState.selectedOverlayIndex).id : keepCurrentId;
        channelOverlayCoordinator.refreshVisibleChannels(keepCurrentId, keepSelectedId);
        syncOverlayStateFromCoordinator();
        refreshOverlayChannelList();
        updateFilterText();
        updateOverlaySearchState();
        if (channels.isEmpty()) {
            return;
        }
        if (overlayNavigationState.currentIndex < 0 || overlayNavigationState.currentIndex >= channels.size()) {
            tuneToIndex(0, true);
        } else if (overlayNavigationState.selectedOverlayIndex >= 0 && channelListComposeView != null) {
            scrollOverlayChannelListToPosition(overlayNavigationState.selectedOverlayIndex);
        }
    }

    private void showOfflineSystemDialog() {
        showOfflineSystemDialog(null);
    }

    private void showOfflineSystemDialog(Runnable onBack) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.settings_offline_system_status));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_offline_system, buildOfflineSystemSummary(), () -> showOfflineSystemDialog(onBack)));
        options.add(getString(R.string.settings_offline_full_sync));
        actions.add(this::runManualOfflineFullSync);
        options.add(getString(R.string.offline_catalog_action_repair));
        actions.add(this::repairOfflineCatalog);
        options.add(getString(R.string.settings_offline_sync_history));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_offline_sync_history, buildOfflineSyncHistorySummary(), () -> showOfflineSystemDialog(onBack)));
        options.add(getString(R.string.offline_catalog_action_refresh));
        actions.add(this::refreshOfflineCatalogFromSettings);
        options.add(getString(R.string.app_update_action_check));
        actions.add(this::checkAppUpdateManually);
        options.add(getString(R.string.app_update_channel_action, currentUpdateChannelLabel()));
        actions.add(() -> showUpdateChannelDialog(() -> showOfflineSystemDialog(onBack)));
        options.add(getString(R.string.app_update_action_rescue));
        actions.add(this::checkRescueAppUpdateManually);
        options.add(getString(R.string.app_update_action_diagnostics));
        actions.add(this::showAppUpdateDiagnosticsDialog);
        options.add(getString(R.string.offline_catalog_action_activate_code));
        actions.add(this::startOfflineActivationCodeFlow);
        options.add(getString(R.string.settings_playback_diagnostics));
        actions.add(this::showPlaybackDiagnosticsDialog);
        showTvOptionsDialog(R.string.settings_section_offline_system, buildOfflineSystemSummary(), options, actions, onBack);
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
        String updateSummary = getString(R.string.app_update_channel_current, currentUpdateChannelLabel()) + "\n" + updateState;
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
                updateSummary,
                buildRecentDiagnosticsSummary(),
                lastMaintenance,
                maintenanceError,
                buildNextOfflineSyncSummary(status),
                buildOfflineCatalogGuardSummary(status)
        );
    }

    private String buildOfflineCatalogGuardSummary(CatalogSnapshotStore.SnapshotStatus status) {
        if (status == null || status.lastRejectedAtMs <= 0L) {
            return getString(R.string.diagnostics_value_no);
        }
        String reason = status.lastRejectedReason == null || status.lastRejectedReason.trim().isEmpty()
                ? getString(R.string.diagnostics_value_unknown)
                : status.lastRejectedReason.trim();
        return getString(
                R.string.settings_offline_catalog_guard_rejected,
                formatDateTime(status.lastRejectedAtMs),
                status.lastRejectedCandidateChannels,
                status.lastRejectedPreviousChannels,
                status.lastRejectedCandidateTotal,
                status.lastRejectedPreviousTotal,
                reason
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
        String diagnostic = buildAppUpdateDiagnosticSummary();
        if (lastAppUpdateError != null && !lastAppUpdateError.trim().isEmpty()) {
            return appendSummary(getString(R.string.settings_update_state_error, checked, classifyOperationalError(lastAppUpdateError), lastAppUpdateError), diagnostic);
        }
        if (lastKnownAppUpdateInfo != null && lastKnownAppUpdateInfo.isNewerThanCurrent()) {
            return appendSummary(getString(R.string.settings_update_state_available, checked, safeUpdateVersionName(lastKnownAppUpdateInfo), lastKnownAppUpdateInfo.versionCode), diagnostic);
        }
        if (lastKnownAppUpdateInfo != null) {
            return appendSummary(getString(R.string.settings_update_state_current, checked, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), diagnostic);
        }
        return appendSummary(getString(R.string.settings_update_state_unknown, checked), diagnostic);
    }

    private String appendSummary(String base, String extra) {
        if (extra == null || extra.trim().isEmpty()) {
            return base == null ? "" : base;
        }
        if (base == null || base.trim().isEmpty()) {
            return extra.trim();
        }
        return base + "\n" + extra.trim();
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
        showOfflineCatalogSettingsDialog(null);
    }

    private void showOfflineCatalogSettingsDialog(Runnable onBack) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.settings_offline_system_status));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_offline_system, buildOfflineSystemSummary(), () -> showOfflineCatalogSettingsDialog(onBack)));
        options.add(getString(R.string.settings_offline_full_sync));
        actions.add(this::runManualOfflineFullSync);
        options.add(getString(R.string.offline_catalog_action_repair));
        actions.add(this::repairOfflineCatalog);
        options.add(getString(R.string.offline_catalog_action_activate_code));
        actions.add(this::startOfflineActivationCodeFlow);
        options.add(getString(R.string.offline_catalog_action_refresh));
        actions.add(this::refreshOfflineCatalogFromSettings);
        options.add(getString(R.string.offline_catalog_action_verify));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_offline_catalog, buildOfflineCatalogVerificationSummary(), () -> showOfflineCatalogSettingsDialog(onBack)));
        options.add(getString(R.string.offline_catalog_action_set_url));
        actions.add(this::showOfflineCatalogUrlDialog);
        options.add(getString(R.string.offline_catalog_action_set_token));
        actions.add(this::showOfflineCatalogTokenDialog);
        options.add(getString(R.string.offline_catalog_action_clear));
        actions.add(() -> confirmSettingsAction(R.string.offline_catalog_action_clear, R.string.offline_catalog_confirm_clear, this::clearOfflineCatalog, () -> showOfflineCatalogSettingsDialog(onBack)));
        options.add(getString(R.string.settings_action_view_summary));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_offline_catalog, buildOfflineCatalogSummary(), () -> showOfflineCatalogSettingsDialog(onBack)));
        showTvOptionsDialog(R.string.settings_section_offline_catalog, null, options, actions, onBack);
    }

    private void startOfflineActivationCodeFlow() {
        if (catalogSnapshotStore == null) {
            return;
        }
        showStatus(getString(R.string.offline_catalog_activation_waiting));
        ioExecutor.execute(() -> {
            try {
                JSONObject payload = catalogSnapshotStore.startActivation(
                        BuildConfig.OFFLINE_BASE_URL,
                        buildOfflineActivationDeviceLabel()
                );
                String code = payload.optString("code", "").trim();
                postUiIfAlive(() -> showOfflineActivationCodeDialog(code));
            } catch (Exception e) {
                Log.e(TAG, "offline activation start failed", e);
                postUiIfAlive(() -> showError(getString(R.string.offline_catalog_activation_error, e.getMessage())));
            }
        });
    }

    private void showOfflineActivationCodeDialog(String code) {
        if (code == null || code.trim().isEmpty()) {
            showError(getString(R.string.offline_catalog_activation_error, "codigo vacio"));
            return;
        }
        final boolean[] active = {true};
        final int[] attempts = {0};
        prepareModalSurface();
        final Dialog[] dialogHolder = new Dialog[1];
        ComposeView composeView = new ComposeView(this);
        attachDialogViewTreeOwners(composeView);
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        actions.add(new TvMessageActionUiModel(getString(R.string.dialog_cancel), false, () -> {
            if (dialogHolder[0] != null) {
                dialogHolder[0].dismiss();
            }
        }));
        TvMessagePanelComposeBinder.bind(
                composeView,
                new TvMessagePanelUiModel(
                        getString(R.string.offline_catalog_activation_title),
                        getString(R.string.offline_catalog_activation_message, formatActivationCode(code)),
                        actions
                )
        );
        Dialog dialog = ComposeDialogHost.showFullscreen(this, composeView, () -> {
            active[0] = false;
            handleModalDismissed();
        });
        dialogHolder[0] = dialog;
        handleModalShown();
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
                    postUiIfAlive(() -> {
                        active[0] = false;
                        if (dialog != null && dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        showStatus(getString(R.string.offline_catalog_activation_approved));
                        showOfflineActivationSummaryAfterRefresh = true;
                        refreshOfflineCatalogFromSettings();
                    });
                    return;
                }
                postUiDelayedIfAlive(() -> pollOfflineActivationCode(code, active, attempts, dialog), 3000L);
            } catch (Exception e) {
                Log.e(TAG, "offline activation poll failed", e);
                postUiDelayedIfAlive(() -> pollOfflineActivationCode(code, active, attempts, dialog), 3000L);
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
        String startupCacheHitAt = status.lastStartupCacheHitMs <= 0L
                ? getString(R.string.diagnostics_value_no)
                : formatDateTime(status.lastStartupCacheHitMs);
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
                status.sourceUrl == null || status.sourceUrl.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : DiagnosticRedactor.sanitizeUrl(status.sourceUrl),
                status.sourceBaseUrl == null || status.sourceBaseUrl.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : DiagnosticRedactor.sanitizeUrl(status.sourceBaseUrl),
                status.deviceId == null || status.deviceId.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : status.deviceId,
                status.hasAccessToken ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
                status.subject == null || status.subject.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : status.subject,
                status.permissions == null || status.permissions.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : status.permissions,
                permissionsChangedAt,
                status.hasLastGoodBackup ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
                startupCacheHitAt,
                buildOfflineVerificationSummary(verification),
                buildShortFingerprint(status.catalogFingerprint),
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
        String sourceUrl = status == null || status.sourceUrl == null ? "" : status.sourceUrl.trim();
        if (status == null || !status.hasAccessToken || sourceUrl.isEmpty()) {
            reportOfflineDeviceStatus(getString(R.string.settings_offline_sync_catalog), false, 0L, getString(R.string.settings_offline_next_sync_blocked));
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
                postUiIfAlive(() -> {
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
                    reportOfflineDeviceStatus(getString(R.string.settings_offline_sync_catalog), true, durationMs, refreshDetail);
                    if (result != null && "refresh-unchanged".equals(result.loadSource)) {
                        Log.i(TAG, "catalog refresh unchanged; keeping current UI state");
                    } else {
                        applyLoadedChannels(result);
                        runPostUpdateStartupHealthCheck("catalog-refresh", result);
                    }
                    if (manual) {
                        showStatus(refreshDetail);
                    }
                    maybeShowOfflineActivationReadySummary(afterStatus);
                });
            } catch (Exception | OutOfMemoryError e) {
                Log.e(TAG, "offline catalog refresh failed", e);
                long durationMs = System.currentTimeMillis() - startMs;
                CatalogLoadResult fallback = null;
                String fallbackError = "";
                if (shouldFallbackOnFailure && !(e instanceof OutOfMemoryError)) {
                    try {
                        fallback = catalogRepository.fetchLastKnownGoodSnapshotCatalog();
                    } catch (Exception fallbackErr) {
                        fallbackError = fallbackErr.getMessage();
                    }
                }
                CatalogLoadResult finalFallback = fallback;
                String finalFallbackError = fallbackError;
                postUiIfAlive(() -> {
                    offlineCatalogRefreshRunning = false;
                    lastOfflineCatalogRefreshError = e.getMessage();
                    lastOfflineMaintenanceError = e.getMessage();
                    recordOfflineSyncEvent(getString(R.string.settings_offline_sync_catalog), false, durationMs, e.getMessage());
                    reportOfflineDeviceStatus(getString(R.string.settings_offline_sync_catalog), false, durationMs, e.getMessage());
                    if (finalFallback != null) {
                        lastCatalogLoadDurationMs = durationMs;
                        applyLoadedChannels(finalFallback);
                        runPostUpdateStartupHealthCheck("last-good-catalog", finalFallback);
                        showStatus(getString(R.string.offline_catalog_status_using_last_good));
                        recordOfflineSyncEvent(getString(R.string.settings_offline_sync_catalog), true, durationMs, getString(R.string.offline_catalog_status_using_last_good));
                        reportOfflineDeviceStatus(getString(R.string.settings_offline_sync_catalog), true, durationMs, getString(R.string.offline_catalog_status_using_last_good));
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

    private void reportOfflineDeviceStatus(String event, boolean success, long durationMs, String detail) {
        if (!BuildConfig.STANDALONE_MODE || catalogSnapshotStore == null || controlExecutor == null) {
            return;
        }
        CatalogSnapshotStore.SnapshotStatus status = catalogSnapshotStore.getStatus(BuildConfig.CATALOG_SNAPSHOT_URL);
        JSONObject extra = buildOfflineDeviceStatusExtra();
        submitControlTask("offline-device-status", () -> {
            try {
                JSONObject response = catalogSnapshotStore.reportDeviceStatus(BuildConfig.OFFLINE_BASE_URL, status, event, success, durationMs, detail, extra);
                if (response != null && response.optBoolean("diagnostic_requested", false)) {
                    sendRemoteDiagnosticReport(status);
                }
                handleOfflineRemoteCommands(response);
            } catch (Exception e) {
                Log.d(TAG, "offline device status report failed", e);
            }
        });
    }

    private void handleOfflineRemoteCommands(JSONObject response) {
        if (response == null || uiHandler == null) {
            return;
        }
        boolean forceCatalogRefresh = response.optBoolean("force_catalog_refresh_requested", false);
        boolean appUpdateCheck = response.optBoolean("app_update_check_requested", false);
        boolean wipeRequested = response.optBoolean("wipe_requested", false);
        if (!forceCatalogRefresh && !appUpdateCheck && !wipeRequested) {
            return;
        }
        postUiIfAlive(() -> {
            if (wipeRequested) {
                performOfflineRemoteWipe();
                return;
            }
            if (forceCatalogRefresh) {
                showStatus(getString(R.string.offline_catalog_status_refreshing));
                refreshOfflineCatalog(false, true, true);
            }
            if (appUpdateCheck) {
                checkAppUpdate(false);
            }
        });
    }

    private void sendRemoteDiagnosticReport(CatalogSnapshotStore.SnapshotStatus status) {
        try {
            JSONObject extra = buildOfflineDeviceStatusExtra();
            extra.put("remote_diagnostic", buildRemoteDiagnosticPayload(status));
            catalogSnapshotStore.reportDeviceStatus(
                    BuildConfig.OFFLINE_BASE_URL,
                    status,
                    "Diagnostico remoto",
                    true,
                    0L,
                    "Diagnostico remoto enviado",
                    extra
            );
        } catch (Exception e) {
            Log.d(TAG, "remote diagnostic report failed", e);
        }
    }

    private JSONObject buildRemoteDiagnosticPayload(CatalogSnapshotStore.SnapshotStatus status) {
        JSONObject payload = new JSONObject();
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            ChannelItem current = getCurrentPlaybackChannelItem();
            PlayerController.PlaybackDiagnostics playbackDiagnostics = playerController == null ? null : playerController.getPlaybackDiagnostics();
            boolean playbackServerTraffic = current != null && isServerTrafficHeartbeat(current, playbackDiagnostics);
            double playbackEstimatedMbps = estimatePlaybackMbps(playbackDiagnostics);
            PlaybackHealthClassifier.Result playbackHealth = PlaybackHealthClassifier.classify(playbackDiagnostics, playbackHeartbeatStartedAtMs <= 0L ? 0L : Math.max(0L, System.currentTimeMillis() - playbackHeartbeatStartedAtMs));
            payload.put("report_id", UUID.randomUUID().toString())
                    .put("created_at_ms", System.currentTimeMillis())
                    .put("package_name", getPackageName())
                    .put("version_name", BuildConfig.VERSION_NAME)
                    .put("version_code", BuildConfig.VERSION_CODE)
                    .put("update_channel", currentUpdateChannel())
                    .put("uptime_ms", activityCreatedAtMs <= 0L ? 0L : Math.max(0L, System.currentTimeMillis() - activityCreatedAtMs))
                    .put("channels_visible", channels.size())
                    .put("channels_total", allChannels.size())
                    .put("selected_filter", overlayNavigationState.selectedFilterKey == null ? "" : overlayNavigationState.selectedFilterKey)
                    .put("last_catalog_load_ms", lastCatalogLoadDurationMs)
                    .put("last_apply_channels_ms", lastApplyChannelsDurationMs)
                    .put("last_epg_now_load_ms", lastEpgNowLoadDurationMs)
                    .put("epg_progress_state", epgProgressState)
                    .put("epg_progress_filter_key", epgProgressFilterKey)
                    .put("epg_progress_label", epgProgressLabel)
                    .put("epg_progress_loaded_channels", epgProgressLoadedChannels)
                    .put("epg_progress_total_channels", epgProgressTotalChannels)
                    .put("epg_progress_last_batch_channels", epgProgressLastBatchChannels)
                    .put("epg_progress_last_batch_updates", epgProgressLastBatchUpdates)
                    .put("epg_progress_completed_filters", epgProgressCompletedFilters)
                    .put("epg_progress_loaded_filter_keys", epgLoadedFilterKeys.size())
                    .put("epg_progress_queued_filter_keys", epgQueuedFilterKeys.size())
                    .put("epg_progress_started_at_ms", epgProgressStartedAtMs)
                    .put("epg_progress_completed_at_ms", epgProgressCompletedAtMs)
                    .put("epg_progress_last_error", epgProgressLastError)
                    .put("vod_loading_active", isVodLoadingActive())
                    .put("vod_loading_channel_id", vodLoadingChannelId == null ? "" : vodLoadingChannelId)
                    .put("vod_loading_kind", vodLoadingKind == null ? "" : vodLoadingKind)
                    .put("vod_loading_title", vodLoadingTitle == null ? "" : vodLoadingTitle)
                    .put("vod_loading_step", vodLoadingStep == null ? "" : vodLoadingStep)
                    .put("vod_loading_detail", vodLoadingDetail == null ? "" : vodLoadingDetail)
                    .put("vod_loading_elapsed_ms", currentVodLoadingElapsedMs())
                    .put("last_app_update_check_ms", lastAppUpdateCheckMs)
                    .put("last_app_update_error", lastAppUpdateError == null ? "" : lastAppUpdateError)
                    .put("last_catalog_error", lastOfflineCatalogRefreshError == null ? "" : lastOfflineCatalogRefreshError)
                    .put("last_maintenance_error", lastOfflineMaintenanceError == null ? "" : lastOfflineMaintenanceError)
                    .put("memory_used_bytes", usedMemory)
                    .put("memory_max_bytes", runtime.maxMemory())
                    .put("free_space_bytes", getFilesDir() == null ? 0L : getFilesDir().getFreeSpace());
            if (current != null) {
                payload.put("current_channel_id", current.id == null ? "" : current.id)
                        .put("current_channel", displayName(current))
                        .put("current_platform", current.platformName == null ? "" : current.platformName)
                        .put("current_group", current.group == null ? "" : current.group)
                        .put("playback_route_class", classifyPlaybackRoute(current, playbackDiagnostics))
                        .put("playback_traffic_scope", playbackTrafficScope(current, playbackDiagnostics, playbackServerTraffic))
                        .put("playback_server_traffic", playbackServerTraffic)
                        .put("playback_quality_label", formatPlaybackQualityCompact(playbackDiagnostics))
                        .put("playback_estimated_mbps", playbackEstimatedMbps)
                        .put("playback_estimated_mb_per_hour", estimatePlaybackMegabytesPerHour(playbackEstimatedMbps))
                        .put("playback_health_level", playbackHealth.level)
                        .put("playback_health_summary", playbackHealth.summary)
                        .put("playback_rebuffer_ratio", playbackHealth.rebufferRatio);
            }
            if (playbackDiagnostics != null) {
                payload.put("playback_state", playbackDiagnostics.playbackState)
                        .put("playback_phase", playbackDiagnostics.playbackPhase)
                        .put("playback_attempt_generation", playbackDiagnostics.attemptGeneration)
                        .put("playback_prepare_elapsed_ms", playbackDiagnostics.prepareElapsedMs)
                        .put("playback_ready_elapsed_ms", playbackDiagnostics.readyElapsedMs)
                        .put("playback_buffering_count", playbackDiagnostics.bufferingCount)
                        .put("playback_buffering_total_ms", playbackDiagnostics.bufferingTotalMs)
                        .put("playback_first_frame_rendered", playbackDiagnostics.firstFrameRendered)
                        .put("playback_route", playbackDiagnostics.routeLabel)
                        .put("playback_mode", playbackDiagnostics.playbackMode)
                        .put("playback_mime", playbackDiagnostics.mimeType)
                        .put("playback_drm", playbackDiagnostics.drmType)
                        .put("playback_video_width", playbackDiagnostics.videoWidth)
                        .put("playback_video_height", playbackDiagnostics.videoHeight)
                        .put("playback_video_codec", playbackDiagnostics.videoCodec == null ? "" : playbackDiagnostics.videoCodec)
                        .put("playback_video_bitrate", playbackDiagnostics.videoBitrate)
                        .put("playback_video_frame_rate", playbackDiagnostics.videoFrameRate)
                        .put("playback_audio_codec", playbackDiagnostics.audioCodec == null ? "" : playbackDiagnostics.audioCodec)
                        .put("playback_error", playbackDiagnostics.lastError == null ? "" : playbackDiagnostics.lastError);
            }
            if (status != null) {
                payload.put("catalog_available", status.available)
                        .put("catalog_expired", status.expired)
                        .put("catalog_channels", status.channelCount)
                        .put("catalog_vod", status.vodCount)
                        .put("catalog_epg_channels", status.epgChannelCount)
                        .put("catalog_epg_programs", status.epgProgramCount)
                        .put("catalog_subject", status.subject)
                        .put("catalog_verification_state", status.verificationState)
                        .put("catalog_verification_message", status.verificationMessage)
                        .put("catalog_payload_fingerprint", status.payloadFingerprint)
                        .put("catalog_fingerprint", status.catalogFingerprint)
                        .put("catalog_permissions_fingerprint", status.permissionsFingerprint)
                        .put("last_startup_cache_hit_ms", status.lastStartupCacheHitMs)
                        .put("catalog_last_rejected_at_ms", status.lastRejectedAtMs)
                        .put("catalog_last_rejected_reason", status.lastRejectedReason)
                        .put("catalog_last_rejected_previous_channels", status.lastRejectedPreviousChannels)
                        .put("catalog_last_rejected_candidate_channels", status.lastRejectedCandidateChannels)
                        .put("catalog_last_rejected_previous_total", status.lastRejectedPreviousTotal)
                        .put("catalog_last_rejected_candidate_total", status.lastRejectedCandidateTotal);
            }
        } catch (Exception e) {
            Log.d(TAG, "remote diagnostic payload failed", e);
        }
        return payload;
    }

    private void startPlaybackHeartbeat(ChannelItem channel) {
        if (!BuildConfig.STANDALONE_MODE || channel == null || catalogSnapshotStore == null) {
            return;
        }
        playbackHeartbeatChannel = channel;
        playbackHeartbeatSessionId = catalogSnapshotStore.getDeviceId()
                + "-"
                + safeHeartbeatText(channel.id)
                + "-"
                + System.currentTimeMillis();
        playbackHeartbeatStartedAtMs = System.currentTimeMillis();
        uiHandler.removeCallbacks(playbackHeartbeatRunnable);
        sendPlaybackHeartbeat("start");
        postUiDelayedIfAlive(playbackHeartbeatRunnable, PLAYBACK_HEARTBEAT_INTERVAL_MS);
    }

    private void stopPlaybackHeartbeat(String state) {
        if (playbackHeartbeatChannel == null || playbackHeartbeatSessionId == null) {
            uiHandler.removeCallbacks(playbackHeartbeatRunnable);
            return;
        }
        sendPlaybackHeartbeat(state == null || state.trim().isEmpty() ? "stop" : state.trim());
        playbackHeartbeatChannel = null;
        playbackHeartbeatSessionId = null;
        playbackHeartbeatStartedAtMs = 0L;
        uiHandler.removeCallbacks(playbackHeartbeatRunnable);
    }

    private void sendPlaybackHeartbeat(String state) {
        if (!BuildConfig.STANDALONE_MODE || catalogSnapshotStore == null || controlExecutor == null) {
            return;
        }
        ChannelItem channel = playbackHeartbeatChannel;
        String sessionId = playbackHeartbeatSessionId;
        if (channel == null || sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }
        long positionMs = playerController == null ? 0L : playerController.getCurrentPlaybackPosition();
        PlayerController.PlaybackDiagnostics diagnostics = playerController == null ? null : playerController.getPlaybackDiagnostics();
        long startupMs = playbackHeartbeatStartedAtMs <= 0L ? 0L : Math.max(0L, System.currentTimeMillis() - playbackHeartbeatStartedAtMs);
        String normalizedState = state == null ? "heartbeat" : state.trim();
        boolean actualDirectPlayback = isDirectPlaybackHeartbeat(channel, diagnostics);
        boolean serverTraffic = isServerTrafficHeartbeat(channel, diagnostics);
        double estimatedMbps = estimatePlaybackMbps(diagnostics);
        PlaybackHealthClassifier.Result playbackHealth = PlaybackHealthClassifier.classify(diagnostics, startupMs);
        JSONObject payload = new JSONObject();
        try {
            payload.put("session_id", sessionId)
                    .put("state", normalizedState)
                    .put("content_type", channel.isVod ? "vod" : "live")
                    .put("channel_id", channel.id == null ? "" : channel.id)
                    .put("channel_name", displayName(channel))
                    .put("platform", channel.platformName == null ? "" : channel.platformName)
                    .put("group", channel.group == null ? "" : channel.group)
                    .put("program_title", currentProgramTitleForHeartbeat(channel))
                    .put("thumbnail_url", thumbnailUrlForHeartbeat(channel))
                    .put("position_ms", Math.max(0L, positionMs))
                    .put("direct_playback", actualDirectPlayback)
                    .put("catalog_direct_playback", channel.directPlayback)
                    .put("server_traffic", serverTraffic)
                    .put("traffic_scope", playbackTrafficScope(channel, diagnostics, serverTraffic))
                    .put("route_class", classifyPlaybackRoute(channel, diagnostics))
                    .put("quality_label", formatPlaybackQualityCompact(diagnostics))
                    .put("estimated_mbps", estimatedMbps)
                    .put("estimated_mb_per_hour", estimatePlaybackMegabytesPerHour(estimatedMbps))
                    .put("playback_health_level", playbackHealth.level)
                    .put("playback_health_summary", playbackHealth.summary)
                    .put("playback_rebuffer_ratio", playbackHealth.rebufferRatio)
                    .put("vod_loading_active", isVodLoadingActive())
                    .put("vod_loading_kind", vodLoadingKind == null ? "" : vodLoadingKind)
                    .put("vod_loading_title", vodLoadingTitle == null ? "" : vodLoadingTitle)
                    .put("vod_loading_step", vodLoadingStep == null ? "" : vodLoadingStep)
                    .put("vod_loading_detail", vodLoadingDetail == null ? "" : vodLoadingDetail)
                    .put("vod_loading_elapsed_ms", currentVodLoadingElapsedMs())
                    .put("playback_profile", channel.playbackProfile == null ? "" : channel.playbackProfile)
                    .put("startup_ms", "ready".equalsIgnoreCase(normalizedState) ? startupMs : 0L);
            if (diagnostics != null) {
                payload.put("playback_mode", diagnostics.playbackMode == null ? "" : diagnostics.playbackMode)
                        .put("playback_phase", diagnostics.playbackPhase == null ? "" : diagnostics.playbackPhase)
                        .put("playback_attempt_generation", diagnostics.attemptGeneration)
                        .put("playback_prepare_elapsed_ms", diagnostics.prepareElapsedMs)
                        .put("playback_ready_elapsed_ms", diagnostics.readyElapsedMs)
                        .put("playback_buffering_count", diagnostics.bufferingCount)
                        .put("playback_buffering_total_ms", diagnostics.bufferingTotalMs)
                        .put("playback_first_frame_rendered", diagnostics.firstFrameRendered)
                        .put("route_label", diagnostics.routeLabel == null ? "" : diagnostics.routeLabel)
                        .put("target_url", DiagnosticRedactor.sanitizeUrl(diagnostics.targetUrl))
                        .put("mime_type", diagnostics.mimeType == null ? "" : diagnostics.mimeType)
                        .put("drm_type", diagnostics.drmType == null ? "" : diagnostics.drmType)
                        .put("video_width", diagnostics.videoWidth)
                        .put("video_height", diagnostics.videoHeight)
                        .put("video_codec", diagnostics.videoCodec == null ? "" : diagnostics.videoCodec)
                        .put("video_bitrate", diagnostics.videoBitrate)
                        .put("video_frame_rate", diagnostics.videoFrameRate)
                        .put("audio_codec", diagnostics.audioCodec == null ? "" : diagnostics.audioCodec)
                        .put("server_traffic", serverTraffic);
                if ("error".equalsIgnoreCase(normalizedState)) {
                    payload.put("error_message", diagnostics.lastError == null ? "" : diagnostics.lastError)
                            .put("error_category", diagnostics.playbackState == null ? "" : diagnostics.playbackState);
                }
            } else {
                payload.put("server_traffic", serverTraffic);
            }
        } catch (Exception e) {
            Log.d(TAG, "playback heartbeat payload failed", e);
            return;
        }
        submitControlTask("playback-heartbeat", () -> {
            try {
                catalogSnapshotStore.reportPlaybackHeartbeat(BuildConfig.OFFLINE_BASE_URL, payload);
            } catch (Exception e) {
                Log.d(TAG, "playback heartbeat failed", e);
            }
        });
    }

    private String currentProgramTitleForHeartbeat(ChannelItem channel) {
        EpgRepository.EpgProgramPair pair = channel == null ? null : epgProgramPairByChannelId.get(channel.id);
        EpgRepository.EpgProgram current = pair == null ? null : pair.current;
        if (current != null && current.title != null && !current.title.trim().isEmpty()) {
            return current.title.trim();
        }
        return channel == null || channel.nowProgram == null ? "" : channel.nowProgram.trim();
    }

    private String thumbnailUrlForHeartbeat(ChannelItem channel) {
        EpgRepository.EpgProgramPair pair = channel == null ? null : epgProgramPairByChannelId.get(channel.id);
        EpgRepository.EpgProgram current = pair == null ? null : pair.current;
        if (current != null && current.icon != null && !current.icon.trim().isEmpty()) {
            return current.icon.trim();
        }
        return channel == null || channel.logoUrl == null ? "" : channel.logoUrl.trim();
    }

    private boolean isServerTrafficHeartbeat(ChannelItem channel, PlayerController.PlaybackDiagnostics diagnostics) {
        if (isDirectPlaybackHeartbeat(channel, diagnostics)) {
            return false;
        }
        if (isRuntimeManifestOnlyHeartbeat(channel, diagnostics)) {
            return false;
        }
        String profile = channel == null || channel.playbackProfile == null ? "" : channel.playbackProfile.trim().toLowerCase(Locale.ROOT);
        if ("server_live".equals(profile) || "hevc_hls".equals(profile)) {
            return true;
        }
        String target = diagnostics == null || diagnostics.targetUrl == null ? "" : diagnostics.targetUrl.trim().toLowerCase(Locale.ROOT);
        if (target.isEmpty()) {
            return false;
        }
        if (target.startsWith("/") && isKnownBackendTrafficPath(target)) {
            return true;
        }
        boolean backendHosted = isBackendHostedTarget(target);
        if (backendHosted && isKnownBackendTrafficPath(target)) {
            return true;
        }
        return target.contains("/proxy/")
                || target.contains("/recordings/")
                || target.contains("/hls/")
                || target.contains("/drm/")
                || target.contains("/api/vod/movistar/")
                || target.contains("/api/u7d/movistar/")
                || target.contains("/api/offline/u7d/");
    }

    private String classifyPlaybackRoute(ChannelItem channel, PlayerController.PlaybackDiagnostics diagnostics) {
        if (channel == null) {
            return "unknown";
        }
        if (isRuntimeManifestOnlyHeartbeat(channel, diagnostics)) {
            return "direct_video_manifest_proxy";
        }
        if (isDirectPlaybackHeartbeat(channel, diagnostics)) {
            return hasBackendAssistTarget(diagnostics) ? "direct_video_backend_assist" : "direct_video";
        }
        if (isServerTrafficHeartbeat(channel, diagnostics)) {
            return "server_video";
        }
        String route = diagnostics == null || diagnostics.routeLabel == null ? "" : diagnostics.routeLabel.trim().toLowerCase(Locale.ROOT);
        String mode = diagnostics == null || diagnostics.playbackMode == null ? "" : diagnostics.playbackMode.trim().toLowerCase(Locale.ROOT);
        String target = diagnostics == null || diagnostics.targetUrl == null ? "" : diagnostics.targetUrl.trim().toLowerCase(Locale.ROOT);
        if (route.contains("drm") || mode.contains("drm") || target.contains("/drm/")) {
            return "license_server";
        }
        if (route.contains("proxy") || mode.contains("proxy") || hasBackendAssistTarget(diagnostics)) {
            return "backend_assist";
        }
        return "unknown";
    }

    private String playbackTrafficScope(ChannelItem channel, PlayerController.PlaybackDiagnostics diagnostics, boolean serverTraffic) {
        if (serverTraffic) {
            return "video_server";
        }
        if (isDirectPlaybackHeartbeat(channel, diagnostics)) {
            return hasBackendAssistTarget(diagnostics) ? "video_direct_backend_assist" : "video_direct";
        }
        if (hasBackendAssistTarget(diagnostics)) {
            return "backend_assist";
        }
        return "unknown";
    }

    private boolean hasBackendAssistTarget(PlayerController.PlaybackDiagnostics diagnostics) {
        String target = diagnostics == null || diagnostics.targetUrl == null ? "" : diagnostics.targetUrl.trim().toLowerCase(Locale.ROOT);
        return isBackendHostedTrafficTarget(target)
                || target.contains("/drm/")
                || target.contains("/api/vod/")
                || target.contains("/api/u7d/")
                || target.contains("/api/offline/u7d/");
    }

    private double estimatePlaybackMbps(PlayerController.PlaybackDiagnostics diagnostics) {
        if (diagnostics == null) {
            return 0d;
        }
        if (diagnostics.videoBitrate > 0) {
            return Math.round((diagnostics.videoBitrate / 1_000_000d) * 10d) / 10d;
        }
        int height = diagnostics.videoHeight;
        if (height >= 2160) {
            return 16d;
        }
        if (height >= 1080) {
            return 6d;
        }
        if (height >= 720) {
            return 3d;
        }
        if (height >= 576) {
            return 2d;
        }
        if (height >= 360) {
            return 1.2d;
        }
        return 0d;
    }

    private long estimatePlaybackMegabytesPerHour(double estimatedMbps) {
        if (estimatedMbps <= 0d) {
            return 0L;
        }
        return Math.round(estimatedMbps * 450d);
    }

    private boolean isDirectPlaybackHeartbeat(ChannelItem channel, PlayerController.PlaybackDiagnostics diagnostics) {
        if (isRuntimeManifestOnlyHeartbeat(channel, diagnostics)) {
            return true;
        }
        String target = diagnostics == null || diagnostics.targetUrl == null ? "" : diagnostics.targetUrl.trim().toLowerCase(Locale.ROOT);
        if (!target.isEmpty()
                && !isBackendHostedTrafficTarget(target)) {
            return true;
        }
        String playbackMode = diagnostics == null || diagnostics.playbackMode == null ? "" : diagnostics.playbackMode.trim().toLowerCase(Locale.ROOT);
        if ("direct".equals(playbackMode)) {
            return true;
        }
        String route = diagnostics == null || diagnostics.routeLabel == null ? "" : diagnostics.routeLabel.trim().toLowerCase(Locale.ROOT);
        if (route.startsWith("directo") || "direct".equals(route)) {
            return true;
        }
        return false;
    }

    private boolean isRuntimeManifestOnlyHeartbeat(ChannelItem channel, PlayerController.PlaybackDiagnostics diagnostics) {
        if (channel == null || diagnostics == null || diagnostics.targetUrl == null) {
            return false;
        }
        String platform = channel.platformName == null ? "" : channel.platformName.trim().toLowerCase(Locale.ROOT);
        String filter = channel.vodFilterKey == null ? "" : channel.vodFilterKey.trim().toLowerCase(Locale.ROOT);
        if (!platform.contains("runtime") && !filter.contains("runtime")) {
            return false;
        }
        String target = diagnostics.targetUrl.trim().toLowerCase(Locale.ROOT);
        return target.contains("/proxy/manifest/") || target.contains("/api/vod/runtime/stream/");
    }

    private boolean isBackendHostedTarget(String target) {
        if (target == null || target.trim().isEmpty()) {
            return false;
        }
        String normalizedTarget = target.trim().toLowerCase(Locale.ROOT);
        String base = baseUrl == null ? "" : baseUrl.trim().toLowerCase(Locale.ROOT);
        if (!base.isEmpty() && normalizedTarget.startsWith(base)) {
            return true;
        }
        try {
            Uri uri = Uri.parse(normalizedTarget);
            String host = uri == null ? null : uri.getHost();
            if (host == null) {
                return false;
            }
        return host.contains("fire.tvbep.com")
                    || host.contains("iptv.bepllorens.com");
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isKnownBackendTrafficPath(String target) {
        if (target == null || target.trim().isEmpty()) {
            return false;
        }
        String normalizedTarget = target.trim().toLowerCase(Locale.ROOT);
        return normalizedTarget.contains("/proxy/")
                || normalizedTarget.contains("/recordings/")
                || normalizedTarget.contains("/api/remux/")
                || normalizedTarget.contains("/api/proxy/")
                || normalizedTarget.contains("/live/")
                || normalizedTarget.contains("/hls/")
                || normalizedTarget.contains("/drm/")
                || normalizedTarget.contains("/api/vod/movistar/")
                || normalizedTarget.contains("/api/u7d/movistar/")
                || normalizedTarget.contains("/api/offline/u7d/");
    }

    private boolean isBackendHostedTrafficTarget(String target) {
        if (target == null || target.trim().isEmpty()) {
            return false;
        }
        String normalizedTarget = target.trim().toLowerCase(Locale.ROOT);
        if (normalizedTarget.startsWith("/")) {
            return isKnownBackendTrafficPath(normalizedTarget);
        }
        return isBackendHostedTarget(normalizedTarget) && isKnownBackendTrafficPath(normalizedTarget);
    }

    private String safeHeartbeatText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private JSONObject buildOfflineDeviceStatusExtra() {
        JSONObject extra = new JSONObject();
        try {
            extra.put("standalone_mode", BuildConfig.STANDALONE_MODE)
                    .put("device_label", buildOfflineActivationDeviceLabel())
                    .put("device_name", buildReadableDeviceName())
                    .put("device_form_factor", detectOfflineDeviceFormFactor())
                    .put("device_manufacturer", safeDeviceBuildValue(Build.MANUFACTURER))
                    .put("device_model", safeDeviceBuildValue(Build.MODEL))
                    .put("device_product", safeDeviceBuildValue(Build.PRODUCT))
                    .put("update_channel", currentUpdateChannel())
                    .put("last_app_update_check_ms", lastAppUpdateCheckMs)
                    .put("last_app_update_error", lastAppUpdateError == null ? "" : lastAppUpdateError)
                    .put("last_catalog_attempt_ms", lastOfflineCatalogRefreshAttemptMs)
                    .put("last_catalog_success_ms", lastOfflineCatalogRefreshSuccessMs)
                    .put("last_catalog_error", lastOfflineCatalogRefreshError == null ? "" : lastOfflineCatalogRefreshError)
                    .put("last_maintenance_ms", lastOfflineMaintenanceMs)
                    .put("last_maintenance_error", lastOfflineMaintenanceError == null ? "" : lastOfflineMaintenanceError)
                    .put("selected_filter", overlayNavigationState.selectedFilterKey == null ? "" : overlayNavigationState.selectedFilterKey)
                    .put("selected_filter_label", buildCurrentFilterLabel())
                    .put("channels_visible", channels.size())
                    .put("channels_total", allChannels.size())
                    .put("epg_progress_state", epgProgressState)
                    .put("epg_progress_label", epgProgressLabel)
                    .put("epg_progress_loaded_channels", epgProgressLoadedChannels)
                    .put("epg_progress_total_channels", epgProgressTotalChannels)
                    .put("epg_progress_completed_filters", epgProgressCompletedFilters)
                    .put("epg_progress_last_error", epgProgressLastError == null ? "" : epgProgressLastError)
                    .put("vod_loading_active", isVodLoadingActive())
                    .put("vod_loading_kind", vodLoadingKind == null ? "" : vodLoadingKind)
                    .put("vod_loading_title", vodLoadingTitle == null ? "" : vodLoadingTitle)
                    .put("vod_loading_step", vodLoadingStep == null ? "" : vodLoadingStep)
                    .put("vod_loading_elapsed_ms", currentVodLoadingElapsedMs())
                    .put("update_health_state", prefs == null ? "" : prefs.getString(PREF_UPDATE_HEALTH_STATE, ""))
                    .put("pending_update_version_code", prefs == null ? 0 : prefs.getInt(PREF_PENDING_UPDATE_HEALTH_VERSION_CODE, 0))
                    .put("last_good_version_code", prefs == null ? 0 : prefs.getInt(PREF_LAST_GOOD_APP_VERSION_CODE, 0))
                    .put("last_good_version_name", prefs == null ? "" : prefs.getString(PREF_LAST_GOOD_APP_VERSION_NAME, ""))
                    .put("last_update_health_at_ms", prefs == null ? 0L : prefs.getLong(PREF_LAST_UPDATE_HEALTH_AT_MS, 0L))
                    .put("last_update_health_error", prefs == null ? "" : prefs.getString(PREF_LAST_UPDATE_HEALTH_ERROR, ""));
            if (catalogSnapshotStore != null) {
                CatalogSnapshotStore.SnapshotStatus status = catalogSnapshotStore.getStatus(BuildConfig.CATALOG_SNAPSHOT_URL);
                extra.put("catalog_last_rejected_at_ms", status.lastRejectedAtMs)
                        .put("last_startup_cache_hit_ms", status.lastStartupCacheHitMs)
                        .put("catalog_cache_age_ms", status.updatedAtMs <= 0L ? 0L : Math.max(0L, System.currentTimeMillis() - status.updatedAtMs))
                        .put("catalog_expired", status.expired)
                        .put("catalog_verification_state", status.verificationState)
                        .put("catalog_last_rejected_reason", status.lastRejectedReason)
                        .put("catalog_last_rejected_previous_channels", status.lastRejectedPreviousChannels)
                        .put("catalog_last_rejected_candidate_channels", status.lastRejectedCandidateChannels)
                        .put("catalog_last_rejected_previous_total", status.lastRejectedPreviousTotal)
                        .put("catalog_last_rejected_candidate_total", status.lastRejectedCandidateTotal);
            }
            appendPlaybackStatusSummary(extra);
            appendDeviceHealthSummary(extra);
            if (prefs != null) {
                String updateDiagnostic = prefs.getString(PREF_APP_UPDATE_DIAGNOSTIC, "");
                if (updateDiagnostic != null && !updateDiagnostic.trim().isEmpty()) {
                    extra.put("app_update_diagnostic", new JSONObject(updateDiagnostic));
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "offline status extra build failed", e);
        }
        return extra;
    }

    private void appendPlaybackStatusSummary(JSONObject extra) {
        if (extra == null) {
            return;
        }
        try {
            ChannelItem current = getCurrentPlaybackChannelItem();
            PlayerController.PlaybackDiagnostics diagnostics = playerController == null ? null : playerController.getPlaybackDiagnostics();
            boolean serverTraffic = current != null && isServerTrafficHeartbeat(current, diagnostics);
            double estimatedMbps = estimatePlaybackMbps(diagnostics);
            PlaybackHealthClassifier.Result playbackHealth = PlaybackHealthClassifier.classify(diagnostics, playbackHeartbeatStartedAtMs <= 0L ? 0L : Math.max(0L, System.currentTimeMillis() - playbackHeartbeatStartedAtMs));
            extra.put("playback_active", current != null)
                    .put("playback_channel_id", current == null || current.id == null ? "" : current.id)
                    .put("playback_channel", current == null ? "" : displayName(current))
                    .put("playback_platform", current == null || current.platformName == null ? "" : current.platformName)
                    .put("playback_group", current == null || current.group == null ? "" : current.group)
                    .put("playback_content_type", current == null ? "" : current.isVod ? "vod" : "live")
                    .put("playback_route_class", current == null ? "" : classifyPlaybackRoute(current, diagnostics))
                    .put("playback_traffic_scope", current == null ? "" : playbackTrafficScope(current, diagnostics, serverTraffic))
                    .put("playback_server_traffic", serverTraffic)
                    .put("playback_quality_label", formatPlaybackQualityCompact(diagnostics))
                    .put("playback_estimated_mbps", estimatedMbps)
                    .put("playback_estimated_mb_per_hour", estimatePlaybackMegabytesPerHour(estimatedMbps))
                    .put("playback_health_level", playbackHealth.level)
                    .put("playback_health_summary", playbackHealth.summary)
                    .put("playback_rebuffer_ratio", playbackHealth.rebufferRatio);
            if (diagnostics != null) {
                extra.put("playback_state", diagnostics.playbackState == null ? "" : diagnostics.playbackState)
                        .put("playback_phase", diagnostics.playbackPhase == null ? "" : diagnostics.playbackPhase)
                        .put("playback_route", diagnostics.routeLabel == null ? "" : diagnostics.routeLabel)
                        .put("playback_mode", diagnostics.playbackMode == null ? "" : diagnostics.playbackMode)
                        .put("playback_prepare_elapsed_ms", diagnostics.prepareElapsedMs)
                        .put("playback_ready_elapsed_ms", diagnostics.readyElapsedMs)
                        .put("playback_buffering_count", diagnostics.bufferingCount)
                        .put("playback_buffering_total_ms", diagnostics.bufferingTotalMs)
                        .put("playback_first_frame_rendered", diagnostics.firstFrameRendered);
            }
        } catch (Exception e) {
            Log.d(TAG, "playback status summary failed", e);
        }
    }

    private void appendDeviceHealthSummary(JSONObject extra) {
        if (extra == null) {
            return;
        }
        try {
            PlayerController.PlaybackDiagnostics diagnostics = playerController == null ? null : playerController.getPlaybackDiagnostics();
            String playbackError = diagnostics == null || diagnostics.lastError == null ? "" : diagnostics.lastError.trim();
            String catalogError = lastOfflineCatalogRefreshError == null ? "" : lastOfflineCatalogRefreshError.trim();
            String maintenanceError = lastOfflineMaintenanceError == null ? "" : lastOfflineMaintenanceError.trim();
            String updateError = lastAppUpdateError == null ? "" : lastAppUpdateError.trim();
            String epgError = epgProgressLastError == null ? "" : epgProgressLastError.trim();
            String level = "ok";
            String summary = "Funcionando";
            if (!playbackError.isEmpty()) {
                level = "error";
                summary = "Error de reproduccion: " + classifyOperationalError(playbackError);
            } else if (!catalogError.isEmpty() && channels.isEmpty()) {
                level = "error";
                summary = "Catalogo sin canales: " + classifyOperationalError(catalogError);
            } else if (isVodLoadingActive()) {
                level = currentVodLoadingElapsedMs() > 30_000L ? "warning" : "loading";
                summary = vodLoadingStep == null || vodLoadingStep.trim().isEmpty()
                        ? "Preparando VOD/U7D"
                        : vodLoadingStep.trim();
            } else if ("loading".equalsIgnoreCase(epgProgressState) || "partial".equalsIgnoreCase(epgProgressState)) {
                level = "loading";
                summary = epgProgressLabel == null || epgProgressLabel.trim().isEmpty()
                        ? "EPG cargando"
                        : epgProgressLabel.trim();
            } else if (!catalogError.isEmpty() || !maintenanceError.isEmpty() || !updateError.isEmpty() || !epgError.isEmpty()) {
                level = "warning";
                summary = !catalogError.isEmpty()
                        ? "Catalogo: " + classifyOperationalError(catalogError)
                        : !maintenanceError.isEmpty()
                        ? "Mantenimiento: " + classifyOperationalError(maintenanceError)
                        : !updateError.isEmpty()
                        ? "Update: " + classifyOperationalError(updateError)
                        : "EPG: " + classifyOperationalError(epgError);
            } else if (channels.isEmpty()) {
                level = "warning";
                summary = "Sin canales visibles";
            }
            extra.put("device_health_level", level)
                    .put("device_health_summary", summary);
        } catch (Exception e) {
            Log.d(TAG, "device health summary failed", e);
        }
    }

    private String buildOfflineActivationDeviceLabel() {
        String deviceName = buildReadableDeviceName();
        String formFactor = detectOfflineDeviceFormFactor();
        if (deviceName.isEmpty()) {
            if ("mobile".equals(formFactor)) {
                return "Android movil offline";
            }
            if ("tablet".equals(formFactor)) {
                return "Android tablet offline";
            }
            return "Android TV offline";
        }
        if ("tv".equals(formFactor)) {
            return deviceName + " offline";
        }
        return deviceName + " (" + formFactor + ") offline";
    }

    private String buildReadableDeviceName() {
        String manufacturer = safeDeviceBuildValue(Build.MANUFACTURER);
        String model = safeDeviceBuildValue(Build.MODEL);
        if (manufacturer.isEmpty()) {
            return model;
        }
        if (model.isEmpty()) {
            return manufacturer;
        }
        String manufacturerLower = manufacturer.toLowerCase(Locale.ROOT);
        String modelLower = model.toLowerCase(Locale.ROOT);
        if (modelLower.startsWith(manufacturerLower)) {
            return model;
        }
        return manufacturer + " " + model;
    }

    private String detectOfflineDeviceFormFactor() {
        PackageManager packageManager = getPackageManager();
        if (packageManager != null && packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            return "tv";
        }
        boolean largeScreen = (getResources().getConfiguration().screenLayout
                & android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK)
                >= android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE;
        if (largeScreen) {
            return "tablet";
        }
        return "mobile";
    }

    private String safeDeviceBuildValue(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "unknown".equalsIgnoreCase(trimmed)) {
            return "";
        }
        return trimmed;
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

    private void maybeShowOfflineActivationReadySummary(CatalogSnapshotStore.SnapshotStatus status) {
        if (!showOfflineActivationSummaryAfterRefresh) {
            return;
        }
        showOfflineActivationSummaryAfterRefresh = false;
        if (status == null && catalogSnapshotStore != null) {
            status = catalogSnapshotStore.getStatus(BuildConfig.CATALOG_SNAPSHOT_URL);
        }
        if (status == null) {
            return;
        }
        String updatedAt = status.updatedAtMs <= 0L ? getString(R.string.diagnostics_value_unknown) : formatDateTime(status.updatedAtMs);
        String expiresAt = status.expiresAtMs <= 0L ? getString(R.string.diagnostics_value_unknown) : formatDateTime(status.expiresAtMs);
        String message = getString(
                R.string.offline_activation_ready_message,
                status.subject == null || status.subject.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : status.subject,
                status.permissions == null || status.permissions.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : status.permissions,
                status.channelCount,
                status.vodCount,
                status.epgChannelCount,
                status.epgProgramCount,
                updatedAt,
                expiresAt
        );
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        actions.add(new TvMessageActionUiModel(getString(R.string.settings_offline_system_status), false, () -> showSettingsInfoDialog(R.string.settings_section_offline_system, buildOfflineSystemSummary())));
        actions.add(new TvMessageActionUiModel(getString(R.string.dialog_close), false, null));
        showTvMessagePanel(getString(R.string.offline_activation_ready_title), message, actions, null);
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
                postUiDelayedIfAlive(offlineCatalogAutoRefreshRunnable, startupMaintenanceGraceRemainingMs());
            }
            return;
        }
        refreshOfflineCatalog(false, false);
    }

    private void maybeShowStartupCatalogCacheValidated() {
        if (!BuildConfig.STANDALONE_MODE || catalogSnapshotStore == null) {
            return;
        }
        CatalogSnapshotStore.SnapshotStatus status = catalogSnapshotStore.getStatus(BuildConfig.CATALOG_SNAPSHOT_URL);
        long hitMs = status == null ? 0L : status.lastStartupCacheHitMs;
        if (hitMs > 0L && System.currentTimeMillis() - hitMs < 60_000L) {
            showStatus(getString(R.string.offline_catalog_status_cache_validated));
        }
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
        if (!BuildConfig.STANDALONE_MODE || !isOfflineRecoveryError(error)) {
            return false;
        }
        if (maybeShowOfflineFirstRunOnboarding(error)) {
            return true;
        }
        String message = error == null || error.getMessage() == null ? getString(R.string.error_unknown_reason) : error.getMessage();
        showOfflineRecoveryActionsDialog(message);
        return true;
    }

    private boolean maybeShowOfflineFirstRunOnboarding(Throwable error) {
        if (!BuildConfig.STANDALONE_MODE || catalogSnapshotStore == null) {
            return false;
        }
        CatalogSnapshotStore.SnapshotStatus status = catalogSnapshotStore.getStatus(BuildConfig.CATALOG_SNAPSHOT_URL);
        boolean missingToken = !status.hasAccessToken;
        boolean missingUrl = status.sourceUrl == null || status.sourceUrl.trim().isEmpty();
        boolean missingCatalog = !status.available && !status.hasLastGoodBackup;
        if (!missingToken && !missingUrl && !missingCatalog) {
            return false;
        }
        if (offlineFirstRunDialogShowing) {
            return true;
        }
        String reason = error == null || error.getMessage() == null || error.getMessage().trim().isEmpty()
                ? buildOfflineFirstRunReason(status)
                : error.getMessage().trim();
        showOfflineFirstRunDialog(status, reason);
        return true;
    }

    private String buildOfflineFirstRunReason(CatalogSnapshotStore.SnapshotStatus status) {
        if (status == null || !status.hasAccessToken) {
            return getString(R.string.offline_first_run_reason_no_token);
        }
        if (status.sourceUrl == null || status.sourceUrl.trim().isEmpty()) {
            return getString(R.string.offline_first_run_reason_no_url);
        }
        if (!status.available) {
            return getString(R.string.offline_first_run_reason_no_catalog);
        }
        return getString(R.string.diagnostics_value_unknown);
    }

    private void showOfflineFirstRunDialog(CatalogSnapshotStore.SnapshotStatus status, String reason) {
        offlineFirstRunDialogShowing = true;
        String message = getString(
                R.string.offline_first_run_message,
                BuildConfig.VERSION_NAME,
                status == null || status.deviceId == null || status.deviceId.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : status.deviceId,
                reason == null || reason.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : reason.trim()
        );
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        actions.add(new TvMessageActionUiModel(getString(R.string.offline_first_run_action_activate), false, () -> {
                    offlineFirstRunDialogShowing = false;
                    startOfflineActivationCodeFlow();
        }));
        actions.add(new TvMessageActionUiModel(getString(R.string.offline_recovery_action_status), false, () -> {
                    offlineFirstRunDialogShowing = false;
                    showSettingsInfoDialog(R.string.settings_section_offline_system, buildOfflineSystemSummary());
        }));
        actions.add(new TvMessageActionUiModel(getString(R.string.dialog_close), false, () -> offlineFirstRunDialogShowing = false));
        showTvMessagePanel(getString(R.string.offline_first_run_title), message, actions, () -> offlineFirstRunDialogShowing = false);
    }

    private boolean isAuthRelatedError(Throwable error) {
        return isAuthRelatedMessage(error == null ? "" : error.getMessage());
    }

    private boolean isOfflineRecoveryError(Throwable error) {
        if (isAuthRelatedError(error)) {
            return true;
        }
        String message = error == null || error.getMessage() == null ? "" : error.getMessage().toLowerCase(Locale.ROOT);
        return message.contains("no hay catalogo")
                || message.contains("no hay url")
                || message.contains("sin catalogo")
                || message.contains("catalogo local caducado")
                || message.contains("snapshot sin firma")
                || message.contains("firma offline no valida")
                || message.contains("catalogo no valido");
    }

    private void showOfflineRecoveryActionsDialog(String reason) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.offline_recovery_action_activate));
        actions.add(this::startOfflineActivationCodeFlow);
        options.add(getString(R.string.offline_recovery_action_refresh));
        actions.add(this::refreshOfflineCatalogFromSettings);
        options.add(getString(R.string.offline_recovery_action_status));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_offline_system, buildOfflineSystemSummary()));
        options.add(getString(R.string.app_update_action_rescue));
        actions.add(this::checkRescueAppUpdateManually);
        options.add(getString(R.string.offline_catalog_action_set_url));
        actions.add(this::showOfflineCatalogUrlDialog);
        options.add(getString(R.string.offline_catalog_action_set_token));
        actions.add(this::showOfflineCatalogTokenDialog);
        showTvOptionsDialog(
                R.string.offline_recovery_title,
                getString(R.string.offline_recovery_message, classifyOperationalError(reason), reason == null ? getString(R.string.error_unknown_reason) : reason),
                options,
                actions
        );
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
        postUiDelayedIfAlive(offlineCatalogRetryRunnable, delayMs);
    }

    private void scheduleOfflineCatalogAutoRefresh() {
        uiHandler.removeCallbacks(offlineCatalogAutoRefreshRunnable);
        if (BuildConfig.STANDALONE_MODE) {
            long delayMs = OFFLINE_CATALOG_AUTO_REFRESH_MS;
            delayMs = Math.max(delayMs, startupMaintenanceGraceRemainingMs());
            postUiDelayedIfAlive(offlineCatalogAutoRefreshRunnable, delayMs);
        }
    }

    private void showOfflineCatalogUrlDialog() {
        showTvTextInputPanel(new TvTextInputPanelUiModel(
                getString(R.string.offline_catalog_action_set_url),
                "",
                getString(android.R.string.ok),
                getString(R.string.dialog_cancel),
                "",
                java.util.Collections.singletonList(new TvTextInputFieldUiModel(getString(R.string.offline_catalog_url_hint), catalogSnapshotStore == null ? BuildConfig.CATALOG_SNAPSHOT_URL : catalogSnapshotStore.getSourceUrl(BuildConfig.CATALOG_SNAPSHOT_URL), false, false)),
                values -> {
                    if (catalogSnapshotStore != null) {
                        catalogSnapshotStore.setSourceUrl(values == null || values.isEmpty() ? "" : values.get(0));
                    }
                    showStatus(getString(R.string.offline_catalog_status_url_saved));
                },
                null,
                null
        ));
    }

    private void showOfflineCatalogTokenDialog() {
        String current = catalogSnapshotStore == null ? "" : catalogSnapshotStore.getAccessToken();
        showTvTextInputPanel(new TvTextInputPanelUiModel(
                getString(R.string.offline_catalog_action_set_token),
                catalogSnapshotStore == null ? "" : getString(R.string.offline_catalog_device_message, catalogSnapshotStore.getDeviceId()),
                getString(android.R.string.ok),
                getString(R.string.dialog_cancel),
                getString(R.string.offline_catalog_action_clear_token),
                java.util.Collections.singletonList(new TvTextInputFieldUiModel(getString(R.string.offline_catalog_token_hint), current == null || current.trim().isEmpty() ? "" : current, true, false)),
                values -> {
                    if (catalogSnapshotStore != null) {
                        catalogSnapshotStore.setAccessToken(values == null || values.isEmpty() ? "" : values.get(0));
                    }
                    showStatus(getString(R.string.offline_catalog_status_token_saved));
                },
                null,
                () -> {
                    if (catalogSnapshotStore != null) {
                        catalogSnapshotStore.setAccessToken("");
                    }
                    showStatus(getString(R.string.offline_catalog_status_token_cleared));
                }
        ));
    }

    private void clearOfflineCatalog() {
        if (catalogSnapshotStore != null) {
            catalogSnapshotStore.clear();
        }
        showStatus(getString(R.string.offline_catalog_status_cleared));
    }

    private void performOfflineRemoteWipe() {
        stopPlaybackHeartbeat("revoked");
        if (playerController != null) {
            playerController.release();
            setupPlayer();
        }
        if (catalogSnapshotStore != null) {
            catalogSnapshotStore.wipeLocalData();
        }
        lastChannelId = "";
        overlayNavigationState.reset();
        epgFullCatalogLoaded = false;
        epgFullCatalogLoadRequested = false;
        epgFullLoadScheduledForChannelId = "";
        channelOverlayCoordinator.applyLoadedChannels(new CatalogLoadResult(new ArrayList<>(), new ArrayList<>(), "all"), "");
        syncOverlayStateFromCoordinator();
        persistNavigationState();
        refreshOverlayChannelList();
        updateFilterText();
        updateOverlaySearchState();
        clearRuntimeCaches();
        hideOverlay();
        showError(getString(R.string.offline_catalog_status_wiped));
    }

    private void showSettingsDiagnosticsDialog() {
        showSettingsDiagnosticsDialog(null);
    }

    private void showSettingsDiagnosticsDialog(Runnable onBack) {
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        actions.add(new TvMessageActionUiModel(getString(onBack == null ? R.string.dialog_close : R.string.dialog_back), false, () -> {
            if (onBack != null) {
                postUiIfAlive(onBack);
            }
        }));
        actions.add(new TvMessageActionUiModel(getString(R.string.settings_performance_clear_caches), true, this::clearRuntimeCaches));
        showTvMessagePanel(getString(R.string.settings_section_diagnostics), buildSettingsDiagnosticsMessage(), actions, onBack);
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
                overlayNavigationState.selectedFilterKey == null ? "all" : overlayNavigationState.selectedFilterKey,
                overlayNavigationState.favoritesOnly ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
                prefs != null && prefs.getBoolean(PREF_STARTUP_HUB_DISABLED, false) ? getString(R.string.diagnostics_value_no) : getString(R.string.diagnostics_value_yes),
                recordingsPanelController.isAutoRefreshEnabled() ? getString(R.string.diagnostics_value_yes) : getString(R.string.diagnostics_value_no),
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
        showResetSettingsDialog(null);
    }

    private void showResetSettingsDialog(Runnable onBack) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.settings_reset_search));
        actions.add(() -> confirmSettingsAction(R.string.settings_reset_search, R.string.settings_confirm_reset_search, this::clearGlobalSearchRecents, () -> showResetSettingsDialog(onBack)));
        options.add(getString(R.string.settings_reset_playback));
        actions.add(() -> confirmSettingsAction(R.string.settings_reset_playback, R.string.settings_confirm_reset_playback, this::resetPlaybackSettings, () -> showResetSettingsDialog(onBack)));
        options.add(getString(R.string.settings_reset_startup));
        actions.add(() -> confirmSettingsAction(R.string.settings_reset_startup, R.string.settings_confirm_reset_startup, this::resetStartupSettings, () -> showResetSettingsDialog(onBack)));
        options.add(getString(R.string.settings_reset_local_data));
        actions.add(() -> confirmSettingsAction(R.string.settings_reset_local_data, R.string.settings_confirm_reset_local_data, this::resetLocalData, () -> showResetSettingsDialog(onBack)));
        options.add(getString(R.string.settings_action_view_summary));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_reset, getString(R.string.settings_reset_summary), () -> showResetSettingsDialog(onBack)));
        showTvOptionsDialog(R.string.settings_section_reset, null, options, actions, onBack);
    }

    private void checkAppUpdateOnStartup() {
        checkAppUpdate(false);
    }

    private void scheduleAppUpdateCheckOnStartup() {
        if (BuildConfig.STANDALONE_MODE) {
            postUiDelayedIfAlive(this::checkAppUpdateOnStartup, OFFLINE_APP_UPDATE_STARTUP_DELAY_MS);
        } else {
            checkAppUpdateOnStartup();
        }
    }

    private void maybeCheckAppUpdateOnResume() {
        if (!BuildConfig.STANDALONE_MODE || appUpdateManager == null || appUpdateCheckRunning) {
            return;
        }
        long now = System.currentTimeMillis();
        if (lastResumeAppUpdateCheckMs > 0L && now - lastResumeAppUpdateCheckMs < OFFLINE_APP_UPDATE_RESUME_CHECK_MS) {
            return;
        }
        if (lastAppUpdateCheckMs > 0L && now - lastAppUpdateCheckMs < OFFLINE_APP_UPDATE_RESUME_CHECK_MS) {
            return;
        }
        lastResumeAppUpdateCheckMs = now;
        postUiDelayedIfAlive(() -> checkAppUpdate(false), 5_000L);
    }

    private void maybeRefreshOfflineCatalogOnResume() {
        if (!BuildConfig.STANDALONE_MODE || catalogSnapshotStore == null || offlineCatalogRefreshRunning) {
            return;
        }
        long now = System.currentTimeMillis();
        if (lastResumeOfflineCatalogCheckMs > 0L && now - lastResumeOfflineCatalogCheckMs < 5L * 60L * 1000L) {
            return;
        }
        CatalogSnapshotStore.SnapshotStatus status = catalogSnapshotStore.getStatus(BuildConfig.CATALOG_SNAPSHOT_URL);
        if (!shouldRefreshOfflineCatalog(status)) {
            return;
        }
        lastResumeOfflineCatalogCheckMs = now;
        postUiDelayedIfAlive(() -> refreshOfflineCatalog(false, false, true), 8_000L);
    }

    private String currentUpdateChannel() {
        String value = prefs == null ? BuildConfig.UPDATE_CHANNEL : prefs.getString(PREF_UPDATE_CHANNEL, BuildConfig.UPDATE_CHANNEL);
        return normalizeUpdateChannel(value);
    }

    private String normalizeUpdateChannel(String value) {
        String clean = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if ("beta".equals(clean)) {
            return "beta";
        }
        if ("rescue".equals(clean)) {
            return "rescue";
        }
        return "stable";
    }

    private int updateChannelLabelRes(String channel) {
        String clean = normalizeUpdateChannel(channel);
        if ("beta".equals(clean)) {
            return R.string.app_update_channel_beta;
        }
        if ("rescue".equals(clean)) {
            return R.string.app_update_channel_rescue;
        }
        return R.string.app_update_channel_stable;
    }

    private String currentUpdateChannelLabel() {
        return getString(updateChannelLabelRes(currentUpdateChannel()));
    }

    private void adoptEffectiveUpdateChannel(AppUpdateManager.UpdateInfo info) {
        if (info == null || prefs == null) {
            return;
        }
        String effective = normalizeUpdateChannel(info.channel);
        if (!effective.equals(currentUpdateChannel())) {
            prefs.edit().putString(PREF_UPDATE_CHANNEL, effective).apply();
        }
    }

    private void showUpdateChannelDialog() {
        showUpdateChannelDialog(null);
    }

    private void showUpdateChannelDialog(Runnable onBack) {
        String[] channels = new String[]{"stable", "beta", "rescue"};
        String[] labels = new String[]{
                getString(R.string.app_update_channel_stable),
                getString(R.string.app_update_channel_beta),
                getString(R.string.app_update_channel_rescue)
        };
        String current = currentUpdateChannel();
        int checked = 0;
        for (int i = 0; i < channels.length; i++) {
            if (channels[i].equals(current)) {
                checked = i;
                break;
            }
        }
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            final int which = i;
            options.add(i == checked ? getString(R.string.settings_selected_prefix, labels[i]) : labels[i]);
            actions.add(() -> {
                    String selectedChannel = channels[Math.max(0, Math.min(which, channels.length - 1))];
                    String selectedLabel = getString(updateChannelLabelRes(selectedChannel));
                    if (prefs != null) {
                        prefs.edit()
                                .putString(PREF_UPDATE_CHANNEL, selectedChannel)
                                .putInt(PREF_LAST_UPDATE_PROMPT_VERSION_CODE, 0)
                                .apply();
                    }
                    String detail = getString(R.string.app_update_channel_saved, selectedLabel);
                    showStatus(detail);
                    recordOfflineSyncEvent(getString(R.string.settings_offline_sync_app_update), true, 0L, detail);
                    reportOfflineDeviceStatus(getString(R.string.settings_offline_sync_app_update), true, 0L, detail);
                    checkAppUpdate(true);
            });
        }
        showTvOptionsDialog(R.string.app_update_channel_title, getString(R.string.app_update_channel_message, currentUpdateChannelLabel()), options, actions, onBack);
    }

    private void checkAppUpdateManually() {
        checkAppUpdate(true);
    }

    private void checkRescueAppUpdateManually() {
        checkRescueAppUpdate(true);
    }

    private void showAppUpdateDiagnosticsDialog() {
        List<PlaybackDiagnosticsRowUiModel> rows = new ArrayList<>();
        String checked = lastAppUpdateCheckMs <= 0L
                ? getString(R.string.diagnostics_value_unknown)
                : formatDateTime(lastAppUpdateCheckMs);
        rows.add(new PlaybackDiagnosticsRowUiModel("Estado", "Canal", currentUpdateChannelLabel(), ""));
        rows.add(new PlaybackDiagnosticsRowUiModel("Estado", "Ultima comprobacion", checked, ""));
        rows.add(new PlaybackDiagnosticsRowUiModel("Estado", "Version instalada", BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")", "ok"));
        if (lastAppUpdateError != null && !lastAppUpdateError.trim().isEmpty()) {
            rows.add(new PlaybackDiagnosticsRowUiModel("Estado", "Error", classifyOperationalError(lastAppUpdateError) + ": " + lastAppUpdateError, "error"));
        } else if (lastKnownAppUpdateInfo != null && lastKnownAppUpdateInfo.isNewerThanCurrent()) {
            rows.add(new PlaybackDiagnosticsRowUiModel("Estado", "Disponible", safeUpdateVersionName(lastKnownAppUpdateInfo) + " (" + lastKnownAppUpdateInfo.versionCode + ")", "warn"));
        } else if (lastKnownAppUpdateInfo != null) {
            rows.add(new PlaybackDiagnosticsRowUiModel("Estado", "Resultado", "Al dia", "ok"));
        }
        JSONObject diagnostic = readLastAppUpdateDiagnostic();
        appendAppUpdateDiagnosticRows(rows, diagnostic);
        List<String> notes = new ArrayList<>();
        String summary = buildAppUpdateDiagnosticSummary();
        if (summary != null && !summary.trim().isEmpty()) {
            notes.add(summary);
        }
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        actions.add(new TvMessageActionUiModel(getString(R.string.app_update_action_check), false, this::checkAppUpdateManually));
        actions.add(new TvMessageActionUiModel(getString(R.string.dialog_close), false, null));
        showStructuredStatusPanel(
                getString(R.string.app_update_action_diagnostics),
                getString(R.string.app_update_channel_current, currentUpdateChannelLabel()),
                buildAppUpdateStateSummary(),
                rows,
                notes,
                actions
        );
    }

    private void checkAppUpdate(boolean manual) {
        if (appUpdateManager == null || appUpdateCheckRunning) {
            return;
        }
        String updateChannel = currentUpdateChannel();
        appUpdateCheckRunning = true;
        if (manual) {
            showStatus(getString(R.string.app_update_status_checking));
        }
        long startMs = System.currentTimeMillis();
        ioExecutor.execute(() -> {
            try {
                AppUpdateManager.UpdateInfo info = appUpdateManager.fetchLatest(BuildConfig.OFFLINE_BASE_URL, updateChannel);
                long durationMs = System.currentTimeMillis() - startMs;
                postUiIfAlive(() -> {
                    appUpdateCheckRunning = false;
                    adoptEffectiveUpdateChannel(info);
                    lastKnownAppUpdateInfo = info;
                    lastAppUpdateCheckMs = System.currentTimeMillis();
                    lastAppUpdateError = "";
                    recordAppUpdateDiagnostic("check", true, info, null, durationMs, info.isNewerThanCurrent()
                            ? getString(R.string.settings_update_state_available_short, safeUpdateVersionName(info), info.versionCode)
                            : getString(R.string.app_update_none, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
                    recordOfflineSyncEvent(
                            getString(R.string.settings_offline_sync_app_update),
                            true,
                            durationMs,
                            info.isNewerThanCurrent()
                                    ? getString(R.string.settings_update_state_available_short, safeUpdateVersionName(info), info.versionCode)
                                    : getString(R.string.app_update_none, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
                    );
                    reportOfflineDeviceStatus(
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
                postUiIfAlive(() -> {
                    appUpdateCheckRunning = false;
                    lastAppUpdateCheckMs = System.currentTimeMillis();
                    lastAppUpdateError = e.getMessage();
                    lastOfflineMaintenanceError = e.getMessage();
                    recordAppUpdateDiagnostic("check", false, null, null, durationMs, e.getMessage());
                    recordOfflineSyncEvent(getString(R.string.settings_offline_sync_app_update), false, durationMs, e.getMessage());
                    reportOfflineDeviceStatus(getString(R.string.settings_offline_sync_app_update), false, durationMs, e.getMessage());
                    if (manual) {
                        showError(getString(R.string.app_update_error, e.getMessage()));
                    }
                });
            }
        });
    }

    private void checkRescueAppUpdate(boolean manual) {
        if (appUpdateManager == null || appUpdateCheckRunning) {
            return;
        }
        appUpdateCheckRunning = true;
        showStatus(getString(R.string.app_update_rescue_checking));
        long startMs = System.currentTimeMillis();
        ioExecutor.execute(() -> {
            try {
                AppUpdateManager.UpdateInfo info = appUpdateManager.fetchLatest(BuildConfig.OFFLINE_BASE_URL, "rescue");
                long durationMs = System.currentTimeMillis() - startMs;
                postUiIfAlive(() -> {
                    appUpdateCheckRunning = false;
                    lastKnownAppUpdateInfo = info;
                    lastAppUpdateCheckMs = System.currentTimeMillis();
                    lastAppUpdateError = "";
                    recordAppUpdateDiagnostic("rescue-check", true, info, null, durationMs, info.isNewerThanCurrent()
                            ? getString(R.string.settings_update_state_available_short, safeUpdateVersionName(info), info.versionCode)
                            : getString(R.string.app_update_rescue_none, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
                    if (info.isNewerThanCurrent()) {
                        showAppUpdateAvailableDialog(info);
                    } else {
                        showSettingsInfoDialog(R.string.app_update_action_rescue, getString(R.string.app_update_rescue_none, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "rescue app update check failed", e);
                long durationMs = System.currentTimeMillis() - startMs;
                postUiIfAlive(() -> {
                    appUpdateCheckRunning = false;
                    lastAppUpdateCheckMs = System.currentTimeMillis();
                    lastAppUpdateError = e.getMessage();
                    recordAppUpdateDiagnostic("rescue-check", false, null, null, durationMs, e.getMessage());
                    showError(getString(R.string.app_update_error, e.getMessage()));
                });
            }
        });
    }

    private void showAppUpdateAvailableDialog(AppUpdateManager.UpdateInfo info) {
        PlaybackDiagnosticsPanelUiModel model = AppUpdateUiFactory.buildAvailable(info, new AppUpdateUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String text(int resId, Object... args) {
                return getString(resId, args);
            }

            @Override
            public String currentVersion() {
                return BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")";
            }

            @Override
            public String currentChannelLabel() {
                return currentUpdateChannelLabel();
            }

            @Override
            public String safeVersionName(AppUpdateManager.UpdateInfo updateInfo) {
                return safeUpdateVersionName(updateInfo);
            }

            @Override
            public void install(AppUpdateManager.UpdateInfo updateInfo) {
                downloadAndInstallAppUpdate(updateInfo);
            }
        });
        showStructuredStatusPanel(model.title, model.subtitle, model.summary, model.rows, model.notes, model.actions);
    }

    private void downloadAndInstallAppUpdate(AppUpdateManager.UpdateInfo info) {
        showStatus(getString(R.string.app_update_status_downloading));
        long startMs = System.currentTimeMillis();
        recordAppUpdateDiagnostic("download", false, info, null, 0L, getString(R.string.app_update_status_downloading));
        ioExecutor.execute(() -> {
            try {
                File apk = appUpdateManager.downloadApk(info, (done, total) -> {
                    if (total > 0L) {
                        int pct = (int) Math.max(0L, Math.min(100L, (done * 100L) / total));
                        postUiIfAlive(() -> showStatus(getString(R.string.app_update_status_downloading_pct, pct)));
                    }
                });
                AppUpdateManager.InstallPreflight preflight = appUpdateManager.checkInstallPreflight(apk);
                long durationMs = System.currentTimeMillis() - startMs;
                recordAppUpdateDiagnostic("preflight", preflight.ok, info, preflight, durationMs, preflight.message);
                if (!preflight.ok) {
                    throw new IllegalStateException(preflight.message);
                }
                postUiIfAlive(() -> {
                    showStatus(getString(R.string.app_update_status_installing));
                    try {
                        recordAppUpdateDiagnostic("installer", true, info, preflight, durationMs, getString(R.string.app_update_status_installing));
                        appUpdateManager.installApk(apk);
                    } catch (Exception e) {
                        Log.e(TAG, "app update install failed", e);
                        recordAppUpdateDiagnostic("installer", false, info, preflight, durationMs, e.getMessage());
                        showError(getString(R.string.app_update_error, e.getMessage()));
                        if (isOfflineRecoveryError(e)) {
                            showOfflineRecoveryActionsDialog(e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "app update download failed", e);
                long durationMs = System.currentTimeMillis() - startMs;
                recordAppUpdateDiagnostic("download", false, info, null, durationMs, e.getMessage());
                postUiIfAlive(() -> {
                    showError(getString(R.string.app_update_error, e.getMessage()));
                    if (isOfflineRecoveryError(e)) {
                        showOfflineRecoveryActionsDialog(e.getMessage());
                    }
                });
            }
        });
    }

    private void recordAppUpdateDiagnostic(String stage, boolean success, AppUpdateManager.UpdateInfo info, AppUpdateManager.InstallPreflight preflight, long durationMs, String detail) {
        if (prefs == null) {
            return;
        }
        try {
            JSONObject payload = new JSONObject()
                    .put("ts", System.currentTimeMillis())
                    .put("stage", stage == null ? "" : stage)
                    .put("success", success)
                    .put("duration_ms", Math.max(0L, durationMs))
                    .put("detail", detail == null ? "" : detail)
                    .put("current_version_name", BuildConfig.VERSION_NAME)
                    .put("current_version_code", BuildConfig.VERSION_CODE)
                    .put("update_channel", currentUpdateChannel())
                    .put("package_name", getPackageName());
            if (info != null) {
                payload.put("target_version_name", safeUpdateVersionName(info))
                        .put("target_version_code", info.versionCode)
                        .put("target_update_channel", info.channel == null ? "" : info.channel)
                        .put("required", info.required);
            }
            if (preflight != null) {
                payload.put("preflight_ok", preflight.ok)
                        .put("apk_package", preflight.apkPackageName)
                        .put("apk_version_code", preflight.apkVersionCode)
                        .put("installed_package", preflight.installedPackageName)
                        .put("installed_version_code", preflight.installedVersionCode);
            }
            prefs.edit().putString(PREF_APP_UPDATE_DIAGNOSTIC, payload.toString()).apply();
        } catch (Exception e) {
            Log.d(TAG, "app update diagnostic write failed", e);
        }
    }

    private String buildAppUpdateDiagnosticSummary() {
        try {
            JSONObject payload = readLastAppUpdateDiagnostic();
            if (payload == null) {
                return "";
            }
            StringBuilder out = new StringBuilder();
            appendDiagnosticLine(out, getString(
                    R.string.app_update_diagnostic_summary,
                    formatDateTime(payload.optLong("ts", 0L)),
                    payload.optString("stage", getString(R.string.diagnostics_value_unknown)),
                    payload.optBoolean("success", false) ? getString(R.string.settings_offline_sync_ok) : getString(R.string.settings_offline_sync_failed),
                    payload.optLong("duration_ms", 0L),
                    payload.optString("detail", getString(R.string.diagnostics_value_unknown))
            ));
            int targetVersion = payload.optInt("target_version_code", 0);
            if (targetVersion > 0) {
                appendDiagnosticLine(out, getString(
                        R.string.app_update_diagnostic_versions,
                        payload.optString("current_version_name", BuildConfig.VERSION_NAME),
                        payload.optInt("current_version_code", BuildConfig.VERSION_CODE),
                        payload.optString("target_version_name", String.valueOf(targetVersion)),
                        targetVersion
                ));
            }
            String apkPackage = payload.optString("apk_package", "").trim();
            if (!apkPackage.isEmpty()) {
                appendDiagnosticLine(out, getString(
                        R.string.app_update_diagnostic_apk,
                        apkPackage,
                        payload.optInt("apk_version_code", 0),
                        payload.optString("installed_package", getPackageName()),
                        payload.optInt("installed_version_code", BuildConfig.VERSION_CODE)
                ));
            }
            return out.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private JSONObject readLastAppUpdateDiagnostic() {
        if (prefs == null) {
            return null;
        }
        String raw = prefs.getString(PREF_APP_UPDATE_DIAGNOSTIC, "");
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return new JSONObject(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private void appendAppUpdateDiagnosticRows(List<PlaybackDiagnosticsRowUiModel> rows, JSONObject payload) {
        if (rows == null || payload == null) {
            return;
        }
        boolean success = payload.optBoolean("success", false);
        rows.add(new PlaybackDiagnosticsRowUiModel("Ultimo intento", "Fecha", formatDateTime(payload.optLong("ts", 0L)), ""));
        rows.add(new PlaybackDiagnosticsRowUiModel("Ultimo intento", "Fase", payload.optString("stage", getString(R.string.diagnostics_value_unknown)), ""));
        rows.add(new PlaybackDiagnosticsRowUiModel("Ultimo intento", "Resultado", success ? getString(R.string.settings_offline_sync_ok) : getString(R.string.settings_offline_sync_failed), success ? "ok" : "error"));
        rows.add(new PlaybackDiagnosticsRowUiModel("Ultimo intento", "Duracion", payload.optLong("duration_ms", 0L) + " ms", ""));
        String detail = payload.optString("detail", "").trim();
        if (!detail.isEmpty()) {
            rows.add(new PlaybackDiagnosticsRowUiModel("Ultimo intento", "Detalle", detail, success ? "" : "error"));
        }
        int targetVersion = payload.optInt("target_version_code", 0);
        if (targetVersion > 0) {
            rows.add(new PlaybackDiagnosticsRowUiModel("Versiones", "Actual", payload.optString("current_version_name", BuildConfig.VERSION_NAME) + " (" + payload.optInt("current_version_code", BuildConfig.VERSION_CODE) + ")", ""));
            rows.add(new PlaybackDiagnosticsRowUiModel("Versiones", "Objetivo", payload.optString("target_version_name", String.valueOf(targetVersion)) + " (" + targetVersion + ")", ""));
            rows.add(new PlaybackDiagnosticsRowUiModel("Versiones", "Canal objetivo", payload.optString("target_update_channel", getString(R.string.diagnostics_value_unknown)), ""));
        }
        String apkPackage = payload.optString("apk_package", "").trim();
        if (!apkPackage.isEmpty()) {
            rows.add(new PlaybackDiagnosticsRowUiModel("APK", "Paquete APK", apkPackage + " (" + payload.optInt("apk_version_code", 0) + ")", ""));
            rows.add(new PlaybackDiagnosticsRowUiModel("APK", "Instalado", payload.optString("installed_package", getPackageName()) + " (" + payload.optInt("installed_version_code", BuildConfig.VERSION_CODE) + ")", ""));
            rows.add(new PlaybackDiagnosticsRowUiModel("APK", "Preflight", payload.optBoolean("preflight_ok", false) ? getString(R.string.settings_offline_sync_ok) : getString(R.string.settings_offline_sync_failed), payload.optBoolean("preflight_ok", false) ? "ok" : "error"));
        }
    }

    private void showPostUpdateNotesIfNeeded() {
        if (prefs == null || appUpdateManager == null) {
            return;
        }
        int lastSeen = prefs.getInt(PREF_LAST_SEEN_APP_VERSION_CODE, 0);
        if (lastSeen >= BuildConfig.VERSION_CODE) {
            return;
        }
        markPostUpdateHealthPending(lastSeen);
        prefs.edit().putInt(PREF_LAST_SEEN_APP_VERSION_CODE, BuildConfig.VERSION_CODE).apply();
        ioExecutor.execute(() -> {
            try {
                AppUpdateManager.UpdateInfo info = appUpdateManager.fetchLatest(BuildConfig.OFFLINE_BASE_URL, currentUpdateChannel());
                if (info.versionCode == BuildConfig.VERSION_CODE && !info.changelog.isEmpty()) {
                    postUiIfAlive(() -> showAppUpdatedPanel(info));
                }
            } catch (Exception e) {
                Log.d(TAG, "post-update notes unavailable", e);
            }
        });
    }

    private void showAppUpdatedPanel(AppUpdateManager.UpdateInfo info) {
        List<PlaybackDiagnosticsRowUiModel> rows = new ArrayList<>();
        rows.add(new PlaybackDiagnosticsRowUiModel("Version", "Instalada", BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")", "ok"));
        rows.add(new PlaybackDiagnosticsRowUiModel("Version", "Canal", currentUpdateChannelLabel(), ""));
        if (info != null) {
            rows.add(new PlaybackDiagnosticsRowUiModel("Version", "Publicada", safeUpdateVersionName(info) + " (" + info.versionCode + ")", "ok"));
            if (info.sha256 != null && !info.sha256.trim().isEmpty()) {
                rows.add(new PlaybackDiagnosticsRowUiModel("APK", "SHA-256", info.sha256, ""));
            }
        }
        appendAppUpdateDiagnosticRows(rows, readLastAppUpdateDiagnostic());
        List<String> notes = new ArrayList<>();
        if (info == null || info.changelog == null || info.changelog.isEmpty()) {
            notes.add(getString(R.string.diagnostics_value_unknown));
        } else {
            for (String item : info.changelog) {
                if (item != null && !item.trim().isEmpty()) {
                    notes.add("- " + item.trim());
                }
            }
        }
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        actions.add(new TvMessageActionUiModel(getString(R.string.app_update_action_diagnostics), false, this::showAppUpdateDiagnosticsDialog));
        actions.add(new TvMessageActionUiModel(getString(R.string.dialog_close), false, null));
        showStructuredStatusPanel(
                getString(R.string.app_update_installed_title),
                getString(R.string.app_update_channel_current, currentUpdateChannelLabel()),
                "Actualizado a " + safeUpdateVersionName(info),
                rows,
                notes,
                actions
        );
    }

    private void markPostUpdateHealthPending(int previousVersionCode) {
        if (!BuildConfig.STANDALONE_MODE || prefs == null || previousVersionCode <= 0 || previousVersionCode >= BuildConfig.VERSION_CODE) {
            return;
        }
        prefs.edit()
                .putInt(PREF_PENDING_UPDATE_HEALTH_VERSION_CODE, BuildConfig.VERSION_CODE)
                .putString(PREF_UPDATE_HEALTH_STATE, UPDATE_HEALTH_PENDING)
                .putLong(PREF_LAST_UPDATE_HEALTH_AT_MS, System.currentTimeMillis())
                .putString(PREF_LAST_UPDATE_HEALTH_ERROR, "")
                .apply();
        reportOfflineDeviceStatus(getString(R.string.app_update_health_event), true, 0L, getString(R.string.app_update_health_pending, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
    }

    private boolean isPostUpdateHealthActive() {
        if (!BuildConfig.STANDALONE_MODE || prefs == null) {
            return false;
        }
        int pendingVersion = prefs.getInt(PREF_PENDING_UPDATE_HEALTH_VERSION_CODE, 0);
        if (pendingVersion != BuildConfig.VERSION_CODE) {
            return false;
        }
        String state = prefs.getString(PREF_UPDATE_HEALTH_STATE, "");
        return UPDATE_HEALTH_PENDING.equals(state) || UPDATE_HEALTH_CATALOG_OK.equals(state) || UPDATE_HEALTH_FAILED.equals(state);
    }

    private void runPostUpdateStartupHealthCheck(String stage, CatalogLoadResult result) {
        if (!isPostUpdateHealthActive()) {
            return;
        }
        int channelCount = result == null || result.channels == null ? 0 : result.channels.size();
        CatalogSnapshotStore.SnapshotStatus status = catalogSnapshotStore == null
                ? null
                : catalogSnapshotStore.getStatus(BuildConfig.CATALOG_SNAPSHOT_URL);
        int epgPrograms = status == null ? 0 : status.epgProgramCount;
        if (channelCount <= 0) {
            String detail = getString(R.string.app_update_health_catalog_failed, stage == null ? "catalog" : stage);
            markPostUpdateHealthFailed(detail, true);
            return;
        }
        String detail = getString(R.string.app_update_health_catalog_ok, channelCount, epgPrograms);
        prefs.edit()
                .putString(PREF_UPDATE_HEALTH_STATE, UPDATE_HEALTH_CATALOG_OK)
                .putLong(PREF_LAST_UPDATE_HEALTH_AT_MS, System.currentTimeMillis())
                .putString(PREF_LAST_UPDATE_HEALTH_ERROR, "")
                .apply();
        reportOfflineDeviceStatus(getString(R.string.app_update_health_event), true, lastCatalogLoadDurationMs, detail);
    }

    private void markPostUpdatePlaybackHealthy(String channelId) {
        if (!isPostUpdateHealthActive() || prefs == null) {
            return;
        }
        String detail = getString(R.string.app_update_health_playback_ok, fallbackUnknown(channelId));
        prefs.edit()
                .putInt(PREF_PENDING_UPDATE_HEALTH_VERSION_CODE, 0)
                .putInt(PREF_LAST_GOOD_APP_VERSION_CODE, BuildConfig.VERSION_CODE)
                .putString(PREF_LAST_GOOD_APP_VERSION_NAME, BuildConfig.VERSION_NAME)
                .putString(PREF_UPDATE_HEALTH_STATE, UPDATE_HEALTH_GOOD)
                .putLong(PREF_LAST_UPDATE_HEALTH_AT_MS, System.currentTimeMillis())
                .putString(PREF_LAST_UPDATE_HEALTH_ERROR, "")
                .apply();
        reportOfflineDeviceStatus(getString(R.string.app_update_health_event), true, 0L, detail);
        showStatus(getString(R.string.app_update_health_marked_good));
    }

    private void markPostUpdateHealthFailed(String detail, boolean showActions) {
        if (!isPostUpdateHealthActive() || prefs == null) {
            return;
        }
        String cleanDetail = detail == null || detail.trim().isEmpty() ? getString(R.string.error_unknown_reason) : detail.trim();
        prefs.edit()
                .putString(PREF_UPDATE_HEALTH_STATE, UPDATE_HEALTH_FAILED)
                .putLong(PREF_LAST_UPDATE_HEALTH_AT_MS, System.currentTimeMillis())
                .putString(PREF_LAST_UPDATE_HEALTH_ERROR, cleanDetail)
                .apply();
        lastOfflineMaintenanceError = cleanDetail;
        reportOfflineDeviceStatus(getString(R.string.app_update_health_event), false, 0L, cleanDetail);
        if (showActions) {
            postUiDelayedIfAlive(() -> showPostUpdateRecoveryDialog(cleanDetail), 600L);
        }
    }

    private void showPostUpdateRecoveryDialog(String reason) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.diagnostics_action_retry));
        actions.add(this::retryCurrentPlayback);
        options.add(getString(R.string.app_update_action_rescue));
        actions.add(this::checkRescueAppUpdateManually);
        options.add(getString(R.string.app_update_action_diagnostics));
        actions.add(this::showAppUpdateDiagnosticsDialog);
        options.add(getString(R.string.offline_recovery_action_status));
        actions.add(() -> showSettingsInfoDialog(R.string.settings_section_offline_system, buildOfflineSystemSummary()));
        showTvOptionsDialog(
                R.string.app_update_health_failed_title,
                getString(R.string.app_update_health_failed_message, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, reason == null ? getString(R.string.error_unknown_reason) : reason),
                options,
                actions
        );
    }

    private void detectUnfinishedAppUpdateIfNeeded() {
        if (prefs == null) {
            return;
        }
        String raw = prefs.getString(PREF_APP_UPDATE_DIAGNOSTIC, "");
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        try {
            JSONObject diagnostic = new JSONObject(raw);
            int targetVersion = diagnostic.optInt("target_version_code", 0);
            String stage = diagnostic.optString("stage", "");
            boolean success = diagnostic.optBoolean("success", false);
            if (targetVersion > BuildConfig.VERSION_CODE && ("installer".equals(stage) || "preflight".equals(stage))) {
                String detail = diagnostic.optString("detail", "").trim();
                if (detail.isEmpty()) {
                    detail = success ? getString(R.string.app_update_diagnostic_installer_unfinished) : getString(R.string.app_update_diagnostic_unfinished);
                }
                lastAppUpdateError = detail;
                lastAppUpdateCheckMs = diagnostic.optLong("ts", System.currentTimeMillis());
                if (stage.equals("preflight") && !success) {
                    showStatus(getString(R.string.app_update_error, detail));
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "unfinished app update diagnostic ignored", e);
        }
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
        showSettingsInfoDialog(titleResId, message, null);
    }

    private void showSettingsInfoDialog(int titleResId, String message, Runnable onBack) {
        String title = getString(titleResId);
        String cleanMessage = message == null || message.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : message.trim();
        List<String> notes = new ArrayList<>();
        List<PlaybackDiagnosticsRowUiModel> rows = buildSettingsInfoRows(title, cleanMessage, notes);
        List<TvMessageActionUiModel> actions = java.util.Collections.singletonList(new TvMessageActionUiModel(getString(onBack == null ? R.string.dialog_close : R.string.dialog_back), false, () -> {
                    if (onBack != null) {
                        postUiIfAlive(onBack);
                    }
                }));
        if (!rows.isEmpty()) {
            showStructuredStatusPanel(
                    title,
                    "",
                    notes.isEmpty() ? "" : notes.get(0),
                    rows,
                    notes.size() <= 1 ? Collections.emptyList() : notes.subList(1, notes.size()),
                    actions,
                    onBack
            );
            return;
        }
        showTvMessagePanel(title, cleanMessage, actions, onBack);
    }

    private List<PlaybackDiagnosticsRowUiModel> buildSettingsInfoRows(String section, String message, List<String> notes) {
        List<PlaybackDiagnosticsRowUiModel> rows = new ArrayList<>();
        if (message == null || message.trim().isEmpty()) {
            return rows;
        }
        String[] lines = message.trim().split("\\n+");
        for (String line : lines) {
            appendSettingsInfoLine(rows, notes, section, line);
        }
        if (rows.size() < 2) {
            rows.clear();
        }
        return rows;
    }

    private void appendSettingsInfoLine(List<PlaybackDiagnosticsRowUiModel> rows, List<String> notes, String section, String line) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }
        String[] chunks = line.trim().split("\\s+·\\s+");
        boolean parsedAny = false;
        for (String chunk : chunks) {
            String clean = chunk == null ? "" : chunk.trim();
            int colon = clean.indexOf(':');
            if (colon > 0 && colon < clean.length() - 1) {
                String label = clean.substring(0, colon).trim();
                String value = clean.substring(colon + 1).trim();
                rows.add(new PlaybackDiagnosticsRowUiModel(section, label, value, settingsInfoTone(label, value)));
                parsedAny = true;
            }
        }
        if (!parsedAny && notes != null) {
            notes.add(line.trim());
        }
    }

    private String settingsInfoTone(String label, String value) {
        String normalized = safeLower(label + " " + value);
        if (normalized.contains("error") || normalized.contains("fall") || normalized.contains("failed")) {
            return "error";
        }
        if (normalized.contains("warning") || normalized.contains("pendiente") || normalized.contains("caduca")) {
            return "warn";
        }
        if (normalized.contains(" ok") || normalized.endsWith("ok") || normalized.contains(" si") || normalized.contains(" disponible")) {
            return "ok";
        }
        return "";
    }

    private void confirmSettingsAction(int titleResId, int messageResId, Runnable action) {
        confirmSettingsAction(titleResId, messageResId, action, null);
    }

    private void confirmSettingsAction(int titleResId, int messageResId, Runnable action, Runnable onBack) {
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        actions.add(new TvMessageActionUiModel(getString(android.R.string.ok), true, () -> {
            if (action != null) {
                action.run();
            }
            if (onBack != null) {
                postUiIfAlive(onBack);
            }
        }));
        actions.add(new TvMessageActionUiModel(getString(R.string.dialog_cancel), false, () -> {
            if (onBack != null) {
                postUiIfAlive(onBack);
            }
        }));
        showTvMessagePanel(getString(titleResId), getString(messageResId), actions, onBack);
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

    private String playbackQualityLabel() {
        switch (PlaybackQualityPolicy.normalize(playbackQualityMode)) {
            case PlaybackQualityPolicy.DATA_SAVER:
                return getString(R.string.settings_playback_quality_data_saver);
            case PlaybackQualityPolicy.HIGH:
                return getString(R.string.settings_playback_quality_high);
            default:
                return getString(R.string.settings_playback_quality_auto);
        }
    }

    private void showPlaybackQualityDialog(Runnable onBack) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        addPlaybackQualityOption(options, actions, PlaybackQualityPolicy.AUTO, R.string.settings_playback_quality_auto, onBack);
        addPlaybackQualityOption(options, actions, PlaybackQualityPolicy.DATA_SAVER, R.string.settings_playback_quality_data_saver, onBack);
        addPlaybackQualityOption(options, actions, PlaybackQualityPolicy.HIGH, R.string.settings_playback_quality_high, onBack);
        showTvOptionsDialog(R.string.settings_playback_quality_title, getString(R.string.settings_playback_quality_message), options, actions, onBack);
    }

    private void addPlaybackQualityOption(List<String> options, List<Runnable> actions, String mode, int labelRes, Runnable onBack) {
        String label = getString(labelRes);
        if (mode.equals(PlaybackQualityPolicy.normalize(playbackQualityMode))) {
            label = getString(R.string.settings_selected_prefix, label);
        }
        options.add(label);
        actions.add(() -> {
            playbackQualityMode = PlaybackQualityPolicy.normalize(mode);
            if (prefs != null) {
                prefs.edit().putString(PREF_PLAYBACK_QUALITY_MODE, playbackQualityMode).apply();
            }
            if (playerController != null) {
                playerController.refreshVideoTrackPolicy();
            }
            showStatus(getString(R.string.settings_playback_quality_changed, playbackQualityLabel()));
            if (onBack != null) {
                postUiIfAlive(onBack);
            }
        });
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
        refreshRecordingsPanelSurface();
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
        overlayNavigationState.favoritesOnly = false;
        if (favoriteOrderStore != null) {
            favoriteOrderStore.load();
        }
        for (ChannelItem item : allChannels) {
            if (item != null) {
                item.favorite = false;
            }
        }
        refreshLocalChannelFilters(lastChannelId);
        refreshOverlayChannelList();
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
        refreshOverlayChannelList();
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
        overlayNavigationState.selectedFilterKey = "all";
        overlayNavigationState.favoritesOnly = false;
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
        hidePlaybackBehindModal();
        showModalBackdrop();
        hideRecordingsPanel();
        if (touchHomeHub != null) {
            touchHomeHub.setVisibility(View.GONE);
        }
        if (timeshiftBarContainer != null) {
            timeshiftBarContainer.setVisibility(View.GONE);
        }
    }

    private void showModalBackdrop() {
        uiHandler.removeCallbacks(hideOverlayRunnable);
        if (!isOverlayVisible()) {
            updateOverlayPanel();
            updateOverlaySearchState();
            channelOverlayCoordinator.showOverlay(channelOverlay, uiHandler, hideOverlayRunnable, 0L);
        }
    }

    private void hidePlaybackBehindModal() {
        if (playbackHiddenBehindModal) {
            return;
        }
        playbackHiddenBehindModal = true;
        if (playerView != null) {
            playerView.setAlpha(0f);
        }
        if (multiViewContainer != null) {
            multiViewContainer.setAlpha(0f);
        }
    }

    private void restorePlaybackAfterModal() {
        if (!playbackHiddenBehindModal) {
            return;
        }
        playbackHiddenBehindModal = false;
        if (playerView != null) {
            playerView.setAlpha(1f);
        }
        if (multiViewContainer != null) {
            multiViewContainer.setAlpha(1f);
        }
    }

    private void beginModalTransition(Runnable nextAction) {
        modalTransitionInProgress = true;
        modalReturnAction = nextAction;
    }

    private void finishModalTransitionWithoutChild() {
        if (!modalTransitionInProgress) {
            return;
        }
        modalTransitionInProgress = false;
        modalReturnAction = null;
        restorePlaybackAfterModal();
        hideOverlay();
    }

    private void finishModalTransitionAfterDelay() {
        postUiDelayedIfAlive(this::finishModalTransitionWithoutChild, 250L);
    }

    private void dismissModalForNextAction(Dialog dialog, Runnable nextAction) {
        beginModalTransition(null);
        if (nextAction != null) {
            postUiIfAlive(() -> {
                nextAction.run();
                postUiDelayedIfAlive(() -> {
                    modalTransitionInProgress = true;
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    finishModalTransitionAfterDelay();
                }, 80L);
            });
            return;
        }
        if (dialog != null) {
            dialog.dismiss();
        }
        finishModalTransitionAfterDelay();
    }

    private void handleModalShown() {
        modalTransitionInProgress = false;
    }

    private void handleModalDismissed() {
        enableImmersiveMode();
        if (modalTransitionInProgress) {
            return;
        }
        if (modalReturnAction != null) {
            Runnable returnAction = modalReturnAction;
            modalReturnAction = null;
            postUiIfAlive(returnAction);
            return;
        }
        restorePlaybackAfterModal();
        hideOverlay();
    }

    private void attachDialogViewTreeOwners(View dialogView) {
        if (dialogView == null) {
            return;
        }
        ViewTreeLifecycleOwner.set(dialogView, this);
        ViewTreeViewModelStoreOwner.set(dialogView, this);
        ViewTreeSavedStateRegistryOwner.set(dialogView, this);
    }

    private void showTvOptionsDialog(int titleResId, String message, List<String> options, List<Runnable> actions) {
        showTvOptionsDialog(titleResId, message, options, actions, null);
    }

    private void showTvOptionsDialog(String title, String message, List<String> options, List<Runnable> actions) {
        showTvOptionsDialog(title, message, options, actions, null);
    }

    private void showTvMessagePanel(String title, String message, List<TvMessageActionUiModel> actions, Runnable onCancel) {
        prepareModalSurface();
        final Dialog[] dialogHolder = new Dialog[1];
        ComposeView composeView = new ComposeView(this);
        attachDialogViewTreeOwners(composeView);
        List<TvMessageActionUiModel> wrappedActions = new ArrayList<>();
        if (actions != null) {
            for (TvMessageActionUiModel action : actions) {
                if (action == null) {
                    continue;
                }
                wrappedActions.add(new TvMessageActionUiModel(action.label, action.destructive, () -> {
                    dismissModalForNextAction(dialogHolder[0], action.onClick);
                }));
            }
        }
        if (wrappedActions.isEmpty()) {
            wrappedActions.add(new TvMessageActionUiModel(getString(R.string.dialog_close), false, () -> {
                if (dialogHolder[0] != null) {
                    dialogHolder[0].dismiss();
                }
            }));
        }
        Runnable closeAction = () -> {
            if (dialogHolder[0] != null) {
                dialogHolder[0].dismiss();
            }
        };
        TvMessagePanelComposeBinder.bind(
                composeView,
                new TvMessagePanelUiModel(
                        title == null || title.trim().isEmpty() ? getString(R.string.app_name) : title,
                        message == null || message.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : message,
                        wrappedActions,
                        closeAction
                )
        );
        Dialog dialog = ComposeDialogHost.showFullscreen(this, composeView, () -> {
            if (onCancel != null) {
                beginModalTransition(null);
                postUiIfAlive(onCancel);
                finishModalTransitionAfterDelay();
            }
        }, this::handleModalDismissed);
        dialogHolder[0] = dialog;
        handleModalShown();
    }

    private void showStructuredStatusPanel(String title, String subtitle, String summary, List<PlaybackDiagnosticsRowUiModel> rows, List<String> notes, List<TvMessageActionUiModel> actions) {
        showStructuredStatusPanel(title, subtitle, summary, rows, notes, actions, null);
    }

    private void showStructuredStatusPanel(String title, String subtitle, String summary, List<PlaybackDiagnosticsRowUiModel> rows, List<String> notes, List<TvMessageActionUiModel> actions, Runnable onCancel) {
        prepareModalSurface();
        final Dialog[] dialogHolder = new Dialog[1];
        ComposeView composeView = new ComposeView(this);
        attachDialogViewTreeOwners(composeView);
        List<TvMessageActionUiModel> wrappedActions = new ArrayList<>();
        if (actions != null) {
            for (TvMessageActionUiModel action : actions) {
                if (action == null) {
                    continue;
                }
                wrappedActions.add(new TvMessageActionUiModel(action.label, action.destructive, () -> {
                    dismissModalForNextAction(dialogHolder[0], action.onClick);
                }));
            }
        }
        if (wrappedActions.isEmpty()) {
            wrappedActions.add(new TvMessageActionUiModel(getString(R.string.dialog_close), false, () -> {
                if (dialogHolder[0] != null) {
                    dialogHolder[0].dismiss();
                }
            }));
        }
        PlaybackDiagnosticsPanelComposeBinder.bind(
                composeView,
                new PlaybackDiagnosticsPanelUiModel(
                        title == null || title.trim().isEmpty() ? getString(R.string.title_playback_diagnostics) : title,
                        subtitle == null ? "" : subtitle,
                        summary == null ? "" : summary,
                        rows == null ? Collections.emptyList() : rows,
                        notes == null ? Collections.emptyList() : notes,
                        wrappedActions
                )
        );
        Dialog dialog = ComposeDialogHost.showFullscreen(this, composeView, () -> {
            if (onCancel != null) {
                beginModalTransition(null);
                postUiIfAlive(onCancel);
                finishModalTransitionAfterDelay();
            }
        }, this::handleModalDismissed);
        dialogHolder[0] = dialog;
        handleModalShown();
    }

    private void showTvTextInputPanel(TvTextInputPanelUiModel model) {
        if (model == null) {
            return;
        }
        prepareModalSurface();
        final Dialog[] dialogHolder = new Dialog[1];
        ComposeView composeView = new ComposeView(this);
        attachDialogViewTreeOwners(composeView);
        TvTextInputPanelUiModel wrapped = new TvTextInputPanelUiModel(
                model.title,
                model.message,
                model.positiveLabel,
                model.negativeLabel,
                model.neutralLabel,
                model.fields,
                values -> {
                    dismissModalForNextAction(dialogHolder[0], model.onSubmit == null ? null : () -> model.onSubmit.submit(values));
                },
                () -> {
                    dismissModalForNextAction(dialogHolder[0], model.onCancel);
                },
                () -> {
                    dismissModalForNextAction(dialogHolder[0], model.onNeutral);
                }
        );
        TvTextInputPanelComposeBinder.bind(composeView, wrapped);
        Dialog dialog = ComposeDialogHost.showFullscreen(this, composeView, () -> {
            if (model.onCancel != null) {
                beginModalTransition(null);
                postUiIfAlive(model.onCancel);
                finishModalTransitionAfterDelay();
            }
        }, this::handleModalDismissed);
        dialogHolder[0] = dialog;
        handleModalShown();
    }

    private void showTvOptionsDialog(int titleResId, String message, List<String> options, List<Runnable> actions, Runnable onBack) {
        showTvOptionsDialog(getString(titleResId), message, options, actions, onBack);
    }

    private void showTvOptionsDialog(String title, String message, List<String> options, List<Runnable> actions, Runnable onBack) {
        prepareModalSurface();
        final boolean[] navigationHandled = {false};
        final Dialog[] dialogHolder = new Dialog[1];
        ComposeView composeView = new ComposeView(this);
        attachDialogViewTreeOwners(composeView);
        List<TvOptionsPanelRowUiModel> rows = new ArrayList<>();
        for (int i = 0; options != null && i < options.size(); i++) {
            final int index = i;
            rows.add(new TvOptionsPanelRowUiModel(
                    options.get(i),
                    String.valueOf(i + 1),
                    () -> {
                        if (index >= 0 && actions != null && index < actions.size()) {
                            navigationHandled[0] = true;
                            dismissModalForNextAction(dialogHolder[0], actions.get(index));
                        }
                    }
            ));
        }
        Runnable backAction = () -> {
            navigationHandled[0] = true;
            dismissModalForNextAction(dialogHolder[0], onBack);
        };
        TvOptionsPanelComposeBinder.bind(
                composeView,
                new TvOptionsPanelUiModel(
                        title == null || title.trim().isEmpty() ? getString(R.string.app_name) : title,
                        message,
                        getString(onBack == null ? R.string.dialog_close : R.string.dialog_back),
                        rows,
                        backAction
                )
        );
        Dialog dialog = ComposeDialogHost.showFullscreen(this, composeView, this::handleModalDismissed);
        dialogHolder[0] = dialog;
        if (onBack != null) {
            dialog.setOnKeyListener((ignored, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP && !navigationHandled[0]) {
                    backAction.run();
                    return true;
                }
                return false;
            });
        }
        handleModalShown();
    }

    private void maybeShowStartupHub() {
        if (startupHubShown || (prefs != null && prefs.getBoolean(PREF_STARTUP_HUB_DISABLED, false))) {
            return;
        }
        startupHubShown = true;
        postUiDelayedIfAlive(this::loadStartupHubStateAndShow, 700L);
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
            postUiIfAlive(() -> showStartupHubDialog(state));
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
                    ? getString(R.string.startup_hub_continue_vod_progress, decorateProtectedItemTitle(lastVod, displayName(lastVod)), formatDurationShort(resumeMs))
                    : getString(R.string.startup_hub_continue_vod, decorateProtectedItemTitle(lastVod, displayName(lastVod))));
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
            actions.add(() -> showVodLibraryDialog(() -> showStartupHubDialog(state)));
        }
        if (shouldShowGenericVodQuickTarget(true)) {
            options.add(decorateProtectedLabel(getString(R.string.touch_home_button_adult, countItemsForQuickTarget("vod-adult")), currentOfflinePermissions != null && currentOfflinePermissions.protectAdultVod));
            actions.add(() -> ensureParentalAccessForFilterKey("vod:tivify:adult", () -> showVodVisualLibraryDialog(VodVisualTypeFilter.ADULT, VodVisualPlatformFilter.ALL, VodVisualStatusFilter.ALL, VodVisualSortFilter.SMART, "", () -> showStartupHubDialog(state))));
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
        options.add(getString(R.string.offline_catalog_action_refresh));
        actions.add(this::refreshOfflineCatalogFromSettings);

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
        if (overlayNavigationState.favoritesOnly || "favorites".equals(overlayNavigationState.selectedFilterKey)) {
            return getString(R.string.touch_home_filter_favorites);
        }
        for (ChannelFilter filter : filters) {
            if (filter != null && overlayNavigationState.selectedFilterKey != null && overlayNavigationState.selectedFilterKey.equals(filter.key) && filter.label != null && !filter.label.trim().isEmpty()) {
                return decorateProtectedFilterLabel(filter);
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
        actions.add(() -> showVodLibraryDialog(this::showQuickHubDialog));
        options.add(getString(R.string.tools_section_tv_guide));
        actions.add(this::showTvAndGuideToolsDialog);
        options.add(getString(R.string.quick_hub_recent));
        actions.add(this::showRecentChannelsQuickDialog);
        options.add(getString(R.string.quick_hub_favorites));
        actions.add(this::showFavoriteChannelsQuickDialog);
        options.add(getString(R.string.quick_hub_lists));
        actions.add(this::showPersonalListsManagerDialog);
        options.add(getString(R.string.tools_menu_open_parental));
        actions.add(this::showParentalSettingsDialog);
        if (!isOfflineRecordingsDisabled()) {
            options.add(getString(R.string.quick_hub_recordings));
            actions.add(this::openRecordingsBrowser);
        }
        if (prefs != null && prefs.getBoolean(PREF_STARTUP_HUB_DISABLED, false)) {
            options.add(getString(R.string.quick_hub_enable_startup));
            actions.add(this::enableStartupHub);
        }
        options.add(getString(R.string.tools_menu_open_advanced));
        actions.add(this::showAdvancedToolsMenu);
        showTvOptionsDialog(R.string.title_quick_hub, null, options, actions);
    }

    private void showVodSearchDialog() {
        showVodSearchDialog("");
    }

    private void showVodSearchDialog(String initialQuery) {
        showVodSearchDialog(initialQuery, null);
    }

    private void showVodSearchDialog(String initialQuery, Runnable onBack) {
        clearQuickSearchOverlay();
        hideOverlay();
        showTvTextInputPanel(new TvTextInputPanelUiModel(
                getString(R.string.title_vod_search),
                "",
                getString(R.string.search_channel_dialog_action),
                getString(R.string.dialog_cancel),
                getString(R.string.vod_search_all),
                java.util.Collections.singletonList(new TvTextInputFieldUiModel(getString(R.string.vod_search_hint), initialQuery == null ? "" : initialQuery.trim(), false, false)),
                values -> showVodSearchResults(values == null || values.isEmpty() ? "" : values.get(0), onBack),
                onBack,
                () -> showVodSearchResults("", onBack)
        ));
    }

    private void showVodSearchResults(String query) {
        showVodSearchResults(query, null);
    }

    private void showVodSearchResults(String query, Runnable onBack) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            showVodVisualLibraryDialog(onBack);
            return;
        }
        showVodVisualLibraryDialog(VodVisualTypeFilter.ALL, VodVisualPlatformFilter.ALL, VodVisualStatusFilter.ALL, VodVisualSortFilter.SMART, trimmed, onBack);
    }

    private void showVodLibraryList(int titleResId, List<ChannelItem> items, boolean progressFirst) {
        showVodLibraryList(titleResId, items, progressFirst, null);
    }

    private void showVodLibraryList(int titleResId, List<ChannelItem> items, boolean progressFirst, Runnable onBack) {
        showVodLibraryList(titleResId, items, progressFirst, onBack, 0);
    }

    private void showVodLibraryList(int titleResId, List<ChannelItem> items, boolean progressFirst, Runnable onBack, int pageIndex) {
        if (progressFirst) {
            sortVodLibraryItems(items);
        }
        showPagedVodLibraryList(getString(titleResId), items, onBack, pageIndex);
    }

    private void showPagedVodLibraryList(String title, List<ChannelItem> items, Runnable onBack, int pageIndex) {
        List<ChannelItem> sourceItems = items == null ? new ArrayList<>() : items;
        int total = sourceItems.size();
        if (total <= VOD_DENSE_PAGE_SIZE) {
            Runnable returnToList = () -> showPagedVodLibraryList(title, sourceItems, onBack, 0);
            showQuickChannelListDialog(
                    title,
                    sourceItems,
                    getString(R.string.vod_library_empty),
                    item -> showVodInfoDialog(item, returnToList),
                    onBack
            );
            return;
        }
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) VOD_DENSE_PAGE_SIZE));
        int safePage = Math.max(0, Math.min(pageIndex, totalPages - 1));
        int start = safePage * VOD_DENSE_PAGE_SIZE;
        int end = Math.min(total, start + VOD_DENSE_PAGE_SIZE);
        List<ChannelItem> pageItems = new ArrayList<>(sourceItems.subList(start, end));
        Runnable returnToPage = () -> showPagedVodLibraryList(title, sourceItems, onBack, safePage);
        List<ZapActionItem> actions = new ArrayList<>();
        actions.add(new ZapActionItem(
                getString(R.string.vod_dense_prev_page),
                safePage > 0,
                false,
                false,
                safePage > 0 ? () -> showPagedVodLibraryList(title, sourceItems, onBack, safePage - 1) : null
        ));
        actions.add(new ZapActionItem(
                getString(R.string.vod_dense_next_page),
                safePage < totalPages - 1,
                false,
                false,
                safePage < totalPages - 1 ? () -> showPagedVodLibraryList(title, sourceItems, onBack, safePage + 1) : null
        ));
        String subtitle = getString(R.string.vod_dense_page_subtitle, start + 1, end, total, safePage + 1, totalPages);
        showQuickChannelListDialog(
                title,
                subtitle,
                pageItems,
                getString(R.string.vod_library_empty),
                item -> showVodInfoDialog(item, returnToPage),
                onBack,
                actions
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

    enum VodVisualTypeFilter {
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

    enum VodVisualPlatformFilter {
        ALL("Todo"),
        MOVISTAR("Movistar"),
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

    enum VodVisualStatusFilter {
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

    enum VodVisualSortFilter {
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
            if (item == null || !item.isVod || shouldHideProtectedItem(item) || !matchesVodVisualType(item, typeFilter) || !matchesVodVisualPlatform(item, platformFilter) || !matchesVodVisualStatus(item, statusFilter)) {
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
        int movistar = 0;
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
            if (matchesVodVisualPlatform(item, VodVisualPlatformFilter.MOVISTAR)) {
                movistar++;
            } else if (matchesVodVisualPlatform(item, VodVisualPlatformFilter.TIVIFY)) {
                tivify++;
            } else if (matchesVodVisualPlatform(item, VodVisualPlatformFilter.RUNTIME)) {
                runtime++;
            }
            if (getVodResumePosition(item.id) > 30_000L) {
                progress++;
            }
        }
        return getString(R.string.vod_search_visual_summary, all.size(), movistar, tivify, runtime, adult, progress);
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
        boolean isMovistar = filterKey.contains("movistar") || platform.contains("movistar");
        boolean isTivify = filterKey.contains("tivify") || platform.contains("tivify");
        boolean isRuntime = filterKey.contains("runtime") || platform.contains("runtime");
        if (platformFilter == VodVisualPlatformFilter.MOVISTAR) {
            return isMovistar;
        }
        if (platformFilter == VodVisualPlatformFilter.TIVIFY) {
            return isTivify;
        }
        if (platformFilter == VodVisualPlatformFilter.RUNTIME) {
            return isRuntime;
        }
        return !isMovistar && !isTivify && !isRuntime;
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
            if (item == null || !item.isVod || shouldHideProtectedItem(item)) {
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
        showVodCategoriesDialog(null);
    }

    private void showVodCategoriesDialog(Runnable onBack) {
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
        for (Map.Entry<String, List<ChannelItem>> entry : entries) {
            List<ChannelItem> categoryItems = new ArrayList<>(entry.getValue());
            sortVodLibraryItems(categoryItems);
            entry.setValue(categoryItems);
        }
        Runnable returnToCategories = () -> showVodCategoriesDialog(onBack);
        TvOptionsMenuModel menu = VodLibraryMenuUiFactory.buildCategories(entries, new VodLibraryMenuUiFactory.AdvancedHost() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String displayName(ChannelItem item) {
                return MainActivity.this.displayName(item);
            }

            @Override
            public void openCategory(String title, List<ChannelItem> items) {
                showPagedVodLibraryList(title, items, returnToCategories, 0);
            }

            @Override
            public void continueVod(ChannelItem item) {
            }

            @Override
            public void startOver(ChannelItem item) {
            }

            @Override
            public void clearProgress(ChannelItem item) {
            }
        });
        showTvOptionsDialog(R.string.vod_library_categories, menu.message, menu.options, menu.actions, onBack);
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
            if (item != null && item.isVod && !shouldHideProtectedItem(item)) {
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
            if (item == null || !item.isVod || shouldHideProtectedItem(item)) {
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

    private List<ChannelItem> buildMovistarVodItems() {
        if (cachedMovistarVodItemsValid) {
            return new ArrayList<>(cachedMovistarVodItems);
        }
        List<ChannelItem> items = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (ChannelItem item : allChannels) {
            if (item == null || !item.isVod || item.isAdultVod || shouldHideProtectedItem(item)) {
                continue;
            }
            String filterKey = item.vodFilterKey == null ? "" : item.vodFilterKey.toLowerCase(Locale.ROOT);
            String platform = item.platformName == null ? "" : item.platformName.toLowerCase(Locale.ROOT);
            if ((filterKey.contains("movistar") || platform.contains("movistar")) && added.add(item.id)) {
                items.add(item);
            }
        }
        sortVodLibraryItems(items);
        cachedMovistarVodItems.clear();
        cachedMovistarVodItems.addAll(items);
        cachedMovistarVodItemsValid = true;
        return items;
    }

    private void invalidateVodDerivedCaches() {
        cachedMovistarVodItems.clear();
        cachedMovistarVodItemsValid = false;
    }

    private List<ChannelItem> buildVodProgressItems() {
        List<ChannelItem> items = new ArrayList<>();
        for (Map.Entry<String, Long> entry : vodResumePositions.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 30_000L) {
                continue;
            }
            ChannelItem item = findChannelItemById(entry.getKey());
            if (item != null && item.isVod && !shouldHideProtectedItem(item)) {
                items.add(item);
            }
        }
        sortVodLibraryItems(items);
        return items;
    }

    private List<ChannelItem> buildVodNotStartedItems() {
        List<ChannelItem> items = new ArrayList<>();
        for (ChannelItem item : allChannels) {
            if (item == null || !item.isVod || shouldHideProtectedItem(item) || item.isAdultVod || getVodResumePosition(item.id) > 0L) {
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
        showVodProgressManagerDialog(null);
    }

    private void showVodProgressManagerDialog(Runnable onBack) {
        showQuickChannelListDialog(
                getString(R.string.vod_library_manage_progress),
                buildVodProgressItems(),
                getString(R.string.vod_continue_empty),
                item -> showVodProgressActionsDialog(item, () -> showVodProgressManagerDialog(onBack)),
                onBack
        );
    }

    private void showVodProgressActionsDialog(ChannelItem item) {
        showVodProgressActionsDialog(item, null);
    }

    private void showVodProgressActionsDialog(ChannelItem item, Runnable onBack) {
        if (item == null) {
            return;
        }
        TvOptionsMenuModel menu = VodLibraryMenuUiFactory.buildProgressActions(item, new VodLibraryMenuUiFactory.AdvancedHost() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String displayName(ChannelItem item) {
                return MainActivity.this.displayName(item);
            }

            @Override
            public void openCategory(String title, List<ChannelItem> items) {
            }

            @Override
            public void continueVod(ChannelItem target) {
                ensureParentalAccessForItem(target, () -> playChannelItemInternal(target, true, getVodResumePosition(target.id)));
            }

            @Override
            public void startOver(ChannelItem target) {
                clearVodResumePosition(target.id);
                ensureParentalAccessForItem(target, () -> playChannelItemInternal(target, true, 0L));
            }

            @Override
            public void clearProgress(ChannelItem target) {
                clearVodResumePosition(target.id);
                showStatus(getString(R.string.vod_status_progress_cleared));
            }
        });
        showTvOptionsDialog(R.string.vod_library_manage_progress, menu.message, menu.options, menu.actions, onBack);
    }

    private List<ChannelItem> buildVodSearchResults(String query, boolean includeAdult) {
        List<ChannelItem> results = new ArrayList<>();
        for (ChannelItem item : allChannels) {
            if (item == null || !item.isVod || shouldHideProtectedItem(item) || (!includeAdult && item.isAdultVod)) {
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
        ensureParentalAccessForItem(item, () -> showVodInfoDialog(item));
    }

    private void showPersonalListsManagerDialog() {
        if (channelCollectionStore == null) {
            return;
        }
        List<ChannelCollectionStore.ChannelCollection> collections = channelCollectionStore.getCollections();
        prepareModalSurface();
        final Dialog[] dialogHolder = new Dialog[1];
        ComposeView personalListComposeView = new ComposeView(this);
        attachDialogViewTreeOwners(personalListComposeView);
        PersonalListComposeBinder.bindPanel(
                personalListComposeView,
                getString(R.string.title_manage_personal_lists),
                getString(R.string.personal_list_manager_hint, collections.size()),
                getString(R.string.personal_list_create),
                getString(R.string.dialog_close),
                buildPersonalListManagerUiModel(collections, () -> {
                    if (dialogHolder[0] != null) {
                        dialogHolder[0].dismiss();
                    }
                }),
                () -> {
                    if (dialogHolder[0] != null) {
                        dialogHolder[0].dismiss();
                    }
                    showCreatePersonalListDialog();
                },
                () -> {
                    if (dialogHolder[0] != null) {
                        dialogHolder[0].dismiss();
                    }
                }
        );
        Dialog dialog = ComposeDialogHost.showFullscreen(this, personalListComposeView, this::handleModalDismissed);
        dialogHolder[0] = dialog;
        handleModalShown();
    }

    private PersonalListManagerUiModel buildPersonalListManagerUiModel(List<ChannelCollectionStore.ChannelCollection> collections) {
        return buildPersonalListManagerUiModel(collections, null);
    }

    private PersonalListManagerUiModel buildPersonalListManagerUiModel(List<ChannelCollectionStore.ChannelCollection> collections, Runnable beforeAction) {
        return PersonalListUiFactory.build(collections, new PersonalListUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String text(int resId, Object... args) {
                return getString(resId, args);
            }

            @Override
            public String preview(ChannelCollectionStore.ChannelCollection collection) {
                return buildPersonalListPreview(collection);
            }

            @Override
            public void beforeAction() {
                if (beforeAction != null) {
                    beforeAction.run();
                }
            }

            @Override
            public void openChannels(ChannelCollectionStore.ChannelCollection collection) {
                showPersonalListChannelsPanel(collection);
            }

            @Override
            public void openActions(ChannelCollectionStore.ChannelCollection collection) {
                showPersonalListActionsDialog(collection);
            }
        });
    }

    private void showPersonalListActionsDialog(ChannelCollectionStore.ChannelCollection collection) {
        if (collection == null) {
            return;
        }
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.personal_list_view_channels));
        actions.add(() -> showPersonalListChannelsPanel(collection));
        options.add(getString(R.string.personal_list_open));
        actions.add(() -> applyPersonalListFilter(collection.key));
        options.add(getString(R.string.personal_list_rename));
        actions.add(() -> showRenamePersonalListDialog(collection));
        options.add(getString(R.string.personal_list_delete));
        actions.add(() -> {
            channelCollectionStore.deleteCollection(collection.key);
            refreshLocalChannelFilters(lastChannelId);
            showStatus(getString(R.string.status_personal_list_deleted));
        });
        showTvOptionsDialog(collection.label, null, options, actions);
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
            postUiIfAlive(() -> showPersonalListChannelsPanel(created));
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
        showTvTextInputPanel(new TvTextInputPanelUiModel(
                getString(R.string.title_manage_personal_lists),
                "",
                getString(android.R.string.ok),
                getString(R.string.dialog_cancel),
                "",
                java.util.Collections.singletonList(new TvTextInputFieldUiModel(getString(R.string.personal_list_name_hint), initialValue == null ? "" : initialValue, false, false)),
                values -> {
                    String value = values == null || values.isEmpty() ? "" : values.get(0).trim();
                    if (value.isEmpty()) {
                        showStatus(getString(R.string.status_personal_list_empty_name));
                        return;
                    }
                    action.apply(value);
                },
                null,
                null
        ));
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
        refreshOverlayChannelList();
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
        prepareModalSurface();
        if (touchControlsBar != null) {
            touchControlsBar.setVisibility(View.GONE);
        }
        prefetchChannelLogos(items, SEARCH_LOGO_PREFETCH_LIMIT, 42, 42);
        ComposeView quickChannelListComposeView = new ComposeView(this);
        attachDialogViewTreeOwners(quickChannelListComposeView);
        final Dialog[] dialogHolder = new Dialog[1];
        QuickChannelListComposeBinder.bind(quickChannelListComposeView, buildQuickChannelListUiModel(
                getString(R.string.personal_list_channels_title, currentCollection.label),
                getString(R.string.personal_list_channels_hint, items.size()),
                items,
                dialogHolder,
                item ->
                showPersonalListChannelActionsDialog(currentCollection, item)
        ), (imageView, item) -> {
            if (imageView == null || item == null) {
                return;
            }
            if (item.vod) {
                bindVodPosterList(imageView, item.logoUrl);
            } else {
                bindChannelLogo(imageView, item.logoUrl, item.channelName, 42, 42);
            }
        });
        Dialog dialog = ComposeDialogHost.showFullscreen(this, quickChannelListComposeView, this::handleModalDismissed);
        dialogHolder[0] = dialog;
        handleModalShown();
    }

    private void showPersonalListChannelActionsDialog(ChannelCollectionStore.ChannelCollection collection, ChannelItem item) {
        if (collection == null || item == null) {
            return;
        }
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.personal_list_channel_action_tune));
        actions.add(() -> tuneQuickAccessChannel(item));
        options.add(getString(R.string.personal_list_channel_action_remove));
        actions.add(() -> {
            if (channelCollectionStore != null) {
                channelCollectionStore.setMembership(collection.key, item.id, false);
            }
            refreshLocalChannelFilters(item.id);
            showStatus(getString(R.string.status_personal_list_channel_removed));
            ChannelCollectionStore.ChannelCollection refreshed = channelCollectionStore == null ? null : channelCollectionStore.getCollection(collection.key);
            if (refreshed != null && !refreshed.channelIds.isEmpty()) {
                postUiIfAlive(() -> showPersonalListChannelsPanel(refreshed));
            }
        });
        options.add(getString(R.string.personal_list_channel_action_profile));
        actions.add(() -> showChannelProfileDialog(item));
        showTvOptionsDialog(displayName(item), null, options, actions);
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
        prepareModalSurface();
        closeMultiView();
        ComposeView searchComposeView = new ComposeView(this);
        attachDialogViewTreeOwners(searchComposeView);
        globalSearchFilter = GLOBAL_SEARCH_FILTER_ALL;

        final Dialog[] dialogHolder = new Dialog[1];
        final String[] queryHolder = new String[]{initialQuery == null ? "" : initialQuery.trim()};
        renderGlobalSearchResults(searchComposeView, buildGlobalSearchLocalResults(queryHolder[0], globalSearchFilter), dialogHolder, queryHolder);

        Dialog dialog = ComposeDialogHost.showFullscreen(this, searchComposeView, this::handleModalDismissed);
        dialogHolder[0] = dialog;

        handleModalShown();
        searchComposeView.requestFocus();
        updateGlobalSearchResults(searchComposeView, dialogHolder, queryHolder, queryHolder[0]);
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

    private void renderGlobalSearchResults(ComposeView composeView, List<GlobalSearchResult> results, Dialog[] dialogHolder, String[] queryHolder) {
        if (composeView == null) {
            return;
        }
        String query = queryHolder == null ? "" : queryHolder[0];
        GlobalSearchListComposeBinder.bind(composeView, buildGlobalSearchListUiModel(results, dialogHolder, queryHolder, composeView, query), (imageView, item) -> {
            if (imageView == null || item == null || item.header) {
                if (imageView != null) {
                    imageView.setImageDrawable(null);
                }
                return;
            }
            switch (item.imageKind) {
                case GlobalSearchRowUiModel.IMAGE_CHANNEL:
                    bindChannelLogo(imageView, item.imageUrl, item.imageName, 42, 42);
                    break;
                case GlobalSearchRowUiModel.IMAGE_PROGRAM:
                    bindProgramPoster(imageView, item.imageUrl);
                    break;
                case GlobalSearchRowUiModel.IMAGE_RECORDING:
                    bindRecordingPoster(imageView, item.imageUrl);
                    break;
                default:
                    imageView.setImageDrawable(null);
                    break;
            }
        });
    }

    private GlobalSearchListUiModel buildGlobalSearchListUiModel(List<GlobalSearchResult> results, Dialog[] dialogHolder, String[] queryHolder, ComposeView composeView, String query) {
        return GlobalSearchUiFactory.build(query, results, new GlobalSearchUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String displayName(ChannelItem item) {
                return MainActivity.this.displayName(item);
            }

            @Override
            public String recordingTitle(RecordingsRepository.RecordingItem item) {
                return buildRecordingTitle(item);
            }

            @Override
            public String protectedTitle(ChannelItem item, String title) {
                return decorateProtectedItemTitle(item, title);
            }

            @Override
            public String protectedMeta(ChannelItem item, String meta) {
                return decorateProtectedMeta(item, meta);
            }

            @Override
            public String protectedBadge(ChannelItem item, String fallback) {
                return buildProtectedTypeBadge(item, fallback);
            }

            @Override
            public String filterLabel(int filter) {
                return globalSearchFilterLabel(filter);
            }

            @Override
            public int[] filters() {
                return new int[]{
                        GLOBAL_SEARCH_FILTER_ALL,
                        GLOBAL_SEARCH_FILTER_TV,
                        GLOBAL_SEARCH_FILTER_VOD,
                        GLOBAL_SEARCH_FILTER_FAVORITES,
                        GLOBAL_SEARCH_FILTER_EPG,
                        GLOBAL_SEARCH_FILTER_RECORDINGS
                };
            }

            @Override
            public int currentFilter() {
                return globalSearchFilter;
            }

            @Override
            public boolean recordingsDisabled() {
                return isOfflineRecordingsDisabled();
            }

            @Override
            public boolean isHeader(GlobalSearchResult result) {
                return result != null && result.type == GLOBAL_SEARCH_HEADER;
            }

            @Override
            public boolean isHistory(GlobalSearchResult result) {
                return result != null && result.type == GLOBAL_SEARCH_HISTORY;
            }

            @Override
            public void applyFilter(int filter) {
                globalSearchFilter = filter;
                updateGlobalSearchResults(composeView, dialogHolder, queryHolder, queryHolder == null ? "" : queryHolder[0]);
            }

            @Override
            public void applyQuery(String query) {
                if (queryHolder != null) {
                    queryHolder[0] = query;
                }
                updateGlobalSearchResults(composeView, dialogHolder, queryHolder, query);
            }

            @Override
            public void rememberQuery(String query) {
                rememberGlobalSearchQuery(query);
            }

            @Override
            public void openResult(GlobalSearchResult result) {
                if (dialogHolder != null && dialogHolder[0] != null) {
                    dialogHolder[0].dismiss();
                }
                handleGlobalSearchResult(result);
            }

            @Override
            public void openActions(GlobalSearchResult result) {
                showGlobalSearchActions(result);
            }
        });
    }

    private void updateGlobalSearchResults(ComposeView composeView, Dialog[] dialogHolder, String[] queryHolder, String query) {
        if (composeView == null) {
            return;
        }
        if (queryHolder != null) {
            queryHolder[0] = query == null ? "" : query;
        }
        int generation = ++globalSearchGeneration;
        if (pendingGlobalSearchRunnable != null) {
            uiHandler.removeCallbacks(pendingGlobalSearchRunnable);
            pendingGlobalSearchRunnable = null;
        }
        renderGlobalSearchResults(composeView, buildGlobalSearchLocalResults(query, globalSearchFilter), dialogHolder, queryHolder);
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() < 2) {
            return;
        }
        int requestedFilter = globalSearchFilter;
        pendingGlobalSearchRunnable = () -> fetchGlobalSearchRemoteResults(composeView, dialogHolder, queryHolder, trimmed, generation, requestedFilter);
        postUiDelayedIfAlive(pendingGlobalSearchRunnable, 450L);
    }

    private void fetchGlobalSearchRemoteResults(ComposeView composeView, Dialog[] dialogHolder, String[] queryHolder, String query, int generation, int requestedFilter) {
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
            postUiIfAlive(() -> {
                if (generation != globalSearchGeneration) {
                    return;
                }
                List<GlobalSearchResult> merged = new ArrayList<>(buildGlobalSearchLocalResults(query, requestedFilter));
                merged.addAll(remoteResults);
                renderGlobalSearchResults(composeView, merged, dialogHolder, queryHolder);
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
            if (shouldHideProtectedItem(item)) {
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
        if (current != null && !shouldHideProtectedItem(current) && globalSearchIncludesItem(filter, current)) {
            results.add(globalSearchHeader(getString(R.string.global_search_section_suggestions)));
            suggestionHeaderAdded = true;
            results.add(new GlobalSearchResult(current.isVod ? GLOBAL_SEARCH_VOD : GLOBAL_SEARCH_CHANNEL, displayName(current), current.isVod ? buildVodRowMeta(current) : getString(R.string.quick_hub_continue), current.isVod ? getString(R.string.channel_badge_vod) : getString(R.string.channel_badge_live), current, null, null, ""));
        }
        ChannelItem lastVod = findChannelItemById(lastVodId);
        if (lastVod != null && lastVod.isVod && !shouldHideProtectedItem(lastVod) && globalSearchIncludesItem(filter, lastVod) && (current == null || !lastVod.id.equals(current.id))) {
            if (!suggestionHeaderAdded) {
                results.add(globalSearchHeader(getString(R.string.global_search_section_suggestions)));
                suggestionHeaderAdded = true;
            }
            results.add(new GlobalSearchResult(GLOBAL_SEARCH_VOD, displayName(lastVod), buildVodInfoMeta(lastVod), getString(R.string.channel_badge_vod), lastVod, null, null, ""));
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
                if (shouldHideProtectedItem(item)) {
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
            ensureParentalAccessForItem(result.channel, () -> {
                if (result.channel.isVod) {
                    showVodInfoDialog(result.channel);
                } else {
                    tuneChannelById(result.channel.id);
                }
            });
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
            overlayNavigationState.currentIndex = findChannelIndexById(channel.id);
            lastChannelId = channel.id;
            showPlaybackDiagnosticsDialog();
        });
        showTvOptionsDialog(R.string.title_global_search, displayName(channel), options, actions);
    }

    private void showEpgSearchDialog() {
        clearQuickSearchOverlay();
        hideOverlay();
        hideRecordingsPanel();
        showTvTextInputPanel(new TvTextInputPanelUiModel(
                getString(R.string.title_epg_search),
                getString(R.string.epg_search_scanned_hint),
                getString(R.string.search_channel_dialog_action),
                getString(R.string.dialog_cancel),
                "",
                java.util.Collections.singletonList(new TvTextInputFieldUiModel(getString(R.string.epg_search_hint), "", false, false)),
                values -> searchEpgPrograms(values == null || values.isEmpty() ? "" : values.get(0)),
                null,
                null
        ));
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
                postUiIfAlive(() -> showEpgSearchResultsDialog(trimmedQuery, results));
            } catch (Exception e) {
                Log.w(TAG, "EPG search failed", e);
                postUiIfAlive(() -> showStatus(getString(R.string.status_failed_load_guide)));
            }
        });
    }

    private List<EpgSearchResult> buildEpgSearchResults(String query) throws Exception {
        List<EpgSearchResult> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<ChannelItem> searchScope = new ArrayList<>();
        for (ChannelItem item : channels) {
            if (item != null && !item.isVod && !shouldHideProtectedItem(item) && item.id != null && !item.id.trim().isEmpty()) {
                searchScope.add(item);
            }
        }
        if (searchScope.isEmpty()) {
            for (ChannelItem item : allChannels) {
                if (item != null && !item.isVod && !shouldHideProtectedItem(item) && item.id != null && !item.id.trim().isEmpty()) {
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
        prepareModalSurface();
        ComposeView composeView = new ComposeView(this);
        attachDialogViewTreeOwners(composeView);
        final Dialog[] dialogHolder = new Dialog[1];
        EpgSearchResultsComposeBinder.bind(composeView, buildEpgSearchResultListUiModel(
                getString(R.string.epg_search_results_title, query),
                getString(R.string.epg_search_results_hint, results.size()),
                results,
                dialogHolder
        ), (imageView, item) -> {
            if (imageView == null || item == null) {
                return;
            }
            bindProgramPoster(imageView, item.imageUrl);
        });
        Dialog dialog = ComposeDialogHost.showFullscreen(this, composeView, this::handleModalDismissed);
        dialogHolder[0] = dialog;
        handleModalShown();
    }

    private void showMiniGuideDialog(ChannelItem channel, List<EpgRepository.EpgProgram> items) {
        prepareModalSurface();
        ComposeView composeView = new ComposeView(this);
        attachDialogViewTreeOwners(composeView);
        MiniGuideComposeBinder.bind(composeView, buildMiniGuideUiModel(
                channel,
                items,
                getString(R.string.title_guide, displayName(channel)),
                getString(R.string.mini_guide_hint, items == null ? 0 : items.size())
        ));
        ComposeDialogHost.showFullscreen(this, composeView, this::handleModalDismissed);
        handleModalShown();
    }

    private EpgSearchResultListUiModel buildEpgSearchResultListUiModel(List<EpgSearchResult> results, Dialog[] dialogHolder) {
        return buildEpgSearchResultListUiModel(null, null, results, dialogHolder);
    }

    private EpgSearchResultListUiModel buildEpgSearchResultListUiModel(String title, String subtitle, List<EpgSearchResult> results, Dialog[] dialogHolder) {
        return EpgGuideUiFactory.buildSearchResults(title, subtitle, results, buildEpgGuideUiHost(dialogHolder));
    }

    private MiniGuideUiModel buildMiniGuideUiModel(ChannelItem channel, List<EpgRepository.EpgProgram> items) {
        return buildMiniGuideUiModel(channel, items, null, null);
    }

    private MiniGuideUiModel buildMiniGuideUiModel(ChannelItem channel, List<EpgRepository.EpgProgram> items, String title, String subtitle) {
        return EpgGuideUiFactory.buildMiniGuide(channel, items, title, subtitle, buildEpgGuideUiHost(null));
    }

    private EpgGuideUiFactory.Host buildEpgGuideUiHost(Dialog[] dialogHolder) {
        return new EpgGuideUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String displayName(ChannelItem item) {
                return MainActivity.this.displayName(item);
            }

            @Override
            public String shortTime(String isoTime) {
                return MainActivity.this.shortTime(isoTime);
            }

            @Override
            public String guideMeta(EpgRepository.EpgProgram program) {
                return buildGuideMeta(program);
            }

            @Override
            public void openProgramActions(ChannelItem channel, EpgRepository.EpgProgram program) {
                Dialog activeDialog = dialogHolder == null ? null : dialogHolder[0];
                if (activeDialog != null && activeDialog.isShowing()) {
                    activeDialog.dismiss();
                }
                postUiIfAlive(() -> channelActionsCoordinator.showProgramActionMenu(channel, program));
            }
        };
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
        ComposeView timelinePanelComposeView = new ComposeView(this);
        timelinePanelComposeView.setFocusable(true);
        timelinePanelComposeView.setFocusableInTouchMode(true);
        final android.app.Dialog[] timelineDialogRef = new android.app.Dialog[1];
        final Runnable focusInitialTimelineProgram = () -> {
            timelinePanelComposeView.requestFocus();
        };
        final java.util.function.Consumer<TimelineProgramDetailUiModel> renderTimelineProgramDetail = model ->
                TimelineGuidePanelComposeBinder.updateDetail(timelinePanelComposeView, model);
        TimelineProgramDetailUiModel initialTimelineDetail = new TimelineProgramDetailUiModel(
                getString(R.string.timeline_no_epg),
                getString(R.string.timeline_program_detail_hint),
                getString(R.string.timeline_program_desc_empty),
                "",
                "",
                getString(R.string.timeline_program_action_hint)
        );

        long windowEndMs = windowStartMs + TIMELINE_WINDOW_MS;
        final long nowMs = System.currentTimeMillis();
        final boolean rememberFocusedCenter = lastTimelineFocusedCenterMinute >= 0 && lastTimelineWindowStartMs == windowStartMs;
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE d MMM", Locale.getDefault());
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        TimelineHeaderUiModel timelineHeaderModel = TimelineGuideUiFactory.buildHeader(windowStartMs, windowEndMs, dayFormat, hourFormat, focusInitialTimelineProgram, new TimelineGuideUiFactory.HeaderHost() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String text(int resId, Object... args) {
                return getString(resId, args);
            }

            @Override
            public void now() {
                if (timelineDialogRef[0] != null) {
                    timelineDialogRef[0].dismiss();
                }
                openTimelineGuideNow();
            }

            @Override
            public void previous() {
                if (timelineDialogRef[0] != null) {
                    timelineDialogRef[0].dismiss();
                }
                int anchorIndex = overlayNavigationState.selectedOverlayIndex >= 0 && overlayNavigationState.selectedOverlayIndex < channels.size() ? overlayNavigationState.selectedOverlayIndex : Math.max(0, overlayNavigationState.currentIndex);
                openTimelineGuide(anchorIndex, Math.max(0L, windowStartMs - TIMELINE_SHIFT_MS));
            }

            @Override
            public void nextChannel() {
                openTimelineGuideNextForAnchor();
            }

            @Override
            public void next() {
                if (timelineDialogRef[0] != null) {
                    timelineDialogRef[0].dismiss();
                }
                int anchorIndex = overlayNavigationState.selectedOverlayIndex >= 0 && overlayNavigationState.selectedOverlayIndex < channels.size() ? overlayNavigationState.selectedOverlayIndex : Math.max(0, overlayNavigationState.currentIndex);
                openTimelineGuide(anchorIndex, windowStartMs + TIMELINE_SHIFT_MS);
            }

            @Override
            public void close() {
                if (timelineDialogRef[0] != null) {
                    timelineDialogRef[0].dismiss();
                }
            }
        });

        android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int dialogWidthPx = (int) (displayMetrics.widthPixels * 0.98f);
        int horizontalChrome = dp(20);
        int labelWidth = Math.max(dp(108), Math.min(dp(132), (int) (dialogWidthPx * 0.18f)));
        int stripWidth = Math.max(dp(540), dialogWidthPx - labelWidth - horizontalChrome);
        int totalWindowMinutes = (int) (TIMELINE_WINDOW_MS / 60000L);
        float minuteWidth = stripWidth / (float) totalWindowMinutes;
        TimelineScaleUiModel timelineScaleModel = TimelineGuideUiFactory.buildScale(windowStartMs, labelWidth, stripWidth, TIMELINE_WINDOW_MS, hourFormat);
        TimelineGuideRowsUiModel timelineRowsModel = buildTimelineGuideRowsUiModel(
                rows,
                windowStartMs,
                windowEndMs,
                minuteWidth,
                labelWidth,
                stripWidth,
                scheduledItems,
                anchorChannelId,
                rememberFocusedCenter,
                renderTimelineProgramDetail
        );
        attachDialogViewTreeOwners(timelinePanelComposeView);
        TimelineGuidePanelComposeBinder.bind(
                timelinePanelComposeView,
                new TimelineGuidePanelUiModel(timelineHeaderModel, timelineScaleModel, timelineRowsModel, initialTimelineDetail),
                (imageView, row) -> bindChannelLogo(imageView, row.logoUrl, row.channelName, 26, 26),
                (imageView, item) -> {
                    if (imageView == null || item == null || item.imageUrl == null || item.imageUrl.trim().isEmpty()) {
                        if (imageView != null) {
                            Glide.with(this).clear(imageView);
                            imageView.setImageDrawable(null);
                        }
                        return;
                    }
                    Glide.with(this).load(item.imageUrl.trim()).fitCenter()
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .override(dp(320), dp(200)).into(imageView);
                }
        );
        Dialog timelineDialog = ComposeDialogHost.showFullscreen(this, timelinePanelComposeView, () -> {
            TimelineGuidePanelComposeBinder.clear(timelinePanelComposeView);
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
            handleModalDismissed();
        });
        timelineDialogRef[0] = timelineDialog;
        activeTimelineDialog = timelineDialog;
        timelineDialog.setCancelable(true);
        handleModalShown();
        timelinePanelComposeView.requestFocus();
    }

    private void showRecordingsDialog(RecordingsRepository.RecordingsResult result) {
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        actions.add(new TvMessageActionUiModel(getString(R.string.dialog_close), false, null));
        showTvMessagePanel(
                getString(R.string.title_recordings_visual),
                getString(R.string.status_recordings_unavailable_offline),
                actions,
                null
        );
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
        showQuickChannelListDialog(title, items, emptyMessage, action, null);
    }

    private void showQuickChannelListDialog(String title, List<ChannelItem> items, String emptyMessage, QuickChannelSelectionAction action, Runnable onBack) {
        showQuickChannelListDialog(title, null, items, emptyMessage, action, onBack, null);
    }

    private void showQuickChannelListDialog(String title, String subtitle, List<ChannelItem> items, String emptyMessage, QuickChannelSelectionAction action, Runnable onBack, List<ZapActionItem> panelActions) {
        QuickChannelDialogUiFactory.Host dialogHost = buildQuickChannelDialogUiHost();
        if (!QuickChannelDialogUiFactory.hasItems(items)) {
            showStatus(QuickChannelDialogUiFactory.emptyMessage(emptyMessage, dialogHost));
            return;
        }
        prepareModalSurface();
        if (touchControlsBar != null) {
            touchControlsBar.setVisibility(View.GONE);
        }
        prefetchChannelLogos(items, SEARCH_LOGO_PREFETCH_LIMIT, 42, 42);
        ComposeView quickChannelListComposeView = new ComposeView(this);
        attachDialogViewTreeOwners(quickChannelListComposeView);
        final Dialog[] dialogHolder = new Dialog[1];
        final boolean[] navigationHandled = {false};
        Runnable panelBackAction = onBack == null ? null : () -> {
            navigationHandled[0] = true;
            dismissModalForNextAction(dialogHolder[0], onBack);
        };
        QuickChannelListComposeBinder.bind(quickChannelListComposeView, buildQuickChannelListUiModel(
                title,
                subtitle == null ? QuickChannelDialogUiFactory.subtitle(items, dialogHost) : subtitle,
                panelActions,
                items,
                dialogHolder,
                action,
                panelBackAction
        ), (imageView, item) -> {
            if (imageView == null || item == null) {
                return;
            }
            if (item.vod) {
                bindVodPosterList(imageView, item.logoUrl);
            } else {
                bindChannelLogo(imageView, item.logoUrl, item.channelName, 42, 42);
            }
        });
        Dialog dialog = ComposeDialogHost.showFullscreen(this, quickChannelListComposeView, () -> {
            if (onBack != null) {
                modalReturnAction = onBack;
            }
        }, this::handleModalDismissed);
        dialogHolder[0] = dialog;
        if (onBack != null) {
            dialog.setOnKeyListener((ignored, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP && !navigationHandled[0]) {
                    panelBackAction.run();
                    return true;
                }
                return false;
            });
        }
        handleModalShown();
    }

    private QuickChannelDialogUiFactory.Host buildQuickChannelDialogUiHost() {
        return new QuickChannelDialogUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String text(int resId, Object... args) {
                return getString(resId, args);
            }
        };
    }

    private QuickChannelListUiModel buildQuickChannelListUiModel(List<ChannelItem> items, Dialog[] dialogHolder, QuickChannelSelectionAction action) {
        return buildQuickChannelListUiModel(null, null, null, items, dialogHolder, action, null);
    }

    private QuickChannelListUiModel buildQuickChannelListUiModel(String title, String subtitle, List<ChannelItem> items, Dialog[] dialogHolder, QuickChannelSelectionAction action) {
        return buildQuickChannelListUiModel(title, subtitle, null, items, dialogHolder, action, null);
    }

    private QuickChannelListUiModel buildQuickChannelListUiModel(String title, String subtitle, List<ZapActionItem> panelActions, List<ChannelItem> items, Dialog[] dialogHolder, QuickChannelSelectionAction action) {
        return buildQuickChannelListUiModel(title, subtitle, panelActions, items, dialogHolder, action, null);
    }

    private QuickChannelListUiModel buildQuickChannelListUiModel(String title, String subtitle, List<ZapActionItem> panelActions, List<ChannelItem> items, Dialog[] dialogHolder, QuickChannelSelectionAction action, Runnable onBack) {
        return QuickChannelListUiFactory.build(title, subtitle, panelActions, items, onBack, new QuickChannelListUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String displayName(ChannelItem item) {
                return MainActivity.this.displayName(item);
            }

            @Override
            public String vodMeta(ChannelItem item) {
                return buildVodRowMeta(item);
            }

            @Override
            public String membershipLabel(ChannelItem item, int maxLabels) {
                return buildChannelMembershipLabel(item, maxLabels);
            }

            @Override
            public String protectedTitle(ChannelItem item, String rowTitle) {
                return decorateProtectedItemTitle(item, rowTitle);
            }

            @Override
            public String protectedMeta(ChannelItem item, String meta) {
                return decorateProtectedMeta(item, meta);
            }

            @Override
            public String protectedTypeBadge(ChannelItem item, String fallback) {
                return buildProtectedTypeBadge(item, fallback);
            }

            @Override
            public void choose(ChannelItem item) {
                Dialog activeDialog = dialogHolder == null ? null : dialogHolder[0];
                if (action != null) {
                    dismissModalForNextAction(activeDialog, () -> action.onChannelChosen(item));
                } else if (activeDialog != null && activeDialog.isShowing()) {
                    activeDialog.dismiss();
                }
            }
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
            if (match != null && !shouldHideProtectedItem(match)) {
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
            if (match != null && !shouldHideProtectedItem(match) && favoriteChannelIds.contains(favoriteId) && addedIds.add(favoriteId)) {
                favorites.add(match);
            }
        }
        for (ChannelItem item : allChannels) {
            if (item != null && item.id != null && !shouldHideProtectedItem(item) && favoriteChannelIds.contains(item.id) && addedIds.add(item.id)) {
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
        String currentId = (overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size()) ? channels.get(overlayNavigationState.currentIndex).id : lastChannelId;
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
        Log.d(TAG, "tuneChannelById channelId=" + channelId + " currentFilter=" + overlayNavigationState.selectedFilterKey + " favoritesOnly=" + overlayNavigationState.favoritesOnly);
        if (channelId == null || channelId.trim().isEmpty()) {
            return;
        }
        ChannelItem directItem = findChannelItemById(channelId);
        if (directItem != null && isProtectedItem(directItem) && isProtectedContentLocked()) {
            ensureParentalAccessForItem(directItem, () -> tuneChannelById(channelId));
            return;
        }
        int index = findChannelIndexById(channelId);
        Log.d(TAG, "tuneChannelById initialIndex=" + index + " visibleSize=" + channels.size() + " allSize=" + allChannels.size());
        if (index < 0) {
            syncOverlayCoordinator();
            channelOverlayCoordinator.setSearchQuery("");
            if (directItem == null || !channelOverlayCoordinator.selectNaturalFilterForChannel(directItem)) {
                channelOverlayCoordinator.setSelectedFilterKey("all");
            }
            channelOverlayCoordinator.setFavoritesOnly(false);
            channelOverlayCoordinator.refreshVisibleChannels(channelId, channelId);
            syncOverlayStateFromCoordinator();
            clearOverlaySearchQuery();
            refreshOverlayChannelList();
            updateFilterText();
            updateOverlaySearchState();
            index = findChannelIndexById(channelId);
            Log.d(TAG, "tuneChannelById afterRefresh index=" + index + " visibleSize=" + channels.size() + " selectedFilter=" + overlayNavigationState.selectedFilterKey + " favoritesOnly=" + overlayNavigationState.favoritesOnly);
        }
        if (index >= 0) {
            Log.d(TAG, "tuneChannelById finalIndex=" + index + " -> tuneToIndex");
            tuneToIndex(index, true);
        } else {
            if (directItem != null) {
                Log.d(TAG, "tuneChannelById directFallback id=" + directItem.id + " name=" + directItem.name);
                overlayNavigationState.currentIndex = -1;
                overlayNavigationState.selectedOverlayIndex = -1;
                refreshOverlayChannelList();
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
        PlaybackDiagnosticsPanelUiModel model = PlaybackDiagnosticsUiFactory.buildCurrent(
                diagnostics,
                currentChannel,
                storedError,
                buildPlaybackDiagnosticsUiHost()
        );
        showStructuredStatusPanel(model.title, model.subtitle, model.summary, model.rows, model.notes, model.actions);
    }

    private String routeDiagnosticTone(String routeLabel) {
        String normalized = safeLower(routeLabel);
        if (normalized.contains("directo")) {
            return "ok";
        }
        if (normalized.contains("proxy") || normalized.contains("compat")) {
            return "warn";
        }
        return "";
    }

    private void showPlaybackDiagnosticsActionsDialog(ChannelItem channelItem) {
        if (channelItem == null) {
            return;
        }
        TvOptionsMenuModel model = PlaybackDiagnosticsUiFactory.buildActionsMenu(channelItem, buildPlaybackDiagnosticsUiHost());
        showTvOptionsDialog(R.string.title_playback_diagnostics, model.message, model.options, model.actions);
    }

    private void showAudioTrackDialog() {
        if (playerController == null) {
            showStatus(getString(R.string.audio_track_unavailable));
            return;
        }
        List<PlayerController.AudioTrackOption> tracks = playerController.getAudioTrackOptions();
        if (tracks.isEmpty()) {
            showStatus(getString(R.string.audio_track_unavailable));
            return;
        }
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.audio_track_auto));
        actions.add(() -> {
            if (playerController != null) {
                playerController.clearAudioTrackOverride();
                showStatus(getString(R.string.audio_track_auto_selected));
            }
        });
        for (PlayerController.AudioTrackOption track : tracks) {
            String label = track.label == null || track.label.trim().isEmpty()
                    ? getString(R.string.audio_track_fallback, options.size())
                    : track.label.trim();
            if (!track.supported) {
                label = getString(R.string.audio_track_unsupported, label);
            } else if (track.selected) {
                label = getString(R.string.audio_track_selected, label);
            }
            options.add(label);
            actions.add(() -> {
                if (playerController == null) {
                    showStatus(getString(R.string.audio_track_unavailable));
                    return;
                }
                if (playerController.selectAudioTrack(track)) {
                    showStatus(getString(R.string.audio_track_changed, track.label));
                } else {
                    showStatus(getString(R.string.audio_track_unavailable));
                }
            });
        }
        showTvOptionsDialog(R.string.audio_track_title, null, options, actions);
    }

    private void showTextTrackDialog(Runnable onBack) {
        if (playerController == null) {
            showStatus(getString(R.string.subtitle_track_unavailable));
            return;
        }
        List<PlayerController.TextTrackOption> tracks = playerController.getTextTrackOptions();
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(getString(R.string.subtitle_track_off));
        actions.add(() -> {
            if (playerController != null) {
                playerController.setTextTracksEnabled(false);
                showStatus(getString(R.string.subtitle_track_off_selected));
            }
        });
        options.add(getString(R.string.subtitle_track_auto));
        actions.add(() -> {
            if (playerController != null) {
                playerController.setTextTracksEnabled(true);
                showStatus(getString(R.string.subtitle_track_auto_selected));
            }
        });
        for (PlayerController.TextTrackOption track : tracks) {
            String label = track.label;
            if (!track.supported) {
                label = getString(R.string.subtitle_track_unsupported, label);
            } else if (track.selected) {
                label = getString(R.string.subtitle_track_selected, label);
            }
            options.add(label);
            actions.add(() -> {
                if (playerController != null && playerController.selectTextTrack(track)) {
                    showStatus(getString(R.string.subtitle_track_changed, track.label));
                } else {
                    showStatus(getString(R.string.subtitle_track_unavailable));
                }
            });
        }
        showTvOptionsDialog(R.string.subtitle_track_title, tracks.isEmpty() ? getString(R.string.subtitle_track_unavailable) : null, options, actions, onBack);
    }

    private void testPlaybackModeNow(ChannelItem channelItem, String playbackMode) {
        if (channelItem == null) {
            return;
        }
        if (PlaybackModeStore.MODE_AUTO.equals(playbackMode)) {
            playbackRecoveryCoordinator.clearTemporaryMode(channelItem.id);
        } else {
            playbackRecoveryCoordinator.setTemporaryMode(channelItem.id, playbackMode);
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
            playbackRecoveryCoordinator.clearTemporaryMode(channelItem.id);
        } else {
            playbackRecoveryCoordinator.setTemporaryMode(channelItem.id, nextMode);
        }
        showStatus(getString(R.string.status_playback_repair_trying, formatPlaybackModeLabel(nextMode)));
        scheduleLearnCurrentPlaybackRoute(channelItem.id, nextMode);
        retryCurrentPlayback();
    }

    private String nextPlaybackMode(String currentMode) {
        return playbackRecoveryCoordinator.nextMode(currentMode);
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
        PlaybackDiagnosticsPanelUiModel model = PlaybackDiagnosticsUiFactory.buildHistory(records, buildPlaybackDiagnosticsUiHost());
        showStructuredStatusPanel(model.title, model.subtitle, model.summary, model.rows, model.notes, model.actions);
    }

    private PlaybackDiagnosticsUiFactory.Host buildPlaybackDiagnosticsUiHost() {
        return new PlaybackDiagnosticsUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String text(int resId, Object... args) {
                return getString(resId, args);
            }

            @Override
            public String displayName(ChannelItem item) {
                return MainActivity.this.displayName(item);
            }

            @Override
            public String safeText(String value) {
                return MainActivity.this.safeText(value);
            }

            @Override
            public String safeDiagnosticUrl(String value) {
                return MainActivity.this.safeText(DiagnosticRedactor.sanitizeUrl(value));
            }

            @Override
            public String fallbackUnknown(String value) {
                return MainActivity.this.fallbackUnknown(value);
            }

            @Override
            public String routeTone(String routeLabel) {
                return routeDiagnosticTone(routeLabel);
            }

            @Override
            public String playbackQuality(PlayerController.PlaybackDiagnostics diagnostics) {
                return formatPlaybackQuality(diagnostics);
            }

            @Override
            public String playbackModeLabel(String playbackMode) {
                return formatPlaybackModeLabel(playbackMode);
            }

            @Override
            public String classifyError(String error) {
                return classifyOperationalError(error);
            }

            @Override
            public String recommendation(PlayerController.PlaybackDiagnostics diagnostics, PlaybackDiagnosticsStore.ErrorRecord storedError) {
                return buildPlaybackDiagnosticsRecommendation(diagnostics, storedError);
            }

            @Override
            public String recentSummary() {
                return buildRecentDiagnosticsSummary();
            }

            @Override
            public String historyItem(PlaybackDiagnosticsStore.ErrorRecord record) {
                return formatPlaybackDiagnosticsHistoryItem(record);
            }

            @Override
            public boolean hasTemporaryMode(ChannelItem item) {
                return item != null && temporaryPlaybackModesByChannelId.containsKey(item.id);
            }

            @Override
            public String temporaryMode(ChannelItem item) {
                return item == null ? "" : temporaryPlaybackModesByChannelId.get(item.id);
            }

            @Override
            public boolean hasLearnedMode(ChannelItem item) {
                return item != null && learnedPlaybackModesByChannelId.containsKey(item.id);
            }

            @Override
            public String learnedMode(ChannelItem item) {
                return item == null ? "" : learnedPlaybackModesByChannelId.get(item.id);
            }

            @Override
            public void retryCurrentPlayback() {
                MainActivity.this.retryCurrentPlayback();
            }

            @Override
            public void retryWithNextRoute(ChannelItem item) {
                retryCurrentPlaybackWithNextRoute(item);
            }

            @Override
            public void testMode(ChannelItem item, String mode) {
                testPlaybackModeNow(item, mode);
            }

            @Override
            public void showAudioTracks() {
                showAudioTrackDialog();
            }

            @Override
            public void showTemporaryMode(ChannelItem item) {
                showTemporaryPlaybackModeDialog(item);
            }

            @Override
            public void showPermanentMode(ChannelItem item) {
                showPlaybackModeDialog(item);
            }

            @Override
            public void saveLearned(ChannelItem item) {
                saveCurrentRouteAsLearned(item);
            }

            @Override
            public void clearLearned(ChannelItem item) {
                clearLearnedPlaybackMode(item);
            }

            @Override
            public void showActions(ChannelItem item) {
                showPlaybackDiagnosticsActionsDialog(item);
            }

            @Override
            public void showHistory() {
                showPlaybackDiagnosticsHistoryDialog();
            }

            @Override
            public void clearError(ChannelItem item) {
                clearPlaybackDiagnosticsError(item);
            }

            @Override
            public void clearHistory() {
                if (playbackDiagnosticsStore != null) {
                    playbackDiagnosticsStore.clearAll();
                }
                if (playerController != null) {
                    playerController.clearLastError();
                }
                showStatus(getString(R.string.status_diagnostics_history_cleared));
            }
        };
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
        if (!BuildConfig.STANDALONE_MODE && isProxyManifestProfile(channelItem) && PlaybackModeStore.MODE_DIRECT.equals(mode)) {
            mode = PlaybackModeStore.MODE_PROXY;
        }
        setLearnedPlaybackMode(channelItem.id, mode, true);
    }

    private void clearLearnedPlaybackMode(ChannelItem channelItem) {
        if (channelItem == null || channelItem.id == null) {
            return;
        }
        if (playbackRecoveryCoordinator.clearLearnedMode(channelItem.id)) {
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
        sendPlaybackHeartbeat("error");
        if (BuildConfig.STANDALONE_MODE && !request.directPlayback) {
            String detail = diagnostics.lastError == null || diagnostics.lastError.trim().isEmpty()
                    ? getString(R.string.error_unknown_reason)
                    : diagnostics.lastError.trim();
            markPostUpdateHealthFailed(getString(R.string.app_update_health_playback_failed, request.channelName, detail), true);
            maybeRefreshOfflineCatalogAfterPlaybackError(detail);
        }
        ChannelItem item = findChannelItemById(request.channelId);
        if (item != null && item.isVod) {
            stopVodLoadingOverlay(request.channelId);
            rememberCurrentVodPosition();
            showStatus(getString(R.string.vod_status_failed));
            postUiDelayedIfAlive(() -> {
                ChannelItem current = getCurrentPlaybackChannelItem();
                if (current != null && request.channelId.equals(current.id)) {
                    showVodPlaybackRecoveryPanel(current, diagnostics);
                }
            }, 500L);
            return;
        }
        maybeRepairPlaybackAfterError(request);
    }

    private void maybeRefreshOfflineCatalogAfterPlaybackError(String detail) {
        if (!BuildConfig.STANDALONE_MODE || catalogSnapshotStore == null || catalogRepository == null || offlineCatalogRefreshRunning) {
            return;
        }
        String normalized = safeLower(detail);
        boolean looksCatalogRelated = normalized.contains("response code: 401")
                || normalized.contains("response code: 403")
                || normalized.contains("response code: 404")
                || normalized.contains("http 401")
                || normalized.contains("http 403")
                || normalized.contains("http 404")
                || normalized.contains("invalidresponsecode")
                || normalized.contains("source error")
                || normalized.contains("token")
                || normalized.contains("unauthorized")
                || normalized.contains("forbidden")
                || normalized.contains("not found");
        if (!looksCatalogRelated) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastOfflinePlaybackRecoveryRefreshMs < 10L * 60L * 1000L) {
            return;
        }
        CatalogSnapshotStore.SnapshotStatus status = catalogSnapshotStore.getStatus(BuildConfig.CATALOG_SNAPSHOT_URL);
        if (!status.hasAccessToken || status.sourceUrl == null || status.sourceUrl.trim().isEmpty()) {
            return;
        }
        lastOfflinePlaybackRecoveryRefreshMs = now;
        showStatus(getString(R.string.offline_catalog_status_refreshing));
        refreshOfflineCatalog(false, true, true);
    }

    private void showVodPlaybackRecoveryPanel(ChannelItem channel, PlayerController.PlaybackDiagnostics diagnostics) {
        if (channel == null) {
            return;
        }
        String error = diagnostics == null || diagnostics.lastError == null || diagnostics.lastError.trim().isEmpty()
                ? getString(R.string.error_unknown_reason)
                : diagnostics.lastError.trim();
        TvMessagePanelUiModel model = VodPlaybackRecoveryUiFactory.build(channel, error, new VodPlaybackRecoveryUiFactory.Host() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String text(int resId, Object... args) {
                return getString(resId, args);
            }

            @Override
            public String displayName(ChannelItem item) {
                return MainActivity.this.displayName(item);
            }

            @Override
            public void retry(ChannelItem item) {
                playVodItem(item, true);
            }

            @Override
            public void retryRoute(ChannelItem item) {
                retryCurrentPlaybackWithNextRoute(item);
            }

            @Override
            public void diagnostics(ChannelItem item) {
                showVodDiagnosticsDialog(item);
            }

            @Override
            public void library() {
                showVodLibraryDialog();
            }
        });
        showTvMessagePanel(model.title, model.message, model.actions, null);
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
        if (!playbackRecoveryCoordinator.markAttempt(request.channelId, nextMode)) {
            return;
        }
        playbackRecoveryCoordinator.setTemporaryMode(request.channelId, nextMode);
        showStatus(getString(R.string.status_playback_repair_trying, formatPlaybackModeLabel(nextMode)));
        postUiDelayedIfAlive(() -> {
            ChannelItem current = getCurrentPlaybackChannelItem();
            if (current != null && request.channelId.equals(current.id)) {
                retryCurrentPlayback();
            }
        }, 700L);
    }

    private void handlePlaybackAutoRecoveryReady(PlayerController.PlaybackRequest request, PlayerController.PlaybackDiagnostics diagnostics, String reason) {
        if (!playbackRepairEnabled || request == null || request.channelId == null || request.channelId.trim().isEmpty() || request.directPlayback) {
            return;
        }
        String mode = diagnostics == null ? sanitizePlaybackMode(request.playbackMode) : inferPlaybackModeFromDiagnostics(diagnostics);
        if (PlaybackModeStore.MODE_AUTO.equals(mode)) {
            mode = sanitizePlaybackMode(request.playbackMode);
        }
        if (PlaybackModeStore.MODE_AUTO.equals(mode)) {
            return;
        }
        playbackRecoveryCoordinator.clearAttempts(request.channelId);
        String previous = playbackRecoveryCoordinator.learnedMode(request.channelId);
        boolean changed = !mode.equals(previous);
        setLearnedPlaybackMode(request.channelId, mode, false);
        if (changed) {
            String label = formatPlaybackModeLabel(mode);
            showStatus(getString(R.string.status_playback_auto_route_saved, label));
            String detail = getString(
                    R.string.status_playback_auto_route_detail,
                    request.channelName == null ? "" : request.channelName,
                    request.platformName == null ? "" : request.platformName,
                    label,
                    reason == null || reason.trim().isEmpty() ? getString(R.string.diagnostics_value_unknown) : reason.trim()
            );
            reportOfflineDeviceStatus(getString(R.string.status_playback_auto_route_event), true, 0L, detail);
            sendPlaybackHeartbeat("recovered");
        }
    }

    private void scheduleLearnCurrentPlaybackRoute(String channelId, String playbackMode) {
        String mode = sanitizePlaybackMode(playbackMode);
        if (!playbackRepairEnabled || PlaybackModeStore.MODE_AUTO.equals(mode) || channelId == null || channelId.trim().isEmpty()) {
            return;
        }
        postUiDelayedIfAlive(() -> learnPlaybackModeIfStillCurrent(channelId, mode), 18_000L);
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
        playbackRecoveryCoordinator.clearAttempts(channelId);
        setLearnedPlaybackMode(channelId, playbackMode, false);
    }

    private void setLearnedPlaybackMode(String channelId, String playbackMode, boolean announce) {
        String mode = sanitizePlaybackMode(playbackMode);
        if (channelId == null || channelId.trim().isEmpty() || PlaybackModeStore.MODE_AUTO.equals(mode)) {
            return;
        }
        ChannelItem channelItem = findChannelItemById(channelId);
        if (!BuildConfig.STANDALONE_MODE && isProxyManifestProfile(channelItem) && PlaybackModeStore.MODE_DIRECT.equals(mode)) {
            mode = PlaybackModeStore.MODE_PROXY;
        }
        playbackRecoveryCoordinator.setLearnedMode(channelId, mode);
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

    private String formatPlaybackQuality(PlayerController.PlaybackDiagnostics diagnostics) {
        if (diagnostics == null || !diagnostics.hasVideoQuality()) {
            return getString(R.string.diagnostics_value_unknown);
        }
        List<String> parts = new ArrayList<>();
        if (diagnostics.videoWidth > 0 && diagnostics.videoHeight > 0) {
            parts.add(diagnostics.videoWidth + "x" + diagnostics.videoHeight);
        }
        if (diagnostics.videoCodec != null && !diagnostics.videoCodec.trim().isEmpty()) {
            parts.add(diagnostics.videoCodec.trim());
        }
        if (diagnostics.videoFrameRate > 0f) {
            parts.add(String.format(Locale.getDefault(), "%.0f fps", diagnostics.videoFrameRate));
        }
        if (diagnostics.videoBitrate > 0) {
            float mbps = diagnostics.videoBitrate / 1_000_000f;
            parts.add(String.format(Locale.getDefault(), "%.1f Mbps", mbps));
        }
        if (diagnostics.audioCodec != null && !diagnostics.audioCodec.trim().isEmpty()) {
            parts.add("Audio " + diagnostics.audioCodec.trim());
        }
        return parts.isEmpty() ? getString(R.string.diagnostics_value_unknown) : joinLabels(parts);
    }

    private String formatPlaybackQualityCompact(PlayerController.PlaybackDiagnostics diagnostics) {
        if (diagnostics == null || !diagnostics.hasVideoQuality()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (diagnostics.videoHeight > 0) {
            if (diagnostics.videoWidth >= 3840 || diagnostics.videoHeight >= 2160) {
                parts.add("4K");
            } else {
                parts.add(diagnostics.videoHeight + "p");
            }
        } else if (diagnostics.videoWidth > 0) {
            parts.add(diagnostics.videoWidth + "px");
        }
        String codec = compactCodecLabel(diagnostics.videoCodec);
        if (!codec.isEmpty()) {
            parts.add(codec);
        }
        if (diagnostics.videoFrameRate > 0f) {
            parts.add(String.format(Locale.getDefault(), "%.0f fps", diagnostics.videoFrameRate));
        }
        if (diagnostics.videoBitrate > 0) {
            parts.add(String.format(Locale.getDefault(), "%.1f Mbps", diagnostics.videoBitrate / 1_000_000f));
        }
        return joinLabels(parts);
    }

    private String compactCodecLabel(String codec) {
        String value = codec == null ? "" : codec.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.isEmpty()) {
            return "";
        }
        if (lower.contains("avc") || lower.contains("h264") || lower.contains("avc1")) {
            return "H.264";
        }
        if (lower.contains("hevc") || lower.contains("h265") || lower.contains("hvc1") || lower.contains("hev1")) {
            return "H.265";
        }
        return value;
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
        ensureTouchControlsEpgPair(channelItem);
        logHudEpgState("showZapBanner", channelItem);
        zapBannerController.show(channelItem);
    }

    private void updateZapBannerContent(ChannelItem channelItem) {
        ensureTouchControlsEpgPair(channelItem);
        logHudEpgState("updateZapBanner", channelItem);
        zapBannerController.updateContent(channelItem);
    }

    private void logHudEpgState(String source, ChannelItem channelItem) {
        if (channelItem == null || channelItem.id == null) {
            return;
        }
        EpgRepository.EpgProgramPair pair = epgProgramPairByChannelId.get(channelItem.id);
        EpgRepository.EpgProgram current = pair == null ? null : pair.current;
        EpgRepository.EpgProgram next = pair == null ? null : pair.next;
        Log.w(TAG, "HUD EPG state source=" + source
                + " playbackId=" + (playerController == null ? "" : playerController.getCurrentRequestChannelId())
                + " channel=" + channelItem.id
                + " name=" + displayName(channelItem)
                + " current=" + (current == null ? "" : current.title)
                + " start=" + (current == null ? "" : current.startTime)
                + " end=" + (current == null ? "" : current.endTime)
                + " icon=" + (current != null && current.icon != null && !current.icon.trim().isEmpty())
                + " next=" + (next == null ? "" : next.title));
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
        String playbackChannelId = playerController == null ? "" : playerController.getCurrentRequestChannelId();
        if (!playbackChannelId.isEmpty()) {
            return playbackChannelId;
        }
        if (overlayNavigationState.currentIndex >= 0 && overlayNavigationState.currentIndex < channels.size()) {
            ChannelItem item = channels.get(overlayNavigationState.currentIndex);
            if (item != null && item.id != null && !item.id.trim().isEmpty()) {
                return item.id;
            }
        }
        return lastChannelId;
    }

    private void showLeaveRecordingPrompt() {
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        actions.add(new TvMessageActionUiModel("Salir", true, this::exitRecordingToPreviousChannel));
        actions.add(new TvMessageActionUiModel(getString(android.R.string.cancel), false, null));
        showTvMessagePanel(getString(R.string.title_recordings_visual), "¿Salir de la grabacion y volver al canal anterior?", actions, null);
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
        invalidateVodDerivedCaches();
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
            invalidateVodDerivedCaches();
            saveVodResumePositions();
        }
    }

    private void loadVodResumePositions() {
        vodResumePositions.clear();
        invalidateVodDerivedCaches();
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
        if (prefs == null) {
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
        if (prefs == null) {
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
        return RecordingsUiFactory.buildSummary(result, buildRecordingsPresentationHost());
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
        return RecordingsUiFactory.statusLabel(item, recordingsController.getCurrentResult(), buildRecordingsPresentationHost());
    }

    private int recordingStatusBadgeColor(RecordingsRepository.RecordingItem item) {
        return RecordingsUiFactory.statusBadgeColor(item, recordingsController.getCurrentResult(), buildRecordingsPresentationHost());
    }

    private int recordingMetaColor(RecordingsRepository.RecordingItem item) {
        return RecordingsUiFactory.metaColor(item, buildRecordingsPresentationHost());
    }

    private RecordingsUiFactory.PresentationHost buildRecordingsPresentationHost() {
        return new RecordingsUiFactory.PresentationHost() {
            @Override
            public String text(int resId) {
                return getString(resId);
            }

            @Override
            public String text(int resId, Object... args) {
                return getString(resId, args);
            }

            @Override
            public String safeLower(String value) {
                return MainActivity.this.safeLower(value);
            }

            @Override
            public String filterLabel() {
                return buildRecordingsFilterLabel();
            }

            @Override
            public boolean hasConflict(RecordingsRepository.RecordingItem item, RecordingsRepository.RecordingsResult result) {
                return MainActivity.this.hasRecordingConflict(item, result);
            }
        };
    }

    private void prefetchChannelLogos(List<ChannelItem> items, int maxItems, int widthDp, int heightDp) {
        if (items == null || items.isEmpty()) {
            return;
        }
        int effectiveMaxItems = devicePerformanceProfile != null && devicePerformanceProfile.lowRam
                ? Math.min(maxItems, 12)
                : maxItems;
        int count = 0;
        for (ChannelItem item : items) {
            if (item == null || item.logoUrl == null || item.logoUrl.trim().isEmpty()) {
                continue;
            }
            String trimmedLogoUrl = normalizeChannelLogoUrl(item.logoUrl);
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
            if (count >= effectiveMaxItems) {
                break;
            }
        }
    }

    private void prefetchCurrentChannelLogos() {
        if (channels.isEmpty()) {
            return;
        }
        List<ChannelItem> warmList = new ArrayList<>();
        int start = overlayNavigationState.currentIndex >= 0 ? overlayNavigationState.currentIndex : 0;
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
        String trimmedLogoUrl = normalizeChannelLogoUrl(logoUrl);
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
                .override(Math.max(1, dp(widthDp)), Math.max(1, dp(heightDp)))
                .placeholder(fallback)
                .error(fallback)
                .into(imageView);
    }

    private String normalizeChannelLogoUrl(String logoUrl) {
        String trimmed = logoUrl == null ? "" : logoUrl.trim();
        if (trimmed.startsWith("http://www.movistarplus.es/")) {
            return "https://www.movistarplus.es/" + trimmed.substring("http://www.movistarplus.es/".length());
        }
        return trimmed;
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
            postUiIfAlive(() -> {
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
            if (isAsciiDigits(token)) {
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
        if (out.length() == 1 && tokens.size() > 1 && !isAsciiDigits(tokens.get(1))) {
            out.append(tokens.get(1).charAt(0));
        }
        return out.length() > 3 ? out.substring(0, 3) : out.toString();
    }

    private static boolean isAsciiDigits(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character < '0' || character > '9') {
                return false;
            }
        }
        return true;
    }

    private void bindProgramPoster(ImageView imageView, String posterUrl) {
        bindPoster(imageView, posterUrl, true);
    }

    private void bindRecordingPoster(ImageView imageView, String posterUrl) {
        bindPoster(imageView, posterUrl, false, 360, 540);
    }

    private void bindVodPosterThumbnail(ImageView imageView, String posterUrl) {
        bindPoster(imageView, posterUrl, false, 180, 270);
    }

    private void bindVodPosterList(ImageView imageView, String posterUrl) {
        bindPoster(imageView, posterUrl, false, 120, 180);
    }

    private void bindPoster(ImageView imageView, String posterUrl, boolean fitInside) {
        bindPoster(imageView, posterUrl, fitInside, 360, 540);
    }

    private void bindPoster(ImageView imageView, String posterUrl, boolean fitInside, int widthDp, int heightDp) {
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
                    .override(dp(widthDp), dp(heightDp))
                    .into(imageView);
            return;
        }
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(imageView.getContext())
                .load(trimmedPosterUrl)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .override(dp(widthDp), dp(heightDp))
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

    private String buildTimelineProgramTitle(EpgRepository.EpgProgram program) {
        if (program == null || program.title == null || program.title.trim().isEmpty()) {
            return getString(R.string.label_program_default);
        }
        return program.title.trim();
    }

    private String buildTimelineProgramBlockTitle(EpgRepository.EpgProgram program, boolean scheduled) {
        String title = buildTimelineProgramTitle(program);
        return scheduled ? getString(R.string.timeline_program_scheduled_prefix) + " " + title : title;
    }

    private String buildTimelineProgramTimeLabel(EpgRepository.EpgProgram program) {
        if (program == null) {
            return "";
        }
        return shortTime(program.startTime) + " - " + shortTime(program.endTime);
    }

    private String buildTimelineProgramMeta(ChannelItem channel, EpgRepository.EpgProgram program, boolean live, boolean scheduled) {
        String channelName = channel == null || channel.name == null ? "" : channel.name;
        String meta = channelName + "  ·  " + buildTimelineProgramTimeLabel(program);
        if (live) {
            meta = meta + "  ·  " + getString(R.string.guide_program_now);
        }
        if (scheduled) {
            meta = meta + "  ·  " + getString(R.string.timeline_program_scheduled_short);
        }
        return meta;
    }

    private String buildTimelineProgramDescription(EpgRepository.EpgProgram program) {
        if (program == null || program.description == null || program.description.trim().isEmpty()) {
            return getString(R.string.timeline_program_desc_empty);
        }
        return program.description.trim();
    }

    private String buildTimelineProgramPosterUrl(ChannelItem channel, EpgRepository.EpgProgram program) {
        return ProgramArtworkResolver.resolve(program, channel);
    }

    private TimelineProgramDetailUiModel buildTimelineProgramDetailModel(ChannelItem channel, EpgRepository.EpgProgram program, boolean live, boolean scheduled) {
        return new TimelineProgramDetailUiModel(
                buildTimelineProgramTitle(program),
                buildTimelineProgramMeta(channel, program, live, scheduled),
                buildTimelineProgramDescription(program),
                buildTimelineProgramPosterUrl(channel, program),
                scheduled ? getString(R.string.timeline_program_scheduled_short) : live ? getString(R.string.guide_program_now) : "",
                getString(R.string.timeline_program_action_hint)
        );
    }

    private List<TimelineVisibleBlock> buildTimelineVisibleBlocks(
            TimelineChannelPrograms row,
            long windowStartMs,
            long windowEndMs,
            float minuteWidth,
            List<RecordingsRepository.RecordingItem> scheduledItems
    ) {
        List<TimelineVisibleBlock> blocks = new ArrayList<>();
        if (row == null || row.programs == null || row.programs.isEmpty()) {
            return blocks;
        }
        int usedWidth = 0;
        for (EpgRepository.EpgProgram program : row.programs) {
            if (program == null) {
                continue;
            }
            long startMs = parseIsoMillis(program.startTime);
            long endMs = parseIsoMillis(program.endTime);
            if (endMs <= windowStartMs || startMs >= windowEndMs || endMs <= startMs) {
                continue;
            }
            int visibleStartMinutes = (int) Math.max(0L, (Math.max(windowStartMs, startMs) - windowStartMs) / 60000L);
            int visibleEndMinutes = (int) Math.max(visibleStartMinutes + 1L, (Math.min(windowEndMs, endMs) - windowStartMs) / 60000L);
            int targetOffsetWidth = Math.round(visibleStartMinutes * minuteWidth);
            int spacerWidth = Math.max(0, targetOffsetWidth - usedWidth);
            int durationMinutes = Math.max(15, visibleEndMinutes - visibleStartMinutes);
            int blockWidth = Math.max(dp(72), Math.round(durationMinutes * minuteWidth));
            int centerMinute = visibleStartMinutes + (durationMinutes / 2);
            boolean scheduled = isProgramScheduled(row.channel, program, scheduledItems);
            boolean live = program.progress >= 0;
            boolean activeNow = System.currentTimeMillis() >= startMs && System.currentTimeMillis() < endMs;
            blocks.add(new TimelineVisibleBlock(program, scheduled, live, activeNow, spacerWidth, blockWidth, centerMinute));
            usedWidth = targetOffsetWidth + blockWidth + dp(2);
        }
        return blocks;
    }

    private TimelineGuideRowsUiModel buildTimelineGuideRowsUiModel(
            List<TimelineChannelPrograms> rows,
            long windowStartMs,
            long windowEndMs,
            float minuteWidth,
            int labelWidth,
            int stripWidth,
            List<RecordingsRepository.RecordingItem> scheduledItems,
            String anchorChannelId,
            boolean rememberFocusedCenter,
            java.util.function.Consumer<TimelineProgramDetailUiModel> renderTimelineProgramDetail
    ) {
        return TimelineGuideUiFactory.buildRows(
                rows,
                windowStartMs,
                labelWidth,
                stripWidth,
                anchorChannelId,
                rememberFocusedCenter,
                lastTimelineFocusedCenterMinute,
                renderTimelineProgramDetail,
                new TimelineGuideUiFactory.Host() {
                    @Override
                    public String text(int resId) {
                        return getString(resId);
                    }

                    @Override
                    public List<TimelineVisibleBlock> visibleBlocks(TimelineChannelPrograms row) {
                        return buildTimelineVisibleBlocks(row, windowStartMs, windowEndMs, minuteWidth, scheduledItems);
                    }

                    @Override
                    public String programBlockTitle(EpgRepository.EpgProgram program, boolean scheduled) {
                        return buildTimelineProgramBlockTitle(program, scheduled);
                    }

                    @Override
                    public String programTimeLabel(EpgRepository.EpgProgram program) {
                        return buildTimelineProgramTimeLabel(program);
                    }

                    @Override
                    public TimelineProgramDetailUiModel programDetail(ChannelItem channel, EpgRepository.EpgProgram program, boolean live, boolean scheduled) {
                        return buildTimelineProgramDetailModel(channel, program, live, scheduled);
                    }

                    @Override
                    public void focusEmpty(ChannelItem channel, long focusedWindowStartMs, java.util.function.Consumer<TimelineProgramDetailUiModel> renderDetail) {
                        activeTimelineAnchorChannelId = channel.id;
                        activeTimelineWindowStartMs = focusedWindowStartMs;
                        activeTimelineFocusedCenterMinute = -1;
                        lastTimelineFocusedCenterMinute = -1;
                        renderDetail.accept(new TimelineProgramDetailUiModel(
                                channel.name,
                                getString(R.string.timeline_no_epg),
                                getString(R.string.timeline_program_desc_empty),
                                "",
                                "",
                                getString(R.string.timeline_program_action_hint)
                        ));
                    }

                    @Override
                    public void focusProgram(ChannelItem channel, long focusedWindowStartMs, int centerMinute, TimelineProgramDetailUiModel detail, java.util.function.Consumer<TimelineProgramDetailUiModel> renderDetail) {
                        activeTimelineAnchorChannelId = channel.id;
                        activeTimelineWindowStartMs = focusedWindowStartMs;
                        activeTimelineFocusedCenterMinute = centerMinute;
                        lastTimelineFocusedCenterMinute = centerMinute;
                        renderDetail.accept(detail);
                    }

                    @Override
                    public void openProgramActions(ChannelItem channel, EpgRepository.EpgProgram program) {
                        Log.i(TAG, "timeline program click channel=" + channel.id
                                + " program=" + (program.title == null ? "" : program.title));
                        channelActionsCoordinator.showProgramActionMenu(channel, program);
                    }

                    @Override
                    public void toggleRecording(ChannelItem channel, EpgRepository.EpgProgram program, boolean scheduled) {
                        Log.i(TAG, "timeline direct recording action channel=" + channel.id
                                + " scheduled=" + scheduled
                                + " program=" + (program.title == null ? "" : program.title));
                        if (scheduled) {
                            cancelScheduledProgram(channel, program);
                        } else {
                            scheduleProgram(channel, program);
                        }
                    }
                }
        );
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

}
