package com.kdlay.meaotodo.domain.assistant

import java.math.BigDecimal
import java.math.RoundingMode

enum class LocalCommandIntent {
    TASK,
    EXPENSE,
    FOCUS,
    QUESTION,
    UNKNOWN
}

data class ParsedAssistantCommand(
    val intent: LocalCommandIntent,
    val originalText: String,
    val pendingAction: PendingAction? = null,
    val query: String? = null,
    val confidence: Float,
    val needsAi: Boolean
)

class LocalAssistantCommandParser {
    fun parse(input: String): ParsedAssistantCommand {
        val text = input.trim()
        if (text.isBlank()) return ParsedAssistantCommand(LocalCommandIntent.UNKNOWN, input, confidence = 0f, needsAi = false)

        parseExpense(text)?.let { return it }
        parseFocus(text)?.let { return it }
        if (text.endsWith("？") || text.endsWith("?") || text.startsWith("帮我") || text.startsWith("建议")) {
            return ParsedAssistantCommand(LocalCommandIntent.QUESTION, text, query = text, confidence = 0.8f, needsAi = true)
        }
        return ParsedAssistantCommand(
            intent = LocalCommandIntent.TASK,
            originalText = text,
            pendingAction = PendingAction(
                type = AssistantActionType.CREATE_TASK,
                title = text,
                explanation = "按任务草稿处理，提交前可继续编辑"
            ),
            confidence = 0.55f,
            needsAi = true
        )
    }

    private fun parseExpense(text: String): ParsedAssistantCommand? {
        if (!EXPENSE_PREFIX.containsMatchIn(text)) return null
        val amount = AMOUNT.find(text)?.groupValues?.getOrNull(1)?.toBigDecimalOrNull() ?: return null
        val cents = amount.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
        if (cents <= 0) return null
        val note = text
            .replace(EXPENSE_PREFIX, "")
            .replace(AMOUNT, "")
            .trim(' ', '，', ',', '元')
        val category = inferExpenseCategory(text)
        return ParsedAssistantCommand(
            intent = LocalCommandIntent.EXPENSE,
            originalText = text,
            pendingAction = PendingAction(
                type = AssistantActionType.RECORD_EXPENSE,
                title = if (note.isBlank()) "记录一笔支出" else note,
                explanation = "识别为 $category 支出 ¥${amount.stripTrailingZeros().toPlainString()}",
                note = note,
                amountCents = cents,
                category = category
            ),
            confidence = 0.95f,
            needsAi = false
        )
    }

    private fun parseFocus(text: String): ParsedAssistantCommand? {
        if (!FOCUS_PREFIX.containsMatchIn(text)) return null
        val minutes = MINUTES.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 180) ?: 25
        val title = text.replace(FOCUS_PREFIX, "").replace(MINUTES, "").trim(' ', '，', ',').ifBlank { "空白专注" }
        return ParsedAssistantCommand(
            intent = LocalCommandIntent.FOCUS,
            originalText = text,
            pendingAction = PendingAction(
                type = AssistantActionType.START_FOCUS,
                title = title,
                explanation = "开始 $minutes 分钟专注",
                focusMinutes = minutes
            ),
            confidence = 0.95f,
            needsAi = false
        )
    }

    private fun inferExpenseCategory(text: String): String = when {
        listOf("饭", "餐", "吃", "外卖").any(text::contains) -> "餐饮"
        listOf("车", "公交", "地铁", "打车").any(text::contains) -> "交通"
        listOf("书", "课", "学习").any(text::contains) -> "学习"
        listOf("咖啡", "奶茶").any(text::contains) -> "咖啡"
        else -> "其他"
    }

    private companion object {
        val EXPENSE_PREFIX = Regex("^(记账|支出|花了|消费)[：:]?\\s*")
        val FOCUS_PREFIX = Regex("^(开始)?(专注|番茄)[：:]?\\s*")
        val AMOUNT = Regex("(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|块)?")
        val MINUTES = Regex("(\\d{1,3})\\s*(?:分钟|min)", RegexOption.IGNORE_CASE)
    }
}

enum class AssistantSuggestionKind {
    ACTIVE_FOCUS,
    OVERDUE_REVIEW,
    NEXT_TASK,
    PLAN_DAY,
    BUDGET_PACE
}

data class AssistantSuggestion(
    val kind: AssistantSuggestionKind,
    val title: String,
    val reason: String,
    val pendingAction: PendingAction? = null
)

class LocalSuggestionEngine {
    fun build(context: DailyContext): List<AssistantSuggestion> {
        val suggestions = mutableListOf<AssistantSuggestion>()
        context.activeFocus?.let { focus ->
            suggestions += AssistantSuggestion(
                kind = AssistantSuggestionKind.ACTIVE_FOCUS,
                title = "继续 ${focus.title}",
                reason = "当前已有进行中的专注，先完成正在做的事"
            )
        }
        context.overdueTasks.firstOrNull()?.let { task ->
            suggestions += AssistantSuggestion(
                kind = AssistantSuggestionKind.OVERDUE_REVIEW,
                title = "处理逾期：${task.title}",
                reason = "这项任务已经逾期，建议确认继续、拆分或延期"
            )
        }
        if (context.activeFocus == null) {
            context.todayTasks.firstOrNull()?.let { task ->
                suggestions += AssistantSuggestion(
                    kind = AssistantSuggestionKind.NEXT_TASK,
                    title = "下一步：${task.title}",
                    reason = if (task.priority >= 2) "这是今天的高优先级任务" else "这是今天尚未完成的任务",
                    pendingAction = PendingAction(
                        type = AssistantActionType.START_FOCUS,
                        title = task.title,
                        explanation = "从一个专注轮次开始推进",
                        taskId = task.id,
                        focusMinutes = 25
                    )
                )
            } ?: run {
                suggestions += AssistantSuggestion(
                    kind = AssistantSuggestionKind.PLAN_DAY,
                    title = "安排今天",
                    reason = "今天还没有明确任务，可以从待办中选择 1 至 3 项"
                )
            }
        }
        if (context.monthlyBudgetCents > 0 && context.monthExpenseCents > context.monthlyBudgetCents) {
            suggestions += AssistantSuggestion(
                kind = AssistantSuggestionKind.BUDGET_PACE,
                title = "检查本月预算",
                reason = "本月支出已经超过预算，建议查看主要支出类别"
            )
        }
        return suggestions.take(3)
    }
}
