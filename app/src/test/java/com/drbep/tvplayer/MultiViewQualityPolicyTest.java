package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class MultiViewQualityPolicyTest {
    @Test public void swappingPromotesOnlyTheNewPrimary() {
        assertEquals(1, MultiViewQualityPolicy.role(true, 0, 0));
        assertEquals(2, MultiViewQualityPolicy.role(true, 1, 0));
        assertEquals(2, MultiViewQualityPolicy.role(true, 0, 1));
        assertEquals(1, MultiViewQualityPolicy.role(true, 1, 1));
    }
    @Test public void mosaicKeepsItsExistingBudget() {
        assertEquals(0, MultiViewQualityPolicy.role(false, 0, 1));
        assertArrayEquals(new int[]{960,540,1800000}, MultiViewQualityPolicy.limits(0));
    }
    @Test public void primaryAndThumbnailBoundTotalDecodePixels() {
        int[] main = MultiViewQualityPolicy.limits(1), mini = MultiViewQualityPolicy.limits(2);
        assertEquals(720, main[1]);
        assertEquals(360, mini[1]);
        assertTrue(main[0]*main[1] + mini[0]*mini[1] < 1280*720*2);
        assertEquals(3800000, main[2]+mini[2]);
    }
}
