@file:Suppress("ForbiddenImport")

package com.clibeats.data.download

import com.clibeats.data.cache.CacheManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TrackDownloadManagerTest {
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var cacheManager: CacheManager
    private lateinit var downloadManager: TrackDownloadManager

    @Before
    fun setUp() {
        okHttpClient = mock()
        cacheManager = mock()
        downloadManager = TrackDownloadManager(okHttpClient, cacheManager)
    }

    @Test
    fun `isTrackDownloaded returns true when cacheManager has file`() =
        runTest {
            val fakeFile = java.io.File.createTempFile("test", ".mp3")
            whenever(cacheManager.getCachedFile("s1")).thenReturn(fakeFile)
            val result = downloadManager.isTrackDownloaded("s1")
            assertThat(result).isTrue()
            fakeFile.delete()
        }
}
