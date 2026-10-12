package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import androidx.test.core.app.ActivityScenario;
import java.util.*;
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class MovistarDirectCatalogDeviceTest {
 @Test public void actualCatchupCatalogUsesOfficialChannelCodes()throws Exception{
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   Thread.sleep(10000);
   scenario.onActivity(a->{try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("openArchiveCatalog",ChannelItem.class,boolean.class);m.setAccessible(true);m.invoke(a,null,false);}catch(Exception e){throw new AssertionError(e);}});
   java.util.concurrent.atomic.AtomicBoolean ready=new java.util.concurrent.atomic.AtomicBoolean();java.util.concurrent.atomic.AtomicReference<String> info=new java.util.concurrent.atomic.AtomicReference<>("");
   for(int i=0;i<90&&!ready.get();i++){
    Thread.sleep(500);
    scenario.onActivity(a->{try{
     java.lang.reflect.Field f=MainActivity.class.getDeclaredField("archivePrograms");f.setAccessible(true);
     Map<String,List<EpgRepository.EpgProgram>> map=(Map<String,List<EpgRepository.EpgProgram>>)f.get(a);boolean dazn=false,liga=false;
     for(List<EpgRepository.EpgProgram> rows:map.values())for(EpgRepository.EpgProgram p:rows){
      if(p.tvgId.equals("DAZNLI")){assertTrue(U7dChannelIdentity.matches(p.channelName,"DAZN LaLiga"));dazn=true;}
      if(p.tvgId.equals("MLIGA")){assertTrue(U7dChannelIdentity.matches(p.channelName,"M+ LaLiga"));liga=true;}
     }
     java.util.Set<String> codes=new java.util.TreeSet<>();for(List<EpgRepository.EpgProgram> rows:map.values())for(EpgRepository.EpgProgram p:rows)codes.add(p.tvgId);
     java.lang.reflect.Field loading=MainActivity.class.getDeclaredField("archiveLoading");loading.setAccessible(true);
     java.lang.reflect.Field errors=MainActivity.class.getDeclaredField("archiveErrors");errors.setAccessible(true);
     info.set("codes="+codes+" loading="+loading.get(a)+" errors="+errors.get(a));
     ready.set(dazn&&liga);
    }catch(Exception e){throw new AssertionError(e);}});
   }
   assertTrue("The visible catchup catalog must receive both official channel identities: "+info.get(),ready.get());
  }
 }
}
