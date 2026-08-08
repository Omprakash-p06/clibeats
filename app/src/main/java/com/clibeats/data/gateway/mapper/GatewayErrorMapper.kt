package com.clibeats.data.gateway.mapper

import com.clibeats.data.gateway.dto.GatewayErrorResponse
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Maps a [Throwable] raised by the Gateway call into a user-facing message.
 * Network/IO transport failures produce a generic retry hint; HTTP error
 * responses are grouped by gateway error code (ErrorResponseSchema).
 */
internal object GatewayErrorMapper {
    private const val CODE_RATE_LIMITED = "RATE_LIMITED"
    private const val CODE_GEO_BLOCKED = "GEO_BLOCKED"
    private const val CODE_NOT_FOUND = "NOT_FOUND"
    private const val CODE_AUTHENTICATION = "AUTHENTICATION_FAILED"
    private const val CODE_PLAYBACK = "PLAYBACK_ERROR"
    private const val CODE_TIMEOUT = "TIMEOUT_ERROR"
    private const val CODE_NETWORK = "NETWORK_ERROR"

    fun message(throwable: Throwable): String {
        val gatewayErr = parseGatewayError(throwable)
        return when (gatewayErr?.code) {
            CODE_RATE_LIMITED -> "Rate limit exceeded, try again shortly"
            CODE_GEO_BLOCKED -> "This track is not available in your region"
            CODE_NOT_FOUND -> "Track not found"
            CODE_AUTHENTICATION -> "Authentication failed"
            CODE_PLAYBACK -> "Unable to play this track"
            CODE_TIMEOUT -> "Request timed out, check your connection"
            CODE_NETWORK -> "Network error, check your connection"
            else -> gatewayErr?.message ?: throwable.message ?: "Request failed"
        }
    }

    private fun parseGatewayError(throwable: Throwable) =
        if (throwable is HttpException) {
            runCatching {
                val body = throwable.response()?.errorBody()?.string() ?: return@runCatching null
                Json.decodeFromString<GatewayErrorResponse>(body).error
            }.getOrNull()
        } else {
            null
        }
}
