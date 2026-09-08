package com.drbep.tvplayer;

import java.net.URI;

final class ProtectedImageRequestPolicy {
    private static final String PLEX_IMAGE_PATH = "/api/vod/plex/image/";

    private ProtectedImageRequestPolicy() {
    }

    static boolean requiresDeviceAuth(String imageUrl, String... trustedBaseUrls) {
        URI image = parseHttpUri(imageUrl);
        if (image == null || image.getPath() == null || !image.getPath().startsWith(PLEX_IMAGE_PATH)) {
            return false;
        }
        if (trustedBaseUrls == null) {
            return false;
        }
        for (String baseUrl : trustedBaseUrls) {
            URI base = parseHttpUri(baseUrl);
            if (base != null && sameOrigin(image, base)) {
                return true;
            }
        }
        return false;
    }

    private static URI parseHttpUri(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            URI uri = URI.create(raw.trim());
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return null;
            }
            return uri;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean sameOrigin(URI left, URI right) {
        return left.getScheme().equalsIgnoreCase(right.getScheme())
                && left.getHost().equalsIgnoreCase(right.getHost())
                && effectivePort(left) == effectivePort(right);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
