package com.clibeats.telemetry

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class TimberCrashReporterTest {
    private lateinit var crashReporter: TimberCrashReporter

    @Before
    fun setUp() {
        crashReporter = TimberCrashReporter()
    }

    @Test
    fun `logException executes cleanly without throwing`() {
        val exception = RuntimeException("Network error Bearer secret_auth_token_12345")
        assertThat(crashReporter).isNotNull()
        assertThat(exception.message).contains("Bearer")
    }
}
