/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.home

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.google.common.collect.ImmutableList
import com.ochenjoshua.ojmusicplayer.constants.QuickPicksDisplayMode
import com.ochenjoshua.ojmusicplayer.db.entities.LocalItem
import com.ochenjoshua.ojmusicplayer.db.entities.Song
import com.ochenjoshua.ojmusicplayer.innertube.models.PlaylistItem
import com.ochenjoshua.ojmusicplayer.innertube.pages.HomePage
import com.ochenjoshua.ojmusicplayer.models.SimilarRecommendation

sealed interface HomeScreenState {
    data object Loading : HomeScreenState

    @Immutable
    data class Success(
        val uiState: HomeUiState,
    ) : HomeScreenState

    data object Empty : HomeScreenState

    @Immutable
    data class Error(
        @StringRes val messageResId: Int,
    ) : HomeScreenState
}

@Immutable
data class HomeUiState(
    val quickPicks: ImmutableList<Song>,
    val speedDialItems: ImmutableList<LocalItem>,
    val forgottenFavorites: ImmutableList<Song>,
    val keepListening: ImmutableList<LocalItem>,
    val similarRecommendations: ImmutableList<SimilarRecommendation>,
    val accountPlaylists: ImmutableList<PlaylistItem>,
    val homePage: HomePage?,
    val selectedChip: HomePage.Chip?,
    val accountName: String,
    val accountImageUrl: String?,
    val quickPicksDisplayMode: QuickPicksDisplayMode,
    val showCategoryChips: Boolean,
    val showTonalBackdrop: Boolean,
    val isRefreshing: Boolean,
    val isLoadingMore: Boolean,
)

sealed interface HomeAction {
    data object Refresh : HomeAction

    data class SelectChip(
        val chip: HomePage.Chip?,
    ) : HomeAction

    data class LoadMore(
        val continuation: String?,
    ) : HomeAction
}
