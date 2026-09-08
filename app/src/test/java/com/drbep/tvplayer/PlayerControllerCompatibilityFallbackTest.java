package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlayerControllerCompatibilityFallbackTest {
    @Test
    public void learnedCompatibilityModeStartsWithFallbackUrl() {
        PlayerController.PlaybackRequest request = new PlayerController.PlaybackRequest(
                "1105031",
                "LA 1",
                "Movistar",
                "https://fire.tvbep.com/proxy/manifest/1105031",
                "https://fire.tvbep.com/proxy/manifest/1105031",
                PlaybackModeStore.MODE_COMPAT,
                "clearkey",
                "",
                false
        );

        assertTrue(PlayerController.shouldStartWithCompatibilityFallback(request));
    }

    @Test
    public void compatibilityModeWithoutFallbackRemainsOnNormalRoute() {
        PlayerController.PlaybackRequest request = new PlayerController.PlaybackRequest(
                "1105031",
                "LA 1",
                "Movistar",
                "https://origin.example.com/manifest.mpd",
                "",
                PlaybackModeStore.MODE_COMPAT,
                "clearkey",
                "",
                false
        );

        assertFalse(PlayerController.shouldStartWithCompatibilityFallback(request));
    }

    @Test
    public void movistarHlsFetchesDeclaredQualityWhenStartupHasNoStreamInfo() {
        PlayerController.PlaybackRequest request = new PlayerController.PlaybackRequest(
                "1113365",
                "Antena 3",
                "Movistar HLS",
                "https://fire.tvbep.com/drm/direct/1113365",
                "",
                PlaybackModeStore.MODE_AUTO,
                "",
                "",
                true
        );

        assertTrue(PlayerController.shouldFetchDeclaredQuality(request, null));

        PlayerController.StreamInfo complete = new PlayerController.StreamInfo();
        complete.videoWidth = 1920;
        complete.videoHeight = 1080;
        complete.bandwidthBps = 6_499_279L;
        assertFalse(PlayerController.shouldFetchDeclaredQuality(request, complete));
    }

    @Test
    public void leavingU7dForcesMedia3SourceCleanup() {
        PlayerController.PlaybackRequest replay = new PlayerController.PlaybackRequest(
                "u7d-1105475",
                "Operación África",
                "Movistar U7D",
                "https://fire.tvbep.com/api/offline/u7d/movistar-ism/stream",
                "",
                PlaybackModeStore.MODE_PROXY,
                "",
                "",
                false,
                true,
                "u7d_proxy"
        );

        assertTrue(PlayerController.shouldResetPlayerBeforeSourceTransition(replay));
        assertFalse(PlayerController.shouldResetPlayerBeforeSourceTransition(null));
    }

    @Test
    public void vodLoadingRemainsVisibleUntilFirstFrame() {
        PlayerController.PlaybackRequest vod = new PlayerController.PlaybackRequest(
                "vod-1", "Pelicula", "Plex", "https://fire.tvbep.com/api/vod/plex/stream/1", "",
                PlaybackModeStore.MODE_AUTO, "", "", true, true, "plex_direct"
        );
        PlayerController.PlaybackRequest live = new PlayerController.PlaybackRequest(
                "1", "Canal", "TV", "https://fire.tvbep.com/proxy/manifest/1", "",
                PlaybackModeStore.MODE_AUTO, "", "", false
        );
		PlayerController.PlaybackRequest u7d = new PlayerController.PlaybackRequest(
				"u7d-1", "Programa", "Movistar U7D", "https://fire.tvbep.com/api/offline/u7d/movistar-ism/stream", "",
				PlaybackModeStore.MODE_PROXY, "", "", true, false, "u7d_proxy"
		);

        assertTrue(MainActivity.shouldKeepLoadingUntilFirstFrame(vod));
		assertTrue(MainActivity.shouldKeepLoadingUntilFirstFrame(u7d));
        assertFalse(MainActivity.shouldKeepLoadingUntilFirstFrame(live));
        assertTrue(PlayerController.playbackReadTimeoutMs(vod) > PlayerController.playbackReadTimeoutMs(live));
    }
}
