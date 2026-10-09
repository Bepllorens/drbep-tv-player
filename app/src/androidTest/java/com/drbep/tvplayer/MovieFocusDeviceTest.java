package com.drbep.tvplayer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import org.junit.Test;import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class MovieFocusDeviceTest {
 android.app.Instrumentation ins=InstrumentationRegistry.getInstrumentation();
 AccessibilityNodeInfo focused(AccessibilityNodeInfo n){if(n==null)return null;n.refresh();if(n.isFocused())return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo f=focused(n.getChild(i));if(f!=null)return f;}return null;}
 String label(AccessibilityNodeInfo n){if(n.getContentDescription()!=null)return n.getContentDescription().toString();if(n.getText()!=null)return n.getText().toString();for(int i=0;i<n.getChildCount();i++){String s=label(n.getChild(i));if(!s.isEmpty())return s;}return "";}
 String focus(){AccessibilityNodeInfo n=focused(ins.getUiAutomation().getRootInActiveWindow());assertNotNull("Input focus must remain attached",n);return label(n);}
 void key(int k)throws Exception{ins.sendKeyDownUpSync(k);Thread.sleep(1000);System.out.println("DPAD "+k+" -> "+focus());}
 void shot(String name)throws Exception{android.graphics.Bitmap b=ins.getUiAutomation().takeScreenshot();try(java.io.FileOutputStream o=ins.getTargetContext().openFileOutput(name,0)){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,o);}finally{b.recycle();}}
 @Test public void filtersRowsAndPaginationUsingOnlyRemote()throws Exception{
  try(ActivityScenario<MainActivity> sc=ActivityScenario.launch(MainActivity.class)){
   Thread.sleep(8000);sc.onActivity(a->{try{ComposeDialogHost.dismissAll(a);java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("showImportedMovies",String.class,Runnable.class);m.setAccessible(true);m.invoke(a,"",(Runnable)()->{});}catch(Exception e){throw new AssertionError(e);}});Thread.sleep(6500);
   android.accessibilityservice.AccessibilityServiceInfo service=ins.getUiAutomation().getServiceInfo();service.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS|android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;ins.getUiAutomation().setServiceInfo(service);Thread.sleep(500);shot("movies692-initial.png");try(java.io.FileOutputStream o=ins.getTargetContext().openFileOutput("movies692-nodes.txt",0)){o.write(ImportedMoviesDeviceTest.dump(ins.getUiAutomation().getRootInActiveWindow()).getBytes());}
   String initial=focus();assertTrue("Initial movie focus: "+initial,initial.contains(" · "));
   for(int i=0;i<8&&!focus().contains("Lista:");i++)key(KeyEvent.KEYCODE_DPAD_UP);
   assertTrue("UP reaches filters: "+focus(),focus().contains("Lista:"));
   String[] labels={"Grupo:","Género:","Buscar","Ordenar"};for(String label:labels){key(KeyEvent.KEYCODE_DPAD_RIGHT);assertTrue(label+": "+focus(),focus().contains(label));}
   shot("movies692-filters.png");key(KeyEvent.KEYCODE_DPAD_DOWN);
   if(focus().contains("Seguir viendo"))key(KeyEvent.KEYCODE_DPAD_DOWN);
   key(KeyEvent.KEYCODE_DPAD_RIGHT);
   key(KeyEvent.KEYCODE_DPAD_CENTER);Thread.sleep(700);assertTrue(ImportedMoviesDeviceTest.contains(ins.getUiAutomation().getRootInActiveWindow(),"Ficha de película"));key(KeyEvent.KEYCODE_BACK);assertTrue("Closing details restores movie focus: "+focus(),focus().contains(" · "));
  }
 }
}
