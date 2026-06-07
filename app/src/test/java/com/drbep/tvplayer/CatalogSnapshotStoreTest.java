package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
}
