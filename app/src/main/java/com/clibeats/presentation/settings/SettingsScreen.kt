// ForbiddenImport: ScanStatus is a plain UI-facing status type from the scanner.
@file:Suppress("ktlint:standard:function-naming", "MagicNumber", "ForbiddenImport")

package com.clibeats.presentation.settings

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.clibeats.BuildConfig
import com.clibeats.data.scan.ScanStatus
import com.clibeats.presentation.component.TuiBlock
import com.clibeats.presentation.theme.AccentColor
import com.clibeats.presentation.theme.CliBeatsThemeMode
import com.clibeats.presentation.theme.LocalAccentColor
import com.clibeats.presentation.update.UpdateSection

private const val BYTES_PER_MB = 1048576L
private const val MB_PER_GB = 1024
private const val GITHUB_URL = "https://github.com/Omprakash-p06/clibeats"
private const val ADITYA_GITHUB_URL = "https://github.com/AdityaKrishnamurthy"
private const val OMPRAKASH_GITHUB_URL = "https://github.com/Omprakash-p06"
private val ARTWORK_CACHE_OPTIONS = listOf(64, 128, 256, 512)
private val SONG_CACHE_OPTIONS = listOf(256, 512, 1024, 2048)
private const val PLAYLIST_FILE_NAME = "clibeats.json"
private val IMPORT_MIME_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")
private const val LOCAL_HINT = "Tags (title / artist / album) are read from the files. FLAC, MP3, WAV, M4A, OGG, OPUS."

private fun trackCountLabel(count: Int): String = if (count == 1) "1 track in library" else "$count tracks in library"

/** "primary:Music/FLAC" -> "Music/FLAC" for display. */
private fun folderLabel(treeUri: String): String =
    runCatching { DocumentsContract.getTreeDocumentId(Uri.parse(treeUri)).substringAfter(':').ifBlank { "/" } }
        .getOrDefault(treeUri)

