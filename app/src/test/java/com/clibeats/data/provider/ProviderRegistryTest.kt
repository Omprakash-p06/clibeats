@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider

import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderRegistry
import com.clibeats.domain.provider.ProviderResult
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProviderRegistryTest {
    private class FakeProvider(
        override val providerId: String,
        override val displayName: String,
    ) : MusicProvider {
        override suspend fun search(
            query: String,
            limit: Int,
        ): ProviderResult<List<Track>> = ProviderResult.Success(emptyList())

        override suspend fun trending(limit: Int): ProviderResult<List<Track>> = ProviderResult.Success(emptyList())

        override suspend fun getTrack(trackId: String): ProviderResult<Track> = ProviderResult.Error("not implemented")

        override suspend fun stream(trackId: String): ProviderResult<String> = ProviderResult.Error("not implemented")

        override suspend fun playlists(): ProviderResult<List<Playlist>> = ProviderResult.Success(emptyList())

        override suspend fun queue(): ProviderResult<List<Track>> = ProviderResult.Success(emptyList())
    }

    private val youtube = FakeProvider("youtube_music", "YouTube Music")
    private val internetArchive = FakeProvider("internet_archive", "Internet Archive")
    private val audius = FakeProvider("audius", "Audius")
    private val jamendo = FakeProvider("jamendo", "Jamendo")
    private val local = FakeProvider("local", "Local Device Media")
    private val registry = MusicProviderRegistry(listOf(youtube, internetArchive, audius, jamendo, local))

    @Test
    fun `getProvider returns provider by id`() {
        assertThat(registry.getProvider("youtube_music")).isSameInstanceAs(youtube)
        assertThat(registry.getProvider("audius")).isSameInstanceAs(audius)
        assertThat(registry.getProvider("local")).isSameInstanceAs(local)
    }

    @Test
    fun `getProvider returns null for unknown id`() {
        assertThat(registry.getProvider("spotify")).isNull()
    }

    @Test
    fun `defaultProvider is youtube music`() {
        assertThat(registry.defaultProvider()).isSameInstanceAs(youtube)
        assertThat(ProviderRegistry.DEFAULT_PROVIDER_ID).isEqualTo("youtube_music")
    }

    @Test
    fun `providers preserves registration order`() {
        assertThat(registry.providers.map { it.providerId })
            .containsExactly("youtube_music", "internet_archive", "audius", "jamendo", "local")
            .inOrder()
    }
}
