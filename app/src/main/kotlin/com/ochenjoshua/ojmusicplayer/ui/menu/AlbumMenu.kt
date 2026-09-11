/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.ochenjoshua.ojmusicplayer.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED
import androidx.media3.exoplayer.offline.Download.STATE_STOPPED
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ochenjoshua.ojmusicplayer.LocalDatabase
import com.ochenjoshua.ojmusicplayer.LocalDownloadUtil
import com.ochenjoshua.ojmusicplayer.LocalPlayerConnection
import com.ochenjoshua.ojmusicplayer.R
import com.ochenjoshua.ojmusicplayer.constants.ArtistSeparatorsKey
import com.ochenjoshua.ojmusicplayer.constants.ListItemHeight
import com.ochenjoshua.ojmusicplayer.constants.ListThumbnailSize
import com.ochenjoshua.ojmusicplayer.constants.SpeedDialSongIdsKey
import com.ochenjoshua.ojmusicplayer.db.entities.Album
import com.ochenjoshua.ojmusicplayer.db.entities.Song
import com.ochenjoshua.ojmusicplayer.extensions.toMediaItem
import com.ochenjoshua.ojmusicplayer.innertube.YouTube
import com.ochenjoshua.ojmusicplayer.playback.ExoDownloadService
import com.ochenjoshua.ojmusicplayer.playback.queues.ListQueue
import com.ochenjoshua.ojmusicplayer.ui.component.AlbumListItem
import com.ochenjoshua.ojmusicplayer.ui.component.ListDialog
import com.ochenjoshua.ojmusicplayer.ui.component.ListItem
import com.ochenjoshua.ojmusicplayer.ui.component.MenuSurfaceSection
import com.ochenjoshua.ojmusicplayer.ui.component.NewAction
import com.ochenjoshua.ojmusicplayer.ui.component.NewActionGrid
import com.ochenjoshua.ojmusicplayer.ui.component.NewMenuItem
import com.ochenjoshua.ojmusicplayer.ui.component.SongListItem
import com.ochenjoshua.ojmusicplayer.ui.utils.HeaderDownloadItem
import com.ochenjoshua.ojmusicplayer.ui.utils.resize
import com.ochenjoshua.ojmusicplayer.ui.utils.sendAddMissingDownloads
import com.ochenjoshua.ojmusicplayer.utils.SpeedDialPin
import com.ochenjoshua.ojmusicplayer.utils.SpeedDialPinType
import com.ochenjoshua.ojmusicplayer.utils.parseSpeedDialPins
import com.ochenjoshua.ojmusicplayer.utils.rememberPreference
import com.ochenjoshua.ojmusicplayer.utils.serializeSpeedDialPins
import com.ochenjoshua.ojmusicplayer.utils.toggleSpeedDialPin

