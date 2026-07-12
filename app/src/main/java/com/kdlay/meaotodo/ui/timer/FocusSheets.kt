package com.kdlay.meaotodo.ui.timer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.core.settings.PomodoroPreferences
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.data.repository.PomodoroRepository
import com.kdlay.meaotodo.ui.components.MeaoBottomSheet
import com.kdlay.meaotodo.ui.components.MeaoChoiceChip
import com.kdlay.meaotodo.ui.components.WheelPickerColumn

@Composable
internal fun FocusTimerSetupSheet(
    preferences: PomodoroPreferences,
    activeLocked: Boolean,
    onDismiss: () -> Unit,
    onApply: (Int, Int, Int) -> Unit
) {
    var focus by rememberSaveable(preferences.focusDurationMinutes) { mutableIntStateOf(preferences.focusDurationMinutes) }
    var rest by rememberSaveable(preferences.breakDurationMinutes) { mutableIntStateOf(preferences.breakDurationMinutes) }
    var rounds by rememberSaveable(preferences.targetFocusCount) { mutableIntStateOf(preferences.targetFocusCount) }

    MeaoBottomSheet(
        title = "设置本轮方案",
        subtitle = if (activeLocked) "当前阶段不会被改动，新方案从下一次开始生效。" else "滚动数字，配置专注、休息时长与轮数。",
        onDismiss = onDismiss,
        primaryActionText = "保存方案",
        secondaryActionText = "取消",
        onPrimaryAction = { onApply(focus, rest, rounds) },
        onSecondaryAction = onDismiss
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WheelPickerColumn(
                title = "专注 / 分钟",
                values = (1..180).toList(),
                selectedValue = focus,
                onCenteredValueChange = { focus = it },
                columnWidth = 90.dp,
                itemWidth = 68.dp,
                viewportHeight = 176.dp
            )
            WheelPickerColumn(
                title = "休息 / 分钟",
                values = (1..120).toList(),
                selectedValue = rest,
                onCenteredValueChange = { rest = it },
                columnWidth = 90.dp,
                itemWidth = 68.dp,
                viewportHeight = 176.dp
            )
            WheelPickerColumn(
                title = "轮数",
                values = (1..12).toList(),
                selectedValue = rounds,
                onCenteredValueChange = { rounds = it },
                itemLabel = { it.toString() },
                columnWidth = 82.dp,
                itemWidth = 62.dp,
                viewportHeight = 176.dp
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(focusPresets()) { preset ->
                MeaoChoiceChip(
                    text = preset.label,
                    selected = focus == preset.focus && rest == preset.rest && rounds == preset.rounds,
                    onClick = {
                        focus = preset.focus
                        rest = preset.rest
                        rounds = preset.rounds
                    }
                )
            }
        }
    }
}

