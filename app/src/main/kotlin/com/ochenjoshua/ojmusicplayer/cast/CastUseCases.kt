/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.cast

class ObserveCastStateUseCase(
    private val repository: CastPlaybackRepository,
) {
    operator fun invoke() = repository.screenState
}

class SelectCastRouteUseCase

class DisconnectCastSessionUseCase(
    private val repository: CastPlaybackRepository,
) {
    operator fun invoke() = repository.disconnect()
}

class SetCastVolumeUseCase(
    private val repository: CastPlaybackRepository,
) {
    operator fun invoke(volume: Float) = repository.setVolume(volume)
}
