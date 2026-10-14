package com.drbep.tvplayer;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.lang.reflect.*;
import android.view.KeyEvent;
import org.json.JSONObject;

@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class MultiViewControlsDeviceTest {
    private Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    private ChannelItem row(int i){return new ChannelItem("fixture-"+i,"Canal de prueba "+i,"","","","","",i,i,false,false,0,"",new ArrayList<>(),"","","",false);}
    @Test public void rememberedPickerRestoresFocusAndNavigatesVirtualRows()throws Exception {
        List<ChannelItem> rows=new ArrayList<>();for(int i=0;i<90;i++)rows.add(row(i));
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            Thread.sleep(12000);
            scenario.onActivity(a->{try{
                ((Map<String,String>)field(a,"multiViewPickerPositions")).put("device-test","fixture-30");
                show(a,rows);
                assertNotNull("Selector window must be shown",ComposeDialogHost.topWindow(a));
                ((Map<String,String>)field(a,"multiViewPickerPositions")).remove("device-test");
            }catch(Exception e){throw new AssertionError(e);}});
            awaitPosition(scenario,"fixture-30");
            for(int i=0;i<15;i++){key(scenario,KeyEvent.KEYCODE_DPAD_DOWN);Thread.sleep(100);}
            awaitPosition(scenario,"fixture-45");
            key(scenario,KeyEvent.KEYCODE_BACK);Thread.sleep(400);
            scenario.onActivity(a->{try{show(a,rows);}catch(Exception e){throw new AssertionError(e);}});
            awaitPosition(scenario,"fixture-45");
            key(scenario,KeyEvent.KEYCODE_DPAD_UP);
            awaitPosition(scenario,"fixture-44");
        }
    }
    private void key(ActivityScenario<MainActivity> scenario,int code) {
        scenario.onActivity(a->{android.view.Window w=ComposeDialogHost.topWindow(a);assertNotNull("Selector remains open",w);w.getCallback().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,code));w.getCallback().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,code));});
    }
    private void show(MainActivity a,List<ChannelItem> rows)throws Exception{
        for(Method m:MainActivity.class.getDeclaredMethods())if(m.getName().equals("showRememberedMultiViewPicker")){
            m.setAccessible(true);Class<?> actionType=m.getParameterTypes()[5];
            Object action=Proxy.newProxyInstance(actionType.getClassLoader(),new Class<?>[]{actionType},(p,method,args)->null);
            m.invoke(a,"device-test","Selector de prueba","Navegación y memoria de foco",rows,"Vacío",action,null,null);return;
        }
        throw new AssertionError("Picker missing");
    }
    private void awaitPosition(ActivityScenario<MainActivity> s,String id)throws Exception{
        final String[] actual={""};
        for(int n=0;n<100;n++){s.onActivity(a->{try{actual[0]=((Map<String,String>)field(a,"multiViewPickerPositions")).get("device-test");}catch(Exception e){throw new AssertionError(e);}});if(id.equals(actual[0]))return;Thread.sleep(100);}
        assertEquals(id,actual[0]);
    }
    @Test public void windowDiagnosticsExposeMetricsWithoutPlaybackAddresses()throws Exception{
        try(ActivityScenario<MainActivity> s=ActivityScenario.launch(MainActivity.class)){
            s.onActivity(a->{try{
                PlayerController pc=(PlayerController)field(a,"playerController");assertNotNull(pc);
                JSONObject d=pc.panelWindowDiagnostics();assertTrue(d.has("bufferingCount"));assertTrue(d.has("resolution"));assertTrue(d.has("playing"));
                for(String key:Arrays.asList("targetUrl","license","headers","token","keys","lastError"))assertFalse(d.has(key));
            }catch(Exception e){throw new AssertionError(e);}});
        }
    }
}
