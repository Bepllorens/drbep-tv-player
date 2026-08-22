package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParentalControlStoreTest {
    @Test
    public void configuredPinUsesSlowHashAndVerifies() {
        ParentalControlStore store = new ParentalControlStore(null, "test");

        store.setPin("4521");

        assertTrue(store.hasPinConfigured());
        assertTrue(store.verifyPin("4521"));
        assertFalse(store.verifyPin("1234"));
    }

    @Test
    public void repeatedFailuresTemporarilyBlockVerification() {
        ParentalControlStore store = new ParentalControlStore(null, "test");
        store.setPin("4521");

        for (int attempt = 0; attempt < 5; attempt++) {
            assertFalse(store.verifyPin("1111"));
        }

        assertTrue(store.getBlockedRemainingMs() > 0L);
        assertFalse(store.verifyPin("4521"));
    }
}
