package com.drbep.tvplayer;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import java.util.*;

/** Native SQLite test; takes ONLY a disposable synthetic database path. */
public final class MetadataGroupsFireBench {
 static List<String> query(SQLiteDatabase db,String source,String kind,boolean after){
  boolean grouped=false;
  if(after)try(Cursor c=db.rawQuery(MetadataIndexGroups.probeSql(kind),MetadataIndexGroups.probeArgs(source,kind))){grouped=MetadataIndexGroups.grouped(c.getCount());}
  List<String> result=new ArrayList<>();
  try(Cursor c=db.rawQuery(MetadataIndexGroups.sql(kind,grouped),MetadataIndexGroups.args(source,kind,grouped))){while(c.moveToNext())result.add(c.getString(0));}
  return result;
 }
 static double median(List<Double> v){Collections.sort(v);return v.get(v.size()/2);}
 public static void main(String[] args)throws Exception {
  if(args.length!=1||!args[0].equals("/data/local/tmp/drbep-groups-20261024.db"))throw new IllegalArgumentException("Synthetic path required");
  SQLiteDatabase db=SQLiteDatabase.openDatabase(args[0],null,SQLiteDatabase.OPEN_READWRITE);
  JSONObject report=new JSONObject();JSONArray timings=new JSONArray();int equal=0;
  try {
   for(String phase:new String[]{"few","unique","all_hidden"}){
    db.beginTransaction();
    try {
     if(!phase.equals("few"))db.execSQL("UPDATE entries SET group_name=printf('Group %09d',rowid)");
     if(phase.equals("all_hidden"))db.execSQL("INSERT OR REPLACE INTO preferences(source,identity,occurrence,hidden) SELECT source,identity,occurrence,1 FROM entries WHERE source='s0'");
     for(String source:new String[]{"s0","s19","absent","x' OR 1=1 --"})for(String kind:new String[]{"","movie","series"}){
      List<String> old=query(db,source,kind,false),now=query(db,source,kind,true);
      if(!old.equals(now))throw new AssertionError(phase+source+kind);equal++;
      if(source.equals("s0")){
       for(int warm=0;warm<2;warm++){query(db,source,kind,false);query(db,source,kind,true);}
       List<Double> before=new ArrayList<>(),after=new ArrayList<>();
       for(int i=0;i<9;i++)for(int j=0;j<2;j++){
        boolean variant=((i+j)%2)==1;long start=System.nanoTime();query(db,source,kind,variant);
        (variant?after:before).add((System.nanoTime()-start)/1e6);
       }
       timings.put(new JSONObject().put("phase",phase).put("kind",kind.isEmpty()?"both":kind).put("returned",old.size()).put("before_ms",median(before)).put("after_ms",median(after)));
      }
     }
    } finally {db.endTransaction();}
   }
   report.put("equivalences",equal).put("timings",timings);System.out.println(report);
  }finally{db.close();}
 }
}
