/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.lyrics

import android.content.Context
import android.util.Log
import com.ochenjoshua.ojmusicplayer.constants.EnableYouLyPlusLyricsKey
import com.ochenjoshua.ojmusicplayer.utils.GlobalLog
import com.ochenjoshua.ojmusicplayer.utils.dataStore
import com.ochenjoshua.ojmusicplayer.utils.get
import com.ochenjoshua.ojmusicplayer.youlyplus.YouLyPlus

object YouLyPlusLyricsProvider : LyricsProvider {
    init {
        YouLyPlus.logger = { message ->
            GlobalLog.append(Log.INFO, "YouLyPlus", message)
        }
    }

    override val name = "YouLyPlus"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableYouLyPlusLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> =
        YouLyPlus.getLyrics(
            title = title,
            artist = artist,
            album = album,
            durationSeconds = duration,
        )

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        YouLyPlus.getAllLyrics(
            title = title,
            artist = artist,
            album = album,
            durationSeconds = duration,
            callback = callback,
        )
    }
}
