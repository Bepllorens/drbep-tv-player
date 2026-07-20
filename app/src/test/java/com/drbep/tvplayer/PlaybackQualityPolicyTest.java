package com.drbep.tvplayer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlaybackQualityPolicyTest {
    @Test
    public void unknownModeFallsBackToAdaptiveAuto() {
        assertEquals(PlaybackQualityPolicy.AUTO, PlaybackQualityPolicy.normalize("unknown"));
        assertFalse(PlaybackQualityPolicy.forceHighestBitrate(null));
    }

    @Test
    public void dataSaverCapsSinglePlaybackAt720p() {
        assertEquals(1280, PlaybackQualityPolicy.maxWidth(PlaybackQualityPolicy.DATA_SAVER, false, false));
        assertEquals(720, PlaybackQualityPolicy.maxHeight(PlaybackQualityPolicy.DATA_SAVER, false, false));
        assertEquals(3_000_000, PlaybackQualityPolicy.maxBitrate(PlaybackQualityPolicy.DATA_SAVER, false));
    }

    @Test
    public void multiViewAlwaysUsesAConservativeTileProfile() {
        assertEquals(960, PlaybackQualityPolicy.maxWidth(PlaybackQualityPolicy.HIGH, false, true));
        assertEquals(540, PlaybackQualityPolicy.maxHeight(PlaybackQualityPolicy.HIGH, false, true));
        assertEquals(1_800_000, PlaybackQualityPolicy.maxBitrate(PlaybackQualityPolicy.HIGH, true));
    }

    @Test
    public void highQualityIsTheOnlyForcedMode() {
        assertTrue(PlaybackQualityPolicy.forceHighestBitrate(PlaybackQualityPolicy.HIGH));
        assertFalse(PlaybackQualityPolicy.forceHighestBitrate(PlaybackQualityPolicy.AUTO));
    }
}
