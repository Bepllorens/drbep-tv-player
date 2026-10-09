package com.drbep.tvplayer;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.WorkManager;
import com.drbep.tvplayer.playlists.PlaylistDatabase;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PlaylistImportUpgradeTest {
 private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
 private PlaylistDatabase db(){return PlaylistDatabase.getInstance(context);}
 private String fixture(int n){StringBuilder s=new StringBuilder("#EXTM3U\n");for(int i=0;i<n;i++)s.append("#EXTINF:-1 type=\"movie\",Test ").append(i).append("\nhttps://example.test/movie\n");return s.toString();}
 private int count(String id){try(Cursor c=db().page(id,"movie",-1,200)){return c.getCount();}}
 private PlaylistDatabase.Progress noProgress(){return new PlaylistDatabase.Progress(){public void check(){}public void update(long count){}};}
 @Test public void previewIsReadableBeforeCompletionAndFailureRollsItBack()throws Exception {
  String id=db().createSource("Test preview","file");boolean[] seen={false};
  try {
   try {db().importReader(id,new StringReader(fixture(600)+"#EXTINF:-1,Broken\n"),new PlaylistDatabase.Progress(){
    public void check(){}public void update(long n){assertEquals(500,n);assertEquals(200,count(id));seen[0]=true;}
   });fail("Truncated list accepted");}catch(IOException expected){}
   assertTrue(seen[0]);assertEquals(0,count(id));
   db().importReader(id,new StringReader(fixture(1)));
   try{db().importReader(id,new StringReader(fixture(600)),new PlaylistDatabase.Progress(){public void check(){}public void update(long n)throws Exception{assertEquals(1,count(id));throw new InterruptedIOException();}});fail();}catch(InterruptedIOException expected){}
   assertEquals(1,count(id));
  }finally{db().deleteSource(id);}
 }
 @Test public void guideReusesDownloadChecks304AndRetainsPreviousOnMalformedXml()throws Exception {
  String one=db().createSource("Test guide 1","file"),two=db().createSource("Test guide 2","file");
  try(GuideServer server=new GuideServer()) {
   String url="http://127.0.0.1:"+server.socket.getLocalPort()+"/guide.xml",key=PlaylistGuideSync.key(url);
   db().setGuideUrl(one,url);db().setGuideUrl(two,url);
   PlaylistGuideSync.refresh(db(),one,noProgress());assertEquals(1,server.requests);
   assertEquals("Guía reciente reutilizada",PlaylistGuideSync.refresh(db(),two,noProgress()));assertEquals(1,server.requests);
   try(Cursor c=db().guide(two,"test",0,Long.MAX_VALUE,0)){assertTrue(c.moveToFirst());assertEquals("Programa",c.getString(c.getColumnIndexOrThrow("title")));}
   db().getWritableDatabase().execSQL("UPDATE guide_feeds SET checked=0 WHERE id=?",new Object[]{key});
   assertEquals("Guía sin cambios",PlaylistGuideSync.refresh(db(),one,noProgress()));assertEquals(2,server.requests);assertTrue(server.conditional);
   db().getWritableDatabase().execSQL("UPDATE guide_feeds SET checked=0,coverage=0 WHERE id=?",new Object[]{key});
   PlaylistGuideSync.refresh(db(),one,noProgress());assertEquals(3,server.requests);assertFalse(server.conditional);
   db().getWritableDatabase().execSQL("UPDATE guide_feeds SET checked=0 WHERE id=?",new Object[]{key});server.malformed=true;
   try{PlaylistGuideSync.refresh(db(),one,noProgress());fail("Invalid guide accepted");}catch(Exception expected){}
   try(Cursor c=db().guide(one,"test",0,Long.MAX_VALUE,0)){assertTrue(c.moveToFirst());assertEquals("Programa",c.getString(c.getColumnIndexOrThrow("title")));}
   assertNotEquals(PlaylistGuideSync.key(url+"?token=a"),PlaylistGuideSync.key(url+"?token=b"));
   db().getWritableDatabase().delete("guide_feeds","id=?",new String[]{key});
  }finally{db().deleteSource(one);db().deleteSource(two);}
 }
 @Test public void durableUploadFinishesWithoutControllerAndSupportsCancellation()throws Exception {
  String id=null,source=null;
  try {
   id=PlaylistImportJobs.queueUpload(context,new ByteArrayInputStream(fixture(1200).getBytes(StandardCharsets.UTF_8)));
   source=sourceFor(id);long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(80);String state="";
   while(System.nanoTime()<until){state=state(id);if(state.equals("complete")||state.equals("error"))break;Thread.sleep(200);}
   assertEquals("complete",state);assertEquals(200,count(source));
   WorkManager.getInstance(context).cancelAllWorkByTag("nonexistent-test-tag").getResult().get();
   String large=PlaylistImportJobs.stage(context,new ByteArrayInputStream(fixture(10000).getBytes(StandardCharsets.UTF_8)));
   String next=PlaylistImportJobs.enqueue(context,source,"file",large);PlaylistImportJobs.cancel(context,next);
   Thread.sleep(500);assertEquals("cancelled",state(next));assertEquals(200,count(source));
  }finally{if(source!=null){db().deleteSource(source);db().getWritableDatabase().delete("import_jobs","source=?",new String[]{source});}}
 }
 private String sourceFor(String id){try(Cursor c=db().getReadableDatabase().rawQuery("SELECT source FROM import_jobs WHERE id=?",new String[]{id})){assertTrue(c.moveToFirst());return c.getString(0);}}
 private String state(String id){try(Cursor c=db().getReadableDatabase().rawQuery("SELECT state FROM import_jobs WHERE id=?",new String[]{id})){assertTrue(c.moveToFirst());return c.getString(0);}}
 static final class GuideServer implements AutoCloseable {
  final ServerSocket socket=new ServerSocket(0);volatile int requests;volatile boolean conditional,malformed;
  final Thread thread;
  GuideServer()throws Exception {thread=new Thread(() -> {while(!socket.isClosed())try(Socket client=socket.accept()){
    BufferedReader reader=new BufferedReader(new InputStreamReader(client.getInputStream(),StandardCharsets.US_ASCII));String line;conditional=false;
    while((line=reader.readLine())!=null&&!line.isEmpty())if(line.toLowerCase(Locale.ROOT).startsWith("if-none-match:"))conditional=true;
    requests++;boolean unchanged=conditional&&!malformed;
    String xml="<tv><programme channel=\"test\" start=\"20260101000000 +0000\" stop=\"20300101000000 +0000\"><title>Programa</title></programme></tv>";
    byte[] bytes=(unchanged?"":malformed?"<tv><programme>":xml).getBytes(StandardCharsets.UTF_8);
    client.getOutputStream().write(((unchanged?"HTTP/1.1 304 Not Modified":"HTTP/1.1 200 OK")+"\r\nETag: \"test-v1\"\r\nLast-Modified: Fri, 09 Oct 2026 00:00:00 GMT\r\nContent-Length: "+bytes.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));client.getOutputStream().write(bytes);
   }catch(Exception e){if(!socket.isClosed())throw new RuntimeException(e);}},"guide-fixture");thread.start();}
  public void close()throws Exception{socket.close();thread.join(2000);}
 }
}
