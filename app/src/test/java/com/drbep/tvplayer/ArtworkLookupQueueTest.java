package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ArtworkLookupQueueTest {
    @Test public void fiftyColdSubscribersShareOneLookup() {
        ArtworkLookupQueue q=new ArtworkLookupQueue();
        List<Runnable> jobs=new ArrayList<>();
        AtomicInteger calls=new AtomicInteger(), delivered=new AtomicInteger();
        for(int i=0;i<50;i++) q.request("same",()->{calls.incrementAndGet();return "image";},
            v->{assertEquals("image",v);delivered.incrementAndGet();},jobs::add,Runnable::run);
        assertEquals(1,jobs.size());jobs.remove(0).run();
        assertEquals(1,calls.get());assertEquals(50,delivered.get());
    }
    @Test public void cancellingOneSubscriberKeepsTheOther() {
        ArtworkLookupQueue q=new ArtworkLookupQueue();
        List<Runnable> jobs=new ArrayList<>();List<String> received=new ArrayList<>();
        Runnable cancel=q.request("a",()->"image",v->fail(),jobs::add,Runnable::run);
        q.request("a",()->{throw new AssertionError();},received::add,jobs::add,Runnable::run);
        cancel.run();jobs.remove(0).run();
        assertEquals(Collections.singletonList("image"),received);
    }
    @Test public void cancelledQueuedWorkDoesNotRunAndCanBeRequestedAgain() {
        ArtworkLookupQueue q=new ArtworkLookupQueue();List<Runnable> jobs=new ArrayList<>();
        Runnable cancel=q.request("a",()->{throw new AssertionError();},v->fail(),jobs::add,Runnable::run);
        cancel.run();jobs.remove(0).run();
        q.request("a",()->"retry",v->assertEquals("retry",v),Runnable::run,Runnable::run);
    }
    @Test public void cancelAfterLookupSuppressesPostedDelivery() {
        ArtworkLookupQueue q=new ArtworkLookupQueue();List<Runnable> deliveries=new ArrayList<>();
        Runnable cancel=q.request("a",()->"image",v->fail(),Runnable::run,deliveries::add);
        cancel.run();deliveries.remove(0).run();
    }
    @Test public void failureNotifiesAllAndDoesNotBlockRetry() {
        ArtworkLookupQueue q=new ArtworkLookupQueue();List<Runnable> jobs=new ArrayList<>();
        AtomicInteger got=new AtomicInteger();
        q.request("a",()->{throw new IllegalStateException();},v->{assertEquals("",v);got.incrementAndGet();},jobs::add,Runnable::run);
        q.request("a",()->"unused",v->{assertEquals("",v);got.incrementAndGet();},jobs::add,Runnable::run);
        jobs.remove(0).run();assertEquals(2,got.get());
        q.request("a",()->"retry",v->assertEquals("retry",v),Runnable::run,Runnable::run);
    }
    @Test public void differentScopeOrShapeIsNotShared() {
        ArtworkLookupQueue q=new ArtworkLookupQueue();List<Runnable> jobs=new ArrayList<>();
        for(String key:Arrays.asList("user-a-poster","user-b-poster","user-a-backdrop"))
            q.request(key,()->key,v->assertEquals(key,v),jobs::add,Runnable::run);
        assertEquals(3,jobs.size());jobs.forEach(Runnable::run);
    }
    @Test public void rejectedExecutorDoesNotLeavePendingEntry() {
        ArtworkLookupQueue q=new ArtworkLookupQueue();
        q.request("a",()->"unused",v->assertEquals("",v),r->{throw new RejectedExecutionException();},Runnable::run);
        q.request("a",()->"retry",v->assertEquals("retry",v),Runnable::run,Runnable::run);
    }
    @Test public void oneBadConsumerCannotStarveOthers() {
        ArtworkLookupQueue q=new ArtworkLookupQueue();List<Runnable> jobs=new ArrayList<>();AtomicInteger got=new AtomicInteger();
        q.request("a",()->"image",v->{throw new IllegalStateException();},jobs::add,Runnable::run);
        q.request("a",()->"unused",v->got.incrementAndGet(),jobs::add,Runnable::run);
        jobs.remove(0).run();assertEquals(1,got.get());
    }
    @Test public void cancelledRunningRequestCanBeJoined() throws Exception {
        ArtworkLookupQueue q=new ArtworkLookupQueue();
        ExecutorService pool=Executors.newSingleThreadExecutor();
        CountDownLatch started=new CountDownLatch(1), release=new CountDownLatch(1), done=new CountDownLatch(1);
        AtomicInteger received=new AtomicInteger();
        try {
            Runnable cancel=q.request("a",()->{
                started.countDown();
                try {if(!release.await(5,TimeUnit.SECONDS)) throw new IllegalStateException();}
                catch(InterruptedException e){throw new IllegalStateException(e);}
                return "image";
            },v->received.addAndGet(100),pool,Runnable::run);
            assertTrue(started.await(5,TimeUnit.SECONDS));cancel.run();
            q.request("a",()->"wrong",v->{if("image".equals(v))received.incrementAndGet();done.countDown();},pool,Runnable::run);
            release.countDown();assertTrue(done.await(5,TimeUnit.SECONDS));
            assertEquals(1,received.get());
        } finally {release.countDown();pool.shutdownNow();}
    }
}
