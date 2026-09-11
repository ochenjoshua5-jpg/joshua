package com.ochenjoshua.ojmusicplayer.models

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val updateUrl: String,
    val isCritical: Boolean,
    val changelog: String = "",
    val imageUrl: String? = null
)