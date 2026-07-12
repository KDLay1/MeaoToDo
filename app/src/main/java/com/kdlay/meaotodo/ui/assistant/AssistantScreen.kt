package com.kdlay.meaotodo.ui.assistant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.domain.assistant.AssistantTaskSnapshot
import com.kdlay.meaotodo.domain.assistant.PendingAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    onOpenSettings: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenRecord: () -> Unit,
    onStartFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    var editingActionIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    editingActionIndex?.let { index ->
        state.pendingActions.getOrNull(index)?.let { action ->
            PendingActionEditorDialog(action, { editingActionIndex = null }) {
                viewModel.updateAction(index, it)
                editingActionIndex = null
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { AssistantHeader(onOpenSettings) }
        item {
            AssistantCommandBar(
                input = input,
                busy = state.isBusy,
                onInputChange = { input = it },
                onSubmit = {
                    viewModel.processInput(input)
                    input = ""
                }
            )
        }
        item { SectionHeader("今天概览", "数据实时更新") }
        item { DailySnapshotGrid(state, onOpenPlan, onOpenRecord) }

        state.suggestions.firstOrNull()?.let { suggestion ->
            item {
                NextStepCard(
                    title = suggestion.title,
                    reason = suggestion.reason,
                    onStartFocus = onStartFocus,
                    onOpenPlan = onOpenPlan
                )
            }
        }

        val todayTasks = state.context?.todayTasks.orEmpty().take(3)
        if (todayTasks.isNotEmpty()) {
            item { SectionHeader("今日安排", "查看日历", onOpenPlan) }
            item { TodayScheduleCard(todayTasks) }
        }

        if (state.isBusy || state.message != null || state.error != null) {
            item { FeedbackCard(state) }
        }
        state.dailyBrief?.let { brief ->
            item {
                AssistantResultCard("每日建议", brief.headline) {
                    brief.priorities.take(2).forEach { Text("• ${it.title}：${it.reason}") }
                    if (brief.encouragement.isNotBlank()) Text(brief.encouragement)
                }
            }
        } ?: state.suggestions.getOrNull(1)?.let { suggestion ->
            item { DailyAdviceCard(suggestion.title, suggestion.reason) { viewModel.generateDailyBrief() } }
        }

        state.eveningReview?.let { review ->
            item {
                AssistantResultCard("今日复盘", review.summary) {
                    review.wins.take(2).forEach { Text("保留：$it") }
                    Text("明日重点：${review.tomorrowFocus}")
                }
            }
        }
        if (state.pendingActions.isNotEmpty()) {
            item { SectionHeader("待确认操作", "${state.pendingActions.size} 项") }
            itemsIndexed(state.pendingActions, key = { index, action -> "$index-${action.type}-${action.title}" }) { index, action ->
                PendingActionCard(action, !state.isBusy, { viewModel.confirmAction(index) }, { editingActionIndex = index }) {
                    viewModel.discardAction(index)
                }
            }
        }
    }
}

@Composable
private fun AssistantHeader(onOpenSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(greeting(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("今天先完成最重要的一件事", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "设置", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun AssistantCommandBar(input: String, busy: Boolean, onInputChange: (String) -> Unit, onSubmit: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Filled.Home, null, Modifier.padding(10.dp).size(22.dp), tint = MaterialTheme.colorScheme.primary)
            }
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                decorationBox = { inner ->
                    Box(Modifier.padding(horizontal = 12.dp)) {
                        if (input.isBlank()) Text("告诉 Meao 你想做什么…", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        inner()
                    }
                }
            )
            IconButton(onClick = onSubmit, enabled = input.isNotBlank() && !busy) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.AutoMirrored.Filled.Send, "发送", Modifier.padding(10.dp).size(21.dp), tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(
            modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(action, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (onClick != null) Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DailySnapshotGrid(state: AssistantUiState, onOpenPlan: () -> Unit, onOpenRecord: () -> Unit) {
    val context = state.context
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SnapshotMetric(Modifier.weight(1f), Icons.Filled.CheckCircle, "${context?.pendingTasks?.size ?: 0}", "待完成", onOpenPlan)
        SnapshotMetric(Modifier.weight(1f), Icons.AutoMirrored.Filled.List, "${context?.focusedMinutes ?: 0}", "专注分钟", onOpenPlan)
        SnapshotMetric(Modifier.weight(1f), Icons.Filled.DateRange, money(context?.todayExpenseCents ?: 0), "今日支出", onOpenRecord)
        val budgetLabel = when {
            context == null || context.monthlyBudgetCents <= 0 -> "未设置"
            context.monthExpenseCents <= context.monthlyBudgetCents -> "正常"
            else -> "超支"
        }
        SnapshotMetric(Modifier.weight(1f), Icons.Filled.Refresh, budgetLabel, "当前预算", onOpenRecord)
    }
}

@Composable
private fun SnapshotMetric(modifier: Modifier, icon: ImageVector, value: String, label: String, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun NextStepCard(title: String, reason: String, onStartFocus: () -> Unit, onOpenPlan: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(Modifier.size(92.dp), RoundedCornerShape(18.dp), MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Home, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("下一步建议", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(reason, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStartFocus) { Icon(Icons.Filled.PlayArrow, null); Text("开始专注") }
                    OutlinedButton(onClick = onOpenPlan) { Text("查看计划") }
                }
            }
        }
    }
}

@Composable
private fun TodayScheduleCard(tasks: List<AssistantTaskSnapshot>) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            tasks.forEachIndexed { index, task ->
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatTime(task.dueAt), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(.22f))
                    Surface(Modifier.size(10.dp), CircleShape, MaterialTheme.colorScheme.primary) {}
                    Column(Modifier.padding(start = 14.dp).weight(.78f)) {
                        Text(task.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("预计 ${task.estimatedPomodoros.coerceAtLeast(1) * 25} 分钟", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (index < tasks.lastIndex) Box(Modifier.fillMaxWidth().padding(start = 88.dp).background(MaterialTheme.colorScheme.outlineVariant).size(height = 1.dp, width = 1.dp))
            }
        }
    }
}

@Composable
private fun DailyAdviceCard(title: String, reason: String, onGenerate: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                Icon(Icons.Filled.Home, null, Modifier.padding(13.dp).size(26.dp), tint = MaterialTheme.colorScheme.tertiary)
            }
            Column(Modifier.weight(1f)) {
                Text("每日建议", fontWeight = FontWeight.Bold)
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Text("生成", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onGenerate))
        }
    }
}

