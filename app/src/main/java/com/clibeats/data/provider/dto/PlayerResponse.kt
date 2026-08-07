package com.clibeats.data.provider.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * InnerTube /player response.
 * streamingData contains adaptive formats with signed audio URLs.
 */
@Serializable
data class PlayerResponse(
    val playabilityStatus: JsonElement? = null,
    val streamingData: JsonElement? = null,
    val videoDetails: VideoDetails? = null,
)

@Serializable
data class VideoDetails(
    val videoId: String? = null,
    val title: String? = null,
    val author: String? = null,
    val lengthSeconds: String? = null,
    val thumbnail: JsonElement? = null,
)
