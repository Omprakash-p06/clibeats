package com.clibeats.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LrcParserTest {
    @Test
    fun `parse standard two-digit hundredths timestamp`() {
        val lrc =
            """
            [00:12.34] First line
            [00:15.67] Second line
            [01:02.50] Third line
            """.trimIndent()

        val parsed = LrcParser.parse(lrc)

        assertThat(parsed).hasSize(3)
        assertThat(parsed[0].timeMs).isEqualTo(12_340L)
        assertThat(parsed[0].text).isEqualTo("First line")
        assertThat(parsed[1].timeMs).isEqualTo(15_670L)
        assertThat(parsed[1].text).isEqualTo("Second line")
        assertThat(parsed[2].timeMs).isEqualTo(62_500L)
        assertThat(parsed[2].text).isEqualTo("Third line")
    }

    @Test
    fun `parse three-digit milliseconds timestamp`() {
        val lrc =
            """
            [00:05.123] High precision line
            [01:30.500] Minute and half
            """.trimIndent()

        val parsed = LrcParser.parse(lrc)

        assertThat(parsed).hasSize(2)
        assertThat(parsed[0].timeMs).isEqualTo(5_123L)
        assertThat(parsed[0].text).isEqualTo("High precision line")
        assertThat(parsed[1].timeMs).isEqualTo(90_500L)
    }

    @Test
    fun `lines without timestamps or empty lines are ignored`() {
        val lrc =
            """
            [ar: Test Artist]
            [ti: Test Title]

            [00:10.00] Actual lyric line
            Some random comment
            """.trimIndent()

        val parsed = LrcParser.parse(lrc)

        assertThat(parsed).hasSize(1)
        assertThat(parsed[0].timeMs).isEqualTo(10_000L)
        assertThat(parsed[0].text).isEqualTo("Actual lyric line")
    }

    @Test
    fun `unordered timestamps are sorted by timeMs`() {
        val lrc =
            """
            [00:20.00] Line two
            [00:05.00] Line one
            [00:35.00] Line three
            """.trimIndent()

        val parsed = LrcParser.parse(lrc)

        assertThat(parsed).hasSize(3)
        assertThat(parsed[0].timeMs).isEqualTo(5_000L)
        assertThat(parsed[0].text).isEqualTo("Line one")
        assertThat(parsed[1].timeMs).isEqualTo(20_000L)
        assertThat(parsed[2].timeMs).isEqualTo(35_000L)
    }
}
