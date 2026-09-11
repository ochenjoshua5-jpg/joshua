package com.ochenjoshua.ojmusicplayer.lossless

import android.content.Context
import com.ochenjoshua.ojmusicplayer.constants.EnableLosslessKey
import com.ochenjoshua.ojmusicplayer.constants.QobuzAppIdKey
import com.ochenjoshua.ojmusicplayer.constants.QobuzAppSecretKey
import com.ochenjoshua.ojmusicplayer.constants.QobuzUserAuthTokenKey
import com.ochenjoshua.ojmusicplayer.flaccore.FlacConfig
import com.ochenjoshua.ojmusicplayer.utils.dataStore
import com.ochenjoshua.ojmusicplayer.utils.getAsync

class FlacConfigImpl(private val context: Context) : FlacConfig {
    override suspend fun qbdlxEnabled(): Boolean {
        return context.dataStore.getAsync(EnableLosslessKey, true)
    }

    override suspend fun qbdlxAppId(): String {
        return context.dataStore.getAsync(QobuzAppIdKey, "")
    }

    override suspend fun qbdlxAppSecret(): String {
        return context.dataStore.getAsync(QobuzAppSecretKey, "")
    }

    override suspend fun qbdlxTokenPool(): String {
        return context.dataStore.getAsync(QobuzUserAuthTokenKey, "")
    }
}
