package com.drbep.tvplayer;

import android.util.JsonReader;
import android.util.JsonToken;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.json.*;
import okhttp3.*;

/** Public programme metadata. Channel identity and artwork come from the same guide row. */
final class MovistarGuideRepository implements AutoCloseable {
    private final File directory;
    private final java.util.concurrent.ExecutorService downloads=java.util.concurrent.Executors.newFixedThreadPool(4);
    private final OkHttpClient client=new OkHttpClient.Builder().connectTimeout(8,TimeUnit.SECONDS).readTimeout(35,TimeUnit.SECONDS).callTimeout(45,TimeUnit.SECONDS).build();
    private final Map<String,Object> locks=new java.util.concurrent.ConcurrentHashMap<>();
    private volatile Guide channelIndex;
    private volatile long channelIndexTime;
    private final Map<String,Guide> memory=new LinkedHashMap<String,Guide>(8,.75f,true){protected boolean removeEldestEntry(Map.Entry<String,Guide> e){return size()>16;}};
    private final Map<String,Long> memoryTime=new HashMap<>();
    static final class Guide {
        final JSONArray rows;
        boolean complete=true;
        final Map<String,JSONObject> channels=new LinkedHashMap<>();
        final Map<String,List<JSONObject>> programmesByCode=new HashMap<>();
        final Map<String,Set<String>> services=new HashMap<>(),names=new HashMap<>();
        Guide(JSONArray rows)throws JSONException{
            this.rows=rows;
            for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i),c=row.getJSONObject("Canal");String code=c.getString("CodCadenaTv");channels.put(code,c);programmesByCode.computeIfAbsent(code,k->new ArrayList<>()).add(row);}
            for(JSONObject c:channels.values()){
                String code=c.getString("CodCadenaTv");
                index(names,U7dChannelIdentity.normalize(c.optString("Nombre")),code);
                for(String field:Arrays.asList("ServiceUid2","ServiceUid","CasId"))index(services,c.optString(field),code);
            }
        }
        private static void index(Map<String,Set<String>> index,String key,String code){if(!key.isEmpty())index.computeIfAbsent(key,k->new HashSet<>()).add(code);}
        JSONObject identity(ChannelItem local){
            Set<String> found=new HashSet<>();
            for(String value:Arrays.asList(ArchiveAvailability.code(local.playUrl),ArchiveAvailability.code(local.fallbackPlayUrl)))found.addAll(services.getOrDefault(value,Collections.emptySet()));
            if(!found.isEmpty())return found.size()==1?channels.get(found.iterator().next()):null;
            for(String code:channels.keySet())if(code.equalsIgnoreCase(local.tvgId))return channels.get(code);
            found=names.getOrDefault(U7dChannelIdentity.normalize(local.name),Collections.emptySet());
            return found.size()==1?channels.get(found.iterator().next()):null;
        }
        List<EpgRepository.EpgProgram> programmes(ChannelItem local,JSONObject channel)throws JSONException {
            List<EpgRepository.EpgProgram> out=new ArrayList<>();if(channel==null)return out;
            String code=channel.getString("CodCadenaTv");
            for(JSONObject p:programmesByCode.getOrDefault(code,Collections.emptyList())){
                long start=p.optLong("FechaHoraInicio"),end=p.optLong("FechaHoraFin");if(start<=0||end<=start)continue;
                String title=p.optString("Titulo"),episode=p.optString("TituloEpisodio").replaceAll("^\"|\"$","");
                if(!episode.isEmpty()&&!title.contains(episode))title+=" - "+episode;
                out.add(new EpgRepository.EpgProgram(local.id,channel.optString("Nombre"),code,title,p.optString("horizontal"),p.optString("Descripcion"),Instant.ofEpochMilli(start).toString(),Instant.ofEpochMilli(end).toString(),p.optString("GeneroComAntena"),0));
            }
            return out;
        }
    }
    MovistarGuideRepository(File cache){directory=new File(cache,"movistar-guide-v1");directory.mkdirs();}
    static JSONObject resolve(Collection<JSONObject> channels,String name,String tvg,String... services){
        // Exact provider code or stream service identity has priority. Never use substring names.
        for(int mode=0;mode<3;mode++){
            JSONObject match=null;
            for(JSONObject c:channels){boolean hit=false;
                if(mode==0)for(String service:services)if(service!=null&&!service.isEmpty()&&(service.equals(c.optString("ServiceUid2"))||service.equals(c.optString("ServiceUid"))||service.equals(c.optString("CasId"))))hit=true;
                if(mode==1)hit=tvg!=null&&!tvg.isEmpty()&&tvg.equalsIgnoreCase(c.optString("CodCadenaTv"));
                if(mode==2)hit=U7dChannelIdentity.matches(name,c.optString("Nombre"));
                if(hit){if(match!=null&&!match.optString("CodCadenaTv").equals(c.optString("CodCadenaTv")))return null;match=c;}
            }
            if(match!=null)return match;
        }
        return null;
    }
    Guide day(String day,List<ChannelItem> locals)throws Exception {return day(day,locals,null);}
    Guide day(String day,List<ChannelItem> locals,java.util.function.Consumer<Guide> partial)throws Exception {
        Guide index;
        synchronized(locks.computeIfAbsent("channels",k->new Object())){
            File file=new File(directory,"channels.json");
            if(channelIndex!=null&&System.currentTimeMillis()-channelIndexTime<86400000L)index=channelIndex;
            else if(file.isFile()&&System.currentTimeMillis()-file.lastModified()<86400000L){
                try{index=read(file);}catch(Exception broken){index=day(LocalDate.now(ZoneId.of("Europe/Madrid")).toString());}
            }else{
                index=day(LocalDate.now(ZoneId.of("Europe/Madrid")).toString());
                JSONArray compact=new JSONArray();for(JSONObject c:index.channels.values())compact.put(new JSONObject().put("Canal",c));
                try(Writer out=new OutputStreamWriter(new FileOutputStream(file),java.nio.charset.StandardCharsets.UTF_8)){out.write(compact.toString());}
            }
        }
        if(channelIndex!=index){channelIndex=index;channelIndexTime=System.currentTimeMillis();}
        LinkedHashSet<String> codes=new LinkedHashSet<>();for(ChannelItem local:locals){JSONObject c=index.identity(local);if(c!=null)codes.add(c.getString("CodCadenaTv"));}
        if(codes.isEmpty())throw new IOException("No hay identidades de canal verificadas");
        if(codes.size()<=10){Guide result=day(day,String.join(",",codes));if(partial!=null)partial.accept(result);return result;}
        android.util.Log.i("DirectGuide", "resolved channels="+codes.size());
        List<String> ordered=new ArrayList<>(codes);
        java.util.concurrent.CompletionService<Guide> completed=new java.util.concurrent.ExecutorCompletionService<>(downloads);
        List<java.util.concurrent.Future<Guide>> pending=new ArrayList<>();
        for(int i=0;i<ordered.size();i+=10){List<String> batch=new ArrayList<>(ordered.subList(i,Math.min(i+10,ordered.size())));Collections.sort(batch);String group=String.join(",",batch);pending.add(completed.submit(()->day(day,group)));}
        JSONArray merged=new JSONArray();Exception failure=null;int succeeded=0;
        try{for(int n=0;n<pending.size();n++){
            Guide part;
            try{part=completed.take().get();}catch(java.util.concurrent.ExecutionException failed){failure=failed;continue;}
            succeeded++;if(partial!=null)partial.accept(part);
            for(int i=0;i<part.rows.length();i++)merged.put(part.rows.getJSONObject(i));
        }}catch(Exception failed){for(java.util.concurrent.Future<Guide> task:pending)task.cancel(true);throw failed;}
        if(succeeded==0&&failure!=null)throw failure;
        Guide result=new Guide(merged);result.complete=failure==null;return result;
    }
    Guide day(String day)throws Exception {return day(day,"");}
    private Guide day(String day,String codes)throws Exception {
        LocalDate requested=LocalDate.parse(day),today=LocalDate.now(ZoneId.of("Europe/Madrid"));
        if(requested.isAfter(today)||requested.isBefore(today.minusDays(7)))throw new IOException("Día fuera del archivo");
        String cacheKey=day+"-"+java.util.UUID.nameUUIDFromBytes(codes.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        synchronized(locks.computeIfAbsent(cacheKey,k->new Object())){
            File file=new File(directory,cacheKey+".json");long ttl=requested.equals(today)?300000:6*3600000L;
            synchronized(memory){Guide hit=memory.get(cacheKey);if(hit!=null&&System.currentTimeMillis()-memoryTime.getOrDefault(cacheKey,0L)<ttl)return hit;}
            if(file.isFile()&&System.currentTimeMillis()-file.lastModified()<ttl){try{Guide hit=read(file);remember(cacheKey,hit,file.lastModified());return hit;}catch(Exception ignored){}}
            HttpUrl url=HttpUrl.parse("https://ottcache.dof6.com/movistarplus/webplayer/OTT/epg").newBuilder().addQueryParameter("from",day+(codes.isEmpty()?"T06:00:00":"T00:00:00")).addQueryParameter("span","1").addQueryParameter("channel",codes).addQueryParameter("network","movistarplus").addQueryParameter("version","8.2").addQueryParameter("mdrm","true").addQueryParameter("tlsstream","true").addQueryParameter("demarcation","1").build();
            JSONArray rows;
            android.util.Log.i("DirectGuide","request day="+day+" channels="+(codes.isEmpty()?0:codes.split(",").length));
            try(Response response=client.newCall(new Request.Builder().url(url).header("User-Agent","Mozilla/5.0 (Android TV) DRBEP/706").build()).execute()){
                if(!response.isSuccessful()||response.body()==null)throw new IOException("Guía Movistar no disponible");
                rows=parse(response.body().charStream(),day);
            }
            android.util.Log.i("DirectGuide","received day="+day+" rows="+rows.length());
            if(rows.length()==0 && codes.isEmpty())throw new IOException("Guía vacía");
            Guide guide=new Guide(rows);File next=new File(file+".next");
            try(Writer out=new OutputStreamWriter(new FileOutputStream(next),java.nio.charset.StandardCharsets.UTF_8)){out.write(rows.toString());}
            if(!next.renameTo(file))next.delete();remember(cacheKey,guide,System.currentTimeMillis());prune(today);return guide;
        }
    }
    private void remember(String key,Guide guide,long time){synchronized(memory){memory.put(key,guide);memoryTime.put(key,time);memoryTime.keySet().retainAll(memory.keySet());}}
    private Guide read(File file)throws Exception{try(Reader in=new InputStreamReader(new FileInputStream(file),java.nio.charset.StandardCharsets.UTF_8)){StringBuilder out=new StringBuilder();char[] buffer=new char[8192];int n;while((n=in.read(buffer))!=-1){if(out.length()+n>8*1024*1024)throw new IOException("Caché demasiado grande");out.append(buffer,0,n);}return new Guide(new JSONArray(out.toString()));}}
    static JSONArray parse(Reader input,String day)throws Exception {
        JSONArray rows=new JSONArray();int count=0;
        try(JsonReader in=new JsonReader(input)){
            in.beginArray();boolean group=false;
            while(in.hasNext()||group){
                if(group&&!in.hasNext()){in.endArray();group=false;continue;}
                if(in.peek()==JsonToken.BEGIN_ARRAY){if(group)throw new IOException("Estructura de guía no válida");in.beginArray();group=true;continue;}
                if(++count>30000)throw new IOException("Guía demasiado grande");
                JSONObject p=new JSONObject();in.beginObject();
                while(in.hasNext()){
                    String key=in.nextName();
                    if(key.equals("Canal")&&in.peek()==JsonToken.BEGIN_OBJECT){JSONObject c=new JSONObject();in.beginObject();while(in.hasNext()){String k=in.nextName();if(Arrays.asList("CodCadenaTv","Nombre","ServiceUid","ServiceUid2","CasId").contains(k))c.put(k,scalar(in));else in.skipValue();}in.endObject();p.put(key,c);}
                    else if(key.equals("Imagenes")&&in.peek()==JsonToken.BEGIN_ARRAY){in.beginArray();while(in.hasNext()){String id="",uri="";in.beginObject();while(in.hasNext()){String k=in.nextName();if(k.equals("id"))id=scalar(in);else if(k.equals("uri"))uri=scalar(in);else in.skipValue();}in.endObject();if(id.equals("horizontal")&&officialImage(uri))p.put("horizontal",uri);}in.endArray();}
                    else if(Arrays.asList("FechaHoraInicio","FechaHoraFin","Titulo","TituloEpisodio","GeneroComAntena","Descripcion","Sinopsis").contains(key)){String value=scalar(in);p.put(key.equals("Sinopsis")?"Descripcion":key,value);}
                    else in.skipValue();
                }
                in.endObject();long start=p.optLong("FechaHoraInicio");
                if(start>0&&Instant.ofEpochMilli(start).atZone(ZoneId.of("Europe/Madrid")).toLocalDate().toString().equals(day)&&p.optJSONObject("Canal")!=null)rows.put(p);
            }in.endArray();
        }return rows;
    }
    private static String scalar(JsonReader in)throws IOException{if(in.peek()==JsonToken.NULL){in.nextNull();return "";}if(in.peek()==JsonToken.STRING||in.peek()==JsonToken.NUMBER)return in.nextString();in.skipValue();return "";}
    static boolean officialImage(String url){HttpUrl u=HttpUrl.parse(url);return u!=null&&u.isHttps()&&u.host().equals("estatico.emisiondof6.com")&&u.encodedPath().contains("/ywcatalogoh/");}
    private void prune(LocalDate today){File[] files=directory.listFiles((d,n)->n.endsWith(".json"));if(files==null)return;Arrays.sort(files,Comparator.comparingLong(File::lastModified));long total=0;for(File f:files)total+=f.length();for(File f:files)if(!f.getName().equals("channels.json")&&(f.getName().substring(0,10).compareTo(today.minusDays(7).toString())<0||total>24L*1024*1024)){long size=f.length();if(f.delete())total-=size;}}
    public void close(){downloads.shutdownNow();client.dispatcher().cancelAll();}
}
