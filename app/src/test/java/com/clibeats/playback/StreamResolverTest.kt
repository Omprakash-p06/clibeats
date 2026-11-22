@file:Suppress("ForbiddenImport")

package com.clibeats.playback

import com.clibeats.data.cache.MediaCaches
import com.clibeats.data.network.NetworkMonitor
import com.clibeats.data.provider.youtube.PoTokenGenerator
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class StreamResolverTest {
    private lateinit var providerRegistry: ProviderRegistry
    private lateinit var provider: MusicProvider
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var resolver: StreamResolver

    @Before
    fun setup() {
        providerRegistry = mock()
        provider = mock()
        networkMonitor = mock()
        whenever(networkMonitor.isOnline).thenReturn(MutableStateFlow(true))
        resolver =
            StreamResolver(
                providerRegistry = providerRegistry,
                mediaCaches = mock<dagger.Lazy<MediaCaches>>(),
                poTokenGenerator = mock<PoTokenGenerator>(),
                networkMonitor = networkMonitor,
            )
    }

    private fun track(
        id: String = "yt1",
        providerId: String = "youtube_music",
        streamUrl: String? = null,
    ) = Track(
        id = id,
        title = "Title",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        artworkUrl = null,
        streamUrl = streamUrl,
        providerId = providerId,
    )

    @Test
    fun `isStreamExpired is false when the URL carries no expire parameter`() {
        assertThat(StreamResolver.isStreamExpired("https://rr1.googlevideo.com/videoplayback?id=1")).isFalse()
    }

    @Test
    fun `isStreamExpired is true once the expire epoch has passed`() {
        assertThat(StreamResolver.isStreamExpired("https://x/videoplayback?expire=1000", nowMs = 2_000_000L)).isTrue()
    }

    @Test
    fun `isStreamExpired keeps a 60 second safety margin`() {
        val now = 1_000_000L
        val expireSec = (now + 59_000L) / 1000L

        assertThat(StreamResolver.isStreamExpired("https://x/videoplayback?expire=$expireSec", nowMs = now)).isTrue()
    }

    @Test
    fun `resolve returns a fresh track without asking the provider`() =
        runTest {
            val expireSec = (System.currentTimeMillis() + 3_600_000L) / 1000L
            val fresh = track(streamUrl = "https://x/videoplayback?expire=$expireSec")

            val resolved = resolver.resolve(fresh)

            assertThat(resolved).isSameInstanceAs(fresh)
            verify(providerRegistry, never()).getProvider(any())
        }

    @Test
    fun `warmTracks does nothing while offline`() =
        runTest {
            whenever(networkMonitor.isOnline).thenReturn(MutableStateFlow(false))

            resolver.warmTracks(listOf(track()))

            verify(providerRegistry, never()).getProvider(any())
        }

    @Test
    fun `warmTracks skips local and already fresh tracks`() =
        runTest {
            val expireSec = (System.currentTimeMillis() + 3_600_000L) / 1000L
            val fresh = track(id = "yt1", streamUrl = "https://x/videoplayback?expire=$expireSec")
            val local = track(id = "l1", providerId = "local")

            resolver.warmTracks(listOf(fresh, local))

            verify(providerRegistry, never()).getProvider(any())
        }
}
