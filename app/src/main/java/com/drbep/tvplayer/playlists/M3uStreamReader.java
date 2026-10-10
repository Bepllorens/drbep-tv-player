package com.drbep.tvplayer.playlists;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Streaming metadata reader; does not resolve URLs or execute playlist directives. */
public final class M3uStreamReader {
    private static final int MAX_LINE = 256 * 1024;
    private static final Pattern ATTRIBUTE = Pattern.compile("([\\w-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s,]+))");
    static final Pattern EPISODE = Pattern.compile("(?i)\\b[ST](\\d{1,4})[\\s._-]*E(\\d{1,4})\\b");
    public interface Sink { void accept(Entry entry) throws Exception; }
    public static final class Entry {
        public String title = "", url = "", kind = "unknown", series = "";
        public int season = -1, episode = -1;
        public final Map<String,String> attributes = new LinkedHashMap<>();
        public final Map<String,String> directives = new LinkedHashMap<>();
    }
    public static final class Result {
        public long imported, rejected;
        public String epgUrl = "";
    }
    public Result read(Reader input, Sink sink) throws Exception {
        BufferedReader reader = new BufferedReader(input, 32768);
        Result result = new Result();
        Entry pending = null;
        String line;
        boolean first = true;
        while ((line = boundedLine(reader)) != null) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Import cancelled");
            if (first) { line = line.replaceFirst("^\\uFEFF", ""); first = false; }
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#EXT-X-")) throw new IOException("HLS manifest is not a channel catalogue");
            if (line.startsWith("#EXTM3U")) {
                Map<String,String> attrs = attributes(line);
                result.epgUrl = attrs.getOrDefault("url-tvg", attrs.getOrDefault("x-tvg-url", ""));
            } else if (line.startsWith("#EXTINF:")) {
                if (pending != null) result.rejected++;
                pending = new Entry();
                int comma = titleSeparator(line);
                if (comma < 0) { pending = null; result.rejected++; continue; }
                pending.title = line.substring(comma + 1).trim();
                pending.attributes.putAll(attributes(line.substring(0, comma)));
                String type = pending.attributes.getOrDefault("type", pending.attributes.getOrDefault("tvg-type", "")).trim().toLowerCase(Locale.ROOT);
                if (type.equals("movie") || type.equals("vod")) pending.kind = "movie";
                else if (type.equals("series")) pending.kind = "series";
                else if (type.equals("live") || type.equals("tv")) pending.kind = "live";
                pending.series = pending.attributes.getOrDefault("serie-title", "").trim();
                if (pending.series.isEmpty()) pending.series = pending.attributes.getOrDefault("tvg-serie", "").trim();
                Matcher ep = EPISODE.matcher(pending.title);
                if (pending.kind.equals("series") && ep.find()) {
                    pending.season = Integer.parseInt(ep.group(1)); pending.episode = Integer.parseInt(ep.group(2));
                    if (pending.series.isEmpty()) pending.series = pending.title.substring(0, ep.start()).replaceFirst("[\\s-]+$", "");
                }
                if (pending.kind.equals("series") && pending.series.isEmpty()) pending.series = pending.title;
            } else if (pending != null && line.startsWith("#EXTGRP:")) {
                pending.attributes.putIfAbsent("group-title", line.substring(8).trim());
            } else if (pending != null && (line.startsWith("#KODIPROP:") || line.startsWith("#EXTVLCOPT:") || line.startsWith("#EXTHTTP:"))) {
                int colon = line.indexOf(':');
                String value = line.substring(colon + 1);
                int equals = value.indexOf('=');
                if (line.startsWith("#EXTHTTP:")) pending.directives.put("http-json", value);
                else if (equals > 0) pending.directives.put(line.substring(1, colon).toLowerCase(Locale.ROOT) + ":" + value.substring(0, equals).trim().toLowerCase(Locale.ROOT), value.substring(equals + 1).trim());
            } else if (!line.startsWith("#") && pending != null) {
                pending.url = line;
                if (pending.title.isEmpty()) result.rejected++;
                else { sink.accept(pending); result.imported++; }
                pending = null;
            }
        }
        if (pending != null) result.rejected++;
        return result;
    }
    private static Map<String,String> attributes(String text) {
        Map<String,String> out = new LinkedHashMap<>();
        Matcher m = ATTRIBUTE.matcher(text);
        while (m.find()) out.put(m.group(1).toLowerCase(Locale.ROOT), m.group(2) != null ? m.group(2) : m.group(3) != null ? m.group(3) : m.group(4));
        return out;
    }
    private static int titleSeparator(String s) {
        char quote = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) { if (c == quote) quote = 0; }
            else if (c == '"' || c == '\'') quote = c;
            else if (c == ',') return i;
        }
        return -1;
    }
    private static String boundedLine(Reader reader) throws IOException {
        StringBuilder b = new StringBuilder();
        int c;
        while ((c = reader.read()) != -1 && c != '\n') {
            if (b.length() >= MAX_LINE) throw new IOException("Playlist line exceeds limit");
            b.append((char)c);
        }
        return c == -1 && b.length() == 0 ? null : b.toString();
    }
}
