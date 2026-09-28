package com.drbep.tvplayer.netflix;
import org.json.*;
import java.util.*;
final class NetflixMediaUrls {
    static String findUrl(Object value, int depth) {
        if (depth > 5) return null;
        if (value instanceof String) {
            try {
                java.net.URL url = new java.net.URL((String) value);
                String host = url.getHost().toLowerCase(java.util.Locale.ROOT);
                if (url.getProtocol().equals("https") && url.getUserInfo() == null
                        && (host.endsWith(".nflxvideo.net") || host.endsWith(".netflix.com"))) return (String) value;
            } catch (Exception ignored) {}
        } else if (value instanceof JSONObject) {
            Iterator<String> keys = ((JSONObject) value).keys();
            while (keys.hasNext()) { String found = findUrl(((JSONObject) value).opt(keys.next()), depth + 1); if (found != null) return found; }
        } else if (value instanceof JSONArray) {
            JSONArray list = (JSONArray) value;
            for (int i = 0; i < list.length(); i++) { String found = findUrl(list.opt(i), depth + 1); if (found != null) return found; }
        }
        return null;
    }
}
