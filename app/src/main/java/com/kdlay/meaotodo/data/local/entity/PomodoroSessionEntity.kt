package com.kdlay.meaotodo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSessionEntity(
    @PrimaryKey val id: String,
    val taskId: String? = null,
    val type: String,
    val plannedDurationSeconds: Int,
    val actualDurationSeconds: Int = 0,
    val startedAt: Long,
    val endedAt: Long? = null,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
