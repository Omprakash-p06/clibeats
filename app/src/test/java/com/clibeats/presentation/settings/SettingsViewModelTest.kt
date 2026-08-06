@file:Suppress("ForbiddenImport")

package com.clibeats.presentation.settings

import com.clibeats.data.cache.CacheManager
import com.clibeats.data.preferences.AppPreferences
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appPreferences: AppPreferences
    private lateinit var cacheManager: CacheManager
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        appPreferences = mock()
        cacheManager = mock()

        whenever(appPreferences.activeProviderId).thenReturn(flowOf("ytmusic"))
        whenever(appPreferences.cacheMaxMb).thenReturn(flowOf(512))
        whenever(appPreferences.highQualityStreaming).thenReturn(flowOf(true))
        whenever(appPreferences.authToken).thenReturn(flowOf(null))
        whenever(cacheManager.getAllCachedAsFlow()).thenReturn(flowOf(emptyList()))

        viewModel = SettingsViewModel(appPreferences, cacheManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects preferences`() =
        runTest(testDispatcher) {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = viewModel.uiState.value
            assertThat(state.activeProviderId).isEqualTo("ytmusic")
            assertThat(state.cacheMaxMb).isEqualTo(512)
            assertThat(state.highQualityStreaming).isTrue()
        }

    @Test
    fun `setActiveProvider delegates to appPreferences`() =
        runTest(testDispatcher) {
            viewModel.setActiveProvider("local")
            testDispatcher.scheduler.advanceUntilIdle()
            verify(appPreferences).setActiveProviderId("local")
        }

    @Test
    fun `setCacheMaxMb delegates to appPreferences and cacheManager`() =
        runTest(testDispatcher) {
            viewModel.setCacheMaxMb(1024)
            testDispatcher.scheduler.advanceUntilIdle()
            verify(appPreferences).setCacheMaxMb(1024)
        }
}
