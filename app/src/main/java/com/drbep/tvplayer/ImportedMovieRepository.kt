package com.drbep.tvplayer

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.drbep.tvplayer.playlists.PlaylistDatabase
import org.json.JSONObject
import java.util.Locale

internal data class ImportedMovie(val channel: ChannelItem,val source: String,val identity: String,val occurrence: Int,
    val group: String,val provider: String,val query: String,val metadata: JSONObject?,val progress: Long,val seriesName:String="") {
    val id get() = if(seriesName.isBlank()) channel.id else "series:$source:$seriesName"
    val title get() = metadata?.optString("title")?.takeIf { it.isNotBlank() } ?: seriesName.ifBlank{channel.name}
    val poster get() = metadata?.optString("poster")?.takeIf { it.isNotBlank() } ?: channel.logoUrl
    val backdrop get() = metadata?.optString("backdrop").orEmpty()
    val description get() = metadata?.optString("overview").orEmpty()
    val meta get() = listOf(metadata?.optString("release_date")?.take(4).orEmpty(),
        metadata?.optInt("runtime")?.takeIf { it>0 }?.let { "$it min" }.orEmpty(),
        metadata?.optDouble("rating")?.takeIf { it>0 }?.let { "★ %.1f".format(Locale.ROOT,it) }.orEmpty(),group.ifBlank { provider }).filter {it.isNotBlank()}.joinToString(" · ")
}
internal data class MovieFilter(val source: String="",val group: String="",val genre: String="",val search: String="",val order: String="added")
internal data class MovieShelf(val name: String,val items: List<ImportedMovie>,val filter:MovieFilter=MovieFilter(),val continuing:Boolean=false,val nextOffset:Int=0,val more:Boolean=false)
internal data class MoviePage(val shelves: List<MovieShelf>,val sources: List<Pair<String,String>>,val groups: List<String>,val genres: List<String>,val more: Boolean,val identified:Int=0,val total:Int=0)

