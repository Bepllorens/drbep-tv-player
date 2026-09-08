package com.drbep.tvplayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class RecordingMetadataFormatter {
    private RecordingMetadataFormatter() {}
    static String clockTime(long epochMillis, Locale locale, TimeZone zone) {
        if (epochMillis <= 0) return "--:--";
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", locale);
        formatter.setTimeZone(zone);
        return formatter.format(new Date(epochMillis));
    }
}
