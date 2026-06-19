package com.kdlay.meaotodo.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {
    val tasks: StateFlow<List<TaskEntity>> = taskRepository.activeTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun addTask(title: String, note: String, priority: Int, dueAt: Long?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskRepository.addTask(
                title = title,
                note = note,
                priority = priority,
                dueAt = dueAt
            )
        }
    }

    fun setDone(task: TaskEntity, isDone: Boolean) {
        viewModelScope.launch {
            taskRepository.setDone(task.id, isDone)
        }
    }

    companion object {
        fun factory(taskRepository: TaskRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(TodoViewModel::class.java))
                    return TodoViewModel(taskRepository) as T
                }
            }
    }
}
