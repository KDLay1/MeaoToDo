package com.kdlay.meaotodo.ui.todo

import com.kdlay.meaotodo.data.local.entity.DEFAULT_TASK_LIST_ID
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmartQuickAddTest {
    private val lists = listOf(
        TodoListOption(DEFAULT_TASK_LIST_ID, "收集箱", 0, TodoListKind.SYSTEM),
        TodoListOption("study", "学习", 0, TodoListKind.CUSTOM)
    )

    @Test
    fun parseSmartQuickAdd_extractsDatePriorityPomodorosAndList() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 10, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val result = requireNotNull(parseSmartQuickAdd("复习统计物理 #明天 !高 🍅3 @学习", lists, now = now))

        assertEquals("复习统计物理", result.title)
        assertEquals("study", result.listId)
        assertEquals(3, result.priority)
        assertEquals(3, result.estimatedPomodoros)
        assertEquals(addDays(startOfDay(now), 1), result.dueAt)
    }

    @Test
    fun parseSmartQuickAdd_keepsUnknownTokensAsTitleText() {
        val result = requireNotNull(parseSmartQuickAdd("联系 @不存在 🍅x", lists))

        assertEquals("联系 @不存在 🍅x", result.title)
        assertEquals(DEFAULT_TASK_LIST_ID, result.listId)
    }

    @Test
    fun parseSmartQuickAdd_rejectsCommandOnlyInput() {
        assertNull(parseSmartQuickAdd("#今天 !高 🍅2", lists))
    }

}
