@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider.dto

import kotlinx.serialization.Serializable

@Serializable
data class NextRequest(
    val context: InnerTubeContext,
    val videoId: String,
    val playlistId: String,
) {
    companion object {
        fun radio(videoId: String): NextRequest =
            NextRequest(
                context = InnerTubeContext.default(),
                videoId = videoId,
                playlistId = "RDAMVM$videoId",
            )
    }
}
