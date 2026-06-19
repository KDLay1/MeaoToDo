package com.kdlay.meaotodo.ui.todo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.data.local.entity.TaskEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TaskEditorDialog(
    title: String,
    task: TaskEntity?,
    listOptions: List<TodoListOption>,
    initialListId: String,
    initialDueAt: Long?,
    initialHasDueTime: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, Long?, Boolean, Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val isNewTask = task == null
    var taskTitle by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var note by remember(task?.id) { mutableStateOf(task?.note.orEmpty()) }
    var listId by remember(task?.id, initialListId) { mutableStateOf(initialListId) }
    var priority by remember(task?.id) { mutableIntStateOf(task?.priority ?: 0) }
    var dueAt by remember(task?.id, initialDueAt) { mutableStateOf(task?.dueAt ?: initialDueAt) }
    var hasDueTime by remember(task?.id, initialHasDueTime) { mutableStateOf((task?.hasDueTime ?: initialHasDueTime) && dueAt != null) }
    var dueHour by remember(task?.id, initialDueAt) { mutableIntStateOf(dueAt?.let(::hourOf) ?: 9) }
    var dueMinute by remember(task?.id, initialDueAt) { mutableIntStateOf(dueAt?.let(::minuteOf) ?: 0) }
    var estimatedPomodoros by remember(task?.id) { mutableIntStateOf(task?.estimatedPomodoros ?: 0) }
    var showMoreOptions by remember { mutableStateOf(!isNewTask) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimeWheel by remember { mutableStateOf(false) }

    fun updateDueTime(hour: Int = dueHour, minute: Int = dueMinute, enabled: Boolean = hasDueTime) {
        val currentDueAt = dueAt ?: return
        dueHour = hour.coerceIn(0, 23)
        dueMinute = minute.coerceIn(0, 59)
        hasDueTime = enabled
        dueAt = if (enabled) combineDateAndTime(currentDueAt, dueHour, dueMinute) else startOfDay(currentDueAt)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    text = if (isNewTask) "先快速记下来，需要时再补细节。" else "调整任务内容、清单和计划。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.32f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SmallBadge(text = selectedListLabel(listOptions, listId))
                            dueAt?.let { SmallBadge(text = "截止 ${formatDueAtLabel(it, hasDueTime)}") }
                            if (priority > 0) SmallBadge(text = priorityLabel(priority))
                        }
                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("任务标题") },
                            placeholder = { Text("比如：复习统计物理第四章") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("备注，可选") },
                            placeholder = { Text("补充地点、材料或提醒") },
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                }

                if (!showMoreOptions) {
                    QuickSettingChips(
                        listLabel = selectedListLabel(listOptions, listId),
                        dueLabel = dueAt?.let { formatDueAtLabel(it, hasDueTime) } ?: "添加日期",
                        priorityLabel = if (priority > 0) priorityLabel(priority) else "普通",
                        onListClick = { showMoreOptions = true },
                        onDateClick = { showDatePicker = true },
                        onPriorityClick = { showMoreOptions = true }
                    )
                }

                FilledTonalButton(onClick = { showMoreOptions = !showMoreOptions }) {
                    Text(if (showMoreOptions) "收起设置" else "展开全部设置")
                }

                if (showMoreOptions) {
                    OptionSection(title = "所属清单") {
                        StringChoiceButtonGroup(
                            options = listOptions,
                            selectedId = listId,
                            onSelect = { listId = it }
                        )
                    }

                    OptionSection(title = "截止日期") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showDatePicker = true }) {
                                Text(dueAt?.let { "截止 ${formatDate(it)}" } ?: "添加截止日期")
                            }
                            if (dueAt != null) {
                                TextButton(onClick = {
                                    dueAt = null
                                    hasDueTime = false
                                }) {
                                    Text("清除")
                                }
                            }
                        }
                    }

                    if (dueAt != null) {
                        OptionSection(title = "截止时间") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (hasDueTime) {
                                    FilledTonalButton(onClick = { showTimeWheel = true }) {
                                        Text("%02d:%02d".format(dueHour, dueMinute))
                                    }
                                    TextButton(onClick = { updateDueTime(enabled = false) }) {
                                        Text("清除时间")
                                    }
                                } else {
                                    OutlinedButton(onClick = { showTimeWheel = true }) {
                                        Text("设置截止时间")
                                    }
                                }
                            }
                        }
                    }

                    OptionSection(title = "优先级") {
                        IntChoiceButtonGroup(
                            values = listOf(0, 1, 2, 3),
                            selectedValue = priority,
                            label = ::priorityLabel,
                            onSelect = { priority = it }
                        )
                    }

                    OptionSection(title = "预计番茄") {
                        IntChoiceButtonGroup(
                            values = listOf(0, 1, 2, 3, 4),
                            selectedValue = estimatedPomodoros,
                            label = { if (it == 0) "无" else "$it 个" },
                            onSelect = { estimatedPomodoros = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = { onSave(listId, taskTitle, note, priority, dueAt, hasDueTime && dueAt != null, estimatedPomodoros) },
                enabled = taskTitle.isNotBlank()
            ) {
                Text(if (isNewTask) "添加任务" else "保存修改")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis
                        dueAt = selected?.let {
                            if (hasDueTime) combineDateAndTime(it, dueHour, dueMinute) else startOfDay(it)
                        }
                        if (dueAt == null) hasDueTime = false
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimeWheel) {
        TimeWheelDialog(
            initialHour = dueHour,
            initialMinute = dueMinute,
            onDismiss = { showTimeWheel = false },
            onConfirm = { hour, minute ->
                updateDueTime(hour = hour, minute = minute, enabled = true)
                showTimeWheel = false
            }
        )
    }
}

@Composable
private fun OptionSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickSettingChips(
    listLabel: String,
    dueLabel: String,
    priorityLabel: String,
    onListClick: () -> Unit,
    onDateClick: () -> Unit,
    onPriorityClick: () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onListClick) { Text("清单 · $listLabel") }
        OutlinedButton(onClick = onDateClick) { Text("日期 · $dueLabel") }
        OutlinedButton(onClick = onPriorityClick) { Text("优先级 · $priorityLabel") }
    }
}

@Composable
private fun SmallBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntChoiceButtonGroup(
    values: List<Int>,
    selectedValue: Int,
    label: (Int) -> String,
    onSelect: (Int) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEach { value ->
            if (selectedValue == value) {
                FilledTonalButton(onClick = { onSelect(value) }) { Text(label(value)) }
            } else {
                OutlinedButton(onClick = { onSelect(value) }) { Text(label(value)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StringChoiceButtonGroup(
    options: List<TodoListOption>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            if (selectedId == option.id) {
                FilledTonalButton(onClick = { onSelect(option.id) }) { Text(option.label) }
            } else {
                OutlinedButton(onClick = { onSelect(option.id) }) { Text(option.label) }
            }
        }
    }
}

private fun selectedListLabel(options: List<TodoListOption>, selectedId: String): String =
    options.firstOrNull { it.id == selectedId }?.label ?: "收集箱"

private fun formatDueAtLabel(timestamp: Long, hasDueTime: Boolean): String =
    if (hasDueTime) "${formatDate(timestamp)} ${formatTime(timestamp)}" else formatDate(timestamp)
