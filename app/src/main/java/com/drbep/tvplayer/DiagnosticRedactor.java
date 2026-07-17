package com.drbep.tvplayer;

import java.net.URI;

final class DiagnosticRedactor {
    private DiagnosticRedactor() {
    }

    static String sanitizeUrl(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        try {
            URI uri = URI.create(trimmed);
            if (uri.getScheme() != null && uri.getHost() != null) {
                URI clean = new URI(
                        uri.getScheme(),
                        uri.getRawAuthority(),
                        uri.getRawPath(),
                        null,
                        null
                );
                return clean.toString();
            }
        } catch (Exception ignored) {
            // Fall through to conservative text redaction below.
        }
        return redactSensitiveText(stripQueryAndFragment(trimmed));
    }

    static String redactSensitiveText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        return value.trim()
                .replaceAll("(?i)(Bearer\\s+)[A-Za-z0-9._~+/=-]+", "$1<redacted>")
                .replaceAll("(?i)([A-Za-z0-9_.-]*(?:access_token|token|refresh_token|authorization|auth|signature|sig|key|kid|device_id|license|nv-authorizations)[A-Za-z0-9_.-]*)=([^&\\s]+)", "$1=<redacted>");
    }

    private static String stripQueryAndFragment(String value) {
        if (value == null) {
            return "";
        }
        int queryIndex = value.indexOf('?');
        int fragmentIndex = value.indexOf('#');
        int cutIndex = -1;
        if (queryIndex >= 0) {
            cutIndex = queryIndex;
        }
        if (fragmentIndex >= 0 && (cutIndex < 0 || fragmentIndex < cutIndex)) {
            cutIndex = fragmentIndex;
        }
        return cutIndex >= 0 ? value.substring(0, cutIndex) : value;
    }
}
