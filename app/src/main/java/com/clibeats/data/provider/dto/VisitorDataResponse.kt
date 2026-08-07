package com.clibeats.data.provider.dto

import kotlinx.serialization.Serializable

@Serializable
data class VisitorDataResponse(
    val responseContext: ResponseContext? = null,
)

@Serializable
data class ResponseContext(
    val visitorData: String? = null,
)

@Serializable
data class VisitorDataRequest(
    val context: InnerTubeContext = InnerTubeContext.default(),
)
