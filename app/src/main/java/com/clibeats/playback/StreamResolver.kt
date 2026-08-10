package com.clibeats.playback

import com.clibeats.domain.model.PlaybackException
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.ProviderRegistry
import com.clibeats.domain.provider.ProviderResult
import com.clibeats.util.DiagnosticLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamResolver
    @Inject
    constructor(
        private val providerRegistry: ProviderRegistry,
    ) {
        suspend fun resolve(
            track: Track,
            traceId: String = DiagnosticLogger.generateTraceId(),
        ): Track {
            if (!track.streamUrl.isNullOrBlank()) {
                return track
            }

            val provider =
                providerRegistry.getProvider(track.providerId)
                    ?: providerRegistry.defaultProvider()

            val result = provider.stream(track.id)
            return when (result) {
                is ProviderResult.Success -> {
                    track.copy(streamUrl = result.data)
                }

                is ProviderResult.Error -> {
                    DiagnosticLogger.logError(
                        traceId,
                        "STREAM_RESOLUTION_FAILED",
                        result.message,
                    )
                    throw PlaybackException.StreamResolutionFailed(result.message, result.cause)
                }

                else -> {
                    DiagnosticLogger.logError(
                        traceId,
                        "STREAM_RESOLUTION_FAILED",
                        "Provider result is loading or unhandled",
                    )
                    throw PlaybackException.StreamResolutionFailed("Provider result is loading or unhandled")
                }
            }
        }
    }
