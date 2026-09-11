/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.innertube.pages

import com.ochenjoshua.ojmusicplayer.innertube.models.MusicResponsiveListItemRenderer.FlexColumn
import com.ochenjoshua.ojmusicplayer.innertube.models.Run

object PageHelper {
    fun extractRuns(
        columns: List<FlexColumn>,
        typeLike: String,
    ): List<Run> {
        val filteredRuns = mutableListOf<Run>()
        for (column in columns) {
            val runs =
                column.musicResponsiveListItemFlexColumnRenderer.text?.runs
                    ?: continue

            for (run in runs) {
                val typeStr =
                    run.navigationEndpoint
                        ?.watchEndpoint
                        ?.watchEndpointMusicSupportedConfigs
                        ?.watchEndpointMusicConfig
                        ?.musicVideoType
                        ?: run.navigationEndpoint
                            ?.browseEndpoint
                            ?.browseEndpointContextSupportedConfigs
                            ?.browseEndpointContextMusicConfig
                            ?.pageType
                        ?: continue

                if (typeLike in typeStr) {
                    filteredRuns.add(run)
                }
            }
        }
        return filteredRuns
    }
}
