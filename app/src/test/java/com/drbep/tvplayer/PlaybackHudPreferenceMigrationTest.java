package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlaybackHudPreferenceMigrationTest {
    @Test
    public void firstV413LaunchActivatesModernHudEvenWhenClassicWasStored() {
        assertTrue(PlaybackHudPreferenceMigration.resolveModernHud(false, false));
    }

    @Test
    public void laterLaunchesRespectTheUsersChoice() {
        assertFalse(PlaybackHudPreferenceMigration.resolveModernHud(false, true));
        assertTrue(PlaybackHudPreferenceMigration.resolveModernHud(true, true));
    }
}
