package com.drbep.tvplayer;
import android.database.sqlite.SQLiteDatabase;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import org.json.*;
public class QueueConcurrentReadFireCheck {
 public static void main(String[] args)throws Exception {
  android.os.Process.setThreadPriority(10);QueueTransactionFireCheck.writer=SQLiteDatabase.openDatabase(args[0],null,0);QueueTransactionFireCheck.writer.enableWriteAheadLogging();QueueTransactionFireCheck.reader=SQLiteDatabase.openDatabase(args[0],null,SQLiteDatabase.OPEN_READONLY);JSONArray results=new JSONArray();
  for(int version:new int[]{8,9}){
   if(version==9)QueueTransactionFireCheck.writer.execSQL("CREATE INDEX entries_series_group ON entries(source,generation,kind,series,group_name)");
   QueueTransactionFireCheck.reset();QueueTransactionFireCheck.run(true,-1,-1,false);final String complete=QueueTransactionFireCheck.snapshot();QueueTransactionFireCheck.reset();final String old=QueueTransactionFireCheck.before;
   AtomicBoolean stop=new AtomicBoolean();AtomicInteger reads=new AtomicInteger();AtomicReference<Throwable> error=new AtomicReference<>();CountDownLatch ready=new CountDownLatch(1);
   Thread thread=new Thread(()->{try{while(!stop.get()){String view=QueueTransactionFireCheck.snapshot();if(!view.equals(old)&&!view.equals(complete))throw new AssertionError("Partial queue across threads");reads.incrementAndGet();ready.countDown();Thread.sleep(1);}}catch(Throwable t){error.set(t);ready.countDown();}},"synthetic-panel-reader");thread.setDaemon(true);thread.start();
   if(!ready.await(5,TimeUnit.SECONDS))throw new AssertionError("Reader timeout");
   QueueTransactionFireCheck.Outcome out;
   try{out=QueueTransactionFireCheck.run(true,-1,-1,false);}finally{stop.set(true);thread.join(5000);}
   if(thread.isAlive()||error.get()!=null)throw new AssertionError("Reader failed",error.get());
   if(!out.prepared||!out.marker||!QueueTransactionFireCheck.snapshot().equals(complete))throw new AssertionError("Commit mismatch");
   results.put(new JSONObject().put("indexes",version).put("concurrentReads",reads.get()).put("onlyOldOrComplete",true));
  }
  QueueTransactionFireCheck.reader.close();QueueTransactionFireCheck.writer.close();System.out.println(new JSONObject().put("scenarios",results).toString(2));
 }
}
