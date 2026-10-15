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
    private final OkHttpClient client;
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
                out.add(new EpgRepository.EpgProgram(local.id,channel.optString("Nombre"),code,title,p.optString("horizontal"),p.optString("Descripcion"),Instant.ofEpochMilli(start).toString(),Instant.ofEpochMilli(end).toString(),p.optString("GeneroComAntena"),0,ProgrammeMetadata.official(p),p.optString("Ficha")));
            }
            return out;
        }
    }
    MovistarGuideRepository(File cache){this(cache,new OkHttpClient.Builder().connectTimeout(8,TimeUnit.SECONDS).readTimeout(35,TimeUnit.SECONDS).callTimeout(45,TimeUnit.SECONDS).build());}
    MovistarGuideRepository(File cache,OkHttpClient client){this.client=client;directory=new File(cache,"movistar-guide-v1");directory.mkdirs();}
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
    Guide day(String day,List<ChannelItem> locals,java.util.function.Consumer<Guide> listener)throws Exception {
        final Object publicationLock=new Object();
        final java.util.function.Consumer<Guide> partial=listener==null?null:guide->{synchronized(publicationLock){listener.accept(guide);}};
        // A provider-looking code can be queried cheaply, but is accepted only when
        // the returned row confirms the exact identity. Unknown aliases retain fallback.
        LinkedHashSet<String> directCodes=new LinkedHashSet<>();
        boolean direct=!locals.isEmpty();
        for(ChannelItem local:locals){String code=local.tvgId==null?"":local.tvgId.trim();if(!code.matches("[A-Z][A-Z0-9_-]{1,15}")){direct=false;break;}directCodes.add(code);}
        if(direct && directCodes.size()<=10){
            try {
                Guide fast=day(day,String.join(",",directCodes),partial);
                boolean verified=true;for(ChannelItem local:locals)if(fast.identity(local)==null){verified=false;break;}
                if(verified){if(partial!=null)partial.accept(fast);return fast;}
            } catch (IOException unavailable) {
                // Alias codes may be rejected by the provider. Resolve through the
                // existing channel index before declaring the channel unavailable.
            }
        }
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
        if(codes.size()<=10){Guide result=day(day,String.join(",",codes),partial);if(partial!=null)partial.accept(result);return result;}
        android.util.Log.i("DirectGuide", "resolved channels="+codes.size());
        List<String> ordered=new ArrayList<>(codes);
        java.util.concurrent.CompletionService<Guide> completed=new java.util.concurrent.ExecutorCompletionService<>(downloads);
        List<java.util.concurrent.Future<Guide>> pending=new ArrayList<>();
        for(int i=0;i<ordered.size();i+=10){List<String> batch=new ArrayList<>(ordered.subList(i,Math.min(i+10,ordered.size())));Collections.sort(batch);String group=String.join(",",batch);pending.add(completed.submit(()->day(day,group,partial)));}
        JSONArray merged=new JSONArray();Exception failure=null;int succeeded=0;boolean allComplete=true;
        try{for(int n=0;n<pending.size();n++){
            Guide part;
            try{part=completed.take().get();}catch(java.util.concurrent.ExecutionException failed){failure=failed;continue;}
            succeeded++;allComplete &= part.complete;if(partial!=null)partial.accept(part);
            for(int i=0;i<part.rows.length();i++)merged.put(part.rows.getJSONObject(i));
        }}catch(Exception failed){for(java.util.concurrent.Future<Guide> task:pending)task.cancel(true);throw failed;}
        if(succeeded==0&&failure!=null)throw failure;
        Guide result=new Guide(merged);result.complete=failure==null && allComplete;return result;
    }
    Guide day(String day)throws Exception {return day(day,"");}
    private Guide day(String day,String codes)throws Exception {return day(day,codes,null);}
    private Guide day(String day,String codes,java.util.function.Consumer<Guide> partial)throws Exception {
        LocalDate requested=LocalDate.parse(day),today=LocalDate.now(ZoneId.of("Europe/Madrid"));
        if(requested.isAfter(today)||requested.isBefore(today.minusDays(7)))throw new IOException("Día fuera del archivo");
        List<String> requestedCodes=new ArrayList<>();for(String c:codes.split(","))if(!c.isEmpty()&&!requestedCodes.contains(c))requestedCodes.add(c);
        Collections.sort(requestedCodes);
        String canonical=String.join(",",requestedCodes);
        String cacheKey=cacheKey(day,canonical);
        long opened=System.nanoTime();
        synchronized(locks.computeIfAbsent(cacheKey,k->new Object())){
            long now=System.currentTimeMillis(),ttl=requested.equals(today)?300000:6*3600000L;
            JSONArray cachedRows=new JSONArray();List<String> missing=new ArrayList<>();
            List<String> shards=requestedCodes.isEmpty()?Collections.singletonList(""):requestedCodes;
            for(String code:shards){
                String key=cacheKey(day,code);Guide hit=null;long stamp=0;
                synchronized(memory){hit=memory.get(key);stamp=memoryTime.getOrDefault(key,0L);}
                File file=new File(directory,key+".json");
                if(hit==null&&file.isFile())try{hit=read(file);stamp=file.lastModified();remember(key,hit,stamp);}catch(Exception ignored){}
                if(hit!=null)for(int i=0;i<hit.rows.length();i++)cachedRows.put(hit.rows.getJSONObject(i));
                if(hit==null||now-stamp>=ttl)missing.add(code);
            }
            Guide cached=new Guide(cachedRows);
            if(cachedRows.length()>0 && partial!=null){
                cached.complete=missing.isEmpty();partial.accept(cached);
                android.util.Log.i("DirectGuide","timing source=cache_first channels="+requestedCodes.size()+" rows="+cachedRows.length()+" first_ms="+((System.nanoTime()-opened)/1000000));
            }
            if(missing.isEmpty()){
                android.util.Log.i("DirectGuide","timing source=cache channels="+requestedCodes.size()+" rows="+cachedRows.length()+" total_ms="+((System.nanoTime()-opened)/1000000));
                return cached;
            }
            String networkCodes=String.join(",",missing);
            HttpUrl url=HttpUrl.parse("https://ottcache.dof6.com/movistarplus/webplayer/OTT/epg").newBuilder().addQueryParameter("from",day+(networkCodes.isEmpty()?"T06:00:00":"T00:00:00")).addQueryParameter("span","1").addQueryParameter("channel",networkCodes).addQueryParameter("network","movistarplus").addQueryParameter("version","8.2").addQueryParameter("mdrm","true").addQueryParameter("tlsstream","true").addQueryParameter("demarcation","1").build();
            long networkStart=System.nanoTime(),headersAt=networkStart;JSONArray rows;
            try(Response response=client.newCall(new Request.Builder().url(url).header("User-Agent","Mozilla/5.0 (Android TV) DRBEP/725").build()).execute()){
                headersAt=System.nanoTime();
                if(!response.isSuccessful()||response.body()==null)throw new IOException("Guía Movistar no disponible");
                rows=parse(response.body().charStream(),day);
            }catch(Exception failure){if(cachedRows.length()>0){cached.complete=false;return cached;}throw failure;}
            long parsedAt=System.nanoTime();
            if(rows.length()==0 && networkCodes.isEmpty())throw new IOException("Guía vacía");
            Guide fresh=new Guide(rows);
            // Store stable per-channel/day shards: single-channel, batch, ISM and HLS
            // requests now reuse the same public metadata without sharing playback state.
            if(requestedCodes.isEmpty())save(cacheKey(day,""),fresh);
            for(String code:fresh.channels.keySet()){
                JSONArray channelRows=new JSONArray();for(JSONObject row:fresh.programmesByCode.getOrDefault(code,Collections.emptyList()))channelRows.put(row);
                save(cacheKey(day,code),new Guide(channelRows));
            }
            JSONArray merged=new JSONArray();
            for(int i=0;i<cachedRows.length();i++){JSONObject row=cachedRows.getJSONObject(i);if(!fresh.channels.containsKey(row.getJSONObject("Canal").getString("CodCadenaTv")))merged.put(row);}
            for(int i=0;i<rows.length();i++)merged.put(rows.getJSONObject(i));
            Guide result=new Guide(merged);prune(today);
            android.util.Log.i("DirectGuide","timing source=network channels="+missing.size()+" rows="+rows.length()+" headers_ms="+((headersAt-networkStart)/1000000)+" read_parse_ms="+((parsedAt-headersAt)/1000000)+" total_ms="+((System.nanoTime()-opened)/1000000));
            return result;
        }
    }
    static String cacheKey(String day,String codes){List<String> sorted=new ArrayList<>(new TreeSet<>(Arrays.asList(codes.split(","))));return day+"-"+java.util.UUID.nameUUIDFromBytes(String.join(",",sorted).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private void save(String key,Guide guide)throws Exception {
        File file=new File(directory,key+".json");
        File next=File.createTempFile("guide-", ".next",directory);
        try{
            try(Writer out=new OutputStreamWriter(new FileOutputStream(next),java.nio.charset.StandardCharsets.UTF_8)){out.write(guide.rows.toString());}
            if(!next.renameTo(file))throw new IOException("No se pudo guardar la guía");
            remember(key,guide,System.currentTimeMillis());
        }finally{next.delete();}
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
                    else if(Arrays.asList("FechaHoraInicio","FechaHoraFin","Titulo","TituloEpisodio","GeneroComAntena","Descripcion","Sinopsis","Anno","Ficha").contains(key)){String value=scalar(in);p.put(key.equals("Sinopsis")?"Descripcion":key,value);}
                    else if(key.equals("NivelMoral")&&in.peek()==JsonToken.BEGIN_OBJECT){JSONObject age=new JSONObject();in.beginObject();while(in.hasNext()){String k=in.nextName();if(k.equals("Id"))age.put(k,scalar(in));else in.skipValue();}in.endObject();p.put(key,age);}
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
