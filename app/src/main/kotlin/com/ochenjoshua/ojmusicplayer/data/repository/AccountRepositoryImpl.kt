package com.ochenjoshua.ojmusicplayer.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.ochenjoshua.ojmusicplayer.constants.AccountChannelHandleKey
import com.ochenjoshua.ojmusicplayer.constants.AccountEmailKey
import com.ochenjoshua.ojmusicplayer.constants.AccountNameKey
import com.ochenjoshua.ojmusicplayer.constants.DataSyncIdKey
import com.ochenjoshua.ojmusicplayer.constants.InnerTubeCookieKey
import com.ochenjoshua.ojmusicplayer.constants.SavedAccountsKey
import com.ochenjoshua.ojmusicplayer.innertube.utils.hasYouTubeLoginCookie
import com.ochenjoshua.ojmusicplayer.utils.PreferenceStore
import com.ochenjoshua.ojmusicplayer.utils.SavedAccountCollection
import com.ochenjoshua.ojmusicplayer.utils.clearPlaybackAuthSession
import com.ochenjoshua.ojmusicplayer.utils.dataStore
import com.ochenjoshua.ojmusicplayer.utils.decodeSavedAccounts
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AccountRepository {
    private val dataStore: DataStore<Preferences> = context.dataStore

    override fun getAccountInfo(): Flow<UserAccountInfo> {
        return dataStore.data.map { preferences ->
            val cookie = preferences[InnerTubeCookieKey].orEmpty()
            val isLoggedIn = hasYouTubeLoginCookie(cookie)
            val savedJson = preferences[SavedAccountsKey].orEmpty()
            val savedAccounts = SavedAccountCollection(decodeSavedAccounts(savedJson))

            UserAccountInfo(
                name = preferences[AccountNameKey].orEmpty(),
                email = preferences[AccountEmailKey].orEmpty(),
                handle = preferences[AccountChannelHandleKey].orEmpty(),
                avatarUrl = null,
                savedAccounts = savedAccounts,
                activeInnerTubeCookie = cookie,
                activeDataSyncId = preferences[DataSyncIdKey].orEmpty(),
                isLoggedIn = isLoggedIn
            )
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            PreferenceStore.launchEdit(context.dataStore) {
                clearPlaybackAuthSession(clearAccountIdentity = true)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
