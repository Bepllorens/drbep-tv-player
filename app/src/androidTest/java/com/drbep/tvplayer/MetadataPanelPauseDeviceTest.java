package com.drbep.tvplayer;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.drbep.tvplayer.playlists.PlaylistDatabase;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

/** Run ONLY in the disposable com.drbep.panelcheck APK, never the real app. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class MetadataPanelPauseDeviceTest {
    Context c; SQLiteDatabase db; DeviceWebPanel panel; String session; int port;
    volatile boolean finish; volatile boolean verifying;
    final JSONArray timings=new JSONArray(); volatile JSONObject verification;
    static class Reply {JSONObject json; double ms; Reply(JSONObject j,double m){json=j;ms=m;}}
    @Test public void isolatedPanel() throws Exception {
        c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        org.junit.Assume.assumeTrue("Disposable package required; never real app data", "com.drbep.panelcheck".equals(c.getPackageName()));
        ServerSocket metadata=new ServerSocket(0,20,InetAddress.getByName("127.0.0.1"));
        Thread stub=new Thread(()->{while(!metadata.isClosed())try(Socket s=metadata.accept()){
            BufferedReader in=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8));
            String line;while((line=in.readLine())!=null&&!line.isEmpty()){}
            byte[] body="{\"matched\":true,\"movie\":{\"title\":\"Synthetic title\",\"genres\":[]}}".getBytes(StandardCharsets.UTF_8);
            s.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "+body.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));s.getOutputStream().write(body);
        }catch(IOException ex){if(!metadata.isClosed())throw new RuntimeException(ex);}},"synthetic-metadata");stub.setDaemon(true);stub.start();
        try {
            CatalogSnapshotStore store=new CatalogSnapshotStore(c);
            store.setSourceUrl("http://127.0.0.1:"+metadata.getLocalPort()+"/catalog");store.setAccessToken("synthetic-local-only");
            PlaylistDatabase helper=PlaylistDatabase.getInstance(c);db=helper.getWritableDatabase();
            String source=helper.createSource("ENSAYO AISLADO · 1200 títulos","file");
            StringBuilder m3u=new StringBuilder("#EXTM3U\n");
            for(int i=0;i<1200;i++)m3u.append("#EXTINF:-1 type=\"movie\" group-title=\"Ensayo\",Synthetic ").append(i).append("\nhttps://example.invalid/").append(i).append("\n");
            helper.importReader(source,new StringReader(m3u.toString()));
            new ImportedMovieRepository(c,null,"",Collections.emptyMap(),item->true,false).queueIndex(source,"");
            db.execSQL("UPDATE metadata_index SET state='done' WHERE rowid<=10");
            prefs().edit().clear().putString("source",source).putString("kind","movie").putString("group","")
                .putString("generation","fixture-before").putString("state","paused").putBoolean("prepare",false).putBoolean("enabled",false).commit();
            byte[] html;try(InputStream in=c.getAssets().open("device-panel.html");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);html=out.toByteArray();}
            panel=new DeviceWebPanel(new DeviceWebPanel.Host(){
                public String status(){return "{\"device\":\"ENSAYO AISLADO\",\"jobs\":[],\"sources\":[]}";}
                public long importPlaylist(InputStream in){throw new AssertionError("Imports disabled");}
                public String metadataIndex(String command){return command.isEmpty()?MetadataIndexWorker.status(c):MetadataIndexWorker.command(c,command);}
                public String metadataIndexSelection(String command,String source,String kind,String group){return MetadataIndexWorker.command(c,command,source,kind,group);}
                public String metadataIndexGroups(String source,String kind){return MetadataIndexWorker.groups(c,source,kind);}
                public String metadataIndexReview(String state,long after){return MetadataIndexWorker.review(c,state,after);}
                public String manage(String body)throws Exception {
                    String action=new JSONObject(body).getString("action");
                    if(action.equals("test-report"))return new JSONObject().put("timings",timings).put("verification",verification).toString();
                    if(action.equals("test-finish")){finish=true;return "{}";}
                    if(action.equals("test-verify")){
                        if(verifying)throw new IllegalStateException("Already verifying");verifying=true;
                        new Thread(()->{try{verification=verify();}catch(Throwable ex){verification=new JSONObject();try{verification.put("error",ex.toString());}catch(Exception ignored){}}},"panel-verification").start();return "{}";
                    }
                    if(action.equals("sources")||action.equals("lists"))return "{\"sources\":[]}";
                    if(action.equals("jobs"))return "{\"jobs\":[]}";
                    throw new IOException("Disabled in isolated test");
                }
            },html);
            port=panel.start();
            JSONObject info=new JSONObject().put("port",port).put("pin",panel.pin());
            write("panel-info.json",info.toString());
            session=post("/session",panel.pin(),null).json.getString("token");
            long until=System.nanoTime()+TimeUnit.MINUTES.toNanos(12);
            while(!finish&&System.nanoTime()<until)Thread.sleep(200);
            assertTrue("Test not explicitly finished",finish);
            assertNotNull("Automated checks missing",verification);
            assertFalse(verification.toString(),verification.has("error"));
            write("panel-result.json",new JSONObject().put("timings",timings).put("verification",verification).toString());
        } finally {
            if(panel!=null)panel.close();metadata.close();
            MetadataIndexWorker.command(c,"pause");
            WorkManager.getInstance(c).cancelAllWork().getResult().get(10,TimeUnit.SECONDS);
        }
    }
    SharedPreferences prefs(){return c.getSharedPreferences("metadata-index",Context.MODE_PRIVATE);}
    void write(String name,String text)throws Exception{try(OutputStream out=c.openFileOutput(name,0)){out.write(text.getBytes(StandardCharsets.UTF_8));}}
    Reply post(String path,String body,String token)throws Exception{
        HttpURLConnection con=(HttpURLConnection)new URL("http://127.0.0.1:"+port+path).openConnection();con.setRequestMethod("POST");con.setDoOutput(true);con.setConnectTimeout(5000);con.setReadTimeout(15000);
        if(token!=null)con.setRequestProperty("X-Panel-Token",token);
        byte[] bytes=body.getBytes(StandardCharsets.UTF_8);con.setFixedLengthStreamingMode(bytes.length);long start=System.nanoTime();
        try{try(OutputStream out=con.getOutputStream()){out.write(bytes);}assertEquals(200,con.getResponseCode());
            try(InputStream in=con.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return new Reply(new JSONObject(out.toString("UTF-8")),(System.nanoTime()-start)/1e6);}
        }finally{con.disconnect();}
    }
    Reply command(String cmd)throws Exception{
        Reply r=post("/metadata-index",new JSONObject().put("command",cmd).toString(),session);
        synchronized(timings){timings.put(new JSONObject().put("command",cmd).put("ms",r.ms).put("state",r.json.getString("state")).put("done",r.json.optInt("done")));}
        return r;
    }
    int done(){try(Cursor cur=db.rawQuery("SELECT COUNT(*) FROM metadata_index WHERE state='done'",null)){cur.moveToFirst();return cur.getInt(0);}}
    Set<String> doneIds(){Set<String> ids=new HashSet<>();try(Cursor cur=db.rawQuery("SELECT identity FROM metadata_index WHERE state='done'",null)){while(cur.moveToNext())ids.add(cur.getString(0));}return ids;}
    void waitProgress(int before)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(done()<=before&&System.nanoTime()<end)Thread.sleep(50);assertTrue("Worker must advance",done()>before);}
    JSONObject verify()throws Exception {
        JSONArray cycles=new JSONArray();
        command("pause");
        for(int i=0;i<5;i++){
            Set<String> preserved=doneIds();int before=done();Reply resume=command("resume");assertTrue(resume.json.getBoolean("enabled"));waitProgress(before);
            Reply pause=command("pause");assertEquals("paused",pause.json.getString("state"));assertFalse(pause.json.getBoolean("enabled"));
            int paused=done();Thread.sleep(1200);assertEquals("Progress stable during pause",paused,done());assertTrue(paused>before);assertTrue("Completed identities preserved",doneIds().containsAll(preserved));
            cycles.put(new JSONObject().put("before",before).put("paused",paused).put("resume_ms",resume.ms).put("pause_ms",pause.ms));
        }
        // Real HTTP pause contending with the actual command mutex. The generation
        // is replaced under that mutex before the delayed pause can persist/cancel.
        Field lockField=MetadataIndexWorker.class.getDeclaredField("lock");lockField.setAccessible(true);Object lock=lockField.get(null);
        Field signalField=MetadataIndexWorker.class.getDeclaredField("pauseSignal");signalField.setAccessible(true);MetadataPauseSignal signal=(MetadataPauseSignal)signalField.get(null);
        // Block only the synthetic catalogue writer, so the real Worker enters
        // preparation and owns its command mutex while waiting for SQLite.
        Set<String> beforePreparationIds=doneIds();int beforePreparation=done();prefs().edit().putBoolean("prepare",true).commit();
        ExecutorService preparationExecutor=Executors.newSingleThreadExecutor();
        Future<Reply> preparingPause=null;long signalledAt=0;
        db.beginTransaction();
        try {
            command("resume");
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);boolean blocked=false;
            while(!blocked&&System.nanoTime()<deadline){
                for(StackTraceElement[] trace:Thread.getAllStackTraces().values()){
                    boolean worker=false,sqlite=false;
                    for(StackTraceElement element:trace){
                        worker|=element.getClassName().equals("com.drbep.tvplayer.MetadataIndexWorker");
                        sqlite|=element.getClassName().contains("SQLiteConnectionPool")&&element.getMethodName().contains("waitForConnection");
                    }
                    if(worker&&sqlite){blocked=true;break;}
                }
                if(!blocked)Thread.sleep(10);
            }
            assertTrue("Actual Worker blocked in SQLite preparation",blocked);
            String generation=prefs().getString("generation","");
            long sentAt=System.nanoTime();preparingPause=preparationExecutor.submit(()->command("pause"));
            deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(!signal.isPaused(generation)&&System.nanoTime()<deadline)Thread.sleep(2);
            assertTrue(signal.isPaused(generation));signalledAt=System.nanoTime()-sentAt;
        }finally{db.endTransaction();}
        try {
            Reply stopped=preparingPause.get(10,TimeUnit.SECONDS);
            assertEquals("paused",stopped.json.getString("state"));
            assertTrue("Unfinished preparation retained",prefs().getBoolean("prepare",false));
            assertEquals("No progress lost during preparation rollback",beforePreparation,done());assertEquals(beforePreparationIds,doneIds());
            command("resume");waitProgress(beforePreparation);command("pause");
            assertFalse("Preparation completed on resume",prefs().getBoolean("prepare",true));
        }finally{preparationExecutor.shutdownNow();}
        ExecutorService executor=Executors.newSingleThreadExecutor();Future<Reply> oldPause;String old,newer;
        try {
            synchronized(lock){
                MetadataIndexWorker.command(c,"resume");
                old=prefs().getString("generation","");
                assertFalse("Fresh generation has no previous pause signal",signal.isPaused(old));
                oldPause=executor.submit(()->command("pause"));
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
                while(!signal.isPaused(old)&&System.nanoTime()<deadline)Thread.sleep(5);
                assertTrue("Pause published while mutex held",signal.isPaused(old));
                // Retire A under the same mutex, as a completed worker would,
                // then create B while A's HTTP pause is waiting for that mutex.
                prefs().edit().putBoolean("enabled",false).putString("state","complete").commit();
                MetadataIndexWorker.command(c,"resume");newer=prefs().getString("generation","");assertNotEquals(old,newer);
            }
            Reply late=oldPause.get(10,TimeUnit.SECONDS);assertTrue("Stale pause must leave newer enabled",late.json.getBoolean("enabled"));
            assertEquals(newer,prefs().getString("generation",""));int before=done();waitProgress(before);
            int active=0;for(WorkInfo info:WorkManager.getInstance(c).getWorkInfosForUniqueWork("drbep-metadata-index").get(5,TimeUnit.SECONDS))if(!info.getState().isFinished())active++;
            assertTrue("New work still scheduled/running",active>0);
            command("pause");
            return new JSONObject().put("cycles",cycles).put("preparation_pause_signal_ms",signalledAt/1e6).put("preparation_rollback_preserved_progress",true).put("stale_pause_ignored",true).put("new_worker_advanced",true).put("final_done",done()).put("total",1200);
        }finally{executor.shutdownNow();}
    }
}
