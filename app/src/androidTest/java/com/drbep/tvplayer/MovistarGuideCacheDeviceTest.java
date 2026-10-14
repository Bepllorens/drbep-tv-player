package com.drbep.tvplayer;

import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import okhttp3.*;
import org.json.*;

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class MovistarGuideCacheDeviceTest {
    private final String day=LocalDate.now(ZoneId.of("Europe/Madrid")).toString();
    private ChannelItem channel(String id,String code){return new ChannelItem(id,code,code,"","","","",0,0,false,false,0,"",new ArrayList<>(),"","","",false);}
    private File directory(){File f=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"guide-test-"+UUID.randomUUID());assertTrue(f.mkdirs());return f;}
    private void clean(File f){File[] children=f.listFiles();if(children!=null)for(File c:children)clean(c);f.delete();}
    private String fixture(String codes)throws Exception{
        JSONArray rows=new JSONArray();long time=LocalDate.parse(day).atTime(12,0).atZone(ZoneId.of("Europe/Madrid")).toInstant().toEpochMilli();
        for(String code:codes.split(","))rows.put(new JSONObject().put("Canal",new JSONObject().put("CodCadenaTv",code).put("Nombre",code)).put("FechaHoraInicio",time).put("FechaHoraFin",time+3600000).put("Titulo","Programa "+code));
        return rows.toString();
    }
    private OkHttpClient client(List<String> calls,AtomicBoolean offline,AtomicBoolean cachePublished){return new OkHttpClient.Builder().addInterceptor(chain->{
        String codes=chain.request().url().queryParameter("channel");calls.add(codes);
        if(offline.get()){if(cachePublished!=null)assertTrue("La caché debe publicarse antes de esperar la red",cachePublished.get());throw new IOException("offline fixture");}
        try{return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(codes.equals("ALIAS")?404:200).message("fixture").body(ResponseBody.create(MediaType.parse("application/json"),fixture(codes.isEmpty()?"TVE":codes))).build();}catch(Exception e){throw new IOException(e);}
    }).build();}
    @Test public void batchesAndPlatformCopiesReuseDiskShards()throws Exception{
        File root=directory();List<String> calls=new ArrayList<>();AtomicBoolean offline=new AtomicBoolean();
        try{
            try(MovistarGuideRepository repo=new MovistarGuideRepository(root,client(calls,offline,null))){repo.day(day,Arrays.asList(channel("ism","TVE"),channel("vamos","VAMOS")));}
            assertEquals(1,calls.size());offline.set(true);
            try(MovistarGuideRepository repo=new MovistarGuideRepository(root,client(calls,offline,null))){MovistarGuideRepository.Guide guide=repo.day(day,Collections.singletonList(channel("hls","TVE")));assertEquals(1,guide.rows.length());assertEquals("TVE",guide.identity(channel("hls","TVE")).getString("CodCadenaTv"));}
            assertEquals("No debe repetir la descarga al cambiar de lista o lote",1,calls.size());
        }finally{clean(root);}
    }
    @Test public void expiredDiskCacheAppearsBeforeFailedRefresh()throws Exception{
        File root=directory();List<String> calls=new ArrayList<>();AtomicBoolean offline=new AtomicBoolean(),published=new AtomicBoolean();
        try{
            try(MovistarGuideRepository repo=new MovistarGuideRepository(root,client(calls,offline,null))){repo.day(day,Collections.singletonList(channel("a","TVE")));}
            for(File file:new File(root,"movistar-guide-v1").listFiles())assertTrue(file.setLastModified(System.currentTimeMillis()-3600000));offline.set(true);
            try(MovistarGuideRepository repo=new MovistarGuideRepository(root,client(calls,offline,published))){MovistarGuideRepository.Guide guide=repo.day(day,Collections.singletonList(channel("a","TVE")),g->published.set(g.rows.length()>0));assertTrue(published.get());assertFalse(guide.complete);assertEquals(1,guide.rows.length());}
        }finally{clean(root);}
    }
    @Test public void changedOfficialAssociationDoesNotReuseOldChannelEvents()throws Exception{
        File root=directory();List<String> calls=new ArrayList<>();AtomicBoolean offline=new AtomicBoolean();
        try(MovistarGuideRepository repo=new MovistarGuideRepository(root,client(calls,offline,null))){
            repo.day(day,Collections.singletonList(channel("same-local-id","TVE")));
            MovistarGuideRepository.Guide changed=repo.day(day,Collections.singletonList(channel("same-local-id","VAMOS")));
            assertEquals("VAMOS",changed.rows.getJSONObject(0).getJSONObject("Canal").getString("CodCadenaTv"));assertEquals(Arrays.asList("TVE","VAMOS"),calls);
        }finally{clean(root);}
    }
    @Test public void rejectedAliasFallsBackToIdentityIndex()throws Exception{
        File root=directory();List<String> calls=new ArrayList<>();AtomicBoolean offline=new AtomicBoolean();
        ChannelItem alias=new ChannelItem("a","TVE","ALIAS","","","","",0,0,false,false,0,"",new ArrayList<>(),"","","",false);
        try(MovistarGuideRepository repo=new MovistarGuideRepository(root,client(calls,offline,null))){assertEquals("TVE",repo.day(day,Collections.singletonList(alias)).identity(alias).getString("CodCadenaTv"));assertEquals(Arrays.asList("ALIAS",""),calls);}finally{clean(root);}
    }
    @Test public void benchmarkRealChannelColdAndWarm()throws Exception{
        File root=directory();ChannelItem channel=channel("measurement","TVE");long start=System.nanoTime();
        try{
            int count;long cold,warm;
            try(MovistarGuideRepository repo=new MovistarGuideRepository(root)){MovistarGuideRepository.Guide g=repo.day(day,Collections.singletonList(channel));count=g.rows.length();assertTrue(count>0);cold=(System.nanoTime()-start)/1000000;}
            start=System.nanoTime();
            try(MovistarGuideRepository repo=new MovistarGuideRepository(root)){assertEquals(count,repo.day(day,Collections.singletonList(channel)).rows.length());warm=(System.nanoTime()-start)/1000000;}
            android.os.Bundle report=new android.os.Bundle();report.putString("stream","\nCatchup Fire: canal=TVE eventos="+count+" cold_ms="+cold+" disk_ms="+warm+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,report);
        }finally{clean(root);}
    }
}
