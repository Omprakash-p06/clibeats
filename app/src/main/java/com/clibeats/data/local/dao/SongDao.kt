// ForbiddenImport: data-layer self-imports are legitimate; Phase 0 com.clibeats.data.* pattern is over-broad.
@file:Suppress("ForbiddenImport", "MaxLineLength")

package com.clibeats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clibeats.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: String): SongEntity?

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllAsFlow(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY COALESCE(cached_at, 0) DESC")
    fun getRecentlyAddedAsFlow(): Flow<List<SongEntity>>

    @Query(
        """
        SELECT * FROM songs 
        WHERE title LIKE '%' || :query || '%' ESCAPE '\' 
           OR artist LIKE '%' || :query || '%' ESCAPE '\'
        """,
    )
    fun searchAsFlow(query: String): Flow<List<SongEntity>>

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM songs")
    suspend fun deleteAll()
}

/**
 * Escapes SQL LIKE wildcards so a user query containing `%`, `_`, or `\`
 * matches literally instead of broadening the search (wildcard injection).
 * Used in conjunction with the `ESCAPE '\'` clause in [SongDao.searchAsFlow].
 */
fun String.escapeForLike(): String =
    this.replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
