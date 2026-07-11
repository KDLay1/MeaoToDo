package com.kdlay.meaotodo.data.backup

import com.kdlay.meaotodo.data.local.entity.LedgerEntryEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFormatTest {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Test
    fun backup_roundTripsCoreRecordsWithoutSecrets() {
        val envelope = MeaoBackupEnvelope(
            exportedAt = 123,
            taskLists = emptyList(),
            tasks = listOf(TaskEntity(id = "task-1", title = "写报告", createdAt = 1, updatedAt = 1)),
            pomodoroRuns = emptyList(),
            pomodoroSessions = emptyList(),
            ledgerEntries = listOf(
                LedgerEntryEntity(
                    id = "expense-1", amountCents = 3_200, type = "expense", category = "餐饮",
                    occurredAt = 1, createdAt = 1, updatedAt = 1
                )
            )
        )

        val encoded = json.encodeToString(envelope)
        val decoded = json.decodeFromString<MeaoBackupEnvelope>(encoded)

        assertEquals("task-1", decoded.tasks.single().id)
        assertEquals(3_200L, decoded.ledgerEntries.single().amountCents)
        assertTrue(encoded.contains("MeaoToDoBackup"))
        assertFalse(encoded.contains("api_key", ignoreCase = true))
        assertFalse(encoded.contains("sync_outbox", ignoreCase = true))
    }
}
