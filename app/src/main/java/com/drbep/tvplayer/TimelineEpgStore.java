package com.drbep.tvplayer;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

/** Separate, disposable EPG cache. Never migrates the user's catalogue. */
final class TimelineEpgStore extends SQLiteOpenHelper {
    TimelineEpgStore(Context context) { this(context, "timeline-epg-v1.db"); }
    TimelineEpgStore(Context context, String name) { super(context.getApplicationContext()==null?context:context.getApplicationContext(), name, null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE programmes (channel TEXT NOT NULL, start INTEGER NOT NULL, end INTEGER NOT NULL, name TEXT, tvg TEXT, title TEXT, icon TEXT, description TEXT, category TEXT, PRIMARY KEY(channel,start))");
        db.execSQL("CREATE INDEX programme_window ON programmes(channel,end,start)");
        db.execSQL("CREATE TABLE refreshed (channel TEXT PRIMARY KEY, time INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int old,int next) { }
    static String key(ChannelItem c) { return c.id+"|"+c.platformId+"|"+c.tvgId; }
    synchronized List<EpgRepository.EpgProgram> read(ChannelItem c,long from,long to) {
        return readWindow(c, from, to, 0);
    }
    synchronized List<EpgRepository.EpgProgram> readUpcoming(ChannelItem c,long from,long to,int limit) {
        if (limit < 1 || limit > 240) throw new IllegalArgumentException("Invalid guide limit");
        return readWindow(c, from, to, limit);
    }
    private List<EpgRepository.EpgProgram> readWindow(ChannelItem c,long from,long to,int limit) {
        List<EpgRepository.EpgProgram> result=new ArrayList<>();
        String bounded = limit > 0 ? " AND end>start" : "";
        String sql="SELECT start,end,name,tvg,title,icon,description,category FROM programmes WHERE channel=? AND end>? AND start<?"+bounded+" ORDER BY start"+(limit>0?" LIMIT ?":"");
        String[] args=limit>0?new String[]{key(c),Long.toString(from),Long.toString(to),Integer.toString(limit)}:new String[]{key(c),Long.toString(from),Long.toString(to)};
        try(Cursor cur=getReadableDatabase().rawQuery(sql,args)) {
            while(cur.moveToNext()) result.add(new EpgRepository.EpgProgram(c.id,cur.getString(2),cur.getString(3),cur.getString(4),cur.getString(5),cur.getString(6),java.time.Instant.ofEpochMilli(cur.getLong(0)).toString(),java.time.Instant.ofEpochMilli(cur.getLong(1)).toString(),cur.getString(7),0));
        } return result;
    }
    synchronized boolean fresh(ChannelItem c) {
        try(Cursor cur=getReadableDatabase().rawQuery("SELECT time FROM refreshed WHERE channel=?",new String[]{key(c)})) { return cur.moveToFirst()&&System.currentTimeMillis()-cur.getLong(0)<15*60000L; }
    }
    synchronized void save(ChannelItem c,List<EpgRepository.EpgProgram> rows) {
        if(rows==null||rows.isEmpty())return;
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            long now=System.currentTimeMillis();
            long first=Long.MAX_VALUE,last=Long.MIN_VALUE;
            for(EpgRepository.EpgProgram p:rows) {
                long start=EpgTimeCodec.parseEpochMillis(p.startTime),end=EpgTimeCodec.parseEpochMillis(p.endTime);
                if(end>start&&end>=now-8*86400000L&&start<=now+30*86400000L){first=Math.min(first,start);last=Math.max(last,end);}
            }
            // Replace the supplied interval atomically: changed start times must not leave duplicate broadcasts.
            if(first<last)db.delete("programmes","channel=? AND start>=? AND start<?",new String[]{key(c),Long.toString(first),Long.toString(last)});
            for(EpgRepository.EpgProgram p:rows) {
                long start=EpgTimeCodec.parseEpochMillis(p.startTime),end=EpgTimeCodec.parseEpochMillis(p.endTime);
                if(end<=start||end<now-8*86400000L||start>now+30*86400000L)continue;
                ContentValues v=new ContentValues();v.put("channel",key(c));v.put("start",start);v.put("end",end);v.put("name",p.channelName);v.put("tvg",p.tvgId);v.put("title",p.title);v.put("icon",p.icon);v.put("description",p.description);v.put("category",p.category);
                db.insertWithOnConflict("programmes",null,v,SQLiteDatabase.CONFLICT_REPLACE);
            }
            ContentValues stamp=new ContentValues();stamp.put("channel",key(c));stamp.put("time",now);db.insertWithOnConflict("refreshed",null,stamp,SQLiteDatabase.CONFLICT_REPLACE);
            db.delete("programmes","end<? OR start>?",new String[]{Long.toString(now-8*86400000L),Long.toString(now+30*86400000L)});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
}
