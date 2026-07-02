package com.kdlay.meaotodo.ui.timer

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.ui.components.MeaoActionRow
import com.kdlay.meaotodo.ui.components.MeaoBottomSheet
import com.kdlay.meaotodo.ui.components.MeaoChoiceChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PomodoroTemplateScreen(
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
    var showDurationSheet by rememberSaveable { mutableStateOf(false) }
    var showStatsSheet by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showTaskSheet by rememberSaveable { mutableStateOf(false) }
    val selectedTask = uiState.tasks.firstOrNull { it.id == selectedTaskId } ?: uiState.tasks.firstOrNull()
    val hasActiveTimer = uiState.activeSession != null
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(uiState.tasks, selectedTaskId) {
        if (selectedTaskId != null && uiState.tasks.none { it.id == selectedTaskId }) {
            selectedTaskId = uiState.tasks.firstOrNull()?.id
        } else if (selectedTaskId == null && uiState.tasks.isNotEmpty()) {
            selectedTaskId = uiState.tasks.first().id
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
        ImmersiveTimerOnlyScreen(
            timeText = formatPomodoroTemplateDuration(uiState.remainingSeconds),
            taskTitle = uiState.taskTitle,
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { PomodoroTemplateHeader(isActive = hasActiveTimer, onOpenSettings = { showSettingsSheet = true }) }
            item {
                if (hasActiveTimer) {
                    ActiveTemplateTimerCard(
                        uiState = uiState,
                        onPause = viewModel::pause,
                        onResume = viewModel::resume,
                        onComplete = viewModel::completeCurrentSession,
                        onSkipBreak = viewModel::skipBreak,
                        onCancel = viewModel::cancel,
                        onEnterImmersive = { immersive = true }
                    )
                } else {
                    IdleTemplateTimerCard(
                        selectedTask = selectedTask,
                        durationMinutes = uiState.preferences.focusDurationMinutes,
                        breakMinutes = uiState.preferences.breakDurationMinutes,
                        targetFocusCount = uiState.preferences.targetFocusCount,
                        onStart = {
                            viewModel.start(
                                taskId = selectedTask?.id,
                                durationMinutes = uiState.preferences.focusDurationMinutes,
                                breakDurationMinutes = uiState.preferences.breakDurationMinutes,
                                targetFocusCount = uiState.preferences.targetFocusCount
                            )
                        },
                        onSwitchTask = {
                            val tasks = uiState.tasks
                            if (tasks.isNotEmpty()) {
                                val index = tasks.indexOfFirst { it.id == selectedTask?.id }.coerceAtLeast(0)
                                selectedTaskId = tasks[(index + 1) % tasks.size].id
                            }
                        },
                        onAdjustDuration = { showDurationSheet = true },
                        onManageTasks = { showTaskSheet = true }
                    )
                }
            }
            item { PomodoroStatsCard(summary = uiState.summary, onOpenDetail = { showStatsSheet = true }) }
            item {
                PomodoroSettingsCard(
                    focusMinutes = uiState.preferences.focusDurationMinutes,
                    breakMinutes = uiState.preferences.breakDurationMinutes,
                    rounds = uiState.preferences.targetFocusCount,
                    clockStyle = uiState.preferences.clockStyle,
                    onAdjustDuration = { showDurationSheet = true },
                    onToggleClock = viewModel::toggleClockStyle,
                    onOpenSettings = { showSettingsSheet = true },
                    onImmersive = { if (hasActiveTimer) immersive = true }
                )
            }
            item {
                PomodoroTaskListCard(
                    tasks = uiState.tasks,
                    selectedTaskId = selectedTask?.id,
                    onSelect = { selectedTaskId = it.id },
                    onManage = { showTaskSheet = true }
                )
            }
        }
    }

    if (showDurationSheet) {
        PomodoroDurationSheet(
            focusMinutes = uiState.preferences.focusDurationMinutes,
            breakMinutes = uiState.preferences.breakDurationMinutes,
            rounds = uiState.preferences.targetFocusCount,
            activeLocked = hasActiveTimer,
            onDismiss = { showDurationSheet = false },
            onApply = { focus, rest, count ->
                viewModel.updateFocusDurationMinutes(focus)
                viewModel.updateBreakDurationMinutes(rest)
                viewModel.updateTargetFocusCount(count)
                showDurationSheet = false
            }
        )
    }

    if (showStatsSheet) {
        PomodoroStatsSheet(
            summary = uiState.summary,
            recentSessions = uiState.recentSessions,
            onDismiss = { showStatsSheet = false }
        )
    }

    if (showSettingsSheet) {
        PomodoroSettingsSheet(
            clockStyle = uiState.preferences.clockStyle,
            onToggleClock = viewModel::toggleClockStyle,
            onAdjustDuration = {
                showSettingsSheet = false
                showDurationSheet = true
            },
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (showTaskSheet) {
        PomodoroTaskManagerSheet(
            tasks = uiState.tasks,
            selectedTaskId = selectedTask?.id,
            hasActiveTimer = hasActiveTimer,
            onSelect = { selectedTaskId = it.id },
            onStart = { task ->
                selectedTaskId = task.id
                viewModel.start(
                    taskId = task.id,
                    durationMinutes = uiState.preferences.focusDurationMinutes,
                    breakDurationMinutes = uiState.preferences.breakDurationMinutes,
                    targetFocusCount = uiState.preferences.targetFocusCount
                )
                showTaskSheet = false
            },
            onDismiss = { showTaskSheet = false }
        )
    }
}

@Composable
private fun PomodoroTemplateHeader(isActive: Boolean, onOpenSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MeaoToDo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⏱", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    modifier = Modifier.clickable(onClick = onOpenSettings),
                    text = "⋮",
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Text("番茄  ⌄", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(
            text = if (isActive) "正在专注，轻触沉浸模式可横屏大字显示" else "专注当下，积少成多，遇见更好的自己",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IdleTemplateTimerCard(
    selectedTask: TaskEntity?,
    durationMinutes: Int,
    breakMinutes: Int,
    targetFocusCount: Int,
    onStart: () -> Unit,
    onSwitchTask: () -> Unit,
    onAdjustDuration: () -> Unit,
    onManageTasks: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TimerCircleShell(
                label = "准备开始专注",
                timeText = "%02d:00".format(durationMinutes),
                targetText = "本轮目标：${selectedTask?.title ?: "空白专注"}",
                caption = "专注 $durationMinutes 分钟 · 休息 $breakMinutes 分钟 · $targetFocusCount 轮",
                active = false
            )
            PrimaryGradientPill(text = "▶  开始专注", onClick = onStart, modifier = Modifier.fillMaxWidth(0.62f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinePill(text = "⇄  切换任务", onClick = onSwitchTask)
                OutlinePill(text = "◷  调整时长", onClick = onAdjustDuration)
            }
            OutlinePill(text = "◎  管理专注任务", onClick = onManageTasks)
        }
    }
}

@Composable
private fun ActiveTemplateTimerCard(
    uiState: PomodoroUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onComplete: () -> Unit,
    onSkipBreak: () -> Unit,
    onCancel: () -> Unit,
    onEnterImmersive: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TimerCircleShell(
                label = "${uiState.statusLabel} · ${uiState.roundLabel}",
                timeText = formatPomodoroTemplateDuration(uiState.remainingSeconds),
                targetText = "本轮目标：${uiState.taskTitle}",
                caption = if (uiState.isPaused) "已暂停 ${formatPomodoroTemplateDuration(uiState.pausedSeconds)}" else uiState.nextLabel,
                active = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SoftControlPill(
                    modifier = Modifier.weight(1f),
                    text = if (uiState.isPaused) "▶  继续" else "Ⅱ  暂停",
                    onClick = if (uiState.isPaused) onResume else onPause,
                    emphasized = true
                )
                SoftControlPill(modifier = Modifier.weight(1f), text = "■  结束", onClick = onComplete)
                SoftControlPill(
                    modifier = Modifier.weight(1f),
                    text = if (uiState.isBreak) "▶  跳过" else "沉浸",
                    onClick = if (uiState.isBreak) onSkipBreak else onEnterImmersive
                )
            }
            Text(
                modifier = Modifier.clickable(onClick = onCancel),
                text = "放弃本轮",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TimerCircleShell(label: String, timeText: String, targetText: String, caption: String, active: Boolean) {
    Surface(
        modifier = Modifier.size(300.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(12.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (active) 0.45f else 0.14f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("⏱", fontSize = 34.sp, color = MaterialTheme.colorScheme.primary)
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Text(timeText, fontSize = 64.sp, lineHeight = 66.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)) {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        text = targetText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun PomodoroStatsCard(summary: PomodoroSummary, onOpenDetail: () -> Unit) {
    TemplateSectionCard(title = "今日番茄统计", leading = "▥", action = "查看详情  ›", onActionClick = onOpenDetail) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(modifier = Modifier.weight(1f), icon = "✓", title = "已完成", value = "${summary.finishedFocusCount} 次")
            MetricTile(modifier = Modifier.weight(1f), icon = "◷", title = "专注时长", value = "${summary.focusSeconds / 60} 分钟")
            MetricTile(modifier = Modifier.weight(1f), icon = "×", title = "放弃", value = "${summary.cancelledFocusCount} 次")
        }
    }
}

@Composable
private fun PomodoroSettingsCard(
    focusMinutes: Int,
    breakMinutes: Int,
    rounds: Int,
    clockStyle: String,
    onAdjustDuration: () -> Unit,
    onToggleClock: () -> Unit,
    onOpenSettings: () -> Unit,
    onImmersive: () -> Unit
) {
    TemplateSectionCard(title = "设置", leading = "⚙", action = "全部设置  ›", onActionClick = onOpenSettings) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShortcutTile(
                modifier = Modifier.weight(1f),
                icon = "◷",
                title = "$focusMinutes/$breakMinutes × $rounds",
                subtitle = "滚轮自定义",
                onClick = onAdjustDuration
            )
            ShortcutTile(
                modifier = Modifier.weight(1f),
                icon = "▤",
                title = if (clockStyle == "flip") "翻页钟" else "数字钟",
                subtitle = "点击切换样式",
                onClick = onToggleClock
            )
            ShortcutTile(
                modifier = Modifier.weight(1f),
                icon = "▣",
                title = "沉浸模式",
                subtitle = "黑屏大字计时",
                onClick = onImmersive
            )
        }
    }
}

@Composable
private fun PomodoroTaskListCard(
    tasks: List<TaskEntity>,
    selectedTaskId: String?,
    onSelect: (TaskEntity) -> Unit,
    onManage: () -> Unit
) {
    TemplateSectionCard(title = "今日专注任务", leading = "◎", action = "管理任务  ›", onActionClick = onManage) {
        val visibleTasks = tasks.take(5)
        if (visibleTasks.isEmpty()) {
            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text = "还没有可专注任务，可以先回到今日页添加一件小事。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                visibleTasks.forEach { task ->
                    FocusTaskLine(task = task, selected = task.id == selectedTaskId, onClick = { onSelect(task) })
                }
            }
        }
    }
}

@Composable
private fun FocusTaskLine(task: TaskEntity, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = priorityAccent(task.priority)) {}
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(task.note.ifBlank { if (selected) "当前专注目标" else "点击设为本轮目标" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(if (selected) "已选" else "选择", style = MaterialTheme.typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PomodoroDurationSheet(
    focusMinutes: Int,
    breakMinutes: Int,
    rounds: Int,
    activeLocked: Boolean,
    onDismiss: () -> Unit,
    onApply: (Int, Int, Int) -> Unit
) {
    var draftFocus by rememberSaveable(focusMinutes) { mutableIntStateOf(focusMinutes.coerceIn(1, 180)) }
    var draftBreak by rememberSaveable(breakMinutes) { mutableIntStateOf(breakMinutes.coerceIn(1, 120)) }
    var draftRounds by rememberSaveable(rounds) { mutableIntStateOf(rounds.coerceIn(1, 12)) }

    MeaoBottomSheet(
        title = "调整番茄参数",
        subtitle = if (activeLocked) "当前进行中的番茄不会被改动；新设置从下一轮开始使用。" else "用滚轮自定义专注时长、休息时长和轮数。",
        onDismiss = onDismiss,
        primaryActionText = "保存设置",
        secondaryActionText = "取消",
        onPrimaryAction = { onApply(draftFocus, draftBreak, draftRounds) },
        onSecondaryAction = onDismiss
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            NumberWheel(
                modifier = Modifier.weight(1f),
                title = "专注",
                unit = "分钟",
                values = (1..180).toList(),
                selected = draftFocus,
                onSelect = { draftFocus = it }
            )
            NumberWheel(
                modifier = Modifier.weight(1f),
                title = "休息",
                unit = "分钟",
                values = (1..120).toList(),
                selected = draftBreak,
                onSelect = { draftBreak = it }
            )
            NumberWheel(
                modifier = Modifier.weight(1f),
                title = "轮数",
                unit = "轮",
                values = (1..12).toList(),
                selected = draftRounds,
                onSelect = { draftRounds = it }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple("25/5 × 4", 25, 5),
                Triple("50/10 × 2", 50, 10),
                Triple("15/5 × 3", 15, 5)
            ).forEach { preset ->
                MeaoChoiceChip(
                    text = preset.first,
                    selected = draftFocus == preset.second && draftBreak == preset.third,
                    onClick = {
                        draftFocus = preset.second
                        draftBreak = preset.third
                        draftRounds = if (preset.second == 50) 2 else if (preset.second == 15) 3 else 4
                    }
                )
            }
        }
    }
}

@Composable
private fun NumberWheel(
    title: String,
    unit: String,
    values: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            LazyColumn(
                modifier = Modifier.height(170.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(values, key = { it }) { value ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(value) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (value == selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (value == selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ) {
                        Text(
                            modifier = Modifier.padding(vertical = 8.dp),
                            text = value.toString(),
                            textAlign = TextAlign.Center,
                            fontSize = if (value == selected) 24.sp else 18.sp,
                            fontWeight = if (value == selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PomodoroStatsSheet(summary: PomodoroSummary, recentSessions: List<PomodoroSessionEntity>, onDismiss: () -> Unit) {
    MeaoBottomSheet(title = "番茄统计", subtitle = "今日完成、放弃、休息与最近记录。", onDismiss = onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(modifier = Modifier.weight(1f), icon = "✓", title = "完成", value = "${summary.finishedFocusCount}")
            MetricTile(modifier = Modifier.weight(1f), icon = "◷", title = "分钟", value = "${summary.focusSeconds / 60}")
            MetricTile(modifier = Modifier.weight(1f), icon = "×", title = "放弃", value = "${summary.cancelledFocusCount}")
        }
        TemplateSubTitle("最近记录")
        if (recentSessions.isEmpty()) {
            Text("暂无番茄记录。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 320.dp)) {
                recentSessions.take(8).forEach { session ->
                    MeaoActionRow(
                        icon = if (session.type.contains("break")) "休" else "专",
                        title = session.titleSnapshot ?: if (session.type.contains("break")) "休息" else "空白专注",
                        subtitle = "${formatSessionTime(session.startedAt)} · ${session.status} · ${session.actualDurationSeconds / 60} 分钟",
                        onClick = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun PomodoroSettingsSheet(
    clockStyle: String,
    onToggleClock: () -> Unit,
    onAdjustDuration: () -> Unit,
    onDismiss: () -> Unit
) {
    var soundEnabled by rememberSaveable { mutableStateOf(false) }
    var strictMode by rememberSaveable { mutableStateOf(false) }

    MeaoBottomSheet(title = "番茄设置", subtitle = "先接入常用设置，复杂提醒和白噪音后续再接系统能力。", onDismiss = onDismiss) {
        MeaoActionRow(icon = "◷", title = "时间与轮数", subtitle = "滚轮自定义专注、休息和轮数", onClick = onAdjustDuration)
        MeaoActionRow(icon = "▤", title = if (clockStyle == "flip") "翻页钟样式" else "数字钟样式", subtitle = "点击切换计时器显示风格", onClick = onToggleClock)
        ToggleLikeRow(title = "白噪音", subtitle = "先保存本地开关，后续接入音频播放", checked = soundEnabled, onClick = { soundEnabled = !soundEnabled })
        ToggleLikeRow(title = "严格模式", subtitle = "先保存本地开关，后续限制退出和分心操作", checked = strictMode, onClick = { strictMode = !strictMode })
    }
}

@Composable
private fun PomodoroTaskManagerSheet(
    tasks: List<TaskEntity>,
    selectedTaskId: String?,
    hasActiveTimer: Boolean,
    onSelect: (TaskEntity) -> Unit,
    onStart: (TaskEntity) -> Unit,
    onDismiss: () -> Unit
) {
    MeaoBottomSheet(title = "专注任务管理", subtitle = "选择本轮目标，也可以直接从这里开始专注。", onDismiss = onDismiss) {
        if (tasks.isEmpty()) {
            Text("还没有可专注任务。先回到今日页添加任务。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tasks, key = { it.id }) { task ->
                    PomodoroTaskManagerRow(
                        task = task,
                        selected = task.id == selectedTaskId,
                        hasActiveTimer = hasActiveTimer,
                        onSelect = { onSelect(task) },
                        onStart = { onStart(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PomodoroTaskManagerRow(task: TaskEntity, selected: Boolean, hasActiveTimer: Boolean, onSelect: () -> Unit, onStart: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.74f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Row(modifier = Modifier.padding(13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(9.dp), shape = CircleShape, color = priorityAccent(task.priority)) {}
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(task.note.ifBlank { "预计 ${task.estimatedPomodoros.coerceAtLeast(1)} 个番茄" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!hasActiveTimer) {
                Surface(modifier = Modifier.clickable(onClick = onStart), shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Text(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), text = "开始", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            } else if (selected) {
                Text("当前", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ToggleLikeRow(title: String, subtitle: String, checked: Boolean, onClick: () -> Unit) {
    MeaoActionRow(icon = if (checked) "开" else "关", title = title, subtitle = subtitle, trailing = if (checked) "已开启" else "已关闭", onClick = onClick)
}

@Composable
private fun TemplateSectionCard(title: String, leading: String, action: String? = null, onActionClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(leading, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                action?.let {
                    Text(
                        modifier = Modifier.clickable(enabled = onActionClick != null) { onActionClick?.invoke() },
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun MetricTile(modifier: Modifier, icon: String, title: String, value: String) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(icon, color = MaterialTheme.colorScheme.primary, fontSize = 22.sp)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TemplateSubTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun ShortcutTile(modifier: Modifier, icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PrimaryGradientPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(56.dp).clickable(onClick = onClick), shape = RoundedCornerShape(999.dp), color = Color.Transparent) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color(0xFF8E8BFF), Color(0xFF657AE8))), RoundedCornerShape(999.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OutlinePill(text: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.clickable(onClick = onClick), shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.34f))) {
        Text(modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp), text = text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SoftControlPill(modifier: Modifier, text: String, onClick: () -> Unit, emphasized: Boolean = false) {
    Surface(modifier = modifier.height(48.dp).clickable(onClick = onClick), shape = RoundedCornerShape(999.dp), color = if (emphasized) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ImmersiveTimerOnlyScreen(timeText: String, taskTitle: String, isLandscape: Boolean, onExit: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onExit),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (isLandscape) 26.dp else 18.dp)) {
            Text(taskTitle, color = Color.White.copy(alpha = 0.70f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = timeText,
                color = Color.White,
                fontSize = if (isLandscape) 144.sp else 112.sp,
                lineHeight = if (isLandscape) 146.sp else 114.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text("点击屏幕退出沉浸模式", color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun priorityAccent(priority: Int): Color = when (priority) {
    3 -> MaterialTheme.colorScheme.secondary
    2 -> MaterialTheme.colorScheme.primary
    1 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.outline
}

private fun formatPomodoroTemplateDuration(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)
private fun formatSessionTime(timestamp: Long): String = SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(timestamp))
