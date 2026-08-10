@file:Suppress("ForbiddenImport", "TooGenericExceptionCaught", "MagicNumber", "SwallowedException")

package com.clibeats.data.provider

import com.clibeats.data.provider.api.InternetArchiveApi
import com.clibeats.data.provider.dto.IaMetadataResponse
import com.clibeats.data.provider.dto.IaSearchDoc
import com.clibeats.data.provider.mapper.IA_PROVIDER_ID
import com.clibeats.data.provider.mapper.bestAudioFile
import com.clibeats.data.provider.mapper.iaStreamUrl
import com.clibeats.data.provider.mapper.scoreIaItem
import com.clibeats.data.provider.mapper.toDomainTrack
import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.MusicProvider
import com.clibeats.domain.provider.ProviderId
import com.clibeats.domain.provider.ProviderResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * MusicProvider backed by the Internet Archive public audio catalog.
 *
 * Search ranks results by title/creator relevance and only returns items that
 * contain a playable audio file (MP3/OGG/FLAC/…). Streams use the Archive's
 * direct download URLs (302 → mirror, HTTP Range supported) — no proxy.
 */
@Singleton
class InternetArchiveMusicProvider
    @Inject
    constructor(
        private val api: InternetArchiveApi,
    ) : MusicProvider {
        override val providerId: String = IA_PROVIDER_ID
        override val displayName: String = "Internet Archive"

        override suspend fun search(
            query: String,
            limit: Int,
        ): ProviderResult<List<Track>> =
            runCatching {
                val docs =
                    api.search(
                        query = "\"$query\" AND mediatype:audio",
                        rows = SEARCH_ROWS,
                    ).response.docs.filter { doc ->
                        doc.mediatype == AUDIO_MEDIATYPE && !doc.identifier.isNullOrBlank()
                    }

                ProviderResult.Success(rankAndMap(docs, query, limit))
            }.getOrElse { e ->
                ProviderResult.Error(e.message ?: "Search failed", e)
            }

        override suspend fun trending(limit: Int): ProviderResult<List<Track>> =
            runCatching {
                val docs =
                    api.search(
                        query = TRENDING_QUERY,
                        rows = SEARCH_ROWS,
                        sort = listOf("downloads desc"),
                    ).response.docs.filter { doc ->
                        doc.mediatype == AUDIO_MEDIATYPE && !doc.identifier.isNullOrBlank()
                    }

                ProviderResult.Success(rankAndMap(docs, query = null, limit))
            }.getOrElse { e ->
                ProviderResult.Error(e.message ?: "Trending fetch failed", e)
            }

        override suspend fun getTrack(trackId: String): ProviderResult<Track> =
            runCatching {
                val rawId = ProviderId.rawSourceId(providerId, trackId)
                val meta = api.metadata(rawId)
                val track =
                    meta.toDomainTrack(
                        doc = IaSearchDoc(identifier = rawId),
                        fallbackIdentifier = rawId,
                    )
                if (track != null) {
                    ProviderResult.Success(track)
                } else {
                    ProviderResult.Error("No playable audio found for: $trackId")
                }
            }.getOrElse { e ->
                ProviderResult.Error(e.message ?: "Track lookup failed for: $trackId", e)
            }

        override suspend fun stream(trackId: String): ProviderResult<String> =
            runCatching {
                val rawId = ProviderId.rawSourceId(providerId, trackId)
                val meta = api.metadata(rawId)
                val file = meta.files.bestAudioFile()
                if (file?.name != null) {
                    ProviderResult.Success(iaStreamUrl(rawId, file.name))
                } else {
                    ProviderResult.Error("No audio stream URL found for: $trackId")
                }
            }.getOrElse { e ->
                ProviderResult.Error(e.message ?: "Stream failed for: $trackId", e)
            }

        override suspend fun playlists(): ProviderResult<List<Playlist>> = ProviderResult.Success(emptyList())

        override suspend fun queue(): ProviderResult<List<Track>> = ProviderResult.Success(emptyList())

        /** Fetches metadata for up to [maxItems] docs (bounded parallelism), scores and maps them. */
        private suspend fun rankAndMap(
            docs: List<IaSearchDoc>,
            query: String?,
            limit: Int,
        ): List<Track> {
            if (docs.isEmpty()) return emptyList()
            val gate = Semaphore(METADATA_PARALLELISM)
            val scored =
                coroutineScope {
                    docs.take(METADATA_CAP).map { doc ->
                        async {
                            gate.acquire()
                            try {
                                val meta = api.metadata(doc.identifier!!)
                                Score(doc, meta, scoreIaItem(doc, meta, query))
                            } catch (e: CancellationException) {
                                // Never swallow cancellation: let it propagate to the scope.
                                throw e
                            } catch (e: Exception) {
                                // A failing metadata fetch just drops that item from ranking.
                                Score(doc, null, -1)
                            } finally {
                                gate.release()
                            }
                        }
                    }.map { it.await() }
                }
            return scored
                .filter { it.meta != null && it.score >= 0 }
                .sortedByDescending { it.score }
                .mapNotNull { it.meta?.toDomainTrack(it.doc) }
                .take(limit)
        }

        private data class Score(
            val doc: IaSearchDoc,
            val meta: IaMetadataResponse?,
            val score: Int,
        )

        companion object {
            private const val AUDIO_MEDIATYPE = "audio"
            private const val SEARCH_ROWS = 20
            private const val METADATA_CAP = 8
            private const val METADATA_PARALLELISM = 4
            private const val TRENDING_QUERY = "mediatype:audio AND collection:opensource_audio"
        }
    }
