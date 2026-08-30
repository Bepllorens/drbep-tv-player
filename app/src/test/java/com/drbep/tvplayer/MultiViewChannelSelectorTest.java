package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiViewChannelSelectorTest {
    @Test
    public void selectorUsesEveryPlatformInTheAllowedCatalog() {
        ChannelItem platformA1 = channel("a-1", "Canal A1", 1, "Plataforma A", false);
        ChannelItem platformA2 = channel("a-2", "Canal A2", 1, "Plataforma A", false);
        ChannelItem platformB1 = channel("b-1", "Canal B1", 2, "Plataforma B", false);
        ChannelItem vod = channel("vod-1", "VOD", 3, "Plataforma C", true);

        List<ChannelItem> catalog = Arrays.asList(platformA1, platformA2, platformB1, vod);
        List<ChannelItem> selected = Arrays.asList(platformA1, platformA2);

        List<ChannelItem> result = MultiViewChannelSelector.selectable(catalog, selected, 1);

        assertEquals(Arrays.asList("a-2", "b-1"), ids(result));
        assertTrue(result.stream().anyMatch(item -> item.platformId == 2));
        assertFalse(result.stream().anyMatch(item -> item.isVod));
    }

    private static List<String> ids(List<ChannelItem> items) {
        List<String> ids = new ArrayList<>();
        for (ChannelItem item : items) {
            ids.add(item.id);
        }
        return ids;
    }

    private static ChannelItem channel(String id, String name, int platformId, String platformName, boolean vod) {
        return new ChannelItem(
                id,
                name,
                "",
                "",
                "General",
                "https://example.test/" + id,
                "",
                1,
                1,
                vod,
                false,
                platformId,
                platformName,
                new ArrayList<>(),
                "",
                "",
                "",
                false
        );
    }
}
