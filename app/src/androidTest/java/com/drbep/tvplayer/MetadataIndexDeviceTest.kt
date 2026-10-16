package com.drbep.tvplayer

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drbep.tvplayer.playlists.PlaylistDatabase
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.StringReader

@RunWith(AndroidJUnit4::class)
class MetadataIndexDeviceTest {
    @Test fun upgradeAndReopenKeepCatalogueAndQueueProgress() {
        val ctx=InstrumentationRegistry.getInstrumentation().targetContext
        val name="metadata-index-upgrade-fixture.db"
        ctx.deleteDatabase(name)
        val constructor=PlaylistDatabase::class.java.getDeclaredConstructor(Context::class.java,String::class.java)
        constructor.isAccessible=true
        var db=constructor.newInstance(ctx,name)
        try {
            val source=db.createSource("Upgrade fixture","file")
            db.importReader(source,StringReader("#EXTM3U\n#EXTINF:-1 type=\"movie\",Fixture\nhttps://example.test/movie\n"))
            db.writableDatabase.execSQL("DROP TABLE metadata_index")
            db.writableDatabase.version=7
            db.close()
            db=constructor.newInstance(ctx,name)
            val repo=ImportedMovieRepository(ctx,null,"http://127.0.0.1:1",emptyMap(),{true},false,db)
            repo.queueIndex()
            db.writableDatabase.execSQL("UPDATE metadata_index SET state='done'")
            db.close()
            db=constructor.newInstance(ctx,name)
            db.readableDatabase.rawQuery("SELECT state FROM metadata_index",null).use {assertTrue(it.moveToFirst());assertEquals("done",it.getString(0))}
            db.readableDatabase.rawQuery("SELECT COUNT(*) FROM entries",null).use {assertTrue(it.moveToFirst());assertEquals(1,it.getInt(0))}
        } finally { db.close();ctx.deleteDatabase(name) }
    }
    @Test fun queueCacheManualCorrectionsAndGenerationArePreserved() {
        val ctx=InstrumentationRegistry.getInstrumentation().targetContext
        val name="metadata-index-fixture.db"
        ctx.deleteDatabase(name)
        val constructor=PlaylistDatabase::class.java.getDeclaredConstructor(Context::class.java,String::class.java)
        constructor.isAccessible=true
        val db=constructor.newInstance(ctx,name)
        try {
            val source=db.createSource("Synthetic fixture","file")
            db.importReader(source,StringReader("""#EXTM3U
#EXTINF:-1 type="movie",Synthetic movie (2025)
https://example.test/movie
#EXTINF:-1 type="series" serie-title="Synthetic series",Synthetic series - S01E01
https://example.test/e1
#EXTINF:-1 type="series" serie-title="Synthetic series",Synthetic series - S01E02
https://example.test/e2
"""))
            val movie=ImportedMovieRepository(ctx,null,"http://127.0.0.1:1",emptyMap(),{true},false,db)
            val series=ImportedMovieRepository(ctx,null,"http://127.0.0.1:1",emptyMap(),{true},true,db)
            movie.queueIndex();series.queueIndex();movie.queueIndex();series.queueIndex()
            db.readableDatabase.rawQuery("SELECT COUNT(*) FROM metadata_index",null).use{assertTrue(it.moveToFirst());assertEquals(2,it.getInt(0))}
            val entry=db.readableDatabase.rawQuery("SELECT source,generation,identity,occurrence FROM metadata_index WHERE kind='movie'",null).use{
                assertTrue(it.moveToFirst());listOf(it.getString(0),it.getString(1),it.getString(2),it.getString(3))
            }
            val item=movie.indexItem(entry[0],entry[1],entry[2],entry[3].toInt())!!
            fun cache(key:String,title:String) {
                val json=JSONObject().put("matched",true).put("movie",JSONObject().put("title",title)).toString()
                db.writableDatabase.execSQL("INSERT OR REPLACE INTO movie_metadata(query,json,updated) VALUES(?,?,?)",arrayOf(key,json,System.currentTimeMillis()))
            }
            cache(item.query,"Automatic fixture")
            assertEquals("Automatic fixture",movie.resolve(item,strict=true).title) // No network exists.
            val manual=JSONObject().put("tmdb_id",12345).toString()
            cache(manual,"Manual fixture")
            assertEquals("Manual fixture",movie.resolve(item,12345,true).title)
            movie.resolve(item,strict=true) // A stale background item must not overwrite the manual match.
            db.readableDatabase.rawQuery("SELECT query,manual FROM movie_match",null).use{
                assertTrue(it.moveToFirst());assertEquals(manual,it.getString(0));assertEquals(1,it.getInt(1))
            }
            val fresh=movie.indexItem(entry[0],entry[1],entry[2],entry[3].toInt())!!
            assertEquals("Manual fixture",movie.resolve(fresh,strict=true).title)
            db.writableDatabase.execSQL("UPDATE metadata_index SET state='error' WHERE kind='movie'")
            movie.queueIndex()
            db.readableDatabase.rawQuery("SELECT state FROM metadata_index WHERE kind='movie'",null).use{assertTrue(it.moveToFirst());assertEquals("error",it.getString(0))}
            db.importReader(source,StringReader("#EXTM3U\n#EXTINF:-1 type=\"movie\",Replacement\nhttps://example.test/replacement\n"))
            assertNull(movie.indexItem(entry[0],entry[1],entry[2],entry[3].toInt()))
            db.deleteSource(source)
            db.readableDatabase.rawQuery("SELECT COUNT(*) FROM metadata_index",null).use{assertTrue(it.moveToFirst());assertEquals(0,it.getInt(0))}
        } finally { db.close();ctx.deleteDatabase(name) }
    }
}
