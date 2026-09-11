package com.ochenjoshua.ojmusicplayer.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class MediaMetadataToSongTest {

    @Test
    fun `toSong maps all fields correctly`() {
        val likedDate = LocalDateTime.now()
        val inLibrary = LocalDateTime.now().minusDays(1)

        val metadata = MediaMetadata(
            id = "test_id",
            title = "Test Title",
            artists = listOf(
                MediaMetadata.Artist(id = "artist_1", name = "Artist One", thumbnailUrl = "artist_thumb_1"),
                MediaMetadata.Artist(id = "artist_2", name = "Artist Two", thumbnailUrl = null)
            ),
            duration = 120,
            thumbnailUrl = "thumb_url",
            album = MediaMetadata.Album(id = "album_1", title = "Album One"),
            explicit = true,
            liked = true,
            likedDate = likedDate,
            inLibrary = inLibrary
        )

        val song = metadata.toSong()

        // Check SongEntity fields
        assertEquals("test_id", song.song.id)
        assertEquals("Test Title", song.song.title)
        assertEquals(120, song.song.duration)
        assertEquals("thumb_url", song.song.thumbnailUrl)
        assertEquals("album_1", song.song.albumId)
        assertEquals("Album One", song.song.albumName)
        assertTrue(song.song.explicit)
        assertTrue(song.song.liked)
        assertEquals(likedDate, song.song.likedDate)
        assertEquals(inLibrary, song.song.inLibrary)

        // Check Artists
        assertEquals(2, song.artists.size)
        assertEquals("artist_1", song.artists[0].id)
        assertEquals("Artist One", song.artists[0].name)
        assertEquals("artist_thumb_1", song.artists[0].thumbnailUrl)

        assertEquals("artist_2", song.artists[1].id)
        assertEquals("Artist Two", song.artists[1].name)
        assertEquals(null, song.artists[1].thumbnailUrl)

        // Check Album
        assertNotNull(song.album)
        assertEquals("album_1", song.album?.id)
        assertEquals("Album One", song.album?.title)
        assertEquals(0, song.album?.songCount)
        assertEquals(0, song.album?.duration)
    }

    @Test
    fun `toSong generates fallback artist id when null`() {
        val metadata = MediaMetadata(
            id = "test_id",
            title = "Test Title",
            artists = listOf(
                MediaMetadata.Artist(id = null, name = "Unknown Artist", thumbnailUrl = null)
            ),
            duration = 120
        )

        val song = metadata.toSong()

        assertEquals(1, song.artists.size)
        val artist = song.artists[0]
        
        assertNotNull(artist.id)
        assertTrue(artist.id.startsWith("LA"))
        assertEquals("Unknown Artist", artist.name)
    }
}
