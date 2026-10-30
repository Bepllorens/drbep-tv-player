package com.drbep.tvplayer;

import org.junit.Test;
import java.util.Locale;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class EpgScheduleFormatterTest {
    private static String old(String start, String end) {
        return EpgTimeCodec.parseEpochMillis(start) <= 0L ||
            EpgTimeCodec.parseEpochMillis(end) <= EpgTimeCodec.parseEpochMillis(start)
            ? "" : EpgClockFormatter.format(EpgTimeCodec.parseEpochMillis(start)) +
            " – " + EpgClockFormatter.format(EpgTimeCodec.parseEpochMillis(end));
    }
    @Test public void matchesPreviousOutputAcrossInputsLocalesAndZones() {
        Locale locale = Locale.getDefault();
        TimeZone zone = TimeZone.getDefault();
        String[] values = {null, "", "invalid", "0", "-1000000000",
            "2026-10-25T00:30:00Z", "2026-10-25T01:30:00Z",
            "2026-03-29T00:30:00Z", "2026-03-29T01:30:00Z",
            "2026-10-30 23:50:00", "2026-10-31 00:10:00",
            "20261030235000 +0100", "2026-10-31T01:10:00+02:00",
            "1792888200", "1792891800000"};
        try {
            for (Locale l : new Locale[]{Locale.US, new Locale("es","ES"), new Locale("ar","EG")}) {
                Locale.setDefault(l);
                for (String z : new String[]{"UTC","Europe/Madrid","America/New_York","GMT+05:45"}) {
                    TimeZone.setDefault(TimeZone.getTimeZone(z));
                    for (String a : values) for (String b : values)
                        assertEquals(old(a,b), EpgScheduleFormatter.format(a,b));
                }
            }
        } finally { Locale.setDefault(locale); TimeZone.setDefault(zone); }
    }
    @Test public void changedProgrammeTimesAreNotReused() {
        assertEquals("", EpgScheduleFormatter.format("2026-10-30T10:00:00Z","2026-10-30T09:00:00Z"));
        assertFalse(EpgScheduleFormatter.format("2026-10-30T10:00:00Z","2026-10-30T11:00:00Z").isEmpty());
        assertEquals("", EpgScheduleFormatter.format("2026-10-30T10:00:00Z","invalid"));
    }
}
