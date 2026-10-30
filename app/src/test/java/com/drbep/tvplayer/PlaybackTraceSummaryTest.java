package com.drbep.tvplayer;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
public class PlaybackTraceSummaryTest {
 static JSONObject event(String session,long op,String event,long elapsed,long a,long b,long c) throws JSONException {
  return new JSONObject().put("session",session).put("op",op).put("event",event).put("elapsed",elapsed)
   .put("at",1000+elapsed).put("a",a).put("b",b).put("c",c);
 }
 @Test public void keepsDifferentPlayersAndProcessesSeparate() throws Exception {
  PlaybackTraceSummary traces=new PlaybackTraceSummary();
  traces.accept(event("one",10,"PLAY_PREPARE",100,0,0,1));traces.accept(event("one",11,"PLAY_PREPARE",110,0,0,1));
  traces.accept(event("one",10,"PLAY_STATE",150,3,0,0));traces.accept(event("one",11,"PLAY_ERROR",180,4001,0,0));
  traces.accept(event("two",10,"PLAY_PREPARE",1000,0,0,1));traces.accept(event("two",10,"PLAY_FRAME",1200,200,1,0));
  JSONArray rows=traces.json();assertEquals(3,rows.length());
  assertEquals(50,rows.getJSONObject(0).getJSONObject("stages").getLong("READY"));
  assertFalse(rows.getJSONObject(0).has("error"));assertEquals(4001,rows.getJSONObject(1).getLong("error"));
  assertEquals(200,rows.getJSONObject(2).getJSONObject("stages").getLong("FRAME"));assertTrue(rows.getJSONObject(2).getBoolean("preview"));
 }
 @Test public void ignoresAmbiguousLegacyRecordsAndEventsBeforePrepare() throws Exception {
  PlaybackTraceSummary traces=new PlaybackTraceSummary();
  traces.accept(event("one",1,"PLAY_PREPARE",100,0,0,0));traces.accept(event("one",1,"PLAY_FRAME",150,50,0,0));
  assertEquals(0,traces.json().length());
  traces.accept(event("one",2,"PLAY_PREPARE",200,0,0,1));traces.accept(event("one",2,"PLAY_STATE",190,3,0,0));
  assertFalse(traces.json().getJSONObject(0).getJSONObject("stages").has("READY"));
 }
 @Test public void firstFrameIsNotReplacedByRepeatedSurfaceFramesAndOutputIsBounded() throws Exception {
  PlaybackTraceSummary traces=new PlaybackTraceSummary();
  for(int i=1;i<=10;i++)traces.accept(event("one",i,"PLAY_PREPARE",100*i,0,0,1));
  traces.accept(event("one",10,"PLAY_FRAME",1050,50,0,0));traces.accept(event("one",10,"PLAY_FRAME",1060,60,0,1));
  JSONArray rows=traces.json();assertEquals(8,rows.length());assertEquals(3,rows.getJSONObject(0).getLong("operation"));
  assertEquals(50,rows.getJSONObject(7).getJSONObject("stages").getLong("FRAME"));
  assertFalse(rows.getJSONObject(7).has("started"));
 }
 @Test public void loadDurationsRemainSeparateFromCumulativeMilestones() throws Exception {
  PlaybackTraceSummary traces=new PlaybackTraceSummary();
  traces.accept(event("one",5,"PLAY_PREPARE",100,0,0,1));
  traces.accept(event("one",5,"PLAY_MANIFEST_LOADED",140,20,1200,-1));
  traces.accept(event("one",5,"PLAY_MEDIA_LOADED",190,35,4096,2));
  traces.accept(event("one",5,"PLAY_VIDEO_DECODER",210,15,0,0));
  traces.accept(event("one",5,"PLAY_FRAME",250,150,0,0));
  JSONObject row=traces.json().getJSONObject(0);
  assertEquals(40,row.getJSONObject("stages").getLong("MANIFEST"));
  assertEquals(90,row.getJSONObject("stages").getLong("MEDIA"));
  assertEquals(110,row.getJSONObject("stages").getLong("VIDEO_DECODER"));
  assertEquals(20,row.getJSONObject("details").getJSONObject("MANIFEST").getLong("durationMs"));
  assertEquals(4096,row.getJSONObject("details").getJSONObject("MEDIA").getLong("bytes"));
  assertEquals(2,row.getJSONObject("details").getJSONObject("MEDIA").getLong("trackType"));
 }
 @Test public void firstLoadCannotBeReplacedAndUnobservedPhasesStayAbsent() throws Exception {
  PlaybackTraceSummary traces=new PlaybackTraceSummary();traces.accept(event("one",1,"PLAY_PREPARE",100,0,0,1));
  traces.accept(event("one",1,"PLAY_MANIFEST_LOADED",120,10,40,-1));
  traces.accept(event("one",1,"PLAY_MANIFEST_LOADED",130,20,80,-1));
  traces.accept(event("one",1,"PLAY_FRAME",150,50,0,0));
  traces.accept(event("one",1,"PLAY_MEDIA_LOADED",160,10,30,2));
  JSONObject row=traces.json().getJSONObject(0);
  assertEquals(10,row.getJSONObject("details").getJSONObject("MANIFEST").getLong("durationMs"));
  assertFalse(row.getJSONObject("stages").has("MEDIA"));assertFalse(row.getJSONObject("stages").has("VIDEO_DECODER"));
 }
 @Test public void invalidLoadsAndUnrelatedOperationsAreIgnored() throws Exception {
  PlaybackTraceSummary traces=new PlaybackTraceSummary();traces.accept(event("one",1,"PLAY_PREPARE",100,0,0,1));
  traces.accept(event("two",1,"PLAY_MANIFEST_LOADED",120,5,20,0));
  traces.accept(event("one",2,"PLAY_MEDIA_LOADED",120,5,20,2));
  traces.accept(event("one",1,"PLAY_MANIFEST_LOADED",120,-1,20,0));
  JSONObject row=traces.json().getJSONObject(0);assertFalse(row.has("details"));assertFalse(row.getJSONObject("stages").has("MANIFEST"));
 }

