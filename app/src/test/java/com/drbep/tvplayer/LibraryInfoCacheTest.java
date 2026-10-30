package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public class LibraryInfoCacheTest {
    @Test public void sharesWorkAndCancellationDoesNotCancelOtherSubscriber() {
        LibraryInfoCache c = new LibraryInfoCache();
        List<Runnable> jobs = new ArrayList<>();
        List<String> results = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        Runnable cancel = c.request("a", () -> {calls.incrementAndGet();return new String[]{"plot","year"};},
            v -> results.add("old"), jobs::add, Runnable::run);
        c.request("a", () -> {throw new AssertionError();}, v -> results.add(v[0]), jobs::add, Runnable::run);
        cancel.run();
        assertEquals(1,jobs.size()); jobs.remove(0).run();
        assertEquals(1,calls.get()); assertEquals(Collections.singletonList("plot"),results);
    }
    @Test public void cachesEmptyResultAndProtectsArrays() {
        LibraryInfoCache c=new LibraryInfoCache();
        String[] source={"","year"};
        c.request("a",()->source,v->v[1]="bad",Runnable::run,Runnable::run);
        source[1]="bad";
        c.request("a",()->{throw new AssertionError();},v->assertEquals("year",v[1]),Runnable::run,Runnable::run);
    }
    @Test public void errorsCanRetry() {
        LibraryInfoCache c=new LibraryInfoCache();
        c.request("a",()->{throw new IllegalStateException();},v->{},Runnable::run,Runnable::run);
        c.request("a",()->new String[]{"retry",""},v->assertEquals("retry",v[0]),Runnable::run,Runnable::run);
    }
    @Test public void cancellationSuppressesAlreadyPostedDelivery() {
        LibraryInfoCache c=new LibraryInfoCache();
        List<Runnable> deliveries=new ArrayList<>();
        Runnable cancel=c.request("a",()->new String[]{"plot",""},v->fail(),Runnable::run,deliveries::add);
        cancel.run(); deliveries.forEach(Runnable::run);
    }
    @Test public void keepsOnly32MostRecentlyUsedResults() {
        LibraryInfoCache c=new LibraryInfoCache();
        for(int i=0;i<32;i++) c.request(""+i,()->new String[]{"ok",""},v->{},Runnable::run,Runnable::run);
        c.request("0",()->{throw new AssertionError();},v->{},Runnable::run,Runnable::run);
        c.request("32",()->new String[]{"new",""},v->{},Runnable::run,Runnable::run);
        c.request("0",()->{throw new AssertionError();},v->{},Runnable::run,Runnable::run);
        c.request("1",()->new String[]{"reload",""},v->assertEquals("reload",v[0]),Runnable::run,Runnable::run);
    }
    @Test public void abandonedQueuedSelectionDoesNotFetch() {
        LibraryInfoCache c=new LibraryInfoCache();
        List<Runnable> jobs=new ArrayList<>();
        Runnable cancel=c.request("a",()->{throw new AssertionError();},v->fail(),jobs::add,Runnable::run);
        cancel.run(); jobs.remove(0).run();
        c.request("a",()->new String[]{"retry",""},v->assertEquals("retry",v[0]),Runnable::run,Runnable::run);
    }
    @Test public void repeatedSelectionsReuseOneLookup() {
        LibraryInfoCache c=new LibraryInfoCache();
        AtomicInteger calls=new AtomicInteger();
        for(int i=0;i<100;i++) c.request("a",()->{calls.incrementAndGet(); return new String[]{"",""};},
            v->{},Runnable::run,Runnable::run);
        assertEquals(1,calls.get());
    }
}
