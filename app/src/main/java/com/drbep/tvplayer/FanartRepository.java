package com.drbep.tvplayer;
import android.util.JsonReader;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import okhttp3.*;

/** Public artwork index only. No playback URL, token or user catalogue is sent. */
final class FanartRepository {
 private final File cache;
 private final OkHttpClient client=new OkHttpClient.Builder().connectTimeout(8,java.util.concurrent.TimeUnit.SECONDS).readTimeout(20,java.util.concurrent.TimeUnit.SECONDS).callTimeout(30,java.util.concurrent.TimeUnit.SECONDS).build();
 private volatile Map<String,List<Entry>> index=Collections.emptyMap();
 private volatile String origin="";
 private volatile long checked;
 private static final Pattern MARKS=Pattern.compile("\\p{M}+");
 private static final Pattern NON_WORD=Pattern.compile("[^a-z0-9]");
 private static final Pattern QUALITY=Pattern.compile("(?i)\\s+(?:uhd|fhd|hd|sd|4k)$");
 private static final Pattern EPISODE=Pattern.compile("(?i)\\s*(?:[-:]?\\s*(?:t\\d+\\s*e\\d+|s\\d+e\\d+|ep(?:isodio)?\\.?\\s*\\d+)).*$");
 static String titleKey(String s){return clean(EPISODE.matcher(s==null?"":s).replaceFirst(""));}
 static String channelKey(String s){return clean(QUALITY.matcher(s==null?"":s.trim()).replaceFirst("").replace("Movistar+","M+").replace("Movistar Plus+","M+"));}
 private static String clean(String s){return NON_WORD.matcher(MARKS.matcher(java.text.Normalizer.normalize(s.toLowerCase(Locale.ROOT),java.text.Normalizer.Form.NFD)).replaceAll("")).replaceAll("");}
 static final class Entry {final long start;final String title,id;Entry(long s,String t,String i){start=s;title=t;id=i;}}
 FanartRepository(File dir){cache=new File(dir,"public-fanart-index.json");}
 synchronized boolean refresh(String base) throws IOException {
  String b=base.replaceAll("/+$","");long now=System.currentTimeMillis();
  if(!b.equals(origin)){origin=b;index=Collections.emptyMap();checked=0;}
  if(now-checked<4*3600000L)return false;
  boolean loaded=false;
  if(index.isEmpty() && cache.isFile()) {try{index=read(cache);loaded=true;}catch(Exception ignored){}}
  if(cache.isFile() && now-cache.lastModified()<4*3600000L && loaded){checked=now;return true;}
  File tmp=new File(cache.getPath()+".next");
  try(Response response=client.newCall(new Request.Builder().url(b+"/api/offline/fanart/index").build()).execute()){
   if(!response.isSuccessful()||response.body()==null){checked=now-3*3600000L;return loaded;}
   try(InputStream in=response.body().byteStream();OutputStream out=new FileOutputStream(tmp)){
    byte[] buf=new byte[16384];int n,total=0;while((n=in.read(buf))!=-1){total+=n;if(total>16*1024*1024)throw new IOException("Artwork index too large");out.write(buf,0,n);}
   }
   Map<String,List<Entry>> next=read(tmp);if(next.isEmpty())throw new IOException("Empty artwork index");
   if(!tmp.renameTo(cache))throw new IOException("Cannot save artwork index");index=next;checked=now;return true;
  } finally {tmp.delete();}
 }
 private static Map<String,List<Entry>> read(File f)throws IOException{
  Map<String,List<Entry>> out=new HashMap<>();int count=0;
  try(JsonReader r=new JsonReader(new InputStreamReader(new FileInputStream(f),java.nio.charset.StandardCharsets.UTF_8))){
   r.beginObject();while(r.hasNext()){
    if(!r.nextName().equals("programmes")){r.skipValue();continue;}
    r.beginArray();while(r.hasNext()){
     r.beginArray();String channel=r.nextString();long start=r.nextLong();r.nextLong();String title=r.nextString(),id=r.nextString();while(r.hasNext())r.skipValue();r.endArray();
     if(++count>150000)throw new IOException("Too many artwork rows");
     if(id.matches("[A-Za-z0-9_-]{1,80}"))out.computeIfAbsent(channelKey(channel),k->new ArrayList<>()).add(new Entry(start,titleKey(title),id));
    }r.endArray();
   }r.endObject();
  }
  for(List<Entry> rows:out.values())rows.sort(Comparator.comparingLong(e->e.start));return out;
 }
 String find(String channel,String tvgId,long startMs,String title){
  String id=match(index.get(channelKey(channel)),startMs/1000,titleKey(title));
  if(id==null)id=match(index.get(channelKey(tvgId)),startMs/1000,titleKey(title));
  return id==null?null:origin+"/api/offline/fanart/image/"+id;
 }
 static String match(List<Entry> rows,long start,String title){
  if(rows==null||title.isEmpty())return null;int lo=0,hi=rows.size();
  while(lo<hi){int mid=(lo+hi)/2;if(rows.get(mid).start<start-120)lo=mid+1;else hi=mid;}
  String result=null;for(int i=lo;i<rows.size()&&rows.get(i).start<=start+120;i++){
   Entry e=rows.get(i);if(e.title.equals(title)) {if(result!=null&&!result.equals(e.id))return null;result=e.id;}
  }return result;
 }
}
