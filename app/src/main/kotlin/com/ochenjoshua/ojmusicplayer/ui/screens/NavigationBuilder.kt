/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.ui.screens

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.runtime.getValue
import com.ochenjoshua.ojmusicplayer.constants.SpotifySpDcKey
import com.ochenjoshua.ojmusicplayer.constants.UseSpotifyHomeKey
import com.ochenjoshua.ojmusicplayer.utils.rememberPreference
import com.ochenjoshua.ojmusicplayer.constants.UpdateChannel
import com.ochenjoshua.ojmusicplayer.defaultUpdateChannel
import com.ochenjoshua.ojmusicplayer.musicrecognition.MusicRecognitionRoute
import com.ochenjoshua.ojmusicplayer.ui.screens.artist.ArtistAlbumsScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.artist.ArtistItemsScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.artist.ArtistScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.artist.ArtistSongsScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.library.LibraryScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.library.LocalSongScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.musicrecognition.MusicRecognitionScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.playlist.AutoPlaylistScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.playlist.CachePlaylistScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.playlist.LocalPlaylistScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.playlist.OnlinePlaylistScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.library.SpotifyLikedSongsScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.playlist.SpotifyPlaylistScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.playlist.TopPlaylistScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.search.OnlineSearchResult
import com.ochenjoshua.ojmusicplayer.ui.screens.search.OnlineSearchResultArgument
import com.ochenjoshua.ojmusicplayer.ui.screens.search.OnlineSearchResultRoute
import com.ochenjoshua.ojmusicplayer.ui.screens.search.OnlineSearchResultRoutePrefix
import com.ochenjoshua.ojmusicplayer.ui.screens.search.SearchScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.AboutScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.AccountSettings
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.AiIntegrationSettings
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.AppearanceSettings
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.BackupAndRestore
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.ChangelogScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.ContentSettings
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.DebugSettings
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.DiscordSettings
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.HiddenPlaylistsScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.IconScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.IntegrationScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.InternetSettings
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.LastFMSettings
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.LyricsSettings
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.MusicTogetherScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.PalettePickerScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.PlayerSettings
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.PoTokenScreen
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.PrivacySettings
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.StorageSettings
import com.ochenjoshua.ojmusicplayer.ui.screens.settings.ThemeCreatorScreen
import com.ochenjoshua.ojmusicplayer.ui.state.UpdateState

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    updateState: UpdateState,
    disableAnimations: Boolean = false,
    onClearUpdateBadge: () -> Unit = {},
    homeScrollConnection: NestedScrollConnection? = null,
    searchScrollConnection: NestedScrollConnection? = null,
) {
    composable(Screens.Home.route) {
        val useSpotify by rememberPreference(UseSpotifyHomeKey, defaultValue = false)
        val spDc by rememberPreference(SpotifySpDcKey, defaultValue = "")
        if (useSpotify && spDc.isNotBlank()) {
            SpotifyHomeScreen(navController, headerScrollConnection = homeScrollConnection)
        } else {
            HomeScreen(navController, headerScrollConnection = homeScrollConnection)
        }
    }
    composable(
        Screens.Library.route,
    ) {
        LibraryScreen(navController)
    }
    composable(Screens.Search.route) {
        SearchScreen(
            navController = navController,
            onSearchClick = {
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("openSearch", true)
            },
            headerScrollConnection = searchScrollConnection,
        )
    }
    composable("local_songs") {
        LocalSongScreen(navController)
    }
    composable("history") {
        HistoryScreen(navController)
    }
    composable("stats") {
        StatsScreen(navController)
    }
    composable("news") {
        NewsScreen(navController)
    }
    composable(
        route = "view_news/{newsId}",
        arguments =
            listOf(
                navArgument("newsId") { type = NavType.StringType },
            ),
    ) {
        ViewNewsScreen(navController)
    }
    composable(
        route = "year_in_music?year={year}",
        arguments =
            listOf(
                navArgument("year") {
                    type = NavType.IntType
                    defaultValue = -1
                },
            ),
    ) { backStackEntry ->
        val selectedYear = backStackEntry.arguments?.getInt("year")?.takeIf { it > 0 }
        YearInMusicScreen(
            navController = navController,
            initialYear = selectedYear,
        )
    }
    composable(MusicRecognitionRoute) {
        MusicRecognitionScreen(navController)
    }
    composable(Screens.MoodAndGenres.route) {
        MoodAndGenresScreen(navController)
    }
    composable("account") {
        AccountScreen(navController, scrollBehavior)
    }
    composable("new_release") {
        NewReleaseScreen(navController, scrollBehavior)
    }
    composable("charts_screen") {
        ChartsScreen(navController)
    }
    composable(
        route = "browse/{browseId}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                },
            ),
    ) {
        BrowseScreen(
            navController,
            scrollBehavior,
            it.arguments?.getString("browseId"),
        )
    }
    composable(
        route = OnlineSearchResultRoute,
        arguments =
            listOf(
                navArgument(OnlineSearchResultArgument) {
                    type = NavType.StringType
                },
            ),
        enterTransition = {
            if (disableAnimations) {
                fadeIn(tween(0))
            } else {
                fadeIn(tween(250))
            }
        },
        exitTransition = {
            if (disableAnimations) {
                fadeOut(tween(0))
            } else if (targetState.destination.route?.startsWith(OnlineSearchResultRoutePrefix) == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (disableAnimations) {
                fadeIn(tween(0))
            } else if (initialState.destination.route?.startsWith(OnlineSearchResultRoutePrefix) == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            if (disableAnimations) {
                fadeOut(tween(0))
            } else {
                fadeOut(tween(200))
            }
        },
    ) {
        OnlineSearchResult(navController)
    }
    composable(
        route = "album/{albumId}",
        arguments =
            listOf(
                navArgument("albumId") {
                    type = NavType.StringType
                },
            ),
    ) {
        AlbumScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/songs",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/albums",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistAlbumsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/items?browseId={browseId}&params={params}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }
    composable(
        route = "online_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "local_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "spotify_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        SpotifyPlaylistScreen(navController, scrollBehavior)
    }
    composable("spotify_liked_songs") {
        SpotifyLikedSongsScreen(navController, scrollBehavior)
    }
    composable(
        route = "auto_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "cache_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "top_playlist/{top}",
        arguments =
            listOf(
                navArgument("top") {
                    type = NavType.StringType
                },
            ),
    ) {
        TopPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        YouTubeBrowseScreen(navController)
    }
    composable("settings/account") {
        AccountSettings(navController, updateState)
    }
    composable("settings/hidden_playlists") {
        HiddenPlaylistsScreen(navController)
    }
    composable("settings/appearance") {
        AppearanceSettings(navController)
    }
    composable("settings/appearance/icon") {
        IconScreen(navController)
    }
    composable("settings/appearance/palette_picker") {
        PalettePickerScreen(navController)
    }
    composable("settings/appearance/theme_creator") {
        ThemeCreatorScreen(navController)
    }
    composable("settings/content") {
        ContentSettings(navController)
    }
    composable("settings/player") {
        PlayerSettings(navController)
    }
    composable("settings/lyrics") {
        LyricsSettings(navController)
    }
    composable("settings/internet") {
        InternetSettings(navController)
    }
    composable("settings/storage") {
        StorageSettings(navController)
    }
    composable("settings/privacy") {
        PrivacySettings(navController)
    }
    composable("settings/backup_restore") {
        BackupAndRestore(navController)
    }
    composable("settings/discord") {
        DiscordSettings(navController)
    }
    composable("settings/integration") {
        IntegrationScreen(navController)
    }
    composable("settings/ai_integration") {
        AiIntegrationSettings(navController)
    }
    composable("settings/music_together") {
        MusicTogetherScreen(navController)
    }
    composable("settings/lastfm") {
        LastFMSettings(navController)
    }
    composable("settings/discord/experimental") {
        com.ochenjoshua.ojmusicplayer.ui.screens.settings
            .DiscordExperimental(navController)
    }
    composable("settings/misc") {
        DebugSettings(navController)
    }
    composable(
        route = "settings/changelog?channel={channel}",
        arguments =
            listOf(
                navArgument("channel") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        val channelName = backStackEntry.arguments?.getString("channel")
        val channel =
            channelName?.let {
                runCatching { UpdateChannel.valueOf(it) }.getOrNull()
            } ?: defaultUpdateChannel
        ChangelogScreen(navController, channel = channel)
    }
    composable("settings/about") {
        AboutScreen(navController)
    }
    composable("settings/po_token") {
        PoTokenScreen(navController)
    }
    composable(
        route = "$LOGIN_ROUTE?$LOGIN_URL_ARGUMENT={$LOGIN_URL_ARGUMENT}",
        arguments =
            listOf(
                navArgument(LOGIN_URL_ARGUMENT) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        LoginScreen(
            navController,
            startUrl = backStackEntry.arguments?.getString(LOGIN_URL_ARGUMENT)?.let(Uri::decode),
        )
    }
}
