package com.drbep.tvplayer.netflix;

import java.util.*;
import org.json.*;

/** Playable choices from the current account's manifest; never invents unavailable tracks. */
final class NetflixTracks {
    static final class Choice {
        final String url, label, language;
        final int width, height;
        final long bitrate;
        final boolean forced;
        Choice(String url, String label, String language, int width, int height, long bitrate) {
            this(url,label,language,width,height,bitrate,false);
        }
        Choice(String url, String label, String language, int width, int height, long bitrate, boolean forced) {
            this.forced=forced;
            this.url=url; this.label=label; this.language=language;
            this.width=width; this.height=height; this.bitrate=bitrate;
        }
    }
    final List<Choice> video = new ArrayList<>(), audio = new ArrayList<>(), subtitles = new ArrayList<>();
    static NetflixTracks parse(Object value) throws Exception {
        if (!(value instanceof JSONObject)) throw new IllegalArgumentException("Invalid manifest");
        JSONObject root=(JSONObject)value;
        if (root.optJSONObject("result")!=null) root=root.optJSONObject("result");
        NetflixTracks choices=new NetflixTracks();
        JSONArray videos=root.optJSONArray("videoTracks");
        if (videos!=null) for(int t=0;t<videos.length();t++) {
            JSONObject track=videos.optJSONObject(t);
            JSONArray streams=track==null?null:track.optJSONArray("streams");
            if(streams==null) continue;
            for(int i=0;i<streams.length();i++) {
                JSONObject s=streams.optJSONObject(i); if(s==null) continue;
                String url=NetflixMediaUrls.findUrl(s.opt("urls"),0);
                int w=s.optInt("resW"),h=s.optInt("resH"); long rate=s.optLong("bitrate");
                if(url!=null && w>0 && h>0) choices.video.add(new Choice(url,
                    w+"×"+h+" · "+bitrateLabel(rate), "",w,h,rate));
            }
        }
        choices.video.sort(Comparator.<Choice>comparingLong(c->(long)c.width*c.height)
                .thenComparingLong(c->c.bitrate).reversed());
        JSONArray audios=root.optJSONArray("audioTracks");
        if(audios!=null) for(int i=0;i<audios.length();i++) {
            JSONObject a=audios.optJSONObject(i); if(a==null)continue;
            JSONArray streams=a.optJSONArray("streams"); JSONObject best=null;
            if(streams!=null) for(int j=0;j<streams.length();j++) {
                JSONObject s=streams.optJSONObject(j);
                if(s!=null && NetflixMediaUrls.findUrl(s.opt("urls"),0)!=null
                        && (best==null || s.optLong("bitrate")>best.optLong("bitrate"))) best=s;
            }
            if(best!=null) choices.audio.add(new Choice(NetflixMediaUrls.findUrl(best.opt("urls"),0),
                languageLabel(a,false),a.optString("language"),0,0,best.optLong("bitrate")));
        }
        for(String key:new String[]{"timedTextTracks","timedtexttracks","textTracks"}) {
            JSONArray texts=root.optJSONArray(key); if(texts==null)continue;
            for(int i=0;i<texts.length();i++) {
                JSONObject t=texts.optJSONObject(i); if(t==null || t.optBoolean("isNoneTrack"))continue;
                // Only the WebVTT profile requested by this client is supported.
                String url=findWebVtt(t,0);
                if(url!=null) choices.subtitles.add(new Choice(url,languageLabel(t,true),t.optString("language"),0,0,0,t.optBoolean("isForcedNarrative")));
            }
            break;
        }
        choices.subtitles.sort(Comparator.<Choice,Boolean>comparing(c->c.forced)
                .thenComparingInt(c->c.language.equals("es-ES")?0:c.language.equals("es")?1:2)
                .thenComparing(c->c.label));
        if(choices.video.isEmpty()) throw new IllegalArgumentException("No playable video");
        return choices;
    }
    static String subtitleSchema(Object value) {
        if (!(value instanceof JSONObject)) return "invalid";
        JSONObject root=(JSONObject)value;
        if(root.optJSONObject("result")!=null)root=root.optJSONObject("result");
        StringBuilder out=new StringBuilder();
        Iterator<String> names=root.keys();
        while(names.hasNext()) {
            String name=names.next();
            if(name.matches("[A-Za-z_]{1,50}")) out.append(name).append(',');
            if(name.toLowerCase(Locale.ROOT).contains("text")) schemaKeys(root.opt(name),out,0);
        }
        return out.toString();
    }
    private static void schemaKeys(Object value,StringBuilder out,int depth) {
        if(depth>4 || out.length()>1600)return;
        if(value instanceof JSONArray){schemaKeys(((JSONArray)value).opt(0),out,depth+1);return;}
        if(!(value instanceof JSONObject))return;
        JSONObject obj=(JSONObject)value;Iterator<String> names=obj.keys();
        while(names.hasNext()) {
            String name=names.next();
            if(name.matches("[A-Za-z0-9_-]{1,50}"))out.append('[').append(name).append(']');
            schemaKeys(obj.opt(name),out,depth+1);
        }
    }
    int defaultAudio() {
        for(int i=0;i<audio.size();i++) if(audio.get(i).language.equals("es") || audio.get(i).language.equals("es-ES"))return i;
        return audio.isEmpty()?-1:0;
    }
    static String bitrateLabel(long kbps) {
        return kbps>0?String.format(Locale.ROOT,"%.2f Mb/s",kbps/1000.0):"bitrate no indicado";
    }
    private static String languageLabel(JSONObject track, boolean subtitle) {
        String language=track.optString("language");
        String label=track.optString("languageDescription");
        if(label.isEmpty() || (subtitle && track.optBoolean("isForcedNarrative"))) label=Locale.forLanguageTag(language).getDisplayName(new Locale("es"));
        if(label.isEmpty()) label="Sin idioma indicado";
        String type=track.optString("trackType");
        if(type.isEmpty())type=track.optString("rawTrackType");
        if(type.toLowerCase(Locale.ROOT).contains("assist"))label+=subtitle?" · SDH":" · Audiodescripción";
        if(type.toLowerCase(Locale.ROOT).contains("closed"))label+=" · SDH";
        if(track.optBoolean("isForcedNarrative"))label+=" · Forzados";
        return label;
    }
    private static String findWebVtt(Object object,int depth) {
        if(depth>5 || !(object instanceof JSONObject))return null;
        JSONObject o=(JSONObject)object;
        Iterator<String> keys=o.keys();
        while(keys.hasNext()) {
            String k=keys.next(); Object child=o.opt(k);
            if(k.startsWith("webvtt")) { String url=NetflixMediaUrls.findUrl(child,0); if(url!=null)return url; }
            String found=findWebVtt(child,depth+1); if(found!=null)return found;
        }
        return null;
    }
}
