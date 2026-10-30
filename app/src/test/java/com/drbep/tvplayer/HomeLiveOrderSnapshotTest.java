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

    @Test public void cleanedOrderMatchesReferenceForMixedValuesAndLimits() throws Exception {
        org.json.JSONArray input = new org.json.JSONArray("[\" A \",\"A\",\"\",null,7,\"B\",\" B \",\"ñ\"]");
        String raw = new org.json.JSONObject().put("k", new org.json.JSONObject().put("ids", input)).toString();
        HomeLiveOrderSnapshot cache = new HomeLiveOrderSnapshot();
        for (int limit : new int[]{0,1,2,5,10000}) {
            java.util.List<String> expected = new java.util.ArrayList<>();
            java.util.Set<String> seen = new java.util.HashSet<>();
            for(int i=0;i<input.length() && expected.size()<limit;i++) {
                String value=input.optString(i,"").trim();
                if(!value.isEmpty() && seen.add(value)) expected.add(value);
            }
            assertEquals(expected, cache.orderedIds("u",raw,"k",limit));
            assertEquals(expected, cache.orderedIds("u",raw,"k",limit));
        }
    }
    @Test public void copiesCannotChangeLaterReadsAndEditsInvalidateCleanOrder() {
        HomeLiveOrderSnapshot cache = new HomeLiveOrderSnapshot();
        String a="{\"k\":{\"ids\":[\"A\",\"B\"]}}";
        String b="{\"k\":{\"ids\":[\"B\",\"A\"]}}";
        cache.orderedIds("one",a,"k",10000).clear();
        assertEquals(java.util.Arrays.asList("A","B"),cache.orderedIds("one",a,"k",10000));
        assertEquals(java.util.Arrays.asList("B","A"),cache.orderedIds("one",b,"k",10000));
        assertEquals(java.util.Arrays.asList("A","B"),cache.orderedIds("two",a,"k",10000));
        assertTrue(cache.orderedIds("two","bad","k",10000).isEmpty());
        assertEquals(java.util.Arrays.asList("B","A"),cache.orderedIds("two",b,"k",10000));
    }
    @Test public void switchingGroupsAndLimitsPreservesAllOrders() throws Exception {
        org.json.JSONObject data=new org.json.JSONObject();
        for(int i=0;i<12;i++) data.put("k"+i,new org.json.JSONObject().put("ids",new org.json.JSONArray().put("a"+i).put("b"+i)));
        HomeLiveOrderSnapshot cache=new HomeLiveOrderSnapshot();
        for(int pass=0;pass<3;pass++) for(int i=0;i<12;i++) {
            assertEquals(java.util.Arrays.asList("a"+i,"b"+i),cache.orderedIds("u",data.toString(),"k"+i,10000));
            assertEquals(java.util.Arrays.asList("a"+i),cache.orderedIds("u",data.toString(),"k"+i,1));
        }
        assertTrue(cache.orderedIds("u",data.toString(),"missing",10000).isEmpty());
    }
}
