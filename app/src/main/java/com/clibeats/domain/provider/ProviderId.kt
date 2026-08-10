package com.clibeats.domain.provider

/**
 * Collision-safe track identifiers across providers.
 *
 * A [Track.id] is a composite of the provider id and the provider-local source
 * id, e.g. `internet_archive:<identifier>`, `audius:<id>`, `jamendo:<id>`,
 * `local:<id>`. This keeps library/queue/cache keys unique when multiple
 * providers are active.
 */
object ProviderId {
    const val SEPARATOR = ":"

    fun composite(
        providerId: String,
        sourceId: String,
    ): String = "$providerId$SEPARATOR$sourceId"

    fun rawSourceId(
        providerId: String,
        compositeId: String,
    ): String {
        val prefix = "$providerId$SEPARATOR"
        return if (compositeId.startsWith(prefix)) compositeId.removePrefix(prefix) else compositeId
    }
}
