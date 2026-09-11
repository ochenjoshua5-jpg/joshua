/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class AutomixPreviewVideoRenderer(
    val content: Content,
) {
    @Serializable
    data class Content(
        val automixPlaylistVideoRenderer: AutomixPlaylistVideoRenderer,
    ) {
        @Serializable
        data class AutomixPlaylistVideoRenderer(
            val navigationEndpoint: NavigationEndpoint,
        )
    }
}
