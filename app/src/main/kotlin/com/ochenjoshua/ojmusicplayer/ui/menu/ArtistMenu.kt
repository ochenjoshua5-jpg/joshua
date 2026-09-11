/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.ui.menu

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ochenjoshua.ojmusicplayer.LocalDatabase
import com.ochenjoshua.ojmusicplayer.LocalPlayerConnection
import com.ochenjoshua.ojmusicplayer.R
import com.ochenjoshua.ojmusicplayer.constants.ArtistSongSortType
import com.ochenjoshua.ojmusicplayer.constants.SpeedDialSongIdsKey
import com.ochenjoshua.ojmusicplayer.db.entities.Artist
import com.ochenjoshua.ojmusicplayer.extensions.toMediaItem
import com.ochenjoshua.ojmusicplayer.playback.queues.ListQueue
import com.ochenjoshua.ojmusicplayer.ui.component.ArtistListItem
import com.ochenjoshua.ojmusicplayer.ui.component.MenuSurfaceSection
import com.ochenjoshua.ojmusicplayer.ui.component.NewAction
import com.ochenjoshua.ojmusicplayer.ui.component.NewActionGrid
import com.ochenjoshua.ojmusicplayer.ui.component.NewMenuItem
import com.ochenjoshua.ojmusicplayer.utils.SpeedDialPin
import com.ochenjoshua.ojmusicplayer.utils.SpeedDialPinType
import com.ochenjoshua.ojmusicplayer.utils.parseSpeedDialPins
import com.ochenjoshua.ojmusicplayer.utils.rememberPreference
import com.ochenjoshua.ojmusicplayer.utils.serializeSpeedDialPins
import com.ochenjoshua.ojmusicplayer.utils.toggleSpeedDialPin

@Composable
fun ArtistMenu(
    originalArtist: Artist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val artistState = database.artist(originalArtist.id).collectAsState(initial = originalArtist)
    val artist = artistState.value ?: originalArtist
    val (speedDialSongIds, onSpeedDialSongIdsChange) = rememberPreference(SpeedDialSongIdsKey, "")
    val speedDialPins = remember(speedDialSongIds) { parseSpeedDialPins(speedDialSongIds) }
    val artistPin = remember(artist.id) { SpeedDialPin(type = SpeedDialPinType.ARTIST, id = artist.id) }
    val isInSpeedDial =
        remember(speedDialPins, artistPin) {
            speedDialPins.any { it.type == artistPin.type && it.id == artistPin.id }
        }

    ArtistListItem(
        artist = artist,
        badges = {},
        trailingContent = {},
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
                            if (artist.songCount > 0) {
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
                                            coroutineScope.launch {
                                                val songs =
                                                    withContext(Dispatchers.IO) {
                                                        database
                                                            .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                                            .first()
                                                            .map { it.toMediaItem() }
                                                    }
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = artist.artist.name,
                                                        items = songs,
                                                    ),
                                                )
                                            }
                                            onDismiss()
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
                                            coroutineScope.launch {
                                                val songs =
                                                    withContext(Dispatchers.IO) {
                                                        database
                                                            .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                                            .first()
                                                            .map { it.toMediaItem() }
                                                            .shuffled()
                                                    }
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = artist.artist.name,
                                                        items = songs,
                                                    ),
                                                )
                                            }
                                            onDismiss()
                                        },
                                    ),
                                )
                            }

                            if (artist.artist.isYouTubeArtist) {
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
                                                        "https://music.youtube.com/channel/${artist.id}",
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
            val actionCount = 2
            MenuSurfaceSection {
                NewMenuItem(
                    headlineContent = {
                        Text(
                            text =
                                if (artist.artist.bookmarkedAt !=
                                    null
                                ) {
                                    stringResource(R.string.subscribed)
                                } else {
                                    stringResource(R.string.subscribe)
                                },
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter =
                                painterResource(
                                    if (artist.artist.bookmarkedAt !=
                                        null
                                    ) {
                                        R.drawable.subscribed
                                    } else {
                                        R.drawable.subscribe
                                    },
                                ),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        database.transaction {
                            update(artist.artist.toggleLike())
                        }
                    },
                    index = 0,
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
                        val updatedPins = toggleSpeedDialPin(speedDialPins, artistPin)
                        onSpeedDialSongIdsChange(serializeSpeedDialPins(updatedPins))
                        onDismiss()
                    },
                    index = 1,
                    count = actionCount,
                )
            }
        }
    }
}
