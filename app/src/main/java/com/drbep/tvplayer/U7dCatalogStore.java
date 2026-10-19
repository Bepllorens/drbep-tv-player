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
    // SQLiteOpenHelper uses this monitor to open/read the database. Never hold it
    // across a batch write: readers must be able to acquire WAL connections.
    private final Object writeLock=new Object();
    private final Object savedLock=new Object();
    private volatile long savedRevision;
    long savedRevision() { return savedRevision; }
    U7dCatalogStore(Context context) { this(context,"u7d-index.db",context.getDatabasePath("u7d-catalog.db")); }
    U7dCatalogStore(Context context,String name,java.io.File legacy) { super(context.getApplicationContext(),name,null,4);legacyFile=legacy;setWriteAheadLoggingEnabled(true); }

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
        if(old>=2)return; // v3/v4 only add query indexes; do not replay legacy metadata.
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
        // Count eligible rows without fetching thousands of full programme payloads.
        db.execSQL("CREATE INDEX IF NOT EXISTS programs_page ON programs(scope,day,start DESC,id,channel,end,genre)");
        db.execSQL("CREATE INDEX IF NOT EXISTS programs_channel ON programs(scope,channel,day,start DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS programs_strip ON programs(scope,channel,start DESC,id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS programs_genres ON programs(scope,genre)");
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
        long op=BlackBox.operation(),started=System.nanoTime();int count=0;boolean success=false;
        try {Page result=pageMeasured(scope,allowed,day,channel,genre,search,savedOnly,offset,limit,now,op);count=result.programs.size();success=true;return result;}
        finally {BlackBox.event(BlackBoxStore.Event.CATCHUP_PAGE,op,BlackBox.elapsed(started),count,success?0:1);}
    }
    private Page pageMeasured(String scope,List<String> allowed,String day,String channel,String genre,String search,boolean savedOnly,int offset,int limit,long now,long op) {
        Page page=new Page(); if(allowed.isEmpty())return page;
        List<String> args=new ArrayList<>();args.add(scope);
        StringBuilder where=new StringBuilder("p.scope=? AND p.channel IN (");
        for(String id:allowed){if(args.size()>1)where.append(',');where.append('?');args.add(id);}where.append(')');
        if(savedOnly)where.append(" AND EXISTS (SELECT 1 FROM saved s WHERE s.scope=p.scope AND s.id=p.id)");
        else {where.append(" AND p.day=? AND p.start<=? AND p.end>?");args.add(day);args.add(Long.toString(now));args.add(Long.toString(now-7L*86400000));}
        if(!channel.isEmpty()){where.append(" AND p.channel=?");args.add(channel);}
        if(!genre.isEmpty()){where.append(" AND p.genre=?");args.add(genre);}
        if(!search.trim().isEmpty()){where.append(" AND instr(p.search,?)>0");args.add(search.trim().toLowerCase(java.util.Locale.ROOT));}
        long stage=System.nanoTime();
        SQLiteDatabase db=getReadableDatabase();String[] values=args.toArray(new String[0]);
        BlackBox.event(BlackBoxStore.Event.CATCHUP_DB_OPEN,op,BlackBox.elapsed(stage),0,0);
        ArchivePageTrace.cost("open",BlackBox.elapsed(stage));
        stage=System.nanoTime();
        try(Cursor c=db.rawQuery("SELECT count(*) FROM programs p WHERE "+where,values)){if(c.moveToFirst())page.total=c.getInt(0);}
        BlackBox.event(BlackBoxStore.Event.CATCHUP_DB_COUNT,op,BlackBox.elapsed(stage),page.total,0);
        ArchivePageTrace.cost("count",BlackBox.elapsed(stage));
        stage=System.nanoTime();long decodeNanos=0;
        int size=Math.max(1,Math.min(240,limit));page.offset=Math.min(Math.max(0,offset),Math.max(0,((page.total-1)/size)*size));
        try(Cursor c=db.rawQuery("SELECT p.payload FROM programs p WHERE "+where+" ORDER BY p.start DESC,p.id LIMIT "+size+" OFFSET "+page.offset,values)){
            while(c.moveToNext()) {
                String payload=c.getString(0);long decoding=System.nanoTime();
                try{page.programs.add(decode(new JSONObject(payload)));}catch(org.json.JSONException ignored){}
                finally{decodeNanos+=System.nanoTime()-decoding;}
            }
        }
        BlackBox.event(BlackBoxStore.Event.CATCHUP_DB_FETCH,op,Math.max(0,(System.nanoTime()-stage-decodeNanos)/1000000),page.programs.size(),0);
        ArchivePageTrace.cost("fetch",Math.max(0,(System.nanoTime()-stage-decodeNanos)/1000000));
        BlackBox.event(BlackBoxStore.Event.CATCHUP_DB_DECODE,op,decodeNanos/1000000,page.programs.size(),0);
        ArchivePageTrace.cost("decode",decodeNanos/1000000);
        return page;
    }
    long nextChange(String scope,long now) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT MIN(start) FROM programs WHERE scope=? AND start>?",new String[]{scope,Long.toString(now)})) {
            if(c.moveToFirst()&&!c.isNull(0))return c.getLong(0);
        }return Long.MAX_VALUE;
    }
    private final ArchiveGenreCache genreCache=new ArchiveGenreCache();
    List<String> cachedGenres(String scope,boolean allowExpired) {
        return allowExpired?genreCache.peek(scope):genreCache.get(scope,android.os.SystemClock.elapsedRealtime());
    }
    List<String> genres(String scope) {
        long now=android.os.SystemClock.elapsedRealtime();
        List<String> cached=genreCache.get(scope,now);
        if(cached!=null)return cached;
        List<String> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT DISTINCT genre FROM programs WHERE scope=? AND genre<>'' ORDER BY genre",new String[]{scope})) {
            while(c.moveToNext())out.add(c.getString(0));
        }
        genreCache.put(scope,now,out);
        return out;
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
    /** Read only the days needed by the visible archive window, using programs_channel. */
    List<EpgRepository.EpgProgram> readDays(String scope,String channel,java.util.Set<String> days) {
        List<EpgRepository.EpgProgram> out=new ArrayList<>();
        if(days.isEmpty())return out;
        List<String> args=new ArrayList<>();args.add(scope);args.add(channel);
        StringBuilder sql=new StringBuilder("SELECT payload FROM programs WHERE scope=? AND channel=? AND day IN (");
        for(String day:days){if(args.size()>2)sql.append(',');sql.append('?');args.add(day);}
        sql.append(") ORDER BY start");
        try(Cursor c=getReadableDatabase().rawQuery(sql.toString(),args.toArray(new String[0]))) {
            while(c.moveToNext())out.add(decode(new JSONObject(c.getString(0))));
        }catch(org.json.JSONException ignored){}
        return out;
    }
    List<EpgRepository.EpgProgram> recent(String scope,String channel,long from,long now,int limit) {
        long op=BlackBox.operation(),started=System.nanoTime();int count=0;boolean success=false;
        try {List<EpgRepository.EpgProgram> result=recentMeasured(scope,channel,from,now,limit);count=result.size();success=true;return result;}
        finally {BlackBox.event(BlackBoxStore.Event.CATCHUP_STRIP,op,BlackBox.elapsed(started),count,success?0:1);}
    }
    private List<EpgRepository.EpgProgram> recentMeasured(String scope,String channel,long from,long now,int limit) {
        List<EpgRepository.EpgProgram> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT payload FROM programs WHERE scope=? AND channel=? AND start<=? AND end>=? ORDER BY start DESC,id LIMIT ?",new String[]{scope,channel,Long.toString(now),Long.toString(from),Integer.toString(Math.min(40,Math.max(1,limit)))})) {
            while(c.moveToNext())try{out.add(decode(new JSONObject(c.getString(0))));}catch(org.json.JSONException ignored){}
        }return out;
    }
    void invalidate(String scope,String channel) { synchronized(writeLock) {
        getWritableDatabase().delete("catalog","scope=? AND channel=?",new String[]{scope,channel});
        getWritableDatabase().delete("programs","scope=? AND channel=? AND NOT EXISTS (SELECT 1 FROM saved s WHERE s.scope=programs.scope AND s.id=programs.id)",new String[]{scope,channel});
    } }
    boolean fresh(String scope,String channel,long now) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT updated FROM catalog WHERE scope=? AND channel=?",new String[]{scope,channel})) {
            return c.moveToFirst() && now>=c.getLong(0) && now-c.getLong(0)<300000L;
        }
    }
    void index(String scope,String channel,List<EpgRepository.EpgProgram> items) throws Exception { synchronized(writeLock) { store(scope,channel,items,false); } }
    void write(String scope,String channel,List<EpgRepository.EpgProgram> items) throws Exception { synchronized(writeLock) { store(scope,channel,items,true); } }
    private void store(String scope,String channel,List<EpgRepository.EpgProgram> items,boolean refreshed) throws Exception {
        SQLiteDatabase db=getWritableDatabase();db.beginTransactionNonExclusive();
        try {
            for(EpgRepository.EpgProgram p:items)if(ArchiveWindow.retained(p.startTime,System.currentTimeMillis()))putProgram(db,scope,p);
            if(refreshed)db.execSQL("INSERT OR REPLACE INTO catalog VALUES(?,?,?,?)",new Object[]{scope,channel,System.currentTimeMillis(),"[]"});
            db.execSQL("DELETE FROM programs WHERE scope=? AND end<? AND NOT EXISTS (SELECT 1 FROM saved s WHERE s.scope=programs.scope AND s.id=programs.id)",new Object[]{scope,System.currentTimeMillis()-8L*86400000});
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
    void indexAll(String scope,java.util.Map<String,List<EpgRepository.EpgProgram>> channels) throws Exception { synchronized(writeLock) {
        SQLiteDatabase db=getWritableDatabase();db.beginTransactionNonExclusive();
        try {for(java.util.Map.Entry<String,List<EpgRepository.EpgProgram>> channel:channels.entrySet())for(EpgRepository.EpgProgram p:channel.getValue())if(ArchiveWindow.retained(p.startTime,System.currentTimeMillis()))putProgram(db,scope,p);db.setTransactionSuccessful();}
        finally{db.endTransaction();}
    } }
    void writeAll(String scope, java.util.Map<String,List<EpgRepository.EpgProgram>> channels) throws Exception { synchronized(writeLock) {
        SQLiteDatabase db=getWritableDatabase();
        db.beginTransactionNonExclusive();
        try {
            for(java.util.Map.Entry<String,List<EpgRepository.EpgProgram>> channel:channels.entrySet())write(scope,channel.getKey(),channel.getValue());
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    } }
    boolean saved(String scope,String id) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM saved WHERE scope=? AND id=?",new String[]{scope,id})) { return c.moveToFirst(); }
    }
    void setSaved(String scope,EpgRepository.EpgProgram p,long expires,boolean desired) throws Exception {
        synchronized(savedLock) { synchronized(writeLock) {
            if(saved(scope,id(p))==desired)return;
            SQLiteDatabase db=getWritableDatabase();
            db.beginTransactionNonExclusive();
            try {
                if(desired) {
                    putProgram(db,scope,p);
                    db.execSQL("INSERT OR REPLACE INTO saved VALUES(?,?,?,?,?)",new Object[]{scope,id(p),p.channelId,expires,encode(p).toString()});
                } else db.delete("saved","scope=? AND id=?",new String[]{scope,id(p)});
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            savedRevision++;
        } }
    }
    List<EpgRepository.EpgProgram> savedPrograms(String scope) { synchronized(savedLock) {
        List<EpgRepository.EpgProgram> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT payload FROM saved WHERE scope=? ORDER BY expires DESC LIMIT 500",new String[]{scope})) {
            while(c.moveToNext()) out.add(decode(new JSONObject(c.getString(0))));
        } catch(Exception ignored) { }
        return new ArchiveSavedSnapshot<>(savedRevision,out);
    } }
}
