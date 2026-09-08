package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlaybackMemoryBudgetTest {
    private static final long MIB = 1024L * 1024L;

    @Test public void fire192MiBReservesSpaceForTheRestOfTheApp() {
        assertEquals(32 * MIB, PlaybackMemoryBudget.targetBytes(192 * MIB, false));
        assertEquals(8 * MIB, PlaybackMemoryBudget.targetBytes(192 * MIB, true));
    }
    @Test public void largeHeapDoesNotCreateUnlimitedBuffers() {
        assertEquals(48 * MIB, PlaybackMemoryBudget.targetBytes(1024 * MIB, false));
        assertEquals(48 * MIB, PlaybackMemoryBudget.targetBytes(Long.MAX_VALUE, false));
    }
    @Test public void targetsAreAlignedAndFourViewsShareSingleBudget() {
        for (int heap = 64; heap <= 512; heap++) {
            int single = PlaybackMemoryBudget.targetBytes(heap * MIB, false);
            int mosaic = PlaybackMemoryBudget.targetBytes(heap * MIB, true);
            assertEquals(0, single % 65536);
            assertEquals(0, mosaic % 65536);
            assertTrue(single > 0 && single <= heap * MIB / 6);
            assertTrue(4L * mosaic <= heap * MIB / 6);
            assertTrue(4L * mosaic <= single);
        }
    }
    @Test public void mosaicSharesTheCapEvenOnLargeHeaps() {
        assertEquals(12 * MIB, PlaybackMemoryBudget.targetBytes(1024 * MIB, true));
        assertEquals(12 * MIB, PlaybackMemoryBudget.targetBytes(Long.MAX_VALUE, true));
    }
    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidHeap() { PlaybackMemoryBudget.targetBytes(0, false); }
}
