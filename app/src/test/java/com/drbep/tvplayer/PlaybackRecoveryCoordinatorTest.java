package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class PlaybackRecoveryCoordinatorTest {
    @Test
    public void persistsCompatibilityAsLearnedRoute() {
        Map<String, String> temporary = new HashMap<>();
        Map<String, String> learned = new HashMap<>();
        Map<String, Set<String>> attempts = new HashMap<>();
        PlaybackRecoveryCoordinator coordinator = new PlaybackRecoveryCoordinator(temporary, learned, attempts);

        coordinator.setLearnedMode("1105031", PlaybackModeStore.MODE_COMPAT);

        assertEquals(PlaybackModeStore.MODE_COMPAT, learned.get("1105031"));
        assertEquals(PlaybackModeStore.MODE_COMPAT, coordinator.learnedMode("1105031"));
        assertEquals(PlaybackModeStore.MODE_AUTO, coordinator.nextMode(PlaybackModeStore.MODE_COMPAT));
    }
}
