/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import com.ochenjoshua.ojmusicplayer.constants.HideExplicitKey
import com.ochenjoshua.ojmusicplayer.constants.HideVideoKey
import com.ochenjoshua.ojmusicplayer.db.MusicDatabase
import com.ochenjoshua.ojmusicplayer.extensions.filterBlockedArtists
import com.ochenjoshua.ojmusicplayer.innertube.YouTube
import com.ochenjoshua.ojmusicplayer.innertube.pages.BrowseResult
import com.ochenjoshua.ojmusicplayer.utils.dataStore
import com.ochenjoshua.ojmusicplayer.utils.get
import com.ochenjoshua.ojmusicplayer.utils.reportException
import javax.inject.Inject

@HiltViewModel
class YouTubeBrowseViewModel
    @Inject
    constructor(
        @ApplicationContext val context: Context,
        private val database: MusicDatabase,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val browseId = savedStateHandle.get<String>("browseId")!!
        private val params = savedStateHandle.get<String>("params")

        val result = MutableStateFlow<BrowseResult?>(null)

        init {
            viewModelScope.launch {
                YouTube
                    .browse(browseId, params)
                    .onSuccess {
                        val hideVideo = context.dataStore.get(HideVideoKey, false)
                        result.value =
                            it
                                .filterExplicit(context.dataStore.get(HideExplicitKey, false))
                                .filterVideo(hideVideo)
                                .filterBlockedArtists(database.getBlockedArtistIds().toSet())
                    }.onFailure {
                        reportException(it)
                    }
            }
        }
    }
