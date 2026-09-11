package com.ochenjoshua.ojmusicplayer.flaccore.qobuz

import com.ochenjoshua.ojmusicplayer.flaccore.model.TrackQuery
import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Ported from Stash (GPL-3.0)
 * Original source: com.stash.data.download.lossless.qobuz.QobuzCandidateMatcherTest.kt
 */

class QobuzCandidateMatcherTest {

    @Test fun `ISRC match short-circuits to 0_95`() {
        val score = QobuzCandidateMatcher.confidence(
            query = TrackQuery(
                artist = "John Frusciante",
                title = "Murderers",
                isrc = "USWB10003085",
                durationMs = 160_000,
            ),
            candTitle = "Murderers",
            candArtist = "John Frusciante",
            candIsrc = "USWB10003085",
            candDurationSec = 160,
            candStreamable = true,
        )
        assertEquals(0.95f, score, 0.0001f)
    }

    @Test fun `ISRC match is case-insensitive`() {
        val score = QobuzCandidateMatcher.confidence(
            query = TrackQuery(artist = "x", title = "y", isrc = "uswb10003085"),
            candTitle = "totally different",
            candArtist = "someone else",
            candIsrc = "USWB10003085",
            candDurationSec = 0,
            candStreamable = true,
        )
        assertEquals(0.95f, score, 0.0001f)
    }

    @Test fun `perfect title+artist+duration agreement scores 1_0`() {
        val score = QobuzCandidateMatcher.confidence(
            query = TrackQuery(
                artist = "John Frusciante",
                title = "Murderers",
                durationMs = 160_000,
            ),
            candTitle = "Murderers",
            candArtist = "John Frusciante",
            candIsrc = null,
            candDurationSec = 160,
            candStreamable = true,
        )
        // titleSim 1.0 * artistSim 1.0 * durationFactor 1.0
        assertEquals(1.0f, score, 0.0001f)
    }

    @Test fun `dramatic duration mismatch downweights to 0_3`() {
        val score = QobuzCandidateMatcher.confidence(
            query = TrackQuery(
                artist = "John Frusciante",
                title = "Murderers",
                durationMs = 160_000,
            ),
            candTitle = "Murderers",
            candArtist = "John Frusciante",
            candIsrc = null,
            candDurationSec = 200, // 25% drift → 0.3 factor
            candStreamable = true,
        )
        assertEquals(0.3f, score, 0.0001f)
    }

    @Test fun `non-streamable candidate scores 0`() {
        val score = QobuzCandidateMatcher.confidence(
            query = TrackQuery(
                artist = "John Frusciante",
                title = "Murderers",
                isrc = "USWB10003085",
                durationMs = 160_000,
            ),
            candTitle = "Murderers",
            candArtist = "John Frusciante",
            candIsrc = "USWB10003085",
            candDurationSec = 160,
            candStreamable = false,
        )
        assertEquals(0f, score, 0.0001f)
    }

    @Test fun `unknown query duration skips the duration penalty`() {
        val score = QobuzCandidateMatcher.confidence(
            query = TrackQuery(artist = "John Frusciante", title = "Murderers", durationMs = null),
            candTitle = "Murderers",
            candArtist = "John Frusciante",
            candIsrc = null,
            candDurationSec = 999,
            candStreamable = true,
        )
        // durationFactor forced to 1.0 → 1.0 * 1.0 * 1.0
        assertEquals(1.0f, score, 0.0001f)
    }

    @Test fun `MIN_CONFIDENCE threshold value is preserved`() {
        assertEquals(0.5f, QobuzCandidateMatcher.MIN_CONFIDENCE, 0.0001f)
    }
}
