package com.kdlay.meaotodo.ai.model

import com.kdlay.meaotodo.domain.assistant.PendingAction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskDraftResponse(
    val summary: String,
    @SerialName("clarifying_question") val clarifyingQuestion: String? = null,
    val tasks: List<AiTaskDraft> = emptyList()
)

@Serializable
data class AiTaskDraft(
    val title: String,
    val note: String = "",
    @SerialName("due_at") val dueAt: Long? = null,
    val priority: Int = 0,
    @SerialName("estimated_pomodoros") val estimatedPomodoros: Int = 0,
    val reason: String = ""
)

@Serializable
data class DailyBriefResponse(
    val headline: String,
    val priorities: List<DailyPriority> = emptyList(),
    val reminders: List<String> = emptyList(),
    val encouragement: String = ""
)

@Serializable
data class DailyPriority(
    @SerialName("task_id") val taskId: String,
    val title: String,
    val reason: String,
    @SerialName("suggested_focus_minutes") val suggestedFocusMinutes: Int = 25
)

@Serializable
data class EveningReviewResponse(
    val summary: String,
    val facts: List<String> = emptyList(),
    val patterns: List<String> = emptyList(),
    @SerialName("possible_causes") val possibleCauses: List<String> = emptyList(),
    val wins: List<String> = emptyList(),
    @SerialName("tomorrow_focus") val tomorrowFocus: String,
    @SerialName("suggested_actions") val suggestedActions: List<PendingAction> = emptyList()
)

@Serializable
data class PlanAdjustmentResponse(
    val summary: String,
    @SerialName("kept_task_ids") val keptTaskIds: List<String> = emptyList(),
    @SerialName("suggested_actions") val suggestedActions: List<PendingAction> = emptyList()
)
