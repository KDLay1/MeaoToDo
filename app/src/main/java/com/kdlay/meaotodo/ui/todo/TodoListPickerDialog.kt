package com.kdlay.meaotodo.ui.todo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.data.local.entity.TaskEntity

internal enum class TodoStatusFilter(val label: String) {
    All("全部"),
    Pending("待办"),
    Today("今天"),
    Completed("已完成")
}

internal enum class TodoPriorityFilter(val label: String, val level: Int?) {
    All("全部优先级", null),
    High("高", 3),
    Medium("中", 2),
    Low("低", 1)
}

internal enum class TodoSortMode(val label: String) {
    Time("时间优先"),
    Priority("优先级优先"),
    Created("创建时间"),
    Manual("手动排序")
}

internal data class TodoFilterState(
    val status: TodoStatusFilter = TodoStatusFilter.All,
    val priority: TodoPriorityFilter = TodoPriorityFilter.All,
    val sortMode: TodoSortMode = TodoSortMode.Time
) {
    val isDefault: Boolean
        get() = this == TodoFilterState()
}

internal fun applyTodoFilterAndSort(groups: TodoGroups, filterState: TodoFilterState): TodoGroups {
    val filteredTasks = groups.all
        .asSequence()
        .filter { task ->
            when (filterState.status) {
                TodoStatusFilter.All -> true
                TodoStatusFilter.Pending -> !task.isDone
                TodoStatusFilter.Today -> !task.isDone && task.dueAt?.let(::isToday) == true
                TodoStatusFilter.Completed -> task.isDone
            }
        }
        .filter { task -> filterState.priority.level?.let { task.priority == it } ?: true }
        .let { sequence -> sortTasks(sequence.toList(), filterState.sortMode) }

    return buildTodoGroups(filteredTasks)
}

private fun sortTasks(tasks: List<TaskEntity>, sortMode: TodoSortMode): List<TaskEntity> = when (sortMode) {
    TodoSortMode.Time -> tasks.sortedWith(taskTimeComparator())
    TodoSortMode.Priority -> tasks.sortedWith(
        compareByDescending<TaskEntity> { it.priority }
            .thenBy { !it.hasDueTime }
            .thenBy { it.dueAt ?: Long.MAX_VALUE }
    )
    TodoSortMode.Created -> tasks.sortedByDescending { it.createdAt }
    TodoSortMode.Manual -> tasks
}

