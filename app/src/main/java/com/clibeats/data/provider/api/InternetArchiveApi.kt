@file:Suppress("ForbiddenImport", "LongParameterList")

package com.clibeats.data.provider.api

import com.clibeats.data.provider.dto.IaMetadataResponse
import com.clibeats.data.provider.dto.IaSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Internet Archive public REST APIs (no account or key required).
 *
 * - advancedsearch.php — Lucene search over item metadata.
 * - metadata/{identifier} — full item metadata including the files[] list.
 *
 * Playable files are streamed directly from https://archive.org/download/...,
 * which issues 302 redirects to storage mirrors and supports HTTP Range (206).
 */
interface InternetArchiveApi {
    @GET("advancedsearch.php")
    suspend fun search(
        @Query("q") query: String,
        @Query("fl[]") fields: List<String> = DEFAULT_FIELDS,
        @Query("rows") rows: Int = 20,
        @Query("page") page: Int = 1,
        @Query("sort[]") sort: List<String>? = null,
        @Query("output") output: String = "json",
    ): IaSearchResponse

    @GET("metadata/{identifier}")
    suspend fun metadata(
        @Path("identifier") identifier: String,
    ): IaMetadataResponse

    companion object {
        val DEFAULT_FIELDS =
            listOf(
                "identifier",
                "title",
                "creator",
                "date",
                "mediatype",
                "collection",
            )
    }
}
