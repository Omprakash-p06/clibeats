package com.clibeats.telemetry

interface CrashReporter {
    fun logException(
        throwable: Throwable,
        message: String? = null,
    )
}