internal class ImportedMovieRepository @JvmOverloads constructor(context: Context,private val controller: PersonalPlaylistController?,
    private val base: String,private val headers: Map<String,String>,private val allowed: (ChannelItem)->Boolean,val series:Boolean=false,database:PlaylistDatabase?=null) {
    private val indexPrefs=context.getSharedPreferences("metadata-index",Context.MODE_PRIVATE)
    fun indexRevision()=indexPrefs.getLong("revision",0)
    val kind get()=if(series) "series" else "movie"
    val label get()=if(series) "series" else "películas"
    private val db=database?:PlaylistDatabase.getInstance(context)
    private fun Cursor.s(name:String)=getString(getColumnIndexOrThrow(name)).orEmpty()
    private val episodeFrom=" FROM entries e JOIN sources s ON s.id=e.source AND COALESCE(s.active,s.preview)=e.generation LEFT JOIN preferences p ON p.source=e.source AND p.identity=e.identity AND p.occurrence=e.occurrence "
    private val from get()=if(!series) episodeFrom+" LEFT JOIN movie_added a ON a.source=e.source AND a.identity=e.identity AND a.occurrence=e.occurrence LEFT JOIN movie_match mm ON mm.source=e.source AND mm.identity=e.identity AND mm.occurrence=e.occurrence LEFT JOIN movie_metadata md ON md.query=mm.query " else
        " FROM (SELECT e.source,e.generation,e.series,MIN(e.rowid) AS representative_id,MAX(COALESCE(p.resume_ms,0)) AS resume_ms"+episodeFrom+" WHERE e.kind='series' AND COALESCE(p.hidden,0)=0 GROUP BY e.source,e.generation,e.series) grouped JOIN entries e ON e.rowid=grouped.representative_id JOIN sources s ON s.id=e.source LEFT JOIN preferences p ON p.source=e.source AND p.identity=e.identity AND p.occurrence=e.occurrence LEFT JOIN series_added a ON a.source=e.source AND a.series=e.series LEFT JOIN series_match mm ON mm.source=e.source AND mm.series=e.series LEFT JOIN movie_metadata md ON md.query=mm.query "
    private val progressColumn get()=if(series) "grouped.resume_ms" else "COALESCE(p.resume_ms,0)"
    private fun query(title:String):String {
        val year=Regex("\\((19\\d{2}|20\\d{2})\\)\\s*$").find(title)?.groupValues?.get(1).orEmpty()
        val name=title.replace(Regex("\\s*\\((19\\d{2}|20\\d{2})\\)\\s*$"),"").replace(Regex("(?i)\\b(2160p|1080p|720p|4k|uhd|fhd)\\b"),"").trim()
        return JSONObject().put("title",name).put("year",year).apply{if(series)put("kind","tv")}.toString()
    }
    fun syncProgress(progress:Map<String,Long>) {
        val sql=db.writableDatabase
        sql.beginTransaction();try {
            sql.execSQL("UPDATE preferences SET resume_ms=0 WHERE EXISTS(SELECT 1 FROM entries e WHERE e.kind IN ('movie','series') AND e.source=preferences.source AND e.identity=preferences.identity AND e.occurrence=preferences.occurrence)")
            for((id,ms) in progress) {
                val parts=id.split(":");if(parts.size!=4||parts[0]!="m3u")continue
                // Resume history can arrive from another device or outlive a deleted list.
                // OR IGNORE does not suppress foreign-key failures: only insert for a local entry.
                sql.execSQL("INSERT OR IGNORE INTO preferences(source,identity,occurrence) SELECT e.source,e.identity,e.occurrence FROM entries e JOIN sources s ON s.id=e.source AND COALESCE(s.active,s.preview)=e.generation WHERE e.source=? AND e.identity=? AND e.occurrence=? AND e.kind IN ('movie','series') LIMIT 1",arrayOf(parts[1],parts[2],parts[3]))
                sql.execSQL("UPDATE preferences SET resume_ms=? WHERE source=? AND identity=? AND occurrence=?",arrayOf(ms,parts[1],parts[2],parts[3]))
            };sql.setTransactionSuccessful()
        }finally{sql.endTransaction()}
    }
    private fun rows(f:MovieFilter,offset:Int=0,limit:Int=24,continuing:Boolean=false,scanned:(Int)->Unit={}):List<ImportedMovie> {
        val args=mutableListOf<String>();val where=StringBuilder(" WHERE e.kind='$kind' AND COALESCE(p.hidden,0)=0")
        for((field,value) in listOf("e.source" to f.source,"e.group_name" to f.group)) if(value.isNotBlank()){where.append(" AND $field=?");args+=value}
        if(f.genre.isNotBlank()){where.append(" AND instr(lower(md.genres),lower(?))>0");args+="|${f.genre}|"}
        if(f.search.isNotBlank()){where.append(" AND (instr(lower(e.title),lower(?))>0 OR instr(lower(md.json),lower(?))>0)");args+=f.search;args+=f.search}
        if(continuing)where.append(" AND $progressColumn>30000")
        val order=when(f.order){"title"->"e.title COLLATE NOCASE";"release"->"md.release_date DESC";"rating"->"md.rating DESC";else->"a.added DESC"}
        args+=limit.toString();args+=offset.toString()
        val result=mutableListOf<ImportedMovie>();var count=0
        db.readableDatabase.rawQuery("SELECT e.*,s.name AS provider,${progressColumn} AS progress,mm.query AS meta_query,md.json AS meta_json"+from+where+" ORDER BY $order,e.source,e.position LIMIT ? OFFSET ?",args.toTypedArray()).use {c->
            while(c.moveToNext()) {
                count++
                val item=PersonalPlaylistController.item(c,c.s("provider"));if(!allowed(item))continue
                val meta=runCatching{JSONObject(c.s("meta_json")).optJSONObject("movie")}.getOrNull()
                result+=ImportedMovie(item,c.s("source"),c.s("identity"),c.getInt(c.getColumnIndexOrThrow("occurrence")),c.s("group_name"),c.s("provider"),c.s("meta_query").ifBlank {query(if(series)c.s("series") else item.name)},meta,c.getLong(c.getColumnIndexOrThrow("progress")),if(series)c.s("series") else "")
            }
        };scanned(count);return result
    }
    fun searchLocal(query:String,genre:Boolean,similar:Boolean):List<ImportedMovie> {
        var f=if(genre)MovieFilter(genre=query,order="rating") else MovieFilter(search=query)
        if(similar) {
            val seed=rows(f,limit=1).firstOrNull()?:return emptyList()
            val genres=seed.metadata?.optJSONArray("genres")
            val first=genres?.optString(0).orEmpty()
            if(first.isBlank())return emptyList()
            f=MovieFilter(genre=first,order="rating")
            return rows(f,limit=24).filter{it.id!=seed.id}.take(12)
        }
        return rows(f,limit=24).take(12)
    }
    fun page(f:MovieFilter,offset:Int):MoviePage {
        val sources=mutableListOf<Pair<String,String>>();db.sources().use{while(it.moveToNext())sources+=it.getString(0) to it.getString(1)}
        val groups=mutableListOf<String>();val genres=linkedSetOf<String>()
        db.readableDatabase.rawQuery("SELECT DISTINCT e.group_name,md.genres"+from+" WHERE e.kind='$kind' AND COALESCE(p.hidden,0)=0 AND (?='' OR e.source=?)",arrayOf(f.source,f.source)).use{c->while(c.moveToNext()){val group=c.getString(0).orEmpty();if(group.isNotBlank())groups+=group;c.getString(1)?.split('|')?.filter{it.isNotBlank()}?.let{genres.addAll(it)}}}
        val shelves=mutableListOf<MovieShelf>()
        if(offset==0&&f.search.isBlank()&&f.genre.isBlank()) {
            val resume=loadShelf(MovieShelf("Seguir viendo",emptyList(),f,true));if(resume.items.isNotEmpty())shelves+=resume
        }
        val main=loadShelf(MovieShelf(when(f.order){"title"->"${if(series) "Series" else "Películas"} · A–Z";"release"->"Estrenos · del más reciente al más antiguo";"rating"->"Mejor valoradas";else->"Añadidas recientemente"},emptyList(),f,nextOffset=offset))
        shelves+=main
        if(offset==0&&f.genre.isBlank()&&f.search.isBlank()) for(g in genres.take(4)) {
            val shelf=loadShelf(MovieShelf(g,emptyList(),f.copy(genre=g)));if(shelf.items.isNotEmpty())shelves+=shelf
        }
        var total=0;var identified=0
        db.readableDatabase.rawQuery("SELECT COUNT(*),SUM(CASE WHEN md.release_date<>'' THEN 1 ELSE 0 END)"+from+" WHERE e.kind='$kind' AND COALESCE(p.hidden,0)=0 AND (?='' OR e.source=?)",arrayOf(f.source,f.source)).use{if(it.moveToFirst()){total=it.getInt(0);identified=it.getInt(1)}}
        return MoviePage(shelves,sources,groups.distinct().sorted(),genres.sorted(),main.more,identified,total)
    }
    /** Read bounded SQLite batches; denied entries still advance the cursor. */
    fun loadShelf(shelf:MovieShelf):MovieShelf {
        var offset=shelf.nextOffset
        var more:Boolean
        var batch:List<ImportedMovie>
        do {
            var scanned=0
            batch=rows(shelf.filter,offset,24,shelf.continuing){scanned=it}
            offset+=scanned;more=scanned==24
        } while(batch.isEmpty()&&more)
        return shelf.copy(items=(shelf.items+batch).distinctBy{it.id},nextOffset=offset,more=more)
    }
    private fun link(item:ImportedMovie,key:String,manual:Boolean=false) {
        if(series) {
            val sql=db.writableDatabase
            sql.execSQL("INSERT OR IGNORE INTO series_match(source,series,query,manual) VALUES(?,?,?,?)",arrayOf(item.source,item.seriesName,key,if(manual)1 else 0))
            sql.execSQL("UPDATE series_match SET query=?,manual=MAX(manual,?) WHERE source=? AND series=? AND (manual=0 OR ?=1)",arrayOf(key,if(manual)1 else 0,item.source,item.seriesName,if(manual)1 else 0));return
        }
        val sql=db.writableDatabase
        sql.execSQL("INSERT OR IGNORE INTO movie_match(source,identity,occurrence,query,manual) VALUES(?,?,?,?,?)",arrayOf(item.source,item.identity,item.occurrence,key,if(manual)1 else 0))
        sql.execSQL("UPDATE movie_match SET query=?,manual=MAX(manual,?) WHERE source=? AND identity=? AND occurrence=? AND (manual=0 OR ?=1)",arrayOf(key,if(manual)1 else 0,item.source,item.identity,item.occurrence,if(manual)1 else 0))
    }
    @JvmOverloads fun resolve(item:ImportedMovie,overrideId:Int=0,strict:Boolean=false):ImportedMovie {
        if(!strict)foregroundAt=android.os.SystemClock.elapsedRealtime()
        val key=if(overrideId>0) "$kind:id:$overrideId" else item.query
        val gate=synchronized(metadataLocks){metadataLocks.getOrPut(key){MetadataGate()}.also{it.users++}}
        try { return synchronized(gate){resolveLocked(item,overrideId,strict)} }
        finally { synchronized(metadataLocks){gate.users--;if(gate.users==0)metadataLocks.remove(key)} }
    }
    private fun resolveLocked(item:ImportedMovie,overrideId:Int,strict:Boolean):ImportedMovie {
        val key=if(overrideId>0)JSONObject().put("tmdb_id",overrideId).apply{if(series)put("kind","tv")}.toString() else item.query
        val now=System.currentTimeMillis()
        var cached:JSONObject?=null;var updated=0L
        db.readableDatabase.rawQuery("SELECT json,updated FROM movie_metadata WHERE query=?",arrayOf(key)).use{if(it.moveToFirst()){cached=JSONObject(it.getString(0));updated=it.getLong(1)}}
        if(series&&cached?.optString("kind")!="tv"){cached=null;updated=0L}
        val ttl=if(cached?.optBoolean("matched")==true)30L*86400000 else 86400000L
        val data=if(cached!=null&&now-updated<ttl) cached!! else {
            try {
                if(strict) {
                    val wait=1000-(android.os.SystemClock.elapsedRealtime()-lastBackgroundRequest)
                    if(wait>0)Thread.sleep(wait)
                    lastBackgroundRequest=android.os.SystemClock.elapsedRealtime()
                }
                val fresh=request(JSONObject(key))
                // Only valid metadata responses are cached; transport failures remain retryable.
                if(!fresh.has("matched")||(series&&fresh.optString("kind")!="tv"))throw IllegalStateException("Metadata unavailable")
                val movie=fresh.optJSONObject("movie")
                val values=ContentValues().apply{put("query",key);put("json",fresh.toString());put("updated",now);put("release_date",movie?.optString("release_date").orEmpty());put("rating",movie?.optDouble("rating")?:0.0);put("genres",movie?.optJSONArray("genres")?.let{a->(0 until a.length()).joinToString("|",prefix="|",postfix="|"){a.getString(it)}}.orEmpty())}
                db.writableDatabase.insertWithOnConflict("movie_metadata",null,values,android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);fresh
            }catch(e:Exception){if(strict)throw e; if(cached==null)return item else cached!!}
        }
        link(item,key,overrideId>0)
        return item.copy(query=key,metadata=data.optJSONObject("movie"))
    }
    fun pending(source:String):List<ImportedMovie> {
        val result=mutableListOf<ImportedMovie>()
        db.readableDatabase.rawQuery("SELECT e.*,s.name AS provider,${progressColumn} AS progress,mm.query AS meta_query,md.json AS meta_json"+from+" WHERE e.kind='$kind' AND COALESCE(p.hidden,0)=0 AND (?='' OR e.source=?) AND (md.updated IS NULL OR md.updated<?) ORDER BY e.source,e.position LIMIT 8",arrayOf(source,source,(System.currentTimeMillis()-30L*86400000).toString())).use{c->
            while(c.moveToNext()) {
                val item=PersonalPlaylistController.item(c,c.s("provider"));if(!allowed(item))continue
                result+=ImportedMovie(item,c.s("source"),c.s("identity"),c.getInt(c.getColumnIndexOrThrow("occurrence")),c.s("group_name"),c.s("provider"),c.s("meta_query").ifBlank{query(if(series)c.s("series") else item.name)},null,c.getLong(c.getColumnIndexOrThrow("progress")),if(series)c.s("series") else "")
            }
        };return result
    }

    companion object {
        private class MetadataGate { var users=0 }
        private val metadataLocks=HashMap<String,MetadataGate>()
        @Volatile private var lastBackgroundRequest=0L
        @Volatile private var foregroundAt=0L
        fun foregroundBusy()=android.os.SystemClock.elapsedRealtime()-foregroundAt<1500
    }
    fun queueIndex() {
        db.writableDatabase.execSQL("INSERT OR IGNORE INTO metadata_index(kind,source,generation,identity,occurrence,series,state) SELECT ?,e.source,e.generation,e.identity,e.occurrence,e.series,'pending'"+from+" WHERE e.kind=? AND COALESCE(p.hidden,0)=0",arrayOf(kind,kind))
    }
    fun indexItem(source:String,generation:String,identity:String,occurrence:Int):ImportedMovie? {
        val matchJoin=if(series) " LEFT JOIN series_match mm ON mm.source=e.source AND mm.series=e.series " else " LEFT JOIN movie_match mm ON mm.source=e.source AND mm.identity=e.identity AND mm.occurrence=e.occurrence "
        db.readableDatabase.rawQuery("SELECT e.*,s.name AS provider,mm.query AS meta_query"+episodeFrom+matchJoin+" WHERE e.kind=? AND e.source=? AND e.generation=? AND e.identity=? AND e.occurrence=? AND COALESCE(p.hidden,0)=0 LIMIT 1",arrayOf(kind,source,generation,identity,occurrence.toString())).use { c->
            if(!c.moveToFirst())return null
            val item=PersonalPlaylistController.item(c,c.s("provider"))
            if(!allowed(item))return null
            return ImportedMovie(item,source,identity,occurrence,c.s("group_name"),c.s("provider"),c.s("meta_query").ifBlank{query(if(series)c.s("series") else item.name)},null,0,if(series)c.s("series") else "")
        }
    }
    fun seasons(item:ImportedMovie):List<Pair<Int,Int>> {
        val result=mutableListOf<Pair<Int,Int>>()
        db.readableDatabase.rawQuery("SELECT e.season,COUNT(*)"+episodeFrom+" WHERE e.kind='series' AND e.source=? AND e.series=? AND COALESCE(p.hidden,0)=0 GROUP BY e.season ORDER BY e.season",arrayOf(item.source,item.seriesName)).use{c->while(c.moveToNext())result+=c.getInt(0) to c.getInt(1)}
        return result
    }
    fun episodes(item:ImportedMovie,season:Int,offset:Int,limit:Int=40):List<Pair<ChannelItem,Long>> {
        val result=mutableListOf<Pair<ChannelItem,Long>>()
        db.readableDatabase.rawQuery("SELECT e.*,s.name AS provider,COALESCE(p.resume_ms,0) AS progress"+episodeFrom+" WHERE e.kind='series' AND e.source=? AND e.series=? AND e.season=? AND COALESCE(p.hidden,0)=0 ORDER BY e.episode,e.position LIMIT ? OFFSET ?",arrayOf(item.source,item.seriesName,season.toString(),limit.coerceIn(1,100).toString(),offset.toString())).use{c->while(c.moveToNext()){
            val channel=PersonalPlaylistController.item(c,c.s("provider"));if(allowed(channel))result+=channel to c.getLong(c.getColumnIndexOrThrow("progress"))
        }}
        return result
    }
    fun candidates(title:String):List<JSONObject> {
        val result=request(JSONObject(query(title)).put("search","1")).optJSONArray("results")?:return emptyList()
        return (0 until result.length()).map{result.getJSONObject(it)}
    }
    private fun request(q:JSONObject):JSONObject {
        val url=StringBuilder(base.trimEnd('/')+"/api/offline/preferences?scope=movie_metadata")
        q.keys().forEach{key->url.append("&").append(Uri.encode(key)).append("=").append(Uri.encode(q.optString(key)))}
        val response=HttpClient().get(url.toString(),5000,12000,headers,256*1024)
        if(response.code!=200)throw IllegalStateException("Metadata unavailable")
        return JSONObject(response.body).also{if(series&&it.optString("kind")!="tv")throw IllegalStateException("Series metadata unavailable")}
    }
}
