package com.ochenjoshua.ojmusicplayer.lossless

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ochenjoshua.ojmusicplayer.flaccore.FlacKvStore
import com.ochenjoshua.ojmusicplayer.utils.dataStore
import com.ochenjoshua.ojmusicplayer.utils.getAsync

class FlacKvStoreImpl(private val context: Context) : FlacKvStore {
    override suspend fun get(key: String): String? {
        val prefKey = stringPreferencesKey("flac_kv_$key")
        return context.dataStore.getAsync(prefKey)
    }

    override suspend fun put(key: String, value: String?) {
        val prefKey = stringPreferencesKey("flac_kv_$key")
        context.dataStore.edit { prefs ->
            if (value == null) {
                prefs.remove(prefKey)
            } else {
                prefs[prefKey] = value
            }
        }
    }
}
