package com.drbep.tvplayer;

import java.util.Collections;

final class BackendFailoverManager {
    static final int PRIMARY_ATTEMPTS = 3;
    private static final int CONNECT_TIMEOUT_MS = 2500;
    private static final int READ_TIMEOUT_MS = 2500;
    private static final long RETRY_DELAY_MS = 350L;

    interface Probe {
        boolean isHealthy(String baseUrl);
    }

    interface DetailedProbe {
        ProbeResult probe(String baseUrl);
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

    static final class ProbeResult {
        final boolean healthy;
        final boolean transportFailure;

        private ProbeResult(boolean healthy, boolean transportFailure) {
            this.healthy = healthy;
            this.transportFailure = transportFailure;
        }

        static ProbeResult healthy() {
            return new ProbeResult(true, false);
        }

        static ProbeResult unavailable() {
            return new ProbeResult(false, false);
        }

        static ProbeResult transportFailure() {
            return new ProbeResult(false, true);
        }
    }

    private BackendFailoverManager() {
    }

    static Decision evaluate(String primaryBaseUrl, String emergencyBaseUrl) {
        return evaluateDetailed(primaryBaseUrl, emergencyBaseUrl, BackendFailoverManager::probeHealthResult, Thread::sleep);
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

    static Decision evaluateDetailed(String primaryBaseUrl, String emergencyBaseUrl,
                                     DetailedProbe probe, Sleeper sleeper) {
        String primary = normalize(primaryBaseUrl);
        String emergency = normalize(emergencyBaseUrl);
        if (primary.isEmpty() || probe == null) {
            return new Decision(primary, false, 0, false);
        }

        int failures = 0;
        for (int attempt = 0; attempt < PRIMARY_ATTEMPTS; attempt++) {
            ProbeResult result = safeProbe(probe, primary);
            if (result.healthy) {
                return new Decision(primary, false, failures, false);
            }
            failures++;
            if (result.transportFailure) {
                break;
            }
            if (attempt + 1 < PRIMARY_ATTEMPTS && sleeper != null) {
                try {
                    sleeper.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return new Decision(primary, false, failures, false);
                }
            }
        }

        return evaluateEmergency(primary, emergency, failures, probe);
    }

    static Decision evaluateAfterTransportFailure(String primaryBaseUrl, String emergencyBaseUrl) {
        return evaluateAfterTransportFailure(
                primaryBaseUrl,
                emergencyBaseUrl,
                BackendFailoverManager::probeHealthResult
        );
    }

    static Decision evaluateAfterTransportFailure(String primaryBaseUrl, String emergencyBaseUrl,
                                                  DetailedProbe probe) {
        String primary = normalize(primaryBaseUrl);
        String emergency = normalize(emergencyBaseUrl);
        if (primary.isEmpty() || probe == null) {
            return new Decision(primary, false, 1, false);
        }
        return evaluateEmergency(primary, emergency, 1, probe);
    }

    private static Decision evaluateEmergency(String primary, String emergency, int primaryFailures,
                                              DetailedProbe probe) {
        if (emergency.isEmpty() || emergency.equalsIgnoreCase(primary)) {
            return new Decision(primary, false, primaryFailures, false);
        }
        ProbeResult emergencyResult = safeProbe(probe, emergency);
        return new Decision(
                emergencyResult.healthy ? emergency : primary,
                emergencyResult.healthy,
                primaryFailures,
                emergencyResult.healthy
        );
    }

    private static ProbeResult safeProbe(DetailedProbe probe, String baseUrl) {
        try {
            ProbeResult result = probe.probe(baseUrl);
            return result == null ? ProbeResult.unavailable() : result;
        } catch (Exception error) {
            return BackendTransportFailurePolicy.isTransportFailure(error)
                    ? ProbeResult.transportFailure()
                    : ProbeResult.unavailable();
        }
    }

    static boolean probeHealth(String baseUrl) {
        return probeHealthResult(baseUrl).healthy;
    }

    static ProbeResult probeHealthResult(String baseUrl) {
        String normalized = normalize(baseUrl);
        if (normalized.isEmpty()) {
            return ProbeResult.unavailable();
        }
        try {
            HttpClient.Response response = new HttpClient().get(
                    normalized + "/health",
                    CONNECT_TIMEOUT_MS,
                    READ_TIMEOUT_MS,
                    Collections.singletonMap("Accept", "application/json")
            );
            int code = response.code;
            return code >= 200 && code < 300
                    ? ProbeResult.healthy()
                    : ProbeResult.unavailable();
        } catch (Exception error) {
            return BackendTransportFailurePolicy.isTransportFailure(error)
                    ? ProbeResult.transportFailure()
                    : ProbeResult.unavailable();
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
