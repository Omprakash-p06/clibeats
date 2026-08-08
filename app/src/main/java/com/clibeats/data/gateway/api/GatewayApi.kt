package com.clibeats.data.gateway.api

import com.clibeats.data.gateway.dto.GatewayAlbumDto
import com.clibeats.data.gateway.dto.GatewayArtistDto
import com.clibeats.data.gateway.dto.GatewayPlaylistDto
import com.clibeats.data.gateway.dto.GatewaySearchResponse
import com.clibeats.data.gateway.dto.GatewayStreamRequest
import com.clibeats.data.gateway.dto.GatewayStreamResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for the CliBeats Provider Gateway REST API.
 * Mirrors the routes registered in gateway/src/app.ts.
 */
interface GatewayApi {
    @GET("api/v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("filterSongs") filterSongs: Boolean = true,
    ): GatewaySearchResponse

    @POST("api/v1/stream")
    suspend fun stream(
        @Body body: GatewayStreamRequest,
    ): GatewayStreamResponse

    @GET("api/v1/album/{id}")
    suspend fun album(
        @Path("id") id: String,
    ): GatewayAlbumDto

    @GET("api/v1/artist/{id}")
    suspend fun artist(
        @Path("id") id: String,
    ): GatewayArtistDto

    @GET("api/v1/playlist/{id}")
    suspend fun playlist(
        @Path("id") id: String,
    ): GatewayPlaylistDto
}