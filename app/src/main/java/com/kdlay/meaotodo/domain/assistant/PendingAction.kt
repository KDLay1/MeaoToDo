package com.kdlay.meaotodo.domain.assistant

import kotlinx.serialization.Serializable

@Serializable
enum class AssistantActionType {
    CREATE_TASK,
    RECORD_EXPENSE,
    START_FOCUS,
    RESCHEDULE_TASK,
    COMPLETE_TASK
}

@Serializable
data class PendingAction(
    val type: AssistantActionType,
    val title: String,
    val explanation: String,
    val taskId: String? = null,
    val listId: String? = null,
    val note: String? = null,
    val dueAt: Long? = null,
    val priority: Int? = null,
    val estimatedPomodoros: Int? = null,
    val amountCents: Long? = null,
    val category: String? = null,
    val focusMinutes: Int? = null
)

data class ActionValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

class PendingActionValidator {
    fun validate(action: PendingAction, context: DailyContext? = null): ActionValidationResult {
        val errors = buildList {
            if (action.title.isBlank()) add("标题不能为空")
            when (action.type) {
                AssistantActionType.CREATE_TASK -> {
                    if (action.priority != null && action.priority !in 0..3) add("优先级必须在 0..3")
                    if (action.estimatedPomodoros != null && action.estimatedPomodoros !in 0..24) add("预计番茄数必须在 0..24")
                }
                AssistantActionType.RECORD_EXPENSE -> {
                    if ((action.amountCents ?: 0L) <= 0L) add("支出金额必须大于 0")
                    if (action.category.isNullOrBlank()) add("支出分类不能为空")
                }
                AssistantActionType.START_FOCUS -> {
                    if ((action.focusMinutes ?: 0) !in 1..180) add("专注时长必须在 1..180 分钟")
                    if (action.taskId != null) validateTaskId(action.taskId, context)?.let(::add)
                }
                AssistantActionType.RESCHEDULE_TASK -> {
                    validateTaskId(action.taskId, context)?.let(::add)
                    if (action.dueAt == null) add("延期动作必须提供新日期")
                }
                AssistantActionType.COMPLETE_TASK -> validateTaskId(action.taskId, context)?.let(::add)
            }
        }
        return ActionValidationResult(errors.isEmpty(), errors)
    }

    private fun validateTaskId(taskId: String?, context: DailyContext?): String? {
        if (taskId.isNullOrBlank()) return "任务 ID 不能为空"
        if (context != null && context.pendingTasks.none { it.id == taskId }) return "任务不存在或不可操作"
        return null
    }
}
