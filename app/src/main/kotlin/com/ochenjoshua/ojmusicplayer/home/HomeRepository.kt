/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.home

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.ochenjoshua.ojmusicplayer.constants.DisableBlurKey
import com.ochenjoshua.ojmusicplayer.constants.QuickPicksDisplayMode
import com.ochenjoshua.ojmusicplayer.constants.QuickPicksDisplayModeKey
import com.ochenjoshua.ojmusicplayer.constants.ShowHomeCategoryChipsKey
import com.ochenjoshua.ojmusicplayer.extensions.toEnum
import com.ochenjoshua.ojmusicplayer.utils.dataStore
import javax.inject.Inject

class HomeRepository
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        val showCategoryChips: Flow<Boolean> =
            context.dataStore.data
                .map { preferences -> preferences[ShowHomeCategoryChipsKey] ?: true }
                .distinctUntilChanged()

        val quickPicksDisplayMode: Flow<QuickPicksDisplayMode> =
            context.dataStore.data
                .map { preferences ->
                    preferences[QuickPicksDisplayModeKey].toEnum(QuickPicksDisplayMode.CARD)
                }.distinctUntilChanged()

        val showTonalBackdrop: Flow<Boolean> =
            context.dataStore.data
                .map { preferences -> preferences[DisableBlurKey] != true }
                .distinctUntilChanged()
    }
