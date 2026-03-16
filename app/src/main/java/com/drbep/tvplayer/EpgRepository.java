package com.drbep.tvplayer;

import org.json.JSONArray;
import org.json.JSONObject;

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
    private final HttpClient httpClient;

    EpgRepository(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new HttpClient();
    }

    Map<String, String> fetchNowPrograms() throws Exception {
        HttpClient.Response response = httpClient.get(baseUrl + "/api/epg/now", 10000, 15000, java.util.Collections.singletonMap("Accept", "application/json"));
        if (!response.isSuccessful()) {
            return new HashMap<>();
        }

        JSONArray arr = new JSONArray(response.body);
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
    }

    List<EpgProgram> fetchChannelPrograms(String channelId, int maxItems) throws Exception {
        HttpClient.Response response = httpClient.get(baseUrl + "/api/epg/channel/" + channelId, 10000, 15000, java.util.Collections.singletonMap("Accept", "application/json"));
        if (!response.isSuccessful()) {
            throw new IllegalStateException("EPG HTTP " + response.code);
        }

        JSONArray arr = new JSONArray(response.body);
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
    }

    EpgProgram fetchProgramForChannel(String channelId, boolean next) throws Exception {
        HttpClient.Response response = httpClient.get(baseUrl + "/api/epg/channel/" + channelId + (next ? "/next" : "/current"), 10000, 15000, java.util.Collections.singletonMap("Accept", "application/json"));
        if (response.code == 404) {
            return null;
        }
        if (!response.isSuccessful()) {
            throw new IllegalStateException("EPG HTTP " + response.code);
        }
        return fromJson(new JSONObject(response.body));
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
}