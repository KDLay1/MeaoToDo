package com.kdlay.meaotodo.data.repository

import com.kdlay.meaotodo.data.local.dao.PomodoroDao
import com.kdlay.meaotodo.data.local.dao.PomodoroRunDao
import com.kdlay.meaotodo.data.local.dao.SyncOutboxDao
import com.kdlay.meaotodo.data.local.entity.PomodoroRunEntity
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.SyncOutboxEntity
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PomodoroRepository(
    private val pomodoroDao: PomodoroDao,
    private val pomodoroRunDao: PomodoroRunDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val taskRepository: TaskRepository? = null
) {
    val sessions = pomodoroDao.observeSessions()
    val recentSessions = pomodoroDao.observeRecentSessions(limit = 12)
    val activeSession = pomodoroDao.observeActiveSession()
    val activeRun = pomodoroRunDao.observeActiveRun()

    suspend fun startRun(
        taskId: String?,
        titleSnapshot: String?,
        focusDurationSeconds: Int,
        breakDurationSeconds: Int = DEFAULT_BREAK_DURATION_SECONDS,
        targetFocusCount: Int = 1
    ): Boolean {
        if (pomodoroDao.findActiveSession() != null || pomodoroRunDao.findActiveRun() != null) return false

        val now = System.currentTimeMillis()
        val run = PomodoroRunEntity(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            titleSnapshot = titleSnapshot?.trim()?.takeIf { it.isNotBlank() },
            focusDurationSeconds = focusDurationSeconds.coerceAtLeast(MIN_DURATION_SECONDS),
            breakDurationSeconds = breakDurationSeconds.coerceAtLeast(MIN_DURATION_SECONDS),
            targetFocusCount = targetFocusCount.coerceIn(1, MAX_TARGET_FOCUS_COUNT),
            status = STATUS_RUNNING,
            startedAt = now,
            createdAt = now,
            updatedAt = now
        )
        val firstSession = createSession(
            run = run,
            type = TYPE_FOCUS,
            roundIndex = 1,
            now = now
        )
        pomodoroRunDao.upsert(run)
        pomodoroDao.upsert(firstSession)
        enqueueRunChange(run = run, operation = "upsert", createdAt = now)
        enqueueSessionChange(session = firstSession, operation = "upsert", createdAt = now)
        return true
    }

    suspend fun startFocus(
        taskId: String?,
        titleSnapshot: String?,
        plannedDurationSeconds: Int
    ): Boolean = startRun(
        taskId = taskId,
        titleSnapshot = titleSnapshot,
        focusDurationSeconds = plannedDurationSeconds,
        breakDurationSeconds = DEFAULT_BREAK_DURATION_SECONDS,
        targetFocusCount = 1
    )

    suspend fun pause(id: String): Boolean {
        val session = pomodoroDao.findById(id)?.takeIf { it.status == STATUS_RUNNING } ?: return false
        val now = System.currentTimeMillis()
        val updatedSession = session.copy(
            status = STATUS_PAUSED,
            pausedAt = now,
            actualDurationSeconds = elapsedSeconds(session, now),
            updatedAt = now
        )
        pomodoroDao.upsert(updatedSession)
        enqueueSessionChange(session = updatedSession, operation = "upsert", createdAt = now)

        session.runId?.let { runId ->
            val run = pomodoroRunDao.findById(runId)
            if (run != null && run.status == STATUS_RUNNING) {
                val updatedRun = run.copy(status = STATUS_PAUSED, updatedAt = now)
                pomodoroRunDao.upsert(updatedRun)
                enqueueRunChange(run = updatedRun, operation = "upsert", createdAt = now)
            }
        }
        return true
    }

    suspend fun resume(id: String): Boolean {
        val session = pomodoroDao.findById(id)?.takeIf { it.status == STATUS_PAUSED } ?: return false
        val now = System.currentTimeMillis()
        val pausedAt = session.pausedAt ?: now
        val pausedSeconds = ((now - pausedAt) / 1_000).toInt().coerceAtLeast(0)
        val updatedSession = session.copy(
            status = STATUS_RUNNING,
            pausedAt = null,
            accumulatedPausedSeconds = session.accumulatedPausedSeconds + pausedSeconds,
            updatedAt = now
        )
        pomodoroDao.upsert(updatedSession)
        enqueueSessionChange(session = updatedSession, operation = "upsert", createdAt = now)

        session.runId?.let { runId ->
            val run = pomodoroRunDao.findById(runId)
            if (run != null && run.status == STATUS_PAUSED) {
                val updatedRun = run.copy(status = STATUS_RUNNING, updatedAt = now)
                pomodoroRunDao.upsert(updatedRun)
                enqueueRunChange(run = updatedRun, operation = "upsert", createdAt = now)
            }
        }
        return true
    }

    suspend fun advanceIfNeeded(now: Long = System.currentTimeMillis()): Boolean {
        val session = pomodoroDao.findActiveSession()?.takeIf { it.status == STATUS_RUNNING } ?: return false
        if (elapsedSeconds(session, now) < session.plannedDurationSeconds) return false
        return completeSessionAndAdvance(session = session, now = now)
    }

    suspend fun completeCurrentSession(): Boolean {
        val session = pomodoroDao.findActiveSession() ?: return false
        val now = System.currentTimeMillis()
        return completeSessionAndAdvance(session = session, now = now)
    }

    suspend fun skipBreak(): Boolean {
        val session = pomodoroDao.findActiveSession()?.takeIf { it.type == TYPE_BREAK } ?: return false
        val now = System.currentTimeMillis()
        return completeSessionAndAdvance(session = session, now = now)
    }

    suspend fun cancelActiveRun(): Boolean {
        val now = System.currentTimeMillis()
        val session = pomodoroDao.findActiveSession()
        val run = pomodoroRunDao.findActiveRun()

        if (session == null && run == null) return false

        if (session != null) {
            val cancelledSession = session.copy(
                status = STATUS_CANCELLED,
                pausedAt = null,
                endedAt = now,
                actualDurationSeconds = elapsedSeconds(session, now),
                updatedAt = now
            )
            pomodoroDao.upsert(cancelledSession)
            enqueueSessionChange(session = cancelledSession, operation = "upsert", createdAt = now)
        }

        if (run != null) {
            val cancelledRun = run.copy(
                status = STATUS_CANCELLED,
                endedAt = now,
                updatedAt = now
            )
            pomodoroRunDao.upsert(cancelledRun)
            enqueueRunChange(run = cancelledRun, operation = "upsert", createdAt = now)
        }
        return true
    }

    suspend fun finish(id: String): Boolean {
        val session = pomodoroDao.findById(id)?.takeIf { it.status == STATUS_RUNNING || it.status == STATUS_PAUSED } ?: return false
        val now = System.currentTimeMillis()
        return completeSessionAndAdvance(session = session, now = now)
    }

    suspend fun cancel(id: String): Boolean {
        val session = pomodoroDao.findById(id)?.takeIf { it.status == STATUS_RUNNING || it.status == STATUS_PAUSED } ?: return false
        if (session.runId != null) return cancelActiveRun()

        val now = System.currentTimeMillis()
        val updated = session.copy(
            status = STATUS_CANCELLED,
            pausedAt = null,
            endedAt = now,
            actualDurationSeconds = elapsedSeconds(session, now),
            updatedAt = now
        )
        pomodoroDao.upsert(updated)
        enqueueSessionChange(session = updated, operation = "upsert", createdAt = now)
        return true
    }

    private suspend fun completeSessionAndAdvance(session: PomodoroSessionEntity, now: Long): Boolean {
        val latestSession = pomodoroDao.findById(session.id) ?: return false
        if (latestSession.status != STATUS_RUNNING && latestSession.status != STATUS_PAUSED) return false

        val completedSession = latestSession.copy(
            status = STATUS_FINISHED,
            pausedAt = null,
            endedAt = now,
            actualDurationSeconds = elapsedSeconds(latestSession, now),
            updatedAt = now
        )
        pomodoroDao.upsert(completedSession)
        enqueueSessionChange(session = completedSession, operation = "upsert", createdAt = now)

        val runId = latestSession.runId ?: return true
        val run = pomodoroRunDao.findById(runId) ?: return true
        if (run.status != STATUS_RUNNING && run.status != STATUS_PAUSED) return true

        when (latestSession.type) {
            TYPE_FOCUS -> finishFocusAndStartBreak(run = run, completedSession = completedSession, now = now)
            TYPE_BREAK -> finishBreakAndContinue(run = run, completedSession = completedSession, now = now)
        }
        return true
    }

    private suspend fun finishFocusAndStartBreak(
        run: PomodoroRunEntity,
        completedSession: PomodoroSessionEntity,
        now: Long
    ) {
        val completedFocusCount = (run.completedFocusCount + 1).coerceAtMost(run.targetFocusCount)
        val updatedRun = run.copy(
            completedFocusCount = completedFocusCount,
            status = STATUS_RUNNING,
            updatedAt = now
        )
        val breakSession = createSession(
            run = updatedRun,
            type = TYPE_BREAK,
            roundIndex = completedSession.roundIndex,
            now = now
        )
        pomodoroRunDao.upsert(updatedRun)
        pomodoroDao.upsert(breakSession)
        completedSession.taskId?.let { taskRepository?.incrementActualPomodoros(it) }
        enqueueRunChange(run = updatedRun, operation = "upsert", createdAt = now)
        enqueueSessionChange(session = breakSession, operation = "upsert", createdAt = now)
    }

    private suspend fun finishBreakAndContinue(
        run: PomodoroRunEntity,
        completedSession: PomodoroSessionEntity,
        now: Long
    ) {
        if (run.completedFocusCount < run.targetFocusCount) {
            val nextFocusSession = createSession(
                run = run.copy(status = STATUS_RUNNING, updatedAt = now),
                type = TYPE_FOCUS,
                roundIndex = run.completedFocusCount + 1,
                now = now
            )
            val updatedRun = run.copy(status = STATUS_RUNNING, updatedAt = now)
            pomodoroRunDao.upsert(updatedRun)
            pomodoroDao.upsert(nextFocusSession)
            enqueueRunChange(run = updatedRun, operation = "upsert", createdAt = now)
            enqueueSessionChange(session = nextFocusSession, operation = "upsert", createdAt = now)
        } else {
            val finishedRun = run.copy(
                status = STATUS_FINISHED,
                endedAt = completedSession.endedAt ?: now,
                updatedAt = now
            )
            pomodoroRunDao.upsert(finishedRun)
            enqueueRunChange(run = finishedRun, operation = "upsert", createdAt = now)
        }
    }

    private fun createSession(
        run: PomodoroRunEntity,
        type: String,
        roundIndex: Int,
        now: Long
    ): PomodoroSessionEntity = PomodoroSessionEntity(
        id = UUID.randomUUID().toString(),
        runId = run.id,
        roundIndex = roundIndex.coerceAtLeast(1),
        taskId = run.taskId,
        titleSnapshot = if (type == TYPE_FOCUS) run.titleSnapshot else "休息",
        type = type,
        plannedDurationSeconds = if (type == TYPE_FOCUS) run.focusDurationSeconds else run.breakDurationSeconds,
        startedAt = now,
        status = STATUS_RUNNING,
        createdAt = now,
        updatedAt = now
    )

    private fun elapsedSeconds(session: PomodoroSessionEntity, now: Long): Int {
        val endPoint = if (session.status == STATUS_PAUSED) session.pausedAt ?: now else now
        return ((endPoint - session.startedAt) / 1_000).toInt()
            .minus(session.accumulatedPausedSeconds)
            .coerceAtLeast(0)
    }

    private suspend fun enqueueSessionChange(session: PomodoroSessionEntity, operation: String, createdAt: Long) {
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

    private suspend fun enqueueRunChange(run: PomodoroRunEntity, operation: String, createdAt: Long) {
        syncOutboxDao.enqueue(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entityType = "pomodoro_run",
                entityId = run.id,
                operation = operation,
                payloadJson = json.encodeToString(run.toSyncPayload()),
                createdAt = createdAt
            )
        )
    }

    private fun PomodoroSessionEntity.toSyncPayload(): PomodoroSessionSyncPayload = PomodoroSessionSyncPayload(
        id = id,
        runId = runId,
        roundIndex = roundIndex,
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

    private fun PomodoroRunEntity.toSyncPayload(): PomodoroRunSyncPayload = PomodoroRunSyncPayload(
        id = id,
        taskId = taskId,
        titleSnapshot = titleSnapshot,
        focusDurationSeconds = focusDurationSeconds,
        breakDurationSeconds = breakDurationSeconds,
        targetFocusCount = targetFocusCount,
        completedFocusCount = completedFocusCount,
        status = status,
        startedAt = startedAt,
        endedAt = endedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    @Serializable
    private data class PomodoroSessionSyncPayload(
        val id: String,
        val runId: String?,
        val roundIndex: Int,
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

    @Serializable
    private data class PomodoroRunSyncPayload(
        val id: String,
        val taskId: String?,
        val titleSnapshot: String?,
        val focusDurationSeconds: Int,
        val breakDurationSeconds: Int,
        val targetFocusCount: Int,
        val completedFocusCount: Int,
        val status: String,
        val startedAt: Long,
        val endedAt: Long?,
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
        const val TYPE_BREAK = "break"
        const val DEFAULT_BREAK_DURATION_SECONDS = 5 * 60
        private const val MIN_DURATION_SECONDS = 60
        private const val MAX_TARGET_FOCUS_COUNT = 12

        val json = Json {
            encodeDefaults = true
        }
    }
}
