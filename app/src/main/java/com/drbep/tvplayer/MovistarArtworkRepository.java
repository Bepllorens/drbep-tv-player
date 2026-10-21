package com.drbep.tvplayer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.json.*;
import okhttp3.*;

/** Public EPG artwork only: no stream URL, credentials, replay or licence requests. */
final class MovistarArtworkRepository implements AutoCloseable {
    private final File directory;
    private final ExecutorService workers=BuildConfig.BOUNDED_ARTWORK_REQUESTS?ArtworkWorkQueue.create(4,64):Executors.newFixedThreadPool(4);
    private final Object[] boundedLocks=ArtworkWorkQueue.locks(64);
    private final OkHttpClient client=new OkHttpClient.Builder().connectTimeout(5,TimeUnit.SECONDS).readTimeout(10,TimeUnit.SECONDS).callTimeout(15,TimeUnit.SECONDS).build();
    private final ArtworkMemoryCache<List<Row>> grids=new ArtworkMemoryCache<>(32);
    private final KeyedArtworkLocks locks=new KeyedArtworkLocks();
    private final ArtworkSingleFlight<String,List<Row>> grouped = new ArtworkSingleFlight<>(workers,256);
    private volatile boolean closed;
    static final class Row {final long start;final String channel,title,image,normalizedTitle;Row(long s,String c,String t,String i){start=s;channel=c;title=t;image=i;normalizedTitle=titleKey(t);}}
    MovistarArtworkRepository(File dir){directory=new File(dir,"movistar-artwork");directory.mkdirs();File[] files=directory.listFiles();if(files!=null)for(File f:files)if(System.currentTimeMillis()-f.lastModified()>9L*86400000)f.delete();}
    static String horizontal(JSONObject object){JSONArray images=object.optJSONArray("Imagenes");if(images!=null)for(int i=0;i<images.length();i++){JSONObject item=images.optJSONObject(i);if(item!=null&&"horizontal".equals(item.optString("id"))){String url=item.optString("uri");HttpUrl parsed=HttpUrl.parse(url);if(parsed!=null&&"https".equals(parsed.scheme()))return url;}}return "";}
    static String titleKey(String title){return FanartRepository.titleKey(title==null?"":title.replaceFirst("\\s*\\((?:19|20)\\d{2}\\)\\s*$",""));}
    private static String day(long start){return Instant.ofEpochMilli(start).atZone(ZoneId.of("Europe/Madrid")).toLocalDate().toString();}
    private static String key(String provider,long start){return provider+"_"+day(start);}
    String cached(String provider,long start,String title){List<Row> rows=grids.get(key(provider,start));return match(rows,provider,start,title);}
    static String match(List<Row> rows,String provider,long start,String title){if(rows==null)return "";String expected=titleKey(title),found="";for(Row row:rows){if(!provider.equalsIgnoreCase(row.channel)||Math.abs(row.start-start)>120000||!expected.equals(row.normalizedTitle))continue;if(!found.isEmpty()&&!found.equals(row.image))return "";found=row.image;}return found;}
    boolean request(String provider,long start,String title,Consumer<String> callback){
        if(closed||provider==null||!provider.matches("[A-Za-z0-9_-]{1,24}")||start<=0)return false;
        if(BuildConfig.COALESCE_ARTWORK_GRIDS) {
            String k=key(provider,start);
            // A callback retains its own programme identity even when the grid is shared.
            java.util.concurrent.atomic.AtomicBoolean delivered=new java.util.concurrent.atomic.AtomicBoolean();
            Consumer<List<Row>> result=rows->{
                if(delivered.compareAndSet(false,true)&&!closed)callback.accept(match(rows,provider,start,title));
            };
            boolean accepted=grouped.request(k,()->locks.run(k,()->loadCached(provider,start,k)),result);
            if(!accepted&&!closed) {
                BlackBox.event(BlackBoxStore.Event.ARTWORK_QUEUE_REJECT,0,0,0,0);
                result.accept(null);
            }
            return accepted;
        }
        boolean accepted=ArtworkWorkQueue.submit(workers,()->{
            String image="";
            try{
                String k=key(provider,start);
                List<Row> rows;
                if(BuildConfig.BOUNDED_ARTWORK_REQUESTS){
                    synchronized(ArtworkWorkQueue.lock(boundedLocks,k)){
                        rows=loadCached(provider,start,k);
                    }
                }else{
                    rows=locks.run(k,()->loadCached(provider,start,k));
                }
                image=match(rows,provider,start,title);
            }catch(Exception ignored){}
            if(!closed)callback.accept(image);
        });
        if(!accepted && !closed){
            BlackBox.event(BlackBoxStore.Event.ARTWORK_QUEUE_REJECT,0,0,0,0);
            callback.accept("");
        }
        return accepted;
    }
    // Caller owns the key lock; cache and trim keep their existing generation guard.
    private List<Row> loadCached(String provider,long start,String k)throws Exception{
        long generation=grids.generation();
        List<Row> rows=grids.get(k);
        if(rows==null){rows=load(provider,start);grids.putIfCurrent(k,rows,generation);}
        return rows;
    }
    private List<Row> load(String provider,long start)throws Exception{
        File cache=new File(directory,key(provider,start)+".json");ArtworkGridDiskCache.Payload payload=ArtworkGridDiskCache.readPayload(cache,System.currentTimeMillis(),true);
        if(payload==null){HttpUrl url=HttpUrl.parse("https://ottcache.dof6.com/movistarplus/webplayer/OTT/epg").newBuilder().addQueryParameter("from",day(start)+"T00:00:00").addQueryParameter("span","2").addQueryParameter("channel",provider).addQueryParameter("network","movistarplus").addQueryParameter("version","8.2").addQueryParameter("mdrm","true").addQueryParameter("tlsstream","true").addQueryParameter("demarcation","1").build();
            boolean downloaded=false;
            try(Response response=client.newCall(new Request.Builder().url(url).build()).execute()){
                if(!response.isSuccessful()||response.body()==null)throw new IOException("Artwork unavailable");
                try{payload=new ArtworkGridDiskCache.Payload(read(response.body().byteStream()));}
                catch(JSONException invalid){throw new IOException("Invalid artwork guide",invalid);}
                downloaded=true;
            }catch(IOException e){
                payload=ArtworkGridDiskCache.readPayload(cache,System.currentTimeMillis(),false);
                if(payload==null)throw e;
            }
            // Only a newly validated response renews the disk cache's timestamp.
            if(downloaded) {
                File next=new File(cache+".next");
                try {
                    try(OutputStream out=new FileOutputStream(next)){out.write(payload.text.getBytes(StandardCharsets.UTF_8));}
                    if(!next.renameTo(cache))next.delete();
                    prune();
                }catch(IOException unavailable){next.delete();} // Keep a valid network result even if disk is full.
            }
        }
        List<Row> rows=new ArrayList<>();JSONArray array=payload.programs;for(int i=0;i<array.length();i++){JSONObject p=array.optJSONObject(i);if(p==null)continue;JSONObject c=p.optJSONObject("Canal");String image=horizontal(p);if(c!=null&&!image.isEmpty())rows.add(new Row(p.optLong("FechaHoraInicio"),c.optString("CodCadenaTv"),p.optString("Titulo"),image));}return rows;
    }
    private void prune(){synchronized(directory){File[] files=directory.listFiles((d,n)->n.endsWith(".json"));if(files==null)return;Arrays.sort(files,Comparator.comparingLong(File::lastModified));long total=0;for(File f:files)total+=f.length();for(File f:files){if(total<=32L*1024*1024)break;long size=f.length();if(f.delete())total-=size;}}}
    private static String read(InputStream stream)throws IOException{try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>4*1024*1024)throw new IOException("Artwork grid too large");out.write(buffer,0,n);}return out.toString("UTF-8");}}
    void clearMemory(){grids.clear();}
    public void close(){closed=true;grouped.close();grids.clear();workers.shutdownNow();client.dispatcher().cancelAll();}
}
