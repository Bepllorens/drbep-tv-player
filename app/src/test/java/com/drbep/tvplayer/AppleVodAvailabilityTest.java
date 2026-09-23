package com.drbep.tvplayer;
import org.junit.Test;
import java.time.LocalDate;
import static org.junit.Assert.*;
public class AppleVodAvailabilityTest {
    @Test public void futureOnlyNotTodayOrMissing() {
        LocalDate today = LocalDate.of(2026,9,23);
        assertTrue(AppleVodAvailability.upcoming("2026-10-09",today));
        assertFalse(AppleVodAvailability.upcoming("2026-09-23",today));
        assertFalse(AppleVodAvailability.upcoming("2026-09-04",today));
        assertFalse(AppleVodAvailability.upcoming("",today));
        assertFalse(AppleVodAvailability.upcoming("invalid",today));
    }
}
