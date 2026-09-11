package com.ochenjoshua.ojmusicplayer.ui.state

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import com.ochenjoshua.ojmusicplayer.lyrics.LyricsEntry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PlayerUiStateTest {

    @Before
    fun setup() {
        mockkStatic(android.graphics.Color::class)
        every { android.graphics.Color.parseColor(any()) } returns 0
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `test default values`() {
        val state = PlayerUiState()
        assertEquals("Yuma", state.title)
        assertEquals("Playback...", state.artist)
        assertEquals(emptyList<LyricsEntry>(), state.lyricsList)
        assertNull(state.lyricsRomanizationPrefs)
    }

    @Test
    fun `test copy with lyricsList`() {
        val state = PlayerUiState()
        val lyrics = listOf(
            LyricsEntry(time = 1000L, text = "Line 1"),
            LyricsEntry(time = 2000L, text = "Line 2")
        )
        val newState = state.copy(lyricsList = lyrics)
        
        assertEquals(2, newState.lyricsList.size)
        assertEquals("Line 1", newState.lyricsList[0].text)
        assertEquals(1000L, newState.lyricsList[0].time)
    }
}
