package com.kdlay.meaotodo.ui.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.domain.assistant.AssistantActionType
import com.kdlay.meaotodo.domain.assistant.PendingAction
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun PendingActionEditorDialog(
    action: PendingAction,
    onDismiss: () -> Unit,
    onSave: (PendingAction) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { isLenient = false } }
    var title by remember(action) { mutableStateOf(action.title) }
    var note by remember(action) { mutableStateOf(action.note.orEmpty()) }
    var date by remember(action) { mutableStateOf(action.dueAt?.let { dateFormat.format(Date(it)) }.orEmpty()) }
    var priority by remember(action) { mutableStateOf(action.priority?.toString().orEmpty()) }
    var pomodoros by remember(action) { mutableStateOf(action.estimatedPomodoros?.toString().orEmpty()) }
    var amount by remember(action) { mutableStateOf(action.amountCents?.let { BigDecimal(it).movePointLeft(2).toPlainString() }.orEmpty()) }
    var category by remember(action) { mutableStateOf(action.category.orEmpty()) }
    var focusMinutes by remember(action) { mutableStateOf(action.focusMinutes?.toString().orEmpty()) }
    var error by remember(action) { mutableStateOf<String?>(null) }

    fun buildAction(): PendingAction? = runCatching {
        val dueAt = date.trim().takeIf { it.isNotBlank() }?.let { dateFormat.parse(it)?.time ?: error("日期无效") }
        val amountCents = amount.trim().takeIf { it.isNotBlank() }?.toBigDecimal()
            ?.multiply(BigDecimal(100))?.setScale(0, RoundingMode.HALF_UP)?.longValueExact()
        action.copy(
            title = title.trim().ifBlank { error("标题不能为空") },
            note = note.trim().takeIf { it.isNotBlank() },
            dueAt = dueAt,
            priority = priority.trim().takeIf { it.isNotBlank() }?.toInt()?.coerceIn(0, 3),
            estimatedPomodoros = pomodoros.trim().takeIf { it.isNotBlank() }?.toInt()?.coerceIn(0, 24),
            amountCents = amountCents,
            category = category.trim().takeIf { it.isNotBlank() },
            focusMinutes = focusMinutes.trim().takeIf { it.isNotBlank() }?.toInt()?.coerceIn(1, 180)
        )
    }.onFailure { error = it.message ?: "输入格式无效" }.getOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑待确认操作") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("标题") })
                when (action.type) {
                    AssistantActionType.CREATE_TASK -> {
                        OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("备注") })
                        OutlinedTextField(value = date, onValueChange = { date = it }, modifier = Modifier.fillMaxWidth(), label = { Text("截止日期 YYYY-MM-DD") })
                        OutlinedTextField(value = priority, onValueChange = { priority = it }, modifier = Modifier.fillMaxWidth(), label = { Text("优先级 0-3") })
                        OutlinedTextField(value = pomodoros, onValueChange = { pomodoros = it }, modifier = Modifier.fillMaxWidth(), label = { Text("预计番茄数 0-24") })
                    }
                    AssistantActionType.RECORD_EXPENSE -> {
                        OutlinedTextField(value = amount, onValueChange = { amount = it }, modifier = Modifier.fillMaxWidth(), label = { Text("金额（元）") })
                        OutlinedTextField(value = category, onValueChange = { category = it }, modifier = Modifier.fillMaxWidth(), label = { Text("分类") })
                        OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("备注") })
                    }
                    AssistantActionType.START_FOCUS ->
                        OutlinedTextField(value = focusMinutes, onValueChange = { focusMinutes = it }, modifier = Modifier.fillMaxWidth(), label = { Text("专注分钟数") })
                    AssistantActionType.RESCHEDULE_TASK ->
                        OutlinedTextField(value = date, onValueChange = { date = it }, modifier = Modifier.fillMaxWidth(), label = { Text("新日期 YYYY-MM-DD") })
                    AssistantActionType.COMPLETE_TASK -> Unit
                }
                error?.let { Text("错误：$it") }
            }
        },
        confirmButton = {
            Button(onClick = { buildAction()?.let(onSave) }) { Text("保存草稿") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } }
    )
}
