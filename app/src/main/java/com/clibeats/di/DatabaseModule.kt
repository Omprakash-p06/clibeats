// ForbiddenImport: data-layer self-imports are legitimate; Phase 0 com.clibeats.data.* pattern is over-broad.
@file:Suppress("ForbiddenImport", "MaxLineLength")

package com.clibeats.di

import android.content.Context
import androidx.room.Room
import com.clibeats.data.local.CliBeatsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideCliBeatsDatabase(
        @ApplicationContext context: Context,
    ): CliBeatsDatabase =
        Room.databaseBuilder(
            context,
            CliBeatsDatabase::class.java,
            "clibeats.db",
        ).build()

    @Provides
    @Singleton
    fun provideSongDao(db: CliBeatsDatabase) = db.songDao()

    @Provides
    @Singleton
    fun providePlaylistDao(db: CliBeatsDatabase) = db.playlistDao()

    @Provides
    @Singleton
    fun provideHistoryDao(db: CliBeatsDatabase) = db.historyDao()

    @Provides
    @Singleton
    fun provideCacheIndexDao(db: CliBeatsDatabase) = db.cacheIndexDao()
}
