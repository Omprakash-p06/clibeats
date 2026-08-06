package com.clibeats.data.provider.api

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InnerTubeHeaderInterceptor
    @Inject
    constructor() : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request =
                chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (compatible; CLIBeats/1.0)")
                    .header("X-YouTube-Client-Name", "67")
                    .header("X-YouTube-Client-Version", "1.20240101.01.00")
                    .header("Content-Type", "application/json")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Origin", "https://music.youtube.com")
                    .header("Referer", "https://music.youtube.com/")
                    .build()
            return chain.proceed(request)
        }
    }
