package com.clibeats.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.clibeats.R
import com.clibeats.util.ARTWORK_FULL_PX
import com.clibeats.util.ARTWORK_THUMB_PX
import com.clibeats.util.artworkUrlFor
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Artwork for the media notification, lock screen and Bluetooth displays, through the app's Coil
 * image cache (the same covers the lists and Now Playing already downloaded).
 *
 * Order: cached high-res → cached smaller cover (shown at once, high-res fetched in the background and
 * swapped in through [onBetterArtwork]) → download high-res → medium (480 px, always exists on YouTube,
 * where `maxresdefault` often doesn't) → thumbnail → the app logo. The notification never ends up
 * with no artwork (which Android shows as a plain coloured card).
 */
@OptIn(UnstableApi::class)
class ArtworkBitmapLoader(
    private val context: Context,
    private val imageLoader: ImageLoader,
    private val onBetterArtwork: () -> Unit,
) : BitmapLoader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logo: Bitmap by lazy {
        ContextCompat.getDrawable(context, R.mipmap.ic_launcher)!!.toBitmap(LOGO_PX, LOGO_PX)
    }

    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        BitmapFactory.decodeByteArray(data, 0, data.size)?.let { Futures.immediateFuture(it) }
            ?: Futures.immediateFuture(logo)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = load(uri.toString())

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        metadata.artworkData?.let { return decodeBitmap(it) }
        val source = metadata.extras?.getString(ARTWORK_SOURCE_KEY) ?: metadata.artworkUri?.toString()
        return source?.let(::load)
    }

    private fun load(source: String): ListenableFuture<Bitmap> {
        val result = SettableFuture.create<Bitmap>()
        val job =
            scope.launch {
                val full = artworkUrlFor(source, ARTWORK_FULL_PX) ?: source
                val smaller =
                    listOfNotNull(artworkUrlFor(source, MEDIUM_PX), artworkUrlFor(source, ARTWORK_THUMB_PX))
                        .distinct()
                        .filter { it != full }

                val cachedFull = fetch(full, cacheOnly = true)
                if (cachedFull != null) {
                    result.set(cachedFull)
                    return@launch
                }
                val cachedSmaller = smaller.firstNotNullOfOrNull { fetch(it, cacheOnly = true) }
                if (cachedSmaller != null) {
                    result.set(cachedSmaller)
                    // Swap in the high-res cover once it's downloaded.
                    if (fetch(full, cacheOnly = false) != null) onBetterArtwork()
                    return@launch
                }
                val downloaded =
                    withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) {
                        fetch(full, cacheOnly = false) ?: smaller.firstNotNullOfOrNull { fetch(it, cacheOnly = false) }
                    }
                result.set(downloaded ?: logo)
            }
        // A cancelled or crashed load must not leave ExoPlayer waiting on a future nobody completes.
        job.invokeOnCompletion {
            if (!result.isDone) runCatching { result.set(logo) }
        }
        return result
    }

    /** Loads [url] through Coil as a software bitmap (notifications can't use hardware bitmaps). */
    private suspend fun fetch(
        url: String,
        cacheOnly: Boolean,
    ): Bitmap? {
        val request =
            ImageRequest
                .Builder(context)
                .data(url)
                .size(NOTIFICATION_PX)
                .allowHardware(false)
                .apply { if (cacheOnly) networkCachePolicy(CachePolicy.DISABLED) }
                .build()
        val result = runCatching { imageLoader.execute(request) }.getOrNull() as? SuccessResult ?: return null
        return (result.drawable as? BitmapDrawable)?.bitmap
    }

    companion object {
        /** MediaMetadata extra holding the track's original artwork URL (sizes are derived from it). */
        const val ARTWORK_SOURCE_KEY = "com.clibeats.ARTWORK_SOURCE"
        private const val MEDIUM_PX = 480
        private const val NOTIFICATION_PX = 720
        private const val LOGO_PX = 256
        private const val DOWNLOAD_TIMEOUT_MS = 8_000L
    }
}
