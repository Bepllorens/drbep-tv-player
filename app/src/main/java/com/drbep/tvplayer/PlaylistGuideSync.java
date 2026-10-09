package com.drbep.tvplayer;

import android.content.ContentValues;
import android.database.Cursor;
import com.drbep.tvplayer.playlists.PlaylistDatabase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import okhttp3.*;

/** Cache identity includes the complete URL, including authorization-bearing query parameters. */
final class PlaylistGuideSync {
    static String key(String url) throws Exception {
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
        StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();
    }
    static synchronized String refresh(PlaylistDatabase db,String source,PlaylistDatabase.Progress progress) throws Exception {
        String url;
        try(Cursor c=db.source(source)){if(!c.moveToFirst())throw new IOException("Missing source");url=c.getString(c.getColumnIndexOrThrow("epg_url"));}
        HttpUrl parsed=HttpUrl.parse(url);if(parsed==null)throw new IOException("Missing guide URL");
        String id=key(url),etag="",modified="";long checked=0,coverage=0,cachedCount=0;boolean cached=false;
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT f.etag,f.modified,f.checked,f.coverage,EXISTS(SELECT 1 FROM guide_items p WHERE p.feed=f.id AND p.generation=f.active),f.count FROM guide_feeds f WHERE f.id=?",new String[]{id})){
            if(c.moveToFirst()){etag=c.getString(0);modified=c.getString(1);checked=c.getLong(2);coverage=c.getLong(3);cached=c.getInt(4)==1;cachedCount=c.getLong(5);}
        }
        progress.check();long now=System.currentTimeMillis();
        boolean covered=cached&&coverage>now+6*3600000L;
        if(covered&&now>=checked&&now-checked<15*60000L){progress.update(cachedCount);attach(db,source,url,id);return "Guía reciente reutilizada";}
        Request.Builder request=new Request.Builder().url(parsed);
        if(covered){if(!etag.isEmpty())request.header("If-None-Match",etag);if(!modified.isEmpty())request.header("If-Modified-Since",modified);}
        // No DRBEP credentials are attached to third-party guide requests.
        try(Response response=NetworkClients.withTimeouts(15000,30000).newCall(request.build()).execute()) {
            progress.check();
            if(response.code()==304&&covered){
                progress.update(cachedCount);
                ContentValues v=new ContentValues();v.put("checked",now);db.getWritableDatabase().update("guide_feeds",v,"id=?",new String[]{id});
                attach(db,source,url,id);return "Guía sin cambios";
            }
            if(!response.isSuccessful()||response.body()==null)throw new IOException("Guide download failed");
            try(InputStream input=decode(response.body().byteStream())){db.importSharedGuide(id,url,input,progress);}
            ContentValues v=new ContentValues();v.put("etag",value(response.header("ETag")));v.put("modified",value(response.header("Last-Modified")));v.put("checked",now);
            db.getWritableDatabase().update("guide_feeds",v,"id=?",new String[]{id});attach(db,source,url,id);return "Guía actualizada";
        }
    }
    private static String value(String s){return s==null?"":s;}
    private static void attach(PlaylistDatabase db,String source,String url,String key) {
        ContentValues v=new ContentValues();v.put("feed_key",key);
        db.getWritableDatabase().update("sources",v,"id=? AND epg_url=?",new String[]{source,url});
    }
    static InputStream decode(InputStream input)throws IOException {
        PushbackInputStream peek=new PushbackInputStream(input,2);int a=peek.read(),b=peek.read();if(b>=0)peek.unread(b);if(a>=0)peek.unread(a);
        return new HttpClient.LimitedInputStream(a==31&&b==139?new java.util.zip.GZIPInputStream(peek):peek,256*1024*1024);
    }
}
