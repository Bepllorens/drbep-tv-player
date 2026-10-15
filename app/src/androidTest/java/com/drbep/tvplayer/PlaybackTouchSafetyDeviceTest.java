package com.drbep.tvplayer;
import android.view.MotionEvent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class PlaybackTouchSafetyDeviceTest {
 @Test public void cancelledAndMultitouchGesturesCannotCompleteSwipe() {
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
   scenario.onActivity(a->{try {
    java.lang.reflect.Field mode=MainActivity.class.getDeclaredField("touchDeviceMode");mode.setAccessible(true);
    boolean original=mode.getBoolean(a);
    java.lang.reflect.Field x=MainActivity.class.getDeclaredField("touchGestureDownX");x.setAccessible(true);
    java.lang.reflect.Field y=MainActivity.class.getDeclaredField("touchGestureDownY");y.setAccessible(true);
    java.lang.reflect.Method touch=MainActivity.class.getDeclaredMethod("handlePlayerSurfaceTouch",MotionEvent.class);touch.setAccessible(true);
    try {
     mode.setBoolean(a,true);
     for(int action:new int[]{MotionEvent.ACTION_CANCEL,MotionEvent.ACTION_POINTER_DOWN}) {
      x.setFloat(a,100f);y.setFloat(a,100f);
      MotionEvent e=MotionEvent.obtain(0,10,action,200,100,0);
      try{assertEquals(true,touch.invoke(a,e));}finally{e.recycle();}
      assertTrue(Float.isNaN(x.getFloat(a)));assertTrue(Float.isNaN(y.getFloat(a)));
      e=MotionEvent.obtain(0,20,MotionEvent.ACTION_UP,500,100,0);
      try{assertEquals(true,touch.invoke(a,e));}finally{e.recycle();}
      assertTrue(Float.isNaN(x.getFloat(a)));
     }
     mode.setBoolean(a,false);
     MotionEvent e=MotionEvent.obtain(0,30,MotionEvent.ACTION_DOWN,100,100,0);
     try{assertEquals(false,touch.invoke(a,e));}finally{e.recycle();}
    } finally {mode.setBoolean(a,original);}
   }catch(Exception e){throw new AssertionError(e);}});
  }
 }
}
