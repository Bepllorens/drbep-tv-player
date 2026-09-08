package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlaybackSystemIntegrationPolicyTest {
    @Test
    public void primaryPlaybackOwnsMediaSessionAndAudioFocus() {
        assertTrue(PlayerController.shouldRegisterSystemMediaControls(false));
        assertTrue(PlayerController.shouldHandleSystemAudioFocus(false));
    }

    @Test
    public void multiViewTilesDoNotCompeteForSystemMediaSessionOrAudioFocus() {
        assertFalse(PlayerController.shouldRegisterSystemMediaControls(true));
        assertFalse(PlayerController.shouldHandleSystemAudioFocus(true));
    }
}
