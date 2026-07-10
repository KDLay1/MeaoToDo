package com.kdlay.meaotodo.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kdlay.meaotodo.data.local.entity.PomodoroRunEntity
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.LedgerEntryEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.data.repository.LedgerRepository
import com.kdlay.meaotodo.data.repository.PomodoroRepository
import com.kdlay.meaotodo.data.repository.TaskRepository
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BoardViewModel(
    taskRepository: TaskRepository,
    private val pomodoroRepository: PomodoroRepository,
    ledgerRepository: LedgerRepository
) : ViewModel() {
    private val nowMillis = MutableStateFlow(System.currentTimeMillis())
    private val sourceData = combine(
        taskRepository.activeTasks,
        pomodoroRepository.sessions,
        ledgerRepository.entries
    ) { tasks, sessions, ledgerEntries ->
        BoardSourceData(tasks, sessions, ledgerEntries)
    }

    val uiState: StateFlow<BoardUiState> = combine(
        sourceData,
        pomodoroRepository.activeSession,
        pomodoroRepository.activeRun,
        nowMillis
    ) { source, activeSession, activeRun, now ->
        val todayRange = dayRange(now)
        val weekStart = startOfWeek(now)
        val weekEnd = addDays(weekStart, 7)
        val monthRange = monthRange(now)
        val tasks = source.tasks
        val pendingTasks = tasks.filterNot { it.isDone }
        val todayTasks = pendingTasks.filter { task -> task.dueAt?.let { it in todayRange.first until todayRange.second } == true }
        val completedTodayCount = tasks.count { task ->
            task.isDone && task.updatedAt in todayRange.first until todayRange.second
        }
        val finishedFocusSessions = source.sessions.filter { session ->
            session.deletedAt == null &&
                session.type == PomodoroRepository.TYPE_FOCUS &&
                session.status == PomodoroRepository.STATUS_FINISHED
        }
        val todayFocusCount = finishedFocusSessions.count { it.startedAt in todayRange.first until todayRange.second }
        val remainingFocus = todayTasks.sumOf { (it.estimatedPomodoros - it.actualPomodoros).coerceAtLeast(0) }
        val focusTarget = (todayFocusCount + remainingFocus).coerceIn(1, 12)
        val weeklyFocusCounts = (0 until 7).map { dayOffset ->
            val start = addDays(weekStart, dayOffset)
            val end = addDays(start, 1)
            finishedFocusSessions.count { it.startedAt in start until end }
        }
        val activeExpenses = source.ledgerEntries.filter { it.deletedAt == null && it.type == "expense" }
        BoardUiState(
            pendingTasks = pendingTasks,
            todayTasks = todayTasks,
            activeSession = activeSession,
            activeRun = activeRun,
            todayExpenseCents = activeExpenses
                .filter { it.occurredAt in todayRange.first until todayRange.second }
                .sumOf { it.amountCents },
            weekExpenseCents = activeExpenses
                .filter { it.occurredAt in weekStart until weekEnd }
                .sumOf { it.amountCents },
            monthExpenseCents = activeExpenses
                .filter { it.occurredAt in monthRange.first until monthRange.second }
                .sumOf { it.amountCents },
            todayFocusCount = todayFocusCount,
            focusTarget = focusTarget,
            weeklyFocusCounts = weeklyFocusCounts,
            completedTodayCount = completedTodayCount,
            overdueCount = pendingTasks.count { task ->
                task.dueAt?.let { it < todayRange.first } == true
            },
            recommendations = buildFocusRecommendations(tasks, now),
            productivityPulse = buildProductivityPulse(
                completedToday = completedTodayCount,
                focusedToday = todayFocusCount,
                overdue = pendingTasks.count { task -> task.dueAt?.let { it < todayRange.first } == true }
            ),
            focusStreakDays = calculateFocusStreak(source.sessions, now),
            nowMillis = now
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BoardUiState()
    )

    init {
        viewModelScope.launch {
            while (true) {
                nowMillis.value = System.currentTimeMillis()
                delay(1_000)
            }
        }
    }

    companion object {
        fun factory(
            taskRepository: TaskRepository,
            pomodoroRepository: PomodoroRepository,
            ledgerRepository: LedgerRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(BoardViewModel::class.java))
                return BoardViewModel(taskRepository, pomodoroRepository, ledgerRepository) as T
            }
        }
    }
}

data class BoardUiState(
    val pendingTasks: List<TaskEntity> = emptyList(),
    val todayTasks: List<TaskEntity> = emptyList(),
    val activeSession: PomodoroSessionEntity? = null,
    val activeRun: PomodoroRunEntity? = null,
    val todayExpenseCents: Long = 0,
    val weekExpenseCents: Long = 0,
    val monthExpenseCents: Long = 0,
    val todayFocusCount: Int = 0,
    val focusTarget: Int = 1,
    val weeklyFocusCounts: List<Int> = List(7) { 0 },
    val completedTodayCount: Int = 0,
    val overdueCount: Int = 0,
    val recommendations: List<FocusRecommendation> = emptyList(),
    val productivityPulse: ProductivityPulse = ProductivityPulse(),
    val focusStreakDays: Int = 0,
    val nowMillis: Long = System.currentTimeMillis()
) {
    val highlightedTasks: List<TaskEntity>
        get() = (todayTasks.ifEmpty { pendingTasks }).take(4)

    val timerTitle: String
        get() = when {
            activeSession == null -> "\u6682\u65e0\u4e13\u6ce8"
            activeSession.type == PomodoroRepository.TYPE_BREAK -> "\u4f11\u606f\u4e2d"
            else -> activeSession.titleSnapshot ?: "\u7a7a\u767d\u4e13\u6ce8"
        }

    val timerStatus: String
        get() = when (activeSession?.status) {
            PomodoroRepository.STATUS_RUNNING -> if (activeSession.type == PomodoroRepository.TYPE_BREAK) "\u4f11\u606f\u8fdb\u884c\u4e2d" else "\u6b63\u5728\u4e13\u6ce8"
            PomodoroRepository.STATUS_PAUSED -> "\u5df2\u6682\u505c"
            else -> "Ready"
        }

    val timerTime: String
        get() = activeSession?.let { formatDuration(remainingSeconds(it, nowMillis)) } ?: "25:00"
}

private data class BoardSourceData(
    val tasks: List<TaskEntity>,
    val sessions: List<PomodoroSessionEntity>,
    val ledgerEntries: List<LedgerEntryEntity>
)

private fun remainingSeconds(session: PomodoroSessionEntity, now: Long): Int {
    val endPoint = if (session.status == PomodoroRepository.STATUS_PAUSED) session.pausedAt ?: now else now
    val elapsed = ((endPoint - session.startedAt) / 1_000).toInt()
        .minus(session.accumulatedPausedSeconds)
        .coerceAtLeast(0)
    return (session.plannedDurationSeconds - elapsed).coerceAtLeast(0)
}

private fun formatDuration(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

private fun dayRange(timestamp: Long): Pair<Long, Long> {
    val start = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return start to Calendar.getInstance().apply {
        timeInMillis = start
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
}

private fun startOfWeek(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = dayRange(timestamp).first
    firstDayOfWeek = Calendar.MONDAY
    while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) add(Calendar.DAY_OF_YEAR, -1)
}.timeInMillis

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

private fun addDays(timestamp: Long, days: Int): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    add(Calendar.DAY_OF_YEAR, days)
}.timeInMillis
