/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.playback.queues

import androidx.media3.common.MediaItem
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import com.ochenjoshua.ojmusicplayer.extensions.toMediaItem
import com.ochenjoshua.ojmusicplayer.innertube.YouTube
import com.ochenjoshua.ojmusicplayer.innertube.models.WatchEndpoint
import com.ochenjoshua.ojmusicplayer.models.MediaMetadata

class YouTubeAlbumRadio(
    internal val playlistId: String,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    private val endpoint: WatchEndpoint
        get() =
            WatchEndpoint(
                playlistId = playlistId,
                params = "wAEB",
            )

    internal var albumSongCount = 0
    internal var continuation: String? = null
    internal var firstTimeLoaded: Boolean = false

    internal constructor(
        playlistId: String,
        albumSongCount: Int,
        continuation: String?,
        firstTimeLoaded: Boolean,
    ) : this(playlistId) {
        this.albumSongCount = albumSongCount
        this.continuation = continuation
        this.firstTimeLoaded = firstTimeLoaded
    }

    override suspend fun getInitialStatus(): Queue.Status =
        withContext(IO) {
            val albumSongs = YouTube.albumSongs(playlistId).getOrThrow()
            albumSongCount = albumSongs.size
            Queue.Status(
                title =
                    albumSongs
                        .first()
                        .album
                        ?.name
                        .orEmpty(),
                items = albumSongs.map { it.toMediaItem() },
                mediaItemIndex = 0,
            )
        }

    override fun hasNextPage(): Boolean = !firstTimeLoaded || continuation != null

    override suspend fun nextPage(): List<MediaItem> =
        withContext(IO) {
            val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
            continuation = nextResult.continuation
            if (!firstTimeLoaded) {
                firstTimeLoaded = true
                nextResult.items.subList(albumSongCount, nextResult.items.size).map { it.toMediaItem() }
            } else {
                nextResult.items.map { it.toMediaItem() }
            }
        }
}
