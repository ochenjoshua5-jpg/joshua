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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.ochenjoshua.ojmusicplayer.constants.HideExplicitKey
import com.ochenjoshua.ojmusicplayer.db.MusicDatabase
import com.ochenjoshua.ojmusicplayer.extensions.filterBlockedArtists
import com.ochenjoshua.ojmusicplayer.innertube.YouTube
import com.ochenjoshua.ojmusicplayer.innertube.models.BrowseEndpoint
import com.ochenjoshua.ojmusicplayer.innertube.models.filterExplicit
import com.ochenjoshua.ojmusicplayer.innertube.pages.ArtistItemsPageLayout
import com.ochenjoshua.ojmusicplayer.models.ItemsPage
import com.ochenjoshua.ojmusicplayer.utils.dataStore
import com.ochenjoshua.ojmusicplayer.utils.get
import com.ochenjoshua.ojmusicplayer.utils.reportException
import javax.inject.Inject

@HiltViewModel
class ArtistItemsViewModel
    @Inject
    constructor(
        @ApplicationContext val context: Context,
        private val database: MusicDatabase,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val browseId = savedStateHandle.get<String>("browseId")!!
        private val params =
            savedStateHandle
                .get<String>("params")
                ?.takeUnless { it.isBlank() || it == "null" }

        val title = MutableStateFlow("")
        val itemsPage = MutableStateFlow<ItemsPage?>(null)
        val itemsLayout = MutableStateFlow(ArtistItemsPageLayout.LIST)

        init {
            viewModelScope.launch {
                YouTube
                    .artistItems(
                        BrowseEndpoint(
                            browseId = browseId,
                            params = params,
                        ),
                    ).onSuccess { artistItemsPage ->
                        title.value = artistItemsPage.title
                        itemsLayout.value = artistItemsPage.layout
                        itemsPage.value =
                            ItemsPage(
                                items =
                                    artistItemsPage.items
                                        .distinctBy { it.id }
                                        .filterExplicit(context.dataStore.get(HideExplicitKey, false))
                                        .filterBlockedArtists(database.getBlockedArtistIds().toSet()),
                                continuation = artistItemsPage.continuation,
                            )
                    }.onFailure {
                        reportException(it)
                    }
            }
        }

        fun loadMore() {
            viewModelScope.launch {
                val oldItemsPage = itemsPage.value ?: return@launch
                val continuation = oldItemsPage.continuation ?: return@launch
                YouTube
                    .artistItemsContinuation(continuation)
                    .onSuccess { artistItemsContinuationPage ->
                        itemsPage.update {
                            ItemsPage(
                                items =
                                    (oldItemsPage.items + artistItemsContinuationPage.items)
                                        .distinctBy { it.id }
                                        .filterExplicit(context.dataStore.get(HideExplicitKey, false))
                                        .filterBlockedArtists(database.getBlockedArtistIds().toSet()),
                                continuation = artistItemsContinuationPage.continuation,
                            )
                        }
                    }.onFailure {
                        reportException(it)
                    }
            }
        }
    }
