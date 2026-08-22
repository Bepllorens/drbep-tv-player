package com.drbep.tvplayer;

import androidx.annotation.OptIn;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.UnstableApi;

import java.util.Locale;

@OptIn(markerClass = UnstableApi.class)
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
        String requestDrmType = safeLower(request.drmScheme);
        String streamInfoDrmType = streamInfo == null ? "" : safeLower(streamInfo.drmType);
        String drmType = streamInfoDrmType.isEmpty() ? requestDrmType : streamInfoDrmType;
        String playbackMode = request.playbackMode == null || request.playbackMode.trim().isEmpty() ? PlaybackModeStore.MODE_AUTO : request.playbackMode;
        String playbackProfile = safeLower(request.playbackProfile);
        boolean standaloneDirectAllowed = shouldPreferDirectInStandalone(request, streamInfo, playbackMode, playbackProfile);

        if (isMovistarIsmRequest(request, streamInfo)) {
            return new Decision(
                    ismHlsUrl(request.channelId),
                    MimeTypes.APPLICATION_M3U8,
                    "",
                    playbackMode,
                    false,
                    false
            );
        }

        if (isMovistarSampleAesHlsRequest(request)) {
            return new Decision(
                    sampleAesDirectStreamUrl(request.channelId),
                    MimeTypes.VIDEO_MP2T,
                    "",
                    playbackMode,
                    false,
                    false
            );
        }

        if (request.directPlayback) {
            String targetUrl = resolveStandaloneDirectUrl(request, streamInfo);
            return new Decision(
                    targetUrl,
                    resolveMimeType(targetUrl, streamInfo, false),
                    safeLower(request.drmScheme),
                    playbackMode,
                    false,
                    false
            );
        }

        if (useFallback) {
            String fallbackMimeType = resolveMimeTypeForRequest(
                    request,
                    directUrl,
                    streamInfo,
                    directUrl != null && directUrl.contains("/proxy/manifest/")
            );
            return new Decision(
                    directUrl,
                    fallbackMimeType,
                    MimeTypes.APPLICATION_MPD.equals(fallbackMimeType) ? drmType : "",
                    playbackMode,
                    true,
                    false
            );
        }

        if ("server_live".equals(playbackProfile)) {
            String streamType = streamInfo == null ? "" : safeLower(streamInfo.type);
            if ("smooth".equals(streamType) || looksSmooth) {
                return new Decision(
                        ismHlsUrl(request.channelId),
                        MimeTypes.APPLICATION_M3U8,
                        "",
                        playbackMode,
                        false,
                        false
                );
            }
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

        if ("proxy_manifest".equals(playbackProfile)
                && !PlaybackModeStore.MODE_DIRECT.equals(playbackMode)
                && (!BuildConfig.STANDALONE_MODE || PlaybackModeStore.MODE_PROXY.equals(playbackMode))) {
            String proxyUrl = proxyManifestUrl(request.channelId);
            return new Decision(
                    proxyUrl,
                    resolveMimeTypeForRequest(request, proxyUrl, streamInfo, true),
                    drmType,
                    playbackMode,
                    false,
                    false
            );
        }

        if ("proxy_manifest".equals(playbackProfile) && standaloneDirectAllowed) {
            String targetUrl = resolveStandaloneDirectUrl(request, streamInfo);
            return new Decision(
                    targetUrl,
                    resolveMimeType(targetUrl, streamInfo, false),
                    drmType,
                    playbackMode,
                    false,
                    true
            );
        }

        if ("direct".equals(playbackProfile) || PlaybackModeStore.MODE_DIRECT.equals(playbackMode)) {
            boolean backendLive = isBackendLiveUrl(request.playUrl);
            // Some catalog sources are container-only URLs (for example
            // adult-proxy). When direct access is not safe, keep the public
            // backend URL instead of leaking an unreachable cleartext host to
            // the Android device.
            String targetUrl = standaloneDirectAllowed
                    ? resolveStandaloneDirectUrl(request, streamInfo)
                    : request.playUrl;
            return new Decision(
                    targetUrl,
                    resolveMimeType(targetUrl, streamInfo, false),
                    backendLive ? "" : drmType,
                    playbackMode,
                    false,
                    request.hasFallback()
            );
        }

        if ("widevine".equals(drmType) || "clearkey".equals(drmType)) {
            String streamType = streamInfo == null ? "" : safeLower(streamInfo.type);
            if ("server_live".equals(safeLower(request.playbackProfile))) {
                if ("smooth".equals(streamType) || looksSmooth) {
                    return new Decision(
                            ismHlsUrl(request.channelId),
                            MimeTypes.APPLICATION_M3U8,
                            "",
                            playbackMode,
                            false,
                            false
                    );
                }
                return new Decision(
                        liveStreamUrl(request.channelId),
                        MimeTypes.VIDEO_MP2T,
                        "",
                        playbackMode,
                        false,
                        false
                );
            }
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
            if (standaloneDirectAllowed) {
                String targetUrl = resolveStandaloneDirectUrl(request, streamInfo);
                return new Decision(
                        targetUrl,
                        resolveMimeType(targetUrl, streamInfo, false),
                        drmType,
                        playbackMode,
                        false,
                        true
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

        if (standaloneDirectAllowed && streamInfo != null && streamInfo.encrypted) {
            String targetUrl = resolveStandaloneDirectUrl(request, streamInfo);
            return new Decision(
                    targetUrl,
                    resolveMimeType(targetUrl, streamInfo, false),
                    "",
                    playbackMode,
                    false,
                    true
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
                    resolveMimeTypeForRequest(request, proxyUrl, streamInfo, true),
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
                    resolveMimeTypeForRequest(request, proxyUrl, streamInfo, true),
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
            if (standaloneDirectAllowed && ("dash".equals(streamType) || "hls".equals(streamType))) {
                String targetUrl = resolveStandaloneDirectUrl(request, streamInfo);
                return new Decision(
                        targetUrl,
                        resolveMimeType(targetUrl, streamInfo, false),
                        "",
                        playbackMode,
                        false,
                        true
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
                String targetUrl = standaloneDirectAllowed ? resolveStandaloneDirectUrl(request, streamInfo) : request.fallbackPlayUrl;
                return new Decision(
                        targetUrl,
                        MimeTypes.APPLICATION_M3U8,
                        "",
                        playbackMode,
                        false,
                        standaloneDirectAllowed && request.hasFallback()
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

        if (standaloneDirectAllowed && looksDash) {
            return new Decision(
                    request.playUrl,
                    MimeTypes.APPLICATION_MPD,
                    "",
                    playbackMode,
                    false,
                    true
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

    private String resolveMimeTypeForRequest(PlayerController.PlaybackRequest request,
                                             String targetUrl,
                                             PlayerController.StreamInfo streamInfo,
                                             boolean defaultDashForProxy) {
        if (isPlutoRequest(request) && targetUrl != null && targetUrl.contains("/proxy/manifest/")) {
            return MimeTypes.APPLICATION_M3U8;
        }
        return resolveMimeType(targetUrl, streamInfo, defaultDashForProxy);
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

    private boolean shouldPreferDirectInStandalone(PlayerController.PlaybackRequest request,
                                                   PlayerController.StreamInfo streamInfo,
                                                   String playbackMode,
                                                   String playbackProfile) {
        if (!BuildConfig.STANDALONE_MODE || request == null) {
            return false;
        }
        if (PlaybackModeStore.MODE_PROXY.equals(playbackMode)) {
            return false;
        }
        if ("server_live".equals(playbackProfile) || "hevc_hls".equals(playbackProfile)) {
            return false;
        }
        if (isMovistarDashCompatOnly(request, streamInfo)) {
            return false;
        }
        if (isAdultOrHotText(request.platformName) || isAdultOrHotText(request.channelName)) {
            return false;
        }
        if (request.playUrl != null && request.playUrl.contains("/proxy/manifest/")) {
            return false;
        }
        String sourceUrl = streamInfo == null ? "" : safeTrim(streamInfo.sourceUrl);
        if (!sourceUrl.isEmpty()) {
            return !isBackendLiveUrl(sourceUrl) && !sourceUrl.contains("runtime-proxy");
        }
        return !isBackendLiveUrl(request.playUrl);
    }

    private String resolveStandaloneDirectUrl(PlayerController.PlaybackRequest request, PlayerController.StreamInfo streamInfo) {
        String patchedClearKeyManifest = streamInfo == null ? "" : safeTrim(streamInfo.patchedClearKeyManifestDataUri);
        if (!patchedClearKeyManifest.isEmpty()) {
            return patchedClearKeyManifest;
        }
        String sourceUrl = streamInfo == null ? "" : safeTrim(streamInfo.sourceUrl);
        if (!sourceUrl.isEmpty() && !sourceUrl.contains("runtime-proxy")) {
            return sourceUrl;
        }
        return request == null ? "" : request.playUrl;
    }

    private boolean isAdultOrHotText(String value) {
        String lower = safeLower(value);
        return lower.contains("adult")
                || lower.contains("hot")
                || lower.contains("playboy")
                || lower.contains("venus")
                || lower.contains("hustler")
                || lower.contains("sex");
    }

    private boolean isPlutoRequest(PlayerController.PlaybackRequest request) {
        if (request == null) {
            return false;
        }
        String platform = safeLower(request.platformName);
        return platform.contains("pluto");
    }

    private boolean isMovistarSampleAesHlsRequest(PlayerController.PlaybackRequest request) {
        if (request == null || request.vod || !"proxy_manifest".equals(safeLower(request.playbackProfile))) {
            return false;
        }
        return safeLower(request.platformName).contains("movistar hls");
    }

    private boolean isMovistarDashCompatOnly(PlayerController.PlaybackRequest request, PlayerController.StreamInfo streamInfo) {
        if (request == null) {
            return false;
        }
        String platform = safeLower(request.platformName);
        if (!platform.contains("movistar") || platform.contains("ism")) {
            return false;
        }
        String streamType = streamInfo == null ? "" : safeLower(streamInfo.type);
        String drmType = streamInfo == null ? "" : safeLower(streamInfo.drmType);
        return "dash".equals(streamType) && "clearkey".equals(drmType);
    }

    private String proxyManifestUrl(String channelId) {
        return baseUrl + "/proxy/manifest/" + channelId;
    }

    private String sampleAesDirectStreamUrl(String channelId) {
        return baseUrl + "/drm/direct/" + channelId;
    }

    private String liveStreamUrl(String channelId) {
        return baseUrl + "/live/" + channelId + "?client=firestick";
    }

    private String ismHlsUrl(String channelId) {
        return baseUrl + "/hls/ism/" + channelId + "/index.m3u8";
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
        if (lower.contains("/api/vod/movistar/manifest/")) {
            return MimeTypes.APPLICATION_MPD;
        }
        if (lower.contains("/api/vod/dazn/manifest/")) {
            return MimeTypes.APPLICATION_MPD;
        }
        if (lower.contains("/api/vod/prime/manifest/")) {
            return MimeTypes.APPLICATION_MPD;
        }
        if (lower.contains("/api/offline/u7d/movistar-ism/stream")
                || lower.contains("/api/offline/u7d/orange/stream")) {
            return MimeTypes.VIDEO_MP2T;
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

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean looksLikeSmooth(String lowerUrl) {
        String value = lowerUrl == null ? "" : lowerUrl;
        return value.contains(".isml/manifest") || value.contains(".ism/manifest");
    }

    private static boolean isMovistarIsmRequest(PlayerController.PlaybackRequest request, PlayerController.StreamInfo streamInfo) {
        if (request == null) {
            return false;
        }
        String platform = safeLower(request.platformName);
        String groupOrChannel = platform + " " + safeLower(request.channelName);
        String url = safeLower(request.playUrl);
        String streamType = streamInfo == null ? "" : safeLower(streamInfo.type);
        boolean looksSmooth = "smooth".equals(streamType) || looksLikeSmooth(url);
        return looksSmooth && groupOrChannel.contains("movistar") && groupOrChannel.contains("ism");
    }
}
