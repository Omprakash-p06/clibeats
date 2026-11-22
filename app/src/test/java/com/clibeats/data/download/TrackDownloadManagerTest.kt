@file:Suppress("ForbiddenImport")

package com.clibeats.data.download

import android.content.Context
import com.clibeats.data.local.dao.DownloadedTrackDao
import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.local.entity.DownloadedTrackEntity
import com.clibeats.playback.StreamResolver
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

class TrackDownloadManagerTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context: Context = mock()
    private val okHttpClient: OkHttpClient = mock()
    private val streamResolver: dagger.Lazy<StreamResolver> = mock()
    private val downloadedTrackDao: DownloadedTrackDao = mock()
    private val songDao: SongDao = mock()

    private lateinit var manager: TrackDownloadManager
    private lateinit var filesDir: File

    @Before
    fun setUp() {
        filesDir = tempFolder.newFolder("files")
        whenever(context.filesDir).thenReturn(filesDir)

        manager =
            TrackDownloadManager(
                context = context,
                okHttpClient = okHttpClient,
                streamResolver = streamResolver,
                downloadedTrackDao = downloadedTrackDao,
                songDao = songDao,
            )
    }

    @Test
    fun `isTrackDownloaded returns true when entity exists and file exists on disk`() =
        runTest {
            val audioFile =
                File(filesDir, "downloads/test.m4a").apply {
                    parentFile?.mkdirs()
                    writeBytes(byteArrayOf(1, 2, 3))
                }
            whenever(downloadedTrackDao.getById("song1")).thenReturn(
                DownloadedTrackEntity(
                    songId = "song1",
                    filePath = audioFile.absolutePath,
                    sizeBytes = 3L,
                    downloadedAt = System.currentTimeMillis(),
                    mimeType = "audio/mp4",
                ),
            )

            assertThat(manager.isTrackDownloaded("song1")).isTrue()
        }

    @Test
    fun `isTrackDownloaded returns false when file does not exist on disk`() =
        runTest {
            whenever(downloadedTrackDao.getById("song2")).thenReturn(
                DownloadedTrackEntity(
                    songId = "song2",
                    filePath = "/non/existent/file.m4a",
                    sizeBytes = 0L,
                    downloadedAt = System.currentTimeMillis(),
                    mimeType = "audio/mp4",
                ),
            )

            assertThat(manager.isTrackDownloaded("song2")).isFalse()
        }
}
