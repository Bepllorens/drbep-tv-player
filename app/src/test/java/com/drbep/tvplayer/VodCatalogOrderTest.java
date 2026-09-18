package com.drbep.tvplayer;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class VodCatalogOrderTest {
    private ChannelItem item(String id, String date) {
        ChannelItem item = new ChannelItem(id, id, "", "", "", "https://example.test/"+id, "", 0, 0, true, false, 0, "SkyShowtime", new ArrayList<>(), "", "", "vod:skyshowtime:movies", true);
        item.vodReleaseDate = date;
        return item;
    }
    @Test public void newestUsesContentDateNotNameOrProvider() {
        ChannelItem old = item("A", "2024-12-31"), newer = item("Z", "2026-01-02"), missing = item("B", "");
        List<ChannelItem> rows = new ArrayList<>(Arrays.asList(missing, old, newer));
        VodCatalogOrder.newest(rows);
        assertEquals(Arrays.asList(newer, old, missing), rows);
    }
    @Test public void yearFallbackAndIsoDatesAreComparable() {
        assertTrue(VodCatalogOrder.dateKey("2026-01-01T00:00:00Z") > VodCatalogOrder.dateKey("2025"));
        assertEquals(0, VodCatalogOrder.dateKey("unknown"));
        assertEquals(0, VodCatalogOrder.dateKey("2026-99-99"));
    }
}
