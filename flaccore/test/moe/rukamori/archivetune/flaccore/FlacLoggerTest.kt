package com.ochenjoshua.ojmusicplayer.flaccore

import org.junit.Test

class FlacLoggerTest {

    @Test
    fun testLoggerMethods() {
        val logger = FlacLogger("TestTag")
        logger.d("Debug message")
        logger.i("Info message")
        logger.w("Warning message")
        logger.e("Error message")
    }
}
