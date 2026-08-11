// ForbiddenImport: DI modules legitimately bind data implementations to domain interfaces
@file:Suppress("ForbiddenImport")

package com.clibeats.di

import com.clibeats.data.repository.HistoryRepositoryImpl
import com.clibeats.data.repository.LibraryRepositoryImpl
import com.clibeats.data.repository.PlaybackRepositoryImpl
import com.clibeats.data.repository.PlaylistRepositoryImpl
import com.clibeats.data.repository.SongRepositoryImpl
import com.clibeats.domain.repository.HistoryRepository
import com.clibeats.domain.repository.LibraryRepository
import com.clibeats.domain.repository.PlaybackRepository
import com.clibeats.domain.repository.PlaylistRepository
import com.clibeats.domain.repository.SongRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSongRepository(impl: SongRepositoryImpl): SongRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackRepository(impl: PlaybackRepositoryImpl): PlaybackRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository
}
