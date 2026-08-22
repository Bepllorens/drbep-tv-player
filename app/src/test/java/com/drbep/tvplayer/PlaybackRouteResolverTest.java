package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.MimeTypes;

import org.junit.Test;

public class PlaybackRouteResolverTest {
    private final PlaybackRouteResolver resolver = new PlaybackRouteResolver("https://iptv.example.com");

    @Test
    public void dashUrlInAutoUsesDirectRouteInStandalone() {
        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("42", "https://origin.example.com/live/channel.mpd", "", PlaybackModeStore.MODE_AUTO, false, ""),
                false,
                null
        );

        assertEquals("https://origin.example.com/live/channel.mpd", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_MPD, decision.mimeType);
        assertEquals("", decision.drmType);
        assertEquals(PlaybackModeStore.MODE_AUTO, decision.playbackMode);
        assertFalse(decision.useFallback);
        assertTrue(decision.allowCompatibilityFallback);
    }

    @Test
    public void directModeKeepsOriginalUrlAndAllowsCompatibilityFallback() {
        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("7", "https://origin.example.com/live/master.m3u8", "https://iptv.example.com/proxy/manifest/7", PlaybackModeStore.MODE_DIRECT, false, ""),
                false,
                null
        );

        assertEquals("https://origin.example.com/live/master.m3u8", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_M3U8, decision.mimeType);
        assertEquals(PlaybackModeStore.MODE_DIRECT, decision.playbackMode);
        assertFalse(decision.useFallback);
        assertTrue(decision.allowCompatibilityFallback);
    }

    @Test
    public void proxyManifestProfileAllowsDirectAttemptInStandalone() {
        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                requestWithProfile("77", "https://blocked.example.com/live/channel.mpd", "", PlaybackModeStore.MODE_DIRECT, "proxy_manifest"),
                false,
                null
        );

        assertEquals("https://blocked.example.com/live/channel.mpd", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_MPD, decision.mimeType);
        assertEquals(PlaybackModeStore.MODE_DIRECT, decision.playbackMode);
        assertFalse(decision.useFallback);
        assertTrue(decision.allowCompatibilityFallback);
    }

    @Test
    public void plutoProxyManifestProfileUsesHlsBeforeStreamInfoResolves() {
        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                requestWithPlatformAndProfile(
                        "1104680",
                        "Realmadrid TV",
                        "PlutoTV",
                        "https://fire.tvbep.com/proxy/manifest/1104680",
                        "",
                        PlaybackModeStore.MODE_PROXY,
                        "proxy_manifest"
                ),
                false,
                null
        );

        assertEquals("https://iptv.example.com/proxy/manifest/1104680", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_M3U8, decision.mimeType);
        assertEquals(PlaybackModeStore.MODE_PROXY, decision.playbackMode);
        assertFalse(decision.useFallback);
    }

    @Test
    public void plutoFallbackProxyManifestUsesHlsBeforeStreamInfoResolves() {
        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                requestWithPlatformAndProfile(
                        "1105483",
                        "Pluto fallback",
                        "PlutoTV",
                        "https://origin.example.com/live/master",
                        "https://fire.tvbep.com/proxy/manifest/1105483",
                        PlaybackModeStore.MODE_AUTO,
                        "proxy_manifest"
                ),
                true,
                null
        );

        assertEquals("https://fire.tvbep.com/proxy/manifest/1105483", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_M3U8, decision.mimeType);
        assertTrue(decision.useFallback);
        assertFalse(decision.allowCompatibilityFallback);
    }

    @Test
    public void widevineProxyManifestKeepsDrmBeforeStreamInfoResolves() {
        PlayerController.PlaybackRequest request = new PlayerController.PlaybackRequest(
                "1114666",
                "Eurosport 1",
                "DAZN",
                "https://iptv.example.com/proxy/manifest/1114666",
                "https://iptv.example.com/proxy/manifest/1114666",
                PlaybackModeStore.MODE_AUTO,
                "widevine",
                "drbep-drm://channel/1114666",
                false,
                "proxy_manifest"
        );

        PlaybackRouteResolver.Decision decision = resolver.buildDecision(request, false, null);

        assertEquals(MimeTypes.APPLICATION_MPD, decision.mimeType);
        assertEquals("widevine", decision.drmType);
    }

    @Test
    public void widevineCompatibilityFallbackKeepsDrmForDashProxy() {
        PlayerController.PlaybackRequest request = new PlayerController.PlaybackRequest(
                "1114667",
                "Eurosport 2",
                "DAZN",
                "https://iptv.example.com/proxy/manifest/1114667",
                "https://iptv.example.com/proxy/manifest/1114667",
                PlaybackModeStore.MODE_AUTO,
                "widevine",
                "drbep-drm://channel/1114667",
                false,
                "proxy_manifest"
        );

        PlaybackRouteResolver.Decision decision = resolver.buildDecision(request, true, null);

        assertEquals(MimeTypes.APPLICATION_MPD, decision.mimeType);
        assertEquals("widevine", decision.drmType);
        assertTrue(decision.useFallback);
    }

    @Test
    public void movistarSampleAesHlsUsesDirectCopyBridge() {
        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                requestWithPlatformAndProfile(
                        "1113364",
                        "LA 2",
                        "Movistar HLS",
                        "https://iptv.example.com/proxy/manifest/1113364",
                        "",
                        PlaybackModeStore.MODE_AUTO,
                        "proxy_manifest"
                ),
                false,
                null
        );

        assertEquals("https://iptv.example.com/drm/direct/1113364", decision.targetUrl);
        assertEquals(MimeTypes.VIDEO_MP2T, decision.mimeType);
        assertEquals("", decision.drmType);
        assertFalse(decision.allowCompatibilityFallback);
    }

    @Test
    public void encryptedStreamInfoUsesDirectRouteInStandalone() {
        PlayerController.StreamInfo streamInfo = new PlayerController.StreamInfo();
        streamInfo.encrypted = true;
        streamInfo.type = "hls";
        streamInfo.sourceUrl = "https://origin.example.com/live/playlist.m3u8";

        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("9", "https://origin.example.com/live/playlist", "", PlaybackModeStore.MODE_AUTO, false, ""),
                false,
                streamInfo
        );

        assertEquals("https://origin.example.com/live/playlist.m3u8", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_M3U8, decision.mimeType);
        assertEquals("", decision.drmType);
    }

    @Test
    public void widevineStreamInfoUsesDirectRouteInStandalone() {
        PlayerController.StreamInfo streamInfo = new PlayerController.StreamInfo();
        streamInfo.drmType = "widevine";
        streamInfo.type = "dash";
        streamInfo.encrypted = true;
        streamInfo.sourceUrl = "https://origin.example.com/live/channel.mpd";

        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("15", "https://origin.example.com/live/channel.mpd", "", PlaybackModeStore.MODE_AUTO, false, ""),
                false,
                streamInfo
        );

        assertEquals("https://origin.example.com/live/channel.mpd", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_MPD, decision.mimeType);
        assertEquals("widevine", decision.drmType);
    }

    @Test
    public void clearkeyStreamInfoUsesDrmDirectRouteInStandalone() {
        PlayerController.StreamInfo streamInfo = new PlayerController.StreamInfo();
        streamInfo.drmType = "clearkey";
        streamInfo.type = "dash";
        streamInfo.encrypted = true;
        streamInfo.sourceUrl = "https://origin.example.com/live/channel.mpd";

        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("1071555", "https://iptv.example.com/live/1071555", "https://iptv.example.com/proxy/manifest/1071555", PlaybackModeStore.MODE_AUTO, false, ""),
                false,
                streamInfo
        );

        assertEquals("https://origin.example.com/live/channel.mpd", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_MPD, decision.mimeType);
        assertEquals("clearkey", decision.drmType);
    }

    @Test
    public void clearkeySmoothStreamInfoUsesNativeSmoothRoute() {
        PlayerController.StreamInfo streamInfo = new PlayerController.StreamInfo();
        streamInfo.drmType = "clearkey";
        streamInfo.type = "smooth";
        streamInfo.encrypted = true;
        streamInfo.sourceUrl = "https://origin.example.com/index.isml/Manifest";

        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("1600001", "https://iptv.example.com/live/1600001", "https://iptv.example.com/proxy/manifest/1600001", PlaybackModeStore.MODE_AUTO, false, ""),
                false,
                streamInfo
        );

        assertEquals("https://origin.example.com/index.isml/Manifest", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_SS, decision.mimeType);
        assertEquals("clearkey", decision.drmType);
        assertTrue(decision.allowCompatibilityFallback);
    }

    @Test
    public void espn4ClearKeyUsesServerSideLiveRoute() {
        PlayerController.StreamInfo streamInfo = new PlayerController.StreamInfo();
        streamInfo.drmType = "clearkey";
        streamInfo.type = "dash";
        streamInfo.encrypted = true;
        streamInfo.sourceUrl = "https://origin.example.com/live/espn4/default.mpd";
        streamInfo.clearKeyLicenseDataUri = "data:application/json;base64,abc";

        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("1071554", "https://iptv.example.com/live/1071554", "https://iptv.example.com/proxy/manifest/1071554", PlaybackModeStore.MODE_DIRECT, false, ""),
                false,
                streamInfo
        );

        assertEquals("https://iptv.example.com/hls/1071554/playlist.m3u8?codec=hevc", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_M3U8, decision.mimeType);
        assertEquals("", decision.drmType);
    }

    @Test
    public void espn4UsesHevcHlsBeforeStreamInfoResolves() {
        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("1071554", "https://iptv.example.com/live/1071554", "https://iptv.example.com/proxy/manifest/1071554", PlaybackModeStore.MODE_AUTO, false, ""),
                false,
                null
        );

        assertEquals("https://iptv.example.com/hls/1071554/playlist.m3u8?codec=hevc", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_M3U8, decision.mimeType);
        assertEquals("", decision.drmType);
    }

    @Test
    public void hlsStreamInfoPrefersDirectSourceUrlInStandalone() {
        PlayerController.StreamInfo streamInfo = new PlayerController.StreamInfo();
        streamInfo.type = "hls";
        streamInfo.sourceUrl = "https://origin.example.com/live/master.m3u8";

        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("20", "https://origin.example.com/live/no-extension", "https://iptv.example.com/proxy/manifest/20", PlaybackModeStore.MODE_AUTO, false, ""),
                false,
                streamInfo
        );

        assertEquals("https://origin.example.com/live/master.m3u8", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_M3U8, decision.mimeType);
        assertFalse(decision.useFallback);
        assertTrue(decision.allowCompatibilityFallback);
    }

    @Test
    public void adultHotPlatformKeepsProxyForSafety() {
        PlayerController.StreamInfo streamInfo = new PlayerController.StreamInfo();
        streamInfo.drmType = "widevine";
        streamInfo.type = "dash";
        streamInfo.encrypted = true;
        streamInfo.sourceUrl = "https://origin.example.com/live/hot-channel.mpd";

        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                requestWithPlatformAndProfile("400", "Hot Example", "Hot", "https://origin.example.com/live/hot-channel.mpd", "", PlaybackModeStore.MODE_AUTO, "proxy_manifest"),
                false,
                streamInfo
        );

        assertEquals("https://iptv.example.com/proxy/manifest/400", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_MPD, decision.mimeType);
        assertEquals("widevine", decision.drmType);
    }

    @Test
    public void adultDirectRecoveryDoesNotExposeInternalSourceUrl() {
        PlayerController.StreamInfo streamInfo = new PlayerController.StreamInfo();
        streamInfo.type = "hls";
        streamInfo.sourceUrl = "http://adult-proxy:8788/channel/internal/index.m3u8";

        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                requestWithPlatformAndProfile(
                        "401",
                        "Adult Example",
                        "Adultos",
                        "https://iptv.example.com/proxy/manifest/401",
                        "",
                        PlaybackModeStore.MODE_DIRECT,
                        "proxy_manifest"
                ),
                false,
                streamInfo
        );

        assertEquals("https://iptv.example.com/proxy/manifest/401", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_M3U8, decision.mimeType);
    }

    @Test
    public void explicitFallbackUsesFallbackUrl() {
        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("33", "https://origin.example.com/live/broken.mpd", "https://iptv.example.com/proxy/manifest/33", PlaybackModeStore.MODE_AUTO, false, ""),
                true,
                null
        );

        assertEquals("https://iptv.example.com/proxy/manifest/33", decision.targetUrl);
        assertTrue(decision.useFallback);
        assertFalse(decision.allowCompatibilityFallback);
    }

    @Test
    public void directVodPlaybackKeepsVodUrlAndDrmScheme() {
        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("vod-1", "https://vod.example.com/movie.mpd", "", PlaybackModeStore.MODE_AUTO, true, "clearkey"),
                false,
                null
        );

        assertEquals("https://vod.example.com/movie.mpd", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_MPD, decision.mimeType);
        assertEquals("clearkey", decision.drmType);
        assertFalse(decision.allowCompatibilityFallback);
    }

    @Test
    public void runtimeVodEndpointWithoutExtensionIsTreatedAsHls() {
        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("vod-runtime-1008", "https://iptv.example.com/api/vod/runtime/stream/1008", "", PlaybackModeStore.MODE_AUTO, true, ""),
                false,
                null
        );

        assertEquals("https://iptv.example.com/api/vod/runtime/stream/1008", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_M3U8, decision.mimeType);
        assertEquals("", decision.drmType);
    }

    @Test
    public void inferMimeTypeDetectsSmoothStreamingManifest() {
        assertEquals(MimeTypes.APPLICATION_SS, PlaybackRouteResolver.inferMimeType("https://origin.example.com/index.isml/Manifest"));
    }

    @Test
    public void inferMimeTypeDetectsDaznVodManifestRoute() {
        assertEquals(
                MimeTypes.APPLICATION_MPD,
                PlaybackRouteResolver.inferMimeType("https://fire.tvbep.com/api/vod/dazn/manifest/asset-1")
        );
    }

    @Test
    public void inferMimeTypeDetectsOrangeOfflineU7dTransportStream() {
        assertEquals(
                MimeTypes.VIDEO_MP2T,
                PlaybackRouteResolver.inferMimeType(
                        "https://iptv.bepllorens.com/api/offline/u7d/orange/stream?channel_id=1108469"
                )
        );
    }

    @Test
    public void inferMimeTypeIsNullSafe() {
        assertNull(PlaybackRouteResolver.inferMimeType(null));
    }

    @Test
    public void primeVodManifestWithoutExtensionIsDash() {
        String url = "https://fire.example.com/api/vod/prime/manifest/amzn1.dv.gti.01234567-89ab-cdef-0123-456789abcdef";

        assertEquals(MimeTypes.APPLICATION_MPD, PlaybackRouteResolver.inferMimeType(url));
        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("prime-vod", url, "", PlaybackModeStore.MODE_AUTO, true, "widevine"),
                false,
                null
        );
        assertEquals(MimeTypes.APPLICATION_MPD, decision.mimeType);
        assertEquals("widevine", decision.drmType);
    }

    private static PlayerController.PlaybackRequest request(String channelId, String playUrl, String fallbackUrl, String playbackMode, boolean directPlayback, String drmScheme) {
        return new PlayerController.PlaybackRequest(
                channelId,
                "Channel " + channelId,
                "Test Platform",
                playUrl,
                fallbackUrl,
                playbackMode,
                drmScheme,
                "",
                directPlayback
        );
    }

    private static PlayerController.PlaybackRequest requestWithProfile(String channelId, String playUrl, String fallbackUrl, String playbackMode, String playbackProfile) {
        return new PlayerController.PlaybackRequest(
                channelId,
                "Channel " + channelId,
                "Test Platform",
                playUrl,
                fallbackUrl,
                playbackMode,
                "",
                "",
                false,
                playbackProfile
        );
    }

    private static PlayerController.PlaybackRequest requestWithPlatformAndProfile(String channelId, String channelName, String platformName, String playUrl, String fallbackUrl, String playbackMode, String playbackProfile) {
        return new PlayerController.PlaybackRequest(
                channelId,
                channelName,
                platformName,
                playUrl,
                fallbackUrl,
                playbackMode,
                "",
                "",
                false,
                playbackProfile
        );
    }
}
