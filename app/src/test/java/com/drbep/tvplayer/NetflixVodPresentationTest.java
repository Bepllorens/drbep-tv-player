package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class NetflixVodPresentationTest {
    @Test public void discoveryBadgeExpiresWithoutLosingDate() throws Exception {
        java.text.SimpleDateFormat parser = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT);
        parser.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        long now = parser.parse("2026-09-23").getTime();
        assertTrue(NetflixVodPresentation.discovery("2026-09-23", now).startsWith("Novedad · Añadido a DRBEP:"));
        assertFalse(NetflixVodPresentation.discovery("2026-09-23", now+31L*86400000L).contains("Novedad"));
        assertEquals("", NetflixVodPresentation.discovery("", now));
        assertEquals("", NetflixVodPresentation.discovery("2026-02-30", now));
    }
    @Test public void dateKeepsProviderCalendarDayAndYearPrecision() {
        assertEquals("23/9/2026", NetflixVodPresentation.date("2026-09-23T00:00:00Z", 0));
        assertEquals("23/9/2026", NetflixVodPresentation.date("2026-09-23", 2025));
        assertEquals("Año: 2026", NetflixVodPresentation.date("", 2026));
        assertEquals("Fecha no disponible", NetflixVodPresentation.date(null, 0));
    }
    @Test public void invalidDatesAreNotDisplayedAsRealDates() {
        assertEquals("Fecha no disponible", NetflixVodPresentation.date("2025-02-29", 0));
        assertEquals("29/2/2024", NetflixVodPresentation.date("2024-02-29", 0));
        assertEquals("Año: 2026", NetflixVodPresentation.date("2026-13-01", 2026));
        assertEquals("Fecha no disponible", NetflixVodPresentation.date("bad", 10000));
    }
    @Test public void unknownNumbersNeverAppearAsSeasonZero() {
        assertEquals("Pilot", NetflixVodPresentation.title("episode", "Pilot", 0, 0));
        assertEquals("E4 — Pilot", NetflixVodPresentation.title("episode", "Pilot", 0, 4));
        assertEquals("T2 · E4 — Pilot", NetflixVodPresentation.title("episode", "Pilot", 2, 4));
        assertEquals("Movie", NetflixVodPresentation.title("movie", "Movie", 2, 4));
    }
    @Test public void factsKeepSeriesContextAndNeverInventZeroDuration() {
        assertEquals("Series · Año: 2026 · 2 min", NetflixVodPresentation.facts("episode", "Series", 0, "", 2026, 61));
        assertEquals("Año: 2026", NetflixVodPresentation.facts("movie", "", 0, "", 2026, 0));
        assertEquals("42 episodios · Último episodio · Año: 2026", NetflixVodPresentation.facts("series", "", 42, "", 2026, 0));
    }
    @Test public void missingSynopsisIsExplicitAndTextIsPreserved() {
        assertEquals("Sin sinopsis disponible.", NetflixVodPresentation.synopsis("  "));
        assertEquals("A story", NetflixVodPresentation.synopsis(" A story "));
    }
}
