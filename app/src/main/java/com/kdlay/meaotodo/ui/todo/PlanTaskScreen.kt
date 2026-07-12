package com.kdlay.meaotodo.ui.todo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.ui.components.MeaoPageHeader

@Composable
fun PlanTaskScreen(
    viewModel: TodoViewModel,
    onStartFocus: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    val customLists by viewModel.taskLists.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedListId by rememberSaveable { mutableStateOf(SMART_ALL) }
    var quickAddTitle by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var statusFilterName by rememberSaveable { mutableStateOf(TodoStatusFilter.All.name) }
    var priorityFilterName by rememberSaveable { mutableStateOf(TodoPriorityFilter.All.name) }
    var sortModeName by rememberSaveable { mutableStateOf(TodoSortMode.Time.name) }
    var showListPicker by remember { mutableStateOf(false) }
    var showFilterSort by remember { mutableStateOf(false) }
    var showAddList by remember { mutableStateOf(false) }
    var showAddTask by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val filterState = remember(statusFilterName, priorityFilterName, sortModeName) {
        TodoFilterState(
            status = TodoStatusFilter.valueOf(statusFilterName),
            priority = TodoPriorityFilter.valueOf(priorityFilterName),
            sortMode = TodoSortMode.valueOf(sortModeName)
        )
    }
    val allGroups = remember(tasks) { buildTodoGroups(tasks) }
    val listOptions = remember(allGroups, customLists) { buildListOptions(allGroups, customLists) }
    val selectedList = listOptions.firstOrNull { it.id == selectedListId } ?: listOptions.first()
    val filteredGroups = remember(allGroups, filterState) { applyTodoFilterAndSort(allGroups, filterState) }
    val searchedTasks = remember(filteredGroups, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) filteredGroups.all else filteredGroups.all.filter { task ->
            task.title.contains(query, ignoreCase = true) || task.note.contains(query, ignoreCase = true)
        }
    }
    val visibleGroups = remember(searchedTasks) { buildTodoGroups(searchedTasks) }
    val selectedTasks = visibleGroups.tasksFor(selectedList.id)

    LaunchedEffect(listOptions, selectedListId) {
        if (listOptions.none { it.id == selectedListId }) selectedListId = SMART_ALL
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
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MeaoPageHeader(
                title = selectedList.label,
                subtitle = buildTaskSummary(tasks, selectedTasks, searchQuery, filterState),
                action = {
                    Text(
                        modifier = Modifier.clickable { showListPicker = true }.padding(8.dp),
                        text = "清单",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
            QuickAddBar(
                title = quickAddTitle,
                onTitleChange = { quickAddTitle = it },
                onSubmit = {
                    val parsed = parseSmartQuickAdd(
                        input = quickAddTitle,
                        listOptions = listOptions,
                        fallbackListId = defaultTaskListIdFor(selectedList.id),
                        fallbackDueAt = defaultDueAtFor(selectedList.id)
                    )
                    if (parsed != null) {
                        viewModel.addTask(
                            listId = parsed.listId,
                            title = parsed.title,
                            note = "",
                            priority = parsed.priority,
                            dueAt = parsed.dueAt,
                            hasDueTime = false,
                            estimatedPomodoros = parsed.estimatedPomodoros
                        )
                        quickAddTitle = ""
                    }
                },
                onOpenFullEditor = { showAddTask = true }
            )
            TaskSearchField(
                value = searchQuery,
                filterActive = !filterState.isDefault,
                onValueChange = { searchQuery = it },
                onClear = { searchQuery = "" },
                onFilter = { showFilterSort = true }
            )
            ListSwitcher(
                listOptions = listOptions,
                selectedListId = selectedList.id,
                onSelect = { selectedListId = it },
                onAddList = { showAddList = true }
            )
            TodoTaskList(
                modifier = Modifier.weight(1f),
                groups = visibleGroups,
                selectedList = selectedList,
                listOptions = listOptions,
                onCheckedChange = viewModel::setDone,
                onEdit = { editingTask = it },
                onRemove = viewModel::removeTask,
                onStartFocus = onStartFocus,
                onDuplicate = viewModel::duplicateTask,
                onMove = viewModel::moveTask,
                onArchive = viewModel::archiveTask,
                onPinToday = viewModel::pinToday
            )
        }
    }

    if (showListPicker) {
        TodoListPickerDialog(
            listOptions = listOptions,
            selectedListId = selectedList.id,
            onSelect = { selectedListId = it },
            onAddList = {
                showListPicker = false
                showAddList = true
            },
            onRenameList = viewModel::renameTaskList,
            onRemoveList = { listId ->
                if (selectedListId == listId) selectedListId = SMART_ALL
                viewModel.removeTaskList(listId)
            },
            onDismiss = { showListPicker = false }
        )
    }

    if (showFilterSort) {
        TodoFilterSortSheet(
            currentState = filterState,
            onApply = { nextState ->
                statusFilterName = nextState.status.name
                priorityFilterName = nextState.priority.name
                sortModeName = nextState.sortMode.name
                showFilterSort = false
            },
            onDismiss = { showFilterSort = false }
        )
    }

    if (showAddList) {
        AddListDialog(
            onDismiss = { showAddList = false },
            onAdd = { name ->
                viewModel.addTaskList(name)
                showAddList = false
            }
        )
    }

    if (showAddTask) {
        TaskEditorDialog(
            title = "新建任务",
            task = null,
            listOptions = listOptions.taskListOptions(),
            initialListId = defaultTaskListIdFor(selectedList.id),
            initialDueAt = defaultDueAtFor(selectedList.id),
            initialHasDueTime = false,
            onDismiss = { showAddTask = false },
            onSave = { listId, title, note, priority, dueAt, hasDueTime, estimatedPomodoros ->
                viewModel.addTask(listId, title, note, priority, dueAt, hasDueTime, estimatedPomodoros)
                showAddTask = false
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
            onSave = { listId, title, note, priority, dueAt, hasDueTime, estimatedPomodoros ->
                viewModel.updateTask(task, listId, title, note, priority, dueAt, hasDueTime, estimatedPomodoros)
                editingTask = null
            }
        )
    }
}

@Composable
private fun TaskSearchField(
    value: String,
    filterActive: Boolean,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onFilter: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("搜索", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            BasicTextField(
                modifier = Modifier.weight(1f),
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                decorationBox = { inner ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (value.isBlank()) {
                            Text(
                                "任务标题或备注",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        inner()
                    }
                }
            )
            if (value.isNotBlank()) {
                Text(
                    modifier = Modifier.clickable(onClick = onClear),
                    text = "清除",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                modifier = Modifier.clickable(onClick = onFilter),
                shape = RoundedCornerShape(999.dp),
                color = if (filterActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    text = if (filterActive) "已筛选" else "筛选",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (filterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun buildTaskSummary(
    tasks: List<TaskEntity>,
    selectedTasks: List<TaskEntity>,
    searchQuery: String,
    filterState: TodoFilterState
): String {
    val pending = tasks.count { !it.isDone }
    val today = tasks.count { !it.isDone && it.dueAt?.let(::isToday) == true }
    val narrowed = searchQuery.isNotBlank() || !filterState.isDefault
    val suffix = if (narrowed) " · 当前显示 ${selectedTasks.size} 项" else ""
    return "待办 $pending 项 · 今天 $today 项$suffix"
}
