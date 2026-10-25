package com.drbep.maintenance;
import android.content.*;import android.database.*;import android.database.sqlite.*;import org.json.*;
import java.io.*;import java.security.*;import java.util.*;
final class IndexPlanProbe {
 static String queue(SQLiteDatabase db)throws Exception {MessageDigest h=MessageDigest.getInstance("SHA-256");try(Cursor c=db.rawQuery("SELECT rowid,kind,source,generation,identity,occurrence,series,state FROM metadata_index ORDER BY rowid",null)){while(c.moveToNext())for(int i=0;i<8;i++)h.update((c.getString(i)+"\n").getBytes("UTF-8"));}return IndexMaintenance.hex(h.digest());}
 static JSONObject run(Context ctx,File backup)throws Exception {
  IndexMaintenance.require(backup.isFile(),"Backup required");File clone=new File(ctx.getNoBackupFilesDir(),"index-perf-test.db");IndexMaintenance.require(!clone.exists(),"Probe clone exists");IndexMaintenance.copy(backup,clone);
  try(SQLiteDatabase db=SQLiteDatabase.openDatabase(clone.getPath(),null,0)){
   String source,group;int episodes;
   try(Cursor c=db.rawQuery("SELECT e.source,e.group_name,COUNT(*) FROM entries e JOIN sources s ON s.id=e.source AND COALESCE(s.active,s.preview)=e.generation LEFT JOIN preferences p ON p.source=e.source AND p.identity=e.identity AND p.occurrence=e.occurrence WHERE e.kind='series' AND e.group_name<>'' AND COALESCE(p.hidden,0)=0 GROUP BY e.source,e.group_name ORDER BY COUNT(*) DESC LIMIT 1",null)){IndexMaintenance.require(c.moveToFirst(),"No current series group");source=c.getString(0);group=c.getString(1);episodes=c.getInt(2);}
   String baseline=IndexMaintenance.digest(db);String sql=MetadataIndexQueue.sql(true,true,true);String[] args=MetadataIndexQueue.args(true,"series",source,group);String expected=null;double[][] times=new double[2][3];
   for(int round=0;round<4;round++)for(int step=0;step<2;step++){
    int variant=(round+step)%2;
    if(IndexMaintenance.index(db))db.execSQL("DROP INDEX entries_series_group");if(variant==1)db.execSQL(IndexMaintenance.CREATE);
    db.beginTransaction();try{
     long start=System.nanoTime();db.execSQL(sql,args);double ms=(System.nanoTime()-start)/1e6;String data=queue(db);
     if(expected==null)expected=data;else IndexMaintenance.require(expected.equals(data),"Index changes queue candidates/order/progress");
     if(round>0)times[variant][round-1]=ms;
    }finally{db.endTransaction();}
   }
   IndexMaintenance.require(baseline.equals(IndexMaintenance.digest(db)),"Probe altered data");Arrays.sort(times[0]);Arrays.sort(times[1]);
   return new JSONObject().put("ok",true).put("episodes_in_test_group",episodes).put("samples",3).put("before_ms",times[0][1]).put("after_ms",times[1][1]).put("same_queue_order_and_progress",true).put("clone_data_unchanged",true).put("schema_version",db.getVersion());
  }finally{clone.delete();new File(clone+"-journal").delete();new File(clone+"-wal").delete();new File(clone+"-shm").delete();}
 }
}
