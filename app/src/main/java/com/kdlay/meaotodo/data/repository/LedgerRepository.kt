package com.kdlay.meaotodo.data.repository

import com.kdlay.meaotodo.data.local.dao.LedgerDao
import com.kdlay.meaotodo.data.local.dao.SyncOutboxDao
import com.kdlay.meaotodo.data.local.entity.LedgerEntryEntity
import com.kdlay.meaotodo.data.local.entity.SyncOutboxEntity
import java.util.UUID

class LedgerRepository(
    private val ledgerDao: LedgerDao,
    private val syncOutboxDao: SyncOutboxDao
) {
    val entries = ledgerDao.observeEntries()

    fun observeExpenseSum(startAt: Long, endAt: Long) = ledgerDao.observeExpenseSum(startAt, endAt)

    suspend fun addExpense(amountCents: Long, category: String, note: String = "") {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        ledgerDao.upsert(
            LedgerEntryEntity(
                id = id,
                amountCents = amountCents,
                type = "expense",
                category = category,
                note = note,
                occurredAt = now,
                createdAt = now,
                updatedAt = now
            )
        )
        enqueueChange(entityId = id, operation = "upsert", createdAt = now)
    }

    private suspend fun enqueueChange(entityId: String, operation: String, createdAt: Long) {
        // 第一版先记录变更索引，后面再替换为完整 JSON 序列化。
        syncOutboxDao.enqueue(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entityType = "ledger_entry",
                entityId = entityId,
                operation = operation,
                payloadJson = "{}",
                createdAt = createdAt
            )
        )
    }
}
