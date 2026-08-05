package com.clibeats.data.preferences

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
// Indentation: detekt 1.23.6 misparses ktlint_official @Inject constructor() style (false positive).
@Suppress("Indentation")
class AppPreferences
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        // Injected via StorageModule as EncryptedSharedPreferences (Android Keystore MasterKey, AES256_GCM).
        // Keystore-backed encryption for sensitive credentials only; non-sensitive settings stay in DataStore.
        private val securePrefs: SharedPreferences,
    ) {
        private object Keys {
            val ACTIVE_PROVIDER_ID = stringPreferencesKey("active_provider_id")
            val CACHE_MAX_MB = intPreferencesKey("cache_max_mb")
            val HIGH_QUALITY_STREAMING = booleanPreferencesKey("high_quality_streaming")
        }

        private object SecureKeys {
            const val AUTH_TOKEN = "auth_token"
        }

        val activeProviderId: Flow<String?> =
            dataStore.data.map { prefs ->
                prefs[Keys.ACTIVE_PROVIDER_ID]
            }

        val cacheMaxMb: Flow<Int> =
            dataStore.data.map { prefs ->
                prefs[Keys.CACHE_MAX_MB] ?: 512
            }

        val highQualityStreaming: Flow<Boolean> =
            dataStore.data.map { prefs ->
                prefs[Keys.HIGH_QUALITY_STREAMING] ?: true
            }

        private val _authToken = MutableStateFlow(securePrefs.getString(SecureKeys.AUTH_TOKEN, null))

        val authToken: Flow<String?> = _authToken.asStateFlow()

        private val prefsListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == SecureKeys.AUTH_TOKEN) {
                    _authToken.value = securePrefs.getString(SecureKeys.AUTH_TOKEN, null)
                }
            }

        init {
            securePrefs.registerOnSharedPreferenceChangeListener(prefsListener)
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
            securePrefs.edit().putString(SecureKeys.AUTH_TOKEN, token).apply()
            _authToken.value = token
        }

        suspend fun clearAuthToken() {
            securePrefs.edit().remove(SecureKeys.AUTH_TOKEN).apply()
            _authToken.value = null
        }
    }
