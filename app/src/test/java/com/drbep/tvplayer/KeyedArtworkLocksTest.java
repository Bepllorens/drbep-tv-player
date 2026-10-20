package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class KeyedArtworkLocksTest {
    @Test public void completedKeysDoNotAccumulate() throws Exception {
        KeyedArtworkLocks locks=new KeyedArtworkLocks();
        for(int i=0;i<10000;i++)assertEquals("ok",locks.run("channel-day-"+i,()->"ok"));
        assertEquals(0,locks.size());
    }
    @Test public void exceptionsReleaseEntryAndNextCallWorks() throws Exception {
        KeyedArtworkLocks locks=new KeyedArtworkLocks();
        try { locks.run("a",()->{throw new IOException("synthetic");}); fail(); }
        catch(IOException expected){}
        assertEquals(0,locks.size());
        assertEquals("next",locks.run("a",()->"next"));
    }
    @Test public void differentKeysProceedWhileOneIsBlocked() throws Exception {
        KeyedArtworkLocks locks=new KeyedArtworkLocks();
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        try {
            Future<?> first=pool.submit(()->locks.run("a",()->{
                entered.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));return null;
            }));
            assertTrue(entered.await(5,TimeUnit.SECONDS));
            assertEquals("b",pool.submit(()->locks.run("b",()->"b")).get(2,TimeUnit.SECONDS));
            assertEquals(1,locks.size());
            release.countDown();first.get(5,TimeUnit.SECONDS);
            assertEquals(0,locks.size());
        } finally {release.countDown();pool.shutdownNow();}
    }
    @Test public void sameKeyNeverOverlapsAcrossRepeatedReleaseAndAcquire() throws Exception {
        KeyedArtworkLocks locks=new KeyedArtworkLocks();
        ExecutorService pool=Executors.newFixedThreadPool(8);
        CountDownLatch start=new CountDownLatch(1);
        AtomicInteger active=new AtomicInteger(),peak=new AtomicInteger(),calls=new AtomicInteger();
        java.util.List<Future<?>> futures=new java.util.ArrayList<>();
        try {
            for(int t=0;t<8;t++)futures.add(pool.submit(()->{
                start.await();
                for(int i=0;i<500;i++)locks.run("same",()->{
                    int n=active.incrementAndGet();peak.accumulateAndGet(n,Math::max);
                    try {Thread.yield();calls.incrementAndGet();return null;}
                    finally {active.decrementAndGet();}
                });
                return null;
            }));
            start.countDown();
            for(Future<?> f:futures)f.get(10,TimeUnit.SECONDS);
            assertEquals(4000,calls.get());assertEquals(1,peak.get());assertEquals(0,locks.size());
        } finally {start.countDown();pool.shutdownNow();}
    }
}
