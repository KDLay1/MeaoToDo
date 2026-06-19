package com.kdlay.meaotodo.data.repository

import com.kdlay.meaotodo.data.local.dao.SyncOutboxDao
import com.kdlay.meaotodo.data.local.dao.TaskDao
import com.kdlay.meaotodo.data.local.entity.SyncOutboxEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import java.util.UUID

class TaskRepository(
    private val taskDao: TaskDao,
    private val syncOutboxDao: SyncOutboxDao
) {
    val activeTasks = taskDao.observeActiveTasks()

    suspend fun addTask(
        title: String,
        note: String = "",
        priority: Int = 0,
        dueAt: Long? = null,
        estimatedPomodoros: Int = 0
    ) {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val task = TaskEntity(
            id = id,
            title = title.trim(),
            note = note.trim(),
            priority = priority.coerceIn(0, 3),
            dueAt = dueAt,
            estimatedPomodoros = estimatedPomodoros.coerceAtLeast(0),
            createdAt = now,
            updatedAt = now
        )
        taskDao.upsert(task)
        enqueueChange(task = task, operation = "upsert", createdAt = now)
    }

    suspend fun updateTask(
        id: String,
        title: String,
        note: String,
        priority: Int,
        dueAt: Long?,
        estimatedPomodoros: Int
    ) {
        val existing = taskDao.findById(id) ?: return
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            title = title.trim(),
            note = note.trim(),
            priority = priority.coerceIn(0, 3),
            dueAt = dueAt,
            estimatedPomodoros = estimatedPomodoros.coerceAtLeast(0),
            updatedAt = now
        )
        taskDao.upsert(updated)
        enqueueChange(task = updated, operation = "upsert", createdAt = now)
    }

    suspend fun setDone(id: String, isDone: Boolean) {
        val now = System.currentTimeMillis()
        taskDao.setDone(id = id, isDone = isDone, updatedAt = now)
        val updated = taskDao.findById(id) ?: return
        enqueueChange(task = updated, operation = "upsert", createdAt = now)
    }

    suspend fun softDelete(id: String) {
        val now = System.currentTimeMillis()
        taskDao.softDelete(id = id, deletedAt = now)
        val deleted = taskDao.findById(id) ?: return
        enqueueChange(task = deleted, operation = "delete", createdAt = now)
    }

    private suspend fun enqueueChange(task: TaskEntity, operation: String, createdAt: Long) {
        syncOutboxDao.enqueue(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entityType = "task",
                entityId = task.id,
                operation = operation,
                payloadJson = task.toPayloadJson(),
                createdAt = createdAt
            )
        )
    }

    private fun TaskEntity.toPayloadJson(): String = buildString {
        append("{")
        appendJsonField("id", id)
        append(",")
        appendJsonField("title", title)
        append(",")
        appendJsonField("note", note)
        append(",\"isDone\":")
        append(isDone)
        append(",\"priority\":")
        append(priority)
        append(",\"dueAt\":")
        append(dueAt?.toString() ?: "null")
        append(",\"estimatedPomodoros\":")
        append(estimatedPomodoros)
        append(",\"actualPomodoros\":")
        append(actualPomodoros)
        append(",\"createdAt\":")
        append(createdAt)
        append(",\"updatedAt\":")
        append(updatedAt)
        append(",\"deletedAt\":")
        append(deletedAt?.toString() ?: "null")
        append("}")
    }

    private fun StringBuilder.appendJsonField(name: String, value: String) {
        append("\"")
        append(name)
        append("\":\"")
        append(value.escapeJson())
        append("\"")
    }

    private fun String.escapeJson(): String = buildString {
        for (char in this@escapeJson) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}
