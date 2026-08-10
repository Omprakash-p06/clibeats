@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider.dto

import com.clibeats.data.provider.youtube.YouTubeClientConfig
import kotlinx.serialization.Serializable

@Serializable
data class SearchRequest(
    val context: InnerTubeContext,
    val query: String,
    val params: String? = null,
) {
    companion object {
        // Base64-encoded protobuf param that filters to "songs" type
        private const val SONGS_FILTER_PARAM = "EgWKAQIIAWoMEA4QChADEAQQCRAF"

        fun forQuery(
            query: String,
            filterSongs: Boolean = true,
        ): SearchRequest =
            SearchRequest(
                context =
                    InnerTubeContext.forClient(
                        YouTubeClientConfig(
                            name = "WEB_REMIX",
                            clientName = "WEB_REMIX",
                            clientVersion = "1.20240618.01.00",
                            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                        ),
                    ),
                query = query,
                params = if (filterSongs) SONGS_FILTER_PARAM else null,
            )
    }
}
