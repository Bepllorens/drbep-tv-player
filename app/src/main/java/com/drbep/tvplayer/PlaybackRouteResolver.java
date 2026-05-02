package com.drbep.tvplayer;

import androidx.media3.common.MimeTypes;

import java.util.Locale;

final class PlaybackRouteResolver {
    private final String baseUrl;

    PlaybackRouteResolver(String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
    }

    static final class Decision {
        final String targetUrl;
        final String mimeType;
        final String drmType;
        final String playbackMode;
        final boolean useFallback;
        final boolean allowCompatibilityFallback;

        Decision(String targetUrl, String mimeType, String drmType, String playbackMode, boolean useFallback, boolean allowCompatibilityFallback) {
            this.targetUrl = targetUrl;
            this.mimeType = mimeType;
            this.drmType = drmType;
            this.playbackMode = playbackMode;
            this.useFallback = useFallback;
            this.allowCompatibilityFallback = allowCompatibilityFallback;
        }

        boolean isEquivalentTo(Decision other) {
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

    Decision buildDecision(PlayerController.PlaybackRequest request, boolean useFallback, PlayerController.StreamInfo streamInfo) {
        String directUrl = useFallback && request.hasFallback() ? request.fallbackPlayUrl : request.playUrl;
        String playUrlLower = request.playUrl == null ? "" : request.playUrl.toLowerCase(Locale.ROOT);
        boolean looksDash = playUrlLower.contains(".mpd");
        String drmType = streamInfo == null ? "" : safeLower(streamInfo.drmType);
        String playbackMode = request.playbackMode == null || request.playbackMode.trim().isEmpty() ? PlaybackModeStore.MODE_AUTO : request.playbackMode;

        if (request.directPlayback) {
            return new Decision(
                    request.playUrl,
                    resolveMimeType(request.playUrl, streamInfo, false),
                    safeLower(request.drmScheme),
                    playbackMode,
                    false,
                    false
            );
        }

        if (useFallback) {
            return new Decision(
                    directUrl,
                    inferMimeType(directUrl),
                    "",
                    playbackMode,
                    true,
                    false
            );
        }

        if ("widevine".equals(drmType) || "clearkey".equals(drmType)) {
            return new Decision(
                    proxyManifestUrl(request.channelId),
                    MimeTypes.APPLICATION_MPD,
                    drmType,
                    playbackMode,
                    false,
                    false
            );
        }

        if (PlaybackModeStore.MODE_PROXY.equals(playbackMode)) {
            String proxyUrl = proxyManifestUrl(request.channelId) + (streamInfo != null && streamInfo.encrypted ? "?nodrm=1" : "");
            return new Decision(
                    proxyUrl,
                    resolveMimeType(proxyUrl, streamInfo, true),
                    "",
                    playbackMode,
                    false,
                    false
            );
        }

        if (streamInfo != null && streamInfo.encrypted) {
            String proxyUrl = proxyManifestUrl(request.channelId) + "?nodrm=1";
            return new Decision(
                    proxyUrl,
                    resolveMimeType(proxyUrl, streamInfo, true),
                    "",
                    playbackMode,
                    false,
                    false
            );
        }

        if (PlaybackModeStore.MODE_DIRECT.equals(playbackMode)) {
            return new Decision(
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
                return new Decision(
                        proxyManifestUrl(request.channelId) + "?nodrm=1",
                        MimeTypes.APPLICATION_MPD,
                        "",
                        playbackMode,
                        false,
                        false
                );
            }
            if ("hls".equals(streamType) && request.hasFallback()) {
                return new Decision(
                        request.fallbackPlayUrl,
                        MimeTypes.APPLICATION_M3U8,
                        "",
                        playbackMode,
                        false,
                        false
                );
            }
            return new Decision(
                    request.playUrl,
                    resolveMimeType(request.playUrl, streamInfo, false),
                    "",
                    playbackMode,
                    false,
                    request.hasFallback()
            );
        }

        if (looksDash) {
            return new Decision(
                    proxyManifestUrl(request.channelId),
                    MimeTypes.APPLICATION_MPD,
                    "",
                    playbackMode,
                    false,
                    false
            );
        }

        return new Decision(
                request.playUrl,
                resolveMimeType(request.playUrl, null, false),
                "",
                playbackMode,
                false,
                request.hasFallback()
        );
    }

    String resolveMimeType(String targetUrl, PlayerController.StreamInfo streamInfo, boolean defaultDashForProxy) {
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

    private String proxyManifestUrl(String channelId) {
        return baseUrl + "/proxy/manifest/" + channelId;
    }

    static String inferMimeType(String url) {
        String lower = url == null ? "" : url.toLowerCase(Locale.ROOT);
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
