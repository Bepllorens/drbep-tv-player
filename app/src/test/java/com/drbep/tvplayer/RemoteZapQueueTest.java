package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class RemoteZapQueueTest {
    static final class Fixture implements RemoteZapQueue.Scheduler, RemoteZapQueue.Host<Object> {
        List<Object> items=new ArrayList<>(); List<Integer> tunes=new ArrayList<>();
        int index=0; Runnable queued; long delay, clock, due;
        final RemoteZapQueue<Object> queue=new RemoteZapQueue<>(this,this);
        Fixture() { for(int i=0;i<8;i++)items.add(new Object()); }
        public void post(Runnable r,long ms){queued=r;delay=ms;due=clock+ms;}
        public void remove(Runnable r){if(queued==r)queued=null;}
        public int size(){return items.size();}
        public int currentIndex(){return index;}
        public Object item(int i){return items.get(i);}
        public void tune(int i){index=i;tunes.add(i);}
        void pressAt(int delta,long at) {
            clock=at;if(queued!=null && due<=clock)flush();
            queue.press(delta,at);
        }
        void advanceTo(long at){clock=at;if(queued!=null && due<=clock)flush();}
        void flush(){Runnable r=queued;queued=null;if(r!=null)r.run();}
    }
    @Test public void firstAndIsolatedPressesAreImmediate() {
        Fixture f=new Fixture();f.queue.press(1,0);f.queue.press(1,1500);
        assertEquals(Arrays.asList(1,2),f.tunes);assertNull(f.queued);
    }
    @Test public void heldKeyTunesFirstAndFinalOnly() {
        Fixture f=new Fixture();
        for(int i=0;i<5;i++)f.queue.press(1,i*70);
        assertEquals(Arrays.asList(1),f.tunes);assertTrue(f.delay>=70 && f.delay<=100);
        f.flush();assertEquals(Arrays.asList(1,5),f.tunes);
    }
    @Test public void wrapAndDirectionReversalUsePendingTarget() {
        Fixture f=new Fixture();f.queue.press(-1,0);f.queue.press(-1,50);f.queue.press(1,100);f.queue.press(1,150);
        f.flush();assertEquals(Arrays.asList(7,0),f.tunes);
    }
    @Test public void staleTaskCannotTuneAfterCancelOrNewPress() {
        Fixture f=new Fixture();f.queue.press(1,0);f.queue.press(1,50);
        Runnable old=f.queued;f.queue.press(1,100);old.run();
        assertEquals(Arrays.asList(1),f.tunes);
        f.queue.cancel();f.flush();assertEquals(Arrays.asList(1),f.tunes);
        f.queue.press(1,150);assertEquals(Arrays.asList(1,2),f.tunes);
    }
    @Test public void changedCatalogueOrSelectionDiscardsPending() {
        Fixture f=new Fixture();f.queue.press(1,0);f.queue.press(1,50);
        f.items.set(2,new Object());f.flush();assertEquals(Arrays.asList(1),f.tunes);
        f.queue.press(1,2000);f.queue.press(1,2050);f.index=6;f.flush();
        assertEquals(Arrays.asList(1,2),f.tunes);
    }
    @Test public void denseBurstSettlesQuicklyWithoutIntermediateTunes() {
        Fixture f=new Fixture();
        for(int i=0;i<6;i++)f.pressAt(1,i*15);
        assertEquals(Arrays.asList(1),f.tunes);
        f.advanceTo(104);assertEquals(Arrays.asList(1),f.tunes);
        f.advanceTo(105);assertEquals(Arrays.asList(1,6),f.tunes);
    }
    @Test public void heldKeyRepeatsKeepEnoughTimeToCoalesce() {
        Fixture f=new Fixture();
        for(int i=0;i<6;i++)f.pressAt(1,i*70);
        assertEquals(Arrays.asList(1),f.tunes);
        f.advanceTo(434);assertEquals(Arrays.asList(1),f.tunes);
        f.advanceTo(435);assertEquals(Arrays.asList(1,6),f.tunes);
    }
    @Test public void settlingNeverExceedsCeilingNearBoundary() {
        Fixture f=new Fixture();f.pressAt(1,0);f.pressAt(1,49);
        assertTrue(f.delay>=49 && f.delay<=100);
    }
    @Test public void emptyCatalogueDiscardsPending() {
        Fixture f=new Fixture();f.queue.press(1,0);f.queue.press(1,50);
        f.items.clear();f.flush();f.queue.press(1,70);
        assertEquals(Arrays.asList(1),f.tunes);
    }
    @Test public void ordinaryHeldKeyJitterStillProducesOnlyFirstAndFinalTune() {
        Fixture f=new Fixture();
        for(long at:new long[]{0,65,135,210,280,350})f.pressAt(1,at);
        assertEquals(Arrays.asList(1),f.tunes);
        f.advanceTo(434);assertEquals(Arrays.asList(1),f.tunes);
        f.advanceTo(435);assertEquals(Arrays.asList(1,6),f.tunes);
    }
    @Test public void heldKeyReleaseCommitsWithoutTrailingDelayAndStartsANewBurst() {
        Fixture f=new Fixture();
        for(long at:new long[]{0,70,140,210,280,350})f.pressAt(1,at);
        assertEquals(Arrays.asList(1),f.tunes);
        Runnable old=f.queued;f.clock=355;f.queue.finishBurst();
        assertEquals(Arrays.asList(1,6),f.tunes);assertNull(f.queued);
        old.run();assertEquals(Arrays.asList(1,6),f.tunes);
        f.pressAt(1,356);assertEquals(Arrays.asList(1,6,7),f.tunes);
    }
    @Test public void releaseCannotDeliverACancelledOrReplacedTarget() {
        Fixture f=new Fixture();f.pressAt(1,0);f.pressAt(1,70);
        f.queue.cancel();f.queue.finishBurst();assertEquals(Arrays.asList(1),f.tunes);
        f.pressAt(1,140);f.pressAt(1,210);f.items.set(3,new Object());
        f.queue.finishBurst();assertEquals(Arrays.asList(1,2),f.tunes);
    }
    @Test public void releaseAfterTimerAlreadyCommittedDoesNotRetune() {
        Fixture f=new Fixture();f.pressAt(1,0);f.pressAt(1,70);f.advanceTo(160);
        assertEquals(Arrays.asList(1,2),f.tunes);f.queue.finishBurst();
        assertEquals(Arrays.asList(1,2),f.tunes);f.pressAt(1,170);
        assertEquals(Arrays.asList(1,2,3),f.tunes);
    }
}
