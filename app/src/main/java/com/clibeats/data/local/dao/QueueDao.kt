// ForbiddenImport: data-layer self-imports are legitimate
@file:Suppress("ForbiddenImport")

package com.clibeats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.clibeats.data.local.entity.QueueEntity
import com.clibeats.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    fun getQueueItemsAsFlow(): Flow<List<QueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<QueueEntity>)

    @Query("DELETE FROM queue_items")
    suspend fun clearQueue()

    @Transaction
    suspend fun replaceQueue(items: List<QueueEntity>) {
        clearQueue()
        insertQueueItems(items)
    }

    @Query(
        """
        SELECT songs.* FROM songs
        INNER JOIN queue_items ON songs.id = queue_items.songId
        ORDER BY queue_items.position ASC
        """,
    )
    fun getQueueSongsAsFlow(): Flow<List<SongEntity>>
}
