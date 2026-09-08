package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class MultiviewMemoryPolicyTest {
    @Test public void visibilityIsNotPressure() {
        for (int level : new int[]{0, 5, 10, 14, 20, 25, 39}) {
            assertFalse("level " + level, MultiviewMemoryPolicy.shouldClose(level));
        }
    }
    @Test public void genuinePressureStillCloses() {
        for (int level : new int[]{15, 16, 19, 40, 60, 80}) {
            assertTrue("level " + level, MultiviewMemoryPolicy.shouldClose(level));
        }
    }
}
