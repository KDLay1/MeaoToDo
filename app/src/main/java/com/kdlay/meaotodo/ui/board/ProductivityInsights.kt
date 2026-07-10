package com.kdlay.meaotodo.ui.board

import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.data.repository.PomodoroRepository
import java.util.Calendar

data class FocusRecommendation(
    val task: TaskEntity,
    val score: Int,
    val reasons: List<String>,
    val remainingPomodoros: Int
)

data class ProductivityPulse(
    val score: Int = 50,
    val label: String = "平稳",
    val completedToday: Int = 0,
    val focusedToday: Int = 0,
    val overdue: Int = 0
)

internal fun buildFocusRecommendations(
    tasks: List<TaskEntity>,
    now: Long,
    limit: Int = 3
): List<FocusRecommendation> {
    val todayStart = boardStartOfDay(now)
    val tomorrowStart = boardAddDays(todayStart, 1)
    val soonEnd = boardAddDays(todayStart, 3)
    return tasks.asSequence()
        .filter { !it.isDone && it.deletedAt == null }
        .map { task ->
            val reasons = mutableListOf<String>()
            var score = task.priority * 20
            when {
                task.dueAt != null && task.dueAt < todayStart -> {
                    score += 40
                    reasons += "已逾期"
                }
                task.dueAt != null && task.dueAt < tomorrowStart -> {
                    score += 30
                    reasons += "今天截止"
                }
                task.dueAt != null && task.dueAt < soonEnd -> {
                    score += 15
                    reasons += "近期截止"
                }
            }
            if (task.priority >= 2) reasons += if (task.priority == 3) "高优先级" else "中优先级"
            val remaining = (task.estimatedPomodoros - task.actualPomodoros).coerceAtLeast(0)
            if (remaining > 0) {
                score += remaining.coerceAtMost(6) * 2
                reasons += "剩余 $remaining 个番茄"
            }
            FocusRecommendation(
                task = task,
                score = score,
                reasons = reasons.ifEmpty { listOf("最近待办") },
                remainingPomodoros = remaining
            )
        }
        .sortedWith(
            compareByDescending<FocusRecommendation> { it.score }
                .thenBy { it.task.dueAt ?: Long.MAX_VALUE }
                .thenByDescending { it.task.updatedAt }
        )
        .take(limit.coerceAtLeast(0))
        .toList()
}

internal fun buildProductivityPulse(
    completedToday: Int,
    focusedToday: Int,
    overdue: Int
): ProductivityPulse {
    val score = (50 + completedToday * 8 + focusedToday * 5 - overdue * 7).coerceIn(0, 100)
    val label = when {
        score >= 80 -> "高效"
        score >= 60 -> "向上"
        score >= 40 -> "平稳"
        else -> "需减负"
    }
    return ProductivityPulse(score, label, completedToday, focusedToday, overdue)
}

internal fun calculateFocusStreak(
    sessions: List<PomodoroSessionEntity>,
    now: Long
): Int {
    val focusedDays = sessions.asSequence()
        .filter {
            it.deletedAt == null &&
                it.type == PomodoroRepository.TYPE_FOCUS &&
                it.status == PomodoroRepository.STATUS_FINISHED
        }
        .map { boardStartOfDay(it.startedAt) }
        .toSet()
    if (focusedDays.isEmpty()) return 0

    val today = boardStartOfDay(now)
    var cursor = if (today in focusedDays) today else boardAddDays(today, -1)
    var streak = 0
    while (cursor in focusedDays) {
        streak += 1
        cursor = boardAddDays(cursor, -1)
    }
    return streak
}

private fun boardStartOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun boardAddDays(timestamp: Long, days: Int): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    add(Calendar.DAY_OF_YEAR, days)
}.timeInMillis
