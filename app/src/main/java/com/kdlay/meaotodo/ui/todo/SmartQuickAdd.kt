package com.kdlay.meaotodo.ui.todo

import com.kdlay.meaotodo.data.local.entity.DEFAULT_TASK_LIST_ID

internal data class SmartQuickAddResult(
    val title: String,
    val listId: String,
    val priority: Int,
    val dueAt: Long?,
    val estimatedPomodoros: Int
)

internal fun parseSmartQuickAdd(
    input: String,
    listOptions: List<TodoListOption>,
    fallbackListId: String = DEFAULT_TASK_LIST_ID,
    fallbackDueAt: Long? = null,
    now: Long = System.currentTimeMillis()
): SmartQuickAddResult? {
    var listId = fallbackListId
    var priority = 0
    var dueAt = fallbackDueAt
    var estimatedPomodoros = 0
    val titleParts = mutableListOf<String>()

    input.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.forEach { token ->
        when {
            token == "#今天" -> dueAt = startOfDay(now)
            token == "#明天" -> dueAt = addDays(startOfDay(now), 1)
            token == "#无日期" -> dueAt = null
            token == "!高" -> priority = 3
            token == "!中" -> priority = 2
            token == "!低" -> priority = 1
            token.startsWith("🍅") -> {
                val count = token.removePrefix("🍅").toIntOrNull()
                if (count == null) titleParts += token else estimatedPomodoros = count.coerceIn(1, 12)
            }
            token.startsWith("@") && token.length > 1 -> {
                val requestedName = token.drop(1)
                val matched = listOptions.firstOrNull { option ->
                    !option.isSmart && option.label.equals(requestedName, ignoreCase = true)
                }
                if (matched == null) titleParts += token else listId = matched.id
            }
            else -> titleParts += token
        }
    }

    val title = titleParts.joinToString(" ").trim()
    if (title.isBlank()) return null
    return SmartQuickAddResult(title, listId, priority, dueAt, estimatedPomodoros)
}
