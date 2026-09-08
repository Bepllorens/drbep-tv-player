package com.drbep.tvplayer;

import java.util.Locale;
import java.util.TimeZone;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecordingMetadataFormatterTest {
    @Test public void displaysLocalTimeWithoutRawIsoDate() {
        long time = EpgTimeCodec.parseEpochMillis("2026-04-09T14:19:28+02:00");
        assertEquals("14:19", RecordingMetadataFormatter.clockTime(time, Locale.US, TimeZone.getTimeZone("Europe/Madrid")));
        assertEquals("12:19", RecordingMetadataFormatter.clockTime(time, Locale.US, TimeZone.getTimeZone("UTC")));
    }
    @Test public void respectsWinterOffsetAndInvalidDates() {
        long time = EpgTimeCodec.parseEpochMillis("2026-01-09T14:19:28Z");
        assertEquals("15:19", RecordingMetadataFormatter.clockTime(time, Locale.US, TimeZone.getTimeZone("Europe/Madrid")));
        assertEquals("--:--", RecordingMetadataFormatter.clockTime(0, Locale.US, TimeZone.getTimeZone("UTC")));
        assertEquals("--:--", RecordingMetadataFormatter.clockTime(-1, Locale.US, TimeZone.getTimeZone("UTC")));
    }
}
