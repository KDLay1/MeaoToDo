package com.kdlay.meaotodo.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kdlay.meaotodo.data.local.entity.LedgerEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries WHERE deletedAt IS NULL ORDER BY occurredAt DESC")
    fun observeEntries(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM ledger_entries WHERE type = 'expense' AND deletedAt IS NULL AND occurredAt BETWEEN :startAt AND :endAt")
    fun observeExpenseSum(startAt: Long, endAt: Long): Flow<Long>

    @Upsert
    suspend fun upsert(entry: LedgerEntryEntity)

    @Query("UPDATE ledger_entries SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)
}
