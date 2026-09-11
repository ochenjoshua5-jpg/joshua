package com.ochenjoshua.ojmusicplayer.playback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import com.ochenjoshua.ojmusicplayer.constants.FlacQuality
import com.ochenjoshua.ojmusicplayer.db.entities.Song
import com.ochenjoshua.ojmusicplayer.db.entities.SongEntity
import com.ochenjoshua.ojmusicplayer.playback.resolvers.LosslessStreamResolver
import com.ochenjoshua.ojmusicplayer.playback.resolvers.StreamUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LosslessStreamResolverTest {

    @Test
    fun `test lossless stream resolver fallback and headers`() = runBlocking {
        val resolver = object : LosslessStreamResolver {
            override suspend fun resolve(song: Song, quality: FlacQuality): StreamUrl? {
                return StreamUrl(
                    url = "https://example.com/flac",
                    expiresAtMs = 0L,
                    origin = "squid"
                )
            }
        }

        val song = Song(
            song = SongEntity(
                id = "1",
                title = "Test"
            ),
            artists = emptyList()
        )

        val losslessResult = runCatching {
            withTimeout(2500L) {
                resolver.resolve(song, FlacQuality.HI_RES)
            }
        }.getOrNull()

        assertNotNull(losslessResult)
        
        var resolvedUrl = "https://youtube.com/fallback"
        val headers = mutableMapOf<String, String>()
        
        if (losslessResult != null && losslessResult.url.isNotBlank()) {
            if (losslessResult.origin in listOf("squid", "kennyy", "arcod", "qobuz")) {
                headers["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
                headers["Referer"] = "https://music.youtube.com/"
            }
            resolvedUrl = losslessResult.url
        }

        assertEquals("https://example.com/flac", resolvedUrl)
        assertEquals("https://music.youtube.com/", headers["Referer"])
    }

    @Test
    fun `test lossless stream resolver timeout fallback`() = runBlocking {
        val resolver = object : LosslessStreamResolver {
            override suspend fun resolve(song: Song, quality: FlacQuality): StreamUrl? {
                delay(3000L)
                return StreamUrl(
                    url = "https://example.com/flac",
                    expiresAtMs = 0L,
                    origin = "squid"
                )
            }
        }

        val song = Song(
            song = SongEntity(
                id = "1",
                title = "Test"
            ),
            artists = emptyList()
        )

        val losslessResult = runCatching {
            withTimeout(2500L) {
                resolver.resolve(song, FlacQuality.HI_RES)
            }
        }.getOrNull()

        assertNull(losslessResult)
        
        var resolvedUrl = "https://youtube.com/fallback"
        val headers = mutableMapOf<String, String>()
        
        if (losslessResult != null && losslessResult.url.isNotBlank()) {
            if (losslessResult.origin in listOf("squid", "kennyy", "arcod", "qobuz")) {
                headers["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
                headers["Referer"] = "https://music.youtube.com/"
            }
            resolvedUrl = losslessResult.url
        }

        assertEquals("https://youtube.com/fallback", resolvedUrl)
        assertEquals(null, headers["Referer"])
    }
}
