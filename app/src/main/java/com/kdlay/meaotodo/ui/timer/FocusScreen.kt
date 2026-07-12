package com.kdlay.meaotodo.ui.timer

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdlay.meaotodo.core.settings.PomodoroPreferences
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.data.repository.PomodoroRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FocusScreen(
    viewModel: PomodoroViewModel,
    modifier: Modifier = Modifier,
    requestedStartTaskId: String? = null,
    onRequestedStartTaskHandled: () -> Unit = {},
    onImmersiveModeChange: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var immersive by rememberSaveable { mutableStateOf(false) }
    var showTimerSetup by rememberSaveable { mutableStateOf(false) }
    var showTaskPicker by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showCancelConfirm by rememberSaveable { mutableStateOf(false) }

    val selectedTask = uiState.tasks.firstOrNull { it.id == selectedTaskId }
        ?: uiState.tasks.sortedWith(focusTaskComparator()).firstOrNull()
    val hasActiveTimer = uiState.activeSession != null
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(uiState.tasks, selectedTaskId) {
        if (selectedTaskId == null || uiState.tasks.none { it.id == selectedTaskId }) {
            selectedTaskId = uiState.tasks.sortedWith(focusTaskComparator()).firstOrNull()?.id
        }
    }

    LaunchedEffect(requestedStartTaskId, uiState.tasks, hasActiveTimer) {
        val taskId = requestedStartTaskId ?: return@LaunchedEffect
        val task = uiState.tasks.firstOrNull { it.id == taskId }
        if (task != null) {
            selectedTaskId = task.id
            if (!hasActiveTimer) {
                viewModel.start(
                    taskId = task.id,
                    durationMinutes = uiState.preferences.focusDurationMinutes,
                    breakDurationMinutes = uiState.preferences.breakDurationMinutes,
                    targetFocusCount = uiState.preferences.targetFocusCount
                )
            }
            onRequestedStartTaskHandled()
        } else if (uiState.tasks.isNotEmpty()) {
            onRequestedStartTaskHandled()
        }
    }

    LaunchedEffect(hasActiveTimer) {
        if (!hasActiveTimer) immersive = false
    }

    LaunchedEffect(hasActiveTimer, immersive) {
        onImmersiveModeChange(hasActiveTimer && immersive)
    }

    DisposableEffect(Unit) {
        onDispose { onImmersiveModeChange(false) }
    }

    if (hasActiveTimer && immersive) {
        ImmersiveFocusClock(
            timeText = formatFocusDuration(uiState.remainingSeconds),
            isLandscape = isLandscape,
            onExit = { immersive = false },
            modifier = modifier
        )
        return
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FocusHeader(
                    active = hasActiveTimer,
                    status = uiState.statusLabel,
                    onOpenSettings = { showSettings = true }
                )
            }
            item {
                FocusTimerHero(
                    uiState = uiState,
                    selectedTask = selectedTask,
                    onStart = {
                        viewModel.start(
                            taskId = selectedTask?.id,
                            durationMinutes = uiState.preferences.focusDurationMinutes,
                            breakDurationMinutes = uiState.preferences.breakDurationMinutes,
                            targetFocusCount = uiState.preferences.targetFocusCount
                        )
                    },
                    onPause = viewModel::pause,
                    onResume = viewModel::resume,
                    onComplete = viewModel::completeCurrentSession,
                    onSkipBreak = viewModel::skipBreak,
                    onPickTask = { showTaskPicker = true },
                    onAdjustTimer = { showTimerSetup = true },
                    onImmersive = { immersive = true },
                    onCancel = { showCancelConfirm = true }
                )
            }
            item { TodayFocusSummary(uiState.summary, onOpenHistory = { showHistory = true }) }
            item {
                FocusQueueCard(
                    tasks = uiState.tasks,
                    selectedTaskId = selectedTask?.id,
                    timerActive = hasActiveTimer,
                    onSelect = { selectedTaskId = it.id },
                    onStart = { task ->
                        selectedTaskId = task.id
                        viewModel.start(
                            taskId = task.id,
                            durationMinutes = uiState.preferences.focusDurationMinutes,
                            breakDurationMinutes = uiState.preferences.breakDurationMinutes,
                            targetFocusCount = uiState.preferences.targetFocusCount
                        )
                    },
                    onOpenAll = { showTaskPicker = true }
                )
            }
            item {
                FocusPlanCard(
                    focusMinutes = uiState.preferences.focusDurationMinutes,
                    breakMinutes = uiState.preferences.breakDurationMinutes,
                    rounds = uiState.preferences.targetFocusCount,
                    notificationsEnabled = uiState.preferences.notificationsEnabled,
                    onEdit = { showTimerSetup = true },
                    onOpenSettings = { showSettings = true }
                )
            }
        }
    }

    if (showTimerSetup) {
        FocusTimerSetupSheet(
            preferences = uiState.preferences,
            activeLocked = hasActiveTimer,
            onDismiss = { showTimerSetup = false },
            onApply = { focus, rest, rounds ->
                viewModel.updateFocusDurationMinutes(focus)
                viewModel.updateBreakDurationMinutes(rest)
                viewModel.updateTargetFocusCount(rounds)
                showTimerSetup = false
            }
        )
    }

    if (showTaskPicker) {
        FocusTaskPickerSheet(
            tasks = uiState.tasks,
            selectedTaskId = selectedTask?.id,
            timerActive = hasActiveTimer,
            onSelect = { selectedTaskId = it.id },
            onStart = { task ->
                selectedTaskId = task.id
                viewModel.start(
                    taskId = task.id,
                    durationMinutes = uiState.preferences.focusDurationMinutes,
                    breakDurationMinutes = uiState.preferences.breakDurationMinutes,
                    targetFocusCount = uiState.preferences.targetFocusCount
                )
                showTaskPicker = false
            },
            onQuickAdd = viewModel::addQuickFocusTask,
            onDismiss = { showTaskPicker = false }
        )
    }

    if (showHistory) {
        FocusHistorySheet(
            summary = uiState.summary,
            sessions = uiState.recentSessions,
            onDismiss = { showHistory = false }
        )
    }

    if (showSettings) {
        FocusSettingsSheet(
            preferences = uiState.preferences,
            onToggleClockStyle = viewModel::toggleClockStyle,
            onEditTimer = {
                showSettings = false
                showTimerSetup = true
            },
            onDismiss = { showSettings = false }
        )
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("结束整个专注计划？") },
            text = { Text("当前阶段会记为放弃，后续轮次也不会继续。已经完成的专注记录会保留。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancel()
                        showCancelConfirm = false
                    }
                ) { Text("结束计划", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showCancelConfirm = false }) { Text("继续专注") } }
        )
    }
}

