package com.drbep.tvplayer;

import java.net.HttpURLConnection;
import java.net.URL;

final class BackendFailoverManager {
    static final int PRIMARY_ATTEMPTS = 3;
    private static final int CONNECT_TIMEOUT_MS = 2500;
    private static final int READ_TIMEOUT_MS = 2500;
    private static final long RETRY_DELAY_MS = 350L;

    interface Probe {
        boolean isHealthy(String baseUrl);
    }

    interface Sleeper {
        void sleep(long delayMs) throws InterruptedException;
    }

    static final class Decision {
        final String selectedBaseUrl;
        final boolean useEmergency;
        final int primaryFailures;
        final boolean emergencyHealthy;

        Decision(String selectedBaseUrl, boolean useEmergency, int primaryFailures, boolean emergencyHealthy) {
            this.selectedBaseUrl = normalize(selectedBaseUrl);
            this.useEmergency = useEmergency;
            this.primaryFailures = Math.max(0, primaryFailures);
            this.emergencyHealthy = emergencyHealthy;
        }
    }

    private BackendFailoverManager() {
    }

    static Decision evaluate(String primaryBaseUrl, String emergencyBaseUrl) {
        return evaluate(primaryBaseUrl, emergencyBaseUrl, BackendFailoverManager::probeHealth, Thread::sleep);
    }

    static Decision evaluate(String primaryBaseUrl, String emergencyBaseUrl, Probe probe, Sleeper sleeper) {
        String primary = normalize(primaryBaseUrl);
        String emergency = normalize(emergencyBaseUrl);
        if (primary.isEmpty() || probe == null) {
            return new Decision(primary, false, 0, false);
        }

        int failures = 0;
        for (int attempt = 0; attempt < PRIMARY_ATTEMPTS; attempt++) {
            if (probe.isHealthy(primary)) {
                return new Decision(primary, false, failures, false);
            }
            failures++;
            if (attempt + 1 < PRIMARY_ATTEMPTS && sleeper != null) {
                try {
                    sleeper.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return new Decision(primary, false, failures, false);
                }
            }
        }

        if (emergency.isEmpty() || emergency.equalsIgnoreCase(primary)) {
            return new Decision(primary, false, failures, false);
        }
        boolean emergencyHealthy = probe.isHealthy(emergency);
        return new Decision(emergencyHealthy ? emergency : primary, emergencyHealthy, failures, emergencyHealthy);
    }

    static boolean probeHealth(String baseUrl) {
        String normalized = normalize(baseUrl);
        if (normalized.isEmpty()) {
            return false;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(normalized + "/health").openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Cache-Control", "no-cache");
            int code = connection.getResponseCode();
            return code >= 200 && code < 300;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    static String normalize(String value) {
        String clean = value == null ? "" : value.trim();
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }
}
