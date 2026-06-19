package com.kdlay.meaotodo.ui.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
            DisplayModeSwitcher(
                displayMode = displayMode,
                calendarMode = calendarMode,
                onDisplayModeChange = { displayModeName = it.name },
                onCalendarModeChange = { calendarModeName = it.name }
            )
            QuickAddBar(onClick = { showAddTaskDialog = true })
            if (displayMode == TodoDisplayMode.List) {
                TodoTaskList(
                    modifier = Modifier.weight(1f),
                    groups = groups,
                    selectedList = selectedList,
                    onCheckedChange = viewModel::setDone,
                    onEdit = { editingTask = it },
                    onRemove = viewModel::removeTask
                )
            } else {
                TodoCalendarContent(
                    modifier = Modifier.weight(1f),
                    groups = groups,
                    selectedList = selectedList,
                    calendarMode = calendarMode,
                    selectedDate = selectedDate,
                    onSelectedDateChange = { selectedDate = startOfDay(it) },
                    onCalendarModeChange = { calendarModeName = it.name },
                    onCheckedChange = viewModel::setDone,
                    onEdit = { editingTask = it },
                    onRemove = viewModel::removeTask
                )
            }
        }
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
