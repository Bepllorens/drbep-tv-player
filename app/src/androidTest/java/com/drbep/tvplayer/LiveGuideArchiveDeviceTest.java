package com.drbep.tvplayer;
import org.junit.Test;import static org.junit.Assert.*;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.*;import java.time.*;
public class LiveGuideArchiveDeviceTest {
 @Test public void historicalDayIsBoundedAndPrivate()throws Exception{
  android.content.Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  String db="guide-archive-fixture.db";c.deleteDatabase(db);
  try(U7dCatalogStore store=new U7dCatalogStore(c,db,new java.io.File(c.getCacheDir(),"nonexistent-guide-fixture"))){
   LocalDate day=LocalDate.now().minusDays(1);long base=day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
   List<EpgRepository.EpgProgram> rows=new ArrayList<>();
   for(int i=0;i<48;i++)rows.add(new EpgRepository.EpgProgram("fixture","Fixture","Programme "+i,"","",Instant.ofEpochMilli(base+i*1800000L).toString(),Instant.ofEpochMilli(base+(i+1)*1800000L).toString(),0));
   store.index("user-a","fixture",rows);
   List<EpgRepository.EpgProgram> window=store.readGuideDay("user-a","fixture",day.toString(),16);
   assertEquals(16,window.size());for(int i=0;i<16;i++)assertEquals(rows.get(i).title,window.get(i).title);
   assertTrue(store.readGuideDay("user-b","fixture",day.toString(),16).isEmpty());
   assertTrue(store.readGuideDay("user-a","other",day.toString(),16).isEmpty());
   assertTrue(store.readGuideDay("user-a","fixture",day.minusDays(1).toString(),16).isEmpty());
   try{store.readGuideDay("user-a","fixture",day.toString(),0);fail();}catch(IllegalArgumentException expected){}
  }finally{c.deleteDatabase(db);}
 }
}
