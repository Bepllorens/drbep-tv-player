package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RecordingsRepositoryTest {
    @Test
    public void completedItemKeepsDatabaseRecordingIdForDeletion() {
        RecordingsRepository.RecordingItem item = new RecordingsRepository.RecordingItem(
                "/grabaciones/partido.mp4", "partido.mp4", "/grabaciones/partido.mp4",
                0L, "", "Canal", "Partido", "", "completed", "", "", true, 42L
        );

        assertEquals(42L, item.recordingId);
        assertEquals("/grabaciones/partido.mp4", item.id);
    }

    @Test
    public void legacyCompletedItemDefaultsToMissingDatabaseId() {
        RecordingsRepository.RecordingItem item = new RecordingsRepository.RecordingItem(
                "legacy.mp4", "legacy.mp4", "legacy.mp4", 0L, "", "", "", "",
                "completed", "", "", true
        );

        assertEquals(0L, item.recordingId);
    }
}
