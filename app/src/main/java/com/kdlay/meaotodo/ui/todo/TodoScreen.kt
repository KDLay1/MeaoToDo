package com.kdlay.meaotodo.ui.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun TodoScreen(
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val pendingTasks = tasks.filterNot { it.isDone }
    val completedTasks = tasks.filter { it.isDone }
    val overdueTasks = pendingTasks.filter { it.dueAt?.let(::isOverdue) == true }
    val todayTasks = pendingTasks.filter { it.dueAt?.let(::isToday) == true }
    val inboxTasks = pendingTasks.filter { it.dueAt == null }
    val upcomingTasks = pendingTasks.filter { it.dueAt?.let { dueAt -> !isOverdue(dueAt) && !isToday(dueAt) } == true }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("＋", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TodoHeader(tasks = tasks)

            if (tasks.isEmpty()) {
                EmptyTodoCard()
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    taskSection(
                        title = "已过期",
                        tasks = overdueTasks,
                        onCheckedChange = viewModel::setDone,
                        onEdit = { editingTask = it },
                        onRemove = viewModel::removeTask
                    )
                    taskSection(
                        title = "今天",
                        tasks = todayTasks,
                        onCheckedChange = viewModel::setDone,
                        onEdit = { editingTask = it },
                        onRemove = viewModel::removeTask
                    )
                    taskSection(
                        title = "收集箱",
                        tasks = inboxTasks,
                        onCheckedChange = viewModel::setDone,
                        onEdit = { editingTask = it },
                        onRemove = viewModel::removeTask
                    )
                    taskSection(
                        title = "未来",
                        tasks = upcomingTasks,
                        onCheckedChange = viewModel::setDone,
                        onEdit = { editingTask = it },
                        onRemove = viewModel::removeTask
                    )
                    taskSection(
                        title = "已完成",
                        tasks = completedTasks,
                        onCheckedChange = viewModel::setDone,
                        onEdit = { editingTask = it },
                        onRemove = viewModel::removeTask
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        TaskEditorDialog(
            title = "新建任务",
            task = null,
            onDismiss = { showAddDialog = false },
            onSave = { taskTitle, note, priority, dueAt, estimatedPomodoros ->
                viewModel.addTask(taskTitle, note, priority, dueAt, estimatedPomodoros)
                showAddDialog = false
            }
        )
    }

    editingTask?.let { task ->
        TaskEditorDialog(
            title = "编辑任务",
            task = task,
            onDismiss = { editingTask = null },
            onSave = { taskTitle, note, priority, dueAt, estimatedPomodoros ->
                viewModel.updateTask(task, taskTitle, note, priority, dueAt, estimatedPomodoros)
                editingTask = null
            }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.taskSection(
    title: String,
    tasks: List<TaskEntity>,
    onCheckedChange: (TaskEntity, Boolean) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onRemove: (TaskEntity) -> Unit
) {
    if (tasks.isEmpty()) return
    item(key = "section-$title") {
        Text(
            text = "$title · ${tasks.size}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
    items(tasks, key = { it.id }) { task ->
        TaskRow(
            task = task,
            onCheckedChange = { isDone -> onCheckedChange(task, isDone) },
            onEdit = { onEdit(task) },
            onRemove = { onRemove(task) }
        )
    }
}

@Composable
private fun TodoHeader(tasks: List<TaskEntity>) {
    val pendingCount = tasks.count { !it.isDone }
    val completedCount = tasks.count { it.isDone }
    val todayCount = tasks.count { !it.isDone && it.dueAt?.let(::isToday) == true }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "今日任务",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "待办 $pendingCount 项 · 今日 $todayCount 项 · 已完成 $completedCount 项",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyTodoCard() {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("这里还没有任务", style = MaterialTheme.typography.titleMedium)
            Text("点击右下角 ＋ 添加第一条任务。")
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    onCheckedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = onCheckedChange
            )
            Spacer(Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                )
                if (task.note.isNotBlank()) {
                    Text(task.note, style = MaterialTheme.typography.bodySmall)
                }
                val metadata = buildList {
                    if (task.priority > 0) add(priorityLabel(task.priority))
                    task.dueAt?.let { add("截止 ${formatDate(it)}") }
                    if (task.estimatedPomodoros > 0) add("番茄 ${task.estimatedPomodoros}")
                    if (task.actualPomodoros > 0) add("已专注 ${task.actualPomodoros}")
                }.joinToString(" · ")
                if (metadata.isNotBlank()) {
                    Text(
                        metadata,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEdit) { Text("编辑") }
                    TextButton(onClick = onRemove) { Text("删除") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorDialog(
    title: String,
    task: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Long?, Int) -> Unit
) {
    var taskTitle by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var note by remember(task?.id) { mutableStateOf(task?.note.orEmpty()) }
    var priority by remember(task?.id) { mutableIntStateOf(task?.priority ?: 0) }
    var dueAt by remember(task?.id) { mutableStateOf(task?.dueAt) }
    var estimatedPomodoros by remember(task?.id) { mutableIntStateOf(task?.estimatedPomodoros ?: 0) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("标题") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注") },
                    maxLines = 3
                )

                Text("优先级", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..3).forEach { value ->
                        val label = priorityLabel(value)
                        if (priority == value) {
                            Button(onClick = { priority = value }) { Text(label) }
                        } else {
                            OutlinedButton(onClick = { priority = value }) { Text(label) }
                        }
                    }
                }

                Text("预计番茄", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 1, 2, 3, 4).forEach { value ->
                        if (estimatedPomodoros == value) {
                            Button(onClick = { estimatedPomodoros = value }) { Text(value.toString()) }
                        } else {
                            OutlinedButton(onClick = { estimatedPomodoros = value }) { Text(value.toString()) }
                        }
                    }
                }

                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(dueAt?.let { "截止 ${formatDate(it)}" } ?: "添加截止日期")
                }
                if (dueAt != null) {
                    TextButton(onClick = { dueAt = null }) {
                        Text("清除截止日期")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(taskTitle, note, priority, dueAt, estimatedPomodoros) },
                enabled = taskTitle.isNotBlank()
            ) {
                Text("保存")
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
                        dueAt = datePickerState.selectedDateMillis
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
}

private fun priorityLabel(priority: Int): String = when (priority) {
    3 -> "高"
    2 -> "中"
    1 -> "低"
    else -> "普通"
}

private fun formatDate(timestamp: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))

private fun isToday(timestamp: Long): Boolean {
    val range = todayRange()
    return timestamp in range.first until range.second
}

private fun isOverdue(timestamp: Long): Boolean = timestamp < todayRange().first

private fun todayRange(): Pair<Long, Long> {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val start = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    return start to calendar.timeInMillis
}
