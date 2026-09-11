/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.ochenjoshua.ojmusicplayer.constants.HideExplicitKey
import com.ochenjoshua.ojmusicplayer.constants.HideVideoKey
import com.ochenjoshua.ojmusicplayer.db.MusicDatabase
import com.ochenjoshua.ojmusicplayer.extensions.filterBlockedArtists
import com.ochenjoshua.ojmusicplayer.innertube.YouTube
import com.ochenjoshua.ojmusicplayer.innertube.models.filterExplicit
import com.ochenjoshua.ojmusicplayer.innertube.models.filterVideo
import com.ochenjoshua.ojmusicplayer.innertube.pages.ExplorePage
import com.ochenjoshua.ojmusicplayer.utils.dataStore
import com.ochenjoshua.ojmusicplayer.utils.get
import com.ochenjoshua.ojmusicplayer.utils.reportException
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel
    @Inject
    constructor(
        @ApplicationContext val context: Context,
        val database: MusicDatabase,
    ) : ViewModel() {
        val explorePage = MutableStateFlow<ExplorePage?>(null)

        private suspend fun load() {
            YouTube
                .explore()
                .onSuccess { page ->
                    val blockedArtistIds = database.getBlockedArtistIds().toSet()
                    val artists: MutableMap<Int, String> = mutableMapOf()
                    val favouriteArtists: MutableMap<Int, String> = mutableMapOf()
                    database.allArtistsByPlayTime().first().let { list ->
                        var favIndex = 0
                        for ((artistsIndex, artist) in list.withIndex()) {
                            artists[artistsIndex] = artist.id
                            if (artist.artist.bookmarkedAt != null) {
                                favouriteArtists[favIndex] = artist.id
                                favIndex++
                            }
                        }
                    }
                    explorePage.value =
                        page.copy(
                            newReleaseAlbums =
                                page.newReleaseAlbums
                                    .sortedBy { album ->
                                        val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                                        val firstArtistKey =
                                            artistIds.firstNotNullOfOrNull { artistId ->
                                                if (artistId in favouriteArtists.values) {
                                                    favouriteArtists.entries.firstOrNull { it.value == artistId }?.key
                                                } else {
                                                    artists.entries.firstOrNull { it.value == artistId }?.key
                                                }
                                            } ?: Int.MAX_VALUE
                                        firstArtistKey
                                    }.filterExplicit(
                                        context.dataStore.get(HideExplicitKey, false),
                                    ).filterVideo(context.dataStore.get(HideVideoKey, false))
                                    .filterBlockedArtists(blockedArtistIds),
                        )
                }.onFailure {
                    reportException(it)
                }
        }

        init {
            viewModelScope.launch(Dispatchers.IO) {
                load()
            }
        }
    }
