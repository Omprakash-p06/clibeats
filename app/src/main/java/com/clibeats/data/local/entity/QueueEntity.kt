package com.clibeats.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queue_items")
data class QueueEntity(
    @PrimaryKey val position: Int,
    val songId: String,
)
