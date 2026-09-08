package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AdaptivePlaybackQualityPolicyTest {
    @Test
    public void twoInstabilitiesInsideWindowDowngradeOneLevel() {
        AdaptivePlaybackQualityPolicy.State state = new AdaptivePlaybackQualityPolicy.State();

        assertFalse(state.recordInstability(1_000L).changed());
        AdaptivePlaybackQualityPolicy.Change change = state.recordInstability(80_000L);

        assertTrue(change.downgraded());
        assertEquals(AdaptivePlaybackQualityPolicy.LEVEL_720P, state.level());
        assertEquals(0, state.failuresInWindow());
    }

    @Test
    public void failuresOutsideWindowDoNotDowngrade() {
        AdaptivePlaybackQualityPolicy.State state = new AdaptivePlaybackQualityPolicy.State();

        state.recordInstability(1_000L);
        AdaptivePlaybackQualityPolicy.Change change = state.recordInstability(
                1_000L + AdaptivePlaybackQualityPolicy.FAILURE_WINDOW_MS + 1L
        );

        assertFalse(change.changed());
        assertEquals(AdaptivePlaybackQualityPolicy.LEVEL_NONE, state.level());
        assertEquals(1, state.failuresInWindow());
    }

    @Test
    public void repeatedWindowsStepDownToSafeFloor() {
        AdaptivePlaybackQualityPolicy.State state = new AdaptivePlaybackQualityPolicy.State();

        state.recordInstability(1_000L);
        state.recordInstability(2_000L);
        state.recordInstability(3_000L);
        AdaptivePlaybackQualityPolicy.Change change = state.recordInstability(4_000L);
        state.recordInstability(5_000L);
        state.recordInstability(6_000L);

        assertTrue(change.downgraded());
        assertEquals(AdaptivePlaybackQualityPolicy.LEVEL_540P, state.level());
    }

    @Test
    public void stablePlaybackRaisesOnlyOneLevelEveryFiveMinutes() {
        AdaptivePlaybackQualityPolicy.State state = new AdaptivePlaybackQualityPolicy.State();
        state.restoreLevel(AdaptivePlaybackQualityPolicy.LEVEL_540P);

        state.recordStable(10_000L);
        assertFalse(state.recordStable(10_000L + AdaptivePlaybackQualityPolicy.STABLE_UPGRADE_MS - 1L).changed());
        AdaptivePlaybackQualityPolicy.Change first = state.recordStable(
                10_000L + AdaptivePlaybackQualityPolicy.STABLE_UPGRADE_MS
        );

        assertTrue(first.upgraded());
        assertEquals(AdaptivePlaybackQualityPolicy.LEVEL_720P, state.level());
        assertFalse(state.recordStable(
                10_000L + (2L * AdaptivePlaybackQualityPolicy.STABLE_UPGRADE_MS) - 1L
        ).changed());
        assertTrue(state.recordStable(
                10_000L + (2L * AdaptivePlaybackQualityPolicy.STABLE_UPGRADE_MS)
        ).upgraded());
        assertEquals(AdaptivePlaybackQualityPolicy.LEVEL_NONE, state.level());
    }

    @Test
    public void pausedPlaybackDoesNotCountAsStableTime() {
        AdaptivePlaybackQualityPolicy.State state = new AdaptivePlaybackQualityPolicy.State();
        state.restoreLevel(AdaptivePlaybackQualityPolicy.LEVEL_720P);

        state.recordStable(10_000L);
        state.resetStabilityWindow();
        assertFalse(state.recordStable(10_000L + AdaptivePlaybackQualityPolicy.STABLE_UPGRADE_MS).changed());
        assertEquals(AdaptivePlaybackQualityPolicy.LEVEL_720P, state.level());
    }

    @Test
    public void capsAndRetentionAreBounded() {
        assertEquals(1280, AdaptivePlaybackQualityPolicy.maxWidth(AdaptivePlaybackQualityPolicy.LEVEL_720P));
        assertEquals(720, AdaptivePlaybackQualityPolicy.maxHeight(AdaptivePlaybackQualityPolicy.LEVEL_720P));
        assertEquals(3_000_000, AdaptivePlaybackQualityPolicy.maxBitrate(AdaptivePlaybackQualityPolicy.LEVEL_720P));
        assertEquals(960, AdaptivePlaybackQualityPolicy.maxWidth(AdaptivePlaybackQualityPolicy.LEVEL_540P));
        assertEquals(540, AdaptivePlaybackQualityPolicy.maxHeight(AdaptivePlaybackQualityPolicy.LEVEL_540P));
        assertEquals(1_800_000, AdaptivePlaybackQualityPolicy.maxBitrate(AdaptivePlaybackQualityPolicy.LEVEL_540P));
        assertTrue(AdaptivePlaybackQualityPolicy.isRetainedLevelValid(1, 20_000L, 10_000L));
        assertFalse(AdaptivePlaybackQualityPolicy.isRetainedLevelValid(1, 10_000L, 10_000L));
        assertFalse(AdaptivePlaybackQualityPolicy.isRetainedLevelValid(0, 20_000L, 10_000L));
    }

    @Test
    public void adaptationOnlyRunsInAutomaticMode() {
        assertTrue(AdaptivePlaybackQualityPolicy.isAutomaticMode("auto"));
        assertFalse(AdaptivePlaybackQualityPolicy.isAutomaticMode("high"));
        assertFalse(AdaptivePlaybackQualityPolicy.isAutomaticMode("data_saver"));
    }
}
