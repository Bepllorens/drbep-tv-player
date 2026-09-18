package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class VodProgressSyncTest {
    @Test public void missingDurationDoesNotEraseLocalBookmark() {
        assertTrue(MainActivity.shouldPreserveLocalVodProgress(100, 200, false, 0));
    }
    @Test public void newerLocalProgressWins() {
        assertTrue(MainActivity.shouldPreserveLocalVodProgress(200, 100, true, 7200000));
    }
    @Test public void explicitRemoteCompletionIsRespected() {
        assertFalse(MainActivity.shouldPreserveLocalVodProgress(100, 200, true, 0));
        assertFalse(MainActivity.shouldPreserveLocalVodProgress(100, 200, false, 7200000));
    }
}
