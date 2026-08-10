package com.clibeats.data.provider.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Internet Archive API DTOs.
 *
 * `creator` and `collection` may arrive as either a JSON string or a JSON
 * array depending on the item, so they are modelled as [JsonElement] and
 * normalised in the mapper.
 */

@Serializable
data class IaSearchResponse(
    val response: IaSearchBody = IaSearchBody(),
)

@Serializable
data class IaSearchBody(
    val numFound: Long = 0L,
    val docs: List<IaSearchDoc> = emptyList(),
)

@Serializable
data class IaSearchDoc(
    val identifier: String? = null,
    val title: String? = null,
    val creator: JsonElement? = null,
    val date: String? = null,
    val mediatype: String? = null,
    val collection: JsonElement? = null,
)

@Serializable
data class IaMetadataResponse(
    val metadata: IaMetadata = IaMetadata(),
    val files: List<IaFile> = emptyList(),
)

@Serializable
data class IaMetadata(
    val identifier: String? = null,
    val title: String? = null,
    val creator: JsonElement? = null,
    val date: String? = null,
    val mediatype: String? = null,
)

@Serializable
data class IaFile(
    val name: String? = null,
    val format: String? = null,
    /** Track length in seconds (string in the API, e.g. "245.5"). */
    val length: String? = null,
    val size: String? = null,
    val source: String? = null,
    val title: String? = null,
)
