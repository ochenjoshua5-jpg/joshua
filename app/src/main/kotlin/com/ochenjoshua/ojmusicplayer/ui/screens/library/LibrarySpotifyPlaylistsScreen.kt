/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.ochenjoshua.ojmusicplayer.LocalPlayerAwareWindowInsets
import com.ochenjoshua.ojmusicplayer.LocalPlayerConnection
import com.ochenjoshua.ojmusicplayer.R
import com.ochenjoshua.ojmusicplayer.spotify.Spotify
import com.ochenjoshua.ojmusicplayer.spotify.SpotifyAccountViewModel
import com.ochenjoshua.ojmusicplayer.spotify.SpotifyLibraryViewModel
import com.ochenjoshua.ojmusicplayer.spotify.SpotifyLikedSongsQueue
import com.ochenjoshua.ojmusicplayer.spotify.SpotifyPlaybackResolver
import com.ochenjoshua.ojmusicplayer.spotify.SpotifyPlaylistQueue
import com.ochenjoshua.ojmusicplayer.ui.component.ExpressivePullToRefreshBox
import com.ochenjoshua.ojmusicplayer.ui.component.LibraryEmptyState
import com.ochenjoshua.ojmusicplayer.ui.component.SpotifyLibraryPlaylistListItem
import com.ochenjoshua.ojmusicplayer.ui.component.SpotifyLikedSongsListCard
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.SpotifyLoginFallback
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.SpotifyLoginSheet
import com.ochenjoshua.ojmusicplayer.ui.settings.SettingsDimensions
import com.ochenjoshua.ojmusicplayer.ui.theme.YumaSegmentPosition

@Composable
fun LibrarySpotifyPlaylistsScreen(
    navController: NavController,
    viewModel: SpotifyLibraryViewModel = hiltViewModel(),
    spotifyAccountViewModel: SpotifyAccountViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope = rememberCoroutineScope()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val spotifyState by spotifyAccountViewModel.uiState.collectAsStateWithLifecycle()
    var showSpotifyLogin by remember { mutableStateOf(false) }

    if (showSpotifyLogin) {
        SpotifyLoginSheet(
            onDismiss = { showSpotifyLogin = false },
            onCookiesCaptured = { spDc, spKey ->
                spotifyAccountViewModel.connectWithCookies(spDc = spDc, spKey = spKey)
                showSpotifyLogin = false
                viewModel.refreshPlaylists()
            },
        )
    }

    if (!spotifyState.isAuthenticated) {
        SpotifyLoginFallback(
            onLoginClick = { showSpotifyLogin = true },
            modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
        return
    }

    ExpressivePullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refreshPlaylists,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = rememberLazyListState(),
            contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(SettingsDimensions.LibraryItemGap),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = SettingsDimensions.ScreenHorizontalPadding),
        ) {
            item(key = "spotify_liked_songs", contentType = "spotify_liked_songs") {
                val likedSongsTotal by viewModel.likedSongsTotal.collectAsStateWithLifecycle()
                SpotifyLikedSongsListCard(
                    likedSongsTotal = likedSongsTotal,
                    position = YumaSegmentPosition.Single,
                    onClick = { navController.navigate("spotify_liked_songs") },
                    onPlay = {
                        playerConnection?.let { conn ->
                            coroutineScope.launch {
                                val preloadTrack = Spotify.likedSongs(limit = 1, offset = 0).getOrNull()?.items?.firstOrNull()?.track
                                val preloadItem = preloadTrack?.let { SpotifyPlaybackResolver.resolveToMetadata(it) }
                                conn.playQueue(
                                    SpotifyLikedSongsQueue(
                                        title = context.getString(R.string.spotify_liked_songs),
                                        preloadItem = preloadItem,
                                    )
                                )
                            }
                        }
                    },
                )
            }

            if (playlists.isEmpty()) {
                item(key = "spotify_empty", contentType = "spotify_empty") {
                    LibraryEmptyState(
                        iconRes = R.drawable.queue_music,
                        titleRes = R.string.no_playlists_yet,
                        subtitleRes = R.string.spotify_no_sources,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                itemsIndexed(
                    items = playlists,
                    key = { _, playlist -> playlist.id },
                    contentType = { _, _ -> "spotify_playlist" },
                ) { index, playlist ->
                    SpotifyLibraryPlaylistListItem(
                        playlist = playlist,
                        position = YumaSegmentPosition.Single,
                        navController = navController,
                        onPlay = {
                            playerConnection?.let { conn ->
                                coroutineScope.launch {
                                    val preloadTrack = Spotify.playlistTracks(playlistId = playlist.id, limit = 1, offset = 0).getOrNull()?.items?.firstOrNull()?.track
                                    val preloadItem = preloadTrack?.let { SpotifyPlaybackResolver.resolveToMetadata(it) }
                                    conn.playQueue(
                                        SpotifyPlaylistQueue(
                                            playlistId = playlist.id,
                                            title = playlist.name,
                                            preloadItem = preloadItem,
                                        )
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
