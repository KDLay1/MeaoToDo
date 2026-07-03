package com.kdlay.meaotodo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_runs")
data class PomodoroRunEntity(
    @PrimaryKey val id: String,
    val taskId: String? = null,
    val titleSnapshot: String? = null,
    val focusDurationSeconds: Int,
    val breakDurationSeconds: Int,
    val targetFocusCount: Int,
    val completedFocusCount: Int = 0,
    val status: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
