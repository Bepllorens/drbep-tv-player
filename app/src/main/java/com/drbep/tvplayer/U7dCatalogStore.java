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
    private final java.io.File legacyFile;
    U7dCatalogStore(Context context) { this(context,"u7d-index.db",context.getDatabasePath("u7d-catalog.db")); }
    U7dCatalogStore(Context context,String name,java.io.File legacy) { super(context.getApplicationContext(),name,null,2);legacyFile=legacy;setWriteAheadLoggingEnabled(true); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE catalog(scope TEXT, channel TEXT, updated INTEGER, payload TEXT, PRIMARY KEY(scope,channel))");
        createPrograms(db);
        db.execSQL("CREATE TABLE saved(scope TEXT, id TEXT, channel TEXT, expires INTEGER, payload TEXT, PRIMARY KEY(scope,id))");
        if(legacyFile!=null && legacyFile.exists()) {
            try(SQLiteDatabase old=SQLiteDatabase.openDatabase(legacyFile.getPath(),null,SQLiteDatabase.OPEN_READONLY)) {
                for(String table:new String[]{"catalog","saved"})try(Cursor c=old.rawQuery("SELECT * FROM "+table,null)) {
                    while(c.moveToNext()) {android.content.ContentValues row=new android.content.ContentValues();
                        for(int i=0;i<c.getColumnCount();i++)if(c.isNull(i))row.putNull(c.getColumnName(i));else row.put(c.getColumnName(i),c.getString(i));
                        try {
                            if(table.equals("catalog")) {
                                JSONArray items=new JSONArray(row.getAsString("payload"));
                                for(int i=0;i<items.length();i++)putProgram(db,row.getAsString("scope"),decode(items.getJSONObject(i)));
                                row.put("payload","[]");
                            } else putProgram(db,row.getAsString("scope"),decode(new JSONObject(row.getAsString("payload"))));
                        }catch(org.json.JSONException corrupt){android.util.Log.w("CatchupDB","Skipped invalid legacy metadata");}
                        db.insertOrThrow(table,null,row);
                    }
                }
            }
            db.execSQL("DELETE FROM programs WHERE end<? AND NOT EXISTS (SELECT 1 FROM saved s WHERE s.scope=programs.scope AND s.id=programs.id)",new Object[]{System.currentTimeMillis()-8L*86400000});
        }
    }
    @Override public void onUpgrade(SQLiteDatabase db, int old, int next) {
        createPrograms(db);
        // SQLiteOpenHelper wraps the migration in a transaction. Keep legacy rows for rollback.
        try (Cursor c=db.rawQuery("SELECT scope,payload FROM catalog",null)) {
            while(c.moveToNext()) { try { JSONArray a=new JSONArray(c.getString(1));
                for(int i=0;i<a.length();i++)putProgram(db,c.getString(0),decode(a.getJSONObject(i)));
            } catch(org.json.JSONException corrupt) { android.util.Log.w("CatchupDB","Skipped invalid legacy metadata"); } }
        }
        try (Cursor c=db.rawQuery("SELECT scope,payload FROM saved",null)) {
            while(c.moveToNext()) { try { putProgram(db,c.getString(0),decode(new JSONObject(c.getString(1)))); }
                catch(org.json.JSONException corrupt) { android.util.Log.w("CatchupDB","Skipped invalid saved metadata"); } }
        }
    }
    private void createPrograms(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS programs(scope TEXT NOT NULL,id TEXT NOT NULL,channel TEXT NOT NULL,day TEXT NOT NULL,start INTEGER NOT NULL,end INTEGER NOT NULL,genre TEXT NOT NULL,search TEXT NOT NULL,payload TEXT NOT NULL,PRIMARY KEY(scope,id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS programs_day ON programs(scope,day,start DESC,id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS programs_channel ON programs(scope,channel,day,start DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS programs_time ON programs(scope,start)");
        db.execSQL("CREATE INDEX IF NOT EXISTS programs_genre ON programs(scope,day,genre,start DESC)");
    }
    private void putProgram(SQLiteDatabase db,String scope,EpgRepository.EpgProgram p) {
        long start=EpgTimeCodec.parseEpochMillis(p.startTime),end=EpgTimeCodec.parseEpochMillis(p.endTime);
        if(start<=0 || end<=start)return;
        try {
            String payload=encode(p).toString();
            db.execSQL("INSERT OR REPLACE INTO programs SELECT ?,?,?,?,?,?,?,?,? WHERE NOT EXISTS (SELECT 1 FROM programs WHERE scope=? AND id=? AND payload=?)",new Object[]{scope,id(p),p.channelId,ArchiveWindow.day(p.startTime),start,end,p.category==null?"":p.category,(p.title+" "+p.channelName).toLowerCase(java.util.Locale.ROOT),payload,scope,id(p),payload});
        }
        catch(Exception e) { throw new IllegalArgumentException(e); }
    }
    static final class Page {
        final List<EpgRepository.EpgProgram> programs=new ArrayList<>();
        int total,offset;
    }
    Page page(String scope,List<String> allowed,String day,String channel,String genre,String search,boolean savedOnly,int offset,int limit,long now) {
        Page page=new Page(); if(allowed.isEmpty())return page;
        List<String> args=new ArrayList<>();args.add(scope);
        StringBuilder where=new StringBuilder("p.scope=? AND p.channel IN (");
        for(String id:allowed){if(args.size()>1)where.append(',');where.append('?');args.add(id);}where.append(')');
        if(savedOnly)where.append(" AND EXISTS (SELECT 1 FROM saved s WHERE s.scope=p.scope AND s.id=p.id)");
        else {where.append(" AND p.day=? AND p.start<=? AND p.end>?");args.add(day);args.add(Long.toString(now));args.add(Long.toString(now-7L*86400000));}
        if(!channel.isEmpty()){where.append(" AND p.channel=?");args.add(channel);}
        if(!genre.isEmpty()){where.append(" AND p.genre=?");args.add(genre);}
        if(!search.trim().isEmpty()){where.append(" AND instr(p.search,?)>0");args.add(search.trim().toLowerCase(java.util.Locale.ROOT));}
        SQLiteDatabase db=getReadableDatabase();String[] values=args.toArray(new String[0]);
        try(Cursor c=db.rawQuery("SELECT count(*) FROM programs p WHERE "+where,values)){if(c.moveToFirst())page.total=c.getInt(0);}
        int size=Math.max(1,Math.min(240,limit));page.offset=Math.min(Math.max(0,offset),Math.max(0,((page.total-1)/size)*size));
        try(Cursor c=db.rawQuery("SELECT p.payload FROM programs p WHERE "+where+" ORDER BY p.start DESC,p.id LIMIT "+size+" OFFSET "+page.offset,values)){
            while(c.moveToNext())try{page.programs.add(decode(new JSONObject(c.getString(0))));}catch(org.json.JSONException ignored){}
        }
        return page;
    }
    long nextChange(String scope,long now) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT MIN(start) FROM programs WHERE scope=? AND start>?",new String[]{scope,Long.toString(now)})) {
            if(c.moveToFirst()&&!c.isNull(0))return c.getLong(0);
        }return Long.MAX_VALUE;
    }
    List<String> genres(String scope) {
        List<String> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT DISTINCT genre FROM programs WHERE scope=? AND genre<>'' ORDER BY genre",new String[]{scope})){while(c.moveToNext())out.add(c.getString(0));}return out;
    }
    EpgRepository.EpgProgram find(String scope,String id) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT payload FROM programs WHERE scope=? AND id=?",new String[]{scope,id})){
            if(c.moveToFirst())return decode(new JSONObject(c.getString(0)));
        }catch(org.json.JSONException ignored){}return null;
    }

    static String id(EpgRepository.EpgProgram p) { return p.channelId + "|" + p.startTime; }
    static JSONObject encode(EpgRepository.EpgProgram p) throws Exception {
        return new JSONObject().put("channel",p.channelId).put("name",p.channelName).put("tvg",p.tvgId)
            .put("title",p.title).put("icon",p.icon).put("description",p.description)
            .put("start",p.startTime).put("end",p.endTime).put("category",p.category).put("metadata",p.metadata==null?JSONObject.NULL:p.metadata.json()).put("details",p.detailsUrl);
    }
    static EpgRepository.EpgProgram decode(JSONObject p) {
        return new EpgRepository.EpgProgram(p.optString("channel"),p.optString("name"),p.optString("tvg"),
            p.optString("title"),p.optString("icon"),p.optString("description"),p.optString("start"),p.optString("end"),p.optString("category"),0,ProgrammeMetadata.stored(p.optJSONObject("metadata")),p.optString("details"));
    }
    List<EpgRepository.EpgProgram> read(String scope, String channel) {
        List<EpgRepository.EpgProgram> out = new ArrayList<>();
        try (Cursor c=getReadableDatabase().rawQuery("SELECT payload FROM programs WHERE scope=? AND channel=? ORDER BY start",new String[]{scope,channel})) {
            while(c.moveToNext())out.add(decode(new JSONObject(c.getString(0))));
        } catch(org.json.JSONException ignored) { }
        return out;
    }
    List<EpgRepository.EpgProgram> recent(String scope,String channel,long from,long now,int limit) {
        List<EpgRepository.EpgProgram> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT payload FROM programs WHERE scope=? AND channel=? AND start<=? AND end>=? ORDER BY start DESC,id LIMIT ?",new String[]{scope,channel,Long.toString(now),Long.toString(from),Integer.toString(Math.min(40,Math.max(1,limit)))})) {
            while(c.moveToNext())try{out.add(decode(new JSONObject(c.getString(0))));}catch(org.json.JSONException ignored){}
        }return out;
    }
    synchronized void invalidate(String scope,String channel) {
        getWritableDatabase().delete("catalog","scope=? AND channel=?",new String[]{scope,channel});
        getWritableDatabase().delete("programs","scope=? AND channel=? AND NOT EXISTS (SELECT 1 FROM saved s WHERE s.scope=programs.scope AND s.id=programs.id)",new String[]{scope,channel});
    }
    boolean fresh(String scope,String channel,long now) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT updated FROM catalog WHERE scope=? AND channel=?",new String[]{scope,channel})) {
            return c.moveToFirst() && now>=c.getLong(0) && now-c.getLong(0)<300000L;
        }
    }
    synchronized void index(String scope,String channel,List<EpgRepository.EpgProgram> items) throws Exception { store(scope,channel,items,false); }
    synchronized void write(String scope,String channel,List<EpgRepository.EpgProgram> items) throws Exception { store(scope,channel,items,true); }
    private void store(String scope,String channel,List<EpgRepository.EpgProgram> items,boolean refreshed) throws Exception {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            for(EpgRepository.EpgProgram p:items)if(ArchiveWindow.retained(p.startTime,System.currentTimeMillis()))putProgram(db,scope,p);
            if(refreshed)db.execSQL("INSERT OR REPLACE INTO catalog VALUES(?,?,?,?)",new Object[]{scope,channel,System.currentTimeMillis(),"[]"});
            db.execSQL("DELETE FROM programs WHERE scope=? AND end<? AND NOT EXISTS (SELECT 1 FROM saved s WHERE s.scope=programs.scope AND s.id=programs.id)",new Object[]{scope,System.currentTimeMillis()-8L*86400000});
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
    synchronized void indexAll(String scope,java.util.Map<String,List<EpgRepository.EpgProgram>> channels) throws Exception {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {for(java.util.Map.Entry<String,List<EpgRepository.EpgProgram>> channel:channels.entrySet())for(EpgRepository.EpgProgram p:channel.getValue())if(ArchiveWindow.retained(p.startTime,System.currentTimeMillis()))putProgram(db,scope,p);db.setTransactionSuccessful();}
        finally{db.endTransaction();}
    }
    synchronized void writeAll(String scope, java.util.Map<String,List<EpgRepository.EpgProgram>> channels) throws Exception {
        SQLiteDatabase db=getWritableDatabase();
        db.beginTransaction();
        try {
            for(java.util.Map.Entry<String,List<EpgRepository.EpgProgram>> channel:channels.entrySet())write(scope,channel.getKey(),channel.getValue());
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    boolean saved(String scope,String id) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM saved WHERE scope=? AND id=?",new String[]{scope,id})) { return c.moveToFirst(); }
    }
    synchronized boolean toggle(String scope,EpgRepository.EpgProgram p,long expires) throws Exception {
        if(saved(scope,id(p))) { getWritableDatabase().delete("saved","scope=? AND id=?",new String[]{scope,id(p)}); return false; }
        putProgram(getWritableDatabase(),scope,p);
        getWritableDatabase().execSQL("INSERT INTO saved VALUES(?,?,?,?,?)",new Object[]{scope,id(p),p.channelId,expires,encode(p).toString()}); return true;
    }
    List<EpgRepository.EpgProgram> savedPrograms(String scope) {
        List<EpgRepository.EpgProgram> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT payload FROM saved WHERE scope=? ORDER BY expires DESC LIMIT 500",new String[]{scope})) {
            while(c.moveToNext()) out.add(decode(new JSONObject(c.getString(0))));
        } catch(Exception ignored) { }
        return out;
    }
}
