package com.drbep.tvplayer;

import java.text.Normalizer;
import java.util.Locale;

/** Canonical channel identity, keeping the name of the Movistar Plus channel. */
final class U7dChannelIdentity {
    static boolean matches(String left, String right) {
        String a=normalize(left), b=normalize(right);
        return !a.isEmpty() && a.equals(b);
    }
    static String normalize(String value) {
        String text = Normalizer.normalize(value == null ? "" : value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String noQuality = text.replaceAll("\\b(uhd|fhd|hd|sd|ism)\\b", " ").trim();
        String compact = noQuality.replaceAll("[^a-z0-9]+", "");
        // These are channel names, not disposable provider prefixes. Removing
        // both words used to leave an empty identity and lose all programmes.
        if (compact.equals("movistarplus") || compact.equals("mplus")
                || noQuality.matches("(?:m|movistar)\\s*\\+")) return "mplus";
        return text.replaceAll("\\b(uhd|fhd|hd|sd|movistar|ism|m|plus)\\b", " ")
                .replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }
}
