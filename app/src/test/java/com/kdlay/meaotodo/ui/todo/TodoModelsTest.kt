package com.kdlay.meaotodo.ui.todo

import com.kdlay.meaotodo.data.local.entity.DEFAULT_TASK_LIST_ID
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoModelsTest {
    @Test
    fun buildTodoGroups_keepsSmartAllDisplayBucketsMutuallyExclusive() {
        val today = todayRange().first
        val future = addDays(today, 2)
        val tasks = listOf(
            task(id = "overdue", dueAt = addDays(today, -1), listId = DEFAULT_TASK_LIST_ID),
            task(id = "today", dueAt = today, listId = DEFAULT_TASK_LIST_ID),
            task(id = "unscheduled", dueAt = null, listId = "custom"),
            task(id = "future", dueAt = future, listId = DEFAULT_TASK_LIST_ID),
            task(id = "done", dueAt = today, isDone = true, listId = DEFAULT_TASK_LIST_ID)
        )

        val groups = buildTodoGroups(tasks)
        val smartAllDisplayIds = listOf(
            groups.overdue,
            groups.today,
            groups.unscheduled,
            groups.upcoming,
            groups.completed
        ).flatten().map { it.id }

        assertEquals(tasks.map { it.id }.toSet(), smartAllDisplayIds.toSet())
        assertEquals(smartAllDisplayIds.size, smartAllDisplayIds.toSet().size)
        assertTrue(groups.unscheduled.single().id == "unscheduled")
        assertFalse(groups.today.any { it.id == "future" })
    }

    private fun task(
        id: String,
        dueAt: Long?,
        listId: String,
        isDone: Boolean = false
    ): TaskEntity = TaskEntity(
        id = id,
        listId = listId,
        title = id,
        isDone = isDone,
        dueAt = dueAt,
        hasDueTime = false,
        createdAt = 1L,
        updatedAt = 1L
    )
}
