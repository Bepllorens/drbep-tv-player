package com.drbep.tvplayer;

import java.text.Normalizer;
import java.util.Locale;
import org.json.JSONObject;

/** Shared presentation only: never changes channel identities, routes or playback. */
final class ProgrammePresentation {
    static String color(String genre) {
        String g=Normalizer.normalize(genre==null?"":genre,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
        if(g.matches(".*(deport|sport|futbol).*"))return "#68C990";
        if(g.matches(".*(notici|informativ|news).*"))return "#83B9F9";
        if(g.matches(".*(infantil|kids|animacion).*"))return "#F2CE66";
        if(g.matches(".*(document|naturaleza).*"))return "#68CEC9";
        if(g.matches(".*(serie|series).*"))return "#FFAD72";
        if(g.matches(".*(music).*"))return "#F092C3";
        if(g.matches(".*(cine|pelicul|movie|thriller|terror|drama|comedia|ficcion|accion).*"))return "#AF8FFF";
        return "#91A3B5";
    }
    static String metadata(JSONObject p) {
        StringBuilder b=new StringBuilder();
        for(String key:new String[]{"subtitle","episode","genre","year","age","rating","country"}){
            String v=p.optString(key).trim();if(v.isEmpty())continue;
            if(b.length()>0)b.append(" · ");
            if(key.equals("episode"))b.append("Episodio: ");
            if(key.equals("age"))b.append("Edad: ");
            if(key.equals("rating"))b.append("★ ");b.append(v);
        }return b.toString();
    }
    static String state(long start,long end,long now) {
        return end<=now?"Emitido":start<=now?"En directo":"Empieza en "+Math.max(1,(start-now+59999)/60000)+" min";
    }
}
