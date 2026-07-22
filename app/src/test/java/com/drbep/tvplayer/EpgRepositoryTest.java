package com.drbep.tvplayer;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

public class EpgRepositoryTest {
    @Test
    public void disabledRemoteFallbackDoesNotMakeNetworkRequestWithoutSnapshot() throws Exception {
        EpgRepository repository = new EpgRepository("http://127.0.0.1:1", null, false);
        ChannelItem channel = new ChannelItem(
                "channel-1",
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

        Map<String, EpgRepository.EpgProgramPair> result =
                repository.fetchProgramPairsForChannels(
                        Collections.singletonList(channel),
                        false,
                        false,
                        false
                );

        assertTrue(result.isEmpty());
    }
}
