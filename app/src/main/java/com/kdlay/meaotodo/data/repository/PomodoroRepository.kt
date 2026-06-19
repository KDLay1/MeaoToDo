package com.kdlay.meaotodo.data.repository

import com.kdlay.meaotodo.data.local.dao.PomodoroDao
import com.kdlay.meaotodo.data.local.dao.SyncOutboxDao
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.SyncOutboxEntity
import java.util.UUID

class PomodoroRepository(
    private val pomodoroDao: PomodoroDao,
    private val syncOutboxDao: SyncOutboxDao
) {
    val sessions = pomodoroDao.observeSessions()
    val runningSession = pomodoroDao.observeRunningSession()

    suspend fun startFocus(taskId: String?, plannedDurationSeconds: Int = 25 * 60) {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        pomodoroDao.upsert(
            PomodoroSessionEntity(
                id = id,
                taskId = taskId,
                type = "focus",
                plannedDurationSeconds = plannedDurationSeconds,
                startedAt = now,
                status = "running",
                createdAt = now,
                updatedAt = now
            )
        )
        enqueueChange(entityId = id, operation = "upsert", createdAt = now)
    }

    suspend fun finish(id: String, actualDurationSeconds: Int) {
        val now = System.currentTimeMillis()
        pomodoroDao.finishSession(
            id = id,
            status = "finished",
            endedAt = now,
            actualDurationSeconds = actualDurationSeconds,
            updatedAt = now
        )
        enqueueChange(entityId = id, operation = "upsert", createdAt = now)
    }

    private suspend fun enqueueChange(entityId: String, operation: String, createdAt: Long) {
        // 第一版先记录变更索引，后面再替换为完整 JSON 序列化。
        syncOutboxDao.enqueue(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entityType = "pomodoro_session",
                entityId = entityId,
                operation = operation,
                payloadJson = "{}",
                createdAt = createdAt
            )
        )
    }
}
