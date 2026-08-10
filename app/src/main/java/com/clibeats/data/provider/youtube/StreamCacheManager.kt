@file:Suppress("ReturnCount", "MagicNumber")

package com.clibeats.data.provider.youtube

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamCacheManager
    @Inject
    constructor() {
        private val cache = ConcurrentHashMap<String, ExtractedStreamInfo>()

        private fun cacheKey(
            providerId: String,
            videoId: String,
        ): String = "$providerId:$videoId"

        fun get(
            providerId: String,
            videoId: String,
        ): ExtractedStreamInfo? {
            val key = cacheKey(providerId, videoId)
            val info = cache[key] ?: return null

            // Check expiration with a 60-second safety margin
            if (System.currentTimeMillis() + 60_000L >= info.expiresAtMs) {
                cache.remove(key)
                return null
            }
            return info
        }

        fun put(
            providerId: String,
            videoId: String,
            info: ExtractedStreamInfo,
        ) {
            val key = cacheKey(providerId, videoId)
            cache[key] = info
        }

        fun invalidate(
            providerId: String,
            videoId: String,
        ) {
            val key = cacheKey(providerId, videoId)
            cache.remove(key)
        }

        fun clear() {
            cache.clear()
        }
    }
