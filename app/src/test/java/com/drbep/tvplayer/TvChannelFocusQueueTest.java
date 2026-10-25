package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class TvChannelFocusQueueTest {
    @Test public void burstRetainsEveryStepUntilTheLazyRowExists() {
        TvChannelFocusQueue q = new TvChannelFocusQueue();
        for (int i=1;i<=12;i++) assertEquals(37+i,q.move(37,1,133));
        q.focused(40); // An old request completing must not discard the latest destination.
        assertEquals(50,q.move(40,1,133));
        q.focused(50);
        assertEquals(51,q.move(50,1,133));
    }
    @Test public void reversalUsesPendingDestination() {
        TvChannelFocusQueue q = new TvChannelFocusQueue();
        assertEquals(8,q.move(7,1,20));
        assertEquals(9,q.move(7,1,20));
        assertEquals(8,q.move(7,-1,20));
        assertEquals(7,q.move(7,-1,20));
    }
    @Test public void leavingTheListDiscardsPendingDestination() {
        TvChannelFocusQueue q = new TvChannelFocusQueue();
        q.move(2,1,10);q.move(2,1,10);q.cancel();
        assertEquals(7,q.move(6,1,10));
    }
    @Test public void boundariesNeverProduceAnInvalidRow() {
        TvChannelFocusQueue q = new TvChannelFocusQueue();
        assertEquals(-1,q.move(0,-1,10));
        assertEquals(1,q.move(0,1,10));
        q.cancel();assertEquals(-1,q.move(9,1,10));
        assertEquals(-1,q.move(0,1,0));
    }
}
