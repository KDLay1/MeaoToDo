package com.kdlay.meaotodo.data.repository

import com.kdlay.meaotodo.data.local.dao.SyncOutboxDao
import com.kdlay.meaotodo.data.local.dao.TaskDao
import com.kdlay.meaotodo.data.local.entity.DEFAULT_TASK_LIST_ID
import com.kdlay.meaotodo.data.local.entity.SyncOutboxEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskRepositoryTest {
    private lateinit var taskDao: FakeTaskDao
    private lateinit var syncOutboxDao: FakeSyncOutboxDao
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        taskDao = FakeTaskDao()
        syncOutboxDao = FakeSyncOutboxDao()
        repository = TaskRepository(taskDao = taskDao, syncOutboxDao = syncOutboxDao)
    }

    @Test
    fun addTask_whenValuesNeedNormalization_trimsTextClampsNumbersAndEnqueuesUpsert() = runTest {
        repository.addTask(
            listId = "",
            title = "  Finish homework  ",
            note = "  chapter 3  ",
            priority = 99,
            dueAt = null,
            hasDueTime = true,
            estimatedPomodoros = -5
        )

        val task = taskDao.allTasks().single()
        assertEquals(DEFAULT_TASK_LIST_ID, task.listId)
        assertEquals("Finish homework", task.title)
        assertEquals("chapter 3", task.note)
        assertEquals(3, task.priority)
        assertEquals(null, task.dueAt)
        assertFalse(task.hasDueTime)
        assertEquals(0, task.estimatedPomodoros)
        assertFalse(task.isDone)

        val change = syncOutboxDao.allPending().single()
        assertEquals("task", change.entityType)
        assertEquals(task.id, change.entityId)
        assertEquals("upsert", change.operation)
        assertTrue(change.payloadJson.contains("Finish homework"))
    }

    @Test
    fun addTask_whenDueTimeIsProvided_keepsDueAtAndHasDueTime() = runTest {
        val dueAt = 1_800_000_000_000L

        repository.addTask(
            listId = "study",
            title = "Read",
            dueAt = dueAt,
            hasDueTime = true,
            priority = -1,
            estimatedPomodoros = 3
        )

        val task = taskDao.allTasks().single()
        assertEquals("study", task.listId)
        assertEquals(dueAt, task.dueAt)
        assertTrue(task.hasDueTime)
        assertEquals(0, task.priority)
        assertEquals(3, task.estimatedPomodoros)
    }

    @Test
    fun updateTask_whenTaskExists_updatesFieldsAndEnqueuesUpsert() = runTest {
        repository.addTask(title = "Old", note = "old note", priority = 1)
        val original = taskDao.allTasks().single()
        val dueAt = 1_900_000_000_000L

        val updated = repository.updateTask(
            id = original.id,
            listId = "project",
            title = "  New title  ",
            note = "  new note  ",
            priority = 10,
            dueAt = dueAt,
            hasDueTime = true,
            estimatedPomodoros = 4
        )

        assertTrue(updated)
        val task = requireNotNullForTest(taskDao.findById(original.id))
        assertEquals("project", task.listId)
        assertEquals("New title", task.title)
        assertEquals("new note", task.note)
        assertEquals(3, task.priority)
        assertEquals(dueAt, task.dueAt)
        assertTrue(task.hasDueTime)
        assertEquals(4, task.estimatedPomodoros)
        assertTrue(task.updatedAt >= original.updatedAt)

        val latestChange = syncOutboxDao.allPending().last()
        assertEquals("task", latestChange.entityType)
        assertEquals(original.id, latestChange.entityId)
        assertEquals("upsert", latestChange.operation)
    }

    @Test
    fun updateTask_whenTaskDoesNotExist_returnsFalseAndDoesNotEnqueueChange() = runTest {
        val updated = repository.updateTask(
            id = "missing",
            listId = "project",
            title = "New",
            note = "",
            priority = 1,
            dueAt = null,
            hasDueTime = false,
            estimatedPomodoros = 0
        )

        assertFalse(updated)
        assertTrue(syncOutboxDao.allPending().isEmpty())
    }

    @Test
    fun setDone_whenTaskExists_togglesDoneStateAndEnqueuesUpsert() = runTest {
        repository.addTask(title = "Task")
        val task = taskDao.allTasks().single()

        val completed = repository.setDone(task.id, isDone = true)
        assertTrue(completed)
        assertTrue(requireNotNullForTest(taskDao.findById(task.id)).isDone)
        assertEquals("upsert", syncOutboxDao.allPending().last().operation)

        val reopened = repository.setDone(task.id, isDone = false)
        assertTrue(reopened)
        assertFalse(requireNotNullForTest(taskDao.findById(task.id)).isDone)
        assertEquals(3, syncOutboxDao.allPending().size)
    }

    @Test
    fun setDone_whenTaskDoesNotExist_returnsFalseAndDoesNotEnqueueChange() = runTest {
        val completed = repository.setDone("missing", isDone = true)

        assertFalse(completed)
        assertTrue(syncOutboxDao.allPending().isEmpty())
    }

    @Test
    fun removeTask_whenTaskExists_softDeletesTaskAndEnqueuesDelete() = runTest {
        repository.addTask(title = "Remove me")
        val task = taskDao.allTasks().single()

        val removed = repository.removeTask(task.id)

        assertTrue(removed)
        val deleted = requireNotNullForTest(taskDao.findById(task.id))
        assertNotNull(deleted.deletedAt)
        assertTrue(taskDao.allActiveTasks().isEmpty())

        val latestChange = syncOutboxDao.allPending().last()
        assertEquals("task", latestChange.entityType)
        assertEquals(task.id, latestChange.entityId)
        assertEquals("delete", latestChange.operation)
    }

    @Test
    fun removeTask_whenTaskDoesNotExist_returnsFalseAndDoesNotEnqueueChange() = runTest {
        val removed = repository.removeTask("missing")

        assertFalse(removed)
        assertTrue(syncOutboxDao.allPending().isEmpty())
    }

    private fun <T : Any> requireNotNullForTest(value: T?): T {
        assertNotNull(value)
        return value!!
    }

    private class FakeTaskDao : TaskDao {
        private val tasks = linkedMapOf<String, TaskEntity>()

        override fun observeActiveTasks(): Flow<List<TaskEntity>> = flow { emit(allActiveTasks()) }

        override suspend fun findById(id: String): TaskEntity? = tasks[id]

        override suspend fun upsert(task: TaskEntity) {
            tasks[task.id] = task
        }

        override suspend fun setDone(id: String, isDone: Boolean, updatedAt: Long) {
            tasks[id]?.let { existing ->
                tasks[id] = existing.copy(isDone = isDone, updatedAt = updatedAt)
            }
        }

        override suspend fun softDelete(id: String, deletedAt: Long) {
            tasks[id]?.let { existing ->
                tasks[id] = existing.copy(deletedAt = deletedAt, updatedAt = deletedAt)
            }
        }

        fun allTasks(): List<TaskEntity> = tasks.values.sortedBy { it.createdAt }

        fun allActiveTasks(): List<TaskEntity> = tasks.values
            .filter { it.deletedAt == null }
            .sortedWith(
                compareBy<TaskEntity> { it.isDone }
                    .thenByDescending { it.priority }
                    .thenBy { it.dueAt ?: Long.MAX_VALUE }
                    .thenByDescending { it.updatedAt }
            )
    }

    private class FakeSyncOutboxDao : SyncOutboxDao {
        private val changes = linkedMapOf<String, SyncOutboxEntity>()

        override suspend fun pending(limit: Int): List<SyncOutboxEntity> = changes.values
            .filter { it.deliveredAt == null }
            .sortedBy { it.createdAt }
            .take(limit)

        override suspend fun enqueue(change: SyncOutboxEntity) {
            changes[change.id] = change
        }

        override suspend fun markDelivered(ids: List<String>, deliveredAt: Long) {
            ids.forEach { id ->
                changes[id]?.let { changes[id] = it.copy(deliveredAt = deliveredAt) }
            }
        }

        fun allPending(): List<SyncOutboxEntity> = changes.values
            .filter { it.deliveredAt == null }
            .sortedBy { it.createdAt }
    }
}
