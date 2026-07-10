package com.kdlay.meaotodo.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kdlay.meaotodo.ai.AssistantAiService
import com.kdlay.meaotodo.ai.config.AiSettingsStore
import com.kdlay.meaotodo.ai.config.AiProviderSettings
import com.kdlay.meaotodo.ai.model.DailyBriefResponse
import com.kdlay.meaotodo.ai.model.EveningReviewResponse
import com.kdlay.meaotodo.ai.model.PlanAdjustmentResponse
import com.kdlay.meaotodo.domain.assistant.AssistantActionExecutor
import com.kdlay.meaotodo.domain.assistant.AssistantSuggestion
import com.kdlay.meaotodo.domain.assistant.DailyContext
import com.kdlay.meaotodo.domain.assistant.DailyContextRepository
import com.kdlay.meaotodo.domain.assistant.LocalAssistantCommandParser
import com.kdlay.meaotodo.domain.assistant.LocalCommandIntent
import com.kdlay.meaotodo.domain.assistant.LocalSuggestionEngine
import com.kdlay.meaotodo.domain.assistant.PendingAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class AssistantViewModel(
    dailyContextRepository: DailyContextRepository,
    private val aiSettingsStore: AiSettingsStore,
    private val aiService: AssistantAiService,
    private val actionExecutor: AssistantActionExecutor,
    private val commandParser: LocalAssistantCommandParser = LocalAssistantCommandParser(),
    private val suggestionEngine: LocalSuggestionEngine = LocalSuggestionEngine()
) : ViewModel() {
    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()
    private val startedAutomations = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            dailyContextRepository.context.collectLatest { context ->
                _uiState.value = _uiState.value.copy(
                    context = context,
                    suggestions = suggestionEngine.build(context)
                )
                maybeRunAutomation()
            }
        }
        viewModelScope.launch {
            aiSettingsStore.settings.collectLatest { settings ->
                _uiState.value = _uiState.value.copy(
                    isAiConfigured = settings.isConfigured,
                    aiModel = settings.model,
                    aiProviderSettings = settings
                )
                maybeRunAutomation()
            }
        }
    }

    fun processInput(input: String) {
        val parsed = commandParser.parse(input)
        when {
            parsed.intent == LocalCommandIntent.UNKNOWN -> setError("请输入任务、支出、专注指令或问题")
            !parsed.needsAi && parsed.pendingAction != null -> {
                _uiState.value = _uiState.value.copy(
                    pendingActions = _uiState.value.pendingActions + parsed.pendingAction,
                    message = "已生成本地操作草稿，请确认后执行",
                    error = null
                )
            }
            parsed.intent == LocalCommandIntent.TASK -> runAi("正在整理任务…") {
                val result = aiService.draftTasks(input)
                _uiState.value = _uiState.value.copy(
                    pendingActions = _uiState.value.pendingActions + aiService.taskDraftActions(result.value),
                    message = result.value.clarifyingQuestion ?: result.value.summary,
                    lastTokenUsage = result.totalTokens
                )
            }
            parsed.intent == LocalCommandIntent.QUESTION -> runAi("正在结合今天的数据思考…") {
                val result = aiService.adjustPlan(input)
                applyPlanAdjustment(result.value, result.totalTokens)
            }
            else -> parsed.pendingAction?.let { action ->
                _uiState.value = _uiState.value.copy(pendingActions = _uiState.value.pendingActions + action)
            }
        }
    }

    fun generateDailyBrief() = generateDailyBriefInternal()

    private fun generateDailyBriefInternal() = runAi("正在生成每日简报…") {
        val result = aiService.dailyBrief()
        _uiState.value = _uiState.value.copy(dailyBrief = result.value, lastTokenUsage = result.totalTokens)
        aiSettingsStore.markDailyBriefGenerated(currentDayKey())
    }

    fun generateEveningReview(reflection: String = "") = generateEveningReviewInternal(reflection)

    private fun generateEveningReviewInternal(reflection: String = "") = runAi("正在复盘今天…") {
        val result = aiService.eveningReview(reflection)
        _uiState.value = _uiState.value.copy(
            eveningReview = result.value,
            pendingActions = _uiState.value.pendingActions + result.value.suggestedActions,
            lastTokenUsage = result.totalTokens
        )
        aiSettingsStore.markEveningReviewGenerated(currentDayKey())
    }

    fun adjustPlan(constraint: String) = runAi("正在调整计划…") {
        val result = aiService.adjustPlan(constraint)
        applyPlanAdjustment(result.value, result.totalTokens)
    }

    fun confirmAction(index: Int) {
        val action = _uiState.value.pendingActions.getOrNull(index) ?: return
        val context = _uiState.value.context ?: return setError("本地数据尚未加载")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, error = null)
            runCatching { actionExecutor.execute(action, context) }
                .onSuccess { success ->
                    if (success) {
                        _uiState.value = _uiState.value.copy(
                            pendingActions = _uiState.value.pendingActions.toMutableList().also { it.removeAt(index) },
                            message = "已执行：${action.title}"
                        )
                    } else setError("操作未执行，请检查当前状态")
                }
                .onFailure { setError(it.message ?: "操作执行失败") }
            _uiState.value = _uiState.value.copy(isBusy = false)
        }
    }

    fun discardAction(index: Int) {
        if (index !in _uiState.value.pendingActions.indices) return
        _uiState.value = _uiState.value.copy(
            pendingActions = _uiState.value.pendingActions.toMutableList().also { it.removeAt(index) }
        )
    }

    fun updateAction(index: Int, action: PendingAction) {
        if (index !in _uiState.value.pendingActions.indices) return
        _uiState.value = _uiState.value.copy(
            pendingActions = _uiState.value.pendingActions.toMutableList().also { it[index] = action },
            message = "草稿已更新",
            error = null
        )
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    private fun applyPlanAdjustment(value: PlanAdjustmentResponse, totalTokens: Int?) {
        _uiState.value = _uiState.value.copy(
            planAdjustment = value,
            pendingActions = _uiState.value.pendingActions + value.suggestedActions,
            message = value.summary,
            lastTokenUsage = totalTokens
        )
    }

    private fun runAi(progressMessage: String, block: suspend () -> Unit) {
        if (!_uiState.value.isAiConfigured) return setError("请先在设置中配置 API Base URL、模型和 API Key")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, message = progressMessage, error = null)
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                setError(error.message ?: "AI 请求失败")
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false)
            }
        }
    }

    private fun maybeRunAutomation() {
        val state = _uiState.value
        val settings = state.aiProviderSettings
        if (!state.isAiConfigured || state.context == null || state.isBusy) return
        val now = Calendar.getInstance()
        val dayKey = currentDayKey(now)
        if (settings.autoEveningReview && now.get(Calendar.HOUR_OF_DAY) >= 20 && settings.lastEveningReviewDay != dayKey) {
            val key = "review:$dayKey"
            if (startedAutomations.add(key)) generateEveningReviewInternal()
            return
        }
        if (settings.autoDailyBrief && now.get(Calendar.HOUR_OF_DAY) >= 6 && settings.lastDailyBriefDay != dayKey) {
            val key = "brief:$dayKey"
            if (startedAutomations.add(key)) generateDailyBriefInternal()
        }
    }

    private fun currentDayKey(calendar: Calendar = Calendar.getInstance()): String =
        "%04d-%02d-%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )

    private fun setError(message: String) {
        _uiState.value = _uiState.value.copy(error = message, message = null)
    }

    companion object {
        fun factory(
            dailyContextRepository: DailyContextRepository,
            aiSettingsStore: AiSettingsStore,
            aiService: AssistantAiService,
            actionExecutor: AssistantActionExecutor
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(AssistantViewModel::class.java))
                return AssistantViewModel(dailyContextRepository, aiSettingsStore, aiService, actionExecutor) as T
            }
        }
    }
}

data class AssistantUiState(
    val context: DailyContext? = null,
    val suggestions: List<AssistantSuggestion> = emptyList(),
    val pendingActions: List<PendingAction> = emptyList(),
    val isAiConfigured: Boolean = false,
    val aiModel: String = "",
    val aiProviderSettings: AiProviderSettings = AiProviderSettings(),
    val isBusy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val dailyBrief: DailyBriefResponse? = null,
    val eveningReview: EveningReviewResponse? = null,
    val planAdjustment: PlanAdjustmentResponse? = null,
    val lastTokenUsage: Int? = null
)
