package com.drbep.tvplayer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.drbep.tvplayer.playlists.PlaylistDatabase;
import org.junit.Test;import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class ImportedMoviesDeviceTest {
 @Test public void movieScreenAndRemote()throws Exception {
  try(ActivityScenario<MainActivity> sc=ActivityScenario.launch(MainActivity.class)) {
   Thread.sleep(8000);
   sc.onActivity(a->{try {
    ComposeDialogHost.dismissAll(a);
    java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("showImportedMovies",String.class,Runnable.class);m.setAccessible(true);m.invoke(a,"",(Runnable)()->{});
   }catch(Exception e){throw new AssertionError(e);}});
   Thread.sleep(6000);
   android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
   instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_DOWN);
   instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_DOWN);
   Thread.sleep(1500);
   android.graphics.Bitmap shot=instrumentation.getUiAutomation().takeScreenshot();
   try(java.io.FileOutputStream out=instrumentation.getTargetContext().openFileOutput("movies691.png",0)){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{shot.recycle();}
   android.accessibilityservice.AccessibilityServiceInfo service=instrumentation.getUiAutomation().getServiceInfo();service.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS|android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;instrumentation.getUiAutomation().setServiceInfo(service);
   Thread.sleep(500);
   android.view.accessibility.AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();
   try(java.io.FileOutputStream out=instrumentation.getTargetContext().openFileOutput("movies691-nodes.txt",0)){out.write(dump(root).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
   android.view.accessibility.AccessibilityNodeInfo card=findClickable(root,"Al filo del mañana");
   if(card==null)card=findClickable(root,"1939-1975");
   assertNotNull("A movie card must be exposed to remote navigation",card);
   assertTrue("Movie can gain input focus",card.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS));
   instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_CENTER);
   Thread.sleep(2000);
   assertTrue("OK opens the movie details",contains(instrumentation.getUiAutomation().getRootInActiveWindow(),"Ficha de película"));
   shot=instrumentation.getUiAutomation().takeScreenshot();
   try(java.io.FileOutputStream out=instrumentation.getTargetContext().openFileOutput("movies691-detail.png",0)){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{shot.recycle();}
   instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);Thread.sleep(700);
   assertTrue("Back restores the catalogue",contains(instrumentation.getUiAutomation().getRootInActiveWindow(),"Películas · Mis listas"));
  }
 }
 static String dump(android.view.accessibility.AccessibilityNodeInfo n){if(n==null)return "null";String s=n.getClassName()+" text="+n.getText()+" desc="+n.getContentDescription()+" click="+n.isClickable()+" focus="+n.isFocused()+" focusable="+n.isFocusable()+"\n";for(int i=0;i<n.getChildCount();i++)s+=dump(n.getChild(i));return s;}
 static boolean contains(android.view.accessibility.AccessibilityNodeInfo n,String text){
  if(n==null)return false;if(String.valueOf(n.getText()).contains(text)||String.valueOf(n.getContentDescription()).contains(text))return true;
  for(int i=0;i<n.getChildCount();i++)if(contains(n.getChild(i),text))return true;return false;
 }
 static android.view.accessibility.AccessibilityNodeInfo findClickable(android.view.accessibility.AccessibilityNodeInfo n,String text){
  if(n==null)return null;if(n.isClickable()&&contains(n,text))return n;
  for(int i=0;i<n.getChildCount();i++){android.view.accessibility.AccessibilityNodeInfo f=findClickable(n.getChild(i),text);if(f!=null)return f;}return null;
 }
 @Test public void catalogueMigrationFilteringAndMetadata()throws Exception {
  try(ActivityScenario<MainActivity> sc=ActivityScenario.launch(MainActivity.class)) {
   Thread.sleep(8000);
   final ImportedMovieRepository[] repo={null};
   sc.onActivity(a->{try{
    PersonalPlaylistController controller=(PersonalPlaylistController)CatchupFocusDeviceTest.call(a,"personalPlaylists");
    CatalogSnapshotStore store=(CatalogSnapshotStore)CatchupFocusDeviceTest.field(a,"catalogSnapshotStore");
    java.util.Map<String,String> headers=new java.util.HashMap<>();headers.put("Authorization","Bearer "+store.getAccessToken());headers.put("X-DRBEP-Device-Id",store.getDeviceId());
    repo[0]=new ImportedMovieRepository(a,controller,(String)CatchupFocusDeviceTest.field(a,"baseUrl"),headers,c->true);
   }catch(Exception e){throw new AssertionError(e);}});
   MovieFilter all=new MovieFilter("","","","","added");
   MoviePage page=repo[0].page(all,0);assertFalse(page.getShelves().get(0).getItems().isEmpty());assertTrue(page.getMore());
   ImportedMovie first=page.getShelves().get(0).getItems().get(0);
   MoviePage next=repo[0].page(all,24);assertNotEquals(first.getId(),next.getShelves().get(0).getItems().get(0).getId());
   MoviePage found=repo[0].page(new MovieFilter("","","",first.getChannel().name,"title"),0);
   assertTrue(found.getShelves().get(0).getItems().stream().anyMatch(m->m.getId().equals(first.getId())));
   ImportedMovie enriched=repo[0].resolve(first,0);assertNotNull("TMDB metadata required for selected fixture",enriched.getMetadata());
   assertTrue(enriched.getMetadata().optInt("id")>0);assertFalse(enriched.getDescription().isEmpty());
   ImportedMovie again=repo[0].resolve(enriched,0);assertEquals(enriched.getMetadata().toString(),again.getMetadata().toString());
   sc.onActivity(a->{try{
    ComposeDialogHost.dismissAll(a);
    java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("showImportedMovies",String.class,Runnable.class);m.setAccessible(true);m.invoke(a,"",(Runnable)()->{});
   }catch(Exception e){throw new AssertionError(e);}});
   Thread.sleep(7000);
   android.graphics.Bitmap shot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
   try(java.io.FileOutputStream out=InstrumentationRegistry.getInstrumentation().getTargetContext().openFileOutput("movies691.png",0)){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{shot.recycle();}
  }
 }
}
