/*
 * OJ Music Player (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package com.ochenjoshua.ojmusicplayer.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.ochenjoshua.ojmusicplayer.constants.InnerTubeCookieKey
import com.ochenjoshua.ojmusicplayer.constants.YtmSyncKey
import com.ochenjoshua.ojmusicplayer.innertube.utils.hasYouTubeLoginCookie
import com.ochenjoshua.ojmusicplayer.utils.dataStore
import com.ochenjoshua.ojmusicplayer.utils.get

fun Context.isSyncEnabled(): Boolean = dataStore.get(YtmSyncKey, true) && isUserLoggedIn()

fun Context.isUserLoggedIn(): Boolean {
    val cookie = dataStore[InnerTubeCookieKey] ?: ""
    return hasYouTubeLoginCookie(cookie) && isInternetConnected()
}

fun Context.isInternetConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
}
