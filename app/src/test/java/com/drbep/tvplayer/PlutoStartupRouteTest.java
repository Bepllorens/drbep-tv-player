package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlutoStartupRouteTest {
    private PlayerController.PlaybackRequest request(String platform, String fallback, boolean vod) {
        return new PlayerController.PlaybackRequest("1", "Test", platform,
                "https://origin.example.com/master.m3u8", fallback,
                PlaybackModeStore.MODE_AUTO, "", "", false, vod, "");
    }
    @Test public void livePlutoStartsWithKnownBackendProxy() {
        assertTrue(PlayerController.shouldStartWithCompatibilityFallback(request("PlutoTV", "https://fire.tvbep.com/proxy/manifest/1", false)));
    }
    @Test public void otherProvidersAndVodKeepTheirRoutes() {
        assertFalse(PlayerController.shouldStartWithCompatibilityFallback(request("Runtime", "https://fire.tvbep.com/proxy/manifest/1", false)));
        assertFalse(PlayerController.shouldStartWithCompatibilityFallback(request("PlutoTV", "https://fire.tvbep.com/proxy/manifest/1", true)));
    }
    @Test public void noInventedFallback() {
        assertFalse(PlayerController.shouldStartWithCompatibilityFallback(request("PlutoTV", "", false)));
        assertFalse(PlayerController.shouldStartWithCompatibilityFallback(request("PlutoTV", "https://origin.example.com/a.m3u8", false)));
    }
}
