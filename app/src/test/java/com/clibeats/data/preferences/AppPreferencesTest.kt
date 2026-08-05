// ForbiddenImport: data-layer test legitimately imports sibling data packages; Phase 0 pattern is over-broad.
@file:Suppress("ForbiddenImport", "MaxLineLength")

package com.clibeats.data.preferences

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class AppPreferencesTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var securePrefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var appPreferences: AppPreferences
    private var tempFile: File? = null

    @Before
    fun setup() {
        editor = mock()
        whenever(editor.putString(anyString(), anyString())).thenReturn(editor)
        whenever(editor.remove(anyString())).thenReturn(editor)
        securePrefs = mock()
        whenever(securePrefs.edit()).thenReturn(editor)
        whenever(securePrefs.getString(anyString(), any())).thenReturn(null)
        dataStore = createTestDataStore()
        appPreferences = AppPreferences(dataStore, securePrefs)
    }

    @After
    fun tearDown() {
        tempFile?.delete()
    }

    private fun createTestDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = CoroutineScope(UnconfinedTestDispatcher() + Job())) {
            File.createTempFile("clibeats_test", ".preferences_pb").also { tempFile = it }
        }

    @Test
    fun authToken_returnsValueStoredInSecurePrefs() =
        runTest {
            whenever(securePrefs.getString("auth_token", null)).thenReturn("tok-123")

            val prefs = AppPreferences(dataStore, securePrefs)

            assertEquals("tok-123", prefs.authToken.first())
        }

    @Test
    fun authToken_returnsNullWhenNoTokenStored() =
        runTest {
            assertNull(appPreferences.authToken.first())
        }

    @Test
    fun setAuthToken_writesToSecurePrefsAndEmits() =
        runTest {
            appPreferences.setAuthToken("tok-1")

            verify(editor).putString("auth_token", "tok-1")
            verify(editor).apply()
            assertEquals("tok-1", appPreferences.authToken.first())
        }

    @Test
    fun clearAuthToken_removesFromSecurePrefsAndEmitsNull() =
        runTest {
            appPreferences.clearAuthToken()

            verify(editor).remove("auth_token")
            verify(editor).apply()
            assertNull(appPreferences.authToken.first())
        }

    @Test
    fun cacheMaxMb_defaultsTo512() =
        runTest {
            assertEquals(512, appPreferences.cacheMaxMb.first())
        }

    @Test
    fun setCacheMaxMb_roundTripsThroughDataStore() =
        runTest {
            appPreferences.setCacheMaxMb(256)

            assertEquals(256, appPreferences.cacheMaxMb.first())
        }
}
