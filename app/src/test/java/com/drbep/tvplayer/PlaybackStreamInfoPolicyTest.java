package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class PlaybackStreamInfoPolicyTest {
    @Test
    public void orangeLiveResolvesBeforePlayback() {
        ChannelItem channel = channel("10", "Orange 1", "Orange TV", "Orange", "https://fire.tvbep.com/orange/live/10.mpd", "", false, "", "", false);
        PlayerController.PlaybackRequest request = request(channel, false);

        assertTrue(PlaybackStreamInfoPolicy.shouldResolveBeforePlayback(true, channel, request, channel.name));
    }

    @Test
    public void movistarIsmSmoothResolvesBeforePlayback() {
        ChannelItem channel = channel("20", "La 1", "Movistar ISM", "Movistar", "https://origin.example.com/live/index.isml/Manifest", "", false, "", "", false);
        PlayerController.PlaybackRequest request = request(channel, false);

        assertTrue(PlaybackStreamInfoPolicy.shouldResolveBeforePlayback(true, channel, request, channel.name));
    }

    @Test
    public void movistarBackendFallbackResolvesBeforePlayback() {
        ChannelItem channel = channel("21", "DAZN 1", "Movistar", "Deportes", "https://origin.example.com/live/channel", "https://fire.tvbep.com/hls/ism/21/index.m3u8", false, "", "", false);
        PlayerController.PlaybackRequest request = request(channel, false);

        assertTrue(PlaybackStreamInfoPolicy.shouldResolveBeforePlayback(true, channel, request, channel.name));
    }

    @Test
    public void vodDoesNotResolveBeforePlayback() {
        ChannelItem channel = channel("30", "Pelicula", "Movistar ISM", "VOD", "https://origin.example.com/vod/movie.mpd", "", true, "widevine", "", false);
        PlayerController.PlaybackRequest request = request(channel, false);

        assertFalse(PlaybackStreamInfoPolicy.shouldResolveBeforePlayback(true, channel, request, channel.name));
    }

    @Test
    public void directClearkeyDashResolvesBeforePlayback() {
        ChannelItem channel = channel("40", "Direct DRM", "TDT", "TDT", "https://origin.example.com/live/channel.mpd", "", false, "clearkey", "", true);
        PlayerController.PlaybackRequest request = request(channel, true);

        assertTrue(PlaybackStreamInfoPolicy.shouldResolveBeforePlayback(true, channel, request, channel.name));
    }

    @Test
    public void plainDirectHlsDoesNotResolveBeforePlayback() {
        ChannelItem channel = channel("50", "Direct HLS", "TDT", "TDT", "https://origin.example.com/live/master.m3u8", "", false, "", "", true);
        PlayerController.PlaybackRequest request = request(channel, true);

        assertFalse(PlaybackStreamInfoPolicy.shouldResolveBeforePlayback(true, channel, request, channel.name));
    }

    @Test
    public void nonStandaloneDoesNotResolveBeforePlayback() {
        ChannelItem channel = channel("60", "Orange 2", "Orange TV", "Orange", "https://fire.tvbep.com/orange/live/60.mpd", "", false, "", "", false);
        PlayerController.PlaybackRequest request = request(channel, false);

        assertFalse(PlaybackStreamInfoPolicy.shouldResolveBeforePlayback(false, channel, request, channel.name));
    }

    private static ChannelItem channel(String id, String name, String platform, String group, String playUrl, String fallbackUrl, boolean vod, String drmScheme, String drmLicenseUrl, boolean directPlayback) {
        return new ChannelItem(
                id,
                name,
                "",
                "",
                group,
                playUrl,
                fallbackUrl,
                0,
                0,
                vod,
                false,
                1,
                platform,
                new ArrayList<>(),
                drmScheme,
                drmLicenseUrl,
                "",
                directPlayback,
                ""
        );
    }

    private static PlayerController.PlaybackRequest request(ChannelItem channel, boolean directPlayback) {
        return new PlayerController.PlaybackRequest(
                channel.id,
                channel.name,
                channel.platformName,
                channel.playUrl,
                channel.fallbackPlayUrl,
                PlaybackModeStore.MODE_AUTO,
                channel.drmScheme,
                channel.drmLicenseUrl,
                directPlayback,
                channel.isVod,
                channel.playbackProfile
        );
    }
}
