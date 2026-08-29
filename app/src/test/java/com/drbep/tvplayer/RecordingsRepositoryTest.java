package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RecordingsRepositoryTest {
    @Test
    public void completedItemKeepsDatabaseRecordingIdForDeletion() {
        RecordingsRepository.RecordingItem item = new RecordingsRepository.RecordingItem(
                "/grabaciones/partido.mp4", "partido.mp4", "/grabaciones/partido.mp4",
                0L, "", "Canal", "Partido", "", "completed", "", "", true, 42L
        );

        assertEquals(42L, item.recordingId);
        assertEquals(1, item.relatedRecordingIds.size());
        assertEquals(Long.valueOf(42L), item.relatedRecordingIds.get(0));
        assertEquals("/grabaciones/partido.mp4", item.id);
    }

    @Test
    public void legacyCompletedItemDefaultsToMissingDatabaseId() {
        RecordingsRepository.RecordingItem item = new RecordingsRepository.RecordingItem(
                "legacy.mp4", "legacy.mp4", "legacy.mp4", 0L, "", "", "", "",
                "completed", "", "", true
        );

        assertEquals(0L, item.recordingId);
        assertTrue(item.relatedRecordingIds.isEmpty());
    }

    @Test
    public void preferredArtifactAvoidsTemporaryMergeFiles() {
        int regular = RecordingsRepository.artifactPreference("partido.mp4", "partido.merge.123.mp4");
        int merge = RecordingsRepository.artifactPreference("partido.merge.123.mp4", "partido.merge.123.mp4");

        assertTrue(regular > merge);
    }

    @Test
    public void webMp4IsPreferredForOfflinePlayback() {
        int web = RecordingsRepository.artifactPreference("partido.web.mp4", "");
        int transportStream = RecordingsRepository.artifactPreference("partido.ts", "");

        assertTrue(web > transportStream);
    }
}
