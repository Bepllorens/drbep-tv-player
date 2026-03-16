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

        PlaybackRequest(String channelId, String channelName, String playUrl, String fallbackPlayUrl) {
            this.channelId = channelId;
            this.channelName = channelName;
            this.playUrl = playUrl;
            this.fallbackPlayUrl = fallbackPlayUrl;
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

    private static final class PlaybackDecision {
        final String targetUrl;
        final String mimeType;
        final String drmType;
        final boolean useFallback;
        final boolean allowCompatibilityFallback;

        PlaybackDecision(String targetUrl, String mimeType, String drmType, boolean useFallback, boolean allowCompatibilityFallback) {
            this.targetUrl = targetUrl;
            this.mimeType = mimeType;
            this.drmType = drmType;
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
                if (decision != null && decision.allowCompatibilityFallback && !usingPlaybackFallback && request != null && request.hasFallback()) {
                    usingPlaybackFallback = true;
                    Log.w(TAG, "primary playback failed, retrying fallback URL", error);
                    host.showStatus(context.getString(R.string.status_retry_compat));
                    playChannelInternal(request, true, true, currentStreamInfo);
                    return;
                }

                String message = context.getString(R.string.error_playback_message, error.getMessage());
                host.showError(message);
                Log.w(TAG, message, error);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
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
        playChannelInternal(request, autoPlay, false, streamInfo);
    }

    void resolveStreamInfoAndReplayIfNeeded(PlaybackRequest request, boolean autoPlay, Map<String, StreamInfo> streamInfoCache) {
        if (request == null || request.channelId == null || request.channelId.trim().isEmpty()) {
            return;
        }

        final String channelId = request.channelId.trim();
        ioExecutor.execute(() -> {
            StreamInfo info = streamInfoCache.get(channelId);
            if (info == null) {
                info = fetchStreamInfo(channelId);
                if (info != null) {
                    streamInfoCache.put(channelId, info);
                }
            }
            if (info == null) {
                return;
            }

            boolean requiresReplay = "widevine".equals(safeLower(info.drmType)) || info.encrypted;
            StreamInfo resolved = info;
            uiHandler.post(() -> {
                if (!host.isChannelCurrent(channelId)) {
                    return;
                }
                PlaybackDecision resolvedDecision = buildPlaybackDecision(request, false, resolved);
                if (!requiresReplay && resolvedDecision.isEquivalentTo(currentPlaybackDecision)) {
                    return;
                }
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
        PlaybackDecision decision = buildPlaybackDecision(request, useFallback, streamInfo);
        currentPlaybackDecision = decision;

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

        Log.i(TAG, "playChannel id=" + request.channelId + " name=" + request.channelName + " drmType=" + decision.drmType + " encrypted=" + (streamInfo != null && streamInfo.encrypted) + " mime=" + (decision.mimeType == null ? "" : decision.mimeType) + " target=" + decision.targetUrl + " useFallback=" + useFallback + " allowFallback=" + decision.allowCompatibilityFallback);
        player.setMediaItem(builder.build());
        player.prepare();
        player.setPlayWhenReady(autoPlay);

        host.showStatus(useFallback
            ? context.getString(R.string.status_channel_compat, request.channelName)
            : request.channelName);
    }

    private PlaybackDecision buildPlaybackDecision(PlaybackRequest request, boolean useFallback, StreamInfo streamInfo) {
        String directUrl = useFallback && request.hasFallback() ? request.fallbackPlayUrl : request.playUrl;
        String directUrlLower = directUrl == null ? "" : directUrl.toLowerCase(Locale.ROOT);
        String playUrlLower = request.playUrl == null ? "" : request.playUrl.toLowerCase(Locale.ROOT);
        boolean looksDash = playUrlLower.contains(".mpd");
        String drmType = streamInfo == null ? "" : safeLower(streamInfo.drmType);

        if (useFallback) {
            return new PlaybackDecision(
                    directUrl,
                    inferMimeType(directUrl),
                    "",
                    true,
                    false
            );
        }

        if ("widevine".equals(drmType) || "clearkey".equals(drmType)) {
            return new PlaybackDecision(
                    baseUrl + "/proxy/manifest/" + request.channelId,
                    MimeTypes.APPLICATION_MPD,
                    drmType,
                    false,
                    false
            );
        }

        if (streamInfo != null && streamInfo.encrypted) {
            return new PlaybackDecision(
                    baseUrl + "/proxy/manifest/" + request.channelId + "?nodrm=1",
                    resolveMimeType(baseUrl + "/proxy/manifest/" + request.channelId + "?nodrm=1", streamInfo, true),
                    "",
                    false,
                    false
            );
        }

        if (streamInfo != null) {
            String streamType = safeLower(streamInfo.type);
            if ("dash".equals(streamType) || looksDash) {
                return new PlaybackDecision(
                        baseUrl + "/proxy/manifest/" + request.channelId + "?nodrm=1",
                        MimeTypes.APPLICATION_MPD,
                        "",
                        false,
                        false
                );
            }
            return new PlaybackDecision(
                    request.playUrl,
                    resolveMimeType(request.playUrl, streamInfo, false),
                    "",
                    false,
                    request.hasFallback()
            );
        }

        if (looksDash) {
            return new PlaybackDecision(
                    baseUrl + "/proxy/manifest/" + request.channelId,
                    MimeTypes.APPLICATION_MPD,
                    "",
                    false,
                    false
            );
        }

        return new PlaybackDecision(
                request.playUrl,
                resolveMimeType(request.playUrl, null, false),
                "",
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
                return null;
            }

            JSONObject jsonObject = httpClient.parseObject(response.body, "cargando stream info");
            StreamInfo info = new StreamInfo();
            info.drmType = jsonObject.optString("drm_type", "").trim();
            info.licenseUrl = jsonObject.optString("license_url", "").trim();
            info.type = jsonObject.optString("type", "").trim();
            info.encrypted = jsonObject.optBoolean("encrypted", false);
            return info;
        } catch (Exception e) {
            Log.w(TAG, "stream info fetch failed for channel " + channelId, e);
            return null;
        }
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