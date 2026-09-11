package com.ochenjoshua.ojmusicplayer.playback.resolvers

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import com.ochenjoshua.ojmusicplayer.constants.FlacQuality
import com.ochenjoshua.ojmusicplayer.db.entities.ArtistEntity
import com.ochenjoshua.ojmusicplayer.db.entities.Song
import com.ochenjoshua.ojmusicplayer.db.entities.SongEntity
import com.ochenjoshua.ojmusicplayer.flaccore.streaming.FlacStreamRegistry
import com.ochenjoshua.ojmusicplayer.flaccore.streaming.FlacStreamUrl
import com.ochenjoshua.ojmusicplayer.flaccore.model.TrackQuery
import com.ochenjoshua.ojmusicplayer.lossless.FlacCoreLosslessStreamResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class FlacCoreLosslessStreamResolverTest {

    @Test
    fun `adapter maps Song to TrackQuery correctly`() = runBlocking {
        val registry = mockk<FlacStreamRegistry>()
        val resolver = FlacCoreLosslessStreamResolver(registry)

        val song = Song(
            song = SongEntity(
                id = "1",
                title = "Test Title",
                duration = 120, // 120 seconds
                explicit = true
            ),
            artists = listOf(ArtistEntity(id = "a1", name = "Test Artist"))
        )
        val quality = FlacQuality.HI_RES

        val expectedQuery = TrackQuery(
            artist = "Test Artist",
            title = "Test Title",
            album = null,
            isrc = null,
            durationMs = 120_000L,
            explicit = true
        )

        val flacUrl = FlacStreamUrl(
            url = "http://test.url",
            expiresAtMs = 1000L,
            codec = "flac",
            bitsPerSample = 24,
            sampleRateHz = 96000,
            bitrateKbps = 1000,
            coverArtUrl = "http://cover.url",
            origin = "qbdlx"
        )

        coEvery { registry.resolve(expectedQuery, quality.streamQuality) } returns flacUrl

        val result = resolver.resolve(song, quality)

        assertEquals("http://test.url", result?.url)
        assertEquals(1000L, result?.expiresAtMs)
        assertEquals("flac", result?.codec)
        assertEquals(24, result?.bitsPerSample)
        assertEquals(96000, result?.sampleRateHz)
        assertEquals(1000, result?.bitrateKbps)
        assertEquals("http://cover.url", result?.coverArtUrl)
        assertEquals("qbdlx", result?.origin)
    }
}
