package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Netflix metadata presentation only. Never infers playback capabilities. */
final class NetflixVodPresentation {
    private static final Pattern DATE = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})(?:T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2}))?$");

    static String date(String raw, int year) {
        Matcher match = DATE.matcher(clean(raw));
        if (match.matches()) {
            int y = Integer.parseInt(match.group(1));
            int m = Integer.parseInt(match.group(2));
            int d = Integer.parseInt(match.group(3));
            int[] days = {31, (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) ? 29 : 28,
                    31,30,31,30,31,31,30,31,30,31};
            if (y > 0 && m > 0 && m <= 12 && d > 0 && d <= days[m-1])
                return d + "/" + m + "/" + y;
        }
        return year > 0 && year <= 9999 ? "Año: " + year : "Fecha no disponible";
    }

    static String title(String kind, String raw, int season, int episode) {
        String title = clean(raw);
        if (title.isEmpty()) title = "Título no disponible";
        if (!"episode".equals(kind)) return title;
        String prefix = season > 0 ? "T" + season : "";
        if (episode > 0) prefix += (prefix.isEmpty() ? "" : " · ") + "E" + episode;
        return prefix.isEmpty() ? title : prefix + " — " + title;
    }

    static String facts(String kind, String series, int episodes, String airDate, int year, long seconds) {
        List<String> parts = new ArrayList<>();
        if ("episode".equals(kind) && !clean(series).isEmpty()) parts.add(clean(series));
        if ("series".equals(kind) && episodes > 0) parts.add(episodes + (episodes == 1 ? " episodio" : " episodios"));
        String date = date(airDate, year);
        if ("series".equals(kind) && !date.equals("Fecha no disponible")) date = "Último episodio · " + date;
        parts.add(date);
        if (!"series".equals(kind) && seconds > 0) parts.add((seconds / 60 + (seconds % 60 == 0 ? 0 : 1)) + " min");
        return String.join(" · ", parts);
    }

    static String synopsis(String raw) {
        String value = clean(raw);
        return value.isEmpty() ? "Sin sinopsis disponible." : value;
    }

    static String discovery(String raw, long now) {
        if (raw == null || !raw.matches("\\d{4}-\\d{2}-\\d{2}")) return "";
        try {
            java.text.SimpleDateFormat parser = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT);
            parser.setLenient(false);
            parser.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            long age = now - parser.parse(raw).getTime();
            return (age >= 0 && age < 30L * 86400000L ? "Novedad · " : "") + "Añadido a DRBEP: " + date(raw, 0) + " · ";
        } catch (java.text.ParseException ignored) { return ""; }
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
}
