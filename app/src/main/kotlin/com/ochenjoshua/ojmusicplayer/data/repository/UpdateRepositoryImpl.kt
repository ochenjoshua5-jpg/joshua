package com.ochenjoshua.ojmusicplayer.data.repository

import com.ochenjoshua.ojmusicplayer.BuildConfig
import com.ochenjoshua.ojmusicplayer.constants.UpdateChannel
import com.ochenjoshua.ojmusicplayer.domain.repository.UpdateRepository
import com.ochenjoshua.ojmusicplayer.models.AppUpdateInfo
import com.ochenjoshua.ojmusicplayer.utils.MarkdownCleaner
import com.ochenjoshua.ojmusicplayer.utils.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepositoryImpl @Inject constructor() : UpdateRepository {

    override fun checkForUpdates(channel: UpdateChannel): Flow<AppUpdateInfo?> = flow {
        if (!BuildConfig.UPDATER_AVAILABLE) {
            emit(null)
            return@flow
        }

        val result = when (channel) {
            UpdateChannel.DAILY_NIGHTLY -> Updater.getLatestCanaryReleaseInfo()
            else -> Updater.getLatestReleaseInfo()
        }

        result.onSuccess { release ->
            val currentVersion = BuildConfig.VERSION_NAME
            if (Updater.isUpdateAvailable(release.tagName, currentVersion)) {
                val downloadUrl = when (channel) {
                    UpdateChannel.DAILY_NIGHTLY -> Updater.getLatestCanaryDownloadUrl()
                    else -> Updater.getLatestDownloadUrl()
                }

                val isCritical = release.body?.contains("[CRITICAL]", ignoreCase = true) == true

                emit(
                    AppUpdateInfo(
                        versionCode = 0,
                        versionName = release.tagName,
                        updateUrl = downloadUrl,
                        isCritical = isCritical,
                        changelog = MarkdownCleaner.clean(release.body),
                        imageUrl = release.imageUrl
                    )
                )
            } else {
                emit(null)
            }
        }.onFailure {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    override fun forceCheckForUpdates(channel: UpdateChannel): Flow<AppUpdateInfo?> = flow {
        if (!BuildConfig.UPDATER_AVAILABLE) {
            emit(null)
            return@flow
        }

        val result = when (channel) {
            UpdateChannel.DAILY_NIGHTLY -> Updater.getAllReleases(forceRefresh = true)
            else -> Updater.getAllReleases(forceRefresh = true)
        }

        result.onSuccess { releases ->
            val latest = when (channel) {
                UpdateChannel.DAILY_NIGHTLY -> Updater.findLatestCanaryRelease(releases)
                else -> Updater.findLatestRelease(releases)
            }

            if (latest != null) {
                val currentVersion = BuildConfig.VERSION_NAME
                if (Updater.isUpdateAvailable(latest.tagName, currentVersion)) {
                    val downloadUrl = when (channel) {
                        UpdateChannel.DAILY_NIGHTLY -> Updater.getLatestCanaryDownloadUrl()
                        else -> Updater.getLatestDownloadUrl()
                    }

                    val isCritical = latest.body?.contains("[CRITICAL]", ignoreCase = true) == true

                    emit(
                        AppUpdateInfo(
                            versionCode = 0,
                            versionName = latest.tagName,
                            updateUrl = downloadUrl,
                            isCritical = isCritical,
                            changelog = MarkdownCleaner.clean(latest.body),
                            imageUrl = latest.imageUrl
                        )
                    )
                } else {
                    emit(null)
                }
            } else {
                emit(null)
            }
        }.onFailure {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)
}