package com.drbep.tvplayer;
import android.content.SharedPreferences;
import java.util.*;import java.util.concurrent.*;import java.util.regex.*;
import java.time.Instant;
import okhttp3.*;
/** Reads only the provider archive manifest; never calls DRBEP stream/replay or a licence endpoint. */
final class ArchiveAvailability {
 enum State { AVAILABLE, UNAVAILABLE, UNKNOWN }
 private final SharedPreferences prefs;
 private final Set<String> pending=ConcurrentHashMap.newKeySet();
 private final ExecutorService executor=Executors.newFixedThreadPool(2);
 private final OkHttpClient client=new OkHttpClient.Builder().connectTimeout(5,TimeUnit.SECONDS).readTimeout(5,TimeUnit.SECONDS).callTimeout(8,TimeUnit.SECONDS).followRedirects(false).build();
 ArchiveAvailability(SharedPreferences p){prefs=p;}
 private static final Pattern SERVICE_CODE=Pattern.compile("[0-9]{3,6}");
 private static final Pattern SERVICE_PATH=Pattern.compile("(?:/CVXCH|/deliverty/)([0-9]{3,6})(?:[/.])|/([0-9]{3,6})/vxfmt=dp/");
 static String code(String value){if(value==null||value.isEmpty())return "";if(SERVICE_CODE.matcher(value).matches())return value;Matcher m=SERVICE_PATH.matcher(value);return m.find()?(m.group(1)!=null?m.group(1):m.group(2)):"";}
 private String key(String scope,String id){return android.net.Uri.encode(scope+"|"+id);}
 void register(String scope,ChannelItem c,String providerCode){String baseKey=key(scope,c.id);if(providerCode!=null&&providerCode.matches("[A-Za-z0-9_-]{1,24}")&&!providerCode.equals(prefs.getString(baseKey+".provider","")))prefs.edit().putString(baseKey+".provider",providerCode).apply();String value=code(providerCode);if(value.isEmpty())value=code(c.tvgId);if(value.isEmpty())value=code(c.playUrl);if(value.isEmpty())value=code(c.fallbackPlayUrl);String k=key(scope,c.id)+".code";if(!value.isEmpty()&&!value.equals(prefs.getString(k,"")))prefs.edit().putString(k,value).apply();}
 void registerOfficial(String scope,ChannelItem c,org.json.JSONObject identity){
  String base=key(scope,c.id),provider=identity.optString("CodCadenaTv"),service=identity.optString("ServiceUid2");
  if(!provider.matches("[A-Za-z0-9_-]{1,24}"))return;
  SharedPreferences.Editor edit=prefs.edit().putString(base+".provider",provider).putString(base+".name",identity.optString("Nombre"));
  if(service.matches("[0-9]{3,6}"))edit.putString(base+".code",service);edit.apply();
 }
 static long ttl(State s){return s==State.AVAILABLE?3*86400000L:s==State.UNAVAILABLE?86400000L:15*60000L;}
 static State classify(int status,String prefix){if(status==404||status==410)return State.UNAVAILABLE;if(status>=200&&status<300&&prefix.trim().startsWith("#EXTM3U"))return State.AVAILABLE;return State.UNKNOWN;}
 State cached(String scope,ChannelItem c){String k=key(scope,c.id);long age=System.currentTimeMillis()-prefs.getLong(k+".time",0);State s;try{s=State.valueOf(prefs.getString(k+".state","UNKNOWN"));}catch(Exception e){s=State.UNKNOWN;}return age>=0&&age<ttl(s)?s:State.UNKNOWN;}
 boolean needsIdentity(String scope,ChannelItem c){String k=key(scope,c.id);return prefs.getString(k+".code","").isEmpty()&&prefs.getString(k+".provider","").isEmpty()&&System.currentTimeMillis()-prefs.getLong(k+".identityAttempt",0)>15*60000L;}
 void identityAttempt(String scope,ChannelItem c){prefs.edit().putLong(key(scope,c.id)+".identityAttempt",System.currentTimeMillis()).apply();}
 boolean unavailable(String scope,ChannelItem c){return cached(scope,c)==State.UNAVAILABLE;}
 void schedule(String scope,ChannelItem c,Runnable changed){register(scope,c,"");String k=key(scope,c.id),code=prefs.getString(k+".code","");if(code.isEmpty()&&prefs.getString(k+".provider","").isEmpty())return;long age=System.currentTimeMillis()-prefs.getLong(k+".time",0);State old=cached(scope,c);if(age>=0&&age<ttl(old))return;if(!pending.add(k))return;
  final String identity=identityFingerprint(c);
  executor.execute(()->{try{long now=System.currentTimeMillis();String resolved=code.isEmpty()?resolveCode(k):code;State state=resolved.isEmpty()?State.UNKNOWN:probe(resolved,now-4*3600000L);if(state==State.UNAVAILABLE){State second=probe(resolved,now-24*3600000L);state=second==State.UNAVAILABLE?State.UNAVAILABLE:second;}
   if(!identity.equals(prefs.getString(k+".identity",identity)))return;
   prefs.edit().putString(k+".state",state.name()).putLong(k+".time",System.currentTimeMillis()).apply();if(state!=old)changed.run();
  }finally{pending.remove(k);}});
 }
 static String identityFingerprint(ChannelItem c){
  // Metadata only. Do not persist signed URLs or rotating playback credentials.
  return U7dChannelIdentity.normalize(c.name)+"|"+Objects.toString(c.tvgId,"").trim().toUpperCase(Locale.ROOT)
    +"|"+code(c.playUrl)+"|"+code(c.fallbackPlayUrl);
 }
 static boolean associationChanged(ChannelItem c,String previous,String official,String provider){
  return previous!=null ? !previous.equals(identityFingerprint(c))
    : !official.isEmpty() && !U7dChannelIdentity.matches(c.name,official)
      && !U7dChannelIdentity.matches(c.tvgId,official)
      && !Objects.toString(c.tvgId,"").equalsIgnoreCase(provider);
 }
 boolean invalidateMismatchedIdentity(String scope,ChannelItem c){return invalidateMismatchedIdentityAtKey(key(scope,c.id),c);}
 synchronized boolean invalidateMismatchedIdentityAtKey(String base,ChannelItem c){
  String current=identityFingerprint(c),previous=prefs.getString(base+".identity",null);
  if(current.equals(previous))return false; // No disk write or preference-map copy for unchanged associations.
  String official=prefs.getString(base+".name","");
  boolean changed=associationChanged(c,previous,official,prefs.getString(base+".provider",""));
  SharedPreferences.Editor edit=prefs.edit().putString(base+".identity",current);
  if(changed)for(String suffix:new String[]{".provider",".code",".name",".state",".time",".identityAttempt"})edit.remove(base+suffix);
  edit.apply();return changed;
 }

