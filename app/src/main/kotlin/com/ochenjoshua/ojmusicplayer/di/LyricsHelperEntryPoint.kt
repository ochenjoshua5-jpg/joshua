/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.ochenjoshua.ojmusicplayer.lyrics.LyricsHelper
import com.ochenjoshua.ojmusicplayer.lyrics.LyricsPreloadManager

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LyricsHelperEntryPoint {
    fun lyricsHelper(): LyricsHelper

    fun lyricsPreloadManager(): LyricsPreloadManager
}
