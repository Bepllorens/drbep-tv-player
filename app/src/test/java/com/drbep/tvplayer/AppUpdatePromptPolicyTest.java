package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppUpdatePromptPolicyTest {
    private static final long NOW_MS = 2_000_000_000L;

    @Test
    public void promptsForANewerVersion() {
        assertTrue(AppUpdatePromptPolicy.shouldPrompt(391, false, 384, NOW_MS, NOW_MS));
    }

    @Test
    public void promptsWhenServerReplacesAWithdrawnVersion() {
        assertTrue(AppUpdatePromptPolicy.shouldPrompt(391, false, 392, NOW_MS, NOW_MS));
    }

    @Test
    public void legacyPromptWithoutTimestampIsNotSilencedForever() {
        assertTrue(AppUpdatePromptPolicy.shouldPrompt(391, false, 391, 0L, NOW_MS));
    }

    @Test
    public void doesNotRepeatDuringRetryInterval() {
        assertFalse(AppUpdatePromptPolicy.shouldPrompt(
                391,
                false,
                391,
                NOW_MS - AppUpdatePromptPolicy.RETRY_INTERVAL_MS + 1L,
                NOW_MS
        ));
    }

    @Test
    public void repeatsAfterRetryInterval() {
        assertTrue(AppUpdatePromptPolicy.shouldPrompt(
                391,
                false,
                391,
                NOW_MS - AppUpdatePromptPolicy.RETRY_INTERVAL_MS,
                NOW_MS
        ));
    }

    @Test
    public void requiredUpdateAlwaysPrompts() {
        assertTrue(AppUpdatePromptPolicy.shouldPrompt(391, true, 391, NOW_MS, NOW_MS));
    }

    @Test
    public void invalidVersionDoesNotPrompt() {
        assertFalse(AppUpdatePromptPolicy.shouldPrompt(0, false, 0, 0L, NOW_MS));
    }
}
