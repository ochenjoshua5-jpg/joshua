/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.viewmodels

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.ochenjoshua.ojmusicplayer.innertube.YouTube
import com.ochenjoshua.ojmusicplayer.innertube.models.AlbumItem
import com.ochenjoshua.ojmusicplayer.innertube.models.ArtistItem
import com.ochenjoshua.ojmusicplayer.innertube.models.PlaylistItem
import com.ochenjoshua.ojmusicplayer.innertube.utils.completed
import com.ochenjoshua.ojmusicplayer.utils.reportException
import javax.inject.Inject

enum class AccountContentType {
    PLAYLISTS,
    ALBUMS,
    ARTISTS,
}

@Immutable
sealed interface AccountScreenUiState {
    data object Loading : AccountScreenUiState

    data class Success(
        val playlists: List<PlaylistItem> = emptyList(),
        val albums: List<AlbumItem> = emptyList(),
        val artists: List<ArtistItem> = emptyList(),
        val selectedContentType: AccountContentType = AccountContentType.PLAYLISTS,
    ) : AccountScreenUiState

    data object Empty : AccountScreenUiState

    data class Error(val message: String? = null) : AccountScreenUiState
}

@HiltViewModel
class AccountViewModel
    @Inject
    constructor() : ViewModel() {

        private val _uiState = MutableStateFlow<AccountScreenUiState>(AccountScreenUiState.Loading)
        val uiState: StateFlow<AccountScreenUiState> = _uiState.asStateFlow()

        init {
            loadAccountContent()
        }

        fun loadAccountContent() {
            viewModelScope.launch {
                _uiState.value = AccountScreenUiState.Loading

                val playlistsDeferred = async {
                    YouTube.library("FEmusic_liked_playlists").completed()
                }
                val albumsDeferred = async {
                    YouTube.library("FEmusic_liked_albums").completed()
                }
                val artistsDeferred = async {
                    YouTube.library("FEmusic_library_corpus_artists").completed()
                }

                val playlistsResult = playlistsDeferred.await()
                val albumsResult = albumsDeferred.await()
                val artistsResult = artistsDeferred.await()

                var playlists: List<PlaylistItem> = emptyList()
                var albums: List<AlbumItem> = emptyList()
                var artists: List<ArtistItem> = emptyList()
                var hasError = false

                playlistsResult
                    .onSuccess {
                        playlists = it.items.filterIsInstance<PlaylistItem>().filterNot { item -> item.id == "SE" }
                    }
                    .onFailure {
                        reportException(it)
                        hasError = true
                    }

                albumsResult
                    .onSuccess {
                        albums = it.items.filterIsInstance<AlbumItem>()
                    }
                    .onFailure {
                        reportException(it)
                        hasError = true
                    }

                artistsResult
                    .onSuccess {
                        artists = it.items.filterIsInstance<ArtistItem>()
                    }
                    .onFailure {
                        reportException(it)
                        hasError = true
                    }

                if (playlists.isEmpty() && albums.isEmpty() && artists.isEmpty()) {
                    _uiState.value = if (hasError) AccountScreenUiState.Error() else AccountScreenUiState.Empty
                } else {
                    _uiState.value = AccountScreenUiState.Success(
                        playlists = playlists,
                        albums = albums,
                        artists = artists,
                        selectedContentType = AccountContentType.PLAYLISTS,
                    )
                }
            }
        }

        fun setSelectedContentType(contentType: AccountContentType) {
            val currentState = _uiState.value
            if (currentState is AccountScreenUiState.Success) {
                _uiState.value = currentState.copy(selectedContentType = contentType)
            }
        }
    }
