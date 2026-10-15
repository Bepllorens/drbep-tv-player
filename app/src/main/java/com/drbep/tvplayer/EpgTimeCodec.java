package com.drbep.tvplayer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

final class EpgTimeCodec {
    private static final Pattern NUMERIC_EPOCH = Pattern.compile("-?[0-9]{10,17}");
    // Time parsing is pure for a value and fallback zone. Keep memory bounded.
    private static final Map<String, Long> PARSED = new LinkedHashMap<String, Long>(256, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Long> entry) { return size() > 2048; }
    };
    private static final DateTimeFormatter XMLTV_WITH_OFFSET = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("yyyyMMddHHmmss")
            .optionalStart()
            .appendLiteral(' ')
            .optionalEnd()
            .appendOffset("+HHMM", "Z")
            .toFormatter(Locale.ROOT);
    private static final DateTimeFormatter LEGACY_LOCAL = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .toFormatter(Locale.ROOT);

    private EpgTimeCodec() {
    }

    static long parseEpochMillis(String value) {
        return parseEpochMillis(value, ZoneId.systemDefault());
    }

    static long parseEpochMillis(String value, ZoneId fallbackZone) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        String clean = value.trim();
        ZoneId effectiveZone = fallbackZone == null ? ZoneId.systemDefault() : fallbackZone;
        if (clean.length() > 160) return parseUncached(clean, effectiveZone);
        String key = effectiveZone.getId() + '\u0000' + clean;
        synchronized (PARSED) { Long cached = PARSED.get(key); if (cached != null) return cached; }
        long parsed = parseUncached(clean, effectiveZone);
        synchronized (PARSED) { PARSED.put(key, parsed); }
        return parsed;
    }

    private static long parseUncached(String clean, ZoneId fallbackZone) {
        Long numeric = parseNumericEpoch(clean);
        if (numeric != null) {
            return numeric;
        }
        try {
            return Instant.parse(clean).toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(clean).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return ZonedDateTime.parse(clean).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(clean, XMLTV_WITH_OFFSET).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        ZoneId zone = fallbackZone == null ? ZoneId.systemDefault() : fallbackZone;
        try {
            return LocalDateTime.parse(clean).atZone(zone).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(clean, LEGACY_LOCAL).atZone(zone).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
            return 0L;
        }
    }

    static String normalizeUtc(String value) {
        long epochMillis = parseEpochMillis(value);
        return epochMillis <= 0L ? safe(value) : formatUtc(epochMillis);
    }

    static String formatUtc(long epochMillis) {
        return epochMillis <= 0L ? "" : Instant.ofEpochMilli(epochMillis).toString();
    }

    private static Long parseNumericEpoch(String value) {
        if (!NUMERIC_EPOCH.matcher(value).matches()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value);
            return Math.abs(parsed) < 100_000_000_000L ? parsed * 1000L : parsed;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
