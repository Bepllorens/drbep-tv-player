package com.drbep.tvplayer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;

final class PlayerController {
    private static final String TAG = "PlayerController";

    interface Host {
        void showStatus(String text);

        void showError(String text);

        void hideError();

        boolean isChannelCurrent(String channelId);
    }

    static final class PlaybackRequest {
        final String channelId;
        final String channelName;
        final String playUrl;
        final String fallbackPlayUrl;
        final String playbackMode;

        PlaybackRequest(String channelId, String channelName, String playUrl, String fallbackPlayUrl, String playbackMode) {
            this.channelId = channelId;
            this.channelName = channelName;
            this.playUrl = playUrl;
            this.fallbackPlayUrl = fallbackPlayUrl;
            this.playbackMode = playbackMode;
        }

        boolean hasFallback() {
            return fallbackPlayUrl != null && !fallbackPlayUrl.trim().isEmpty();
        }
    }

    static final class StreamInfo {
        String drmType;
        String licenseUrl;
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

    private static final class PlaybackDecision {
        final String targetUrl;
        final String mimeType;
        final String drmType;
        final String playbackMode;
        final boolean useFallback;
        final boolean allowCompatibilityFallback;

        PlaybackDecision(String targetUrl, String mimeType, String drmType, String playbackMode, boolean useFallback, boolean allowCompatibilityFallback) {
            this.targetUrl = targetUrl;
            this.mimeType = mimeType;
            this.drmType = drmType;
            this.playbackMode = playbackMode;
            this.useFallback = useFallback;
            this.allowCompatibilityFallback = allowCompatibilityFallback;
        }

        boolean isEquivalentTo(PlaybackDecision other) {
            if (other == null) {
                return false;
            }
            return equalsNullable(targetUrl, other.targetUrl)
                    && equalsNullable(mimeType, other.mimeType)
                    && equalsNullable(drmType, other.drmType)
                    && equalsNullable(playbackMode, other.playbackMode)
                    && useFallback == other.useFallback;
        }

        private static boolean equalsNullable(String left, String right) {
            if (left == null) {
                return right == null;
            }
            return left.equals(right);
        }
    }

    private final Context context;
    private final PlayerView playerView;
    private final String baseUrl;
    private final ExecutorService ioExecutor;
    private final Handler uiHandler;
    private final Host host;
    private final HttpClient httpClient;

    private ExoPlayer player;
    private PlaybackRequest currentRequest;
    private StreamInfo currentStreamInfo;
    private PlaybackDecision currentPlaybackDecision;
    private boolean usingPlaybackFallback;
    private String lastPlaybackState = "IDLE";
    private String lastErrorSummary;

    PlayerController(Context context, PlayerView playerView, String baseUrl, ExecutorService ioExecutor, Handler uiHandler, Host host) {
        this.context = context;
        this.playerView = playerView;
        this.baseUrl = baseUrl;
        this.ioExecutor = ioExecutor;
        this.uiHandler = uiHandler;
        this.host = host;
        this.httpClient = new HttpClient();
    }

