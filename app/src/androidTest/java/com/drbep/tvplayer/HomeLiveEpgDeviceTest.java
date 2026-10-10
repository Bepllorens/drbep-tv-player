package com.drbep.tvplayer;

import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HomeLiveEpgDeviceTest {
    final android.app.Instrumentation ins = InstrumentationRegistry.getInstrumentation();
    AccessibilityNodeInfo root() { return ins.getUiAutomation().getRootInActiveWindow(); }
    String focus() {
        AccessibilityNodeInfo n = root();
        n = n == null ? null : n.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        return n == null ? "" : ImportedMoviesDeviceTest.dump(n);
    }
    void key(int code) throws Exception { ins.sendKeyDownUpSync(code); Thread.sleep(450); }
    void shot(String name) throws Exception {
        android.graphics.Bitmap b = ins.getUiAutomation().takeScreenshot();
        try (java.io.FileOutputStream o = ins.getTargetContext().openFileOutput(name,0)) {
            b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,o);
        } finally { b.recycle(); }
    }
    @Test public void homeGroupEpgAndExploreRemote() throws Exception {
        try (ActivityScenario<MainActivity> sc = ActivityScenario.launch(MainActivity.class)) {
            Thread.sleep(8000);
            sc.onActivity(a -> { try {
                ComposeDialogHost.dismissAll(a);
                StartupHomeHubUiModel.Discovery d = (StartupHomeHubUiModel.Discovery) CatchupFocusDeviceTest.field(a,"discoveryHomeState");
                ChannelItem ism = null;
                for (ChannelItem c : (java.util.List<ChannelItem>) CatchupFocusDeviceTest.field(a,"allChannels")) {
                    if (c.platformName.equalsIgnoreCase("Movistar ISM")) { ism=c; break; }
                }
                assertNotNull(ism);
                d.section="Inicio"; d.homeFocusedChannel="";
                d.selectedProviders.put("En directo","live:"+ism.platformId);
                // Keep the current playback source: the visible group must load
                // even when it differs from the channel playing behind home.
                CatchupFocusDeviceTest.call(a,"loadStartupHubStateAndShow");
            } catch(Exception e) { throw new AssertionError(e); }});
            boolean[] ready={false};
            for(int i=0;i<45 && !ready[0];i++) {
                Thread.sleep(2000);
                sc.onActivity(a -> {try {
                    java.util.List<ChannelItem> group=(java.util.List<ChannelItem>)CatchupFocusDeviceTest.call(a,"resolvePriorityEpgSourceChannels");
                    assertFalse(group.isEmpty());
                    java.util.Map<String,EpgRepository.EpgProgramPair> pairs=(java.util.Map<String,EpgRepository.EpgProgramPair>)CatchupFocusDeviceTest.field(a,"epgProgramPairByChannelId");
                    int count=0;
                    for(ChannelItem c:group) if(pairs.get(c.id)!=null && pairs.get(c.id).current!=null)count++;
                    ready[0]=count>=Math.min(8,group.size());
                    if(ready[0])System.out.println("ISM EPG populated="+count+" / "+group.size());
                }catch(Exception e){throw new AssertionError(e);}});
            }
            assertTrue("Displayed ISM group must load EPG",ready[0]);
            android.accessibilityservice.AccessibilityServiceInfo service=ins.getUiAutomation().getServiceInfo();
            service.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            ins.getUiAutomation().setServiceInfo(service);Thread.sleep(500);
            shot("home697-epg.png");
            AccessibilityNodeInfo explore=ImportedMoviesDeviceTest.findClickable(root(),"Explorar canales");
            assertNotNull(explore); assertTrue(explore.performAction(AccessibilityNodeInfo.ACTION_FOCUS));
            key(KeyEvent.KEYCODE_DPAD_CENTER);Thread.sleep(1500);
            shot("explore697-first.png");
            String first=focus();
            assertFalse("Explore must acquire remote focus",first.isEmpty());
            assertFalse(first.contains("Explorar canales"));
            key(KeyEvent.KEYCODE_DPAD_DOWN);shot("explore697-down.png");String second=focus();
            try(java.io.FileOutputStream o=ins.getTargetContext().openFileOutput("home697-nodes.txt",0)){o.write(ImportedMoviesDeviceTest.dump(root()).getBytes());}
            assertNotEquals("Down moves to another channel",first,second);
            key(KeyEvent.KEYCODE_DPAD_UP);assertEquals("Up restores previous channel",first,focus());
            assertTrue("EPG shown in Explore",!ImportedMoviesDeviceTest.dump(root()).contains("Programación no disponible"));
            shot("explore697-epg-focus.png");
            System.out.println("Explore remote focus: "+first+" -> "+second+" -> "+focus());
        }
    }
}
