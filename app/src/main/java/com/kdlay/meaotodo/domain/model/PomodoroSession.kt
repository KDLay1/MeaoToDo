package com.kdlay.meaotodo.domain.model

data class PomodoroSession(
    val id: String,
    val taskId: String?,
    val type: String,
    val plannedDurationSeconds: Int,
    val actualDurationSeconds: Int,
    val startedAt: Long,
    val endedAt: Long?,
    val status: String
)
