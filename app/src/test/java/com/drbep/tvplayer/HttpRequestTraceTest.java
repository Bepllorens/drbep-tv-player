package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class HttpRequestTraceTest {
    @Test public void separatesHeadersBodyAndTotal() {
        AtomicLong time=new AtomicLong(100);List<long[]> events=new ArrayList<>();
        HttpRequestTrace t=new HttpRequestTrace(7,time::get,(p,o,a,b,c)->events.add(new long[]{p,o,a,b,c}));
        time.set(140);t.headers(200);time.set(190);t.finish(0);
        assertArrayEquals(new long[]{0,7,0,0,0},events.get(0));
        assertArrayEquals(new long[]{1,7,40,200,0},events.get(1));
        assertArrayEquals(new long[]{2,7,90,50,0},events.get(2));
    }
    @Test public void failureBeforeHeadersHasNoInventedBodyDuration() {
        AtomicLong time=new AtomicLong(10);List<long[]> events=new ArrayList<>();
        HttpRequestTrace t=new HttpRequestTrace(9,time::get,(p,o,a,b,c)->events.add(new long[]{p,o,a,b,c}));
        time.set(20);t.finish(2);t.finish(0);t.headers(200);
        assertEquals(2,events.size());assertArrayEquals(new long[]{2,9,10,-1,2},events.get(1));
    }
    @Test public void cancellationIsNotOverwrittenByFinally() {
        List<Long> outcomes=new ArrayList<>();
        HttpRequestTrace t=new HttpRequestTrace(1,()->0,(p,o,a,b,c)->{if(p==2)outcomes.add(c);});
        t.finish(1);t.finish(0);assertEquals(Collections.singletonList(1L),outcomes);
    }
    @Test public void operationsDoNotMixWhenInterleaved() {
        AtomicLong time=new AtomicLong(100);List<long[]> events=new ArrayList<>();
        HttpRequestTrace.Sink sink=(p,o,a,b,c)->{if(p==2)events.add(new long[]{o,a,b,c});};
        HttpRequestTrace a=new HttpRequestTrace(1,time::get,sink);
        time.set(110);HttpRequestTrace b=new HttpRequestTrace(2,time::get,sink);
        time.set(120);b.headers(404);time.set(130);a.headers(200);
        time.set(140);b.finish(0);time.set(180);a.finish(0);
        assertArrayEquals(new long[]{2,30,20,0},events.get(0));
        assertArrayEquals(new long[]{1,80,50,0},events.get(1));
    }
    @Test public void duplicateHeadersAreIgnored() {
        List<Integer> phases=new ArrayList<>();
        HttpRequestTrace t=new HttpRequestTrace(1,()->0,(p,o,a,b,c)->phases.add(p));
        t.headers(200);t.headers(503);t.finish(0);
        assertEquals(Arrays.asList(0,1,2),phases);
    }
    @Test public void disabledSingletonCanBeReusedWithoutTrackingAnything() {
        for(int i=0;i<1000;i++){HttpRequestTrace.OFF.headers(200);HttpRequestTrace.OFF.finish(0);}
    }
}
