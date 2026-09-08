package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class BoundedMemoryCacheTest {
    @Test public void evictsLeastRecentlyUsedByEntryCount() {
        BoundedMemoryCache<String, String> c = new BoundedMemoryCache<>(2, 100, String::length);
        c.put("a", "a"); c.put("b", "b"); c.get("a"); c.put("c", "c");
        assertNull(c.get("b")); assertEquals("a", c.get("a")); assertEquals(2, c.size());
    }
    @Test public void aggregateWeightIsBounded() {
        BoundedMemoryCache<String, String> c = new BoundedMemoryCache<>(10, 5, String::length);
        c.put("a", "abc"); c.put("b", "def");
        assertNull(c.get("a")); assertEquals(3, c.weight());
        c.put("b", "x"); assertEquals(1, c.weight());
    }
    @Test public void oversizedReplacementDoesNotLeaveStaleValue() {
        BoundedMemoryCache<String, String> c = new BoundedMemoryCache<>(2, 3, String::length);
        c.put("a", "x"); c.put("a", "oversized");
        assertNull(c.get("a")); assertEquals(0, c.weight());
    }
    @Test public void emptyResultsAlsoHaveAnEntryCost() {
        BoundedMemoryCache<Integer, String> c = new BoundedMemoryCache<>(100, 3, String::length);
        for (int i=0; i<1000; i++) c.put(i, "");
        assertEquals(3, c.size()); assertEquals(3, c.weight());
    }
}
