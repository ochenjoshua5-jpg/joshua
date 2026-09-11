package com.ochenjoshua.ojmusicplayer.utils

import com.ochenjoshua.ojmusicplayer.spotify.SpotifyMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncUtilsTitleCleaningTest {

    @Test
    fun testTitleCleaning() {
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Remastered 2021)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Live at Wembley)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (feat. Artist)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (ft. Artist)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name [Radio Edit]"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Acoustic Version)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Remix)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (2011 Remaster)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Live)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Taylor's Version)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (feat. Artist 1 & Artist 2)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Remastered)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Remaster)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Live Version)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Live 2021)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Live at The O2)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Live at The O2, London)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Live at The O2, London, 2021)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Live at The O2, London, 2021) (Remastered)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Live at The O2, London, 2021) (Remastered 2021)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Live at The O2, London, 2021) (Remastered 2021) (Live)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Live at The O2, London, 2021) (Remastered 2021) (Live) (Remastered)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Live at The O2, London, 2021) (Remastered 2021) (Live) (Remastered) (Live)"))
        assertEquals("Song Name", SpotifyMapper.cleanTrackTitle("Song Name (Live at The O2, London, 2021) (Remastered 2021) (Live) (Remastered) (Live) (Remastered)"))
    }

    @Test
    fun testDurationValidation() {
        // Exact match
        assertTrue(SpotifyMapper.isValidDuration(180000, 180))
        
        // Within 5 seconds
        assertTrue(SpotifyMapper.isValidDuration(180000, 185))
        assertTrue(SpotifyMapper.isValidDuration(180000, 175))
        assertTrue(SpotifyMapper.isValidDuration(180000, 183))
        assertTrue(SpotifyMapper.isValidDuration(180000, 177))
        
        // Outside 5 seconds
        assertFalse(SpotifyMapper.isValidDuration(180000, 186))
        assertFalse(SpotifyMapper.isValidDuration(180000, 174))
        assertFalse(SpotifyMapper.isValidDuration(180000, 190))
        assertFalse(SpotifyMapper.isValidDuration(180000, 170))
        
        // Edge cases
        assertFalse(SpotifyMapper.isValidDuration(0, 180))
        assertFalse(SpotifyMapper.isValidDuration(180000, null))
    }
}
