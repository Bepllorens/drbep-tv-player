package com.drbep.tvplayer;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class U7dBrowserRowTest {
    @Test public void constructingAndReadingRowDoesNotStartReplay() {
        AtomicInteger plays = new AtomicInteger();
        U7dBrowserRow row = new U7dBrowserRow("viernes", "Programa", "20:00 – 21:00",
                "Sinopsis", null, plays::incrementAndGet);
        assertEquals("viernes", row.day);
        assertEquals("Programa", row.title);
        assertEquals("20:00 – 21:00", row.meta);
        assertEquals("Sinopsis", row.description);
        assertEquals(0, plays.get());
        row.play.run();
        assertEquals(1, plays.get());
    }

    @Test public void artworkIsOptionalWithoutLosingProgramDetails() {
        U7dBrowserRow row = new U7dBrowserRow("sábado", "Sin póster", "12:00 – 13:00",
                "Descripción disponible", null, () -> {});
        assertNull(row.artwork);
        assertEquals("Descripción disponible", row.description);
    }
}
