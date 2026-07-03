package com.kdlay.meaotodo.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kdlay.meaotodo.data.local.entity.PomodoroRunEntity
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
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
    private val todayRange = dayRange(System.currentTimeMillis())

    val uiState: StateFlow<BoardUiState> = combine(
        taskRepository.activeTasks,
        pomodoroRepository.activeSession,
        pomodoroRepository.activeRun,
        ledgerRepository.observeExpenseSum(todayRange.first, todayRange.second),
        nowMillis
    ) { tasks, activeSession, activeRun, todayExpenseCents, now ->
        val pendingTasks = tasks.filterNot { it.isDone }
        val todayTasks = pendingTasks.filter { task -> task.dueAt?.let { it in todayRange.first until todayRange.second } == true }
        BoardUiState(
            pendingTasks = pendingTasks,
            todayTasks = todayTasks,
            activeSession = activeSession,
            activeRun = activeRun,
            todayExpenseCents = todayExpenseCents,
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
                pomodoroRepository.advanceIfNeeded(nowMillis.value)
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
