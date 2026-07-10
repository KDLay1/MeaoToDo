package com.kdlay.meaotodo.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.ai.config.AiProviderSettings
import com.kdlay.meaotodo.ui.components.MeaoSettingsSwitchRow

@Composable
internal fun AiSettingsCard(
    settings: AiProviderSettings,
    status: AiSettingsStatus,
    onSave: (baseUrl: String, model: String, apiKey: String) -> Unit,
    onTest: () -> Unit,
    onClearKey: () -> Unit,
    onAutoDailyBriefChange: (Boolean) -> Unit,
    onAutoEveningReviewChange: (Boolean) -> Unit,
    onUsageLimitsChange: (dailyRequests: Int, monthlyTokens: Int) -> Unit
) {
    var baseUrl by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    // 密钥不进入 SavedState，避免 Activity 重建时被系统持久化。
    var apiKey by remember { mutableStateOf("") }

    LaunchedEffect(settings.baseUrl, settings.model) {
        if (baseUrl.isBlank()) baseUrl = settings.baseUrl
        if (model.isBlank()) model = settings.model
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("用户自带 API Key；密钥经 Android Keystore 加密后仅保存在本机。")
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("API Base URL") },
            placeholder = { Text("https://api.example.com/v1") },
            singleLine = true
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = model,
            onValueChange = { model = it },
            label = { Text("模型名称") },
            placeholder = { Text("model-name") },
            singleLine = true
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(if (settings.hasApiKey) "替换 API Key（当前 ${settings.maskedApiKey}）" else "API Key") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onSave(baseUrl, model, apiKey).also { apiKey = "" } },
                enabled = status !is AiSettingsStatus.Working
            ) { Text("保存") }
            OutlinedButton(
                onClick = onTest,
                enabled = settings.isConfigured && status !is AiSettingsStatus.Working
            ) { Text("测试连接") }
            if (settings.hasApiKey) {
                OutlinedButton(onClick = onClearKey, enabled = status !is AiSettingsStatus.Working) {
                    Text("删除 Key")
                }
            }
        }
        when (status) {
            AiSettingsStatus.Idle -> Unit
            AiSettingsStatus.Working -> Text("正在处理…")
            is AiSettingsStatus.Success -> Text(status.message)
            is AiSettingsStatus.Error -> Text("错误：${status.message}")
        }
        MeaoSettingsSwitchRow(
            icon = "☀",
            title = "自动每日建议",
            subtitle = "开启后，当天首次在 06:00 后进入应用时生成一次",
            checked = settings.autoDailyBrief,
            onCheckedChange = onAutoDailyBriefChange
        )
        MeaoSettingsSwitchRow(
            icon = "☾",
            title = "自动晚间复盘",
            subtitle = "开启后，当天首次在 20:00 后进入应用时生成一次",
            checked = settings.autoEveningReview,
            onCheckedChange = onAutoEveningReviewChange
        )
        Text("用量：今日 ${settings.requestsToday}/${settings.dailyRequestLimit} 次 · 本月 ${settings.tokensThisMonth}/${settings.monthlyTokenLimit} tokens")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple(10, 100_000, "节制"),
                Triple(20, 200_000, "标准"),
                Triple(50, 500_000, "宽松")
            ).forEach { (requests, tokens, label) ->
                OutlinedButton(onClick = { onUsageLimitsChange(requests, tokens) }) { Text(label) }
            }
        }
    }
}
