package com.clibeats.domain.provider

/**
 * The single provider-selection mechanism for the app.
 *
 * The active provider id is persisted via AppPreferences; the registry maps it
 * to a concrete [MusicProvider]. No provider implementation detail leaks into
 * the UI — screens only ever see [MusicProvider] instances.
 */
interface ProviderRegistry {
    /** All registered providers, in display priority order. */
    val providers: List<MusicProvider>

    /** Returns the provider with [id], or null when unknown. */
    fun getProvider(id: String): MusicProvider?

    /** The provider used when no selection has been persisted yet. */
    fun defaultProvider(): MusicProvider

    companion object {
        const val DEFAULT_PROVIDER_ID = "youtube_music"
    }
}
