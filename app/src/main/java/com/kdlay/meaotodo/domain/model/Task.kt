package com.kdlay.meaotodo.domain.model

data class Task(
    val id: String,
    val title: String,
    val note: String,
    val isDone: Boolean,
    val priority: Int,
    val dueAt: Long?,
    val estimatedPomodoros: Int,
    val actualPomodoros: Int
)
