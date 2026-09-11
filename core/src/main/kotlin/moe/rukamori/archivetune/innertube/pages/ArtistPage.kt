/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.innertube.pages

import com.ochenjoshua.ojmusicplayer.innertube.models.Album
import com.ochenjoshua.ojmusicplayer.innertube.models.AlbumItem
import com.ochenjoshua.ojmusicplayer.innertube.models.Artist
import com.ochenjoshua.ojmusicplayer.innertube.models.ArtistItem
import com.ochenjoshua.ojmusicplayer.innertube.models.BrowseEndpoint
import com.ochenjoshua.ojmusicplayer.innertube.models.MusicCarouselShelfRenderer
import com.ochenjoshua.ojmusicplayer.innertube.models.MusicResponsiveListItemRenderer
import com.ochenjoshua.ojmusicplayer.innertube.models.MusicShelfRenderer
import com.ochenjoshua.ojmusicplayer.innertube.models.MusicTwoRowItemRenderer
import com.ochenjoshua.ojmusicplayer.innertube.models.PlaylistItem
import com.ochenjoshua.ojmusicplayer.innertube.models.SectionListRenderer
import com.ochenjoshua.ojmusicplayer.innertube.models.SongItem
import com.ochenjoshua.ojmusicplayer.innertube.models.YTItem
import com.ochenjoshua.ojmusicplayer.innertube.models.getItems
import com.ochenjoshua.ojmusicplayer.innertube.models.oddElements

enum class ArtistSectionLayout {
    LIST,
    GRID,
}

data class ArtistSection(
    val title: String,
    val items: List<YTItem>,
    val moreEndpoint: BrowseEndpoint?,
    val layout: ArtistSectionLayout,
)

data class ArtistPage(
    val artist: ArtistItem,
    val sections: List<ArtistSection>,
    val description: String?,
) {
    companion object {
        fun fromSectionListRendererContent(content: SectionListRenderer.Content): ArtistSection? =
            when {
                content.musicShelfRenderer != null -> fromMusicShelfRenderer(content.musicShelfRenderer)
                content.musicCarouselShelfRenderer != null -> fromMusicCarouselShelfRenderer(content.musicCarouselShelfRenderer)
                else -> null
            }

        private fun fromMusicShelfRenderer(renderer: MusicShelfRenderer): ArtistSection? {
            return ArtistSection(
                title =
                    renderer.title
                        ?.runs
                        ?.firstOrNull()
                        ?.text ?: "",
                items =
                    renderer.contents
                        ?.getItems()
                        ?.mapNotNull {
                            fromMusicResponsiveListItemRenderer(it)
                        }?.ifEmpty { null } ?: return null,
                moreEndpoint =
                    renderer.title
                        ?.runs
                        ?.firstOrNull()
                        ?.navigationEndpoint
                        ?.browseEndpoint,
                layout = ArtistSectionLayout.LIST,
            )
        }

        private fun fromMusicCarouselShelfRenderer(renderer: MusicCarouselShelfRenderer): ArtistSection? {
            return ArtistSection(
                title =
                    renderer.header
                        ?.musicCarouselShelfBasicHeaderRenderer
                        ?.title
                        ?.runs
                        ?.firstOrNull()
                        ?.text ?: return null,
                items =
                    renderer.contents
                        .mapNotNull {
                            it.musicTwoRowItemRenderer?.let { renderer ->
                                fromMusicTwoRowItemRenderer(renderer)
                            }
                        }.ifEmpty { null } ?: return null,
                moreEndpoint =
                    renderer.header.musicCarouselShelfBasicHeaderRenderer.moreContentButton
                        ?.buttonRenderer
                        ?.navigationEndpoint
                        ?.browseEndpoint,
                layout = ArtistSectionLayout.GRID,
            )
        }

        private fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): SongItem? {
            val endpoint =
                renderer.overlay
                    ?.musicItemThumbnailOverlayRenderer
                    ?.content
                    ?.musicPlayButtonRenderer
                    ?.playNavigationEndpoint
            val watchEndpoint =
                endpoint?.anyWatchEndpoint
                    ?: renderer.navigationEndpoint?.anyWatchEndpoint

            return SongItem(
                id =
                    renderer.playlistItemData?.videoId
                        ?: watchEndpoint?.videoId
                        ?: return null,
                title =
                    renderer.flexColumns
                        .firstOrNull()
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.runs
                        ?.firstOrNull()
                        ?.text ?: return null,
                artists =
                    PageHelper
                        .extractRuns(renderer.flexColumns, "MUSIC_PAGE_TYPE_ARTIST")
                        .ifEmpty {
                            renderer.flexColumns
                                .getOrNull(1)
                                ?.musicResponsiveListItemFlexColumnRenderer
                                ?.text
                                ?.runs
                        }?.oddElements()
                        ?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        }.orEmpty(),
                album =
                    PageHelper
                        .extractRuns(renderer.flexColumns, "MUSIC_PAGE_TYPE_ALBUM")
                        .ifEmpty {
                            renderer.flexColumns
                                .getOrNull(3)
                                ?.musicResponsiveListItemFlexColumnRenderer
                                ?.text
                                ?.runs
                        }?.firstOrNull()
                        ?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null,
                            )
                        },
                duration = null,
                thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                explicit =
                    renderer.badges?.find {
                        it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                    } != null,
                endpoint = watchEndpoint,
            )
        }

        private fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
            return when {
                renderer.isSong -> {
                    SongItem(
                        id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
                        title =
                            renderer.title.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        artists =
                            listOfNotNull(
                                renderer.subtitle?.runs?.firstOrNull()?.let {
                                    Artist(
                                        name = it.text,
                                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                    )
                                },
                            ),
                        album = null,
                        duration = null,
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit =
                            renderer.subtitleBadges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null,
                    )
                }

                renderer.isAlbum -> {
                    AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId =
                            renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer
                                ?.content
                                ?.musicPlayButtonRenderer
                                ?.playNavigationEndpoint
                                ?.anyWatchEndpoint
                                ?.playlistId ?: return null,
                        title =
                            renderer.title.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        artists = null,
                        year =
                            renderer.subtitle
                                ?.runs
                                ?.lastOrNull()
                                ?.text
                                ?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit =
                            renderer.subtitleBadges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null,
                    )
                }

                renderer.isPlaylist -> {
                    // Playlist from YouTube Music
                    PlaylistItem(
                        id =
                            renderer.navigationEndpoint.browseEndpoint
                                ?.browseId
                                ?.removePrefix("VL") ?: return null,
                        title =
                            renderer.title.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        author =
                            Artist(
                                name =
                                    renderer.subtitle
                                        ?.runs
                                        ?.lastOrNull()
                                        ?.text ?: return null,
                                id = null,
                            ),
                        songCountText = null,
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        playEndpoint =
                            renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer
                                ?.content
                                ?.musicPlayButtonRenderer
                                ?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint ?: return null,
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

                renderer.isArtist -> {
                    ArtistItem(
                        id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        title =
                            renderer.title.runs
                                ?.lastOrNull()
                                ?.text ?: return null,
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        channelId =
                            renderer.menu
                                ?.menuRenderer
                                ?.items
                                ?.find {
                                    it.toggleMenuServiceItemRenderer?.defaultIcon?.iconType == "SUBSCRIBE"
                                }?.toggleMenuServiceItemRenderer
                                ?.defaultServiceEndpoint
                                ?.subscribeEndpoint
                                ?.channelIds
                                ?.firstOrNull(),
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
    }
}
