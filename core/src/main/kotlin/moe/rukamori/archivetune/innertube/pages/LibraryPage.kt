/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.innertube.pages

import com.ochenjoshua.ojmusicplayer.innertube.models.AlbumItem
import com.ochenjoshua.ojmusicplayer.innertube.models.Artist
import com.ochenjoshua.ojmusicplayer.innertube.models.ArtistItem
import com.ochenjoshua.ojmusicplayer.innertube.models.MusicResponsiveListItemRenderer
import com.ochenjoshua.ojmusicplayer.innertube.models.MusicTwoRowItemRenderer
import com.ochenjoshua.ojmusicplayer.innertube.models.PlaylistItem
import com.ochenjoshua.ojmusicplayer.innertube.models.Run
import com.ochenjoshua.ojmusicplayer.innertube.models.YTItem

data class LibraryPage(
    val items: List<YTItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
            return when {
                renderer.isAlbum -> {
                    val browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null
                    val playlistId =
                        renderer.thumbnailOverlay
                            ?.musicItemThumbnailOverlayRenderer
                            ?.content
                            ?.musicPlayButtonRenderer
                            ?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint
                            ?.playlistId
                            ?: renderer.menu
                                ?.menuRenderer
                                ?.items
                                ?.firstOrNull()
                                ?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint
                                ?.playlistId
                            ?: browseId.removePrefix("MPREb_").let { "OLAK5uy_$it" }

                    AlbumItem(
                        browseId = browseId,
                        playlistId = playlistId,
                        title =
                            renderer.title.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        artists = parseArtists(renderer.subtitle?.runs),
                        year =
                            renderer.subtitle
                                ?.runs
                                ?.lastOrNull()
                                ?.text
                                ?.toIntOrNull(),
                        thumbnail =
                            renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                        explicit =
                            renderer.subtitleBadges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null,
                    )
                }

                renderer.isPlaylist -> {
                    PlaylistItem(
                        id =
                            renderer.navigationEndpoint.browseEndpoint
                                ?.browseId
                                ?.removePrefix("VL") ?: return null,
                        title =
                            renderer.title.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        author = null,
                        songCountText =
                            renderer.subtitle
                                ?.runs
                                ?.lastOrNull()
                                ?.text,
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        playEndpoint =
                            renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer
                                ?.content
                                ?.musicPlayButtonRenderer
                                ?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint,
                        shuffleEndpoint =
                            renderer.menu
                                ?.menuRenderer
                                ?.items
                                ?.find {
                                    it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                                }?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint,
                        radioEndpoint =
                            renderer.menu
                                ?.menuRenderer
                                ?.items
                                ?.find {
                                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                                }?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint,
                        isEditable =
                            renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "EDIT"
                            } != null,
                    )
                }

                renderer.isArtist -> {
                    ArtistItem(
                        id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        title =
                            renderer.title.runs
                                ?.lastOrNull()
                                ?.text ?: return null,
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        shuffleEndpoint =
                            renderer.menu
                                ?.menuRenderer
                                ?.items
                                ?.find {
                                    it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                                }?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint ?: return null,
                        radioEndpoint =
                            renderer.menu.menuRenderer.items
                                .find {
                                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                                }?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint ?: return null,
                    )
                }

                else -> {
                    null
                }
            }
        }

        fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): YTItem? {
            return when {
                renderer.isSong -> {
                    renderer.toSongItem()
                }

                renderer.isArtist -> {
                    ArtistItem(
                        id = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                        title =
                            renderer.flexColumns
                                .firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer
                                ?.text
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                                ?: return null,
                        thumbnail =
                            renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                        shuffleEndpoint =
                            renderer.menu
                                ?.menuRenderer
                                ?.items
                                ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                                ?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint,
                        radioEndpoint =
                            renderer.menu
                                ?.menuRenderer
                                ?.items
                                ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                                ?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint,
                    )
                }

                else -> {
                    null
                }
            }
        }

        private fun parseArtists(runs: List<Run>?): List<Artist> {
            val artists = mutableListOf<Artist>()

            if (runs != null) {
                for (run in runs) {
                    if (run.navigationEndpoint != null) {
                        artists.add(
                            Artist(
                                id = run.navigationEndpoint.browseEndpoint?.browseId!!,
                                name = run.text,
                            ),
                        )
                    }
                }
            }
            return artists
        }
    }
}
