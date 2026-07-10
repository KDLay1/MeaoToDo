package com.kdlay.meaotodo.domain.assistant

import com.kdlay.meaotodo.data.repository.LedgerRepository
import com.kdlay.meaotodo.data.repository.PomodoroRepository
import com.kdlay.meaotodo.data.repository.TaskRepository

class AssistantActionExecutor(
    private val taskRepository: TaskRepository,
    private val pomodoroRepository: PomodoroRepository,
    private val ledgerRepository: LedgerRepository,
    private val validator: PendingActionValidator = PendingActionValidator()
) {
    suspend fun execute(action: PendingAction, context: DailyContext): Boolean {
        val validation = validator.validate(action, context)
        require(validation.isValid) { validation.errors.joinToString("；") }
        return when (action.type) {
            AssistantActionType.CREATE_TASK -> taskRepository.addTask(
                listId = action.listId.orEmpty().ifBlank { "inbox" },
                title = action.title,
                note = action.note.orEmpty(),
                priority = action.priority ?: 0,
                dueAt = action.dueAt,
                hasDueTime = action.dueAt != null,
                estimatedPomodoros = action.estimatedPomodoros ?: 0
            )
            AssistantActionType.RECORD_EXPENSE -> ledgerRepository.addExpense(
                amountCents = requireNotNull(action.amountCents),
                category = requireNotNull(action.category),
                note = action.note ?: action.title
            )
            AssistantActionType.START_FOCUS -> pomodoroRepository.startFocus(
                taskId = action.taskId,
                titleSnapshot = action.title,
                plannedDurationSeconds = requireNotNull(action.focusMinutes) * 60
            )
            AssistantActionType.RESCHEDULE_TASK -> taskRepository.rescheduleTask(
                id = requireNotNull(action.taskId),
                dueAt = action.dueAt,
                hasDueTime = false
            )
            AssistantActionType.COMPLETE_TASK -> taskRepository.setDone(
                id = requireNotNull(action.taskId),
                isDone = true
            )
        }
    }
}
