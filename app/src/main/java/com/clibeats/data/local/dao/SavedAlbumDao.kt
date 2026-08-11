// ForbiddenImport: data-layer self-imports are legitimate; Phase 0 com.clibeats.data.* pattern is over-broad.
@file:Suppress("ForbiddenImport", "MaxLineLength")

package com.clibeats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clibeats.data.local.entity.SavedAlbumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedAlbumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(album: SavedAlbumEntity)

    @Query("DELETE FROM saved_albums WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_albums WHERE id = :id)")
    fun isSavedFlow(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_albums WHERE id = :id)")
    suspend fun isSaved(id: String): Boolean

    @Query("SELECT * FROM saved_albums ORDER BY saved_at DESC")
    fun getAllAsFlow(): Flow<List<SavedAlbumEntity>>
}
