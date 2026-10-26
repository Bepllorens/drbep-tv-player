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
}
