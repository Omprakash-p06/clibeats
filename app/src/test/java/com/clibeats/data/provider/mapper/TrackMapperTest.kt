package com.clibeats.data.provider.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrackMapperTest {
    @Test
    fun `parseDurationMs parses mm-ss format correctly`() {
        assertThat(parseDurationMs("4:19")).isEqualTo(259_000L)
    }

    @Test
    fun `parseDurationMs parses hh-mm-ss format correctly`() {
        assertThat(parseDurationMs("1:00:00")).isEqualTo(3_600_000L)
    }

    @Test
    fun `parseDurationMs returns 0 for empty string`() {
        assertThat(parseDurationMs("")).isEqualTo(0L)
    }

    @Test
    fun `parseDurationMs returns 0 for zero duration`() {
        assertThat(parseDurationMs("0:00")).isEqualTo(0L)
    }

    @Test
    fun `parseDurationMs handles single-digit seconds`() {
        assertThat(parseDurationMs("3:05")).isEqualTo(185_000L)
    }

    @Test
    fun `parseDurationMs returns 0 for malformed input`() {
        assertThat(parseDurationMs("abc")).isEqualTo(0L)
    }
}
