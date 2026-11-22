@file:Suppress("ForbiddenImport")

package com.clibeats.data.cache

import android.content.Context
import com.clibeats.data.local.dao.CacheIndexDao
import com.clibeats.data.local.entity.CacheIndexEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CacheManagerTest {
    private lateinit var context: Context
    private lateinit var cacheIndexDao: CacheIndexDao
    private lateinit var cacheManager: CacheManager

    @Before
    fun setUp() {
        context = mock()
        cacheIndexDao = mock()
        whenever(context.cacheDir).thenReturn(java.io.File(System.getProperty("java.io.tmpdir") ?: "."))
        whenever(cacheIndexDao.getAllAsFlow()).thenReturn(flowOf(emptyList()))
        cacheManager = CacheManager(context, cacheIndexDao)
    }

    @Test
    fun `getCachedFile returns null when song is not in index`() =
        runTest {
            whenever(cacheIndexDao.getById("song_999")).thenReturn(null)
            val file = cacheManager.getCachedFile("song_999")
            assertThat(file).isNull()
        }

    @Test
    fun `getCachedFile drops an index row whose file is gone`() =
        runTest {
            whenever(cacheIndexDao.getById("song_1")).thenReturn(
                CacheIndexEntity(
                    songId = "song_1",
                    localPath = "/nonexistent/song_1.mp3",
                    fileSizeBytes = 1_024L,
                    cachedAt = 0L,
                    expiresAt = null,
                ),
            )

            val file = cacheManager.getCachedFile("song_1")

            assertThat(file).isNull()
            verify(cacheIndexDao).deleteById("song_1")
        }

    @Test
    fun `maxCacheSizeBytes can be updated`() {
        cacheManager.maxCacheSizeBytes = 100 * 1024 * 1024L
        assertThat(cacheManager.maxCacheSizeBytes).isEqualTo(100 * 1024 * 1024L)
    }
}
