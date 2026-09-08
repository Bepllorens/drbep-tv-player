package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class DaznEventPolicyTest {
    @Test
    public void scheduledEventUnlocksFiveMinutesBeforeKickoff() {
        ChannelItem item = daznEvent("Premier League", "premier", "2026-08-30T19:00:00Z");
        long startMs = Instant.parse(item.daznStart).toEpochMilli();

        assertFalse(DaznEventPolicy.isPlayableNow(item, startMs - DaznEventPolicy.PLAYABLE_LEAD_MS - 1L));
        assertTrue(DaznEventPolicy.isPlayableNow(item, startMs - DaznEventPolicy.PLAYABLE_LEAD_MS));
    }

    @Test
    public void competitionsGroupEventsAndKeepCompetitionArtwork() {
        ChannelItem premierOne = daznEvent("Premier League", "premier", "2026-08-30T19:00:00Z");
        premierOne.daznCompetitionLogo = "https://img.example/premier.png";
        ChannelItem premierTwo = daznEvent("Premier League", "premier", "2026-08-31T16:00:00Z");
        ChannelItem bundesliga = daznEvent("Bundesliga", "bundesliga", "2026-08-29T18:00:00Z");

        List<DaznEventPolicy.Competition> competitions = DaznEventPolicy.competitions(Arrays.asList(premierOne, premierTwo, bundesliga));

        assertEquals(2, competitions.size());
        DaznEventPolicy.Competition premier = competitions.stream()
                .filter(item -> "premier".equals(item.id))
                .findFirst()
                .orElseThrow(AssertionError::new);
        assertEquals(2, premier.events.size());
        assertEquals("https://img.example/premier.png", premier.logoUrl);
    }

    @Test
    public void competitionUsesFirstEventPosterWhenCompetitionArtworkIsMissing() {
        ChannelItem event = daznEvent("Serie A", "serie-a", "2026-08-30T19:00:00Z");
        event.daznCompetitionLogo = "";

        DaznEventPolicy.Competition competition = DaznEventPolicy.competitions(Arrays.asList(event)).get(0);

        assertEquals("https://img.example/event.jpg", competition.logoUrl);
    }

    @Test
    public void scheduledEventMetaKeepsCompetitionAndScheduleOnSeparateLines() {
        ChannelItem event = daznEvent("Serie A", "serie-a", "2026-08-30T19:00:00Z");

        String meta = DaznEventPolicy.eventMeta(event, Instant.parse("2026-08-28T10:00:00Z").toEpochMilli());

        assertTrue(meta.startsWith("Serie A\n"));
        assertEquals(2, meta.split("\n").length);
    }

    @Test
    public void eventCardKeepsCompetitionAndScheduleInDedicatedSlots() {
        ChannelItem event = daznEvent("Serie A", "serie-a", "2026-08-30T19:00:00Z");
        long nowMs = Instant.parse("2026-08-28T10:00:00Z").toEpochMilli();

        String meta = DaznEventPolicy.eventCardMeta(event, nowMs);
        assertTrue(meta.startsWith("Serie A\n"));
        assertTrue(meta.matches("Serie A\\n30 ago · \\d{2}:\\d{2}"));
    }

    @Test
    public void liveEventExposesJoinLiveAndStartOverWindow() {
        ChannelItem event = daznEvent("Serie A", "serie-a", "2026-08-30T19:00:00Z", "vod:dazn:live");
        event.daznScheduled = false;
        event.daznPlayable = true;
        event.daznEnd = "2026-08-30T21:30:00Z";

        assertTrue(DaznEventPolicy.isLiveEventInProgress(
                event,
                Instant.parse("2026-08-30T20:00:00Z").toEpochMilli()
        ));
        assertFalse(DaznEventPolicy.isLiveEventInProgress(
                event,
                Instant.parse("2026-08-30T18:59:59Z").toEpochMilli()
        ));
    }

    @Test
    public void catalogAddsLinearLauncherForAvailableDaznCompetitions() {
        CatalogRepository repository = new CatalogRepository("https://fire.tvbep.com", null, true);
        List<ChannelItem> parsed = new ArrayList<>();
        ChannelItem liveDazn = liveDaznChannel();
        parsed.add(liveDazn);
        parsed.add(daznEvent("Premier League", "premier", "2026-08-30T19:00:00Z"));
        repository.appendDaznCompetitionLauncher(parsed, daznPermissions(true));

        ChannelItem launcher = parsed.stream()
                .filter(DaznEventPolicy::isCompetitionLauncher)
                .findFirst()
                .orElseThrow(AssertionError::new);
        assertFalse(launcher.isVod);
        assertEquals(liveDazn.platformId, launcher.platformId);
        assertEquals("Competiciones de fútbol", launcher.name);
    }

    @Test
    public void catalogAddsLauncherBeforeDaznEventsAreLoaded() {
        CatalogRepository repository = new CatalogRepository("https://fire.tvbep.com", null, true);
        List<ChannelItem> parsed = new ArrayList<>();
        parsed.add(liveDaznChannel());

        repository.appendDaznCompetitionLauncher(parsed, daznPermissions(true));

        assertTrue(parsed.stream().anyMatch(DaznEventPolicy::isCompetitionLauncher));
    }

    @Test
    public void catalogDoesNotExposeLauncherWithoutDaznPermission() {
        CatalogRepository repository = new CatalogRepository("https://fire.tvbep.com", null, true);
        List<ChannelItem> parsed = new ArrayList<>();
        parsed.add(liveDaznChannel());

        repository.appendDaznCompetitionLauncher(parsed, daznPermissions(false));

        assertFalse(parsed.stream().anyMatch(DaznEventPolicy::isCompetitionLauncher));
    }

    private static OfflinePermissions daznPermissions(boolean enabled) {
        OfflinePermissions permissions = new OfflinePermissions();
        permissions.vodEnabled = true;
        permissions.daznVodEnabled = enabled;
        return permissions;
    }

    private static ChannelItem daznEvent(String competition, String competitionId, String start) {
        return daznEvent(competition, competitionId, start, "vod:dazn:scheduled");
    }

    private static ChannelItem daznEvent(String competition, String competitionId, String start, String filterKey) {
        ChannelItem item = new ChannelItem(
                "event-" + competitionId + "-" + start,
                "Partido",
                "",
                "https://img.example/event.jpg",
                "Fútbol",
                "https://fire.tvbep.com/api/vod/dazn/manifest/event",
                "",
                1,
                1,
                true,
                false,
                0,
                "DAZN",
                new ArrayList<>(),
                "widevine",
                "https://fire.tvbep.com/api/vod/dazn/license/event",
                filterKey,
                true
        );
        item.daznCompetition = competition;
        item.daznCompetitionId = competitionId;
        item.daznStart = start;
        item.daznEnd = "2026-09-01T00:00:00Z";
        item.daznScheduled = true;
        item.daznPlayable = false;
        return item;
    }

    private static ChannelItem liveDaznChannel() {
        ChannelItem item = new ChannelItem(
                "live-dazn-1",
                "DAZN 1",
                "",
                "https://img.example/dazn.png",
                "Deportes",
                "https://fire.tvbep.com/live/dazn-1",
                "",
                1,
                1,
                false,
                false,
                44,
                "DAZN",
                new ArrayList<>(),
                "",
                "",
                "",
                false
        );
        item.platformLogoUrl = "https://img.example/dazn-platform.png";
        return item;
    }
}
