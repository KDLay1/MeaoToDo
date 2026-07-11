package com.kdlay.meaotodo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "task_lists")
@Serializable
data class TaskListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
