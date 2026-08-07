package com.clibeats.di

import com.clibeats.telemetry.CrashReporter
import com.clibeats.telemetry.TelemetryTracker
import com.clibeats.telemetry.TimberCrashReporter
import com.clibeats.telemetry.TimberTelemetryTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TelemetryModule {
    @Binds
    @Singleton
    abstract fun bindTelemetryTracker(impl: TimberTelemetryTracker): TelemetryTracker

    @Binds
    @Singleton
    abstract fun bindCrashReporter(impl: TimberCrashReporter): CrashReporter
}
