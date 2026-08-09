@file:Suppress("ForbiddenImport")

package com.clibeats.di

import com.clibeats.data.provider.YouTubeMusicProvider
import com.clibeats.domain.provider.MusicProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {
    @Binds
    @Singleton
    abstract fun bindMusicProvider(impl: YouTubeMusicProvider): MusicProvider
}
