package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OfflineCatalogRecoveryPolicyTest {
    private static final String SOURCE_URL = "https://example.invalid/catalog";

    @Test
    public void recognizesFailuresThatCanBeRepairedByRefreshingTheCatalog() {
        assertTrue(OfflineCatalogRecoveryPolicy.isCatalogRelated("HTTP 403 forbidden"));
        assertTrue(OfflineCatalogRecoveryPolicy.isCatalogRelated("InvalidResponseCode: 404"));
        assertTrue(OfflineCatalogRecoveryPolicy.isCatalogRelated("source error: token expired"));
        assertTrue(OfflineCatalogRecoveryPolicy.isCatalogRelated("channel not found"));
        assertFalse(OfflineCatalogRecoveryPolicy.isCatalogRelated("decoder initialization failed"));
        assertFalse(OfflineCatalogRecoveryPolicy.isCatalogRelated("buffer underrun"));
    }

    @Test
    public void requiresUsableCredentialsAndSource() {
        long now = 1_000_000L;
        assertFalse(OfflineCatalogRecoveryPolicy.shouldRefresh("HTTP 403", now, 0L, false, SOURCE_URL));
        assertFalse(OfflineCatalogRecoveryPolicy.shouldRefresh("HTTP 403", now, 0L, true, "  "));
        assertTrue(OfflineCatalogRecoveryPolicy.shouldRefresh("HTTP 403", now, 0L, true, SOURCE_URL));
    }

    @Test
    public void appliesCooldownWithoutBlockingTheFirstRecovery() {
        long now = 2_000_000L;
        long cooldown = OfflineCatalogRecoveryPolicy.REFRESH_COOLDOWN_MS;
        assertTrue(OfflineCatalogRecoveryPolicy.shouldRefresh("token expired", now, 0L, true, SOURCE_URL));
        assertFalse(OfflineCatalogRecoveryPolicy.shouldRefresh("token expired", now, now - cooldown + 1L, true, SOURCE_URL));
        assertTrue(OfflineCatalogRecoveryPolicy.shouldRefresh("token expired", now, now - cooldown, true, SOURCE_URL));
        assertFalse(OfflineCatalogRecoveryPolicy.shouldRefresh("decoder failed", now, 0L, true, SOURCE_URL));
    }
}