@Composable
internal fun TodoListPickerButton(
    selectedList: TodoListOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("清单", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(selectedList.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text("▼", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodoListPickerDialog(
    listOptions: List<TodoListOption>,
    selectedListId: String,
    onSelect: (String) -> Unit,
    onAddList: () -> Unit,
    onRenameList: (String, String) -> Unit,
    onRemoveList: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var manageMode by rememberSaveable { mutableStateOf(false) }
    var renamingId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    val customOptions = listOptions.filter { it.kind == TodoListKind.CUSTOM }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BottomSheetTitle(
                title = if (manageMode) "管理任务列表" else "选择任务列表",
                subtitle = if (manageMode) "重命名或删除自定义列表；删除列表时任务会移回收集箱。" else "快速切换清单，或新建一个专门的列表。",
                onClose = onDismiss
            )
            if (manageMode) {
                if (customOptions.isEmpty()) {
                    Text("还没有自定义列表。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(customOptions, key = { it.id }) { option ->
                            ManageListRow(
                                option = option,
                                editing = renamingId == option.id,
                                renameText = renameText,
                                onRenameTextChange = { renameText = it },
                                onStartRename = {
                                    renamingId = option.id
                                    renameText = option.label
                                },
                                onCancelRename = {
                                    renamingId = null
                                    renameText = ""
                                },
                                onSaveRename = {
                                    val cleanName = renameText.trim()
                                    if (cleanName.isNotEmpty()) onRenameList(option.id, cleanName)
                                    renamingId = null
                                    renameText = ""
                                },
                                onRemove = { onRemoveList(option.id) }
                            )
                        }
                    }
                }
                Button(modifier = Modifier.fillMaxWidth().height(52.dp), onClick = { manageMode = false }) {
                    Text("完成管理", fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listSection(
                        title = "智能视图",
                        options = listOptions.filter { it.kind == TodoListKind.SMART },
                        selectedListId = selectedListId,
                        onSelect = {
                            onSelect(it)
                            onDismiss()
                        }
                    )
                    listSection(
                        title = "系统清单",
                        options = listOptions.filter { it.kind == TodoListKind.SYSTEM },
                        selectedListId = selectedListId,
                        onSelect = {
                            onSelect(it)
                            onDismiss()
                        }
                    )
                    listSection(
                        title = "我的清单",
                        options = customOptions,
                        selectedListId = selectedListId,
                        onSelect = {
                            onSelect(it)
                            onDismiss()
                        }
                    )
                }
                Button(
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    onClick = {
                        onDismiss()
                        onAddList()
                    }
                ) {
                    Text("＋ 新建列表", fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondarySheetAction(
                        modifier = Modifier.weight(1f),
                        icon = "☷",
                        title = "管理列表",
                        subtitle = "重命名 / 删除",
                        onClick = { manageMode = true }
                    )
                    SecondarySheetAction(
                        modifier = Modifier.weight(1f),
                        icon = "↕",
                        title = "列表排序",
                        subtitle = "稍后接入拖拽",
                        onClick = { manageMode = true }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TodoFilterSortSheet(
    currentState: TodoFilterState,
    onApply: (TodoFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var statusName by rememberSaveable(currentState) { mutableStateOf(currentState.status.name) }
    var priorityName by rememberSaveable(currentState) { mutableStateOf(currentState.priority.name) }
    var sortName by rememberSaveable(currentState) { mutableStateOf(currentState.sortMode.name) }
    val draftState = remember(statusName, priorityName, sortName) {
        TodoFilterState(
            status = TodoStatusFilter.valueOf(statusName),
            priority = TodoPriorityFilter.valueOf(priorityName),
            sortMode = TodoSortMode.valueOf(sortName)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            BottomSheetTitle(
                title = "筛选与排序",
                subtitle = "只影响当前页面展示，不会修改任务数据。",
                onClose = onDismiss
            )
            SheetSection(title = "状态") {
                TodoStatusFilter.entries.forEach { option ->
                    SheetChip(text = option.label, selected = option.name == statusName, onClick = { statusName = option.name })
                }
            }
            SheetSection(title = "优先级") {
                TodoPriorityFilter.entries.forEach { option ->
                    SheetChip(text = option.label, selected = option.name == priorityName, onClick = { priorityName = option.name })
                }
            }
            SheetSection(title = "排序方式") {
                TodoSortMode.entries.forEach { option ->
                    SheetChoiceRow(
                        title = option.label,
                        subtitle = when (option) {
                            TodoSortMode.Time -> "先显示有明确时间的任务"
                            TodoSortMode.Priority -> "高优先级任务靠前"
                            TodoSortMode.Created -> "最近创建的任务靠前"
                            TodoSortMode.Manual -> "保留当前列表顺序"
                        },
                        selected = option.name == sortName,
                        onClick = { sortName = option.name }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    modifier = Modifier.weight(1f).height(50.dp),
                    onClick = {
                        statusName = TodoStatusFilter.All.name
                        priorityName = TodoPriorityFilter.All.name
                        sortName = TodoSortMode.Time.name
                    }
                ) { Text("重置") }
                Button(modifier = Modifier.weight(1f).height(50.dp), onClick = { onApply(draftState) }) {
                    Text("应用筛选", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun BottomSheetTitle(title: String, subtitle: String, onClose: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.size(width = 42.dp, height = 4.dp).align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.56f)
        ) {}
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClose) { Text("关闭") }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.listSection(
    title: String,
    options: List<TodoListOption>,
    selectedListId: String,
    onSelect: (String) -> Unit
) {
    if (options.isEmpty()) return
    item(key = "title-$title") {
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
    items(options, key = { it.id }) { option ->
        ListPickerRow(option = option, selected = option.id == selectedListId, onClick = { onSelect(option.id) })
    }
}

@Composable
private fun ListPickerRow(option: TodoListOption, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.74f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (selected) 0.0f else 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(34.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when (option.kind) {
                                TodoListKind.SMART -> "★"
                                TodoListKind.SYSTEM -> "□"
                                TodoListKind.CUSTOM -> "▤"
                            },
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(option.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = when (option.kind) {
                            TodoListKind.SMART -> "智能视图"
                            TodoListKind.SYSTEM -> "系统默认清单"
                            TodoListKind.CUSTOM -> "自定义清单"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${option.count} 项", style = MaterialTheme.typography.labelMedium)
                if (selected) Text("✓", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ManageListRow(
    option: TodoListOption,
    editing: Boolean,
    renameText: String,
    onRenameTextChange: (String) -> Unit,
    onStartRename: () -> Unit,
    onCancelRename: () -> Unit,
    onSaveRename: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (editing) {
                BasicTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = renameText,
                    onValueChange = onRenameTextChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold),
                    decorationBox = { innerTextField ->
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
                                if (renameText.isBlank()) Text("输入新列表名", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                innerTextField()
                            }
                        }
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = onCancelRename) { Text("取消") }
                    Button(modifier = Modifier.weight(1f), onClick = onSaveRename) { Text("保存") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(34.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) { Text("▤", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(option.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${option.count} 项任务", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onStartRename) { Text("重命名") }
                    TextButton(onClick = onRemove) { Text("删除", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun SecondarySheetAction(
    modifier: Modifier,
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SheetSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun SheetChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Text(modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp), text = text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SheetChoiceRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Row(modifier = Modifier.padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (selected) "●" else "○", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
