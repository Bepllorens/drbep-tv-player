
package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class CatchupConcurrencyDeviceTest {
 @Test public void readersContinueWhileBatchWriterIsBusy()throws Exception{
  Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
  String name="catchup-concurrency-test.db";ctx.deleteDatabase(name);
  ExecutorService pool=Executors.newFixedThreadPool(2);
  CountDownLatch writing=new CountDownLatch(1),finish=new CountDownLatch(1);
  U7dCatalogStore db=new U7dCatalogStore(ctx,name,null);
  long now=System.currentTimeMillis();
  EpgRepository.EpgProgram p=U7dCatalogStore.decode(new JSONObject()
   .put("channel","fixture").put("title","Synthetic fixture")
   .put("start",java.time.Instant.ofEpochMilli(now-60000).toString())
   .put("end",java.time.Instant.ofEpochMilli(now+60000).toString()));
  try{
   db.write("test","fixture",Collections.singletonList(p));
   List<EpgRepository.EpgProgram> slow=new AbstractList<EpgRepository.EpgProgram>(){
    public int size(){return 1;}
    public EpgRepository.EpgProgram get(int i){
     writing.countDown();
     try{if(!finish.await(10,TimeUnit.SECONDS))throw new AssertionError("writer timeout");}
     catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}
     return p;
    }
   };
   Future<?> writer=pool.submit(()->{try{db.indexAll("test",Collections.singletonMap("fixture",slow));}catch(Exception e){throw new RuntimeException(e);}});
   assertTrue(writing.await(5,TimeUnit.SECONDS));
   Future<U7dCatalogStore.Page> reader=pool.submit(()->db.page("test",Collections.singletonList("fixture"),ArchiveWindow.day(p.startTime),"","","",false,0,60,now));
   // This times out with indexAll synchronized on SQLiteOpenHelper itself.
   U7dCatalogStore.Page result=reader.get(2,TimeUnit.SECONDS);
   assertEquals(1,result.total);assertEquals(1,result.programs.size());
   finish.countDown();writer.get(5,TimeUnit.SECONDS);
   assertEquals(1,db.page("test",Collections.singletonList("fixture"),ArchiveWindow.day(p.startTime),"","","",false,0,60,now).total);
  }finally{finish.countDown();pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);db.close();ctx.deleteDatabase(name);}
 }
 @Test public void helperMonitorIsTheBlockingBoundary()throws Exception{
  Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
  String name="catchup-helper-monitor-test.db";ctx.deleteDatabase(name);
  U7dCatalogStore db=new U7dCatalogStore(ctx,name,null);db.getReadableDatabase();
  ExecutorService pool=Executors.newSingleThreadExecutor();
  CountDownLatch entered=new CountDownLatch(1);
  try{
   Future<?> read;
   synchronized(db){
    read=pool.submit(()->{entered.countDown();db.getReadableDatabase();});
    assertTrue(entered.await(2,TimeUnit.SECONDS));
    try{read.get(300,TimeUnit.MILLISECONDS);fail("Expected helper monitor contention");}
    catch(TimeoutException expected){}
   }
   read.get(2,TimeUnit.SECONDS);
  }finally{pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);db.close();ctx.deleteDatabase(name);}
 }
}
