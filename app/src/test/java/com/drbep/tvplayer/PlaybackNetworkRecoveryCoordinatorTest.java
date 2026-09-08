package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlaybackNetworkRecoveryCoordinatorTest {
    @Test
    public void lossWaitsAndRestoreSchedulesBoundedRecovery() {
        PlaybackNetworkRecoveryCoordinator coordinator = new PlaybackNetworkRecoveryCoordinator();

        PlaybackNetworkRecoveryCoordinator.Outcome lost = coordinator.updateNetworkState(
                false, false, "sin red", true, false, false, "playing"
        );
        assertEquals(PlaybackNetworkRecoveryCoordinator.Action.WAIT_FOR_NETWORK, lost.action);
        assertTrue(coordinator.snapshot().recoveryPending);

        PlaybackNetworkRecoveryCoordinator.Outcome restored = coordinator.updateNetworkState(
                true, true, "wifi", true, false, false, "waiting_network"
        );
        assertEquals(PlaybackNetworkRecoveryCoordinator.Action.SCHEDULE_RECOVERY, restored.action);
        assertEquals(750L, restored.delayMs);
        assertTrue(coordinator.beginScheduledRecovery(true));
        assertEquals(1, coordinator.snapshot().recoveryAttempts);
    }

    @Test
    public void activePlaybackAfterRestoreDoesNotRestartStream() {
        PlaybackNetworkRecoveryCoordinator coordinator = new PlaybackNetworkRecoveryCoordinator();
        coordinator.updateNetworkState(false, false, "sin red", true, false, false, "playing");

        PlaybackNetworkRecoveryCoordinator.Outcome restored = coordinator.updateNetworkState(
                true, true, "ethernet", true, false, true, "waiting_network"
        );

        assertEquals(PlaybackNetworkRecoveryCoordinator.Action.MARK_PLAYING, restored.action);
        assertFalse(coordinator.snapshot().recoveryPending);
        assertEquals(0, coordinator.snapshot().recoveryAttempts);
    }

    @Test
    public void errorsOnlyDeferWhileOfflineAndFirstFrameClearsPending() {
        PlaybackNetworkRecoveryCoordinator coordinator = new PlaybackNetworkRecoveryCoordinator();
        assertFalse(coordinator.deferUntilRestored(true));
        coordinator.updateNetworkState(false, false, "sin red", false, false, false, "idle");
        assertFalse(coordinator.deferUntilRestored(false));
        assertTrue(coordinator.deferUntilRestored(true));
        coordinator.onFirstFrame();
        assertFalse(coordinator.snapshot().recoveryPending);
    }

    @Test
    public void staleScheduledRecoveryCannotRunAndChannelChangeResetsAttempts() {
        PlaybackNetworkRecoveryCoordinator coordinator = new PlaybackNetworkRecoveryCoordinator();
        coordinator.updateNetworkState(false, false, "sin red", true, false, false, "playing");
        coordinator.updateNetworkState(true, true, "wifi", true, false, false, "waiting_network");
        assertFalse(coordinator.beginScheduledRecovery(false));
        assertTrue(coordinator.beginScheduledRecovery(true));
        assertEquals(1, coordinator.snapshot().recoveryAttempts);
        coordinator.resetAttemptsForChannelChange(true);
        assertEquals(0, coordinator.snapshot().recoveryAttempts);
    }
}
