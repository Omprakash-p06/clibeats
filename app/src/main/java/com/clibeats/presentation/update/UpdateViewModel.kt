// ForbiddenImport: the updater is app infrastructure with no domain model; same pattern as SettingsViewModel.
@file:Suppress("ForbiddenImport")

package com.clibeats.presentation.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clibeats.data.update.UpdateFrequency
import com.clibeats.data.update.UpdateManager
import com.clibeats.data.update.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel
    @Inject
    constructor(
        private val updateManager: UpdateManager,
    ) : ViewModel() {
        val state: StateFlow<UpdateState> = updateManager.state
        val promptVisible: StateFlow<Boolean> = updateManager.promptVisible
        val frequency: StateFlow<UpdateFrequency> =
            updateManager.frequency.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000L),
                UpdateFrequency.DAILY,
            )
        val installedVersion: String = updateManager.installedVersion

        fun onAppStart() = updateManager.onAppStart()

        fun checkNow() = updateManager.checkNow()

        fun setFrequency(frequency: UpdateFrequency) = updateManager.setFrequency(frequency)

        fun download(fromPrompt: Boolean) {
            updateManager.startedFromPrompt = fromPrompt
            updateManager.download()
        }

        fun install() = updateManager.install()

        fun later() = updateManager.later()

        fun hidePrompt() = updateManager.hidePrompt()

        fun showPrompt() = updateManager.showPrompt()
    }
