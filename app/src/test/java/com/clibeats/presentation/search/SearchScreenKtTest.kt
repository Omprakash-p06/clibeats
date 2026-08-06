package com.clibeats.presentation.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SearchScreenKtTest {
    @Test
    fun `formatDuration returns dash for zero duration`() {
        assertThat(formatDuration(0L)).isEqualTo("–")
    }

    @Test
    fun `formatDuration returns dash for negative duration`() {
        assertThat(formatDuration(-1L)).isEqualTo("–")
    }

    @Test
    fun `formatDuration formats minutes and seconds correctly`() {
        assertThat(formatDuration(259_000L)).isEqualTo("4:19")
    }

    @Test
    fun `formatDuration pads seconds with leading zero`() {
        assertThat(formatDuration(185_000L)).isEqualTo("3:05")
    }

    @Test
    fun `formatDuration formats hours correctly`() {
        assertThat(formatDuration(3_661_000L)).isEqualTo("1:01:01")
    }
}
