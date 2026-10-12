package com.drbep.tvplayer;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.drbep.tvplayer.playlists.PlaylistDatabase;
import java.lang.reflect.Constructor;
import org.junit.Test;

/** Covers first use on a device with no imported lists without changing its real catalogue. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class EmptyImportedLibraryDeviceTest {
 @Test public void freshDatabaseAndEmptyScreens() throws Exception {
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  Context isolated=context.createDeviceProtectedStorageContext();
  String name="empty-library-regression.db";
  Constructor<PlaylistDatabase> ctor=PlaylistDatabase.class.getDeclaredConstructor(Context.class,String.class);
  ctor.setAccessible(true);
  PlaylistDatabase db=ctor.newInstance(isolated,name);
  try {
   SQLiteDatabase sql=db.getWritableDatabase();
   assertEquals(6,sql.getVersion());
   for(String table:new String[]{"entries","preferences","movie_metadata","series_match"})
    try(android.database.Cursor c=sql.rawQuery("SELECT COUNT(*) FROM "+table,null)){assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0));}
   try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
    Thread.sleep(15000);
    android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    android.accessibilityservice.AccessibilityServiceInfo service=instrumentation.getUiAutomation().getServiceInfo();
    service.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
    instrumentation.getUiAutomation().setServiceInfo(service);
    for(boolean series:new boolean[]{false,true}) {
     scenario.onActivity(a->{try {
      PersonalPlaylistController controller=(PersonalPlaylistController)CatchupFocusDeviceTest.call(a,"personalPlaylists");
      ImportedMovieRepository repo=new ImportedMovieRepository(a,controller,"",java.util.Collections.emptyMap(),c->true,series);
      java.lang.reflect.Field field=ImportedMovieRepository.class.getDeclaredField("db");field.setAccessible(true);field.set(repo,db);
      java.util.Map<String,Long> stale=new java.util.HashMap<>();
      stale.put("m3u:missing-source:missing-entry:0",90000L);
      repo.syncProgress(stale);
      try(android.database.Cursor c=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM preferences",null)){assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0));}
      assertEquals(0,repo.page(new MovieFilter("","","","","added"),0).getTotal());
      assertTrue(repo.pending("").isEmpty());
      ComposeDialogHost.dismissAll(a);
      java.lang.reflect.Method method=MainActivity.class.getDeclaredMethod("showImportedLibrary",String.class,boolean.class,Runnable.class);
      method.setAccessible(true);method.invoke(a,"missing-list-regression",series,(Runnable)()->{});
     } catch(Exception e){throw new AssertionError(e);}});
     Thread.sleep(3500);
     scenario.onActivity(a->{try {
      java.lang.reflect.Field open=ComposeDialogHost.class.getDeclaredField("OPEN");open.setAccessible(true);
      java.util.List<?> dialogs=(java.util.List<?>)open.get(null);
      assertFalse("Empty screen remains open",dialogs.isEmpty());
      android.app.Dialog last=(android.app.Dialog)((java.lang.ref.WeakReference<?>)dialogs.get(dialogs.size()-1)).get();
      assertNotNull(last);assertTrue(last.isShowing());
     } catch(Exception e){throw new AssertionError(e);}});
     android.graphics.Bitmap shot=instrumentation.getUiAutomation().takeScreenshot();
     try(java.io.FileOutputStream out=context.openFileOutput(series?"empty-series.png":"empty-movies.png",0)){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{shot.recycle();}
    }
   }
  } finally {db.close();isolated.deleteDatabase(name);}
 }
 @Test public void validProgressSurvivesMissingAndDeletedSources() throws Exception {
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  String name="resume-library-regression.db";
  Constructor<PlaylistDatabase> ctor=PlaylistDatabase.class.getDeclaredConstructor(Context.class,String.class);
  ctor.setAccessible(true);
  PlaylistDatabase db=ctor.newInstance(context,name);
  try {
   String source=db.createSource("Resume regression","file");
   db.importReader(source,new java.io.StringReader("#EXTM3U\n#EXTINF:-1 type=\"movie\",Movie\nhttps://example.test/movie\n#EXTINF:-1 type=\"series\" serie-title=\"Series\",Series S01E01\nhttps://example.test/episode\n"));
   PersonalPlaylistController controller=new PersonalPlaylistController(context,null);
   controller.close();
   ImportedMovieRepository repo=new ImportedMovieRepository(context,controller,"",java.util.Collections.emptyMap(),c->true,false);
   java.lang.reflect.Field field=ImportedMovieRepository.class.getDeclaredField("db");field.setAccessible(true);field.set(repo,db);
   java.util.Map<String,Long> progress=new java.util.HashMap<>();
   try(android.database.Cursor c=db.getReadableDatabase().rawQuery("SELECT identity,occurrence FROM entries WHERE source=?",new String[]{source})) {
    while(c.moveToNext())progress.put("m3u:"+source+":"+c.getString(0)+":"+c.getInt(1),120000L);
   }
   progress.put("m3u:absent-source:absent-entry:0",60000L);
   repo.syncProgress(progress);
   try(android.database.Cursor c=db.getReadableDatabase().rawQuery("SELECT COUNT(*),MIN(resume_ms) FROM preferences",null)){assertTrue(c.moveToFirst());assertEquals(2,c.getInt(0));assertEquals(120000L,c.getLong(1));}
   db.deleteSource(source);
   repo.syncProgress(progress);
   try(android.database.Cursor c=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM preferences",null)){assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0));}
  } finally {db.close();context.deleteDatabase(name);}
 }
}
