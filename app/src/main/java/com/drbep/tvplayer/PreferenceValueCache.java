package com.drbep.tvplayer;

/** Process-local memoization; preference storage remains encrypted and authoritative. */
final class PreferenceValueCache {
    interface Decoder { String decode(String encoded) throws Exception; }
    private String source;
    private String value;
    synchronized String read(String encoded, Decoder decoder) throws Exception {
        if (encoded.equals(source)) return value;
        clear();
        String decoded = decoder.decode(encoded);
        // Failed decodes are never cached or replaced by a previous value.
        if (decoded != null) { source = encoded; value = decoded; }
        return decoded;
    }
    synchronized void clear() { source = null; value = null; }
}
