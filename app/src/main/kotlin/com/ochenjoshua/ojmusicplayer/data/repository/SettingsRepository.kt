package com.ochenjoshua.ojmusicplayer.data.repository

import kotlinx.coroutines.flow.Flow
import com.ochenjoshua.ojmusicplayer.constants.AppFontPreference
import com.ochenjoshua.ojmusicplayer.lyrics.LyricsRomanizationPreferences

data class UserSettings(
    val isDarkTheme: Boolean = true,
    val themeColorHex: String = "ED5564",
    val fontPreference: AppFontPreference = AppFontPreference.DEFAULT,
)

interface SettingsRepository {
    val userSettings: Flow<UserSettings>
    val lyricsRomanizationPrefsFlow: Flow<LyricsRomanizationPreferences>
    suspend fun updateThemeColor(colorHex: String): Result<Unit>
    suspend fun updateFontPreference(preference: AppFontPreference): Result<Unit>
    fun isBlurBackgroundEnabled(): Boolean
    fun setBlurBackgroundEnabled(enabled: Boolean)
    fun isAutoDownloadLyricsEnabled(): Boolean
    fun setAutoDownloadLyricsEnabled(enabled: Boolean)
    fun isFirstLaunch(): Boolean
    fun setFirstLaunch(isFirst: Boolean)
    fun isSearchHistoryPaused(): Boolean
    fun isImmersiveEnabled(): Boolean
    fun setImmersiveEnabled(enabled: Boolean)
    fun isShowCodecInfoEnabled(): Boolean
    fun setShowCodecInfoEnabled(enabled: Boolean)
    fun isAlbumCoverGlowEnabled(): Boolean
    fun setAlbumCoverGlowEnabled(enabled: Boolean)
}