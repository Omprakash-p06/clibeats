@file:Suppress("ForbiddenImport", "LongParameterList")

package com.clibeats.data.provider.api

import com.clibeats.data.provider.dto.JamendoResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Jamendo API v3.0 — free developer API (Creative Commons catalog).
 *
 * Requires a free `client_id` obtained from developer.jamendo.com. The id is
 * injected via BuildConfig (gradle property `JAMENDO_CLIENT_ID`), never
 * hardcoded.
 */
interface JamendoApi {
    @GET("tracks/")
    suspend fun tracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null,
        @Query("id") id: String? = null,
        @Query("order") order: String? = null,
        @Query("audioformat") audioFormat: String = "mp32",
    ): JamendoResponse
}
