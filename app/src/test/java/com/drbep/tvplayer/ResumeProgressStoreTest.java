package com.drbep.tvplayer;

import org.junit.Test;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

public class ResumeProgressStoreTest {
    static final class Queue implements Executor {
        final ArrayDeque<Runnable> tasks=new ArrayDeque<>();
        public void execute(Runnable task){tasks.add(task);}
        void run(){while(!tasks.isEmpty())tasks.remove().run();}
    }
    static ResumeProgressStore.Snapshot snapshot(long position) {
        return ResumeProgressStore.Snapshot.capture(false,Collections.singletonMap("movie",position),
                Collections.singletonMap("movie",900000L),Collections.singletonMap("movie",50L),100L);
    }
    @Test public void coalescesBurstAndPreservesDifferentKinds() throws Exception {
        Queue queue=new Queue();Map<String,String> disk=new HashMap<>();AtomicInteger writes=new AtomicInteger();
        ResumeProgressStore store=new ResumeProgressStore((key,s)->{writes.incrementAndGet();disk.put(key,s.encode());return true;},queue);
        ResumeProgressStore.Session owner=store.openSession("profile");
        for(int i=1;i<=60;i++)store.submit(owner,"vod",snapshot(i));
        store.submit(owner,"recording",ResumeProgressStore.Snapshot.capture(true,Collections.singletonMap("rec",5L),
                Collections.emptyMap(),Collections.emptyMap(),10L));
        queue.run();
        assertEquals(2,writes.get());
        assertEquals(60,new JSONObject(disk.get("vod")).getJSONObject("movie").getLong("position"));
        assertEquals(5,new JSONObject(disk.get("recording")).getJSONObject("rec").getLong("position_ms"));
        assertFalse(store.submit(owner,"vod",snapshot(60)));
        assertTrue(queue.tasks.isEmpty());
    }
    @Test public void immutableSnapshotAndRecreationUseLatestPendingProgress() {
        Queue queue=new Queue();
        ResumeProgressStore store=new ResumeProgressStore((key,s)->true,queue);
        ResumeProgressStore.Session old=store.openSession("same");
        Map<String,Long> input=new HashMap<>();input.put("movie",20L);
        ResumeProgressStore.Snapshot value=ResumeProgressStore.Snapshot.capture(false,input,Collections.emptyMap(),Collections.emptyMap(),1L);
        store.submit(old,"vod",value);input.put("movie",900L);
        ResumeProgressStore.Session current=store.openSession("same");
        Map<String,Long> p=new HashMap<>(),d=new HashMap<>(),t=new HashMap<>();
        store.latest(current,"vod").restore(p,d,t);
        assertEquals(Long.valueOf(20),p.get("movie"));
        assertFalse(store.submit(old,"vod",snapshot(10)));
        assertFalse(store.current(old));
    }
    @Test public void failedWriteRemainsRetryableWithoutChangingPosition() {
        Queue queue=new Queue();AtomicInteger calls=new AtomicInteger();
        ResumeProgressStore store=new ResumeProgressStore((key,s)->calls.incrementAndGet()>1,queue);
        ResumeProgressStore.Session owner=store.openSession("profile");
        store.submit(owner,"vod",snapshot(10));queue.run();
        store.retry(owner,"vod");queue.run();
        store.retry(owner,"vod");queue.run();
        assertEquals(2,calls.get());
    }
    @Test public void returningToSavedValueCancelsDifferentPendingValue() throws Exception {
        Queue queue=new Queue();List<Long> saved=new ArrayList<>();
        ResumeProgressStore store=new ResumeProgressStore((key,s)->{saved.add(s.entries.get("movie").position);return true;},queue);
        ResumeProgressStore.Session owner=store.openSession("profile");
        store.submit(owner,"vod",snapshot(10));queue.run();
        store.submit(owner,"vod",snapshot(20));
        store.submit(owner,"vod",snapshot(10));queue.run();
        assertEquals(Arrays.asList(10L,10L),saved);
    }
    @Test public void clearAfterInFlightSaveCannotResurrectHistory() throws Exception {
        ExecutorService executor=Executors.newSingleThreadExecutor();
        try {
            CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1),done=new CountDownLatch(1);
            AtomicReference<String> disk=new AtomicReference<>();
            ResumeProgressStore store=new ResumeProgressStore((key,s)->{
                if(!s.entries.isEmpty()){started.countDown();release.await();}
                disk.set(s.encode());if(s.entries.isEmpty())done.countDown();return true;
            },executor);
            ResumeProgressStore.Session owner=store.openSession("profile");
            store.submit(owner,"vod",snapshot(10));assertTrue(started.await(1,TimeUnit.SECONDS));
            store.submit(owner,"vod",ResumeProgressStore.Snapshot.capture(false,Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),1));
            release.countDown();assertTrue(done.await(1,TimeUnit.SECONDS));
            assertEquals(0,new JSONObject(disk.get()).length());
        } finally {executor.shutdownNow();}
    }
    @Test public void changingProfileDiscardsOldQueuedWorkAndRejectsLateCallbacks() {
        Queue queue=new Queue();List<Long> saved=new ArrayList<>();
        ResumeProgressStore store=new ResumeProgressStore((key,s)->{saved.add(s.entries.get("movie").position);return true;},queue);
        ResumeProgressStore.Session old=store.openSession("one");
        store.submit(old,"vod",snapshot(10));
        ResumeProgressStore.Session current=store.openSession("two");
        assertNull(store.latest(current,"vod"));
        assertFalse(store.submit(old,"vod",snapshot(30)));
        store.submit(current,"vod",snapshot(40));queue.run();
        assertEquals(Arrays.asList(40L),saved);
    }
    @Test public void zeroAndBlankEntriesMatchLegacyNormalization() throws Exception {
        Map<String,Long> positions=new LinkedHashMap<>();
        positions.put(" ",12L);positions.put("zero",0L);positions.put("negative",-2L);positions.put("valid",10L);
        JSONObject json=new JSONObject(ResumeProgressStore.Snapshot.capture(false,positions,
                Collections.singletonMap("valid",-1L),Collections.singletonMap("valid",5L),7L).encode());
        assertEquals(1,json.length());assertEquals(0,json.getJSONObject("valid").getLong("duration"));
        assertEquals(5,json.getJSONObject("valid").getLong("updated_at"));
    }
}
