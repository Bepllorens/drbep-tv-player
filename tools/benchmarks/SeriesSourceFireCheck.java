package com.drbep.tvplayer;
import android.database.*;import android.database.sqlite.*;import org.json.*;
import java.util.*;import java.security.*;import java.nio.charset.StandardCharsets;

/** Actual prepared queue SQL versus previous outer-source filter, synthetic DB only. */
public class SeriesSourceFireCheck {
 static SQLiteDatabase db;
 static String sql(String kind,String source,String group,boolean after){
  boolean series=kind.equals("series");
  return series&&!source.isEmpty()&&!after?MetadataIndexQueue.sql(true,!group.isEmpty()):MetadataIndexQueue.sql(series,!group.isEmpty(),!source.isEmpty());
 }
 static String[] args(String kind,String source,String group,boolean after){
  boolean series=kind.equals("series");String[] a=MetadataIndexQueue.args(series,kind,source,group);
  if(series&&!source.isEmpty()&&!after){List<String> list=new ArrayList<>(Arrays.asList(a));list.remove(1);return list.toArray(new String[0]);}return a;
 }
 static String snapshot()throws Exception{
  MessageDigest h=MessageDigest.getInstance("SHA-256");
  try(Cursor c=db.rawQuery("SELECT kind,source,generation,identity,occurrence,series,state FROM metadata_index ORDER BY rowid",null)){
   while(c.moveToNext())for(int i=0;i<7;i++)h.update((c.getString(i)+"\n").getBytes(StandardCharsets.UTF_8));
  }return android.util.Base64.encodeToString(h.digest(),android.util.Base64.NO_WRAP);
 }
 static class Result{String hash;int count;double ms;}
 static void reset(){
  db.execSQL("DELETE FROM metadata_index");
  int offset=0;
  for(String state:new String[]{"done","error","unmatched","skipped"}){
   db.execSQL("INSERT OR IGNORE INTO metadata_index(kind,source,generation,identity,occurrence,series,state) SELECT e.kind,e.source,e.generation,e.identity,e.occurrence,e.series,? FROM entries e JOIN sources s ON s.id=e.source AND COALESCE(s.active,s.preview)=e.generation WHERE e.kind='series' ORDER BY e.rowid LIMIT 1 OFFSET ?",new Object[]{state,offset++});
  }
 }
 static Result run(String kind,String source,String group,boolean after,boolean retry)throws Exception{
  db.beginTransaction();try{
   reset();Result r=new Result();long start=System.nanoTime();db.execSQL(sql(kind,source,group,after),args(kind,source,group,after));r.ms=(System.nanoTime()-start)/1e6;r.hash=snapshot();
   try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM metadata_index",null)){c.moveToFirst();r.count=c.getInt(0);}
   try(Cursor c=db.rawQuery("SELECT state,COUNT(*) FROM metadata_index WHERE state<>'pending' GROUP BY state",null)){
    int states=0;while(c.moveToNext()){if(c.getInt(1)!=1)throw new AssertionError("Saved state changed");states++;}if(states!=4)throw new AssertionError("Saved progress lost");
   }
   if(retry){db.execSQL(sql(kind,source,group,after),args(kind,source,group,after));if(!r.hash.equals(snapshot()))throw new AssertionError("Retry changed order/progress");}
   db.setTransactionSuccessful();return r;
  }finally{db.endTransaction();}
 }
 static double median(double[] v){Arrays.sort(v);return v[v.length/2];}
 public static void main(String[] argv)throws Exception{
  if(argv.length!=1||!argv[0].equals("/data/local/tmp/drbep-series-source-20261024.db"))throw new IllegalArgumentException("Synthetic DB required");
  android.os.Process.setThreadPriority(10);db=SQLiteDatabase.openDatabase(argv[0],null,0);
  JSONArray timings=new JSONArray(),cases=new JSONArray();int checks=0;
  String[][] scopes={{"series","",""},{"series","s0",""},{"series","s0","Drama"},{"series","s19",""},{"series","s19","Drama"},{"series","absent",""},{"series","x' OR 1=1 --","Drama"},{"series","s0","Missing"},{"movie","s0",""}};
  try{
   for(int version:new int[]{8,9}){
    if(version==9)db.execSQL("CREATE INDEX entries_series_group ON entries(source,generation,kind,series,group_name)");
    for(String phase:new String[]{"visible","hidden","old_generation"}){
     db.beginTransaction();try{
      if(phase.equals("hidden"))db.execSQL("INSERT OR REPLACE INTO preferences(source,identity,occurrence,hidden) SELECT source,identity,occurrence,1 FROM entries WHERE source='s0'");
      if(phase.equals("old_generation"))db.execSQL("UPDATE sources SET active='g0' WHERE id='s0'");
      for(String[] scope:scopes){
       String kind=scope[0],source=scope[1],group=scope[2];
       if(!phase.equals("visible")&&!(source.equals("s0")&&kind.equals("series")))continue;
       Result a=run(kind,source,group,false,false),b=run(kind,source,group,true,true);
       if(!a.hash.equals(b.hash)||a.count!=b.count)throw new AssertionError("Membership/order mismatch "+Arrays.toString(scope));checks++;
       cases.put(new JSONObject().put("indexes",version).put("phase",phase).put("kind",kind).put("source",source).put("group",group).put("count",b.count).put("sha256",b.hash));
       if(phase.equals("visible")&&kind.equals("series")&&(source.equals("s0")||source.equals("s19"))&&group.isEmpty()){
        double[][] values=new double[2][3];
        for(int i=0;i<5;i++)for(int j=0;j<2;j++){int v=(i+j)%2;Result r=run(kind,source,group,v==1,false);if(!r.hash.equals(a.hash))throw new AssertionError("Benchmark changed candidates");if(i>=2)values[v][i-2]=r.ms;}
        timings.put(new JSONObject().put("indexes",version).put("source",source).put("group",group).put("before_ms",median(values[0])).put("after_ms",median(values[1])).put("samples",3));
       } else if(phase.equals("visible")&&kind.equals("series")&&(source.equals("s0")||source.equals("s19"))&&!group.isEmpty()){
        timings.put(new JSONObject().put("indexes",version).put("source",source).put("group",group).put("before_ms",a.ms).put("after_ms",b.ms).put("samples",1));
       }
       System.out.println("CHECK "+version+" "+phase+" "+kind+" "+source+" "+group);System.out.flush();
      }
     }finally{db.endTransaction();}
    }
   }
  }finally{db.close();}
  System.out.println(new JSONObject().put("checks",checks).put("saved_states_preserved",true).put("retry_idempotent",true).put("order_unchanged",true).put("cases",cases).put("timings",timings));
 }
}
