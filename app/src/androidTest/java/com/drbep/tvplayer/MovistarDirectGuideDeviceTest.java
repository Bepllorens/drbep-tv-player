package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import androidx.test.platform.app.InstrumentationRegistry;
import android.content.Context;
import org.json.*;
import java.util.*;
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class MovistarDirectGuideDeviceTest {
 private ChannelItem channel(String id,String name){return new ChannelItem(id,name,"","","","","",0,0,false,false,0,"",new ArrayList<>(),"","","",false);}
 @Test public void nestedGuideKeepsImagesAttachedToTheirChannel()throws Exception{
  Context test=InstrumentationRegistry.getInstrumentation().getContext();
  JSONArray rows=MovistarGuideRepository.parse(new java.io.InputStreamReader(test.getAssets().open("movistar-guide-liga-fixture.json"),"UTF-8"),"2026-10-10");
  MovistarGuideRepository.Guide guide=new MovistarGuideRepository.Guide(rows);
  ChannelItem liga=channel("liga","M+ LaLiga HD"),dazn=channel("dazn","DAZN LaLiga");
  assertEquals("MLIGA",guide.identity(liga).getString("CodCadenaTv"));assertEquals("DAZNLI",guide.identity(dazn).getString("CodCadenaTv"));
  List<EpgRepository.EpgProgram> a=guide.programmes(liga,guide.identity(liga)),b=guide.programmes(dazn,guide.identity(dazn));assertFalse(a.isEmpty());assertFalse(b.isEmpty());
  for(EpgRepository.EpgProgram p:a){assertEquals("MLIGA",p.tvgId);assertEquals(expectedImage(rows,"MLIGA",p.startTime),p.icon);}
  for(EpgRepository.EpgProgram p:b){assertEquals("DAZNLI",p.tvgId);assertEquals(expectedImage(rows,"DAZNLI",p.startTime),p.icon);}
 }
 private String expectedImage(JSONArray rows,String code,String start)throws Exception{
  long time=java.time.Instant.parse(start).toEpochMilli();
  for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);if(code.equals(r.getJSONObject("Canal").getString("CodCadenaTv"))&&r.getLong("FechaHoraInicio")==time)return r.getString("horizontal");}
  throw new AssertionError("Missing fixture emission");
 }
 @Test public void liveOfficialGuideAndDiskCacheWorkOnFire()throws Exception{
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();MovistarGuideRepository repo=new MovistarGuideRepository(context.getCacheDir());
  String day=java.time.LocalDate.now(java.time.ZoneId.of("Europe/Madrid")).toString();
  ChannelItem liga=channel("liga","M+ LaLiga"),dazn=channel("dazn","DAZN LaLiga");List<ChannelItem> channels=Arrays.asList(liga,dazn);
  try{
   MovistarGuideRepository.Guide guide=repo.day(day,channels);
   assertEquals("MLIGA",guide.identity(liga).getString("CodCadenaTv"));assertEquals("DAZNLI",guide.identity(dazn).getString("CodCadenaTv"));
   List<EpgRepository.EpgProgram> a=guide.programmes(liga,guide.identity(liga)),b=guide.programmes(dazn,guide.identity(dazn));assertFalse(a.isEmpty());assertFalse(b.isEmpty());
   int images=0;for(EpgRepository.EpgProgram p:b)if(MovistarGuideRepository.officialImage(p.icon))images++;assertTrue(images>0);
   MovistarGuideRepository.Guide cached=repo.day(day,channels);assertEquals(guide.rows.length(),cached.rows.length());
   android.os.Bundle report=new android.os.Bundle();report.putString("stream","\nGuía real: M+ LaLiga="+a.size()+", DAZN="+b.size()+", imágenes DAZN="+images+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,report);
  }finally{repo.close();}
 }
}
