/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.spotify

import androidx.media3.common.MediaItem
import com.ochenjoshua.ojmusicplayer.extensions.toMediaItem
import com.ochenjoshua.ojmusicplayer.models.MediaMetadata
import com.ochenjoshua.ojmusicplayer.playback.queues.Queue
import com.ochenjoshua.ojmusicplayer.spotify.models.SpotifyTrack

class SpotifyLikedSongsQueue(
    private val title: String? = null,
    private val initialTracks: List<SpotifyTrack> = emptyList(),
    private val startIndex: Int = 0,
    override val preloadItem: MediaMetadata? = null,
) : Queue {
    override suspend fun getInitialStatus(): Queue.Status {
        if (initialTracks.isEmpty()) {
            return Queue.Status(title = title, items = emptyList(), mediaItemIndex = 0)
        }
        val targetIndex = startIndex.coerceIn(initialTracks.indices)
        val stubItems = initialTracks.map { it.toStubMediaItem() }
        return Queue.Status(
            title = title,
            items = stubItems,
            mediaItemIndex = targetIndex,
        )
    }

    override fun hasNextPage(): Boolean = false

    override suspend fun nextPage(): List<MediaItem> = emptyList()

    private fun SpotifyTrack.toStubMediaItem(): MediaItem {
        val metadata =
            MediaMetadata(
                id = id,
                title = name,
                artists = artists.map { MediaMetadata.Artist(id = it.id, name = it.name) },
                duration = if (durationMs > 0) durationMs / 1000 else -1,
                thumbnailUrl = SpotifyMapper.getTrackThumbnail(this),
                album = album?.let { MediaMetadata.Album(id = it.id, title = it.name) },
                explicit = explicit,
                spotifyTrackId = id.takeIf(String::isNotBlank),
                isrc = externalIds?.isrc?.takeIf { it.isNotBlank() },
            )
        return metadata.toMediaItem()
    }
}
