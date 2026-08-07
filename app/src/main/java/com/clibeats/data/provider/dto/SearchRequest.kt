package com.clibeats.data.provider.dto

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
                context = InnerTubeContext.default(),
                query = query,
                params = if (filterSongs) SONGS_FILTER_PARAM else null,
            )
    }
}

@Serializable
data class InnerTubeContext(
    val client: InnerTubeClient,
) {
    companion object {
        fun default(): InnerTubeContext =
            InnerTubeContext(
                client =
                    InnerTubeClient(
                        clientName = "WEB_REMIX",
                        clientVersion = "1.20240101.01.00",
                        hl = "en",
                        gl = "US",
                    ),
            )

        fun tvHtml5(): InnerTubeContext =
            InnerTubeContext(
                client =
                    InnerTubeClient(
                        clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
                        clientVersion = "2.0",
                        hl = "en",
                        gl = "US",
                    ),
            )

        fun android(): InnerTubeContext =
            InnerTubeContext(
                client =
                    InnerTubeClient(
                        clientName = "ANDROID",
                        clientVersion = "19.05.36",
                        hl = "en",
                        gl = "US",
                    ),
            )
    }
}

@Serializable
data class InnerTubeClient(
    val clientName: String,
    val clientVersion: String,
    val hl: String = "en",
    val gl: String = "US",
)
