package com.clibeats.data.provider.api

import com.clibeats.data.provider.dto.InnerTubeContext
import com.clibeats.data.provider.dto.PlayerRequest
import com.clibeats.data.provider.dto.SearchRequest

object InnerTubeRequestBuilder {
    fun buildSearchRequest(query: String): SearchRequest {
        return SearchRequest.forQuery(query)
    }

    fun buildPlayerRequest(
        videoId: String,
        context: InnerTubeContext = InnerTubeContext.tvHtml5(),
    ): PlayerRequest {
        return PlayerRequest(
            context = context,
            videoId = videoId,
        )
    }
}
