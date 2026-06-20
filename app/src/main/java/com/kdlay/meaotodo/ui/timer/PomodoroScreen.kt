package com.kdlay.meaotodo.ui.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.using
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.DisposableEffect
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

private const val CLOCK_STYLE_DIGITAL = "digital"
private const val CLOCK_STYLE_FLIP = "flip"

@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel,
    modifier: Modifier = Modifier,
    onImmersiveModeChange: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedDurationMinutes by rememberSaveable { mutableIntStateOf(25) }
    var selectedBreakDurationMinutes by rememberSaveable { mutableIntStateOf(5) }
    var targetFocusCount by rememberSaveable { mutableIntStateOf(1) }
    var selectedTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var showTaskPicker by remember { mutableStateOf(false) }
    var showDurationWheel by remember { mutableStateOf(false) }
    var showBreakDurationWheel by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }
    var isImmersiveMode by rememberSaveable { mutableStateOf(false) }
    var clockStyle by rememberSaveable { mutableStateOf(CLOCK_STYLE_DIGITAL) }
    val hasActiveTimer = uiState.activeSession != null

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

    LaunchedEffect(hasActiveTimer, isImmersiveMode) {
        if (!hasActiveTimer && isImmersiveMode) {
            isImmersiveMode = false
        }
        onImmersiveModeChange(hasActiveTimer && isImmersiveMode)
    }

    DisposableEffect(Unit) {
        onDispose { onImmersiveModeChange(false) }
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
                .padding(horizontal = 20.dp, vertical = if (hasActiveTimer && isImmersiveMode) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!hasActiveTimer || !isImmersiveMode) {
                PomodoroHeader(uiState = uiState)
            }

            if (!hasActiveTimer) {
                IdlePomodoroPanel(
                    tasks = uiState.tasks,
                    selectedTaskId = selectedTaskId,
                    selectedDurationMinutes = selectedDurationMinutes,
                    selectedBreakDurationMinutes = selectedBreakDurationMinutes,
                    targetFocusCount = targetFocusCount,
                    summary = uiState.summary,
                    onDurationChange = { selectedDurationMinutes = it },
                    onBreakDurationChange = { selectedBreakDurationMinutes = it },
                    onTargetFocusCountChange = { targetFocusCount = it },
                    onPickTask = { showTaskPicker = true },
                    onOpenDurationWheel = { showDurationWheel = true },
                    onOpenBreakDurationWheel = { showBreakDurationWheel = true },
                    onOpenSummary = { showSummary = true },
                    onStart = {
                        viewModel.start(
                            taskId = selectedTaskId,
                            durationMinutes = selectedDurationMinutes,
                            breakDurationMinutes = selectedBreakDurationMinutes,
                            targetFocusCount = targetFocusCount
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                ActivePomodoroPanel(
                    uiState = uiState,
                    isImmersiveMode = isImmersiveMode,
                    clockStyle = clockStyle,
                    onToggleImmersiveMode = { isImmersiveMode = !isImmersiveMode },
                    onToggleClockStyle = {
                        clockStyle = if (clockStyle == CLOCK_STYLE_FLIP) CLOCK_STYLE_DIGITAL else CLOCK_STYLE_FLIP
                    },
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
            title = "专注时长",
            description = "滚轮只在点击确定后写入专注时长。",
            initialDurationMinutes = selectedDurationMinutes,
            wheelDurations = listOf(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 75, 90, 120),
            onDismiss = { showDurationWheel = false },
            onConfirm = { duration ->
                selectedDurationMinutes = duration
                showDurationWheel = false
            }
        )
    }

    if (showBreakDurationWheel) {
        DurationWheelDialog(
            title = "休息时长",
            description = "休息时长会应用到本轮每次短休息。",
            initialDurationMinutes = selectedBreakDurationMinutes,
            wheelDurations = listOf(1, 3, 5, 8, 10, 12, 15, 20, 25, 30),
            onDismiss = { showBreakDurationWheel = false },
            onConfirm = { duration ->
                selectedBreakDurationMinutes = duration
                showBreakDurationWheel = false
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
    val trailingText = if (uiState.activeSession == null) {
        "今日 ${uiState.summary.finishedFocusCount} 个"
    } else {
        uiState.statusLabel
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("番茄钟", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            StatusPill(text = trailingText)
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun IdlePomodoroPanel(
    tasks: List<TaskEntity>,
    selectedTaskId: String?,
    selectedDurationMinutes: Int,
    selectedBreakDurationMinutes: Int,
    targetFocusCount: Int,
    summary: PomodoroSummary,
    onDurationChange: (Int) -> Unit,
    onBreakDurationChange: (Int) -> Unit,
    onTargetFocusCountChange: (Int) -> Unit,
    onPickTask: () -> Unit,
    onOpenDurationWheel: () -> Unit,
    onOpenBreakDurationWheel: () -> Unit,
    onOpenSummary: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId }
    val quickDurations = listOf(15, 25, 45, 60, 90)
    val quickBreakDurations = listOf(3, 5, 10, 15)
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompactPlanCard(
            selectedTask = selectedTask,
            selectedDurationMinutes = selectedDurationMinutes,
            selectedBreakDurationMinutes = selectedBreakDurationMinutes,
            targetFocusCount = targetFocusCount,
            onPickTask = onPickTask
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onStart
        ) {
            Text("开始专注", modifier = Modifier.padding(vertical = 8.dp), fontWeight = FontWeight.Bold)
        }

        CompactSettingsPanel(
            quickDurations = quickDurations,
            quickBreakDurations = quickBreakDurations,
            selectedDurationMinutes = selectedDurationMinutes,
            selectedBreakDurationMinutes = selectedBreakDurationMinutes,
            targetFocusCount = targetFocusCount,
            onDurationChange = onDurationChange,
            onBreakDurationChange = onBreakDurationChange,
            onTargetFocusCountChange = onTargetFocusCountChange,
            onOpenDurationWheel = onOpenDurationWheel,
            onOpenBreakDurationWheel = onOpenBreakDurationWheel
        )

        SummaryBar(summary = summary, onClick = onOpenSummary)
    }
}

@Composable
private fun CompactPlanCard(
    selectedTask: TaskEntity?,
    selectedDurationMinutes: Int,
    selectedBreakDurationMinutes: Int,
    targetFocusCount: Int,
    onPickTask: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatDuration(selectedDurationMinutes * 60),
                fontSize = 58.sp,
                lineHeight = 60.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("当前任务", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = selectedTask?.title ?: "空白专注",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(onClick = onPickTask) { Text("更换") }
            }
            Text(
                text = "$targetFocusCount 个番茄 · 每次休息 $selectedBreakDurationMinutes 分钟",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactSettingsPanel(
    quickDurations: List<Int>,
    quickBreakDurations: List<Int>,
    selectedDurationMinutes: Int,
    selectedBreakDurationMinutes: Int,
    targetFocusCount: Int,
    onDurationChange: (Int) -> Unit,
    onBreakDurationChange: (Int) -> Unit,
    onTargetFocusCountChange: (Int) -> Unit,
    onOpenDurationWheel: () -> Unit,
    onOpenBreakDurationWheel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactSettingRow(label = "时长") {
                quickDurations.forEach { duration ->
                    DurationChip(
                        duration = duration,
                        selected = selectedDurationMinutes == duration,
                        onClick = { onDurationChange(duration) }
                    )
                }
                SmallActionChip(text = "滚轮", onClick = onOpenDurationWheel)
            }
            CompactSettingRow(label = "休息") {
                quickBreakDurations.forEach { duration ->
                    DurationChip(
                        duration = duration,
                        selected = selectedBreakDurationMinutes == duration,
                        onClick = { onBreakDurationChange(duration) }
                    )
                }
                SmallActionChip(text = "滚轮", onClick = onOpenBreakDurationWheel)
            }
            CompactSettingRow(label = "轮数") {
                listOf(1, 2, 3, 4).forEach { count ->
                    CountChip(
                        count = count,
                        selected = targetFocusCount == count,
                        onClick = { onTargetFocusCountChange(count) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactSettingRow(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
private fun ActivePomodoroPanel(
    uiState: PomodoroUiState,
    isImmersiveMode: Boolean,
    clockStyle: String,
    onToggleImmersiveMode: () -> Unit,
    onToggleClockStyle: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCompleteCurrentSession: () -> Unit,
    onSkipBreak: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session = uiState.activeSession ?: return
    if (isImmersiveMode) {
        ImmersivePomodoroPanel(
            uiState = uiState,
            session = session,
            clockStyle = clockStyle,
            onToggleImmersiveMode = onToggleImmersiveMode,
            onToggleClockStyle = onToggleClockStyle,
            onPause = onPause,
            onResume = onResume,
            onCompleteCurrentSession = onCompleteCurrentSession,
            onSkipBreak = onSkipBreak,
            onCancel = onCancel,
            modifier = modifier
        )
        return
    }

    val scrollState = rememberScrollState()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isWideLayout = maxWidth > maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ActiveModeBar(
                isImmersiveMode = false,
                clockStyle = clockStyle,
                onToggleImmersiveMode = onToggleImmersiveMode,
                onToggleClockStyle = onToggleClockStyle
            )

            if (isWideLayout) {
                ActiveTimerWideCard(
                    uiState = uiState,
                    session = session,
                    clockStyle = clockStyle,
                    isImmersiveMode = false
                )
            } else {
                ActiveTimerCard(
                    uiState = uiState,
                    session = session,
                    clockStyle = clockStyle,
                    isImmersiveMode = false
                )
            }

            ActiveControlPanel(
                uiState = uiState,
                onPause = onPause,
                onResume = onResume,
                onCompleteCurrentSession = onCompleteCurrentSession,
                onSkipBreak = onSkipBreak,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun ImmersivePomodoroPanel(
    uiState: PomodoroUiState,
    session: PomodoroSessionEntity,
    clockStyle: String,
    onToggleImmersiveMode: () -> Unit,
    onToggleClockStyle: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCompleteCurrentSession: () -> Unit,
    onSkipBreak: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLayout = maxWidth > maxHeight
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ActiveModeBar(
                isImmersiveMode = true,
                clockStyle = clockStyle,
                onToggleImmersiveMode = onToggleImmersiveMode,
                onToggleClockStyle = onToggleClockStyle
            )

            if (isWideLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TimerClockDisplay(
                            timeText = formatDuration(uiState.remainingSeconds),
                            clockStyle = clockStyle,
                            compact = false,
                            prominent = true
                        )
                        SegmentedProgressBar(progress = uiState.progress)
                    }
                    Column(
                        modifier = Modifier.weight(0.8f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PhaseInfo(uiState = uiState, compact = false)
                        ImmersiveTaskLine(uiState = uiState, session = session)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimerClockDisplay(
                        timeText = formatDuration(uiState.remainingSeconds),
                        clockStyle = clockStyle,
                        compact = false,
                        prominent = true
                    )
                    Column(
                        modifier = Modifier.padding(top = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SegmentedProgressBar(progress = uiState.progress)
                        PhaseInfo(uiState = uiState, compact = false)
                        ImmersiveTaskLine(uiState = uiState, session = session)
                    }
                }
            }

            ActiveControlPanel(
                uiState = uiState,
                onPause = onPause,
                onResume = onResume,
                onCompleteCurrentSession = onCompleteCurrentSession,
                onSkipBreak = onSkipBreak,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun ActiveModeBar(
    isImmersiveMode: Boolean,
    clockStyle: String,
    onToggleImmersiveMode: () -> Unit,
    onToggleClockStyle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmallActionChip(
            text = if (clockStyle == CLOCK_STYLE_FLIP) "数字时钟" else "翻页时钟",
            onClick = onToggleClockStyle
        )
        SmallActionChip(
            text = if (isImmersiveMode) "退出沉浸" else "沉浸模式",
            onClick = onToggleImmersiveMode
        )
    }
}

@Composable
private fun ActiveTimerCard(
    uiState: PomodoroUiState,
    session: PomodoroSessionEntity,
    clockStyle: String,
    isImmersiveMode: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = if (isImmersiveMode) 22.dp else 28.dp),
            verticalArrangement = Arrangement.spacedBy(if (isImmersiveMode) 14.dp else 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimerClockDisplay(
                timeText = formatDuration(uiState.remainingSeconds),
                clockStyle = clockStyle,
                compact = false
            )
            SegmentedProgressBar(progress = uiState.progress)
            PhaseInfo(uiState = uiState, compact = isImmersiveMode)
            ActiveTaskInfo(uiState = uiState, session = session)
        }
    }
}

@Composable
private fun ActiveTimerWideCard(
    uiState: PomodoroUiState,
    session: PomodoroSessionEntity,
    clockStyle: String,
    isImmersiveMode: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = if (isImmersiveMode) 16.dp else 20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1.1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimerClockDisplay(
                    timeText = formatDuration(uiState.remainingSeconds),
                    clockStyle = clockStyle,
                    compact = true
                )
                SegmentedProgressBar(progress = uiState.progress)
            }
            Column(
                modifier = Modifier.weight(0.9f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PhaseInfo(uiState = uiState, compact = false)
                ActiveTaskInfo(uiState = uiState, session = session)
            }
        }
    }
}

@Composable
private fun PhaseInfo(uiState: PomodoroUiState, compact: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = uiState.statusLabel,
            style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
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
}

@Composable
private fun ImmersiveTaskLine(
    uiState: PomodoroUiState,
    session: PomodoroSessionEntity
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = uiState.taskTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
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

@Composable
private fun ActiveTaskInfo(
    uiState: PomodoroUiState,
    session: PomodoroSessionEntity
) {
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

@Composable
private fun ActiveControlPanel(
    uiState: PomodoroUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCompleteCurrentSession: () -> Unit,
    onSkipBreak: () -> Unit,
    onCancel: () -> Unit
) {
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

@Composable
private fun TimerClockDisplay(
    timeText: String,
    clockStyle: String,
    compact: Boolean,
    prominent: Boolean = false
) {
    if (clockStyle == CLOCK_STYLE_FLIP) {
        FlipClockDisplay(timeText = timeText, compact = compact, prominent = prominent)
    } else {
        Text(
            text = timeText,
            fontSize = when {
                prominent -> 96.sp
                compact -> 58.sp
                else -> 78.sp
            },
            lineHeight = when {
                prominent -> 98.sp
                compact -> 60.sp
                else -> 80.sp
            },
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FlipClockDisplay(timeText: String, compact: Boolean, prominent: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(
            when {
                prominent -> 7.dp
                compact -> 3.dp
                else -> 5.dp
            }
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        timeText.forEach { char ->
            if (char == ':') {
                Text(
                    text = ":",
                    fontSize = when {
                        prominent -> 64.sp
                        compact -> 44.sp
                        else -> 54.sp
                    },
                    lineHeight = when {
                        prominent -> 66.sp
                        compact -> 46.sp
                        else -> 56.sp
                    },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlipDigitCard(value = char.toString(), compact = compact, prominent = prominent)
            }
        }
    }
}

@Composable
private fun FlipDigitCard(value: String, compact: Boolean, prominent: Boolean) {
    val width = when {
        prominent -> 58.dp
        compact -> 38.dp
        else -> 46.dp
    }
    val corner = when {
        prominent -> 22.dp
        compact -> 14.dp
        else -> 18.dp
    }
    Surface(
        modifier = Modifier.width(width),
        shape = RoundedCornerShape(corner),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = if (prominent) 12.dp else if (compact) 7.dp else 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    (slideInVertically { height -> -height } + fadeIn()) togetherWith
                        (slideOutVertically { height -> height } + fadeOut()) using
                        SizeTransform(clip = false)
                },
                label = "flip-digit"
            ) { digit ->
                Text(
                    text = digit,
                    fontSize = when {
                        prominent -> 54.sp
                        compact -> 34.sp
                        else -> 42.sp
                    },
                    lineHeight = when {
                        prominent -> 56.sp
                        compact -> 36.sp
                        else -> 44.sp
                    },
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.22f)
            ) {}
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
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            text = "${duration}分",
            style = MaterialTheme.typography.labelMedium,
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
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SmallActionChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DurationWheelDialog(
    title: String,
    description: String,
    initialDurationMinutes: Int,
    wheelDurations: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var draftDuration by remember { mutableIntStateOf(initialDurationMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    text = description,
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
