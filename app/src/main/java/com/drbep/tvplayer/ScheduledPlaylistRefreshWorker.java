package com.drbep.tvplayer;
import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import androidx.work.*;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import com.drbep.tvplayer.playlists.PlaylistDatabase;

/** Optional calendar checks only. Does not refresh guides or run database maintenance. */
public final class ScheduledPlaylistRefreshWorker extends Worker {
 private static final String WORK="personal-playlists-calendar";
 public ScheduledPlaylistRefreshWorker(Context context,WorkerParameters params){super(context,params);}
 public Result doWork() {
  PlaylistDatabase db=PlaylistDatabase.getInstance(getApplicationContext());
  boolean failed=false;
  try(Cursor ids=db.getReadableDatabase().rawQuery("SELECT id FROM sources WHERE auto_refresh=1 AND refresh_schedule<>''",null)){
   while(ids.moveToNext()) {
    if(isStopped())return Result.retry();
    String id=ids.getString(0);
    // Same lock as manual/legacy enqueue and configuration/deletion in the panel.
    try {
     PlaylistScheduledDispatch.run(PlaylistImportJobs.class,new PlaylistScheduledDispatch.Source() {
      String encoded;long now;java.time.ZoneId zone;
      public boolean due()throws Exception {
       if(isStopped())return false;
       try(Cursor c=db.source(id)) {
        if(!c.moveToFirst())return false;
        now=System.currentTimeMillis();zone=ZoneId.systemDefault();
        encoded=c.getString(c.getColumnIndexOrThrow("refresh_schedule"));
        boolean refreshable="server".equals(c.getString(c.getColumnIndexOrThrow("origin"))) ||
            !c.getString(c.getColumnIndexOrThrow("locator")).isEmpty();
        boolean busy;
        try(Cursor jobs=db.getReadableDatabase().rawQuery("SELECT 1 FROM import_jobs WHERE source=? AND state IN ('queued','running','retry') LIMIT 1",new String[]{id})){
         busy=jobs.moveToFirst();
        }
        return PlaylistRefreshDecision.due(encoded,c.getInt(c.getColumnIndexOrThrow("auto_refresh"))!=0,
            refreshable,c.getLong(c.getColumnIndexOrThrow("updated")),c.getLong(c.getColumnIndexOrThrow("refresh_slot")),
            now,zone,busy);
       }
      }
      public void enqueue()throws Exception { PlaylistImportJobs.enqueue(getApplicationContext(),id,"url",""); }
      public void markHandled() {
       ContentValues values=new ContentValues();
       values.put("refresh_slot",PlaylistRefreshSchedule.decode(encoded).latestAtOrBefore(now,zone));
       db.getWritableDatabase().update("sources",values,"id=? AND refresh_schedule=?",new String[]{id,encoded});
      }
     });
    }catch(Exception error){failed=true;}
   }
  }catch(Exception error){return Result.retry();}
  return failed?Result.retry():Result.success();
 }
 static void reconcile(Context context) {
  PlaylistDatabase db=PlaylistDatabase.getInstance(context);boolean enabled;
  try(Cursor c=db.getReadableDatabase().rawQuery("SELECT 1 FROM sources WHERE auto_refresh=1 AND refresh_schedule<>'' LIMIT 1",null)){enabled=c.moveToFirst();}
  WorkManager manager=WorkManager.getInstance(context);
  if(enabled)manager.enqueueUniquePeriodicWork(WORK,ExistingPeriodicWorkPolicy.KEEP,
      new PeriodicWorkRequest.Builder(ScheduledPlaylistRefreshWorker.class,15,TimeUnit.MINUTES)
      .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
          .setRequiresStorageNotLow(true).build()).build());
  else manager.cancelUniqueWork(WORK);
 }
}
