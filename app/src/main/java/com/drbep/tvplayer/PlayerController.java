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

import java.net.HttpURLConnection;
import java.net.URL;
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

    private final Context context;
    private final PlayerView playerView;
    private final String baseUrl;
    private final ExecutorService ioExecutor;
    private final Handler uiHandler;
    private final Host host;

    private ExoPlayer player;
    private PlaybackRequest currentRequest;
    private StreamInfo currentStreamInfo;
    private boolean usingPlaybackFallback;

    PlayerController(Context context, PlayerView playerView, String baseUrl, ExecutorService ioExecutor, Handler uiHandler, Host host) {
        this.context = context;
        this.playerView = playerView;
        this.baseUrl = baseUrl;
        this.ioExecutor = ioExecutor;
        this.uiHandler = uiHandler;
        this.host = host;
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
                StreamInfo streamInfo = currentStreamInfo;
                boolean allowFallback = streamInfo == null || (!streamInfo.encrypted && !"dash".equals(safeLower(streamInfo.type)));
                if (allowFallback && !usingPlaybackFallback && request != null && request.hasFallback()) {
                    usingPlaybackFallback = true;
                    Log.w(TAG, "primary playback failed, retrying fallback URL", error);
                    host.showStatus(context.getString(R.string.status_retry_compat));
                    playChannelInternal(request, true, true, streamInfo);
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
            if (!requiresReplay) {
                return;
            }

            StreamInfo resolved = info;
            uiHandler.post(() -> {
                if (!host.isChannelCurrent(channelId)) {
                    return;
                }
                playChannelInternal(request, autoPlay, false, resolved);
                if ("widevine".equals(safeLower(resolved.drmType))) {
                    host.showStatus(request.channelName + " (Widevine)");
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
        host.showStatus("Reproduciendo grabacion: " + recordingName);
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

        String drmType = streamInfo == null ? "" : safeLower(streamInfo.drmType);
        String targetUrl = useFallback && request.hasFallback() ? request.fallbackPlayUrl : request.playUrl;
        String playUrlLower = request.playUrl == null ? "" : request.playUrl.toLowerCase(Locale.ROOT);
        boolean looksDash = playUrlLower.contains(".mpd");

        if (!useFallback) {
            if (streamInfo != null) {
                if ("widevine".equals(drmType) || "clearkey".equals(drmType)) {
                    targetUrl = baseUrl + "/proxy/manifest/" + request.channelId;
                } else if (streamInfo.encrypted || looksDash) {
                    targetUrl = baseUrl + "/proxy/manifest/" + request.channelId + "?nodrm=1";
                }
            } else {
                targetUrl = baseUrl + "/proxy/manifest/" + request.channelId;
            }
        }

        if (targetUrl == null || targetUrl.trim().isEmpty()) {
            host.showError(context.getString(R.string.error_empty_playback_url));
            return;
        }

        String mimeType = inferMimeType(targetUrl);
        if ((mimeType == null || mimeType.trim().isEmpty()) && streamInfo != null && streamInfo.type != null) {
            String streamType = safeLower(streamInfo.type);
            if ("dash".equals(streamType)) {
                mimeType = MimeTypes.APPLICATION_MPD;
            } else if ("hls".equals(streamType)) {
                mimeType = MimeTypes.APPLICATION_M3U8;
            }
        }
        if ((mimeType == null || mimeType.trim().isEmpty()) && looksDash) {
            mimeType = MimeTypes.APPLICATION_MPD;
        }
        if ((mimeType == null || mimeType.trim().isEmpty()) && !useFallback && streamInfo == null && targetUrl.contains("/proxy/manifest/")) {
            mimeType = MimeTypes.APPLICATION_MPD;
        }
        if ((mimeType == null || mimeType.trim().isEmpty()) && (targetUrl.contains("/proxy/manifest/") || targetUrl.contains("/drm/manifest/"))) {
            String streamType = streamInfo == null ? "" : safeLower(streamInfo.type);
            mimeType = "hls".equals(streamType) ? MimeTypes.APPLICATION_M3U8 : MimeTypes.APPLICATION_MPD;
        }

        MediaItem.Builder builder = new MediaItem.Builder().setUri(targetUrl);
        if (mimeType != null && !mimeType.trim().isEmpty()) {
            builder.setMimeType(mimeType);
        }
        if ("widevine".equals(drmType)) {
            builder.setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(baseUrl + "/api/widevine/" + request.channelId)
                    .build());
        } else if ("clearkey".equals(drmType)) {
            builder.setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                    .setLicenseUri(baseUrl + "/api/clearkey/" + request.channelId)
                    .build());
        }

        Log.i(TAG, "playChannel id=" + request.channelId + " name=" + request.channelName + " drmType=" + drmType + " encrypted=" + (streamInfo != null && streamInfo.encrypted) + " mime=" + (mimeType == null ? "" : mimeType) + " target=" + targetUrl + " useFallback=" + useFallback);
        player.setMediaItem(builder.build());
        player.prepare();
        player.setPlayWhenReady(autoPlay);

        String mode = useFallback ? " (modo compat)" : "";
        host.showStatus(request.channelName + mode);
    }

    private StreamInfo fetchStreamInfo(String channelId) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/api/stream/" + channelId);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                return null;
            }

            JSONObject jsonObject = new JSONObject(MainActivity.readAll(conn.getInputStream()));
            StreamInfo info = new StreamInfo();
            info.drmType = jsonObject.optString("drm_type", "").trim();
            info.licenseUrl = jsonObject.optString("license_url", "").trim();
            info.type = jsonObject.optString("type", "").trim();
            info.encrypted = jsonObject.optBoolean("encrypted", false);
            return info;
        } catch (Exception e) {
            Log.w(TAG, "stream info fetch failed for channel " + channelId, e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
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