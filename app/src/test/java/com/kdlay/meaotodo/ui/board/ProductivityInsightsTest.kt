package com.kdlay.meaotodo.ui.board

import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.data.repository.PomodoroRepository
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductivityInsightsTest {
    private val now = Calendar.getInstance().apply {
        set(2026, Calendar.JULY, 10, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun recommendations_prioritizeOverdueHighPriorityWork() {
        val start = startOfDay(now)
        val recommendations = buildFocusRecommendations(
            listOf(
                task("normal", priority = 0, dueAt = null),
                task("urgent", priority = 3, dueAt = addDays(start, -1), estimated = 3),
                task("today", priority = 1, dueAt = start)
            ),
            now
        )

        assertEquals("urgent", recommendations.first().task.id)
        assertTrue(recommendations.first().reasons.contains("已逾期"))
        assertTrue(recommendations.first().reasons.contains("高优先级"))
    }

    @Test
    fun productivityPulse_isTransparentAndBounded() {
        val pulse = buildProductivityPulse(completedToday = 3, focusedToday = 2, overdue = 1)

        assertEquals(77, pulse.score)
        assertEquals("向上", pulse.label)
        assertEquals(100, buildProductivityPulse(100, 100, 0).score)
        assertEquals(0, buildProductivityPulse(0, 0, 100).score)
    }

    @Test
    fun focusStreak_countsBackFromTodayOrYesterday() {
        val today = startOfDay(now)
        val sessions = listOf(
            session(addDays(today, -1)),
            session(addDays(today, -2)),
            session(addDays(today, -3))
        )

        assertEquals(3, calculateFocusStreak(sessions, now))
    }

    @Test
    fun budgetPace_projectsMonthAndWarnsWhenCurrentPaceWillOvershoot() {
        val pace = buildBudgetPace(
            monthSpentCents = 200_000,
            budgetCents = 300_000,
            now = now
        )

        assertEquals(300_000L, pace.budgetCents)
        assertTrue(pace.projectedMonthCents > pace.budgetCents)
        assertEquals("按当前节奏可能超支", pace.status)
    }

    private fun task(id: String, priority: Int, dueAt: Long?, estimated: Int = 0) = TaskEntity(
        id = id,
        title = id,
        priority = priority,
        dueAt = dueAt,
        estimatedPomodoros = estimated,
        createdAt = 1,
        updatedAt = 1
    )

    private fun session(startedAt: Long) = PomodoroSessionEntity(
        id = startedAt.toString(),
        type = PomodoroRepository.TYPE_FOCUS,
        plannedDurationSeconds = 1_500,
        actualDurationSeconds = 1_500,
        startedAt = startedAt,
        endedAt = startedAt + 1_500_000,
        status = PomodoroRepository.STATUS_FINISHED,
        createdAt = startedAt,
        updatedAt = startedAt
    )

    private fun startOfDay(timestamp: Long) = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun addDays(timestamp: Long, days: Int) = Calendar.getInstance().apply {
        timeInMillis = timestamp
        add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis
}
