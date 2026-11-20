@file:Suppress("ForbiddenImport", "TooManyFunctions", "LongParameterList")

package com.clibeats.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clibeats.data.backup.LibrarySnapshotManager
import com.clibeats.data.cache.CacheManager
import com.clibeats.data.cache.CacheUsage
import com.clibeats.data.cache.MediaCaches
import com.clibeats.data.playlist.PlaylistExchangeManager
import com.clibeats.data.preferences.AppPreferences
import com.clibeats.data.scan.LocalMusicScanner
import com.clibeats.data.scan.ScanStatus
import com.clibeats.domain.provider.ProviderRegistry
import com.clibeats.domain.repository.SongRepository
import com.clibeats.presentation.theme.AccentColor
import com.clibeats.presentation.theme.CliBeatsThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val appPreferences: AppPreferences,
        private val cacheManager: CacheManager,
        private val providerRegistry: ProviderRegistry,
        private val playlistExchangeManager: PlaylistExchangeManager,
        private val songRepository: SongRepository,
        private val snapshotManager: LibrarySnapshotManager? = null,
        private val localMusicScanner: LocalMusicScanner? = null,
        private val mediaCaches: MediaCaches? = null,
    ) : ViewModel() {
        val cacheUsage: StateFlow<CacheUsage> = mediaCaches?.usage ?: MutableStateFlow(CacheUsage())

        val artworkCacheMb: StateFlow<Int> =
            appPreferences.artworkCacheMb.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), 128)

        init {
            viewModelScope.launch { runCatching { mediaCaches?.refreshUsage() } }
        }

        fun setSongCacheMb(mb: Int) {
            viewModelScope.launch {
                appPreferences.setCacheMaxMb(mb)
                cacheManager.maxCacheSizeBytes = mb * BYTES_IN_MB
                runCatching { cacheManager.evictLruIfNeeded() }
                runCatching { mediaCaches?.setSongLimitMb(mb) }
            }
        }

        fun setArtworkCacheMb(mb: Int) {
            viewModelScope.launch {
                appPreferences.setArtworkCacheMb(mb)
                runCatching { mediaCaches?.setArtworkLimitMb(mb) }
            }
        }

        fun clearSongCache() {
            viewModelScope.launch {
                runCatching { cacheManager.clearAllCache() }
                runCatching { mediaCaches?.clearSongs() }
            }
        }

        fun clearArtworkCache() {
            viewModelScope.launch { runCatching { mediaCaches?.clearArtwork() } }
        }

        val autoQueueRelated: StateFlow<Boolean> =
            appPreferences.autoQueueRelated
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

        val localMusicFolders: StateFlow<List<String>> =
            appPreferences.localMusicTreeUris
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

        val scanStatus: StateFlow<ScanStatus> = localMusicScanner?.status ?: MutableStateFlow(ScanStatus.Idle)

        /** Songs in the local library, read from the database so it's right after a restart too. */
        val localTrackCount: StateFlow<Int> =
            songRepository.getLocalTrackCountAsFlow()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), 0)

        fun setAutoQueueRelated(enabled: Boolean) {
            viewModelScope.launch { appPreferences.setAutoQueueRelated(enabled) }
        }

        /** Adds a picked folder (permission already persisted by the screen) and rescans all folders. */
        fun onLocalFolderPicked(treeUri: Uri) {
            viewModelScope.launch {
                appPreferences.addLocalMusicTreeUri(treeUri.toString())
                scanAllFolders()
            }
        }

        /** Removes a folder (the screen releases its permission); its songs disappear on the rescan. */
        fun removeLocalFolder(uri: String) {
            viewModelScope.launch {
                appPreferences.removeLocalMusicTreeUri(uri)
                scanAllFolders()
            }
        }

        fun rescanLocalMusic() {
            viewModelScope.launch { scanAllFolders() }
        }

        private suspend fun scanAllFolders() {
            val folders = appPreferences.localMusicTreeUris.first().map(Uri::parse)
            runCatching { localMusicScanner?.scan(folders) }
        }

        private data class RawSettings(
            val providerId: String?,
            val cacheMaxMb: Int,
            val highQualityStreaming: Boolean,
            val themeModeName: String,
            val accentColorName: String,
        )

        val uiState: StateFlow<SettingsUiState> =
            combine(
                appPreferences.activeProviderId,
                appPreferences.cacheMaxMb,
                appPreferences.highQualityStreaming,
                appPreferences.themeMode,
                appPreferences.accentColorName,
            ) { providerId, cacheMaxMb, highQuality, modeName, accentName ->
                RawSettings(providerId, cacheMaxMb, highQuality, modeName, accentName)
            }.combine(cacheManager.getAllCachedAsFlow()) { raw, cachedEntities ->
                SettingsUiState(
                    activeProviderId = raw.providerId ?: ProviderRegistry.DEFAULT_PROVIDER_ID,
                    providers =
                        providerRegistry.providers.map { provider ->
                            ProviderOption(provider.providerId, provider.displayName)
                        },
                    cacheMaxMb = raw.cacheMaxMb,
                    highQualityStreaming = raw.highQualityStreaming,
                    currentCacheSizeBytes = cachedEntities.sumOf { it.fileSizeBytes },
                    themeMode =
                        CliBeatsThemeMode.entries.firstOrNull { it.name == raw.themeModeName }
                            ?: CliBeatsThemeMode.DARK,
                    accentColor =
                        AccentColor.entries.firstOrNull { it.name == raw.accentColorName }
                            ?: AccentColor.GREEN,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = SettingsUiState(),
            )

        private val _exchangeMessage = MutableStateFlow<String?>(null)
        val exchangeMessage: StateFlow<String?> = _exchangeMessage.asStateFlow()

        fun setActiveProvider(providerId: String) {
            viewModelScope.launch {
                appPreferences.setActiveProviderId(providerId)
            }
        }

        fun setHighQualityStreaming(enabled: Boolean) {
            viewModelScope.launch {
                appPreferences.setHighQualityStreaming(enabled)
            }
        }

        fun setThemeMode(mode: CliBeatsThemeMode) {
            viewModelScope.launch {
                appPreferences.setThemeMode(mode.name)
            }
        }

        fun setAccentColor(accent: AccentColor) {
            viewModelScope.launch {
                appPreferences.setAccentColorName(accent.name)
            }
        }

        fun exportPlaylists(target: Uri) {
            viewModelScope.launch {
                _exchangeMessage.value =
                    playlistExchangeManager.export(target).fold(
                        onSuccess = { count -> "Exported $count playlist(s)" },
                        onFailure = { e -> "Export failed: ${e.message}" },
                    )
            }
        }

        fun importPlaylists(source: Uri) {
            viewModelScope.launch {
                _exchangeMessage.value =
                    playlistExchangeManager.import(source).fold(
                        onSuccess = { count -> "Imported $count track(s)" },
                        onFailure = { e -> "Import failed: ${e.message}" },
                    )
            }
        }

        fun clearExchangeMessage() {
            _exchangeMessage.value = null
        }

        /** File handed to the share sheet; cleared once the screen has launched the chooser. */
        private val _shareFile = MutableStateFlow<File?>(null)
        val shareFile: StateFlow<File?> = _shareFile.asStateFlow()

        /** Takes a fresh snapshot and exposes it for ACTION_SEND (Drive, email, nearby, ...). */
        fun shareSnapshot() {
            val manager = snapshotManager ?: return
            viewModelScope.launch {
                manager.snapshotIfDue(force = true)
                val shared = manager.prepareShareFile()
                _shareFile.value = shared
                if (shared == null) {
                    _exchangeMessage.value =
                        "Nothing to share yet: save a song or create a playlist first"
                }
            }
        }

        fun clearShareFile() {
            _shareFile.value = null
        }

        val anonymousUsageOptIn: StateFlow<Boolean> =
            appPreferences.anonymousUsageOptIn.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000L),
                false,
            )

        fun setAnonymousUsageOptIn(optIn: Boolean) {
            viewModelScope.launch {
                appPreferences.setAnonymousUsageOptIn(optIn)
            }
        }

        companion object {
            private const val BYTES_IN_MB = 1048576L
        }
    }
