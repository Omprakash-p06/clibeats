package com.clibeats.di

import com.clibeats.data.cache.CacheManager
import com.clibeats.data.download.TrackDownloadManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {
    @Provides
    @Singleton
    fun provideTrackDownloadManager(
        okHttpClient: OkHttpClient,
        cacheManager: CacheManager,
    ): TrackDownloadManager = TrackDownloadManager(okHttpClient, cacheManager)
}
