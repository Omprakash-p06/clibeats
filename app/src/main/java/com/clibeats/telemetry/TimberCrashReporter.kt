package com.clibeats.telemetry

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimberCrashReporter
    @Inject
    constructor() : CrashReporter {
        override fun logException(
            throwable: Throwable,
            message: String?,
        ) {
            val sanitizedMsg = message?.replace(Regex("Bearer\\s+[A-Za-z0-9._-]+"), "Bearer [REDACTED]")
            Log.e("CLIBeatsCrash", sanitizedMsg ?: throwable.localizedMessage ?: "Unknown error", throwable)
        }
    }
