/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.innertube.models.body

import kotlinx.serialization.Serializable
import com.ochenjoshua.ojmusicplayer.innertube.models.Context

@Serializable
data class GetSearchSuggestionsBody(
    val context: Context,
    val input: String,
)
