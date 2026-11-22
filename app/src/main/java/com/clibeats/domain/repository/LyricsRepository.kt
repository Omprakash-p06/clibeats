package com.clibeats.domain.repository

import com.clibeats.domain.model.LyricLine
import com.clibeats.domain.model.Track

interface LyricsRepository {
    suspend fun getLyrics(track: Track): Result<List<LyricLine>>
}