 String provider(String scope,ChannelItem c){String value=prefs.getString(key(scope,c.id)+".provider","");if(value.isEmpty()&&c.tvgId!=null&&!c.tvgId.matches("[0-9]+"))value=c.tvgId;return value;}
 String officialName(String scope,ChannelItem c,String fallback){return prefs.getString(key(scope,c.id)+".name",fallback);}
 private String resolveCode(String key){
  String provider=prefs.getString(key+".provider","");
  HttpUrl url=HttpUrl.parse("https://ottcache.dof6.com/movistarplus/webplayer/OTT/epg").newBuilder()
   .addQueryParameter("from",java.time.LocalDate.now().toString()+"T06:00:00").addQueryParameter("span","1")
   .addQueryParameter("channel",provider).addQueryParameter("network","movistarplus").addQueryParameter("version","8.2")
   .addQueryParameter("mdrm","true").addQueryParameter("tlsstream","true").addQueryParameter("demarcation","1").build();
  try(Response r=client.newCall(new Request.Builder().url(url).build()).execute()){
   if(!r.isSuccessful()||r.body()==null)return "";
   String body=r.peekBody(1048576).string();org.json.JSONArray rows=new org.json.JSONArray(body);
   for(int i=0;i<rows.length();i++){org.json.JSONObject channel=rows.getJSONObject(i).optJSONObject("Canal");
    if(channel==null||!provider.equalsIgnoreCase(channel.optString("CodCadenaTv","")))continue;
    String result=code(channel.optString("ServiceUid2",""));if(!result.isEmpty()){prefs.edit().putString(key+".code",result).putString(key+".name",channel.optString("Nombre","")).apply();return result;}
   }
  }catch(Exception ignored){}return "";
 }
 private State probe(String code,long start){String url="https://cutv-wp10.cdn.telefonica.com/"+code+"/vxfmt=dp/playlist.m3u8";HttpUrl parsed=HttpUrl.parse(url);if(parsed==null)return State.UNKNOWN;HttpUrl target=parsed.newBuilder().addQueryParameter("device_profile","HLS_TV_FAIRPLAY").addQueryParameter("start_time",Instant.ofEpochSecond(start/1000).toString()).addQueryParameter("end_time",Instant.ofEpochSecond(start/1000+60).toString()).build();
  try(Response r=client.newCall(new Request.Builder().url(target).header("Range","bytes=0-4095").build()).execute()){return classify(r.code(),r.body()==null?"":r.peekBody(4096).string());}catch(Exception ignored){return State.UNKNOWN;}
 }
 void close(){executor.shutdownNow();client.dispatcher().cancelAll();}
}
