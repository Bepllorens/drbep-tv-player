package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class RemoteZapQueueTest {
    static final class Fixture implements RemoteZapQueue.Scheduler, RemoteZapQueue.Host<Object> {
        List<Object> items=new ArrayList<>(); List<Integer> tunes=new ArrayList<>();
        int index=0; Runnable queued; long delay;
        final RemoteZapQueue<Object> queue=new RemoteZapQueue<>(this,this);
        Fixture() { for(int i=0;i<8;i++)items.add(new Object()); }
        public void post(Runnable r,long ms){queued=r;delay=ms;}
        public void remove(Runnable r){if(queued==r)queued=null;}
        public int size(){return items.size();}
        public int currentIndex(){return index;}
        public Object item(int i){return items.get(i);}
        public void tune(int i){index=i;tunes.add(i);}
        void flush(){Runnable r=queued;queued=null;if(r!=null)r.run();}
    }
    @Test public void firstAndIsolatedPressesAreImmediate() {
        Fixture f=new Fixture();f.queue.press(1,0);f.queue.press(1,1500);
        assertEquals(Arrays.asList(1,2),f.tunes);assertNull(f.queued);
    }
    @Test public void heldKeyTunesFirstAndFinalOnly() {
        Fixture f=new Fixture();
        for(int i=0;i<5;i++)f.queue.press(1,i*70);
        assertEquals(Arrays.asList(1),f.tunes);assertEquals(250,f.delay);
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
    @Test public void emptyCatalogueDiscardsPending() {
        Fixture f=new Fixture();f.queue.press(1,0);f.queue.press(1,50);
        f.items.clear();f.flush();f.queue.press(1,70);
        assertEquals(Arrays.asList(1),f.tunes);
    }
}
