package com.drbep.tvplayer;
import android.content.Context;import android.database.Cursor;import androidx.work.*;
import com.drbep.tvplayer.playlists.PlaylistDatabase;import java.io.*;import java.util.concurrent.TimeUnit;
public final class PlaylistRefreshWorker extends Worker {
    public PlaylistRefreshWorker(Context context,WorkerParameters params){super(context,params);}
    public Result doWork(){
        PlaylistDatabase db=PlaylistDatabase.getInstance(getApplicationContext());boolean failed=false;
        try(Cursor c=db.dueSources(System.currentTimeMillis()-TimeUnit.HOURS.toMillis(12))){while(c.moveToNext()){
            if(isStopped())return Result.retry();String id=c.getString(0),url=c.getString(2);
            try {
                synchronized(PlaylistImportJobs.class) {
                    try(Cursor current=db.source(id)) {
                        if(!current.moveToFirst() || current.getInt(current.getColumnIndexOrThrow("auto_refresh"))==0 ||
                            !current.getString(current.getColumnIndexOrThrow("refresh_schedule")).isEmpty())continue;
                        PlaylistImportJobs.enqueue(getApplicationContext(),id,"url","");
                    }
                }
            }catch(Exception e){failed=true;}
        }}catch(Exception e){return Result.retry();}
        try(Cursor c=db.dueGuides(System.currentTimeMillis()-TimeUnit.HOURS.toMillis(12))){while(c.moveToNext()){
            if(isStopped())return Result.retry();
            try {PlaylistImportJobs.enqueue(getApplicationContext(),c.getString(0),"guide","");}catch(Exception e){failed=true;}
        }}catch(Exception e){failed=true;}
        try {db.maintainGuides(System.currentTimeMillis());}catch(Exception e){failed=true;}
        return failed?Result.retry():Result.success();
    }
    static void schedule(Context context){PlaylistImportJobs.STAGING.execute(() -> {try{ScheduledPlaylistRefreshWorker.reconcile(context.getApplicationContext());}catch(Exception ignored){}});WorkManager.getInstance(context).enqueueUniquePeriodicWork("personal-playlists-refresh",ExistingPeriodicWorkPolicy.KEEP,new PeriodicWorkRequest.Builder(PlaylistRefreshWorker.class,12,TimeUnit.HOURS).setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresStorageNotLow(true).build()).build());}
}
