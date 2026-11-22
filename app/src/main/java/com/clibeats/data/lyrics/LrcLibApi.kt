package com.clibeats.data.lyrics

import retrofit2.http.GET
import retrofit2.http.Query

interface LrcLibApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") duration: Int? = null,
    ): LrcLibDto

    companion object {
        const val BASE_URL = "https://lrclib.net/"
    }
}
