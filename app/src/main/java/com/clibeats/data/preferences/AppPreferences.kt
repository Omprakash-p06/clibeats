package com.clibeats.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val ACTIVE_PROVIDER_ID = stringPreferencesKey("active_provider_id")
        val CACHE_MAX_MB = intPreferencesKey("cache_max_mb")
        val HIGH_QUALITY_STREAMING = booleanPreferencesKey("high_quality_streaming")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
    }

    val activeProviderId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_PROVIDER_ID]
    }

    val cacheMaxMb: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.CACHE_MAX_MB] ?: 512
    }

    val highQualityStreaming: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.HIGH_QUALITY_STREAMING] ?: true
    }

    val authToken: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.AUTH_TOKEN]
    }

    suspend fun setActiveProviderId(providerId: String) {
        dataStore.edit { it[Keys.ACTIVE_PROVIDER_ID] = providerId }
    }

    suspend fun setCacheMaxMb(maxMb: Int) {
        dataStore.edit { it[Keys.CACHE_MAX_MB] = maxMb }
    }

    suspend fun setHighQualityStreaming(enabled: Boolean) {
        dataStore.edit { it[Keys.HIGH_QUALITY_STREAMING] = enabled }
    }

    suspend fun setAuthToken(token: String) {
        dataStore.edit { it[Keys.AUTH_TOKEN] = token }
    }

    suspend fun clearAuthToken() {
        dataStore.edit { it.remove(Keys.AUTH_TOKEN) }
    }
}
