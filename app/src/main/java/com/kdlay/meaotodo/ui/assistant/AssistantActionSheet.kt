package com.kdlay.meaotodo.ui.assistant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.domain.assistant.AssistantActionType
import com.kdlay.meaotodo.domain.assistant.PendingAction
import com.kdlay.meaotodo.ui.components.MeaoBottomSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun AssistantActionSheet(
    action: PendingAction,
    position: Int,
    total: Int,
    enabled: Boolean,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDiscard: () -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    MeaoBottomSheet(
        title = "准备执行",
        subtitle = "所有数据操作都需要确认。当前草稿 ${position + 1} / $total。",
        onDismiss = onDismiss,
        primaryActionText = "确认执行",
        secondaryActionText = "编辑草稿",
        onPrimaryAction = onConfirm,
        onSecondaryAction = onEdit
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = actionTypeLabel(action.type),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (action.explanation.isNotBlank()) {
                    Text(
                        text = action.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                actionDetails(action).forEach { detail ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            text = detail,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        if (total > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onPrevious ?: {}, enabled = onPrevious != null) { Text("上一个") }
                TextButton(onClick = onNext ?: {}, enabled = onNext != null) { Text("下一个") }
            }
        }

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onDiscard,
            enabled = enabled
        ) {
            Text("放弃这项操作", color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun actionTypeLabel(type: AssistantActionType): String = when (type) {
    AssistantActionType.CREATE_TASK -> "新建任务"
    AssistantActionType.RECORD_EXPENSE -> "记录支出"
    AssistantActionType.START_FOCUS -> "开始专注"
    AssistantActionType.RESCHEDULE_TASK -> "调整日期"
    AssistantActionType.COMPLETE_TASK -> "完成任务"
}

private fun actionDetails(action: PendingAction): List<String> = buildList {
    action.dueAt?.let { add("日期：${SimpleDateFormat("M 月 d 日", Locale.getDefault()).format(Date(it))}") }
    action.priority?.let { add("优先级：${priorityLabel(it)}") }
    action.estimatedPomodoros?.let { add("预计番茄：$it") }
    action.amountCents?.let { add("金额：${formatMoney(it)}") }
    action.category?.takeIf { it.isNotBlank() }?.let { add("分类：$it") }
    action.focusMinutes?.let { add("专注：$it 分钟") }
    action.note?.takeIf { it.isNotBlank() }?.let { add("备注：$it") }
}

private fun priorityLabel(priority: Int): String = when (priority) {
    3 -> "高"
    2 -> "中"
    1 -> "低"
    else -> "普通"
}

private fun formatMoney(cents: Long): String =
    if (cents % 100L == 0L) "¥${cents / 100}" else "¥%.2f".format(cents / 100.0)
