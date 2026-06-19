package com.kdlay.meaotodo.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.data.local.entity.TaskListEntity
import com.kdlay.meaotodo.data.repository.TodoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(
    private val todoRepository: TodoRepository
) : ViewModel() {
    val tasks: StateFlow<List<TaskEntity>> = todoRepository.activeTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val taskLists: StateFlow<List<TaskListEntity>> = todoRepository.activeTaskLists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun addTaskList(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            todoRepository.addTaskList(name)
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
            todoRepository.addTask(
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
            todoRepository.updateTask(
                id = task.id,
                listId = listId,
                title = title,
                note = note,
                priority = priority,
                dueAt = dueAt,
                estimatedPomodoros = estimatedPomodoros
            )
        }
    }

    fun setDone(task: TaskEntity, isDone: Boolean) {
        viewModelScope.launch {
            todoRepository.setDone(task.id, isDone)
        }
    }

    fun removeTask(task: TaskEntity) {
        viewModelScope.launch {
            todoRepository.removeTask(task.id)
        }
    }

    companion object {
        fun factory(todoRepository: TodoRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(TodoViewModel::class.java))
                    return TodoViewModel(todoRepository) as T
                }
            }
    }
}
