package com.ochenjoshua.ojmusicplayer.playback.resolvers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class StreamUrlCacheTest {

    private lateinit var cache: StreamUrlCache

    @Before
    fun setup() {
        cache = StreamUrlCache()
    }

    @Test
    fun `get returns valid entry`() {
        val futureMs = System.currentTimeMillis() + 120_000L
        val url = StreamUrl("http://test", futureMs, origin = "test")
        cache.put("1", url)

        assertEquals(url, cache.get("1"))
    }

    @Test
    fun `get returns null and removes expired entry`() {
        val pastMs = System.currentTimeMillis() - 10_000L
        val url = StreamUrl("http://test", pastMs, origin = "test")
        cache.put("1", url)

        assertNull(cache.get("1"))
        assertEquals(0, cache.size())
    }

    @Test
    fun `get returns null and removes entry within safety margin`() {
        val nearFutureMs = System.currentTimeMillis() + 30_000L // Less than 60_000L safety margin
        val url = StreamUrl("http://test", nearFutureMs, origin = "test")
        cache.put("1", url)

        assertNull(cache.get("1"))
        assertEquals(0, cache.size())
    }

    @Test
    fun `put evicts when size reaches 256`() {
        val futureMs = System.currentTimeMillis() + 120_000L
        for (i in 1..257) {
            cache.put(i.toString(), StreamUrl("http://test/$i", futureMs, origin = "test"))
        }

        assertEquals(256, cache.size())
    }

    @Test
    fun `remove deletes entry`() {
        val futureMs = System.currentTimeMillis() + 120_000L
        val url = StreamUrl("http://test", futureMs, origin = "test")
        cache.put("1", url)
        cache.remove("1")

        assertNull(cache.get("1"))
        assertEquals(0, cache.size())
    }
}
