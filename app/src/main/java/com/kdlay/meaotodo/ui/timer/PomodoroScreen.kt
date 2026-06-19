package com.kdlay.meaotodo.ui.timer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.ui.components.WheelPickerColumn

@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedDurationMinutes by rememberSaveable { mutableIntStateOf(25) }
    var selectedTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var showTaskPicker by remember { mutableStateOf(false) }

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
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PomodoroHeader(uiState = uiState)

            if (uiState.activeSession == null) {
                IdlePomodoroPanel(
                    tasks = uiState.tasks,
                    selectedTaskId = selectedTaskId,
                    selectedDurationMinutes = selectedDurationMinutes,
                    onDurationChange = { selectedDurationMinutes = it },
                    onPickTask = { showTaskPicker = true },
                    onClearTask = { selectedTaskId = null },
                    onStart = { viewModel.start(selectedTaskId, selectedDurationMinutes) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                ActivePomodoroPanel(
                    uiState = uiState,
                    onPause = viewModel::pause,
                    onResume = viewModel::resume,
                    onFinish = viewModel::finish,
                    onCancel = viewModel::cancel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showTaskPicker) {
        TaskPickerDialog(
            tasks = uiState.tasks.filter { !it.isDone },
            selectedTaskId = selectedTaskId,
            onSelect = { selectedTaskId = it },
            onClear = { selectedTaskId = null },
            onDismiss = { showTaskPicker = false }
        )
    }
}

@Composable
private fun PomodoroHeader(uiState: PomodoroUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("番茄钟", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = if (uiState.activeSession == null) "选择时长和任务，也可以空白开始" else uiState.taskTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    text = when {
                        uiState.isRunning -> "专注中"
                        uiState.isPaused -> "暂停"
                        else -> "Ready"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdlePomodoroPanel(
    tasks: List<TaskEntity>,
    selectedTaskId: String?,
    selectedDurationMinutes: Int,
    onDurationChange: (Int) -> Unit,
    onPickTask: () -> Unit,
    onClearTask: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId }
    val quickDurations = listOf(15, 25, 45, 60, 90)
    val wheelDurations = listOf(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 75, 90, 120)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("当前任务", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = selectedTask?.title ?: "空白专注",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = onPickTask) {
                            Text(if (selectedTask == null) "选择任务" else "更换任务")
                        }
                        if (selectedTask != null) {
                            TextButton(onClick = onClearTask) { Text("清除绑定") }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("专注时长", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "$selectedDurationMinutes min",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center.spacedBy(8.dp),
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
                    WheelPickerColumn(
                        title = "分钟",
                        values = wheelDurations,
                        selectedValue = selectedDurationMinutes,
                        onCenteredValueChange = onDurationChange,
                        itemLabel = { it.toString() },
                        columnWidth = 112.dp,
                        itemWidth = 86.dp
                    )
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onStart
        ) {
            Text("开始专注", modifier = Modifier.padding(vertical = 6.dp), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActivePomodoroPanel(
    uiState: PomodoroUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session = uiState.activeSession ?: return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 30.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatDuration(uiState.remainingSeconds),
                    fontSize = 76.sp,
                    lineHeight = 78.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (uiState.isPaused) "暂停中" else "专注中",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
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
                            text = "计划 ${session.plannedDurationSeconds / 60} min · 已专注 ${formatDuration(uiState.elapsedSeconds)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

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
                Button(modifier = Modifier.weight(1f), onClick = onFinish) { Text("完成") }
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onCancel) { Text("放弃本次") }
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
            text = "$duration",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
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
                    text = "也可以清除绑定，直接开始空白番茄钟。",
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
            if (selected) {
                Text("已选", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val remainingSeconds = safeSeconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}
