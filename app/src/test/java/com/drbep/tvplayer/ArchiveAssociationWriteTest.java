package com.drbep.tvplayer;

import android.content.SharedPreferences;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArchiveAssociationWriteTest {
    @Test public void unchangedAssociationDoesNotRewritePreferencesButCorrectionStillInvalidates() {
        Map<String, String> values = new HashMap<>();
        int[] writes = {0};
        SharedPreferences.Editor editor = (SharedPreferences.Editor) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{SharedPreferences.Editor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("putString")) values.put((String) args[0], (String) args[1]);
                    if (method.getName().equals("remove")) values.remove(args[0]);
                    if (method.getName().equals("apply")) { writes[0]++; return null; }
                    return proxy;
                });
        SharedPreferences prefs = (SharedPreferences) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{SharedPreferences.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getString")) return values.getOrDefault((String) args[0], (String) args[1]);
                    if (method.getName().equals("edit")) return editor;
                    throw new AssertionError(method.getName());
                });
        ArchiveAvailability availability = new ArchiveAvailability(prefs);
        try {
            ChannelItem item = channel("TVE");
            assertFalse(availability.invalidateMismatchedIdentityAtKey("test", item));
            assertEquals(1, writes[0]);
            values.put("test.provider", "TVE");
            values.put("test.code", "4521");
            values.put("test.name", "LA 1");
            for (int i = 0; i < 50; i++) assertFalse(availability.invalidateMismatchedIdentityAtKey("test", item));
            assertEquals("Unchanged channels must not enqueue disk writes", 1, writes[0]);
            assertEquals("TVE", values.get("test.provider"));
            assertEquals("4521", values.get("test.code"));
            assertTrue(availability.invalidateMismatchedIdentityAtKey("test", channel("TVE-CORRECTED")));
            assertEquals(2, writes[0]);
            assertFalse(values.containsKey("test.provider"));
            assertFalse(values.containsKey("test.code"));
        } finally { availability.close(); }
    }
    private ChannelItem channel(String tvg) {
        ChannelItem item = new ChannelItem("one", "La 1", tvg, "", "", "", "", 1, 1, false, false, 1, "Movistar", Collections.emptyList(), "", "", "", false);
        return item;
    }
}
