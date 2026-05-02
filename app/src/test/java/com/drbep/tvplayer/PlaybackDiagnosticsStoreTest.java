package com.drbep.tvplayer;

import org.junit.Test;

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
}
