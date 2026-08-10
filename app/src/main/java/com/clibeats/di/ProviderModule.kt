@file:Suppress("ForbiddenImport")

package com.clibeats.di

import com.clibeats.BuildConfig
import com.clibeats.data.provider.AudiusMusicProvider
import com.clibeats.data.provider.InternetArchiveMusicProvider
import com.clibeats.data.provider.JamendoMusicProvider
import com.clibeats.data.provider.LocalMusicProvider
import com.clibeats.data.provider.MusicProviderRegistry
import com.clibeats.data.provider.YouTubeMusicProvider
import com.clibeats.domain.provider.ProviderRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Provides the [ProviderRegistry] — the single provider-selection mechanism.
 * Providers are registered in display-priority order; the active one is chosen
 * via AppPreferences and looked up by id.
 */
@Module
@InstallIn(SingletonComponent::class)
object ProviderModule {
    @Provides
    @Singleton
    fun provideProviderRegistry(
        youtube: YouTubeMusicProvider,
        internetArchive: InternetArchiveMusicProvider,
        audius: AudiusMusicProvider,
        jamendo: JamendoMusicProvider,
        local: LocalMusicProvider,
    ): ProviderRegistry =
        MusicProviderRegistry(
            listOf(youtube, internetArchive, audius, jamendo, local),
        )

    /** Optional free Jamendo API key (developer.jamendo.com); empty when unset. */
    @Provides
    @Singleton
    @Named(JAMENDO_CLIENT_ID_QUALIFIER)
    fun provideJamendoClientId(): String = BuildConfig.JAMENDO_CLIENT_ID

    const val JAMENDO_CLIENT_ID_QUALIFIER = "jamendo_client_id"
}
