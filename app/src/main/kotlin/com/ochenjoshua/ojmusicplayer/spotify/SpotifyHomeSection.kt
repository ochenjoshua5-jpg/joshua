package com.ochenjoshua.ojmusicplayer.spotify

import androidx.compose.runtime.Immutable
import com.ochenjoshua.ojmusicplayer.spotify.models.SpotifyAlbum
import com.ochenjoshua.ojmusicplayer.spotify.models.SpotifyArtist
import com.ochenjoshua.ojmusicplayer.spotify.models.SpotifyPlaylist
import com.ochenjoshua.ojmusicplayer.spotify.models.SpotifyTrack

@Immutable
data class SpotifyHomeSection(
    val title: String,
    val type: SectionType,
    val tracks: List<SpotifyTrack> = emptyList(),
    val artists: List<SpotifyArtist> = emptyList(),
    val albums: List<SpotifyAlbum> = emptyList(),
    val playlists: List<SpotifyPlaylist> = emptyList()
)

enum class SectionType {
    TRACKS,
    ARTISTS,
    ALBUMS,
    PLAYLISTS
}
