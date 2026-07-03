package com.kdlay.meaotodo.ui.todo

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdlay.meaotodo.data.local.entity.DEFAULT_TASK_LIST_ID
import com.kdlay.meaotodo.data.local.entity.TaskEntity

@Composable
internal fun TodoHeader(
    tasks: List<TaskEntity>,
    selectedList: TodoListOption,
    selectedTasks: List<TaskEntity>
) {
    val pendingCount = tasks.count { !it.isDone }
    val completedCount = tasks.count { it.isDone }
    val todayCount = tasks.count { !it.isDone && it.dueAt?.let(::isToday) == true }
    val listDescription = when (selectedList.kind) {
        TodoListKind.SMART -> "智能视图"
        TodoListKind.SYSTEM -> "系统默认清单"
        TodoListKind.CUSTOM -> "自定义清单"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = selectedList.label,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$listDescription · 今天 $todayCount 项 · 待办 $pendingCount 项 · 已完成 $completedCount 项",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        text = "${selectedTasks.size} 项",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(modifier = Modifier.weight(1f), value = pendingCount.toString(), label = "待处理")
                MiniStat(modifier = Modifier.weight(1f), value = todayCount.toString(), label = "今天")
                MiniStat(modifier = Modifier.weight(1f), value = completedCount.toString(), label = "完成")
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.56f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f))
        }
    }
}

