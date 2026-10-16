package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class PreferenceValueCacheTest {
    @Test public void reusesOnlyIdenticalPreference() throws Exception {
        PreferenceValueCache cache=new PreferenceValueCache(); int[] calls={0};
        PreferenceValueCache.Decoder decoder=s->{calls[0]++;return "decoded-"+s;};
        assertEquals("decoded-a",cache.read("a",decoder));
        assertEquals("decoded-a",cache.read("a",decoder));
        assertEquals(1,calls[0]);
        assertEquals("decoded-b",cache.read("b",decoder));
        assertEquals(2,calls[0]);
    }
    @Test public void explicitResetDecodesAgain() throws Exception {
        PreferenceValueCache cache=new PreferenceValueCache(); int[] calls={0};
        PreferenceValueCache.Decoder decoder=s->{calls[0]++;return s;};
        cache.read("a",decoder);cache.clear();cache.read("a",decoder);
        assertEquals(2,calls[0]);
    }
    @Test public void failureDoesNotRetainOldOrCacheError() throws Exception {
        PreferenceValueCache cache=new PreferenceValueCache();
        cache.read("a",s->"old");
        try {cache.read("b",s->{throw new Exception("fixture");});fail();}catch(Exception expected){}
        assertEquals("new",cache.read("b",s->"new"));
        assertEquals("fresh",cache.read("a",s->"fresh"));
    }
}
