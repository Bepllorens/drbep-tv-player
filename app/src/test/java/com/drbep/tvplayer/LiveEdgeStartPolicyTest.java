package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class LiveEdgeStartPolicyTest {
    @Test public void retainsMovistarLiveStartExceptDedicatedIsmPath() {
        assertTrue(LiveEdgeStartPolicy.shouldForce("Movistar", false, false, 0, false));
        assertFalse(LiveEdgeStartPolicy.shouldForce("Movistar ISM", false, false, 0, true));
    }
    @Test public void neverResetsVodEvenWhenStartingAtZero() {
        assertFalse(LiveEdgeStartPolicy.shouldForce("Movistar Peliculas", true, false, 64739, false));
        assertFalse(LiveEdgeStartPolicy.shouldForce("Movistar Peliculas", true, false, 0, false));
    }
    @Test public void neverResetsReplayOrExplicitResume() {
        assertFalse(LiveEdgeStartPolicy.shouldForce("Movistar", false, true, 0, false));
        assertFalse(LiveEdgeStartPolicy.shouldForce("Movistar", false, false, 30000, false));
    }
    @Test public void ignoresOtherOrMissingPlatforms() {
        assertFalse(LiveEdgeStartPolicy.shouldForce("Tivify", false, false, 0, false));
        assertFalse(LiveEdgeStartPolicy.shouldForce(null, false, false, 0, false));
    }
}
