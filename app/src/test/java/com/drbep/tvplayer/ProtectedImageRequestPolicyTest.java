package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProtectedImageRequestPolicyTest {
    @Test
    public void authenticatesApplePostersOnlyOnTrustedOrigin() {
        String path = "/api/vod/private/appletv/poster?kind=movie&id=mayday";
        assertTrue(ProtectedImageRequestPolicy.requiresDeviceAuth("https://example.test" + path, "https://example.test/play"));
        assertFalse(ProtectedImageRequestPolicy.requiresDeviceAuth("https://other.test" + path, "https://example.test"));
        assertFalse(ProtectedImageRequestPolicy.requiresDeviceAuth("https://example.test:444" + path, "https://example.test"));
        assertFalse(ProtectedImageRequestPolicy.requiresDeviceAuth("https://example.test/api/vod/private/appletv/poster-elsewhere", "https://example.test"));
    }
    @Test
    public void authenticatesDisneyPostersOnlyOnTrustedOrigin() {
        String path = "/api/vod/private/disneyplus/poster?kind=movie&id=test&revision=test";
        assertTrue(ProtectedImageRequestPolicy.requiresDeviceAuth("https://example.test" + path, "https://example.test/play"));
        assertFalse(ProtectedImageRequestPolicy.requiresDeviceAuth("https://other.test" + path, "https://example.test"));
        assertFalse(ProtectedImageRequestPolicy.requiresDeviceAuth("https://example.test:444" + path, "https://example.test"));
        assertFalse(ProtectedImageRequestPolicy.requiresDeviceAuth("https://example.test/api/vod/private/disneyplus/poster-elsewhere", "https://example.test"));
    }

    @Test
    public void authenticatesPlexPostersOnlyOnTrustedOrigin() {
        assertTrue(ProtectedImageRequestPolicy.requiresDeviceAuth(
                "https://iptv.bepllorens.com/api/vod/plex/image/472733",
                "https://fire.tvbep.com",
                "https://iptv.bepllorens.com/play"
        ));
        assertFalse(ProtectedImageRequestPolicy.requiresDeviceAuth(
                "https://example.test/api/vod/plex/image/472733",
                "https://iptv.bepllorens.com/play"
        ));
        assertFalse(ProtectedImageRequestPolicy.requiresDeviceAuth(
                "https://iptv.bepllorens.com/assets/poster.jpg",
                "https://iptv.bepllorens.com/play"
        ));
    }
    @org.junit.Test public void hboPostersAuthenticateOnlyAgainstTrustedOrigin() {
        org.junit.Assert.assertTrue(ProtectedImageRequestPolicy.requiresDeviceAuth("https://fire.tvbep.com/api/vod/private/hbomax/poster?id=a", "https://fire.tvbep.com"));
        org.junit.Assert.assertFalse(ProtectedImageRequestPolicy.requiresDeviceAuth("https://untrusted.invalid/api/vod/private/hbomax/poster?id=a", "https://fire.tvbep.com"));
    }
}
