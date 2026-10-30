package com.drbep.tvplayer;
import org.junit.Test;import static org.junit.Assert.*;
import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
import java.time.*;
public class PlaylistScheduledDispatchTest {
 @Test public void concurrentChecksEnqueueOncePerSlot()throws Exception {
  Object lock=new Object();AtomicBoolean handled=new AtomicBoolean();AtomicInteger enqueues=new AtomicInteger();
  PlaylistScheduledDispatch.Source source=new PlaylistScheduledDispatch.Source(){
   public boolean due(){return !handled.get();}
   public void enqueue(){enqueues.incrementAndGet();}
   public void markHandled(){handled.set(true);}
  };
  ExecutorService executor=Executors.newFixedThreadPool(8);
  try {
   List<Future<Boolean>> tasks=new ArrayList<>();
   for(int i=0;i<40;i++)tasks.add(executor.submit(()->PlaylistScheduledDispatch.run(lock,source)));
   int dispatched=0;for(Future<Boolean> result:tasks)if(result.get())dispatched++;
   assertEquals(1,dispatched);assertEquals(1,enqueues.get());
  }finally{executor.shutdownNow();}
 }
 @Test public void failedEnqueueDoesNotConsumeSlot()throws Exception {
  AtomicInteger marks=new AtomicInteger();
  try{PlaylistScheduledDispatch.run(new Object(),new PlaylistScheduledDispatch.Source(){
   public boolean due(){return true;}public void enqueue()throws Exception{throw new Exception("queue failed");}
   public void markHandled(){marks.incrementAndGet();}
  });fail();}catch(Exception expected){assertEquals("queue failed",expected.getMessage());}
  assertEquals(0,marks.get());
 }
 @Test public void disabledOrBusyNeverEnqueuesOrMarks()throws Exception {
  assertFalse(PlaylistScheduledDispatch.run(new Object(),new PlaylistScheduledDispatch.Source(){
   public boolean due(){return false;}public void enqueue(){fail();}public void markHandled(){fail();}
  }));
 }
 @Test public void manualQueueTakingLockFirstIsSeenBeforeScheduledCheck()throws Exception {
  Object lock=new Object();AtomicBoolean busy=new AtomicBoolean();
  synchronized(lock){busy.set(true);}
  assertFalse(PlaylistScheduledDispatch.run(lock,new PlaylistScheduledDispatch.Source(){
   public boolean due(){return !busy.get();}public void enqueue(){fail();}public void markHandled(){fail();}
  }));
 }
 @Test public void decisionHonoursUpdatedSlotBusyAndManualModes() {
  ZoneId zone=ZoneId.of("UTC");long now=Instant.parse("2026-10-30T12:00:00Z").toEpochMilli();
  String schedule="daily:480:240:4";
  assertTrue(PlaylistRefreshDecision.due(schedule,true,true,0,0,now,zone,false));
  assertFalse(PlaylistRefreshDecision.due(schedule,false,true,0,0,now,zone,false));
  assertFalse(PlaylistRefreshDecision.due(schedule,true,false,0,0,now,zone,false));
  assertFalse(PlaylistRefreshDecision.due(schedule,true,true,0,0,now,zone,true));
  assertFalse(PlaylistRefreshDecision.due(schedule,true,true,now,0,now,zone,false));
  assertFalse(PlaylistRefreshDecision.due(schedule,true,true,0,now,now,zone,false));
  assertFalse(PlaylistRefreshDecision.due("",true,true,0,0,now,zone,false));
  assertFalse(PlaylistRefreshDecision.due("bad",true,true,0,0,now,zone,false));
  assertTrue(PlaylistRefreshDecision.due(schedule,true,true,0,now,now+4*3600000L,zone,false));
 }
 @Test public void editingScheduleStartsAfterSaveAndCancellationWaitsNextSlot() {
  ZoneId zone=ZoneId.of("UTC");long saved=Instant.parse("2026-10-30T11:30:00Z").toEpochMilli();
  assertFalse(PlaylistRefreshDecision.due("daily:480:240:4",true,true,0,saved,saved,zone,false));
  long next=Instant.parse("2026-10-30T12:00:00Z").toEpochMilli();
  assertTrue(PlaylistRefreshDecision.due("daily:480:240:4",true,true,0,saved,next,zone,false));
  assertFalse(PlaylistRefreshDecision.due("daily:480:240:4",true,true,0,next,next+900000,zone,false));
 }
}
