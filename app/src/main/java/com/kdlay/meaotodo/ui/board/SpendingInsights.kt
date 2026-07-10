package com.kdlay.meaotodo.ui.board

import java.util.Calendar

data class BudgetPace(
    val budgetCents: Long,
    val spentCents: Long,
    val expectedByTodayCents: Long,
    val projectedMonthCents: Long,
    val progress: Float,
    val status: String
)

internal fun buildBudgetPace(monthSpentCents: Long, budgetCents: Long, now: Long): BudgetPace {
    val calendar = Calendar.getInstance().apply { timeInMillis = now }
    val day = calendar.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(day)
    val safeSpent = monthSpentCents.coerceAtLeast(0)
    val safeBudget = budgetCents.coerceAtLeast(0)
    val projected = safeSpent
        .coerceAtMost(Long.MAX_VALUE / daysInMonth)
        .times(daysInMonth)
        .div(day)
    if (safeBudget == 0L) {
        return BudgetPace(0, safeSpent, 0, projected, 0f, "未设置预算")
    }
    val expected = safeBudget
        .coerceAtMost(Long.MAX_VALUE / day)
        .times(day)
        .div(daysInMonth)
    val progress = (safeSpent.toDouble() / safeBudget.toDouble()).toFloat().coerceAtLeast(0f)
    val status = when {
        safeSpent > safeBudget -> "已超预算"
        projected > safeBudget -> "按当前节奏可能超支"
        safeSpent > expected * 11 / 10 -> "支出略快"
        else -> "节奏健康"
    }
    return BudgetPace(safeBudget, safeSpent, expected, projected, progress, status)
}
