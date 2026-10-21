package com.drbep.tvplayer

import android.content.Context
import androidx.work.*
import com.drbep.tvplayer.playlists.PlaylistDatabase
import org.json.JSONObject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Durable, opt-in metadata work. No Activity, playback URL or account token in WorkManager data. */
class MetadataIndexWorker(context:Context, parameters:WorkerParameters):Worker(context,parameters) {
    override fun doWork():Result {
        val previousPriority=android.os.Process.getThreadPriority(android.os.Process.myTid())
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            return runIndex()
        } finally {
            android.os.Process.setThreadPriority(previousPriority)
        }
    }
    private fun runIndex():Result {
        val ctx=applicationContext
        val generation=inputData.getString("generation")?:return Result.success()
        val selection=synchronized(lock){selection(ctx)}
        fun active()=!isStopped && synchronized(lock){prefs(ctx).getBoolean("enabled",false)&&prefs(ctx).getString("generation","")==generation}
        if(!active())return Result.success()
        try {
            val cooperative=CooperativeWorkYield(
                { BackgroundWorkBudget.now() }, { Thread.sleep(it) },
                { if(!active())throw InterruptedException("Index stopped") },
                { BackgroundWorkBudget.INSTANCE.remaining(BackgroundWorkBudget.now()) },15000)
            val quietStatus=object:CooperativeWorkYield.Status {
                override fun waiting() {}
                override fun resumed() {}
            }
            // The experiment bounds interaction deferrals, not foreground metadata priority.
            if(BuildConfig.COOPERATIVE_METADATA)cooperative.yield(8000,quietStatus)
            // Preserve the existing policy outside the experiment.
            val quietDeadline=android.os.SystemClock.elapsedRealtime()+15000
            while(!BuildConfig.COOPERATIVE_METADATA && active() && BackgroundWorkBudget.busy()) {
                if(android.os.SystemClock.elapsedRealtime()>=quietDeadline)return Result.retry()
                Thread.sleep(250)
            }
            if(!active())return Result.success()
            val session=CatalogSnapshotStore(ctx)
            val endpoint=session.getSourceUrl(BuildConfig.OFFLINE_BASE_URL).toHttpUrlOrNull()
            val token=session.accessToken
            if(endpoint==null||token.isBlank()) {
                synchronized(lock){if(active())prefs(ctx).edit().putBoolean("enabled",false).putString("state","account").commit()}
                return Result.success()
            }
            val base=endpoint.newBuilder().username("").password("").encodedPath("/").query(null).fragment(null).build().toString().trimEnd('/')
            val headers=mapOf("Authorization" to "Bearer $token","X-DRBEP-Access-Token" to token,"X-DRBEP-Device-Id" to session.deviceId)
            val repos=listOf(false,true).map{ImportedMovieRepository(ctx,null,base,headers,{!it.isAdultVod},it)}
            val db=PlaylistDatabase.getInstance(ctx).writableDatabase
            synchronized(lock) {
                if(!active())return Result.success()
                if(prefs(ctx).getBoolean("prepare",false)) {
                    val prepared=MetadataIndexQueue.prepare(object:MetadataIndexQueue.Transaction {
                        override fun begin() { db.beginTransaction() }
                        override fun successful() { db.setTransactionSuccessful() }
                        override fun end() { db.endTransaction() }
                    }, { active() }, {
                        db.execSQL(MetadataIndexQueue.CLEAN_STALE)
                        repos.filter{selection.kind.isEmpty() || it.kind==selection.kind}
                            .forEach{it.queueIndex(selection.source,selection.group)}
                    }, {
                        check(prefs(ctx).edit().putBoolean("prepare",false).commit()) { "Queue marker unavailable" }
                    })
                    if(!prepared)return Result.success()
                }
                prefs(ctx).edit().putString("state","running").commit()
            }
            // Short, bounded jobs avoid Android's ten-minute worker limit.
            val deadline=android.os.SystemClock.elapsedRealtime()+90000
            var processed=0
            while(active()&&processed<20&&android.os.SystemClock.elapsedRealtime()<deadline) {
                if(BuildConfig.COOPERATIVE_METADATA) {
                    cooperative.yield(250,quietStatus)
                    if(!active())return Result.success()
                    // Do not repeatedly read the same queue row while a focused title has priority.
                    if(ImportedMovieRepository.foregroundBusy()) { Thread.sleep(200); continue }
                }
                var row:List<String>?=null
                db.rawQuery("SELECT rowid,kind,source,generation,identity,occurrence FROM metadata_index WHERE state='pending' AND "+selection.where()+" ORDER BY rowid LIMIT 1",selection.args()).use{c->
                    if(c.moveToFirst())row=(0..5).map{c.getString(it)}
                }
                val entry=row?:break
                // Give focused titles priority and keep a modest maximum request rate.
                Thread.sleep(50)
                if(!active())return Result.success()
                if(ImportedMovieRepository.foregroundBusy() || (!BuildConfig.COOPERATIVE_METADATA && BackgroundWorkBudget.busy())) { Thread.sleep(200); continue }
                if(session.accessToken!=token) {
                    synchronized(lock){if(active())prefs(ctx).edit().putBoolean("enabled",false).putString("state","account").commit()}
                    return Result.success()
                }
                val repo=repos[if(entry[1]=="series")1 else 0]
                val state=try {
                    val item=repo.indexItem(entry[2],entry[3],entry[4],entry[5].toInt())
                    if(item==null)"skipped" else if(repo.resolve(item,strict=true).metadata==null)"unmatched" else "done"
                }catch(e:Exception){"error"}
                synchronized(lock) {
                    if(!active())return Result.success()
                    db.execSQL("UPDATE metadata_index SET state=? WHERE rowid=? AND state='pending'",arrayOf(state,entry[0]))
                    prefs(ctx).edit().putLong("revision",System.currentTimeMillis()).commit()
                }
                processed++
            }
            synchronized(lock) {
                if(!active())return Result.success()
                var pending=false
                db.rawQuery("SELECT 1 FROM metadata_index WHERE state='pending' AND "+selection.where()+" LIMIT 1",selection.args()).use{pending=it.moveToFirst()}
                if(pending) {
                    enqueue(ctx,generation,ExistingWorkPolicy.APPEND_OR_REPLACE)
                    prefs(ctx).edit().putString("state","queued").commit()
                } else prefs(ctx).edit().putBoolean("enabled",false).putString("state","complete").commit()
            }
            return Result.success()
        } catch(e:Exception) {
            if(!active())return Result.success()
            synchronized(lock){if(active())prefs(ctx).edit().putString("state","waiting").commit()}
            return Result.retry()
        }
    }
    companion object {
        private const val WORK="drbep-metadata-index"
        private val lock=Any()
        private fun prefs(c:Context)=c.getSharedPreferences("metadata-index",Context.MODE_PRIVATE)
        private fun selection(c:Context)=MetadataIndexSelection(prefs(c).getString("source","")?:"",prefs(c).getString("kind","")?:"",prefs(c).getString("group","")?:"")
        private fun enqueue(c:Context,generation:String,policy:ExistingWorkPolicy) {
            val request=OneTimeWorkRequest.Builder(MetadataIndexWorker::class.java)
                .setInputData(Data.Builder().putString("generation",generation).build())
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresStorageNotLow(true).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build()
            WorkManager.getInstance(c).enqueueUniqueWork(WORK,policy,request)
        }
        @JvmStatic fun command(c:Context,command:String):String = command(c,command,"","","")
        @JvmStatic fun command(c:Context,command:String,source:String,kind:String):String = command(c,command,source,kind,"")
        @JvmStatic fun command(c:Context,command:String,source:String,kind:String,group:String):String = synchronized(lock) {
            val p=prefs(c)
            require(command in listOf("start","pause","resume","retry"))
            if(command=="pause") {
                p.edit().putBoolean("enabled",false).putString("state","paused").commit()
                WorkManager.getInstance(c).cancelUniqueWork(WORK)
            } else {
                // Repeated start/resume while active never creates another worker.
                if(!p.getBoolean("enabled",false)) {
                    if(command=="start") {
                        val chosen=MetadataIndexSelection(source,kind,group)
                        if(chosen.source.isNotEmpty())PlaylistDatabase.getInstance(c).readableDatabase.rawQuery("SELECT 1 FROM sources WHERE id=?",arrayOf(chosen.source)).use {
                            require(it.moveToFirst()){"La lista ya no existe"}
                        }
                        p.edit().putString("source",chosen.source).putString("kind",chosen.kind).putString("group",chosen.group).commit()
                    }
                    val chosen=selection(c)
                    if(command=="retry")PlaylistDatabase.getInstance(c).writableDatabase.execSQL("UPDATE metadata_index SET state='pending' WHERE state='error' AND "+chosen.where(),chosen.args())
                    val id=UUID.randomUUID().toString()
                    p.edit().putBoolean("enabled",true).putString("generation",id).putString("state","queued")
                        .putBoolean("prepare",command=="start"||p.getBoolean("prepare",false)).commit()
                    enqueue(c,id,ExistingWorkPolicy.REPLACE)
                }
            }
            status(c)
        }
        @JvmStatic fun groups(c:Context,source:String,kind:String):String {
            MetadataIndexSelection(source,kind)
            val rows=org.json.JSONArray()
            if(source.isNotEmpty())PlaylistDatabase.getInstance(c).readableDatabase.rawQuery(
                "SELECT DISTINCT e.group_name FROM entries e JOIN sources s ON s.id=e.source AND COALESCE(s.active,s.preview)=e.generation LEFT JOIN preferences p ON p.source=e.source AND p.identity=e.identity AND p.occurrence=e.occurrence WHERE e.source=? AND e.kind IN ('movie','series') AND (?='' OR e.kind=?) AND e.group_name<>'' AND COALESCE(p.hidden,0)=0 ORDER BY e.group_name LIMIT 1001",
                arrayOf(source,kind,kind)).use {cursor->while(cursor.moveToNext())rows.put(cursor.getString(0))}
            val truncated=rows.length()>1000
            if(truncated)rows.remove(1000)
            return JSONObject().put("groups",rows).put("truncated",truncated).toString()
        }
        @JvmStatic fun review(c:Context,state:String,after:Long):String {
            MetadataIndexReview.validate(state,after)
            val chosen=synchronized(lock){selection(c)}
            val rows=org.json.JSONArray()
            var scanned=0;var next=after;var more=false
            PlaylistDatabase.getInstance(c).readableDatabase.rawQuery(MetadataIndexReview.sql(chosen),MetadataIndexReview.args(chosen,state,after)).use {cursor->
                while(cursor.moveToNext()) {
                    if(scanned==50){more=true;break}
                    scanned++;next=cursor.getLong(cursor.getColumnIndexOrThrow("review_row"))
                    val provider=cursor.getString(cursor.getColumnIndexOrThrow("source_name"))
                    val item=try {PersonalPlaylistController.item(cursor,provider)}catch(e:Exception){null}
                    if(item==null || item.isAdultVod)continue
                    val kind=cursor.getString(cursor.getColumnIndexOrThrow("index_kind"))
                    val title=if(kind=="series")cursor.getString(cursor.getColumnIndexOrThrow("series")).ifBlank{item.name} else item.name
                    rows.put(JSONObject().put("title",title).put("provider",provider).put("kind",kind).put("state",state))
                }
            }
            return JSONObject().put("rows",rows).put("next",next).put("more",more)
                .put("source",chosen.source).put("kind",chosen.kind).put("group",chosen.group).toString()
        }
        @JvmStatic fun status(c:Context):String = synchronized(lock) {
            val p=prefs(c);val chosen=selection(c);val result=JSONObject().put("source",chosen.source).put("kind",chosen.kind).put("group",chosen.group).put("state",p.getString("state","idle")).put("enabled",p.getBoolean("enabled",false))
            var total=0
            PlaylistDatabase.getInstance(c).readableDatabase.rawQuery("SELECT state,COUNT(*) FROM metadata_index WHERE "+chosen.countWhere()+" GROUP BY state",chosen.countArgs()).use{rows->
                while(rows.moveToNext()){val count=rows.getInt(1);result.put(rows.getString(0),count);total+=count}
            }
            val sources=org.json.JSONArray()
            PlaylistDatabase.getInstance(c).readableDatabase.rawQuery("SELECT id,name FROM sources ORDER BY name",null).use { rows->
                while(rows.moveToNext())sources.put(JSONObject().put("id",rows.getString(0)).put("name",rows.getString(1)))
            }
            result.put("sources",sources).put("total",total).toString()
        }
    }
}
