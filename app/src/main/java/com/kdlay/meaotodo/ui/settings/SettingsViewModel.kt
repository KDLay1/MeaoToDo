package com.kdlay.meaotodo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kdlay.meaotodo.ai.AssistantAiService
import com.kdlay.meaotodo.ai.config.AiProviderSettings
import com.kdlay.meaotodo.ai.config.AiSettingsStore
import android.net.Uri
import com.kdlay.meaotodo.data.backup.DataBackupService
import com.kdlay.meaotodo.core.settings.AppSettingsStore
import com.kdlay.meaotodo.core.settings.AppPreferences
import com.kdlay.meaotodo.core.settings.PomodoroPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: AppSettingsStore,
    private val aiSettingsStore: AiSettingsStore,
    private val assistantAiService: AssistantAiService,
    private val dataBackupService: DataBackupService
) : ViewModel() {
    val preferences: StateFlow<AppPreferences> = settingsStore.appPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppPreferences()
    )
    val aiSettings: StateFlow<AiProviderSettings> = aiSettingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AiProviderSettings()
    )
    val pomodoroPreferences: StateFlow<PomodoroPreferences> = settingsStore.pomodoroPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PomodoroPreferences()
    )
    val aiStatus = MutableStateFlow<AiSettingsStatus>(AiSettingsStatus.Idle)
    val dataTransferStatus = MutableStateFlow<DataTransferStatus>(DataTransferStatus.Idle)

    fun setBoardShowToday(enabled: Boolean) = viewModelScope.launch { settingsStore.setBoardShowToday(enabled) }
    fun setBoardShowPomodoro(enabled: Boolean) = viewModelScope.launch { settingsStore.setBoardShowPomodoro(enabled) }
    fun setBoardShowLedger(enabled: Boolean) = viewModelScope.launch { settingsStore.setBoardShowLedger(enabled) }
    fun setBoardShowSchedule(enabled: Boolean) = viewModelScope.launch { settingsStore.setBoardShowSchedule(enabled) }
    fun setBoardShowStatus(enabled: Boolean) = viewModelScope.launch { settingsStore.setBoardShowStatus(enabled) }
    fun setMonthlyBudgetCents(cents: Long) = viewModelScope.launch { settingsStore.setMonthlyBudgetCents(cents) }
    fun setPomodoroNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsStore.setPomodoroNotificationsEnabled(enabled)
    }

    fun saveAiConfiguration(baseUrl: String, model: String, apiKey: String) = viewModelScope.launch {
        runAiOperation("AI 配置已安全保存在本机") {
            aiSettingsStore.saveProvider(baseUrl, model)
            if (apiKey.isNotBlank()) aiSettingsStore.saveApiKey(apiKey)
        }
    }

    fun testAiConnection() = viewModelScope.launch {
        runAiOperation("连接成功") { assistantAiService.testConnection() }
    }

    fun clearAiApiKey() = viewModelScope.launch {
        runAiOperation("API Key 已删除") { aiSettingsStore.clearApiKey() }
    }

    fun setAutoDailyBrief(enabled: Boolean) = viewModelScope.launch {
        aiSettingsStore.setAutoDailyBrief(enabled)
    }

    fun setAutoEveningReview(enabled: Boolean) = viewModelScope.launch {
        aiSettingsStore.setAutoEveningReview(enabled)
    }

    fun setAiUsageLimits(dailyRequests: Int, monthlyTokens: Int) = viewModelScope.launch {
        aiSettingsStore.setUsageLimits(dailyRequests, monthlyTokens)
    }

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        dataTransferStatus.value = DataTransferStatus.Working("正在导出…")
        dataTransferStatus.value = runCatching { dataBackupService.exportTo(uri) }
            .fold(
                onSuccess = { DataTransferStatus.Success("已导出 ${it.totalRecords} 条本地记录") },
                onFailure = { DataTransferStatus.Error(it.message ?: "导出失败") }
            )
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        dataTransferStatus.value = DataTransferStatus.Working("正在恢复…")
        dataTransferStatus.value = runCatching { dataBackupService.importFrom(uri) }
            .fold(
                onSuccess = { DataTransferStatus.Success("已合并恢复 ${it.totalRecords} 条记录") },
                onFailure = { DataTransferStatus.Error(it.message ?: "恢复失败") }
            )
    }

    fun clearAiStatus() {
        aiStatus.value = AiSettingsStatus.Idle
    }

    private suspend fun runAiOperation(successMessage: String, block: suspend () -> Unit) {
        aiStatus.value = AiSettingsStatus.Working
        aiStatus.value = runCatching { block() }
            .fold(
                onSuccess = { AiSettingsStatus.Success(successMessage) },
                onFailure = { AiSettingsStatus.Error(it.message ?: "操作失败") }
            )
    }

    companion object {
        fun factory(
            settingsStore: AppSettingsStore,
            aiSettingsStore: AiSettingsStore,
            assistantAiService: AssistantAiService,
            dataBackupService: DataBackupService
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
                return SettingsViewModel(settingsStore, aiSettingsStore, assistantAiService, dataBackupService) as T
            }
        }
    }
}

sealed interface AiSettingsStatus {
    data object Idle : AiSettingsStatus
    data object Working : AiSettingsStatus
    data class Success(val message: String) : AiSettingsStatus
    data class Error(val message: String) : AiSettingsStatus
}

sealed interface DataTransferStatus {
    data object Idle : DataTransferStatus
    data class Working(val message: String) : DataTransferStatus
    data class Success(val message: String) : DataTransferStatus
    data class Error(val message: String) : DataTransferStatus
}
