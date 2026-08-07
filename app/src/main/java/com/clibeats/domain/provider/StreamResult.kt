package com.clibeats.domain.provider

sealed interface StreamResult {
    data class Success(
        val url: String,
        val format: String = "audio/mp4",
        val expiresMs: Long = 0L,
    ) : StreamResult

    object SignatureExpired : StreamResult

    object InvalidSignature : StreamResult

    object GeoBlocked : StreamResult

    object LoginRequired : StreamResult

    object RateLimited : StreamResult

    object NoFormats : StreamResult

    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : StreamResult
}
