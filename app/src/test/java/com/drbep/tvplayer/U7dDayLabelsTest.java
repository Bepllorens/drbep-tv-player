package com.drbep.tvplayer;
import org.junit.Test;
import java.util.Calendar;
import static org.junit.Assert.*;
public class U7dDayLabelsTest {
    private long date(int year, int month, int day, int hour) {
        Calendar c = Calendar.getInstance(); c.clear(); c.set(year, month-1, day, hour, 0); return c.getTimeInMillis();
    }
    @Test public void yesterdayWorksAcrossYearBoundary() {
        assertEquals("Hoy", U7dDayLabels.primary(date(2027,1,1,1), date(2027,1,1,23), ""));
        assertEquals("Ayer", U7dDayLabels.primary(date(2026,12,31,23), date(2027,1,1,1), ""));
        assertEquals("Miércoles", U7dDayLabels.primary(date(2026,12,30,12), date(2027,1,1,1), ""));
    }
    @Test public void programmeSectionsFollowStartHour() {
        assertEquals("Madrugada", U7dDayLabels.section(date(2026,10,8,5)));
        assertEquals("Mañana", U7dDayLabels.section(date(2026,10,8,6)));
        assertEquals("Tarde", U7dDayLabels.section(date(2026,10,8,14)));
        assertEquals("Noche", U7dDayLabels.section(date(2026,10,8,21)));
    }
}
