package com.kdlay.meaotodo.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.data.repository.PomodoroRepository
import com.kdlay.meaotodo.data.repository.TaskRepository
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
    taskRepository: TaskRepository
) : ViewModel() {
    private val nowMillis = MutableStateFlow(System.currentTimeMillis())
    private val tasksState = taskRepository.activeTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val uiState: StateFlow<PomodoroUiState> = combine(
        pomodoroRepository.activeSession,
        tasksState,
        nowMillis
    ) { activeSession, tasks, now ->
        PomodoroUiState(
            activeSession = activeSession,
            tasks = tasks,
            nowMillis = now
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
                nowMillis.value = System.currentTimeMillis()
                delay(1_000)
            }
        }
    }

    fun start(taskId: String?, durationMinutes: Int) {
        viewModelScope.launch {
            val cleanTaskId = taskId?.takeIf { it.isNotBlank() }
            val task = cleanTaskId?.let { id -> tasksState.value.firstOrNull { it.id == id } }
            val started = pomodoroRepository.startFocus(
                taskId = task?.id,
                titleSnapshot = task?.title,
                plannedDurationSeconds = durationMinutes.coerceAtLeast(1) * 60
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

    fun finish() {
        val id = uiState.value.activeSession?.id ?: return
        viewModelScope.launch {
            if (!pomodoroRepository.finish(id)) {
                _messages.emit("完成失败")
            }
        }
    }

    fun cancel() {
        val id = uiState.value.activeSession?.id ?: return
        viewModelScope.launch {
            if (!pomodoroRepository.cancel(id)) {
                _messages.emit("放弃失败")
            }
        }
    }

    companion object {
        fun factory(
            pomodoroRepository: PomodoroRepository,
            taskRepository: TaskRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(PomodoroViewModel::class.java))
                return PomodoroViewModel(pomodoroRepository, taskRepository) as T
            }
        }
    }
}

data class PomodoroUiState(
    val activeSession: PomodoroSessionEntity? = null,
    val tasks: List<TaskEntity> = emptyList(),
    val nowMillis: Long = System.currentTimeMillis()
) {
    val isRunning: Boolean
        get() = activeSession?.status == PomodoroRepository.STATUS_RUNNING

    val isPaused: Boolean
        get() = activeSession?.status == PomodoroRepository.STATUS_PAUSED

    val taskTitle: String
        get() = activeSession?.titleSnapshot ?: "空白专注"

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
