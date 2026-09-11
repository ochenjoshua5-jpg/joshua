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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ochenjoshua.ojmusicplayer.LocalDatabase
import com.ochenjoshua.ojmusicplayer.LocalPlayerConnection
import com.ochenjoshua.ojmusicplayer.R
import com.ochenjoshua.ojmusicplayer.constants.SpeedDialSongIdsKey
import com.ochenjoshua.ojmusicplayer.db.entities.ArtistEntity
import com.ochenjoshua.ojmusicplayer.innertube.models.ArtistItem
import com.ochenjoshua.ojmusicplayer.playback.queues.YouTubeQueue
import com.ochenjoshua.ojmusicplayer.ui.component.MenuSurfaceSection
import com.ochenjoshua.ojmusicplayer.ui.component.NewAction
import com.ochenjoshua.ojmusicplayer.ui.component.NewActionGrid
import com.ochenjoshua.ojmusicplayer.ui.component.NewMenuItem
import com.ochenjoshua.ojmusicplayer.ui.component.YouTubeListItem
import com.ochenjoshua.ojmusicplayer.utils.SpeedDialPin
import com.ochenjoshua.ojmusicplayer.utils.SpeedDialPinType
import com.ochenjoshua.ojmusicplayer.utils.parseSpeedDialPins
import com.ochenjoshua.ojmusicplayer.utils.rememberPreference
import com.ochenjoshua.ojmusicplayer.utils.serializeSpeedDialPins
import com.ochenjoshua.ojmusicplayer.utils.toggleSpeedDialPin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeArtistMenu(
    artist: ArtistItem,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val libraryArtist by database.artist(artist.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val (speedDialSongIds, onSpeedDialSongIdsChange) = rememberPreference(SpeedDialSongIdsKey, "")
    val speedDialPins = remember(speedDialSongIds) { parseSpeedDialPins(speedDialSongIds) }
    val artistPin = remember(artist.id) { SpeedDialPin(type = SpeedDialPinType.ARTIST, id = artist.id) }
    val isInSpeedDial =
        remember(speedDialPins, artistPin) {
            speedDialPins.any { it.type == artistPin.type && it.id == artistPin.id }
        }

    YouTubeListItem(
        item = artist,
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
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            MenuSurfaceSection {
                NewActionGrid(
                    actions =
                        buildList {
                            artist.radioEndpoint?.let { watchEndpoint ->
                                add(
                                    NewAction(
                                        icon = {
                                            Icon(
                                                painter = painterResource(R.drawable.radio),
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        },
                                        text = stringResource(R.string.start_radio),
                                        onClick = {
                                            playerConnection.playQueue(YouTubeQueue(watchEndpoint))
                                            onDismiss()
                                        },
                                    ),
                                )
                            }

                            artist.shuffleEndpoint?.let { watchEndpoint ->
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
                                            playerConnection.playQueue(YouTubeQueue(watchEndpoint))
                                            onDismiss()
                                        },
                                    ),
                                )
                            }

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
                                        val intent =
                                            Intent().apply {
                                                action = Intent.ACTION_SEND
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, artist.shareLink)
                                            }
                                        context.startActivity(Intent.createChooser(intent, null))
                                        onDismiss()
                                    },
                                ),
                            )
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
                                if (libraryArtist?.artist?.bookmarkedAt !=
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
                                    if (libraryArtist?.artist?.bookmarkedAt != null) {
                                        R.drawable.subscribed
                                    } else {
                                        R.drawable.subscribe
                                    },
                                ),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        database.query {
                            val libraryArtist = libraryArtist
                            if (libraryArtist != null) {
                                update(libraryArtist.artist.toggleLike())
                            } else {
                                insert(
                                    ArtistEntity(
                                        id = artist.id,
                                        name = artist.title,
                                        channelId = artist.channelId,
                                        thumbnailUrl = artist.thumbnail,
                                    ).toggleLike(),
                                )
                            }
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
                        coroutineScope.launch {
                            if (!isInSpeedDial) {
                                withContext(Dispatchers.IO) {
                                    database.transaction {
                                        insert(
                                            ArtistEntity(
                                                id = artist.id,
                                                name = artist.title,
                                                channelId = artist.channelId,
                                                thumbnailUrl = artist.thumbnail,
                                            ),
                                        )
                                    }
                                }
                            }

                            val updatedPins = toggleSpeedDialPin(speedDialPins, artistPin)
                            onSpeedDialSongIdsChange(serializeSpeedDialPins(updatedPins))
                            onDismiss()
                        }
                    },
                    index = 1,
                    count = actionCount,
                )
            }
        }
    }
}
