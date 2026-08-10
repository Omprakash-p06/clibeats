@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider.api

import com.clibeats.data.provider.dto.AudiusSearchResponse
import com.clibeats.data.provider.dto.AudiusTrackResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Audius discovery provider REST API (v1).
 *
 * Base URL: https://discoveryprovider.audius.co/v1/
 *
 * Public read endpoints require no account or API key — only an `app_name`
 * query parameter identifying the consuming application.
 */
interface AudiusApi {
    @GET("tracks/search")
    suspend fun searchTracks(
        @Query("query") query: String,
        @Query("limit") limit: Int = 20,
        @Query("app_name") appName: String = DEFAULT_APP_NAME,
    ): AudiusSearchResponse

    @GET("tracks/trending")
    suspend fun trendingTracks(
        @Query("limit") limit: Int = 20,
        @Query("app_name") appName: String = DEFAULT_APP_NAME,
    ): AudiusSearchResponse

    @GET("tracks/{trackId}")
    suspend fun getTrack(
        @Path("trackId") trackId: String,
        @Query("app_name") appName: String = DEFAULT_APP_NAME,
    ): AudiusTrackResponse

    companion object {
        const val DEFAULT_APP_NAME = "clibeats"

        /** Base URL for the Audius v1 REST API. */
        const val BASE_URL = "https://discoveryprovider.audius.co/v1/"
    }
}
