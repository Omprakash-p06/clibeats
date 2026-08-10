@file:Suppress("MaxLineLength")

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
            val original = chain.request()
            val builder = original.newBuilder()

            if (original.header("User-Agent") == null) {
                builder.header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
                )
            }
            if (original.header("Content-Type") == null) {
                builder.header("Content-Type", "application/json")
            }
            if (original.header("Accept-Language") == null) {
                builder.header("Accept-Language", "en-US,en;q=0.9")
            }
            if (original.header("Origin") == null) {
                builder.header("Origin", "https://music.youtube.com")
            }
            if (original.header("Referer") == null) {
                builder.header("Referer", "https://music.youtube.com/")
            }

            return chain.proceed(builder.build())
        }
    }
