package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProtectedImageRequestPolicyTest {
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
}
