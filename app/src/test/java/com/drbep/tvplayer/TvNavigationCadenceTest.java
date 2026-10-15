package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TvNavigationCadenceTest {
    @Test public void isolatedPressAndPauseKeepNormalTransition() {
        TvNavigationCadence cadence = new TvNavigationCadence();
        cadence.onNavigation(1000, 0);
        assertEquals(100, cadence.durationMs());
        cadence.onNavigation(1080, 0);
        assertEquals(40, cadence.durationMs());
        cadence.onNavigation(1500, 0);
        assertEquals(100, cadence.durationMs());
    }

    @Test public void heldRemoteUsesShortTransitionEvenWithSlowDelivery() {
        TvNavigationCadence cadence = new TvNavigationCadence();
        cadence.onNavigation(1000, 0);
        cadence.onNavigation(1400, 1);
        assertEquals(40, cadence.durationMs());
    }
}
