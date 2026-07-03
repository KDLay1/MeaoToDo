package com.kdlay.meaotodo.ui.todo

import androidx.compose.runtime.Composable
import com.kdlay.meaotodo.data.local.entity.TaskEntity

@Composable
internal fun TaskRow(
    task: TaskEntity,
    onCheckedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onStartFocus: () -> Unit
) {
    TaskRow(
        task = task,
        listOptions = emptyList(),
        onCheckedChange = onCheckedChange,
        onEdit = onEdit,
        onRemove = onRemove,
        onStartFocus = onStartFocus,
        onDuplicate = {},
        onMove = {},
        onArchive = {},
        onPinToday = {}
    )
}
