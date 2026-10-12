package com.drbep.tvplayer;

import android.content.Context;
import android.database.Cursor;
import androidx.work.*;
import com.drbep.tvplayer.playlists.PlaylistDatabase;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class PlaylistImportWorker extends Worker {
    public PlaylistImportWorker(Context c,WorkerParameters parameters){super(c,parameters);}
    public Result doWork(){
        Context app=getApplicationContext();PlaylistDatabase db=PlaylistDatabase.getInstance(app);String id=getId().toString();
        String source=getInputData().getString("source"),kind=getInputData().getString("kind"),filename=getInputData().getString("file");
        long[] count={0};
        PlaylistDatabase.Progress progress=new PlaylistDatabase.Progress(){
            public void check()throws Exception {if(isStopped()||Thread.currentThread().isInterrupted())throw new InterruptedIOException();}
            public void update(long n)throws Exception {check();count[0]=n;PlaylistImportJobs.update(db,id,"running","Procesando "+("guide".equals(kind)?"guía":"lista"),n);}
        };
        try {
            try(Cursor job=db.getReadableDatabase().rawQuery("SELECT state FROM import_jobs WHERE id=?",new String[]{id})) {if(!job.moveToFirst()||"cancelled".equals(job.getString(0)))return Result.failure();}
            progress.check();PlaylistImportJobs.update(db,id,"running","Preparando importación",0);
            String message="Lista importada";
            if("guide".equals(kind))message=PlaylistGuideSync.refresh(db,source,progress);
            else {
                String origin,url;try(Cursor c=db.source(source)){if(!c.moveToFirst())throw new IOException("Missing source");origin=c.getString(c.getColumnIndexOrThrow("origin"));url=c.getString(c.getColumnIndexOrThrow("locator"));}
                if("server".equals(origin))ServerPlaylistSync.refresh(app,db,source,progress);
                else {
                    PlaylistImportJobs.update(db,id,"running","Descargando y leyendo lista",0);
                    InputStream raw;
                    if("file".equals(kind)){if(filename==null||!filename.matches("[a-f0-9-]+\\.m3u"))throw new IOException("Invalid staged file");raw=new FileInputStream(new File(PlaylistImportJobs.spoolDir(app),filename));}
                    else raw=PersonalPlaylistController.openSource(url);
                    try(InputStream input=raw;Reader reader=new InputStreamReader(input,StandardCharsets.UTF_8)){count[0]=db.importReader(source,reader,progress).imported;}
                }
                try(Cursor total=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM entries e JOIN sources s ON s.id=e.source AND s.active=e.generation WHERE s.id=?",new String[]{source})) {if(total.moveToFirst())count[0]=total.getLong(0);}
                long importedCount=count[0];
                progress.check();String guide;
                try(Cursor c=db.source(source)){guide=c.moveToFirst()?c.getString(c.getColumnIndexOrThrow("epg_url")):"";}
                if(!guide.isEmpty()) {
                    PlaylistImportJobs.update(db,id,"running","Lista disponible · actualizando guía",count[0]);
                    try {message="Lista importada · "+PlaylistGuideSync.refresh(db,source,progress);}
                    catch(Exception e){progress.check();message="Lista importada · guía pendiente de actualizar";}
                }
                count[0]=importedCount;
            }
            progress.check();PlaylistImportJobs.update(db,id,"complete",message,count[0]);removeSpool(filename);return Result.success();
        } catch(Exception e) {
            if(isStopped())return Result.retry();
            if(getRunAttemptCount()<2&&!(e instanceof FileNotFoundException)){
                PlaylistImportJobs.update(db,id,"retry","Interrumpida · reintentando",count[0]);return Result.retry();
            }
            PlaylistImportJobs.update(db,id,"error",failureMessage(e)+" · se conserva la lista anterior",count[0]);removeSpool(filename);return Result.failure();
        }
    }
    static String failureMessage(Exception error) {
        for(Throwable e=error;e!=null;e=e.getCause()) {
            if(e instanceof android.database.sqlite.SQLiteFullException)return "Sin espacio. Libera almacenamiento y reintenta";
            if(e instanceof java.net.SocketTimeoutException)return "La conexión tardó demasiado. Comprueba la red y reintenta";
            if(e instanceof java.net.UnknownHostException||e instanceof java.net.ConnectException)return "No se pudo conectar. Comprueba la red y la dirección";
            if(e instanceof FileNotFoundException)return "El archivo ya no está disponible. Vuelve a seleccionarlo";
        }
        return "No se pudo leer la lista o guía. Comprueba el formato y la dirección";
    }
    private void removeSpool(String filename){if(filename!=null&&filename.matches("[a-f0-9-]+\\.m3u"))new File(PlaylistImportJobs.spoolDir(getApplicationContext()),filename).delete();}
    @Override public void onStopped(){super.onStopped();}
}