 @Test public void resolutionIsExplicitAndDoesNotChangeExistingStageTimes() throws Exception {
  PlaybackTraceSummary t=new PlaybackTraceSummary();
  t.accept(event("one",10,"PLAY_RESOLVE_BEGIN",100,3,0,0));
  t.accept(event("one",11,"PLAY_PREPARE",150,0,0,1));
  t.accept(event("one",11,"PLAY_RESOLVE_LINK",151,10,50,3));
  t.accept(event("one",11,"PLAY_FRAME",200,50,0,0));
  JSONObject row=t.json().getJSONObject(0);
  assertEquals(50,row.getJSONObject("resolution").getLong("durationMs"));
  assertEquals(10,row.getJSONObject("resolution").getLong("operation"));
  assertEquals(50,row.getJSONObject("stages").getLong("FRAME"));
 }
 @Test public void wrongProcessGenerationAndMissingStartCannotBeInferred() throws Exception {
  PlaybackTraceSummary t=new PlaybackTraceSummary();
  t.accept(event("one",10,"PLAY_RESOLVE_BEGIN",100,3,0,0));
  t.accept(event("two",11,"PLAY_PREPARE",150,0,0,1));
  t.accept(event("two",11,"PLAY_RESOLVE_LINK",151,10,50,3));
  assertFalse(t.json().getJSONObject(0).has("resolution"));
  t.accept(event("one",12,"PLAY_PREPARE",160,0,0,1));
  t.accept(event("one",12,"PLAY_RESOLVE_LINK",161,10,60,4));
  assertFalse(t.json().getJSONObject(1).has("resolution"));
  t.accept(event("one",12,"PLAY_RESOLVE_LINK",162,99,60,3));
  assertFalse(t.json().getJSONObject(1).has("resolution"));
 }
}
