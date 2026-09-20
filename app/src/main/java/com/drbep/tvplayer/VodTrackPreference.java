package com.drbep.tvplayer;

/** Stable identity independent of manifest ordering and transient track IDs. */
final class VodTrackPreference {
    /** Prefer an exact variant; tolerate changed encoding, never language/role changes. */
    static int matchScore(String saved, String candidate) {
        if (saved == null || saved.isEmpty()) return 0;
        if (saved.equals(candidate)) return 1000;
        String[] a = decode(saved), b = decode(candidate);
        if (a == null || b == null || a[0].isEmpty() || !a[0].equals(b[0])
                || !a[6].equals(b[6]) || !a[7].equals(b[7])) return 0;
        return 100 + (a[1].equals(b[1]) ? 40 : 0) + (a[2].equals(b[2]) ? 20 : 0)
                + (a[3].equals(b[3]) ? 10 : 0) + (a[4].equals(b[4]) ? 8 : 0)
                + (a[5].equals(b[5]) ? 4 : 0);
    }

    static String language(String identity) {
        String[] fields = decode(identity);
        return fields == null ? "" : fields[0];
    }

    private static String[] decode(String identity) {
        if (identity == null) return null;
        try {
            String[] fields = new String[8];
            int offset = 0;
            for (int i = 0; i < 4; i++) {
                int colon = identity.indexOf(':', offset);
                int length = Integer.parseInt(identity.substring(offset, colon));
                if (length < 0 || length > identity.length() - colon - 1) return null;
                fields[i] = identity.substring(colon + 1, colon + 1 + length);
                offset = colon + 1 + length;
            }
            String[] technical = identity.substring(offset).split(":", -1);
            if (technical.length != 4) return null;
            System.arraycopy(technical, 0, fields, 4, 4);
            // The manifest's default marker is not a semantic user preference.
            fields[7] = Integer.toString(Integer.parseInt(fields[7]) & ~1);
            return fields;
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    static String identity(String language, String label, String mime, String codecs,
                           int channels, int bitrate, int roles, int flags) {
        return part(language) + part(label) + part(mime) + part(codecs)
                + channels + ":" + bitrate + ":" + roles + ":" + flags;
    }
    private static String part(String value) {
        String text = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        return text.length() + ":" + text;
    }
}
