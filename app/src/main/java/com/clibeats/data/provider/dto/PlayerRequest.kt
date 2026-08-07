package com.clibeats.data.provider.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlayerRequest(
    val context: InnerTubeContext,
    val videoId: String,
    val playbackContext: PlaybackContext? = PlaybackContext(),
    val racyCheckOk: Boolean = true,
    val contentCheckOk: Boolean = true,
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
    val contentPlaybackContext: ContentPlaybackContext = ContentPlaybackContext(),
)

@Serializable
data class ContentPlaybackContext(
    val signatureTimestamp: Int = 19842,
    val html5Preference: String = "HTML5_PREFER_FORMAT_22",
)
