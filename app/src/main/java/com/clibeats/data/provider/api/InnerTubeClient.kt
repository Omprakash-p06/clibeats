package com.clibeats.data.provider.api

import com.clibeats.data.provider.dto.PlayerRequest
import com.clibeats.data.provider.dto.PlayerResponse
import com.clibeats.data.provider.dto.SearchResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InnerTubeClient
    @Inject
    constructor(
        private val api: InnerTubeApi,
    ) {
        suspend fun search(query: String): SearchResponse {
            val request = InnerTubeRequestBuilder.buildSearchRequest(query)
            return api.search(request)
        }

        suspend fun player(request: PlayerRequest): PlayerResponse {
            return api.player(request)
        }
    }
