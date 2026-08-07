package com.clibeats.data.provider.resolver

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamValidator
    @Inject
    constructor() {
        private val client: OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()

        suspend fun validate(url: String): Boolean =
            withContext(Dispatchers.IO) {
                runCatching {
                    val request =
                        Request.Builder()
                            .url(url)
                            .head()
                            .header("User-Agent", "Mozilla/5.0")
                            .build()
                    val response = client.newCall(request).execute()
                    response.isSuccessful || response.code == 206
                }.getOrDefault(false)
            }
    }
