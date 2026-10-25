package com.drbep.tvplayer;
import android.content.*;import android.database.*;import android.database.sqlite.*;
import androidx.test.platform.app.InstrumentationRegistry;import androidx.work.WorkManager;
import com.drbep.tvplayer.playlists.PlaylistDatabase;
import org.json.*;import org.junit.Test;import static org.junit.Assert.*;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.concurrent.*;
/** Real Worker on Fire, synthetic catalogue and loopback metadata ONLY. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class MetadataSeriesWorkerDeviceTest {
 Context c;SQLiteDatabase db;volatile boolean unavailable;ServerSocket server;
 Set<String> done(){Set<String> out=new TreeSet<>();try(Cursor q=db.rawQuery("SELECT identity FROM metadata_index WHERE state='done' ORDER BY rowid",null)){while(q.moveToNext())out.add(q.getString(0));}return out;}
 int count(String state){try(Cursor q=db.rawQuery("SELECT COUNT(*) FROM metadata_index WHERE state=?",new String[]{state})){q.moveToFirst();return q.getInt(0);}}
 void progress(int before)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(40);while(done().size()<=before&&System.nanoTime()<end)Thread.sleep(100);assertTrue("Real series Worker advances",done().size()>before);}
 void pause()throws Exception{MetadataIndexWorker.command(c,"pause");Thread.sleep(1300);Set<String> ids=done();Thread.sleep(1300);assertEquals("Stable pause",ids,done());}
 void write(JSONObject report)throws Exception{try(FileOutputStream f=c.openFileOutput("series-worker-result.json",0)){f.write(report.toString().getBytes(StandardCharsets.UTF_8));}}
 @Test public void seriesWorker()throws Exception {
  c=InstrumentationRegistry.getInstrumentation().getTargetContext();org.junit.Assume.assumeTrue("Disposable package only","com.drbep.panelcheck".equals(c.getPackageName()));
  server=new ServerSocket(0,20,InetAddress.getByName("127.0.0.1"));
  Thread network=new Thread(()->{while(!server.isClosed())try(Socket socket=server.accept()){
   BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));String line;while((line=in.readLine())!=null&&!line.isEmpty()){}
   byte[] bytes="{\"kind\":\"tv\",\"matched\":true,\"movie\":{\"title\":\"Synthetic series\",\"genres\":[]}}".getBytes(StandardCharsets.UTF_8);
   socket.getOutputStream().write(((unavailable?"HTTP/1.1 503 Unavailable":"HTTP/1.1 200 OK")+"\r\nContent-Length: "+bytes.length+"\r\nContent-Type: application/json\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().write(bytes);
  }catch(IOException e){if(!server.isClosed())throw new RuntimeException(e);}},"series-loopback");network.setDaemon(true);network.start();
  try {
   CatalogSnapshotStore session=new CatalogSnapshotStore(c);session.setSourceUrl("http://127.0.0.1:"+server.getLocalPort()+"/catalog");session.setAccessToken("synthetic-local-only");
   PlaylistDatabase helper=PlaylistDatabase.getInstance(c);db=helper.getWritableDatabase();SharedPreferences fixture=c.getSharedPreferences("series-test",0);
   String phase=InstrumentationRegistry.getArguments().getString("phase","prepare"),source;
   if(phase.equals("prepare")) {
    source=helper.createSource("Synthetic series · 30000 episodes","file");StringBuilder list=new StringBuilder("#EXTM3U\n");
    for(int s=0;s<300;s++)for(int e=1;e<=100;e++)list.append("#EXTINF:-1 type=\"series\" serie-title=\"Synthetic ").append(s).append("\" group-title=\"").append(s%2==0?"Selected":"Other").append("\",Synthetic ").append(s).append(" - S01E").append(String.format(Locale.ROOT,"%02d",e)).append(" - Episode\nhttps://example.invalid/").append(s).append('/').append(e).append("\n");
    helper.importReader(source,new StringReader(list.toString()));
    fixture.edit().putString("source",source).commit();
    new ImportedMovieRepository(c,null,"",Collections.emptyMap(),item->true,true).queueIndex(source,"Selected");
    assertEquals(150,count("pending"));db.execSQL("UPDATE metadata_index SET state='done' WHERE rowid IN (SELECT rowid FROM metadata_index ORDER BY rowid LIMIT 5)");
    Set<String> preserved=done();long t=System.nanoTime();MetadataIndexWorker.command(c,"start",source,"series","Selected");progress(5);pause();assertTrue(done().containsAll(preserved));
    fixture.edit().putStringSet("done",done()).commit();
    write(new JSONObject().put("phase",phase).put("episodes",30000).put("selected_series",150).put("done",done().size()).put("start_progress_pause_ms",(System.nanoTime()-t)/1e6).put("stable_pause",true));
   } else {
    source=fixture.getString("source","");assertFalse(source.isEmpty());Set<String> preserved=fixture.getStringSet("done",Collections.emptySet());assertEquals("Progress survives process restart",preserved,done());
    unavailable=true;MetadataIndexWorker.command(c,"resume");long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(count("error")==0&&System.nanoTime()<end)Thread.sleep(100);assertTrue("HTTP503 yields retryable errors",count("error")>0);pause();assertTrue(done().containsAll(preserved));
    unavailable=false;int before=done().size();MetadataIndexWorker.command(c,"retry");progress(before);pause();assertTrue(done().containsAll(preserved));
    assertEquals("No foreign group queued",0,scalar("SELECT COUNT(*) FROM metadata_index WHERE series NOT IN (SELECT series FROM entries WHERE group_name='Selected')"));
    long started=System.nanoTime();MetadataIndexWorker.command(c,"resume");long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(250);
    while(done().size()<150&&System.nanoTime()<deadline)Thread.sleep(250);
    assertEquals("Full selected group completes across bounded Worker batches",150,done().size());assertEquals(0,count("pending"));assertEquals(0,count("error"));
    long finishDeadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
    while(!c.getSharedPreferences("metadata-index",0).getString("state","").equals("complete")&&System.nanoTime()<finishDeadline)Thread.sleep(100);
    assertEquals("complete",c.getSharedPreferences("metadata-index",0).getString("state",""));
    write(new JSONObject().put("phase",phase).put("restart_preserved",true).put("http503_recovered",true).put("done",done().size()).put("remaining",count("pending")).put("groups_preserved",true).put("bounded_worker_batches_completed",true).put("complete_after_resume_ms",(System.nanoTime()-started)/1e6));
   }
  }finally{MetadataIndexWorker.command(c,"pause");WorkManager.getInstance(c).cancelAllWork().getResult().get(10,TimeUnit.SECONDS);server.close();}
 }
 long scalar(String sql){try(Cursor q=db.rawQuery(sql,null)){q.moveToFirst();return q.getLong(0);}}
}
