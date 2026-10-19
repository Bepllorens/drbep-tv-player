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
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        val ctx=applicationContext
        val generation=inputData.getString("generation")?:return Result.success()
        fun active()=!isStopped && synchronized(lock){prefs(ctx).getBoolean("enabled",false)&&prefs(ctx).getString("generation","")==generation}
        if(!active())return Result.success()
        try {
            // Wait before opening/indexing the catalogue as well as between titles.
            val quietDeadline=android.os.SystemClock.elapsedRealtime()+15000
            while(active() && BackgroundWorkBudget.busy()) {
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
                    // Generation changes leave stale queue rows harmless; remove them before a new scan.
                    db.execSQL("DELETE FROM metadata_index WHERE NOT EXISTS(SELECT 1 FROM sources s WHERE s.id=metadata_index.source AND COALESCE(s.active,s.preview)=metadata_index.generation)")
                    repos.forEach{it.queueIndex()}
                    prefs(ctx).edit().putBoolean("prepare",false).commit()
                }
                prefs(ctx).edit().putString("state","running").commit()
            }
            // Short, bounded jobs avoid Android's ten-minute worker limit.
            val deadline=android.os.SystemClock.elapsedRealtime()+90000
            var processed=0
            while(active()&&processed<20&&android.os.SystemClock.elapsedRealtime()<deadline) {
                var row:List<String>?=null
                db.rawQuery("SELECT rowid,kind,source,generation,identity,occurrence FROM metadata_index WHERE state='pending' ORDER BY rowid LIMIT 1",null).use{c->
                    if(c.moveToFirst())row=(0..5).map{c.getString(it)}
                }
                val entry=row?:break
                // Give focused titles priority and keep a modest maximum request rate.
                Thread.sleep(50)
                if(!active())return Result.success()
                if(ImportedMovieRepository.foregroundBusy() || BackgroundWorkBudget.busy()) { Thread.sleep(200); continue }
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
                db.rawQuery("SELECT 1 FROM metadata_index WHERE state='pending' LIMIT 1",null).use{pending=it.moveToFirst()}
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
        private fun enqueue(c:Context,generation:String,policy:ExistingWorkPolicy) {
            val request=OneTimeWorkRequest.Builder(MetadataIndexWorker::class.java)
                .setInputData(Data.Builder().putString("generation",generation).build())
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresStorageNotLow(true).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build()
            WorkManager.getInstance(c).enqueueUniqueWork(WORK,policy,request)
        }
        @JvmStatic fun command(c:Context,command:String):String = synchronized(lock) {
            val p=prefs(c)
            require(command in listOf("start","pause","resume","retry"))
            if(command=="pause") {
                p.edit().putBoolean("enabled",false).putString("state","paused").commit()
                WorkManager.getInstance(c).cancelUniqueWork(WORK)
            } else {
                // Repeated start/resume while active never creates another worker.
                if(!p.getBoolean("enabled",false)) {
                    if(command=="retry")PlaylistDatabase.getInstance(c).writableDatabase.execSQL("UPDATE metadata_index SET state='pending' WHERE state='error'")
                    val id=UUID.randomUUID().toString()
                    p.edit().putBoolean("enabled",true).putString("generation",id).putString("state","queued")
                        .putBoolean("prepare",command=="start"||p.getBoolean("prepare",false)).commit()
                    enqueue(c,id,ExistingWorkPolicy.REPLACE)
                }
            }
            status(c)
        }
        @JvmStatic fun status(c:Context):String = synchronized(lock) {
            val p=prefs(c);val result=JSONObject().put("state",p.getString("state","idle")).put("enabled",p.getBoolean("enabled",false))
            var total=0
            PlaylistDatabase.getInstance(c).readableDatabase.rawQuery("SELECT state,COUNT(*) FROM metadata_index GROUP BY state",null).use{rows->
                while(rows.moveToNext()){val count=rows.getInt(1);result.put(rows.getString(0),count);total+=count}
            }
            result.put("total",total).toString()
        }
    }
}
