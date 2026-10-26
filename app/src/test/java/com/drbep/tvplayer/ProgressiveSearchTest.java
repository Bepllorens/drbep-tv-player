package com.drbep.tvplayer;

import org.junit.Test;
import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ProgressiveSearchTest {
    private static ProgressiveSearch.Source<String> source(String id, Callable<List<String>> body) {
        return new ProgressiveSearch.Source<>(id, 2000L, false, body);
    }
    @Test public void slowProviderDoesNotDelayGuideAndFinalResultsMatch() throws Exception {
        try (ProgressiveSearch<String> search = new ProgressiveSearch<>()) {
            CountDownLatch release = new CountDownLatch(1), guide = new CountDownLatch(1), done = new CountDownLatch(1);
            List<ProgressiveSearch.Snapshot<String>> events = new CopyOnWriteArrayList<>();
            search.start(Arrays.asList(source("local", () -> Arrays.asList("tv")),
                    source("plex", () -> { release.await(); return Arrays.asList("film"); }),
                    source("epg", () -> Arrays.asList("show"))), false, s -> {
                events.add(s);
                if (s.parts.get(2).state == ProgressiveSearch.State.READY) guide.countDown();
                if (s.complete()) done.countDown();
            });
            assertTrue(guide.await(1, TimeUnit.SECONDS));
            assertEquals(ProgressiveSearch.State.LOADING, events.get(events.size()-1).parts.get(1).state);
            release.countDown();
            assertTrue(done.await(1, TimeUnit.SECONDS));
            List<String> rows = new ArrayList<>();
            for (ProgressiveSearch.Part<String> p : events.get(events.size()-1).parts) rows.addAll(p.rows);
            assertEquals(Arrays.asList("tv", "film", "show"), rows);
        }
    }
    @Test public void canceledUncooperativeResultCannotOverwriteNewSearch() throws Exception {
        try (ProgressiveSearch<String> search = new ProgressiveSearch<>()) {
            CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), returned = new CountDownLatch(1);
            AtomicInteger oldReady = new AtomicInteger();
            search.start(Arrays.asList(source("old", () -> {
                started.countDown();
                while (release.getCount() > 0) { try { release.await(); } catch (InterruptedException ignored) {} }
                returned.countDown(); return Arrays.asList("obsolete");
            })), false, s -> { if (s.complete()) oldReady.incrementAndGet(); });
            assertTrue(started.await(1,TimeUnit.SECONDS));
            CountDownLatch done = new CountDownLatch(1);
            search.start(Arrays.asList(source("new", () -> Arrays.asList("current"))), false,
                    s -> { if (s.complete()) { assertEquals(Arrays.asList("current"),s.parts.get(0).rows); done.countDown(); } });
            assertTrue(done.await(1,TimeUnit.SECONDS));
            release.countDown(); assertTrue(returned.await(1,TimeUnit.SECONDS));
            search.cancel();
            assertEquals(0,oldReady.get());
        }
    }
    @Test public void deadlineIsExplicitAndDoesNotEraseSuccessfulSource() throws Exception {
        try (ProgressiveSearch<String> search = new ProgressiveSearch<>()) {
            CountDownLatch done=new CountDownLatch(1);
            List<ProgressiveSearch.Snapshot<String>> events=new CopyOnWriteArrayList<>();
            search.start(Arrays.asList(source("local", () -> Arrays.asList("saved")),
                    new ProgressiveSearch.Source<>("offline",40L,true,()-> {new CountDownLatch(1).await();return Collections.emptyList();})),
                    false,s->{events.add(s);if(s.complete())done.countDown();});
            assertTrue(done.await(1,TimeUnit.SECONDS));
            ProgressiveSearch.Snapshot<String> last=events.get(events.size()-1);
            assertEquals(Arrays.asList("saved"),last.parts.get(0).rows);
            assertEquals(ProgressiveSearch.State.TIMED_OUT,last.parts.get(1).state);
        }
    }
    @Test public void networkFailureHasCooldownAndExplicitRetryBypassesIt() throws Exception {
        try (ProgressiveSearch<String> search = new ProgressiveSearch<>()) {
            AtomicInteger calls=new AtomicInteger();
            ProgressiveSearch.Source<String> offline=new ProgressiveSearch.Source<>("plex",1000L,true,()->{calls.incrementAndGet();throw new ConnectException();});
            CountDownLatch first=new CountDownLatch(1);
            search.start(Arrays.asList(offline),false,s->{if(s.complete())first.countDown();});
            assertTrue(first.await(1,TimeUnit.SECONDS));
            List<ProgressiveSearch.Snapshot<String>> next=new ArrayList<>();
            search.start(Arrays.asList(offline),false,next::add);
            assertEquals(ProgressiveSearch.State.COOLDOWN,next.get(0).parts.get(0).state);
            assertEquals(1,calls.get());
            CountDownLatch retried=new CountDownLatch(1);
            search.start(Arrays.asList(offline),true,s->{if(s.complete())retried.countDown();});
            assertTrue(retried.await(1,TimeUnit.SECONDS));
            assertEquals(2,calls.get());
        }
    }
    @Test public void emptySuccessIsNotNetworkFailureAndCanBeSearchedAgain() throws Exception {
        try(ProgressiveSearch<String> search=new ProgressiveSearch<>()) {
            AtomicInteger calls=new AtomicInteger();
            for(int i=0;i<2;i++) {
                CountDownLatch done=new CountDownLatch(1);
                search.start(Arrays.asList(new ProgressiveSearch.Source<>("plex",1000L,true,()->{calls.incrementAndGet();return Collections.emptyList();})),false,
                        s->{if(s.complete()){assertEquals(ProgressiveSearch.State.READY,s.parts.get(0).state);done.countDown();}});
                assertTrue(done.await(1,TimeUnit.SECONDS));
            }
            assertEquals(2,calls.get());
        }
    }
    @Test public void snapshotsAreImmutableAndCloseSuppressesNotifications() throws Exception {
        ProgressiveSearch<String> search=new ProgressiveSearch<>();
        CountDownLatch done=new CountDownLatch(1);
        List<ProgressiveSearch.Snapshot<String>> events=new CopyOnWriteArrayList<>();
        List<String> input=new ArrayList<>(Arrays.asList("one"));
        search.start(Arrays.asList(source("local",()->input)),false,s->{events.add(s);if(s.complete())done.countDown();});
        assertTrue(done.await(1,TimeUnit.SECONDS));
        input.add("two");
        assertEquals(Arrays.asList("one"),events.get(events.size()-1).parts.get(0).rows);
        search.close();
        int before=events.size();
        search.start(Arrays.asList(source("none",Collections::emptyList)),false,events::add);
        assertEquals(before,events.size());
    }
}
