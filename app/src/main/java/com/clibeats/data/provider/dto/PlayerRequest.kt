package com.clibeats.data.provider.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlayerRequest(
    val context: InnerTubeContext,
    val videoId: String,
    val playbackContext: PlaybackContext? = null,
) {
    companion object {
        fun forVideoId(videoId: String): PlayerRequest =
            PlayerRequest(
                context = InnerTubeContext.default(),
                videoId = videoId,
            )
    }
}

@Serializable
data class PlaybackContext(
    val contentPlaybackContext: ContentPlaybackContext,
)

@Serializable
data class ContentPlaybackContext(
    val signatureTimestamp: Int = 0,
)
