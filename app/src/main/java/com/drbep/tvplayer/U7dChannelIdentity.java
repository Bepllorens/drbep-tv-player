package com.drbep.tvplayer;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Canonical channel identity, keeping the name of the Movistar Plus channel. */
final class U7dChannelIdentity {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern QUALITY = Pattern.compile("\\b(uhd|fhd|hd|sd|ism)\\b");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern PLUS_ALIAS = Pattern.compile("(?:m|movistar)\\s*\\+");
    private static final Pattern PREFIXES = Pattern.compile("\\b(uhd|fhd|hd|sd|movistar|ism|m|plus)\\b");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    static boolean matches(String left, String right) {
        String a=normalize(left), b=normalize(right);
        return !a.isEmpty() && a.equals(b);
    }
    static String normalize(String value) {
        String text = DIACRITICS.matcher(Normalizer.normalize(value == null ? "" : value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)).replaceAll("");
        String noQuality = QUALITY.matcher(text).replaceAll(" ").trim();
        String compact = NON_ALNUM.matcher(noQuality).replaceAll("");
        if (compact.equals("movistarplus") || compact.equals("mplus")
                || PLUS_ALIAS.matcher(noQuality).matches()) return "mplus";
        return WHITESPACE.matcher(NON_ALNUM.matcher(PREFIXES.matcher(text).replaceAll(" ")).replaceAll(" ").trim()).replaceAll(" ");
    }
}
