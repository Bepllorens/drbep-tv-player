package com.drbep.tvplayer;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Keep Prime manifest and license requests in the same backend session. */
final class PrimeLicenseContext {
    static Map<String, String> headers(String backend, String license, Map<String, String> playback) {
        try {
            URI base = URI.create(backend);
            URI target = URI.create(license);
            if (base.getHost() == null || target.getHost() == null
                    || !base.getHost().equalsIgnoreCase(target.getHost())
                    || !base.getScheme().equalsIgnoreCase(target.getScheme())
                    || port(base) != port(target)
                    || target.getRawUserInfo() != null
                    || target.getRawPath() == null
                    || !target.getRawPath().startsWith("/api/vod/prime/license/")) {
                return Collections.emptyMap();
            }
            Map<String, String> headers = new HashMap<>();
            for (String key : new String[]{"X-DRBEP-Prime-Profile", "X-DRBEP-Device-Id"}) {
                String value = playback.get(key);
                if (value != null && !value.trim().isEmpty()) headers.put(key, value);
            }
            return headers;
        } catch (RuntimeException ignored) {
            return Collections.emptyMap();
        }
    }

    private static int port(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort() : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
