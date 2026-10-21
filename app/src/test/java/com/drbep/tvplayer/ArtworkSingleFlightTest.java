package com.drbep.tvplayer;

import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ArtworkSingleFlightTest {
    static final class Queue implements Executor {
        final List<Runnable> tasks=new ArrayList<>();
        public synchronized void execute(Runnable task) { tasks.add(task); }
        void runNext(){tasks.remove(0).run();}
    }
    @Test public void manyProgrammesUseOneLoadAndKeepIndividualSelection() {
        Queue q=new Queue();
        ArtworkSingleFlight<String,List<String>> f=new ArtworkSingleFlight<>(q,256);
        AtomicInteger loads=new AtomicInteger(); List<String> images=new ArrayList<>();
        for(int i=0;i<60;i++) {
            final int index=i;
            assertTrue(f.request("channel/day",()->{loads.incrementAndGet();return Arrays.asList("first","second");},
                    rows->images.add(rows.get(index%2))));
        }
        assertEquals(1,q.tasks.size());q.runNext();
        assertEquals(1,loads.get());assertEquals(60,images.size());
        for(int i=0;i<60;i++)assertEquals(i%2==0?"first":"second",images.get(i));
    }
    @Test public void differentKeysDoNotShareResults() {
        Queue q=new Queue();ArtworkSingleFlight<String,String> f=new ArtworkSingleFlight<>(q,8);
        List<String> results=new ArrayList<>();
        f.request("a/day1",()->"a",results::add);f.request("a/day2",()->"b",results::add);
        f.request("b/day1",()->"c",results::add);
        assertEquals(3,q.tasks.size());while(!q.tasks.isEmpty())q.runNext();
        assertEquals(Arrays.asList("a","b","c"),results);
    }
    @Test public void waiterLimitIncludesFollowersAndReleasesAfterCompletion() {
        Queue q=new Queue();ArtworkSingleFlight<String,String> f=new ArtworkSingleFlight<>(q,2);
        assertTrue(f.request("a",()->"ok",x->{}));assertTrue(f.request("a",()->"unused",x->{}));
        assertFalse(f.request("b",()->"no",x->fail()));
        q.runNext();assertTrue(f.request("b",()->"ok",x->{}));
    }
    @Test public void failureNotifiesEveryoneAndCanRetry() {
        Queue q=new Queue();ArtworkSingleFlight<String,String> f=new ArtworkSingleFlight<>(q,8);
        List<String> results=new ArrayList<>();
        f.request("a",()->{throw new Exception("test");},results::add);
        f.request("a",()->"unused",results::add);q.runNext();
        assertEquals(Arrays.asList(null,null),results);
        f.request("a",()->"retry",results::add);q.runNext();assertEquals("retry",results.get(2));
    }
    @Test public void executorRejectionReleasesKeyAndDeliversFallbackOnce() {
        AtomicInteger calls=new AtomicInteger();
        ArtworkSingleFlight<String,String> f=new ArtworkSingleFlight<>(r->{throw new RejectedExecutionException();},1);
        for(int i=0;i<2;i++)assertFalse(f.request("a",()->"unused",x->{assertNull(x);calls.incrementAndGet();}));
        assertEquals(2,calls.get());
    }
    @Test public void closedQueuedWorkDoesNotLoadOrNotify() {
        Queue q=new Queue();ArtworkSingleFlight<String,String> f=new ArtworkSingleFlight<>(q,2);
        f.request("a",()->{fail();return "";},x->fail());
        f.close();q.runNext();assertFalse(f.request("b",()->"",x->fail()));
    }
    @Test public void closeDuringLoadDiscardsResult() throws Exception {
        ExecutorService e=Executors.newSingleThreadExecutor();
        CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);
        ArtworkSingleFlight<String,String> f=new ArtworkSingleFlight<>(e,2);
        AtomicInteger callbacks=new AtomicInteger();
        try {
            f.request("a",()->{started.countDown();release.await();return "ok";},x->callbacks.incrementAndGet());
            assertTrue(started.await(5,TimeUnit.SECONDS));f.close();release.countDown();
            e.shutdown();assertTrue(e.awaitTermination(5,TimeUnit.SECONDS));assertEquals(0,callbacks.get());
        } finally {release.countDown();e.shutdownNow();}
    }
    @Test public void abandonedCallbackDoesNotStarveOthersAndCanRequestAgain() {
        Queue q=new Queue();ArtworkSingleFlight<String,String> f=new ArtworkSingleFlight<>(q,8);
        List<String> results=new ArrayList<>();
        f.request("a",()->"first",x->{throw new IllegalStateException();});
        f.request("a",()->"unused",x->{results.add(x);f.request("a",()->"next",results::add);});
        q.runNext();q.runNext();assertEquals(Arrays.asList("first","next"),results);
    }
    @Test public void concurrentFollowersScheduleOneTask() throws Exception {
        Queue q=new Queue();ArtworkSingleFlight<String,String> f=new ArtworkSingleFlight<>(q,100);
        ExecutorService e=Executors.newFixedThreadPool(4);AtomicInteger callbacks=new AtomicInteger();
        try {
            List<Future<Boolean>> results=new ArrayList<>();
            for(int i=0;i<80;i++)results.add(e.submit(()->f.request("a",()->"ok",x->callbacks.incrementAndGet())));
            for(Future<Boolean> result:results)assertTrue(result.get(5,TimeUnit.SECONDS));
            assertEquals(1,q.tasks.size());q.runNext();assertEquals(80,callbacks.get());
        } finally {e.shutdownNow();}
    }
}
