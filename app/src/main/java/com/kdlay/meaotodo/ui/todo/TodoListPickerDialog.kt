package com.kdlay.meaotodo.ui.todo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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

@Composable
internal fun TodoListPickerDialog(
    listOptions: List<TodoListOption>,
    selectedListId: String,
    onSelect: (String) -> Unit,
    onAddList: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("切换清单", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    options = listOptions.filter { it.kind == TodoListKind.CUSTOM },
                    selectedListId = selectedListId,
                    onSelect = {
                        onSelect(it)
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onAddList()
                }
            ) {
                Text("＋ 新建清单")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
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
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
    items(options, key = { it.id }) { option ->
        ListPickerRow(
            option = option,
            selected = option.id == selectedListId,
            onClick = { onSelect(option.id) }
        )
    }
}

@Composable
private fun ListPickerRow(
    option: TodoListOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (selected) 0.0f else 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(option.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${option.count} 项", style = MaterialTheme.typography.labelMedium)
                if (selected) Text("✓", fontWeight = FontWeight.Bold)
            }
        }
    }
}
