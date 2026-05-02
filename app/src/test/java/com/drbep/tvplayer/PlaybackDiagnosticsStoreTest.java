package com.drbep.tvplayer;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PlaybackDiagnosticsStoreTest {
    @Test
    public void recordsAndClearsLastErrorForChannel() {
        PlaybackDiagnosticsStore store = new PlaybackDiagnosticsStore(null, "diagnostics");

        store.recordError("ch-1", "Canal", "fallo", "Directo", "direct");

        PlaybackDiagnosticsStore.ErrorRecord record = store.getLastError("ch-1");
        assertEquals("Canal", record.channelName);
        assertEquals("fallo", record.message);
        assertEquals("Directo", record.routeLabel);
        assertEquals("direct", record.playbackMode);
        assertTrue(record.timestampMs > 0L);

        store.clear("ch-1");

        assertNull(store.getLastError("ch-1"));
    }

    @Test
    public void returnsRecentErrorsAndClearsAll() throws Exception {
        PlaybackDiagnosticsStore store = new PlaybackDiagnosticsStore(null, "diagnostics");

        store.recordError("ch-1", "Uno", "fallo 1", "Directo", "direct");
        Thread.sleep(2L);
        store.recordError("ch-2", "Dos", "fallo 2", "Proxy", "proxy");

        List<PlaybackDiagnosticsStore.ErrorRecord> recent = store.getRecentErrors(10);

        assertEquals(2, recent.size());
        assertEquals("ch-2", recent.get(0).channelId);
        assertEquals("ch-1", recent.get(1).channelId);

        store.clearAll();

        assertTrue(store.getRecentErrors(10).isEmpty());
    }
}
