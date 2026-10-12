package com.drbep.tvplayer;

import android.database.Cursor;
import com.drbep.tvplayer.playlists.PlaylistDatabase;
import org.json.*;
import java.io.IOException;
import java.text.*;
import java.util.*;

/** Authenticated panel adapter. IDs are scoped to a list; no stream URLs or headers leave it. */
final class PlaylistGuidePanel {
    private final PlaylistDatabase db;
    PlaylistGuidePanel(PlaylistDatabase db){this.db=db;}
    String manage(JSONObject r)throws Exception {
        String source=r.getString("source"),action=r.getString("action");
        if(action.equals("guide-policy")){
            db.guideMode(source,r.getString("mode"));return new JSONObject().put("ok",true).toString();
        }
        JSONObject out=new JSONObject();
        try(Cursor c=db.source(source)){
            if(!c.moveToFirst())throw new IOException("Missing source");
            String url=c.getString(c.getColumnIndexOrThrow("epg_url"));
            okhttp3.HttpUrl parsed=okhttp3.HttpUrl.parse(url);
            out.put("feed",parsed==null?"Sin guía":parsed.host());
            out.put("mode",c.getString(c.getColumnIndexOrThrow("guide_mode")));
        }
        int offset=Math.max(0,r.optInt("offset"));
        if(action.equals("guide-channels")) {
            String q=r.optString("query");if(q.length()>200)throw new IOException("Query too long");
            JSONArray rows=new JSONArray();
            try(Cursor c=db.browse(source,"live",q,false,false,offset,51)){
                while(c.moveToNext()){
                    JSONObject attrs=new JSONObject(c.getString(c.getColumnIndexOrThrow("attributes")));
                    rows.put(new JSONObject().put("id",c.getString(c.getColumnIndexOrThrow("identity"))).put("occurrence",c.getInt(c.getColumnIndexOrThrow("occurrence"))).put("name",c.getString(c.getColumnIndexOrThrow("display_title"))).put("epgId",attrs.optString("tvg-id")));
                }
            }return out.put("channels",rows).toString();
        }
        if(!action.equals("guide-day")&&!action.equals("guide-detail"))throw new IOException("Unknown guide action");
        String identity=r.getString("identity"),channel;
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT e.attributes,e.title FROM entries e JOIN sources s ON s.id=e.source AND COALESCE(s.active,s.preview)=e.generation WHERE e.source=? AND e.identity=? AND e.occurrence=? AND e.kind='live' AND NOT EXISTS(SELECT 1 FROM preferences p WHERE p.source=e.source AND p.identity=e.identity AND p.occurrence=e.occurrence AND p.hidden=1)",new String[]{source,identity,Integer.toString(r.optInt("occurrence"))})){
            if(!c.moveToFirst())throw new IOException("Missing channel");channel=db.resolveGuideChannel(source,new JSONObject(c.getString(0)).optString("tvg-id"),c.getString(1));
        }
        String day=r.getString("day");long[] range=dayRange(day);
        long now=System.currentTimeMillis();
        if(range[0]<now-9L*86400000||range[0]>now+15L*86400000)throw new IOException("Day outside guide window");
        JSONArray programmes=new JSONArray();
        try(Cursor c=db.guide(source,channel,range[0],range[1],offset)){
            while(c.moveToNext()){
                long start=c.getLong(c.getColumnIndexOrThrow("start_ms")),end=c.getLong(c.getColumnIndexOrThrow("end_ms"));
                if(action.equals("guide-detail")&&start!=r.getLong("start"))continue;
                JSONObject meta=new JSONObject(c.getString(c.getColumnIndexOrThrow("metadata")));
                JSONObject p=new JSONObject().put("start",start).put("end",end).put("title",c.getString(c.getColumnIndexOrThrow("title"))).put("meta",ProgrammePresentation.metadata(meta)).put("color",ProgrammePresentation.color(meta.optString("genre"))).put("state",ProgrammePresentation.state(start,end,now));
                if(action.equals("guide-detail"))p.put("description",c.getString(c.getColumnIndexOrThrow("description"))).put("credits",meta.optJSONObject("credits")).put("image",meta.optString("image"));
                programmes.put(p);
            }
        }
        return out.put("programmes",programmes).put("epgId",channel).put("day",day).put("now",now).put("timeZone",TimeZone.getDefault().getID()).toString();
    }
    static long[] dayRange(String day)throws Exception {
        if(!day.matches("\\d{4}-\\d{2}-\\d{2}"))throw new IOException("Invalid day");
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);f.setLenient(false);
        Date date=f.parse(day);Calendar c=Calendar.getInstance();c.setTime(date);long start=c.getTimeInMillis();c.add(Calendar.DAY_OF_MONTH,1);return new long[]{start,c.getTimeInMillis()};
    }
}
