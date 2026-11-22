// ForbiddenImport: data-layer self-imports are legitimate
@file:Suppress("ForbiddenImport")

package com.clibeats.data.repository

import com.clibeats.data.local.dao.LyricsCacheDao
import com.clibeats.data.local.entity.LyricsCacheEntity
import com.clibeats.data.lyrics.LrcLibApi
import com.clibeats.domain.model.LrcParser
import com.clibeats.domain.model.LyricLine
import com.clibeats.domain.model.Track
import com.clibeats.domain.repository.LyricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepositoryImpl
    @Inject
    constructor(
        private val lrcLibApi: LrcLibApi,
        private val lyricsCacheDao: LyricsCacheDao,
    ) : LyricsRepository {
        override suspend fun getLyrics(track: Track): Result<List<LyricLine>> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val cached = lyricsCacheDao.getById(track.id)
                    if (cached != null) {
                        return@runCatching cached.syncedLrc?.let { LrcParser.parse(it) }.orEmpty()
                    }

                    val durationSec = (track.durationMs / MS_PER_SEC).toInt().takeIf { it > 0 }
                    val cleanTitle = cleanTrackTitle(track.title)

                    val resp =
                        runCatching {
                            lrcLibApi.getLyrics(
                                trackName = cleanTitle,
                                artistName = track.artist,
                                albumName = track.album.ifBlank { null },
                                duration = durationSec,
                            )
                        }.getOrNull()

                    val synced = resp?.syncedLyrics
                    lyricsCacheDao.insert(
                        LyricsCacheEntity(
                            songId = track.id,
                            syncedLrc = synced,
                            plainLyrics = resp?.plainLyrics,
                            fetchedAt = System.currentTimeMillis(),
                        ),
                    )

                    if (!synced.isNullOrBlank()) {
                        LrcParser.parse(synced)
                    } else {
                        emptyList()
                    }
                }
            }

        private fun cleanTrackTitle(title: String): String =
            title.replace(
                Regex(
                    """\s*[\(\[](official|audio|video|lyric|remastered|feat\.).*?[\)\]]""",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            ).trim()

        private companion object {
            const val MS_PER_SEC = 1000L
        }
    }
