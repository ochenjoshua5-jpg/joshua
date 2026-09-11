/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.ui.screens.settings.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.ochenjoshua.ojmusicplayer.data.repository.AccountRepository
import com.ochenjoshua.ojmusicplayer.utils.ColorExtractor
import com.ochenjoshua.ojmusicplayer.utils.SavedAccountCollection
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        observeAccountInfo()
    }

    private fun observeAccountInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            accountRepository.getAccountInfo().collect { info ->
                _uiState.update { current ->
                    val status = if (info.isLoggedIn) {
                        AccountStatus.LoggedIn(
                            name = info.name,
                            email = info.email,
                            handle = info.handle,
                            avatarUrl = info.avatarUrl,
                            hasMultipleAccounts = info.savedAccounts.accounts.size > 1
                        )
                    } else {
                        AccountStatus.LoggedOut
                    }
                    current.copy(status = status)
                }
            }
        }
    }

    /**
     * Processes avatar ARGB pixel array asynchronously on Dispatchers.IO.
     */
    fun processAvatarPixels(pixels: IntArray) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isExtractingColor = true) }
            val hex = ColorExtractor.extractVibrantHex(pixels)
            _uiState.update { it.copy(extractedColorHex = hex, isExtractingColor = false) }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            accountRepository.logout()
        }
    }
}
