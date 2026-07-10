package com.kdlay.meaotodo.domain.assistant

import com.kdlay.meaotodo.data.local.entity.LedgerEntryEntity
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.data.repository.PomodoroRepository
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyContextTest {
    private val now = Calendar.getInstance().apply {
        set(2026, Calendar.JULY, 11, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private val dayStart = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun build_combinesTasksFocusLedgerAndBudget() {
        val tasks = listOf(
            task("today", dueAt = dayStart + 1_000, priority = 3),
            task("overdue", dueAt = dayStart - 1_000),
            task("done", dueAt = dayStart, isDone = true, updatedAt = now)
        )
        val sessions = listOf(finishedFocus("focus", dayStart + 2_000, actualSeconds = 1_500))
        val entries = listOf(expense("lunch", 3_200, dayStart + 3_000))

        val context = DailyContextBuilder().build(tasks, sessions, entries, null, 300_000, now)

        assertEquals(listOf("today"), context.todayTasks.map { it.id })
        assertEquals(listOf("overdue"), context.overdueTasks.map { it.id })
        assertEquals(1, context.completedTodayCount)
        assertEquals(1, context.completedFocusCount)
        assertEquals(25, context.focusedMinutes)
        assertEquals(3_200L, context.todayExpenseCents)
        assertEquals(296_800L, context.remainingBudgetCents)
        assertEquals(3, context.timelineEvents.size)
    }

    @Test
    fun suggestionEngine_prefersActiveFocusAndLimitsOutput() {
        val active = PomodoroSessionEntity(
            id = "active", taskId = "today", titleSnapshot = "写报告", type = PomodoroRepository.TYPE_FOCUS,
            plannedDurationSeconds = 1_500, startedAt = now, status = PomodoroRepository.STATUS_RUNNING,
            createdAt = now, updatedAt = now
        )
        val context = DailyContextBuilder().build(
            tasks = listOf(task("today", dueAt = dayStart + 1_000), task("overdue", dueAt = dayStart - 1_000)),
            sessions = emptyList(), ledgerEntries = emptyList(), activeSession = active,
            monthlyBudgetCents = 100, now = now
        ).copy(monthExpenseCents = 200)

        val suggestions = LocalSuggestionEngine().build(context)

        assertEquals(3, suggestions.size)
        assertEquals(AssistantSuggestionKind.ACTIVE_FOCUS, suggestions.first().kind)
        assertTrue(suggestions.any { it.kind == AssistantSuggestionKind.OVERDUE_REVIEW })
        assertFalse(suggestions.any { it.kind == AssistantSuggestionKind.NEXT_TASK })
    }

    private fun task(id: String, dueAt: Long?, priority: Int = 0, isDone: Boolean = false, updatedAt: Long = now) = TaskEntity(
        id = id, title = id, priority = priority, dueAt = dueAt, isDone = isDone,
        createdAt = now, updatedAt = updatedAt
    )

    private fun finishedFocus(id: String, startedAt: Long, actualSeconds: Int) = PomodoroSessionEntity(
        id = id, type = PomodoroRepository.TYPE_FOCUS, plannedDurationSeconds = 1_500,
        actualDurationSeconds = actualSeconds, startedAt = startedAt, endedAt = startedAt + actualSeconds * 1_000L,
        status = PomodoroRepository.STATUS_FINISHED, createdAt = startedAt, updatedAt = startedAt
    )

    private fun expense(id: String, cents: Long, occurredAt: Long) = LedgerEntryEntity(
        id = id, amountCents = cents, type = "expense", category = "餐饮",
        occurredAt = occurredAt, createdAt = occurredAt, updatedAt = occurredAt
    )
}
