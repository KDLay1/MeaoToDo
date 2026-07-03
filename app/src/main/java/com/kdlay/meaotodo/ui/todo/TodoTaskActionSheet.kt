package com.kdlay.meaotodo.ui.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.ui.components.MeaoActionRow
import com.kdlay.meaotodo.ui.components.MeaoBottomSheet

@Composable
internal fun TodoTaskActionSheet(
    task: TaskEntity,
    listOptions: List<TodoListOption>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onStartFocus: () -> Unit,
    onToggleDone: () -> Unit,
    onRemove: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveToList: (String) -> Unit,
    onArchive: () -> Unit,
    onPinToday: () -> Unit
) {
    var showMoveSheet by rememberSaveable { mutableStateOf(false) }

    if (showMoveSheet) {
        TodoMoveTaskSheet(
            task = task,
            listOptions = listOptions,
            onDismiss = { showMoveSheet = false },
            onMoveToList = { targetListId ->
                showMoveSheet = false
                onDismiss()
                onMoveToList(targetListId)
            }
        )
    }

    MeaoBottomSheet(
        title = "任务更多操作",
        subtitle = task.title,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!task.isDone) {
                MeaoActionRow(
                    icon = "▶",
                    title = "开始专注",
                    subtitle = "以这个任务作为当前番茄目标",
                    onClick = {
                        onDismiss()
                        onStartFocus()
                    }
                )
            }
            MeaoActionRow(
                icon = "✎",
                title = "编辑",
                subtitle = "修改标题、备注、日期、优先级和预计番茄",
                onClick = {
                    onDismiss()
                    onEdit()
                }
            )
            MeaoActionRow(
                icon = "✓",
                title = if (task.isDone) "标记为待办" else "标记为完成",
                subtitle = if (task.isDone) "把任务移回待办区域" else "完成后会进入已完成分组",
                onClick = {
                    onDismiss()
                    onToggleDone()
                }
            )
            MeaoActionRow(
                icon = "★",
                title = "设为今日重点",
                subtitle = "自动设为今天任务，并提高到中优先级以上",
                onClick = {
                    onDismiss()
                    onPinToday()
                }
            )
            MeaoActionRow(
                icon = "⇄",
                title = "移动到列表",
                subtitle = "把任务移动到另一个项目或收集箱",
                onClick = { showMoveSheet = true }
            )
            MeaoActionRow(
                icon = "⧉",
                title = "复制任务",
                subtitle = "复制标题、备注、时间、优先级和预计番茄",
                onClick = {
                    onDismiss()
                    onDuplicate()
                }
            )
            MeaoActionRow(
                icon = "□",
                title = "归档",
                subtitle = "从当前活跃任务列表移除",
                onClick = {
                    onDismiss()
                    onArchive()
                }
            )
            MeaoActionRow(
                icon = "!",
                title = "删除",
                subtitle = "从当前任务列表移除",
                danger = true,
                onClick = {
                    onDismiss()
                    onRemove()
                }
            )
        }
    }
}

@Composable
private fun TodoMoveTaskSheet(
    task: TaskEntity,
    listOptions: List<TodoListOption>,
    onDismiss: () -> Unit,
    onMoveToList: (String) -> Unit
) {
    val realLists = listOptions.taskListOptions()
    MeaoBottomSheet(
        title = "移动任务",
        subtitle = "选择「${task.title}」要移动到的列表。",
        onDismiss = onDismiss
    ) {
        LazyColumn(
            modifier = Modifier.heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(realLists, key = { it.id }) { option ->
                MeaoActionRow(
                    icon = if (option.id == task.listId) "✓" else "▤",
                    title = option.label,
                    subtitle = if (option.id == task.listId) "当前所在列表" else "移动到这里 · ${option.count} 项",
                    trailing = if (option.id == task.listId) "当前" else null,
                    onClick = {
                        if (option.id != task.listId) {
                            onMoveToList(option.id)
                        }
                    }
                )
            }
        }
        Text(
            text = "删除自定义列表时，列表里的任务会自动移回收集箱。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
