package com.ochenjoshua.ojmusicplayer.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ochenjoshua.ojmusicplayer.constants.DownloadLocationUriKey
import com.ochenjoshua.ojmusicplayer.utils.dataStore
import com.ochenjoshua.ojmusicplayer.utils.getAsync
import java.io.File

object FlacDownloader {
    fun downloadFlac(
        context: Context,
        songId: String,
        title: String,
        artist: String,
        album: String,
    ) {
        val data =
            Data.Builder()
                .putString("songId", songId)
                .putString("title", title)
                .putString("artist", artist)
                .putString("album", album)
                .build()

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val request =
            OneTimeWorkRequestBuilder<FlacDownloadWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

        WorkManager.getInstance(context).enqueueUniqueWork("flac_download_$songId", ExistingWorkPolicy.KEEP, request)
    }

    fun deleteFlac(
        context: Context,
        songId: String,
        title: String,
        artist: String,
        album: String,
    ) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork("flac_download_$songId")
        workManager.pruneWork()

        CoroutineScope(Dispatchers.IO).launch {
            val treeUriString = context.dataStore.getAsync(DownloadLocationUriKey, "")
            var deleted = false
            var targetPath = ""
            if (treeUriString.isNotEmpty()) {
                val treeUri = Uri.parse(treeUriString)
                val safDirectoryManager = SafDirectoryManager(context)
                val albumDir = safDirectoryManager.getOrCreateAlbumDirectory(treeUri, artist, album)
                if (albumDir != null) {
                    val fileName = "${sanitizeFileName(title)}.flac"
                    val existingFile = albumDir.findFile(fileName)
                    if (existingFile != null && existingFile.exists()) {
                        targetPath = existingFile.uri.path ?: fileName
                        deleted = existingFile.delete()
                    }
                }
                safDirectoryManager.clearCache()
            } else {
                val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val yumaDir = File(musicDir, "OJ Music Player")
                val artistDir = File(yumaDir, sanitizeFileName(artist))
                val albumDir = File(artistDir, sanitizeFileName(album))
                val file = File(albumDir, "${sanitizeFileName(title)}.flac")
                if (file.exists()) {
                    targetPath = file.absolutePath
                    deleted = file.delete()
                }
            }
            if (deleted) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(com.ochenjoshua.ojmusicplayer.R.string.flac_download_deleted, targetPath), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
