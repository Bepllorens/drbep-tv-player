package com.drbep.tvplayer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.drbep.tvplayer.playlists.PlaylistDatabase;
import android.database.Cursor;
import org.junit.Test;import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class PersonalPlaylistDeviceTest {
 @Test public void importedMovieActuallyRenders() throws Exception {
  try(ActivityScenario<MainActivity> sc=ActivityScenario.launch(MainActivity.class)) {
   Thread.sleep(6000);
   final String[] requested={""};
   sc.onActivity(a->{try {
    PersonalPlaylistController controller=(PersonalPlaylistController)CatchupFocusDeviceTest.call(a,"personalPlaylists");
    PlaylistDatabase db=PlaylistDatabase.getInstance(a);
    String id; try(Cursor sources=db.sources()){assertTrue(sources.moveToFirst());id=sources.getString(0);}
    try(Cursor entries=db.browse(id,"movie","",false,false,0,1)) {
     assertTrue("Imported movie exists",entries.moveToFirst());
     java.lang.reflect.Method m=PersonalPlaylistController.class.getDeclaredMethod("item",Cursor.class,String.class);m.setAccessible(true);
     ChannelItem item=(ChannelItem)m.invoke(controller,entries,"Lista web");
     requested[0]=item.id;
     assertEquals("Imported metadata must be playable","",item.playlistPlaybackError);
     ((android.view.View)CatchupFocusDeviceTest.field(a,"touchHomeHub")).setVisibility(android.view.View.GONE);
     m=MainActivity.class.getDeclaredMethod("playChannelItem",ChannelItem.class,boolean.class);m.setAccessible(true);m.invoke(a,item,true);
    }
   }catch(Exception e){throw new AssertionError(e);}});
   final boolean[] rendered={false};
   for(int i=0;i<160;i++) {
    sc.onActivity(a->{try {
     PlayerController pc=(PlayerController)CatchupFocusDeviceTest.field(a,"playerController");
     java.lang.reflect.Field f=PlayerController.class.getDeclaredField("player");f.setAccessible(true);
     androidx.media3.exoplayer.ExoPlayer ex=(androidx.media3.exoplayer.ExoPlayer)f.get(pc);
     androidx.media3.exoplayer.DecoderCounters c=ex.getVideoDecoderCounters();if(c!=null)c.ensureUpdated();
     f=PlayerController.class.getDeclaredField("currentRequest");f.setAccessible(true);
     PlayerController.PlaybackRequest request=(PlayerController.PlaybackRequest)f.get(pc);
     rendered[0]=request!=null&&requested[0].equals(request.channelId)&&ex.isPlaying()&&ex.getCurrentPosition()>2000&&c!=null&&c.renderedOutputBufferCount>20;
    }catch(Exception e){throw new AssertionError(e);}});
    if(rendered[0])break;Thread.sleep(250);
   }
   assertTrue("Imported movie must render video and advance",rendered[0]);
   sc.onActivity(a->{try {
    ((android.view.View)CatchupFocusDeviceTest.field(a,"touchHomeHub")).setVisibility(android.view.View.GONE);
   }catch(Exception e){throw new AssertionError(e);}});
   Thread.sleep(500);
   android.app.Instrumentation instrumentation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
   android.graphics.Bitmap screenshot=instrumentation.getUiAutomation().takeScreenshot();
   try(java.io.FileOutputStream out=instrumentation.getTargetContext().openFileOutput("m3u-validation.png",0)) {
    screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);
   } finally { screenshot.recycle(); }

  }
 }
}
