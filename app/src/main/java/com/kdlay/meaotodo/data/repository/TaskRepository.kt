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
        dueAt: Long? = null
    ) {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val task = TaskEntity(
            id = id,
            title = title.trim(),
            note = note.trim(),
            priority = priority,
            dueAt = dueAt,
            createdAt = now,
            updatedAt = now
        )
        taskDao.upsert(task)
        enqueueChange(entityType = "task", entityId = id, operation = "upsert", createdAt = now)
    }

    suspend fun setDone(id: String, isDone: Boolean) {
        val now = System.currentTimeMillis()
        taskDao.setDone(id = id, isDone = isDone, updatedAt = now)
        enqueueChange(entityType = "task", entityId = id, operation = "upsert", createdAt = now)
    }

    private suspend fun enqueueChange(entityType: String, entityId: String, operation: String, createdAt: Long) {
        // 第一版先记录变更索引，后面再替换为完整 JSON 序列化。
        syncOutboxDao.enqueue(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payloadJson = "{}",
                createdAt = createdAt
            )
        )
    }
}
