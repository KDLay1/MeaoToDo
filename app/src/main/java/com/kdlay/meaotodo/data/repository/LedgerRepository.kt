package com.kdlay.meaotodo.data.repository

import com.kdlay.meaotodo.data.local.dao.LedgerDao
import com.kdlay.meaotodo.data.local.dao.SyncOutboxDao
import com.kdlay.meaotodo.data.local.entity.LedgerEntryEntity
import com.kdlay.meaotodo.data.local.entity.SyncOutboxEntity
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LedgerRepository(
    private val ledgerDao: LedgerDao,
    private val syncOutboxDao: SyncOutboxDao
) {
    val entries = ledgerDao.observeEntries()

    fun observeExpenseSum(startAt: Long, endAt: Long) = ledgerDao.observeExpenseSum(startAt, endAt)

    suspend fun addExpense(amountCents: Long, category: String, note: String = ""): Boolean {
        val cleanCategory = category.trim().ifBlank { "??" }
        val safeAmount = amountCents.coerceAtLeast(0)
        if (safeAmount <= 0) return false

        val now = System.currentTimeMillis()
        val entry = LedgerEntryEntity(
            id = UUID.randomUUID().toString(),
            amountCents = safeAmount,
            type = TYPE_EXPENSE,
            category = cleanCategory,
            note = note.trim(),
            occurredAt = now,
            createdAt = now,
            updatedAt = now
        )
        ledgerDao.upsert(entry)
        enqueueChange(entry = entry, operation = "upsert", createdAt = now)
        return true
    }

    suspend fun removeEntry(id: String): Boolean {
        val now = System.currentTimeMillis()
        ledgerDao.softDelete(id = id, deletedAt = now)
        val deleted = ledgerDao.findById(id) ?: return false
        enqueueChange(entry = deleted, operation = "delete", createdAt = now)
        return true
    }

    private suspend fun enqueueChange(entry: LedgerEntryEntity, operation: String, createdAt: Long) {
        syncOutboxDao.enqueue(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entityType = "ledger_entry",
                entityId = entry.id,
                operation = operation,
                payloadJson = json.encodeToString(entry.toSyncPayload()),
                createdAt = createdAt
            )
        )
    }

    private fun LedgerEntryEntity.toSyncPayload(): LedgerEntrySyncPayload = LedgerEntrySyncPayload(
        id = id,
        amountCents = amountCents,
        type = type,
        category = category,
        note = note,
        occurredAt = occurredAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    @Serializable
    private data class LedgerEntrySyncPayload(
        val id: String,
        val amountCents: Long,
        val type: String,
        val category: String,
        val note: String,
        val occurredAt: Long,
        val createdAt: Long,
        val updatedAt: Long,
        val deletedAt: Long?
    )

    private companion object {
        const val TYPE_EXPENSE = "expense"
        val json = Json { encodeDefaults = true }
    }
}
