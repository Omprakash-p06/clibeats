package com.clibeats.data.provider.resolver

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamValidator @Inject constructor() {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun validate(url: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val response = client.newCall(request).execute()
            val isOk = response.isSuccessful || response.code == 206
            val contentType = response.header("Content-Type") ?: "unknown"
            val contentLength = response.header("Content-Length") ?: "unknown"
            android.util.Log.d(
                "StreamValidatorDiagnostics",
                "[STREAM_VALIDATION] HTTP Code: ${response.code}, Content-Type: $contentType, Content-Length: $contentLength, Pass: $isOk, URL: $url",
            )
            isOk
        }.getOrElse { e ->
            android.util.Log.e(
                "StreamValidatorDiagnostics",
                "[STREAM_VALIDATION_ERROR] Exception validating URL: ${e.message}, URL: $url",
                e,
            )
            false
        }
    }
}
