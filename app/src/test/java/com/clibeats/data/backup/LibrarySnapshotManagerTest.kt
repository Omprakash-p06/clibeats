@file:Suppress("ForbiddenImport")

package com.clibeats.data.backup

import android.content.Context
import com.clibeats.data.playlist.CliBeatsFile
import com.clibeats.data.playlist.CliBeatsPlaylist
import com.clibeats.data.playlist.PlaylistExchangeManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.io.IOException
import java.nio.file.Files

class LibrarySnapshotManagerTest {
    private lateinit var context: Context
    private lateinit var exchange: PlaylistExchangeManager
    private lateinit var manager: LibrarySnapshotManager
    private lateinit var filesDir: File

    @Before
    fun setUp() {
        filesDir = Files.createTempDirectory("snapshot-test").toFile()
        File(filesDir, "backups").mkdirs()
        context = mock()
        exchange = mock()
        whenever(context.filesDir).thenReturn(filesDir)
        manager = LibrarySnapshotManager(context, exchange)
    }

    @Test
    fun `snapshot is written whole and leaves no staged file behind`() =
        runTest {
            whenever(exchange.buildFile()).thenReturn(libraryWithOnePlaylist())
            whenever(exchange.exportToFile(any())).thenAnswer { invocation ->
                (invocation.arguments[0] as File).writeText(PAYLOAD)
                Result.success(1)
            }

            val written = manager.snapshotIfDue().getOrNull()

            assertThat(written).isEqualTo(manager.latestFile)
            assertThat(manager.latestFile.readText()).isEqualTo(PAYLOAD)
            assertThat(File(filesDir, "backups/${manager.latestFile.name}.tmp").exists()).isFalse()
        }

    @Test
    fun `a fresh snapshot is skipped until it is due`() =
        runTest {
            whenever(exchange.buildFile()).thenReturn(libraryWithOnePlaylist())
            whenever(exchange.exportToFile(any())).thenAnswer { invocation ->
                (invocation.arguments[0] as File).writeText(PAYLOAD)
                Result.success(1)
            }
            manager.snapshotIfDue()

            val again = manager.snapshotIfDue().getOrNull()

            assertThat(again).isNull()
        }

    @Test
    fun `an empty library never replaces the existing backup`() =
        runTest {
            whenever(exchange.buildFile()).thenReturn(CliBeatsFile())

            val written = manager.snapshotIfDue(force = true).getOrNull()

            assertThat(written).isNull()
            assertThat(manager.hasSnapshot()).isFalse()
        }

    @Test
    fun `a failed export leaves no half-written snapshot`() =
        runTest {
            whenever(exchange.buildFile()).thenReturn(libraryWithOnePlaylist())
            whenever(exchange.exportToFile(any())).thenAnswer { invocation ->
                (invocation.arguments[0] as File).writeText("""{"partial"""")
                throw IOException("disk full")
            }

            val written = manager.snapshotIfDue().getOrNull()

            assertThat(written).isNull()
            assertThat(manager.hasSnapshot()).isFalse()
            assertThat(File(filesDir, "backups/${manager.latestFile.name}.tmp").exists()).isFalse()
        }

    private fun libraryWithOnePlaylist() = CliBeatsFile(playlists = listOf(CliBeatsPlaylist(name = "Mix")))

    private companion object {
        const val PAYLOAD = """{"version":2,"playlists":[{"name":"Mix","tracks":[]}]}"""
    }
}
