package com.kdlay.meaotodo.data.repository

import com.kdlay.meaotodo.data.local.dao.SyncOutboxDao
import com.kdlay.meaotodo.data.local.dao.TaskListDao
import com.kdlay.meaotodo.data.local.entity.DEFAULT_TASK_LIST_ID
import com.kdlay.meaotodo.data.local.entity.SyncOutboxEntity
import com.kdlay.meaotodo.data.local.entity.TaskListEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskListRepositoryTest {
    private lateinit var taskListDao: FakeTaskListDao
    private lateinit var syncOutboxDao: FakeSyncOutboxDao
    private lateinit var repository: TaskListRepository

    @Before
    fun setUp() {
        taskListDao = FakeTaskListDao()
        syncOutboxDao = FakeSyncOutboxDao()
        repository = TaskListRepository(taskListDao = taskListDao, syncOutboxDao = syncOutboxDao)
    }

    @Test
    fun ensureDefaultList_whenInboxMissing_createsInboxWithoutSyncOutboxChange() = runTest {
        repository.ensureDefaultList()

        val inbox = requireNotNullForTest(taskListDao.findById(DEFAULT_TASK_LIST_ID))
        assertEquals("收集箱", inbox.name)
        assertEquals(Int.MIN_VALUE, inbox.sortOrder)
        assertTrue(syncOutboxDao.allPending().isEmpty())
    }

    @Test
    fun ensureDefaultList_whenInboxAlreadyExists_doesNotDuplicateInbox() = runTest {
        repository.ensureDefaultList()
        repository.ensureDefaultList()

        assertEquals(1, taskListDao.allLists().count { it.id == DEFAULT_TASK_LIST_ID })
        assertTrue(syncOutboxDao.allPending().isEmpty())
    }

    @Test
    fun activeTaskLists_excludesDefaultInboxAndDeletedLists() = runTest {
        repository.ensureDefaultList()
        assertTrue(repository.createList(" Study "))
        assertTrue(repository.createList("Life"))
        val study = requireNotNullForTest(taskListDao.allLists().firstOrNull { it.name == "Study" })
        assertTrue(repository.removeList(study.id))

        val activeNames = repository.activeTaskLists.first().map { it.name }

        assertEquals(listOf("Life"), activeNames)
    }

    @Test
    fun createList_whenNameIsValid_trimsNameCreatesListAndEnqueuesUpsert() = runTest {
        val created = repository.createList("  Reading  ")

        assertTrue(created)
        val list = taskListDao.allActiveLists().single()
        assertEquals("Reading", list.name)
        assertTrue(list.id != DEFAULT_TASK_LIST_ID)

        val change = syncOutboxDao.allPending().single()
        assertEquals("task_list", change.entityType)
        assertEquals(list.id, change.entityId)
        assertEquals("upsert", change.operation)
        assertTrue(change.payloadJson.contains("Reading"))
    }

    @Test
    fun createList_whenNameIsBlank_returnsFalseAndDoesNotEnqueueChange() = runTest {
        val created = repository.createList("   ")

        assertFalse(created)
        assertTrue(taskListDao.allLists().isEmpty())
        assertTrue(syncOutboxDao.allPending().isEmpty())
    }

    @Test
    fun renameList_whenListExists_trimsNameUpdatesListAndEnqueuesUpsert() = runTest {
        assertTrue(repository.createList("Old"))
        val list = taskListDao.allActiveLists().single()

        val renamed = repository.renameList(list.id, "  New  ")

        assertTrue(renamed)
        val updated = requireNotNullForTest(taskListDao.findById(list.id))
        assertEquals("New", updated.name)
        assertTrue(updated.updatedAt >= list.updatedAt)

        val latestChange = syncOutboxDao.allPending().last()
        assertEquals("task_list", latestChange.entityType)
        assertEquals(list.id, latestChange.entityId)
        assertEquals("upsert", latestChange.operation)
        assertTrue(latestChange.payloadJson.contains("New"))
    }

    @Test
    fun renameList_whenTargetIsDefaultInbox_returnsFalseAndKeepsInboxName() = runTest {
        repository.ensureDefaultList()

        val renamed = repository.renameList(DEFAULT_TASK_LIST_ID, "Other")

        assertFalse(renamed)
        assertEquals("收集箱", requireNotNullForTest(taskListDao.findById(DEFAULT_TASK_LIST_ID)).name)
        assertTrue(syncOutboxDao.allPending().isEmpty())
    }

    @Test
    fun renameList_whenNameIsBlankOrListMissing_returnsFalseAndDoesNotEnqueueChange() = runTest {
        assertTrue(repository.createList("Work"))
        val list = taskListDao.allActiveLists().single()
        syncOutboxDao.clear()

        assertFalse(repository.renameList(list.id, "   "))
        assertFalse(repository.renameList("missing", "New"))
        assertEquals("Work", requireNotNullForTest(taskListDao.findById(list.id)).name)
        assertTrue(syncOutboxDao.allPending().isEmpty())
    }

    @Test
    fun setSortOrder_whenListExists_updatesOrderAndEnqueuesUpsert() = runTest {
        assertTrue(repository.createList("Work"))
        val list = taskListDao.allActiveLists().single()
        syncOutboxDao.clear()

        val updated = repository.setSortOrder(list.id, -10)

        assertTrue(updated)
        assertEquals(-10, requireNotNullForTest(taskListDao.findById(list.id)).sortOrder)
        val change = syncOutboxDao.allPending().single()
        assertEquals("task_list", change.entityType)
        assertEquals(list.id, change.entityId)
        assertEquals("upsert", change.operation)
    }

    @Test
    fun removeList_whenListExists_softDeletesListAndEnqueuesDelete() = runTest {
        assertTrue(repository.createList("Temporary"))
        val list = taskListDao.allActiveLists().single()
        syncOutboxDao.clear()

        val removed = repository.removeList(list.id)

        assertTrue(removed)
        val deleted = requireNotNullForTest(taskListDao.findById(list.id))
        assertNotNull(deleted.deletedAt)
        assertTrue(taskListDao.allActiveLists().isEmpty())

        val change = syncOutboxDao.allPending().single()
        assertEquals("task_list", change.entityType)
        assertEquals(list.id, change.entityId)
        assertEquals("delete", change.operation)
    }

    @Test
    fun removeList_whenTargetIsDefaultInboxOrMissing_returnsFalseAndDoesNotEnqueueChange() = runTest {
        repository.ensureDefaultList()

        assertFalse(repository.removeList(DEFAULT_TASK_LIST_ID))
        assertFalse(repository.removeList("missing"))
        assertEquals(null, requireNotNullForTest(taskListDao.findById(DEFAULT_TASK_LIST_ID)).deletedAt)
        assertTrue(syncOutboxDao.allPending().isEmpty())
    }

    private fun <T : Any> requireNotNullForTest(value: T?): T {
        assertNotNull(value)
        return value!!
    }

    private class FakeTaskListDao : TaskListDao {
        private val lists = linkedMapOf<String, TaskListEntity>()

        override fun observeActiveLists(): Flow<List<TaskListEntity>> = flow { emit(allActiveListsIncludingInbox()) }

        override suspend fun findById(id: String): TaskListEntity? = lists[id]

        override suspend fun upsert(taskList: TaskListEntity) {
            lists[taskList.id] = taskList
        }

        override suspend fun rename(id: String, name: String, updatedAt: Long) {
            lists[id]?.takeIf { it.deletedAt == null }?.let { existing ->
                lists[id] = existing.copy(name = name, updatedAt = updatedAt)
            }
        }

        override suspend fun setSortOrder(id: String, sortOrder: Int, updatedAt: Long) {
            lists[id]?.takeIf { it.deletedAt == null }?.let { existing ->
                lists[id] = existing.copy(sortOrder = sortOrder, updatedAt = updatedAt)
            }
        }

        override suspend fun softDelete(id: String, deletedAt: Long) {
            lists[id]?.takeIf { it.deletedAt == null }?.let { existing ->
                lists[id] = existing.copy(deletedAt = deletedAt, updatedAt = deletedAt)
            }
        }

        fun allLists(): List<TaskListEntity> = lists.values.sortedBy { it.createdAt }

        fun allActiveLists(): List<TaskListEntity> = allActiveListsIncludingInbox()
            .filterNot { it.id == DEFAULT_TASK_LIST_ID }

        private fun allActiveListsIncludingInbox(): List<TaskListEntity> = lists.values
            .filter { it.deletedAt == null }
            .sortedWith(compareBy<TaskListEntity> { it.sortOrder }.thenBy { it.createdAt })
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

        fun clear() {
            changes.clear()
        }
    }
}