@Suppress("FunctionNaming", "LongMethod", "CyclomaticComplexMethod")
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val exchangeMessage by viewModel.exchangeMessage.collectAsState()
    val autoQueueRelated by viewModel.autoQueueRelated.collectAsState()
    val localFolders by viewModel.localMusicFolders.collectAsState()
    val scanStatus by viewModel.scanStatus.collectAsState()
    val shareFile by viewModel.shareFile.collectAsState()
    val localTrackCount by viewModel.localTrackCount.collectAsState()
    val accent = LocalAccentColor.current
    val context = LocalContext.current

    // Share the snapshot through any app the user has (Drive, email, Nearby, ...).
    LaunchedEffect(shareFile) {
        val file = shareFile ?: return@LaunchedEffect
        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send =
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "CLIBeats library backup")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(Intent.createChooser(send, "Share CLIBeats backup"))
        }
        viewModel.clearShareFile()
    }

    val pickFolder =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                // Keep read access across reboots so rescans and playback keep working.
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                viewModel.onLocalFolderPicked(uri)
            }
        }
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) viewModel.exportPlaylists(uri)
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.importPlaylists(uri)
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Section: Theme Mode ─────────────────────────────────────────────
        TuiBlock(title = "Theme — ${uiState.themeMode.displayName}", isActive = true) {
            Column {
                CliBeatsThemeMode.entries.forEach { mode ->
                    val isSelected = uiState.themeMode == mode
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setThemeMode(mode) }
                                .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isSelected) "(•) " else "( ) ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // ── Section: Accent Color ───────────────────────────────────────────
        TuiBlock(title = "Accent Color — ${uiState.accentColor.displayName}", isActive = true) {
            AccentColorPicker(
                selected = uiState.accentColor,
                onSelect = viewModel::setAccentColor,
            )
        }

        // ── Section: Local Music ────────────────────────────────────────────
        TuiBlock(title = "Local Music", isActive = true) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (localFolders.isEmpty()) {
                    Text(
                        text = "FOLDERS : none chosen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                localFolders.forEach { folder ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "▸ ${folderLabel(folder)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "[ x ]",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier =
                                Modifier
                                    .semantics { contentDescription = "Remove folder ${folderLabel(folder)}" }
                                    .clickable(role = Role.Button) {
                                        runCatching {
                                            context.contentResolver.releasePersistableUriPermission(
                                                Uri.parse(folder),
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION,
                                            )
                                        }
                                        viewModel.removeLocalFolder(folder)
                                    }.padding(8.dp),
                        )
                    }
                }
                Text(
                    text =
                        when (val status = scanStatus) {
                            is ScanStatus.Idle, is ScanStatus.Done ->
                                if (localFolders.isEmpty()) LOCAL_HINT else trackCountLabel(localTrackCount)
                            is ScanStatus.Scanning ->
                                if (status.total == 0) {
                                    "Scanning..."
                                } else {
                                    "Scanning ${status.processed}/${status.total}..."
                                }
                            is ScanStatus.Failed -> "Scan failed: ${status.message}"
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (scanStatus is ScanStatus.Failed) MaterialTheme.colorScheme.error else accent,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { pickFolder.launch(null) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                    ) {
                        Text("+ ADD FOLDER")
                    }
                    Button(
                        onClick = viewModel::rescanLocalMusic,
                        enabled = localFolders.isNotEmpty() && scanStatus !is ScanStatus.Scanning,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                    ) {
                        Text("RESCAN")
                    }
                }
            }
        }

        // ── Section: Playback ───────────────────────────────────────────────
        TuiBlock(title = "Playback") {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setAutoQueueRelated(!autoQueueRelated) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AUTO-QUEUE RELATED SONGS",
                        style = MaterialTheme.typography.titleSmall,
                        color = accent,
                    )
                    Text(
                        text = "When off, tapping a song plays only that song (no suggestions queued after it)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (autoQueueRelated) "[ ON ]" else "[ OFF ]",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (autoQueueRelated) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Section: Active Music Provider ──────────────────────────────────
        TuiBlock(title = "Active Provider") {
            Column {
                uiState.providers.forEach { option ->
                    val isSelected = uiState.activeProviderId == option.id
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setActiveProvider(option.id) }
                                .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isSelected) "(•) " else "( ) ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // ── Section: Storage (two independent caches) ───────────────────────
        val cacheUsage by viewModel.cacheUsage.collectAsState()
        val artworkCacheMb by viewModel.artworkCacheMb.collectAsState()
        TuiBlock(title = "Storage") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CacheRow(
                    label = "ARTWORK",
                    usedBytes = cacheUsage.artworkBytes,
                    options = ARTWORK_CACHE_OPTIONS,
                    selectedMb = artworkCacheMb,
                    onSelect = viewModel::setArtworkCacheMb,
                    onClear = viewModel::clearArtworkCache,
                    note = "new limit applies on next app start",
                )
                CacheRow(
                    label = "SONGS",
                    usedBytes = cacheUsage.songBytes,
                    options = SONG_CACHE_OPTIONS,
                    selectedMb = uiState.cacheMaxMb,
                    onSelect = viewModel::setSongCacheMb,
                    onClear = viewModel::clearSongCache,
                )
            }
        }

        // ── Section: Streaming Quality ──────────────────────────────────────
        TuiBlock(title = "Audio Quality") {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setHighQualityStreaming(!uiState.highQualityStreaming) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "HIGH QUALITY STREAMING",
                        style = MaterialTheme.typography.titleSmall,
                        color = accent,
                    )
                    Text(
                        text = "Prefer high-bitrate audio streams",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (uiState.highQualityStreaming) "[ ON ]" else "[ OFF ]",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (uiState.highQualityStreaming) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Section: Playlist Exchange ──────────────────────────────────────
        TuiBlock(title = "Playlist Exchange") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { exportLauncher.launch(PLAYLIST_FILE_NAME) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                    ) {
                        Text("EXPORT")
                    }
                    Button(
                        onClick = { importLauncher.launch(IMPORT_MIME_TYPES) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                    ) {
                        Text("IMPORT")
                    }
                }
                Button(
                    onClick = { viewModel.shareSnapshot() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                ) {
                    Text("SHARE BACKUP")
                }
                Text(
                    text =
                        "A backup is saved automatically once a day (on Android 10+ also to Download/CLIBeats, " +
                            "so it survives an uninstall). Share it to keep an off-device copy.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                exchangeMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                    )
                }
            }
        }

        // ── Section: Updates (GitHub Releases) ──────────────────────────────
        UpdateSection()

        // ── Section: Privacy & Usage ─────────────────────────────────────────
        val telemetryOptIn by viewModel.anonymousUsageOptIn.collectAsState()
        TuiBlock(title = "Privacy & Usage") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Report usage anonymously",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (telemetryOptIn) "[● ON •]" else "[○ OFF •]",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (telemetryOptIn) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .clickable { viewModel.setAnonymousUsageOptIn(!telemetryOptIn) }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
                Text(
                    text =
                        "Reports usage anonymously to GitHub for real-time user count. " +
                            "None of your personal data or listening history is collected.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Section: About (developer credit; name links to GitHub) ─────────
        TuiBlock(title = "About") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "CLIBeats v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Developed by",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DeveloperLine(name = "Omprakash Panda", url = OMPRAKASH_GITHUB_URL)
                DeveloperLine(name = "Aditya Krishnamurthy", url = ADITYA_GITHUB_URL, cursor = true)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "source: github.com/Omprakash-p06/clibeats · MIT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .semantics { contentDescription = "Source code on GitHub" }
                            .clickable(role = Role.Button) {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))) }
                            }.padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Suppress("FunctionNaming", "LongParameterList")
