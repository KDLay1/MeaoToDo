package com.kdlay.meaotodo.domain.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAssistantTest {
    private val parser = LocalAssistantCommandParser()

    @Test
    fun parseExpense_createsLocalPendingAction() {
        val parsed = parser.parse("记账 午饭 32.50元")

        assertEquals(LocalCommandIntent.EXPENSE, parsed.intent)
        assertEquals(3_250L, parsed.pendingAction?.amountCents)
        assertEquals("餐饮", parsed.pendingAction?.category)
        assertFalse(parsed.needsAi)
    }

    @Test
    fun parseFocus_extractsDuration() {
        val parsed = parser.parse("开始专注 写实验报告 45分钟")

        assertEquals(LocalCommandIntent.FOCUS, parsed.intent)
        assertEquals(45, parsed.pendingAction?.focusMinutes)
        assertEquals("写实验报告", parsed.pendingAction?.title)
    }

    @Test
    fun unknownTask_becomesAiEnhancedDraft() {
        val parsed = parser.parse("下周完成实验报告并检查图表")

        assertEquals(LocalCommandIntent.TASK, parsed.intent)
        assertTrue(parsed.needsAi)
        assertEquals(AssistantActionType.CREATE_TASK, parsed.pendingAction?.type)
    }

    @Test
    fun explicitTask_staysOffline() {
        val parsed = parser.parse("todo write report")

        assertEquals(LocalCommandIntent.TASK, parsed.intent)
        assertFalse(parsed.needsAi)
        assertEquals("write report", parsed.pendingAction?.title)
    }

    @Test
    fun validator_rejectsUnknownTaskAndInvalidExpense() {
        val context = DailyContext(
            generatedAt = 0, dayStart = 0, dayEnd = 1, pendingTasks = emptyList(), todayTasks = emptyList(),
            overdueTasks = emptyList(), completedTodayCount = 0, completedFocusCount = 0, focusedMinutes = 0,
            activeFocus = null, todayExpenseCents = 0, monthExpenseCents = 0, monthlyBudgetCents = 0
        )
        val badFocus = PendingAction(AssistantActionType.START_FOCUS, "专注", "", taskId = "missing", focusMinutes = 0)
        val badExpense = PendingAction(AssistantActionType.RECORD_EXPENSE, "支出", "", amountCents = 0, category = "")

        assertFalse(PendingActionValidator().validate(badFocus, context).isValid)
        assertFalse(PendingActionValidator().validate(badExpense).isValid)
    }
}
