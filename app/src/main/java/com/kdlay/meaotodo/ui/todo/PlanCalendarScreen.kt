package com.kdlay.meaotodo.ui.todo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.kdlay.meaotodo.ui.components.MeaoPageHeader
import com.kdlay.meaotodo.ui.components.MeaoSegmentedControl

@Composable
fun PlanCalendarScreen(
    viewModel: TodoViewModel,
    onStartFocus: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    val customLists by viewModel.taskLists.collectAsState()
    var selectedListId by rememberSaveable { mutableStateOf(SMART_ALL) }
    var calendarModeName by rememberSaveable { mutableStateOf(TodoCalendarMode.Week.name) }
    var selectedDate by rememberSaveable { mutableLongStateOf(startOfDay(System.currentTimeMillis())) }
    var showListPicker by remember { mutableStateOf(false) }
    var showAddTask by remember { mutableStateOf(false) }
    var showAddList by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }

    val groups = remember(tasks) { buildTodoGroups(tasks) }
    val listOptions = remember(groups, customLists) { buildListOptions(groups, customLists) }
    val selectedList = listOptions.firstOrNull { it.id == selectedListId } ?: listOptions.first()
    val calendarMode = TodoCalendarMode.valueOf(calendarModeName)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MeaoPageHeader(
            title = "日历",
            subtitle = "按时间安排任务；点击日期查看当天内容",
            action = {
                Text(
                    modifier = Modifier.clickable { showAddTask = true }.padding(8.dp),
                    text = "新增",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.clickable { showListPicker = true },
                text = "${selectedList.label}  ▾",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${groups.tasksFor(selectedList.id).count { it.dueAt != null }} 项已安排",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MeaoSegmentedControl(
            options = TodoCalendarMode.entries.map { it.label },
            selectedIndex = TodoCalendarMode.entries.indexOf(calendarMode),
            onSelect = { index -> calendarModeName = TodoCalendarMode.entries[index].name }
        )
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
            onRemove = viewModel::removeTask,
            onStartFocus = onStartFocus
        )
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

    if (showAddTask) {
        TaskEditorDialog(
            title = "新建日程任务",
            task = null,
            listOptions = listOptions.taskListOptions(),
            initialListId = defaultTaskListIdFor(selectedList.id),
            initialDueAt = selectedDate,
            initialHasDueTime = true,
            onDismiss = { showAddTask = false },
            onSave = { listId, title, note, priority, dueAt, hasDueTime, estimatedPomodoros ->
                viewModel.addTask(listId, title, note, priority, dueAt, hasDueTime, estimatedPomodoros)
                showAddTask = false
            }
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
