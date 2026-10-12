package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import com.drbep.tvplayer.playlists.PlaylistDatabase;
import java.lang.reflect.Constructor;
import org.json.*;
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class CompanionDatabaseDeviceTest {
 @Test public void upgradingV5PreservesHistoryAndPreferences()throws Exception{
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();String name="companion-v5-upgrade-test.db";context.deleteDatabase(name);
  try{
   SQLiteDatabase old=context.openOrCreateDatabase(name,0,null);old.execSQL("CREATE TABLE preferences(source TEXT,identity TEXT,occurrence INTEGER,favorite INTEGER,hidden INTEGER,resume_ms INTEGER,label TEXT,sort_order INTEGER)");old.execSQL("INSERT INTO preferences VALUES('source','identity',0,1,0,90000,'Mi episodio',7)");old.execSQL("CREATE TABLE sources(id TEXT,epg_url TEXT)");old.execSQL("CREATE TABLE guide_items(feed TEXT)");old.execSQL("CREATE TABLE programmes(source TEXT)");old.execSQL("CREATE TABLE guide_feeds(id TEXT PRIMARY KEY)");old.setVersion(5);old.close();
   Constructor<PlaylistDatabase> ctor=PlaylistDatabase.class.getDeclaredConstructor(Context.class,String.class);ctor.setAccessible(true);PlaylistDatabase helper=ctor.newInstance(context,name);
   try {SQLiteDatabase upgraded=helper.getWritableDatabase();assertEquals(7,upgraded.getVersion());try(android.database.Cursor c=upgraded.rawQuery("SELECT favorite,resume_ms,label,sort_order,watched FROM preferences",null)){assertTrue(c.moveToFirst());assertEquals(1,c.getInt(0));assertEquals(90000,c.getLong(1));assertEquals("Mi episodio",c.getString(2));assertEquals(7,c.getInt(3));assertEquals(0,c.getInt(4));}}finally{helper.close();}
  }finally{context.deleteDatabase(name);}
 }
 @Test public void officialArtworkLoadsAndIsReusableWithoutNetworkLookup()throws Exception {
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  okhttp3.OkHttpClient client=new okhttp3.OkHttpClient.Builder().callTimeout(15,java.util.concurrent.TimeUnit.SECONDS).build();
  String day=java.time.LocalDate.now(java.time.ZoneId.of("Europe/Madrid")).toString();
  JSONObject programme=null;
  try(okhttp3.Response response=client.newCall(new okhttp3.Request.Builder().url("https://ottcache.dof6.com/movistarplus/webplayer/OTT/epg?from="+day+"T00:00:00&span=2&channel=MPLUS&network=movistarplus&version=8.2&mdrm=true&tlsstream=true&demarcation=1").build()).execute()) {
   assertTrue(response.isSuccessful());JSONArray rows=new JSONArray(response.body().string());for(int i=0;i<rows.length();i++){JSONObject p=rows.getJSONObject(i);if(!MovistarArtworkRepository.horizontal(p).isEmpty()){programme=p;break;}}
  }
  assertNotNull(programme);long start=programme.getLong("FechaHoraInicio");String title=programme.getString("Titulo"),expected=MovistarArtworkRepository.horizontal(programme);
  MovistarArtworkRepository repo=new MovistarArtworkRepository(context.getCacheDir());
  try {java.util.concurrent.CompletableFuture<String> image=new java.util.concurrent.CompletableFuture<>();repo.request("MPLUS",start,title,image::complete);assertEquals(expected,image.get(20,java.util.concurrent.TimeUnit.SECONDS));assertEquals(expected,repo.cached("MPLUS",start,title));
   com.bumptech.glide.request.FutureTarget<android.graphics.Bitmap> target=com.bumptech.glide.Glide.with(context).asBitmap().load(expected).submit(480,270);
   try{android.graphics.Bitmap bitmap=target.get(15,java.util.concurrent.TimeUnit.SECONDS);assertTrue("Official artwork is horizontal",bitmap.getWidth()>bitmap.getHeight());}finally{com.bumptech.glide.Glide.with(context).clear(target);}
  }finally{repo.close();}
 }
 @Test public void prefersRealHorizontalArtworkAndRejectsNonHttps()throws Exception{
  assertEquals("https://example.com/wide.jpg",MovistarArtworkRepository.horizontal(new JSONObject("{\"Imagenes\":[{\"id\":\"vertical\",\"uri\":\"https://example.com/poster.jpg\"},{\"id\":\"horizontal\",\"uri\":\"https://example.com/wide.jpg\"}]}")));
  assertEquals("",MovistarArtworkRepository.horizontal(new JSONObject("{\"Imagenes\":[{\"id\":\"horizontal\",\"uri\":\"file:///secret\"}]}")));
 }
}
