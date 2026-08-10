@file:Suppress("ForbiddenImport", "TooGenericExceptionCaught")

package com.clibeats.data.playlist

import android.content.Context
import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.ProviderId
import com.clibeats.domain.repository.PlaylistRepository
import com.clibeats.domain.repository.SongRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exports and imports playlists as `clibeats.json` stored in the app's
 * external files directory.
 *
 * Export writes provider + source ids; import re-persists them under composite
 * ids and attempts resolution when the track is played. Tracks whose provider
 * is unavailable retain their metadata (marked by a null stream URL).
 */
@Singleton
class PlaylistExchangeManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val playlistRepository: PlaylistRepository,
        private val songRepository: SongRepository,
    ) {
        val exportFile: File
            get() = File(exchangeDir, FILE_NAME)

        suspend fun export(): Result<File> =
            runCatching {
                val playlists = playlistRepository.getAllPlaylistsAsFlow().first()
                val file =
                    CliBeatsFile(
                        playlists =
                            playlists.map { playlist ->
                                val songs = playlistRepository.getSongsForPlaylistAsFlow(playlist.id).first()
                                CliBeatsPlaylist(
                                    name = playlist.name,
                                    tracks =
                                        songs.map { song ->
                                            CliBeatsTrack(
                                                providerId = song.providerId,
                                                sourceId = ProviderId.rawSourceId(song.providerId, song.id),
                                                title = song.title,
                                                artist = song.artist,
                                                album = song.album,
                                                durationMs = song.durationMs,
                                                artworkUrl = song.artworkUrl,
                                                sourceUrl = song.streamUrl,
                                            )
                                        },
                                )
                            },
                    )
                exportFile.writeText(CliBeatsFileCodec.encode(file))
                exportFile
            }

        /** Returns the number of tracks imported. */
        suspend fun import(): Result<Int> =
            runCatching {
                if (!exportFile.exists()) {
                    error("No clibeats.json found at ${exportFile.absolutePath}")
                }
                val cliBeatsFile = CliBeatsFileCodec.decode(exportFile.readText())
                var importedTracks = 0
                cliBeatsFile.playlists.forEach { playlistData ->
                    if (playlistData.name.isBlank() || playlistData.tracks.isEmpty()) return@forEach
                    val playlistId = "imported_${playlistData.name.hashCode()}"
                    val tracks =
                        playlistData.tracks.map { trackData ->
                            Track(
                                id = ProviderId.composite(trackData.providerId, trackData.sourceId),
                                title = trackData.title,
                                artist = trackData.artist,
                                album = trackData.album,
                                durationMs = trackData.durationMs,
                                artworkUrl = trackData.artworkUrl,
                                streamUrl = trackData.sourceUrl,
                                providerId = trackData.providerId,
                            )
                        }
                    songRepository.upsertTracks(tracks)
                    playlistRepository.upsertPlaylist(
                        Playlist(
                            id = playlistId,
                            name = playlistData.name,
                            description = "Imported from clibeats.json",
                            artworkUrl = null,
                            trackCount = tracks.size,
                            isOwned = true,
                            providerId = "local",
                        ),
                    )
                    tracks.forEachIndexed { index, track ->
                        playlistRepository.addSongToPlaylist(playlistId, track.id, index)
                    }
                    importedTracks += tracks.size
                }
                importedTracks
            }

        private val exchangeDir: File
            get() =
                File(
                    context.getExternalFilesDir(null) ?: context.filesDir,
                    "clibeats_exchange",
                ).apply { mkdirs() }

        companion object {
            const val FILE_NAME = "clibeats.json"
        }
    }
