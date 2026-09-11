package com.ochenjoshua.ojmusicplayer.flaccore

import java.util.logging.Level
import java.util.logging.Logger

class FlacLogger(tag: String) {
    private val logger = Logger.getLogger(tag)

    fun d(message: String) {
        logger.log(Level.FINE, message)
    }

    fun i(message: String) {
        logger.log(Level.INFO, message)
    }

    fun w(message: String) {
        logger.log(Level.WARNING, message)
    }

    fun e(message: String) {
        logger.log(Level.SEVERE, message)
    }
}
