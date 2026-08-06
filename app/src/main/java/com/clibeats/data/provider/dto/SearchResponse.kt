package com.clibeats.data.provider.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Top-level InnerTube search response.
 * Inner structure is parsed imperatively in TrackMapper due to deep nesting and
 * frequent YouTube API shape changes. Using JsonElement for the contents tree
 * allows null-safe navigation without brittle deeply-nested data class hierarchies.
 */
@Serializable
data class SearchResponse(
    val contents: JsonElement? = null,
)