@Composable
private fun FeedbackCard(state: AssistantUiState) {
    Surface(shape = RoundedCornerShape(16.dp), color = if (state.error != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.fillMaxWidth().padding(14.dp), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
            if (state.isBusy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            Text(state.error ?: state.message.orEmpty())
        }
    }
}

@Composable
private fun AssistantResultCard(title: String, summary: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(summary)
            content()
        }
    }
}

@Composable
private fun PendingActionCard(action: PendingAction, enabled: Boolean, onConfirm: () -> Unit, onEdit: () -> Unit, onDiscard: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(action.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(action.explanation, color = MaterialTheme.colorScheme.onSurfaceVariant)
            action.amountCents?.let { Text("金额：${money(it)} · ${action.category}") }
            action.focusMinutes?.let { Text("专注：$it 分钟") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConfirm, enabled = enabled) { Text("确认执行") }
                OutlinedButton(onClick = onEdit, enabled = enabled) { Text("编辑") }
                OutlinedButton(onClick = onDiscard, enabled = enabled, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("放弃") }
            }
        }
    }
}

private fun greeting(): String = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
    in 5..10 -> "早上好"
    in 11..13 -> "中午好"
    in 14..18 -> "下午好"
    else -> "晚上好"
}

private fun money(cents: Long): String = if (cents % 100L == 0L) "¥${cents / 100}" else "¥%.2f".format(cents / 100.0)
private fun formatTime(timestamp: Long?): String = timestamp?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)) } ?: "全天"
