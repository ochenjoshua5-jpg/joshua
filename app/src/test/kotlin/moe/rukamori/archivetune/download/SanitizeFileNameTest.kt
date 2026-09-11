package com.ochenjoshua.ojmusicplayer.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SanitizeFileNameTest {

    @Test
    fun `special characters are replaced`() {
        val result = sanitizeFileName("a/b\\c:d*e?f\"g<h>i|j")
        assertFalse(result.any { it in listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|') })
        assertEquals("a_b_c_d_e_f_g_h_i_j", result)
    }

    @Test
    fun `leading and trailing whitespace is trimmed`() {
        assertEquals("Track", sanitizeFileName("  Track  "))
    }

    @Test
    fun `empty or blank input returns fallback`() {
        assertEquals("untitled", sanitizeFileName(""))
        assertEquals("untitled", sanitizeFileName("   "))
    }

    @Test
    fun `overlong name is capped`() {
        val result = sanitizeFileName("x".repeat(500))
        assertEquals(200, result.length)
    }
}