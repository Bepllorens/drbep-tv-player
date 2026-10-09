package com.drbep.tvplayer;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.core.app.ActivityScenario;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class U7dCatalogDeviceTest {
    @Test public void metadataAndSavedArePersistentAndAccountIsolated() throws Exception {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String scope="test:"+System.nanoTime();
        EpgRepository.EpgProgram p=new EpgRepository.EpgProgram("test-channel","Canal de prueba","tvg","Programa","","Sinopsis","2026-10-09T12:00:00Z","2026-10-09T13:00:00Z","Cine",0);
        try(U7dCatalogStore db=new U7dCatalogStore(c)) {
            try {
                db.write(scope,p.channelId,Collections.singletonList(p));
                assertTrue(db.fresh(scope,p.channelId,System.currentTimeMillis()));
                assertEquals("Programa",db.read(scope,p.channelId).get(0).title);
                assertTrue(db.read(scope+"other",p.channelId).isEmpty());
                assertTrue(db.toggle(scope,p,123));
                assertTrue(db.saved(scope,U7dCatalogStore.id(p)));
                assertTrue(db.savedPrograms(scope+"other").isEmpty());
                try(U7dCatalogStore reopened=new U7dCatalogStore(c)) {
                    assertEquals("Cine",reopened.savedPrograms(scope).get(0).category);
                }
                assertFalse(db.toggle(scope,p,123));assertTrue(db.savedPrograms(scope).isEmpty());
            } finally {
                db.getWritableDatabase().delete("catalog","scope=?",new String[]{scope});
                db.getWritableDatabase().delete("saved","scope=?",new String[]{scope});
            }
        }
    }
    @Test public void catalogOpensAndBackKeepsActivityAlive() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            Thread.sleep(5000);
            scenario.onActivity(a->{
                try {
                    java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("openArchiveCatalog",ChannelItem.class,boolean.class);
                    m.setAccessible(true);m.invoke(a,null,false);
                    java.lang.reflect.Field d=MainActivity.class.getDeclaredField("archiveDialog");d.setAccessible(true);
                    assertNotNull(d.get(a));
                }catch(Exception e){throw new AssertionError(e);}
            });
            final boolean[] loaded={false};
            for(int attempt=0;attempt<120;attempt++) {
                scenario.onActivity(a->loaded[0]=!state(a).getEntries().isEmpty());
                if(loaded[0])break;
                Thread.sleep(250);
            }
            scenario.onActivity(a->{
                try {
                    java.lang.reflect.Field f=MainActivity.class.getDeclaredField("archiveState");f.setAccessible(true);
                    U7dCatalogState state=(U7dCatalogState)f.get(a);
                    assertFalse("Real account should have cached programs",state.getEntries().isEmpty());
                    assertTrue("Catalog must show several channels",state.getEntries().stream().map(e->e.channelId).distinct().count()>1);
                    android.util.Log.i("U7dTest","Visible entries="+state.getEntries().size()+" channels="+state.getEntries().stream().map(e->e.channelId).distinct().count());
                }catch(Exception e){throw new AssertionError(e);}
            });
            // Regression: selecting a date must not detach focus from that date button.
            press(android.view.KeyEvent.KEYCODE_DPAD_RIGHT);
            press(android.view.KeyEvent.KEYCODE_DPAD_CENTER);
            scenario.onActivity(a->assertEquals(java.time.LocalDate.now().minusDays(1).toString(),state(a).getDay()));
            press(android.view.KeyEvent.KEYCODE_DPAD_RIGHT);
            press(android.view.KeyEvent.KEYCODE_DPAD_CENTER);
            scenario.onActivity(a->assertEquals(java.time.LocalDate.now().minusDays(2).toString(),state(a).getDay()));
            press(android.view.KeyEvent.KEYCODE_DPAD_DOWN);
            press(android.view.KeyEvent.KEYCODE_DPAD_DOWN);
            final String[] selected=new String[1];
            for(int attempt=0;attempt<40;attempt++) {
                scenario.onActivity(a->selected[0]=state(a).getSelectedId());
                if(!selected[0].isEmpty())break;
                Thread.sleep(250);
            }
            scenario.onActivity(a->{selected[0]=state(a).getSelectedId();assertFalse("Grid should receive focus after date changes",selected[0].isEmpty());});
            press(android.view.KeyEvent.KEYCODE_DPAD_RIGHT);
            scenario.onActivity(a->assertNotEquals("Grid focus should move right",selected[0],state(a).getSelectedId()));
            press(android.view.KeyEvent.KEYCODE_DPAD_CENTER);
            press(android.view.KeyEvent.KEYCODE_BACK);
            scenario.onActivity(a->selected[0]=state(a).getSelectedId());
            press(android.view.KeyEvent.KEYCODE_DPAD_LEFT);
            scenario.onActivity(a->assertNotEquals("Returning from details should restore grid focus",selected[0],state(a).getSelectedId()));
            press(android.view.KeyEvent.KEYCODE_BACK);
            scenario.onActivity(a->assertFalse(a.isFinishing()));
        }
    }
    private static void press(int key) throws Exception {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(key);
        Thread.sleep(350);
    }
    private static U7dCatalogState state(MainActivity a) {
        try { java.lang.reflect.Field f=MainActivity.class.getDeclaredField("archiveState");f.setAccessible(true);return (U7dCatalogState)f.get(a); }
        catch(Exception e) { throw new AssertionError(e); }
    }
}
