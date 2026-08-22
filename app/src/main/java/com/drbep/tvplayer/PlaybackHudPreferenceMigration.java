package com.drbep.tvplayer;

final class PlaybackHudPreferenceMigration {
    private PlaybackHudPreferenceMigration() {
    }

    static boolean resolveModernHud(boolean storedModernHud, boolean v413Migrated) {
        return v413Migrated ? storedModernHud : true;
    }
}
