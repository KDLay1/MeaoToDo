package com.kdlay.meaotodo.ui.todo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdlay.meaotodo.data.local.entity.TaskEntity

@Composable
fun TodoScreen(
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier,
    onStartFocus: (TaskEntity) -> Unit = {}
) {
    val tasks by viewModel.tasks.collectAsState()
    val customLists by viewModel.taskLists.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedListId by rememberSaveable { mutableStateOf(SMART_ALL) }
    var displayModeName by rememberSaveable { mutableStateOf(TodoDisplayMode.List.name) }
    var calendarModeName by rememberSaveable { mutableStateOf(TodoCalendarMode.Week.name) }
    var statusFilterName by rememberSaveable { mutableStateOf(TodoStatusFilter.All.name) }
    var priorityFilterName by rememberSaveable { mutableStateOf(TodoPriorityFilter.All.name) }
    var sortModeName by rememberSaveable { mutableStateOf(TodoSortMode.Time.name) }
    var selectedDate by rememberSaveable { mutableLongStateOf(startOfDay(System.currentTimeMillis())) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddListDialog by remember { mutableStateOf(false) }
    var showListPickerDialog by remember { mutableStateOf(false) }
    var showFilterSortSheet by remember { mutableStateOf(false) }
    var quickAddTitle by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val displayMode = remember(displayModeName) { TodoDisplayMode.valueOf(displayModeName) }
    val calendarMode = remember(calendarModeName) { TodoCalendarMode.valueOf(calendarModeName) }
    val filterState = remember(statusFilterName, priorityFilterName, sortModeName) {
        TodoFilterState(
            status = TodoStatusFilter.valueOf(statusFilterName),
            priority = TodoPriorityFilter.valueOf(priorityFilterName),
            sortMode = TodoSortMode.valueOf(sortModeName)
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    val groups = remember(tasks) { buildTodoGroups(tasks) }
    val filteredGroups = remember(groups, filterState) { applyTodoFilterAndSort(groups, filterState) }
    val visibleGroups = remember(filteredGroups, searchQuery) { applyTodoSearch(filteredGroups, searchQuery) }
    val listOptions = remember(groups, customLists) { buildListOptions(groups, customLists) }

    LaunchedEffect(listOptions, selectedListId) {
        if (listOptions.none { it.id == selectedListId }) selectedListId = SMART_ALL
    }

    val selectedList = listOptions.firstOrNull { it.id == selectedListId } ?: listOptions.first()
    val selectedTasks = visibleGroups.tasksFor(selectedList.id)

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
                    groups = visibleGroups,
                    selectedList = selectedList,
                    selectedTasks = selectedTasks,
                    listOptions = listOptions,
                    displayMode = displayMode,
                    calendarMode = calendarMode,
                    filterState = filterState,
                    searchActive = searchActive,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onToggleSearch = {
                        searchActive = !searchActive
                        if (!searchActive) searchQuery = ""
                    },
                    onClearSearch = {
                        searchQuery = ""
                        searchActive = false
                    },
                    onPickList = { showListPickerDialog = true },
                    onOpenFilterSort = { showFilterSortSheet = true },
                    onDisplayModeChange = { displayModeName = it.name },
                    onCalendarModeChange = { calendarModeName = it.name },
                    onAddTask = { showAddTaskDialog = true },
                    quickAddTitle = quickAddTitle,
                    onQuickAddTitleChange = { quickAddTitle = it },
                    onQuickAddSubmit = {
                        val parsed = parseSmartQuickAdd(
                            input = quickAddTitle,
                            listOptions = listOptions,
                            fallbackListId = defaultTaskListIdFor(selectedList.id),
                            fallbackDueAt = defaultDueAtFor(selectedList.id, displayMode, selectedDate)
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
                    onCheckedChange = viewModel::setDone,
                    onEdit = { editingTask = it },
                    onRemove = viewModel::removeTask,
                    onStartFocus = onStartFocus,
                    onDuplicate = viewModel::duplicateTask,
                    onMove = viewModel::moveTask,
                    onArchive = viewModel::archiveTask,
                    onPinToday = viewModel::pinToday,
                    modifier = Modifier.weight(1f)
                )
            } else {
                TodoCalendarModeScreen(
                    groups = visibleGroups,
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
                    onStartFocus = onStartFocus,
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
            onAddList = {
                showListPickerDialog = false
                showAddListDialog = true
            },
            onRenameList = viewModel::renameTaskList,
            onRemoveList = { listId ->
                if (selectedListId == listId) selectedListId = SMART_ALL
                viewModel.removeTaskList(listId)
            },
            onDismiss = { showListPickerDialog = false }
        )
    }

    if (showFilterSortSheet) {
        TodoFilterSortSheet(
            currentState = filterState,
            onApply = { nextState ->
                statusFilterName = nextState.status.name
                priorityFilterName = nextState.priority.name
                sortModeName = nextState.sortMode.name
                showFilterSortSheet = false
            },
            onDismiss = { showFilterSortSheet = false }
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
    listOptions: List<TodoListOption>,
    displayMode: TodoDisplayMode,
    calendarMode: TodoCalendarMode,
    filterState: TodoFilterState,
    searchActive: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onPickList: () -> Unit,
    onOpenFilterSort: () -> Unit,
    onDisplayModeChange: (TodoDisplayMode) -> Unit,
    onCalendarModeChange: (TodoCalendarMode) -> Unit,
    onAddTask: () -> Unit,
    quickAddTitle: String,
    onQuickAddTitleChange: (String) -> Unit,
    onQuickAddSubmit: () -> Unit,
    onCheckedChange: (TaskEntity, Boolean) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onRemove: (TaskEntity) -> Unit,
    onStartFocus: (TaskEntity) -> Unit,
    onDuplicate: (TaskEntity) -> Unit,
    onMove: (TaskEntity, String) -> Unit,
    onArchive: (TaskEntity) -> Unit,
    onPinToday: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactTodoHeader(
            tasks = tasks,
            selectedList = selectedList,
            selectedTasks = selectedTasks,
            filterState = filterState,
            searchActive = searchActive,
            searchQuery = searchQuery,
            onSearchChange = onSearchChange,
            onToggleSearch = onToggleSearch,
            onClearSearch = onClearSearch,
            onPickList = onPickList,
            onOpenFilterSort = onOpenFilterSort
        )
        QuickAddBar(
            title = quickAddTitle,
            onTitleChange = onQuickAddTitleChange,
            onSubmit = onQuickAddSubmit,
            onOpenFullEditor = onAddTask
        )
        DisplayModeSwitcher(
            displayMode = displayMode,
            calendarMode = calendarMode,
            onDisplayModeChange = onDisplayModeChange,
            onCalendarModeChange = onCalendarModeChange
        )
        TodoTaskList(
            modifier = Modifier.weight(1f),
            groups = groups,
            selectedList = selectedList,
            listOptions = listOptions,
            onCheckedChange = onCheckedChange,
            onEdit = onEdit,
            onRemove = onRemove,
            onStartFocus = onStartFocus,
            onDuplicate = onDuplicate,
            onMove = onMove,
            onArchive = onArchive,
            onPinToday = onPinToday
        )
    }
}

@Composable
private fun CompactTodoHeader(
    tasks: List<TaskEntity>,
    selectedList: TodoListOption,
    selectedTasks: List<TaskEntity>,
    filterState: TodoFilterState,
    searchActive: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onPickList: () -> Unit,
    onOpenFilterSort: () -> Unit
) {
    val pendingCount = tasks.count { !it.isDone }
    val todayCount = tasks.count { !it.isDone && it.dueAt?.let(::isToday) == true }
    val completedCount = tasks.count { it.isDone }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = "MeaoToDo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.clickable(onClick = onPickList),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedList.label,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = "⌄", fontSize = 30.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.clickable(onClick = onToggleSearch),
                    text = if (searchActive) "×" else "⌕",
                    fontSize = 34.sp,
                    color = if (searchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                )
                Surface(
                    modifier = Modifier.clickable(onClick = onOpenFilterSort),
                    shape = RoundedCornerShape(999.dp),
                    color = if (filterState.isDefault) Color.Transparent else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 3.dp),
                        text = "⋮",
                        fontSize = 30.sp,
                        color = if (filterState.isDefault) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        if (searchActive) {
            TodoSearchBar(
                query = searchQuery,
                onQueryChange = onSearchChange,
                onClear = onClearSearch
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            SummaryCount(label = "待办", count = pendingCount, color = MaterialTheme.colorScheme.primary)
            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
            SummaryCount(label = "今天", count = todayCount, color = MaterialTheme.colorScheme.primary)
            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
            SummaryCount(label = "完成", count = completedCount, color = MaterialTheme.colorScheme.tertiary)
            if (!filterState.isDefault || searchQuery.isNotBlank()) {
                Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                SummaryCount(label = "当前", count = selectedTasks.size, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun TodoSearchBar(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit) {
    val focusManager = LocalFocusManager.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⌕", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            BasicTextField(
                modifier = Modifier.weight(1f),
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (query.isBlank()) Text("搜索标题或备注", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        innerTextField()
                    }
                }
            )
            if (query.isNotBlank()) {
                Text(
                    modifier = Modifier.clickable(onClick = onClear),
                    text = "清除",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SummaryCount(label: String, count: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            fontWeight = FontWeight.Bold
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
    onStartFocus: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CalendarTopBar(selectedList = selectedList, onPickList = onPickList, onAddTask = onAddTask)
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
            onRemove = onRemove,
            onStartFocus = onStartFocus
        )
    }
}

@Composable
private fun CalendarTopBar(selectedList: TodoListOption, onPickList: () -> Unit, onAddTask: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactCalendarListButton(selectedList = selectedList, onClick = onPickList)
        FilledTonalButton(onClick = onAddTask) { Text("＋") }
    }
}

@Composable
private fun CompactCalendarListButton(selectedList: TodoListOption, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("日历", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(selectedList.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text("▼", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun applyTodoSearch(groups: TodoGroups, query: String): TodoGroups {
    val cleanQuery = query.trim()
    if (cleanQuery.isBlank()) return groups
    return buildTodoGroups(
        groups.all.filter { task ->
            task.title.contains(cleanQuery, ignoreCase = true) ||
                task.note.contains(cleanQuery, ignoreCase = true)
        }
    )
}
