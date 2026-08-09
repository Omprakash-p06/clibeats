@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider.api

import com.clibeats.data.provider.dto.PlayerRequest
import com.clibeats.data.provider.dto.PlayerResponse
import com.clibeats.data.provider.dto.SearchRequest
import com.clibeats.data.provider.dto.SearchResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface InnerTubeApi {
    @POST("search")
    suspend fun search(
        @Body body: SearchRequest,
    ): SearchResponse

    @POST("player")
    suspend fun player(
        @Body body: PlayerRequest,
    ): PlayerResponse
}
