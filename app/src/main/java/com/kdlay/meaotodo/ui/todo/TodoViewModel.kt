package com.kdlay.meaotodo.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kdlay.meaotodo.data.local.entity.DEFAULT_TASK_LIST_ID
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
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val created = taskListRepository.createList(cleanName)
            _messages.emit(if (created) "清单已创建" else "清单创建失败")
        }
    }

    fun renameTaskList(id: String, name: String) {
        viewModelScope.launch {
            if (taskListRepository.renameList(id, name)) {
                _messages.emit("清单已重命名")
            } else {
                _messages.emit("清单重命名失败")
            }
        }
    }

    fun removeTaskList(id: String) {
        viewModelScope.launch {
            if (id == DEFAULT_TASK_LIST_ID) {
                _messages.emit("收集箱不能删除")
                return@launch
            }
            val movedCount = taskRepository.moveTasksFromList(id, DEFAULT_TASK_LIST_ID)
            val removed = taskListRepository.removeList(id)
            _messages.emit(
                if (removed) {
                    if (movedCount > 0) "清单已删除，$movedCount 个任务已移入收集箱" else "清单已删除"
                } else {
                    "清单删除失败"
                }
            )
        }
    }

    fun addTask(
        listId: String,
        title: String,
        note: String,
        priority: Int,
        dueAt: Long?,
        hasDueTime: Boolean,
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
                hasDueTime = hasDueTime,
                estimatedPomodoros = estimatedPomodoros
            )
            _messages.emit("任务已添加")
        }
    }

    fun updateTask(
        task: TaskEntity,
        listId: String,
        title: String,
        note: String,
        priority: Int,
        dueAt: Long?,
        hasDueTime: Boolean,
        estimatedPomodoros: Int
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            if (taskRepository.updateTask(task.id, listId, title, note, priority, dueAt, hasDueTime, estimatedPomodoros)) {
                _messages.emit("任务已更新")
            } else {
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

    fun moveTask(task: TaskEntity, targetListId: String) {
        viewModelScope.launch {
            if (taskRepository.moveTask(task.id, targetListId)) {
                _messages.emit("任务已移动")
            } else {
                _messages.emit("任务移动失败")
            }
        }
    }

    fun duplicateTask(task: TaskEntity) {
        viewModelScope.launch {
            if (taskRepository.duplicateTask(task.id)) {
                _messages.emit("任务已复制")
            } else {
                _messages.emit("任务复制失败")
            }
        }
    }

    fun pinToday(task: TaskEntity) {
        viewModelScope.launch {
            if (taskRepository.pinToday(task.id)) {
                _messages.emit("已设为今日重点")
            } else {
                _messages.emit("设置今日重点失败")
            }
        }
    }

    fun archiveTask(task: TaskEntity) {
        viewModelScope.launch {
            if (taskRepository.removeTask(task.id)) {
                _messages.emit("任务已归档")
            } else {
                _messages.emit("任务归档失败")
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
