/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.extensions

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import com.ochenjoshua.ojmusicplayer.db.entities.Song
import com.ochenjoshua.ojmusicplayer.innertube.models.SongItem
import com.ochenjoshua.ojmusicplayer.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import com.ochenjoshua.ojmusicplayer.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_UGC
import com.ochenjoshua.ojmusicplayer.models.MediaMetadata
import com.ochenjoshua.ojmusicplayer.models.toMediaMetadata
import com.ochenjoshua.ojmusicplayer.ui.utils.YTThumbQuality
import com.ochenjoshua.ojmusicplayer.ui.utils.buildYTThumbnailUrl
import com.ochenjoshua.ojmusicplayer.ui.utils.resize
import com.ochenjoshua.ojmusicplayer.utils.isLocalMediaId

const val ExtraIsMusicVideo = "com.ochenjoshua.ojmusicplayer.extra.IS_MUSIC_VIDEO"
private const val NotificationArtworkSizePx = 1080

val MediaItem.metadata: MediaMetadata?
    get() = localConfiguration?.tag as? MediaMetadata

private fun String?.toNotificationArtworkUri() = this?.resize(NotificationArtworkSizePx, NotificationArtworkSizePx)?.toUri()

private fun MediaItem.Builder.setCacheKeyIfRemote(mediaId: String): MediaItem.Builder {
    if (!mediaId.isLocalMediaId()) {
        setCustomCacheKey(mediaId)
    }
    return this
}

fun Song.toMediaItem(): MediaItem {
    val metadata = toMediaMetadata()
    return MediaItem
        .Builder()
        .setMediaId(song.id)
        .setUri(song.id)
        .setCacheKeyIfRemote(song.id)
        .setTag(metadata)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(song.title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(
                    if (metadata.isMusicVideo) {
                        buildYTThumbnailUrl(song.id, YTThumbQuality.HQ).toUri()
                    } else {
                        song.thumbnailUrl.toNotificationArtworkUri()
                    },
                )
                .setAlbumTitle(song.albumName)
                .setIsPlayable(true)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setExtras(Bundle().apply { putBoolean(ExtraIsMusicVideo, metadata.isMusicVideo) })
                .build(),
        ).build()
}

fun SongItem.toMediaItem() =
    MediaItem
        .Builder()
        .setMediaId(id)
        .setUri(id)
        .setCacheKeyIfRemote(id)
        .setTag(toMediaMetadata())
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(
                    if (isMusicVideo()) {
                        buildYTThumbnailUrl(id, YTThumbQuality.HQ).toUri()
                    } else {
                        thumbnail.toNotificationArtworkUri()
                    },
                ).setAlbumTitle(album?.name)
                .setIsPlayable(true)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setExtras(Bundle().apply { putBoolean(ExtraIsMusicVideo, isMusicVideo()) })
                .build(),
        ).build()

fun MediaMetadata.toMediaItem() =
    MediaItem
        .Builder()
        .setMediaId(id)
        .setUri(id)
        .setCacheKeyIfRemote(id)
        .setTag(this)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(
                    if (isMusicVideo) {
                        buildYTThumbnailUrl(id, YTThumbQuality.HQ).toUri()
                    } else {
                        thumbnailUrl.toNotificationArtworkUri()
                    },
                ).setAlbumTitle(album?.title)
                .setIsPlayable(true)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setExtras(Bundle().apply { putBoolean(ExtraIsMusicVideo, isMusicVideo) })
                .build(),
        ).build()

private fun SongItem.isMusicVideo(): Boolean {
    val musicVideoType = endpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
    return musicVideoType == MUSIC_VIDEO_TYPE_OMV || musicVideoType == MUSIC_VIDEO_TYPE_UGC
}
