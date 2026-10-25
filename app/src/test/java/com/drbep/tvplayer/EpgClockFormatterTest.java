package com.drbep.tvplayer;

import org.junit.Test;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class EpgClockFormatterTest {
    @Test public void preservesLegacyClockAcrossLocaleZoneAndDstChanges() {
        Locale originalLocale = Locale.getDefault();
        TimeZone originalZone = TimeZone.getDefault();
        try {
            for (Locale locale : new Locale[]{Locale.US, new Locale("es", "ES"), new Locale("ar", "EG")}) {
                Locale.setDefault(locale);
                for (String zone : new String[]{"Europe/Madrid", "UTC", "America/New_York", "GMT+05:45"}) {
                    TimeZone.setDefault(TimeZone.getTimeZone(zone));
                    SimpleDateFormat reference = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    // Both sides of the Madrid DST transitions, day boundaries and repeated use.
                    for (long time : new long[]{1, 1774744200000L, 1774747800000L,
                            1792888200000L, 1792891800000L, 1792888200000L}) {
                        assertEquals(reference.format(new Date(time)), EpgClockFormatter.format(time));
                    }
                }
            }
            assertEquals("--:--", EpgClockFormatter.format(0));
            assertEquals("--:--", EpgClockFormatter.format(-1));
        } finally {
            Locale.setDefault(originalLocale);
            TimeZone.setDefault(originalZone);
        }
    }
    @Test public void concurrentCallersDoNotShareMutableFormatter() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Boolean>> jobs = new ArrayList<>();
            for (int worker=0; worker<4; worker++) {
                final int offset=worker;
                jobs.add(pool.submit(() -> {
                    SimpleDateFormat reference=new SimpleDateFormat("HH:mm", Locale.getDefault());
                    for (int i=0; i<500; i++) {
                        long time=1792888200000L+(offset*500L+i)*60000L;
                        if (!reference.format(new Date(time)).equals(EpgClockFormatter.format(time))) return false;
                    }
                    return true;
                }));
            }
            for (Future<Boolean> job:jobs) assertTrue(job.get());
        } finally { pool.shutdownNow(); }
    }
}
