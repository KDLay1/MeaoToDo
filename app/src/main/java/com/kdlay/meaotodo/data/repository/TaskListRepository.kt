package com.kdlay.meaotodo.data.repository

import com.kdlay.meaotodo.data.local.dao.SyncOutboxDao
import com.kdlay.meaotodo.data.local.dao.TaskListDao
import com.kdlay.meaotodo.data.local.entity.DEFAULT_TASK_LIST_ID
import com.kdlay.meaotodo.data.local.entity.SyncOutboxEntity
import com.kdlay.meaotodo.data.local.entity.TaskListEntity
import java.util.UUID
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TaskListRepository(
    private val taskListDao: TaskListDao,
    private val syncOutboxDao: SyncOutboxDao
) {
    val activeTaskLists = taskListDao.observeActiveLists()
        .map { lists -> lists.filterNot { it.id == DEFAULT_TASK_LIST_ID } }

    suspend fun ensureDefaultList() {
        val existing = taskListDao.findById(DEFAULT_TASK_LIST_ID)
        if (existing != null) return
        val now = System.currentTimeMillis()
        val taskList = TaskListEntity(
            id = DEFAULT_TASK_LIST_ID,
            name = "收集箱",
            sortOrder = Int.MIN_VALUE,
            createdAt = now,
            updatedAt = now
        )
        taskListDao.upsert(taskList)
    }

    suspend fun createList(name: String): Boolean {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return false
        val now = System.currentTimeMillis()
        val taskList = TaskListEntity(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            sortOrder = (now % Int.MAX_VALUE).toInt(),
            createdAt = now,
            updatedAt = now
        )
        taskListDao.upsert(taskList)
        enqueueChange(taskList, operation = "upsert", createdAt = now)
        return true
    }

    suspend fun renameList(id: String, name: String): Boolean {
        val cleanName = name.trim()
        if (id == DEFAULT_TASK_LIST_ID || cleanName.isBlank()) return false
        val existing = taskListDao.findById(id) ?: return false
        val now = System.currentTimeMillis()
        val updated = existing.copy(name = cleanName, updatedAt = now)
        taskListDao.rename(id = id, name = cleanName, updatedAt = now)
        enqueueChange(updated, operation = "upsert", createdAt = now)
        return true
    }

    suspend fun setSortOrder(id: String, sortOrder: Int): Boolean {
        val existing = taskListDao.findById(id) ?: return false
        val now = System.currentTimeMillis()
        val updated = existing.copy(sortOrder = sortOrder, updatedAt = now)
        taskListDao.setSortOrder(id = id, sortOrder = sortOrder, updatedAt = now)
        enqueueChange(updated, operation = "upsert", createdAt = now)
        return true
    }

    suspend fun removeList(id: String): Boolean {
        if (id == DEFAULT_TASK_LIST_ID) return false
        val existing = taskListDao.findById(id) ?: return false
        val now = System.currentTimeMillis()
        val updated = existing.copy(deletedAt = now, updatedAt = now)
        taskListDao.softDelete(id = id, deletedAt = now)
        enqueueChange(updated, operation = "delete", createdAt = now)
        return true
    }

    private suspend fun enqueueChange(taskList: TaskListEntity, operation: String, createdAt: Long) {
        syncOutboxDao.enqueue(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entityType = "task_list",
                entityId = taskList.id,
                operation = operation,
                payloadJson = json.encodeToString(taskList.toSyncPayload()),
                createdAt = createdAt
            )
        )
    }

    private fun TaskListEntity.toSyncPayload(): TaskListSyncPayload = TaskListSyncPayload(
        id = id,
        name = name,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    @Serializable
    private data class TaskListSyncPayload(
        val id: String,
        val name: String,
        val sortOrder: Int,
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
