package com.drbep.tvplayer;
import org.junit.Test;import static org.junit.Assert.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import android.view.accessibility.AccessibilityNodeInfo;
public class LiveMenuLayoutDeviceTest {
 final android.app.Instrumentation ins=InstrumentationRegistry.getInstrumentation();
 AccessibilityNodeInfo root(){return ins.getUiAutomation().getRootInActiveWindow();}
 AccessibilityNodeInfo editable(AccessibilityNodeInfo node){if(node==null)return null;if(node.isEditable())return node;for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo hit=editable(node.getChild(i));if(hit!=null)return hit;}return null;}
 @Test public void searchRemoteAndFullscreen()throws Exception {
  try(ActivityScenario<MainActivity> sc=ActivityScenario.launch(MainActivity.class)) {
   Thread.sleep(8000);
   sc.onActivity(a->{try{ComposeDialogHost.dismissAll(a);StartupHomeHubUiModel.Discovery d=(StartupHomeHubUiModel.Discovery)CatchupFocusDeviceTest.field(a,"discoveryHomeState");d.section="En directo";d.liveQuery="";CatchupFocusDeviceTest.call(a,"loadStartupHubStateAndShow");}catch(Exception e){throw new AssertionError(e);}});
   Thread.sleep(2500);
   ins.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_DOWN);ins.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_UP);Thread.sleep(4500);
   android.graphics.Bitmap shot=ins.getUiAutomation().takeScreenshot();try(java.io.FileOutputStream out=ins.getTargetContext().openFileOutput("tv708.png",0)){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{shot.recycle();}
   AccessibilityNodeInfo search=ImportedMoviesDeviceTest.findClickable(root(),"⌕ Buscar");assertNotNull(search);assertTrue(search.performAction(AccessibilityNodeInfo.ACTION_CLICK));Thread.sleep(600);
   AccessibilityNodeInfo field=editable(root());assertNotNull("Search gets editable focus",field);
   android.os.Bundle text=new android.os.Bundle();text.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"M+");assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,text));Thread.sleep(400);
   sc.onActivity(a->{try{assertEquals("M+",((StartupHomeHubUiModel.Discovery)CatchupFocusDeviceTest.field(a,"discoveryHomeState")).liveQuery);}catch(Exception e){throw new AssertionError(e);}});
   text.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"");assertTrue(editable(root()).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,text));
   AccessibilityNodeInfo close=ImportedMoviesDeviceTest.findClickable(root(),"Cerrar búsqueda");assertNotNull(close);assertTrue(close.performAction(AccessibilityNodeInfo.ACTION_CLICK));Thread.sleep(400);assertNull(editable(root()));
   AccessibilityNodeInfo channel=ImportedMoviesDeviceTest.findClickable(root(),"M+ LALIGA.");assertNotNull("Channel remains accessible after search",channel);assertTrue(channel.performAction(AccessibilityNodeInfo.ACTION_FOCUS));Thread.sleep(500);
   ins.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_CENTER);Thread.sleep(1000);
   sc.onActivity(a->{try{assertNull("OK restores fullscreen",CatchupFocusDeviceTest.field(a,"startupLivePreviewPlayerView"));}catch(Exception e){throw new AssertionError(e);}});
  }
 }
}
