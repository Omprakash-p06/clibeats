// ForbiddenImport: data-layer self-imports are legitimate; Phase 0 com.clibeats.data.* pattern is over-broad.
@file:Suppress("ForbiddenImport", "MaxLineLength", "TooManyFunctions")

package com.clibeats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clibeats.data.local.entity.PlaylistEntity
import com.clibeats.data.local.entity.PlaylistSongCrossRef
import com.clibeats.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: String): PlaylistEntity?

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllAsFlow(): Flow<List<PlaylistEntity>>

    @Query(
        """
        SELECT * FROM playlists 
        WHERE name LIKE '%' || :query || '%' ESCAPE '\'
        """,
    )
    fun searchAsFlow(query: String): Flow<List<PlaylistEntity>>

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun removeSongFromPlaylist(
        playlistId: String,
        songId: String,
    )

    @Query(
        """
        SELECT songs.* FROM songs
        INNER JOIN playlist_song_cross_ref ON songs.id = playlist_song_cross_ref.song_id
        WHERE playlist_song_cross_ref.playlist_id = :playlistId
        ORDER BY playlist_song_cross_ref.position ASC
        """,
    )
    fun getSongsForPlaylistAsFlow(playlistId: String): Flow<List<SongEntity>>

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlist_id = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: String)

    @androidx.room.Transaction
    suspend fun reorderPlaylistSongs(
        playlistId: String,
        songIds: List<String>,
    ) {
        clearPlaylistSongs(playlistId)
        songIds.forEachIndexed { index, songId ->
            addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId, index))
        }
    }
}
