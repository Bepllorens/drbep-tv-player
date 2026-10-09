package com.drbep.tvplayer.playlists;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.json.JSONObject;

/** Separate catalogue storage. Call imports/queries on a worker thread and close returned cursors. */
public final class PlaylistDatabase extends SQLiteOpenHelper {
    private static PlaylistDatabase instance;
    private final Object importLock=new Object();
    public static synchronized PlaylistDatabase getInstance(Context context) {
        if (instance == null) instance = new PlaylistDatabase(context);
        return instance;
    }
    private PlaylistDatabase(Context context) {
        super(context.getApplicationContext(), "personal-playlists.db", null, 4);
        setWriteAheadLoggingEnabled(true);
    }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE sources(id TEXT PRIMARY KEY,name TEXT NOT NULL,origin TEXT NOT NULL CHECK(origin IN ('file','url','server')),active TEXT,updated INTEGER NOT NULL DEFAULT 0,epg_url TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE entries(source TEXT NOT NULL REFERENCES sources(id) ON DELETE CASCADE,generation TEXT NOT NULL,identity TEXT NOT NULL,occurrence INTEGER NOT NULL,position INTEGER NOT NULL,kind TEXT NOT NULL,title TEXT NOT NULL,series TEXT NOT NULL,season INTEGER NOT NULL,episode INTEGER NOT NULL,group_name TEXT NOT NULL,logo TEXT NOT NULL,url TEXT NOT NULL,attributes TEXT NOT NULL,directives TEXT NOT NULL,PRIMARY KEY(source,generation,identity,occurrence))");
        db.execSQL("CREATE INDEX entries_page ON entries(source,generation,kind,position)");
        db.execSQL("CREATE INDEX entries_group ON entries(source,generation,group_name,kind,position)");
        db.execSQL("CREATE INDEX entries_series ON entries(source,generation,series,season,episode)");
        db.execSQL("CREATE TABLE preferences(source TEXT NOT NULL REFERENCES sources(id) ON DELETE CASCADE,identity TEXT NOT NULL,occurrence INTEGER NOT NULL,favorite INTEGER NOT NULL DEFAULT 0,hidden INTEGER NOT NULL DEFAULT 0,resume_ms INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(source,identity,occurrence))");
        migrateV2(db);
        migrateV3(db);
        migrateV4(db);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) migrateV2(db);
        if (oldVersion < 3) migrateV3(db);
        if (oldVersion < 4) migrateV4(db);
    }
    private void migrateV2(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE sources ADD COLUMN locator TEXT NOT NULL DEFAULT ''");
        db.execSQL("ALTER TABLE sources ADD COLUMN auto_refresh INTEGER NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE sources ADD COLUMN epg_active TEXT NOT NULL DEFAULT ''");
        db.execSQL("ALTER TABLE preferences ADD COLUMN label TEXT NOT NULL DEFAULT ''");
        db.execSQL("ALTER TABLE preferences ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0");
        db.execSQL("CREATE TABLE programmes(source TEXT NOT NULL REFERENCES sources(id) ON DELETE CASCADE,generation TEXT NOT NULL,channel TEXT NOT NULL,start_ms INTEGER NOT NULL,end_ms INTEGER NOT NULL,title TEXT NOT NULL,description TEXT NOT NULL,PRIMARY KEY(source,generation,channel,start_ms,end_ms))");
        db.execSQL("CREATE INDEX programme_window ON programmes(source,generation,channel,start_ms)");
    }
    private void migrateV3(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE sources ADD COLUMN preview TEXT");
        db.execSQL("ALTER TABLE sources ADD COLUMN feed_key TEXT NOT NULL DEFAULT ''");
        db.execSQL("CREATE TABLE guide_feeds(id TEXT PRIMARY KEY,url TEXT NOT NULL,active TEXT NOT NULL DEFAULT '',etag TEXT NOT NULL DEFAULT '',modified TEXT NOT NULL DEFAULT '',checked INTEGER NOT NULL DEFAULT 0,coverage INTEGER NOT NULL DEFAULT 0,count INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE guide_items(feed TEXT NOT NULL REFERENCES guide_feeds(id) ON DELETE CASCADE,generation TEXT NOT NULL,channel TEXT NOT NULL,start_ms INTEGER NOT NULL,end_ms INTEGER NOT NULL,title TEXT NOT NULL,description TEXT NOT NULL,PRIMARY KEY(feed,generation,channel,start_ms,end_ms))");
        db.execSQL("CREATE INDEX guide_items_window ON guide_items(feed,generation,channel,start_ms)");
        db.execSQL("CREATE TABLE import_jobs(id TEXT PRIMARY KEY,source TEXT NOT NULL,kind TEXT NOT NULL,file TEXT NOT NULL DEFAULT '',state TEXT NOT NULL,label TEXT NOT NULL,count INTEGER NOT NULL DEFAULT 0,bytes INTEGER NOT NULL DEFAULT 0,updated INTEGER NOT NULL)");
    }
    private void migrateV4(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE movie_added(source TEXT NOT NULL REFERENCES sources(id) ON DELETE CASCADE,identity TEXT NOT NULL,occurrence INTEGER NOT NULL,added INTEGER NOT NULL,PRIMARY KEY(source,identity,occurrence))");
        db.execSQL("INSERT OR IGNORE INTO movie_added SELECT e.source,e.identity,e.occurrence,s.updated FROM entries e JOIN sources s ON s.id=e.source WHERE e.kind='movie'");
        db.execSQL("CREATE TABLE movie_metadata(query TEXT PRIMARY KEY,json TEXT NOT NULL,updated INTEGER NOT NULL,release_date TEXT NOT NULL DEFAULT '',rating REAL NOT NULL DEFAULT 0,genres TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE movie_match(source TEXT NOT NULL REFERENCES sources(id) ON DELETE CASCADE,identity TEXT NOT NULL,occurrence INTEGER NOT NULL,query TEXT NOT NULL,manual INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(source,identity,occurrence))");
        db.execSQL("CREATE INDEX movie_match_query ON movie_match(query)");
    }
    public interface Progress {
        void check() throws Exception;
        void update(long count) throws Exception;
    }
    private static final Progress NO_PROGRESS = new Progress(){public void check() throws Exception {if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();} public void update(long count) {}};
    public long importSharedGuide(String key,String url,InputStream input,Progress progress) throws Exception {
        synchronized(importLock) {
            SQLiteDatabase db=getWritableDatabase();
            ContentValues seed=new ContentValues();seed.put("id",key);seed.put("url",url);
            db.insertWithOnConflict("guide_feeds",null,seed,SQLiteDatabase.CONFLICT_IGNORE);
            String generation=UUID.randomUUID().toString();long[] count={0},coverage={0};boolean[] complete={false};
            try(SQLiteStatement insert=db.compileStatement("INSERT OR REPLACE INTO guide_items VALUES(?,?,?,?,?,?,?)")) {
                insert.bindString(1,key);insert.bindString(2,generation);db.beginTransaction();
                try {
                    new XmltvStreamReader().read(input,p -> {
                        progress.check();insert.bindString(3,p.channel);insert.bindLong(4,p.start);insert.bindLong(5,p.end);
                        insert.bindString(6,p.title);insert.bindString(7,p.description);insert.executeInsert();coverage[0]=Math.max(coverage[0],p.end);
                        if(++count[0]%1000==0){db.setTransactionSuccessful();db.endTransaction();progress.update(count[0]);db.beginTransaction();}
                    });
                    progress.check();if(count[0]==0)throw new IOException("Empty guide");
                    ContentValues v=new ContentValues();v.put("active",generation);v.put("coverage",coverage[0]);v.put("count",count[0]);
                    db.update("guide_feeds",v,"id=?",new String[]{key});db.setTransactionSuccessful();complete[0]=true;
                } finally {if(db.inTransaction())db.endTransaction();}
            } finally {
                db.delete("guide_items",complete[0]?"feed=? AND generation<>?":"feed=? AND generation=?",new String[]{key,generation});
            }
            progress.update(count[0]);return count[0];
        }
    }
    public synchronized void configure(String id,String name,String locator,boolean auto) {
        ContentValues v=new ContentValues();v.put("name",name);v.put("locator",locator);v.put("auto_refresh",auto?1:0);
        getWritableDatabase().update("sources",v,"id=?",new String[]{id});
    }
    public Cursor source(String id) { return getReadableDatabase().rawQuery("SELECT * FROM sources WHERE id=?",new String[]{id}); }
    public Cursor dueSources(long before) { return getReadableDatabase().rawQuery("SELECT id,name,locator,origin FROM sources WHERE auto_refresh=1 AND (origin='server' OR locator<>'') AND updated<?",new String[]{Long.toString(before)}); }
    public Cursor browse(String id,String kind,String query,boolean favorites,boolean hidden,int offset,int limit) {
        return getReadableDatabase().rawQuery("SELECT e.*,COALESCE(p.favorite,0) AS favorite,COALESCE(p.hidden,0) AS hidden,COALESCE(NULLIF(p.label,''),e.title) AS display_title FROM entries e JOIN sources s ON s.id=e.source AND COALESCE(s.active,s.preview)=e.generation LEFT JOIN preferences p ON p.source=e.source AND p.identity=e.identity AND p.occurrence=e.occurrence WHERE e.source=? AND (?='' OR e.kind=?) AND (?='' OR instr(lower(e.title),lower(?))>0) AND (?='0' OR p.favorite=1) AND (?='1' OR COALESCE(p.hidden,0)=0) ORDER BY COALESCE(p.sort_order,0),e.position LIMIT ? OFFSET ?",new String[]{id,kind,kind,query,query,favorites?"1":"0",hidden?"1":"0",Integer.toString(Math.min(100,Math.max(1,limit))),Integer.toString(Math.max(0,offset))});
    }
    public Cursor preferenceState(String source,String identity,int occurrence) { return getReadableDatabase().rawQuery("SELECT label,sort_order FROM preferences WHERE source=? AND identity=? AND occurrence=?",new String[]{source,identity,Integer.toString(occurrence)}); }
    public synchronized void setGuideUrl(String source,String url) { ContentValues v=new ContentValues();v.put("epg_url",url);v.put("feed_key","");getWritableDatabase().update("sources",v,"id=?",new String[]{source}); }
    public synchronized void preference(String source,String identity,int occurrence,boolean favorite,boolean hidden,String label,int order) {
        SQLiteDatabase db=getWritableDatabase();ContentValues seed=new ContentValues();seed.put("source",source);seed.put("identity",identity);seed.put("occurrence",occurrence);
        db.insertWithOnConflict("preferences",null,seed,SQLiteDatabase.CONFLICT_IGNORE);
        ContentValues v=new ContentValues();v.put("favorite",favorite?1:0);v.put("hidden",hidden?1:0);v.put("label",label);v.put("sort_order",order);
        db.update("preferences",v,"source=? AND identity=? AND occurrence=?",new String[]{source,identity,Integer.toString(occurrence)});
    }
    public long importGuide(String source,InputStream input) throws Exception {
        synchronized(importLock) {
        SQLiteDatabase db=getWritableDatabase();String generation=UUID.randomUUID().toString();long[] count={0};
        db.beginTransaction();
        try {
            new XmltvStreamReader().read(input,p -> {
                ContentValues v=new ContentValues();v.put("source",source);v.put("generation",generation);v.put("channel",p.channel);v.put("start_ms",p.start);v.put("end_ms",p.end);v.put("title",p.title);v.put("description",p.description);
                db.insertWithOnConflict("programmes",null,v,SQLiteDatabase.CONFLICT_REPLACE);
                if(++count[0]%300==0){db.setTransactionSuccessful();db.endTransaction();db.beginTransaction();}
            });
            if(count[0]==0||Thread.currentThread().isInterrupted())throw new IOException("Empty or cancelled guide");
            ContentValues v=new ContentValues();v.put("epg_active",generation);db.update("sources",v,"id=?",new String[]{source});db.setTransactionSuccessful();
        } finally { if(db.inTransaction())db.endTransaction(); }
        db.delete("programmes","source=? AND generation<>?",new String[]{source,generation});return count[0];
    }
    }
    public Cursor guide(String source,String channel,long start,long end,int offset) {
        try(Cursor feed=getReadableDatabase().rawQuery("SELECT feed_key FROM sources WHERE id=?",new String[]{source})) {
            if(feed.moveToFirst()&&!feed.getString(0).isEmpty())return getReadableDatabase().rawQuery("SELECT p.* FROM guide_items p JOIN guide_feeds f ON f.id=p.feed AND f.active=p.generation WHERE p.feed=? AND p.channel=? AND p.end_ms>? AND p.start_ms<? ORDER BY p.start_ms LIMIT 51 OFFSET ?",new String[]{feed.getString(0),channel,Long.toString(start),Long.toString(end),Integer.toString(Math.max(0,offset))});
        }
        return getReadableDatabase().rawQuery("SELECT p.* FROM programmes p JOIN sources s ON s.id=p.source AND s.epg_active=p.generation WHERE p.source=? AND p.channel=? AND p.end_ms>? AND p.start_ms<? ORDER BY p.start_ms LIMIT 51 OFFSET ?",new String[]{source,channel,Long.toString(start),Long.toString(end),Integer.toString(Math.max(0,offset))});
    }
    public synchronized String createSource(String name, String origin) {
        String id = UUID.randomUUID().toString();
        ContentValues v = new ContentValues(); v.put("id", id); v.put("name", name); v.put("origin", origin);
        getWritableDatabase().insertOrThrow("sources", null, v);
        return id;
    }
    /** Reader ownership stays with caller. Active generation changes only after complete validation. */
    public M3uStreamReader.Result importReader(String source, Reader reader) throws Exception {
        return importReader(source,reader,NO_PROGRESS);
    }
    public M3uStreamReader.Result importReader(String source, Reader reader, Progress progress) throws Exception {
        synchronized(importLock) {
        SQLiteDatabase db = getWritableDatabase();
        try (Cursor c = db.rawQuery("SELECT id FROM sources WHERE id=?", new String[]{source})) {
            if (!c.moveToFirst()) throw new IllegalArgumentException("Unknown source");
        }
        // Recover staging rows from a previous interrupted import; keep active data/preferences.
        db.execSQL("DELETE FROM entries WHERE source=? AND generation<>COALESCE((SELECT active FROM sources WHERE id=?),'')", new Object[]{source, source});
        ContentValues resetPreview=new ContentValues();resetPreview.putNull("preview");db.update("sources",resetPreview,"id=?",new String[]{source});
        String generation = UUID.randomUUID().toString();
        long[] count = {0};boolean[] completed={false};
        try(SQLiteStatement duplicate=db.compileStatement("SELECT COUNT(*) FROM entries WHERE source=? AND generation=? AND identity=?");
            SQLiteStatement insert=db.compileStatement("INSERT INTO entries(source,generation,identity,occurrence,position,kind,title,series,season,episode,group_name,logo,url,attributes,directives) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
        duplicate.bindString(1,source);duplicate.bindString(2,generation);insert.bindString(1,source);insert.bindString(2,generation);
        db.beginTransaction();
        try {
            M3uStreamReader.Result result = new M3uStreamReader().read(reader, entry -> {
                progress.check();
                String identity = identity(entry);
                duplicate.bindString(3,identity);long occurrence=duplicate.simpleQueryForLong();
                insert.bindString(3,identity);insert.bindLong(4,occurrence);insert.bindLong(5,count[0]);
                insert.bindString(6,entry.kind);insert.bindString(7,entry.title);insert.bindString(8,entry.series);
                insert.bindLong(9,entry.season);insert.bindLong(10,entry.episode);
                insert.bindString(11,entry.attributes.getOrDefault("group-title", ""));insert.bindString(12,entry.attributes.getOrDefault("tvg-logo", ""));insert.bindString(13,entry.url);
                insert.bindString(14,new JSONObject(entry.attributes).toString());insert.bindString(15,new JSONObject(entry.directives).toString());
                insert.executeInsert();
                if ("movie".equals(entry.kind)) db.execSQL("INSERT OR IGNORE INTO movie_added VALUES(?,?,?,?)",new Object[]{source,identity,occurrence,System.currentTimeMillis()});
                if (++count[0] % 500 == 0) {
                    ContentValues preview=new ContentValues();preview.put("preview",generation);
                    db.update("sources",preview,"id=? AND active IS NULL",new String[]{source});
                    db.setTransactionSuccessful();db.endTransaction();progress.update(count[0]);db.beginTransaction();
                }
            });
            if (result.imported == 0 || result.rejected > 0) throw new IOException("Incomplete or empty playlist; previous catalogue retained");
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Import cancelled");
            progress.check();
            ContentValues active = new ContentValues(); active.putNull("preview");active.put("active",generation); active.put("updated",System.currentTimeMillis()); if (!result.epgUrl.isEmpty()) active.put("epg_url",result.epgUrl);
            if(!result.epgUrl.isEmpty())db.execSQL("UPDATE sources SET feed_key='' WHERE id=? AND epg_url<>?",new Object[]{source,result.epgUrl});
            db.update("sources",active,"id=?",new String[]{source});
            db.setTransactionSuccessful();completed[0]=true;
            return result;
        } finally {
            if (db.inTransaction()) db.endTransaction();
            if(!completed[0]) {
                db.update("sources",resetPreview,"id=?",new String[]{source});
                db.delete("entries","source=? AND generation=?",new String[]{source,generation});
            }
            // Active catalogue and preferences survive failed refreshes.
        }
    }
    }
    }
    /** Keyset page: pass the last position returned, or -1 for the first page. */
    public Cursor page(String source, String kind, long afterPosition, int requestedLimit) {
        int limit = Math.max(1, Math.min(200, requestedLimit));
        return getReadableDatabase().rawQuery("SELECT e.* FROM entries e JOIN sources s ON s.id=e.source AND COALESCE(s.active,s.preview)=e.generation WHERE e.source=? AND e.kind=? AND e.position>? ORDER BY e.position LIMIT ?",new String[]{source,kind,Long.toString(afterPosition),Integer.toString(limit)});
    }
    public Cursor sources() {
        return getReadableDatabase().rawQuery("SELECT id,name,origin,updated FROM sources WHERE COALESCE(active,preview) IS NOT NULL ORDER BY name,id", null);
    }
    public static String serverSourceId(String identity) { return "server-" + UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)); }
    public synchronized String serverSource(String serverIdentity,String name) {
        String id = serverSourceId(serverIdentity);
        ContentValues v = new ContentValues(); v.put("id",id); v.put("name",name); v.put("origin","server");
        getWritableDatabase().insertWithOnConflict("sources",null,v,SQLiteDatabase.CONFLICT_IGNORE);
        return id;
    }
    public synchronized void deleteUnpublishedSource(String id) {
        getWritableDatabase().delete("sources","id=? AND active IS NULL",new String[]{id});
    }
    public synchronized void deleteSource(String id) {
        getWritableDatabase().delete("sources", "id=?", new String[]{id});
    }
    public Cursor series(String source, String afterName, int limit) {
        return getReadableDatabase().rawQuery("SELECT e.series,COUNT(*) AS episodes,MIN(e.logo) AS logo FROM entries e JOIN sources s ON s.id=e.source AND COALESCE(s.active,s.preview)=e.generation WHERE e.source=? AND e.kind='series' AND NOT EXISTS(SELECT 1 FROM preferences p WHERE p.source=e.source AND p.identity=e.identity AND p.occurrence=e.occurrence AND p.hidden=1) AND e.series>? GROUP BY e.series ORDER BY e.series LIMIT ?", new String[]{source,afterName,Integer.toString(Math.max(1,Math.min(100,limit)))});
    }
    public Cursor episodes(String source, String series, long afterOffset, int limit) {
        return getReadableDatabase().rawQuery("SELECT e.* FROM entries e JOIN sources s ON s.id=e.source AND COALESCE(s.active,s.preview)=e.generation WHERE e.source=? AND e.series=? AND e.kind='series' AND NOT EXISTS(SELECT 1 FROM preferences p WHERE p.source=e.source AND p.identity=e.identity AND p.occurrence=e.occurrence AND p.hidden=1) ORDER BY e.season,e.episode,e.position LIMIT ? OFFSET ?", new String[]{source,series,Integer.toString(Math.max(1,Math.min(100,limit))),Long.toString(Math.max(0,afterOffset+1))});
    }
    private static String identity(M3uStreamReader.Entry e) throws Exception {
        // URLs frequently contain expiring tokens. Keep them out of identity.
        String basis = e.kind + "\u0000" + e.attributes.getOrDefault("group-title", "") + "\u0000" + e.attributes.getOrDefault("tvg-id", "") + "\u0000" + e.title + "\u0000" + e.series + "\u0000" + e.season + ":" + e.episode;
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(basis.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(64);
        for (byte b : digest) { int value=b & 255; hex.append("0123456789abcdef".charAt(value >>> 4)); hex.append("0123456789abcdef".charAt(value & 15)); }
        return hex.toString();
    }
}
