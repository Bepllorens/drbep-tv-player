package com.drbep.tvplayer;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class DiscoveryVodOrderTest {
    private StartupHomeHubUiModel.ContinueCard row(String title, String provider, String date, int year) {
        StartupHomeHubUiModel.ContinueCard row = new StartupHomeHubUiModel.ContinueCard(title, provider, "", title, true, 0f, null);
        row.releaseDateKey = DiscoveryVodOrder.dateKey(date, year);
        return row;
    }
    @Test public void sourcesInterleaveByDateWithUndatedLast() {
        var old = row("A", "HBO Max", "2020-01-01", 2020);
        var newest = row("Z", "Movistar", "2026-10-02", 2026);
        var middle = row("C", "Netflix", "", 2025);
        var unknown = row("B", "Disney+", "", 0);
        var rows = new ArrayList<>(Arrays.asList(old, unknown, newest, middle));
        DiscoveryVodOrder.newest(rows);
        assertEquals(Arrays.asList(newest, middle, old, unknown), rows);
    }
    @Test public void fullDateWinsOverYearAndBadDateFallsBack() {
        assertEquals(20241003, DiscoveryVodOrder.dateKey("2024-10-03T00:00:00Z", 2026));
        assertEquals(20250000, DiscoveryVodOrder.dateKey("unknown", 2025));
        assertEquals(0, DiscoveryVodOrder.dateKey("", 0));
    }
    @Test public void capAppliesAfterMergingWithoutProviderQuotas() {
        var rows = new ArrayList<StartupHomeHubUiModel.ContinueCard>();
        for (int i=0; i<40; i++) rows.add(row("Old "+i, "HBO Max", "", 2020));
        for (int i=0; i<10; i++) rows.add(row("New "+i, "Movistar", "", 2026));
        DiscoveryVodOrder.newest(rows);
        assertEquals(40, rows.size());
        for (int i=0; i<10; i++) assertEquals("Movistar", rows.get(i).subtitle);
    }
    @Test public void equalDatesKeepDeterministicOrderRegardlessOfArrival() {
        var a = row("A", "Netflix", "", 2026);
        var b = row("A", "HBO Max", "", 2026);
        var rows = new ArrayList<>(Arrays.asList(a,b));
        DiscoveryVodOrder.newest(rows);
        assertEquals(Arrays.asList(b,a), rows);
    }
}
