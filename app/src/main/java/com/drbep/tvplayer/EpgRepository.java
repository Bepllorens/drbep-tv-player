package com.drbep.tvplayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class EpgRepository {
    static final class EpgProgram {
        final String title;
        final String icon;
        final String startTime;
        final String endTime;
        final int progress;

        EpgProgram(String title, String icon, String startTime, String endTime, int progress) {
            this.title = title;
            this.icon = icon;
            this.startTime = startTime;
            this.endTime = endTime;
            this.progress = progress;
        }
    }

    private final String baseUrl;

    EpgRepository(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    Map<String, String> fetchNowPrograms() throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/api/epg/now");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                return new HashMap<>();
            }

            JSONArray arr = new JSONArray(readAll(conn.getInputStream()));
            Map<String, String> updates = new HashMap<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String channelId = String.valueOf(item.optLong("channel_id", -1L));
                if ("-1".equals(channelId)) {
                    continue;
                }
                String title = item.optString("title", "").trim();
                int progress = item.optInt("progress", -1);
                if (title.isEmpty()) {
                    continue;
                }
                if (progress >= 0) {
                    title = title + " (" + progress + "%)";
                }
                updates.put(channelId, title);
            }
            return updates;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    List<EpgProgram> fetchChannelPrograms(String channelId, int maxItems) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/api/epg/channel/" + channelId);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("EPG HTTP " + code);
            }

            JSONArray arr = new JSONArray(readAll(conn.getInputStream()));
            List<EpgProgram> programs = new ArrayList<>();
            int limit = Math.min(arr.length(), maxItems);
            for (int i = 0; i < limit; i++) {
                JSONObject item = arr.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                programs.add(fromJson(item));
            }
            return programs;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    EpgProgram fetchProgramForChannel(String channelId, boolean next) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/api/epg/channel/" + channelId + (next ? "/next" : "/current"));
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code == 404) {
                return null;
            }
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("EPG HTTP " + code);
            }
            return fromJson(new JSONObject(readAll(conn.getInputStream())));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static EpgProgram fromJson(JSONObject item) {
        return new EpgProgram(
                item.optString("title", "Sin titulo"),
                item.optString("icon", ""),
                item.optString("start_time", ""),
                item.optString("end_time", ""),
                item.optInt("progress", -1)
        );
    }

    private static String readAll(InputStream inputStream) throws Exception {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        return output.toString();
    }
}