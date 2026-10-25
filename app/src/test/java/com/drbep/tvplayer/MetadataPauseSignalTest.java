package com.drbep.tvplayer;

import org.junit.Test;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

public class MetadataPauseSignalTest {
    @Test public void signalDoesNotWaitForQueueLockAndRollsBackRemainingStages() throws Exception {
        Object queueLock=new Object();
        MetadataPauseSignal signal=new MetadataPauseSignal();
        CountDownLatch inSql=new CountDownLatch(1), finishSql=new CountDownLatch(1);
        AtomicBoolean committed=new AtomicBoolean(), ended=new AtomicBoolean();
        AtomicReference<Throwable> failure=new AtomicReference<>();
        Thread worker=new Thread(()->{try{synchronized(queueLock){
            boolean prepared=MetadataIndexQueue.prepareStages(new MetadataIndexQueue.Transaction(){
                public void begin(){} public void successful(){committed.set(true);}
                public void end(){ended.set(true);}
            },()->!signal.isPaused("A"),Arrays.asList(
                ()->{inSql.countDown();try{assertTrue(finishSql.await(5,TimeUnit.SECONDS));}
                    catch(InterruptedException e){throw new AssertionError(e);}},
                ()->{throw new AssertionError("Later SQL must not run");}),
                ()->{throw new AssertionError("Marker must not run");});
            assertFalse(prepared);
        }}catch(Throwable e){failure.set(e);}});
        worker.start();
        try {
            assertTrue(inSql.await(5,TimeUnit.SECONDS));
            assertEquals("A",signal.requestPause(()->"A"));
            assertTrue(worker.isAlive()); assertTrue(signal.isPaused("A"));
        } finally {finishSql.countDown();worker.join(5000);}
        assertFalse(worker.isAlive());assertNull(failure.get());
        assertTrue(ended.get());assertFalse(committed.get());
    }
    @Test public void pauseIsScopedToGeneration() {
        MetadataPauseSignal s=new MetadataPauseSignal();
        assertEquals("A",s.requestPause(()->"A"));
        assertTrue(s.isPaused("A"));assertFalse(s.isPaused("B"));
    }
    @Test public void newGenerationClearsOldSignalAndStaleCommandCanBeRejected() {
        MetadataPauseSignal s=new MetadataPauseSignal();AtomicReference<String> current=new AtomicReference<>("A");
        String paused=s.requestPause(current::get);
        s.publishGeneration(()->current.set("B"));
        assertNotEquals(paused,current.get());assertFalse(s.isPaused("B"));
        s.requestPause(current::get);assertTrue(s.isPaused("B"));
    }
    @Test public void failedPublicationDoesNotClearPause() {
        MetadataPauseSignal s=new MetadataPauseSignal();s.requestPause(()->"A");
        try{s.publishGeneration(()->{throw new IllegalStateException();});fail();}
        catch(IllegalStateException expected){}
        assertTrue(s.isPaused("A"));
    }
    @Test public void signalIsVisibleToOtherThreads() throws Exception {
        MetadataPauseSignal s=new MetadataPauseSignal();
        ExecutorService e=Executors.newSingleThreadExecutor();
        try {for(int i=0;i<100;i++){
            final String id="generation-"+i;
            s.publishGeneration(()->{});
            e.submit(()->s.requestPause(()->id)).get(5,TimeUnit.SECONDS);
            assertTrue(s.isPaused(id));
        }}finally{e.shutdownNow();}
    }
    @Test public void pauseReadsNewGenerationAfterPublicationGate() throws Exception {
        MetadataPauseSignal s=new MetadataPauseSignal();AtomicReference<String> current=new AtomicReference<>("A");
        CountDownLatch publishing=new CountDownLatch(1), finish=new CountDownLatch(1);
        ExecutorService e=Executors.newFixedThreadPool(2);
        try {
            Future<?> published=e.submit(()->s.publishGeneration(()->{
                publishing.countDown();
                try{if(!finish.await(5,TimeUnit.SECONDS))throw new AssertionError("timeout");}
                catch(InterruptedException ex){throw new AssertionError(ex);}
                current.set("B");
            }));
            assertTrue(publishing.await(5,TimeUnit.SECONDS));
            Future<String> pause=e.submit(()->s.requestPause(current::get));
            finish.countDown();published.get(5,TimeUnit.SECONDS);
            assertEquals("B",pause.get(5,TimeUnit.SECONDS));
            assertTrue(s.isPaused("B"));assertFalse(s.isPaused("A"));
        } finally {finish.countDown();e.shutdownNow();}
    }

}
