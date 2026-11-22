package com.clibeats.domain.model

object LrcParser {
    private val TIMESTAMP_REGEX = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\]""")
    private const val MS_PER_SEC = 1000L
    private const val MS_PER_MIN = 60_000L
    private const val TWO_DIGIT_FRAC_MULTIPLIER = 10L
    private const val TWO_DIGIT_LENGTH = 2
    private const val GROUP_MIN = 1
    private const val GROUP_SEC = 2
    private const val GROUP_FRAC = 3

    @Suppress("LoopWithTooManyJumpStatements")
    fun parse(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        for (rawLine in lrcText.lineSequence()) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue
            val match = TIMESTAMP_REGEX.find(trimmed) ?: continue
            val min = match.groupValues[GROUP_MIN].toLongOrNull() ?: continue
            val sec = match.groupValues[GROUP_SEC].toLongOrNull() ?: continue
            val fracStr = match.groupValues[GROUP_FRAC]
            val ms =
                if (fracStr.length == TWO_DIGIT_LENGTH) {
                    (fracStr.toLongOrNull() ?: 0L) * TWO_DIGIT_FRAC_MULTIPLIER
                } else {
                    fracStr.toLongOrNull() ?: 0L
                }
            val totalMs = min * MS_PER_MIN + sec * MS_PER_SEC + ms
            val lyricText = trimmed.substring(match.range.last + 1).trim()
            lines.add(LyricLine(timeMs = totalMs, text = lyricText))
        }
        return lines.sortedBy { it.timeMs }
    }
}
