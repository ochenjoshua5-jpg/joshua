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
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ochenjoshua.ojmusicplayer.LocalDatabase
import com.ochenjoshua.ojmusicplayer.LocalDownloadUtil
import com.ochenjoshua.ojmusicplayer.LocalPlayerConnection
import com.ochenjoshua.ojmusicplayer.R
import com.ochenjoshua.ojmusicplayer.constants.ArtistSeparatorsKey
import com.ochenjoshua.ojmusicplayer.constants.ListItemHeight
import com.ochenjoshua.ojmusicplayer.constants.ListThumbnailSize
import com.ochenjoshua.ojmusicplayer.constants.SpeedDialSongIdsKey
import com.ochenjoshua.ojmusicplayer.db.entities.Song
import com.ochenjoshua.ojmusicplayer.extensions.toMediaItem
import com.ochenjoshua.ojmusicplayer.innertube.YouTube
import com.ochenjoshua.ojmusicplayer.innertube.models.AlbumItem
import com.ochenjoshua.ojmusicplayer.playback.ExoDownloadService
import com.ochenjoshua.ojmusicplayer.playback.queues.YouTubeAlbumRadio
import com.ochenjoshua.ojmusicplayer.ui.component.ListDialog
import com.ochenjoshua.ojmusicplayer.ui.component.MenuSurfaceSection
import com.ochenjoshua.ojmusicplayer.ui.component.NewAction
import com.ochenjoshua.ojmusicplayer.ui.component.NewActionGrid
import com.ochenjoshua.ojmusicplayer.ui.component.NewMenuItem
import com.ochenjoshua.ojmusicplayer.ui.component.SongListItem
import com.ochenjoshua.ojmusicplayer.ui.component.YouTubeListItem
import com.ochenjoshua.ojmusicplayer.ui.utils.HeaderDownloadItem
import com.ochenjoshua.ojmusicplayer.ui.utils.sendAddMissingDownloads
import com.ochenjoshua.ojmusicplayer.utils.SpeedDialPin
import com.ochenjoshua.ojmusicplayer.utils.SpeedDialPinType
import com.ochenjoshua.ojmusicplayer.utils.parseSpeedDialPins
import com.ochenjoshua.ojmusicplayer.utils.rememberPreference
import com.ochenjoshua.ojmusicplayer.utils.reportException
import com.ochenjoshua.ojmusicplayer.utils.serializeSpeedDialPins
import com.ochenjoshua.ojmusicplayer.utils.toggleSpeedDialPin

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubeAlbumMenu(
    albumItem: AlbumItem,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val album by database.albumWithSongs(albumItem.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        database.album(albumItem.id).collect { album ->
            if (album == null) {
                YouTube
                    .album(albumItem.id)
                    .onSuccess { albumPage ->
                        database.transaction {
                            insert(albumPage)
                        }
                    }.onFailure {
                        reportException(it)
                    }
            }
        }
    }

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(album) {
        val songs = album?.songs?.map { it.id } ?: return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                            downloads[it]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    val (artistSeparators) = rememberPreference(ArtistSeparatorsKey, defaultValue = ",;/&")
    val (speedDialSongIds, onSpeedDialSongIdsChange) = rememberPreference(SpeedDialSongIdsKey, "")
    val speedDialPins = remember(speedDialSongIds) { parseSpeedDialPins(speedDialSongIds) }
    val albumPin = remember(albumItem.id) { SpeedDialPin(type = SpeedDialPinType.ALBUM, id = albumItem.id) }
    val isInSpeedDial =
        remember(speedDialPins, albumPin) {
            speedDialPins.any { it.type == albumPin.type && it.id == albumPin.id }
        }

    // Split artists by configured separators
    data class SplitArtist(
        val name: String,
        val originalArtist: com.ochenjoshua.ojmusicplayer.db.entities.ArtistEntity?,
    )

    val splitArtists =
        remember(album?.artists, artistSeparators) {
            val artists = album?.artists ?: emptyList()
            if (artistSeparators.isEmpty()) {
                artists.map { SplitArtist(it.name, it) }
            } else {
                val separatorRegex = "[${Regex.escape(artistSeparators)}]".toRegex()
                artists.flatMap { artist ->
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

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<Song>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            album?.songs?.map { it.id }.orEmpty()
        },
        onDismiss = { showChoosePlaylistDialog = false },
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
                    headlineContent = { Text(text = stringResource(R.string.already_in_playlist)) },
                    leadingContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier = Modifier.clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(notAddedList) { song ->
                SongListItem(song = song)
            }
        }
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
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
                        contentAlignment = Alignment.CenterStart,
                        modifier =
                            Modifier
                                .fillParentMaxWidth()
                                .height(ListItemHeight)
                                .padding(horizontal = 24.dp),
                    ) {
                        Text(
                            text = splitArtist.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    YouTubeListItem(
        item = albumItem,
        badges = {},
        trailingContent = {
            IconButton(
                onClick = {
                    database.query {
                        album?.album?.toggleLike()?.let(::update)
                    }
                },
            ) {
                Icon(
                    painter = painterResource(if (album?.album?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                    tint = if (album?.album?.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
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
                        listOf(
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
                                    album?.songs?.let { songs ->
                                        if (songs.isNotEmpty()) {
                                            playerConnection.playQueue(YouTubeAlbumRadio(albumItem.playlistId))
                                        }
                                    }
                                },
                            ),
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
                                    album?.songs?.let { songs ->
                                        if (songs.isNotEmpty()) {
                                            playerConnection.playQueue(YouTubeAlbumRadio(albumItem.playlistId))
                                        }
                                    }
                                },
                            ),
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
                                            putExtra(Intent.EXTRA_TEXT, albumItem.shareLink)
                                        }
                                    context.startActivity(Intent.createChooser(intent, null))
                                },
                            ),
                        ),
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
                        album
                            ?.songs
                            ?.map { it.toMediaItem() }
                            ?.let(playerConnection::playNext)
                        onDismiss()
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
                        album
                            ?.songs
                            ?.map { it.toMediaItem() }
                            ?.let(playerConnection::addToQueue)
                        onDismiss()
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
                        coroutineScope.launch {
                            val shouldTogglePin =
                                if (isInSpeedDial) {
                                    true
                                } else {
                                    withContext(Dispatchers.IO) {
                                        if (album != null) {
                                            true
                                        } else {
                                            val result = YouTube.album(albumItem.id)
                                            result
                                                .onSuccess { albumPage ->
                                                    database.transaction {
                                                        insert(albumPage)
                                                    }
                                                }.onFailure(::reportException)
                                            result.isSuccess
                                        }
                                    }
                                }

                            if (!shouldTogglePin) return@launch

                            val updatedPins = toggleSpeedDialPin(speedDialPins, albumPin)
                            onSpeedDialSongIdsChange(serializeSpeedDialPins(updatedPins))
                            onDismiss()
                        }
                    },
                    index = 3,
                    count = actionCount,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            MenuSurfaceSection {
                when (downloadState) {
                    Download.STATE_COMPLETED -> {
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
                                album?.songs?.forEach { song ->
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

                    Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                        NewMenuItem(
                            headlineContent = { Text(text = stringResource(R.string.downloading)) },
                            leadingContent = {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            onClick = {
                                album?.songs?.forEach { song ->
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
                                album?.songs?.let { songs ->
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
                                }
                            },
                            index = 0,
                            count = 1,
                        )
                    }
                }
            }
        }

        if (albumItem.artists != null) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
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
                        count = 1,
                    )
                }
            }
        }
    }
}
