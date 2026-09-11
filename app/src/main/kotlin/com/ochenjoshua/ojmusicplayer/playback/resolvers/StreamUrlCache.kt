package com.ochenjoshua.ojmusicplayer.playback.resolvers

import java.util.concurrent.ConcurrentHashMap

class StreamUrlCache {
    private val cache = ConcurrentHashMap<String, StreamUrl>()
    private val maxSize = 256

    fun get(mediaId: String): StreamUrl? {
        val entry = cache[mediaId] ?: return null
        val now = System.currentTimeMillis()
        // Entry is valid if expiresAtMs > now + STREAM_URL_EXPIRY_SAFETY_MS
        // YTPlayerUtils.STREAM_URL_EXPIRY_SAFETY_MS is 60_000L
        if (entry.expiresAtMs > now + 60_000L) {
            return entry
        }
        cache.remove(mediaId)
        return null
    }

    fun put(mediaId: String, url: StreamUrl) {
        if (cache.size >= maxSize && !cache.containsKey(mediaId)) {
            // Evict one entry (first from iterator)
            val iterator = cache.keys().iterator()
            if (iterator.hasNext()) {
                cache.remove(iterator.next())
            }
        }
        cache[mediaId] = url
    }

    fun remove(mediaId: String) {
        cache.remove(mediaId)
    }
    
    // For testing
    internal fun size(): Int = cache.size
}
