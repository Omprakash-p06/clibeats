@file:Suppress("ForbiddenImport")

package com.clibeats.di

import android.content.Context
import com.clibeats.data.download.TrackDownloadManager
import com.clibeats.data.local.dao.DownloadedTrackDao
import com.clibeats.data.local.dao.SongDao
import com.clibeats.playback.StreamResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {
    @Provides
    @Singleton
    fun provideTrackDownloadManager(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
        streamResolver: dagger.Lazy<StreamResolver>,
        downloadedTrackDao: DownloadedTrackDao,
        songDao: SongDao,
    ): TrackDownloadManager =
        TrackDownloadManager(
            context = context,
            okHttpClient = okHttpClient,
            streamResolver = streamResolver,
            downloadedTrackDao = downloadedTrackDao,
            songDao = songDao,
        )
}
