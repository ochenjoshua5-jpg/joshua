package com.ochenjoshua.ojmusicplayer.download

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ochenjoshua.ojmusicplayer.constants.DownloadLocationUriKey
import com.ochenjoshua.ojmusicplayer.utils.dataStore

private val FORBIDDEN_FILE_NAME_CHARS = Regex("[\\\\/:*?\"<>|]")
private val WHITESPACE_RUNS = Regex("\\s+")
private const val MAX_FILE_NAME_LENGTH = 200
private const val FALLBACK_FILE_NAME = "untitled"

fun sanitizeFileName(name: String): String {
    val sanitized = name
        .replace(FORBIDDEN_FILE_NAME_CHARS, "_")
        .trim()
        .replace(WHITESPACE_RUNS, " ")
    if (sanitized.isEmpty()) return FALLBACK_FILE_NAME
    return if (sanitized.length > MAX_FILE_NAME_LENGTH) sanitized.take(MAX_FILE_NAME_LENGTH) else sanitized
}

class SafDirectoryManager(private val context: Context) {
    // Cache for created folder URIs during a download session (Constraint C2)
    private val folderCache = HashMap<String, Uri>()

    /**
     * Gets or creates a directory hierarchy: [PickedFolder]/[Artist]/[Album]
     * Returns the DocumentFile for the Album directory, or null if it fails.
     */
    suspend fun getOrCreateAlbumDirectory(
        baseTreeUri: Uri,
        artist: String,
        album: String
    ): DocumentFile? = withContext(Dispatchers.IO) {
        val rootDir = getRootDirectory(baseTreeUri) ?: return@withContext null

        val artistDir = getOrCreateDirectory(rootDir, sanitizeFileName(artist)) ?: return@withContext null
        val albumDir = getOrCreateDirectory(artistDir, sanitizeFileName(album))

        albumDir
    }

    /**
     * Gets the root directory from the tree URI.
     * Handles SecurityException and clears the saved preference if access is revoked (Constraint C6).
     */
    private suspend fun getRootDirectory(treeUri: Uri): DocumentFile? {
        return runCatching {
            DocumentFile.fromTreeUri(context, treeUri)?.takeIf { it.canWrite() }
        }.onFailure {
            // Clear the saved preference if we get a SecurityException or other error
            context.dataStore.edit { prefs ->
                prefs.remove(DownloadLocationUriKey)
            }
            // In a real app, we would also post an error state to the UI here
        }.getOrNull()
    }

    /**
     * Gets or creates a subdirectory within a parent directory, using the cache to avoid IPC lags.
     */
    private fun getOrCreateDirectory(parentDir: DocumentFile, dirName: String): DocumentFile? {
        val cacheKey = "${parentDir.uri}/$dirName"
        
        // Check cache first
        folderCache[cacheKey]?.let { cachedUri ->
            val cachedDir = DocumentFile.fromTreeUri(context, cachedUri)
            if (cachedDir != null && cachedDir.exists()) {
                return cachedDir
            }
        }

        // Find existing or create new
        val dir = parentDir.findFile(dirName) ?: parentDir.createDirectory(dirName)
        
        // Update cache
        dir?.uri?.let { uri ->
            folderCache[cacheKey] = uri
        }
        
        return dir
    }

    /**
     * Clears the folder cache. Should be called when a download session ends.
     */
    fun clearCache() {
        folderCache.clear()
    }
}
