package com.drbep.tvplayer;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
public class StartupTraceSummaryTest {
 private JSONObject e(String kind,String session,long elapsed,long duration)throws Exception {
  return new JSONObject().put("event",kind).put("session",session).put("elapsed",elapsed)
    .put("a",duration).put("at",1000+elapsed).put("version",771);
 }
 @Test public void usesFirstMilestoneAndKeepsMissingStagesMissing()throws Exception {
  StartupTraceSummary s=new StartupTraceSummary();
  s.accept(e("ACTIVITY","a",100,0));s.accept(e("WINDOW","a",120,20));
  s.accept(e("WINDOW","a",125,25));s.accept(e("STARTUP_FRAME","a",300,200));
  JSONObject stages=s.json().getJSONObject(0).getJSONObject("stages");
  assertEquals(20,stages.getLong("WINDOW"));assertEquals(200,stages.getLong("STARTUP_FRAME"));
  assertFalse(stages.has("CATALOG_READY"));
 }
 @Test public void separatesActivityRecreationAndProcesses()throws Exception {
  StartupTraceSummary s=new StartupTraceSummary();
  s.accept(e("ACTIVITY","a",100,0));s.accept(e("WINDOW","a",120,20));
  s.accept(e("ACTIVITY","a",200,0));s.accept(e("STARTUP_FRAME","a",190,90));
  s.accept(e("STARTUP_FRAME","b",300,100));s.accept(e("WINDOW","a",220,20));
  s.accept(e("SESSION","b",1,0));s.accept(e("CATALOG_READY","a",250,150));
  assertEquals(2,s.json().length());
  JSONObject second=s.json().getJSONObject(1).getJSONObject("stages");
  assertEquals(1,second.length());assertEquals(20,second.getLong("WINDOW"));
 }
 @Test public void partialRingDoesNotInventLaunchAndHistoryIsBounded()throws Exception {
  StartupTraceSummary s=new StartupTraceSummary();s.accept(e("STARTUP_FRAME","a",50,40));
  assertEquals(0,s.json().length());
  for(int i=0;i<10;i++){s.accept(e("ACTIVITY","a",i*100,0));s.accept(e("WINDOW","a",i*100+20,-1));}
  assertEquals(6,s.json().length());assertEquals(1400,s.json().getJSONObject(0).getLong("at"));
  assertEquals(0,s.json().getJSONObject(0).getJSONObject("stages").length());
 }
}
