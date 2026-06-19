package com.kdlay.meaotodo.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kdlay.meaotodo.data.local.entity.SyncOutboxEntity

@Dao
interface SyncOutboxDao {
    @Query("SELECT * FROM sync_outbox WHERE deliveredAt IS NULL ORDER BY createdAt ASC LIMIT :limit")
    suspend fun pending(limit: Int = 100): List<SyncOutboxEntity>

    @Upsert
    suspend fun enqueue(change: SyncOutboxEntity)

    @Query("UPDATE sync_outbox SET deliveredAt = :deliveredAt WHERE id IN (:ids)")
    suspend fun markDelivered(ids: List<String>, deliveredAt: Long)
}
