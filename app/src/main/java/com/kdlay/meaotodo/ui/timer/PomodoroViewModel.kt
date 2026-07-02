package com.kdlay.meaotodo.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kdlay.meaotodo.data.local.entity.PomodoroRunEntity
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.core.settings.AppSettingsStore
import com.kdlay.meaotodo.core.settings.PomodoroPreferences
import com.kdlay.meaotodo.data.repository.PomodoroRepository
import com.kdlay.meaotodo.data.repository.TaskRepository
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PomodoroViewModel(
    private val pomodoroRepository: PomodoroRepository,
    private val settingsStore: AppSettingsStore,
    taskRepository: TaskRepository
) : ViewModel() {
    private val nowMillis = MutableStateFlow(System.currentTimeMillis())
    private val tasksState = taskRepository.activeTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val timerConfigState = combine(
        nowMillis,
        settingsStore.pomodoroPreferences
    ) { now, preferences -> now to preferences }

    val uiState: StateFlow<PomodoroUiState> = combine(
        pomodoroRepository.activeSession,
        pomodoroRepository.activeRun,
        pomodoroRepository.sessions,
        tasksState,
        timerConfigState
    ) { activeSession, activeRun, sessions, tasks, timerConfig ->
        val (now, preferences) = timerConfig
        PomodoroUiState(
            activeSession = activeSession,
            activeRun = activeRun,
            tasks = tasks,
            recentSessions = sessions.take(10),
            summary = buildTodaySummary(sessions, now),
            nowMillis = now,
            preferences = preferences
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PomodoroUiState()
    )

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                nowMillis.value = now
                pomodoroRepository.advanceIfNeeded(now)
                delay(1_000)
            }
        }
    }

    fun updateFocusDurationMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsStore.setPomodoroFocusDurationMinutes(minutes)
        }
    }

    fun updateBreakDurationMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsStore.setPomodoroBreakDurationMinutes(minutes)
        }
    }

    fun updateTargetFocusCount(count: Int) {
        viewModelScope.launch {
            settingsStore.setPomodoroTargetFocusCount(count)
        }
    }

    fun toggleClockStyle() {
        viewModelScope.launch {
            val nextStyle = if (uiState.value.preferences.clockStyle == PomodoroPreferences.CLOCK_STYLE_FLIP) {
                PomodoroPreferences.DEFAULT_CLOCK_STYLE
            } else {
                PomodoroPreferences.CLOCK_STYLE_FLIP
            }
            settingsStore.setPomodoroClockStyle(nextStyle)
        }
    }
    fun start(
        taskId: String?,
        durationMinutes: Int,
        breakDurationMinutes: Int,
        targetFocusCount: Int
    ) {
        viewModelScope.launch {
            val cleanTaskId = taskId?.takeIf { it.isNotBlank() }
            val task = cleanTaskId?.let { id -> tasksState.value.firstOrNull { it.id == id } }
            val started = pomodoroRepository.startRun(
                taskId = task?.id,
                titleSnapshot = task?.title,
                focusDurationSeconds = durationMinutes.coerceAtLeast(1) * 60,
                breakDurationSeconds = breakDurationMinutes.coerceIn(1, 120) * 60,
                targetFocusCount = targetFocusCount.coerceIn(1, 12)
            )
            if (!started) {
                _messages.emit("已有进行中的番茄钟")
            }
        }
    }

    fun pause() {
        val id = uiState.value.activeSession?.id ?: return
        viewModelScope.launch {
            if (!pomodoroRepository.pause(id)) {
                _messages.emit("暂停失败")
            }
        }
    }

    fun resume() {
        val id = uiState.value.activeSession?.id ?: return
        viewModelScope.launch {
            if (!pomodoroRepository.resume(id)) {
                _messages.emit("继续失败")
            }
        }
    }

    fun completeCurrentSession() {
        viewModelScope.launch {
            if (!pomodoroRepository.completeCurrentSession()) {
                _messages.emit("完成当前阶段失败")
            }
        }
    }

    fun skipBreak() {
        viewModelScope.launch {
            if (!pomodoroRepository.skipBreak()) {
                _messages.emit("跳过休息失败")
            }
        }
    }

    fun cancel() {
        viewModelScope.launch {
            if (!pomodoroRepository.cancelActiveRun()) {
                _messages.emit("放弃失败")
            }
        }
    }

    companion object {
        fun factory(
            pomodoroRepository: PomodoroRepository,
            settingsStore: AppSettingsStore,
            taskRepository: TaskRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(PomodoroViewModel::class.java))
                return PomodoroViewModel(pomodoroRepository, settingsStore, taskRepository) as T
            }
        }
    }
}

