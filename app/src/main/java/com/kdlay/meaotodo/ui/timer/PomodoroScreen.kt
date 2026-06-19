package com.kdlay.meaotodo.ui.timer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.data.repository.PomodoroRepository
import com.kdlay.meaotodo.ui.components.WheelPickerColumn

@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedDurationMinutes by rememberSaveable { mutableIntStateOf(25) }
    var targetFocusCount by rememberSaveable { mutableIntStateOf(1) }
    var selectedTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var showTaskPicker by remember { mutableStateOf(false) }
    var showDurationWheel by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(uiState.tasks, selectedTaskId) {
        if (selectedTaskId != null && uiState.tasks.none { it.id == selectedTaskId }) {
            selectedTaskId = null
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PomodoroHeader(uiState = uiState)

            if (uiState.activeSession == null) {
                IdlePomodoroPanel(
                    tasks = uiState.tasks,
                    selectedTaskId = selectedTaskId,
                    selectedDurationMinutes = selectedDurationMinutes,
                    targetFocusCount = targetFocusCount,
                    summary = uiState.summary,
                    onDurationChange = { selectedDurationMinutes = it },
                    onTargetFocusCountChange = { targetFocusCount = it },
                    onPickTask = { showTaskPicker = true },
                    onClearTask = { selectedTaskId = null },
                    onOpenDurationWheel = { showDurationWheel = true },
                    onOpenSummary = { showSummary = true },
                    onStart = { viewModel.start(selectedTaskId, selectedDurationMinutes, targetFocusCount) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                ActivePomodoroPanel(
                    uiState = uiState,
                    onPause = viewModel::pause,
                    onResume = viewModel::resume,
                    onCompleteCurrentSession = viewModel::completeCurrentSession,
                    onSkipBreak = viewModel::skipBreak,
                    onCancel = viewModel::cancel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showTaskPicker) {
        TaskPickerDialog(
            tasks = uiState.tasks,
            selectedTaskId = selectedTaskId,
            onSelect = { selectedTaskId = it },
            onClear = { selectedTaskId = null },
            onDismiss = { showTaskPicker = false }
        )
    }

    if (showDurationWheel) {
        DurationWheelDialog(
            initialDurationMinutes = selectedDurationMinutes,
            onDismiss = { showDurationWheel = false },
            onConfirm = { duration ->
                selectedDurationMinutes = duration
                showDurationWheel = false
            }
        )
    }

    if (showSummary) {
        PomodoroSummaryDialog(
            summary = uiState.summary,
            recentSessions = uiState.recentSessions,
            onDismiss = { showSummary = false }
        )
    }
}

@Composable
private fun PomodoroHeader(uiState: PomodoroUiState) {
    val statusLabel = if (uiState.activeSession == null) "准备开始" else uiState.statusLabel
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("番茄钟", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = if (uiState.activeSession == null) {
                        "设置任务、时长和轮数"
                    } else {
                        "${uiState.roundLabel} · ${uiState.taskTitle}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.74f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusPill(text = statusLabel)
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdlePomodoroPanel(
    tasks: List<TaskEntity>,
    selectedTaskId: String?,
    selectedDurationMinutes: Int,
    targetFocusCount: Int,
    summary: PomodoroSummary,
    onDurationChange: (Int) -> Unit,
    onTargetFocusCountChange: (Int) -> Unit,
    onPickTask: () -> Unit,
    onClearTask: () -> Unit,
    onOpenDurationWheel: () -> Unit,
    onOpenSummary: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId }
    val quickDurations = listOf(15, 25, 45, 60, 90)
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SummaryBar(summary = summary, onClick = onOpenSummary)

        FocusPlanCard(
            selectedTask = selectedTask,
            selectedDurationMinutes = selectedDurationMinutes,
            targetFocusCount = targetFocusCount,
            taskCount = tasks.size,
            onPickTask = onPickTask,
            onClearTask = onClearTask
        )

        SettingCard(
            title = "专注时长",
            caption = "常用时长一键选择，需要细调时再打开滚轮。"
        ) {
            Text(
                text = "$selectedDurationMinutes 分钟",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickDurations.forEach { duration ->
                    DurationChip(
                        duration = duration,
                        selected = selectedDurationMinutes == duration,
                        onClick = { onDurationChange(duration) }
                    )
                }
            }
            OutlinedButton(onClick = onOpenDurationWheel) {
                Text("滚轮微调")
            }
        }

        SettingCard(
            title = "本轮番茄数",
            caption = "每个番茄后自动进入 5 分钟休息。"
        ) {
            Text("$targetFocusCount 个", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 2, 3, 4).forEach { count ->
                    CountChip(
                        count = count,
                        selected = targetFocusCount == count,
                        onClick = { onTargetFocusCountChange(count) }
                    )
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onStart
        ) {
            Text("开始本轮专注", modifier = Modifier.padding(vertical = 8.dp), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FocusPlanCard(
    selectedTask: TaskEntity?,
    selectedDurationMinutes: Int,
    targetFocusCount: Int,
    taskCount: Int,
    onPickTask: () -> Unit,
    onClearTask: () -> Unit
) {
    val totalFocusMinutes = selectedDurationMinutes * targetFocusCount
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "本轮计划",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$selectedDurationMinutes:00",
                fontSize = 64.sp,
                lineHeight = 66.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "番茄",
                    value = "${targetFocusCount}个"
                )
                MiniMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "专注",
                    value = "${totalFocusMinutes}分"
                )
                MiniMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "休息",
                    value = "5分"
                )
            }
            TaskBindingCard(
                selectedTask = selectedTask,
                taskCount = taskCount,
                onPickTask = onPickTask,
                onClearTask = onClearTask
            )
        }
    }
}

@Composable
private fun MiniMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    caption: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
private fun SummaryBar(summary: PomodoroSummary, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "今日 ${summary.finishedFocusCount} 个番茄 · ${formatCompactDuration(summary.focusSeconds)} · 中断 ${summary.cancelledFocusCount} 次",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("›", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TaskBindingCard(
    selectedTask: TaskEntity?,
    taskCount: Int,
    onPickTask: () -> Unit,
    onClearTask: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPickTask),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("当前任务", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text("点击更换", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = selectedTask?.title ?: "空白专注",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (selectedTask == null) {
                    if (taskCount == 0) "当前还没有任务，可先直接开始空白番茄钟。" else "可以绑定一个 Todo，也可以保留空白专注。"
                } else {
                    "完成后会保存任务标题快照。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selectedTask != null) {
                TextButton(onClick = onClearTask) { Text("清除绑定") }
            }
        }
    }
}

@Composable
private fun ActivePomodoroPanel(
    uiState: PomodoroUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCompleteCurrentSession: () -> Unit,
    onSkipBreak: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session = uiState.activeSession ?: return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ActiveTimerCard(uiState = uiState, session = session)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.isPaused) {
                    Button(modifier = Modifier.weight(1f), onClick = onResume) { Text("继续") }
                } else {
                    FilledTonalButton(modifier = Modifier.weight(1f), onClick = onPause) { Text("暂停") }
                }
                if (uiState.isBreak) {
                    Button(modifier = Modifier.weight(1f), onClick = onSkipBreak) { Text("跳过休息") }
                } else {
                    Button(modifier = Modifier.weight(1f), onClick = onCompleteCurrentSession) { Text("完成本阶段") }
                }
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onCancel) { Text("放弃本轮") }
        }
    }
}

@Composable
private fun ActiveTimerCard(
    uiState: PomodoroUiState,
    session: PomodoroSessionEntity
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatDuration(uiState.remainingSeconds),
                fontSize = 78.sp,
                lineHeight = 80.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            SegmentedProgressBar(progress = uiState.progress)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = uiState.statusLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = uiState.roundLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.taskTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "计划 ${session.plannedDurationSeconds / 60} 分钟 · 已运行 ${formatDuration(uiState.elapsedSeconds)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.nextLabel.isNotBlank()) {
                        Text(
                            text = uiState.nextLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedProgressBar(
    progress: Float,
    segmentCount: Int = 24
) {
    val activeCount = (progress * segmentCount).toInt().coerceIn(0, segmentCount)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(segmentCount) { index ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp),
                shape = RoundedCornerShape(999.dp),
                color = if (index < activeCount) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {}
        }
    }
}

@Composable
private fun DurationChip(
    duration: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            text = "${duration}分",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CountChip(
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            text = "$count",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DurationWheelDialog(
    initialDurationMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var draftDuration by remember { mutableIntStateOf(initialDurationMinutes) }
    val wheelDurations = listOf(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 75, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("滚轮微调", fontWeight = FontWeight.Bold)
                Text(
                    text = "滚轮只在点击确定后写入时长。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        text = "$draftDuration 分钟",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                WheelPickerColumn(
                    title = "分钟",
                    values = wheelDurations,
                    selectedValue = draftDuration,
                    onCenteredValueChange = { draftDuration = it },
                    itemLabel = { it.toString() },
                    columnWidth = 112.dp,
                    itemWidth = 86.dp,
                    viewportHeight = 180.dp
                )
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = { onConfirm(draftDuration) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun TaskPickerDialog(
    tasks: List<TaskEntity>,
    selectedTaskId: String?,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("绑定任务", fontWeight = FontWeight.Bold)
                Text(
                    text = "选择一个 Todo 任务，或保留空白专注。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onClear()
                            onDismiss()
                        },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selectedTaskId == null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                ) {
                    Text(
                        modifier = Modifier.padding(14.dp),
                        text = "空白专注",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (tasks.isEmpty()) {
                    Text(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        text = "还没有可绑定的 Todo 任务。可以先空白开始，或回到今日页添加任务。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            TaskPickerItem(
                                task = task,
                                selected = task.id == selectedTaskId,
                                onClick = {
                                    onSelect(task.id)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}

@Composable
private fun TaskPickerItem(
    task: TaskEntity,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(task.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (task.note.isNotBlank()) {
                    Text(
                        task.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = when {
                    selected -> "已选"
                    task.isDone -> "已完成"
                    else -> "选择"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PomodoroSummaryDialog(
    summary: PomodoroSummary,
    recentSessions: List<PomodoroSessionEntity>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("今日专注摘要", fontWeight = FontWeight.Bold)
                Text(
                    text = "休息记录暂时保留显示，方便验证循环。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("完成番茄：${summary.finishedFocusCount}", fontWeight = FontWeight.SemiBold)
                        Text("专注时长：${formatCompactDuration(summary.focusSeconds)}")
                        Text("中断次数：${summary.cancelledFocusCount}")
                        Text("休息次数：${summary.finishedBreakCount}")
                    }
                }
                Text("最近记录", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                if (recentSessions.isEmpty()) {
                    Text("还没有专注记录。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentSessions, key = { it.id }) { session ->
                            RecentSessionItem(session = session)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun RecentSessionItem(session: PomodoroSessionEntity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (session.type == PomodoroRepository.TYPE_BREAK) "休息" else session.titleSnapshot ?: "空白专注",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${sessionTypeLabel(session)} · 第 ${session.roundIndex} 轮 · ${sessionStatusLabel(session)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatCompactDuration(if (session.actualDurationSeconds > 0) session.actualDurationSeconds else session.plannedDurationSeconds),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun sessionTypeLabel(session: PomodoroSessionEntity): String =
    if (session.type == PomodoroRepository.TYPE_BREAK) "休息" else "专注"

private fun sessionStatusLabel(session: PomodoroSessionEntity): String = when (session.status) {
    PomodoroRepository.STATUS_FINISHED -> "已完成"
    PomodoroRepository.STATUS_CANCELLED -> "已放弃"
    PomodoroRepository.STATUS_PAUSED -> "暂停中"
    PomodoroRepository.STATUS_RUNNING -> "进行中"
    else -> session.status
}

private fun formatDuration(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val remainingSeconds = safeSeconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

private fun formatCompactDuration(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
