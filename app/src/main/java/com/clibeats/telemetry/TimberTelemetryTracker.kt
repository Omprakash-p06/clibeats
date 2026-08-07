package com.clibeats.telemetry

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimberTelemetryTracker
    @Inject
    constructor() : TelemetryTracker {
        override fun trackEvent(event: AnalyticsEvent) {
            Log.d("CLIBeatsTelemetry", "event=${event.name} params=${event.params}")
        }
    }
