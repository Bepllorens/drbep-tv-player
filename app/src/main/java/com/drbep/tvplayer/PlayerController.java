package com.drbep.tvplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaDrmException;
import android.media.MediaDrm;
import android.media.DeniedByServerException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.decoder.CryptoConfig;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.drm.FrameworkCryptoConfig;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import androidx.media3.exoplayer.drm.DrmSession;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.drm.ExoMediaDrm;
import androidx.media3.exoplayer.drm.FrameworkMediaDrm;
import androidx.media3.exoplayer.drm.MediaDrmCallback;
import androidx.media3.exoplayer.drm.MediaDrmCallbackException;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PlayerController {
    private static final String TAG = "PlayerController";
    private static final String PREFS = "drbep_tv_prefs";
    private static final String CLEARKEY_DATA_URI_PREFIX = "data:application/json;base64,";
    private static final String SECURE_STREAM_LICENSE_PREFIX = "drbep-secure-stream:";
    private static final int PLAYBACK_CONNECT_TIMEOUT_MS = 20_000;
    private static final int PLAYBACK_READ_TIMEOUT_MS = 30_000;
    private static final long MOVISTAR_ISM_FAST_ZAP_LIVE_OFFSET_MS = 12_000L;
    private static final long TIMESHIFT_MAX_BACK_MS = 2L * 60L * 60L * 1000L;
    private static final long TIMESHIFT_SEEK_STEP_MS = 30_000L;
    private static volatile boolean playbackCrashHandlerInstalled;
    private static volatile Thread.UncaughtExceptionHandler previousUncaughtExceptionHandler;
    private static volatile PlayerController activePlaybackController;


    interface Host {
        void showStatus(String text);

        void showError(String text);

        void hideError();

        boolean isChannelCurrent(String channelId);

        void showHdrBadge(String label);

        boolean isPlaybackRepairEnabled();

        default boolean isCompactTouchDeviceMode() {
            return false;
        }

        void recordPlaybackError(PlaybackRequest request, PlaybackDiagnostics diagnostics);

        void onPlaybackReady(PlaybackRequest request);

        void onFirstVideoFrameRendered(String channelId);

        default void onPlaybackQualityChanged(PlaybackDiagnostics diagnostics) {
        }

        default void onPlaybackAutoRecoveryReady(PlaybackRequest request, PlaybackDiagnostics diagnostics, String reason) {
        }
    }

    static final class PlaybackRequest {
        final String channelId;
        final String channelName;
        final String platformName;
        final String playUrl;
        final String fallbackPlayUrl;
        final String playbackMode;
        final String drmScheme;
        final String drmLicenseUrl;
        final boolean directPlayback;
        final boolean vod;
        final String playbackProfile;

        PlaybackRequest(String channelId, String channelName, String platformName, String playUrl, String fallbackPlayUrl, String playbackMode, String drmScheme, String drmLicenseUrl, boolean directPlayback) {
            this(channelId, channelName, platformName, playUrl, fallbackPlayUrl, playbackMode, drmScheme, drmLicenseUrl, directPlayback, false, "");
        }

        PlaybackRequest(String channelId, String channelName, String platformName, String playUrl, String fallbackPlayUrl, String playbackMode, String drmScheme, String drmLicenseUrl, boolean directPlayback, String playbackProfile) {
            this(channelId, channelName, platformName, playUrl, fallbackPlayUrl, playbackMode, drmScheme, drmLicenseUrl, directPlayback, false, playbackProfile);
        }

        PlaybackRequest(String channelId, String channelName, String platformName, String playUrl, String fallbackPlayUrl, String playbackMode, String drmScheme, String drmLicenseUrl, boolean directPlayback, boolean vod, String playbackProfile) {
            this.channelId = channelId;
            this.channelName = channelName;
            this.platformName = platformName == null ? "" : platformName.trim();
            this.playUrl = playUrl;
            this.fallbackPlayUrl = fallbackPlayUrl;
            this.playbackMode = playbackMode;
            this.drmScheme = drmScheme == null ? "" : drmScheme.trim();
            this.drmLicenseUrl = drmLicenseUrl == null ? "" : drmLicenseUrl.trim();
            this.directPlayback = directPlayback;
            this.vod = vod;
            this.playbackProfile = playbackProfile == null ? "" : playbackProfile.trim().toLowerCase(Locale.ROOT);
        }

        boolean hasFallback() {
            return fallbackPlayUrl != null && !fallbackPlayUrl.trim().isEmpty();
        }
    }

    static final class StreamInfo {
        String drmType;
        String licenseUrl;
        String clearKeyLicenseDataUri;
        String clearKeyKidHex;
        String clearKeyKeyHex;
        String patchedClearKeyManifestDataUri;
        String patchedSmoothClearKeyManifestDataUri;
        String sourceUrl;
        String type;
        boolean encrypted;
    }

    static final class AudioTrackOption {
        final int groupIndex;
        final int trackIndex;
        final String label;
        final String language;
        final boolean selected;
        final boolean supported;

        AudioTrackOption(int groupIndex, int trackIndex, String label, String language, boolean selected, boolean supported) {
            this.groupIndex = groupIndex;
            this.trackIndex = trackIndex;
            this.label = label;
            this.language = language;
            this.selected = selected;
            this.supported = supported;
        }
    }

    static final class PlaybackDiagnostics {
        final String channelName;
        final String playbackState;
        final String playbackPhase;
        final String routeLabel;
        final String targetUrl;
        final String mimeType;
        final String drmType;
        final String playbackMode;
        final boolean encrypted;
        final boolean usingFallback;
        final String lastError;
        final int videoWidth;
        final int videoHeight;
        final String videoCodec;
        final int videoBitrate;
        final float videoFrameRate;
        final String audioCodec;
        final int attemptGeneration;
        final long prepareElapsedMs;
        final long readyElapsedMs;
        final int bufferingCount;
        final long bufferingTotalMs;
        final boolean firstFrameRendered;

        PlaybackDiagnostics(String channelName, String playbackState, String playbackPhase, String routeLabel, String targetUrl, String mimeType, String drmType, String playbackMode, boolean encrypted, boolean usingFallback, String lastError, int videoWidth, int videoHeight, String videoCodec, int videoBitrate, float videoFrameRate, String audioCodec, int attemptGeneration, long prepareElapsedMs, long readyElapsedMs, int bufferingCount, long bufferingTotalMs, boolean firstFrameRendered) {
            this.channelName = channelName;
            this.playbackState = playbackState;
            this.playbackPhase = playbackPhase;
            this.routeLabel = routeLabel;
            this.targetUrl = targetUrl;
            this.mimeType = mimeType;
            this.drmType = drmType;
            this.playbackMode = playbackMode;
            this.encrypted = encrypted;
            this.usingFallback = usingFallback;
            this.lastError = lastError;
            this.videoWidth = videoWidth;
            this.videoHeight = videoHeight;
            this.videoCodec = videoCodec;
            this.videoBitrate = videoBitrate;
            this.videoFrameRate = videoFrameRate;
            this.audioCodec = audioCodec;
            this.attemptGeneration = Math.max(0, attemptGeneration);
            this.prepareElapsedMs = Math.max(0L, prepareElapsedMs);
            this.readyElapsedMs = Math.max(0L, readyElapsedMs);
            this.bufferingCount = Math.max(0, bufferingCount);
            this.bufferingTotalMs = Math.max(0L, bufferingTotalMs);
            this.firstFrameRendered = firstFrameRendered;
        }

        boolean hasVideoQuality() {
            return videoWidth > 0 || videoHeight > 0 || (videoCodec != null && !videoCodec.trim().isEmpty());
        }
    }

    static final class TimeshiftState {
        final long startMs;
        final long endMs;
        final long currentMs;
        final String label;

        TimeshiftState(long startMs, long endMs, long currentMs, String label) {
            this.startMs = startMs;
            this.endMs = endMs;
            this.currentMs = currentMs;
            this.label = label;
        }
    }

    static final class PlaybackSeekState {
        final long startMs;
        final long endMs;
        final long currentMs;
        final String label;
        final boolean liveCapable;

        PlaybackSeekState(long startMs, long endMs, long currentMs, String label, boolean liveCapable) {
            this.startMs = startMs;
            this.endMs = endMs;
            this.currentMs = currentMs;
            this.label = label;
            this.liveCapable = liveCapable;
        }
    }

    private final Context context;
    private final PlayerView playerView;
    private final String baseUrl;
    private final ExecutorService ioExecutor;
    private final Handler uiHandler;
    private final Host host;
    private final HttpClient httpClient;
    private final SharedPreferences prefs;
    private final PlaybackRouteResolver playbackRouteResolver;
    private final CatalogSnapshotStore catalogSnapshotStore;
    private final LocalSmoothManifestServer localSmoothManifestServer;
    private final LocalDashManifestServer localDashManifestServer;

    private DefaultTrackSelector trackSelector;
    private DefaultHttpDataSource.Factory httpDataSourceFactory;
    private ExoPlayer player;
    private PlaybackRequest currentRequest;
    private StreamInfo currentStreamInfo;
    private PlaybackRouteResolver.Decision currentPlaybackDecision;
    private final AtomicInteger playbackAttemptGeneration = new AtomicInteger();
    private boolean usingPlaybackFallback;
    private final Set<String> attemptedRecoveryRoutes = new HashSet<>();
    private String currentRecordingUrl;
    private String lastPlaybackState = "IDLE";
    private String lastPlaybackPhase = "idle";
    private String lastErrorSummary;
    private String lastHdrBadgeChannelId;
    private boolean forceLiveEdgeOnNextReady;
    private boolean movistarIsmFastZapOffsetPending;
    private boolean usingVideoCompatibilityCap;
    private int lastVideoWidth;
    private int lastVideoHeight;
    private String lastVideoCodec;
    private int lastVideoBitrate;
    private float lastVideoFrameRate;
    private String lastAudioCodec;
    private String lastPlaybackQualityKey;
    private boolean pendingAutoRecoveryReadyReport;
    private String pendingAutoRecoveryReason;
    private boolean firstFrameRenderedForCurrentItem;
    private long currentPrepareStartedMs;
    private long currentReadyElapsedMs;
    private long currentBufferingStartedMs;
    private int currentBufferingCount;
    private long currentBufferingTotalMs;
    private final Runnable firstFrameRecoveryRunnable;
    private final Runnable forceLiveEdgeRunnable = () -> {
        if (player != null && forceLiveEdgeOnNextReady && isTimeshiftAvailable()) {
            player.seekToDefaultPosition();
            player.play();
            Log.d(TAG, "forced delayed live edge for channel=" + describeRequest(currentRequest));
        }
    };

    PlayerController(Context context, PlayerView playerView, String baseUrl, ExecutorService ioExecutor, Handler uiHandler, Host host) {
        this.context = context;
        this.playerView = playerView;
        this.baseUrl = baseUrl;
        this.ioExecutor = ioExecutor;
        this.uiHandler = uiHandler;
        this.host = host;
        this.httpClient = new HttpClient();
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.playbackRouteResolver = new PlaybackRouteResolver(baseUrl);
        this.catalogSnapshotStore = new CatalogSnapshotStore(context);
        this.localSmoothManifestServer = new LocalSmoothManifestServer();
        this.localDashManifestServer = new LocalDashManifestServer();
        this.firstFrameRecoveryRunnable = this::recoverPlaybackWhenReadyHasNoFirstFrame;
    }

    void initialize() {
        installPlaybackCrashGuard();
        activePlaybackController = this;
        trackSelector = new DefaultTrackSelector(context);
        trackSelector.setParameters(trackSelector.buildUponParameters()
                .setForceHighestSupportedBitrate(true));

        httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(PLAYBACK_CONNECT_TIMEOUT_MS)
                .setReadTimeoutMs(PLAYBACK_READ_TIMEOUT_MS)
                .setDefaultRequestProperties(buildPlaybackRequestHeaders());
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context, httpDataSourceFactory);

        player = new ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .setLoadControl(new DefaultLoadControl.Builder()
                        // Keep TV zapping fast, but hold a small extra cushion before
                        // resume to avoid short rebuffer loops on live HLS edges.
                        .setBufferDurationsMs(
                                host.isCompactTouchDeviceMode() ? 14_000 : 18_000,
                                host.isCompactTouchDeviceMode() ? 60_000 : 50_000,
                                host.isCompactTouchDeviceMode() ? 1_500 : 1_500,
                                host.isCompactTouchDeviceMode() ? 3_000 : 6_000)
                        .setPrioritizeTimeOverSizeThresholds(true)
                        .build())
                .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory)
                        .setDrmSessionManagerProvider(createDrmSessionManagerProvider()))
                .setSeekBackIncrementMs(TIMESHIFT_SEEK_STEP_MS)
                .setSeekForwardIncrementMs(TIMESHIFT_SEEK_STEP_MS)
                .build();
        playerView.setPlayer(player);
        playerView.setUseController(false);
        playerView.setKeepContentOnPlayerReset(false);
        playerView.setKeepScreenOn(true);
        playerView.setFocusable(true);
        playerView.setFocusableInTouchMode(true);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                PlaybackRequest request = currentRequest;
                PlaybackRouteResolver.Decision decision = currentPlaybackDecision;
                Log.w(TAG, "onPlayerError channel=" + describeRequest(request)
                        + " decision=" + describeDecision(decision)
                        + " streamInfo=" + describeStreamInfo(currentStreamInfo)
                    + " errorCode=" + PlaybackException.getErrorCodeName(error.errorCode)
                        + " message=" + safeLogValue(error.getMessage()), error);
                if (tryAutoRecovery(request, decision, error)) {
                    return;
                }

                String message = context.getString(R.string.error_playback_message, error.getMessage());
                lastErrorSummary = message;
                lastPlaybackPhase = "error";
                host.showError(message);
                host.recordPlaybackError(request, getPlaybackDiagnostics());
                Log.w(TAG, message, error);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                lastPlaybackState = playbackStateToString(playbackState);
                long elapsedMs = currentPrepareStartedMs <= 0L ? -1L : SystemClock.elapsedRealtime() - currentPrepareStartedMs;
                Log.d(TAG, "playbackState=" + playbackStateToString(playbackState)
                        + " channel=" + describeRequest(currentRequest)
                        + " decision=" + describeDecision(currentPlaybackDecision)
                        + " playWhenReady=" + (player != null && player.getPlayWhenReady())
                        + " elapsedMs=" + elapsedMs);
                if (playbackState == Player.STATE_BUFFERING) {
                    lastPlaybackPhase = firstFrameRenderedForCurrentItem ? "rebuffering" : "buffering";
                    currentBufferingCount++;
                    currentBufferingStartedMs = SystemClock.elapsedRealtime();
                    Log.w(TAG, "playbackBufferingStart channel=" + describeRequest(currentRequest)
                            + " count=" + currentBufferingCount
                            + " elapsedMs=" + elapsedMs
                            + " compactTouch=" + host.isCompactTouchDeviceMode()
                            + playbackBufferDebugSuffix());
                } else if (currentBufferingStartedMs > 0L) {
                    long bufferingMs = Math.max(0L, SystemClock.elapsedRealtime() - currentBufferingStartedMs);
                    currentBufferingTotalMs += bufferingMs;
                    currentBufferingStartedMs = 0L;
                    Log.w(TAG, "playbackBufferingEnd channel=" + describeRequest(currentRequest)
                            + " state=" + playbackStateToString(playbackState)
                            + " lastBufferMs=" + bufferingMs
                            + " totalBufferMs=" + currentBufferingTotalMs
                            + " count=" + currentBufferingCount
                            + playbackBufferDebugSuffix());
                }
                uiHandler.removeCallbacks(firstFrameRecoveryRunnable);
                if (playbackState == Player.STATE_READY) {
                    currentReadyElapsedMs = elapsedMs;
                    lastPlaybackPhase = firstFrameRenderedForCurrentItem ? "playing" : "ready_waiting_first_frame";
                    Log.w(TAG, "playbackReady channel=" + describeRequest(currentRequest)
                            + " readyElapsedMs=" + elapsedMs
                            + " bufferCount=" + currentBufferingCount
                            + " bufferTotalMs=" + currentBufferingTotalMs
                            + " compactTouch=" + host.isCompactTouchDeviceMode());
                    updateSelectedPlaybackFormats();
                    applyMovistarIsmFastZapOffsetIfNeeded();
                    if (forceLiveEdgeOnNextReady && isTimeshiftAvailable()) {
                        player.seekToDefaultPosition();
                        player.play();
                        uiHandler.removeCallbacks(forceLiveEdgeRunnable);
                        uiHandler.postDelayed(forceLiveEdgeRunnable, 900L);
                        forceLiveEdgeOnNextReady = false;
                        Log.d(TAG, "forced live edge on ready for channel=" + describeRequest(currentRequest));
                    }
                    host.hideError();
                    maybeShowHdrBadge();
                    host.onPlaybackReady(currentRequest);
                    if (shouldRecoverWhenReadyHasNoFirstFrame(currentRequest, currentPlaybackDecision)) {
                        uiHandler.postDelayed(firstFrameRecoveryRunnable, 4_000L);
                    }
                    if (pendingAutoRecoveryReadyReport && currentRequest != null) {
                        host.onPlaybackAutoRecoveryReady(currentRequest, getPlaybackDiagnostics(), pendingAutoRecoveryReason);
                        pendingAutoRecoveryReadyReport = false;
                        pendingAutoRecoveryReason = "";
                    }
                }
            }

            @Override
            public void onRenderedFirstFrame() {
                PlaybackRequest request = currentRequest;
                firstFrameRenderedForCurrentItem = true;
                lastPlaybackPhase = "playing";
                uiHandler.removeCallbacks(firstFrameRecoveryRunnable);
                long elapsedMs = currentPrepareStartedMs <= 0L ? -1L : SystemClock.elapsedRealtime() - currentPrepareStartedMs;
                Log.w(TAG, "firstFrame channel=" + describeRequest(request)
                        + " decision=" + describeDecision(currentPlaybackDecision)
                        + " readyElapsedMs=" + currentReadyElapsedMs
                        + " firstFrameElapsedMs=" + elapsedMs
                        + " bufferCount=" + currentBufferingCount
                        + " bufferTotalMs=" + currentBufferingTotalMs);
                host.onFirstVideoFrameRendered(request == null ? "" : request.channelId);
            }

            @Override
            public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
                if (videoSize.width > 0) {
                    lastVideoWidth = videoSize.width;
                }
                if (videoSize.height > 0) {
                    lastVideoHeight = videoSize.height;
                }
                updateSelectedPlaybackFormats();
            }

            @Override
            public void onTracksChanged(@NonNull Tracks tracks) {
                updateSelectedPlaybackFormats();
            }
        });
    }

    private void installPlaybackCrashGuard() {
        if (playbackCrashHandlerInstalled) {
            return;
        }
        synchronized (PlayerController.class) {
            if (playbackCrashHandlerInstalled) {
                return;
            }
            previousUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                PlayerController controller = activePlaybackController;
                if (controller != null && controller.handlePlaybackThreadCrash(thread, throwable)) {
                    return;
                }
                Thread.UncaughtExceptionHandler previous = previousUncaughtExceptionHandler;
                if (previous != null) {
                    previous.uncaughtException(thread, throwable);
                }
            });
            playbackCrashHandlerInstalled = true;
        }
    }

    private boolean handlePlaybackThreadCrash(Thread thread, Throwable throwable) {
        if (!isKnownHlsPlaybackThreadCrash(thread, throwable)) {
            return false;
        }
        PlaybackRequest request = currentRequest;
        PlaybackRouteResolver.Decision decision = currentPlaybackDecision;
        StreamInfo streamInfo = currentStreamInfo;
        Log.e(TAG, "guarded internal HLS playback crash channel=" + describeRequest(request)
                + " decision=" + describeDecision(decision)
                + " streamInfo=" + describeStreamInfo(streamInfo), throwable);
        uiHandler.post(() -> recoverAfterInternalPlaybackThreadCrash(request, streamInfo));
        return true;
    }

    private boolean isKnownHlsPlaybackThreadCrash(Thread thread, Throwable throwable) {
        if (throwable == null || !(throwable instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        String threadName = thread == null ? "" : thread.getName();
        if (!threadName.toLowerCase(Locale.ROOT).contains("exoplayer")) {
            return false;
        }
        for (StackTraceElement element : throwable.getStackTrace()) {
            String className = element == null ? "" : element.getClassName();
            if (className.contains("androidx.media3.exoplayer.hls")
                    || className.contains("BaseTrackSelection")) {
                return true;
            }
        }
        return false;
    }

    private void recoverAfterInternalPlaybackThreadCrash(PlaybackRequest request, StreamInfo streamInfo) {
        if (request == null) {
            host.showError(context.getString(R.string.error_playback_message, "Error interno HLS"));
            return;
        }
        try {
            if (player != null) {
                playerView.setPlayer(null);
                player.release();
                player = null;
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "failed to release crashed player", e);
        }
        host.showStatus(context.getString(R.string.status_playback_repair_trying, formatPlaybackModeLabel(request.playbackMode)));
        attemptedRecoveryRoutes.add(routeAttemptKey(currentPlaybackDecision) + "|internal-hls-crash");
        usingVideoCompatibilityCap = true;
        clearPlaybackQuality();
        initialize();
        markPendingAutoRecovery(context.getString(R.string.status_playback_repair_reason_video_cap));
        playChannelInternal(request, true, false, streamInfo, 0L);
    }

    private void clearPlaybackQuality() {
        lastVideoWidth = 0;
        lastVideoHeight = 0;
        lastVideoCodec = "";
        lastVideoBitrate = 0;
        lastVideoFrameRate = 0f;
        lastAudioCodec = "";
        lastPlaybackQualityKey = "";
    }

    private void updateSelectedPlaybackFormats() {
        if (player == null) {
            return;
        }
        Format videoFormat = player.getVideoFormat();
        if (videoFormat != null) {
            if (videoFormat.width > 0) {
                lastVideoWidth = videoFormat.width;
            }
            if (videoFormat.height > 0) {
                lastVideoHeight = videoFormat.height;
            }
            if (videoFormat.bitrate > 0) {
                lastVideoBitrate = videoFormat.bitrate;
            }
            if (videoFormat.frameRate > 0f) {
                lastVideoFrameRate = videoFormat.frameRate;
            }
            lastVideoCodec = firstNonEmpty(videoFormat.codecs, formatMimeLabel(videoFormat.sampleMimeType), lastVideoCodec);
        }
        Tracks tracks = player.getCurrentTracks();
        for (Tracks.Group group : tracks.getGroups()) {
            if (group == null || group.getType() != C.TRACK_TYPE_AUDIO || !group.isSelected()) {
                continue;
            }
            for (int i = 0; i < group.length; i++) {
                if (!group.isTrackSelected(i)) {
                    continue;
                }
                Format audioFormat = group.getTrackFormat(i);
                if (audioFormat != null) {
                    lastAudioCodec = firstNonEmpty(audioFormat.codecs, formatMimeLabel(audioFormat.sampleMimeType), lastAudioCodec);
                    notifyPlaybackQualityChangedIfNeeded();
                    return;
                }
            }
        }
        notifyPlaybackQualityChangedIfNeeded();
    }

    private void notifyPlaybackQualityChangedIfNeeded() {
        PlaybackDiagnostics diagnostics = getPlaybackDiagnostics();
        String key = diagnostics.videoWidth + "x"
                + diagnostics.videoHeight + "|"
                + safeLower(diagnostics.videoCodec) + "|"
                + diagnostics.videoBitrate + "|"
                + Math.round(diagnostics.videoFrameRate) + "|"
                + safeLower(diagnostics.audioCodec);
        if (key.equals(lastPlaybackQualityKey)) {
            return;
        }
        lastPlaybackQualityKey = key;
        host.onPlaybackQualityChanged(diagnostics);
    }

    private static String firstNonEmpty(String... values) {
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

    private static String formatMimeLabel(String mimeType) {
        String value = mimeType == null ? "" : mimeType.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return "";
        }
        if (value.contains("avc")) {
            return "H.264";
        }
        if (value.contains("hevc") || value.contains("h265")) {
            return "H.265";
        }
        if (value.contains("mp4a") || value.contains("aac")) {
            return "AAC";
        }
        if (value.contains("ac-3")) {
            return "AC-3";
        }
        if (value.contains("ec-3")) {
            return "E-AC-3";
        }
        return mimeType.trim();
    }

    void resetFallbackState() {
        usingPlaybackFallback = false;
        usingVideoCompatibilityCap = false;
        clearPlaybackQuality();
        attemptedRecoveryRoutes.clear();
        pendingAutoRecoveryReadyReport = false;
        pendingAutoRecoveryReason = "";
        forceLiveEdgeOnNextReady = false;
        uiHandler.removeCallbacks(forceLiveEdgeRunnable);
        Log.d(TAG, "compatibility fallback state reset");
    }

    private DrmSessionManagerProvider createDrmSessionManagerProvider() {
        DefaultDrmSessionManagerProvider defaultProvider = new DefaultDrmSessionManagerProvider();
        return mediaItem -> {
            if (mediaItem.localConfiguration == null || mediaItem.localConfiguration.drmConfiguration == null) {
                return DrmSessionManager.DRM_UNSUPPORTED;
            }
            MediaItem.DrmConfiguration drmConfiguration = mediaItem.localConfiguration.drmConfiguration;
            String licenseUri = drmConfiguration.licenseUri == null ? "" : drmConfiguration.licenseUri.toString();
            if (C.CLEARKEY_UUID.equals(drmConfiguration.scheme) && licenseUri.startsWith(CLEARKEY_DATA_URI_PREFIX)) {
                byte[] clearKeyResponse = decodeClearKeyDataUri(licenseUri);
                if (clearKeyResponse != null && clearKeyResponse.length > 0) {
                    String mimeType = mediaItem.localConfiguration.mimeType == null ? "" : mediaItem.localConfiguration.mimeType;
                    if (MimeTypes.APPLICATION_SS.equals(mimeType)) {
                        return new LocalClearKeyDrmSessionManager(clearKeyResponse);
                    }
                    DefaultDrmSessionManager drmSessionManager = new DefaultDrmSessionManager.Builder()
                            .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                            .setMultiSession(drmConfiguration.multiSession)
                            .setPlayClearSamplesWithoutKeys(drmConfiguration.playClearContentWithoutKey)
                            .build(new LocalClearKeyMediaDrmCallback(clearKeyResponse));
                    drmSessionManager.setMode(DefaultDrmSessionManager.MODE_PLAYBACK, drmConfiguration.getKeySetId());
                    return drmSessionManager;
                }
                Log.w(TAG, "invalid local clearkey response, falling back to default DRM provider");
            }
            return defaultProvider.get(mediaItem);
        };
    }

    @Nullable
    private static byte[] decodeClearKeyDataUri(String licenseUri) {
        if (licenseUri == null || !licenseUri.startsWith(CLEARKEY_DATA_URI_PREFIX)) {
            return null;
        }
        try {
            return Base64.decode(licenseUri.substring(CLEARKEY_DATA_URI_PREFIX.length()), Base64.DEFAULT);
        } catch (Exception e) {
            Log.w(TAG, "failed to decode local clearkey response", e);
            return null;
        }
    }

    private static final class LocalClearKeyMediaDrmCallback implements MediaDrmCallback {
        private final byte[] keyResponse;

        LocalClearKeyMediaDrmCallback(byte[] keyResponse) {
            this.keyResponse = keyResponse;
        }

        @Override
        public Response executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) throws MediaDrmCallbackException {
            return new Response(new byte[0]);
        }

        @Override
        public Response executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) throws MediaDrmCallbackException {
            return new Response(keyResponse);
        }
    }


    private static final class LocalClearKeyDrmSessionManager implements DrmSessionManager {
        private final LocalClearKeyDrmSession session;

        LocalClearKeyDrmSessionManager(byte[] keyResponse) {
            this.session = new LocalClearKeyDrmSession(keyResponse);
        }

        @Override
        public void setPlayer(Looper looper, PlayerId playerId) {
        }

        @Override
        public DrmSession acquireSession(DrmSessionEventListener.EventDispatcher eventDispatcher, Format format) {
            session.acquire(eventDispatcher);
            return session;
        }

        @Override
        public int getCryptoType(Format format) {
            return C.CRYPTO_TYPE_FRAMEWORK;
        }
    }

    private static final class LocalClearKeyDrmSession implements DrmSession {
        private final byte[] keyResponse;
        private MediaDrm mediaDrm;
        private byte[] sessionId;
        private DrmSessionException error;
        private int acquireCount;

        LocalClearKeyDrmSession(byte[] keyResponse) {
            this.keyResponse = keyResponse;
        }

        @Override
        public int getState() {
            if (error != null) {
                return STATE_ERROR;
            }
            return sessionId == null ? STATE_OPENING : STATE_OPENED_WITH_KEYS;
        }

        @Nullable
        @Override
        public DrmSessionException getError() {
            return error;
        }

        @Override
        public UUID getSchemeUuid() {
            return C.CLEARKEY_UUID;
        }

        @Nullable
        @Override
        public CryptoConfig getCryptoConfig() {
            return sessionId == null ? null : new FrameworkCryptoConfig(C.CLEARKEY_UUID, sessionId);
        }

        @Override
        public Map<String, String> queryKeyStatus() {
            if (mediaDrm == null || sessionId == null) {
                return Collections.emptyMap();
            }
            return mediaDrm.queryKeyStatus(sessionId);
        }

        @Nullable
        @Override
        public byte[] getOfflineLicenseKeySetId() {
            return null;
        }

        @Override
        public boolean requiresSecureDecoder(String mimeType) {
            return false;
        }

        @Override
        public synchronized void acquire(DrmSessionEventListener.EventDispatcher eventDispatcher) {
            acquireCount++;
            if (sessionId != null || error != null) {
                return;
            }
            try {
                mediaDrm = new MediaDrm(C.CLEARKEY_UUID);
                sessionId = mediaDrm.openSession();
                mediaDrm.provideKeyResponse(sessionId, keyResponse);
                Log.w(TAG, "local clearkey drm session opened for Smooth");
            } catch (MediaDrmException | RuntimeException e) {
                error = new DrmSessionException(e, PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR);
                if (eventDispatcher != null) {
                    eventDispatcher.drmSessionManagerError(error);
                }
                Log.w(TAG, "local clearkey drm session failed for Smooth", e);
            }
        }

        @Override
        public synchronized void release(DrmSessionEventListener.EventDispatcher eventDispatcher) {
            acquireCount = Math.max(0, acquireCount - 1);
            if (acquireCount > 0) {
                return;
            }
            if (mediaDrm != null) {
                if (sessionId != null) {
                    try {
                        mediaDrm.closeSession(sessionId);
                    } catch (RuntimeException ignored) {
                    }
                }
                mediaDrm.release();
            }
            mediaDrm = null;
            sessionId = null;
            error = null;
        }
    }

    private boolean tryAutoRecovery(PlaybackRequest request, PlaybackRouteResolver.Decision decision, PlaybackException error) {
        if (request == null || decision == null) {
            return false;
        }
        if (request.directPlayback) {
            return false;
        }
        if (!host.isPlaybackRepairEnabled()) {
            return false;
        }
        if (error != null && error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            String recoveryKey = routeAttemptKey(decision) + "|behind-live-window";
            if (!attemptedRecoveryRoutes.contains(recoveryKey)) {
                attemptedRecoveryRoutes.add(recoveryKey);
                Log.w(TAG, "retrying playback after behind live window channel=" + describeRequest(request)
                        + " decision=" + describeDecision(decision));
                markPendingAutoRecovery(context.getString(R.string.status_playback_repair_reason_route, formatPlaybackModeLabel(decision.playbackMode)));
                playChannelInternal(request, true, usingPlaybackFallback, currentStreamInfo);
                return true;
            }
        }
        if (!usingVideoCompatibilityCap && shouldRetryWithVideoCompatibilityCap(request, decision, error)) {
            usingVideoCompatibilityCap = true;
            attemptedRecoveryRoutes.add(routeAttemptKey(decision) + "|video720");
            Log.w(TAG, "retrying playback with 720p video compatibility cap channel=" + describeRequest(request)
                    + " decision=" + describeDecision(decision));
            host.showStatus(context.getString(R.string.status_playback_repair_trying, formatPlaybackModeLabel(decision.playbackMode)));
            markPendingAutoRecovery(context.getString(R.string.status_playback_repair_reason_video_cap));
            playChannelInternal(request, true, usingPlaybackFallback, currentStreamInfo);
            return true;
        }
        if (decision.allowCompatibilityFallback && !usingPlaybackFallback && request.hasFallback()) {
            usingPlaybackFallback = true;
            attemptedRecoveryRoutes.add(routeAttemptKey(decision));
            Log.w(TAG, "retrying compatibility fallback for channel=" + describeRequest(request));
            host.showStatus(context.getString(R.string.status_retry_compat));
            markPendingAutoRecovery(context.getString(R.string.status_playback_repair_reason_fallback));
            playChannelInternal(request, true, true, currentStreamInfo);
            return true;
        }
        String playbackMode = request.playbackMode == null || request.playbackMode.trim().isEmpty() ? PlaybackModeStore.MODE_AUTO : request.playbackMode;
        if (!PlaybackModeStore.MODE_AUTO.equals(playbackMode)) {
            return false;
        }
        attemptedRecoveryRoutes.add(routeAttemptKey(decision));
        PlaybackRequest[] alternatives = BuildConfig.STANDALONE_MODE
                ? new PlaybackRequest[]{
                cloneRequestWithMode(request, PlaybackModeStore.MODE_DIRECT),
                cloneRequestWithMode(request, PlaybackModeStore.MODE_PROXY)
        }
                : new PlaybackRequest[]{
                cloneRequestWithMode(request, PlaybackModeStore.MODE_DIRECT),
                cloneRequestWithMode(request, PlaybackModeStore.MODE_PROXY)
        };
        for (PlaybackRequest alternative : alternatives) {
            PlaybackRouteResolver.Decision alternativeDecision = buildPlaybackDecision(alternative, false, currentStreamInfo);
            String routeKey = routeAttemptKey(alternativeDecision);
            if (routeKey.equals(routeAttemptKey(decision)) || attemptedRecoveryRoutes.contains(routeKey)) {
                continue;
            }
            attemptedRecoveryRoutes.add(routeKey);
            Log.w(TAG, "retrying automatic playback recovery channel=" + describeRequest(request)
                    + " via mode=" + alternative.playbackMode
                    + " decision=" + describeDecision(alternativeDecision));
            host.showStatus(context.getString(R.string.status_playback_repair_trying, formatPlaybackModeLabel(alternative.playbackMode)));
            markPendingAutoRecovery(context.getString(R.string.status_playback_repair_reason_route, formatPlaybackModeLabel(alternative.playbackMode)));
            playChannelInternal(alternative, true, false, currentStreamInfo);
            return true;
        }
        return false;
    }

    private void markPendingAutoRecovery(String reason) {
        pendingAutoRecoveryReadyReport = true;
        pendingAutoRecoveryReason = reason == null ? "" : reason.trim();
    }

    private boolean shouldRetryWithVideoCompatibilityCap(PlaybackRequest request, PlaybackRouteResolver.Decision decision, PlaybackException error) {
        if (request == null || decision == null || error == null) {
            return false;
        }
        String drmType = safeLower(decision.drmType);
        if (!"clearkey".equals(drmType)) {
            return false;
        }
        String message = safeLower(error.getMessage());
        Throwable cause = error.getCause();
        String causeMessage = cause == null ? "" : safeLower(cause.getMessage());
        return message.contains("mediacodecvideorenderer")
                || message.contains("video/avc")
                || causeMessage.contains("queuesecureinputbuffer")
                || causeMessage.contains("media codec");
    }

    private void recoverPlaybackWhenReadyHasNoFirstFrame() {
        PlaybackRequest request = currentRequest;
        PlaybackRouteResolver.Decision decision = currentPlaybackDecision;
        if (player == null || request == null || decision == null || firstFrameRenderedForCurrentItem) {
            return;
        }
        if (!shouldRecoverWhenReadyHasNoFirstFrame(request, decision)) {
            return;
        }
        if (!usingVideoCompatibilityCap) {
            usingVideoCompatibilityCap = true;
            attemptedRecoveryRoutes.add(routeAttemptKey(decision) + "|video720-ready-no-frame");
            Log.w(TAG, "retrying playback after READY without first frame channel=" + describeRequest(request)
                    + " decision=" + describeDecision(decision));
            host.showStatus(context.getString(R.string.status_playback_repair_trying, formatPlaybackModeLabel(decision.playbackMode)));
            markPendingAutoRecovery(context.getString(R.string.status_playback_repair_reason_video_cap));
            playChannelInternal(request, true, usingPlaybackFallback, currentStreamInfo);
            return;
        }
        PlaybackRequest proxyRequest = cloneRequestWithMode(request, PlaybackModeStore.MODE_PROXY);
        PlaybackRouteResolver.Decision proxyDecision = buildPlaybackDecision(proxyRequest, false, currentStreamInfo);
        String proxyRouteKey = routeAttemptKey(proxyDecision) + "|ready-no-frame";
        if (routeAttemptKey(proxyDecision).equals(routeAttemptKey(decision)) || attemptedRecoveryRoutes.contains(proxyRouteKey)) {
            return;
        }
        attemptedRecoveryRoutes.add(proxyRouteKey);
        Log.w(TAG, "retrying playback via proxy after READY without first frame channel=" + describeRequest(request)
                + " decision=" + describeDecision(proxyDecision));
        host.showStatus(context.getString(R.string.status_playback_repair_trying, formatPlaybackModeLabel(proxyRequest.playbackMode)));
        markPendingAutoRecovery(context.getString(R.string.status_playback_repair_reason_route, formatPlaybackModeLabel(proxyRequest.playbackMode)));
        playChannelInternal(proxyRequest, true, false, currentStreamInfo);
    }

    private boolean shouldRecoverWhenReadyHasNoFirstFrame(PlaybackRequest request, PlaybackRouteResolver.Decision decision) {
        if (request == null || decision == null || request.directPlayback) {
            return false;
        }
        if (!host.isPlaybackRepairEnabled()) {
            return false;
        }
        if (!isKnownClearKeyDecoderSensitiveChannel(request, decision)) {
            return false;
        }
        if (!usingVideoCompatibilityCap) {
            return true;
        }
        String playbackMode = request.playbackMode == null || request.playbackMode.trim().isEmpty()
                ? PlaybackModeStore.MODE_AUTO
                : request.playbackMode;
        return !PlaybackModeStore.MODE_PROXY.equals(playbackMode);
    }

    private String formatPlaybackModeLabel(String playbackMode) {
        if (PlaybackModeStore.MODE_DIRECT.equals(playbackMode)) {
            return context.getString(R.string.playback_mode_direct);
        }
        if (PlaybackModeStore.MODE_PROXY.equals(playbackMode)) {
            return context.getString(R.string.playback_mode_proxy);
        }
        return context.getString(R.string.playback_mode_auto);
    }

    private PlaybackRequest cloneRequestWithMode(PlaybackRequest request, String playbackMode) {
        return new PlaybackRequest(
                request.channelId,
                request.channelName,
                request.platformName,
                request.playUrl,
                request.fallbackPlayUrl,
                playbackMode,
                request.drmScheme,
                request.drmLicenseUrl,
                request.directPlayback,
                request.vod,
                request.playbackProfile
        );
    }

    private String routeAttemptKey(PlaybackRouteResolver.Decision decision) {
        if (decision == null) {
            return "";
        }
        return safeLower(decision.playbackMode) + "|"
                + safeLower(decision.targetUrl) + "|"
                + safeLower(decision.mimeType) + "|"
                + safeLower(decision.drmType) + "|"
                + decision.useFallback;
    }

    PlaybackDiagnostics getPlaybackDiagnostics() {
        String channelName = currentRequest == null ? "" : safeLogValue(currentRequest.channelName);
        String routeLabel = describeRouteLabel(currentPlaybackDecision);
        String targetUrl = currentPlaybackDecision == null ? "" : safeLogValue(currentPlaybackDecision.targetUrl);
        String mimeType = currentPlaybackDecision == null ? "" : safeLogValue(currentPlaybackDecision.mimeType);
        String drmType = currentPlaybackDecision == null ? "" : safeLogValue(currentPlaybackDecision.drmType);
        String playbackMode = currentPlaybackDecision == null ? PlaybackModeStore.MODE_AUTO : safeLogValue(currentPlaybackDecision.playbackMode);
        boolean encrypted = currentStreamInfo != null && currentStreamInfo.encrypted;
        long elapsedSincePrepareMs = currentPrepareStartedMs <= 0L ? 0L : Math.max(0L, SystemClock.elapsedRealtime() - currentPrepareStartedMs);
        long prepareElapsedMs = currentReadyElapsedMs > 0L ? currentReadyElapsedMs : elapsedSincePrepareMs;
        return new PlaybackDiagnostics(
                channelName,
                lastPlaybackState,
                safeLogValue(lastPlaybackPhase),
                routeLabel,
                targetUrl,
                mimeType,
                drmType,
                playbackMode,
                encrypted,
                usingPlaybackFallback,
                safeLogValue(lastErrorSummary),
                lastVideoWidth,
                lastVideoHeight,
                safeLogValue(lastVideoCodec),
                lastVideoBitrate,
                lastVideoFrameRate,
                safeLogValue(lastAudioCodec),
                playbackAttemptGeneration.get(),
                prepareElapsedMs,
                currentReadyElapsedMs,
                currentBufferingCount,
                currentBufferingTotalMs,
                firstFrameRenderedForCurrentItem
        );
    }

    String getCurrentRequestChannelId() {
        return currentRequest == null || currentRequest.channelId == null ? "" : currentRequest.channelId.trim();
    }

    String getCurrentRequestChannelName() {
        return currentRequest == null || currentRequest.channelName == null ? "" : currentRequest.channelName.trim();
    }

    void clearLastError() {
        lastErrorSummary = null;
    }

    boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    void setMuted(boolean muted) {
        if (player != null) {
            player.setVolume(muted ? 0f : 1f);
        }
    }

    void setPlayWhenReady(boolean playWhenReady) {
        if (player != null) {
            player.setPlayWhenReady(playWhenReady);
        }
    }

    void togglePlayback() {
        if (player == null) {
            return;
        }
        boolean playing = player.isPlaying();
        player.setPlayWhenReady(!playing);
        if (isTimeshiftAvailable()) {
            host.showStatus(getTimeshiftStatusLabel());
            return;
        }
        host.showStatus(context.getString(playing ? R.string.status_paused : R.string.status_playing));
    }

    void playChannel(PlaybackRequest request, boolean autoPlay, StreamInfo streamInfo) {
        Log.d(TAG, "playChannel request=" + describeRequest(request)
                + " autoPlay=" + autoPlay
                + " initialStreamInfo=" + describeStreamInfo(streamInfo));
        playChannel(request, autoPlay, streamInfo, 0L);
    }

    void playChannel(PlaybackRequest request, boolean autoPlay, StreamInfo streamInfo, long resumePositionMs) {
        Log.d(TAG, "playChannel request=" + describeRequest(request)
                + " autoPlay=" + autoPlay
                + " initialStreamInfo=" + describeStreamInfo(streamInfo)
                + " resumeMs=" + resumePositionMs);
        int generation = beginPlaybackAttempt(request, "playChannel");
        playChannelInternal(request, autoPlay, false, streamInfo, resumePositionMs, generation);
    }

    void resolveStreamInfoAndReplayIfNeeded(PlaybackRequest request, boolean autoPlay, Map<String, StreamInfo> streamInfoCache) {
        resolveStreamInfoAndReplayIfNeeded(request, autoPlay, streamInfoCache, 0L);
    }

    void resolveStreamInfoAndReplayIfNeeded(PlaybackRequest request, boolean autoPlay, Map<String, StreamInfo> streamInfoCache, long resumePositionMs) {
        if (request == null || request.channelId == null || request.channelId.trim().isEmpty() || (request.directPlayback && !hasLocalDrmInfo(request))) {
            return;
        }

        final String channelId = request.channelId.trim();
        final int generation = playbackAttemptGeneration.get();
        ioExecutor.execute(() -> {
            StreamInfo info = streamInfoCache.get(channelId);
            boolean fromCache = info != null;
            if (info == null) {
                info = buildLocalStreamInfoFromRequest(request);
                if (info == null) {
                    info = fetchStreamInfo(channelId);
                }
                if (info != null) {
                    streamInfoCache.put(channelId, info);
                }
            }
            info = ensurePatchedClearKeyManifests(request, info);
            Log.d(TAG, "resolveStreamInfo channelId=" + channelId
                    + " fromCache=" + fromCache
                    + " streamInfo=" + describeStreamInfo(info));
            if (info == null) {
                Log.d(TAG, "resolveStreamInfo aborted: no stream info for channelId=" + channelId);
                return;
            }

            String resolvedDrmType = safeLower(info.drmType);
            boolean requiresReplay = "widevine".equals(resolvedDrmType)
                    || "clearkey".equals(resolvedDrmType)
                    || info.encrypted;
            StreamInfo resolved = info;
            uiHandler.post(() -> {
                if (!isPlaybackAttemptCurrent(generation, request)) {
                    Log.d(TAG, "resolveStreamInfo ignored because playback attempt changed: channelId=" + channelId
                            + " generation=" + generation
                            + " currentGeneration=" + playbackAttemptGeneration.get());
                    return;
                }
                PlaybackRouteResolver.Decision resolvedDecision = buildPlaybackDecision(request, false, resolved);
                if (!requiresReplay && resolvedDecision.isEquivalentTo(currentPlaybackDecision)) {
                    Log.d(TAG, "resolveStreamInfo no replay needed channel=" + describeRequest(request)
                            + " resolvedDecision=" + describeDecision(resolvedDecision));
                    return;
                }
                Log.i(TAG, "resolveStreamInfo replaying channel=" + describeRequest(request)
                        + " generation=" + generation
                        + " requiresReplay=" + requiresReplay
                        + " previousDecision=" + describeDecision(currentPlaybackDecision)
                        + " resolvedDecision=" + describeDecision(resolvedDecision));
                playChannelInternal(request, autoPlay, false, resolved, resumePositionMs, generation);
            });
        });
    }

    void playChannelAfterResolvingStreamInfo(PlaybackRequest request, boolean autoPlay, Map<String, StreamInfo> streamInfoCache, long resumePositionMs) {
        final int generation = beginPlaybackAttempt(request, "playChannelAfterResolvingStreamInfo");
        if (request == null || request.channelId == null || request.channelId.trim().isEmpty() || (request.directPlayback && !hasLocalDrmInfo(request))) {
            playChannelInternal(request, autoPlay, false, null, resumePositionMs, generation);
            return;
        }
        final String channelId = request.channelId.trim();
        ioExecutor.execute(() -> {
            StreamInfo info = streamInfoCache == null ? null : streamInfoCache.get(channelId);
            boolean fromCache = info != null;
            if (info == null) {
                info = buildLocalStreamInfoFromRequest(request);
                if (info == null) {
                    info = fetchStreamInfo(channelId);
                }
                if (info != null && streamInfoCache != null) {
                    streamInfoCache.put(channelId, info);
                }
            }
            info = ensurePatchedClearKeyManifests(request, info);
            StreamInfo resolved = info;
            Log.d(TAG, "playChannelAfterResolvingStreamInfo channelId=" + channelId
                    + " generation=" + generation
                    + " fromCache=" + fromCache
                    + " streamInfo=" + describeStreamInfo(resolved));
            uiHandler.post(() -> {
                if (!isPlaybackAttemptCurrent(generation, request)) {
                    Log.d(TAG, "playChannelAfterResolvingStreamInfo ignored because playback attempt changed: channelId=" + channelId
                            + " generation=" + generation
                            + " currentGeneration=" + playbackAttemptGeneration.get());
                    return;
                }
                playChannelInternal(request, autoPlay, false, resolved, resumePositionMs, generation);
            });
        });
    }

    void playRecording(String recordingName, String recordingUrl, long resumePositionMs) {
        if (player == null || recordingUrl == null || recordingUrl.trim().isEmpty()) {
            return;
        }

        Log.i(TAG, "playRecording name=" + safeLogValue(recordingName)
                + " url=" + shortenUrl(recordingUrl)
                + " mime=" + safeLogValue(PlaybackRouteResolver.inferMimeType(recordingUrl))
                + " resumeMs=" + resumePositionMs);

        String mimeType = PlaybackRouteResolver.inferMimeType(recordingUrl);
        MediaItem.Builder builder = new MediaItem.Builder().setUri(recordingUrl);
        if (mimeType != null && !mimeType.trim().isEmpty()) {
            builder.setMimeType(mimeType);
        }

        currentRequest = null;
        currentStreamInfo = null;
        beginPlaybackAttempt(null, "playRecording");
        currentRecordingUrl = recordingUrl;
        usingPlaybackFallback = false;
        clearPlaybackQuality();
        uiHandler.removeCallbacks(forceLiveEdgeRunnable);
        player.setMediaItem(builder.build());
        if (resumePositionMs > 0L) {
            player.seekTo(resumePositionMs);
        }
        player.prepare();
        player.setPlayWhenReady(true);
        host.hideError();
        host.showStatus(context.getString(R.string.status_playing_recording, recordingName));
    }

    boolean isPlayingRecording() {
        return currentRecordingUrl != null && !currentRecordingUrl.trim().isEmpty();
    }

    long getCurrentPlaybackPosition() {
        if (player == null) {
            return 0L;
        }
        long value = player.getCurrentPosition();
        return value < 0L ? 0L : value;
    }

    void seekToPosition(long positionMs) {
        if (player != null && positionMs > 0L) {
            player.seekTo(positionMs);
        }
    }

    void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    boolean isTimeshiftSupportedForCurrentChannel() {
        return isTimeshiftAvailable();
    }

    boolean seekTimeshiftBack() {
        if (isTimeshiftAvailable()) {
            return seekTimeshiftBy(-TIMESHIFT_SEEK_STEP_MS);
        }
        if (player == null || !player.isCurrentMediaItemSeekable()) {
            return false;
        }
        player.seekBack();
        host.showStatus(context.getString(R.string.status_seek_back));
        return true;
    }

    boolean seekTimeshiftForward() {
        if (isTimeshiftAvailable()) {
            return seekTimeshiftBy(TIMESHIFT_SEEK_STEP_MS);
        }
        if (player == null || !player.isCurrentMediaItemSeekable()) {
            return false;
        }
        player.seekForward();
        host.showStatus(context.getString(R.string.status_seek_forward));
        return true;
    }

    boolean resumeTimeshiftLive() {
        if (!isTimeshiftAvailable() || player == null) {
            host.showStatus(context.getString(R.string.timeshift_status_unavailable));
            return false;
        }
        player.seekToDefaultPosition();
        player.play();
        host.showStatus(context.getString(R.string.timeshift_status_live));
        return true;
    }

    void showTimeshiftStatus() {
        host.showStatus(getTimeshiftStatusLabel());
    }

    TimeshiftState getTimeshiftState() {
        TimeshiftWindow window = getTimeshiftWindow();
        if (window == null) {
            return null;
        }
        return new TimeshiftState(window.startMs, window.endMs, window.currentMs, getTimeshiftStatusLabel());
    }

    PlaybackSeekState getPlaybackSeekState() {
        if (player == null || !player.isCurrentMediaItemSeekable()) {
            return null;
        }
        TimeshiftWindow timeshiftWindow = getTimeshiftWindow();
        if (timeshiftWindow != null) {
            return new PlaybackSeekState(
                    timeshiftWindow.startMs,
                    timeshiftWindow.endMs,
                    timeshiftWindow.currentMs,
                    getTimeshiftStatusLabel(),
                    true
            );
        }
        long durationMs = player.getDuration();
        if (durationMs == C.TIME_UNSET || durationMs <= 0L) {
            return null;
        }
        long currentMs = Math.max(0L, Math.min(durationMs, player.getCurrentPosition()));
        return new PlaybackSeekState(0L, durationMs, currentMs, formatPlaybackProgressLabel(currentMs, durationMs), false);
    }

    boolean seekTimeshiftTo(long targetPositionMs) {
        PlaybackSeekState state = getPlaybackSeekState();
        if (state == null || player == null) {
            host.showStatus(context.getString(R.string.timeshift_status_unavailable));
            return false;
        }
        long target = Math.max(state.startMs, Math.min(state.endMs, targetPositionMs));
        player.seekTo(target);
        player.play();
        host.showStatus(state.liveCapable ? formatTimeshiftOffset(state.endMs - target) : formatPlaybackProgressLabel(target, state.endMs));
        return true;
    }

    private void playChannelInternal(PlaybackRequest request, boolean autoPlay, boolean useFallback, StreamInfo streamInfo) {
        playChannelInternal(request, autoPlay, useFallback, streamInfo, 0L, playbackAttemptGeneration.get());
    }

    private void playChannelInternal(PlaybackRequest request, boolean autoPlay, boolean useFallback, StreamInfo streamInfo, long resumePositionMs) {
        playChannelInternal(request, autoPlay, useFallback, streamInfo, resumePositionMs, playbackAttemptGeneration.get());
    }

    private void playChannelInternal(PlaybackRequest request, boolean autoPlay, boolean useFallback, StreamInfo streamInfo, long resumePositionMs, int generation) {
        if (request == null || player == null) {
            return;
        }
        if (!isPlaybackAttemptCurrent(generation, request)) {
            Log.d(TAG, "playChannelInternal ignored because playback attempt changed: channel=" + describeRequest(request)
                    + " generation=" + generation
                    + " currentGeneration=" + playbackAttemptGeneration.get());
            return;
        }

        uiHandler.removeCallbacks(forceLiveEdgeRunnable);
        PlaybackRequest previousRequest = currentRequest;
        if (!isSameChannel(request, previousRequest)) {
            pendingAutoRecoveryReadyReport = false;
            pendingAutoRecoveryReason = "";
        }
        currentRequest = request;
        streamInfo = ensurePatchedClearKeyManifests(request, streamInfo);
        currentStreamInfo = streamInfo;
        currentRecordingUrl = null;
        usingPlaybackFallback = useFallback;
        if (!isSameChannel(request, previousRequest)) {
            usingVideoCompatibilityCap = false;
            clearPlaybackQuality();
        }
        firstFrameRenderedForCurrentItem = false;
        lastPlaybackPhase = "preparing";
        currentBufferingStartedMs = 0L;
        currentBufferingCount = 0;
        currentBufferingTotalMs = 0L;
        movistarIsmFastZapOffsetPending = false;
        lastErrorSummary = null;
        lastHdrBadgeChannelId = null;
        uiHandler.removeCallbacks(firstFrameRecoveryRunnable);
        PlaybackRouteResolver.Decision decision = buildPlaybackDecision(request, useFallback, streamInfo);
        forceLiveEdgeOnNextReady = request != null
                && request.platformName != null
                && request.platformName.toLowerCase(Locale.ROOT).contains("movistar")
                && !isMovistarIsmHlsDecision(decision);
        movistarIsmFastZapOffsetPending = request != null
                && !request.vod
                && resumePositionMs <= 0L
                && isMovistarIsmHlsDecision(decision);
        if (!request.vod
                && !useFallback
                && resumePositionMs <= 0L
                && isSameChannel(request, previousRequest)
                && decision != null
                && decision.isEquivalentTo(currentPlaybackDecision)
                && player.getPlaybackState() != Player.STATE_IDLE) {
            Log.i(TAG, "skip duplicate live prepare channel=" + describeRequest(request)
                    + " decision=" + describeDecision(decision)
                    + " streamInfo=" + describeStreamInfo(streamInfo));
            return;
        }
        currentPlaybackDecision = decision;
        Log.w(TAG, "zapPrepare channel=" + describeRequest(request)
            + " generation=" + generation
            + " autoPlay=" + autoPlay
            + " requestedFallback=" + useFallback
            + " decision=" + describeDecision(decision)
            + " streamInfo=" + describeStreamInfo(streamInfo)
            + " compactTouch=" + host.isCompactTouchDeviceMode());

        if (decision.targetUrl == null || decision.targetUrl.trim().isEmpty()) {
            host.showError(context.getString(R.string.error_empty_playback_url));
            return;
        }
        updatePlaybackRequestHeaders();
        applyVideoTrackPolicy(request, decision);

        String mediaTargetUrl = appendOfflineAccessToken(decision.targetUrl);
        MediaItem.Builder builder = new MediaItem.Builder().setUri(mediaTargetUrl);
        if (isMovistarIsmHlsDecision(decision)) {
            builder.setLiveConfiguration(new MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(35_000)
                    .setMinOffsetMs(24_000)
                    .setMaxOffsetMs(75_000)
                    .setMinPlaybackSpeed(0.98f)
                    .setMaxPlaybackSpeed(1.01f)
                    .build());
        } else if (isHevcHlsDecision(decision)) {
            builder.setLiveConfiguration(new MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(18_000)
                    .setMinOffsetMs(12_000)
                    .setMaxOffsetMs(30_000)
                    .setMinPlaybackSpeed(0.97f)
                    .setMaxPlaybackSpeed(1.02f)
                    .build());
        } else if (isProxyDashDecision(decision)) {
            // DASH live streams (e.g. France 2/3 HEVC ClearKey): use a 30-second
            // live offset so ExoPlayer requests segments that are well past the live
            // edge and already stable on the CDN, avoiding repeated rebuffering at
            // 14-15 Mbps that occurs when playing too close to the live edge.
            builder.setLiveConfiguration(new MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(30_000)
                    .setMinOffsetMs(15_000)
                    .setMaxOffsetMs(60_000)
                    .build());
        } else if (!request.vod && host.isCompactTouchDeviceMode()) {
            builder.setLiveConfiguration(new MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(18_000)
                    .setMinOffsetMs(10_000)
                    .setMaxOffsetMs(45_000)
                    .setMinPlaybackSpeed(0.97f)
                    .setMaxPlaybackSpeed(1.02f)
                    .build());
        }
        if (decision.mimeType != null && !decision.mimeType.trim().isEmpty()) {
            builder.setMimeType(decision.mimeType);
        }
        if ("widevine".equals(decision.drmType)) {
            String licenseUrl = resolveWidevineLicenseUrl(request);
            if (isSecureDrmReference(licenseUrl)) {
                licenseUrl = streamInfo != null && streamInfo.licenseUrl != null && !streamInfo.licenseUrl.trim().isEmpty()
                        ? streamInfo.licenseUrl
                        : baseUrl + "/api/widevine/" + request.channelId;
            }
            builder.setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(appendOfflineAccessToken(licenseUrl))
                    .build());
        } else if ("clearkey".equals(decision.drmType)) {
            String resolvedLocalClearKeyLicense = streamInfo != null
                    && streamInfo.clearKeyLicenseDataUri != null
                    && !streamInfo.clearKeyLicenseDataUri.trim().isEmpty()
                    ? streamInfo.clearKeyLicenseDataUri
                    : "";
            String licenseUrl = !resolvedLocalClearKeyLicense.isEmpty()
                    ? resolvedLocalClearKeyLicense
                    : shouldUseRuntimeClearKeyLicense(request, decision)
                    ? baseUrl + "/api/clearkey/" + request.channelId
                    : request.drmLicenseUrl != null && !request.drmLicenseUrl.trim().isEmpty()
                    ? request.drmLicenseUrl
                    : streamInfo != null && streamInfo.licenseUrl != null && !streamInfo.licenseUrl.trim().isEmpty()
                    ? streamInfo.licenseUrl
                    : baseUrl + "/api/clearkey/" + request.channelId;
            if (isSecureDrmReference(licenseUrl)) {
                licenseUrl = !resolvedLocalClearKeyLicense.isEmpty()
                        ? resolvedLocalClearKeyLicense
                        : streamInfo != null && streamInfo.licenseUrl != null && !streamInfo.licenseUrl.trim().isEmpty()
                        ? streamInfo.licenseUrl
                        : baseUrl + "/api/clearkey/" + request.channelId;
            }
            builder.setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                    .setLicenseUri(appendOfflineAccessToken(licenseUrl))
                    .build());
        }

        Log.d(TAG, "preparePlayback channel=" + describeRequest(request)
            + " generation=" + generation
            + " decision=" + describeDecision(decision)
            + " streamInfo=" + describeStreamInfo(streamInfo)
            + " resumeMs=" + resumePositionMs);
        currentPrepareStartedMs = SystemClock.elapsedRealtime();
        currentReadyElapsedMs = -1L;
        player.setMediaItem(builder.build());
        if (resumePositionMs > 0L) {
            player.seekTo(resumePositionMs);
        }
        player.prepare();
        player.setPlayWhenReady(autoPlay);

    }

    private int beginPlaybackAttempt(PlaybackRequest request, String origin) {
        int generation = playbackAttemptGeneration.incrementAndGet();
        lastPlaybackPhase = "playChannelAfterResolvingStreamInfo".equals(origin) ? "resolving_stream_info" : "starting";
        Log.d(TAG, "playbackAttempt begin generation=" + generation
                + " origin=" + safeLogValue(origin)
                + " channel=" + describeRequest(request));
        return generation;
    }

    private boolean isPlaybackAttemptCurrent(int generation, PlaybackRequest request) {
        if (generation != playbackAttemptGeneration.get()) {
            return false;
        }
        if (request == null || request.channelId == null || request.channelId.trim().isEmpty()) {
            return true;
        }
        return host.isChannelCurrent(request.channelId.trim());
    }

    private String resolveWidevineLicenseUrl(PlaybackRequest request) {
        String backendLicenseUrl = baseUrl + "/api/widevine/" + request.channelId;
        String requestLicenseUrl = request.drmLicenseUrl == null ? "" : request.drmLicenseUrl.trim();
        if (requestLicenseUrl.isEmpty()) {
            return backendLicenseUrl;
        }
        String normalizedBase = baseUrl == null ? "" : baseUrl.trim();
        if (!normalizedBase.isEmpty() && requestLicenseUrl.startsWith(normalizedBase)) {
            return requestLicenseUrl;
        }
        if (request.directPlayback) {
            return requestLicenseUrl;
        }
        return backendLicenseUrl;
    }

    private void applyVideoTrackPolicy(PlaybackRequest request, PlaybackRouteResolver.Decision decision) {
        if (trackSelector == null) {
            return;
        }
        boolean capForCompatibility = usingVideoCompatibilityCap;
        DefaultTrackSelector.Parameters.Builder builder = trackSelector.buildUponParameters()
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .setForceHighestSupportedBitrate(!isHlsDecision(decision));
        if (capForCompatibility) {
            builder.setMaxVideoSize(1280, 720);
            Log.i(TAG, "using 720p video compatibility cap channel=" + describeRequest(request)
                    + " decision=" + describeDecision(decision));
        } else {
            builder.clearVideoSizeConstraints();
        }
        trackSelector.setParameters(builder);
    }

    private boolean isHlsDecision(PlaybackRouteResolver.Decision decision) {
        if (decision == null) {
            return false;
        }
        String mimeType = safeLower(decision.mimeType);
        String targetUrl = safeLower(decision.targetUrl);
        return MimeTypes.APPLICATION_M3U8.equals(mimeType)
                || mimeType.contains("mpegurl")
                || targetUrl.contains(".m3u8");
    }

    private boolean isOrangePlayback(PlaybackRequest request, PlaybackRouteResolver.Decision decision) {
        String platform = request == null ? "" : safeLower(request.platformName);
        String playUrl = request == null ? "" : safeLower(request.playUrl);
        String targetUrl = decision == null ? "" : safeLower(decision.targetUrl);
        return platform.contains("orange")
                || playUrl.contains("/orange/")
                || targetUrl.contains("/orange/");
    }

    List<AudioTrackOption> getAudioTrackOptions() {
        List<AudioTrackOption> options = new ArrayList<>();
        if (player == null) {
            return options;
        }
        Tracks tracks = player.getCurrentTracks();
        if (tracks == null) {
            return options;
        }
        List<Tracks.Group> groups = tracks.getGroups();
        int audioNumber = 1;
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            Tracks.Group group = groups.get(groupIndex);
            if (group == null || group.getType() != C.TRACK_TYPE_AUDIO) {
                continue;
            }
            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                Format format = group.getTrackFormat(trackIndex);
                boolean supported = group.isTrackSupported(trackIndex);
                String label = audioTrackLabel(format, audioNumber);
                options.add(new AudioTrackOption(
                        groupIndex,
                        trackIndex,
                        label,
                        format == null ? "" : safeString(format.language),
                        group.isTrackSelected(trackIndex),
                        supported
                ));
                audioNumber++;
            }
        }
        return options;
    }

    boolean selectAudioTrack(AudioTrackOption option) {
        if (option == null || player == null || trackSelector == null) {
            return false;
        }
        Tracks tracks = player.getCurrentTracks();
        if (tracks == null || option.groupIndex < 0 || option.groupIndex >= tracks.getGroups().size()) {
            return false;
        }
        Tracks.Group group = tracks.getGroups().get(option.groupIndex);
        if (group == null || group.getType() != C.TRACK_TYPE_AUDIO || option.trackIndex < 0 || option.trackIndex >= group.length) {
            return false;
        }
        if (!group.isTrackSupported(option.trackIndex)) {
            return false;
        }
        trackSelector.setParameters(trackSelector.buildUponParameters()
                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .setOverrideForType(new TrackSelectionOverride(group.getMediaTrackGroup(), option.trackIndex)));
        return true;
    }

    void clearAudioTrackOverride() {
        if (trackSelector == null) {
            return;
        }
        trackSelector.setParameters(trackSelector.buildUponParameters()
                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false));
    }

    private String audioTrackLabel(Format format, int fallbackNumber) {
        String label = format == null ? "" : safeString(format.label);
        String language = format == null ? "" : safeString(format.language);
        if (!label.isEmpty() && !language.isEmpty()) {
            return label + " (" + language.toUpperCase(Locale.ROOT) + ")";
        }
        if (!label.isEmpty()) {
            return label;
        }
        if (!language.isEmpty()) {
            return language.toUpperCase(Locale.ROOT);
        }
        return context.getString(R.string.audio_track_fallback, fallbackNumber);
    }

    private static String safeString(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isKnownClearKeyDecoderSensitiveChannel(PlaybackRequest request, PlaybackRouteResolver.Decision decision) {
        if (request == null || decision == null || !"clearkey".equals(safeLower(decision.drmType))) {
            return false;
        }
        return "1079794".equals(request.channelId)
                || "1079795".equals(request.channelId)
                || "1079796".equals(request.channelId);
    }

    private boolean shouldUseRuntimeClearKeyLicense(PlaybackRequest request, PlaybackRouteResolver.Decision decision) {
        if (request == null || decision == null || !"clearkey".equals(safeLower(decision.drmType))) {
            return false;
        }
        return !request.directPlayback && "proxy_manifest".equals(safeLower(request.playbackProfile));
    }

    private boolean isSameChannel(PlaybackRequest left, PlaybackRequest right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.channelId == null) {
            return right.channelId == null;
        }
        return left.channelId.equals(right.channelId);
    }

    private PlaybackRouteResolver.Decision buildPlaybackDecision(PlaybackRequest request, boolean useFallback, StreamInfo streamInfo) {
        return playbackRouteResolver.buildDecision(request, useFallback, streamInfo);
    }

    private boolean isHevcHlsDecision(PlaybackRouteResolver.Decision decision) {
        return decision != null
                && decision.targetUrl != null
                && decision.targetUrl.contains("/hls/1071554/")
                && decision.targetUrl.contains("codec=hevc");
    }

    private boolean isProxyDashDecision(PlaybackRouteResolver.Decision decision) {
        return decision != null
                && decision.targetUrl != null
                && decision.targetUrl.contains("/proxy/manifest/")
                && MimeTypes.APPLICATION_MPD.equals(decision.mimeType);
    }

    private boolean isMovistarIsmHlsDecision(PlaybackRouteResolver.Decision decision) {
        return decision != null
                && decision.targetUrl != null
                && (decision.targetUrl.contains("/hls/ism/")
                || decision.targetUrl.contains("/hls/ism-mux/"))
                && isHlsDecision(decision);
    }

    private void maybeShowHdrBadge() {
        if (player == null || currentRequest == null || currentRequest.channelId == null) {
            return;
        }
        if (currentRequest.channelId.equals(lastHdrBadgeChannelId)) {
            return;
        }
        androidx.media3.common.Format format = player.getVideoFormat();
        if (format == null) {
            return;
        }
        ColorInfo colorInfo = format.colorInfo;
        if (colorInfo == null) {
            return;
        }
        int transfer = colorInfo.colorTransfer;
        if (transfer == C.COLOR_TRANSFER_ST2084 || transfer == C.COLOR_TRANSFER_HLG) {
            lastHdrBadgeChannelId = currentRequest.channelId;
            String label = transfer == C.COLOR_TRANSFER_HLG
                    ? context.getString(R.string.status_hlg_detected)
                    : context.getString(R.string.status_hdr10_detected);
            host.showHdrBadge(label);
        }
    }

    private boolean seekTimeshiftBy(long deltaMs) {
        TimeshiftWindow window = getTimeshiftWindow();
        if (window == null || player == null) {
            host.showStatus(context.getString(R.string.timeshift_status_unavailable));
            return false;
        }
        long target = Math.max(window.startMs, Math.min(window.endMs, player.getCurrentPosition() + deltaMs));
        player.seekTo(target);
        player.play();
        host.showStatus(formatTimeshiftOffset(window.endMs - target));
        return true;
    }

    private boolean isTimeshiftAvailable() {
        return player != null
                && currentRequest != null
                && safeLower(currentRequest.platformName).contains("movistar")
                && player.isCurrentMediaItemSeekable();
    }

    private String playbackBufferDebugSuffix() {
        if (player == null) {
            return "";
        }
        long positionMs = Math.max(0L, player.getCurrentPosition());
        long bufferedPositionMs = Math.max(0L, player.getBufferedPosition());
        long bufferedMs = Math.max(0L, bufferedPositionMs - positionMs);
        long liveOffsetMs = player.getCurrentLiveOffset();
        long durationMs = player.getDuration();
        return " positionMs=" + positionMs
                + " bufferedMs=" + bufferedMs
                + " bufferedPositionMs=" + bufferedPositionMs
                + " durationMs=" + durationMs
                + " liveOffsetMs=" + liveOffsetMs;
    }

    private void applyMovistarIsmFastZapOffsetIfNeeded() {
        if (!movistarIsmFastZapOffsetPending || player == null || !isMovistarIsmHlsDecision(currentPlaybackDecision)) {
            return;
        }
        movistarIsmFastZapOffsetPending = false;
        long durationMs = player.getDuration();
        if (durationMs == C.TIME_UNSET || durationMs <= MOVISTAR_ISM_FAST_ZAP_LIVE_OFFSET_MS + 3_000L) {
            Log.w(TAG, "fastZapLiveOffset skipped channel=" + describeRequest(currentRequest)
                    + " durationMs=" + durationMs
                    + playbackBufferDebugSuffix());
            return;
        }
        long currentPositionMs = Math.max(0L, player.getCurrentPosition());
        long bufferedMs = Math.max(0L, player.getBufferedPosition() - currentPositionMs);
        long currentOffsetMs = Math.max(0L, durationMs - currentPositionMs);
        if (currentOffsetMs >= MOVISTAR_ISM_FAST_ZAP_LIVE_OFFSET_MS - 1_000L && bufferedMs >= 4_000L) {
            Log.w(TAG, "fastZapLiveOffset not needed channel=" + describeRequest(currentRequest)
                    + " currentOffsetMs=" + currentOffsetMs
                    + playbackBufferDebugSuffix());
            return;
        }
        long targetPositionMs = Math.max(0L, durationMs - MOVISTAR_ISM_FAST_ZAP_LIVE_OFFSET_MS);
        if (Math.abs(targetPositionMs - currentPositionMs) >= 1_000L) {
            player.seekTo(targetPositionMs);
        }
        player.play();
        Log.w(TAG, "fastZapLiveOffset applied channel=" + describeRequest(currentRequest)
                + " currentOffsetMs=" + currentOffsetMs
                + " targetOffsetMs=" + MOVISTAR_ISM_FAST_ZAP_LIVE_OFFSET_MS
                + " targetPositionMs=" + targetPositionMs
                + playbackBufferDebugSuffix());
    }

    private String getTimeshiftStatusLabel() {
        TimeshiftWindow window = getTimeshiftWindow();
        if (window == null) {
            return context.getString(R.string.timeshift_status_unavailable);
        }
        long offsetMs = Math.max(0L, window.endMs - window.currentMs);
        if (offsetMs < 1500L) {
            return context.getString(R.string.timeshift_status_live);
        }
        return formatTimeshiftOffset(offsetMs);
    }

    private String formatTimeshiftOffset(long offsetMs) {
        long totalSeconds = Math.max(0L, Math.round(offsetMs / 1000f));
        long mins = totalSeconds / 60L;
        long secs = totalSeconds % 60L;
        return context.getString(R.string.timeshift_status_delayed, mins, secs);
    }

    private String formatPlaybackProgressLabel(long currentMs, long durationMs) {
        return formatClockLabel(currentMs) + " / " + formatClockLabel(durationMs);
    }

    private String formatClockLabel(long valueMs) {
        long totalSeconds = Math.max(0L, Math.round(valueMs / 1000f));
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private TimeshiftWindow getTimeshiftWindow() {
        if (!isTimeshiftAvailable() || player == null) {
            return null;
        }
        long durationMs = player.getDuration();
        if (durationMs == C.TIME_UNSET || durationMs <= 0L) {
            return null;
        }
        long liveEdgeMs = durationMs;
        Timeline timeline = player.getCurrentTimeline();
        if (!timeline.isEmpty()) {
            Timeline.Window window = new Timeline.Window();
            timeline.getWindow(player.getCurrentMediaItemIndex(), window);
            long defaultPositionMs = window.getDefaultPositionMs();
            if (defaultPositionMs != C.TIME_UNSET && defaultPositionMs > 0L) {
                liveEdgeMs = Math.min(durationMs, Math.max(defaultPositionMs, player.getCurrentPosition()));
            }
        }
        long endMs = liveEdgeMs;
        long startMs = Math.max(0L, endMs - TIMESHIFT_MAX_BACK_MS);
        long currentMs = Math.max(startMs, Math.min(endMs, player.getCurrentPosition()));
        return new TimeshiftWindow(startMs, endMs, currentMs);
    }

    private static final class TimeshiftWindow {
        final long startMs;
        final long endMs;
        final long currentMs;

        TimeshiftWindow(long startMs, long endMs, long currentMs) {
            this.startMs = startMs;
            this.endMs = endMs;
            this.currentMs = currentMs;
        }
    }

    private StreamInfo buildLocalStreamInfoFromRequest(PlaybackRequest request) {
        if (request == null || isBlank(request.playUrl)) {
            return null;
        }
        String drmType = safeLower(request.drmScheme);
        if (!"clearkey".equals(drmType) && !"widevine".equals(drmType)) {
            return null;
        }
        if (isSecureDrmReference(request.drmLicenseUrl)) {
            return null;
        }
        String playUrlLower = request.playUrl.toLowerCase(Locale.ROOT);
        StreamInfo info = new StreamInfo();
        info.drmType = drmType;
        info.licenseUrl = request.drmLicenseUrl;
        info.sourceUrl = request.playUrl.trim();
        info.clearKeyLicenseDataUri = request.drmLicenseUrl != null && request.drmLicenseUrl.startsWith(CLEARKEY_DATA_URI_PREFIX)
                ? request.drmLicenseUrl
                : "";
        populateFirstClearKeyFromLicenseDataUri(info, info.clearKeyLicenseDataUri);
        info.encrypted = true;
        if (playUrlLower.contains(".isml/manifest") || playUrlLower.contains(".ism/manifest")) {
            info.type = "smooth";
        } else if (playUrlLower.contains(".mpd")) {
            info.type = "dash";
        } else if (playUrlLower.contains(".m3u8")) {
            info.type = "hls";
        }
        if (isBlank(info.type)) {
            return null;
        }
        Log.d(TAG, "built local stream info channel=" + describeRequest(request) + " streamInfo=" + describeStreamInfo(info));
        return info;
    }

    private boolean hasLocalDrmInfo(PlaybackRequest request) {
        if (request == null) {
            return false;
        }
        String drmType = safeLower(request.drmScheme);
        if (!"clearkey".equals(drmType) && !"widevine".equals(drmType)) {
            return false;
        }
        return !isBlank(request.drmLicenseUrl) || !isBlank(request.playbackProfile);
    }

    static boolean isSecureDrmReference(String licenseUrl) {
        return licenseUrl != null && licenseUrl.trim().startsWith(SECURE_STREAM_LICENSE_PREFIX);
    }

    private StreamInfo fetchStreamInfo(String channelId) {
        try {
            HttpClient.Response response = httpClient.get(baseUrl + "/api/stream/" + channelId, 5000, 20000, buildPlaybackRequestHeaders());
            if (!response.isSuccessful()) {
                Log.d(TAG, "fetchStreamInfo non-success channelId=" + channelId + " code=" + response.code);
                return null;
            }

            JSONObject jsonObject = httpClient.parseObject(response.body, "cargando stream info");
            StreamInfo info = new StreamInfo();
            info.drmType = jsonObject.optString("drm_type", "").trim();
            info.licenseUrl = jsonObject.optString("license_url", "").trim();
            info.sourceUrl = jsonObject.optString("url", "").trim();
            info.type = jsonObject.optString("type", "").trim();
            info.encrypted = jsonObject.optBoolean("encrypted", false);
            JSONObject clearKeyObject = jsonObject.optJSONObject("clearkey");
            info.clearKeyLicenseDataUri = buildClearKeyLicenseDataUri(clearKeyObject);
            populateFirstClearKey(info, clearKeyObject);
            info.patchedClearKeyManifestDataUri = buildPatchedClearKeyProxyManifestDataUri(channelId, info);
            info.patchedSmoothClearKeyManifestDataUri = buildPatchedSmoothClearKeyManifestDataUri(channelId, info);
            Log.d(TAG, "fetchStreamInfo success channelId=" + channelId + " streamInfo=" + describeStreamInfo(info));
            return info;
        } catch (Exception e) {
            Log.w(TAG, "stream info fetch failed for channel " + channelId, e);
            return null;
        }
    }

    private void populateFirstClearKey(StreamInfo info, JSONObject clearKeyObject) {
        if (info == null || clearKeyObject == null || clearKeyObject.length() == 0) {
            return;
        }
        java.util.Iterator<String> iterator = clearKeyObject.keys();
        while (iterator.hasNext()) {
            String kidHex = iterator.next();
            String keyHex = clearKeyObject.optString(kidHex, "");
            if (!encodeHexAsBase64Url(kidHex).isEmpty() && !encodeHexAsBase64Url(keyHex).isEmpty()) {
                info.clearKeyKidHex = kidHex.trim();
                info.clearKeyKeyHex = keyHex.trim();
                return;
            }
        }
    }

    private void populateFirstClearKeyFromLicenseDataUri(StreamInfo info, String licenseDataUri) {
        if (info == null || isBlank(licenseDataUri) || !licenseDataUri.startsWith(CLEARKEY_DATA_URI_PREFIX)) {
            return;
        }
        try {
            String encoded = licenseDataUri.substring(CLEARKEY_DATA_URI_PREFIX.length());
            String json = new String(Base64.decode(encoded, Base64.DEFAULT), StandardCharsets.UTF_8);
            JSONArray keys = new JSONObject(json).optJSONArray("keys");
            if (keys == null || keys.length() == 0) {
                return;
            }
            JSONObject first = keys.optJSONObject(0);
            if (first == null) {
                return;
            }
            String kidHex = decodeBase64UrlAsHex(first.optString("kid", ""));
            String keyHex = decodeBase64UrlAsHex(first.optString("k", ""));
            if (!kidHex.isEmpty() && !keyHex.isEmpty()) {
                info.clearKeyKidHex = kidHex;
                info.clearKeyKeyHex = keyHex;
            }
        } catch (Exception e) {
            Log.w(TAG, "failed to parse local clearkey data uri", e);
        }
    }

    private String buildPatchedClearKeyProxyManifestDataUri(String channelId, StreamInfo info) {
        if (!"1071554".equals(channelId)
                || info == null
                || isBlank(info.sourceUrl)
                || isBlank(info.clearKeyKidHex)
                || isBlank(info.clearKeyKeyHex)
                || !"clearkey".equals(safeLower(info.drmType))) {
            return "";
        }
        try {
            HttpClient.Response response = httpClient.get(baseUrl + "/proxy/manifest/" + channelId + "?nodrm=1", 5000, 12000, java.util.Collections.singletonMap("Accept", "application/dash+xml"));
            if (!response.isSuccessful() || isBlank(response.body)) {
                Log.w(TAG, "patched clearkey manifest unavailable channelId=" + channelId + " code=" + response.code);
                return "";
            }
            String originBaseUrl = info.sourceUrl.substring(0, info.sourceUrl.lastIndexOf('/') + 1) + "6/";
            String patched = response.body.replaceAll("(?s)\\s*<BaseURL>.*?</BaseURL>", "");
            patched = rewriteDashTemplateAttribute(patched, "initialization", originBaseUrl, null, null, null);
            patched = rewriteDashTemplateAttribute(patched, "media", originBaseUrl, baseUrl + "/proxy/segment/" + channelId, info.clearKeyKidHex, info.clearKeyKeyHex);
            String encoded = Base64.encodeToString(patched.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
            Log.i(TAG, "built patched clearkey proxy manifest channelId=" + channelId + " bytes=" + patched.length());
            return "data:application/dash+xml;base64," + encoded;
        } catch (Exception e) {
            Log.w(TAG, "failed to build patched clearkey proxy manifest channelId=" + channelId, e);
            return "";
        }
    }


    private StreamInfo ensurePatchedClearKeyManifests(String channelId, StreamInfo info) {
        if (info == null) {
            return null;
        }
        if ("dash".equals(safeLower(info.type)) && isBlank(info.patchedClearKeyManifestDataUri)) {
            info.patchedClearKeyManifestDataUri = buildPatchedDirectClearKeyManifestDataUri(channelId, info);
        }
        if ("smooth".equals(safeLower(info.type))) {
            info = ensurePatchedSmoothClearKeyManifest(channelId, info);
        }
        return info;
    }

    private StreamInfo ensurePatchedClearKeyManifests(PlaybackRequest request, StreamInfo info) {
        if (request == null) {
            return ensurePatchedClearKeyManifests("", info);
        }
        if (isMovistarIsmRequest(request, info)) {
            return info;
        }
        return ensurePatchedClearKeyManifests(request.channelId, info);
    }

    private StreamInfo ensurePatchedSmoothClearKeyManifest(String channelId, StreamInfo info) {
        if (info == null || !isBlank(info.patchedSmoothClearKeyManifestDataUri)) {
            return info;
        }
        info.patchedSmoothClearKeyManifestDataUri = buildPatchedSmoothClearKeyManifestDataUri(channelId, info);
        return info;
    }

    private String buildPatchedDirectClearKeyManifestDataUri(String channelId, StreamInfo info) {
        if (info == null
                || !"clearkey".equals(safeLower(info.drmType))
                || !"dash".equals(safeLower(info.type))
                || isBlank(info.sourceUrl)
                || isBlank(info.clearKeyKidHex)
                || isBlank(info.clearKeyLicenseDataUri)) {
            return "";
        }
        if (!info.sourceUrl.toLowerCase(Locale.ROOT).contains("vfsmartcdn.gb.vodafone.es/")
                && !info.sourceUrl.toLowerCase(Locale.ROOT).contains("vfpc.gb.vodafone.es/")) {
            return "";
        }
        try {
            String localUrl = localDashManifestServer.register(channelId, info.sourceUrl, info.clearKeyKidHex);
            Log.w(TAG, "registered live dash clearkey manifest channelId=" + channelId + " url=" + localUrl);
            return localUrl;
        } catch (Exception e) {
            Log.w(TAG, "failed to build direct clearkey manifest channelId=" + channelId, e);
            return "";
        }
    }

    private String buildPatchedSmoothClearKeyManifestDataUri(String channelId, StreamInfo info) {
        if (info == null
                || !"clearkey".equals(safeLower(info.drmType))
                || !"smooth".equals(safeLower(info.type))
                || isBlank(info.sourceUrl)
                || isBlank(info.clearKeyLicenseDataUri)) {
            return "";
        }
        try {
            String localUrl = localSmoothManifestServer.register(channelId, info.sourceUrl);
            Log.w(TAG, "registered live smooth manifest channelId=" + channelId + " url=" + localUrl);
            return localUrl;
        } catch (Exception e) {
            Log.w(TAG, "failed to register local smooth manifest channelId=" + channelId, e);
            return "";
        }
    }

    private static boolean isMovistarIsmRequest(PlaybackRequest request, StreamInfo info) {
        if (request == null) {
            return false;
        }
        String platform = safeLower(request.platformName);
        String channel = safeLower(request.channelName);
        String playUrl = safeLower(request.playUrl);
        String sourceUrl = info == null ? "" : safeLower(info.sourceUrl);
        String streamType = info == null ? "" : safeLower(info.type);
        boolean looksSmooth = "smooth".equals(streamType)
                || playUrl.contains(".isml/manifest")
                || playUrl.contains(".ism/manifest")
                || sourceUrl.contains(".isml/manifest")
                || sourceUrl.contains(".ism/manifest");
        return looksSmooth
                && (platform.contains("movistar") || channel.contains("movistar"))
                && (platform.contains("ism") || channel.contains("ism") || playUrl.contains(".isml/") || sourceUrl.contains(".isml/"));
    }

    private static String rewriteDashTemplateAttribute(String manifest, String attribute, String originBaseUrl, String proxySegmentUrl, String kidHex, String keyHex) throws Exception {
        Pattern pattern = Pattern.compile(attribute + "=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(manifest);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String attributeUrl = xmlUnescape(matcher.group(1));
            String template = extractQueryParam(attributeUrl, "template");
            if (isBlank(template)) {
                matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            String replacementUrl;
            if (proxySegmentUrl == null) {
                replacementUrl = originBaseUrl + template;
            } else {
                String mediaUrl = originBaseUrl + template;
                String encodedMediaUrl = URLEncoder.encode(mediaUrl, StandardCharsets.UTF_8.name())
                        .replace("%24RepresentationID%24", "$RepresentationID$")
                        .replace("%24Time%24", "$Time$");
                replacementUrl = proxySegmentUrl
                        + "?key=" + keyHex
                        + "&kid=" + kidHex
                        + "&method=CTR"
                        + "&url=" + encodedMediaUrl;
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(attribute + "=\"" + xmlEscape(replacementUrl) + "\""));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    static String patchDashManifestForLocalClearKey(String manifest, String manifestUrl, String kidHex) {
        if (isBlank(manifest) || isBlank(kidHex)) {
            return manifest;
        }
        String patched = absolutizeDashTemplateAttributes(manifest, manifestUrl);
        String normalizedKid = kidHex.toLowerCase(Locale.ROOT).replace("-", "");
        if (normalizedKid.length() != 32) {
            return patched;
        }
        String kidDashed = normalizedKid.substring(0, 8) + "-"
                + normalizedKid.substring(8, 12) + "-"
                + normalizedKid.substring(12, 16) + "-"
                + normalizedKid.substring(16, 20) + "-"
                + normalizedKid.substring(20);
        Pattern mp4Protection = Pattern.compile("<ContentProtection[^>]*schemeIdUri=\"urn:mpeg:dash:mp4protection:2011\"[^>]*(?:/>|>)");
        Matcher matcher = mp4Protection.matcher(patched);
        StringBuffer output = new StringBuffer();
        boolean found = false;
        while (matcher.find()) {
            found = true;
            String tag = matcher.group(0);
            String lower = tag.toLowerCase(Locale.ROOT);
            if (!lower.contains("xmlns:cenc=")) {
                tag = tag.replaceFirst("<ContentProtection", "<ContentProtection xmlns:cenc=\"urn:mpeg:cenc:2013\"");
            }
            if (!lower.contains("default_kid=")) {
                if (tag.endsWith("/>")) {
                    tag = tag.substring(0, tag.length() - 2) + " cenc:default_KID=\"" + kidDashed + "\"/>";
                } else if (tag.endsWith(">")) {
                    tag = tag.substring(0, tag.length() - 1) + " cenc:default_KID=\"" + kidDashed + "\">";
                }
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(tag));
        }
        matcher.appendTail(output);
        patched = output.toString();
        if (!found) {
            Pattern adaptationSet = Pattern.compile("(<AdaptationSet[^>]*>)");
            patched = adaptationSet.matcher(patched).replaceAll("$1"
                    + "<ContentProtection value=\"cenc\" schemeIdUri=\"urn:mpeg:dash:mp4protection:2011\" xmlns:cenc=\"urn:mpeg:cenc:2013\" cenc:default_KID=\""
                    + kidDashed
                    + "\"/>");
        }
        if (!patched.contains("urn:uuid:e2719d58-a985-b3c9-781a-b030af78d30e")) {
            Pattern cencProtection = Pattern.compile("(<ContentProtection[^>]*schemeIdUri=\"urn:mpeg:dash:mp4protection:2011\"[^>]*(?:/>|>[^<]*</ContentProtection>))");
            patched = cencProtection.matcher(patched).replaceAll("$1"
                    + "<ContentProtection schemeIdUri=\"urn:uuid:e2719d58-a985-b3c9-781a-b030af78d30e\" value=\"ClearKey1.0\">"
                    + "<cenc:pssh xmlns:cenc=\"urn:mpeg:cenc:2013\">AAAAA0JUY0sAAAAA</cenc:pssh>"
                    + "</ContentProtection>");
        }
        return patched;
    }

    private static String absolutizeDashTemplateAttributes(String manifest, String manifestUrl) {
        if (isBlank(manifest) || isBlank(manifestUrl)) {
            return manifest;
        }
        String baseUrl = manifestUrl;
        int query = baseUrl.indexOf('?');
        if (query >= 0) {
            baseUrl = baseUrl.substring(0, query);
        }
        int slash = baseUrl.lastIndexOf('/');
        if (slash >= 0) {
            baseUrl = baseUrl.substring(0, slash + 1);
        }
        baseUrl = baseUrl.replace("$", "%24");
        String patched = absolutizeDashTemplateAttribute(manifest, "initialization", baseUrl);
        return absolutizeDashTemplateAttribute(patched, "media", baseUrl);
    }

    private static String absolutizeDashTemplateAttribute(String manifest, String attribute, String baseUrl) {
        Pattern pattern = Pattern.compile(attribute + "=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(manifest);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String value = xmlUnescape(matcher.group(1));
            if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("data:")) {
                matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(attribute + "=\"" + xmlEscape(baseUrl + value) + "\""));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String extractQueryParam(String url, String name) {
        int queryStart = url.indexOf('?');
        if (queryStart < 0 || queryStart >= url.length() - 1) {
            return "";
        }
        String[] params = url.substring(queryStart + 1).split("&");
        for (String param : params) {
            int equals = param.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            if (name.equals(param.substring(0, equals))) {
                try {
                    return java.net.URLDecoder.decode(param.substring(equals + 1), StandardCharsets.UTF_8.name());
                } catch (Exception e) {
                    return "";
                }
            }
        }
        return "";
    }

    private static String xmlEscape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("\"", "&quot;");
    }

    private static String xmlUnescape(String value) {
        return value == null ? "" : value.replace("&amp;", "&").replace("&quot;", "\"");
    }

    private static String buildClearKeyLicenseDataUri(JSONObject clearKeyObject) {
        if (clearKeyObject == null || clearKeyObject.length() == 0) {
            return "";
        }
        try {
            JSONArray keys = new JSONArray();
            java.util.Iterator<String> iterator = clearKeyObject.keys();
            while (iterator.hasNext()) {
                String kidHex = iterator.next();
                String keyHex = clearKeyObject.optString(kidHex, "");
                String kid = encodeHexAsBase64Url(kidHex);
                String key = encodeHexAsBase64Url(keyHex);
                if (kid.isEmpty() || key.isEmpty()) {
                    continue;
                }
                JSONObject entry = new JSONObject();
                entry.put("kty", "oct");
                entry.put("kid", kid);
                entry.put("k", key);
                keys.put(entry);
            }
            if (keys.length() == 0) {
                return "";
            }
            JSONObject license = new JSONObject();
            license.put("keys", keys);
            license.put("type", "temporary");
            String encoded = Base64.encodeToString(license.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), Base64.NO_WRAP);
            return "data:application/json;base64," + encoded;
        } catch (Exception e) {
            Log.w(TAG, "failed to build clearkey data uri", e);
            return "";
        }
    }

    private static String encodeHexAsBase64Url(String hex) {
        if (hex == null) {
            return "";
        }
        String normalized = hex.trim();
        if (normalized.length() == 0 || normalized.length() % 2 != 0) {
            return "";
        }
        try {
            byte[] bytes = new byte[normalized.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                int index = i * 2;
                bytes[i] = (byte) Integer.parseInt(normalized.substring(index, index + 2), 16);
            }
            return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    private static String decodeBase64UrlAsHex(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        try {
            byte[] bytes = Base64.decode(value.trim(), Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            }
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void updatePlaybackRequestHeaders() {
        if (httpDataSourceFactory != null) {
            httpDataSourceFactory.setDefaultRequestProperties(buildPlaybackRequestHeaders());
        }
    }

    private String appendOfflineAccessToken(String url) {
        if (url == null || url.trim().isEmpty() || catalogSnapshotStore == null) {
            return url;
        }
        String trimmed = url.trim();
        if (!shouldAttachOfflineAccessToken(trimmed)) {
            return trimmed;
        }
        String token = catalogSnapshotStore.getAccessToken();
        String deviceId = catalogSnapshotStore.getDeviceId();
        if ((token == null || token.trim().isEmpty()) && (deviceId == null || deviceId.trim().isEmpty())) {
            return trimmed;
        }
        Uri.Builder builder = Uri.parse(trimmed).buildUpon();
        if (!trimmed.contains("access_token=") && token != null && !token.trim().isEmpty()) {
            builder.appendQueryParameter("access_token", token.trim());
        }
        if (!trimmed.contains("device_id=") && deviceId != null && !deviceId.trim().isEmpty()) {
            builder.appendQueryParameter("device_id", deviceId.trim());
        }
        return builder.build().toString();
    }

    private boolean shouldAttachOfflineAccessToken(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String normalizedBase = baseUrl == null ? "" : baseUrl.trim();
        if (!normalizedBase.isEmpty() && url.startsWith(normalizedBase)) {
            return true;
        }
        Uri uri;
        try {
            uri = Uri.parse(url);
        } catch (Exception ignored) {
            return false;
        }
        String host = uri.getHost();
        String path = uri.getPath();
        if (host == null || path == null) {
            return false;
        }
        String lowerHost = host.trim().toLowerCase(Locale.ROOT);
        if (!"fire.tvbep.com".equals(lowerHost) && !"iptv.bepllorens.com".equals(lowerHost)) {
            return false;
        }
        return path.startsWith("/proxy/")
                || path.startsWith("/drm/")
                || path.startsWith("/hls/")
                || path.startsWith("/ios/")
                || path.startsWith("/live/")
                || path.startsWith("/api/stream/")
                || path.startsWith("/api/clearkey/")
                || path.startsWith("/api/widevine/")
                || path.startsWith("/api/u7d/")
                || path.startsWith("/api/offline/u7d/")
                || path.startsWith("/api/vod/");
    }

    private boolean isBackendLivePlaybackUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String trimmed = url.trim();
        String normalizedBase = baseUrl == null ? "" : baseUrl.trim();
        return !normalizedBase.isEmpty() && trimmed.startsWith(normalizedBase + "/live/");
    }

    private Map<String, String> buildPlaybackRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "*/*");
        if (catalogSnapshotStore != null) {
            String token = catalogSnapshotStore.getAccessToken();
            if (token != null && !token.trim().isEmpty()) {
                headers.put("Authorization", "Bearer " + token.trim());
                headers.put("X-DRBEP-Access-Token", token.trim());
            }
            String deviceId = catalogSnapshotStore.getDeviceId();
            if (deviceId != null && !deviceId.trim().isEmpty()) {
                headers.put("X-DRBEP-Device-Id", deviceId.trim());
            }
        }
        return headers;
    }

    private static String describeRequest(PlaybackRequest request) {
        if (request == null) {
            return "null";
        }
        return "{" + request.channelId + "," + safeLogValue(request.channelName) + "}";
    }

    private static String describeDecision(PlaybackRouteResolver.Decision decision) {
        if (decision == null) {
            return "null";
        }
        return "{target=" + shortenUrl(decision.targetUrl)
                + ",mime=" + safeLogValue(decision.mimeType)
                + ",drm=" + safeLogValue(decision.drmType)
                + ",mode=" + safeLogValue(decision.playbackMode)
                + ",fallback=" + decision.useFallback
                + ",allowCompat=" + decision.allowCompatibilityFallback
                + "}";
    }

    private static String describeStreamInfo(StreamInfo streamInfo) {
        if (streamInfo == null) {
            return "null";
        }
        return "{drm=" + safeLogValue(streamInfo.drmType)
                + ",type=" + safeLogValue(streamInfo.type)
                + ",encrypted=" + streamInfo.encrypted
                + ",license=" + shortenUrl(streamInfo.licenseUrl)
                + ",source=" + shortenUrl(streamInfo.sourceUrl)
                + ",clearKeyData=" + (!isBlank(streamInfo.clearKeyLicenseDataUri))
                + ",patchedSmooth=" + (!isBlank(streamInfo.patchedSmoothClearKeyManifestDataUri))
                + "}";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String describeRouteLabel(PlaybackRouteResolver.Decision decision) {
        if (decision == null) {
            return context.getString(R.string.diagnostics_state_idle);
        }
        boolean directRequest = currentRequest != null && currentRequest.directPlayback;
        boolean directTarget = isDirectDecisionTarget(decision);
        if (decision.useFallback) {
            return context.getString(R.string.diagnostics_route_compat);
        }
        if (directRequest || directTarget) {
            if (MimeTypes.APPLICATION_M3U8.equals(decision.mimeType)) {
                return context.getString(R.string.diagnostics_route_direct_hls);
            }
            if (MimeTypes.APPLICATION_MPD.equals(decision.mimeType)) {
                return context.getString(R.string.diagnostics_route_direct_dash);
            }
            if (MimeTypes.APPLICATION_SS.equals(decision.mimeType)) {
                return "Directo Smooth";
            }
            if ("widevine".equals(decision.drmType) || "clearkey".equals(decision.drmType)) {
                return "Directo DRM";
            }
        }
        if (isRuntimeManifestOnlyRoute(decision)) {
            return context.getString(R.string.diagnostics_route_direct_hls);
        }
        if (isBackendVideoRoute(decision)) {
            return context.getString(R.string.diagnostics_route_server_hls);
        }
        if ("widevine".equals(decision.drmType) || "clearkey".equals(decision.drmType)) {
            return context.getString(R.string.diagnostics_route_proxy_drm);
        }
        if (decision.targetUrl != null && decision.targetUrl.contains("?nodrm=1")) {
            return context.getString(R.string.diagnostics_route_proxy_clear);
        }
        if (decision.targetUrl != null && decision.targetUrl.contains("/proxy/manifest/")) {
            return context.getString(R.string.diagnostics_route_proxy_auto);
        }
        if (MimeTypes.APPLICATION_M3U8.equals(decision.mimeType)) {
            return context.getString(R.string.diagnostics_route_direct_hls);
        }
        if (MimeTypes.APPLICATION_MPD.equals(decision.mimeType)) {
            return context.getString(R.string.diagnostics_route_direct_dash);
        }
        if (MimeTypes.APPLICATION_SS.equals(decision.mimeType)) {
            return "Directo Smooth";
        }
        return context.getString(R.string.diagnostics_route_direct_generic);
    }

    private boolean isRuntimeManifestOnlyRoute(PlaybackRouteResolver.Decision decision) {
        if (decision == null || decision.targetUrl == null || currentRequest == null) {
            return false;
        }
        String target = decision.targetUrl.trim().toLowerCase(Locale.ROOT);
        if (!target.contains("/proxy/manifest/") && !target.contains("/api/vod/runtime/stream/")) {
            return false;
        }
        String platform = safeLower(currentRequest.platformName);
        String name = safeLower(currentRequest.channelName);
        return platform.contains("runtime") || name.contains("runtime");
    }

    private boolean isBackendVideoRoute(PlaybackRouteResolver.Decision decision) {
        if (decision == null || decision.targetUrl == null) {
            return false;
        }
        String target = decision.targetUrl.trim().toLowerCase(Locale.ROOT);
        return target.contains("/hls/")
                || target.contains("/live/")
                || target.contains("/drm/")
                || target.contains("/api/vod/movistar/")
                || target.contains("/api/u7d/movistar/")
                || target.contains("/api/offline/u7d/");
    }

    private boolean isDirectDecisionTarget(PlaybackRouteResolver.Decision decision) {
        if (decision == null || decision.targetUrl == null || decision.targetUrl.trim().isEmpty()) {
            return false;
        }
        String target = decision.targetUrl.trim().toLowerCase(Locale.ROOT);
        Uri targetUri = Uri.parse(target);
        String targetHost = targetUri == null ? "" : safeLower(targetUri.getHost());
        Uri backendUri = Uri.parse(baseUrl);
        String backendHost = backendUri == null ? "" : safeLower(backendUri.getHost());
        boolean backendHosted = !targetHost.isEmpty()
                && (targetHost.equals(backendHost)
                || targetHost.contains("fire.tvbep.com")
                || targetHost.contains("iptv.bepllorens.com"));
        if (!backendHosted) {
            return true;
        }
        return !target.contains("/proxy/")
                && !target.contains("/recordings/")
                && !target.contains("/api/remux/")
                && !target.contains("/api/proxy/")
                && !target.contains("/live/")
                && !target.contains("/hls/")
                && !target.contains("/drm/")
                && !target.contains("/api/vod/movistar/")
                && !target.contains("/api/u7d/movistar/")
                && !target.contains("/api/offline/u7d/");
    }

    private static String playbackStateToString(int state) {
        switch (state) {
            case Player.STATE_IDLE:
                return "IDLE";
            case Player.STATE_BUFFERING:
                return "BUFFERING";
            case Player.STATE_READY:
                return "READY";
            case Player.STATE_ENDED:
                return "ENDED";
            default:
                return String.valueOf(state);
        }
    }

    private static String shortenUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }
        String sanitized = DiagnosticRedactor.sanitizeUrl(url);
        if (sanitized.length() <= 120) {
            return sanitized;
        }
        return sanitized.substring(0, 117) + "...";
    }

    private static String safeLogValue(String value) {
        return DiagnosticRedactor.redactSensitiveText(value);
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
