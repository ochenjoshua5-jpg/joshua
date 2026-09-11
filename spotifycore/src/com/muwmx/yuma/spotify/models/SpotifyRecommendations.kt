/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyRecommendations(
    val tracks: List<SpotifyTrack> = emptyList(),
    val seeds: List<SpotifyRecommendationSeed> = emptyList(),
)

@Serializable
data class SpotifyRecommendationSeed(
    val id: String? = null,
    val type: String? = null,
    val href: String? = null,
)