@Composable
private fun CacheRow(
    label: String,
    usedBytes: Long,
    options: List<Int>,
    selectedMb: Int,
    onSelect: (Int) -> Unit,
    onClear: () -> Unit,
    note: String? = null,
) {
    val accent = LocalAccentColor.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label  ${usedBytes / BYTES_PER_MB} / $selectedMb MB",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "[ CLEAR ]",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .semantics { contentDescription = "Clear ${label.lowercase()} cache" }
                        .clickable(role = Role.Button, onClick = onClear)
                        .padding(8.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { mb ->
                val isSelected = mb == selectedMb
                Text(
                    text = if (mb >= MB_PER_GB) "${mb / MB_PER_GB}G" else "${mb}M",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .border(1.dp, if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant)
                            .semantics { selected = isSelected }
                            .clickable(role = Role.RadioButton) { onSelect(mb) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Suppress("FunctionNaming")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentColorPicker(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit,
) {
    val currentAccent = LocalAccentColor.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AccentColor.entries.forEach { accentOption ->
            val isSelected = selected == accentOption
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(accentOption) },
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentOption.color)
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, currentAccent, RoundedCornerShape(4.dp))
                                } else {
                                    Modifier
                                },
                            ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = accentOption.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) currentAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One credit line, terminal style: `> Name`, linking to GitHub. The last line carries the prompt cursor. */
@Suppress("FunctionNaming")
@Composable
private fun DeveloperLine(
    name: String,
    url: String,
    cursor: Boolean = false,
) {
    val context = LocalContext.current
    val accent = LocalAccentColor.current
    val blink by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(CURSOR_BLINK_MS, easing = blinkEasing), RepeatMode.Restart),
        label = "blink",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .semantics { contentDescription = "$name on GitHub" }
                .clickable(role = Role.Button) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                }.padding(vertical = 4.dp),
    ) {
        Text(
            text = "> ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = accent,
            textDecoration = TextDecoration.Underline,
            maxLines = 1,
        )
        if (cursor) {
            Text(text = " ▌", style = MaterialTheme.typography.bodyMedium, color = accent.copy(alpha = blink))
        }
    }
}

private const val CURSOR_BLINK_MS = 1_100

/** On for the first half of each cycle, off for the second, like a terminal cursor. */
private val blinkEasing = Easing { fraction -> if (fraction < HALF_CYCLE) 0f else 1f }
private const val HALF_CYCLE = 0.5f
