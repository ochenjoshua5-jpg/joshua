/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ochenjoshua.ojmusicplayer.LocalDatabase
import com.ochenjoshua.ojmusicplayer.LocalDownloadUtil
import com.ochenjoshua.ojmusicplayer.LocalPlayerConnection
import com.ochenjoshua.ojmusicplayer.R
import com.ochenjoshua.ojmusicplayer.constants.GridThumbnailCornerRadius
import com.ochenjoshua.ojmusicplayer.constants.GridThumbnailHeight
import com.ochenjoshua.ojmusicplayer.constants.ListThumbnailSize
import com.ochenjoshua.ojmusicplayer.constants.SwipeToSongKey
import com.ochenjoshua.ojmusicplayer.constants.ThumbnailCornerRadius
import com.ochenjoshua.ojmusicplayer.db.entities.Song
import com.ochenjoshua.ojmusicplayer.extensions.toMediaItem
import com.ochenjoshua.ojmusicplayer.innertube.YouTube
import com.ochenjoshua.ojmusicplayer.innertube.models.AlbumItem
import com.ochenjoshua.ojmusicplayer.innertube.models.ArtistItem
import com.ochenjoshua.ojmusicplayer.innertube.models.PlaylistItem
import com.ochenjoshua.ojmusicplayer.innertube.models.SongItem
import com.ochenjoshua.ojmusicplayer.innertube.models.YTItem
import com.ochenjoshua.ojmusicplayer.models.MediaMetadata
import com.ochenjoshua.ojmusicplayer.playback.queues.LocalAlbumRadio
import com.ochenjoshua.ojmusicplayer.ui.utils.resize
import com.ochenjoshua.ojmusicplayer.utils.joinByBullet
import com.ochenjoshua.ojmusicplayer.utils.makeTimeString
import com.ochenjoshua.ojmusicplayer.utils.rememberPreference
import com.ochenjoshua.ojmusicplayer.utils.reportException

