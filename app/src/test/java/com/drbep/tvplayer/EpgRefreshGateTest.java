package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class EpgRefreshGateTest {
    @Test public void emptyResponsesDoNotCauseRefreshLoop() {
        EpgRefreshGate gate = new EpgRefreshGate();
        assertTrue(gate.begin("a", 0));
        assertFalse(gate.begin("a", 100));
        gate.complete("a", 200, false, false);
        for (long time = 201; time < 300_200; time += 100) assertFalse(gate.begin("a", time));
        assertTrue(gate.begin("a", 300_200));
        gate.complete("a", 300_300, false, false);
        assertFalse(gate.begin("a", 900_299));
        assertTrue(gate.begin("a", 900_300));
    }

    @Test public void errorsRetryAndSuccessResetsBackoff() {
        EpgRefreshGate gate = new EpgRefreshGate();
        assertTrue(gate.begin("a", 0));
        gate.complete("a", 0, false, true);
        assertFalse(gate.begin("a", 14_999));
        assertTrue(gate.begin("a", 15_000));
        gate.complete("a", 15_000, false, true);
        assertFalse(gate.begin("a", 44_999));
        assertTrue(gate.begin("a", 45_000));
        gate.complete("a", 45_000, true, false);
        assertTrue(gate.begin("a", 105_000));
        gate.complete("a", 105_000, false, true);
        assertTrue(gate.begin("a", 120_000));
    }

    @Test public void channelsAreIndependentAndInFlightEntriesAreNeverEvicted() {
        EpgRefreshGate gate = new EpgRefreshGate();
        assertFalse(gate.begin("", 0));
        for (int i = 0; i < 256; i++) assertTrue(gate.begin("c" + i, 0));
        assertFalse(gate.begin("new", 1));
        gate.complete("c1", 2, false, false);
        assertTrue(gate.begin("new", 3));
        assertFalse(gate.begin("c0", 4));
    }
}
