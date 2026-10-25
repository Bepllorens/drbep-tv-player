package com.drbep.tvplayer;
import android.database.sqlite.SQLiteDatabase;import android.database.Cursor;import android.os.SystemClock;import org.json.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
public class QueueTransactionFireCheck {
 static SQLiteDatabase writer,reader;static String before;static int observations;static double maxRead,maxSmallRead,maxFullRead;
 static class Injected extends RuntimeException{}
 static String snapshot(){JSONArray rows=new JSONArray();try(Cursor c=reader.rawQuery("SELECT kind,source,generation,identity,occurrence,series,state FROM metadata_index ORDER BY kind,source,generation,identity,occurrence",null)){while(c.moveToNext()){JSONArray row=new JSONArray();for(int i=0;i<7;i++)row.put(c.getString(i));rows.put(row);}}return rows.toString();}
 static void observe(){long t=SystemClock.elapsedRealtimeNanos();String current=snapshot();double elapsed=(SystemClock.elapsedRealtimeNanos()-t)/1e6;maxRead=Math.max(maxRead,elapsed);if(before.length()<1000)maxSmallRead=Math.max(maxSmallRead,elapsed);else maxFullRead=Math.max(maxFullRead,elapsed);if(!current.equals(before))throw new AssertionError("Reader saw partial queue");observations++;}
 static void reset(){writer.beginTransaction();try{writer.execSQL("DELETE FROM metadata_index");writer.execSQL("INSERT INTO metadata_index SELECT kind,source,generation,identity,occurrence,series,'done' FROM entries WHERE kind='movie' AND generation='g1' LIMIT 1");writer.execSQL("INSERT INTO metadata_index SELECT kind,source,generation,identity,occurrence,series,'pending' FROM entries WHERE source='s0' AND generation='g0' LIMIT 1");writer.setTransactionSuccessful();}finally{writer.endTransaction();}before=snapshot();}
 static class Outcome{boolean prepared,marker;int ran;double ms;}
 static Outcome run(boolean staged,int cancel,int fail,boolean markerFail){
  AtomicBoolean active=new AtomicBoolean(true);Outcome result=new Outcome();List<Runnable> stages=new ArrayList<>();
  for(int i=0;i<3;i++){int step=i;stages.add(()->{result.ran++;if(step==0)writer.execSQL(MetadataIndexQueue.CLEAN_STALE);else {String kind=step==1?"movie":"series";writer.execSQL(MetadataIndexQueue.sql(step==2,false,false),MetadataIndexQueue.args(step==2,kind,"",""));}observe();if(step==fail)throw new Injected();if(step==cancel)active.set(false);});}
  MetadataIndexQueue.Transaction tx=new MetadataIndexQueue.Transaction(){public void begin(){writer.beginTransaction();}public void successful(){writer.setTransactionSuccessful();}public void end(){writer.endTransaction();}};
  Runnable marker=()->{if(markerFail)throw new Injected();result.marker=true;};long start=SystemClock.elapsedRealtimeNanos();
  try{result.prepared=staged?MetadataIndexQueue.prepareStages(tx,active::get,stages,marker):MetadataIndexQueue.prepare(tx,active::get,()->{for(Runnable stage:stages)stage.run();},marker);if(fail>=0||markerFail)throw new AssertionError("Expected failure");}catch(Injected e){if(fail<0&&!markerFail)throw e;}
  result.ms=(SystemClock.elapsedRealtimeNanos()-start)/1e6;return result;
 }
 static void assertBaseline(Outcome o){if(o.prepared||o.marker||!snapshot().equals(before))throw new AssertionError("Rollback or marker failed");}
 public static void main(String[] args)throws Exception{
  android.os.Process.setThreadPriority(10);writer=SQLiteDatabase.openDatabase(args[0],null,0);writer.enableWriteAheadLogging();reader=SQLiteDatabase.openDatabase(args[0],null,SQLiteDatabase.OPEN_READONLY);JSONArray results=new JSONArray();int checks=0;
  for(int version:new int[]{8,9}){
   if(version==9)writer.execSQL("CREATE INDEX entries_series_group ON entries(source,generation,kind,series,group_name)");
   for(int fail=0;fail<3;fail++){reset();Outcome o=run(true,-1,fail,false);assertBaseline(o);if(o.ran!=fail+1)throw new AssertionError("Did not stop failure");checks++;}
   for(int cancel=0;cancel<3;cancel++){reset();Outcome o=run(true,cancel,-1,false);assertBaseline(o);if(o.ran!=cancel+1)throw new AssertionError("Ran canceled stage");checks++;}
   reset();Outcome old=run(false,-1,-1,false);String complete=snapshot();if(!old.prepared||!old.marker||complete.equals(before))throw new AssertionError("Reference commit failed");checks++;
   reset();Outcome fresh=run(true,-1,-1,false);if(!fresh.prepared||!fresh.marker||!complete.equals(snapshot()))throw new AssertionError("Successful candidates differ");checks++;
   // A failed preference marker happens AFTER SQL commit; retry must be idempotent.
   reset();Outcome markerFailure=run(true,-1,-1,true);if(markerFailure.marker||!complete.equals(snapshot()))throw new AssertionError("Marker boundary failed");checks++;
   before=snapshot();Outcome retry=run(true,-1,-1,false);if(!retry.prepared||!retry.marker||!complete.equals(snapshot()))throw new AssertionError("Retry changed states/rows");checks++;
   double[][] times=new double[2][7];for(int i=0;i<9;i++)for(int j=0;j<2;j++){int v=i%2==0?j:1-j;reset();Outcome o=run(v==1,0,-1,false);assertBaseline(o);if(o.ran!=(v==0?3:1))throw new AssertionError("Canceled work count mismatch");if(i>=2)times[v][i-2]=o.ms;}
   double[][] normal=new double[2][7];for(int i=0;i<9;i++)for(int j=0;j<2;j++){int v=i%2==0?j:1-j;reset();Outcome o=run(v==1,-1,-1,false);if(!o.prepared||!o.marker||!complete.equals(snapshot()))throw new AssertionError("Normal result mismatch");if(i>=2)normal[v][i-2]=o.ms;}
   Arrays.sort(normal[0]);Arrays.sort(normal[1]);Arrays.sort(times[0]);Arrays.sort(times[1]);results.put(new JSONObject().put("indexes",version).put("cancel_after_cleanup_before_ms",times[0][3]).put("cancel_after_cleanup_after_ms",times[1][3]).put("successful_before_ms",old.ms).put("successful_after_ms",fresh.ms).put("successful_before_median_ms",normal[0][3]).put("successful_after_median_ms",normal[1][3]));
  }
  reader.close();writer.close();System.out.println(new JSONObject().put("rows",12000).put("checks",checks).put("readerObservations",observations).put("maxReaderMs",maxRead).put("maxBaselineReaderMs",maxSmallRead).put("maxFullSnapshotReaderMs",maxFullRead).put("timings",results).toString(2));
 }
}