@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    viewCountText: String? = null,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    showSongIconPlaceholder: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {
        if (showLikedIcon && song.song.liked) {
            ItemFavoriteBadge()
        }
        if (song.song.explicit) {
            ItemExplicitBadge()
        }
        if (showInLibraryIcon && song.song.inLibrary != null) {
            ItemLibraryBadge()
        }
        if (showDownloadIcon) {
            val download by LocalDownloadUtil.current
                .getDownload(song.id)
                .collectAsState(initial = null)
            ItemDownloadBadge(download?.state, percent = download?.percentDownloaded ?: -1f)
        }
    },
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSwipeable: Boolean = true,
    swipeContentBackgroundColor: Color? = null,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val swipeEnabled by rememberPreference(SwipeToSongKey, defaultValue = false)
    val resolvedSwipeContentBackgroundColor = swipeContentBackgroundColor ?: MaterialTheme.colorScheme.surface

    val content: @Composable () -> Unit = {
        ListItem(
            title = song.song.title,
            subtitle =
                joinByBullet(
                    song.artists.joinToString { it.name },
                    makeTimeString(song.song.duration * 1000L),
                    viewCountText,
                ),
            badges = badges,
            thumbnailContent = {
                ItemThumbnail(
                    thumbnailUrl = song.song.thumbnailUrl?.resize(200, 200),
                    albumIndex = albumIndex,
                    isSelected = isSelected,
                    isActive = isActive,
                    isPlaying = isPlaying,
                    shape = RoundedCornerShape(ThumbnailCornerRadius),
                    placeholderIconRes = if (showSongIconPlaceholder) R.drawable.music_note else null,
                    modifier = Modifier.size(ListThumbnailSize),
                )
            },
            trailingContent = trailingContent,
            modifier = modifier,
            isActive = isActive,
        )
    }

    if (isSwipeable && swipeEnabled) {
        SwipeToSongBox(
            mediaItem = song.toMediaItem(),
            modifier = Modifier.fillMaxWidth(),
            contentBackgroundColor = resolvedSwipeContentBackgroundColor,
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
fun SongGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        if (showLikedIcon && song.song.liked) {
            ItemFavoriteBadge()
        }
        if (showInLibraryIcon && song.song.inLibrary != null) {
            ItemLibraryBadge()
        }
        if (showDownloadIcon) {
            val download by LocalDownloadUtil.current.getDownload(song.id).collectAsState(initial = null)
            ItemDownloadBadge(download?.state, percent = download?.percentDownloaded ?: -1f)
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = {
        MarqueeText(
            text = song.song.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier = Modifier,
        )
    },
    subtitle = {
        MarqueeText(
            text =
                joinByBullet(
                    song.artists.joinToString { it.name },
                    makeTimeString(song.song.duration * 1000L),
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 2,
        )
    },
    badges = badges,
    thumbnailContent = {
        ItemThumbnail(
            thumbnailUrl = song.song.thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = RoundedCornerShape(GridThumbnailCornerRadius),
            modifier = Modifier.size(GridThumbnailHeight),
        )
        if (!isActive) {
            OverlayPlayButton(
                visible = true,
            )
        }
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun MediaMetadataListItem(
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    shouldLoadImage: Boolean = true,
    cropToSquare: Boolean? = null,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    ListItem(
        title = mediaMetadata.title,
        subtitle =
            joinByBullet(
                mediaMetadata.artists.joinToString { it.name },
                makeTimeString(mediaMetadata.duration * 1000L),
            ),
        thumbnailContent = {
            ItemThumbnail(
                thumbnailUrl = mediaMetadata.thumbnailUrl,
                albumIndex = null,
                isSelected = isSelected,
                isActive = isActive,
                isPlaying = isPlaying,
                shouldLoadImage = shouldLoadImage,
                shape = RoundedCornerShape(ThumbnailCornerRadius),
                cropToSquare = cropToSquare,
                modifier = Modifier.size(ListThumbnailSize),
            )
        },
        trailingContent = trailingContent,
        modifier = modifier,
        isActive = isActive,
    )
}

@Composable
fun YouTubeListItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    viewCountText: String? = null,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSwipeable: Boolean = true,
    trailingContent: @Composable RowScope.() -> Unit = {},
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by database.song(item.id).collectAsState(initial = null)
        val album by database.album(item.id).collectAsState(initial = null)

        if ((item is SongItem && song?.song?.liked == true) ||
            (item is AlbumItem && album?.album?.bookmarkedAt != null)
        ) {
            ItemFavoriteBadge()
        }
        if (item.explicit) ItemExplicitBadge()
        if (item is SongItem && song?.song?.inLibrary != null) {
            ItemLibraryBadge()
        }
        if (item is SongItem) {
            val downloads by LocalDownloadUtil.current.downloads.collectAsState()
            val download = downloads[item.id]
            ItemDownloadBadge(download?.state, percent = download?.percentDownloaded ?: -1f)
        }
    },
) {
    val swipeEnabled by rememberPreference(SwipeToSongKey, defaultValue = false)

    val content: @Composable () -> Unit = {
        ListItem(
            title = item.title,
            subtitle =
                when (item) {
                    is SongItem -> {
                        joinByBullet(
                            item.artists.joinToString { it.name },
                            makeTimeString(item.duration?.times(1000L)),
                            viewCountText,
                        )
                    }

                    is AlbumItem -> {
                        joinByBullet(item.artists?.joinToString { it.name }, item.year?.toString())
                    }

                    is ArtistItem -> {
                        null
                    }

                    is PlaylistItem -> {
                        joinByBullet(item.author?.name, item.songCountText)
                    }
                },
            badges = badges,
            thumbnailContent = {
                ItemThumbnail(
                    thumbnailUrl = item.thumbnail,
                    albumIndex = albumIndex,
                    isSelected = isSelected,
                    isActive = isActive,
                    isPlaying = isPlaying,
                    shape = if (item is ArtistItem) CircleShape else RoundedCornerShape(ThumbnailCornerRadius),
                    modifier = Modifier.size(ListThumbnailSize),
                )
            },
            trailingContent = trailingContent,
            modifier = modifier,
            isActive = isActive,
        )
    }

    if (item is SongItem && isSwipeable && swipeEnabled) {
        SwipeToSongBox(
            mediaItem = item.copy(thumbnail = item.thumbnail.resize(1080, 1080)).toMediaItem(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
fun YouTubeGridItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope? = null,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by database.song(item.id).collectAsState(initial = null)
        val album by database.album(item.id).collectAsState(initial = null)

        if (item is SongItem && song?.song?.liked == true ||
            item is AlbumItem && album?.album?.bookmarkedAt != null
        ) {
            ItemFavoriteBadge()
        }
        if (item.explicit) ItemExplicitBadge()
        if (item is SongItem && song?.song?.inLibrary != null) ItemLibraryBadge()
        if (item is SongItem) {
            val downloads by LocalDownloadUtil.current.downloads.collectAsState()
            val download = downloads[item.id]
            ItemDownloadBadge(download?.state, percent = download?.percentDownloaded ?: -1f)
        }
    },
    thumbnailRatio: Float = if (item is SongItem) 16f / 9 else 1f,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = {
        MarqueeText(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier = Modifier,
        )
    },
    subtitle = {
        val subtitle =
            when (item) {
                is SongItem -> joinByBullet(item.artists.joinToString { it.name }, makeTimeString(item.duration?.times(1000L)))
                is AlbumItem -> joinByBullet(item.artists?.joinToString { it.name }, item.year?.toString())
                is ArtistItem -> null
                is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
            }
        if (subtitle != null) {
            MarqueeText(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 2,
            )
        }
    },
    badges = badges,
    thumbnailContent = {
        val database = LocalDatabase.current
        val playerConnection = LocalPlayerConnection.current ?: return@GridItem
        val shape = if (item is ArtistItem) CircleShape else RoundedCornerShape(GridThumbnailCornerRadius)

        ItemThumbnail(
            thumbnailUrl = item.thumbnail,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = shape,
            thumbnailRatio = thumbnailRatio,
        )

        if (item is SongItem && !isActive) {
            OverlayPlayButton(
                visible = true,
            )
        }

        AlbumPlayButton(
            visible = item is AlbumItem && !isActive,
            onClick = {
                coroutineScope?.launch(Dispatchers.IO) {
                    var albumWithSongs = database.albumWithSongs(item.id).first()
                    if (albumWithSongs?.songs.isNullOrEmpty()) {
                        YouTube
                            .album(item.id)
                            .onSuccess { albumPage ->
                                database.transaction { insert(albumPage) }
                                albumWithSongs = database.albumWithSongs(item.id).first()
                            }.onFailure { reportException(it) }
                    }
                    albumWithSongs?.let {
                        withContext(Dispatchers.Main) {
                            playerConnection.playQueue(LocalAlbumRadio(it))
                        }
                    }
                }
            },
        )
    },
    thumbnailRatio = thumbnailRatio,
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)
