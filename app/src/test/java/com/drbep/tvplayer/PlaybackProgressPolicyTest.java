package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlaybackProgressPolicyTest {
    @Test
    public void watchesOnlyLiveCompactPlaybackThatShouldBeAdvancing() {
        assertTrue(PlaybackProgressPolicy.shouldWatch(true, false, true, true, true));
        assertFalse(PlaybackProgressPolicy.shouldWatch(false, false, true, true, true));
        assertFalse(PlaybackProgressPolicy.shouldWatch(true, true, true, true, true));
        assertFalse(PlaybackProgressPolicy.shouldWatch(true, false, false, true, true));
        assertFalse(PlaybackProgressPolicy.shouldWatch(true, false, true, false, true));
        assertFalse(PlaybackProgressPolicy.shouldWatch(true, false, true, true, false));
    }

    @Test
    public void detectsRealProgressAndSeeks() {
        assertTrue(PlaybackProgressPolicy.hasAdvanced(-1L, 1_000L));
        assertTrue(PlaybackProgressPolicy.hasAdvanced(10_000L, 10_300L));
        assertTrue(PlaybackProgressPolicy.hasAdvanced(10_000L, 5_000L));
        assertFalse(PlaybackProgressPolicy.hasAdvanced(10_000L, 10_100L));
    }

    @Test
    public void boundsAutomaticRecoveriesAndHonorsCooldown() {
        assertTrue(PlaybackProgressPolicy.shouldRecover(12_000L, 0, 20_000L));
        assertFalse(PlaybackProgressPolicy.shouldRecover(11_999L, 0, 20_000L));
        assertFalse(PlaybackProgressPolicy.shouldRecover(12_000L, 0, 19_999L));
        assertFalse(PlaybackProgressPolicy.shouldRecover(12_000L, 3, 20_000L));
    }
}
