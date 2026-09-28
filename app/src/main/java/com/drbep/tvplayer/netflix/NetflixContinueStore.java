package com.drbep.tvplayer.netflix;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Local viewing metadata only; account and DRM state remain in their own stores. */
public final class NetflixContinueStore {
    private static final String[] FIELDS={"title","poster_base","poster_query","vod_facts","vod_synopsis"};
    private NetflixContinueStore() {}
    public static void save(Context context, Intent intent, long position, long duration) {
        long id=intent.getLongExtra("manifest_title",0);
        if(id<=0)return;
        SharedPreferences prefs=context.getSharedPreferences("netflix_resume",0);
        SharedPreferences.Editor edit=prefs.edit().putLong("title:"+id,Math.max(0,position));
        if(position<=30000) { edit.remove("card:"+id).apply(); return; }
        try {
            JSONObject entry=new JSONObject().put("id",id).put("position",Math.max(0,position))
                    .put("duration",Math.max(0,duration)).put("updated",System.currentTimeMillis());
            for(String field:FIELDS)entry.put(field,intent.getStringExtra(field));
            edit.putString("card:"+id,entry.toString());
            // Bound artwork/metadata history, retaining the most recently watched titles.
            List<JSONObject> entries=list(context);
            for(int i=79;i<entries.size();i++) {
                long old=entries.get(i).optLong("id");
                if(old!=id)edit.remove("card:"+old);
            }
        } catch(org.json.JSONException ignored) { }
        edit.apply();
    }
    public static List<JSONObject> list(Context context) {
        List<JSONObject> entries=new ArrayList<>();
        for(java.util.Map.Entry<String,?> row:context.getSharedPreferences("netflix_resume",0).getAll().entrySet()) {
            if(!row.getKey().startsWith("card:") || !(row.getValue() instanceof String))continue;
            try {
                JSONObject entry=new JSONObject((String)row.getValue());
                if(entry.optLong("id")>0 && entry.optLong("position")>30000)entries.add(entry);
            } catch(org.json.JSONException ignored) { }
        }
        entries.sort((a,b)->Long.compare(b.optLong("updated"),a.optLong("updated")));
        return entries;
    }
    public static Intent playbackIntent(Context context, JSONObject entry) {
        Intent intent=new Intent(context,NetflixPlaybackActivity.class).putExtra("manifest_title",entry.optLong("id"));
        for(String field:FIELDS)intent.putExtra(field,entry.optString(field));
        return intent;
    }
}
