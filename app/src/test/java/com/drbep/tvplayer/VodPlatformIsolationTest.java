package com.drbep.tvplayer;

import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

public class VodPlatformIsolationTest {
    private ChannelItem item(String id, String platform, String filter) {
        return new ChannelItem(id, id, "", "", "", "https://example.test/video", "", 0, 0,
                true, false, 0, platform, new ArrayList<>(), "", "", filter, true);
    }

    @Test public void privateProvidersDoNotMatchOtherOrEachOther() {
        String[] names = {"Apple TV+", "Netflix", "HBO Max"};
        String[] ids = {"appletv:1", "netflix:1", "hbomax:1"};
        MainActivity.VodVisualPlatformFilter[] providers = {
                MainActivity.VodVisualPlatformFilter.APPLETV,
                MainActivity.VodVisualPlatformFilter.NETFLIX,
                MainActivity.VodVisualPlatformFilter.HBO};
        for (int i = 0; i < names.length; i++) {
            ChannelItem channel = item(ids[i], names[i], "");
            for (int j = 0; j < providers.length; j++)
                assertEquals(i == j, MainActivity.matchesVodVisualPlatform(channel, providers[j]));
            assertFalse(MainActivity.matchesVodVisualPlatform(channel, MainActivity.VodVisualPlatformFilter.OTHER));
        }
    }

    @Test public void unrelatedChannelCannotSupplyPrivateProviderLogo() {
        ChannelItem channel = item("hot:1", "HOT", "");
        assertFalse(MainActivity.matchesVodVisualPlatform(channel, MainActivity.VodVisualPlatformFilter.APPLETV));
        assertFalse(MainActivity.matchesVodVisualPlatform(channel, MainActivity.VodVisualPlatformFilter.NETFLIX));
        assertTrue(MainActivity.matchesVodVisualPlatform(channel, MainActivity.VodVisualPlatformFilter.OTHER));
    }
}
