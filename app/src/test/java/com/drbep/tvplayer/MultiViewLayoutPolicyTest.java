package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;

public class MultiViewLayoutPolicyTest {
    @Test public void primaryFillsScreenAndSwapOnlyChangesGeometry() {
        assertArrayEquals(new int[]{0,0,1920,1080}, MultiViewLayoutPolicy.bounds(1920,1080,2,0,true,0,0,16));
        assertArrayEquals(new int[]{0,0,1920,1080}, MultiViewLayoutPolicy.bounds(1920,1080,2,1,true,1,0,16));
        assertArrayEquals(MultiViewLayoutPolicy.bounds(1920,1080,2,1,true,0,0,16),
                MultiViewLayoutPolicy.bounds(1920,1080,2,0,true,1,0,16));
    }
    @Test public void allCornersRemainInsideScreenWithSixteenNineThumbnail() {
        for (int c=0;c<4;c++) {
            int[] b=MultiViewLayoutPolicy.bounds(1920,1080,2,1,true,0,c,16);
            assertEquals(576,b[2]); assertEquals(324,b[3]);
            assertTrue(b[0]>=0 && b[1]>=0 && b[0]+b[2]<=1920 && b[1]+b[3]<=1080);
        }
    }
    @Test public void mosaicKeepsEverySlotSeparate() {
        assertArrayEquals(new int[]{960,540,960,540}, MultiViewLayoutPolicy.bounds(1920,1080,4,3,false,0,0,16));
        assertArrayEquals(new int[]{960,0,960,1080}, MultiViewLayoutPolicy.bounds(1920,1080,2,1,false,0,0,16));
    }
}
