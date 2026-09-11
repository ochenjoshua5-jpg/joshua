package com.ochenjoshua.ojmusicplayer.ui.screens.settings

import com.ochenjoshua.ojmusicplayer.constants.AppFontPreference

sealed interface SettingsUiEvent {
    data class SelectAccentColor(val colorHex: String) : SettingsUiEvent
    data class SelectFont(val preference: AppFontPreference) : SettingsUiEvent
    object DismissError : SettingsUiEvent
}
