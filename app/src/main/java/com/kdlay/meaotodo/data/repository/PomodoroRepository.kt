package com.kdlay.meaotodo.data.repository

import com.kdlay.meaotodo.data.local.dao.PomodoroDao
import com.kdlay.meaotodo.data.local.dao.SyncOutboxDao
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.SyncOutboxEntity
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PomodoroRepository(
    private val pomodoroDao: PomodoroDao,
    private val syncOutboxDao: SyncOutboxDao
) {
    val sessions = pomodoroDao.observeSessions()
    val activeSession = pomodoroDao.observeActiveSession()

    suspend fun startFocus(
        taskId: String?,
        titleSnapshot: String?,
        plannedDurationSeconds: Int
    ): Boolean {
        if (pomodoroDao.findActiveSession() != null) return false

        val now = System.currentTimeMillis()
        val session = PomodoroSessionEntity(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            titleSnapshot = titleSnapshot?.trim()?.takeIf { it.isNotBlank() },
            type = TYPE_FOCUS,
            plannedDurationSeconds = plannedDurationSeconds.coerceAtLeast(MIN_DURATION_SECONDS),
            startedAt = now,
            status = STATUS_RUNNING,
            createdAt = now,
            updatedAt = now
        )
        pomodoroDao.upsert(session)
        enqueueChange(session = session, operation = "upsert", createdAt = now)
        return true
    }

    suspend fun pause(id: String): Boolean {
        val session = pomodoroDao.findById(id)?.takeIf { it.status == STATUS_RUNNING } ?: return false
        val now = System.currentTimeMillis()
        val updated = session.copy(
            status = STATUS_PAUSED,
            pausedAt = now,
            actualDurationSeconds = elapsedSeconds(session, now),
            updatedAt = now
        )
        pomodoroDao.upsert(updated)
        enqueueChange(session = updated, operation = "upsert", createdAt = now)
        return true
    }

    suspend fun resume(id: String): Boolean {
        val session = pomodoroDao.findById(id)?.takeIf { it.status == STATUS_PAUSED } ?: return false
        val now = System.currentTimeMillis()
        val pausedAt = session.pausedAt ?: now
        val pausedSeconds = ((now - pausedAt) / 1_000).toInt().coerceAtLeast(0)
        val updated = session.copy(
            status = STATUS_RUNNING,
            pausedAt = null,
            accumulatedPausedSeconds = session.accumulatedPausedSeconds + pausedSeconds,
            updatedAt = now
        )
        pomodoroDao.upsert(updated)
        enqueueChange(session = updated, operation = "upsert", createdAt = now)
        return true
    }

    suspend fun finish(id: String): Boolean {
        val session = pomodoroDao.findById(id)?.takeIf { it.status == STATUS_RUNNING || it.status == STATUS_PAUSED } ?: return false
        val now = System.currentTimeMillis()
        val updated = session.copy(
            status = STATUS_FINISHED,
            pausedAt = null,
            endedAt = now,
            actualDurationSeconds = elapsedSeconds(session, now),
            updatedAt = now
        )
        pomodoroDao.upsert(updated)
        enqueueChange(session = updated, operation = "upsert", createdAt = now)
        return true
    }

    suspend fun cancel(id: String): Boolean {
        val session = pomodoroDao.findById(id)?.takeIf { it.status == STATUS_RUNNING || it.status == STATUS_PAUSED } ?: return false
        val now = System.currentTimeMillis()
        val updated = session.copy(
            status = STATUS_CANCELLED,
            pausedAt = null,
            endedAt = now,
            actualDurationSeconds = elapsedSeconds(session, now),
            updatedAt = now
        )
        pomodoroDao.upsert(updated)
        enqueueChange(session = updated, operation = "upsert", createdAt = now)
        return true
    }

    private fun elapsedSeconds(session: PomodoroSessionEntity, now: Long): Int {
        val endPoint = if (session.status == STATUS_PAUSED) session.pausedAt ?: now else now
        return ((endPoint - session.startedAt) / 1_000).toInt()
            .minus(session.accumulatedPausedSeconds)
            .coerceAtLeast(0)
    }

    private suspend fun enqueueChange(session: PomodoroSessionEntity, operation: String, createdAt: Long) {
        syncOutboxDao.enqueue(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entityType = "pomodoro_session",
                entityId = session.id,
                operation = operation,
                payloadJson = json.encodeToString(session.toSyncPayload()),
                createdAt = createdAt
            )
        )
    }

    private fun PomodoroSessionEntity.toSyncPayload(): PomodoroSessionSyncPayload = PomodoroSessionSyncPayload(
        id = id,
        taskId = taskId,
        titleSnapshot = titleSnapshot,
        type = type,
        plannedDurationSeconds = plannedDurationSeconds,
        actualDurationSeconds = actualDurationSeconds,
        startedAt = startedAt,
        pausedAt = pausedAt,
        endedAt = endedAt,
        accumulatedPausedSeconds = accumulatedPausedSeconds,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    @Serializable
    private data class PomodoroSessionSyncPayload(
        val id: String,
        val taskId: String?,
        val titleSnapshot: String?,
        val type: String,
        val plannedDurationSeconds: Int,
        val actualDurationSeconds: Int,
        val startedAt: Long,
        val pausedAt: Long?,
        val endedAt: Long?,
        val accumulatedPausedSeconds: Int,
        val status: String,
        val createdAt: Long,
        val updatedAt: Long,
        val deletedAt: Long?
    )

    companion object {
        const val STATUS_RUNNING = "running"
        const val STATUS_PAUSED = "paused"
        const val STATUS_FINISHED = "finished"
        const val STATUS_CANCELLED = "cancelled"
        const val TYPE_FOCUS = "focus"
        private const val MIN_DURATION_SECONDS = 60

        val json = Json {
            encodeDefaults = true
        }
    }
}
