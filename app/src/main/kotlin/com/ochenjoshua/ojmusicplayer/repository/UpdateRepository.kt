package com.ochenjoshua.ojmusicplayer.domain.repository

import com.ochenjoshua.ojmusicplayer.constants.UpdateChannel
import com.ochenjoshua.ojmusicplayer.models.AppUpdateInfo
import kotlinx.coroutines.flow.Flow

interface UpdateRepository {
    fun checkForUpdates(channel: UpdateChannel): Flow<AppUpdateInfo?>
    fun forceCheckForUpdates(channel: UpdateChannel): Flow<AppUpdateInfo?>
}