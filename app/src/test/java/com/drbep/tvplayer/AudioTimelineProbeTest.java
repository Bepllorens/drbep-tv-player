package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AudioTimelineProbeTest {
    @Test public void coherentDolbyTimelineDoesNotDrift() {
        AudioTimelineProbe probe = new AudioTimelineProbe();
        for (int i=0; i<20_000; i++)
            assertEquals(0, probe.observe(309_000_000L + i * 32_000L, 1536, 48000));
    }
    @Test public void catchesSampleCountMismatch() {
        AudioTimelineProbe probe = new AudioTimelineProbe();
        probe.observe(0,768,48000);
        assertEquals(16000, probe.observe(32000,768,48000));
    }
    @Test public void seekAndResetStartNewWindow() {
        AudioTimelineProbe probe = new AudioTimelineProbe();
        probe.observe(1_000_000,1536,48000);
        assertEquals(0, probe.observe(0,1536,48000));
        probe.reset();
        assertEquals(0, probe.observe(9_000_000,1536,48000));
    }
    @Test public void unknownRateOrCountIsIgnored() {
        AudioTimelineProbe probe = new AudioTimelineProbe();
        assertEquals(Long.MIN_VALUE,probe.observe(0,0,48000));
        assertEquals(Long.MIN_VALUE,probe.observe(0,1536,0));
        assertEquals(0,probe.observe(500000,1536,48000));
    }
}
