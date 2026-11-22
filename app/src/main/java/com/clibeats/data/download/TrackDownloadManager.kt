// ForbiddenImport: data-layer self-imports are legitimate
@file:Suppress("ForbiddenImport", "TooGenericExceptionCaught", "LongParameterList")

package com.clibeats.data.download

import android.content.Context
import com.clibeats.data.local.dao.DownloadedTrackDao
import com.clibeats.data.local.dao.SongDao
import com.clibeats.data.local.entity.DownloadedTrackEntity
import com.clibeats.data.local.mapper.toEntity
import com.clibeats.domain.model.Track
import com.clibeats.playback.StreamResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackDownloadManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val okHttpClient: OkHttpClient,
        private val streamResolver: dagger.Lazy<StreamResolver>,
        private val downloadedTrackDao: DownloadedTrackDao,
        private val songDao: SongDao,
    ) {
        private val scope = CoroutineScope(Dispatchers.IO)
        private val _downloads = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
        val downloads: StateFlow<Map<String, DownloadStatus>> = _downloads.asStateFlow()

        private val activeJobs = ConcurrentHashMap<String, Job>()

        private val downloadsDir: File by lazy {
            File(context.filesDir, "downloads").apply {
                if (!exists()) mkdirs()
            }
        }

        suspend fun isTrackDownloaded(songId: String): Boolean =
            withContext(Dispatchers.IO) {
                val record = downloadedTrackDao.getById(songId) ?: return@withContext false
                File(record.filePath).exists()
            }

        fun getDownloadedTracks(): Flow<List<DownloadedTrackEntity>> = downloadedTrackDao.getAllFlow()

        @Suppress("LongMethod", "CyclomaticComplexMethod")
        fun downloadTrack(track: Track) {
            val hash = sha256(track.id)
            val finalFile = File(downloadsDir, "$hash.m4a")
            val partFile = File(downloadsDir, "$hash.part")

            if (finalFile.exists()) {
                updateStatus(track.id, DownloadStatus.Completed(finalFile))
                return
            }

            activeJobs[track.id]?.cancel()

            val job =
                scope.launch {
                    try {
                        updateStatus(track.id, DownloadStatus.Downloading(0))

                        val streamUrl =
                            track.streamUrl?.takeIf { !it.contains("expire") }
                                ?: runCatching { streamResolver.get().resolve(track).streamUrl }.getOrNull()

                        if (streamUrl.isNullOrBlank()) {
                            updateStatus(track.id, DownloadStatus.Failed("No stream URL available"))
                            return@launch
                        }

                        val request = Request.Builder().url(streamUrl).build()
                        val response = okHttpClient.newCall(request).execute()

                        if (!response.isSuccessful || response.body == null) {
                            updateStatus(track.id, DownloadStatus.Failed("HTTP ${response.code}"))
                            return@launch
                        }

                        val body = response.body!!
                        val contentLength = body.contentLength()
                        val inputStream: InputStream = body.byteStream()
                        val outputStream = FileOutputStream(partFile)

                        outputStream.use { out ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var bytesRead: Int
                            var totalBytesRead = 0L
                            var lastProgress = 0

                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                out.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead

                                if (contentLength > 0) {
                                    val progress = ((totalBytesRead * 100) / contentLength).toInt()
                                    if (progress != lastProgress) {
                                        lastProgress = progress
                                        updateStatus(track.id, DownloadStatus.Downloading(progress))
                                    }
                                }
                            }
                        }

                        if (partFile.renameTo(finalFile)) {
                            songDao.upsert(track.toEntity())
                            downloadedTrackDao.insert(
                                DownloadedTrackEntity(
                                    songId = track.id,
                                    filePath = finalFile.absolutePath,
                                    sizeBytes = finalFile.length(),
                                    downloadedAt = System.currentTimeMillis(),
                                    mimeType = "audio/mp4",
                                ),
                            )
                            updateStatus(track.id, DownloadStatus.Completed(finalFile))
                        } else {
                            partFile.delete()
                            updateStatus(track.id, DownloadStatus.Failed("Failed to finalize download file"))
                        }
                    } catch (e: CancellationException) {
                        if (partFile.exists()) partFile.delete()
                        updateStatus(track.id, DownloadStatus.Idle)
                        throw e
                    } catch (e: Exception) {
                        if (partFile.exists()) partFile.delete()
                        updateStatus(track.id, DownloadStatus.Failed(e.message ?: "Download failed"))
                    } finally {
                        activeJobs.remove(track.id)
                    }
                }

            activeJobs[track.id] = job
        }

        fun cancelDownload(trackId: String) {
            activeJobs.remove(trackId)?.cancel()
            val hash = sha256(trackId)
            val partFile = File(downloadsDir, "$hash.part")
            if (partFile.exists()) partFile.delete()
            updateStatus(trackId, DownloadStatus.Idle)
        }

        suspend fun deleteDownload(trackId: String) {
            withContext(Dispatchers.IO) {
                cancelDownload(trackId)
                val entity = downloadedTrackDao.getById(trackId)
                if (entity != null) {
                    val file = File(entity.filePath)
                    if (file.exists()) file.delete()
                    downloadedTrackDao.deleteById(trackId)
                }
                updateStatus(trackId, DownloadStatus.Idle)
            }
        }

        private fun updateStatus(
            songId: String,
            status: DownloadStatus,
        ) {
            _downloads.value = _downloads.value + (songId to status)
        }

        private fun sha256(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }

        private companion object {
            const val BUFFER_SIZE = 8192
        }
    }
