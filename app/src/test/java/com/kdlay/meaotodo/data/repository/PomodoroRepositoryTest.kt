package com.kdlay.meaotodo.data.repository

import com.kdlay.meaotodo.data.local.dao.PomodoroDao
import com.kdlay.meaotodo.data.local.dao.PomodoroRunDao
import com.kdlay.meaotodo.data.local.dao.SyncOutboxDao
import com.kdlay.meaotodo.data.local.entity.PomodoroRunEntity
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PomodoroRepositoryTest {
    private lateinit var pomodoroDao: FakePomodoroDao
    private lateinit var pomodoroRunDao: FakePomodoroRunDao
    private lateinit var syncOutboxDao: FakeSyncOutboxDao
    private lateinit var repository: PomodoroRepository

    @Before
    fun setUp() {
        pomodoroDao = FakePomodoroDao()
        pomodoroRunDao = FakePomodoroRunDao()
        syncOutboxDao = FakeSyncOutboxDao()
        repository = PomodoroRepository(
            pomodoroDao = pomodoroDao,
            pomodoroRunDao = pomodoroRunDao,
            syncOutboxDao = syncOutboxDao
        )
    }

    @Test
    fun startRun_whenNoActiveRun_createsRunningRunFirstFocusSessionAndOutboxChanges() = runTest {
        val started = repository.startRun(
            taskId = "task-1",
            titleSnapshot = "  Read paper  ",
            focusDurationSeconds = 25 * 60,
            breakDurationSeconds = 5 * 60,
            targetFocusCount = 2
        )

        assertTrue(started)

        val run = requireNotNullForTest(pomodoroRunDao.activeRunOrNull())
        assertEquals("task-1", run.taskId)
        assertEquals("Read paper", run.titleSnapshot)
        assertEquals(25 * 60, run.focusDurationSeconds)
        assertEquals(5 * 60, run.breakDurationSeconds)
        assertEquals(2, run.targetFocusCount)
        assertEquals(0, run.completedFocusCount)
        assertEquals(PomodoroRepository.STATUS_RUNNING, run.status)

        val session = requireNotNullForTest(pomodoroDao.activeSessionOrNull())
        assertEquals(run.id, session.runId)
        assertEquals("task-1", session.taskId)
        assertEquals("Read paper", session.titleSnapshot)
        assertEquals(PomodoroRepository.TYPE_FOCUS, session.type)
        assertEquals(1, session.roundIndex)
        assertEquals(25 * 60, session.plannedDurationSeconds)
        assertEquals(PomodoroRepository.STATUS_RUNNING, session.status)

        val pending = syncOutboxDao.allPending()
        assertEquals(2, pending.size)
        assertTrue(pending.any { it.entityType == "pomodoro_run" && it.entityId == run.id && it.operation == "upsert" })
        assertTrue(pending.any { it.entityType == "pomodoro_session" && it.entityId == session.id && it.operation == "upsert" })
    }

    @Test
    fun startRun_whenActiveRunAlreadyExists_returnsFalseAndDoesNotCreateSecondRun() = runTest {
        assertTrue(repository.startRun(null, "first", focusDurationSeconds = 25 * 60, targetFocusCount = 1))

        val startedAgain = repository.startRun(null, "second", focusDurationSeconds = 25 * 60, targetFocusCount = 1)

        assertFalse(startedAgain)
        assertEquals(1, pomodoroRunDao.allRuns().size)
        assertEquals(1, pomodoroDao.allSessions().size)
        assertEquals(2, syncOutboxDao.allPending().size)
    }

    @Test
    fun startRun_whenDurationAndTargetAreOutOfRange_clampsToSafeBounds() = runTest {
        assertTrue(
            repository.startRun(
                taskId = null,
                titleSnapshot = "short",
                focusDurationSeconds = 1,
                breakDurationSeconds = 0,
                targetFocusCount = 99
            )
        )

        val run = requireNotNullForTest(pomodoroRunDao.activeRunOrNull())
        val session = requireNotNullForTest(pomodoroDao.activeSessionOrNull())

        assertEquals(60, run.focusDurationSeconds)
        assertEquals(60, run.breakDurationSeconds)
        assertEquals(12, run.targetFocusCount)
        assertEquals(60, session.plannedDurationSeconds)
    }

    @Test
    fun advanceIfNeeded_whenFocusHasNotReachedPlannedDuration_keepsCurrentSessionRunning() = runTest {
        assertTrue(repository.startRun(null, "focus", focusDurationSeconds = 25 * 60, targetFocusCount = 1))
        val session = requireNotNullForTest(pomodoroDao.activeSessionOrNull())

        val advanced = repository.advanceIfNeeded(
            now = session.startedAt + session.plannedDurationSeconds * 1_000L - 1L
        )

        assertFalse(advanced)
        val active = requireNotNullForTest(pomodoroDao.activeSessionOrNull())
        assertEquals(session.id, active.id)
        assertEquals(PomodoroRepository.TYPE_FOCUS, active.type)
        assertEquals(PomodoroRepository.STATUS_RUNNING, active.status)
        assertEquals(1, pomodoroDao.allSessions().size)
    }

    @Test
    fun advanceIfNeeded_whenFocusDurationElapsed_finishesFocusAndStartsBreak() = runTest {
        assertTrue(
            repository.startRun(
                taskId = "task-1",
                titleSnapshot = "Deep work",
                focusDurationSeconds = 25 * 60,
                breakDurationSeconds = 5 * 60,
                targetFocusCount = 2
            )
        )
        val focus = requireNotNullForTest(pomodoroDao.activeSessionOrNull())

        val advanced = repository.advanceIfNeeded(now = focus.finishedAtByPlan())

        assertTrue(advanced)
        val finishedFocus = requireNotNullForTest(pomodoroDao.findById(focus.id))
        assertEquals(PomodoroRepository.STATUS_FINISHED, finishedFocus.status)
        assertEquals(25 * 60, finishedFocus.actualDurationSeconds)
        assertNotNull(finishedFocus.endedAt)

        val breakSession = requireNotNullForTest(pomodoroDao.activeSessionOrNull())
        assertEquals(PomodoroRepository.TYPE_BREAK, breakSession.type)
        assertEquals("休息", breakSession.titleSnapshot)
        assertEquals(1, breakSession.roundIndex)
        assertEquals(5 * 60, breakSession.plannedDurationSeconds)
        assertEquals(PomodoroRepository.STATUS_RUNNING, breakSession.status)

        val run = requireNotNullForTest(pomodoroRunDao.activeRunOrNull())
        assertEquals(1, run.completedFocusCount)
        assertEquals(PomodoroRepository.STATUS_RUNNING, run.status)
    }

    @Test
    fun advanceIfNeeded_whenBreakDurationElapsedAndMoreFocusRoundsRemain_startsNextFocusRound() = runTest {
        assertTrue(repository.startRun(null, "Cycle", focusDurationSeconds = 25 * 60, breakDurationSeconds = 5 * 60, targetFocusCount = 2))
        advanceActiveSessionToPlannedEnd()
        val breakSession = requireNotNullForTest(pomodoroDao.activeSessionOrNull())

        val advanced = repository.advanceIfNeeded(now = breakSession.finishedAtByPlan())

        assertTrue(advanced)
        val finishedBreak = requireNotNullForTest(pomodoroDao.findById(breakSession.id))
        assertEquals(PomodoroRepository.STATUS_FINISHED, finishedBreak.status)

        val nextFocus = requireNotNullForTest(pomodoroDao.activeSessionOrNull())
        assertEquals(PomodoroRepository.TYPE_FOCUS, nextFocus.type)
        assertEquals(2, nextFocus.roundIndex)
        assertEquals(PomodoroRepository.STATUS_RUNNING, nextFocus.status)

        val run = requireNotNullForTest(pomodoroRunDao.activeRunOrNull())
        assertEquals(1, run.completedFocusCount)
        assertEquals(PomodoroRepository.STATUS_RUNNING, run.status)
    }

    @Test
    fun advanceIfNeeded_whenFinalBreakDurationElapsed_finishesRunAndLeavesNoActiveSession() = runTest {
        assertTrue(repository.startRun(null, "single", focusDurationSeconds = 25 * 60, breakDurationSeconds = 5 * 60, targetFocusCount = 1))
        advanceActiveSessionToPlannedEnd()
        val finalBreak = requireNotNullForTest(pomodoroDao.activeSessionOrNull())

        val advanced = repository.advanceIfNeeded(now = finalBreak.finishedAtByPlan())

        assertTrue(advanced)
        assertNull(pomodoroDao.activeSessionOrNull())
        assertNull(pomodoroRunDao.activeRunOrNull())

        val finishedRun = requireNotNullForTest(pomodoroRunDao.allRuns().single())
        assertEquals(PomodoroRepository.STATUS_FINISHED, finishedRun.status)
        assertEquals(1, finishedRun.completedFocusCount)
        assertNotNull(finishedRun.endedAt)

        val sessions = pomodoroDao.allSessions()
        assertEquals(2, sessions.size)
        assertTrue(sessions.all { it.status == PomodoroRepository.STATUS_FINISHED })
    }

    @Test
    fun pauseAndResume_whenSessionIsActive_updatesBothSessionAndRunStatus() = runTest {
        assertTrue(repository.startRun(null, "pause", focusDurationSeconds = 25 * 60, targetFocusCount = 1))
        val runningSession = requireNotNullForTest(pomodoroDao.activeSessionOrNull())

        assertTrue(repository.pause(runningSession.id))
        val pausedSession = requireNotNullForTest(pomodoroDao.activeSessionOrNull())
        val pausedRun = requireNotNullForTest(pomodoroRunDao.activeRunOrNull())
        assertEquals(PomodoroRepository.STATUS_PAUSED, pausedSession.status)
        assertEquals(PomodoroRepository.STATUS_PAUSED, pausedRun.status)
        assertNotNull(pausedSession.pausedAt)

        assertFalse(repository.pause(runningSession.id))

        assertTrue(repository.resume(runningSession.id))
        val resumedSession = requireNotNullForTest(pomodoroDao.activeSessionOrNull())
        val resumedRun = requireNotNullForTest(pomodoroRunDao.activeRunOrNull())
        assertEquals(PomodoroRepository.STATUS_RUNNING, resumedSession.status)
        assertEquals(PomodoroRepository.STATUS_RUNNING, resumedRun.status)
        assertNull(resumedSession.pausedAt)
    }

    @Test
    fun cancelActiveRun_whenRunIsActive_marksRunAndSessionCancelledAndClearsActiveState() = runTest {
        assertTrue(repository.startRun("task-1", "cancel", focusDurationSeconds = 25 * 60, targetFocusCount = 1))
        val runningSession = requireNotNullForTest(pomodoroDao.activeSessionOrNull())
        val runningRun = requireNotNullForTest(pomodoroRunDao.activeRunOrNull())

        assertTrue(repository.cancelActiveRun())

        assertNull(pomodoroDao.activeSessionOrNull())
        assertNull(pomodoroRunDao.activeRunOrNull())
        assertEquals(PomodoroRepository.STATUS_CANCELLED, requireNotNullForTest(pomodoroDao.findById(runningSession.id)).status)
        assertEquals(PomodoroRepository.STATUS_CANCELLED, requireNotNullForTest(pomodoroRunDao.findById(runningRun.id)).status)
        assertTrue(syncOutboxDao.allPending().count { it.entityType == "pomodoro_session" } >= 2)
        assertTrue(syncOutboxDao.allPending().count { it.entityType == "pomodoro_run" } >= 2)
    }

    @Test
    fun skipBreak_whenCurrentSessionIsFocus_returnsFalseAndKeepsFocusRunning() = runTest {
        assertTrue(repository.startRun(null, "focus", focusDurationSeconds = 25 * 60, targetFocusCount = 2))
        val focus = requireNotNullForTest(pomodoroDao.activeSessionOrNull())

        val skipped = repository.skipBreak()

        assertFalse(skipped)
        val active = requireNotNullForTest(pomodoroDao.activeSessionOrNull())
        assertEquals(focus.id, active.id)
        assertEquals(PomodoroRepository.TYPE_FOCUS, active.type)
        assertEquals(PomodoroRepository.STATUS_RUNNING, active.status)
    }

    @Test
    fun skipBreak_whenCurrentSessionIsBreak_completesBreakAndStartsNextFocus() = runTest {
        assertTrue(repository.startRun(null, "break", focusDurationSeconds = 25 * 60, breakDurationSeconds = 5 * 60, targetFocusCount = 2))
        advanceActiveSessionToPlannedEnd()
        val breakSession = requireNotNullForTest(pomodoroDao.activeSessionOrNull())
        assertEquals(PomodoroRepository.TYPE_BREAK, breakSession.type)

        val skipped = repository.skipBreak()

        assertTrue(skipped)
        assertEquals(PomodoroRepository.STATUS_FINISHED, requireNotNullForTest(pomodoroDao.findById(breakSession.id)).status)
        val nextFocus = requireNotNullForTest(pomodoroDao.activeSessionOrNull())
        assertEquals(PomodoroRepository.TYPE_FOCUS, nextFocus.type)
        assertEquals(2, nextFocus.roundIndex)
    }

    private suspend fun advanceActiveSessionToPlannedEnd() {
        val active = requireNotNullForTest(pomodoroDao.activeSessionOrNull())
        val advanced = repository.advanceIfNeeded(now = active.finishedAtByPlan())
        assertTrue(advanced)
    }

    private fun PomodoroSessionEntity.finishedAtByPlan(): Long = startedAt + plannedDurationSeconds * 1_000L

    private fun <T : Any> requireNotNullForTest(value: T?): T {
        assertNotNull(value)
        return value!!
    }

    private class FakePomodoroDao : PomodoroDao {
        private val sessions = linkedMapOf<String, PomodoroSessionEntity>()

        override fun observeSessions(): Flow<List<PomodoroSessionEntity>> = flow { emit(allSessions()) }

        override fun observeRecentSessions(limit: Int): Flow<List<PomodoroSessionEntity>> = flow {
            emit(allSessions().take(limit))
        }

        override fun observeActiveSession(): Flow<PomodoroSessionEntity?> = flow { emit(findActiveSession()) }

        override suspend fun findActiveSession(): PomodoroSessionEntity? = activeSessionOrNull()

        override suspend fun findById(id: String): PomodoroSessionEntity? = sessions[id]

        override suspend fun upsert(session: PomodoroSessionEntity) {
            sessions[session.id] = session
        }

        fun allSessions(): List<PomodoroSessionEntity> = sessions.values
            .filter { it.deletedAt == null }
            .sortedByDescending { it.startedAt }

        fun activeSessionOrNull(): PomodoroSessionEntity? = sessions.values
            .filter { it.deletedAt == null && it.status in ACTIVE_STATUSES }
            .maxWithOrNull(compareBy<PomodoroSessionEntity> { it.startedAt }.thenBy { it.updatedAt })
    }

    private class FakePomodoroRunDao : PomodoroRunDao {
        private val runs = linkedMapOf<String, PomodoroRunEntity>()

        override fun observeActiveRun(): Flow<PomodoroRunEntity?> = flow { emit(findActiveRun()) }

        override suspend fun findActiveRun(): PomodoroRunEntity? = activeRunOrNull()

        override suspend fun findById(id: String): PomodoroRunEntity? = runs[id]

        override suspend fun upsert(run: PomodoroRunEntity) {
            runs[run.id] = run
        }

        fun allRuns(): List<PomodoroRunEntity> = runs.values
            .filter { it.deletedAt == null }
            .sortedByDescending { it.startedAt }

        fun activeRunOrNull(): PomodoroRunEntity? = runs.values
            .filter { it.deletedAt == null && it.status in ACTIVE_STATUSES }
            .maxWithOrNull(compareBy<PomodoroRunEntity> { it.startedAt }.thenBy { it.updatedAt })
    }

    private class FakeSyncOutboxDao : SyncOutboxDao {
        private val changes = linkedMapOf<String, SyncOutboxEntity>()

        override suspend fun pending(limit: Int): List<SyncOutboxEntity> = changes.values
            .filter { it.deliveredAt == null }
            .sortedBy { it.createdAt }
            .take(limit)

        override suspend fun enqueue(change: SyncOutboxEntity) {
            changes[change.id] = change
        }

        override suspend fun markDelivered(ids: List<String>, deliveredAt: Long) {
            ids.forEach { id ->
                changes[id]?.let { changes[id] = it.copy(deliveredAt = deliveredAt) }
            }
        }

        fun allPending(): List<SyncOutboxEntity> = changes.values
            .filter { it.deliveredAt == null }
            .sortedBy { it.createdAt }
    }

    private companion object {
        val ACTIVE_STATUSES = setOf(
            PomodoroRepository.STATUS_RUNNING,
            PomodoroRepository.STATUS_PAUSED
        )
    }
}
