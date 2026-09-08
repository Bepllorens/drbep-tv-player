package com.drbep.tvplayer;

import java.util.Locale;

final class OfflineCatalogRecoveryPolicy {
    static final long REFRESH_COOLDOWN_MS = 10L * 60L * 1000L;

    private OfflineCatalogRecoveryPolicy() {
    }

    static boolean shouldRefresh(
            String errorDetail,
            long nowMs,
            long lastRefreshMs,
            boolean hasAccessToken,
            String sourceUrl
    ) {
        if (!hasAccessToken || clean(sourceUrl).isEmpty()) {
            return false;
        }
        if (!isCatalogRelated(errorDetail)) {
            return false;
        }
        long elapsedMs = nowMs - lastRefreshMs;
        return lastRefreshMs <= 0L || elapsedMs >= REFRESH_COOLDOWN_MS;
    }

    static boolean isCatalogRelated(String errorDetail) {
        String normalized = clean(errorDetail).toLowerCase(Locale.ROOT);
        return normalized.contains("response code: 401")
                || normalized.contains("response code: 403")
                || normalized.contains("response code: 404")
                || normalized.contains("http 401")
                || normalized.contains("http 403")
                || normalized.contains("http 404")
                || normalized.contains("invalidresponsecode")
                || normalized.contains("source error")
                || normalized.contains("token")
                || normalized.contains("unauthorized")
                || normalized.contains("forbidden")
                || normalized.contains("not found");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
