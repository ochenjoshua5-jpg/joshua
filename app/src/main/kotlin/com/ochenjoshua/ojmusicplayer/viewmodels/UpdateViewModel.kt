package com.ochenjoshua.ojmusicplayer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ochenjoshua.ojmusicplayer.constants.UpdateChannel
import com.ochenjoshua.ojmusicplayer.domain.repository.UpdateRepository
import com.ochenjoshua.ojmusicplayer.ui.state.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository
) : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.NoUpdate)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun checkUpdates(channel: UpdateChannel) {
        updateRepository.checkForUpdates(channel)
            .onEach { info ->
                _updateState.value = when {
                    info == null -> UpdateState.NoUpdate
                    info.isCritical -> UpdateState.CriticalUpdate(
                        versionName = info.versionName,
                        updateUrl = info.updateUrl,
                        changelog = info.changelog,
                        imageUrl = info.imageUrl
                    )
                    else -> UpdateState.SoftUpdate(
                        versionName = info.versionName,
                        updateUrl = info.updateUrl,
                        changelog = info.changelog,
                        imageUrl = info.imageUrl
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun forceCheck(channel: UpdateChannel) {
        updateRepository.forceCheckForUpdates(channel)
            .onEach { info ->
                _updateState.value = when {
                    info == null -> UpdateState.NoUpdate
                    info.isCritical -> UpdateState.CriticalUpdate(
                        versionName = info.versionName,
                        updateUrl = info.updateUrl,
                        changelog = info.changelog,
                        imageUrl = info.imageUrl
                    )
                    else -> UpdateState.SoftUpdate(
                        versionName = info.versionName,
                        updateUrl = info.updateUrl,
                        changelog = info.changelog,
                        imageUrl = info.imageUrl
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun dismissUpdate() {
        val currentState = _updateState.value
        _updateState.value = when (currentState) {
            is UpdateState.SoftUpdate -> currentState.copy(isOverlayDismissed = true)
            is UpdateState.CriticalUpdate -> currentState.copy(isOverlayDismissed = true)
            else -> currentState
        }
    }
}