package com.drbep.tvplayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RecordingsRepository {
    static final class RecordingItem {
        final String id;
        final String name;
        final String path;
        final long size;
        final String modified;
        final String channelName;
        final String programTitle;
        final String poster;
        final String status;
        final String startTime;
        final String endTime;
        final boolean playable;

        RecordingItem(String id, String name, String path, long size, String modified, String channelName, String programTitle, String poster, String status, String startTime, String endTime, boolean playable) {
            this.id = id;
            this.name = name;
            this.path = path;
            this.size = size;
            this.modified = modified;
            this.channelName = channelName;
            this.programTitle = programTitle;
            this.poster = poster;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
            this.playable = playable;
        }
    }

    static final class RecordingsResult {
        final String basePath;
        final List<RecordingItem> items;
        final boolean scheduledMode;

        RecordingsResult(String basePath, List<RecordingItem> items, boolean scheduledMode) {
            this.basePath = basePath;
            this.items = items;
            this.scheduledMode = scheduledMode;
        }
    }

    private final String baseUrl;
    private final HttpClient httpClient;

    RecordingsRepository(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new HttpClient();
    }

    RecordingsResult fetchCompletedRecordings() throws Exception {
        JSONObject body = httpClient.getJsonObject(
                baseUrl + "/api/recordings/files",
                10000,
                20000,
                java.util.Collections.singletonMap("Accept", "application/json"),
                "cargando grabaciones"
        );
        String basePath = body.optString("path", "");
        JSONArray files = body.optJSONArray("files");
        List<RecordingItem> items = new ArrayList<>();
        if (files != null) {
            for (int i = 0; i < files.length(); i++) {
                JSONObject file = files.optJSONObject(i);
                if (file == null) {
                    continue;
                }
                String path = file.optString("path", "");
                String name = file.optString("name", "");
                items.add(new RecordingItem(
                        path.isEmpty() ? name : path,
                        name,
                        path,
                        file.optLong("size", 0L),
                        file.optString("modified", ""),
                        file.optString("channel_name", ""),
                        file.optString("program_title", ""),
                        file.optString("poster", ""),
                        "completed",
                        "",
                        "",
                        true
                ));
            }
        }
        return new RecordingsResult(basePath, items, false);
    }

    RecordingsResult fetchScheduledRecordings() throws Exception {
        JSONObject body = httpClient.getJsonObject(
                baseUrl + "/api/recordings/scheduled",
                10000,
                20000,
                java.util.Collections.singletonMap("Accept", "application/json"),
                "cargando grabaciones programadas"
        );
        JSONArray records = body.optJSONArray("records");
        List<RecordingItem> items = new ArrayList<>();
        if (records != null) {
            for (int i = 0; i < records.length(); i++) {
                JSONObject record = records.optJSONObject(i);
                if (record == null) {
                    continue;
                }
                String id = String.valueOf(record.optLong("id", 0L));
                String programTitle = record.optString("program_title", "");
                String channelName = record.optString("channel_name", "");
                items.add(new RecordingItem(
                        id,
                        programTitle == null || programTitle.trim().isEmpty() ? channelName : programTitle,
                        "",
                        0L,
                        record.optString("updated_at", ""),
                        channelName,
                        programTitle,
                        record.optString("poster", ""),
                        record.optString("status", "scheduled"),
                        record.optString("start_time", ""),
                        record.optString("end_time", ""),
                        false
                ));
            }
        }
        return new RecordingsResult("", items, true);
    }

    void deleteScheduledRecording(String recordingId) throws Exception {
        if (recordingId == null || recordingId.trim().isEmpty()) {
            throw new IllegalArgumentException("recording id vacio");
        }
        HttpClient.Response response = httpClient.delete(
                baseUrl + "/api/recordings/scheduled?id=" + URLEncoder.encode(recordingId.trim(), "UTF-8"),
                10000,
                15000,
                Collections.singletonMap("Accept", "application/json")
        );
        httpClient.requireSuccess(response, "cancelando grabacion programada");
    }

    void updateScheduledRecording(String recordingId, String startTime, String endTime) throws Exception {
        if (recordingId == null || recordingId.trim().isEmpty()) {
            throw new IllegalArgumentException("recording id vacio");
        }
        JSONObject payload = new JSONObject();
        payload.put("id", Long.parseLong(recordingId.trim()));
        payload.put("start_time", startTime == null ? "" : startTime.trim());
        payload.put("end_time", endTime == null ? "" : endTime.trim());
        HttpClient.Response response = httpClient.putJson(
                baseUrl + "/api/recordings/scheduled",
                payload,
                10000,
                15000,
                Collections.singletonMap("Content-Type", "application/json")
        );
        httpClient.requireSuccess(response, "actualizando grabacion programada");
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
