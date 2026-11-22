@file:Suppress("ForbiddenImport")

package com.clibeats.data.backup

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.clibeats.data.playlist.PlaylistExchangeManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps a rolling `clibeats-backup.json` of the whole library, so a reinstall (or a signing-key change
 * that forces an uninstall) no longer means losing everything. Two copies:
 *
 * - `files/backups/` inside the app: part of Android's own backup, so it comes back with a restore.
 * - `Download/CLIBeats/` on Android 10+: survives an uninstall. A new install can't read it directly
 *   (it doesn't own the file any more), so onboarding opens the file picker on it.
 *
 * A snapshot is written at most once a day and never for an empty library, so a wipe or a failed scan
 * can't overwrite a good backup. The previous internal copy is kept as `.prev.json`.
 */
@Singleton
class LibrarySnapshotManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val exchange: PlaylistExchangeManager,
    ) {
        private val mutex = Mutex()

        private val folder: File
            get() = context.filesDir.resolve(FOLDER_NAME)

        val latestFile: File
            get() = File(folder, PlaylistExchangeManager.BACKUP_FILE_NAME)

        private val previousFile: File
            get() = File(folder, "clibeats-backup.prev.json")

        fun hasSnapshot(): Boolean = latestFile.isFile && latestFile.length() > 0L

        /** Writes a snapshot when the newest one is older than a day (or [force] is set). */
        suspend fun snapshotIfDue(force: Boolean = false): Result<File?> =
            mutex.withLock {
                runCatching {
                    val latest = latestFile
                    val fresh =
                        !force &&
                            latest.isFile &&
                            System.currentTimeMillis() - latest.lastModified() < SNAPSHOT_INTERVAL_MS
                    if (fresh) return@runCatching null

                    val file = exchange.buildFile()
                    // Never write an empty library (first run, wiped library): it would replace a good backup.
                    if (file.playlists.isEmpty() && file.albums.isEmpty() && file.artists.isEmpty()) {
                        return@runCatching null
                    }
                    withContext(Dispatchers.IO) {
                        if (latest.isFile) latest.copyTo(previousFile, overwrite = true)
                    }
                    // Stage the export and swap it in, so an interrupted write can't leave a half
                    // file that still looks like a restorable backup.
                    val staged = File(folder, "${latest.name}.tmp")
                    try {
                        exchange.exportToFile(staged).getOrThrow()
                        withContext(Dispatchers.IO) {
                            if (!staged.renameTo(latest)) staged.copyTo(latest, overwrite = true)
                        }
                    } finally {
                        withContext(Dispatchers.IO) { staged.delete() }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Best effort: the internal copy is already safe.
                        runCatching { writePublicCopy(latest) }
                    }
                    latest
                }
            }

        /**
         * Copies [source] to `Download/CLIBeats/clibeats-backup.json`, replacing this install's earlier
         * copy. A copy left by a previous install isn't visible to this one, so Android names the new
         * file `clibeats-backup (1).json`; both stay restorable.
         */
        @RequiresApi(Build.VERSION_CODES.Q)
        private suspend fun writePublicCopy(source: File) =
            withContext(Dispatchers.IO) {
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val existing: Uri? =
                    resolver.query(
                        collection,
                        arrayOf(MediaStore.MediaColumns._ID),
                        "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                        arrayOf(PUBLIC_RELATIVE_PATH, PlaylistExchangeManager.BACKUP_FILE_NAME),
                        null,
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) ContentUris.withAppendedId(collection, cursor.getLong(0)) else null
                    }
                val target =
                    existing ?: resolver.insert(
                        collection,
                        ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, PlaylistExchangeManager.BACKUP_FILE_NAME)
                            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, PUBLIC_RELATIVE_PATH)
                        },
                    ) ?: error("Couldn't create the backup in Downloads")
                resolver.openOutputStream(target, "wt")?.use { out ->
                    source.inputStream().use { it.copyTo(out) }
                } ?: error("Couldn't write the backup in Downloads")
            }

        /**
         * Copies the latest snapshot into the cache so a `FileProvider` URI can be shared through
         * the system share sheet. Returns null when there is nothing to share yet.
         */
        suspend fun prepareShareFile(): File? =
            withContext(Dispatchers.IO) {
                val latest = latestFile.takeIf { it.isFile } ?: return@withContext null
                val shared = context.cacheDir.resolve(SHARED_FOLDER).apply { mkdirs() }
                val target = File(shared, PlaylistExchangeManager.BACKUP_FILE_NAME)
                latest.copyTo(target, overwrite = true)
                target
            }

        companion object {
            private const val FOLDER_NAME = "backups"
            private const val SHARED_FOLDER = "shared"
            private const val SNAPSHOT_INTERVAL_MS = 24 * 60 * 60 * 1000L

            /** Where the uninstall-proof copy goes, as MediaStore stores it (trailing slash included). */
            val PUBLIC_RELATIVE_PATH = Environment.DIRECTORY_DOWNLOADS + "/CLIBeats/"
        }
    }
