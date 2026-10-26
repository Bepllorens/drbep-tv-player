package com.drbep.tvplayer

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PalantirPlaybackTest {
    private val path = "/api/vod/palantir/stream/" + "a".repeat(32)
    private fun movie() = JSONObject().put("play_url", path).put("id", "palantir:42:movie:123")
        .put("title", "Prueba").put("kind", "movie").put("plot", "Sinopsis").put("year", "2026")

    @Test fun playbackUsesOwnBackendAndHeadersWithoutProviderCredentials() {
        val api = PalantirHttp("https://drbep.test/", mapOf("Authorization" to "Bearer device-session"))
        val item = api.playbackItem(movie())
        assertEquals("https://drbep.test" + path, item.playUrl)
        assertEquals("palantir:42:movie:123", item.id)
        assertTrue(item.isVod)
        assertEquals("Palantir", item.group)
        assertEquals(mapOf("Authorization" to "Bearer device-session"), item.playlistStreamHeaders)
    }

    @Test fun rejectForeignDestinationsAndWrongTickets() {
        val api = PalantirHttp("https://drbep.test", emptyMap())
        for (invalid in listOf("https://foreign.test/video.mp4", "//foreign.test/" + path, path + "?secret=x", path + "/extra", "/api/vod/palantir/stream/short")) {
            try {
                api.playbackItem(movie().put("play_url", invalid))
                fail("Accepted a foreign or malformed destination")
            } catch (_: IllegalArgumentException) { }
        }
        try {
            api.playbackItem(movie().put("id", "m3u:other"))
            fail("Accepted an unrelated title")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun seriesKeepsEpisodeAndAnIndependentUserIdentity() {
        val api = PalantirHttp("https://drbep.test", emptyMap())
        val item = api.playbackItem(movie().put("kind", "series").put("id", "palantir:42:series:23:2:5").put("season", 2).put("episode", 5))
        assertEquals(2, item.vodSeason)
        assertEquals(5, item.vodEpisode)
        assertEquals("Prueba", item.vodSeriesTitle)
        assertTrue(item.id.startsWith("palantir:42:"))
    }
}
