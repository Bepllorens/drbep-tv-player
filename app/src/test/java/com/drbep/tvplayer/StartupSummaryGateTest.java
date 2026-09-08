package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class StartupSummaryGateTest {
    @Test public void acceptsOnlyWhileVisible() {
        StartupSummaryGate gate = new StartupSummaryGate();
        assertFalse(gate.accepts(gate.current()));
        long request = gate.open();
        assertTrue(gate.accepts(request));
        assertTrue(gate.close(request));
        assertFalse(gate.accepts(request));
        assertFalse(gate.close(request));
    }
    @Test public void lateResponseCannotUpdateReopenedHome() {
        StartupSummaryGate gate = new StartupSummaryGate();
        long oldRequest = gate.open();
        gate.close(oldRequest);
        long currentRequest = gate.open();
        assertFalse(gate.accepts(oldRequest));
        assertFalse(gate.close(oldRequest));
        assertTrue(gate.accepts(currentRequest));
    }
    @Test public void newerOpenInvalidatesPreviousEvenBeforeDismiss() {
        StartupSummaryGate gate = new StartupSummaryGate();
        long oldRequest = gate.open();
        long next = gate.open();
        assertFalse(gate.accepts(oldRequest));
        assertFalse(gate.close(oldRequest));
        assertTrue(gate.accepts(next));
    }
}
