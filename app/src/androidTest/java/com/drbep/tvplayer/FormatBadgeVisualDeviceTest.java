package com.drbep.tvplayer;

import android.graphics.Bitmap;
import android.widget.LinearLayout;
import androidx.compose.ui.platform.ComposeView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/** Offline visual fixture: renders supplied vector marks without changing playback formats. */
public class FormatBadgeVisualDeviceTest {
 @Test public void renderAllMarksWithoutNetwork() throws Exception {
  android.app.Instrumentation ins=InstrumentationRegistry.getInstrumentation();
  try(ActivityScenario<LiveGuideQaActivity> scene=ActivityScenario.launch(LiveGuideQaActivity.class)){
   scene.onActivity(a->{
    LinearLayout column=new LinearLayout(a);column.setOrientation(LinearLayout.VERTICAL);
    column.setPadding(48,48,48,48);column.setBackgroundColor(0xff08121c);
    for(String label:new String[]{"DOLBY ATMOS  ·  HDR · PQ","DOLBY VISION  ·  DOLBY DIGITAL PLUS","DOLBY DIGITAL  ·  HDR · HLG","DOLBY TRUEHD"}){
     ComposeView v=new ComposeView(a);column.addView(v);SurfaceBadgeComposeBinder.bindFormats(v,label);
    }
    a.setContentView(column);
   });
   ins.waitForIdleSync();Thread.sleep(1500);
   Bitmap shot=ins.getUiAutomation().takeScreenshot();assertNotNull(shot);
   File dest=new File(ins.getTargetContext().getFilesDir(),"format-badges-783.png");
   try(FileOutputStream output=new FileOutputStream(dest)){assertTrue(shot.compress(Bitmap.CompressFormat.PNG,100,output));}
  }
 }
}
