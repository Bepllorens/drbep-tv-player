package com.drbep.tvplayer;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class DisneyVodCatalog {
    private static final String ROOT = "/api/vod/private/disneyplus/";
    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");
    interface PageSource { JSONObject get(String path) throws Exception; }
    private final String baseUrl;
    private final PageSource source;
    private String revision = "";

    DisneyVodCatalog(String baseUrl, PageSource source) {
        this.baseUrl = baseUrl;
        this.source = source;
    }

    List<ChannelItem> catalog() throws Exception {
        List<ChannelItem> items = new ArrayList<>();
        load(items, ROOT + "catalog?kind=movie", "movie");
        load(items, ROOT + "catalog?kind=series", "series");
        return items;
    }

    List<ChannelItem> episodes(String id) throws Exception {
        if (!UUID.matcher(id).matches()) throw new IOException("Serie Disney+ no válida");
        List<ChannelItem> items = new ArrayList<>();
        load(items, ROOT + "series/" + id + "?", "episode");
        return items;
    }

    private void load(List<ChannelItem> items, String path, String kind) throws Exception {
        String after = "";
        Set<String> cursors = new HashSet<>();
        Set<String> ids = new HashSet<>();
        do {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Carga cancelada");
            JSONObject page = source.get(path + "&revision=" + revision + "&after=" + after);
            String current = page.optString("revision", "");
            JSONArray rows = page.optJSONArray("items");
            if (!current.matches("[0-9a-f]{32}") || (!revision.isEmpty() && !revision.equals(current))
                    || !page.optBoolean("paged", false) || rows == null || rows.length() > 60
                    || page.optInt("count", -1) != rows.length() || items.size() + rows.length() > 100000) {
                throw new IOException("El catálogo Disney+ ha cambiado o no es válido");
            }
            revision = current;
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String id = row.optString("id", "");
                String title = row.optString("title", "").trim();
                if (!UUID.matcher(id).matches() || !ids.add(id) || title.isEmpty() || !kind.equals(row.optString("kind"))) {
                    throw new IOException("Contenido Disney+ no válido");
                }
                boolean series = kind.equals("series");
                if (kind.equals("episode") && row.optInt("season") > 0 && row.optInt("episode") > 0) {
                    title = String.format(Locale.ROOT, "T%02dE%02d · %s", row.optInt("season"), row.optInt("episode"), title);
                }
                String play = series ? "disneyplus-series:" + id : internalUrl(row.getString("play_url"));
                String license = series ? "" : internalUrl(row.getString("license_url"));
                String poster = row.optString("poster", "");
                int order = items.size() + 1;
                ChannelItem item = new ChannelItem("disneyplus:" + id, title, "", poster.isEmpty() ? "" : internalUrl(poster),
                        "Disney+", play, "", order, order, true, false, 0, "Disney+", new ArrayList<>(),
                        series ? "" : "widevine", license, kind.equals("movie") ? "vod:disneyplus:movies" : "vod:disneyplus:series",
                        !series, row.optString("description", ""), "", Math.max(0L, row.optLong("duration_seconds", 0L)));
                item.vodReleaseDate = row.optString("air_date", "");
                item.vodSeriesId = row.optString("series_id", "");
                item.vodSeriesTitle = row.optString("series_title", "");
                item.vodSeason = Math.max(0, row.optInt("season", 0));
                item.vodEpisode = Math.max(0, row.optInt("episode", 0));
                items.add(item);
            }
            after = page.optString("next", "");
            if (!after.isEmpty() && (!UUID.matcher(after).matches() || rows.length() == 0 || !cursors.add(after))) {
                throw new IOException("Paginación Disney+ no válida");
            }
        } while (!after.isEmpty());
    }

    private String internalUrl(String path) throws IOException {
        if (!path.startsWith(ROOT) || path.contains("..") || path.contains("\\") || path.contains("\r") || path.contains("\n")) {
            throw new IOException("Ruta Disney+ no válida");
        }
        return baseUrl + path;
    }
}
