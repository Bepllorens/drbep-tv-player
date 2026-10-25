package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class HomeLiveOrderSnapshotTest {
    @Test public void reusesUnchangedSnapshotAndInvalidatesImmediatelyOnEdit() {
        HomeLiveOrderSnapshot cache = new HomeLiveOrderSnapshot();
        String original = "{\"categories\":{\"ids\":[\"A\",\"B\"]}}";
        Object first = cache.ids("user1", original, "categories");
        assertSame(first, cache.ids("user1", original, "categories"));
        assertEquals("B", cache.ids("user1", "{\"categories\":{\"ids\":[\"B\",\"A\"]}}", "categories").optString(0));
    }
    @Test public void neverSharesParsedStateAcrossUserScopes() {
        HomeLiveOrderSnapshot cache = new HomeLiveOrderSnapshot();
        String data = "{\"channels:one\":{\"ids\":[\"1\",\"2\"]}}";
        Object first = cache.ids("user1", data, "channels:one");
        assertNotSame(first, cache.ids("user2", data, "channels:one"));
        assertNull(cache.ids("user2", "{}", "channels:one"));
    }
    @Test public void malformedOrMissingOrderCanRecoverAfterAnUpdate() {
        HomeLiveOrderSnapshot cache = new HomeLiveOrderSnapshot();
        assertNull(cache.ids("user1", "bad", "categories"));
        assertNull(cache.ids("user1", "bad", "categories"));
        assertEquals("A", cache.ids("user1", "{\"categories\":{\"ids\":[\"A\"]}}", "categories").optString(0));
        assertNull(cache.ids("user1", "{}", "categories"));
    }
}
