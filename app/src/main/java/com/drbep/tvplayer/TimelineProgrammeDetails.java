package com.drbep.tvplayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.*;

/** Public guide descriptions only. Never follows playback or licence links. */
final class TimelineProgrammeDetails {
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(7, TimeUnit.SECONDS).followRedirects(false).build();
    private static final Map<String, CachedDescription> CACHE = new LinkedHashMap<String, CachedDescription>(32, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, CachedDescription> e) { return size() > 64; }
    };
    private static final Map<String, GuideEntry> GUIDES = new LinkedHashMap<String, GuideEntry>(8, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, GuideEntry> e) { return size() > 8; }
    };
    private static final class GuideEntry {
        final JSONArray rows; final long time=System.currentTimeMillis();
        GuideEntry(JSONArray rows) { this.rows=rows; }
    }
    private static final class CachedDescription {
        final ProgrammeMetadata metadata; final long expires;
        CachedDescription(ProgrammeMetadata metadata, boolean success) { this.metadata=metadata; expires=System.currentTimeMillis()+(success?86400000:60000); }
    }
    private static volatile java.io.File disk;
    static void initialize(java.io.File cache) { if(disk==null)disk=new java.io.File(cache,"programme-details-v1"); }
    static String key(String code,String start) { return code+"|"+EpgTimeCodec.parseEpochMillis(start); }
    static ProgrammeMetadata peek(String key) {
        synchronized(CACHE) { CachedDescription hit=CACHE.get(key); return hit!=null&&hit.expires>System.currentTimeMillis()?hit.metadata:null; }
    }
    static String load(String code,String startTime)throws Exception { return loadMetadata(code,startTime,"").description; }
    // One metadata request at a time; called exclusively on Dispatchers.IO after focus settles.
    static synchronized ProgrammeMetadata loadMetadata(String code, String startTime, String suppliedFicha) throws Exception {
        if(Thread.currentThread().isInterrupted())throw new InterruptedException();
        long opened=System.nanoTime();
        if (code == null || !code.matches("[A-Za-z0-9_-]{1,32}")) return ProgrammeMetadata.EMPTY;
        long start=EpgTimeCodec.parseEpochMillis(startTime);
        if(start<=0)return ProgrammeMetadata.EMPTY;
        String key=key(code,startTime);
        ProgrammeMetadata hit=peek(key);if(hit!=null)return hit;
        java.io.File file=disk==null?null:new java.io.File(disk,java.util.UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8))+".json");
        if(file!=null&&file.isFile()&&System.currentTimeMillis()-file.lastModified()<86400000L&&file.length()<65536) {
            try {
                JSONObject saved=new JSONObject(new String(java.nio.file.Files.readAllBytes(file.toPath()),java.nio.charset.StandardCharsets.UTF_8));
                ProgrammeMetadata result=new ProgrammeMetadata(saved.optString("genre"),saved.optString("year"),saved.optString("age"),saved.optString("rating"),saved.optString("description"));
                synchronized(CACHE){CACHE.put(key,new CachedDescription(result,true));}return result;
            }catch(Exception ignored){}
        }
        try {
            String ficha=suppliedFicha;
            if(!allowedFicha(ficha)) {
                String day=java.time.Instant.ofEpochMilli(start).atZone(java.time.ZoneId.of("Europe/Madrid")).toLocalDate().toString();
                HttpUrl url=HttpUrl.parse("https://ottcache.dof6.com/movistarplus/webplayer/OTT/epg").newBuilder()
                    .addQueryParameter("from",day+"T00:00:00").addQueryParameter("span","1")
                    .addQueryParameter("channel",code).addQueryParameter("network","movistarplus")
                    .addQueryParameter("version","8.2").addQueryParameter("mdrm","true")
                    .addQueryParameter("tlsstream","true").addQueryParameter("demarcation","1").build();
                String guideKey=code+"|"+day;
                JSONArray rows;
                synchronized(GUIDES) { GuideEntry g=GUIDES.get(guideKey); rows=g!=null&&System.currentTimeMillis()-g.time<15*60000L?g.rows:null; }
                if(rows==null){rows=new JSONArray(read(url));synchronized(GUIDES){GUIDES.put(guideKey,new GuideEntry(rows));}}
                ficha=findFicha(rows,code,start);
            }
            if(!allowedFicha(ficha)){synchronized(CACHE){CACHE.put(key,new CachedDescription(ProgrammeMetadata.EMPTY,false));}return ProgrammeMetadata.EMPTY;}
            if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            ProgrammeMetadata result=ProgrammeMetadata.official(new JSONObject(read(HttpUrl.parse(ficha))));
            if("beta".equals(BuildConfig.UPDATE_CHANNEL))android.util.Log.i("ProgrammeMetadata","source=network elapsed_ms="+((System.nanoTime()-opened)/1000000));
            synchronized(CACHE){CACHE.put(key,new CachedDescription(result,true));}
            if(file!=null)try{
                disk.mkdirs();
                JSONObject saved=new JSONObject().put("genre",result.genre).put("year",result.year).put("age",result.age).put("rating",result.rating).put("description",result.description);
                java.io.File next=new java.io.File(file.getPath()+".next");
                java.nio.file.Files.write(next.toPath(),saved.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                if(!next.renameTo(file))next.delete();
                java.io.File[] entries=disk.listFiles((d,n)->n.endsWith(".json"));
                if(entries!=null&&entries.length>128){java.util.Arrays.sort(entries,java.util.Comparator.comparingLong(java.io.File::lastModified));for(int i=0;i<entries.length-128;i++)entries[i].delete();}
            }catch(Exception ignored){}
            return result;
        }catch(Exception failure){if(Thread.currentThread().isInterrupted())throw new InterruptedException();synchronized(CACHE){CACHE.put(key,new CachedDescription(ProgrammeMetadata.EMPTY,false));}return ProgrammeMetadata.EMPTY;}
    }
    static String findFicha(JSONArray rows,String code,long start) {
        for(int i=0;i<rows.length();i++) {
            JSONArray group=rows.optJSONArray(i);
            if(group!=null){String hit=findFicha(group,code,start);if(!hit.isEmpty())return hit;continue;}
            JSONObject p=rows.optJSONObject(i);if(p==null)continue;
            JSONObject channel=p.optJSONObject("Canal");
            if(channel!=null&&code.equals(channel.optString("CodCadenaTv"))&&p.optLong("FechaHoraInicio")==start)return p.optString("Ficha");
        }
        return "";
    }
    static boolean allowedFicha(String value) {
        if(value==null||value.isEmpty())return false;
        HttpUrl u=HttpUrl.parse(value);
        return u!=null&&u.isHttps()&&u.host().equals("ottcache.dof6.com")&&u.port()==443
            &&u.username().isEmpty()&&u.password().isEmpty()
            &&u.encodedPath().matches("/movistarplus/webplayer/contents/[0-9]+/details");
    }
    static String description(JSONObject p) {
        String text=p.optString("Descripcion","").trim();
        return text.isEmpty()?p.optString("Sinopsis","").trim():text;
    }
    private static String read(HttpUrl url)throws Exception {
        try(Response r=CLIENT.newCall(new Request.Builder().url(url).build()).execute()) {
            if(!r.isSuccessful()||r.body()==null)throw new java.io.IOException("Ficha no disponible");
            try(java.io.Reader in=r.body().charStream()) {
                StringBuilder out=new StringBuilder();char[] buffer=new char[4096];int n;
                while((n=in.read(buffer))!=-1){if(out.length()+n>2*1024*1024)throw new java.io.IOException("Ficha demasiado grande");out.append(buffer,0,n);}
                return out.toString();
            }
        }
    }
}
