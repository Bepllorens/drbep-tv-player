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
        final String text; final long expires;
        CachedDescription(String text) { this.text=text; expires=System.currentTimeMillis()+(text.isEmpty()?60000:86400000); }
    }
    static String load(String code, String startTime) throws Exception {
        if (code == null || !code.matches("[A-Za-z0-9_-]{1,32}")) return "";
        long start=EpgTimeCodec.parseEpochMillis(startTime);
        if(start<=0)return "";
        String key=code+"|"+start;
        synchronized(CACHE) { CachedDescription hit=CACHE.get(key); if(hit!=null&&hit.expires>System.currentTimeMillis())return hit.text; }
        String day=java.time.Instant.ofEpochMilli(start).atZone(java.time.ZoneId.of("Europe/Madrid")).toLocalDate().toString();
        HttpUrl url=HttpUrl.parse("https://ottcache.dof6.com/movistarplus/webplayer/OTT/epg").newBuilder()
            .addQueryParameter("from",day+"T00:00:00").addQueryParameter("span","1")
            .addQueryParameter("channel",code).addQueryParameter("network","movistarplus")
            .addQueryParameter("version","8.2").addQueryParameter("mdrm","true")
            .addQueryParameter("tlsstream","true").addQueryParameter("demarcation","1").build();
        String guideKey=code+"|"+day;
        JSONArray rows;
        synchronized(GUIDES) { GuideEntry hit=GUIDES.get(guideKey); rows=hit!=null&&System.currentTimeMillis()-hit.time<15*60000L?hit.rows:null; }
        if(rows==null){ rows=new JSONArray(read(url)); synchronized(GUIDES){GUIDES.put(guideKey,new GuideEntry(rows));} }
        String ficha=findFicha(rows,code,start);
        String text="";
        if(allowedFicha(ficha))text=description(new JSONObject(read(HttpUrl.parse(ficha))));
        synchronized(CACHE) { CACHE.put(key,new CachedDescription(text)); }
        return text;
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