@Composable
internal fun ListSwitcher(
    listOptions: List<TodoListOption>,
    selectedListId: String,
    onSelect: (String) -> Unit,
    onAddList: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOptions.forEach { option ->
            ListChip(option = option, selected = selectedListId == option.id, onClick = { onSelect(option.id) })
        }
        Surface(
            modifier = Modifier.clickable(onClick = onAddList),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                text = "新建清单",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ListChip(option: TodoListOption, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = option.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Surface(
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    text = option.count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
internal fun QuickAddBar(
    title: String,
    onTitleChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenFullEditor: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val canSubmit = title.isNotBlank()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(46.dp).clickable(onClick = onOpenFullEditor),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Box(contentAlignment = Alignment.Center) { Text("+", fontSize = 30.sp, fontWeight = FontWeight.Light) }
            }
            BasicTextField(
                modifier = Modifier.weight(1f),
                value = title,
                onValueChange = onTitleChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (canSubmit) {
                            onSubmit()
                            focusManager.clearFocus()
                        }
                    }
                ),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (!canSubmit) {
                            Text(
                                text = "添加一件今天要做的小事",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (canSubmit) {
                ActionPill(
                    text = "添加",
                    onClick = {
                        onSubmit()
                        focusManager.clearFocus()
                    },
                    emphasis = true
                )
            }
        }
    }
}

@Composable
internal fun TodoTaskList(
    groups: TodoGroups,
    selectedList: TodoListOption,
    listOptions: List<TodoListOption>,
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
    val selectedTasks = groups.tasksFor(selectedList.id)
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (selectedTasks.isEmpty()) {
            item(key = "empty-${selectedList.id}") { EmptyTodoCard(selectedList = selectedList) }
        } else if (selectedList.id == SMART_ALL) {
            taskSection("已过期", groups.overdue, listOptions, onCheckedChange, onEdit, onRemove, onStartFocus, onDuplicate, onMove, onArchive, onPinToday)
            taskSection("今天", groups.today, listOptions, onCheckedChange, onEdit, onRemove, onStartFocus, onDuplicate, onMove, onArchive, onPinToday)
            taskSection("无日期", groups.unscheduled, listOptions, onCheckedChange, onEdit, onRemove, onStartFocus, onDuplicate, onMove, onArchive, onPinToday)
            taskSection("未来", groups.upcoming, listOptions, onCheckedChange, onEdit, onRemove, onStartFocus, onDuplicate, onMove, onArchive, onPinToday)
            taskSection("已完成", groups.completed, listOptions, onCheckedChange, onEdit, onRemove, onStartFocus, onDuplicate, onMove, onArchive, onPinToday)
        } else {
            taskSection(selectedList.label, selectedTasks, listOptions, onCheckedChange, onEdit, onRemove, onStartFocus, onDuplicate, onMove, onArchive, onPinToday)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.taskSection(
    title: String,
    tasks: List<TaskEntity>,
    listOptions: List<TodoListOption>,
    onCheckedChange: (TaskEntity, Boolean) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onRemove: (TaskEntity) -> Unit,
    onStartFocus: (TaskEntity) -> Unit,
    onDuplicate: (TaskEntity) -> Unit,
    onMove: (TaskEntity, String) -> Unit,
    onArchive: (TaskEntity) -> Unit,
    onPinToday: (TaskEntity) -> Unit
) {
    if (tasks.isEmpty()) return
    val sectionKey = "section-$title"
    item(key = sectionKey) { SectionHeader(title = title, count = tasks.size) }
    items(tasks, key = { task -> "$sectionKey-${task.id}" }) { task ->
        TaskRow(
            task = task,
            listOptions = listOptions,
            onCheckedChange = { isDone -> onCheckedChange(task, isDone) },
            onEdit = { onEdit(task) },
            onRemove = { onRemove(task) },
            onStartFocus = { onStartFocus(task) },
            onDuplicate = { onDuplicate(task) },
            onMove = { targetListId -> onMove(task, targetListId) },
            onArchive = { onArchive(task) },
            onPinToday = { onPinToday(task) }
        )
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    val icon = when (title) {
        "今天" -> "☼"
        "已过期" -> "!"
        "无日期" -> "▣"
        "未来" -> "↗"
        "已完成" -> "✓"
        else -> "▤"
    }
    val accent = when (title) {
        "已完成" -> MaterialTheme.colorScheme.tertiary
        "已过期" -> MaterialTheme.colorScheme.error
        "今天" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = accent.copy(alpha = 0.14f), contentColor = accent) {
                Box(contentAlignment = Alignment.Center) { Text(icon, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(text = count.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(text = if (title == "已完成") "⌄" else "⌃", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
internal fun EmptyTodoCard(selectedList: TodoListOption) {
    val message = when (selectedList.id) {
        SMART_ALL -> "现在还没有任务，先把脑子里的事放下来。"
        DEFAULT_TASK_LIST_ID -> "收集箱是系统默认清单，用来临时存放还没分类的任务。"
        SMART_TODAY -> "今天没有待办。可以给自己留一点空白。"
        SMART_UPCOMING -> "还没有未来任务。设置截止日期后会显示在这里。"
        SMART_COMPLETED -> "还没有完成任务。完成后会在这里安静地收起来。"
        else -> "这个清单还没有任务。添加一件真正想完成的小事吧。"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                Text(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), text = "Meao", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Text(text = "${selectedList.label}为空", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun TaskRow(
    task: TaskEntity,
    listOptions: List<TodoListOption>,
    onCheckedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onStartFocus: () -> Unit,
    onDuplicate: () -> Unit,
    onMove: (String) -> Unit,
    onArchive: () -> Unit,
    onPinToday: () -> Unit
) {
    var showActionSheet by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        color = if (task.isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (task.isDone) 0.14f else 0.18f)),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompleteButton(checked = task.isDone, onCheckedChange = onCheckedChange)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                        color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        modifier = Modifier.clickable { showActionSheet = true },
                        text = "⋮",
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (task.note.isNotBlank()) {
                    Text(
                        text = task.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                MetadataRow(task = task)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!task.isDone) {
                        ActionPill(text = "开始专注", onClick = onStartFocus, emphasis = true)
                    }
                    ActionPill(text = "更多", onClick = { showActionSheet = true })
                }
            }
        }
    }

    if (showActionSheet) {
        TodoTaskActionSheet(
            task = task,
            listOptions = listOptions,
            onDismiss = { showActionSheet = false },
            onEdit = onEdit,
            onStartFocus = onStartFocus,
            onToggleDone = { onCheckedChange(!task.isDone) },
            onRemove = onRemove,
            onDuplicate = onDuplicate,
            onMoveToList = onMove,
            onArchive = onArchive,
            onPinToday = onPinToday
        )
    }
}

@Composable
private fun CompleteButton(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Box(modifier = Modifier.size(28.dp).clickable { onCheckedChange(!checked) }, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.size(25.dp),
            shape = CircleShape,
            color = if (checked) MaterialTheme.colorScheme.tertiary else Color.Transparent,
            contentColor = if (checked) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.outline,
            border = if (checked) null else BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (checked) Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetadataRow(task: TaskEntity) {
    val metadata = buildList {
        task.dueAt?.let { add("截止 ${formatDueAt(task)}") }
        if (task.priority > 0) add("${priorityLabel(task.priority)}优先级")
        if (task.estimatedPomodoros > 0) add("预计 ${task.estimatedPomodoros} 番茄")
        if (task.actualPomodoros > 0) add("已专注 ${task.actualPomodoros}")
    }
    if (metadata.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        metadata.forEach { label -> MetadataBadge(label = label) }
    }
}

@Composable
private fun MetadataBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ActionPill(
    text: String,
    onClick: () -> Unit,
    danger: Boolean = false,
    emphasis: Boolean = false
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = when {
            emphasis -> MaterialTheme.colorScheme.secondaryContainer
            danger -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
        },
        contentColor = when {
            emphasis -> MaterialTheme.colorScheme.onSecondaryContainer
            danger -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
