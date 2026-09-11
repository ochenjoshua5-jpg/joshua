package com.ochenjoshua.ojmusicplayer.playback.resolvers

import com.ochenjoshua.ojmusicplayer.constants.FlacQuality
import com.ochenjoshua.ojmusicplayer.db.entities.Song

data class StreamUrl(
    val url: String,
    val expiresAtMs: Long,
    val codec: String? = null,
    val bitsPerSample: Int? = null,
    val sampleRateHz: Int? = null,
    val bitrateKbps: Int? = null,
    val coverArtUrl: String? = null,
    val origin: String
)

interface LosslessStreamResolver {
    suspend fun resolve(song: Song, quality: FlacQuality): StreamUrl?
}
