package com.clibeats

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CLIBeatsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("CLIBeatsApp", "Resolved GATEWAY_BASE_URL: ${BuildConfig.GATEWAY_BASE_URL}")
    }
}
