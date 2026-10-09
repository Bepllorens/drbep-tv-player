package com.drbep.tvplayer;
import java.util.Calendar;
import java.util.TimeZone;
final class LiveDvrClock {
    static long resolve(String input, long start, long end, TimeZone zone) {
        if (input == null || !input.trim().matches("[0-2][0-9]:[0-5][0-9]") || start <= 0 || end < start) return -1;
        String value = input.trim(); int hour = Integer.parseInt(value.substring(0,2));
        if (hour > 23) return -1;
        Calendar candidate = Calendar.getInstance(zone); candidate.setTimeInMillis(end);
        candidate.set(Calendar.HOUR_OF_DAY, hour); candidate.set(Calendar.MINUTE, Integer.parseInt(value.substring(3)));
        candidate.set(Calendar.SECOND, 0); candidate.set(Calendar.MILLISECOND, 0);
        if (candidate.getTimeInMillis() > end) candidate.add(Calendar.DAY_OF_MONTH, -1);
        long time = candidate.getTimeInMillis(); return time >= start && time <= end ? time : -1;
    }
}
