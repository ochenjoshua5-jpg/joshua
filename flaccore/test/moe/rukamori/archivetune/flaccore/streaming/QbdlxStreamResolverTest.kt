package com.ochenjoshua.ojmusicplayer.flaccore.streaming

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import com.ochenjoshua.ojmusicplayer.flaccore.model.TrackQuery
import com.ochenjoshua.ojmusicplayer.flaccore.qbdlx.QbdlxQobuzSource
import com.ochenjoshua.ojmusicplayer.flaccore.qbdlx.QbdlxResolvedStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QbdlxStreamResolverTest {

    private val source = mockk<QbdlxQobuzSource>()
    private val resolver = QbdlxStreamResolver(source)
    private val query = TrackQuery(artist = "Artist", title = "Title", durationMs = 1000)
    private val quality = 27

    @Test
    fun `returns null if source is not enabled for streaming`() = runTest {
        coEvery { source.isEnabledForStreaming() } returns false

        val result = resolver.resolve(query, quality)
        assertNull(result)
    }

    @Test
    fun `returns null if source returns null`() = runTest {
        coEvery { source.isEnabledForStreaming() } returns true
        coEvery { source.resolveImmediate(query, quality) } returns null

        val result = resolver.resolve(query, quality)
        assertNull(result)
    }

    @Test
    fun `returns null if url has no etsp`() = runTest {
        coEvery { source.isEnabledForStreaming() } returns true
        coEvery { source.resolveImmediate(query, quality) } returns QbdlxResolvedStream(
            url = "https://example.com/stream.flac",
            codec = "flac",
            bitDepth = 16,
            sampleRateHz = 44100,
            confidence = 1.0f,
            coverArtUrl = null,
            sourceTrackId = "123"
        )

        val result = resolver.resolve(query, quality)
        assertNull(result)
    }

    @Test
    fun `parses etsp and maps fields correctly`() = runTest {
        coEvery { source.isEnabledForStreaming() } returns true
        coEvery { source.resolveImmediate(query, quality) } returns QbdlxResolvedStream(
            url = "https://example.com/stream.flac?etsp=1700000000",
            codec = "flac",
            bitDepth = 24,
            sampleRateHz = 96000,
            confidence = 1.0f,
            coverArtUrl = "https://example.com/art.jpg",
            sourceTrackId = "123"
        )

        val result = resolver.resolve(query, quality)
        
        assertEquals("https://example.com/stream.flac?etsp=1700000000", result?.url)
        assertEquals(1700000000000L, result?.expiresAtMs)
        assertEquals("flac", result?.codec)
        assertEquals(24, result?.bitsPerSample)
        assertEquals(96000, result?.sampleRateHz)
        assertNull(result?.bitrateKbps)
        assertEquals("https://example.com/art.jpg", result?.coverArtUrl)
        assertEquals("qbdlx", result?.origin)
    }
}
