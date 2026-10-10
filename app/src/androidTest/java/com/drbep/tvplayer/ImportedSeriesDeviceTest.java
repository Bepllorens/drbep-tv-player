package com.drbep.tvplayer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import android.view.KeyEvent;
import org.junit.Test;import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class ImportedSeriesDeviceTest extends MovieContinuousDeviceTest {
 @Test public void seriesGroupingSeasonsAndRemote()throws Exception {
  try(ActivityScenario<MainActivity> sc=ActivityScenario.launch(MainActivity.class)) {
   Thread.sleep(8000); final ImportedMovieRepository[] repo={null};
   sc.onActivity(a->{try{
    PersonalPlaylistController controller=(PersonalPlaylistController)CatchupFocusDeviceTest.call(a,"personalPlaylists");
    CatalogSnapshotStore store=(CatalogSnapshotStore)CatchupFocusDeviceTest.field(a,"catalogSnapshotStore");
    java.util.Map<String,String> h=new java.util.HashMap<>();h.put("Authorization","Bearer "+store.getAccessToken());h.put("X-DRBEP-Device-Id",store.getDeviceId());
    repo[0]=new ImportedMovieRepository(a,controller,(String)CatchupFocusDeviceTest.field(a,"baseUrl"),h,c->true,true);
   }catch(Exception e){throw new AssertionError(e);}});
   MoviePage page=repo[0].page(new MovieFilter("","","","","added"),0);
   assertEquals(1200,page.getTotal());
   MovieShelf shelf=page.getShelves().stream().filter(s->s.getName().equals("Añadidas recientemente")).findFirst().get();
   while(shelf.getMore())shelf=repo[0].loadShelf(shelf);
   assertEquals(page.getTotal(),shelf.getItems().size());
   ImportedMovie first=shelf.getItems().get(0);
   assertTrue(first.getQuery().contains("tv"));
   java.util.List<kotlin.Pair<Integer,Integer>> seasons=repo[0].seasons(first);assertFalse(seasons.isEmpty());assertTrue(seasons.get(0).getFirst()>=0);
   java.util.List<kotlin.Pair<ChannelItem,Long>> episodes=repo[0].episodes(first,seasons.get(0).getFirst(),0,40);assertFalse(episodes.isEmpty());
   assertTrue(episodes.get(0).getFirst().id.startsWith("m3u:"));
   System.out.println("Series grouped: "+page.getTotal()+", first season episodes: "+episodes.size());
   sc.onActivity(a->{try{ComposeDialogHost.dismissAll(a);java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("showImportedSeries",String.class,Runnable.class);m.setAccessible(true);m.invoke(a,"",(Runnable)()->{});}catch(Exception e){throw new AssertionError(e);}});
   Thread.sleep(6500);
   android.accessibilityservice.AccessibilityServiceInfo service=ins.getUiAutomation().getServiceInfo();service.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS|android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;ins.getUiAutomation().setServiceInfo(service);Thread.sleep(1000);
   shot("series694-before-focus.png");
   try(java.io.FileOutputStream o=ins.getTargetContext().openFileOutput("series694-nodes.txt",0)){o.write(ImportedMoviesDeviceTest.dump(ins.getUiAutomation().getRootInActiveWindow()).getBytes());}

   if(focus().contains("Seguir viendo"))key(KeyEvent.KEYCODE_DPAD_DOWN);
   java.util.Set<String> visited=new java.util.HashSet<>();visited.add(focus());
   for(int i=0;i<27;i++){key(KeyEvent.KEYCODE_DPAD_RIGHT);assertTrue("Continuous series carousel",visited.add(focus()));}
   key(KeyEvent.KEYCODE_DPAD_UP);if(focus().contains("Seguir viendo"))key(KeyEvent.KEYCODE_DPAD_UP);assertTrue(focus().contains("Lista:"));
   for(int i=0;i<4;i++)key(KeyEvent.KEYCODE_DPAD_RIGHT);assertTrue(focus().contains("Ordenar"));
   key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_CENTER);assertTrue(focus().contains("Temporadas y episodios"));
   key(KeyEvent.KEYCODE_DPAD_CENTER);assertTrue(focus().contains("Volver a la ficha"));
   key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_DOWN);assertFalse(focus().isEmpty());shot("series694-episodes.png");
   key(KeyEvent.KEYCODE_BACK);assertTrue(focus().contains("Temporadas y episodios"));
   key(KeyEvent.KEYCODE_BACK);assertTrue(focus().contains(" · "));shot("series694-catalogue.png");
  }
 }
}
