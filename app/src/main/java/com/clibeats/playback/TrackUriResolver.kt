@file:Suppress("ForbiddenImport")

package com.clibeats.playback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import com.clibeats.data.cache.CacheManager
import com.clibeats.data.local.dao.DownloadedTrackDao
import com.clibeats.domain.model.Track
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException

/**
 * Lazily turns `clibeats://track?p=<providerId>&id=<trackId>` placeholder URIs into real
 * stream URLs when ExoPlayer opens the item. Runs on ExoPlayer's loader thread, so
 * blocking here never touches the main thread.
 *
 * Priority order:
 * 1. Durable offline downloads ([DownloadedTrackDao])
 * 2. Transient LRU disk cache ([CacheManager])
 * 3. Fresh remote resolution via [StreamResolver]
 */
@OptIn(UnstableApi::class)
class TrackUriResolver(
    private val streamResolver: dagger.Lazy<StreamResolver>,
    private val cacheManager: dagger.Lazy<CacheManager>,
    private val downloadedTrackDao: dagger.Lazy<DownloadedTrackDao>? = null,
) : ResolvingDataSource.Resolver {
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val uri = dataSpec.uri
        if (uri.scheme != SCHEME) return dataSpec
        val providerId = uri.getQueryParameter("p").orEmpty()
        val trackId = uri.getQueryParameter("id") ?: throw IOException("Missing track id in $uri")

        val resolved =
            runBlocking {
                // 1. Check durable offline downloads
                downloadedTrackDao?.get()?.getById(trackId)?.let { entity ->
                    val file = File(entity.filePath)
                    if (file.exists()) return@runBlocking Uri.fromFile(file)
                }

                // 2. Check transient LRU cache
                cacheManager.get().getCachedFile(trackId)?.takeIf { it.exists() }?.let { Uri.fromFile(it) }
                    ?: runCatching {
                        // 3. Resolve stream URL
                        streamResolver.get().resolve(placeholderTrack(trackId, providerId)).streamUrl
                    }.getOrElse { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        throw IOException("Stream resolution failed for $trackId", error)
                    }?.let { Uri.parse(it) }
            } ?: throw IOException("No stream URL for $trackId")
        return dataSpec.withUri(resolved)
    }

    companion object {
        const val SCHEME = "clibeats"

        fun placeholderUri(track: Track): Uri =
            Uri.Builder()
                .scheme(SCHEME)
                .authority("track")
                .appendQueryParameter("p", track.providerId)
                .appendQueryParameter("id", track.id)
                .build()

        private fun placeholderTrack(
            id: String,
            providerId: String,
        ) = Track(
            id = id,
            title = "",
            artist = "",
            album = "",
            durationMs = 0L,
            artworkUrl = null,
            streamUrl = null,
            providerId = providerId,
        )
    }
}
