package com.drbep.tvplayer;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Metadata only: never stores playback URLs, headers, licences or replay sessions. */
final class U7dCatalogStore extends SQLiteOpenHelper {
    U7dCatalogStore(Context context) { super(context.getApplicationContext(), "u7d-catalog.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE catalog(scope TEXT, channel TEXT, updated INTEGER, payload TEXT, PRIMARY KEY(scope,channel))");
        db.execSQL("CREATE TABLE saved(scope TEXT, id TEXT, channel TEXT, expires INTEGER, payload TEXT, PRIMARY KEY(scope,id))");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int old, int next) { }
    static String id(EpgRepository.EpgProgram p) { return p.channelId + "|" + p.startTime; }
    static JSONObject encode(EpgRepository.EpgProgram p) throws Exception {
        return new JSONObject().put("channel",p.channelId).put("name",p.channelName).put("tvg",p.tvgId)
            .put("title",p.title).put("icon",p.icon).put("description",p.description)
            .put("start",p.startTime).put("end",p.endTime).put("category",p.category);
    }
    static EpgRepository.EpgProgram decode(JSONObject p) {
        return new EpgRepository.EpgProgram(p.optString("channel"),p.optString("name"),p.optString("tvg"),
            p.optString("title"),p.optString("icon"),p.optString("description"),p.optString("start"),p.optString("end"),p.optString("category"),0);
    }
    synchronized List<EpgRepository.EpgProgram> read(String scope, String channel) {
        List<EpgRepository.EpgProgram> out = new ArrayList<>();
        try (Cursor c=getReadableDatabase().rawQuery("SELECT payload FROM catalog WHERE scope=? AND channel=?",new String[]{scope,channel})) {
            if(c.moveToFirst()) { JSONArray a=new JSONArray(c.getString(0)); for(int i=0;i<a.length();i++) out.add(decode(a.getJSONObject(i))); }
        } catch(Exception ignored) { }
        return out;
    }
    synchronized void invalidate(String scope,String channel) {
        getWritableDatabase().delete("catalog","scope=? AND channel=?",new String[]{scope,channel});
    }
    synchronized boolean fresh(String scope,String channel,long now) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT updated FROM catalog WHERE scope=? AND channel=?",new String[]{scope,channel})) {
            return c.moveToFirst() && now>=c.getLong(0) && now-c.getLong(0)<300000L;
        }
    }
    synchronized void write(String scope,String channel,List<EpgRepository.EpgProgram> items) throws Exception {
        JSONArray a=new JSONArray(); for(EpgRepository.EpgProgram p:items) if(ArchiveWindow.retained(p.startTime,System.currentTimeMillis()))a.put(encode(p));
        SQLiteDatabase db=getWritableDatabase();
        db.execSQL("INSERT OR REPLACE INTO catalog VALUES(?,?,?,?)",new Object[]{scope,channel,System.currentTimeMillis(),a.toString()});
        db.execSQL("DELETE FROM catalog WHERE updated<?",new Object[]{System.currentTimeMillis()-8L*86400000});
    }
    synchronized void writeAll(String scope, java.util.Map<String,List<EpgRepository.EpgProgram>> channels) throws Exception {
        SQLiteDatabase db=getWritableDatabase();
        db.beginTransaction();
        try {
            for(java.util.Map.Entry<String,List<EpgRepository.EpgProgram>> channel:channels.entrySet())write(scope,channel.getKey(),channel.getValue());
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    synchronized boolean saved(String scope,String id) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM saved WHERE scope=? AND id=?",new String[]{scope,id})) { return c.moveToFirst(); }
    }
    synchronized boolean toggle(String scope,EpgRepository.EpgProgram p,long expires) throws Exception {
        if(saved(scope,id(p))) { getWritableDatabase().delete("saved","scope=? AND id=?",new String[]{scope,id(p)}); return false; }
        getWritableDatabase().execSQL("INSERT INTO saved VALUES(?,?,?,?,?)",new Object[]{scope,id(p),p.channelId,expires,encode(p).toString()}); return true;
    }
    synchronized List<EpgRepository.EpgProgram> savedPrograms(String scope) {
        List<EpgRepository.EpgProgram> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT payload FROM saved WHERE scope=? ORDER BY expires DESC LIMIT 500",new String[]{scope})) {
            while(c.moveToNext()) out.add(decode(new JSONObject(c.getString(0))));
        } catch(Exception ignored) { }
        return out;
    }
}
