package com.kdlay.meaotodo.ui.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.data.local.entity.TaskEntity

@Composable
fun TodoScreen(
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    val customLists by viewModel.taskLists.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedListId by rememberSaveable { mutableStateOf(SMART_ALL) }
    var displayModeName by rememberSaveable { mutableStateOf(TodoDisplayMode.List.name) }
    var calendarModeName by rememberSaveable { mutableStateOf(TodoCalendarMode.Week.name) }
    var selectedDate by rememberSaveable { mutableLongStateOf(startOfDay(System.currentTimeMillis())) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddListDialog by remember { mutableStateOf(false) }
    var showListPickerDialog by remember { mutableStateOf(false) }

    val displayMode = remember(displayModeName) { TodoDisplayMode.valueOf(displayModeName) }
    val calendarMode = remember(calendarModeName) { TodoCalendarMode.valueOf(calendarModeName) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val groups = remember(tasks) { buildTodoGroups(tasks) }
    val listOptions = remember(groups, customLists) { buildListOptions(groups, customLists) }

    LaunchedEffect(listOptions, selectedListId) {
        if (listOptions.none { it.id == selectedListId }) {
            selectedListId = SMART_ALL
        }
    }

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
            if (displayMode == TodoDisplayMode.List) {
                TodoListModeScreen(
                    tasks = tasks,
                    groups = groups,
                    selectedList = selectedList,
                    selectedTasks = selectedTasks,
                    displayMode = displayMode,
                    calendarMode = calendarMode,
                    onPickList = { showListPickerDialog = true },
                    onDisplayModeChange = { displayModeName = it.name },
                    onCalendarModeChange = { calendarModeName = it.name },
                    onAddTask = { showAddTaskDialog = true },
                    onCheckedChange = viewModel::setDone,
                    onEdit = { editingTask = it },
                    onRemove = viewModel::removeTask,
                    modifier = Modifier.weight(1f)
                )
            } else {
                TodoCalendarModeScreen(
                    groups = groups,
                    selectedList = selectedList,
                    displayMode = displayMode,
                    calendarMode = calendarMode,
                    selectedDate = selectedDate,
                    onPickList = { showListPickerDialog = true },
                    onAddTask = { showAddTaskDialog = true },
                    onDisplayModeChange = { displayModeName = it.name },
                    onCalendarModeChange = { calendarModeName = it.name },
                    onSelectedDateChange = { selectedDate = startOfDay(it) },
                    onCheckedChange = viewModel::setDone,
                    onEdit = { editingTask = it },
                    onRemove = viewModel::removeTask,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showListPickerDialog) {
        TodoListPickerDialog(
            listOptions = listOptions,
            selectedListId = selectedList.id,
            onSelect = { selectedListId = it },
            onAddList = { showAddListDialog = true },
            onDismiss = { showListPickerDialog = false }
        )
    }

    if (showAddTaskDialog) {
        val initialListId = defaultTaskListIdFor(selectedList.id)
        TaskEditorDialog(
            title = "新建任务",
            task = null,
            listOptions = listOptions.taskListOptions(),
            initialListId = initialListId,
            initialDueAt = defaultDueAtFor(selectedList.id, displayMode, selectedDate),
            initialHasDueTime = false,
            onDismiss = { showAddTaskDialog = false },
            onSave = { listId, taskTitle, note, priority, dueAt, hasDueTime, estimatedPomodoros ->
                viewModel.addTask(listId, taskTitle, note, priority, dueAt, hasDueTime, estimatedPomodoros)
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
            initialHasDueTime = task.hasDueTime,
            onDismiss = { editingTask = null },
            onSave = { listId, taskTitle, note, priority, dueAt, hasDueTime, estimatedPomodoros ->
                viewModel.updateTask(task, listId, taskTitle, note, priority, dueAt, hasDueTime, estimatedPomodoros)
                editingTask = null
            }
        )
    }
}

@Composable
private fun TodoListModeScreen(
    tasks: List<TaskEntity>,
    groups: TodoGroups,
    selectedList: TodoListOption,
    selectedTasks: List<TaskEntity>,
    displayMode: TodoDisplayMode,
    calendarMode: TodoCalendarMode,
    onPickList: () -> Unit,
    onDisplayModeChange: (TodoDisplayMode) -> Unit,
    onCalendarModeChange: (TodoCalendarMode) -> Unit,
    onAddTask: () -> Unit,
    onCheckedChange: (TaskEntity, Boolean) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onRemove: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TodoHeader(
            tasks = tasks,
            selectedList = selectedList,
            selectedTasks = selectedTasks
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TodoListPickerButton(selectedList = selectedList, onClick = onPickList)
        }
        DisplayModeSwitcher(
            displayMode = displayMode,
            calendarMode = calendarMode,
            onDisplayModeChange = onDisplayModeChange,
            onCalendarModeChange = onCalendarModeChange
        )
        QuickAddBar(onClick = onAddTask)
        TodoTaskList(
            modifier = Modifier.weight(1f),
            groups = groups,
            selectedList = selectedList,
            onCheckedChange = onCheckedChange,
            onEdit = onEdit,
            onRemove = onRemove
        )
    }
}

@Composable
private fun TodoCalendarModeScreen(
    groups: TodoGroups,
    selectedList: TodoListOption,
    displayMode: TodoDisplayMode,
    calendarMode: TodoCalendarMode,
    selectedDate: Long,
    onPickList: () -> Unit,
    onAddTask: () -> Unit,
    onDisplayModeChange: (TodoDisplayMode) -> Unit,
    onCalendarModeChange: (TodoCalendarMode) -> Unit,
    onSelectedDateChange: (Long) -> Unit,
    onCheckedChange: (TaskEntity, Boolean) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onRemove: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CalendarTopBar(
            selectedList = selectedList,
            onPickList = onPickList,
            onAddTask = onAddTask
        )
        DisplayModeSwitcher(
            displayMode = displayMode,
            calendarMode = calendarMode,
            onDisplayModeChange = onDisplayModeChange,
            onCalendarModeChange = onCalendarModeChange
        )
        TodoCalendarContent(
            modifier = Modifier.weight(1f),
            groups = groups,
            selectedList = selectedList,
            calendarMode = calendarMode,
            selectedDate = selectedDate,
            onSelectedDateChange = onSelectedDateChange,
            onCalendarModeChange = onCalendarModeChange,
            onCheckedChange = onCheckedChange,
            onEdit = onEdit,
            onRemove = onRemove
        )
    }
}

@Composable
private fun CalendarTopBar(
    selectedList: TodoListOption,
    onPickList: () -> Unit,
    onAddTask: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text("日历", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${selectedList.label} · 按时间查看", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            TodoListPickerButton(selectedList = selectedList, onClick = onPickList)
            FilledTonalButton(onClick = onAddTask) { Text("＋") }
        }
    }
}
