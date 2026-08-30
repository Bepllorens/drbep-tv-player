package com.drbep.tvplayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

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
        final String description;
        final String status;
        final String startTime;
        final String endTime;
        final boolean playable;
        final long recordingId;
        final List<Long> relatedRecordingIds;

        RecordingItem(String id, String name, String path, long size, String modified, String channelName, String programTitle, String poster, String status, String startTime, String endTime, boolean playable) {
            this(id, name, path, size, modified, channelName, programTitle, poster, "", status, startTime, endTime, playable, 0L, null);
        }

        RecordingItem(String id, String name, String path, long size, String modified, String channelName, String programTitle, String poster, String status, String startTime, String endTime, boolean playable, long recordingId) {
            this(id, name, path, size, modified, channelName, programTitle, poster, "", status, startTime, endTime, playable, recordingId, null);
        }

        RecordingItem(String id, String name, String path, long size, String modified, String channelName, String programTitle, String poster, String status, String startTime, String endTime, boolean playable, long recordingId, List<Long> relatedRecordingIds) {
            this(id, name, path, size, modified, channelName, programTitle, poster, "", status, startTime, endTime, playable, recordingId, relatedRecordingIds);
        }

        RecordingItem(String id, String name, String path, long size, String modified, String channelName, String programTitle, String poster, String description, String status, String startTime, String endTime, boolean playable, long recordingId, List<Long> relatedRecordingIds) {
            this.id = id;
            this.name = name;
            this.path = path;
            this.size = size;
            this.modified = modified;
            this.channelName = channelName;
            this.programTitle = programTitle;
            this.poster = poster;
            this.description = description;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
            this.playable = playable;
            this.recordingId = recordingId;
            if (relatedRecordingIds == null || relatedRecordingIds.isEmpty()) {
                this.relatedRecordingIds = recordingId > 0L
                        ? Collections.singletonList(recordingId)
                        : Collections.emptyList();
            } else {
                this.relatedRecordingIds = Collections.unmodifiableList(new ArrayList<>(relatedRecordingIds));
            }
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
    private final CatalogSnapshotStore snapshotStore;

    RecordingsRepository(String baseUrl) {
        this(baseUrl, null);
    }

    RecordingsRepository(String baseUrl, CatalogSnapshotStore snapshotStore) {
        this.baseUrl = baseUrl;
        this.snapshotStore = snapshotStore;
        this.httpClient = new HttpClient();
    }

    RecordingsResult fetchCompletedRecordings() throws Exception {
        JSONArray completed = httpClient.getJsonArray(
                withCacheBuster(baseUrl + "/api/recordings/completed"),
                10000,
                20000,
                jsonHeaders(false),
                "cargando grabaciones completadas"
        );
        JSONObject filesBody = httpClient.getJsonObject(
                withCacheBuster(baseUrl + "/api/recordings/files"),
                10000,
                20000,
                jsonHeaders(false),
                "cargando archivos de grabaciones"
        );
        String basePath = filesBody.optString("path", "");
        return new RecordingsResult(basePath, parseCompletedItems(completed, filesBody.optJSONArray("files")), false);
    }

    static List<RecordingItem> parseCompletedItems(JSONArray completed, JSONArray files) {
        List<RecordingItem> items = new ArrayList<>();
        List<JSONObject> allFiles = new ArrayList<>();
        Map<Long, List<JSONObject>> filesByRecording = new HashMap<>();
        if (files != null) {
            for (int i = 0; i < files.length(); i++) {
                JSONObject file = files.optJSONObject(i);
                if (file == null) {
                    continue;
                }
                allFiles.add(file);
                long recordingId = file.optLong("recording_id", 0L);
                if (recordingId > 0L) {
                    List<JSONObject> candidates = filesByRecording.get(recordingId);
                    if (candidates == null) {
                        candidates = new ArrayList<>();
                        filesByRecording.put(recordingId, candidates);
                    }
                    candidates.add(file);
                }
            }
        }

        Map<String, List<JSONObject>> completedGroups = new java.util.LinkedHashMap<>();
        java.util.Set<String> completedArtifactKeys = new java.util.HashSet<>();
        if (completed != null) {
            for (int i = 0; i < completed.length(); i++) {
                JSONObject record = completed.optJSONObject(i);
                if (record == null) {
                    continue;
                }
                long recordingId = record.optLong("id", 0L);
                if (recordingId <= 0L) {
                    continue;
                }
                String key = completedRecordingKey(record);
                List<JSONObject> group = completedGroups.get(key);
                if (group == null) {
                    group = new ArrayList<>();
                    completedGroups.put(key, group);
                }
                group.add(record);
            }
        }
        for (List<JSONObject> records : completedGroups.values()) {
            if (records.isEmpty()) {
                continue;
            }
            JSONObject representative = records.get(0);
            String outputPath = representative.optString("output_path", "");
            completedArtifactKeys.add(legacyArtifactKey(outputPath));
            List<Long> recordingIds = new ArrayList<>();
            List<JSONObject> candidates = new ArrayList<>();
            for (JSONObject record : records) {
                long recordingId = record.optLong("id", 0L);
                if (recordingId <= 0L) {
                    continue;
                }
                recordingIds.add(recordingId);
                List<JSONObject> recordFiles = filesByRecording.get(recordingId);
                if (recordFiles != null) {
                    candidates.addAll(recordFiles);
                }
            }
            JSONObject file = choosePreferredFile(candidates, outputPath);
            if (file == null) {
                file = chooseOutputPathMatch(allFiles, outputPath);
            }
            if (file == null) {
                continue;
            }
            items.add(buildCompletedItem(representative, file, recordingIds.get(0), recordingIds));
        }

        // Conserva grabaciones antiguas sin registro de base de datos, pero agrupa
        // sus artefactos original/web/merge para no mostrar el mismo contenido varias veces.
        Map<String, JSONObject> legacyFiles = new java.util.LinkedHashMap<>();
        for (JSONObject file : allFiles) {
            if (file.optLong("recording_id", 0L) > 0L) {
                continue;
            }
            String key = legacyArtifactKey(file.optString("name", file.optString("path", "")));
            if (completedArtifactKeys.contains(key)) {
                continue;
            }
            JSONObject previous = legacyFiles.get(key);
            if (previous == null || artifactPreference(file.optString("path", file.optString("name", "")), "")
                    > artifactPreference(previous.optString("path", previous.optString("name", "")), "")) {
                legacyFiles.put(key, file);
            }
        }
        for (JSONObject file : legacyFiles.values()) {
            items.add(buildCompletedItem(null, file, 0L, Collections.emptyList()));
        }
        return items;
    }

    private static RecordingItem buildCompletedItem(JSONObject record, JSONObject file, long recordingId, List<Long> relatedRecordingIds) {
        String path = file.optString("path", "");
        String name = file.optString("name", "");
        String poster = preferRecordValue(record, "poster", file.optString("poster", ""));
        if (poster == null || poster.trim().isEmpty()) {
            poster = preferRecordValue(record, "epg_poster", file.optString("epg_poster", ""));
        }
        return new RecordingItem(
                path.isEmpty() ? name : path,
                name,
                path,
                file.optLong("size", record == null ? 0L : record.optLong("file_size_bytes", 0L)),
                file.optString("modified", record == null ? "" : record.optString("updated_at", "")),
                preferRecordValue(record, "channel_name", file.optString("channel_name", "")),
                preferRecordValue(record, "program_title", file.optString("program_title", "")),
                poster,
                preferRecordValue(record, "description", file.optString("description", "")),
                "completed",
                record == null ? "" : record.optString("start_time", ""),
                record == null ? "" : record.optString("end_time", ""),
                true,
                recordingId,
                relatedRecordingIds
        );
    }

    private static String completedRecordingKey(JSONObject record) {
        String outputPath = record.optString("output_path", "").trim().toLowerCase(Locale.US);
        if (!outputPath.isEmpty()) {
            return "path:" + outputPath;
        }
        return "meta:"
                + record.optString("channel_name", "").trim().toLowerCase(Locale.US) + "|"
                + record.optString("program_title", "").trim().toLowerCase(Locale.US) + "|"
                + record.optString("start_time", "").trim() + "|"
                + record.optString("end_time", "").trim();
    }

    private static String preferRecordValue(JSONObject record, String key, String fallback) {
        String value = record == null ? "" : record.optString(key, "");
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static JSONObject chooseOutputPathMatch(List<JSONObject> files, String outputPath) {
        if (outputPath == null || outputPath.trim().isEmpty()) {
            return null;
        }
        List<JSONObject> matches = new ArrayList<>();
        String outputName = fileName(outputPath);
        for (JSONObject file : files) {
            String path = file.optString("path", "");
            String name = file.optString("name", "");
            if (outputPath.equals(path) || outputName.equals(name) || outputName.equals(fileName(path))) {
                matches.add(file);
            }
        }
        return choosePreferredFile(matches, outputPath);
    }

    private static JSONObject choosePreferredFile(List<JSONObject> candidates, String outputPath) {
        JSONObject best = null;
        int bestScore = Integer.MIN_VALUE;
        if (candidates == null) {
            return null;
        }
        for (JSONObject candidate : candidates) {
            String path = candidate.optString("path", candidate.optString("name", ""));
            int score = artifactPreference(path, outputPath);
            if (best == null || score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    static int artifactPreference(String path, String outputPath) {
        String normalized = path == null ? "" : path.trim().toLowerCase(Locale.US);
        String expected = outputPath == null ? "" : outputPath.trim().toLowerCase(Locale.US);
        int score = !expected.isEmpty() && normalized.equals(expected) ? 100 : 0;
        if (normalized.endsWith(".web.mp4")) {
            score += 70;
        } else if (normalized.endsWith(".mp4")) {
            score += 55;
        } else if (normalized.endsWith(".mkv")) {
            score += 40;
        } else if (normalized.endsWith(".ts")) {
            score += 30;
        }
        if (normalized.contains(".merge.")) {
            score -= 120;
        }
        return score;
    }

    private static String legacyArtifactKey(String value) {
        String name = fileName(value == null ? "" : value).toLowerCase(Locale.US);
        name = name.replaceAll("(?i)\\.merge\\.[^.]+(?=\\.[^.]+$)", "");
        name = name.replaceAll("(?i)\\.part\\.[^.]+(?=\\.[^.]+$)", "");
        name = name.replaceAll("(?i)\\.web(?=\\.mp4$)", "");
        return name;
    }

    private static String fileName(String value) {
        if (value == null) {
            return "";
        }
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        return slash >= 0 ? value.substring(slash + 1) : value;
    }

    RecordingsResult fetchScheduledRecordings() throws Exception {
        JSONObject body = httpClient.getJsonObject(
                withCacheBuster(baseUrl + "/api/recordings/scheduled"),
                10000,
                20000,
                jsonHeaders(false),
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
                String status = record.optString("status", "scheduled");
                String endTime = record.optString("end_time", "");
                if (!isLiveScheduledRecord(status, endTime)) {
                    continue;
                }
                items.add(new RecordingItem(
                        id,
                        programTitle == null || programTitle.trim().isEmpty() ? channelName : programTitle,
                        "",
                        0L,
                        record.optString("updated_at", ""),
                        channelName,
                        programTitle,
                        record.optString("poster", ""),
                        record.optString("description", ""),
                        status,
                        record.optString("start_time", ""),
                        endTime,
                        false,
                        record.optLong("id", 0L),
                        null
                ));
            }
        }
        return new RecordingsResult("", items, true);
    }

    private static String withCacheBuster(String url) {
        String separator = url == null || !url.contains("?") ? "?" : "&";
        return url + separator + "t=" + System.currentTimeMillis();
    }

    private static boolean isLiveScheduledRecord(String status, String endTime) {
        String normalized = status == null ? "" : status.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) {
            normalized = "scheduled";
        }
        switch (normalized) {
            case "scheduled":
            case "pending":
            case "recording":
            case "running":
            case "in_progress":
                break;
            default:
                return false;
        }
        long endMs = parseIsoMillis(endTime);
        long staleGraceMs = 10L * 60L * 1000L;
        return endMs <= 0L || endMs >= System.currentTimeMillis() - staleGraceMs;
    }

    private static long parseIsoMillis(String iso) {
        if (iso == null || iso.trim().isEmpty()) {
            return 0L;
        }
        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                Date parsed = format.parse(iso.trim());
                if (parsed != null) {
                    return parsed.getTime();
                }
            } catch (Exception ignored) {
            }
        }
        return 0L;
    }

    void deleteScheduledRecording(String recordingId) throws Exception {
        if (recordingId == null || recordingId.trim().isEmpty()) {
            throw new IllegalArgumentException("recording id vacio");
        }
        HttpClient.Response response = httpClient.delete(
                baseUrl + "/api/recordings/scheduled?id=" + URLEncoder.encode(recordingId.trim(), "UTF-8"),
                10000,
                15000,
                jsonHeaders(false)
        );
        httpClient.requireSuccess(response, "cancelando grabacion programada");
    }

    void deleteCompletedRecording(long recordingId) throws Exception {
        if (recordingId <= 0L) {
            throw new IllegalArgumentException("recording id invalido");
        }
        HttpClient.Response response = httpClient.delete(
                baseUrl + "/api/recordings/completed?id=" + recordingId,
                10000,
                20000,
                jsonHeaders(false)
        );
        httpClient.requireSuccess(response, "eliminando grabacion completada");
    }

    void deleteCompletedRecordings(List<Long> recordingIds) throws Exception {
        if (recordingIds == null || recordingIds.isEmpty()) {
            throw new IllegalArgumentException("recording ids vacios");
        }
        for (Long recordingId : recordingIds) {
            if (recordingId != null && recordingId > 0L) {
                deleteCompletedRecording(recordingId);
            }
        }
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
                jsonHeaders(true)
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
        String url = baseUrl + "/recordings/remux/" + encodePath(relativePath);
        String token = snapshotStore == null ? "" : snapshotStore.getAccessToken();
        if (token != null && !token.trim().isEmpty()) {
            url += "?access_token=" + encodeQuery(token.trim());
        }
        return url;
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

    private static String encodeQuery(String raw) {
        try {
            return URLEncoder.encode(raw == null ? "" : raw, "UTF-8");
        } catch (Exception ignored) {
            return raw == null ? "" : raw;
        }
    }

    private Map<String, String> jsonHeaders(boolean contentType) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Cache-Control", "no-cache");
        headers.put("Pragma", "no-cache");
        if (contentType) {
            headers.put("Content-Type", "application/json");
        }
        String token = snapshotStore == null ? "" : snapshotStore.getAccessToken();
        if (token != null && !token.trim().isEmpty()) {
            headers.put("Authorization", "Bearer " + token.trim());
            headers.put("X-DRBEP-Access-Token", token.trim());
        }
        String deviceId = snapshotStore == null ? "" : snapshotStore.getDeviceId();
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            headers.put("X-DRBEP-Device-Id", deviceId.trim());
        }
        return headers;
    }
}
