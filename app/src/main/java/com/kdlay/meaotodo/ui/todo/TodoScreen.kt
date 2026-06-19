package com.kdlay.meaotodo.ui.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.data.local.entity.DEFAULT_TASK_LIST_ID
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.data.local.entity.TaskListEntity
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

private const val SMART_ALL = "smart_all"
private const val SMART_TODAY = "smart_today"
private const val SMART_UPCOMING = "smart_upcoming"
private const val SMART_COMPLETED = "smart_completed"

private data class TodoListOption(
    val id: String,
    val label: String,
    val count: Int,
    val isSmart: Boolean = false
)

@Composable
fun TodoScreen(
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    val customLists by viewModel.taskLists.collectAsState()
    var selectedListId by rememberSaveable { mutableStateOf(SMART_ALL) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddListDialog by remember { mutableStateOf(false) }

    val groups = remember(tasks) { buildTodoGroups(tasks) }
    val listOptions = remember(groups, customLists) { buildListOptions(groups, customLists) }
    val selectedList = listOptions.firstOrNull { it.id == selectedListId } ?: listOptions.first()

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTaskDialog = true }) {
                Text("＋", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TodoHeader(tasks = tasks, selectedList = selectedList)
            ListSwitcher(
                listOptions = listOptions,
                selectedListId = selectedList.id,
                onSelect = { selectedListId = it },
                onAddList = { showAddListDialog = true }
            )
            TodoTaskList(
                groups = groups,
                selectedList = selectedList,
                onCheckedChange = viewModel::setDone,
                onEdit = { editingTask = it },
                onRemove = viewModel::removeTask
            )
        }
    }

    if (showAddTaskDialog) {
        val initialListId = defaultTaskListIdFor(selectedList.id)
        TaskEditorDialog(
            title = "新建任务",
            task = null,
            listOptions = listOptions.taskListOptions(),
            initialListId = initialListId,
            initialDueAt = defaultDueAtFor(selectedList.id),
            onDismiss = { showAddTaskDialog = false },
            onSave = { listId, taskTitle, note, priority, dueAt, estimatedPomodoros ->
                viewModel.addTask(listId, taskTitle, note, priority, dueAt, estimatedPomodoros)
                showAddTaskDialog = false
            }
        )
    }

    if (showAddListDialog) {
        AddListDialog(
            onDismiss = { showAddListDialog = false },
            onAdd = { name ->
                viewModel.addTaskList(name)
                showAddListDialog = false
            }
        )
    }

    editingTask?.let { task ->
        TaskEditorDialog(
            title = "编辑任务",
            task = task,
            listOptions = listOptions.taskListOptions(),
            initialListId = task.listId,
            initialDueAt = task.dueAt,
            onDismiss = { editingTask = null },
            onSave = { listId, taskTitle, note, priority, dueAt, estimatedPomodoros ->
                viewModel.updateTask(task, listId, taskTitle, note, priority, dueAt, estimatedPomodoros)
                editingTask = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListSwitcher(
    listOptions: List<TodoListOption>,
    selectedListId: String,
    onSelect: (String) -> Unit,
    onAddList: () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOptions.forEach { option ->
            val text = "${option.label} ${option.count}"
            if (selectedListId == option.id) {
                Button(onClick = { onSelect(option.id) }) { Text(text) }
            } else {
                OutlinedButton(onClick = { onSelect(option.id) }) { Text(text) }
            }
        }
        OutlinedButton(onClick = onAddList) { Text("新建清单") }
    }
}

@Composable
private fun TodoTaskList(
    groups: TodoGroups,
    selectedList: TodoListOption,
    onCheckedChange: (TaskEntity, Boolean) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onRemove: (TaskEntity) -> Unit
) {
    val selectedTasks = groups.tasksFor(selectedList.id)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (selectedTasks.isEmpty()) {
            item(key = "empty-${selectedList.id}") {
                EmptyTodoCard(selectedList = selectedList)
            }
        } else if (selectedList.id == SMART_ALL) {
            taskSection("已过期", groups.overdue, onCheckedChange, onEdit, onRemove)
            taskSection("今天", groups.today, onCheckedChange, onEdit, onRemove)
            taskSection("收集箱", groups.inbox, onCheckedChange, onEdit, onRemove)
            taskSection("未来", groups.upcoming, onCheckedChange, onEdit, onRemove)
            taskSection("已完成", groups.completed, onCheckedChange, onEdit, onRemove)
        } else {
            taskSection(selectedList.label, selectedTasks, onCheckedChange, onEdit, onRemove)
        }
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
private fun TodoHeader(tasks: List<TaskEntity>, selectedList: TodoListOption) {
    val pendingCount = tasks.count { !it.isDone }
    val completedCount = tasks.count { it.isDone }
    val todayCount = tasks.count { !it.isDone && it.dueAt?.let(::isToday) == true }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = selectedList.label,
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
private fun EmptyTodoCard(selectedList: TodoListOption) {
    val message = when (selectedList.id) {
        SMART_ALL -> "点击右下角 ＋ 添加第一条任务。"
        DEFAULT_TASK_LIST_ID -> "没有未安排到自定义清单的任务。"
        SMART_TODAY -> "今天没有待办。可以在这里添加今天要做的事。"
        SMART_UPCOMING -> "没有未来任务。给任务设置后续截止日期后会显示在这里。"
        SMART_COMPLETED -> "还没有完成任务。"
        else -> "这个清单还没有任务。点击右下角 ＋ 添加。"
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("${selectedList.label}为空", style = MaterialTheme.typography.titleMedium)
            Text(message)
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

@Composable
private fun AddListDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建清单") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("清单名称") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name) },
                enabled = name.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TaskEditorDialog(
    title: String,
    task: TaskEntity?,
    listOptions: List<TodoListOption>,
    initialListId: String,
    initialDueAt: Long?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, Long?, Int) -> Unit
) {
    val scrollState = rememberScrollState()
    var taskTitle by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var note by remember(task?.id) { mutableStateOf(task?.note.orEmpty()) }
    var listId by remember(task?.id, initialListId) { mutableStateOf(initialListId) }
    var priority by remember(task?.id) { mutableIntStateOf(task?.priority ?: 0) }
    var dueAt by remember(task?.id, initialDueAt) { mutableStateOf(task?.dueAt ?: initialDueAt) }
    var estimatedPomodoros by remember(task?.id) { mutableIntStateOf(task?.estimatedPomodoros ?: 0) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                TextButton(onClick = { showMoreOptions = !showMoreOptions }) {
                    Text(if (showMoreOptions) "收起更多选项" else "更多选项")
                }

                if (showMoreOptions) {
                    Text("所属清单", style = MaterialTheme.typography.labelLarge)
                    StringChoiceButtonGroup(
                        options = listOptions,
                        selectedId = listId,
                        onSelect = { listId = it }
                    )

                    Text("截止日期", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text(dueAt?.let { "截止 ${formatDate(it)}" } ?: "添加截止日期")
                    }
                    if (dueAt != null) {
                        TextButton(onClick = { dueAt = null }) {
                            Text("清除截止日期")
                        }
                    }

                    Text("优先级", style = MaterialTheme.typography.labelLarge)
                    IntChoiceButtonGroup(
                        values = listOf(0, 1, 2, 3),
                        selectedValue = priority,
                        label = ::priorityLabel,
                        onSelect = { priority = it }
                    )

                    Text("预计番茄", style = MaterialTheme.typography.labelLarge)
                    IntChoiceButtonGroup(
                        values = listOf(0, 1, 2, 3, 4),
                        selectedValue = estimatedPomodoros,
                        label = { it.toString() },
                        onSelect = { estimatedPomodoros = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(listId, taskTitle, note, priority, dueAt, estimatedPomodoros) },
                enabled = taskTitle.isNotBlank()
            ) {
                Text(if (task == null) "添加" else "保存")
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

private data class TodoGroups(
    val all: List<TaskEntity>,
    val overdue: List<TaskEntity>,
    val today: List<TaskEntity>,
    val inbox: List<TaskEntity>,
    val upcoming: List<TaskEntity>,
    val completed: List<TaskEntity>,
    val byList: Map<String, List<TaskEntity>>
) {
    fun tasksFor(listId: String): List<TaskEntity> = when (listId) {
        SMART_ALL -> all
        SMART_TODAY -> overdue + today
        SMART_UPCOMING -> upcoming
        SMART_COMPLETED -> completed
        DEFAULT_TASK_LIST_ID -> inbox
        else -> byList[listId].orEmpty()
    }
}

private fun buildTodoGroups(tasks: List<TaskEntity>): TodoGroups {
    val pendingTasks = tasks.filterNot { it.isDone }
    val completedTasks = tasks.filter { it.isDone }
    val overdueTasks = pendingTasks.filter { it.dueAt?.let(::isOverdue) == true }
    val todayTasks = pendingTasks.filter { it.dueAt?.let(::isToday) == true }
    val inboxTasks = pendingTasks.filter { it.listId == DEFAULT_TASK_LIST_ID }
    val upcomingTasks = pendingTasks.filter { it.dueAt?.let { dueAt -> !isOverdue(dueAt) && !isToday(dueAt) } == true }

    return TodoGroups(
        all = tasks,
        overdue = overdueTasks,
        today = todayTasks,
        inbox = inboxTasks,
        upcoming = upcomingTasks,
        completed = completedTasks,
        byList = tasks.groupBy { it.listId }
    )
}

private fun buildListOptions(groups: TodoGroups, customLists: List<TaskListEntity>): List<TodoListOption> =
    buildList {
        add(TodoListOption(SMART_ALL, "全部", groups.tasksFor(SMART_ALL).size, isSmart = true))
        add(TodoListOption(DEFAULT_TASK_LIST_ID, "收集箱", groups.tasksFor(DEFAULT_TASK_LIST_ID).size))
        customLists.forEach { taskList ->
            add(TodoListOption(taskList.id, taskList.name, groups.tasksFor(taskList.id).size))
        }
        add(TodoListOption(SMART_TODAY, "今天", groups.tasksFor(SMART_TODAY).size, isSmart = true))
        add(TodoListOption(SMART_UPCOMING, "未来", groups.tasksFor(SMART_UPCOMING).size, isSmart = true))
        add(TodoListOption(SMART_COMPLETED, "已完成", groups.tasksFor(SMART_COMPLETED).size, isSmart = true))
    }

private fun List<TodoListOption>.taskListOptions(): List<TodoListOption> =
    filterNot { it.isSmart }

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

private fun defaultTaskListIdFor(selectedListId: String): String = when (selectedListId) {
    SMART_ALL, SMART_TODAY, SMART_UPCOMING, SMART_COMPLETED -> DEFAULT_TASK_LIST_ID
    else -> selectedListId
}

private fun defaultDueAtFor(selectedListId: String): Long? = when (selectedListId) {
    SMART_TODAY -> todayRange().first
    else -> null
}

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
