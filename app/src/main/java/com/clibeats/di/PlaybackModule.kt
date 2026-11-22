@file:Suppress("ForbiddenImport", "LongParameterList")

package com.clibeats.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.clibeats.data.cache.CacheManager
import com.clibeats.data.cache.MediaCaches
import com.clibeats.data.local.dao.DownloadedTrackDao
import com.clibeats.playback.DeferredDataSourceFactory
import com.clibeats.playback.LocalOrRemoteMediaSourceFactory
import com.clibeats.playback.StreamResolver
import com.clibeats.playback.TrackUriResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule {
    private const val MIN_BUFFER_MS = 15_000
    private const val MAX_BUFFER_MS = 50_000
    private const val BUFFER_FOR_PLAYBACK_MS = 300
    private const val BUFFER_AFTER_REBUFFER_MS = 1_000

    @Provides
    @Singleton
    fun provideAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes,
        okHttpClient: OkHttpClient,
        streamResolver: dagger.Lazy<StreamResolver>,
        cacheManager: dagger.Lazy<CacheManager>,
        mediaCaches: dagger.Lazy<MediaCaches>,
        downloadedTrackDao: dagger.Lazy<DownloadedTrackDao>,
    ): ExoPlayer {
        // Start playback after 0.3s of audio instead of Media3's 2.5s default.
        val loadControl =
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    MIN_BUFFER_MS,
                    MAX_BUFFER_MS,
                    BUFFER_FOR_PLAYBACK_MS,
                    BUFFER_AFTER_REBUFFER_MS,
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        // Reuse the app's OkHttp pool: warm TLS sessions and HTTP/2 instead of a cold handshake per play.
        val httpFactory = OkHttpDataSource.Factory(okHttpClient)
        val resolvingFactory =
            ResolvingDataSource.Factory(
                httpFactory,
                TrackUriResolver(streamResolver, cacheManager, downloadedTrackDao),
            )
        // Remote audio goes through the song cache. The cache key is the stable placeholder/track URI,
        // so a cached song replays without re-resolving (or any network). Local files skip the cache.
        // The cache is opened on first use, not while the player is being built at app start.
        val cachedFactory =
            DeferredDataSourceFactory {
                CacheDataSource.Factory()
                    .setCache(mediaCaches.get().songCache)
                    .setUpstreamDataSourceFactory(resolvingFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            }
        val mediaSourceFactory =
            LocalOrRemoteMediaSourceFactory(
                local = DefaultMediaSourceFactory(DefaultDataSource.Factory(context)),
                remote = DefaultMediaSourceFactory(cachedFactory),
            )
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
    }
}
