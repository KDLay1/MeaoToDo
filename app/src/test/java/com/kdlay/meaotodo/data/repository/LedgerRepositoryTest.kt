package com.kdlay.meaotodo.data.repository

import com.kdlay.meaotodo.data.local.dao.LedgerDao
import com.kdlay.meaotodo.data.local.dao.SyncOutboxDao
import com.kdlay.meaotodo.data.local.entity.LedgerEntryEntity
import com.kdlay.meaotodo.data.local.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LedgerRepositoryTest {
    private lateinit var ledgerDao: FakeLedgerDao
    private lateinit var syncOutboxDao: FakeSyncOutboxDao
    private lateinit var repository: LedgerRepository

    @Before
    fun setUp() {
        ledgerDao = FakeLedgerDao()
        syncOutboxDao = FakeSyncOutboxDao()
        repository = LedgerRepository(ledgerDao, syncOutboxDao)
    }

    @Test
    fun addExpense_createsEntryAndFullOutboxPayload() = runTest {
        val added = repository.addExpense(1234L, "  Food  ", " lunch ")

        assertTrue(added)
        val entry = ledgerDao.activeEntries().single()
        assertEquals(1234L, entry.amountCents)
        assertEquals("Food", entry.category)
        assertEquals("lunch", entry.note)

        val change = syncOutboxDao.allPending().single()
        assertEquals("ledger_entry", change.entityType)
        assertEquals(entry.id, change.entityId)
        assertEquals("upsert", change.operation)
        assertFalse(change.payloadJson == "{}")
        assertTrue(change.payloadJson.contains("Food"))
    }

    @Test
    fun removeEntry_softDeletesEntryAndEnqueuesDeletePayload() = runTest {
        assertTrue(repository.addExpense(500L, "Other"))
        val entry = ledgerDao.activeEntries().single()

        assertTrue(repository.removeEntry(entry.id))

        assertTrue(ledgerDao.activeEntries().isEmpty())
        val changes = syncOutboxDao.allPending()
        assertEquals(listOf("upsert", "delete"), changes.map { it.operation })
        assertTrue(changes.last().payloadJson.contains(entry.id))
    }

    private class FakeLedgerDao : LedgerDao {
        private val entries = linkedMapOf<String, LedgerEntryEntity>()

        override fun observeEntries(): Flow<List<LedgerEntryEntity>> = flow { emit(activeEntries()) }

        override fun observeExpenseSum(startAt: Long, endAt: Long): Flow<Long> = flow {
            emit(activeEntries().filter { it.type == "expense" && it.occurredAt in startAt..endAt }.sumOf { it.amountCents })
        }

        override suspend fun findById(id: String): LedgerEntryEntity? = entries[id]

        override suspend fun upsert(entry: LedgerEntryEntity) {
            entries[entry.id] = entry
        }

        override suspend fun softDelete(id: String, deletedAt: Long) {
            entries[id]?.let { entries[id] = it.copy(deletedAt = deletedAt, updatedAt = deletedAt) }
        }

        fun activeEntries(): List<LedgerEntryEntity> = entries.values.filter { it.deletedAt == null }.sortedByDescending { it.occurredAt }
    }

    private class FakeSyncOutboxDao : SyncOutboxDao {
        private val changes = linkedMapOf<String, SyncOutboxEntity>()

        override suspend fun pending(limit: Int): List<SyncOutboxEntity> = allPending().take(limit)

        override suspend fun enqueue(change: SyncOutboxEntity) {
            changes[change.id] = change
        }

        override suspend fun markDelivered(ids: List<String>, deliveredAt: Long) {
            ids.forEach { id -> changes[id]?.let { changes[id] = it.copy(deliveredAt = deliveredAt) } }
        }

        fun allPending(): List<SyncOutboxEntity> = changes.values.filter { it.deliveredAt == null }.sortedBy { it.createdAt }
    }
}
