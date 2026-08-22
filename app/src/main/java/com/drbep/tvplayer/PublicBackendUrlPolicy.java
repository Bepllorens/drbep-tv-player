package com.drbep.tvplayer;

import java.util.Locale;

final class PublicBackendUrlPolicy {
    private static final String LEGACY_HTTP = "http://iptv.bepllorens.com";
    private static final String LEGACY_HTTPS = "https://iptv.bepllorens.com";

    private PublicBackendUrlPolicy() {
    }

    static String rebaseLegacyUrl(String value, String publicBaseUrl) {
        String url = value == null ? "" : value.trim();
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (url.isEmpty() || base.isEmpty()) {
            return url;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.equals(LEGACY_HTTPS) || lower.startsWith(LEGACY_HTTPS + "/")) {
            return base + url.substring(LEGACY_HTTPS.length());
        }
        if (lower.equals(LEGACY_HTTP) || lower.startsWith(LEGACY_HTTP + "/")) {
            return base + url.substring(LEGACY_HTTP.length());
        }
        return url;
    }
}
