package com.ochenjoshua.ojmusicplayer.di

import com.ochenjoshua.ojmusicplayer.data.repository.AccountRepository
import com.ochenjoshua.ojmusicplayer.data.repository.AccountRepositoryImpl
import com.ochenjoshua.ojmusicplayer.data.repository.SettingsRepository
import com.ochenjoshua.ojmusicplayer.data.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        impl: AccountRepositoryImpl
    ): AccountRepository
}