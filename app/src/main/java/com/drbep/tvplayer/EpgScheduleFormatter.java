package com.drbep.tvplayer;

/** Display-only interval; no programme or channel cache. */
final class EpgScheduleFormatter {
    static String format(String start, String end) {
        long startMs = EpgTimeCodec.parseEpochMillis(start);
        if (startMs <= 0L) return "";
        long endMs = EpgTimeCodec.parseEpochMillis(end);
        if (endMs <= startMs) return "";
        return EpgClockFormatter.format(startMs) + " – " + EpgClockFormatter.format(endMs);
    }
    private EpgScheduleFormatter() {}
}
