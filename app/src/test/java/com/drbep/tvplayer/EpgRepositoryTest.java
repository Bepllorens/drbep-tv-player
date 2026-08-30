package com.drbep.tvplayer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Arrays;
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

    @Test
    public void futureProgramIsNextAndNeverPresentedAsCurrent() {
        long now = 1_800_000L;
        EpgRepository.EpgProgram future = new EpgRepository.EpgProgram(
                "101",
                "Canal de prueba",
                "Programa de las 12",
                "",
                "",
                EpgTimeCodec.formatUtc(now + 600_000L),
                EpgTimeCodec.formatUtc(now + 1_200_000L),
                0
        );

        EpgRepository.EpgProgramPair pair =
                EpgRepository.selectCurrentAndNext(Arrays.asList(future), now);

        assertEquals(null, pair.current);
        assertEquals("Programa de las 12", pair.next.title);
    }

    @Test
    public void staleFutureCurrentIsDemotedWhenRestoredFromCache() {
        long now = 1_800_000L;
        EpgRepository.EpgProgram future = new EpgRepository.EpgProgram(
                "101",
                "Canal de prueba",
                "Programa futuro",
                "",
                "",
                EpgTimeCodec.formatUtc(now + 600_000L),
                EpgTimeCodec.formatUtc(now + 1_200_000L),
                0
        );

        EpgRepository.EpgProgramPair normalized = EpgRepository.normalizePairForNow(
                new EpgRepository.EpgProgramPair(future, future), now);

        assertEquals(null, normalized.current);
        assertEquals("Programa futuro", normalized.next.title);
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
