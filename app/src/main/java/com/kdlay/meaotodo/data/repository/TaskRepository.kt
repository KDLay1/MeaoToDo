package com.kdlay.meaotodo.data.repository

import com.kdlay.meaotodo.data.local.dao.SyncOutboxDao
import com.kdlay.meaotodo.data.local.dao.TaskDao
import com.kdlay.meaotodo.data.local.entity.DEFAULT_TASK_LIST_ID
import com.kdlay.meaotodo.data.local.entity.SyncOutboxEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TaskRepository(
    private val taskDao: TaskDao,
    private val syncOutboxDao: SyncOutboxDao
) {
    val activeTasks = taskDao.observeActiveTasks()

    suspend fun addTask(
        listId: String = DEFAULT_TASK_LIST_ID,
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
            listId = listId.ifBlank { DEFAULT_TASK_LIST_ID },
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
        listId: String,
        title: String,
        note: String,
        priority: Int,
        dueAt: Long?,
        estimatedPomodoros: Int
    ): Boolean {
        val existing = taskDao.findById(id) ?: return false
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            listId = listId.ifBlank { DEFAULT_TASK_LIST_ID },
            title = title.trim(),
            note = note.trim(),
            priority = priority.coerceIn(0, 3),
            dueAt = dueAt,
            estimatedPomodoros = estimatedPomodoros.coerceAtLeast(0),
            updatedAt = now
        )
        taskDao.upsert(updated)
        enqueueChange(task = updated, operation = "upsert", createdAt = now)
        return true
    }

    suspend fun setDone(id: String, isDone: Boolean): Boolean {
        val now = System.currentTimeMillis()
        taskDao.setDone(id = id, isDone = isDone, updatedAt = now)
        val updated = taskDao.findById(id) ?: return false
        enqueueChange(task = updated, operation = "upsert", createdAt = now)
        return true
    }

    suspend fun removeTask(id: String): Boolean {
        val now = System.currentTimeMillis()
        taskDao.softDelete(id = id, deletedAt = now)
        val deleted = taskDao.findById(id) ?: return false
        enqueueChange(task = deleted, operation = "delete", createdAt = now)
        return true
    }

    private suspend fun enqueueChange(task: TaskEntity, operation: String, createdAt: Long) {
        syncOutboxDao.enqueue(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entityType = "task",
                entityId = task.id,
                operation = operation,
                payloadJson = json.encodeToString(task.toSyncPayload()),
                createdAt = createdAt
            )
        )
    }

    private fun TaskEntity.toSyncPayload(): TaskSyncPayload = TaskSyncPayload(
        id = id,
        listId = listId,
        title = title,
        note = note,
        isDone = isDone,
        priority = priority,
        dueAt = dueAt,
        estimatedPomodoros = estimatedPomodoros,
        actualPomodoros = actualPomodoros,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    @Serializable
    private data class TaskSyncPayload(
        val id: String,
        val listId: String,
        val title: String,
        val note: String,
        val isDone: Boolean,
        val priority: Int,
        val dueAt: Long?,
        val estimatedPomodoros: Int,
        val actualPomodoros: Int,
        val createdAt: Long,
        val updatedAt: Long,
        val deletedAt: Long?
    )

    private companion object {
        val json = Json {
            encodeDefaults = true
        }
    }
}
