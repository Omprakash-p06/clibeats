// ForbiddenImport: the detekt rule is meant for the presentation layer; data classes may use data.
@file:Suppress("ForbiddenImport")

package com.clibeats.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.clibeats.BuildConfig
import com.clibeats.data.preferences.AppPreferences
import com.clibeats.data.preferences.NO_DOWNLOAD
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-app updater backed by GitHub Releases.
 *
 * - On launch it checks at most once per [UpdateFrequency] interval; failures stay silent.
 * - The APK is downloaded by the system DownloadManager into Downloads (survives leaving the app),
 *   the previous update APK it downloaded is deleted, and the file must match the release's
 *   `SHA256SUMS.txt` before it can be installed.
 * - Installing hands the file to Android's package installer ("Do you want to update this app?").
 */
@Suppress("TooManyFunctions", "LargeClass")
@Singleton
class UpdateManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val client: GitHubReleaseClient,
        private val preferences: AppPreferences,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val checkMutex = Mutex()
        private val started = AtomicBoolean(false)
        private var pollJob: Job? = null
        private var periodicCheckJob: Job? = null
        private val downloadManager: DownloadManager? = context.getSystemService(DownloadManager::class.java)

        private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
        val state: StateFlow<UpdateState> = _state.asStateFlow()

        /** True while the launch pop-up should be on screen. */
        private val _promptVisible = MutableStateFlow(false)
        val promptVisible: StateFlow<Boolean> = _promptVisible.asStateFlow()

        val frequency: Flow<UpdateFrequency> = preferences.updateFrequency.map(::frequencyOf)

        val installedVersion: String = BuildConfig.VERSION_NAME

        /** Called once per app launch: restores a known update, checks GitHub immediately, then polls periodically. */
        fun onAppStart() {
            if (!started.compareAndSet(false, true)) return
            scope.launch {
                var record = preferences.updateRecord()
                // The update was installed: delete its APK from Downloads.
                if (record.downloadId != NO_DOWNLOAD &&
                    UpdateRules.isLeftoverDownload(record.downloadVersion, installedVersion)
                ) {
                    removeDownload(record.downloadId)
                    record = preferences.updateRecord()
                }
                val frequency = frequencyOf(record.frequency)
                if (frequency == UpdateFrequency.OFF) return@launch
                val cached = UpdateRules.decode(record.cachedReleaseJson)
                if (cached != null && UpdateRules.isNewer(cached.version, installedVersion)) {
                    present(cached, prompt = true)
                }
                check(userInitiated = false)
                startPeriodicCheck()
            }
        }

        private fun startPeriodicCheck() {
            periodicCheckJob?.cancel()
            periodicCheckJob =
                scope.launch {
                    while (isActive) {
                        delay(PERIODIC_CHECK_INTERVAL_MS)
                        val frequency = frequencyOf(preferences.updateRecord().frequency)
                        if (frequency != UpdateFrequency.OFF) {
                            check(userInitiated = false)
                        }
                    }
                }
        }

        /** "Check now" in Settings: always hits GitHub and reports errors. */
        fun checkNow() {
            scope.launch { check(userInitiated = true) }
        }

        fun setFrequency(frequency: UpdateFrequency) {
            scope.launch { preferences.setUpdateFrequency(frequency.name) }
            if (frequency == UpdateFrequency.OFF) _promptVisible.value = false
        }

        /** "Later": hide the pop-up for a day. Settings → Updates still works meanwhile. */
        fun later() {
            _promptVisible.value = false
            scope.launch { preferences.setUpdateSnoozeUntil(System.currentTimeMillis() + DAY_MS) }
        }

        /** Hides the pop-up without snoozing (e.g. while the download continues in the background). */
        fun hidePrompt() {
            _promptVisible.value = false
        }

        /** Brings up the update prompt dialog if an update is available or in progress. */
        fun showPrompt() {
            if (releaseOf(_state.value) != null) {
                _promptVisible.value = true
            }
        }

        fun download() {
            val release = releaseOf(_state.value) ?: return
            if (_state.value is UpdateState.Downloading || _state.value is UpdateState.Verifying) return
            scope.launch { startDownload(release) }
        }

        /**
         * Opens Android's installer for the verified APK. If CLIBeats isn't yet allowed to install apps,
         * opens that settings page first; the user comes back and taps Install again.
         */
        fun install() {
            val ready = _state.value as? UpdateState.ReadyToInstall ?: return
            scope.launch {
                val record = preferences.updateRecord()
                val uri =
                    record.downloadId
                        .takeIf { it != NO_DOWNLOAD }
                        ?.let { runCatching { downloadManager?.getUriForDownloadedFile(it) }.getOrNull() }
                if (uri == null) {
                    preferences.clearUpdateDownload()
                    _state.value = UpdateState.Failed("The downloaded file is gone. Download it again.", ready.release)
                    return@launch
                }
                if (!context.packageManager.canRequestPackageInstalls()) {
                    launch(
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")),
                    )
                    return@launch
                }
                val installIntent =
                    Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, APK_MIME_TYPE)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                launch(installIntent)
            }
        }

        private suspend fun check(userInitiated: Boolean) {
            checkMutex.withLock {
                val current = _state.value
                if (current is UpdateState.Downloading || current is UpdateState.Verifying) return
                if (userInitiated) _state.value = UpdateState.Checking
                val record = preferences.updateRecord()
                val cached = UpdateRules.decode(record.cachedReleaseJson)
                val now = System.currentTimeMillis()
                // Only send the ETag when we still hold the release it describes.
                val etag = record.etag.takeIf { cached != null }
                when (val result = client.fetchLatest(etag)) {
                    is GitHubReleaseClient.Result.NotModified -> {
                        preferences.setUpdateLastCheckAt(now)
                        evaluate(cached, userInitiated)
                    }
                    is GitHubReleaseClient.Result.Found -> {
                        preferences.saveUpdateCheck(now, result.etag, result.release?.let(UpdateRules::encode))
                        evaluate(result.release, userInitiated)
                    }
                    is GitHubReleaseClient.Result.Failed ->
                        if (userInitiated) {
                            _state.value = UpdateState.Failed("Couldn't check for updates (${result.message})")
                        }
                }
            }
        }

        private suspend fun evaluate(
            release: ReleaseInfo?,
            userInitiated: Boolean,
        ) {
            if (release != null && UpdateRules.isNewer(release.version, installedVersion)) {
                val record = preferences.updateRecord()
                val cached = UpdateRules.decode(record.cachedReleaseJson)
                if (cached == null || UpdateRules.isNewer(release.version, cached.version) || userInitiated) {
                    preferences.setUpdateSnoozeUntil(0L)
                }
                present(release, prompt = true)
            } else {
                _promptVisible.value = false
                _state.value = if (userInitiated) UpdateState.UpToDate else UpdateState.Idle
            }
        }

        /** Shows [release], resuming a download that already exists for it. */
        private suspend fun present(
            release: ReleaseInfo,
            prompt: Boolean,
        ) {
            val record = preferences.updateRecord()
            val existing =
                record.downloadId.takeIf { it != NO_DOWNLOAD && record.downloadVersion == release.version }
            val status = existing?.let(::queryDownload)
            when {
                existing == null || status == null -> _state.value = UpdateState.Available(release)
                status.status == DownloadManager.STATUS_SUCCESSFUL -> verify(release, existing, record.downloadSha256)
                status.status == DownloadManager.STATUS_FAILED -> {
                    removeDownload(existing)
                    _state.value = UpdateState.Available(release)
                }
                else -> {
                    _state.value = UpdateState.Downloading(release, status.progress)
                    poll(release, existing)
                }
            }
            if (prompt && !UpdateRules.isSnoozed(System.currentTimeMillis(), record.snoozeUntil)) {
                _promptVisible.value = true
            }
        }

        @Suppress("ReturnCount")
        private suspend fun startDownload(release: ReleaseInfo) {
            val manager = downloadManager
            if (manager == null) {
                _state.value = UpdateState.Failed("Downloads aren't available on this device", release)
                return
            }
            _state.value = UpdateState.Downloading(release, UNKNOWN_PROGRESS)
            val checksums = release.checksumsUrl?.let { client.fetchText(it) }
            val expected = checksums?.let { UpdateRules.sha256For(it, release.apkName) }
            if (expected == null) {
                _state.value = UpdateState.Failed(NO_CHECKSUM_MESSAGE, release)
                return
            }
            // Keep only one update APK: delete the one we downloaded last time.
            preferences.updateRecord().downloadId.takeIf { it != NO_DOWNLOAD }?.let { removeDownload(it) }

            val id =
                runCatching {
                    val request =
                        DownloadManager
                            .Request(Uri.parse(release.apkUrl))
                            .setTitle("CLIBeats v${release.version}")
                            .setDescription("Downloading update")
                            .setMimeType(APK_MIME_TYPE)
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, release.apkName)
                    } else {
                        // Android 8–9 need a storage permission for public Downloads; use the app's own folder.
                        val downloads = Environment.DIRECTORY_DOWNLOADS
                        request.setDestinationInExternalFilesDir(context, downloads, release.apkName)
                    }
                    manager.enqueue(request)
                }.getOrElse { e ->
                    _state.value = UpdateState.Failed("Couldn't start the download (${e.message})", release)
                    return
                }
            preferences.saveUpdateDownload(id, release.version, expected)
            poll(release, id)
        }

        private fun poll(
            release: ReleaseInfo,
            id: Long,
        ) {
            pollJob?.cancel()
            pollJob =
                scope.launch {
                    while (isActive) {
                        val status = queryDownload(id)
                        when (status?.status) {
                            null -> {
                                // Cancelled from the notification or the Downloads app.
                                preferences.clearUpdateDownload()
                                _state.value = UpdateState.Available(release)
                                return@launch
                            }
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                verify(release, id, preferences.updateRecord().downloadSha256)
                                return@launch
                            }
                            DownloadManager.STATUS_FAILED -> {
                                removeDownload(id)
                                _state.value = UpdateState.Failed(DOWNLOAD_FAILED_MESSAGE, release)
                                return@launch
                            }
                            else -> _state.value = UpdateState.Downloading(release, status.progress)
                        }
                        delay(POLL_INTERVAL_MS)
                    }
                }
        }

        private suspend fun verify(
            release: ReleaseInfo,
            id: Long,
            expectedSha256: String?,
        ) {
            _state.value = UpdateState.Verifying(release)
            val actual =
                runCatching {
                    val uri = downloadManager?.getUriForDownloadedFile(id) ?: return@runCatching null
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val digest = MessageDigest.getInstance("SHA-256")
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            digest.update(buffer, 0, read)
                        }
                        digest.digest().joinToString("") { "%02x".format(it) }
                    }
                }.getOrNull()
            val signerError = if (actual != null && actual == expectedSha256) verifySigner(id) else null
            if (actual != null && actual == expectedSha256 && signerError == null) {
                _state.value = UpdateState.ReadyToInstall(release)
                // Bring back the prompt so the user is given the immediate choice to install.
                _promptVisible.value = true
            } else {
                removeDownload(id)
                _state.value =
                    UpdateState.Failed(signerError ?: DAMAGED_MESSAGE, release)
            }
            startedFromPrompt = false
        }

        /**
         * Android refuses an in-place update signed with a different certificate, which forces the
         * user to uninstall and lose their library. Compare the downloaded APK's signer with the
         * installed app's before offering INSTALL. Null means "matches / safe to install".
         */
        private fun verifySigner(id: Long): String? =
            runCatching {
                val uri = downloadManager?.getUriForDownloadedFile(id) ?: return@runCatching SIGNING_UNKNOWN_MESSAGE
                val temp = File(context.cacheDir, SIGNER_CHECK_FILE)
                try {
                    val copied =
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            temp.outputStream().use { output -> input.copyTo(output) }
                        }
                    if (copied == null || temp.length() == 0L) return@runCatching SIGNING_UNKNOWN_MESSAGE
                    val archive =
                        context.packageManager.archiveInfo(temp.absolutePath)
                            ?: return@runCatching SIGNING_UNKNOWN_MESSAGE
                    val installed =
                        context.packageManager.installedInfo(context.packageName)
                            ?: return@runCatching SIGNING_UNKNOWN_MESSAGE
                    val archiveSigners = archive.signerDigests()
                    val installedSigners = installed.signerDigests()
                    when {
                        archiveSigners.isEmpty() || installedSigners.isEmpty() -> SIGNING_UNKNOWN_MESSAGE
                        archiveSigners == installedSigners -> null
                        else -> SIGNING_MISMATCH_MESSAGE
                    }
                } finally {
                    temp.delete()
                }
            }.getOrDefault(SIGNING_UNKNOWN_MESSAGE)

        @Suppress("DEPRECATION")
        private fun PackageManager.archiveInfo(path: String): PackageInfo? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getPackageArchiveInfo(path, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                getPackageArchiveInfo(path, PackageManager.GET_SIGNATURES)
            }

        @Suppress("DEPRECATION")
        private fun PackageManager.installedInfo(packageName: String): PackageInfo? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }

        @Suppress("DEPRECATION")
        private fun PackageInfo.signerDigests(): Set<String> {
            val certificates =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    signingInfo?.apkContentsSigners
                } else {
                    signatures
                }
            return certificates.orEmpty().mapTo(HashSet()) { signature ->
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(signature.toByteArray())
                    .joinToString("") { "%02x".format(it) }
            }
        }

        /** Set by the pop-up's Download button so the finished download re-opens it with Install. */
        @Volatile
        var startedFromPrompt: Boolean = false

        private suspend fun removeDownload(id: Long) {
            runCatching { downloadManager?.remove(id) }
            preferences.clearUpdateDownload()
        }

        private data class DownloadStatus(
            val status: Int,
            val progress: Int,
        )

        private fun queryDownload(id: Long): DownloadStatus? =
            runCatching {
                downloadManager?.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val doneColumn = DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                    val done = cursor.getLong(cursor.getColumnIndexOrThrow(doneColumn))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val progress = if (total > 0) ((done * PERCENT) / total).toInt() else UNKNOWN_PROGRESS
                    DownloadStatus(status, progress)
                }
            }.getOrNull()

        private fun launch(intent: Intent) {
            runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                .onFailure { e -> _state.value = UpdateState.Failed("Couldn't open the installer (${e.message})") }
        }

        private fun releaseOf(state: UpdateState): ReleaseInfo? =
            when (state) {
                is UpdateState.Available -> state.release
                is UpdateState.Failed -> state.release
                is UpdateState.ReadyToInstall -> state.release
                else -> null
            }

        private fun frequencyOf(name: String?): UpdateFrequency =
            UpdateFrequency.entries.firstOrNull { it.name == name } ?: UpdateFrequency.DAILY

        private companion object {
            const val APK_MIME_TYPE = "application/vnd.android.package-archive"
            const val POLL_INTERVAL_MS = 500L
            const val PERIODIC_CHECK_INTERVAL_MS = 30 * 60 * 1000L // 30 minutes
            const val BUFFER_SIZE = 64 * 1024
            const val PERCENT = 100
            const val UNKNOWN_PROGRESS = -1
            const val NO_CHECKSUM_MESSAGE = "Couldn't get this release's checksum, so it can't be verified"
            const val DOWNLOAD_FAILED_MESSAGE = "Download failed. Check your connection and retry."
            const val DAMAGED_MESSAGE = "The download was damaged (checksum mismatch) and was deleted. Try again."
            const val SIGNING_MISMATCH_MESSAGE =
                "This update uses a different signing key and would require uninstalling. " +
                    "Back up your library, then install the APK from the release page."
            const val SIGNING_UNKNOWN_MESSAGE =
                "Couldn't verify the update's signature, so it was not installed. Try downloading it again."
            const val SIGNER_CHECK_FILE = "update-signer-check.apk"
        }
    }
