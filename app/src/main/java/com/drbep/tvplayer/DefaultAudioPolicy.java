package com.drbep.tvplayer;

import java.util.Locale;

/** Shared by every provider: Spain Spanish, unqualified Spanish, other Spanish. */
final class DefaultAudioPolicy {
    private DefaultAudioPolicy() {}

    static int priority(String language, String label) {
        String lang = language == null ? "" : language.toLowerCase(Locale.ROOT).replace('_', '-');
        String name = label == null ? "" : label.toLowerCase(Locale.ROOT);
        if (lang.equals("es-es") || lang.equals("spa-es")) return 300;
        if (name.contains("castellano") || name.contains("español de españa")
                || name.contains("español (españa)") || name.contains("spanish (spain)")) return 300;
        if (lang.equals("es") || lang.equals("spa")) {
            if (name.contains("latino") || name.contains("latin") || name.contains("méxico")) return 100;
            return 200;
        }
        if (lang.startsWith("es-") || lang.startsWith("spa-")) return 100;
        return 0;
    }
}