@Composable
internal fun FocusTaskPickerSheet(
    tasks: List<TaskEntity>,
    selectedTaskId: String?,
    timerActive: Boolean,
    onSelect: (TaskEntity) -> Unit,
    onStart: (TaskEntity) -> Unit,
    onQuickAdd: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var quickTitle by rememberSaveable { mutableStateOf("") }
    var quickPomodoros by rememberSaveable { mutableIntStateOf(1) }
    val focusManager = LocalFocusManager.current
    val visible = remember(tasks, query) {
        tasks.filter {
            query.isBlank() || it.title.contains(query, ignoreCase = true) || it.note.contains(query, ignoreCase = true)
        }.sortedWith(focusTaskComparator())
    }

    MeaoBottomSheet(
        title = "选择专注任务",
        subtitle = if (timerActive) "当前计时不会切换任务；选择项将作为下一次专注目标。" else "从任务列表选择，或快速创建一个专注任务。",
        onDismiss = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("快速创建", fontWeight = FontWeight.Bold)
                FocusTextField(
                    value = quickTitle,
                    placeholder = "例如：复习统计物理第四章",
                    onValueChange = { quickTitle = it },
                    onDone = {
                        val title = quickTitle.trim()
                        if (title.isNotEmpty()) {
                            onQuickAdd(title, quickPomodoros)
                            quickTitle = ""
                            quickPomodoros = 1
                            focusManager.clearFocus()
                        }
                    }
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items((1..6).toList()) { value ->
                        MeaoChoiceChip(
                            text = "$value 番茄",
                            selected = quickPomodoros == value,
                            onClick = { quickPomodoros = value }
                        )
                    }
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val title = quickTitle.trim()
                        if (title.isNotEmpty()) {
                            onQuickAdd(title, quickPomodoros)
                            quickTitle = ""
                            quickPomodoros = 1
                            focusManager.clearFocus()
                        }
                    },
                    enabled = quickTitle.isNotBlank()
                ) { Text("添加任务") }
            }
        }

        FocusTextField(
            value = query,
            placeholder = "搜索任务",
            onValueChange = { query = it },
            onDone = { focusManager.clearFocus() }
        )

        if (visible.isEmpty()) {
            Text("没有匹配的任务。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 430.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.id }) { task ->
                    FocusTaskRow(
                        task = task,
                        selected = task.id == selectedTaskId,
                        timerActive = timerActive,
                        onSelect = { onSelect(task) },
                        onStart = { onStart(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    BasicTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { inner ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
            }
        }
    )
}

@Composable
internal fun FocusHistorySheet(
    summary: PomodoroSummary,
    sessions: List<PomodoroSessionEntity>,
    onDismiss: () -> Unit
) {
    MeaoBottomSheet(
        title = "专注记录",
        subtitle = "展示今天的汇总和最近完成、放弃的阶段。",
        onDismiss = onDismiss
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FocusMetric(Modifier.weight(1f), summary.finishedFocusCount.toString(), "完成")
            FocusMetric(Modifier.weight(1f), "${summary.focusSeconds / 60}", "分钟")
            FocusMetric(Modifier.weight(1f), summary.cancelledFocusCount.toString(), "放弃")
        }
        if (sessions.isEmpty()) {
            Text("还没有专注记录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 430.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions.take(10), key = { it.id }) { session ->
                    FocusHistoryRow(session)
                }
            }
        }
    }
}

@Composable
private fun FocusHistoryRow(session: PomodoroSessionEntity) {
    val isBreak = session.type == PomodoroRepository.TYPE_BREAK
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = if (isBreak) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (isBreak) "休" else "专", fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    session.titleSnapshot ?: if (isBreak) "休息" else "空白专注",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${formatSessionTime(session.startedAt)} · ${focusSessionStatus(session.status)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("${session.actualDurationSeconds / 60} 分", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun FocusSettingsSheet(
    preferences: PomodoroPreferences,
    onToggleClockStyle: () -> Unit,
    onEditTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    MeaoBottomSheet(
        title = "专注设置",
        subtitle = "这些设置保存在本机，并在下一次专注时继续使用。",
        onDismiss = onDismiss
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onEditTimer),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        ) {
            Row(
                modifier = Modifier.padding(15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("时间与轮数", fontWeight = FontWeight.SemiBold)
                    Text(
                        "${preferences.focusDurationMinutes}/${preferences.breakDurationMinutes} 分钟 · ${preferences.targetFocusCount} 轮",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("修改", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("时钟样式", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeaoChoiceChip(
                    text = "数字钟",
                    selected = preferences.clockStyle == PomodoroPreferences.DEFAULT_CLOCK_STYLE,
                    onClick = {
                        if (preferences.clockStyle != PomodoroPreferences.DEFAULT_CLOCK_STYLE) onToggleClockStyle()
                    }
                )
                MeaoChoiceChip(
                    text = "翻页钟",
                    selected = preferences.clockStyle == PomodoroPreferences.CLOCK_STYLE_FLIP,
                    onClick = {
                        if (preferences.clockStyle != PomodoroPreferences.CLOCK_STYLE_FLIP) onToggleClockStyle()
                    }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("阶段结束通知", fontWeight = FontWeight.SemiBold)
                Text(
                    if (preferences.notificationsEnabled) "已开启：专注或休息结束时发送系统通知" else "未开启：可在应用设置中开启系统通知",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            "沉浸模式仅在计时进行时可用。进入后自动横屏，屏幕只保留倒计时，轻触任意位置退出。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class FocusPreset(val label: String, val focus: Int, val rest: Int, val rounds: Int)

private fun focusPresets(): List<FocusPreset> = listOf(
    FocusPreset("轻量 15/5 × 3", 15, 5, 3),
    FocusPreset("经典 25/5 × 4", 25, 5, 4),
    FocusPreset("深度 50/10 × 2", 50, 10, 2),
    FocusPreset("长时 90/15 × 1", 90, 15, 1)
)
