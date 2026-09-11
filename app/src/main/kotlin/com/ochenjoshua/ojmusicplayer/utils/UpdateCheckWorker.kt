/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.ochenjoshua.ojmusicplayer.BuildConfig
import com.ochenjoshua.ojmusicplayer.constants.EnableUpdateNotificationKey
import com.ochenjoshua.ojmusicplayer.constants.UpdateChannel
import com.ochenjoshua.ojmusicplayer.constants.UpdateChannelKey
import com.ochenjoshua.ojmusicplayer.defaultUpdateChannel

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!BuildConfig.UPDATER_AVAILABLE) {
            return Result.success()
        }

        return try {
            val dataStore = applicationContext.dataStore

            val isEnabled = dataStore.data.map { it[EnableUpdateNotificationKey] ?: false }.first()
            if (!isEnabled) return Result.success()

            val updateChannel =
                dataStore.data
                    .map {
                        it[UpdateChannelKey]?.let { value ->
                            try {
                                UpdateChannel.valueOf(value)
                            } catch (_: IllegalArgumentException) {
                                defaultUpdateChannel
                            }
                        } ?: defaultUpdateChannel
                    }.first()

            when (updateChannel) {
                UpdateChannel.NIGHTLY -> {
                    return Result.success()
                }

                UpdateChannel.DAILY_NIGHTLY -> {
                    Updater.getLatestCanaryVersionName().onSuccess { latestVersion ->
                        if (Updater.isUpdateAvailable(latestVersion, BuildConfig.VERSION_NAME)) {
                            UpdateNotificationManager.notifyIfNewVersion(
                                applicationContext,
                                latestVersion,
                                updateChannel,
                            )
                        }
                    }
                }

                else -> {
                    Updater.getLatestVersionName().onSuccess { latestVersion ->
                        if (Updater.isUpdateAvailable(latestVersion, BuildConfig.VERSION_NAME)) {
                            UpdateNotificationManager.notifyIfNewVersion(applicationContext, latestVersion)
                        }
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
