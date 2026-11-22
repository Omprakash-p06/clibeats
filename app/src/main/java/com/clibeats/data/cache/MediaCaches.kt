@file:Suppress("ForbiddenImport")
@file:OptIn(UnstableApi::class)

package com.clibeats.data.cache

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.ContentMetadataMutations
import androidx.media3.datasource.cache.SimpleCache
import coil.ImageLoader
import com.clibeats.data.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.TreeSet
import javax.inject.Inject
import javax.inject.Singleton

data class CacheUsage(
    val songBytes: Long = 0L,
    val artworkBytes: Long = 0L,
)

/**
 * Two independent caches with user-set limits:
 * - songs: Media3 [SimpleCache] used by ExoPlayer for streamed audio (keyed per track, see PlaybackModule)
 * - artwork: Coil's disk cache ([ResizableDiskCache], limit applies immediately)
 */
@Singleton
class MediaCaches
    @Inject
    constructor(
        @ApplicationContext context: Context,
        appPreferences: AppPreferences,
        private val imageLoader: dagger.Lazy<ImageLoader>,
        private val artworkDiskCache: dagger.Lazy<ResizableDiskCache>,
    ) {
        private val evictor =
            AdjustableLruEvictor(DEFAULT_SONG_CACHE_MB * BYTES_IN_MB)

        // Opening the cache scans its directory and database: defer that off the startup path.
        val songCache: SimpleCache by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            SimpleCache(File(context.cacheDir, "song_cache"), evictor, StandaloneDatabaseProvider(context))
        }

        private val _usage = MutableStateFlow(CacheUsage())
        val usage: StateFlow<CacheUsage> = _usage.asStateFlow()

        private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        init {
            cacheScope.launch {
                launch {
                    appPreferences.cacheMaxMb.collect { mb ->
                        setSongLimitMb(mb)
                    }
                }
                launch {
                    appPreferences.artworkCacheMb.collect { mb ->
                        setArtworkLimitMb(mb)
                    }
                }
            }
        }

        /** Stores a stream's bitrate with its cached audio, so it's known on cached replays too. */
        fun rememberBitrate(
            cacheKey: String,
            kbps: Int,
        ) {
            runCatching {
                val mutations = ContentMetadataMutations().set(KEY_KBPS, kbps.toLong())
                songCache.applyContentMetadataMutations(cacheKey, mutations)
            }
        }

        /**
         * Bitrate for a cached song: the stored value, else estimated from the cached file's length
         * and the track duration (covers songs cached before bitrates were stored).
         */
        fun cachedBitrateKbps(
            cacheKey: String,
            durationMs: Long,
        ): Int? {
            val metadata = runCatching { songCache.getContentMetadata(cacheKey) }.getOrNull()
            val stored = metadata?.get(KEY_KBPS, -1L)?.takeIf { it > 0 }?.toInt()
            val bytes = metadata?.let(ContentMetadata::getContentLength) ?: -1L
            val estimated = if (bytes > 0 && durationMs > 0) (bytes * BITS_PER_BYTE / durationMs).toInt() else null
            return stored ?: estimated
        }

        suspend fun refreshUsage() =
            withContext(Dispatchers.IO) {
                _usage.value =
                    CacheUsage(
                        songBytes = songCache.cacheSpace,
                        artworkBytes = imageLoader.get().diskCache?.size ?: 0L,
                    )
            }

        suspend fun setSongLimitMb(mb: Int) =
            withContext(Dispatchers.IO) {
                evictor.maxBytes = mb.toLong() * BYTES_IN_MB
                evictor.trim(songCache)
                refreshUsage()
            }

        suspend fun setArtworkLimitMb(mb: Int) =
            withContext(Dispatchers.IO) {
                artworkDiskCache.get().resize(mb.toLong() * BYTES_IN_MB)
                // The trim runs on Coil's background thread; give it a moment before re-measuring.
                delay(TRIM_SETTLE_MS)
                refreshUsage()
            }

        suspend fun clearSongs() =
            withContext(Dispatchers.IO) {
                songCache.keys.toList().forEach { runCatching { songCache.removeResource(it) } }
                refreshUsage()
            }

        suspend fun clearArtwork() =
            withContext(Dispatchers.IO) {
                imageLoader.get().diskCache?.clear()
                imageLoader.get().memoryCache?.clear()
                refreshUsage()
            }

        companion object {
            const val BYTES_IN_MB = 1_048_576L
            private const val DEFAULT_SONG_CACHE_MB = 512L
            private const val KEY_KBPS = "clibeats_kbps"
            private const val BITS_PER_BYTE = 8
            private const val TRIM_SETTLE_MS = 500L
        }
    }

/** LRU evictor like Media3's, but the limit can change at runtime. */
internal class AdjustableLruEvictor(
    @Volatile var maxBytes: Long,
) : CacheEvictor {
    private val spans = TreeSet<CacheSpan>(compareBy<CacheSpan>({ it.lastTouchTimestamp }, { it.key }, { it.position }))
    private var currentSize = 0L

    override fun requiresCacheSpanTouches() = true

    override fun onCacheInitialized() = Unit

    override fun onStartFile(
        cache: Cache,
        key: String,
        position: Long,
        length: Long,
    ) {
        if (length != androidx.media3.common.C.LENGTH_UNSET.toLong()) evict(cache, length)
    }

    override fun onSpanAdded(
        cache: Cache,
        span: CacheSpan,
    ) {
        spans.add(span)
        currentSize += span.length
        evict(cache, 0)
    }

    override fun onSpanRemoved(
        cache: Cache,
        span: CacheSpan,
    ) {
        spans.remove(span)
        currentSize -= span.length
    }

    override fun onSpanTouched(
        cache: Cache,
        oldSpan: CacheSpan,
        newSpan: CacheSpan,
    ) {
        onSpanRemoved(cache, oldSpan)
        onSpanAdded(cache, newSpan)
    }

    // SimpleCache invokes evictor callbacks while holding its own monitor; take the same one here.
    fun trim(cache: Cache) = synchronized(cache) { evict(cache, 0) }

    private fun evict(
        cache: Cache,
        requiredSpace: Long,
    ) {
        while (currentSize + requiredSpace > maxBytes && spans.isNotEmpty()) {
            cache.removeSpan(spans.first())
        }
    }
}
