/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.innertube.models.body

import kotlinx.serialization.Serializable
import com.ochenjoshua.ojmusicplayer.innertube.models.Context

@Serializable
data class PlayerBody(
    val context: Context,
    val videoId: String,
    val playlistId: String?,
    val playbackContext: PlaybackContext? = null,
    val serviceIntegrityDimensions: ServiceIntegrityDimensions? = null,
) {
    @Serializable
    data class PlaybackContext(
        val contentPlaybackContext: ContentPlaybackContext,
    ) {
        @Serializable
        data class ContentPlaybackContext(
            val signatureTimestamp: Int,
        )
    }

    @Serializable
    data class ServiceIntegrityDimensions(
        val poToken: String,
    )
}
