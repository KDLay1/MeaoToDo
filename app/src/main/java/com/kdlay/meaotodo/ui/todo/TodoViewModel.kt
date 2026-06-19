package com.kdlay.meaotodo.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.data.local.entity.TaskListEntity
import com.kdlay.meaotodo.data.repository.TaskListRepository
import com.kdlay.meaotodo.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(
    private val taskRepository: TaskRepository,
    private val taskListRepository: TaskListRepository
) : ViewModel() {
    val tasks: StateFlow<List<TaskEntity>> = taskRepository.activeTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val taskLists: StateFlow<List<TaskListEntity>> = taskListRepository.activeTaskLists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            taskListRepository.ensureDefaultList()
        }
    }

    fun addTaskList(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            if (!taskListRepository.createList(name)) {
                _messages.emit("清单创建失败")
            }
        }
    }

    fun renameTaskList(id: String, name: String) {
        viewModelScope.launch {
            if (!taskListRepository.renameList(id, name)) {
                _messages.emit("清单重命名失败")
            }
        }
    }

    fun addTask(
        listId: String,
        title: String,
        note: String,
        priority: Int,
        dueAt: Long?,
        estimatedPomodoros: Int
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskRepository.addTask(
                listId = listId,
                title = title,
                note = note,
                priority = priority,
                dueAt = dueAt,
                estimatedPomodoros = estimatedPomodoros
            )
        }
    }

    fun updateTask(
        task: TaskEntity,
        listId: String,
        title: String,
        note: String,
        priority: Int,
        dueAt: Long?,
        estimatedPomodoros: Int
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            if (!taskRepository.updateTask(task.id, listId, title, note, priority, dueAt, estimatedPomodoros)) {
                _messages.emit("任务更新失败")
            }
        }
    }

    fun setDone(task: TaskEntity, isDone: Boolean) {
        viewModelScope.launch {
            if (!taskRepository.setDone(task.id, isDone)) {
                _messages.emit("任务状态更新失败")
            }
        }
    }

    fun removeTask(task: TaskEntity) {
        viewModelScope.launch {
            if (!taskRepository.removeTask(task.id)) {
                _messages.emit("任务删除失败")
            }
        }
    }

    companion object {
        fun factory(
            taskRepository: TaskRepository,
            taskListRepository: TaskListRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(TodoViewModel::class.java))
                return TodoViewModel(taskRepository, taskListRepository) as T
            }
        }
    }
}
