package com.ochenjoshua.ojmusicplayer.ui.state

sealed interface PlayerEvent {
    data class ShareTrack(val url: String) : PlayerEvent
    data class Navigate(val route: String) : PlayerEvent
}