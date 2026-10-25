package com.drbep.tvplayer;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.os.SystemClock;
import org.json.*;
import java.util.*;
public class MetadataFireBench {
 static JSONArray rows(SQLiteDatabase db,String sql,String[] args){
  JSONArray all=new JSONArray();try(Cursor c=db.rawQuery(sql,args)){while(c.moveToNext()){JSONArray row=new JSONArray();for(int i=0;i<c.getColumnCount();i++)row.put(c.isNull(i)?JSONObject.NULL:c.getString(i));all.put(row);}}return all;
 }
 static double[] pair(SQLiteDatabase db,String old,String[] oa,String sql,String[] a){
  for(int i=0;i<2;i++){rows(db,old,oa);rows(db,sql,a);}double[][] times=new double[2][9];
  for(int i=0;i<9;i++)for(int j=0;j<2;j++){int v=i%2==0?j:1-j;long t=SystemClock.elapsedRealtimeNanos();rows(db,v==0?old:sql,v==0?oa:a);times[v][i]=(SystemClock.elapsedRealtimeNanos()-t)/1e6;}
  Arrays.sort(times[0]);Arrays.sort(times[1]);return new double[]{times[0][4],times[1][4]};
 }
 public static void main(String[] args)throws Exception {
  android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
  JSONObject report=new JSONObject();JSONArray timings=new JSONArray();int equal=0;
  report.put("priority",android.os.Process.getThreadPriority(android.os.Process.myTid()));
  for(String file:args){boolean grouped=file.contains("12000.db");SQLiteDatabase db=SQLiteDatabase.openDatabase(file,null,SQLiteDatabase.OPEN_READWRITE);
   report.put("sqliteVersion",rows(db,"SELECT sqlite_version()",null).getJSONArray(0).getString(0));
   for(String phase:new String[]{"mixed","complete"}){
    if(phase.equals("complete"))db.execSQL("UPDATE metadata_index SET state='done' WHERE state='pending'");
    for(String source:grouped?new String[]{"s0"}:new String[]{"","s0","s19","missing","x' OR 1=1 --"})for(String kind:new String[]{"","movie","series"}){
     String group=grouped?"Drama":"";MetadataIndexSelection chosen=new MetadataIndexSelection(source,kind,group);
     String oldWhere="(?='' OR source=?) AND (?='' OR kind=?)"+(grouped?" AND "+MetadataIndexSelection.groupPredicate("metadata_index"):"");
     String[] oldArgs=grouped?new String[]{source,source,kind,kind,group}:new String[]{source,source,kind,kind};
     for(String name:new String[]{"counts","next","exists"}){
      String prefix=name.equals("counts")?"SELECT state,COUNT(*) FROM metadata_index WHERE ":name.equals("next")?"SELECT rowid,kind,source,generation,identity,occurrence FROM metadata_index WHERE state='pending' AND ":"SELECT 1 FROM metadata_index WHERE state='pending' AND ";
      String suffix=name.equals("counts")?" GROUP BY state":name.equals("next")?" ORDER BY rowid LIMIT 1":" LIMIT 1";
      String before=prefix+oldWhere+suffix,after=prefix+(name.equals("counts")?chosen.countWhere():chosen.where())+suffix;
      String[] ca=name.equals("counts")?chosen.countArgs():chosen.args();
      if(!rows(db,before,oldArgs).toString().equals(rows(db,after,ca).toString()))throw new AssertionError("Mismatch "+phase+" "+name+" "+source+" "+kind);
      equal++;
      if(!source.equals("missing")&&!source.startsWith("x'")&&!name.equals("exists")){
       double[] t=pair(db,before,oldArgs,after,ca);timings.put(new JSONObject().put("rows",grouped?12000:120000).put("phase",phase).put("source",source).put("kind",kind).put("group",group).put("query",name).put("before_ms",t[0]).put("after_ms",t[1]));
      }
     }
    }
   }db.close();
  }
  report.put("equivalences",equal).put("timings",timings);System.out.println(report.toString(2));
 }
}
