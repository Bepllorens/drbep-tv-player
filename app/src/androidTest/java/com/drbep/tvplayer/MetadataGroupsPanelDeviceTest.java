package com.drbep.tvplayer;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.WorkManager;
import com.drbep.tvplayer.playlists.PlaylistDatabase;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Only the disposable package is permitted; never run against the real catalogue. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class MetadataGroupsPanelDeviceTest {
 Context c;SQLiteDatabase db;volatile boolean finish;volatile String delaySource="";
 JSONArray records=new JSONArray();String[] ids=new String[5];Map<String,?> savedPrefs;String savedQueue;
 @Test public void groupsPanel()throws Exception {
  c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  org.junit.Assume.assumeTrue("Disposable package required",c.getPackageName().equals("com.drbep.panelcheck"));
  PlaylistDatabase helper=PlaylistDatabase.getInstance(c);db=helper.getWritableDatabase();
  String[] names={"A · ENSAYO mixto 12000","B · ENSAYO alternativo","C · ENSAYO 1105 grupos","D · ENSAYO preview","E · ENSAYO vacía"};
  for(int i=0;i<ids.length;i++)ids[i]=helper.createSource(names[i],"file");
  db.beginTransaction();try {
   for(int i=0;i<ids.length;i++)db.execSQL("UPDATE sources SET active=?,preview=? WHERE id=?",new Object[]{i==3?null:"g1",i==3?"g2":null,ids[i]});
   try(SQLiteStatement entry=db.compileStatement("INSERT INTO entries(source,generation,identity,occurrence,position,kind,title,series,season,episode,group_name,logo,url,attributes,directives) VALUES(?,?,?,0,?,?,?,?,1,1,?,'','https://example.invalid/test','{}','[]')");SQLiteStatement hidden=db.compileStatement("INSERT INTO preferences(source,identity,occurrence,hidden) VALUES(?,?,0,1)")){
    for(int s=0;s<4;s++){
     int count=s==0?12000:s==1?300:s==2?1105:64;
     for(int i=0;i<count;i++){
      String kind=s==2?"movie":i%2==0?"movie":"series";
      String group;
      if(s==0){String[] g=kind.equals("movie")?new String[]{"Cine","Drama's","Compartido","Oculto","","Niños — Familia"}:new String[]{"Series","Documentales","Compartido","Oculto","","Niños — Familia"};group=g[(i/2)%g.length];}
      else if(s==1)group=kind.equals("movie")?"B cine":"B series";
      else if(s==2)group=String.format(Locale.ROOT,"G%04d",i);
      else group=kind.equals("movie")?"Avance cine":"Avance series";
      put(entry,ids[s],s==3?"g2":"g1","entry-"+i,i,kind,group);
      if(group.equals("Oculto")||i%101==0){hidden.bindString(1,ids[s]);hidden.bindString(2,"entry-"+i);hidden.executeInsert();}
     }
    }
    put(entry,ids[0],"g0","old",20000,"movie","Antiguo");
    put(entry,ids[0],"g1","live",20001,"live","Solo TV");
   }
   db.setTransactionSuccessful();
  }finally{db.endTransaction();}
  new ImportedMovieRepository(c,null,"",Collections.emptyMap(),item->true,false).queueIndex(ids[0],"");
  db.execSQL("UPDATE metadata_index SET state='done' WHERE rowid<=5");
  SharedPreferences prefs=c.getSharedPreferences("metadata-index",0);
  prefs.edit().clear().putString("source",ids[0]).putString("kind","movie").putString("group","Drama's").putString("generation","groups-fixture").putString("state","paused").putBoolean("enabled",false).putBoolean("prepare",false).commit();
  savedPrefs=new HashMap<>(prefs.getAll());savedQueue=digest();
  byte[] html;try(InputStream in=c.getAssets().open("device-panel.html");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);html=out.toByteArray();}
  // Measurement only: production HTML/handlers stay intact; append a DOM observer.
  String observer="<script>(()=>{const source=document.getElementById('metadataSource'),kind=document.getElementById('metadataKind'),group=document.getElementById('metadataGroup'),out=document.createElement('pre');out.id='test-measurements';out.hidden=true;document.body.append(out);let pending=null,rows=[];const state=()=>({source:source.value,kind:kind.value,group:group.value,disabled:group.disabled,groups:Array.from(group.options).map(x=>x.value)});const write=()=>out.textContent=JSON.stringify({rows,state:state()});for(const field of [source,kind])field.addEventListener('change',()=>{pending={label:field.id,start:performance.now()};write()},true);group.addEventListener('change',write);new MutationObserver(()=>{if(group.disabled&&source.value&&!pending)pending={label:'initial-or-programmatic',start:performance.now()};if(pending&&!group.disabled){const measurement=pending;pending=null;requestAnimationFrame(()=>{rows.push({label:measurement.label,ms:performance.now()-measurement.start,...state()});write()})}else write()}).observe(group,{attributes:true,childList:true,subtree:true});write()})()</script>";
  DeviceWebPanel panel=new DeviceWebPanel(new DeviceWebPanel.Host(){
   public String status(){return "{\"Dispositivo\":\"ENSAYO AISLADO grupos\",\"Importaciones\":[]}";}
   public long importPlaylist(InputStream in){throw new AssertionError("No imports");}
   public String metadataIndex(String command){if(!command.isEmpty())throw new AssertionError("Read-only test");return MetadataIndexWorker.status(c);}
   public String metadataIndexSelection(String command,String source,String kind,String group){throw new AssertionError("Read-only test");}
   public String metadataIndexGroups(String source,String kind)throws Exception {
    if(source.equals(delaySource))Thread.sleep(1200);
    long start=System.nanoTime();String result=MetadataIndexWorker.groups(c,source,kind);double ms=(System.nanoTime()-start)/1e6;
    synchronized(records){records.put(new JSONObject().put("source",source).put("kind",kind).put("ms",ms).put("delayed",source.equals(delaySource)));}
    return result;
   }
   public String manage(String body)throws Exception {
    JSONObject input=new JSONObject(body);String action=input.getString("action");
    if(action.equals("jobs"))return "{\"jobs\":[]}";
    if(action.equals("lists"))return "{\"sources\":[]}";
    if(action.equals("test-delay")){delaySource=input.optString("source","");return "{}";}
    if(action.equals("test-report"))return report().toString();
    if(action.equals("test-finish")){finish=true;return "{}";}
    throw new IOException("Not part of test");
   }
  },(new String(html,StandardCharsets.UTF_8)+observer).getBytes(StandardCharsets.UTF_8));
  try {
   int port=panel.start();JSONObject info=new JSONObject().put("port",port).put("pin",panel.pin()).put("ids",new JSONArray(Arrays.asList(ids)));
   write("groups-panel-info.json",info.toString());
   long deadline=System.nanoTime()+TimeUnit.MINUTES.toNanos(15);
   while(!finish&&System.nanoTime()<deadline)Thread.sleep(200);
   assertTrue("Explicit finish required",finish);JSONObject result=report();
   assertEquals(savedPrefs,prefs.getAll());assertEquals(savedQueue,digest());
   assertTrue(WorkManager.getInstance(c).getWorkInfosForUniqueWork("drbep-metadata-index").get(5,TimeUnit.SECONDS).isEmpty());
   write("groups-panel-result.json",result.toString());
  }finally{panel.close();}
 }
 void put(SQLiteStatement e,String source,String gen,String id,int pos,String kind,String group){
  e.bindString(1,source);e.bindString(2,gen);e.bindString(3,id);e.bindLong(4,pos);e.bindString(5,kind);e.bindString(6,"Synthetic "+id);e.bindString(7,kind.equals("series")?"Synthetic series "+pos:"");e.bindString(8,group);e.executeInsert();
 }
 void write(String name,String text)throws Exception{try(OutputStream out=c.openFileOutput(name,0)){out.write(text.getBytes(StandardCharsets.UTF_8));}}
 String digest()throws Exception {
  MessageDigest h=MessageDigest.getInstance("SHA-256");
  try(Cursor rows=db.rawQuery("SELECT kind,source,generation,identity,occurrence,series,state FROM metadata_index ORDER BY rowid",null)){while(rows.moveToNext())for(int i=0;i<7;i++)h.update((rows.getString(i)+"\n").getBytes(StandardCharsets.UTF_8));}
  return android.util.Base64.encodeToString(h.digest(),android.util.Base64.NO_WRAP);
 }
 JSONObject report()throws Exception {
  int equal=0;JSONArray cases=new JSONArray();
  for(String source:ids)for(String kind:new String[]{"","movie","series"}){
   JSONObject actual=new JSONObject(MetadataIndexWorker.groups(c,source,kind));JSONArray expected=new JSONArray();
   try(Cursor rows=db.rawQuery(MetadataIndexGroups.LEGACY,new String[]{source,kind,kind})){while(rows.moveToNext())expected.put(rows.getString(0));}
   boolean truncated=expected.length()>1000;if(truncated)expected.remove(1000);
   assertEquals(expected.toString(),actual.getJSONArray("groups").toString());assertEquals(truncated,actual.getBoolean("truncated"));equal++;
   cases.put(new JSONObject().put("source",source).put("kind",kind).put("groups",expected).put("truncated",truncated));
  }
  assertEquals(savedPrefs,c.getSharedPreferences("metadata-index",0).getAll());assertEquals(savedQueue,digest());
  return new JSONObject().put("equivalences",equal).put("cases",cases).put("timings",records).put("preferences_unchanged",true).put("queue_unchanged",true);
 }
}
