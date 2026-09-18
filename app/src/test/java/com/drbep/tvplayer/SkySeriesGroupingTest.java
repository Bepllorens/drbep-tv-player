package com.drbep.tvplayer;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class SkySeriesGroupingTest {
    private ChannelItem episode(String id, String series, String date) {
        ChannelItem item = new ChannelItem(id, "Episode " + id, "", "", "", "https://example.test/" + id, "", 0, 0, true, false, 0, "SkyShowtime", new ArrayList<>(), "", "", "vod:skyshowtime:series", true);
        item.vodSeriesId = series; item.vodSeriesTitle = "Series " + series; item.vodReleaseDate = date;
        return item;
    }
    @Test public void groupsEpisodesWithoutMergingDifferentSeries() {
        List<ChannelItem> rows = MainActivity.groupSkySeries(Arrays.asList(episode("1", "a", "2024-01-01"), episode("2", "a", "2026-02-01"), episode("3", "b", "2025-01-01")));
        assertEquals(2, rows.size());
        assertEquals("Series a", rows.get(0).name);
        assertEquals("sky-series:a", rows.get(0).playUrl);
        assertEquals("2026-02-01", rows.get(0).vodReleaseDate);
    }
    @Test public void leavesUngroupedItemsAndOtherProvidersUntouched() {
        ChannelItem unknown = episode("1", "", "");
        assertSame(unknown, MainActivity.groupSkySeries(Collections.singletonList(unknown)).get(0));
    }
}
