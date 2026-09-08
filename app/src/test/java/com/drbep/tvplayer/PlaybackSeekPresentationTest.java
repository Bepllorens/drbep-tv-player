package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlaybackSeekPresentationTest {
    @Test public void onDemandHidesLiveWithoutChangingSeekCoordinates() {
        PlayerController.PlaybackSeekState raw = new PlayerController.PlaybackSeekState(200, 8000, 1200, "raw", true);
        PlayerController.PlaybackSeekState result = PlaybackSeekPresentation.forOnDemand(raw, true);
        assertFalse(result.liveCapable);
        assertEquals(raw.startMs, result.startMs);
        assertEquals(raw.endMs, result.endMs);
        assertEquals(raw.currentMs, result.currentMs);
        assertTrue(raw.liveCapable);
    }
    @Test public void liveAndReplayStatesAreNotChanged() {
        PlayerController.PlaybackSeekState raw = new PlayerController.PlaybackSeekState(0, 8000, 1200, "raw", true);
        assertSame(raw, PlaybackSeekPresentation.forOnDemand(raw, false));
        assertNull(PlaybackSeekPresentation.forOnDemand(null, true));
    }
    @Test public void alreadyOnDemandStateIsReused() {
        PlayerController.PlaybackSeekState raw = new PlayerController.PlaybackSeekState(0, 8000, 1200, "raw", false);
        assertSame(raw, PlaybackSeekPresentation.forOnDemand(raw, true));
    }
}
