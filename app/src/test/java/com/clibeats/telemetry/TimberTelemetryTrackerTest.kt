package com.clibeats.telemetry

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class TimberTelemetryTrackerTest {
    private lateinit var tracker: TimberTelemetryTracker

    @Before
    fun setUp() {
        tracker = TimberTelemetryTracker()
    }

    @Test
    fun `trackEvent logs analytics event cleanly`() {
        val event = AnalyticsEvent.PlaybackStarted(trackId = "t1", providerId = "ytmusic")
        assertThat(event.name).isEqualTo("playback_started")
        assertThat(event.params["track_id"]).isEqualTo("t1")
    }
}
