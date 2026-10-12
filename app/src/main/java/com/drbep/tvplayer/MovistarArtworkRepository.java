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
    private final ExecutorService workers=Executors.newFixedThreadPool(4);
    private final OkHttpClient client=new OkHttpClient.Builder().connectTimeout(5,TimeUnit.SECONDS).readTimeout(10,TimeUnit.SECONDS).callTimeout(15,TimeUnit.SECONDS).build();
    private final Map<String,List<Row>> grids=Collections.synchronizedMap(new LinkedHashMap<String,List<Row>>(32,.75f,true){protected boolean removeEldestEntry(Map.Entry<String,List<Row>> e){return size()>32;}});
    private final Map<String,Object> locks=new ConcurrentHashMap<>();
    private volatile boolean closed;
    static final class Row {final long start;final String channel,title,image;Row(long s,String c,String t,String i){start=s;channel=c;title=t;image=i;}}
    MovistarArtworkRepository(File dir){directory=new File(dir,"movistar-artwork");directory.mkdirs();File[] files=directory.listFiles();if(files!=null)for(File f:files)if(System.currentTimeMillis()-f.lastModified()>9L*86400000)f.delete();}
    static String horizontal(JSONObject object){JSONArray images=object.optJSONArray("Imagenes");if(images!=null)for(int i=0;i<images.length();i++){JSONObject item=images.optJSONObject(i);if(item!=null&&"horizontal".equals(item.optString("id"))){String url=item.optString("uri");HttpUrl parsed=HttpUrl.parse(url);if(parsed!=null&&"https".equals(parsed.scheme()))return url;}}return "";}
    static String titleKey(String title){return FanartRepository.titleKey(title==null?"":title.replaceFirst("\\s*\\((?:19|20)\\d{2}\\)\\s*$",""));}
    private static String day(long start){return Instant.ofEpochMilli(start).atZone(ZoneId.of("Europe/Madrid")).toLocalDate().toString();}
    private static String key(String provider,long start){return provider+"_"+day(start);}
    String cached(String provider,long start,String title){List<Row> rows=grids.get(key(provider,start));return match(rows,provider,start,title);}
    static String match(List<Row> rows,String provider,long start,String title){if(rows==null)return "";String expected=titleKey(title),found="";for(Row row:rows){if(!provider.equalsIgnoreCase(row.channel)||Math.abs(row.start-start)>120000||!expected.equals(titleKey(row.title)))continue;if(!found.isEmpty()&&!found.equals(row.image))return "";found=row.image;}return found;}
    void request(String provider,long start,String title,Consumer<String> callback){if(closed||provider==null||!provider.matches("[A-Za-z0-9_-]{1,24}")||start<=0)return;workers.execute(()->{String image="";try{String k=key(provider,start);synchronized(locks.computeIfAbsent(k,x->new Object())){List<Row> rows=grids.get(k);if(rows==null){rows=load(provider,start);grids.put(k,rows);}image=match(rows,provider,start,title);}}catch(Exception ignored){}if(!closed)callback.accept(image);});}
    private List<Row> load(String provider,long start)throws Exception{
        File cache=new File(directory,key(provider,start)+".json");String payload=null;
        if(cache.isFile()&&System.currentTimeMillis()-cache.lastModified()<4*3600000L)payload=read(new FileInputStream(cache));
        if(payload==null){HttpUrl url=HttpUrl.parse("https://ottcache.dof6.com/movistarplus/webplayer/OTT/epg").newBuilder().addQueryParameter("from",day(start)+"T00:00:00").addQueryParameter("span","2").addQueryParameter("channel",provider).addQueryParameter("network","movistarplus").addQueryParameter("version","8.2").addQueryParameter("mdrm","true").addQueryParameter("tlsstream","true").addQueryParameter("demarcation","1").build();
            try(Response response=client.newCall(new Request.Builder().url(url).build()).execute()){if(!response.isSuccessful()||response.body()==null)throw new IOException("Artwork unavailable");payload=read(response.body().byteStream());}catch(IOException e){if(!cache.isFile())throw e;payload=read(new FileInputStream(cache));}
            new JSONArray(payload);File next=new File(cache+".next");try(OutputStream out=new FileOutputStream(next)){out.write(payload.getBytes(StandardCharsets.UTF_8));}if(!next.renameTo(cache))next.delete();prune();
        }
        List<Row> rows=new ArrayList<>();JSONArray array=new JSONArray(payload);for(int i=0;i<array.length();i++){JSONObject p=array.optJSONObject(i);if(p==null)continue;JSONObject c=p.optJSONObject("Canal");String image=horizontal(p);if(c!=null&&!image.isEmpty())rows.add(new Row(p.optLong("FechaHoraInicio"),c.optString("CodCadenaTv"),p.optString("Titulo"),image));}return rows;
    }
    private void prune(){synchronized(directory){File[] files=directory.listFiles((d,n)->n.endsWith(".json"));if(files==null)return;Arrays.sort(files,Comparator.comparingLong(File::lastModified));long total=0;for(File f:files)total+=f.length();for(File f:files){if(total<=32L*1024*1024)break;long size=f.length();if(f.delete())total-=size;}}}
    private static String read(InputStream stream)throws IOException{try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>4*1024*1024)throw new IOException("Artwork grid too large");out.write(buffer,0,n);}return out.toString("UTF-8");}}
    public void close(){closed=true;workers.shutdownNow();client.dispatcher().cancelAll();}
}
