package com.drbep.tvplayer;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

final class U7dDayLabels {
    private static final Locale ES = new Locale("es", "ES");
    static String primary(long start, long now, String fallback) {
        if (start <= 0) return fallback;
        Calendar current = Calendar.getInstance(); current.setTimeInMillis(now);
        Calendar date = Calendar.getInstance(); date.setTimeInMillis(start);
        if (sameDay(current, date)) return "Hoy";
        current.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(current, date)) return "Ayer";
        String label = new SimpleDateFormat("EEEE", ES).format(new Date(start));
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }
    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
    static String secondary(long start, String fallback) {
        return start <= 0 ? fallback : new SimpleDateFormat("d MMM", ES).format(new Date(start));
    }
    static String section(long start) {
        Calendar date = Calendar.getInstance(); date.setTimeInMillis(start);
        int hour = date.get(Calendar.HOUR_OF_DAY);
        return hour < 6 ? "Madrugada" : hour < 14 ? "Mañana" : hour < 21 ? "Tarde" : "Noche";
    }
}
