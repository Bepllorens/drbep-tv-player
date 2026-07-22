package com.drbep.tvplayer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

public class EpgRepositoryTest {
    @Test
    public void buildsBoundedRemoteNowQueryForVisibleChannels() {
        ChannelItem first = channel("101");
        ChannelItem second = channel("202");

        assertEquals(
                "/api/epg/now?channel_ids=101,202",
                EpgRepository.buildRemoteNowPathForChannels(java.util.Arrays.asList(first, second, first))
        );
    }

    @Test
    public void disabledRemoteFallbackDoesNotMakeNetworkRequestWithoutSnapshot() throws Exception {
        EpgRepository repository = new EpgRepository("http://127.0.0.1:1", null, false);
        ChannelItem channel = channel("channel-1");

        Map<String, EpgRepository.EpgProgramPair> result =
                repository.fetchProgramPairsForChannels(
                        Collections.singletonList(channel),
                        false,
                        false,
                        false
                );

        assertTrue(result.isEmpty());
    }

    private static ChannelItem channel(String id) {
        return new ChannelItem(
                id,
                "Canal de prueba",
                "",
                "",
                "TV",
                "https://example.com/live.m3u8",
                "",
                1,
                1,
                false,
                false,
                1,
                "Prueba",
                Collections.emptyList(),
                "",
                "",
                "",
                true
        );
    }
}
