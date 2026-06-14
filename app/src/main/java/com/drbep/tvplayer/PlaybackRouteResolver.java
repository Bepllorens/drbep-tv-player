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
        boolean looksSmooth = looksLikeSmooth(playUrlLower);
        String drmType = streamInfo == null ? "" : safeLower(streamInfo.drmType);
        String playbackMode = request.playbackMode == null || request.playbackMode.trim().isEmpty() ? PlaybackModeStore.MODE_AUTO : request.playbackMode;
        String playbackProfile = safeLower(request.playbackProfile);

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
                    resolveMimeType(directUrl, streamInfo, directUrl != null && directUrl.contains("/proxy/manifest/")),
                    "",
                    playbackMode,
                    true,
                    false
            );
        }

        if ("server_live".equals(playbackProfile)) {
            return new Decision(
                    liveStreamUrl(request.channelId),
                    MimeTypes.VIDEO_MP2T,
                    "",
                    playbackMode,
                    false,
                    false
            );
        }

        if ("hevc_hls".equals(playbackProfile) || "1071554".equals(request.channelId)) {
            return new Decision(
                    hevcHlsUrl(request.channelId),
                    MimeTypes.APPLICATION_M3U8,
                    "",
                    playbackMode,
                    false,
                    false
            );
        }

        if ("proxy_manifest".equals(playbackProfile)) {
            String proxyUrl = proxyManifestUrl(request.channelId);
            return new Decision(
                    proxyUrl,
                    resolveMimeType(proxyUrl, streamInfo, true),
                    drmType,
                    playbackMode,
                    false,
                    false
            );
        }

        if ("direct".equals(playbackProfile) || PlaybackModeStore.MODE_DIRECT.equals(playbackMode)) {
            boolean backendLive = isBackendLiveUrl(request.playUrl);
            return new Decision(
                    request.playUrl,
                    resolveMimeType(request.playUrl, streamInfo, false),
                    backendLive ? "" : drmType,
                    playbackMode,
                    false,
                    request.hasFallback()
            );
        }

        if ("widevine".equals(drmType) || "clearkey".equals(drmType)) {
            if ("server_live".equals(safeLower(request.playbackProfile))) {
                return new Decision(
                        liveStreamUrl(request.channelId),
                        MimeTypes.VIDEO_MP2T,
                        "",
                        playbackMode,
                        false,
                        false
                );
            }
            String streamType = streamInfo == null ? "" : safeLower(streamInfo.type);
            if ("smooth".equals(streamType) || looksSmooth) {
                String targetUrl = streamInfo != null && streamInfo.sourceUrl != null && !streamInfo.sourceUrl.trim().isEmpty()
                        ? streamInfo.sourceUrl
                        : request.playUrl;
                if (streamInfo != null && streamInfo.patchedSmoothClearKeyManifestDataUri != null && !streamInfo.patchedSmoothClearKeyManifestDataUri.trim().isEmpty()) {
                    targetUrl = streamInfo.patchedSmoothClearKeyManifestDataUri;
                }
                return new Decision(
                        targetUrl,
                        MimeTypes.APPLICATION_SS,
                        drmType,
                        playbackMode,
                        false,
                        request.hasFallback()
                );
            }
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
            String streamType = streamInfo == null ? "" : safeLower(streamInfo.type);
            if ("hls".equals(streamType)) {
                return new Decision(
                        request.playUrl,
                        MimeTypes.APPLICATION_M3U8,
                        "",
                        playbackMode,
                        false,
                    request.hasFallback()
                );
            }
            if ("smooth".equals(streamType)) {
                return new Decision(
                        request.playUrl,
                        MimeTypes.APPLICATION_SS,
                        "",
                        playbackMode,
                        false,
                        request.hasFallback()
                );
            }
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

        if (streamInfo != null) {
            String streamType = safeLower(streamInfo.type);
            if (isBackendLiveUrl(request.playUrl)) {
                return new Decision(
                        request.playUrl,
                        MimeTypes.VIDEO_MP2T,
                        "",
                        playbackMode,
                        false,
                        request.hasFallback()
                );
            }
            if ("smooth".equals(streamType) || looksSmooth) {
                return new Decision(
                        request.playUrl,
                        MimeTypes.APPLICATION_SS,
                        "",
                        playbackMode,
                        false,
                        request.hasFallback()
                );
            }
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

        if (looksSmooth) {
            return new Decision(
                    request.playUrl,
                    MimeTypes.APPLICATION_SS,
                    "",
                    playbackMode,
                    false,
                    request.hasFallback()
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
        if ((mimeType == null || mimeType.trim().isEmpty()) && isBackendLiveUrl(targetUrl)) {
            return MimeTypes.VIDEO_MP2T;
        }
        if ((mimeType == null || mimeType.trim().isEmpty()) && streamInfo != null && streamInfo.type != null) {
            String streamType = safeLower(streamInfo.type);
            if ("dash".equals(streamType)) {
                mimeType = MimeTypes.APPLICATION_MPD;
            } else if ("hls".equals(streamType)) {
                mimeType = MimeTypes.APPLICATION_M3U8;
            } else if ("smooth".equals(streamType)) {
                mimeType = MimeTypes.APPLICATION_SS;
            }
        }
        if ((mimeType == null || mimeType.trim().isEmpty()) && defaultDashForProxy) {
            mimeType = streamInfo != null && "hls".equals(safeLower(streamInfo.type))
                    ? MimeTypes.APPLICATION_M3U8
                    : MimeTypes.APPLICATION_MPD;
        }
        return mimeType;
    }

    private boolean isBackendLiveUrl(String targetUrl) {
        if (targetUrl == null) {
            return false;
        }
        String normalizedBase = baseUrl == null ? "" : baseUrl.trim();
        String trimmed = targetUrl.trim();
        if (!normalizedBase.isEmpty() && trimmed.startsWith(normalizedBase + "/live/")) {
            return true;
        }
        return trimmed.contains("/live/") && trimmed.contains("client=firestick");
    }

    private String proxyManifestUrl(String channelId) {
        return baseUrl + "/proxy/manifest/" + channelId;
    }

    private String liveStreamUrl(String channelId) {
        return baseUrl + "/live/" + channelId + "?client=firestick";
    }

    private String hevcHlsUrl(String channelId) {
        return baseUrl + "/hls/" + channelId + "/playlist.m3u8?codec=hevc";
    }

    static String inferMimeType(String url) {
        String lower = url == null ? "" : url.toLowerCase(Locale.ROOT);
        if (lower.contains(".mpd")) {
            return MimeTypes.APPLICATION_MPD;
        }
        if (lower.contains(".m3u8")) {
            return MimeTypes.APPLICATION_M3U8;
        }
        if (looksLikeSmooth(lower)) {
            return MimeTypes.APPLICATION_SS;
        }
        if (lower.contains("/api/vod/runtime/stream/")) {
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

    private static boolean looksLikeSmooth(String lowerUrl) {
        String value = lowerUrl == null ? "" : lowerUrl;
        return value.contains(".isml/manifest") || value.contains(".ism/manifest");
    }
}
