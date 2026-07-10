package com.kdlay.meaotodo.domain.assistant

import com.kdlay.meaotodo.data.local.entity.LedgerEntryEntity
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.data.repository.PomodoroRepository
import java.util.Calendar
import kotlinx.serialization.Serializable

@Serializable
data class DailyContext(
    val generatedAt: Long,
    val dayStart: Long,
    val dayEnd: Long,
    val pendingTasks: List<AssistantTaskSnapshot>,
    val todayTasks: List<AssistantTaskSnapshot>,
    val overdueTasks: List<AssistantTaskSnapshot>,
    val completedTodayCount: Int,
    val completedFocusCount: Int,
    val focusedMinutes: Int,
    val activeFocus: ActiveFocusSnapshot?,
    val todayExpenseCents: Long,
    val monthExpenseCents: Long,
    val monthlyBudgetCents: Long,
    val timelineEvents: List<DailyTimelineEvent> = emptyList()
) {
    val remainingBudgetCents: Long?
        get() = monthlyBudgetCents.takeIf { it > 0 }?.let { (it - monthExpenseCents).coerceAtLeast(0) }
}

@Serializable
data class AssistantTaskSnapshot(
    val id: String,
    val title: String,
    val listId: String,
    val priority: Int,
    val dueAt: Long?,
    val estimatedPomodoros: Int,
    val actualPomodoros: Int
) {
    val remainingPomodoros: Int
        get() = (estimatedPomodoros - actualPomodoros).coerceAtLeast(0)
}

@Serializable
data class ActiveFocusSnapshot(
    val sessionId: String,
    val taskId: String?,
    val title: String,
    val status: String,
    val plannedDurationSeconds: Int,
    val actualDurationSeconds: Int
)

@Serializable
enum class DailyTimelineEventType { TASK_COMPLETED, FOCUS_COMPLETED, EXPENSE }

@Serializable
data class DailyTimelineEvent(
    val id: String,
    val type: DailyTimelineEventType,
    val occurredAt: Long,
    val title: String,
    val detail: String,
    val amountCents: Long? = null
)

class DailyContextBuilder {
    fun build(
        tasks: List<TaskEntity>,
        sessions: List<PomodoroSessionEntity>,
        ledgerEntries: List<LedgerEntryEntity>,
        activeSession: PomodoroSessionEntity?,
        monthlyBudgetCents: Long,
        now: Long = System.currentTimeMillis()
    ): DailyContext {
        val (dayStart, dayEnd) = dayRange(now)
        val (monthStart, monthEnd) = monthRange(now)
        val activeTasks = tasks.filter { it.deletedAt == null }
        val pendingTasks = activeTasks.filterNot { it.isDone }
        val snapshots = pendingTasks.associate { it.id to it.toSnapshot() }
        val todayTasks = pendingTasks
            .filter { it.dueAt?.let { due -> due in dayStart until dayEnd } == true }
            .sortedWith(compareByDescending<TaskEntity> { it.priority }.thenBy { it.dueAt ?: Long.MAX_VALUE })
        val overdueTasks = pendingTasks
            .filter { it.dueAt?.let { due -> due < dayStart } == true }
            .sortedBy { it.dueAt }
        val finishedFocus = sessions.filter {
            it.deletedAt == null &&
                it.type == PomodoroRepository.TYPE_FOCUS &&
                it.status == PomodoroRepository.STATUS_FINISHED &&
                it.startedAt in dayStart until dayEnd
        }
        val expenses = ledgerEntries.filter { it.deletedAt == null && it.type == "expense" }

        return DailyContext(
            generatedAt = now,
            dayStart = dayStart,
            dayEnd = dayEnd,
            pendingTasks = pendingTasks.map { snapshots.getValue(it.id) },
            todayTasks = todayTasks.map { snapshots.getValue(it.id) },
            overdueTasks = overdueTasks.map { snapshots.getValue(it.id) },
            completedTodayCount = activeTasks.count { it.isDone && it.updatedAt in dayStart until dayEnd },
            completedFocusCount = finishedFocus.size,
            focusedMinutes = finishedFocus.sumOf { it.actualDurationSeconds }.div(60),
            activeFocus = activeSession?.takeIf { it.deletedAt == null }?.toActiveFocusSnapshot(),
            todayExpenseCents = expenses.filter { it.occurredAt in dayStart until dayEnd }.sumOf { it.amountCents },
            monthExpenseCents = expenses.filter { it.occurredAt in monthStart until monthEnd }.sumOf { it.amountCents },
            monthlyBudgetCents = monthlyBudgetCents.coerceAtLeast(0),
            timelineEvents = buildTimeline(activeTasks, sessions, expenses, dayStart, dayEnd)
        )
    }

    private fun buildTimeline(
        tasks: List<TaskEntity>,
        sessions: List<PomodoroSessionEntity>,
        expenses: List<LedgerEntryEntity>,
        dayStart: Long,
        dayEnd: Long
    ): List<DailyTimelineEvent> = buildList {
        tasks.filter { it.isDone && it.updatedAt in dayStart until dayEnd }.forEach { task ->
            add(
                DailyTimelineEvent(
                    id = "task:${task.id}:${task.updatedAt}",
                    type = DailyTimelineEventType.TASK_COMPLETED,
                    occurredAt = task.updatedAt,
                    title = task.title,
                    detail = "完成任务"
                )
            )
        }
        sessions.filter {
            it.deletedAt == null && it.type == PomodoroRepository.TYPE_FOCUS &&
                it.status == PomodoroRepository.STATUS_FINISHED && it.startedAt in dayStart until dayEnd
        }.forEach { session ->
            add(
                DailyTimelineEvent(
                    id = "focus:${session.id}",
                    type = DailyTimelineEventType.FOCUS_COMPLETED,
                    occurredAt = session.endedAt ?: session.updatedAt,
                    title = session.titleSnapshot ?: "空白专注",
                    detail = "专注 ${session.actualDurationSeconds / 60} 分钟"
                )
            )
        }
        expenses.filter { it.occurredAt in dayStart until dayEnd }.forEach { entry ->
            add(
                DailyTimelineEvent(
                    id = "expense:${entry.id}",
                    type = DailyTimelineEventType.EXPENSE,
                    occurredAt = entry.occurredAt,
                    title = entry.note.ifBlank { entry.category },
                    detail = entry.category,
                    amountCents = entry.amountCents
                )
            )
        }
    }.sortedByDescending { it.occurredAt }
}

private fun TaskEntity.toSnapshot() = AssistantTaskSnapshot(
    id = id,
    title = title,
    listId = listId,
    priority = priority,
    dueAt = dueAt,
    estimatedPomodoros = estimatedPomodoros,
    actualPomodoros = actualPomodoros
)

private fun PomodoroSessionEntity.toActiveFocusSnapshot() = ActiveFocusSnapshot(
    sessionId = id,
    taskId = taskId,
    title = titleSnapshot ?: if (type == PomodoroRepository.TYPE_BREAK) "休息" else "空白专注",
    status = status,
    plannedDurationSeconds = plannedDurationSeconds,
    actualDurationSeconds = actualDurationSeconds
)

private fun dayRange(timestamp: Long): Pair<Long, Long> {
    val start = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val end = Calendar.getInstance().apply {
        timeInMillis = start
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
    return start to end
}

private fun monthRange(timestamp: Long): Pair<Long, Long> {
    val start = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val end = Calendar.getInstance().apply {
        timeInMillis = start
        add(Calendar.MONTH, 1)
    }.timeInMillis
    return start to end
}
