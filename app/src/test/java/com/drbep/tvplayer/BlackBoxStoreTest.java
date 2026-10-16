package com.drbep.tvplayer;
import org.junit.Test;
import org.json.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;
public class BlackBoxStoreTest {
 @Test public void retainsHistoryAcrossProcessesAndNeverAcceptsText()throws Exception{
  File dir=Files.createTempDirectory("blackbox").toFile();
  try(BlackBoxStore s=new BlackBoxStore(dir,745)){
   s.event(BlackBoxStore.Event.HOME_MODEL,7,91,0,0);
   JSONObject out=new JSONObject(s.summary());
   assertTrue(out.getJSONArray("events").toString().contains("HOME_MODEL"));
   assertEquals(0,out.getLong("writeErrors"));
  }
  try(BlackBoxStore s=new BlackBoxStore(dir,745)){
   JSONArray events=new JSONObject(s.summary()).getJSONArray("events");
   Set<String> sessions=new HashSet<>();for(int i=0;i<events.length();i++)sessions.add(events.getJSONObject(i).getString("session"));
   assertEquals(2,sessions.size());
  }
 }
 @Test public void rotatesAndExportsOnlyOwnedFilesWithBoundedSnapshots()throws Exception{
  File dir=Files.createTempDirectory("blackbox").toFile();
  Files.write(new File(dir,"secret.txt").toPath(),"DO-NOT-EXPORT".getBytes("UTF-8"));
  try(BlackBoxStore s=new BlackBoxStore(dir,745,1024)){
   for(int batch=0;batch<20;batch++){
    for(int i=0;i<20;i++)s.event(BlackBoxStore.Event.PLAY_FRAME,batch,i,1,0);
    s.summary();
   }
   s.mark();s.mark();
   long size=0;for(File f:dir.listFiles())if(f.getName().endsWith(".jsonl"))size+=f.length();
   assertTrue(size<=12*1024);
   try(ZipInputStream in=new ZipInputStream(new ByteArrayInputStream(s.export()))){
    ZipEntry e;int files=0;while((e=in.getNextEntry())!=null){assertNotEquals("secret.txt",e.getName());files++;}
    assertTrue(files>=6);
   }
  }
 }
 @Test public void pauseAndDetailAreExplicitAndQueueIsBounded()throws Exception{
  File dir=Files.createTempDirectory("blackbox").toFile();
  try(BlackBoxStore s=new BlackBoxStore(dir,745)){
   s.setEnabled(false);s.event(BlackBoxStore.Event.PLAY_ERROR,0,123,0,0);
   assertEquals(0,new JSONObject(s.summary()).getJSONArray("events").length());
   s.setEnabled(true);assertFalse(s.detailed());s.detail();assertTrue(s.detailed());
   for(int i=0;i<100000;i++)s.event(BlackBoxStore.Event.PLAY_STATE,1,i,0,0);
   JSONObject out=new JSONObject(s.summary());assertTrue(out.getLong("lost")>0);
   assertTrue(out.getJSONArray("events").length()<=200);
  }
 }
 @Test public void malformedTailDoesNotLoseEarlierEvents()throws Exception{
  File dir=Files.createTempDirectory("blackbox").toFile();
  try(BlackBoxStore s=new BlackBoxStore(dir,745)){s.event(BlackBoxStore.Event.MARK,0,0,0,0);s.summary();}
  Files.write(new File(dir,"events-0.jsonl").toPath(),"{bad\n".getBytes("UTF-8"),StandardOpenOption.APPEND);
  try(BlackBoxStore s=new BlackBoxStore(dir,745)){assertTrue(s.summary().contains("MARK"));}
 }

 @Test public void extendedDiagnosticsKeepNumericSchemaAndIndependentTimings()throws Exception{
  File dir=Files.createTempDirectory("blackbox-extended").toFile();
  try(BlackBoxStore s=new BlackBoxStore(dir,747)){
   s.event(BlackBoxStore.Event.GUIDE_HTTP,101,80,503,2);
   s.event(BlackBoxStore.Event.GUIDE_FALLBACK,101,90,1,1);
   s.event(BlackBoxStore.Event.GUIDE_LOAD,101,95,40,2);
   s.event(BlackBoxStore.Event.CATCHUP_PAGE,102,3,60,0);
   s.event(BlackBoxStore.Event.MULTI_FRAME,103,1200,1,0);
   JSONObject summary=new JSONObject(s.summary());
   JSONArray rows=summary.getJSONArray("events");
   for(int i=0;i<rows.length();i++){
    JSONObject row=rows.getJSONObject(i);assertEquals(9,row.length());
    for(String field:Arrays.asList("version","at","elapsed","op","a","b","c"))
     assertTrue(row.get(field) instanceof Number);
   }
   JSONArray stats=summary.getJSONArray("stats");
   Map<String,Long> medians=new HashMap<>();
   for(int i=0;i<stats.length();i++)medians.put(stats.getJSONObject(i).getString("event"),stats.getJSONObject(i).getLong("p50"));
   assertEquals(Long.valueOf(3),medians.get("CATCHUP_PAGE"));
   assertEquals(Long.valueOf(1200),medians.get("MULTI_FRAME"));
   assertEquals(Long.valueOf(95),medians.get("GUIDE_LOAD"));
   assertEquals(0,summary.getLong("writeErrors"));
  }
 }
}
