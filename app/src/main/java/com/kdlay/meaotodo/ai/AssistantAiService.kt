package com.kdlay.meaotodo.ai

import com.kdlay.meaotodo.ai.model.AiTaskDraft
import com.kdlay.meaotodo.ai.model.DailyBriefResponse
import com.kdlay.meaotodo.ai.model.EveningReviewResponse
import com.kdlay.meaotodo.ai.model.PlanAdjustmentResponse
import com.kdlay.meaotodo.ai.model.TaskDraftResponse
import com.kdlay.meaotodo.ai.network.AiClient
import com.kdlay.meaotodo.ai.network.AiCompletionRequest
import com.kdlay.meaotodo.ai.network.AiProviderConfig
import com.kdlay.meaotodo.ai.network.AiProviderConfigSource
import com.kdlay.meaotodo.ai.network.AiUsagePolicy
import com.kdlay.meaotodo.ai.network.NoOpAiUsagePolicy
import com.kdlay.meaotodo.ai.prompt.AssistantPromptFactory
import com.kdlay.meaotodo.domain.assistant.AssistantActionType
import com.kdlay.meaotodo.domain.assistant.DailyContext
import com.kdlay.meaotodo.domain.assistant.DailyContextSource
import com.kdlay.meaotodo.domain.assistant.PendingAction
import com.kdlay.meaotodo.domain.assistant.PendingActionValidator
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class AiConfigurationException(message: String) : IllegalStateException(message)
class AiStructuredOutputException(message: String) : IllegalArgumentException(message)

data class AiFeatureResult<T>(
    val value: T,
    val rawContent: String,
    val totalTokens: Int?
)

class AssistantAiService(
    private val providerConfigSource: AiProviderConfigSource,
    private val client: AiClient,
    private val dailyContextSource: DailyContextSource,
    private val usagePolicy: AiUsagePolicy = NoOpAiUsagePolicy,
    private val prompts: AssistantPromptFactory = AssistantPromptFactory(),
    private val actionValidator: PendingActionValidator = PendingActionValidator(),
    private val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false }
) {
    suspend fun testConnection(): Int? {
        val result = complete(
            AiCompletionRequest(
                systemPrompt = "只返回合法 JSON，不要输出其他内容。",
                userPrompt = "返回 {\"ok\":true}",
                temperature = 0.0,
                maxOutputTokens = 64
            )
        )
        if (!result.content.contains("ok", ignoreCase = true)) {
            throw AiStructuredOutputException("模型已响应，但未遵循 JSON 测试协议")
        }
        return result.totalTokens
    }

    suspend fun draftTasks(userText: String): AiFeatureResult<TaskDraftResponse> {
        require(userText.isNotBlank()) { "请输入要整理的内容" }
        val context = dailyContextSource.context.first()
        val result = complete(prompts.taskDraft(context, userText))
        val parsed = decode<TaskDraftResponse>(result.content)
        validateTaskDraft(parsed, context)
        return AiFeatureResult(parsed, result.content, result.totalTokens)
    }

    suspend fun dailyBrief(): AiFeatureResult<DailyBriefResponse> {
        val context = dailyContextSource.context.first()
        val result = complete(prompts.dailyBrief(context))
        val parsed = decode<DailyBriefResponse>(result.content)
        val pendingIds = context.pendingTasks.mapTo(hashSetOf()) { it.id }
        if (parsed.priorities.size > 3 || parsed.priorities.any { it.taskId !in pendingIds || it.suggestedFocusMinutes !in 5..90 }) {
            throw AiStructuredOutputException("每日简报包含不存在的任务或无效专注时长")
        }
        return AiFeatureResult(parsed, result.content, result.totalTokens)
    }

    suspend fun eveningReview(userReflection: String = ""): AiFeatureResult<EveningReviewResponse> {
        val context = dailyContextSource.context.first()
        val result = complete(prompts.eveningReview(context, userReflection))
        val parsed = decode<EveningReviewResponse>(result.content)
        validateActions(parsed.suggestedActions, context)
        return AiFeatureResult(parsed, result.content, result.totalTokens)
    }

    suspend fun adjustPlan(constraint: String): AiFeatureResult<PlanAdjustmentResponse> {
        require(constraint.isNotBlank()) { "请输入新的时间或状态限制" }
        val context = dailyContextSource.context.first()
        val result = complete(prompts.adjustPlan(context, constraint))
        val parsed = decode<PlanAdjustmentResponse>(result.content)
        if (parsed.suggestedActions.size > 5) throw AiStructuredOutputException("计划调整动作过多")
        validateActions(parsed.suggestedActions, context)
        return AiFeatureResult(parsed, result.content, result.totalTokens)
    }

    fun taskDraftActions(response: TaskDraftResponse): List<PendingAction> = response.tasks.map { draft ->
        PendingAction(
            type = AssistantActionType.CREATE_TASK,
            title = draft.title.trim(),
            explanation = draft.reason.ifBlank { "由 AI 整理的任务草稿" },
            note = draft.note.trim().takeIf { it.isNotBlank() },
            dueAt = draft.dueAt,
            priority = draft.priority,
            estimatedPomodoros = draft.estimatedPomodoros
        )
    }.onEach { action ->
        val validation = actionValidator.validate(action)
        if (!validation.isValid) throw AiStructuredOutputException(validation.errors.joinToString("；"))
    }

    private suspend fun complete(request: AiCompletionRequest) = runCatching {
        usagePolicy.beforeRequest()
        client.complete(providerConfigSource.getConfig(), request).also { usagePolicy.recordUsage(it.totalTokens) }
    }.getOrElse { error ->
        if (error is IllegalStateException && error !is AiConfigurationException) {
            throw AiConfigurationException(error.message ?: "AI 配置不可用")
        }
        throw error
    }

    private inline fun <reified T> decode(content: String): T {
        val normalized = content.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return runCatching { json.decodeFromString<T>(normalized) }
            .getOrElse { throw AiStructuredOutputException("AI 返回内容不符合结构化协议") }
    }

    private fun validateTaskDraft(response: TaskDraftResponse, context: DailyContext) {
        if (response.tasks.size > 5) throw AiStructuredOutputException("任务草稿超过 5 项")
        val existingTitles = context.pendingTasks.mapTo(hashSetOf()) { it.title.trim().lowercase() }
        response.tasks.forEach { task ->
            validateTask(task)
            if (task.title.trim().lowercase() in existingTitles) throw AiStructuredOutputException("任务草稿与现有待办重复：${task.title}")
            if (task.dueAt != null && task.dueAt < context.dayStart) throw AiStructuredOutputException("任务草稿包含过去日期：${task.title}")
        }
    }

    private fun validateTask(task: AiTaskDraft) {
        if (task.title.isBlank()) throw AiStructuredOutputException("任务标题不能为空")
        if (task.priority !in 0..3) throw AiStructuredOutputException("任务优先级无效")
        if (task.estimatedPomodoros !in 0..24) throw AiStructuredOutputException("任务番茄估算无效")
    }

    private fun validateActions(actions: List<PendingAction>, context: DailyContext) {
        actions.forEach { action ->
            val validation = actionValidator.validate(action, context)
            if (!validation.isValid) throw AiStructuredOutputException(validation.errors.joinToString("；"))
        }
    }
}
