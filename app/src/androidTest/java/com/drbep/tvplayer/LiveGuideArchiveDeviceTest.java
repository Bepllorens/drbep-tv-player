package com.drbep.tvplayer;
import org.junit.Test;import static org.junit.Assert.*;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.*;import java.time.*;
public class LiveGuideArchiveDeviceTest {
 @Test public void refreshedScheduleReplacesOldStartWithoutMergingRepeatsOrDeletingBookmarks()throws Exception{
  android.content.Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  String db="guide-replace-fixture.db";c.deleteDatabase(db);
  try(U7dCatalogStore store=new U7dCatalogStore(c,db,new java.io.File(c.getCacheDir(),"no-fixture"))){
   LocalDate day=LocalDate.now();long base=day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
   EpgRepository.EpgProgram old=programme("c",base+3600000,base+7200000);
   EpgRepository.EpgProgram fresh=programme("c",base+4500000,base+7200000);
   EpgRepository.EpgProgram repeat=programme("c",base+10800000,base+14400000);
   EpgRepository.EpgProgram saved=programme("c",base+18000000,base+21600000);
   EpgRepository.EpgProgram other=programme("other",base+3600000,base+7200000);
   store.index("a","c",Arrays.asList(old,repeat,saved));store.setSaved("a",saved,base+86400000,true);
   store.index("b","c",Arrays.asList(old));store.index("a","other",Arrays.asList(other));
   store.replaceGuideDay("a","c",day.toString(),Arrays.asList(fresh,repeat));
   List<EpgRepository.EpgProgram> rows=store.read("a","c");
   assertEquals(3,rows.size());assertEquals(fresh.startTime,rows.get(0).startTime);
   assertTrue(store.saved("a",U7dCatalogStore.id(saved)));assertEquals(1,store.savedPrograms("a").size());
   assertEquals(old.startTime,store.read("b","c").get(0).startTime);
   assertEquals(1,store.read("a","other").size());
   store.replaceGuideDay("a","c",day.toString(),Collections.emptyList());assertEquals(3,store.read("a","c").size());
   try{store.replaceGuideDay("a","c",day.toString(),Arrays.asList(other));fail();}catch(IllegalArgumentException expected){}
   assertEquals(3,store.read("a","c").size());
  }finally{c.deleteDatabase(db);}
 }
 private EpgRepository.EpgProgram programme(String c,long start,long end){
  return new EpgRepository.EpgProgram(c,"Channel","Same title","","",Instant.ofEpochMilli(start).toString(),Instant.ofEpochMilli(end).toString(),0);
 }
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
