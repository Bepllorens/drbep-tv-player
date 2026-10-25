package com.drbep.tvplayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Reuse the clock formatter, retaining runtime locale and time-zone changes. */
final class EpgClockFormatter {
    private static final ThreadLocal<State> LOCAL = new ThreadLocal<>();
    private static final class State {
        final Locale locale;
        final TimeZone zone;
        final SimpleDateFormat formatter;
        State(Locale locale, TimeZone zone) {
            this.locale = locale;
            this.zone = zone;
            formatter = new SimpleDateFormat("HH:mm", locale);
            formatter.setTimeZone(zone);
        }
    }
    static String format(long epochMillis) {
        if (epochMillis <= 0L) return "--:--";
        Locale locale = Locale.getDefault();
        TimeZone zone = TimeZone.getDefault();
        State state = LOCAL.get();
        if (state == null || !state.locale.equals(locale) || !state.zone.equals(zone)) {
            state = new State(locale, zone);
            LOCAL.set(state);
        }
        return state.formatter.format(new Date(epochMillis));
    }
    private EpgClockFormatter() { }
}
