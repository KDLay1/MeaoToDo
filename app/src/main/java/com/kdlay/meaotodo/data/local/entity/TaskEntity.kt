package com.kdlay.meaotodo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

const val DEFAULT_TASK_LIST_ID = "inbox"

@Entity(tableName = "tasks")
@Serializable
data class TaskEntity(
    @PrimaryKey val id: String,
    val listId: String = DEFAULT_TASK_LIST_ID,
    val title: String,
    val note: String = "",
    val isDone: Boolean = false,
    val priority: Int = 0,
    val dueAt: Long? = null,
    val hasDueTime: Boolean = false,
    val estimatedPomodoros: Int = 0,
    val actualPomodoros: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
