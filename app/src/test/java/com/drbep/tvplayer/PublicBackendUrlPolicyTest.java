package com.drbep.tvplayer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PublicBackendUrlPolicyTest {
    @Test
    public void rebasesLegacyInternalPlaybackUrlToOfflinePublicHost() {
        assertEquals(
                "https://fire.tvbep.com/api/vod/movistar/manifest/980918?start=1",
                PublicBackendUrlPolicy.rebaseLegacyUrl(
                        "https://iptv.bepllorens.com/api/vod/movistar/manifest/980918?start=1",
                        "https://fire.tvbep.com/"
                )
        );
    }

    @Test
    public void keepsProviderUrlUntouched() {
        assertEquals(
                "https://provider.example/live/channel.mpd",
                PublicBackendUrlPolicy.rebaseLegacyUrl(
                        "https://provider.example/live/channel.mpd",
                        "https://fire.tvbep.com"
                )
        );
    }
}
