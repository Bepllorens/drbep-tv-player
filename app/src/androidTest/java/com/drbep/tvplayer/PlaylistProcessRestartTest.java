package com.drbep.tvplayer;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.drbep.tvplayer.playlists.PlaylistDatabase;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Run phase=enqueue, stop the app process, then phase=verify. All fixtures remain local. */
@RunWith(AndroidJUnit4.class)
public class PlaylistProcessRestartTest {
 @Test public void importSurvivesProcessRestart()throws Exception {
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();PlaylistDatabase db=PlaylistDatabase.getInstance(c);
  android.content.SharedPreferences prefs=c.getSharedPreferences("playlist-restart-test",0);
  if("enqueue".equals(InstrumentationRegistry.getArguments().getString("phase"))) {
   File file=new File(PlaylistImportJobs.spoolDir(c),UUID.randomUUID()+".m3u");
   try(Writer out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),java.nio.charset.StandardCharsets.UTF_8))){out.write("#EXTM3U\n");for(int i=0;i<100000;i++)out.write("#EXTINF:-1 type=\"movie\",Reinicio "+i+"\nhttps://example.test/video\n");}
   String source=db.createSource("Prueba reinicio 678","file");String job=PlaylistImportJobs.enqueue(c,source,"file",file.getName());
   assertTrue(prefs.edit().putString("source",source).putString("job",job).commit());
   return;
  }
  PlaylistRefreshWorker.schedule(c);
  String source=prefs.getString("source",""),job=prefs.getString("job","");assertFalse("Run enqueue first",job.isEmpty());
  try {
   long until=System.nanoTime()+TimeUnit.MINUTES.toNanos(5);String state="";
   while(System.nanoTime()<until){try(Cursor rows=db.getReadableDatabase().rawQuery("SELECT state FROM import_jobs WHERE id=?",new String[]{job})){assertTrue(rows.moveToFirst());state=rows.getString(0);}
    if(state.equals("complete")||state.equals("error")||state.equals("cancelled"))break;Thread.sleep(250);
   }
   assertEquals("complete",state);
   try(Cursor rows=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM entries e JOIN sources s ON s.id=e.source AND s.active=e.generation WHERE s.id=?",new String[]{source})){assertTrue(rows.moveToFirst());assertEquals(100000,rows.getInt(0));}
  }finally{PlaylistImportJobs.cancel(c,job);db.deleteSource(source);db.getWritableDatabase().delete("import_jobs","id=?",new String[]{job});prefs.edit().clear().commit();}
 }
}
