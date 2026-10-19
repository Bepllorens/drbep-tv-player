package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class ArchivePreloadBudgetTest {
    @Test public void repeatedRefreshDoesNotReloadSameImage() {
        ArchivePreloadBudget b = new ArchivePreloadBudget(3, 1000);
        assertTrue(b.acquire("a", 10));
        for (int i=11;i<1010;i++) assertFalse(b.acquire("a", i));
        assertTrue(b.acquire("a", 1010));
    }
    @Test public void changedImageIsImmediatelyEligible() {
        ArchivePreloadBudget b = new ArchivePreloadBudget(3, 1000);
        assertTrue(b.acquire("old", 10));assertTrue(b.acquire("new", 11));
        assertFalse(b.acquire("new", 12));
    }
    @Test public void oldestAttemptIsEvictedWithoutClearingEverything() {
        ArchivePreloadBudget b = new ArchivePreloadBudget(2, 1000);
        b.acquire("a", 10);b.acquire("b", 11);b.acquire("c", 12);
        assertFalse(b.acquire("b", 13));assertFalse(b.acquire("c", 13));
        assertTrue(b.acquire("a", 13));
    }
    @Test public void emptyUrlsAreIgnoredAndClockResetCanRetry() {
        ArchivePreloadBudget b = new ArchivePreloadBudget(2, 1000);
        assertFalse(b.acquire(null, 1));assertFalse(b.acquire("", 1));
        assertTrue(b.acquire("a", 100));assertTrue(b.acquire("a", 1));
    }
}
