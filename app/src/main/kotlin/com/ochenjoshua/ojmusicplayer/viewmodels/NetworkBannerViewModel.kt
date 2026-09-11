/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import com.ochenjoshua.ojmusicplayer.network.NetworkBannerUiState
import com.ochenjoshua.ojmusicplayer.network.ObserveNetworkBannerStateUseCase
import javax.inject.Inject

@HiltViewModel
class NetworkBannerViewModel
    @Inject
    constructor(
        observeNetworkBannerStateUseCase: ObserveNetworkBannerStateUseCase,
    ) : ViewModel() {
        val bannerState: StateFlow<NetworkBannerUiState> =
            observeNetworkBannerStateUseCase()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = NetworkBannerUiState.Hidden,
                )
    }