data class PomodoroUiState(
    val activeSession: PomodoroSessionEntity? = null,
    val activeRun: PomodoroRunEntity? = null,
    val tasks: List<TaskEntity> = emptyList(),
    val recentSessions: List<PomodoroSessionEntity> = emptyList(),
    val summary: PomodoroSummary = PomodoroSummary(),
    val nowMillis: Long = System.currentTimeMillis(),
    val preferences: PomodoroPreferences = PomodoroPreferences()
) {
    val isRunning: Boolean
        get() = activeSession?.status == PomodoroRepository.STATUS_RUNNING

    val isPaused: Boolean
        get() = activeSession?.status == PomodoroRepository.STATUS_PAUSED

    val isBreak: Boolean
        get() = activeSession?.type == PomodoroRepository.TYPE_BREAK

    val phaseTitle: String
        get() = when {
            activeSession == null -> "准备开始"
            isBreak -> "休息中"
            else -> "专注中"
        }

    val statusLabel: String
        get() = when {
            activeSession == null -> "准备开始"
            isPaused && isBreak -> "休息暂停"
            isPaused -> "专注暂停"
            isBreak -> "休息中"
            else -> "专注中"
        }

    val taskTitle: String
        get() = if (isBreak) "休息" else activeSession?.titleSnapshot ?: "空白专注"

    val roundLabel: String
        get() {
            val session = activeSession ?: return ""
            val run = activeRun
            val total = run?.targetFocusCount ?: 1
            return if (session.type == PomodoroRepository.TYPE_BREAK) {
                "第 ${session.roundIndex} / $total 次休息"
            } else {
                "第 ${session.roundIndex} / $total 个番茄"
            }
        }

    val nextLabel: String
        get() {
            val session = activeSession ?: return ""
            val run = activeRun ?: return ""
            val breakMinutes = (run.breakDurationSeconds / 60).coerceAtLeast(1)
            return if (session.type == PomodoroRepository.TYPE_BREAK) {
                if (run.completedFocusCount < run.targetFocusCount) "下一轮：第 ${run.completedFocusCount + 1} 个番茄" else "休息结束后完成本轮"
            } else {
                "结束后休息 $breakMinutes 分钟"
            }
        }

    val elapsedSeconds: Int
        get() {
            val session = activeSession ?: return 0
            val endPoint = if (session.status == PomodoroRepository.STATUS_PAUSED) {
                session.pausedAt ?: nowMillis
            } else {
                nowMillis
            }
            return ((endPoint - session.startedAt) / 1_000).toInt()
                .minus(session.accumulatedPausedSeconds)
                .coerceAtLeast(0)
        }

    val remainingSeconds: Int
        get() {
            val session = activeSession ?: return 0
            return (session.plannedDurationSeconds - elapsedSeconds).coerceAtLeast(0)
        }

    val progress: Float
        get() {
            val session = activeSession ?: return 0f
            if (session.plannedDurationSeconds <= 0) return 0f
            return (elapsedSeconds.toFloat() / session.plannedDurationSeconds.toFloat()).coerceIn(0f, 1f)
        }
}

data class PomodoroSummary(
    val finishedFocusCount: Int = 0,
    val focusSeconds: Int = 0,
    val cancelledFocusCount: Int = 0,
    val finishedBreakCount: Int = 0
)

private fun buildTodaySummary(sessions: List<PomodoroSessionEntity>, now: Long): PomodoroSummary {
    val todayStart = startOfDay(now)
    val todaySessions = sessions.filter { it.deletedAt == null && it.startedAt >= todayStart }
    val finishedFocusSessions = todaySessions.filter {
        it.type == PomodoroRepository.TYPE_FOCUS && it.status == PomodoroRepository.STATUS_FINISHED
    }
    return PomodoroSummary(
        finishedFocusCount = finishedFocusSessions.size,
        focusSeconds = finishedFocusSessions.sumOf { it.actualDurationSeconds },
        cancelledFocusCount = todaySessions.count {
            it.type == PomodoroRepository.TYPE_FOCUS && it.status == PomodoroRepository.STATUS_CANCELLED
        },
        finishedBreakCount = todaySessions.count {
            it.type == PomodoroRepository.TYPE_BREAK && it.status == PomodoroRepository.STATUS_FINISHED
        }
    )
}

private fun startOfDay(timestamp: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
