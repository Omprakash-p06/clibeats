@file:Suppress("ForbiddenImport")

package com.clibeats.di

import com.clibeats.data.provider.YouTubeMusicProvider
import com.clibeats.data.provider.resolver.YouTubeStreamResolver
import com.clibeats.data.provider.resolver.cipher.DefaultSignatureDecipher
import com.clibeats.data.provider.resolver.cipher.SignatureDecipher
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.StreamResolver
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

    @Binds
    @Singleton
    abstract fun bindStreamResolver(impl: YouTubeStreamResolver): StreamResolver

    @Binds
    @Singleton
    abstract fun bindSignatureDecipher(impl: DefaultSignatureDecipher): SignatureDecipher
}
