package com.drbep.tvplayer;
import org.junit.Test;
import java.time.*;
import static org.junit.Assert.*;

public class PlaylistRefreshScheduleTest {
 private static final ZoneId MADRID=ZoneId.of("Europe/Madrid");
 private static long at(String date){return LocalDateTime.parse(date).atZone(MADRID).toInstant().toEpochMilli();}
 @Test public void nextSlotIsStrictlyAfterAndDueIsInclusive() {
  PlaylistRefreshSchedule s=new PlaylistRefreshSchedule(8*60,240,4);
  assertEquals(at("2026-10-30T08:00"),s.nextAfter(at("2026-10-30T07:59"),MADRID));
  assertEquals(at("2026-10-30T12:00"),s.nextAfter(at("2026-10-30T08:00"),MADRID));
  assertFalse(s.due(at("2026-10-30T08:00"),at("2026-10-30T11:59"),MADRID));
  assertTrue(s.due(at("2026-10-30T08:00"),at("2026-10-30T12:00"),MADRID));
  assertFalse(s.due(at("2026-10-30T12:00"),at("2026-10-30T12:00"),MADRID));
 }
 @Test public void crossingMidnightKeepsDailySlots() {
  PlaylistRefreshSchedule s=new PlaylistRefreshSchedule(22*60,180,3);
  assertEquals(at("2026-10-31T01:00"),s.nextAfter(at("2026-10-30T22:00"),MADRID));
  assertEquals(at("2026-10-31T04:00"),s.nextAfter(at("2026-10-31T01:00"),MADRID));
  assertEquals(at("2026-10-31T22:00"),s.nextAfter(at("2026-10-31T04:00"),MADRID));
 }
 @Test public void springGapCollapsesCoincidentSlots() {
  PlaylistRefreshSchedule s=new PlaylistRefreshSchedule(150,60,2);
  long first=Instant.parse("2026-03-29T01:30:00Z").toEpochMilli();
  assertEquals(first,s.nextAfter(Instant.parse("2026-03-29T00:00:00Z").toEpochMilli(),MADRID));
  assertEquals(at("2026-03-30T02:30"),s.nextAfter(first,MADRID));
 }
 @Test public void autumnDoesNotRunTwiceForRepeatedWallTime() {
  PlaylistRefreshSchedule s=new PlaylistRefreshSchedule(150,60,1);
  long first=Instant.parse("2026-10-25T00:30:00Z").toEpochMilli();
  long second=Instant.parse("2026-10-25T01:30:00Z").toEpochMilli();
  assertEquals(first,s.latestAtOrBefore(second,MADRID));
  assertFalse(s.due(first,second,MADRID));
  assertEquals(at("2026-10-26T02:30"),s.nextAfter(first,MADRID));
 }
 @Test public void absenceDoesNotReplayOldSlotsAndFutureSuccessIsNotDue() {
  PlaylistRefreshSchedule s=new PlaylistRefreshSchedule(480,240,4);
  long now=at("2026-10-30T19:00");
  assertEquals(at("2026-10-30T16:00"),s.latestAtOrBefore(now,MADRID));
  assertTrue(s.due(at("2026-10-01T08:00"),now,MADRID));
  assertFalse(s.due(now,now,MADRID));
  assertFalse(s.due(at("2026-11-01T08:00"),now,MADRID));
 }
 @Test public void encodingRoundTripsAndRejectsInvalidConfiguration() {
  PlaylistRefreshSchedule s=new PlaylistRefreshSchedule(480,240,4);
  assertEquals(s.encode(),PlaylistRefreshSchedule.decode(s.encode()).encode());
  for(String raw:new String[]{"","daily:0:0:4","daily:1440:30:1","daily:0:30:25",
      "daily:0:720:4","daily:x:30:1","daily:0:30:1:extra","weekly:0:30:1"})
   try{PlaylistRefreshSchedule.decode(raw);fail(raw);}catch(IllegalArgumentException expected){}
 }
 @Test public void slotsStayOrderedAcrossZonesAndYearBoundaries() {
  PlaylistRefreshSchedule s=new PlaylistRefreshSchedule(1380,30,24);
  for(String name:new String[]{"UTC","Europe/Madrid","America/New_York","Asia/Kathmandu","Australia/Lord_Howe"}) {
   ZoneId zone=ZoneId.of(name);long now=Instant.parse("2026-12-31T12:00:00Z").toEpochMilli();
   for(int i=0;i<100;i++){long next=s.nextAfter(now,zone);assertTrue(next>now);
    assertEquals(next,s.latestAtOrBefore(next,zone));assertFalse(s.due(next,next,zone));now=next;}
  }
 }
}
