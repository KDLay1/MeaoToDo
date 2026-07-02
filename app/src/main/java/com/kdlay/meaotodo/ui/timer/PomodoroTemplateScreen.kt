package com.kdlay.meaotodo.ui.timer

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import com.kdlay.meaotodo.data.local.entity.TaskEntity

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
            item { PomodoroTemplateHeader(isActive = hasActiveTimer) }
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
                        onAdjustDuration = {
                            val presets = listOf(15, 25, 45, 50, 60)
                            val index = presets.indexOf(uiState.preferences.focusDurationMinutes).takeIf { it >= 0 } ?: 1
                            viewModel.updateFocusDurationMinutes(presets[(index + 1) % presets.size])
                        }
                    )
                }
            }
            item { PomodoroStatsCard(summary = uiState.summary) }
            item {
                PomodoroSettingsCard(
                    focusMinutes = uiState.preferences.focusDurationMinutes,
                    breakMinutes = uiState.preferences.breakDurationMinutes,
                    onPreset = {
                        viewModel.updateFocusDurationMinutes(25)
                        viewModel.updateBreakDurationMinutes(5)
                    },
                    onDeep = {
                        viewModel.updateFocusDurationMinutes(50)
                        viewModel.updateBreakDurationMinutes(10)
                    },
                    onImmersive = { if (hasActiveTimer) immersive = true }
                )
            }
            item {
                PomodoroTaskListCard(
                    tasks = uiState.tasks.take(4),
                    selectedTaskId = selectedTask?.id,
                    onSelect = { selectedTaskId = it.id }
                )
            }
        }
    }
}

@Composable
private fun PomodoroTemplateHeader(isActive: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MeaoToDo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⏱", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
                Text("⋮", fontSize = 28.sp, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        Text("番茄  ⌄", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(
            text = if (isActive) "正在专注，轻触沉浸模式可横屏大字显示" else "专注当下，积少成多，遇见更好的自己 ✨",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IdleTemplateTimerCard(
    selectedTask: TaskEntity?,
    durationMinutes: Int,
    targetFocusCount: Int,
    onStart: () -> Unit,
    onSwitchTask: () -> Unit,
    onAdjustDuration: () -> Unit
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
                caption = "预计 $targetFocusCount 个番茄 · ${durationMinutes}/5 标准",
                active = false
            )
            PrimaryGradientPill(text = "▶  开始专注", onClick = onStart, modifier = Modifier.fillMaxWidth(0.62f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinePill(text = "⇄  切换任务", onClick = onSwitchTask)
                OutlinePill(text = "◷  调整时长", onClick = onAdjustDuration)
            }
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
                caption = if (uiState.isPaused) "已暂停 ${formatPomodoroTemplateDuration(uiState.pausedSeconds)}" else "专注结束将提醒",
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
private fun TimerCircleShell(
    label: String,
    timeText: String,
    targetText: String,
    caption: String,
    active: Boolean
) {
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
                Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PomodoroStatsCard(summary: PomodoroSummary) {
    TemplateSectionCard(title = "今日番茄统计", leading = "▥", action = "查看详情  ›") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(modifier = Modifier.weight(1f), icon = "✓", title = "已完成", value = "${summary.finishedFocusCount} 次")
            MetricTile(modifier = Modifier.weight(1f), icon = "◷", title = "专注时长", value = "${summary.focusSeconds / 60} 分钟")
            MetricTile(modifier = Modifier.weight(1f), icon = "🔥", title = "连续专注", value = "2 天")
        }
    }
}

@Composable
private fun PomodoroSettingsCard(
    focusMinutes: Int,
    breakMinutes: Int,
    onPreset: () -> Unit,
    onDeep: () -> Unit,
    onImmersive: () -> Unit
) {
    TemplateSectionCard(title = "设置", leading = "⚙") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShortcutTile(modifier = Modifier.weight(1f), icon = "◷", title = "$focusMinutes/$breakMinutes 标准", subtitle = "专注 $focusMinutes 分钟", onClick = onPreset)
            ShortcutTile(modifier = Modifier.weight(1f), icon = "♪", title = "白噪音", subtitle = "海浪 · 中等", onClick = onDeep)
            ShortcutTile(modifier = Modifier.weight(1f), icon = "▣", title = "沉浸模式", subtitle = "黑屏大字计时", onClick = onImmersive)
        }
    }
}

@Composable
private fun PomodoroTaskListCard(tasks: List<TaskEntity>, selectedTaskId: String?, onSelect: (TaskEntity) -> Unit) {
    TemplateSectionCard(title = "今日专注任务", leading = "◎", action = "管理任务  ›") {
        if (tasks.isEmpty()) {
            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text = "还没有可专注任务，可以先回到今日页添加一件小事。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                tasks.forEach { task ->
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
        Text("⏱ 预计 ${task.estimatedPomodoros.coerceAtLeast(1)} 个番茄", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("⋮", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TemplateSectionCard(
    title: String,
    leading: String,
    action: String? = null,
    content: @Composable () -> Unit
) {
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
                action?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
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
private fun ShortcutTile(modifier: Modifier, icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
    ) {
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
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.34f))
    ) {
        Text(modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp), text = text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SoftControlPill(modifier: Modifier, text: String, onClick: () -> Unit, emphasized: Boolean = false) {
    Surface(
        modifier = modifier.height(48.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (emphasized) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ImmersiveTimerOnlyScreen(
    timeText: String,
    taskTitle: String,
    isLandscape: Boolean,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
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
