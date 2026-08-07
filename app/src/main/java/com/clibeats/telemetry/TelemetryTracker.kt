package com.clibeats.telemetry

interface TelemetryTracker {
    fun trackEvent(event: AnalyticsEvent)
}
