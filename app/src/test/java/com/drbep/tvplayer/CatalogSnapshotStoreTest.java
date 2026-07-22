package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;

public class CatalogSnapshotStoreTest {
    @Test
    public void suspiciousDropRejectsLargeCatalogRegression() {
        assertTrue(CatalogSnapshotStore.isSuspiciousCatalogDrop(150, 80, 20, 10));
    }

    @Test
    public void suspiciousDropAllowsSmallOrModerateChanges() {
        assertFalse(CatalogSnapshotStore.isSuspiciousCatalogDrop(150, 120, 20, 10));
        assertFalse(CatalogSnapshotStore.isSuspiciousCatalogDrop(12, 1, 20, 10));
    }

    @Test
    public void oversizedSnapshotIsRejectedBeforeReading() throws Exception {
        File file = File.createTempFile("catalog-snapshot", ".json");
        try (RandomAccessFile output = new RandomAccessFile(file, "rw")) {
            output.setLength(CatalogSnapshotStore.MAX_LOCAL_SNAPSHOT_BYTES + 1L);
        }
        try {
            assertThrows(IllegalStateException.class, () -> CatalogSnapshotStore.ensureSnapshotFileWithinLimit(file));
        } finally {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }
}
