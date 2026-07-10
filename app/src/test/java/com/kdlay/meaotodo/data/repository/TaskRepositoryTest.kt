package com.kdlay.meaotodo.data.repository

import com.kdlay.meaotodo.data.local.dao.SyncOutboxDao
import com.kdlay.meaotodo.data.local.dao.TaskDao
import com.kdlay.meaotodo.data.local.entity.SyncOutboxEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskRepositoryTest {
    private lateinit var taskDao: FakeTaskDao
    private lateinit var outboxDao: FakeSyncOutboxDao
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        taskDao = FakeTaskDao()
        outboxDao = FakeSyncOutboxDao()
        repository = TaskRepository(taskDao, outboxDao)
    }

    @Test
    fun updateTask_persistsEditedNoteAndEnqueuesLatestPayload() = runTest {
        assertTrue(repository.addTask(title = "原任务", note = "旧备注"))
        val task = taskDao.activeTasks().single()

        assertTrue(
            repository.updateTask(
                id = task.id,
                listId = task.listId,
                title = "新任务",
                note = "新备注",
                priority = 3,
                dueAt = null,
                hasDueTime = false,
                estimatedPomodoros = 4
            )
        )

        val updated = requireNotNull(taskDao.findById(task.id))
        assertEquals("新任务", updated.title)
        assertEquals("新备注", updated.note)
        assertEquals(3, updated.priority)
        assertEquals(4, updated.estimatedPomodoros)
        assertTrue(outboxDao.pending().last().payloadJson.contains("新备注"))
    }

    @Test
    fun addAndUpdateTask_rejectBlankTitles() = runTest {
        assertFalse(repository.addTask(title = "   "))
        taskDao.upsert(task("task-1", "list-a"))

        assertFalse(repository.updateTask("task-1", "list-a", " ", "note", 0, null, false, 0))
        assertEquals("task-1", taskDao.findById("task-1")?.title)
        assertTrue(outboxDao.pending().isEmpty())
    }

    @Test
    fun moveTasksFromList_enqueuesEachMovedTaskForSync() = runTest {
        taskDao.upsert(task("task-1", "list-a"))
        taskDao.upsert(task("task-2", "list-a"))

        val moved = repository.moveTasksFromList("list-a", "inbox")

        assertEquals(2, moved)
        assertTrue(taskDao.activeTasks().all { it.listId == "inbox" })
        assertEquals(setOf("task-1", "task-2"), outboxDao.pending().map { it.entityId }.toSet())
    }

    private fun task(id: String, listId: String) = TaskEntity(
        id = id,
        listId = listId,
        title = id,
        createdAt = 1L,
        updatedAt = 1L
    )

    private class FakeTaskDao : TaskDao {
        private val tasks = linkedMapOf<String, TaskEntity>()

        override fun observeActiveTasks(): Flow<List<TaskEntity>> = flow { emit(activeTasks()) }
        override suspend fun findById(id: String): TaskEntity? = tasks[id]
        override suspend fun findActiveByListId(listId: String): List<TaskEntity> =
            activeTasks().filter { it.listId == listId }

        override suspend fun upsert(task: TaskEntity) {
            tasks[task.id] = task
        }

        override suspend fun setDone(id: String, isDone: Boolean, updatedAt: Long) {
            tasks[id]?.let { tasks[id] = it.copy(isDone = isDone, updatedAt = updatedAt) }
        }

        override suspend fun moveToList(id: String, targetListId: String, updatedAt: Long): Int {
            val task = tasks[id]?.takeIf { it.deletedAt == null } ?: return 0
            tasks[id] = task.copy(listId = targetListId, updatedAt = updatedAt)
            return 1
        }

        override suspend fun moveTasksFromList(sourceListId: String, targetListId: String, updatedAt: Long): Int {
            val matches = activeTasks().filter { it.listId == sourceListId }
            matches.forEach { tasks[it.id] = it.copy(listId = targetListId, updatedAt = updatedAt) }
            return matches.size
        }

        override suspend fun softDelete(id: String, deletedAt: Long) {
            tasks[id]?.let { tasks[id] = it.copy(deletedAt = deletedAt, updatedAt = deletedAt) }
        }

        override suspend fun incrementActualPomodoros(id: String, count: Int, updatedAt: Long) {
            tasks[id]?.let {
                tasks[id] = it.copy(actualPomodoros = it.actualPomodoros + count, updatedAt = updatedAt)
            }
        }

        fun activeTasks() = tasks.values.filter { it.deletedAt == null }
    }

    private class FakeSyncOutboxDao : SyncOutboxDao {
        private val changes = linkedMapOf<String, SyncOutboxEntity>()

        override suspend fun pending(limit: Int): List<SyncOutboxEntity> =
            changes.values.filter { it.deliveredAt == null }.take(limit)

        override suspend fun enqueue(change: SyncOutboxEntity) {
            changes[change.id] = change
        }

        override suspend fun markDelivered(ids: List<String>, deliveredAt: Long) {
            ids.forEach { id -> changes[id]?.let { changes[id] = it.copy(deliveredAt = deliveredAt) } }
        }
    }
}