    void initialize() {
        player = new ExoPlayer.Builder(context).build();
        playerView.setPlayer(player);
        playerView.setUseController(false);
        playerView.setKeepScreenOn(true);
        playerView.setFocusable(true);
        playerView.setFocusableInTouchMode(true);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                PlaybackRequest request = currentRequest;
                PlaybackDecision decision = currentPlaybackDecision;
                Log.w(TAG, "onPlayerError channel=" + describeRequest(request)
                        + " decision=" + describeDecision(decision)
                        + " streamInfo=" + describeStreamInfo(currentStreamInfo)
                    + " errorCode=" + PlaybackException.getErrorCodeName(error.errorCode)
                        + " message=" + safeLogValue(error.getMessage()), error);
                if (decision != null && decision.allowCompatibilityFallback && !usingPlaybackFallback && request != null && request.hasFallback()) {
                    usingPlaybackFallback = true;
                    Log.w(TAG, "retrying compatibility fallback for channel=" + describeRequest(request));
                    host.showStatus(context.getString(R.string.status_retry_compat));
                    playChannelInternal(request, true, true, currentStreamInfo);
                    return;
                }

                String message = context.getString(R.string.error_playback_message, error.getMessage());
                lastErrorSummary = message;
                host.showError(message);
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
                    host.showStatus(context.getString(R.string.status_buffering));
                } else if (playbackState == Player.STATE_READY) {
                    host.hideError();
                    host.showStatus(currentRequest != null && currentRequest.channelName != null && !currentRequest.channelName.trim().isEmpty()
                            ? currentRequest.channelName
                            : context.getString(R.string.status_ready));
                }
            }
        });
    }

    void resetFallbackState() {
        usingPlaybackFallback = false;
        Log.d(TAG, "compatibility fallback state reset");
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

    boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    void togglePlayback() {
        if (player == null) {
            return;
        }
        boolean playing = player.isPlaying();
        player.setPlayWhenReady(!playing);
        host.showStatus(context.getString(playing ? R.string.status_paused : R.string.status_playing));
    }

    void playChannel(PlaybackRequest request, boolean autoPlay, StreamInfo streamInfo) {
        Log.d(TAG, "playChannel request=" + describeRequest(request)
                + " autoPlay=" + autoPlay
                + " initialStreamInfo=" + describeStreamInfo(streamInfo));
        playChannelInternal(request, autoPlay, false, streamInfo);
    }

    void resolveStreamInfoAndReplayIfNeeded(PlaybackRequest request, boolean autoPlay, Map<String, StreamInfo> streamInfoCache) {
        if (request == null || request.channelId == null || request.channelId.trim().isEmpty()) {
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

            boolean requiresReplay = "widevine".equals(safeLower(info.drmType)) || info.encrypted;
            StreamInfo resolved = info;
            uiHandler.post(() -> {
                if (!host.isChannelCurrent(channelId)) {
                    Log.d(TAG, "resolveStreamInfo ignored because channel changed: channelId=" + channelId);
                    return;
                }
                PlaybackDecision resolvedDecision = buildPlaybackDecision(request, false, resolved);
                if (!requiresReplay && resolvedDecision.isEquivalentTo(currentPlaybackDecision)) {
                    Log.d(TAG, "resolveStreamInfo no replay needed channel=" + describeRequest(request)
                            + " resolvedDecision=" + describeDecision(resolvedDecision));
                    return;
                }
                Log.i(TAG, "resolveStreamInfo replaying channel=" + describeRequest(request)
                        + " requiresReplay=" + requiresReplay
                        + " previousDecision=" + describeDecision(currentPlaybackDecision)
                        + " resolvedDecision=" + describeDecision(resolvedDecision));
                playChannelInternal(request, autoPlay, false, resolved);
                if ("widevine".equals(safeLower(resolved.drmType))) {
                    host.showStatus(context.getString(R.string.status_channel_widevine, request.channelName));
                }
            });
        });
    }

    void playRecording(String recordingName, String recordingUrl) {
        if (player == null || recordingUrl == null || recordingUrl.trim().isEmpty()) {
            return;
        }

        Log.i(TAG, "playRecording name=" + safeLogValue(recordingName)
                + " url=" + shortenUrl(recordingUrl)
                + " mime=" + safeLogValue(inferMimeType(recordingUrl)));

        String mimeType = inferMimeType(recordingUrl);
        MediaItem.Builder builder = new MediaItem.Builder().setUri(recordingUrl);
        if (mimeType != null && !mimeType.trim().isEmpty()) {
            builder.setMimeType(mimeType);
        }

        currentRequest = null;
        currentStreamInfo = null;
        usingPlaybackFallback = false;
        player.setMediaItem(builder.build());
        player.prepare();
        player.setPlayWhenReady(true);
        host.hideError();
        host.showStatus(context.getString(R.string.status_playing_recording, recordingName));
    }

    void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void playChannelInternal(PlaybackRequest request, boolean autoPlay, boolean useFallback, StreamInfo streamInfo) {
        if (request == null || player == null) {
            return;
        }

        currentRequest = request;
        currentStreamInfo = streamInfo;
        usingPlaybackFallback = useFallback;
        lastErrorSummary = null;
        PlaybackDecision decision = buildPlaybackDecision(request, useFallback, streamInfo);
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

        MediaItem.Builder builder = new MediaItem.Builder().setUri(decision.targetUrl);
        if (decision.mimeType != null && !decision.mimeType.trim().isEmpty()) {
            builder.setMimeType(decision.mimeType);
        }
        if ("widevine".equals(decision.drmType)) {
            builder.setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(baseUrl + "/api/widevine/" + request.channelId)
                    .build());
        } else if ("clearkey".equals(decision.drmType)) {
            builder.setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                    .setLicenseUri(baseUrl + "/api/clearkey/" + request.channelId)
                    .build());
        }

        Log.i(TAG, "preparePlayback channel=" + describeRequest(request)
            + " decision=" + describeDecision(decision)
            + " streamInfo=" + describeStreamInfo(streamInfo));
        player.setMediaItem(builder.build());
        player.prepare();
        player.setPlayWhenReady(autoPlay);

        host.showStatus(useFallback
            ? context.getString(R.string.status_channel_compat, request.channelName)
            : request.channelName);
    }

    private PlaybackDecision buildPlaybackDecision(PlaybackRequest request, boolean useFallback, StreamInfo streamInfo) {
        String directUrl = useFallback && request.hasFallback() ? request.fallbackPlayUrl : request.playUrl;
        String playUrlLower = request.playUrl == null ? "" : request.playUrl.toLowerCase(Locale.ROOT);
        boolean looksDash = playUrlLower.contains(".mpd");
        String drmType = streamInfo == null ? "" : safeLower(streamInfo.drmType);
        String playbackMode = request.playbackMode == null || request.playbackMode.trim().isEmpty() ? PlaybackModeStore.MODE_AUTO : request.playbackMode;

        if (useFallback) {
            return new PlaybackDecision(
                    directUrl,
                    inferMimeType(directUrl),
                    "",
                    playbackMode,
                    true,
                    false
            );
        }

        if ("widevine".equals(drmType) || "clearkey".equals(drmType)) {
            return new PlaybackDecision(
                    baseUrl + "/proxy/manifest/" + request.channelId,
                    MimeTypes.APPLICATION_MPD,
                    drmType,
                    playbackMode,
                    false,
                    false
            );
        }

        if (PlaybackModeStore.MODE_PROXY.equals(playbackMode)) {
            String proxyUrl = baseUrl + "/proxy/manifest/" + request.channelId + (streamInfo != null && streamInfo.encrypted ? "?nodrm=1" : "");
            return new PlaybackDecision(
                    proxyUrl,
                    resolveMimeType(proxyUrl, streamInfo, true),
                    "",
                    playbackMode,
                    false,
                    false
            );
        }

        if (streamInfo != null && streamInfo.encrypted) {
            return new PlaybackDecision(
                    baseUrl + "/proxy/manifest/" + request.channelId + "?nodrm=1",
                    resolveMimeType(baseUrl + "/proxy/manifest/" + request.channelId + "?nodrm=1", streamInfo, true),
                    "",
                    playbackMode,
                    false,
                    false
            );
        }

        if (PlaybackModeStore.MODE_DIRECT.equals(playbackMode)) {
            return new PlaybackDecision(
                    request.playUrl,
                    resolveMimeType(request.playUrl, streamInfo, false),
                    "",
                    playbackMode,
                    false,
                    request.hasFallback()
            );
        }

        if (streamInfo != null) {
            String streamType = safeLower(streamInfo.type);
            if ("dash".equals(streamType) || looksDash) {
                return new PlaybackDecision(
                        baseUrl + "/proxy/manifest/" + request.channelId + "?nodrm=1",
                        MimeTypes.APPLICATION_MPD,
                        "",
                        playbackMode,
                        false,
                        false
                );
            }
            return new PlaybackDecision(
                    request.playUrl,
                    resolveMimeType(request.playUrl, streamInfo, false),
                    "",
                    playbackMode,
                    false,
                    request.hasFallback()
            );
        }

        if (looksDash) {
            return new PlaybackDecision(
                    baseUrl + "/proxy/manifest/" + request.channelId,
                    MimeTypes.APPLICATION_MPD,
                    "",
                    playbackMode,
                    false,
                    false
            );
        }

        return new PlaybackDecision(
                request.playUrl,
                resolveMimeType(request.playUrl, null, false),
                "",
                playbackMode,
                false,
                request.hasFallback()
        );
    }

    private String resolveMimeType(String targetUrl, StreamInfo streamInfo, boolean defaultDashForProxy) {
        String mimeType = inferMimeType(targetUrl);
        if ((mimeType == null || mimeType.trim().isEmpty()) && streamInfo != null && streamInfo.type != null) {
            String streamType = safeLower(streamInfo.type);
            if ("dash".equals(streamType)) {
                mimeType = MimeTypes.APPLICATION_MPD;
            } else if ("hls".equals(streamType)) {
                mimeType = MimeTypes.APPLICATION_M3U8;
            }
        }
        if ((mimeType == null || mimeType.trim().isEmpty()) && defaultDashForProxy) {
            mimeType = streamInfo != null && "hls".equals(safeLower(streamInfo.type))
                    ? MimeTypes.APPLICATION_M3U8
                    : MimeTypes.APPLICATION_MPD;
        }
        return mimeType;
    }

    private StreamInfo fetchStreamInfo(String channelId) {
        try {
            HttpClient.Response response = httpClient.get(baseUrl + "/api/stream/" + channelId, 5000, 20000, java.util.Collections.singletonMap("Accept", "application/json"));
            if (!response.isSuccessful()) {
                Log.d(TAG, "fetchStreamInfo non-success channelId=" + channelId + " code=" + response.code);
                return null;
            }

            JSONObject jsonObject = httpClient.parseObject(response.body, "cargando stream info");
            StreamInfo info = new StreamInfo();
            info.drmType = jsonObject.optString("drm_type", "").trim();
            info.licenseUrl = jsonObject.optString("license_url", "").trim();
            info.type = jsonObject.optString("type", "").trim();
            info.encrypted = jsonObject.optBoolean("encrypted", false);
            Log.d(TAG, "fetchStreamInfo success channelId=" + channelId + " streamInfo=" + describeStreamInfo(info));
            return info;
        } catch (Exception e) {
            Log.w(TAG, "stream info fetch failed for channel " + channelId, e);
            return null;
        }
    }

    private static String describeRequest(PlaybackRequest request) {
        if (request == null) {
            return "null";
        }
        return "{" + request.channelId + "," + safeLogValue(request.channelName) + "}";
    }

    private static String describeDecision(PlaybackDecision decision) {
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
                + "}";
    }

    private String describeRouteLabel(PlaybackDecision decision) {
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

    private static String inferMimeType(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains(".mpd")) {
            return MimeTypes.APPLICATION_MPD;
        }
        if (lower.contains(".m3u8")) {
            return MimeTypes.APPLICATION_M3U8;
        }
        if (lower.contains(".mp4")) {
            return MimeTypes.VIDEO_MP4;
        }
        if (lower.contains(".ts")) {
            return MimeTypes.VIDEO_MP2T;
        }
        if (lower.contains(".mkv")) {
            return MimeTypes.VIDEO_MATROSKA;
        }
        return null;
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}