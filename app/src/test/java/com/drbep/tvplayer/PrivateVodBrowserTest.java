package com.drbep.tvplayer;

import org.junit.Test;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import static org.junit.Assert.*;

public class PrivateVodBrowserTest {
    @Test public void labelsDistinguishAllFromOneSeason() {
        assertEquals("Todas las temporadas", PrivateVodBrowser.seasonLabel(0));
        assertEquals("Temporada 2", PrivateVodBrowser.seasonLabel(2));
    }
    static final class Host implements PrivateVodBrowser.Host {
        final BlockingQueue<Runnable> ui = new LinkedBlockingQueue<>();
        String message;
        int displays;
        int dismisses;
        public void dismiss() { dismisses++; }
        public void show(String title, String message, List<String> labels, List<Runnable> actions, Runnable back) {
            this.message = message; displays++;
        }
        public void search(String value, Consumer<String> submit, Runnable back) {}
        public void play(String provider, String revision, String assetId, String title) {}
        public void ui(Runnable action) { ui.add(action); }
    }

    @Test public void closeDiscardsAlreadyQueuedCompletion() throws Exception {
        Host host = new Host();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            PrivateVodBrowser browser = new PrivateVodBrowser(host, query -> {
                throw new IllegalStateException("HTTP 503");
            }, executor);
            browser.open(() -> {});
            Runnable completion = host.ui.poll(3, TimeUnit.SECONDS);
            assertNotNull(completion);
            browser.close();
            completion.run();
            assertEquals(1, host.displays);
            assertTrue(host.dismisses >= 2);
        } finally { executor.shutdownNow(); }
    }

    @Test public void errorsDoNotExposeServerDetails() throws Exception {
        Host host = new Host();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            PrivateVodBrowser browser = new PrivateVodBrowser(host, query -> {
                throw new IllegalStateException("HTTP 403 https://provider.invalid/?secret=PRIVATE");
            }, executor);
            browser.open(() -> {});
            Runnable completion = host.ui.poll(3, TimeUnit.SECONDS);
            assertNotNull(completion); completion.run();
            assertTrue(host.message.contains("no está habilitado"));
            assertFalse(host.message.contains("PRIVATE"));
            browser.close();
        } finally { executor.shutdownNow(); }
    }
}
