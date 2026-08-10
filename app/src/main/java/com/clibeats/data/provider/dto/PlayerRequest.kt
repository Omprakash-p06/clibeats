@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider.dto

import com.clibeats.data.provider.youtube.YouTubeClientConfig
import kotlinx.serialization.Serializable

@Serializable
data class PlayerRequest(
    val context: InnerTubeContext,
    val videoId: String,
    val playbackContext: PlaybackContext? = null,
) {
    companion object {
        fun forClient(
            videoId: String,
            config: YouTubeClientConfig,
            poToken: String? = null,
            visitorData: String? = null,
            signatureTimestamp: Int = 0,
        ): PlayerRequest =
            PlayerRequest(
                context = InnerTubeContext.forClient(config, poToken, visitorData),
                videoId = videoId,
                playbackContext =
                    PlaybackContext(
                        contentPlaybackContext = ContentPlaybackContext(signatureTimestamp = signatureTimestamp),
                    ),
            )
    }
}

@Serializable
data class InnerTubeContext(
    val client: InnerTubeClient,
    val user: InnerTubeUser? = null,
    val serviceIntegrityDimensions: ServiceIntegrityDimensions? = null,
) {
    companion object {
        fun default(): InnerTubeContext =
            InnerTubeContext(
                client =
                    InnerTubeClient(
                        clientName = "WEB_REMIX",
                        clientVersion = "1.20240618.01.00",
                        hl = "en",
                        gl = "US",
                    ),
            )

        fun forClient(
            config: YouTubeClientConfig,
            poToken: String? = null,
            visitorData: String? = null,
        ): InnerTubeContext =
            InnerTubeContext(
                client =
                    InnerTubeClient(
                        clientName = config.clientName,
                        clientVersion = config.clientVersion,
                        osName = config.osName,
                        osVersion = config.osVersion,
                        deviceModel = config.deviceModel,
                        userAgent = config.userAgent,
                        visitorData = visitorData,
                        hl = "en",
                        gl = "US",
                    ),
                serviceIntegrityDimensions = poToken?.let { ServiceIntegrityDimensions(poToken = it) },
            )
    }
}

@Serializable
data class InnerTubeClient(
    val clientName: String,
    val clientVersion: String,
    val osName: String? = null,
    val osVersion: String? = null,
    val deviceModel: String? = null,
    val userAgent: String? = null,
    val visitorData: String? = null,
    val hl: String = "en",
    val gl: String = "US",
)

@Serializable
data class InnerTubeUser(
    val lockedSafetyMode: Boolean = false,
)

@Serializable
data class ServiceIntegrityDimensions(
    val poToken: String,
)

@Serializable
data class PlaybackContext(
    val contentPlaybackContext: ContentPlaybackContext,
)

@Serializable
data class ContentPlaybackContext(
    val signatureTimestamp: Int = 0,
)
