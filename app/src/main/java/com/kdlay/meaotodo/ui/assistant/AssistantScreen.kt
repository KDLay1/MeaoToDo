package com.kdlay.meaotodo.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.domain.assistant.PendingAction

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    var editingActionIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    editingActionIndex?.let { index ->
        state.pendingActions.getOrNull(index)?.let { action ->
            PendingActionEditorDialog(
                action = action,
                onDismiss = { editingActionIndex = null },
                onSave = { updated ->
                    viewModel.updateAction(index, updated)
                    editingActionIndex = null
                }
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Meao 助手", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (state.isAiConfigured) "AI 已连接：${state.aiModel}" else "本地助手可用 · AI 尚未配置",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onOpenSettings) { Text("设置") }
            }
        }
        item { DailySnapshotCard(state) }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("一句话安排", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("例如：下周交实验报告，拆成任务；记账 午饭 32；开始专注 写报告 45分钟")
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("告诉 Meao 你想做什么") },
                        minLines = 2
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.processInput(input)
                                input = ""
                            },
                            enabled = input.isNotBlank() && !state.isBusy
                        ) { Text("解析") }
                        OutlinedButton(onClick = viewModel::generateDailyBrief, enabled = !state.isBusy) {
                            Text("每日建议")
                        }
                        OutlinedButton(onClick = { viewModel.generateEveningReview(input) }, enabled = !state.isBusy) {
                            Text("晚间复盘")
                        }
                    }
                }
            }
        }
        if (state.isBusy || state.message != null || state.error != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (state.error != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.isBusy) CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                        Text(state.error ?: state.message.orEmpty())
                    }
                }
            }
        }
        state.dailyBrief?.let { brief ->
            item {
                AssistantResultCard("今日简报", brief.headline) {
                    brief.priorities.forEach { priority -> Text("• ${priority.title}：${priority.reason}") }
                    brief.reminders.forEach { reminder -> Text("提醒：$reminder") }
                    if (brief.encouragement.isNotBlank()) Text(brief.encouragement)
                }
            }
        }
        state.eveningReview?.let { review ->
            item {
                AssistantResultCard("今日复盘", review.summary) {
                    review.facts.take(3).forEach { Text("事实：$it") }
                    review.wins.take(2).forEach { Text("保留：$it") }
                    Text("明日重点：${review.tomorrowFocus}")
                }
            }
        }
        if (state.pendingActions.isNotEmpty()) {
            item { Text("待确认操作", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            itemsIndexed(state.pendingActions, key = { index, action -> "$index-${action.type}-${action.title}" }) { index, action ->
                PendingActionCard(
                    action = action,
                    enabled = !state.isBusy,
                    onConfirm = { viewModel.confirmAction(index) },
                    onEdit = { editingActionIndex = index },
                    onDiscard = { viewModel.discardAction(index) }
                )
            }
        }
        if (state.suggestions.isNotEmpty()) {
            item { Text("本地建议", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            itemsIndexed(state.suggestions) { _, suggestion ->
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(suggestion.title, fontWeight = FontWeight.Bold)
                        Text(suggestion.reason, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailySnapshotCard(state: AssistantUiState) {
    val context = state.context
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("今天", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (context == null) {
                Text("正在读取本地数据…")
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SnapshotMetric("待办", context.pendingTasks.size.toString())
                    SnapshotMetric("完成", context.completedTodayCount.toString())
                    SnapshotMetric("专注", "${context.focusedMinutes} 分钟")
                    SnapshotMetric("支出", "¥${context.todayExpenseCents / 100.0}")
                }
            }
        }
    }
}

@Composable
private fun SnapshotMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun AssistantResultCard(title: String, summary: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(summary)
            content()
        }
    }
}

@Composable
private fun PendingActionCard(
    action: PendingAction,
    enabled: Boolean,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDiscard: () -> Unit
) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(action.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(action.explanation)
            action.amountCents?.let { Text("金额：¥${it / 100.0} · ${action.category}") }
            action.focusMinutes?.let { Text("专注：$it 分钟") }
            action.estimatedPomodoros?.takeIf { it > 0 }?.let { Text("预计：$it 个番茄") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConfirm, enabled = enabled) { Text("确认执行") }
                OutlinedButton(onClick = onEdit, enabled = enabled) { Text("编辑") }
                OutlinedButton(onClick = onDiscard, enabled = enabled) { Text("放弃") }
            }
        }
    }
}
