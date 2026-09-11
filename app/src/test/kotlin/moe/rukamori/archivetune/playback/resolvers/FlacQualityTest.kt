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
import com.ochenjoshua.ojmusicplayer.lossless.FlacCoreLosslessStreamResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class FlacQualityTest {

    @Test
    fun `FlacCoreLosslessStreamResolver passes correct quality to registry`() = runBlocking {
        val registry = mockk<FlacStreamRegistry>()
        val resolver = FlacCoreLosslessStreamResolver(registry)

        val song = Song(
            song = SongEntity(id = "1", title = "Test"),
            artists = listOf(ArtistEntity(id = "1", name = "Artist"))
        )

        coEvery { registry.resolve(any(), 6) } returns FlacStreamUrl(url = "http://test.com/cd", expiresAtMs = 0, origin = "qbdlx")
        coEvery { registry.resolve(any(), 7) } returns FlacStreamUrl(url = "http://test.com/hires", expiresAtMs = 0, origin = "qbdlx")
        coEvery { registry.resolve(any(), 27) } returns FlacStreamUrl(url = "http://test.com/max", expiresAtMs = 0, origin = "qbdlx")

        val cdResult = resolver.resolve(song, FlacQuality.CD)
        assertEquals("http://test.com/cd", cdResult?.url)

        val hiresResult = resolver.resolve(song, FlacQuality.HI_RES)
        assertEquals("http://test.com/hires", hiresResult?.url)

        val maxResult = resolver.resolve(song, FlacQuality.MAX)
        assertEquals("http://test.com/max", maxResult?.url)
    }
}
