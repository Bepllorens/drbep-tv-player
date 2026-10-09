package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;

public class U7dContinuityPolicyTest {
    @Test public void archiveMustBeFinishedAndAdjacent() {
        assertEquals(U7dContinuityPolicy.Next.ARCHIVE, U7dContinuityPolicy.next(100000,100000,200000,300000));
        assertEquals(U7dContinuityPolicy.Next.NONE, U7dContinuityPolicy.next(100000,170000,200000,300000));
        assertEquals(U7dContinuityPolicy.Next.NONE, U7dContinuityPolicy.next(100000,90000,200000,300000));
    }
    @Test public void currentProgrammeOffersLiveButFutureAndInvalidEntriesDoNot() {
        assertEquals(U7dContinuityPolicy.Next.LIVE, U7dContinuityPolicy.next(100000,100000,300000,200000));
        assertEquals(U7dContinuityPolicy.Next.NONE, U7dContinuityPolicy.next(100000,100000,300000,90000));
        assertEquals(U7dContinuityPolicy.Next.NONE, U7dContinuityPolicy.next(100000,100000,100000,300000));
    }
    @Test public void earlyEndIsAnInterruptionNotProgrammeCompletion() {
        assertFalse(U7dContinuityPolicy.reachedEnd(900000,3600000));
        assertTrue(U7dContinuityPolicy.reachedEnd(3596000,3600000));
        assertFalse(U7dContinuityPolicy.reachedEnd(0,0));
    }
    @Test public void retriesAreBoundedAndPreservePreviousSeekOffset() {
        assertTrue(U7dContinuityPolicy.canRetry(0,80000,3600000));
        assertTrue(U7dContinuityPolicy.canRetry(1,80000,3600000));
        assertFalse(U7dContinuityPolicy.canRetry(2,80000,3600000));
        assertFalse(U7dContinuityPolicy.canRetry(0,3599000,3600000));
        assertEquals(780000,U7dContinuityPolicy.retryPosition(600000,180000,3600000));
    }
    @Test public void firstFrameGetsLongerGraceThanAPlaybackFreeze() {
        assertEquals(25000,U7dContinuityPolicy.freezeTimeout(false));
        assertEquals(12000,U7dContinuityPolicy.freezeTimeout(true));
    }
}
