@file:Suppress("ForbiddenImport")

package com.clibeats.data.cache

import android.content.Context
import com.clibeats.data.local.dao.CacheIndexDao
import com.clibeats.data.local.entity.CacheIndexEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val cacheIndexDao: CacheIndexDao,
    ) {
        private val cacheDir: File =
            File(context.cacheDir, "audio_cache").apply {
                if (!exists()) mkdirs()
            }

        var maxCacheSizeBytes: Long = DEFAULT_MAX_CACHE_BYTES

        suspend fun getCachedFile(songId: String): File? {
            val entry = cacheIndexDao.getById(songId)
            val cached = entry?.let { File(it.localPath) }?.takeIf { it.exists() && it.length() > 0 }
            if (entry != null && cached == null) {
                // The file was evicted or removed behind the index's back. Drop the row, otherwise its
                // bytes keep counting towards the size limit and eviction removes the wrong entries.
                cacheIndexDao.deleteById(songId)
            }
            return cached
        }

        private fun safeFileName(songId: String): String =
            java.util.UUID.nameUUIDFromBytes(songId.toByteArray(Charsets.UTF_8)).toString() + ".mp3"

        suspend fun saveTrackToCache(
            songId: String,
            inputStream: InputStream,
        ): File {
            val targetFile = File(cacheDir, safeFileName(songId))
            targetFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            val fileSize = targetFile.length()
            val entry =
                CacheIndexEntity(
                    songId = songId,
                    localPath = targetFile.absolutePath,
                    fileSizeBytes = fileSize,
                    cachedAt = System.currentTimeMillis(),
                    expiresAt = null,
                )
            cacheIndexDao.upsert(entry)
            evictLruIfNeeded()
            return targetFile
        }

        suspend fun evictLruIfNeeded() {
            var currentSize = cacheIndexDao.getTotalCacheSizeBytes() ?: 0L
            if (currentSize <= maxCacheSizeBytes) return

            val entries = cacheIndexDao.getAllAsFlow().first().sortedBy { it.cachedAt }
            for (entry in entries) {
                if (currentSize <= maxCacheSizeBytes) break
                val file = File(entry.localPath)
                if (file.exists()) {
                    file.delete()
                }
                cacheIndexDao.deleteById(entry.songId)
                currentSize -= entry.fileSizeBytes
            }
        }

        suspend fun clearAllCache() {
            cacheDir.listFiles()?.forEach { it.delete() }
            cacheIndexDao.deleteBefore(System.currentTimeMillis() + BUFFER_TIME_MS)
        }

        fun getAllCachedAsFlow(): Flow<List<CacheIndexEntity>> = cacheIndexDao.getAllAsFlow()

        companion object {
            private const val DEFAULT_MAX_CACHE_BYTES = 524_288_000L // 500 MB
            private const val BUFFER_TIME_MS = 1000L
        }
    }
