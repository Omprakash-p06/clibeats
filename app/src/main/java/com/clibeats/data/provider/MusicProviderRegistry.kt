@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider

import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderRegistry

/**
 * Registry over an explicitly ordered provider list (constructed by the DI
 * module so Dagger never has to resolve a raw `List<MusicProvider>` binding).
 */
class MusicProviderRegistry(
    providers: List<MusicProvider>,
) : ProviderRegistry {
    override val providers: List<MusicProvider> = providers

    override fun getProvider(id: String): MusicProvider? = providers.firstOrNull { it.providerId == id }

    override fun defaultProvider(): MusicProvider {
        return getProvider(ProviderRegistry.DEFAULT_PROVIDER_ID) ?: providers.first()
    }
}
