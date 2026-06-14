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
    public void dashUrlInAutoUsesProxyManifest() {
        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("42", "https://origin.example.com/live/channel.mpd", "", PlaybackModeStore.MODE_AUTO, false, ""),
                false,
                null
        );

        assertEquals("https://iptv.example.com/proxy/manifest/42", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_MPD, decision.mimeType);
        assertEquals("", decision.drmType);
        assertEquals(PlaybackModeStore.MODE_AUTO, decision.playbackMode);
        assertFalse(decision.useFallback);
        assertFalse(decision.allowCompatibilityFallback);
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
    public void proxyManifestProfileIgnoresLearnedDirectMode() {
        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                requestWithProfile("77", "https://blocked.example.com/live/channel.mpd", "", PlaybackModeStore.MODE_DIRECT, "proxy_manifest"),
                false,
                null
        );

        assertEquals("https://iptv.example.com/proxy/manifest/77", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_MPD, decision.mimeType);
        assertEquals(PlaybackModeStore.MODE_DIRECT, decision.playbackMode);
        assertFalse(decision.useFallback);
        assertFalse(decision.allowCompatibilityFallback);
    }

    @Test
    public void encryptedStreamInfoUsesClearProxyRoute() {
        PlayerController.StreamInfo streamInfo = new PlayerController.StreamInfo();
        streamInfo.encrypted = true;
        streamInfo.type = "hls";

        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("9", "https://origin.example.com/live/playlist", "", PlaybackModeStore.MODE_AUTO, false, ""),
                false,
                streamInfo
        );

        assertEquals("https://iptv.example.com/proxy/manifest/9?nodrm=1", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_M3U8, decision.mimeType);
        assertEquals("", decision.drmType);
    }

    @Test
    public void widevineStreamInfoUsesDrmProxyRoute() {
        PlayerController.StreamInfo streamInfo = new PlayerController.StreamInfo();
        streamInfo.drmType = "widevine";
        streamInfo.type = "dash";
        streamInfo.encrypted = true;

        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("15", "https://origin.example.com/live/channel.mpd", "", PlaybackModeStore.MODE_AUTO, false, ""),
                false,
                streamInfo
        );

        assertEquals("https://iptv.example.com/proxy/manifest/15", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_MPD, decision.mimeType);
        assertEquals("widevine", decision.drmType);
    }

    @Test
    public void clearkeyStreamInfoUsesDrmProxyRoute() {
        PlayerController.StreamInfo streamInfo = new PlayerController.StreamInfo();
        streamInfo.drmType = "clearkey";
        streamInfo.type = "dash";
        streamInfo.encrypted = true;

        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("1071555", "https://iptv.example.com/live/1071555", "https://iptv.example.com/proxy/manifest/1071555", PlaybackModeStore.MODE_AUTO, false, ""),
                false,
                streamInfo
        );

        assertEquals("https://iptv.example.com/proxy/manifest/1071555", decision.targetUrl);
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
    public void hlsStreamInfoPrefersFallbackUrlWhenAvailable() {
        PlayerController.StreamInfo streamInfo = new PlayerController.StreamInfo();
        streamInfo.type = "hls";

        PlaybackRouteResolver.Decision decision = resolver.buildDecision(
                request("20", "https://origin.example.com/live/no-extension", "https://iptv.example.com/proxy/manifest/20", PlaybackModeStore.MODE_AUTO, false, ""),
                false,
                streamInfo
        );

        assertEquals("https://iptv.example.com/proxy/manifest/20", decision.targetUrl);
        assertEquals(MimeTypes.APPLICATION_M3U8, decision.mimeType);
        assertFalse(decision.useFallback);
        assertFalse(decision.allowCompatibilityFallback);
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
    public void inferMimeTypeIsNullSafe() {
        assertNull(PlaybackRouteResolver.inferMimeType(null));
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
}
