package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class CatalogSnapshotStoreTest {
    @Test
    public void binaryPermissionsPreserveDisneyGrantAndFollowingFields() throws Exception {
        OfflinePermissions original = new OfflinePermissions();
        original.vodEnabled = true;
        original.disneyplusVodEnabled = true;
        original.canScheduleRecordings = true;
        original.allowedPlatformIds.add(42);
        original.protectedFilterKeys.add("vod:adult");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CatalogSnapshotStore.writeOfflinePermissions(new DataOutputStream(bytes), original);
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        OfflinePermissions restored = CatalogSnapshotStore.readOfflinePermissions(input, 10);
        assertTrue(restored.allowsDisneyplusVod());
        assertEquals(original.canScheduleRecordings, restored.canScheduleRecordings);
        assertEquals(original.allowedPlatformIds, restored.allowedPlatformIds);
        assertEquals(original.protectedFilterKeys, restored.protectedFilterKeys);
        assertEquals(-1, input.read());
    }

    @Test
    public void legacyBinaryPermissionsDoNotGrantDisney() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        for (int i = 0; i < 12; i++) output.writeBoolean(true);
        for (int i = 0; i < 4; i++) output.writeInt(0);
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        OfflinePermissions restored = CatalogSnapshotStore.readOfflinePermissions(input, 9);
        assertTrue(restored.vodEnabled);
        assertFalse(restored.allowsDisneyplusVod());
        assertTrue(restored.canScheduleRecordings);
        assertEquals(-1, input.read());
    }

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

    @Test
    public void startupLiveQueryPreservesExistingParameters() {
        assertEquals(
                "https://example.test/api/offline/snapshot?device_id=fire&startup_live=1",
                CatalogSnapshotStore.appendStartupLiveQuery(
                        "https://example.test/api/offline/snapshot?device_id=fire"
                )
        );
    }

    @Test
    public void snapshotModeQueryReplacesConflictingMode() {
        assertEquals(
                "https://example.test/api/offline/snapshot?device_id=fire&startup_lite=1",
                CatalogSnapshotStore.appendStartupLiteQuery(
                        "https://example.test/api/offline/snapshot?device_id=fire&startup_live=1"
                )
        );
    }

    @Test
    public void snapshotSourceUrlDoesNotPersistTransientMode() {
        assertEquals(
                "https://example.test/api/offline/snapshot?device_id=fire#catalog",
                CatalogSnapshotStore.normalizeSnapshotSourceUrl(
                        "https://example.test/api/offline/snapshot?device_id=fire&startup_live=1#catalog"
                )
        );
    }
}
