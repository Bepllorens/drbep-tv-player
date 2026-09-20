package com.drbep.tvplayer;

import java.util.Locale;

final class TrackDisplayPolicy {
    private TrackDisplayPolicy() {}

    static String subtitle(String name, boolean forced) {
        return forced && !name.toLowerCase(Locale.ROOT).contains("forzad")
                ? name + " · Forzados" : name;
    }

    static String audio(String name, String mime, String codec, int channels, int bitrate,
                        boolean description, int number) {
        String encoding = codec == null ? "" : codec.toLowerCase(Locale.ROOT);
        String type = mime == null ? "" : mime;
        String format = type.contains("eac3-joc") ? "Dolby Atmos"
                : type.contains("eac3") || encoding.startsWith("ec-3") ? "Dolby Digital Plus"
                : type.contains("ac3") || encoding.startsWith("ac-3") ? "Dolby Digital"
                : type.contains("mp4a") || encoding.startsWith("mp4a") ? "AAC" : type.replace("audio/", "");
        StringBuilder result = new StringBuilder(name);
        if (!format.isEmpty()) result.append(" · ").append(format);
        if (channels > 0) result.append(" · ").append(channels == 2 ? "Estéreo" : channels == 6 ? "5.1"
                : channels == 8 ? "7.1" : channels + " canales");
        if (bitrate > 0) result.append(" · ").append(Math.round(bitrate / 1000f)).append(" kb/s");
        if (description) result.append(" · Audiodescripción");
        return result.append(" · Pista ").append(number).toString();
    }

    static int forcedPriority(boolean forced, String textLanguage, String preferredLanguage) {
        if (!forced || textLanguage == null || preferredLanguage == null) return 0;
        String text = textLanguage.toLowerCase(Locale.ROOT).replace('_', '-');
        String preferred = preferredLanguage.toLowerCase(Locale.ROOT).replace('_', '-');
        if (text.equals(preferred)) return 2;
        // A generic language may match a regional preference, not a different region.
        if (text.equals(preferred.split("-")[0])) return 1;
        return 0;
    }
}
