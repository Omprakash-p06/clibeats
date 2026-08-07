package com.clibeats.domain.provider

interface StreamResolver {
    suspend fun resolve(videoId: String): StreamResult
}
