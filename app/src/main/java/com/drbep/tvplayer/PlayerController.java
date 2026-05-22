package com.drbep.tvplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider;
import androidx.media3.exoplayer.drm.DrmSessionManager;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PlayerController {
    private static final String TAG = "PlayerController";
    private static final String PREFS = "drbep_tv_prefs";
    private static final String CLEARKEY_DATA_URI_PREFIX = "data:application/json;base64,";
    private static final int PLAYBACK_CONNECT_TIMEOUT_MS = 20_000;
    private static final int PLAYBACK_READ_TIMEOUT_MS = 30_000;
    private static final long TIMESHIFT_MAX_BACK_MS = 2L * 60L * 60L * 1000L;
    private static final long TIMESHIFT_SEEK_STEP_MS = 30_000L;


    interface Host {
        void showStatus(String text);

        void showError(String text);

        void hideError();

        boolean isChannelCurrent(String channelId);

        void showHdrBadge(String label);

        boolean isPlaybackRepairEnabled();

        void recordPlaybackError(PlaybackRequest request, PlaybackDiagnostics diagnostics);

        void onFirstVideoFrameRendered(String channelId);
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

        PlaybackRequest(String channelId, String channelName, String platformName, String playUrl, String fallbackPlayUrl, String playbackMode, String drmScheme, String drmLicenseUrl, boolean directPlayback) {
            this.channelId = channelId;
            this.channelName = channelName;
            this.platformName = platformName == null ? "" : platformName.trim();
            this.playUrl = playUrl;
            this.fallbackPlayUrl = fallbackPlayUrl;
            this.playbackMode = playbackMode;
            this.drmScheme = drmScheme == null ? "" : drmScheme.trim();
            this.drmLicenseUrl = drmLicenseUrl == null ? "" : drmLicenseUrl.trim();
            this.directPlayback = directPlayback;
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
        String sourceUrl;
        String type;
        boolean encrypted;
    }

    static final class PlaybackDiagnostics {
        final String channelName;
        final String playbackState;
        final String routeLabel;
        final String targetUrl;
        final String mimeType;
        final String drmType;
        final String playbackMode;
        final boolean encrypted;
        final boolean usingFallback;
        final String lastError;

        PlaybackDiagnostics(String channelName, String playbackState, String routeLabel, String targetUrl, String mimeType, String drmType, String playbackMode, boolean encrypted, boolean usingFallback, String lastError) {
            this.channelName = channelName;
            this.playbackState = playbackState;
            this.routeLabel = routeLabel;
            this.targetUrl = targetUrl;
            this.mimeType = mimeType;
            this.drmType = drmType;
            this.playbackMode = playbackMode;
            this.encrypted = encrypted;
            this.usingFallback = usingFallback;
            this.lastError = lastError;
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

    private DefaultTrackSelector trackSelector;
    private DefaultHttpDataSource.Factory httpDataSourceFactory;
    private ExoPlayer player;
    private PlaybackRequest currentRequest;
    private StreamInfo currentStreamInfo;
    private PlaybackRouteResolver.Decision currentPlaybackDecision;
    private boolean usingPlaybackFallback;
    private final Set<String> attemptedRecoveryRoutes = new HashSet<>();
    private String currentRecordingUrl;
    private String lastPlaybackState = "IDLE";
    private String lastErrorSummary;
    private String lastHdrBadgeChannelId;
    private boolean forceLiveEdgeOnNextReady;
    private boolean usingVideoCompatibilityCap;
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
    }

    void initialize() {
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
                        .setBufferDurationsMs(20_000, 75_000, 4_000, 8_000)
                        .build())
                .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory)
                        .setDrmSessionManagerProvider(createDrmSessionManagerProvider()))
                .setSeekBackIncrementMs(TIMESHIFT_SEEK_STEP_MS)
                .setSeekForwardIncrementMs(TIMESHIFT_SEEK_STEP_MS)
                .build();
        playerView.setPlayer(player);
        playerView.setUseController(false);
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
                host.showError(message);
                host.recordPlaybackError(request, getPlaybackDiagnostics());
                Log.w(TAG, message, error);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                lastPlaybackState = playbackStateToString(playbackState);
                Log.d(TAG, "playbackState=" + playbackStateToString(playbackState)
                        + " channel=" + describeRequest(currentRequest)
                        + " decision=" + describeDecision(currentPlaybackDecision)
                        + " playWhenReady=" + (player != null && player.getPlayWhenReady()));
                if (playbackState == Player.STATE_BUFFERING) {
                    if (currentRequest != null && currentRequest.directPlayback) {
                        String mimeType = currentPlaybackDecision == null ? "" : currentPlaybackDecision.mimeType;
                        if (mimeType != null && mimeType.toLowerCase(java.util.Locale.ROOT).contains("mpegurl")) {
                            host.showStatus(context.getString(R.string.vod_status_detecting_hls));
                        } else if (mimeType != null && mimeType.toLowerCase(java.util.Locale.ROOT).contains("mpd")) {
                            host.showStatus(context.getString(R.string.vod_status_detecting_dash));
                        } else {
                            host.showStatus(context.getString(R.string.vod_status_detecting_stream));
                        }
                    } else {
                        host.showStatus(context.getString(R.string.status_buffering));
                    }
                } else if (playbackState == Player.STATE_READY) {
                    if (forceLiveEdgeOnNextReady && isTimeshiftAvailable()) {
                        player.seekToDefaultPosition();
                        player.play();
                        uiHandler.removeCallbacks(forceLiveEdgeRunnable);
                        uiHandler.postDelayed(forceLiveEdgeRunnable, 900L);
                        forceLiveEdgeOnNextReady = false;
                        Log.d(TAG, "forced live edge on ready for channel=" + describeRequest(currentRequest));
                    }
                    host.hideError();
                    if (currentRequest != null && currentRequest.directPlayback) {
                        host.showStatus(context.getString(R.string.vod_status_ready, currentRequest.channelName));
                    } else {
                        host.showStatus(currentRequest != null && currentRequest.channelName != null && !currentRequest.channelName.trim().isEmpty()
                                ? currentRequest.channelName
                                : context.getString(R.string.status_ready));
                    }
                    maybeShowHdrBadge();
                }
            }

            @Override
            public void onRenderedFirstFrame() {
                PlaybackRequest request = currentRequest;
                host.onFirstVideoFrameRendered(request == null ? "" : request.channelId);
            }
        });
    }

    void resetFallbackState() {
        usingPlaybackFallback = false;
        usingVideoCompatibilityCap = false;
        attemptedRecoveryRoutes.clear();
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
        if (!usingVideoCompatibilityCap && shouldRetryWithVideoCompatibilityCap(request, decision, error)) {
            usingVideoCompatibilityCap = true;
            attemptedRecoveryRoutes.add(routeAttemptKey(decision) + "|video720");
            Log.w(TAG, "retrying playback with 720p video compatibility cap channel=" + describeRequest(request)
                    + " decision=" + describeDecision(decision));
            host.showStatus(context.getString(R.string.status_playback_repair_trying, formatPlaybackModeLabel(decision.playbackMode)));
            playChannelInternal(request, true, usingPlaybackFallback, currentStreamInfo);
            return true;
        }
        if (BuildConfig.STANDALONE_MODE) {
            return false;
        }
        if (decision.allowCompatibilityFallback && !usingPlaybackFallback && request.hasFallback()) {
            usingPlaybackFallback = true;
            attemptedRecoveryRoutes.add(routeAttemptKey(decision));
            Log.w(TAG, "retrying compatibility fallback for channel=" + describeRequest(request));
            host.showStatus(context.getString(R.string.status_retry_compat));
            playChannelInternal(request, true, true, currentStreamInfo);
            return true;
        }
        String playbackMode = request.playbackMode == null || request.playbackMode.trim().isEmpty() ? PlaybackModeStore.MODE_AUTO : request.playbackMode;
        if (!PlaybackModeStore.MODE_AUTO.equals(playbackMode)) {
            return false;
        }
        attemptedRecoveryRoutes.add(routeAttemptKey(decision));
        PlaybackRequest[] alternatives = new PlaybackRequest[]{
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
            playChannelInternal(alternative, true, false, currentStreamInfo);
            return true;
        }
        return false;
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
                request.directPlayback
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
        return new PlaybackDiagnostics(
                channelName,
                lastPlaybackState,
                routeLabel,
                targetUrl,
                mimeType,
                drmType,
            playbackMode,
                encrypted,
                usingPlaybackFallback,
                safeLogValue(lastErrorSummary)
        );
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
        playChannelInternal(request, autoPlay, false, streamInfo, resumePositionMs);
    }

    void resolveStreamInfoAndReplayIfNeeded(PlaybackRequest request, boolean autoPlay, Map<String, StreamInfo> streamInfoCache) {
        resolveStreamInfoAndReplayIfNeeded(request, autoPlay, streamInfoCache, 0L);
    }

    void resolveStreamInfoAndReplayIfNeeded(PlaybackRequest request, boolean autoPlay, Map<String, StreamInfo> streamInfoCache, long resumePositionMs) {
        if (request == null || request.directPlayback || request.channelId == null || request.channelId.trim().isEmpty()) {
            return;
        }

        final String channelId = request.channelId.trim();
        ioExecutor.execute(() -> {
            StreamInfo info = streamInfoCache.get(channelId);
            boolean fromCache = info != null;
            if (info == null) {
                info = fetchStreamInfo(channelId);
                if (info != null) {
                    streamInfoCache.put(channelId, info);
                }
            }
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
                if (!host.isChannelCurrent(channelId)) {
                    Log.d(TAG, "resolveStreamInfo ignored because channel changed: channelId=" + channelId);
                    return;
                }
                PlaybackRouteResolver.Decision resolvedDecision = buildPlaybackDecision(request, false, resolved);
                if (!requiresReplay && resolvedDecision.isEquivalentTo(currentPlaybackDecision)) {
                    Log.d(TAG, "resolveStreamInfo no replay needed channel=" + describeRequest(request)
                            + " resolvedDecision=" + describeDecision(resolvedDecision));
                    return;
                }
                Log.i(TAG, "resolveStreamInfo replaying channel=" + describeRequest(request)
                        + " requiresReplay=" + requiresReplay
                        + " previousDecision=" + describeDecision(currentPlaybackDecision)
                        + " resolvedDecision=" + describeDecision(resolvedDecision));
                playChannelInternal(request, autoPlay, false, resolved, resumePositionMs);
                if ("widevine".equals(safeLower(resolved.drmType))) {
                    host.showStatus(context.getString(R.string.status_channel_widevine, request.channelName));
                }
            });
        });
    }

    void playChannelAfterResolvingStreamInfo(PlaybackRequest request, boolean autoPlay, Map<String, StreamInfo> streamInfoCache, long resumePositionMs) {
        if (request == null || request.directPlayback || request.channelId == null || request.channelId.trim().isEmpty()) {
            playChannel(request, autoPlay, null, resumePositionMs);
            return;
        }
        final String channelId = request.channelId.trim();
        ioExecutor.execute(() -> {
            StreamInfo info = streamInfoCache == null ? null : streamInfoCache.get(channelId);
            boolean fromCache = info != null;
            if (info == null) {
                info = fetchStreamInfo(channelId);
                if (info != null && streamInfoCache != null) {
                    streamInfoCache.put(channelId, info);
                }
            }
            StreamInfo resolved = info;
            Log.d(TAG, "playChannelAfterResolvingStreamInfo channelId=" + channelId
                    + " fromCache=" + fromCache
                    + " streamInfo=" + describeStreamInfo(resolved));
            uiHandler.post(() -> {
                if (!host.isChannelCurrent(channelId)) {
                    Log.d(TAG, "playChannelAfterResolvingStreamInfo ignored because channel changed: channelId=" + channelId);
                    return;
                }
                playChannelInternal(request, autoPlay, false, resolved, resumePositionMs);
                if (resolved != null && "widevine".equals(safeLower(resolved.drmType))) {
                    host.showStatus(context.getString(R.string.status_channel_widevine, request.channelName));
                }
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
        currentRecordingUrl = recordingUrl;
        usingPlaybackFallback = false;
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
        playChannelInternal(request, autoPlay, useFallback, streamInfo, 0L);
    }

    private void playChannelInternal(PlaybackRequest request, boolean autoPlay, boolean useFallback, StreamInfo streamInfo, long resumePositionMs) {
        if (request == null || player == null) {
            return;
        }

        uiHandler.removeCallbacks(forceLiveEdgeRunnable);
        PlaybackRequest previousRequest = currentRequest;
        currentRequest = request;
        currentStreamInfo = streamInfo;
        currentRecordingUrl = null;
        usingPlaybackFallback = useFallback;
        if (!isSameChannel(request, previousRequest)) {
            usingVideoCompatibilityCap = false;
        }
        lastErrorSummary = null;
        lastHdrBadgeChannelId = null;
        forceLiveEdgeOnNextReady = request != null
                && request.platformName != null
                && request.platformName.toLowerCase(Locale.ROOT).contains("movistar");
        PlaybackRouteResolver.Decision decision = buildPlaybackDecision(request, useFallback, streamInfo);
        currentPlaybackDecision = decision;
        Log.d(TAG, "playChannelInternal channel=" + describeRequest(request)
            + " autoPlay=" + autoPlay
            + " requestedFallback=" + useFallback
            + " decision=" + describeDecision(decision)
            + " streamInfo=" + describeStreamInfo(streamInfo));

        if (decision.targetUrl == null || decision.targetUrl.trim().isEmpty()) {
            host.showError(context.getString(R.string.error_empty_playback_url));
            return;
        }
        updatePlaybackRequestHeaders();
        applyVideoTrackPolicy(request, decision);

        String mediaTargetUrl = appendOfflineAccessToken(decision.targetUrl);
        MediaItem.Builder builder = new MediaItem.Builder().setUri(mediaTargetUrl);
        if (isHevcHlsDecision(decision)) {
            builder.setLiveConfiguration(new MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(18_000)
                    .setMinOffsetMs(12_000)
                    .setMaxOffsetMs(30_000)
                    .setMinPlaybackSpeed(0.97f)
                    .setMaxPlaybackSpeed(1.02f)
                    .build());
        }
        if (decision.mimeType != null && !decision.mimeType.trim().isEmpty()) {
            builder.setMimeType(decision.mimeType);
        }
        if ("widevine".equals(decision.drmType)) {
            String licenseUrl = resolveWidevineLicenseUrl(request);
            builder.setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(appendOfflineAccessToken(licenseUrl))
                    .build());
        } else if ("clearkey".equals(decision.drmType)) {
            String licenseUrl = request.drmLicenseUrl != null && !request.drmLicenseUrl.trim().isEmpty()
                    ? request.drmLicenseUrl
                    : streamInfo != null && streamInfo.clearKeyLicenseDataUri != null && !streamInfo.clearKeyLicenseDataUri.trim().isEmpty()
                    ? streamInfo.clearKeyLicenseDataUri
                    : streamInfo != null && streamInfo.licenseUrl != null && !streamInfo.licenseUrl.trim().isEmpty()
                    ? streamInfo.licenseUrl
                    : baseUrl + "/api/clearkey/" + request.channelId;
            builder.setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                    .setLicenseUri(appendOfflineAccessToken(licenseUrl))
                    .build());
        }

        Log.i(TAG, "preparePlayback channel=" + describeRequest(request)
            + " decision=" + describeDecision(decision)
            + " streamInfo=" + describeStreamInfo(streamInfo)
            + " resumeMs=" + resumePositionMs);
        player.setMediaItem(builder.build());
        if (resumePositionMs > 0L) {
            player.seekTo(resumePositionMs);
        }
        player.prepare();
        player.setPlayWhenReady(autoPlay);

        if (request.directPlayback) {
            String platform = request.platformName == null ? "" : request.platformName.toLowerCase(java.util.Locale.ROOT);
            if (platform.contains("runtime")) {
                host.showStatus(context.getString(R.string.vod_status_opening_runtime));
            } else if (platform.contains("tivify")) {
                host.showStatus(context.getString(R.string.vod_status_opening_tivify));
            } else {
                host.showStatus(context.getString(R.string.vod_status_preparing, request.channelName));
            }
        } else {
            host.showStatus(useFallback
                    ? context.getString(R.string.status_channel_compat, request.channelName)
                    : request.channelName);
        }
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
        boolean capForCompatibility = usingVideoCompatibilityCap || isKnownClearKeyDecoderSensitiveChannel(request, decision);
        DefaultTrackSelector.Parameters.Builder builder = trackSelector.buildUponParameters()
                .setForceHighestSupportedBitrate(true);
        if (capForCompatibility) {
            builder.setMaxVideoSize(1280, 720);
            Log.i(TAG, "using 720p video compatibility cap channel=" + describeRequest(request)
                    + " decision=" + describeDecision(decision));
        } else {
            builder.clearVideoSizeConstraints();
        }
        trackSelector.setParameters(builder);
    }

    private boolean isKnownClearKeyDecoderSensitiveChannel(PlaybackRequest request, PlaybackRouteResolver.Decision decision) {
        if (request == null || decision == null || !"clearkey".equals(safeLower(decision.drmType))) {
            return false;
        }
        return "1079794".equals(request.channelId)
                || "1079795".equals(request.channelId)
                || "1079796".equals(request.channelId);
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
        String normalizedBase = baseUrl == null ? "" : baseUrl.trim();
        if (normalizedBase.isEmpty() || !trimmed.startsWith(normalizedBase)) {
            return trimmed;
        }
        if (trimmed.contains("access_token=")) {
            return trimmed;
        }
        String token = catalogSnapshotStore.getAccessToken();
        if (token == null || token.trim().isEmpty()) {
            return trimmed;
        }
        String separator = trimmed.contains("?") ? "&" : "?";
        return trimmed + separator + "access_token=" + Uri.encode(token.trim());
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
                + "}";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String describeRouteLabel(PlaybackRouteResolver.Decision decision) {
        if (decision == null) {
            return context.getString(R.string.diagnostics_state_idle);
        }
        if (decision.useFallback) {
            return context.getString(R.string.diagnostics_route_compat);
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
        return context.getString(R.string.diagnostics_route_direct_generic);
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
        if (url.length() <= 120) {
            return url;
        }
        return url.substring(0, 117) + "...";
    }

    private static String safeLogValue(String value) {
        return value == null ? "" : value;
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