@SuppressLint("MutableCollectionMutableState")
@Composable
fun AlbumMenu(
    originalAlbum: Album,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val scope = rememberCoroutineScope()
    val libraryAlbum by database.album(originalAlbum.id).collectAsState(initial = originalAlbum)
    val album = libraryAlbum ?: originalAlbum
    var songs by remember {
        mutableStateOf(emptyList<Song>())
    }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        database.albumSongs(album.id).collect {
            songs = it
        }
    }

    var downloadState by remember {
        mutableStateOf(STATE_STOPPED)
    }

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == STATE_COMPLETED }) {
                    STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.id]?.state == STATE_QUEUED ||
                            downloads[it.id]?.state == STATE_DOWNLOADING ||
                            downloads[it.id]?.state == STATE_COMPLETED
                    }
                ) {
                    STATE_DOWNLOADING
                } else {
                    STATE_STOPPED
                }
        }
    }

    var refetchIconDegree by remember { mutableFloatStateOf(0f) }

    val rotationAnimation by animateFloatAsState(
        targetValue = refetchIconDegree,
        animationSpec = tween(durationMillis = 800),
        label = "",
    )

    // Artist separators for splitting artist names
    val (artistSeparators) = rememberPreference(ArtistSeparatorsKey, defaultValue = ",;/&")
    val (speedDialSongIds, onSpeedDialSongIdsChange) = rememberPreference(SpeedDialSongIdsKey, "")
    val speedDialPins = remember(speedDialSongIds) { parseSpeedDialPins(speedDialSongIds) }
    val albumPin = remember(album.id) { SpeedDialPin(type = SpeedDialPinType.ALBUM, id = album.id) }
    val isInSpeedDial =
        remember(speedDialPins, albumPin) {
            speedDialPins.any { it.type == albumPin.type && it.id == albumPin.id }
        }
    val isLocalAlbum = album.album.isLocal

    // Split artists by configured separators
    data class SplitArtist(
        val name: String,
        val originalArtist: com.ochenjoshua.ojmusicplayer.db.entities.ArtistEntity?,
    )

    val splitArtists =
        remember(album.artists, artistSeparators) {
            if (artistSeparators.isEmpty()) {
                album.artists.map { SplitArtist(it.name, it) }
            } else {
                val separatorRegex = "[${Regex.escape(artistSeparators)}]".toRegex()
                album.artists.flatMap { artist ->
                    val parts =
                        artist.name
                            .split(separatorRegex)
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                    if (parts.size > 1) {
                        parts.mapIndexed { index, name ->
                            SplitArtist(name, if (index == 0) artist else null)
                        }
                    } else {
                        listOf(SplitArtist(artist.name, artist))
                    }
                }
            }
        }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<Song>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            songs.map { it.id }
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
        onAddComplete = { songCount, playlistNames ->
            val message =
                when {
                    songCount == 1 && playlistNames.size == 1 -> {
                        context.getString(R.string.added_to_playlist, playlistNames.first())
                    }

                    songCount > 1 && playlistNames.size == 1 -> {
                        context.getString(
                            R.string.added_n_songs_to_playlist,
                            songCount,
                            playlistNames.first(),
                        )
                    }

                    songCount == 1 -> {
                        context.getString(R.string.added_to_n_playlists, playlistNames.size)
                    }

                    else -> {
                        context.getString(R.string.added_n_songs_to_n_playlists, songCount, playlistNames.size)
                    }
                }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    title = stringResource(R.string.already_in_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier =
                        Modifier
                            .clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(notAddedList) { song ->
                SongListItem(song = song)
            }
        }
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(
                items = splitArtists.distinctBy { it.name },
                key = { it.name },
            ) { splitArtist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .height(ListItemHeight)
                            .clickable {
                                splitArtist.originalArtist?.let { artist ->
                                    navController.navigate("artist/${artist.id}")
                                    showSelectArtistDialog = false
                                    onDismiss()
                                }
                            }.padding(horizontal = 12.dp),
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = splitArtist.originalArtist?.thumbnailUrl?.resize(200, 200),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .size(ListThumbnailSize)
                                    .clip(CircleShape),
                        )
                    }
                    Text(
                        text = splitArtist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }

    AlbumListItem(
        album = album,
        showLikedIcon = false,
        badges = {},
        trailingContent = {
            IconButton(
                onClick = {
                    database.query {
                        update(album.album.toggleLike())
                    }
                },
            ) {
                Icon(
                    painter = painterResource(if (album.album.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                    tint = if (album.album.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    contentDescription = null,
                )
            }
        },
    )

    Spacer(modifier = Modifier.height(16.dp))

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    LazyColumn(
        userScrollEnabled = true,
        contentPadding =
            PaddingValues(
                start = 0.dp,
                top = 0.dp,
                end = 0.dp,
                bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
            ),
    ) {
        item {
            MenuSurfaceSection {
                NewActionGrid(
                    actions =
                        buildList {
                            add(
                                NewAction(
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.play),
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    text = stringResource(R.string.play),
                                    onClick = {
                                        onDismiss()
                                        if (songs.isNotEmpty()) {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = album.album.title,
                                                    items = songs.map(Song::toMediaItem),
                                                ),
                                            )
                                        }
                                    },
                                ),
                            )
                            add(
                                NewAction(
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.shuffle),
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    text = stringResource(R.string.shuffle),
                                    onClick = {
                                        onDismiss()
                                        if (songs.isNotEmpty()) {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = album.album.title,
                                                    items = songs.shuffled().map(Song::toMediaItem),
                                                ),
                                            )
                                        }
                                    },
                                ),
                            )
                            if (!isLocalAlbum) {
                                add(
                                    NewAction(
                                        icon = {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_share),
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        },
                                        text = stringResource(R.string.share),
                                        onClick = {
                                            onDismiss()
                                            val intent =
                                                Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    type = "text/plain"
                                                    putExtra(
                                                        Intent.EXTRA_TEXT,
                                                        "https://music.youtube.com/playlist?list=${album.album.playlistId}",
                                                    )
                                                }
                                            context.startActivity(Intent.createChooser(intent, null))
                                        },
                                    ),
                                )
                            }
                        },
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            val actionCount = 4
            MenuSurfaceSection {
                NewMenuItem(
                    headlineContent = { Text(text = stringResource(R.string.play_next)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.playlist_play),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        onDismiss()
                        playerConnection.playNext(songs.map { it.toMediaItem() })
                    },
                    index = 0,
                    count = actionCount,
                )

                NewMenuItem(
                    headlineContent = { Text(text = stringResource(R.string.add_to_queue)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        onDismiss()
                        playerConnection.addToQueue(songs.map { it.toMediaItem() })
                    },
                    index = 1,
                    count = actionCount,
                )

                NewMenuItem(
                    headlineContent = { Text(text = stringResource(R.string.add_to_playlist)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.playlist_add),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        showChoosePlaylistDialog = true
                    },
                    index = 2,
                    count = actionCount,
                )

                NewMenuItem(
                    headlineContent = {
                        Text(
                            text =
                                stringResource(
                                    if (isInSpeedDial) {
                                        R.string.remove_from_speed_dial
                                    } else {
                                        R.string.pin_to_speed_dial
                                    },
                                ),
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(if (isInSpeedDial) R.drawable.bookmark_filled else R.drawable.bookmark),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        val updatedPins = toggleSpeedDialPin(speedDialPins, albumPin)
                        onSpeedDialSongIdsChange(serializeSpeedDialPins(updatedPins))
                        onDismiss()
                    },
                    index = 3,
                    count = actionCount,
                )
            }
        }

        if (!isLocalAlbum) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                MenuSurfaceSection {
                    when (downloadState) {
                        STATE_COMPLETED -> {
                            NewMenuItem(
                                headlineContent = {
                                    Text(
                                        text = stringResource(R.string.remove_download),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.offline),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    songs.forEach { song ->
                                        DownloadService.sendRemoveDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            song.id,
                                            false,
                                        )
                                    }
                                },
                                index = 0,
                                count = 1,
                            )
                        }

                        STATE_QUEUED, STATE_DOWNLOADING -> {
                            NewMenuItem(
                                headlineContent = { Text(text = stringResource(R.string.downloading)) },
                                leadingContent = {
                                    CircularWavyProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                                onClick = {
                                    songs.forEach { song ->
                                        DownloadService.sendRemoveDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            song.id,
                                            false,
                                        )
                                    }
                                },
                                index = 0,
                                count = 1,
                            )
                        }

                        else -> {
                            NewMenuItem(
                                headlineContent = { Text(text = stringResource(R.string.action_download)) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.download),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    sendAddMissingDownloads(
                                        context = context,
                                        songs =
                                            songs.map { song ->
                                                HeaderDownloadItem(
                                                    id = song.id,
                                                    title = song.song.title,
                                                )
                                            },
                                        downloads = downloadUtil.downloads.value,
                                    )
                                },
                                index = 0,
                                count = 1,
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            val navItemCount = if (!isLocalAlbum) 2 else 1
            MenuSurfaceSection {
                NewMenuItem(
                    headlineContent = { Text(text = stringResource(R.string.view_artist)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.artist),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        if (splitArtists.size == 1 && splitArtists[0].originalArtist != null) {
                            navController.navigate("artist/${splitArtists[0].originalArtist!!.id}")
                            onDismiss()
                        } else {
                            showSelectArtistDialog = true
                        }
                    },
                    index = 0,
                    count = navItemCount,
                )

                if (!isLocalAlbum) {
                    NewMenuItem(
                        headlineContent = { Text(text = stringResource(R.string.refetch)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.sync),
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer(rotationZ = rotationAnimation),
                            )
                        },
                        onClick = {
                            refetchIconDegree -= 360
                            scope.launch(Dispatchers.IO) {
                                YouTube.album(album.id).onSuccess {
                                    database.transaction {
                                        update(album.album, it, album.artists)
                                    }
                                }
                            }
                        },
                        index = 1,
                        count = navItemCount,
                    )
                }
            }
        }
    }
}
