package com.drbep.tvplayer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import org.junit.Test;import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class MovieContinuousDeviceTest {
 android.app.Instrumentation ins=InstrumentationRegistry.getInstrumentation();
 AccessibilityNodeInfo focused(AccessibilityNodeInfo n){if(n==null)return null;n.refresh();if(n.isFocused())return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo f=focused(n.getChild(i));if(f!=null)return f;}return null;}
 String label(AccessibilityNodeInfo n){if(n.getContentDescription()!=null)return n.getContentDescription().toString();if(n.getText()!=null)return n.getText().toString();for(int i=0;i<n.getChildCount();i++){String s=label(n.getChild(i));if(!s.isEmpty())return s;}return "";}
 String focus(){AccessibilityNodeInfo n=focused(ins.getUiAutomation().getRootInActiveWindow());assertNotNull("Input focus must remain attached",n);return label(n);}
 void key(int k)throws Exception{ins.sendKeyDownUpSync(k);Thread.sleep(1000);System.out.println("DPAD "+k+" -> "+focus());}
 void shot(String name)throws Exception{android.graphics.Bitmap b=ins.getUiAutomation().takeScreenshot();try(java.io.FileOutputStream o=ins.getTargetContext().openFileOutput(name,0)){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,o);}finally{b.recycle();}}
 @Test public void continuousCarouselDoesNotJumpOrStopAtPageBoundary()throws Exception{
  try(ActivityScenario<MainActivity> sc=ActivityScenario.launch(MainActivity.class)){
   Thread.sleep(8000);sc.onActivity(a->{try{ComposeDialogHost.dismissAll(a);java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("showImportedMovies",String.class,Runnable.class);m.setAccessible(true);m.invoke(a,"",(Runnable)()->{});}catch(Exception e){throw new AssertionError(e);}});Thread.sleep(6500);
   android.accessibilityservice.AccessibilityServiceInfo service=ins.getUiAutomation().getServiceInfo();service.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS|android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;ins.getUiAutomation().setServiceInfo(service);Thread.sleep(500);shot("movies692-initial.png");try(java.io.FileOutputStream o=ins.getTargetContext().openFileOutput("movies692-nodes.txt",0)){o.write(ImportedMoviesDeviceTest.dump(ins.getUiAutomation().getRootInActiveWindow()).getBytes());}
   if(focus().contains("Seguir viendo"))key(KeyEvent.KEYCODE_DPAD_DOWN);
   String first=focus();assertTrue(first.contains("Añadidas recientemente"));
   android.graphics.Rect before=new android.graphics.Rect();focused(ins.getUiAutomation().getRootInActiveWindow()).getBoundsInScreen(before);
   key(KeyEvent.KEYCODE_DPAD_RIGHT);
   android.graphics.Rect after=new android.graphics.Rect();focused(ins.getUiAutomation().getRootInActiveWindow()).getBoundsInScreen(after);
   assertTrue("Focus moves right without realigning each card: "+before+" -> "+after,after.left>before.left+50);
   assertEquals("Horizontal navigation must not jump vertically",before.top,after.top);
   java.util.Set<String> visited=new java.util.HashSet<>();visited.add(first);visited.add(focus());
   for(int i=0;i<51;i++){key(KeyEvent.KEYCODE_DPAD_RIGHT);String now=focus();assertTrue("Stay in main carousel",now.contains("Añadidas recientemente"));assertTrue("Each RIGHT advances, including batches 24 and 48: "+now,visited.add(now));}
   shot("movies693-continuous.png");
   key(KeyEvent.KEYCODE_DPAD_LEFT);key(KeyEvent.KEYCODE_DPAD_UP);
   if(focus().contains("Seguir viendo"))key(KeyEvent.KEYCODE_DPAD_UP);
   assertTrue("UP still reaches filters: "+focus(),focus().contains("Lista:"));
   key(KeyEvent.KEYCODE_DPAD_DOWN);assertTrue("Return to carousel",focus().contains(" · "));
   key(KeyEvent.KEYCODE_DPAD_CENTER);Thread.sleep(700);assertTrue(ImportedMoviesDeviceTest.contains(ins.getUiAutomation().getRootInActiveWindow(),"Ficha de película"));key(KeyEvent.KEYCODE_BACK);assertTrue(focus().contains(" · "));
  }
 }
 @Test public void sqliteBatchesReachEntireCatalogue()throws Exception {
  try(ActivityScenario<MainActivity> sc=ActivityScenario.launch(MainActivity.class)){
   Thread.sleep(8000);final ImportedMovieRepository[] repo={null};
   sc.onActivity(a->{try{PersonalPlaylistController controller=(PersonalPlaylistController)CatchupFocusDeviceTest.call(a,"personalPlaylists");repo[0]=new ImportedMovieRepository(a,controller,"",new java.util.HashMap<>(),c->true);}catch(Exception e){throw new AssertionError(e);}});
   MoviePage page=repo[0].page(new MovieFilter("","","","","added"),0);
   MovieShelf shelf=page.getShelves().stream().filter(s->s.getName().equals("Añadidas recientemente")).findFirst().get();
   int batches=1;while(shelf.getMore()){int previous=shelf.getNextOffset();shelf=repo[0].loadShelf(shelf);assertTrue(shelf.getNextOffset()>previous||!shelf.getMore());assertTrue("Bounded batches must finish",++batches<10000);}
   assertEquals("Every catalogue movie is reachable",page.getTotal(),shelf.getItems().size());
   assertEquals(shelf.getItems().size(),shelf.getItems().stream().map(m->m.getId()).distinct().count());
   System.out.println("Complete catalogue: "+shelf.getItems().size()+" movies in "+batches+" batches");
  }
 }
}
