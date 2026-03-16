package com.drbep.tvplayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

final class RecordingsRepository {
    static final class RecordingItem {
        final String name;
        final String path;
        final long size;
        final String modified;

        RecordingItem(String name, String path, long size, String modified) {
            this.name = name;
            this.path = path;
            this.size = size;
            this.modified = modified;
        }
    }

    static final class RecordingsResult {
        final String basePath;
        final List<RecordingItem> items;

        RecordingsResult(String basePath, List<RecordingItem> items) {
            this.basePath = basePath;
            this.items = items;
        }
    }

    private final String baseUrl;
    private final HttpClient httpClient;

    RecordingsRepository(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new HttpClient();
    }

    RecordingsResult fetchRecordings() throws Exception {
        HttpClient.Response response = httpClient.get(baseUrl + "/api/recordings/files", 10000, 20000, java.util.Collections.singletonMap("Accept", "application/json"));
        if (!response.isSuccessful()) {
            throw new IllegalStateException("recordings HTTP " + response.code);
        }

        JSONObject body = new JSONObject(response.body);
            String basePath = body.optString("path", "");
            JSONArray files = body.optJSONArray("files");
            List<RecordingItem> items = new ArrayList<>();
            if (files != null) {
                for (int i = 0; i < files.length(); i++) {
                    JSONObject file = files.optJSONObject(i);
                    if (file == null) {
                        continue;
                    }
                    items.add(new RecordingItem(
                            file.optString("name", ""),
                            file.optString("path", ""),
                            file.optLong("size", 0L),
                            file.optString("modified", "")
                    ));
                }
            }
            return new RecordingsResult(basePath, items);
    }

    String buildPlaybackUrl(RecordingItem item, String basePath) {
        if (item == null) {
            return "";
        }
        String relativePath = item.path;
        if (basePath != null && !basePath.trim().isEmpty() && relativePath != null && relativePath.startsWith(basePath)) {
            relativePath = relativePath.substring(basePath.length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
        }
        if (relativePath == null || relativePath.trim().isEmpty()) {
            relativePath = item.name;
        }
        return baseUrl + "/recordings/remux/" + encodePath(relativePath);
    }

    private static String encodePath(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String[] parts = raw.split("/");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append('/');
            }
            try {
                out.append(URLEncoder.encode(part, "UTF-8").replace("+", "%20"));
            } catch (Exception ignored) {
                out.append(part);
            }
        }
        return out.toString();
    }
}