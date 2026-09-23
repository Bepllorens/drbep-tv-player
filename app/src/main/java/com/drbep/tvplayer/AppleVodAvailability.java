package com.drbep.tvplayer;

import java.time.LocalDate;
import java.time.ZoneId;

final class AppleVodAvailability {
    static boolean upcoming(String date) {
        return upcoming(date, LocalDate.now(ZoneId.of("Europe/Madrid")));
    }
    static boolean upcoming(String date, LocalDate today) {
        try { return LocalDate.parse(date).isAfter(today); }
        catch (RuntimeException ignored) { return false; }
    }
}
