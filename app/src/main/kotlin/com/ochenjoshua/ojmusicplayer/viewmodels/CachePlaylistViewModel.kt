/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.ochenjoshua.ojmusicplayer.constants.HideExplicitKey
import com.ochenjoshua.ojmusicplayer.db.MusicDatabase
import com.ochenjoshua.ojmusicplayer.db.entities.Song
import com.ochenjoshua.ojmusicplayer.di.DownloadCache
import com.ochenjoshua.ojmusicplayer.di.PlayerCache
import com.ochenjoshua.ojmusicplayer.extensions.filterExplicit
import com.ochenjoshua.ojmusicplayer.utils.dataStore
import com.ochenjoshua.ojmusicplayer.utils.get
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class CachePlaylistViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MusicDatabase,
        @PlayerCache private val playerCache: Cache,
        @DownloadCache private val downloadCache: Cache,
    ) : ViewModel() {
        private val _cachedSongs = MutableStateFlow<List<Song>>(emptyList())
        val cachedSongs: StateFlow<List<Song>> = _cachedSongs

        init {
            viewModelScope.launch(Dispatchers.IO) {
                while (true) {
                    val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                    val cachedIds = playerCache.keys.map { it.removePrefix("flac_") }.toSet()
                    val downloadedIds = downloadCache.keys.map { it.removePrefix("flac_") }.toSet()
                    val pureCacheIds = cachedIds.subtract(downloadedIds)

                    val songs =
                        if (pureCacheIds.isNotEmpty()) {
                            database.getSongsByIds(pureCacheIds.toList())
                        } else {
                            emptyList()
                        }

                    val completeSongs =
                        songs.filter {
                            val id = it.song.id
                            val flacId = "flac_$id"

                            val ytLength = playerCache.getContentMetadata(id).get(ContentMetadata.KEY_CONTENT_LENGTH, -1L)
                            val flacLength = playerCache.getContentMetadata(flacId).get(ContentMetadata.KEY_CONTENT_LENGTH, -1L)

                            (ytLength > 0 && playerCache.isCached(id, 0, ytLength)) ||
                                (flacLength > 0 && playerCache.isCached(flacId, 0, flacLength))
                        }

                    if (completeSongs.isNotEmpty()) {
                        database.query {
                            completeSongs.forEach {
                                if (it.song.dateDownload == null) {
                                    update(it.song.copy(dateDownload = LocalDateTime.now()))
                                }
                            }
                        }
                    }

                    _cachedSongs.value =
                        completeSongs
                            .filter { it.song.dateDownload != null }
                            .sortedByDescending { it.song.dateDownload }
                            .filterExplicit(hideExplicit)

                    delay(1000)
                }
            }
        }

        fun removeSongFromCache(songId: String) {
            playerCache.removeResource(songId)
            playerCache.removeResource("flac_$songId")
        }
    }
