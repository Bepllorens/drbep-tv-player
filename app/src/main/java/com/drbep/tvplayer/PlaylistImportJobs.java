package com.drbep.tvplayer;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import androidx.work.*;
import com.drbep.tvplayer.playlists.PlaylistDatabase;
import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import org.json.*;

/** Application-scoped durable jobs. Only source IDs and private spool filenames enter WorkManager. */
final class PlaylistImportJobs {
    static final java.util.concurrent.ExecutorService STAGING=Executors.newSingleThreadExecutor();
    static synchronized String enqueue(Context context,String source,String kind,String file)throws Exception {
        Context app=context.getApplicationContext();PlaylistDatabase db=PlaylistDatabase.getInstance(app);
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id FROM import_jobs WHERE source=? AND state IN ('queued','running','retry') LIMIT 1",new String[]{source})){
            if(c.moveToFirst())return c.getString(0);
        }
        Data data=new Data.Builder().putString("source",source).putString("kind",kind).putString("file",file==null?"":file).build();
        OneTimeWorkRequest work=new OneTimeWorkRequest.Builder(PlaylistImportWorker.class).setInputData(data)
            .setConstraints(new Constraints.Builder().setRequiresStorageNotLow(true).build()).addTag("playlist-import").build();
        String id=work.getId().toString();ContentValues v=new ContentValues();v.put("id",id);v.put("source",source);v.put("kind",kind);v.put("file",file==null?"":file);v.put("state","queued");v.put("label","En cola");v.put("updated",System.currentTimeMillis());
        db.getWritableDatabase().insertOrThrow("import_jobs",null,v);
        try {WorkManager.getInstance(app).enqueue(work).getResult().get();}catch(Exception e){update(db,id,"error","No se pudo iniciar",0);throw e;}
        return id;
    }
    static void update(PlaylistDatabase db,String id,String state,String label,long count){
        ContentValues v=new ContentValues();v.put("state",state);v.put("label",label);v.put("count",count);v.put("updated",System.currentTimeMillis());
        db.getWritableDatabase().update("import_jobs",v,"id=? AND state<>'cancelled'",new String[]{id});
    }
    static JSONArray list(Context context)throws Exception {
        JSONArray result=new JSONArray();PlaylistDatabase db=PlaylistDatabase.getInstance(context);
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT j.id,j.source,j.kind,j.state,j.label,j.count,j.updated,COALESCE(s.name,'Lista eliminada') FROM import_jobs j LEFT JOIN sources s ON s.id=j.source ORDER BY CASE WHEN j.state IN ('queued','running','retry') THEN 0 ELSE 1 END,j.updated DESC LIMIT 20",null)){
            while(c.moveToNext())result.put(new JSONObject().put("id",c.getString(0)).put("source",c.getString(1)).put("kind",c.getString(2)).put("state",c.getString(3)).put("phase",c.getString(4)).put("count",c.getLong(5)).put("updated",c.getLong(6)).put("name",c.getString(7)));
        }return result;
    }
    static void cancel(Context context,String id)throws Exception {
        UUID uuid=UUID.fromString(id);PlaylistDatabase db=PlaylistDatabase.getInstance(context);
        ContentValues v=new ContentValues();v.put("state","cancelled");v.put("label","Cancelada");v.put("updated",System.currentTimeMillis());
        int changed=db.getWritableDatabase().update("import_jobs",v,"id=? AND state IN ('queued','running','retry')",new String[]{id});
        if(changed>0){
            WorkManager.getInstance(context).cancelWorkById(uuid).getResult().get();
            try(Cursor c=db.getReadableDatabase().rawQuery("SELECT file FROM import_jobs WHERE id=?",new String[]{id})){
                if(c.moveToFirst()&&c.getString(0).matches("[a-f0-9-]+\\.m3u"))new File(spoolDir(context),c.getString(0)).delete();
            }
        }
    }
    static File spoolDir(Context context){File dir=new File(context.getFilesDir(),"playlist-imports");dir.mkdirs();return dir;}
    static String stage(Context context,InputStream input)throws Exception {
        File dir=spoolDir(context),part=new File(dir,UUID.randomUUID()+".part");long total=0;
        try(OutputStream out=new FileOutputStream(part)) {
            byte[] buffer=new byte[32768];int n;while((n=input.read(buffer))!=-1){total+=n;if(total>256L*1024*1024||dir.getUsableSpace()<32L*1024*1024)throw new IOException("Storage limit");out.write(buffer,0,n);}
        }catch(Exception e){part.delete();throw e;}
        File ready=new File(dir,part.getName().replace(".part",".m3u"));if(!part.renameTo(ready)){part.delete();throw new IOException("Unable to stage");}return ready.getName();
    }
    static String queueUpload(Context context,InputStream input)throws Exception {
        String file=stage(context,input);PlaylistDatabase db=PlaylistDatabase.getInstance(context);String source=db.createSource("Lista web","file");
        try{return enqueue(context,source,"file",file);}catch(Exception e){new File(spoolDir(context),file).delete();db.deleteUnpublishedSource(source);throw e;}
    }
}
