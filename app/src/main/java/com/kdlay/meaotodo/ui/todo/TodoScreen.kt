package com.kdlay.meaotodo.ui.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.collect

@Composable
fun TodoScreen(
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    val customLists by viewModel.taskLists.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedListId by rememberSaveable { mutableStateOf(SMART_ALL) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddListDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val groups = remember(tasks) { buildTodoGroups(tasks) }
    val listOptions = remember(groups, customLists) { buildListOptions(groups, customLists) }
    val selectedList = listOptions.firstOrNull { it.id == selectedListId } ?: listOptions.first()
    val selectedTasks = groups.tasksFor(selectedList.id)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TodoHeader(
                tasks = tasks,
                selectedList = selectedList,
                selectedTasks = selectedTasks
            )
            ListSwitcher(
                listOptions = listOptions,
                selectedListId = selectedList.id,
                onSelect = { selectedListId = it },
                onAddList = { showAddListDialog = true }
            )
            QuickAddBar(onClick = { showAddTaskDialog = true })
            TodoTaskList(
                modifier = Modifier.weight(1f),
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
