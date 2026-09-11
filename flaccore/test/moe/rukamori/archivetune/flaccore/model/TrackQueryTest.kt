package com.ochenjoshua.ojmusicplayer.flaccore.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackQueryTest {

    @Test
    fun `with ISRC — isrc first then full artist title`() {
        val query = TrackQuery(artist = "Artist", title = "Title", isrc = "USRC123")
        assertEquals(listOf("USRC123", "Artist Title"), query.searchTerms())
    }

    @Test
    fun `multi-artist with ISRC — isrc, full, primary`() {
        val query = TrackQuery(artist = "Artist1, Artist2", title = "Title", isrc = "USRC123")
        assertEquals(
            listOf("USRC123", "Artist1, Artist2 Title", "Artist1 Title"),
            query.searchTerms(),
        )
    }

    @Test
    fun `without ISRC — full artist title only`() {
        val query = TrackQuery(artist = "Artist", title = "Title")
        assertEquals(listOf("Artist Title"), query.searchTerms())
    }

    @Test
    fun `blank ISRC treated as absent`() {
        val query = TrackQuery(artist = "Artist", title = "Title", isrc = "  ")
        assertEquals(listOf("Artist Title"), query.searchTerms())
    }

    @Test
    fun `multi-artist without ISRC — full and primary`() {
        val query = TrackQuery(artist = "Artist1, Artist2", title = "Title")
        assertEquals(
            listOf("Artist1, Artist2 Title", "Artist1 Title"),
            query.searchTerms(),
        )
    }

    @Test
    fun `single artist with comma in name — no duplicate fallback`() {
        val query = TrackQuery(artist = "Tyler, The Creator", title = "See You Again")
        val terms = query.searchTerms()
        assertEquals(
            listOf("Tyler, The Creator See You Again", "Tyler See You Again"),
            terms,
        )
    }

    @Test
    fun `duplicates are removed`() {
        val query = TrackQuery(artist = "Solo", title = "Track", isrc = "Solo Track")
        assertEquals(listOf("Solo Track"), query.searchTerms())
    }
}
