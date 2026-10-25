package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.TimeZone;

public class EpgUtcReuseTest {
    @Test public void utcReuseDoesNotDependOnDeviceTimezone() {
        TimeZone previous=TimeZone.getDefault();
        try {
            String value="2026-10-25T01:30:00.000Z";
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"));
            long epoch=EpgTimeCodec.parseEpochMillis(value);
            String normalized=EpgTimeCodec.normalizeUtc(value);
            assertEquals("2026-10-25T01:30:00Z", normalized);
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            assertEquals(epoch,EpgTimeCodec.parseEpochMillis(value));
            assertSame(normalized,EpgTimeCodec.normalizeUtc(value));
        } finally { TimeZone.setDefault(previous); }
    }
    @Test public void localTimesStillFollowDeviceTimezoneAfterUtcCacheIsWarm() {
        TimeZone previous=TimeZone.getDefault();
        try {
            EpgTimeCodec.normalizeUtc("2026-07-01T12:00:00Z");
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            long utc=EpgTimeCodec.parseEpochMillis("2026-07-01T12:00:00");
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"));
            assertEquals(utc-7200000,EpgTimeCodec.parseEpochMillis("2026-07-01T12:00:00"));
        } finally { TimeZone.setDefault(previous); }
    }
    @Test public void invalidZuluAndExplicitOffsetsPreservePreviousContract() {
        assertEquals(0,EpgTimeCodec.parseEpochMillis("not-a-dateZ"));
        assertEquals("not-a-dateZ",EpgTimeCodec.normalizeUtc("not-a-dateZ"));
        assertEquals("2026-10-21T12:00:00Z", EpgTimeCodec.normalizeUtc("2026-10-21T14:00:00+02:00"));
        assertEquals("2026-10-21T12:00:00.123Z", EpgTimeCodec.normalizeUtc("2026-10-21T12:00:00.123Z"));
    }
}
