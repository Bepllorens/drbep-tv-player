package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import android.content.Context;
import android.database.Cursor;
import androidx.test.platform.app.InstrumentationRegistry;
import com.drbep.tvplayer.playlists.PlaylistDatabase;
import java.lang.reflect.Constructor;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class GuideLifecycleDeviceTest {
 @Test public void sharedFeedSurvivesRemovalAndManualPolicySurvivesReimport()throws Exception {
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();String file="guide-lifecycle-isolated.db";c.deleteDatabase(file);
  Constructor<PlaylistDatabase> ctor=PlaylistDatabase.class.getDeclaredConstructor(Context.class,String.class);ctor.setAccessible(true);
  try(PlaylistDatabase db=ctor.newInstance(c,file)){
   String a=db.createSource("A","file"),b=db.createSource("B","file");db.setGuideUrl(a,"https://example.com/epg");db.setGuideUrl(b,"https://example.com/epg");
   String xml="<tv><programme channel='c' start='20990101120000 +0000' stop='20990101130000 +0000'><title>Title</title><desc>Info</desc></programme></tv>";
   db.importSharedGuide("feed","https://example.com/epg",new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),new PlaylistDatabase.Progress(){public void check(){}public void update(long n){}});
   db.deleteSource(a);db.maintainGuides(System.currentTimeMillis());try(Cursor rows=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM guide_feeds",null)){assertTrue(rows.moveToFirst());assertEquals(1,rows.getInt(0));}
   db.importReader(b,new StringReader("#EXTM3U url-tvg=\"https://wrong.example/epg\"\n#EXTINF:-1 tvg-id=\"c\",Channel\nhttps://example.com/live\n"));
   try(Cursor source=db.source(b)){assertTrue(source.moveToFirst());assertEquals("https://example.com/epg",source.getString(source.getColumnIndexOrThrow("epg_url")));assertEquals("manual",source.getString(source.getColumnIndexOrThrow("guide_mode")));}
   db.deleteSource(b);db.maintainGuides(System.currentTimeMillis());try(Cursor rows=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM guide_feeds",null)){assertTrue(rows.moveToFirst());assertEquals(0,rows.getInt(0));}
  }finally{c.deleteDatabase(file);}
 }
}