@Composable
private fun FocusHeader(active: Boolean, status: String, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("专注", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                if (active) status else "选择一件事，然后只做这一件事",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "专注设置")
        }
    }
}

@Composable
private fun FocusTimerHero(
    uiState: PomodoroUiState,
    selectedTask: TaskEntity?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onComplete: () -> Unit,
    onSkipBreak: () -> Unit,
    onPickTask: () -> Unit,
    onAdjustTimer: () -> Unit,
    onImmersive: () -> Unit,
    onCancel: () -> Unit
) {
    val active = uiState.activeSession != null
    val timeText = if (active) {
        formatFocusDuration(uiState.remainingSeconds)
    } else {
        "%02d:00".format(uiState.preferences.focusDurationMinutes)
    }
    val taskTitle = if (active) uiState.taskTitle else selectedTask?.title ?: "空白专注"
    val status = if (active) uiState.statusLabel else "准备开始"
    val supporting = when {
        active && uiState.isPaused -> "已暂停 ${formatFocusDuration(uiState.pausedSeconds)}"
        active -> uiState.nextLabel
        else -> "${uiState.preferences.focusDurationMinutes} 分钟专注 · ${uiState.preferences.breakDurationMinutes} 分钟休息"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            FocusProgressDial(
                progress = if (active) uiState.progress else 0f,
                status = status,
                timeText = timeText,
                taskTitle = taskTitle,
                supporting = supporting,
                flipStyle = uiState.preferences.clockStyle == PomodoroPreferences.CLOCK_STYLE_FLIP
            )

            if (active) {
                FocusRoundIndicator(
                    total = uiState.activeRun?.targetFocusCount ?: 1,
                    completed = uiState.activeRun?.completedFocusCount ?: 0,
                    currentRound = uiState.activeSession?.roundIndex ?: 1,
                    isBreak = uiState.isBreak
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f).height(52.dp),
                        onClick = if (uiState.isPaused) onResume else onPause
                    ) {
                        Text(if (uiState.isPaused) "继续" else "暂停", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f).height(52.dp),
                        onClick = if (uiState.isBreak) onSkipBreak else onComplete
                    ) {
                        Text(if (uiState.isBreak) "跳过休息" else "完成本轮")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onImmersive) { Text("沉浸计时") }
                    TextButton(onClick = onCancel) {
                        Text("结束计划", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onPickTask),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("本轮任务", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(taskTitle, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("更换", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    onClick = onStart
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("开始专注", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onAdjustTimer) {
                    Text("调整时长与轮数")
                }
            }
        }
    }
}

@Composable
private fun FocusProgressDial(
    progress: Float,
    status: String,
    timeText: String,
    taskTitle: String,
    supporting: String,
    flipStyle: Boolean
) {
    val trackColor = MaterialTheme.colorScheme.primaryContainer
    val progressColor = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.size(286.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            if (progress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = stroke
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(status, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            FocusClock(timeText = timeText, flipStyle = flipStyle)
            Text(taskTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun FocusClock(timeText: String, flipStyle: Boolean) {
    if (!flipStyle) {
        Text(
            text = timeText,
            fontSize = 58.sp,
            lineHeight = 62.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp
        )
        return
    }

    val parts = timeText.split(":")
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        FlipClockBlock(parts.getOrElse(0) { "00" })
        Text(":", fontSize = 42.sp, fontWeight = FontWeight.Bold)
        FlipClockBlock(parts.getOrElse(1) { "00" })
    }
}

@Composable
private fun FlipClockBlock(value: String) {
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.onSurface,
        contentColor = MaterialTheme.colorScheme.surface
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            text = value,
            fontSize = 42.sp,
            lineHeight = 46.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FocusRoundIndicator(total: Int, completed: Int, currentRound: Int, isBreak: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(total.coerceIn(1, 12)) { index ->
                val round = index + 1
                val color = when {
                    round <= completed -> MaterialTheme.colorScheme.primary
                    round == currentRound -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.outlineVariant
                }
                Surface(modifier = Modifier.size(if (round == currentRound) 12.dp else 9.dp), shape = CircleShape, color = color) {}
            }
        }
        Text(
            if (isBreak) "第 $currentRound / $total 轮 · 休息" else "第 $currentRound / $total 轮 · 专注",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TodayFocusSummary(summary: PomodoroSummary, onOpenHistory: () -> Unit) {
    FocusSection(title = "今日进度", action = "查看记录", onAction = onOpenHistory) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FocusMetric(Modifier.weight(1f), summary.finishedFocusCount.toString(), "完成番茄")
            FocusMetric(Modifier.weight(1f), "${summary.focusSeconds / 60}", "专注分钟")
            FocusMetric(Modifier.weight(1f), summary.finishedBreakCount.toString(), "完成休息")
        }
    }
}

@Composable
internal fun FocusMetric(modifier: Modifier, value: String, label: String) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FocusQueueCard(
    tasks: List<TaskEntity>,
    selectedTaskId: String?,
    timerActive: Boolean,
    onSelect: (TaskEntity) -> Unit,
    onStart: (TaskEntity) -> Unit,
    onOpenAll: () -> Unit
) {
    val visible = remember(tasks) { tasks.sortedWith(focusTaskComparator()).take(4) }
    FocusSection(title = "专注队列", action = "全部任务", onAction = onOpenAll) {
        if (visible.isEmpty()) {
            Text(
                "还没有可专注任务。可以从这里快速创建，或直接进行空白专注。",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visible.forEach { task ->
                    FocusTaskRow(
                        task = task,
                        selected = task.id == selectedTaskId,
                        timerActive = timerActive,
                        onSelect = { onSelect(task) },
                        onStart = { onStart(task) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun FocusTaskRow(
    task: TaskEntity,
    selected: Boolean,
    timerActive: Boolean,
    onSelect: () -> Unit,
    onStart: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(10.dp),
                shape = CircleShape,
                color = focusPriorityColor(task.priority)
            ) {}
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(task.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    focusTaskMeta(task),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Text("当前", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            } else if (!timerActive) {
                TextButton(onClick = onStart, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("开始")
                }
            }
        }
    }
}

@Composable
private fun FocusPlanCard(
    focusMinutes: Int,
    breakMinutes: Int,
    rounds: Int,
    notificationsEnabled: Boolean,
    onEdit: () -> Unit,
    onOpenSettings: () -> Unit
) {
    FocusSection(title = "专注方案", action = "设置", onAction = onOpenSettings) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("$focusMinutes / $breakMinutes 分钟 · $rounds 轮", fontWeight = FontWeight.Bold)
                    Text(
                        if (notificationsEnabled) "阶段结束时发送通知" else "阶段通知未开启",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("修改", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FocusSection(
    title: String,
    action: String,
    onAction: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    modifier = Modifier.clickable(onClick = onAction),
                    text = action,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

@Composable
private fun ImmersiveFocusClock(
    timeText: String,
    isLandscape: Boolean,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color(0xFF080A0C)).clickable(onClick = onExit),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = timeText,
            color = Color(0xFFF3F1EC),
            fontSize = if (isLandscape) 138.sp else 92.sp,
            lineHeight = if (isLandscape) 144.sp else 98.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp
        )
    }
}

internal fun focusTaskComparator(): Comparator<TaskEntity> =
    compareByDescending<TaskEntity> { isTaskDueToday(it) }
        .thenByDescending { it.priority }
        .thenBy { it.dueAt ?: Long.MAX_VALUE }
        .thenByDescending { it.estimatedPomodoros }
        .thenBy { it.createdAt }

private fun isTaskDueToday(task: TaskEntity): Boolean {
    val dueAt = task.dueAt ?: return false
    val due = Calendar.getInstance().apply { timeInMillis = dueAt }
    val now = Calendar.getInstance()
    return due.get(Calendar.YEAR) == now.get(Calendar.YEAR) && due.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
}

private fun focusTaskMeta(task: TaskEntity): String {
    val pieces = mutableListOf<String>()
    if (isTaskDueToday(task)) pieces += "今天"
    else if (task.dueAt != null) pieces += SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(task.dueAt))
    pieces += when (task.priority) {
        3 -> "高优先级"
        2 -> "中优先级"
        1 -> "低优先级"
        else -> "普通"
    }
    if (task.estimatedPomodoros > 0) pieces += "预计 ${task.estimatedPomodoros} 番茄"
    return pieces.joinToString(" · ")
}

@Composable
private fun focusPriorityColor(priority: Int): Color = when (priority) {
    3 -> MaterialTheme.colorScheme.error
    2 -> MaterialTheme.colorScheme.secondary
    1 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.outline
}

internal fun focusSessionStatus(status: String): String = when (status) {
    PomodoroRepository.STATUS_FINISHED -> "已完成"
    PomodoroRepository.STATUS_CANCELLED -> "已放弃"
    PomodoroRepository.STATUS_PAUSED -> "已暂停"
    PomodoroRepository.STATUS_RUNNING -> "进行中"
    else -> status
}

internal fun formatSessionTime(timestamp: Long): String =
    SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun formatFocusDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}
