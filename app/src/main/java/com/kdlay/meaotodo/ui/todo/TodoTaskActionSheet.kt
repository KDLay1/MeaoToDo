package com.kdlay.meaotodo.ui.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.ui.components.MeaoActionRow
import com.kdlay.meaotodo.ui.components.MeaoBottomSheet

@Composable
internal fun TodoTaskActionSheet(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onStartFocus: () -> Unit,
    onToggleDone: () -> Unit,
    onRemove: () -> Unit,
    onCopyShell: () -> Unit = onDismiss,
    onMoveShell: () -> Unit = onDismiss,
    onArchiveShell: () -> Unit = onDismiss,
    onPinTodayShell: () -> Unit = onDismiss
) {
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
                subtitle = "后续会同步到看板的今日重点模块",
                onClick = onPinTodayShell
            )
            MeaoActionRow(
                icon = "⇄",
                title = "移动到列表",
                subtitle = "后续接入列表选择底部弹层",
                onClick = onMoveShell
            )
            MeaoActionRow(
                icon = "⧉",
                title = "复制任务",
                subtitle = "后续会复制标题、备注和元信息",
                onClick = onCopyShell
            )
            MeaoActionRow(
                icon = "□",
                title = "归档",
                subtitle = "后续会进入归档任务区域",
                onClick = onArchiveShell
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
            Text(
                text = "提示：复制、移动、归档和今日重点目前先作为二级菜单入口壳子，后续接入真实数据逻辑。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
