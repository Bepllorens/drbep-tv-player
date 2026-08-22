package com.drbep.tvplayer;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/** Shares user-scoped state with the web app through the existing authenticated contract. */
final class UserPreferenceSyncRepository {
    private static final int TIMEOUT_MS = 12_000;
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;

    private final CatalogSnapshotStore snapshotStore;
    private final HttpClient httpClient;

    UserPreferenceSyncRepository(CatalogSnapshotStore snapshotStore) {
        this(snapshotStore, new HttpClient());
    }

    UserPreferenceSyncRepository(CatalogSnapshotStore snapshotStore, HttpClient httpClient) {
        this.snapshotStore = snapshotStore;
        this.httpClient = httpClient;
    }

    JSONObject load(String baseUrl) throws Exception {
        HttpClient.Response response = httpClient.get(
                endpoint(baseUrl),
                TIMEOUT_MS,
                TIMEOUT_MS,
                headers(),
                MAX_RESPONSE_BYTES
        );
        return httpClient.parseObject(
                httpClient.requireSuccess(response, "preferencias compartidas").body,
                "preferencias compartidas"
        );
    }

    JSONObject save(String baseUrl, JSONObject payload) throws Exception {
        HttpClient.Response response = httpClient.putJson(
                endpoint(baseUrl),
                payload == null ? new JSONObject() : payload,
                TIMEOUT_MS,
                TIMEOUT_MS,
                headers()
        );
        return httpClient.parseObject(
                httpClient.requireSuccess(response, "guardar preferencias compartidas").body,
                "guardar preferencias compartidas"
        );
    }

    boolean isConfigured() {
        return snapshotStore != null && !safe(snapshotStore.getAccessToken()).isEmpty();
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        if (snapshotStore != null) {
            String token = safe(snapshotStore.getAccessToken());
            if (!token.isEmpty()) {
                headers.put("Authorization", "Bearer " + token);
                headers.put("X-DRBEP-Access-Token", token);
            }
            String deviceId = safe(snapshotStore.getDeviceId());
            if (!deviceId.isEmpty()) {
                headers.put("X-DRBEP-Device-Id", deviceId);
            }
        }
        return headers;
    }

    static String endpoint(String baseUrl) {
        return safe(baseUrl).replaceAll("/+$", "") + "/api/offline/preferences";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
