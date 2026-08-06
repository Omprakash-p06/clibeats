package com.clibeats.data.download

import com.clibeats.data.cache.CacheManager
import com.clibeats.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackDownloadManager
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
        private val cacheManager: CacheManager,
    ) {
        private val scope = CoroutineScope(Dispatchers.IO)
        private val _downloads = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
        val downloads: StateFlow<Map<String, DownloadStatus>> = _downloads.asStateFlow()

        suspend fun isTrackDownloaded(songId: String): Boolean = cacheManager.getCachedFile(songId) != null

        fun downloadTrack(track: Track) {
            val streamUrl = track.streamUrl ?: run {
                updateStatus(track.id, DownloadStatus.Failed("No stream URL available"))
                return
            }

            scope.launch {
                try {
                    updateStatus(track.id, DownloadStatus.Downloading(0))
                    val request = Request.Builder().url(streamUrl).build()
                    val response = okHttpClient.newCall(request).execute()

                    if (!response.isSuccessful || response.body == null) {
                        updateStatus(track.id, DownloadStatus.Failed("HTTP ${response.code}"))
                        return@launch
                    }

                    val inputStream: InputStream = response.body!!.byteStream()
                    val savedFile = cacheManager.saveTrackToCache(track.id, inputStream)
                    updateStatus(track.id, DownloadStatus.Completed(savedFile))
                } catch (e: Exception) {
                    updateStatus(track.id, DownloadStatus.Failed(e.message ?: "Download failed"))
                }
            }
        }

        private fun updateStatus(
            songId: String,
            status: DownloadStatus,
        ) {
            _downloads.value = _downloads.value.toMutableMap().apply {
                put(songId, status)
            }
        }
    }
