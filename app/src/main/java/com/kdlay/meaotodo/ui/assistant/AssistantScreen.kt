package com.kdlay.meaotodo.ui.assistant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.domain.assistant.ActiveFocusSnapshot
import com.kdlay.meaotodo.domain.assistant.AssistantTaskSnapshot
import com.kdlay.meaotodo.ui.components.MeaoCompactStat
import com.kdlay.meaotodo.ui.components.MeaoPageHeader
import com.kdlay.meaotodo.ui.components.MeaoSectionTitle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    onOpenSettings: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenRecord: () -> Unit,
    onStartFocus: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = state.context
    val recommendedTask = context?.todayTasks?.firstOrNull()
        ?: context?.overdueTasks?.firstOrNull()
        ?: context?.pendingTasks?.firstOrNull()
    var input by rememberSaveable { mutableStateOf("") }
    var actionSheetIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var editingActionIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var previousActionCount by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(state.pendingActions.size) {
        val count = state.pendingActions.size
        if (count > previousActionCount) actionSheetIndex = count - 1
        if (count == 0) actionSheetIndex = null
        if (actionSheetIndex != null && actionSheetIndex !in state.pendingActions.indices) {
            actionSheetIndex = state.pendingActions.lastIndex.takeIf { it >= 0 }
        }
        previousActionCount = count
    }

    editingActionIndex?.let { index ->
        state.pendingActions.getOrNull(index)?.let { action ->
            PendingActionEditorDialog(
                action = action,
                onDismiss = { editingActionIndex = null },
                onSave = { updated ->
                    viewModel.updateAction(index, updated)
                    editingActionIndex = null
                    actionSheetIndex = index
                }
            )
        }
    }

    actionSheetIndex?.let { index ->
        state.pendingActions.getOrNull(index)?.let { action ->
            AssistantActionSheet(
                action = action,
                position = index,
                total = state.pendingActions.size,
                enabled = !state.isBusy,
                onConfirm = { if (!state.isBusy) viewModel.confirmAction(index) },
                onEdit = {
                    actionSheetIndex = null
                    editingActionIndex = index
                },
                onDiscard = { if (!state.isBusy) viewModel.discardAction(index) },
                onPrevious = if (index > 0) ({ actionSheetIndex = index - 1 }) else null,
                onNext = if (index < state.pendingActions.lastIndex) ({ actionSheetIndex = index + 1 }) else null,
                onDismiss = { actionSheetIndex = null }
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            MeaoPageHeader(
                title = greeting(),
                subtitle = "${formatHomeDate(System.currentTimeMillis())} · 先完成最重要的一件事",
                action = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                }
            )
        }

        item {
            AssistantCommandBar(
                input = input,
                busy = state.isBusy,
                aiConfigured = state.isAiConfigured,
                aiModel = state.aiModel,
                onInputChange = { input = it },
                onSubmit = {
                    val cleanInput = input.trim()
                    if (cleanInput.isNotEmpty()) {
                        viewModel.processInput(cleanInput)
                        input = ""
                    }
                }
            )
        }

        context?.activeFocus?.let { activeFocus ->
            item {
                ActiveFocusCard(
                    focus = activeFocus,
                    onOpen = { onStartFocus(activeFocus.taskId) }
                )
            }
        }

        if (state.pendingActions.isNotEmpty()) {
            item {
                PendingActionBanner(
                    count = state.pendingActions.size,
                    onOpen = { actionSheetIndex = 0 }
                )
            }
        }

        if (recommendedTask != null) {
            item { MeaoSectionTitle("下一步") }
            item {
                NextStepCard(
                    task = recommendedTask,
                    reason = state.suggestions.firstOrNull()?.reason
                        ?: buildTaskReason(
                            recommendedTask,
                            context?.overdueTasks.orEmpty().any { it.id == recommendedTask.id }
                        ),
                    onStartFocus = { onStartFocus(recommendedTask.id) },
                    onOpenPlan = onOpenPlan
                )
            }
        }

        item { MeaoSectionTitle("今日状态") }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                MeaoCompactStat(
                    modifier = Modifier.weight(1f),
                    value = "${context?.pendingTasks?.size ?: 0}",
                    label = "待完成",
                    onClick = onOpenPlan
                )
                MeaoCompactStat(
                    modifier = Modifier.weight(1f),
                    value = "${context?.focusedMinutes ?: 0}",
                    label = "专注分钟",
                    onClick = { onStartFocus(null) }
                )
                MeaoCompactStat(
                    modifier = Modifier.weight(1f),
                    value = money(context?.todayExpenseCents ?: 0),
                    label = "今日支出",
                    onClick = onOpenRecord
                )
            }
        }

        val todayTasks = context?.todayTasks.orEmpty().take(4)
        if (todayTasks.isNotEmpty()) {
            item {
                MeaoSectionTitle(
                    title = "今天的任务",
                    actionText = "查看全部",
                    onAction = onOpenPlan
                )
            }
            item { TodayTaskCard(todayTasks, onOpenPlan) }
        }

        if (state.isBusy || state.message != null || state.error != null) {
            item { FeedbackCard(state) }
        }

        state.dailyBrief?.let { brief ->
            item {
                AssistantResultCard("Meao 建议", brief.headline) {
                    brief.priorities.take(2).forEach { priority ->
                        Text(
                            text = "• ${priority.title}：${priority.reason}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (brief.encouragement.isNotBlank()) {
                        Text(brief.encouragement, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } ?: state.suggestions.getOrNull(1)?.let { suggestion ->
            item {
                DailyAdviceCard(
                    title = suggestion.title,
                    reason = suggestion.reason,
                    aiConfigured = state.isAiConfigured,
                    onGenerate = viewModel::generateDailyBrief,
                    onOpenSettings = onOpenSettings
                )
            }
        }

        state.eveningReview?.let { review ->
            item {
                AssistantResultCard("今日复盘", review.summary) {
                    review.wins.take(2).forEach { Text("保留：$it") }
                    Text("明日重点：${review.tomorrowFocus}", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AssistantCommandBar(
    input: String,
    busy: Boolean,
    aiConfigured: Boolean,
    aiModel: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    ),
                    decorationBox = { inner ->
                        Box(Modifier.padding(horizontal = 12.dp)) {
                            if (input.isBlank()) {
                                Text(
                                    "记任务、记支出或调整今天的计划…",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            inner()
                        }
                    }
                )
                IconButton(onClick = onSubmit, enabled = input.isNotBlank() && !busy) {
                    Surface(
                        shape = CircleShape,
                        color = if (input.isNotBlank() && !busy) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(10.dp).size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "发送",
                                modifier = Modifier.padding(10.dp).size(20.dp),
                                tint = if (input.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Text(
                text = if (aiConfigured) {
                    "本地命令始终可用 · AI 已连接${aiModel.takeIf { it.isNotBlank() }?.let { "：$it" }.orEmpty()}"
                } else {
                    "本地命令可用 · 配置 API 后可生成建议和复盘"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActiveFocusCard(focus: ActiveFocusSnapshot, onOpen: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "正在专注",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    focus.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun PendingActionBanner(count: Int, onOpen: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text("有 $count 项操作等待确认", fontWeight = FontWeight.Bold)
                Text(
                    "Meao 不会在后台直接修改数据",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("查看", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NextStepCard(
    task: AssistantTaskSnapshot,
    reason: String,
    onStartFocus: () -> Unit,
    onOpenPlan: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PriorityDot(task.priority)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        reason,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip(if (task.dueAt != null) formatTaskDate(task.dueAt) else "未安排日期")
                MetaChip("${task.remainingPomodoros.coerceAtLeast(1)} 个番茄")
                MetaChip(priorityText(task.priority))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(onClick = onStartFocus) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text("开始专注")
                }
                OutlinedButton(onClick = onOpenPlan) { Text("查看计划") }
            }
        }
    }
}

@Composable
private fun TodayTaskCard(tasks: List<AssistantTaskSnapshot>, onOpenPlan: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
            tasks.forEach { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenPlan)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PriorityDot(task.priority)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            task.title,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${task.remainingPomodoros.coerceAtLeast(1)} 个番茄${task.dueAt?.let { " · ${formatTaskTime(it)}" }.orEmpty()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PriorityDot(priority: Int) {
    Surface(
        modifier = Modifier.size(10.dp),
        shape = CircleShape,
        color = when (priority) {
            3 -> MaterialTheme.colorScheme.error
            2 -> MaterialTheme.colorScheme.secondary
            1 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.outline
        }
    ) {}
}

@Composable
private fun MetaChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DailyAdviceCard(
    title: String,
    reason: String,
    aiConfigured: Boolean,
    onGenerate: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = null,
                    modifier = Modifier.padding(11.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("每日建议", fontWeight = FontWeight.Bold)
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Text(
                text = if (aiConfigured) "生成" else "配置",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = if (aiConfigured) onGenerate else onOpenSettings)
            )
        }
    }
}

@Composable
private fun FeedbackCard(state: AssistantUiState) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (state.error != null) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.isBusy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            Text(state.error ?: state.message.orEmpty())
        }
    }
}

@Composable
private fun AssistantResultCard(
    title: String,
    summary: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(summary)
            content()
        }
    }
}

private fun buildTaskReason(task: AssistantTaskSnapshot, overdue: Boolean): String = when {
    overdue -> "已经逾期，建议先清理这项任务。"
    task.priority >= 3 -> "优先级较高，适合放在今天最清醒的时间完成。"
    task.remainingPomodoros > 1 -> "需要连续投入，先完成第一轮会更容易推进。"
    else -> "预计一轮专注即可取得明显进展。"
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..10 -> "早上好"
    in 11..13 -> "中午好"
    in 14..18 -> "下午好"
    else -> "晚上好"
}

private fun formatHomeDate(timestamp: Long): String =
    SimpleDateFormat("M 月 d 日 EEEE", Locale.CHINESE).format(Date(timestamp))

private fun money(cents: Long): String =
    if (cents % 100L == 0L) "¥${cents / 100}" else "¥%.2f".format(cents / 100.0)

private fun formatTaskDate(timestamp: Long): String =
    SimpleDateFormat("M 月 d 日", Locale.getDefault()).format(Date(timestamp))

private fun formatTaskTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun priorityText(priority: Int): String = when (priority) {
    3 -> "高优先级"
    2 -> "中优先级"
    1 -> "低优先级"
    else -> "普通"
}
