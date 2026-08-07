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
                        clientName = "TVHTML5",
                        clientVersion = "7.20230405.08.01",
                        hl = "en",
                        gl = "US",
                    ),
            )

        fun androidTestSuite(): InnerTubeContext =
            InnerTubeContext(
                client =
                    InnerTubeClient(
                        clientName = "ANDROID_TESTSUITE",
                        clientVersion = "1.9",
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

        fun androidMusic(): InnerTubeContext =
            InnerTubeContext(
                client =
                    InnerTubeClient(
                        clientName = "ANDROID_MUSIC",
                        clientVersion = "6.42.52",
                        hl = "en",
                        gl = "US",
                    ),
            )

        fun ios(): InnerTubeContext =
            InnerTubeContext(
                client =
                    InnerTubeClient(
                        clientName = "IOS",
                        clientVersion = "19.05.2",
                        hl = "en",
                        gl = "US",
                    ),
            )

        fun web(): InnerTubeContext =
            InnerTubeContext(
                client =
                    InnerTubeClient(
                        clientName = "WEB",
                        clientVersion = "2.20240101.01.00",
                        hl = "en",
                        gl = "US",
                    ),
            )
        fun webEmbedded(videoId: String = ""): InnerTubeContext =
            InnerTubeContext(
                client =
                    InnerTubeClient(
                        clientName = "WEB_EMBEDDED_PLAYER",
                        clientVersion = "1.20210629.00.00",
                        hl = "en",
                        gl = "US",
                        originalUrl = if (videoId.isNotBlank()) "https://www.youtube.com/watch?v=$videoId" else null,
                    ),
            )

        fun iosStandalone(): InnerTubeContext =
            InnerTubeContext(
                client =
                    InnerTubeClient(
                        clientName = "IOS",
                        clientVersion = "19.05.2",
                        deviceModel = "iPhone16,2",
                        userAgent = "com.google.ios.youtube/19.05.2 (iPhone16,2; U; CPU iOS 17_4_1 like Mac OS X; en_US)",
                        osName = "iPhone",
                        osVersion = "17.4.1.21E236",
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
    val deviceModel: String? = null,
    val userAgent: String? = null,
    val osName: String? = null,
    val osVersion: String? = null,
    val visitorData: String? = null,
    val originalUrl: String? = null,
)
