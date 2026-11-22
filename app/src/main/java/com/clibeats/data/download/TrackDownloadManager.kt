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
import java.io.OutputStream
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
            val partFile = File(downloadsDir, "$hash.part")

            activeJobs[track.id]?.cancel()

            val job =
                scope.launch {
                    try {
                        existingDownload(track.id)?.let { existing ->
                            updateStatus(track.id, DownloadStatus.Completed(existing))
                            return@launch
                        }

                        updateStatus(track.id, DownloadStatus.Downloading(0))

                        val streamUrl =
                            track.streamUrl?.takeIf { !it.contains("expire") }
                                ?: runCatching { streamResolver.get().resolve(track).streamUrl }.getOrNull()

                        if (streamUrl.isNullOrBlank()) {
                            updateStatus(track.id, DownloadStatus.Failed("No stream URL available"))
                            return@launch
                        }

                        val request = Request.Builder().url(streamUrl).build()
                        // Close the response on every path: an abandoned body keeps its socket out of the pool.
                        okHttpClient.newCall(request).execute().use { response ->
                            val body = response.body
                            if (!response.isSuccessful || body == null) {
                                updateStatus(track.id, DownloadStatus.Failed("HTTP ${response.code}"))
                                return@use
                            }

                            writeBody(track.id, body.byteStream(), body.contentLength(), partFile)

                            // Name the file after the container the provider actually served.
                            val format = DownloadFormat.from(response.header("Content-Type"), streamUrl)
                            val finalFile = File(downloadsDir, "$hash.${format.extension}")
                            if (finalFile.exists()) finalFile.delete()

                            if (!partFile.renameTo(finalFile)) {
                                partFile.delete()
                                updateStatus(track.id, DownloadStatus.Failed("Failed to finalize download file"))
                                return@use
                            }

                            songDao.upsert(track.toEntity())
                            downloadedTrackDao.insert(
                                DownloadedTrackEntity(
                                    songId = track.id,
                                    filePath = finalFile.absolutePath,
                                    sizeBytes = finalFile.length(),
                                    downloadedAt = System.currentTimeMillis(),
                                    mimeType = format.mimeType,
                                ),
                            )
                            updateStatus(track.id, DownloadStatus.Completed(finalFile))
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

        /** An earlier download whose file is still on disk; a record pointing at a missing file is cleared. */
        private suspend fun existingDownload(songId: String): File? {
            val record = downloadedTrackDao.getById(songId)
            val file = record?.let { File(it.filePath) }
            return if (file?.exists() == true) {
                file
            } else {
                if (record != null) downloadedTrackDao.deleteById(songId)
                null
            }
        }

        /** Streams [input] into [target] and publishes progress once per whole percent. */
        private fun writeBody(
            songId: String,
            input: InputStream,
            contentLength: Long,
            target: File,
        ) {
            input.use { inputStream ->
                FileOutputStream(target).use { outputStream ->
                    copyWithProgress(songId, inputStream, outputStream, contentLength)
                }
            }
        }

        private fun copyWithProgress(
            songId: String,
            inputStream: InputStream,
            outputStream: OutputStream,
            contentLength: Long,
        ) {
            val buffer = ByteArray(BUFFER_SIZE)
            var totalBytesRead = 0L
            var lastProgress = -1

            while (true) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                if (contentLength > 0) {
                    val progress = ((totalBytesRead * PERCENT) / contentLength).toInt()
                    if (progress != lastProgress) {
                        lastProgress = progress
                        updateStatus(songId, DownloadStatus.Downloading(progress))
                    }
                }
            }
        }

        /**
         * Maps what the server actually sends to a file extension and MIME type, so an Opus/WebM or MP3
         * stream isn't stored as `.m4a` (which misleads other apps and the system media scanner).
         */
        private data class DownloadFormat(
            val extension: String,
            val mimeType: String,
        ) {
            companion object {
                private val MIME_FORMATS =
                    mapOf(
                        "audio/mp4" to DownloadFormat("m4a", "audio/mp4"),
                        "audio/m4a" to DownloadFormat("m4a", "audio/mp4"),
                        "audio/x-m4a" to DownloadFormat("m4a", "audio/mp4"),
                        "audio/aac" to DownloadFormat("m4a", "audio/mp4"),
                        "audio/webm" to DownloadFormat("webm", "audio/webm"),
                        "audio/ogg" to DownloadFormat("ogg", "audio/ogg"),
                        "application/ogg" to DownloadFormat("ogg", "audio/ogg"),
                        "audio/opus" to DownloadFormat("opus", "audio/opus"),
                        "audio/mpeg" to DownloadFormat("mp3", "audio/mpeg"),
                        "audio/mp3" to DownloadFormat("mp3", "audio/mpeg"),
                        "audio/flac" to DownloadFormat("flac", "audio/flac"),
                        "audio/x-flac" to DownloadFormat("flac", "audio/flac"),
                        "audio/wav" to DownloadFormat("wav", "audio/wav"),
                        "audio/x-wav" to DownloadFormat("wav", "audio/wav"),
                        "audio/wave" to DownloadFormat("wav", "audio/wav"),
                    )

                private val EXTENSION_FORMATS =
                    MIME_FORMATS.values.associateBy { it.extension } +
                        mapOf("mp4" to DownloadFormat("m4a", "audio/mp4"))

                private val DEFAULT = DownloadFormat("m4a", "audio/mp4")

                fun from(
                    contentType: String?,
                    url: String,
                ): DownloadFormat {
                    val mime = contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
                    MIME_FORMATS[mime]?.let { return it }
                    val extension = url.substringBefore('?').substringAfterLast('.', "").lowercase()
                    return EXTENSION_FORMATS[extension] ?: DEFAULT
                }
            }
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
            const val PERCENT = 100
        }
    }
