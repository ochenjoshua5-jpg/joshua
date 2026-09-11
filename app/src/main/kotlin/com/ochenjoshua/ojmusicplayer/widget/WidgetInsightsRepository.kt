/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.widget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.ochenjoshua.ojmusicplayer.db.MusicDatabase
import com.ochenjoshua.ojmusicplayer.db.entities.Artist
import com.ochenjoshua.ojmusicplayer.db.entities.LibraryTopMixEntity
import com.ochenjoshua.ojmusicplayer.db.entities.ListeningTotals
import com.ochenjoshua.ojmusicplayer.db.entities.Song
import com.ochenjoshua.ojmusicplayer.db.entities.SongWithStats
import java.time.Duration
import javax.inject.Inject

internal class WidgetInsightsRepository
    @Inject
    constructor(
        private val database: MusicDatabase,
    ) {
        suspend fun load(nowMs: Long): WidgetInsightsData =
            withContext(Dispatchers.IO) {
                val fromMs = nowMs - Duration.ofDays(30).toMillis()
                WidgetInsightsData(
                    recentSongs = database.recentSongs(limit = 4).first(),
                    totals = database.listeningTotals(fromTimestamp = fromMs, toTimestamp = nowMs).first(),
                    topSongs = database.mostPlayedSongsStats(fromTimeStamp = fromMs, limit = 4, toTimeStamp = nowMs).first(),
                    recommendations = database.quickPicks(now = nowMs).first().take(6),
                    topArtists = database.mostPlayedArtists(fromTimeStamp = fromMs, limit = 6, toTimeStamp = nowMs).first(),
                    topMixes = database.libraryTopMixes(limit = 4).first(),
                )
            }
    }

internal data class WidgetInsightsData(
    val recentSongs: List<Song>,
    val totals: ListeningTotals,
    val topSongs: List<SongWithStats>,
    val recommendations: List<Song>,
    val topArtists: List<Artist>,
    val topMixes: List<LibraryTopMixEntity>,
)
