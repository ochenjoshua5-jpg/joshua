package com.ochenjoshua.ojmusicplayer.download

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.ochenjoshua.ojmusicplayer.db.MusicDatabase
import com.ochenjoshua.ojmusicplayer.playback.resolvers.LosslessStreamResolver

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FlacDownloaderEntryPoint {
    fun losslessStreamResolver(): LosslessStreamResolver

    fun database(): MusicDatabase
}
