package com.drbep.tvplayer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class DaznEventPolicy {
    static final String COMPETITIONS_CHANNEL_ID = "virtual-dazn-football-competitions";
    static final String COMPETITIONS_PLAY_URL = "drbep-action:dazn-football-competitions";
    static final long PLAYABLE_LEAD_MS = 5L * 60L * 1000L;
    private static final DateTimeFormatter LOCAL_TIME_FORMAT = DateTimeFormatter
            .ofPattern("EEE d MMM · HH:mm", new Locale("es", "ES"));
    private static final DateTimeFormatter CARD_TIME_FORMAT = DateTimeFormatter
            .ofPattern("d MMM · HH:mm", new Locale("es", "ES"));

    private DaznEventPolicy() {
    }

    static boolean isDazn(ChannelItem item) {
        if (item == null || !item.isVod) {
            return false;
        }
        String filterKey = clean(item.vodFilterKey).toLowerCase(Locale.ROOT);
        String platform = clean(item.platformName).toLowerCase(Locale.ROOT);
        return filterKey.contains("dazn") || platform.contains("dazn");
    }

    static boolean isCompetitionLauncher(ChannelItem item) {
        return item != null && (COMPETITIONS_CHANNEL_ID.equals(item.id) || COMPETITIONS_PLAY_URL.equals(item.playUrl));
    }

    static boolean isScheduled(ChannelItem item) {
        return isDazn(item) && (item.daznScheduled || clean(item.vodFilterKey).endsWith(":scheduled"));
    }

    static boolean isPlayableNow(ChannelItem item, long nowMs) {
        if (!isScheduled(item)) {
            return true;
        }
        long startMs = parseInstantMs(item.daznStart);
        if (startMs <= 0L) {
            return item.daznPlayable;
        }
        long endMs = parseInstantMs(item.daznEnd);
        boolean started = nowMs >= startMs - PLAYABLE_LEAD_MS;
        boolean notExpired = endMs <= 0L || nowMs <= endMs + PLAYABLE_LEAD_MS;
        return started && notExpired;
    }

    static long playableAtMs(ChannelItem item) {
        if (!isScheduled(item)) {
            return 0L;
        }
        long startMs = parseInstantMs(item.daznStart);
        return startMs > 0L ? Math.max(1L, startMs - PLAYABLE_LEAD_MS) : 0L;
    }

    static boolean isUpcoming(ChannelItem item, long nowMs) {
        long playableAtMs = playableAtMs(item);
        return playableAtMs > nowMs;
    }

    static boolean isLiveEventInProgress(ChannelItem item, long nowMs) {
        if (!isDazn(item) || item == null || !item.daznPlayable) {
            return false;
        }
        String filterKey = clean(item.vodFilterKey).toLowerCase(Locale.ROOT);
        if (!filterKey.endsWith(":live")) {
            return false;
        }
        long startMs = parseInstantMs(item.daznStart);
        long endMs = parseInstantMs(item.daznEnd);
        return startMs > 0L && nowMs >= startMs && (endMs <= 0L || nowMs <= endMs + PLAYABLE_LEAD_MS);
    }

    static String eventState(ChannelItem item, long nowMs) {
        if (!isScheduled(item)) {
            return item != null && item.daznPlayable ? "Disponible" : "DAZN";
        }
        if (isPlayableNow(item, nowMs)) {
            return "En directo / disponible";
        }
        long startMs = parseInstantMs(item.daznStart);
        long endMs = parseInstantMs(item.daznEnd);
        if (endMs > 0L && nowMs > endMs + PLAYABLE_LEAD_MS) {
            return "Finalizado";
        }
        return startMs > 0L ? "Empieza " + formatLocalTime(startMs) : "Próximamente";
    }

    static String eventMeta(ChannelItem item, long nowMs) {
        List<String> lines = new ArrayList<>();
        if (item != null && !clean(item.daznCompetition).isEmpty()) {
            lines.add(clean(item.daznCompetition));
        }
        if (item != null && !clean(item.daznStart).isEmpty()) {
            long startMs = parseInstantMs(item.daznStart);
            if (startMs > 0L) {
                lines.add(formatLocalTime(startMs));
            }
        }
        if (lines.isEmpty()) {
            lines.add(eventState(item, nowMs));
        }
        return String.join("\n", lines);
    }

    static String eventCardMeta(ChannelItem item, long nowMs) {
        List<String> lines = new ArrayList<>();
        String competition = item == null ? "" : clean(item.daznCompetition);
        if (!competition.isEmpty()) {
            lines.add(competition);
        }
        String schedule = eventCardSchedule(item);
        if (!schedule.isEmpty()) {
            lines.add(schedule);
        }
        if (lines.isEmpty()) {
            lines.add(eventState(item, nowMs));
        }
        return String.join("\n", lines);
    }

    static String eventCardSchedule(ChannelItem item) {
        long startMs = parseInstantMs(item == null ? "" : item.daznStart);
        if (startMs <= 0L) {
            return "";
        }
        return CARD_TIME_FORMAT.format(Instant.ofEpochMilli(startMs).atZone(ZoneId.systemDefault()));
    }

    static List<Competition> competitions(List<ChannelItem> items) {
        Map<String, Competition> grouped = new LinkedHashMap<>();
        if (items != null) {
            for (ChannelItem item : items) {
                if (!isDazn(item)) {
                    continue;
                }
                String name = firstNonEmpty(item.daznCompetition, item.group, "Otros eventos DAZN");
                String id = firstNonEmpty(item.daznCompetitionId, name.toLowerCase(Locale.ROOT), "dazn");
                Competition competition = grouped.get(id);
                if (competition == null) {
                    competition = new Competition(id, name, clean(item.daznCompetitionLogo));
                    grouped.put(id, competition);
                } else if (competition.logoUrl.isEmpty() && !clean(item.daznCompetitionLogo).isEmpty()) {
                    competition.logoUrl = clean(item.daznCompetitionLogo);
                }
                competition.events.add(item);
            }
        }
        List<Competition> result = new ArrayList<>(grouped.values());
        for (Competition competition : result) {
            competition.events.sort(DaznEventPolicy::compareEvents);
            if (competition.logoUrl.isEmpty()) {
                for (ChannelItem event : competition.events) {
                    String fallback = clean(event == null ? "" : event.logoUrl);
                    if (!fallback.isEmpty()) {
                        competition.logoUrl = fallback;
                        break;
                    }
                }
            }
        }
        result.sort((left, right) -> {
            int upcoming = Long.compare(left.nextStartMs(), right.nextStartMs());
            return upcoming != 0 ? upcoming : left.name.compareToIgnoreCase(right.name);
        });
        return result;
    }

    static String competitionSummary(Competition competition, long nowMs) {
        if (competition == null) {
            return "";
        }
        int playable = 0;
        long nextStart = Long.MAX_VALUE;
        for (ChannelItem item : competition.events) {
            if (isPlayableNow(item, nowMs)) {
                playable++;
            }
            long start = parseInstantMs(item == null ? "" : item.daznStart);
            if (start >= nowMs - PLAYABLE_LEAD_MS && start < nextStart) {
                nextStart = start;
            }
        }
        List<String> parts = new ArrayList<>();
        parts.add(competition.events.size() + (competition.events.size() == 1 ? " evento" : " eventos"));
        if (playable > 0) {
            parts.add(playable + " disponible" + (playable == 1 ? "" : "s"));
        } else if (nextStart < Long.MAX_VALUE) {
            parts.add("Próximo " + formatLocalTime(nextStart));
        }
        return String.join("  ·  ", parts);
    }

    static long parseInstantMs(String value) {
        String normalized = clean(value);
        if (normalized.isEmpty()) {
            return 0L;
        }
        try {
            return Instant.parse(normalized).toEpochMilli();
        } catch (DateTimeParseException ignored) {
            return 0L;
        }
    }

    private static String formatLocalTime(long epochMs) {
        return LOCAL_TIME_FORMAT.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
    }

    private static int compareEvents(ChannelItem left, ChannelItem right) {
        long leftStart = parseInstantMs(left == null ? "" : left.daznStart);
        long rightStart = parseInstantMs(right == null ? "" : right.daznStart);
        if (leftStart <= 0L) leftStart = Long.MAX_VALUE;
        if (rightStart <= 0L) rightStart = Long.MAX_VALUE;
        int byStart = Long.compare(leftStart, rightStart);
        if (byStart != 0) {
            return byStart;
        }
        return clean(left == null ? "" : left.name).compareToIgnoreCase(clean(right == null ? "" : right.name));
    }

    private static String firstNonEmpty(String... values) {
        if (values != null) {
            for (String value : values) {
                String cleaned = clean(value);
                if (!cleaned.isEmpty()) {
                    return cleaned;
                }
            }
        }
        return "";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    static final class Competition {
        final String id;
        final String name;
        String logoUrl;
        final List<ChannelItem> events = new ArrayList<>();

        Competition(String id, String name, String logoUrl) {
            this.id = clean(id);
            this.name = clean(name);
            this.logoUrl = clean(logoUrl);
        }

        private long nextStartMs() {
            return events.stream()
                    .map(item -> parseInstantMs(item == null ? "" : item.daznStart))
                    .filter(value -> value > 0L)
                    .min(Comparator.naturalOrder())
                    .orElse(Long.MAX_VALUE);
        }
    }
}
