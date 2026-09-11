package com.ochenjoshua.ojmusicplayer.playback.resolvers

import kotlinx.coroutines.runBlocking
import com.ochenjoshua.ojmusicplayer.flaccore.streaming.FlacStreamRegistry
import com.ochenjoshua.ojmusicplayer.flaccore.streaming.FlacStreamUrl
import com.ochenjoshua.ojmusicplayer.flaccore.model.TrackQuery
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamSourceRegistryOrderTest {

    @Test
    fun `registry resolves qbdlx`() = runBlocking {
        val expectedUrl = FlacStreamUrl(
            url = "http://qbdlx.test",
            expiresAtMs = 0L,
            origin = "qbdlx"
        )

        val registry = FlacStreamRegistry(
            qbdlx = { _, _ -> expectedUrl }
        )

        val query = TrackQuery(artist = "A", title = "T", durationMs = 1000L)
        val result = registry.resolve(query, 7)

        assertEquals(expectedUrl, result)
    }
}
