/*
 * OJ MUSIC (2026) | Made by Ochen Joshua
 * Based on YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.kugou.models

import kotlinx.serialization.Serializable

@Serializable
data class DownloadLyricsResponse(
    val content: String,
)
