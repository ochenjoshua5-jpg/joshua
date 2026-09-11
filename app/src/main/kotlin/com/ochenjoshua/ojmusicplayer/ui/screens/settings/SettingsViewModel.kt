package com.ochenjoshua.ojmusicplayer.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.ochenjoshua.ojmusicplayer.data.repository.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.userSettings
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
                }
                .collect { settings ->
                    _uiState.update { settings.toUiState().copy(isLoading = false) }
                }
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsUiEvent.SelectAccentColor -> {
                    repository.updateThemeColor(event.colorHex)
                        .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                }
                is SettingsUiEvent.SelectFont -> {
                    repository.updateFontPreference(event.preference)
                        .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                }
                SettingsUiEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }
}
