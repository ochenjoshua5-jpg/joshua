package com.ochenjoshua.ojmusicplayer.download

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SafDirectoryManagerTest {

    private lateinit var context: Context
    private lateinit var safDirectoryManager: SafDirectoryManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        every { context.applicationContext } returns context
        every { context.filesDir } returns java.io.File("/tmp")
        safDirectoryManager = SafDirectoryManager(context)
        mockkStatic(DocumentFile::class)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `getOrCreateAlbumDirectory caches folder URIs`() = runBlocking {
        val baseTreeUri = mockk<Uri>()
        val rootDir = mockk<DocumentFile>()
        val artistDir = mockk<DocumentFile>()
        val albumDir = mockk<DocumentFile>()
        val artistUri = mockk<Uri>()
        val albumUri = mockk<Uri>()

        every { DocumentFile.fromTreeUri(context, baseTreeUri) } returns rootDir
        every { rootDir.canWrite() } returns true
        every { rootDir.uri } returns baseTreeUri
        
        every { rootDir.findFile("Artist") } returns null
        every { rootDir.createDirectory("Artist") } returns artistDir
        every { artistDir.uri } returns artistUri
        
        every { artistDir.findFile("Album") } returns null
        every { artistDir.createDirectory("Album") } returns albumDir
        every { albumDir.uri } returns albumUri

        // First call should create directories
        val result1 = safDirectoryManager.getOrCreateAlbumDirectory(baseTreeUri, "Artist", "Album")
        assertNotNull(result1)
        assertEquals(albumDir, result1)

        // Mock cache hit for artist and album
        every { DocumentFile.fromTreeUri(context, artistUri) } returns artistDir
        every { artistDir.exists() } returns true
        every { DocumentFile.fromTreeUri(context, albumUri) } returns albumDir
        every { albumDir.exists() } returns true

        // Second call should use cache
        val result2 = safDirectoryManager.getOrCreateAlbumDirectory(baseTreeUri, "Artist", "Album")
        assertNotNull(result2)
        assertEquals(albumDir, result2)
    }

    @Test
    fun `getRootDirectory handles SecurityException and clears preference`() = runBlocking {
        val baseTreeUri = mockk<Uri>()
        
        every { DocumentFile.fromTreeUri(context, baseTreeUri) } throws SecurityException("Permission denied")

        val result = safDirectoryManager.getOrCreateAlbumDirectory(baseTreeUri, "Artist", "Album")
        assertNull(result)
    }
}
