package com.drbep.tvplayer;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Requests public artwork through our authenticated backend; no TMDB secret in the APK. */
final class HomeTmdbArtwork {
 static final ExecutorService IO=Executors.newFixedThreadPool(2);
 static final Handler MAIN=new Handler(Looper.getMainLooper());
 static android.content.SharedPreferences disk;
 static void initialize(android.content.Context context) {
  disk=context.getApplicationContext().getSharedPreferences("public_artwork_locations_v1",0);
 }
 static String cacheKey(Source source) {
  try {
   byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(
    (source.endpoint+source.headers.toString()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
   StringBuilder key=new StringBuilder();for(byte b:hash)key.append(String.format(java.util.Locale.ROOT,"%02x",b));return key.toString();
  } catch(java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
 }
 static void save(String key,String url) {
  if(disk==null || url.isEmpty())return;
  android.content.SharedPreferences.Editor edit=disk.edit();
  java.util.List<String> keys=new java.util.ArrayList<>();
  for(String name:disk.getAll().keySet())if(name.startsWith("time:"))keys.add(name);
  keys.sort(java.util.Comparator.comparingLong(k->disk.getLong(k,0)));
  for(int i=0;i<keys.size()-199;i++){String old=keys.get(i);edit.remove(old).remove(old.substring(5));}
  edit.putString(key,url).putLong("time:"+key,System.currentTimeMillis()).apply();
 }
 static final ArtworkLookupQueue lookups = new ArtworkLookupQueue();

 static final class Source {
  final Object original; final String endpoint; final Map<String,String> headers;
  Source(Object original,String base,CatalogSnapshotStore store,String title,String subtitle,String kind,boolean portrait) {
   this.original=original;
   String year="";java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\b(19|20)\\d{2}\\b").matcher(subtitle);if(m.find())year=m.group();
   endpoint=base.replaceAll("/+$","")+"/api/offline/preferences?scope=artwork&title="+Uri.encode(title)+"&kind="+("Series".equals(kind)||"series".equals(kind)?"tv":"movie")+"&year="+year+"&shape="+(portrait?"poster":"backdrop");
   headers=new HashMap<>();headers.put("Authorization","Bearer "+store.getAccessToken());headers.put("X-DRBEP-Device-Id",store.getDeviceId());
  }
 }
 static Runnable resolve(Source source,Consumer<Object> ready) {
  AtomicBoolean cancelled=new AtomicBoolean(); final Runnable[] fallback={null};
  Consumer<String> deliver=url->{ if(cancelled.get())return; if(url.isEmpty())fallback[0]=HomePublicArtwork.resolve(source.original,ready);else ready.accept(url); };
  String key=cacheKey(source);
  String cached=disk==null?null:disk.getString(key,null);
  boolean served=cached!=null;
  if(served) {
   deliver.accept(cached);
   if(System.currentTimeMillis()-disk.getLong("time:"+key,0)<7L*24*60*60*1000)
    return ()->{cancelled.set(true);if(fallback[0]!=null)fallback[0].run();};
  }

  Runnable cancelLookup=lookups.request(key, ()->{
   // Another lookup may have filled disk after resolve checked it.
   String recent=disk==null?null:disk.getString(key,null);
   if(recent!=null && System.currentTimeMillis()-disk.getLong("time:"+key,0)<7L*24*60*60*1000) return recent;
   String url="";
   try {
    HttpClient http=new HttpClient();HttpClient.Response response=http.get(source.endpoint,4000,6500,source.headers,4096);
    if(response.code==200) {
     JSONObject data=http.parseObject(response.body,"home artwork");
     String candidate=data.optString(source.endpoint.endsWith("poster")?"poster":"backdrop");
     if(candidate.matches("https://image\\.tmdb\\.org/t/p/(w1280|w500)/[A-Za-z0-9_-]+\\.(jpg|png)"))url=candidate;
     save(key,url);
    }
   }catch(Exception ignored) { }
   return url;
  }, result -> { if(!served) deliver.accept(result); }, IO, action -> MAIN.post(action));
  return ()->{cancelled.set(true);cancelLookup.run();if(fallback[0]!=null)fallback[0].run();};
 }
}
