package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParentalControlStoreTest {
    @Test
    public void verificationDoesNotUnlockUntilExplicitlyGranted() {
        ParentalControlStore store = new ParentalControlStore(null, "test");
        store.setPin("4521");
        assertTrue(store.verifyPin("4521"));
        assertFalse(store.isUnlocked());
        store.unlockSession();
        assertTrue(store.isUnlocked());
        store.lockSession();
        assertFalse(store.isUnlocked());
        assertTrue(store.hasPinConfigured());
    }

    @Test
    public void changingPinRevokesPreviousUnlockAndOldPin() {
        ParentalControlStore store = new ParentalControlStore(null, "test");
        store.setPin("4521");
        store.unlockSession();
        store.setPin("6789");
        assertFalse(store.isUnlocked());
        assertFalse(store.verifyPin("4521"));
        assertTrue(store.verifyPin("6789"));
    }

    @Test
    public void clearedOrUnconfiguredStoreCannotUnlock() {
        ParentalControlStore store = new ParentalControlStore(null, "test");
        store.unlockSession();
        assertFalse(store.isUnlocked());
        store.setPin("4521");
        store.unlockSession();
        store.clearPin();
        assertFalse(store.hasPinConfigured());
        assertFalse(store.verifyPin("4521"));
        store.unlockSession();
        assertFalse(store.isUnlocked());
    }

    @Test
    public void successfulVerificationResetsFailedAttemptCounter() {
        ParentalControlStore store = new ParentalControlStore(null, "test");
        store.setPin("4521");
        for (int i = 0; i < 4; i++) assertFalse(store.verifyPin("1111"));
        assertTrue(store.verifyPin("4521"));
        assertFalse(store.verifyPin("1111"));
        assertTrue(store.verifyPin("4521"));
    }

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
